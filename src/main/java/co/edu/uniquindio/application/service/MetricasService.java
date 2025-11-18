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

/**
 * Servicio de gestión de métricas y análisis de datos del sistema.
 * <p>
 * Proporciona funcionalidades para registrar eventos, analizar exportaciones de favoritos,
 * y generar reportes estadísticos sobre artistas, géneros y canciones más populares.
 * </p>
 * <p>
 * Características principales:
 * </p>
 * <ul>
 *   <li>Registro de eventos en archivo CSV centralizado (metricas.csv)</li>
 *   <li>Análisis de exportaciones de favoritos por usuario y día</li>
 *   <li>Estadísticas de artistas, géneros y canciones más descargados</li>
 *   <li>Consultas con rango de fechas</li>
 *   <li>Acceso thread-safe mediante ReentrantLock</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@Service
public class MetricasService {

    private static final String BASE_DIR = "src/main/resources/data";
    private static final String METRICAS_DIR = BASE_DIR + "/metricas";
    private static final String METRICAS_MASTER = METRICAS_DIR + "/metricas.csv";
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Lock para garantizar acceso thread-safe al archivo de métricas.
     */
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

    /**
     * Registra un evento en el archivo maestro de métricas.
     * <p>
     * Añade una nueva línea con timestamp, usuario, acción y detalles.
     * Acceso sincronizado mediante ReentrantLock para thread-safety.
     * </p>
     *
     * @param username el usuario que realiza la acción
     * @param accion la acción realizada (ej: "EXPORT_FAVORITOS")
     * @param detalle información adicional sobre la acción
     */
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

    /**
     * Registra específicamente una exportación de favoritos.
     * <p>
     * Conveniencia para registrar eventos de exportación con la cantidad de favoritos.
     * </p>
     *
     * @param username el usuario que realiza la exportación
     * @param cantidadFavoritos el número de canciones favoritas exportadas
     */
    public void registrarExportFavoritos(String username, int cantidadFavoritos) {
        registrarEvento(username, "EXPORT_FAVORITOS", "count=" + cantidadFavoritos);
    }

    /* =========================
       LECTURAS / AGREGACIONES
       ========================= */

    /**
     * Conteo de exportaciones de favoritos agrupadas por día (formato yyyy-MM-dd).
     *
     * @return mapa con fechas como clave y cantidad de exportaciones como valor
     */
    public Map<String, Long> descargasFavoritosPorDia() {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        return rows.stream()
                .filter(r -> r.length >= 3 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2]))
                .collect(Collectors.groupingBy(
                        r -> r[0].substring(0, 10), // fecha
                        Collectors.counting()
                ));
    }

    /**
     * Obtiene los usuarios con más exportaciones de favoritos.
     *
     * @param limit el número máximo de usuarios a retornar
     * @return lista de pares usuario-cantidad ordenados descendentemente
     */
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
     * Obtiene los artistas más descargados desde todos los archivos de favoritos.
     * <p>
     * Lee los archivos favoritos_*.csv y agrega el conteo por artista.
     * </p>
     *
     * @param limit el número máximo de artistas a retornar
     * @return mapa ordenado descendentemente por cantidad de descargas
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

    /**
     * Obtiene los géneros más descargados desde todos los archivos de favoritos.
     * <p>
     * Lee los archivos favoritos_*.csv y agrega el conteo por género.
     * </p>
     *
     * @param limit el número máximo de géneros a retornar
     * @return mapa ordenado descendentemente por cantidad de descargas
     */
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

    /**
     * Obtiene el total de eventos EXPORT_FAVORITOS registrados.
     *
     * @return cantidad total de exportaciones de favoritos
     */
    public long totalDescargasFavoritos() {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        return rows.stream()
                .filter(r -> r.length >= 3 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2]))
                .count();
    }

    /**
     * Obtiene un mapa de usuarios y su cantidad de exportaciones de favoritos.
     *
     * @return mapa con username como clave y cantidad de exportaciones como valor
     */
    public Map<String, Long> descargasFavoritosPorUsuario() {
        List<String[]> rows = leerCSV(Paths.get(METRICAS_MASTER));
        return rows.stream()
                .filter(r -> r.length >= 3 && "EXPORT_FAVORITOS".equalsIgnoreCase(r[2]))
                .collect(Collectors.groupingBy(r -> r[1], Collectors.counting()));
    }

    /**
     * Conteo de exportaciones de favoritos por día dentro de un rango de fechas.
     * <p>
     * Incluye días sin eventos con conteo 0 para facilitar gráficos continuos.
     * </p>
     *
     * @param desde fecha de inicio (formato yyyy-MM-dd)
     * @param hasta fecha de fin (formato yyyy-MM-dd)
     * @return mapa con fechas completas del rango y sus conteos
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
     * Calcula el promedio de canciones favoritas exportadas por evento.
     * <p>
     * Extrae el conteo del campo detalle (formato "count=X") y calcula el promedio.
     * </p>
     *
     * @return el promedio de favoritos por exportación, o 0.0 si no hay datos
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

    /**
     * Obtiene los últimos N eventos de exportación de favoritos.
     * <p>
     * Ordena cronológicamente en orden descendente.
     * </p>
     *
     * @param n el número máximo de eventos a retornar
     * @return lista de eventos ordenados descendentemente por timestamp
     */
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
     * Obtiene las canciones más descargadas globalmente desde todos los archivos de favoritos.
     * <p>
     * Agrega el conteo por título de canción desde todos los archivos favoritos_*.csv.
     * </p>
     *
     * @param limit el número máximo de canciones a retornar
     * @return mapa ordenado descendentemente por cantidad de descargas
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

    /**
     * Obtiene los artistas más descargados dentro del archivo de favoritos de un usuario específico.
     *
     * @param username el nombre del usuario
     * @param limit el número máximo de artistas a retornar
     * @return mapa ordenado descendentemente por cantidad de descargas, o vacío si el archivo no existe
     */
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

    /**
     * Obtiene los géneros más descargados dentro del archivo de favoritos de un usuario específico.
     *
     * @param username el nombre del usuario
     * @param limit el número máximo de géneros a retornar
     * @return mapa ordenado descendentemente por cantidad de descargas, o vacío si el archivo no existe
     */
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

    /**
     * Lista todos los archivos de favoritos en el directorio de reportes.
     * <p>
     * Busca archivos con patrón favoritos_*.csv.
     * </p>
     *
     * @return lista de rutas a archivos de favoritos, o lista vacía si el directorio no existe
     */
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

    /**
     * Lee un archivo CSV y lo convierte en una lista de arrays de strings.
     * <p>
     * Maneja correctamente caracteres especiales y comillas dobles según el estándar CSV.
     * </p>
     *
     * @param path la ruta del archivo CSV a leer
     * @return lista de arrays de strings representando las filas del CSV, o lista vacía si el archivo no existe
     */
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

    /**
     * Analiza una línea CSV respetando comillas dobles como delimitador de campos.
     * <p>
     * Implementa parsing estándar de CSV:
     * </p>
     * <ul>
     *   <li>Los campos se separan por comas</li>
     *   <li>Las comillas dobles dentro de un campo se escapan duplicándose</li>
     *   <li>Los campos entrecomillados pueden contener comas y saltos de línea</li>
     * </ul>
     *
     * @param line la línea de texto a analizar
     * @return array de strings con los campos parseados
     */
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

    /**
     * Escapa un valor de texto para cumplir con el estándar CSV.
     * <p>
     * Duplica las comillas internas y envuelve el valor con comillas si contiene
     * caracteres especiales (comas, comillas, saltos de línea o retornos de carro).
     * </p>
     *
     * @param v el valor a escapar
     * @return el valor escapado listo para usar en CSV, o cadena vacía si es {@code null}
     */
    private String escape(String v) {
        if (v == null) return "";
        boolean needs = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String e = v.replace("\"", "\"\"");
        return needs ? "\"" + e + "\"" : e;
    }

    /* =========================
       DTOs
       ========================= */

    /**
     * DTO que representa un evento registrado en las métricas.
     * <p>
     * Contiene la información de un evento: timestamp, usuario, acción y detalles.
     * </p>
     */
    public static class EventoDTO {
        /**
         * Timestamp del evento en formato "yyyy-MM-dd HH:mm:ss".
         */
        public final String timestamp;

        /**
         * Nombre de usuario que realizó la acción.
         */
        public final String username;

        /**
         * Tipo de acción realizada (ej: "EXPORT_FAVORITOS").
         */
        public final String accion;

        /**
         * Información adicional sobre la acción.
         */
        public final String detalle;

        /**
         * Constructor de EventoDTO.
         *
         * @param timestamp el timestamp del evento
         * @param username el usuario que realizó la acción
         * @param accion la acción realizada
         * @param detalle información adicional
         */
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

    /**
     * Extrae el conteo de favoritos desde el campo detalle.
     * <p>
     * Espera formato "count=NUM" y devuelve el número extraído.
     * </p>
     *
     * @param detalle el campo detalle del evento
     * @return el conteo extraído, o {@code null} si el formato no es válido
     */
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
