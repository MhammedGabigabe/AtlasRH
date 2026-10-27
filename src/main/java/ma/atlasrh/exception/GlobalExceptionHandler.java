package ma.atlasrh.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.query.sqm.PathElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduit les exceptions de l'application en réponses JSON homogènes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                HttpServletRequest request) {
        log.warn("Ressource introuvable : {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex,
                                                                 HttpServletRequest request) {
        log.warn("Valeur déjà utilisée : {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex,
                                                        HttpServletRequest request) {
        log.warn("Conflit métier : {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex,
                                                          HttpServletRequest request) {
        log.warn("Requête invalide : {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Valeur invalide"
                                : fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        log.warn("Validation échouée : {}", fieldErrors);
        return buildError(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides.", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpServletRequest request) {
        log.warn("Corps de requête illisible : {}", ex.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.BAD_REQUEST,
                "Le corps de la requête est invalide ou mal formé (JSON, énumération ou date incorrecte).",
                request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String message = "Le paramètre '" + ex.getName() + "' a une valeur invalide : " + ex.getValue();
        log.warn(message);
        return buildError(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex,
                                                                     HttpServletRequest request) {
        String message = "L'en-tête obligatoire '" + ex.getHeaderName() + "' est manquant.";
        log.warn(message);
        return buildError(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleUnknownSortProperty(PropertyReferenceException ex,
                                                                   HttpServletRequest request) {
        String message = "Le champ de tri '" + ex.getPropertyName() + "' n'existe pas.";
        log.warn(message);
        return buildError(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccessApiUsage(InvalidDataAccessApiUsageException ex,
                                                                         HttpServletRequest request) {
        // Hibernate 7 signale un attribut de tri inconnu via PathElementException
        // (et non plus PropertyReferenceException comme avec les versions précédentes).
        Throwable cause = ex;
        while (cause != null && !(cause instanceof PathElementException)) {
            cause = cause.getCause();
        }
        if (cause != null) {
            String message = "Le champ de tri demandé n'existe pas.";
            String causeMessage = cause.getMessage();
            if (causeMessage != null) {
                int start = causeMessage.indexOf('\'');
                int end = causeMessage.indexOf('\'', start + 1);
                if (start >= 0 && end > start) {
                    message = "Le champ de tri '" + causeMessage.substring(start + 1, end) + "' n'existe pas.";
                }
            }
            log.warn(message);
            return buildError(HttpStatus.BAD_REQUEST, message, request, null);
        }
        return handleUnexpectedException(ex, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                      HttpServletRequest request) {
        log.warn("Violation d'intégrité des données : {}", ex.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.CONFLICT,
                "L'opération viole une contrainte d'intégrité des données.",
                request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex,
                                                                   HttpServletRequest request) {
        log.error("Erreur interne inattendue", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue.",
                request, null);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status,
                                                     String message,
                                                     HttpServletRequest request,
                                                     Map<String, String> errors) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                errors);
        return ResponseEntity.status(status).body(body);
    }
}
