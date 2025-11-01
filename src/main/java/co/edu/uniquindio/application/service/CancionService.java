package co.edu.uniquindio.application.service;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.repository.CancionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
public class CancionService {

    private final CancionRepository cancionRepository;

    @Autowired
    public CancionService(CancionRepository cancionRepository) {
        this.cancionRepository = cancionRepository;
    }

    public Collection<Cancion> listarCanciones() {
        return cancionRepository.listarCanciones();
    }

    public Cancion buscarPorId(String id) {
        return cancionRepository.buscarPorId(id);
    }

    public void agregarCancion(Cancion cancion) {
        cancionRepository.agregarCancion(cancion);
    }

    public boolean actualizarCancion(Cancion cancion) {
        return cancionRepository.actualizarCancion(cancion);
    }

    public boolean eliminarCancion(String id) {
        return cancionRepository.eliminarCancion(id);
    }

    // 🎵 Búsqueda básica por título o género
    public List<Cancion> buscarPorFiltro(String titulo, String genero) {
        return cancionRepository.buscarPorFiltro(titulo, genero);
    }

    // 🔍 Búsqueda avanzada concurrente (RF-004 + RF-030)
    public List<Cancion> buscarAvanzada(String titulo,
                                        String artista,
                                        String genero,
                                        Integer anioFrom,
                                        Integer anioTo,
                                        String op) {
        return cancionRepository.buscarAvanzadaConcurrente(titulo, artista, genero, anioFrom, anioTo, op);
    }

    public int cargarCancionesMasivamente(MultipartFile archivo) throws Exception {
        int contador = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length == 6) {
                    String id = partes[0].trim();
                    String titulo = partes[1].trim();
                    String artista = partes[2].trim();
                    String genero = partes[3].trim();
                    int anio = Integer.parseInt(partes[4].trim());
                    double duracion = Double.parseDouble(partes[5].trim().replace(",", "."));

                    Cancion nueva = new Cancion(id, titulo, artista, genero, anio, duracion);
                    cancionRepository.agregarCancion(nueva);
                    contador++;
                }
            }
        } catch (Exception e) {
            throw new Exception("Error al procesar el archivo: " + e.getMessage());
        }

        return contador;
    }
}
