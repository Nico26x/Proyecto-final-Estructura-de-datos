package co.edu.uniquindio.application.repository;

import co.edu.uniquindio.application.model.Cancion;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Repositorio con persistencia en archivo canciones.txt
 */
@Repository
public class CancionRepository {

    private final Map<String, Cancion> canciones = new ConcurrentHashMap<>();
    private static final String FILE_PATH = "src/main/resources/data/canciones.txt";

    public CancionRepository() {
        cargarCancionesDesdeArchivo();
    }

    // ✅ Buscar canción por ID
    public Cancion buscarPorId(String id) {
        return canciones.get(id);
    }

    // ✅ Listar todas las canciones
    public Collection<Cancion> listarCanciones() {
        return canciones.values();
    }

    // ✅ Agregar canción
    // ✅ Agregar canción (ahora con control de IDs duplicados)
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


    // ✅ Actualizar canción
    public boolean actualizarCancion(Cancion cancion) {
        if (!canciones.containsKey(cancion.getId())) return false;
        canciones.put(cancion.getId(), cancion);
        guardarCancionesEnArchivo();
        return true;
    }

    // ✅ Eliminar canción
    public boolean eliminarCancion(String id) {
        Cancion eliminada = canciones.remove(id);
        if (eliminada != null) {
            guardarCancionesEnArchivo();
            return true;
        }
        return false;
    }

    // 🔹 Cargar canciones desde archivo
    private void cargarCancionesDesdeArchivo() {
        File archivo = new File(FILE_PATH);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length == 6) {
                    String id = partes[0];
                    String titulo = partes[1];
                    String artista = partes[2];
                    String genero = partes[3];
                    int anio = Integer.parseInt(partes[4]);
                    double duracion = Double.parseDouble(partes[5].replace(",", "."));
                    canciones.put(id, new Cancion(id, titulo, artista, genero, anio, duracion));
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error al cargar canciones: " + e.getMessage());
        }
    }

    // 💾 Guardar canciones en archivo
    private void guardarCancionesEnArchivo() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_PATH), StandardCharsets.UTF_8))) {

            for (Cancion c : canciones.values()) {
                bw.write(String.format("%s;%s;%s;%s;%d;%.2f",
                        c.getId(), c.getTitulo(), c.getArtista(),
                        c.getGenero(), c.getAnio(), c.getDuracion()));
                bw.newLine();
            }

        } catch (IOException e) {
            System.err.println("❌ Error al guardar canciones: " + e.getMessage());
        }
    }

    // 🔍 Búsqueda simple (por título y género)
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

    // ⚡ Búsqueda avanzada concurrente (RF-004 + RF-030)
    public List<Cancion> buscarAvanzadaConcurrente(String titulo,
                                                   String artista,
                                                   String genero,
                                                   Integer anioFrom,
                                                   Integer anioTo,
                                                   String op) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<List<Cancion>>> tareas = new ArrayList<>();

        // Crear tareas independientes por criterio
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

        // Esperar los resultados
        List<List<Cancion>> resultados = new ArrayList<>();
        for (Future<List<Cancion>> tarea : tareas) {
            try {
                resultados.add(tarea.get());
            } catch (Exception e) {
                System.err.println("⚠️ Error en hilo de búsqueda: " + e.getMessage());
            }
        }

        executor.shutdown();

        // Combinar resultados según operador lógico
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

    // ✅ Agregar esta función auxiliar dentro de CancionRepository
    private String obtenerSiguienteIdDisponible() {
        if (canciones.isEmpty()) {
            return "1";
        }

        // Obtener el número máximo actual de IDs (asumiendo que son numéricos)
        int maxId = canciones.keySet().stream()
                .filter(id -> id.matches("\\d+")) // solo números
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return String.valueOf(maxId + 1);
    }

}
