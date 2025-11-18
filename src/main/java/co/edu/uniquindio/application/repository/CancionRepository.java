package co.edu.uniquindio.application.repository;

import co.edu.uniquindio.application.model.Cancion;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Repositorio de canciones con persistencia en archivo.
 * <p>
 * Gestiona la lectura, escritura y búsqueda de canciones utilizando un archivo
 * de texto (canciones.txt) como almacenamiento. Proporciona operaciones CRUD completas
 * y búsqueda avanzada concurrente.
 * </p>
 * <p>
 * Características:
 * </p>
 * <ul>
 *   <li>Persistencia en archivo con soporte para 6 o 7 columnas</li>
 *   <li>Almacenamiento en memoria con ConcurrentHashMap para thread-safety</li>
 *   <li>Búsqueda simple por título y género</li>
 *   <li>Búsqueda avanzada concurrente con operadores AND/OR</li>
 *   <li>Control automático de IDs duplicados</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@Repository
public class CancionRepository {

    /**
     * Almacenamiento en memoria de canciones (thread-safe).
     */
    private final Map<String, Cancion> canciones = new ConcurrentHashMap<>();

    /**
     * Ruta del archivo de persistencia de canciones.
     */
    private static final String FILE_PATH = "src/main/resources/data/canciones.txt";

    /**
     * Constructor que carga las canciones desde el archivo al inicializar.
     */
    public CancionRepository() {
        cargarCancionesDesdeArchivo();
    }

    /**
     * Busca una canción por su identificador único.
     *
     * @param id el identificador de la canción a buscar
     * @return la canción si existe, {@code null} en caso contrario
     */
    public Cancion buscarPorId(String id) {
        return canciones.get(id);
    }

    /**
     * Lista todas las canciones registradas en el repositorio.
     *
     * @return colección con todas las canciones almacenadas
     */
    public Collection<Cancion> listarCanciones() {
        return canciones.values();
    }

    /**
     * Agrega una nueva canción al repositorio.
     * <p>
     * Si el ID de la canción ya existe, se asigna automáticamente un nuevo ID disponible
     * y se registra una advertencia en la consola.
     * </p>
     *
     * @param cancion la canción a agregar
     */
    public void agregarCancion(Cancion cancion) {
        String id = cancion.getId();

        // Si el ID ya existe, asignar el siguiente disponible
        if (canciones.containsKey(id)) {
            id = obtenerSiguienteIdDisponible();
            cancion.setId(id);
            System.out.println("⚠️ ID duplicado detectado. Se asignó nuevo ID: " + id);
        }

        canciones.put(id, cancion);
        guardarCancionesEnArchivo();
    }

    /**
     * Actualiza una canción existente en el repositorio.
     *
     * @param cancion la canción con los datos actualizados
     * @return {@code true} si la actualización fue exitosa, {@code false} si la canción no existe
     */
    public boolean actualizarCancion(Cancion cancion) {
        if (!canciones.containsKey(cancion.getId())) return false;
        canciones.put(cancion.getId(), cancion);
        guardarCancionesEnArchivo();
        return true;
    }

    /**
     * Elimina una canción del repositorio por su identificador.
     *
     * @param id el identificador de la canción a eliminar
     * @return {@code true} si la eliminación fue exitosa, {@code false} si la canción no existe
     */
    public boolean eliminarCancion(String id) {
        Cancion eliminada = canciones.remove(id);
        if (eliminada != null) {
            guardarCancionesEnArchivo();
            return true;
        }
        return false;
    }

    /**
     * Carga las canciones desde el archivo de persistencia.
     * <p>
     * Soporta archivos con 6 o 7 columnas separadas por punto y coma.
     * Si la lectura falla, registra un mensaje de error pero no detiene la ejecución.
     * </p>
     */
    private void cargarCancionesDesdeArchivo() {
        File archivo = new File(FILE_PATH);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                Cancion c = Cancion.fromString(linea);
                if (c != null) {
                    canciones.put(c.getId(), c);
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error al cargar canciones: " + e.getMessage());
        }
    }

    /**
     * Guarda todas las canciones en el archivo de persistencia.
     * <p>
     * Escribe 6 o 7 columnas según si la canción contiene o no un nombre de archivo asociado.
     * Las columnas se separan con punto y coma y se codifican en UTF-8.
     * </p>
     */
    private void guardarCancionesEnArchivo() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_PATH), StandardCharsets.UTF_8))) {

            for (Cancion c : canciones.values()) {
                if (c.getFileName() != null && !c.getFileName().isBlank()) {
                    bw.write(String.format("%s;%s;%s;%s;%d;%.2f;%s",
                            c.getId(), c.getTitulo(), c.getArtista(),
                            c.getGenero(), c.getAnio(), c.getDuracion(), c.getFileName()));
                } else {
                    bw.write(String.format("%s;%s;%s;%s;%d;%.2f",
                            c.getId(), c.getTitulo(), c.getArtista(),
                            c.getGenero(), c.getAnio(), c.getDuracion()));
                }
                bw.newLine();
            }

        } catch (IOException e) {
            System.err.println("❌ Error al guardar canciones: " + e.getMessage());
        }
    }

    /**
     * Realiza una búsqueda simple de canciones por título y/o género.
     * <p>
     * La búsqueda es case-insensitive y utiliza contención de subcadenas.
     * Ambos parámetros son opcionales (pueden ser {@code null}).
     * </p>
     *
     * @param titulo el título o parte del título a buscar (opcional)
     * @param genero el género o parte del género a buscar (opcional)
     * @return lista de canciones que coinciden con los criterios
     */
    public List<Cancion> buscarPorFiltro(String titulo, String genero) {
        List<Cancion> resultado = new ArrayList<>();

        for (Cancion c : canciones.values()) {
            boolean coincideTitulo = (titulo == null || c.getTitulo().toLowerCase().contains(titulo.toLowerCase()));
            boolean coincideGenero = (genero == null || c.getGenero().toLowerCase().contains(genero.toLowerCase()));

            if (coincideTitulo && coincideGenero) {
                resultado.add(c);
            }
        }
        return resultado;
    }

    /**
     * Realiza una búsqueda avanzada concurrente de canciones con múltiples criterios.
     * <p>
     * Ejecuta búsquedas por título, artista, género y rango de años en paralelo,
     * combinando los resultados con el operador especificado (AND u OR).
     * </p>
     * <p>
     * Implementa RF-004 (Búsqueda avanzada) y RF-030 (Búsqueda concurrente).
     * </p>
     *
     * @param titulo título o parte del título a buscar (opcional)
     * @param artista artista o parte del nombre a buscar (opcional)
     * @param genero género o parte del género a buscar (opcional)
     * @param anioFrom año inicial del rango (opcional)
     * @param anioTo año final del rango (opcional)
     * @param op operador de combinación: "AND" para intersección, "OR" para unión
     * @return lista de canciones que cumplen con los criterios de búsqueda
     */
    public List<Cancion> buscarAvanzadaConcurrente(String titulo,
                                                   String artista,
                                                   String genero,
                                                   Integer anioFrom,
                                                   Integer anioTo,
                                                   String op) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<List<Cancion>>> tareas = new ArrayList<>();

        if (titulo != null && !titulo.isBlank()) {
            tareas.add(executor.submit(() -> canciones.values().stream()
                    .filter(c -> c.getTitulo().toLowerCase().contains(titulo.toLowerCase()))
                    .collect(Collectors.toList())));
        }

        if (artista != null && !artista.isBlank()) {
            tareas.add(executor.submit(() -> canciones.values().stream()
                    .filter(c -> c.getArtista().toLowerCase().contains(artista.toLowerCase()))
                    .collect(Collectors.toList())));
        }

        if (genero != null && !genero.isBlank()) {
            tareas.add(executor.submit(() -> canciones.values().stream()
                    .filter(c -> c.getGenero().toLowerCase().contains(genero.toLowerCase()))
                    .collect(Collectors.toList())));
        }

        if (anioFrom != null || anioTo != null) {
            tareas.add(executor.submit(() -> canciones.values().stream()
                    .filter(c -> (anioFrom == null || c.getAnio() >= anioFrom) &&
                            (anioTo == null || c.getAnio() <= anioTo))
                    .collect(Collectors.toList())));
        }

        List<List<Cancion>> resultados = new ArrayList<>();
        for (Future<List<Cancion>> tarea : tareas) {
            try {
                resultados.add(tarea.get());
            } catch (Exception e) {
                System.err.println("⚠️ Error en hilo de búsqueda: " + e.getMessage());
            }
        }
        executor.shutdown();

        if (resultados.isEmpty()) return new ArrayList<>();

        Set<Cancion> combinado = new HashSet<>(resultados.get(0));
        if ("AND".equalsIgnoreCase(op)) {
            for (List<Cancion> lista : resultados.subList(1, resultados.size())) {
                combinado.retainAll(lista);
            }
        } else { // OR por defecto
            for (List<Cancion> lista : resultados.subList(1, resultados.size())) {
                combinado.addAll(lista);
            }
        }

        return new ArrayList<>(combinado);
    }

    /**
     * Auxiliar privado que genera el siguiente ID disponible de forma automática.
     * <p>
     * Busca el ID numérico máximo existente e incrementa en 1.
     * Si no hay IDs numéricos, comienza con "1".
     * </p>
     *
     * @return el siguiente ID disponible como String
     */
    private String obtenerSiguienteIdDisponible() {
        if (canciones.isEmpty()) {
            return "1";
        }
        int maxId = canciones.keySet().stream()
                .filter(id -> id.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return String.valueOf(maxId + 1);
    }
}
