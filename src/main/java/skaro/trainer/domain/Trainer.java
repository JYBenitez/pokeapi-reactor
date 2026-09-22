package skaro.trainer.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Aggregate root. Fijo/único en esta entrega (ver Restricciones de
// docs/specs/001-equipo-activo-y-baul.md) — el id no se autogenera, se
// siembra con skaro.trainer.default-trainer-id al arrancar.
@Entity
public class Trainer {

	@Id
	private Long id;

	protected Trainer() {
	}

	public Trainer(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

}
