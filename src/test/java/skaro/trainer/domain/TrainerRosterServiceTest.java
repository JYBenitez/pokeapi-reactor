package skaro.trainer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import skaro.pokeapi.client.PokeApiClient;
import skaro.pokeapi.resource.NamedApiResource;
import skaro.pokeapi.resource.ability.Ability;
import skaro.pokeapi.resource.pokemon.Pokemon;
import skaro.pokeapi.resource.pokemon.PokemonAbility;
import skaro.trainer.api.CaptureRequest;
import skaro.trainer.api.StatBlockPayload;
import skaro.trainer.config.TrainerConfigurationProperties;
import skaro.trainer.persistence.TrainerRosterRepository;

// Cobertura unitaria de BR-002..BR-008 (TrainerRosterService.validateEvs/
// validateIvs/validateMoves/resolveAbility), sin Spring context, H2 ni
// MockWebServer — complementa a los *IntegrationTest de skaro.trainer.api
// (que cubren el mismo comportamiento end-to-end vía HTTP).
@ExtendWith(SpringExtension.class)
class TrainerRosterServiceTest {

	private TrainerRosterRepository repository;
	private PokeApiClient pokeApiClient;
	private TrainerRosterService service;

	@BeforeEach
	void setup() {
		repository = mock(TrainerRosterRepository.class);
		pokeApiClient = mock(PokeApiClient.class);
		service = new TrainerRosterService(repository, new TrainerConfigurationProperties(), pokeApiClient);
	}

	// Spec 001 — BR-004: un EV individual no puede superar 252
	@Test
	void unEvIndividualPorEncimaDe252RechazaSinLlamarAlRepositorioNiAPokeApi() {
		StatBlockPayload evs = new StatBlockPayload(253, 0, 0, 0, 0, 0);

		StepVerifier.create(service.capture(1L, requestConEvs(evs)))
				.expectErrorSatisfies(error -> {
					assertThat(error).isInstanceOf(CaptureValidationException.class);
					assertThat(error).hasMessageContaining("BR-004");
				})
				.verify();

		verifyNoInteractions(repository, pokeApiClient);
	}

	// Spec 001 — BR-003: la suma de EVs no puede superar 510
	@Test
	void laSumaDeEvsPorEncimaDe510Rechaza() {
		// cada EV individual <=252 a propósito, para aislar BR-003 de BR-004
		// (mismo motivo documentado en PokemonCaptureIntegrationTest)
		StatBlockPayload evs = new StatBlockPayload(86, 86, 86, 86, 86, 90);

		StepVerifier.create(service.capture(1L, requestConEvs(evs)))
				.expectErrorSatisfies(error -> {
					assertThat(error).isInstanceOf(CaptureValidationException.class);
					assertThat(error).hasMessageContaining("BR-003");
				})
				.verify();

		verifyNoInteractions(repository, pokeApiClient);
	}

	// Spec 001 — BR-002: un IV tiene que estar entre 0 y 31
	@Test
	void unIvFueraDelRangoDe0A31Rechaza() {
		StatBlockPayload ivs = new StatBlockPayload(32, 0, 0, 0, 0, 0);

		StepVerifier.create(service.capture(1L, requestConIvs(ivs)))
				.expectErrorSatisfies(error -> {
					assertThat(error).isInstanceOf(CaptureValidationException.class);
					assertThat(error).hasMessageContaining("BR-002");
				})
				.verify();

		verifyNoInteractions(repository, pokeApiClient);
	}

	// Spec 001 — BR-005: la naturaleza tiene que ser una de las 25 oficiales
	@Test
	void unaNaturalezaInvalidaRechaza() {
		CaptureRequest request = new CaptureRequest("pikachu", null, null, "COOL", "static", null, null, null, null,
				null, null, null);

		StepVerifier.create(service.capture(1L, request))
				.expectErrorSatisfies(error -> {
					assertThat(error).isInstanceOf(CaptureValidationException.class);
					assertThat(error).hasMessageContaining("BR-005");
				})
				.verify();

		verifyNoInteractions(repository, pokeApiClient);
	}

	// No spec'd formalmente en docs/specs/001-equipo-activo-y-baul.md (a
	// diferencia de la naturaleza, que sí tiene BR-005 + escenario Gherkin) —
	// se trata igual por consistencia con Nature, mismo tipo de campo y
	// mismo bug estructural (enum inválido sin capturar). Falta sumar un BR
	// propio a la spec si esto se formaliza.
	@Test
	void unGeneroInvalidoRechaza() {
		CaptureRequest request = new CaptureRequest("pikachu", null, null, "HARDY", "static", null, "ALIEN", null,
				null, null, null, null);

		StepVerifier.create(service.capture(1L, request))
				.expectErrorSatisfies(error -> assertThat(error).isInstanceOf(CaptureValidationException.class))
				.verify();

		verifyNoInteractions(repository, pokeApiClient);
	}

	// Spec 001 — BR-008: el set de movimientos no puede tener más de 4 elementos
	@Test
	void masDeCuatroMovimientosRechaza() {
		CaptureRequest request = new CaptureRequest("pikachu", null, null, "HARDY", "static", null, null,
				List.of("thunderbolt", "quick-attack", "iron-tail", "agility", "thunder"), null, null, null, null);

		StepVerifier.create(service.capture(1L, request))
				.expectErrorSatisfies(error -> {
					assertThat(error).isInstanceOf(CaptureValidationException.class);
					assertThat(error).hasMessageContaining("BR-008");
				})
				.verify();

		verifyNoInteractions(repository, pokeApiClient);
	}

	// Spec 001 — BR-006: la habilidad tiene que pertenecer al conjunto de la especie
	@Test
	void unaHabilidadQueNoPerteneceALaEspecieRechaza() {
		Pokemon pikachu = pokemonConHabilidades("static");
		when(pokeApiClient.getResource(Pokemon.class, "pikachu")).thenReturn(Mono.just(pikachu));

		CaptureRequest request = new CaptureRequest("pikachu", null, null, "HARDY", "levitate", null, null, null,
				null, null, null, null);

		StepVerifier.create(service.capture(1L, request))
				.expectErrorSatisfies(error -> {
					assertThat(error).isInstanceOf(CaptureValidationException.class);
					assertThat(error).hasMessageContaining("BR-006");
				})
				.verify();

		verifyNoInteractions(repository);
	}

	// Spec 001 — BR-006 / valores por defecto: sin habilidad especificada, se
	// usa la primera habilidad de la especie
	@Test
	void sinHabilidadEspecificadaUsaLaPrimeraDeLaEspecie() {
		Pokemon pikachu = pokemonConHabilidades("static", "lightning-rod");
		when(pokeApiClient.getResource(Pokemon.class, "pikachu")).thenReturn(Mono.just(pikachu));
		when(repository.capture(eq(1L), anyInt(), anyInt(), any())).thenAnswer(invocation -> {
			BiFunction<Trainer, Location, PokemonInstance> instanceFactory = invocation.getArgument(3);
			return Mono.just(instanceFactory.apply(new Trainer(1L), Location.TEAM));
		});

		CaptureRequest request = new CaptureRequest("pikachu", null, null, null, null, null, null, null, null, null,
				null, null);

		StepVerifier.create(service.capture(1L, request))
				.assertNext(instance -> assertThat(instance.getAbility()).isEqualTo("static"))
				.verifyComplete();
	}

	// Spec 001 — BR-006: especie sin habilidades no puede resolver un default
	@Test
	void especieSinHabilidadesYSinAbilityEspecificadaRechaza() {
		Pokemon sinHabilidades = pokemonConHabilidades();
		when(pokeApiClient.getResource(Pokemon.class, "pikachu")).thenReturn(Mono.just(sinHabilidades));

		CaptureRequest request = new CaptureRequest("pikachu", null, null, null, null, null, null, null, null, null,
				null, null);

		StepVerifier.create(service.capture(1L, request))
				.expectErrorSatisfies(error -> {
					assertThat(error).isInstanceOf(CaptureValidationException.class);
					assertThat(error).hasMessageContaining("BR-006");
				})
				.verify();

		verifyNoInteractions(repository);
	}

	private CaptureRequest requestConEvs(StatBlockPayload evs) {
		return new CaptureRequest("pikachu", null, evs, "HARDY", "static", null, null, null, null, null, null, null);
	}

	private CaptureRequest requestConIvs(StatBlockPayload ivs) {
		return new CaptureRequest("pikachu", ivs, null, "HARDY", "static", null, null, null, null, null, null, null);
	}

	private Pokemon pokemonConHabilidades(String... nombres) {
		Pokemon pokemon = new Pokemon();
		pokemon.setName("pikachu");
		pokemon.setAbilities(List.of(nombres).stream().map(this::habilidad).toList());
		return pokemon;
	}

	private PokemonAbility habilidad(String nombre) {
		NamedApiResource<Ability> resource = new NamedApiResource<>();
		resource.setName(nombre);
		PokemonAbility ability = new PokemonAbility();
		ability.setAbility(resource);
		return ability;
	}

}
