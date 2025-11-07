package co.edu.uniquindio.application.service;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.GrafoDeSimilitud;
import co.edu.uniquindio.application.repository.CancionRepository;
import co.edu.uniquindio.application.trie.TrieAutocompletado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class CancionService {

    private final CancionRepository cancionRepository;

    // ✅ Soporte para autocompletado con Trie
    private final TrieAutocompletado trieAutocompletado;

    // ✅ Grafo de similitud entre canciones
    private final GrafoDeSimilitud grafoDeSimilitud;

    @Autowired
    public CancionService(CancionRepository cancionRepository) {
        this.cancionRepository = cancionRepository;
        this.trieAutocompletado = new TrieAutocompletado();
        this.grafoDeSimilitud = new GrafoDeSimilitud();
        inicializarTrie();             // carga inicial de títulos
        construirGrafoDeSimilitud();   // construye el grafo desde las canciones actuales
    }

    // ✅ Carga inicial de títulos en el Trie
    private void inicializarTrie() {
        // Nota: TrieAutocompletado no implementa 'limpiar()' en tu versión actual,
        // por eso no intentamos llamar a un método inexistente. Insertar duplicados
        // generalmente no rompe la búsqueda por prefijos; si quieres evitar duplicados
        // preferimos hacerlo dentro del propio Trie (recomendado).
        for (Cancion c : cancionRepository.listarCanciones()) {
            trieAutocompletado.insertarPalabra(c.getTitulo());
        }
    }

    // ✅ Método de autocompletado
    public List<String> autocompletarTitulo(String prefijo) {
        return trieAutocompletado.buscarPorPrefijo(prefijo);
    }

    public Collection<Cancion> listarCanciones() {
        return cancionRepository.listarCanciones();
    }

    public Cancion buscarPorId(String id) {
        return cancionRepository.buscarPorId(id);
    }

    public void agregarCancion(Cancion cancion) {
        cancionRepository.agregarCancion(cancion);
        trieAutocompletado.insertarPalabra(cancion.getTitulo());
        construirGrafoDeSimilitud(); // reconstruye el grafo al agregar
    }

    public boolean actualizarCancion(Cancion cancion) {
        boolean actualizado = cancionRepository.actualizarCancion(cancion);

        // Si se actualiza el título, refrescar el Trie y el grafo
        if (actualizado) {
            inicializarTrie();
            construirGrafoDeSimilitud();
        }
        return actualizado;
    }

    public boolean eliminarCancion(String id) {
        boolean eliminado = cancionRepository.eliminarCancion(id);

        // Si se elimina, refrescar el Trie y el grafo
        if (eliminado) {
            inicializarTrie();
            construirGrafoDeSimilitud();
        }
        return eliminado;
    }

    // Búsqueda básica por título o género
    public List<Cancion> buscarPorFiltro(String titulo, String genero) {
        return cancionRepository.buscarPorFiltro(titulo, genero);
    }

    // Búsqueda avanzada concurrente (RF-004 + RF-030)
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
                    trieAutocompletado.insertarPalabra(titulo);
                    contador++;
                }
            }

            // reconstruimos el grafo después de la carga masiva
            construirGrafoDeSimilitud();

        } catch (Exception e) {
            throw new Exception("Error al procesar el archivo: " + e.getMessage());
        }

        return contador;
    }

    // Construcción y consulta del grafo de similitud
    public void construirGrafoDeSimilitud() {
        grafoDeSimilitud.construirGrafo(cancionRepository.listarCanciones());
    }

    public List<Cancion> obtenerCancionesSimilares(String idCancion, int limite) {
        Cancion origen = cancionRepository.buscarPorId(idCancion);
        if (origen == null) return List.of();
        return grafoDeSimilitud.obtenerSimilares(origen, limite);
    }

    public List<Cancion> iniciarRadio(String idCancion, int limite) {
        Cancion origen = cancionRepository.buscarPorId(idCancion);
        if (origen == null) return List.of();

        List<Cancion> similares = grafoDeSimilitud.obtenerSimilares(origen, limite);

        // Insertar la canción original al inicio de la "cola"
        List<Cancion> cola = new ArrayList<>();
        cola.add(origen);
        cola.addAll(similares);

        return cola;
    }

}
