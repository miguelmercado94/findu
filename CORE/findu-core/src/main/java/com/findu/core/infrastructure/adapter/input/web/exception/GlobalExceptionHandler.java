package com.findu.core.infrastructure.adapter.input.web.exception;

import com.findu.core.application.util.ApiMessages;
import com.findu.core.domain.exception.ArchivoProveedorException;
import com.findu.core.domain.exception.InvalidProviderDocumentException;
import com.findu.core.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para Spring MVC. Devuelve ErrorResponse en JSON.
 * Intercepta excepciones lanzadas en controllers/services y las mapea a HTTP status apropiados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ─── 422 Unprocessable Entity ─────────────────────────────────────────────

    @ExceptionHandler(InvalidProviderDocumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProviderDocument(InvalidProviderDocumentException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ─── Archivos del proveedor (409/413/415/422); el codigo viaja en el campo "error" ───

    @ExceptionHandler(ArchivoProveedorException.class)
    public ResponseEntity<ErrorResponse> handleArchivoProveedor(ArchivoProveedorException ex, HttpServletRequest request) {
        HttpStatus status = switch (ex.getCodigo()) {
            case BIOMETRIA_REQUERIDA -> HttpStatus.CONFLICT;
            case ARCHIVO_DEMASIADO_GRANDE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case FORMATO_NO_SOPORTADO -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case ROSTRO_BIOMETRICO_NO_ENCONTRADO, FOTO_NO_COINCIDE, IMAGEN_INVALIDA -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        ErrorResponse body = ErrorResponse.of(status.value(), ex.getCodigo().name(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    // ─── 502/503 almacenamiento de objetos (S3) no disponible ───

    @ExceptionHandler(com.findu.core.domain.exception.StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorage(com.findu.core.domain.exception.StorageException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_GATEWAY.value(), "STORAGE_ERROR",
                "El almacenamiento de archivos no respondio correctamente. Intenta de nuevo mas tarde.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(software.amazon.awssdk.core.exception.SdkClientException.class)
    public ResponseEntity<ErrorResponse> handleStorageUnreachable(software.amazon.awssdk.core.exception.SdkClientException ex, HttpServletRequest request) {
        log.error("Almacenamiento no alcanzable path={} : {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse body = ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "STORAGE_UNAVAILABLE",
                "El almacenamiento de archivos no esta disponible. Intenta de nuevo mas tarde.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "ARCHIVO_DEMASIADO_GRANDE",
                "La imagen supera el tamano maximo permitido",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    // ─── 400 Bad Request ──────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Errores de validación de @Valid (campos requeridos, formato inválido, etc.)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        String message = details.isEmpty() ? "Datos de entrada inválidos" : details.get(0);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ─── 409 Conflict (duplicados) ────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ─── 500 Internal Server Error (catch-all) ────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado path={} : {}", request.getRequestURI(), ex.toString(), ex);
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : ApiMessages.Error.INTERNAL;
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
