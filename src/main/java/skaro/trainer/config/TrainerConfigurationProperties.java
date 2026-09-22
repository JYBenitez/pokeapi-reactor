package skaro.trainer.config;

import java.time.Duration;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Mismo patrón que skaro.pokeapi.PokeApiConfigurationProperties: bean con
// getter/setter y defaults razonables, no constantes sueltas — ver
// docs/specs/001-equipo-activo-y-baul.md § Parametrización.
//
// @Validated acá, no en el método @Bean de TrainerConfiguration: Spring
// Boot resuelve la anotación sobre la clase de la instancia del bean, no
// sobre el método factory — puesta ahí no dispara Bean Validation.
@Validated
public class TrainerConfigurationProperties {

	@Min(1)
	private int teamLimit = 6;

	@Min(1)
	private int boxLimit = 300;

	@NotNull
	private Long defaultTrainerId = 1L;

	@NotNull
	private Duration pokeapiCallTimeout = Duration.ofSeconds(3);

	public int getTeamLimit() {
		return teamLimit;
	}

	public void setTeamLimit(int teamLimit) {
		this.teamLimit = teamLimit;
	}

	public int getBoxLimit() {
		return boxLimit;
	}

	public void setBoxLimit(int boxLimit) {
		this.boxLimit = boxLimit;
	}

	public Long getDefaultTrainerId() {
		return defaultTrainerId;
	}

	public void setDefaultTrainerId(Long defaultTrainerId) {
		this.defaultTrainerId = defaultTrainerId;
	}

	public Duration getPokeapiCallTimeout() {
		return pokeapiCallTimeout;
	}

	public void setPokeapiCallTimeout(Duration pokeapiCallTimeout) {
		this.pokeapiCallTimeout = pokeapiCallTimeout;
	}

}
