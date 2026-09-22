package skaro.trainer.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;
import skaro.trainer.domain.Location;
import skaro.trainer.domain.TrainerRosterService;

@RestController
@RequestMapping("/trainers/{trainerId}/pokemon")
public class PokemonController {

	private final TrainerRosterService rosterService;

	public PokemonController(TrainerRosterService rosterService) {
		this.rosterService = rosterService;
	}

	@PostMapping
	public Mono<ResponseEntity<PokemonInstanceResponse>> capture(@PathVariable Long trainerId,
			@Valid @RequestBody CaptureRequest request) {
		return rosterService.capture(trainerId, request)
				.map(instance -> ResponseEntity.status(HttpStatus.CREATED).body(PokemonInstanceResponse.from(instance)));
	}

	@GetMapping
	public Mono<ResponseEntity<List<?>>> list(@PathVariable Long trainerId, @RequestParam String location) {
		if ("team".equalsIgnoreCase(location)) {
			return rosterService.listTeam(trainerId)
					.map(TeamMemberResponse::from)
					.collectList()
					.map(ResponseEntity::ok);
		}
		return rosterService.listBox(trainerId)
				.map(PokemonInstanceResponse::from)
				.collectList()
				.map(ResponseEntity::ok);
	}

	@GetMapping("/{id}")
	public Mono<ResponseEntity<PokemonInstanceResponse>> detail(@PathVariable Long trainerId, @PathVariable Long id) {
		return rosterService.findById(trainerId, id)
				.map(instance -> ResponseEntity.ok(PokemonInstanceResponse.from(instance)))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@PatchMapping("/{id}")
	public Mono<ResponseEntity<PokemonInstanceResponse>> move(@PathVariable Long trainerId, @PathVariable Long id,
			@Valid @RequestBody MoveRequest request) {
		Location target = Location.valueOf(request.location().toUpperCase());
		return rosterService.move(trainerId, id, target)
				.map(instance -> ResponseEntity.ok(PokemonInstanceResponse.from(instance)));
	}

}
