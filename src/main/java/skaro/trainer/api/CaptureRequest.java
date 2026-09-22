package skaro.trainer.api;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

// Único campo obligatorio: species (ver docs/specs/001-equipo-activo-y-baul.md
// § "Valores por defecto del payload de captura"). El resto se completa con
// defaults en el escenario correspondiente (Nivel 1b #13) — acá solo se
// declara el contrato del payload.
public record CaptureRequest(
		@NotBlank String species,
		StatBlockPayload ivs,
		StatBlockPayload evs,
		String nature,
		String ability,
		Boolean shiny,
		String gender,
		List<String> moves,
		String heldItem,
		String pokeball,
		Integer initialLevel,
		String originLocation) {
}
