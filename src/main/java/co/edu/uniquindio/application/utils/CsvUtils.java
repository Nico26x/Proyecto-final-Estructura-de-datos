package co.edu.uniquindio.application.utils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilidad para manejar operaciones de formato CSV (Comma-Separated Values).
 * <p>
 * Proporciona métodos para escapar valores y construir filas CSV válidas,
 * manejando correctamente caracteres especiales como comas, comillas y saltos de línea.
 * </p>
 *
 * @author SyncUp
 * @version 1.0
 */
public final class CsvUtils {

    /**
     * Constructor privado. Esta clase es una utilidad y no debe ser instanciada.
     */
    private CsvUtils() {}

    /**
     * Escapa un valor de texto para cumplir con el estándar CSV.
     * <p>
     * Este método realiza las siguientes operaciones:
     * </p>
     * <ul>
     *   <li>Duplica las comillas internas (") para escaparlas</li>
     *   <li>Envuelve el valor entre comillas si contiene comas, comillas, saltos de línea o retornos de carro</li>
     * </ul>
     *
     * @param value el valor a escapar. Puede ser {@code null}
     * @return el valor escapado listo para usar en CSV, o una cadena vacía si el valor es {@code null}
     */
    public static String escape(String value) {
        if (value == null) return "";
        // 1) duplicar comillas internas
        String escaped = value.replace("\"", "\"\"");
        // 2) ¿necesita envolver? (coma, comillas o salto de línea)
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    /**
     * Construye una fila CSV a partir de una lista de celdas.
     * <p>
     * Escapa automáticamente cada celda según las reglas CSV y las une con comas.
     * </p>
     *
     * @param cells lista de valores de las celdas a unir
     * @return una cadena de texto formada según el formato CSV, con celdas separadas por comas
     */
    public static String joinRow(java.util.List<String> cells) {
        return cells.stream().map(CsvUtils::escape).collect(java.util.stream.Collectors.joining(","));
    }
}

