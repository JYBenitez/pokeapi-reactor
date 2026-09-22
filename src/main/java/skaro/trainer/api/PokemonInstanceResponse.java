package skaro.trainer.api;

import java.time.Instant;
import java.util.List;

import skaro.trainer.domain.Evs;
import skaro.trainer.domain.Ivs;
import skaro.trainer.domain.PokemonInstance;

public record PokemonInstanceResponse(
		Long id,
		String species,
		StatBlockPayload ivs,
		StatBlockPayload evs,
		String nature,
		String ability,
		boolean shiny,
		String gender,
		List<String> moves,
		String heldItem,
		String pokeball,
		Instant captureDate,
		int initialLevel,
		String originLocation,
		String location) {

	public static PokemonInstanceResponse from(PokemonInstance instance) {
		return new PokemonInstanceResponse(
				instance.getId(),
				instance.getSpecies(),
				toPayload(instance.getIvs()),
				toPayload(instance.getEvs()),
				instance.getNature() != null ? instance.getNature().name() : null,
				instance.getAbility(),
				instance.isShiny(),
				instance.getGender() != null ? instance.getGender().name() : null,
				instance.getMoves(),
				instance.getHeldItem(),
				instance.getPokeball(),
				instance.getCaptureDate(),
				instance.getInitialLevel(),
				instance.getOriginLocation(),
				// contrato en inglés minúscula: "team"/"box" (ver Cambia en la spec)
				instance.getLocation() != null ? instance.getLocation().name().toLowerCase() : null);
	}

	private static StatBlockPayload toPayload(Ivs ivs) {
		return ivs == null ? null
				: new StatBlockPayload(ivs.getHp(), ivs.getAttack(), ivs.getDefense(), ivs.getSpecialAttack(),
						ivs.getSpecialDefense(), ivs.getSpeed());
	}

	private static StatBlockPayload toPayload(Evs evs) {
		return evs == null ? null
				: new StatBlockPayload(evs.getHp(), evs.getAttack(), evs.getDefense(), evs.getSpecialAttack(),
						evs.getSpecialDefense(), evs.getSpeed());
	}

}
