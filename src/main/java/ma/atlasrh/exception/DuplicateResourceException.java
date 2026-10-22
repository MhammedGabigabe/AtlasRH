package ma.atlasrh.exception;

/**
 * Valeur déjà utilisée, par exemple un matricule ou un nom déjà pris (HTTP 409).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
