package skaro.trainer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

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
class PokemonMoveIntegrationTest {

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

	// Spec 001 — Escenario: Mover del Baúl al Equipo Activo con espacio disponible
	@Test
	void moverDelBaulAlEquipoActivoConEspacioDisponible() {
		Trainer trainer = trainerRepository.findById(1L).orElseThrow();
		PokemonInstance instance = new PokemonInstance(trainer, "eevee");
		instance.setIvs(new Ivs(1, 1, 1, 1, 1, 1));
		instance.setEvs(new Evs(0, 0, 0, 0, 0, 0));
		instance.setNature(Nature.HARDY);
		instance.setAbility("run-away");
		instance.setLocation(Location.BOX);
		PokemonInstance saved = pokemonInstanceRepository.save(instance);

		webTestClient.patch().uri("/trainers/1/pokemon/" + saved.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"location\": \"team\"}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(saved.getId())
				.jsonPath("$.location").isEqualTo("team");
	}

	// Spec 001 — Escenario: Mover del Equipo Activo al Baúl
	@Test
	void moverDelEquipoActivoAlBaul() {
		Trainer trainer = trainerRepository.findById(1L).orElseThrow();
		PokemonInstance instance = new PokemonInstance(trainer, "eevee");
		instance.setIvs(new Ivs(1, 1, 1, 1, 1, 1));
		instance.setEvs(new Evs(0, 0, 0, 0, 0, 0));
		instance.setNature(Nature.HARDY);
		instance.setAbility("run-away");
		instance.setLocation(Location.TEAM);
		PokemonInstance saved = pokemonInstanceRepository.save(instance);

		webTestClient.patch().uri("/trainers/1/pokemon/" + saved.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"location\": \"box\"}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(saved.getId())
				.jsonPath("$.location").isEqualTo("box");
	}

}
