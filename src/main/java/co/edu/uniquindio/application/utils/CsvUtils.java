package co.edu.uniquindio.application.utils;

import java.util.List;
import java.util.stream.Collectors;

public final class CsvUtils {

    private CsvUtils() {}

    public static String escape(String value) {
        if (value == null) return "";
        // 1) duplicar comillas internas
        String escaped = value.replace("\"", "\"\"");
        // 2) ¿necesita envolver? (coma, comillas o salto de línea)
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    public static String joinRow(java.util.List<String> cells) {
        return cells.stream().map(CsvUtils::escape).collect(java.util.stream.Collectors.joining(","));
    }
}

