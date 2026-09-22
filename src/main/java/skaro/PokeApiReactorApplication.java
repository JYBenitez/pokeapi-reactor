package skaro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import reactor.netty.http.client.HttpClient;
import skaro.pokeapi.PokeApiReactorNonCachingConfiguration;

// scanBasePackages se acota a skaro.trainer: skaro.pokeapi trae dos
// @Configuration alternativas (caching/non-caching) que registran el mismo
// bean PokeApiClient — escanearlas ambas colisiona. Se importa
// explícitamente la variante elegida, como espera el propio diseño de la
// librería (un consumidor importa una u otra).
@SpringBootApplication(scanBasePackages = "skaro.trainer")
@Import(PokeApiReactorNonCachingConfiguration.class)
public class PokeApiReactorApplication {

	public static void main(String[] args) {
		SpringApplication.run(PokeApiReactorApplication.class, args);
	}

	// PokeApiReactorBaseConfiguration.webClient(HttpClient, ...) espera que
	// quien consume la librería le provea este bean — nunca existió un
	// contexto Spring real antes (los tests lo armaban a mano con
	// HttpClient.create()). No es tocar skaro.pokeapi, es su contrato de
	// consumo.
	@Bean
	public HttpClient httpClient() {
		return HttpClient.create();
	}

}
