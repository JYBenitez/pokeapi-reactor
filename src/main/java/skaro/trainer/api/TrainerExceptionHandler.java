package skaro.trainer.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import skaro.trainer.domain.CaptureValidationException;
import skaro.trainer.domain.NoCapacityAvailableException;

// Modelo de errores HTTP de la spec 001 — resuelve P8 para las excepciones
// propias del dominio (skaro.trainer). La traducción de errores de
// PokéAPI (502/504) es un punto distinto, ver Restricciones de la spec.
@RestControllerAdvice
public class TrainerExceptionHandler {

	@ExceptionHandler(NoCapacityAvailableException.class)
	public ResponseEntity<ErrorResponse> handleNoCapacity(NoCapacityAvailableException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(CaptureValidationException.class)
	public ResponseEntity<ErrorResponse> handleCaptureValidation(CaptureValidationException ex) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(ex.getMessage()));
	}

}
