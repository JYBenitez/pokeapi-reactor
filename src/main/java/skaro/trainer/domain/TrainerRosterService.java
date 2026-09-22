package skaro.trainer.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import skaro.pokeapi.client.PokeApiClient;
import skaro.pokeapi.resource.pokemon.Pokemon;
import skaro.trainer.api.CaptureRequest;
import skaro.trainer.api.StatBlockPayload;
import skaro.trainer.config.TrainerConfigurationProperties;
import skaro.trainer.persistence.TrainerRosterRepository;

// Orquesta las reglas de negocio del Entrenador (límites, asignación
// automática, traslado) — ver "Alcance del cambio" de
// docs/specs/001-equipo-activo-y-baul.md.
@Service
public class TrainerRosterService {

	private final TrainerRosterRepository repository;
	private final TrainerConfigurationProperties properties;
	private final PokeApiClient pokeApiClient;

	public TrainerRosterService(TrainerRosterRepository repository, TrainerConfigurationProperties properties,
			PokeApiClient pokeApiClient) {
		this.repository = repository;
		this.properties = properties;
		this.pokeApiClient = pokeApiClient;
	}

	public Mono<PokemonInstance> capture(Long trainerId, CaptureRequest request) {
		return Mono.defer(() -> {
			validateEvs(request.evs());
			validateIvs(request.ivs());
			validateMoves(request.moves());
			Nature nature = resolveNature(request.nature());
			Gender gender = resolveGender(request.gender());
			// A3 (PRODUCT.md): Equipo Activo primero, Baúl si está lleno — la
			// decisión de destino vive dentro de la transacción del lock
			// pesimista (docs/DESIGN.md D3), no acá.
			return resolveAbility(request)
					.flatMap(ability -> repository.capture(trainerId, properties.getTeamLimit(),
							properties.getBoxLimit(),
							(trainer, location) -> toEntity(trainer, request, location, ability, nature, gender)));
		});
	}

	private void validateMoves(List<String> moves) {
		if (moves != null && moves.size() > 4) {
			throw new CaptureValidationException("BR-008: el set de movimientos no puede tener más de 4 elementos");
		}
	}

	// BR-006: la habilidad tiene que pertenecer al conjunto que la especie
	// posee en PokéAPI. Si no se especifica, el default es la primera
	// habilidad de la especie (ver "Valores por defecto del payload de
	// captura" de la spec).
	private Mono<String> resolveAbility(CaptureRequest request) {
		return pokeApiClient.getResource(Pokemon.class, request.species()).flatMap(species -> {
			List<String> abilities = species.getAbilities() == null ? List.of()
					: species.getAbilities().stream().map(a -> a.getAbility().getName()).toList();
			if (request.ability() == null) {
				return abilities.isEmpty()
						? Mono.error(new CaptureValidationException("BR-006: la especie no tiene habilidades"))
						: Mono.just(abilities.get(0));
			}
			boolean valid = abilities.stream().anyMatch(a -> a.equalsIgnoreCase(request.ability()));
			if (!valid) {
				return Mono.error(new CaptureValidationException("BR-006: la habilidad no pertenece a la especie"));
			}
			return Mono.just(request.ability());
		});
	}

	private void validateEvs(StatBlockPayload evs) {
		if (evs == null) {
			return;
		}
		for (int stat : new int[] { evs.hp(), evs.attack(), evs.defense(), evs.specialAttack(),
				evs.specialDefense(), evs.speed() }) {
			if (stat > 252) {
				throw new CaptureValidationException("BR-004: un EV individual no puede superar 252");
			}
		}
		int total = evs.hp() + evs.attack() + evs.defense() + evs.specialAttack() + evs.specialDefense()
				+ evs.speed();
		if (total > 510) {
			throw new CaptureValidationException("BR-003: la suma de EVs no puede superar 510");
		}
	}

	private void validateIvs(StatBlockPayload ivs) {
		if (ivs == null) {
			return;
		}
		for (int stat : new int[] { ivs.hp(), ivs.attack(), ivs.defense(), ivs.specialAttack(),
				ivs.specialDefense(), ivs.speed() }) {
			if (stat < 0 || stat > 31) {
				throw new CaptureValidationException("BR-002: un IV tiene que estar entre 0 y 31");
			}
		}
	}

	// BR-005: la naturaleza tiene que ser una de las 25 oficiales.
	private Nature resolveNature(String nature) {
		if (nature == null) {
			return Nature.HARDY;
		}
		try {
			return Nature.valueOf(nature.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new CaptureValidationException("BR-005: la naturaleza indicada no es una de las 25 oficiales");
		}
	}

	// Sin BR propio todavía en la spec (a diferencia de la naturaleza, que sí
	// tiene BR-005 con escenario Gherkin) — mismo tratamiento por consistencia
	// con resolveNature: es el mismo campo de enum sin validar, tres líneas
	// más abajo en el payload original.
	private Gender resolveGender(String gender) {
		if (gender == null) {
			return Gender.GENDERLESS;
		}
		try {
			return Gender.valueOf(gender.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new CaptureValidationException("el género indicado no es válido");
		}
	}

	// FR-021: combina cada ejemplar del Equipo Activo con los atributos
	// estáticos de su especie, resueltos vía PokeApiClient (ya probado).
	public Flux<TeamMember> listTeam(Long trainerId) {
		return repository.findByLocation(trainerId, Location.TEAM)
				.flatMap(instance -> pokeApiClient.getResource(Pokemon.class, instance.getSpecies())
						.map(species -> new TeamMember(instance, species)));
	}

	public Flux<PokemonInstance> listBox(Long trainerId) {
		return repository.findByLocation(trainerId, Location.BOX);
	}

	public Mono<PokemonInstance> findById(Long trainerId, Long id) {
		return repository.findByIdAndTrainer(id, trainerId);
	}

	public Mono<PokemonInstance> move(Long trainerId, Long pokemonId, Location targetLocation) {
		return repository.move(trainerId, pokemonId, targetLocation);
	}

	// Defaults según "Valores por defecto del payload de captura" de la
	// spec — único campo obligatorio: species. nature/gender ya vienen
	// resueltos (resolveNature/resolveGender) — acá es solo mapeo, sin
	// parseo que pueda fallar.
	private PokemonInstance toEntity(Trainer trainer, CaptureRequest request, Location location, String ability,
			Nature nature, Gender gender) {
		PokemonInstance instance = new PokemonInstance(trainer, request.species());
		instance.setIvs(toIvs(request.ivs()));
		instance.setEvs(toEvs(request.evs()));
		instance.setNature(nature);
		instance.setAbility(ability);
		instance.setShiny(Boolean.TRUE.equals(request.shiny()));
		instance.setGender(gender);
		instance.setMoves(request.moves() != null ? request.moves() : List.of());
		instance.setHeldItem(request.heldItem());
		instance.setPokeball(request.pokeball() != null ? request.pokeball() : "Poké Ball");
		instance.setCaptureDate(Instant.now());
		instance.setInitialLevel(request.initialLevel() != null ? request.initialLevel() : 1);
		instance.setOriginLocation(request.originLocation() != null ? request.originLocation() : "Desconocida");
		instance.setLocation(location);
		return instance;
	}

	private Ivs toIvs(StatBlockPayload payload) {
		if (payload == null) {
			return new Ivs(0, 0, 0, 0, 0, 0);
		}
		return new Ivs(payload.hp(), payload.attack(), payload.defense(), payload.specialAttack(),
				payload.specialDefense(), payload.speed());
	}

	private Evs toEvs(StatBlockPayload payload) {
		if (payload == null) {
			return new Evs(0, 0, 0, 0, 0, 0);
		}
		return new Evs(payload.hp(), payload.attack(), payload.defense(), payload.specialAttack(),
				payload.specialDefense(), payload.speed());
	}

}
