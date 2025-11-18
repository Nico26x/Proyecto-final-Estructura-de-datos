package co.edu.uniquindio.application.api;

import java.time.Instant;

/**
 * Clase genérica que encapsula las respuestas de la API en un formato estándar (envelope).
 * <p>
 * Proporciona una estructura uniforme para todas las respuestas REST con información
 * sobre éxito/error, mensaje descriptivo, datos opcionales y timestamp.
 * </p>
 * <p>
 * Uso típico:
 * <ul>
 *   <li>Respuesta exitosa: {@code ApiResponse.ok(data)}</li>
 *   <li>Respuesta exitosa con mensaje: {@code ApiResponse.ok("mensaje", data)}</li>
 *   <li>Respuesta de error: {@code ApiResponse.error("mensaje de error")}</li>
 * </ul>
 * </p>
 *
 * @param <T> el tipo de dato contenido en la respuesta
 * @author SyncUp
 * @version 1.0
 */
public class ApiResponse<T> {
    /**
     * Indica si la operación fue exitosa.
     */
    private final boolean success;

    /**
     * Mensaje descriptivo sobre la operación (puede ser null para respuestas exitosas).
     */
    private final String message;

    /**
     * Datos contenidos en la respuesta (null en caso de error).
     */
    private final T data;

    /**
     * Timestamp en milisegundos desde epoch cuando se generó la respuesta.
     */
    private final long timestamp;

    /**
     * Constructor privado para crear una respuesta API.
     *
     * @param success indicador de éxito
     * @param message mensaje descriptivo
     * @param data los datos a retornar
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toEpochMilli();
    }

    /**
     * Crea una respuesta API exitosa con datos.
     *
     * @param data los datos a retornar
     * @param <T> el tipo de dato
     * @return una respuesta API con éxito
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    /**
     * Crea una respuesta API exitosa con mensaje y datos.
     *
     * @param msg el mensaje descriptivo
     * @param data los datos a retornar
     * @param <T> el tipo de dato
     * @return una respuesta API con éxito y mensaje
     */
    public static <T> ApiResponse<T> ok(String msg, T data) {
        return new ApiResponse<>(true, msg, data);
    }

    /**
     * Crea una respuesta API de error sin datos.
     *
     * @param msg el mensaje de error
     * @param <T> el tipo de dato (no utilizado en errores)
     * @return una respuesta API con estado de error
     */
    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(false, msg, null);
    }

    /**
     * Verifica si la operación fue exitosa.
     *
     * @return {@code true} si el estado es éxito, {@code false} si es error
     */
    public boolean isSuccess() { return success; }

    /**
     * Obtiene el mensaje descriptivo de la respuesta.
     *
     * @return el mensaje, o {@code null} si no hay mensaje
     */
    public String getMessage() { return message; }

    /**
     * Obtiene los datos contenidos en la respuesta.
     *
     * @return los datos, o {@code null} si no hay datos (típicamente en errores)
     */
    public T getData() { return data; }

    /**
     * Obtiene el timestamp de generación de la respuesta.
     *
     * @return timestamp en milisegundos desde epoch
     */
    public long getTimestamp() { return timestamp; }
}
