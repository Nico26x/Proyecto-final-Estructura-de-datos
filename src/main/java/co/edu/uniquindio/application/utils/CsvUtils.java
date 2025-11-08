package co.edu.uniquindio.application.utils;

import java.util.List;
import java.util.stream.Collectors;

public final class CsvUtils {

    private CsvUtils() {}

    public static String escape(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    public static String joinRow(List<String> cells) {
        return cells.stream().map(CsvUtils::escape).collect(Collectors.joining(","));
    }
}
