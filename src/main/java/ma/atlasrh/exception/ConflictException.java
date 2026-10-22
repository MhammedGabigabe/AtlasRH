package ma.atlasrh.exception;

/**
 * Règle métier ou état incompatible avec l'opération demandée (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
