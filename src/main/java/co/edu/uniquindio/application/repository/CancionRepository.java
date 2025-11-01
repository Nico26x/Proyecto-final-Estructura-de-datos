package co.edu.uniquindio.application.repository;

import co.edu.uniquindio.application.model.Cancion;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    public void agregarCancion(Cancion cancion) {
        canciones.put(cancion.getId(), cancion);
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
}
