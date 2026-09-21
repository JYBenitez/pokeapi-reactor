package skaro.pokeapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;
import skaro.pokeapi.client.PokeApiEntityFactory;
import skaro.pokeapi.resource.pokemon.Pokemon;

public class PokeApiReactorBaseConfigurationTest {

	private MockWebServer mockPokeApiServer;
	private PokeApiReactorBaseConfiguration configuration;
	private WebClient webClient;

	@BeforeEach
	public void setup() throws IOException {
		mockPokeApiServer = new MockWebServer();
		mockPokeApiServer.start();

		configuration = new PokeApiReactorBaseConfiguration();

		PokeApiConfigurationProperties properties = new PokeApiConfigurationProperties();
		properties.setBaseUri(URI.create(String.format("http://localhost:%s", mockPokeApiServer.getPort())));

		Jackson2JsonEncoder encoder = configuration.jsonEncoder();
		Jackson2JsonDecoder decoder = configuration.jsonDecoder();
		webClient = configuration.webClient(HttpClient.create(), encoder, decoder, properties);
	}

	@AfterEach
	public void tearDown() throws IOException {
		mockPokeApiServer.shutdown();
	}

	// Spec 000 — Escenario: cada request saliente a PokéAPI se loguea en INFO con método + URL
	@Test
	public void logueaCadaRequestSalienteEnInfo() {
		Logger requestLogger = (Logger) org.slf4j.LoggerFactory.getLogger(PokeApiEntityFactory.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		requestLogger.addAppender(appender);

		mockPokeApiServer.enqueue(new MockResponse()
				.setBody("{}")
				.addHeader("Content-Type", "application/json"));

		StepVerifier.create(webClient.get().uri("/pokemon/1").retrieve().bodyToMono(Pokemon.class))
			.expectNextCount(1)
			.expectComplete()
			.verify();

		requestLogger.detachAppender(appender);
		List<ILoggingEvent> logged = appender.list;

		assertTrue(logged.stream().anyMatch(event ->
				event.getLevel() == Level.INFO
				&& event.getFormattedMessage().contains("GET")
				&& event.getFormattedMessage().contains("/pokemon/1")));
	}

	// Spec 000 — Escenario: la deserialización usa snake_case automático
	@Test
	public void deserializaPropiedadesSnakeCaseACamelCase() {
		mockPokeApiServer.enqueue(new MockResponse()
				.setBody("{\"base_experience\": 64}")
				.addHeader("Content-Type", "application/json"));

		StepVerifier.create(webClient.get().uri("/pokemon/1").retrieve().bodyToMono(Pokemon.class))
			.expectNextMatches(pokemon -> Integer.valueOf(64).equals(pokemon.getBaseExperience()))
			.expectComplete()
			.verify();
	}

	// Spec 000 — Escenario: deserializar una propiedad desconocida del JSON de origen falla
	// Corregido respecto del hallazgo original de la arqueología (AS-IS rareza #2): antes
	// se ignoraba en silencio. Se preserva el nuevo comportamiento hasta que una spec lo cambie.
	@Test
	public void fallaAlDeserializarUnaPropiedadDesconocida() {
		mockPokeApiServer.enqueue(new MockResponse()
				.setBody("{\"campo_que_no_existe_en_el_dto\": true}")
				.addHeader("Content-Type", "application/json"));

		StepVerifier.create(webClient.get().uri("/pokemon/1").retrieve().bodyToMono(Pokemon.class))
			.expectErrorMatches(error -> error instanceof DecodingException
					&& error.getCause() instanceof UnrecognizedPropertyException)
			.verify();
	}

}
