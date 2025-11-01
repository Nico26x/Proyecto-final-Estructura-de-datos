package co.edu.uniquindio.application.repository;

import co.edu.uniquindio.application.model.Cancion;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

@Repository
public class CancionRepository {

    private final String RUTA_ARCHIVO = "src/main/resources/data/canciones.txt";
    private List<Cancion> canciones;

    public CancionRepository() {
        this.canciones = new LinkedList<>();
        cargarCanciones();
    }

    /**
     * 🔹 Devuelve la lista completa de canciones en memoria.
     */
    public List<Cancion> listarCanciones() {
        return canciones;
    }

    /**
     * 🔹 Busca una canción por su ID.
     */
    public Cancion buscarPorId(String id) {
        return canciones.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 🔹 Agrega una nueva canción (si no existe ya).
     * También la guarda en el archivo.
     */
    public boolean agregarCancion(Cancion nuevaCancion) {
        if (buscarPorId(nuevaCancion.getId()) != null) {
            return false; // Ya existe una canción con ese ID
        }
        canciones.add(nuevaCancion);
        guardarCancionesEnArchivo();
        return true;
    }

    /**
     * 🔹 Elimina una canción por su ID.
     */
    public boolean eliminarCancion(String id) {
        Cancion encontrada = buscarPorId(id);
        if (encontrada != null) {
            canciones.remove(encontrada);
            guardarCancionesEnArchivo();
            return true;
        }
        return false;
    }

    /**
     * 🔹 Actualiza una canción existente.
     * Busca por ID y reemplaza sus datos con los del objeto recibido.
     */
    public boolean actualizarCancion(Cancion cancionActualizada) {
        Cancion existente = buscarPorId(cancionActualizada.getId());
        if (existente != null) {
            canciones.remove(existente);
            canciones.add(cancionActualizada);
            guardarCancionesEnArchivo();
            return true;
        }
        return false;
    }

    /**
     * 🔹 Carga las canciones desde el archivo .txt.
     */
    private void cargarCanciones() {
        File archivo = new File(RUTA_ARCHIVO);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                Cancion cancion = Cancion.fromString(linea);
                if (cancion != null) {
                    canciones.add(cancion);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 🔹 Guarda todas las canciones en el archivo .txt.
     */
    private void guardarCancionesEnArchivo() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(RUTA_ARCHIVO))) {
            for (Cancion c : canciones) {
                bw.write(c.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 🔹 Busca canciones por título o género (o ambos).
     * Si no se pasa ningún parámetro, devuelve todas las canciones.
     */
    public List<Cancion> buscarPorFiltro(String titulo, String genero) {
        return canciones.stream()
                .filter(c -> {
                    boolean coincideTitulo = (titulo == null || titulo.isEmpty()) ||
                            c.getTitulo().toLowerCase().contains(titulo.toLowerCase());
                    boolean coincideGenero = (genero == null || genero.isEmpty()) ||
                            c.getGenero().toLowerCase().contains(genero.toLowerCase());
                    return coincideTitulo && coincideGenero;
                })
                .toList();
    }
}
