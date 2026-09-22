package skaro.trainer.api;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
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
class PokemonCaptureIntegrationTest {

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

	private String payloadValido() {
		return """
				{
				  "species": "pikachu",
				  "ivs": {"hp":1,"attack":2,"defense":3,"specialAttack":4,"specialDefense":5,"speed":6},
				  "evs": {"hp":10,"attack":10,"defense":10,"specialAttack":10,"specialDefense":10,"speed":10},
				  "nature": "HARDY",
				  "ability": "static"
				}
				""";
	}

	// abilities según BR-006 se valida contra la especie real en PokéAPI —
	// enqueue de la respuesta canónica de "pikachu" con la habilidad "static".
	private void enqueuePikachuConHabilidadStatic() {
		mockPokeApiServer.enqueue(new MockResponse()
				.setBody("""
						{"id":25,"name":"pikachu","abilities":[
						  {"is_hidden":false,"slot":1,"ability":{"name":"static","url":"https://pokeapi.co/api/v2/ability/9/"}}
						]}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	// Spec 001 — Escenario: Captura exitosa con espacio en el Equipo Activo
	@Test
	void capturaExitosaConEspacioEnElEquipoActivoAsignaAlEquipo() {
		enqueuePikachuConHabilidadStatic();

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payloadValido())
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.species").isEqualTo("pikachu")
				.jsonPath("$.location").isEqualTo("team")
				.jsonPath("$.id").isNumber();
	}

	// Spec 001 — Escenario: Captura con el Equipo Activo lleno va al Baúl
	@Test
	void capturaConElEquipoActivoLlenoVaAlBaul() {
		Trainer trainer = trainerRepository.findById(1L).orElseThrow();
		for (int i = 0; i < 6; i++) {
			pokemonInstanceRepository.save(rellenoEn(trainer, Location.TEAM));
		}
		enqueuePikachuConHabilidadStatic();

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payloadValido())
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.location").isEqualTo("box");
	}

	// Spec 001 — Escenario: Captura sin espacio en ningún destino
	@Test
	void capturaSinEspacioEnNingunDestino() {
		Trainer trainer = trainerRepository.findById(1L).orElseThrow();
		for (int i = 0; i < 6; i++) {
			pokemonInstanceRepository.save(rellenoEn(trainer, Location.TEAM));
		}
		for (int i = 0; i < 300; i++) {
			pokemonInstanceRepository.save(rellenoEn(trainer, Location.BOX));
		}
		enqueuePikachuConHabilidadStatic();

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payloadValido())
				.exchange()
				.expectStatus().isEqualTo(409)
				.expectBody()
				.jsonPath("$.message").isNotEmpty();

		Assertions.assertEquals(306, pokemonInstanceRepository.count());
	}

	// Spec 001 — Escenario: Captura con EVs por encima del máximo total
	// Cada EV individual queda <=252 (86*5+90=520) a propósito, para aislar
	// BR-003 (suma total) de BR-004 (máximo individual) — un solo EV en 511
	// dispara ambas reglas a la vez y no prueba BR-003 de verdad, porque
	// TrainerRosterService.validateEvs revisa el máximo individual primero.
	@Test
	void capturaConEvsPorEncimaDelMaximoTotal() {
		String payload = """
				{
				  "species": "pikachu",
				  "ivs": {"hp":1,"attack":2,"defense":3,"specialAttack":4,"specialDefense":5,"speed":6},
				  "evs": {"hp":86,"attack":86,"defense":86,"specialAttack":86,"specialDefense":86,"speed":90},
				  "nature": "HARDY",
				  "ability": "static"
				}
				""";

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payload)
				.exchange()
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("BR-003"));

		Assertions.assertEquals(0, pokemonInstanceRepository.count());
	}

	// Spec 001 — Escenario: Captura con un EV individual por encima del máximo
	@Test
	void capturaConUnEvIndividualPorEncimaDelMaximo() {
		String payload = """
				{
				  "species": "pikachu",
				  "ivs": {"hp":1,"attack":2,"defense":3,"specialAttack":4,"specialDefense":5,"speed":6},
				  "evs": {"hp":253,"attack":0,"defense":0,"specialAttack":0,"specialDefense":0,"speed":0},
				  "nature": "HARDY",
				  "ability": "static"
				}
				""";

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payload)
				.exchange()
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.message").isNotEmpty();

		Assertions.assertEquals(0, pokemonInstanceRepository.count());
	}

	// Spec 001 — Escenario: Captura con un IV fuera de rango
	@Test
	void capturaConUnIvFueraDeRango() {
		String payload = """
				{
				  "species": "pikachu",
				  "ivs": {"hp":32,"attack":0,"defense":0,"specialAttack":0,"specialDefense":0,"speed":0},
				  "evs": {"hp":0,"attack":0,"defense":0,"specialAttack":0,"specialDefense":0,"speed":0},
				  "nature": "HARDY",
				  "ability": "static"
				}
				""";

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payload)
				.exchange()
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.message").isNotEmpty();

		Assertions.assertEquals(0, pokemonInstanceRepository.count());
	}

	// Spec 001 — Escenario: Captura con una Habilidad que no pertenece a la especie
	@Test
	void capturaConUnaHabilidadQueNoPerteneceALaEspecie() {
		mockPokeApiServer.enqueue(new MockResponse()
				.setBody("""
						{"id":25,"name":"pikachu","abilities":[
						  {"is_hidden":false,"slot":1,"ability":{"name":"static","url":"https://pokeapi.co/api/v2/ability/9/"}}
						]}
						""")
				.addHeader("Content-Type", "application/json"));

		String payload = """
				{
				  "species": "pikachu",
				  "ivs": {"hp":1,"attack":2,"defense":3,"specialAttack":4,"specialDefense":5,"speed":6},
				  "evs": {"hp":0,"attack":0,"defense":0,"specialAttack":0,"specialDefense":0,"speed":0},
				  "nature": "HARDY",
				  "ability": "levitate"
				}
				""";

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payload)
				.exchange()
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.message").isNotEmpty();

		Assertions.assertEquals(0, pokemonInstanceRepository.count());
	}

	// Spec 001 — Escenario: Captura con más de 4 movimientos
	@Test
	void capturaConMasDeCuatroMovimientos() {
		String payload = """
				{
				  "species": "pikachu",
				  "ivs": {"hp":1,"attack":2,"defense":3,"specialAttack":4,"specialDefense":5,"speed":6},
				  "evs": {"hp":0,"attack":0,"defense":0,"specialAttack":0,"specialDefense":0,"speed":0},
				  "nature": "HARDY",
				  "ability": "static",
				  "moves": ["thunderbolt","quick-attack","iron-tail","agility","thunder"]
				}
				""";

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payload)
				.exchange()
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.message").isNotEmpty();

		Assertions.assertEquals(0, pokemonInstanceRepository.count());
	}

	// Spec 001 — Escenario: Captura con una naturaleza inválida
	@Test
	void capturaConUnaNaturalezaInvalida() {
		String payload = """
				{
				  "species": "pikachu",
				  "ivs": {"hp":1,"attack":2,"defense":3,"specialAttack":4,"specialDefense":5,"speed":6},
				  "evs": {"hp":0,"attack":0,"defense":0,"specialAttack":0,"specialDefense":0,"speed":0},
				  "nature": "COOL",
				  "ability": "static"
				}
				""";

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(payload)
				.exchange()
				.expectStatus().isEqualTo(422)
				.expectBody()
				.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("BR-005"));

		Assertions.assertEquals(0, pokemonInstanceRepository.count());
	}

	// Spec 001 — Escenario: Captura con payload mínimo aplica los valores por defecto
	@Test
	void capturaConPayloadMinimoAplicaLosValoresPorDefecto() {
		mockPokeApiServer.enqueue(new MockResponse()
				.setBody("""
						{"id":25,"name":"pikachu","abilities":[
						  {"is_hidden":false,"slot":1,"ability":{"name":"static","url":"https://pokeapi.co/api/v2/ability/9/"}},
						  {"is_hidden":true,"slot":3,"ability":{"name":"lightning-rod","url":"https://pokeapi.co/api/v2/ability/31/"}}
						]}
						""")
				.addHeader("Content-Type", "application/json"));

		webTestClient.post().uri("/trainers/1/pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"species\": \"pikachu\"}")
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.species").isEqualTo("pikachu")
				.jsonPath("$.ivs.hp").isEqualTo(0)
				.jsonPath("$.evs.hp").isEqualTo(0)
				.jsonPath("$.nature").isEqualTo("HARDY")
				.jsonPath("$.ability").isEqualTo("static")
				.jsonPath("$.shiny").isEqualTo(false)
				.jsonPath("$.gender").isEqualTo("GENDERLESS")
				.jsonPath("$.moves").isEmpty();
	}

	private PokemonInstance rellenoEn(Trainer trainer, Location location) {
		PokemonInstance filler = new PokemonInstance(trainer, "rattata");
		filler.setIvs(new Ivs(1, 1, 1, 1, 1, 1));
		filler.setEvs(new Evs(0, 0, 0, 0, 0, 0));
		filler.setNature(Nature.HARDY);
		filler.setAbility("run-away");
		filler.setLocation(location);
		return filler;
	}

}
