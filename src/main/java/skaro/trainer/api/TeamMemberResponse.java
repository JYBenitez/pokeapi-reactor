package skaro.trainer.api;

import skaro.pokeapi.resource.pokemon.Pokemon;
import skaro.trainer.domain.TeamMember;

public record TeamMemberResponse(PokemonInstanceResponse instance, Pokemon species) {

	public static TeamMemberResponse from(TeamMember member) {
		return new TeamMemberResponse(PokemonInstanceResponse.from(member.instance()), member.species());
	}

}
