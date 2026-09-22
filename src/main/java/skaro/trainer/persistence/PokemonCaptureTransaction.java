package skaro.trainer.persistence;

import java.util.function.BiFunction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import skaro.trainer.domain.Location;
import skaro.trainer.domain.NoCapacityAvailableException;
import skaro.trainer.domain.PokemonInstance;
import skaro.trainer.domain.Trainer;

// Lock pesimista + conteo + inserción en una única transacción — el lock
// solo sirve si las tres operaciones comparten transacción (docs/DESIGN.md
// D3). Clase separada de TrainerRosterRepository a propósito: si
// @Transactional viviera en la misma clase que la invoca, la
// auto-invocación saltearía el proxy de Spring y el lock no aplicaría.
@Component
class PokemonCaptureTransaction {

	private final TrainerRepository trainerRepository;
	private final PokemonInstanceRepository pokemonInstanceRepository;

	PokemonCaptureTransaction(TrainerRepository trainerRepository,
			PokemonInstanceRepository pokemonInstanceRepository) {
		this.trainerRepository = trainerRepository;
		this.pokemonInstanceRepository = pokemonInstanceRepository;
	}

	@Transactional
	public PokemonInstance execute(Long trainerId, int teamLimit, int boxLimit,
			BiFunction<Trainer, Location, PokemonInstance> instanceFactory) {
		Trainer trainer = trainerRepository.findByIdForUpdate(trainerId).orElse(null);
		if (trainer == null) {
			return null;
		}
		long teamCount = pokemonInstanceRepository.countByTrainerIdAndLocation(trainerId, Location.TEAM);
		if (teamCount < teamLimit) {
			return pokemonInstanceRepository.save(instanceFactory.apply(trainer, Location.TEAM));
		}
		long boxCount = pokemonInstanceRepository.countByTrainerIdAndLocation(trainerId, Location.BOX);
		if (boxCount < boxLimit) {
			return pokemonInstanceRepository.save(instanceFactory.apply(trainer, Location.BOX));
		}
		throw new NoCapacityAvailableException();
	}

}
