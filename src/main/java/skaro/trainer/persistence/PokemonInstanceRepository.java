package skaro.trainer.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import skaro.trainer.domain.Location;
import skaro.trainer.domain.PokemonInstance;

public interface PokemonInstanceRepository extends JpaRepository<PokemonInstance, Long> {

	List<PokemonInstance> findByTrainerIdAndLocation(Long trainerId, Location location);

	long countByTrainerIdAndLocation(Long trainerId, Location location);

	Optional<PokemonInstance> findByIdAndTrainerId(Long id, Long trainerId);

}
