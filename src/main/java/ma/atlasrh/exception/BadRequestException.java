package ma.atlasrh.exception;

/**
 * Requête invalide (HTTP 400).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
