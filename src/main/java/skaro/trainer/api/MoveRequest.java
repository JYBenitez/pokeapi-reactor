package skaro.trainer.api;

import jakarta.validation.constraints.NotBlank;

public record MoveRequest(@NotBlank String location) {
}
