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

/**
 * Servicio de negocio para la gestión de canciones.
 * <p>
 * Proporciona funcionalidades de CRUD, búsqueda avanzada, autocompletado de títulos
 * y recomendación de canciones similares mediante un grafo de similitud.
 * </p>
 * <p>
 * Características principales:
 * </p>
 * <ul>
 *   <li>Gestión de canciones (agregar, actualizar, eliminar, listar)</li>
 *   <li>Autocompletado de títulos usando estructura Trie</li>
 *   <li>Búsqueda avanzada concurrente por múltiples criterios</li>
 *   <li>Construcción de grafo de similitud entre canciones</li>
 *   <li>Recomendación de canciones similares y radio personalizada</li>
 *   <li>Carga masiva de canciones desde archivo CSV</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@Service
public class CancionService {

    private final CancionRepository cancionRepository;

    /**
     * Estructura Trie para autocompletado eficiente de títulos de canciones.
     */
    private final TrieAutocompletado trieAutocompletado;

    /**
     * Grafo de similitud que almacena las relaciones de parecido entre canciones
     * basado en géneros, artistas y otras características.
     */
    private final GrafoDeSimilitud grafoDeSimilitud;

    /**
     * Constructor que inicializa el servicio de canciones.
     * <p>
     * Carga inicial de títulos en el Trie y construcción del grafo de similitud.
     * </p>
     *
     * @param cancionRepository el repositorio de canciones
     */
    @Autowired
    public CancionService(CancionRepository cancionRepository) {
        this.cancionRepository = cancionRepository;
        this.trieAutocompletado = new TrieAutocompletado();
        this.grafoDeSimilitud = new GrafoDeSimilitud();
        inicializarTrie();             // carga inicial de títulos
        construirGrafoDeSimilitud();   // construye el grafo desde las canciones actuales
    }

    /**
     * Carga inicial de todos los títulos de canciones en la estructura Trie.
     * <p>
     * Se ejecuta en la inicialización del servicio para habilitar el autocompletado
     * desde el primer momento.
     * </p>
     */
    private void inicializarTrie() {
    // Nota: TrieAutocompletado no implementa 'limpiar()' en tu versión actual,
        // por eso no intentamos llamar a un método inexistente. Insertar duplicados
        // generalmente no rompe la búsqueda por prefijos; si quieres evitar duplicados
        // preferimos hacerlo dentro del propio Trie (recomendado).
        for (Cancion c : cancionRepository.listarCanciones()) {
            trieAutocompletado.insertarPalabra(c.getTitulo());
        }
    }

    /**
     * Obtiene sugerencias de títulos de canciones que coinciden con un prefijo.
     *
     * @param prefijo el prefijo a buscar. No debe ser {@code null}
     * @return lista de títulos de canciones que comienzan con el prefijo especificado
     */
    public List<String> autocompletarTitulo(String prefijo) {
        return trieAutocompletado.buscarPorPrefijo(prefijo);
    }

    /**
     * Obtiene todas las canciones del repositorio.
     *
     * @return colección de todas las canciones disponibles
     */
    public Collection<Cancion> listarCanciones() {
        return cancionRepository.listarCanciones();
    }

    /**
     * Busca una canción por su identificador único.
     *
     * @param id el identificador de la canción
     * @return la canción si existe, {@code null} en caso contrario
     */
    public Cancion buscarPorId(String id) {
        return cancionRepository.buscarPorId(id);
    }

    /**
     * Agrega una nueva canción al repositorio.
     * <p>
     * También inserta el título en el Trie para autocompletado
     * y reconstruye el grafo de similitud.
     * </p>
     *
     * @param cancion la canción a agregar. No debe ser {@code null}
     */
    public void agregarCancion(Cancion cancion) {
        cancionRepository.agregarCancion(cancion);
        trieAutocompletado.insertarPalabra(cancion.getTitulo());
        construirGrafoDeSimilitud(); // reconstruye el grafo al agregar
    }

    /**
     * Actualiza los datos de una canción existente.
     * <p>
     * Si la actualización es exitosa, refresca el Trie y el grafo de similitud.
     * </p>
     *
     * @param cancion la canción con los datos actualizados. No debe ser {@code null}
     * @return {@code true} si la canción fue actualizada, {@code false} en caso contrario
     */
    public boolean actualizarCancion(Cancion cancion) {
        boolean actualizado = cancionRepository.actualizarCancion(cancion);

        // Si se actualiza el título, refrescar el Trie y el grafo
        if (actualizado) {
            inicializarTrie();
            construirGrafoDeSimilitud();
        }
        return actualizado;
    }

    /**
     * Elimina una canción del repositorio por su identificador.
     * <p>
     * Si la eliminación es exitosa, refresca el Trie y el grafo de similitud.
     * </p>
     *
     * @param id el identificador de la canción a eliminar
     * @return {@code true} si la canción fue eliminada, {@code false} en caso contrario
     */
    public boolean eliminarCancion(String id) {
        boolean eliminado = cancionRepository.eliminarCancion(id);

        // Si se elimina, refrescar el Trie y el grafo
        if (eliminado) {
            inicializarTrie();
            construirGrafoDeSimilitud();
        }
        return eliminado;
    }

    /**
     * Busca canciones por filtros básicos (título y género).
     *
     * @param titulo el título de la canción a buscar (puede ser parcial)
     * @param genero el género de la canción
     * @return lista de canciones que coinciden con los filtros especificados
     */
    public List<Cancion> buscarPorFiltro(String titulo, String genero) {
        return cancionRepository.buscarPorFiltro(titulo, genero);
    }

    /**
     * Realiza una búsqueda avanzada y concurrente de canciones por múltiples criterios.
     * <p>
     * Implementa los requisitos funcionales RF-004 y RF-030.
     * </p>
     *
     * @param titulo el título de la canción (opcional)
     * @param artista el artista de la canción (opcional)
     * @param genero el género de la canción (opcional)
     * @param anioFrom el año mínimo (opcional)
     * @param anioTo el año máximo (opcional)
     * @param op el operador lógico para combinar criterios (AND/OR)
     * @return lista de canciones que coinciden con los criterios especificados
     */
    public List<Cancion> buscarAvanzada(String titulo,
                                        String artista,
                                        String genero,
                                        Integer anioFrom,
                                        Integer anioTo,
                                        String op) {
        return cancionRepository.buscarAvanzadaConcurrente(titulo, artista, genero, anioFrom, anioTo, op);
    }

    /**
     * Carga un lote masivo de canciones desde un archivo CSV.
     * <p>
     * El archivo debe tener el siguiente formato (con separador semicolon):
     * </p>
     * <ul>
     *   <li>6 columnas: id;titulo;artista;genero;anio;fileName</li>
     *   <li>7 columnas: id;titulo;artista;genero;anio;duracion;fileName</li>
     * </ul>
     * <p>
     * Líneas en blanco o que comienzan con '#' son ignoradas.
     * </p>
     *
     * @param archivo el archivo MultipartFile con las canciones a cargar
     * @return el número de canciones cargadas exitosamente
     * @throws Exception si ocurre un error al procesar el archivo
     */
    public int cargarCancionesMasivamente(MultipartFile archivo) throws Exception {
        int contador = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.isBlank() || linea.trim().startsWith("#")) continue;

                String[] partes = linea.split(";");
                for (int i = 0; i < partes.length; i++) partes[i] = partes[i].trim();

                if (partes.length != 6 && partes.length != 7) {
                    // formato inválido -> saltar fila
                    continue;
                }

                String id      = partes[0];
                String titulo  = partes[1];
                String artista = partes[2];
                String genero  = partes[3];
                int anio       = Integer.parseInt(partes[4]);

                Double duracion = null;
                String fileName;

                if (partes.length == 7) {
                    // id;titulo;artista;genero;anio;duracion;fileName
                    String durRaw = partes[5].replace(',', '.');
                    duracion = Double.valueOf(durRaw);
                    fileName = partes[6];
                } else {
                    // 6 columnas → asumimos que es fileName
                    // id;titulo;artista;genero;anio;fileName
                    fileName = partes[5];
                }

                // Crea la entidad y asigna el fileName
                Cancion nueva = new Cancion(id, titulo, artista, genero, anio,
                        duracion != null ? duracion : 0.0);
                // Asegúrate de que tu entidad tenga setter; si no, agrega un constructor con fileName
                nueva.setFileName(fileName);

                // Persistir
                cancionRepository.agregarCancion(nueva);

                // Autocompletar (puedes incluir artista si quieres)
                trieAutocompletado.insertarPalabra(titulo);

                contador++;
            }

            // reconstruimos el grafo después de la carga masiva
            construirGrafoDeSimilitud();

            // Si tu repositorio necesita reescribir canciones.txt explícitamente, llama aquí:
            // cancionRepository.reconstruirArchivoCanciones();

        } catch (Exception e) {
            throw new Exception("Error al procesar el archivo: " + e.getMessage());
        }

        return contador;
    }


    /**
     * Construye o reconstruye el grafo de similitud entre canciones.
     * <p>
     * Utiliza todas las canciones del repositorio para establecer relaciones
     * de similitud basadas en criterios como género, artista, etc.
     * </p>
     */
    public void construirGrafoDeSimilitud() {
        grafoDeSimilitud.construirGrafo(cancionRepository.listarCanciones());
    }

    /**
     * Obtiene un conjunto de canciones similares a una canción especificada.
     *
     * @param idCancion el identificador de la canción de origen
     * @param limite el número máximo de canciones similares a retornar
     * @return lista de canciones similares, o lista vacía si la canción no existe
     */
    public List<Cancion> obtenerCancionesSimilares(String idCancion, int limite) {
        Cancion origen = cancionRepository.buscarPorId(idCancion);
        if (origen == null) return List.of();
        return grafoDeSimilitud.obtenerSimilares(origen, limite);
    }

    /**
     * Inicia una estación de radio personalizada basada en una canción.
     * <p>
     * Retorna la canción original seguida de una lista de canciones similares
     * para crear una cola de reproducción personalizada.
     * </p>
     *
     * @param idCancion el identificador de la canción a partir de la cual iniciar la radio
     * @param limite el número máximo de canciones similares a incluir
     * @return lista de canciones con la canción original al inicio, o lista vacía si la canción no existe
     */
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
