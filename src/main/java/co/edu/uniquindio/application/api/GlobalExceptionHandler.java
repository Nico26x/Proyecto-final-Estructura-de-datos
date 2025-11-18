package co.edu.uniquindio.application.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Map;

/**
 * Manejador global de excepciones para la aplicación REST.
 * <p>
 * Intercepta excepciones lanzadas en controladores y servicios para proporcionar
 * respuestas de error consistentes y bien estructuradas con información de debugging.
 * </p>
 * <p>
 * Maneja:
 * </p>
 * <ul>
 *   <li>{@code IllegalArgumentException}: Argumentos inválidos (400 Bad Request)</li>
 *   <li>{@code SecurityException}: Problemas de seguridad (403 Forbidden)</li>
 *   <li>{@code Exception}: Excepciones genéricas (500 Internal Server Error)</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones de argumentos ilegales.
     * <p>
     * Devuelve una respuesta 400 Bad Request con detalles del error.
     * </p>
     *
     * @param ex la excepción de argumento ilegal
     * @param req la solicitud HTTP que causó el error
     * @return respuesta 400 con cuerpo de error estructurado
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), req));
    }

    /**
     * Maneja excepciones de seguridad.
     * <p>
     * Devuelve una respuesta 403 Forbidden cuando se detectan problemas de autorización.
     * </p>
     *
     * @param ex la excepción de seguridad
     * @param req la solicitud HTTP que causó el error
     * @return respuesta 403 con cuerpo de error estructurado
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurity(SecurityException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(HttpStatus.FORBIDDEN, ex.getMessage(), req));
    }

    /**
     * Maneja cualquier excepción no capturada específicamente.
     * <p>
     * Devuelve una respuesta 500 Internal Server Error. El mensaje es genérico
     * por razones de seguridad (no expone detalles internos al cliente).
     * </p>
     *
     * @param ex la excepción genérica
     * @param req la solicitud HTTP que causó el error
     * @return respuesta 500 con cuerpo de error estructurado
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", req));
    }

    /**
     * Construye un cuerpo de respuesta de error estructurado.
     * <p>
     * Incluye información sobre el código de estado HTTP, mensaje de error,
     * ruta solicitada y timestamp para facilitar debugging y auditoría.
     * </p>
     *
     * @param status el estado HTTP de la respuesta
     * @param msg el mensaje de error descriptivo
     * @param req la solicitud HTTP para extraer la ruta
     * @return mapa con la estructura de error completa
     */
    private Map<String, Object> errorBody(HttpStatus status, String msg, HttpServletRequest req) {
        return Map.of(
                "success", false,
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", msg,
                "path", req.getRequestURI(),
                "timestamp", Instant.now().toEpochMilli()
        );
    }
}
