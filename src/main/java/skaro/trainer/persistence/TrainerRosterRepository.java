package skaro.trainer.persistence;

import java.util.function.BiFunction;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import skaro.trainer.domain.Location;
import skaro.trainer.domain.PokemonInstance;
import skaro.trainer.domain.Trainer;

// Cada método envuelve la llamada JPA (bloqueante) en
// Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic()) y expone
// Mono/Flux hacia skaro.trainer.domain — ver docs/DESIGN.md D1. El resto
// del dominio no se entera de que acá adentro hay I/O bloqueante.
@Repository
public class TrainerRosterRepository {

	private final PokemonCaptureTransaction captureTransaction;
	private final PokemonMoveTransaction moveTransaction;
	private final PokemonInstanceRepository pokemonInstanceRepository;

	public TrainerRosterRepository(PokemonCaptureTransaction captureTransaction,
			PokemonMoveTransaction moveTransaction, PokemonInstanceRepository pokemonInstanceRepository) {
		this.captureTransaction = captureTransaction;
		this.moveTransaction = moveTransaction;
		this.pokemonInstanceRepository = pokemonInstanceRepository;
	}

	public Mono<PokemonInstance> capture(Long trainerId, int teamLimit, int boxLimit,
			BiFunction<Trainer, Location, PokemonInstance> instanceFactory) {
		return Mono.fromCallable(() -> captureTransaction.execute(trainerId, teamLimit, boxLimit, instanceFactory))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<PokemonInstance> move(Long trainerId, Long pokemonId, Location targetLocation) {
		return Mono.fromCallable(() -> moveTransaction.execute(trainerId, pokemonId, targetLocation))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<PokemonInstance> findByLocation(Long trainerId, Location location) {
		return Flux
				.defer(() -> Flux.fromIterable(pokemonInstanceRepository.findByTrainerIdAndLocation(trainerId, location)))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<PokemonInstance> findByIdAndTrainer(Long id, Long trainerId) {
		return Mono.fromCallable(() -> pokemonInstanceRepository.findByIdAndTrainerId(id, trainerId).orElse(null))
				.subscribeOn(Schedulers.boundedElastic());
	}

}
