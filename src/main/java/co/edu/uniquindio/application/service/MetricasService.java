package co.edu.uniquindio.application.service;

import co.edu.uniquindio.application.model.Cancion;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class MetricasService {

    private static final String BASE_DIR = "src/main/resources/data";
    private static final String METRICAS_DIR = BASE_DIR + "/metricas";
    private static final String METRICAS_MASTER = METRICAS_DIR + "/metricas.csv";
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReentrantLock lock = new ReentrantLock();

    public MetricasService() {
        try {
            Files.createDirectories(Paths.get(METRICAS_DIR));
            if (!Files.exists(Paths.get(METRICAS_MASTER))) {
                // header del master
                Files.write(Paths.get(METRICAS_MASTER),
                        ("timestamp,username,accion,detalle\n").getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar directorio de métricas", e);
        }
    }

    /* =========================
       REGISTRO DE EVENTOS
       ========================= */

    /** Registra una línea en metricas.csv */
    public void registrarEvento(String username, String accion, String detalle) {
        lock.lock();
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(METRICAS_MASTER),
                StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            String ts = LocalDateTime.now().format(TS_FMT);
            bw.write(escape(ts) + "," + escape(username) + "," + escape(accion) + "," + escape(detalle));
            bw.write("\n");
        } catch (IOException e) {
            throw new RuntimeException("Error escribiendo métricas", e);
        } finally {
            lock.unlock();
        }
    }

    /** Conveniencia específica para exportación de favoritos */
    public void registrarExportFavoritos(String username, int cantidadFavoritos) {
        registrarEvento(username, "EXPORT_FAVORITOS", "count=" + cantidadFavoritos);
    }

    /* =========================
       LECTURAS / AGREGACIONES
       ========================= */

    /** Conteo de EXPORT_FAVORITOS por día (yyyy-MM-dd) */
    public Map<String, Long> descargasFavoritosPorDia() {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        return rows.stream()
                .filter(r -> r.length >= 3 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2]))
                .collect(Collectors.groupingBy(
                        r -> r[0].substring(0, 10), // fecha
                        Collectors.counting()
                ));
    }

    /** Top usuarios por cantidad de EXPORT_FAVORITOS */
    public List<Map.Entry<String, Long>> topUsuariosExportadores(int limit) {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        Map<String, Long> porUsuario = rows.stream()
                .filter(r -> r.length >= 3 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2]))
                .collect(Collectors.groupingBy(r -> r[1], Collectors.counting()));
        return porUsuario.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Lee TODOS los CSV de favoritos per-user en:
     *   src/main/resources/data/reportes/favoritos_*.csv
     * y calcula top artistas / géneros.
     */
    public Map<String, Long> topArtistasDesdeFavoritos(int limit) {
        Map<String, Long> conteo = new HashMap<>();
        for (Path p : listarFavoritosCsv()) {
            List<String[]> rows = leerCSV(p);
            // saltar header
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (r.length >= 3) {
                    String artista = r[2];
                    conteo.merge(artista, 1L, Long::sum);
                }
            }
        }
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    public Map<String, Long> topGenerosDesdeFavoritos(int limit) {
        Map<String, Long> conteo = new HashMap<>();
        for (Path p : listarFavoritosCsv()) {
            List<String[]> rows = leerCSV(p);
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (r.length >= 4) {
                    String genero = r[3];
                    conteo.merge(genero, 1L, Long::sum);
                }
            }
        }
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /* =========================
       NUEVOS MÉTODOS DE MÉTRICAS
       ========================= */

    /** Total de eventos EXPORT_FAVORITOS registrados. */
    public long totalDescargasFavoritos() {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        return rows.stream()
                .filter(r -> r.length >= 3 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2]))
                .count();
    }

    /** Mapa username -> total de EXPORT_FAVORITOS. */
    public Map<String, Long> descargasFavoritosPorUsuario() {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        return rows.stream()
                .filter(r -> r.length >= 3 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2]))
                .collect(Collectors.groupingBy(r -> r[1], Collectors.counting()));
    }

    /**
     * Conteo por día de EXPORT_FAVORITOS en rango [desde, hasta], formato yyyy-MM-dd.
     * Incluye días sin eventos con conteo 0 para facilitar gráficos continuos.
     */
    public Map<String, Long> descargasFavoritosPorRango(String desde, String hasta) {
        LocalDate start = LocalDate.parse(desde);
        LocalDate end = LocalDate.parse(hasta);
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));

        Map<String, Long> conteo = rows.stream()
                .filter(r -> r.length >= 3 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2]))
                .map(r -> r[0].substring(0, 10))
                .filter(fecha -> {
                    LocalDate d = LocalDate.parse(fecha);
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()));

        // Rellenar días vacíos
        Map<String, Long> completo = new LinkedHashMap<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            String key = cur.toString();
            completo.put(key, conteo.getOrDefault(key, 0L));
            cur = cur.plusDays(1);
        }
        return completo;
    }

    /**
     * Promedio de favoritos exportados por evento (usa detalle "count=X").
     * Si no hay datos, devuelve 0.0
     */
    public double promedioFavoritosPorExport() {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        long eventos = 0;
        long totalFavs = 0;
        for (String[] r : rows) {
            if (r.length >= 4 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2])) {
                Integer c = parseCountFromDetalle(r[3]);
                if (c != null) {
                    eventos++;
                    totalFavs += c;
                }
            }
        }
        return eventos == 0 ? 0.0 : (double) totalFavs / eventos;
    }

    /** Últimos N eventos (cualquier acción), orden cronológico descendente. */
    public List<EventoDTO> ultimasExportaciones(int n) {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        List<EventoDTO> eventos = new ArrayList<>();
        for (String[] r : rows) {
            if (r.length >= 4) {
                eventos.add(new EventoDTO(r[0], r[1], r[2], r[3]));
            }
        }
        // ordenar por timestamp desc (asumiendo formato fijo)
        eventos.sort(Comparator.comparing((EventoDTO e) -> e.timestamp).reversed());
        return eventos.stream()
                .filter(e -> "EXPORT_FAVORITOS".equalsIgnoreCase(e.accion))
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Top canciones global desde TODOS los CSV de favoritos (por título).
     * Si quieres por ID (más confiable), cambia el índice 1->0 abajo.
     */
    public Map<String, Long> topCancionesDesdeFavoritos(int limit) {
        Map<String, Long> conteo = new HashMap<>();
        for (Path p : listarFavoritosCsv()) {
            List<String[]> rows = leerCSV(p);
            for (int i = 1; i < rows.size(); i++) { // saltar header
                String[] r = rows.get(i);
                if (r.length >= 2) {
                    String titulo = r[1];
                    conteo.merge(titulo, 1L, Long::sum);
                }
            }
        }
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Top artistas dentro del CSV de favoritos de UN usuario. */
    public Map<String, Long> topArtistasPorUsuario(String username, int limit) {
        Path file = Paths.get(BASE_DIR, "reportes", "favoritos_" + username + ".csv");
        Map<String, Long> conteo = new HashMap<>();
        List<String[]> rows = leerCSV(file);
        for (int i = 1; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (r.length >= 3) {
                String artista = r[2];
                conteo.merge(artista, 1L, Long::sum);
            }
        }
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Top géneros dentro del CSV de favoritos de UN usuario. */
    public Map<String, Long> topGenerosPorUsuario(String username, int limit) {
        Path file = Paths.get(BASE_DIR, "reportes", "favoritos_" + username + ".csv");
        Map<String, Long> conteo = new HashMap<>();
        List<String[]> rows = leerCSV(file);
        for (int i = 1; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (r.length >= 4) {
                String genero = r[3];
                conteo.merge(genero, 1L, Long::sum);
            }
        }
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /* =========================
       HELPERS CSV / FS
       ========================= */

    private List<Path> listarFavoritosCsv() {
        Path dir = Paths.get(BASE_DIR, "reportes");
        if (!Files.exists(dir)) return List.of();
        try {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "favoritos_*.csv")) {
                List<Path> r = new ArrayList<>();
                for (Path p : ds) r.add(p);
                return r;
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    private List<String[]> leerCSV(Path path) {
        if (!Files.exists(path)) return List.of();
        List<String[]> out = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                out.add(parse(line));
            }
        } catch (IOException e) {
            // ignora lectura fallida
        }
        return out;
    }

    private String[] parse(String line) {
        // parse simple: divide por coma respetando comillas dobles
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    cur.append('\"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                cells.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        cells.add(cur.toString());
        return cells.toArray(new String[0]);
    }

    private String escape(String v) {
        if (v == null) return "";
        boolean needs = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String e = v.replace("\"", "\"\"");
        return needs ? "\"" + e + "\"" : e;
    }

    /* =========================
       DTOs
       ========================= */

    public static class EventoDTO {
        public final String timestamp; // "yyyy-MM-dd HH:mm:ss"
        public final String username;
        public final String accion;
        public final String detalle;

        public EventoDTO(String timestamp, String username, String accion, String detalle) {
            this.timestamp = timestamp;
            this.username = username;
            this.accion = accion;
            this.detalle = detalle;
        }
    }

    /* =========================
       PARSERS ESPECÍFICOS
       ========================= */

    private Integer parseCountFromDetalle(String detalle) {
        // espera formato "count=NUM"
        if (detalle == null) return null;
        String d = detalle.trim();
        if (d.startsWith("count=")) {
            try {
                return Integer.parseInt(d.substring("count=".length()).trim());
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }
}
