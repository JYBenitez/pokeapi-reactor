package skaro.trainer.domain;

// Payload bien formado pero viola una regla de negocio de captura — 422
// Unprocessable Entity (tabla de errores de la spec).
public class CaptureValidationException extends RuntimeException {

	public CaptureValidationException(String message) {
		super(message);
	}

}
