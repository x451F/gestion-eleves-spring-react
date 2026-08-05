package fr.afpa.gestioneleves.exception;

import fr.afpa.gestioneleves.security.AuthenticationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ProblemDetail> authenticationFailed(AuthenticationFailedException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        detail.setType(URI.create("urn:problem:authentication_failed"));
        detail.setTitle("Échec de l’authentification");
        detail.setDetail("Les identifiants fournis sont invalides.");
        detail.setProperty("code", "authentication_failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(detail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({DuplicateResourceException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiError> conflict(Exception ex, HttpServletRequest request) {
        String message = ex instanceof DuplicateResourceException
                ? ex.getMessage() : "La ressource est encore référencée ou viole une contrainte d'unicité";
        return response(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", message, request, List.of());
    }

    @ExceptionHandler({BusinessRuleException.class, ConstraintViolationException.class, FileStorageException.class})
    ResponseEntity<ApiError> badRequest(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La requête est invalide", request, fields);
    }

    @ExceptionHandler({UnsupportedPhotoTypeException.class, HttpMediaTypeNotSupportedException.class})
    ResponseEntity<ApiError> unsupportedType(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({PhotoTooLargeException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<ApiError> tooLarge(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "La photo dépasse la taille maximale autorisée", request, List.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message,
                                              HttpServletRequest request,
                                              List<ApiError.FieldViolation> fields) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), code, message, request.getRequestURI(), fields));
    }
}
