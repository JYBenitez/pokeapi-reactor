package skaro.trainer.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import skaro.trainer.domain.Trainer;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

	// Lock pesimista: serializa el ciclo check-then-act de captura/traslado
	// contra un mismo Trainer — ver docs/DESIGN.md D3.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Trainer t where t.id = :id")
	Optional<Trainer> findByIdForUpdate(@Param("id") Long id);

}
