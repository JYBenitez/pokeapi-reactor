package skaro.trainer.api;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import skaro.trainer.domain.Evs;
import skaro.trainer.domain.Ivs;
import skaro.trainer.domain.Location;
import skaro.trainer.domain.Nature;
import skaro.trainer.domain.PokemonInstance;
import skaro.trainer.domain.Trainer;
import skaro.trainer.persistence.PokemonInstanceRepository;
import skaro.trainer.persistence.TrainerRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class PokemonTeamListingIntegrationTest {

	private static MockWebServer mockPokeApiServer;

	@DynamicPropertySource
	static void pokeApiBaseUri(DynamicPropertyRegistry registry) throws IOException {
		mockPokeApiServer = new MockWebServer();
		mockPokeApiServer.start();
		registry.add("skaro.pokeapi.base-uri", () -> mockPokeApiServer.url("/").toString());
	}

	@AfterAll
	static void shutdownMockServer() throws IOException {
		mockPokeApiServer.shutdown();
	}

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private PokemonInstanceRepository pokemonInstanceRepository;

	@Autowired
	private TrainerRepository trainerRepository;

	@BeforeEach
	void limpiarRoster() {
		pokemonInstanceRepository.deleteAll();
	}

	// Spec 001 — Escenario: Listar el Equipo Activo devuelve vista compuesta
	@Test
	void listarEquipoActivoDevuelveVistaCompuesta() {
		Trainer trainer = trainerRepository.findById(1L).orElseThrow();
		PokemonInstance instance = new PokemonInstance(trainer, "pikachu");
		instance.setIvs(new Ivs(1, 2, 3, 4, 5, 6));
		instance.setEvs(new Evs(10, 10, 10, 10, 10, 10));
		instance.setNature(Nature.HARDY);
		instance.setAbility("static");
		instance.setLocation(Location.TEAM);
		pokemonInstanceRepository.save(instance);

		mockPokeApiServer.enqueue(new MockResponse()
				.setBody("{\"id\":25,\"name\":\"pikachu\"}")
				.addHeader("Content-Type", "application/json"));

		webTestClient.get().uri("/trainers/1/pokemon?location=team")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].instance.species").isEqualTo("pikachu")
				.jsonPath("$[0].instance.location").isEqualTo("team")
				.jsonPath("$[0].species.name").isEqualTo("pikachu");
	}

}
