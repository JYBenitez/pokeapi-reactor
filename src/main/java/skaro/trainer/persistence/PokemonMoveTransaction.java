package skaro.trainer.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import skaro.trainer.domain.Location;
import skaro.trainer.domain.PokemonInstance;
import skaro.trainer.domain.Trainer;

// Mismo patrón que PokemonCaptureTransaction: lock pesimista + lectura +
// actualización en una única transacción, en una clase separada para que
// el proxy de @Transactional de Spring aplique (ver esa clase).
@Component
class PokemonMoveTransaction {

	private final TrainerRepository trainerRepository;
	private final PokemonInstanceRepository pokemonInstanceRepository;

	PokemonMoveTransaction(TrainerRepository trainerRepository,
			PokemonInstanceRepository pokemonInstanceRepository) {
		this.trainerRepository = trainerRepository;
		this.pokemonInstanceRepository = pokemonInstanceRepository;
	}

	@Transactional
	public PokemonInstance execute(Long trainerId, Long pokemonId, Location targetLocation) {
		Trainer trainer = trainerRepository.findByIdForUpdate(trainerId).orElse(null);
		if (trainer == null) {
			return null;
		}
		PokemonInstance instance = pokemonInstanceRepository.findByIdAndTrainerId(pokemonId, trainerId).orElse(null);
		if (instance == null) {
			return null;
		}
		instance.setLocation(targetLocation);
		return pokemonInstanceRepository.save(instance);
	}

}
