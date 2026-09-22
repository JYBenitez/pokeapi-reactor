package skaro.trainer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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
class PokemonBoxListingIntegrationTest {

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

	// Spec 001 — Escenario: Listar el Baúl
	@Test
	void listarElBaulDevuelveLosEjemplaresAlmacenados() {
		Trainer trainer = trainerRepository.findById(1L).orElseThrow();
		PokemonInstance instance = new PokemonInstance(trainer, "rattata");
		instance.setIvs(new Ivs(1, 1, 1, 1, 1, 1));
		instance.setEvs(new Evs(0, 0, 0, 0, 0, 0));
		instance.setNature(Nature.HARDY);
		instance.setAbility("run-away");
		instance.setLocation(Location.BOX);
		pokemonInstanceRepository.save(instance);

		webTestClient.get().uri("/trainers/1/pokemon?location=box")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].species").isEqualTo("rattata")
				.jsonPath("$[0].location").isEqualTo("box");
	}

}
