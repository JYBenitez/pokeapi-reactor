package skaro.trainer.domain;

// Ni el Equipo Activo ni el Baúl tienen espacio — 409 Conflict (decisión
// A4 de docs/PRODUCT.md).
public class NoCapacityAvailableException extends RuntimeException {

	public NoCapacityAvailableException() {
		super("No hay espacio disponible en el Equipo Activo ni en el Baúl");
	}

}
