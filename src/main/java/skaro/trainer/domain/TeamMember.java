package skaro.trainer.domain;

import skaro.pokeapi.resource.pokemon.Pokemon;

// Vista compuesta de FR-021: metadata propia del ejemplar + atributos
// estáticos de la especie desde PokéAPI. Vive en domain (no en api) porque
// combina PokemonInstance con un recurso de skaro.pokeapi.resource, que ya
// es una dependencia reutilizada del dominio (ver "Alcance del cambio").
public record TeamMember(PokemonInstance instance, Pokemon species) {
}
