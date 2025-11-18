package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.service.CancionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

/**
 * Controlador REST para la gestión del catálogo de canciones.
 * <p>
 * Proporciona endpoints para operaciones CRUD, búsqueda, recomendaciones
 * y gestión de metadatos de canciones.
 * </p>
 * <p>
 * Implementa:
 * <ul>
 *   <li>RF-010: CRUD de canciones</li>
 *   <li>RF-003: Búsqueda por título y género</li>
 *   <li>RF-004: Búsqueda avanzada con filtros</li>
 *   <li>RF-030: Búsqueda concurrente</li>
 *   <li>Autocompletado de títulos</li>
 *   <li>Canciones similares y radio</li>
 * </ul>
 * </p>
 *
 * @author SyncUp
 * @version 1.0
 */
@RestController
@RequestMapping("/api/canciones")
@CrossOrigin(origins = "http://localhost:3000")
public class CancionController {

    /**
     * Servicio de gestión de canciones inyectado.
     */
    private final CancionService cancionService;

    /**
     * Constructor que inyecta el servicio de canciones.
     *
     * @param cancionService el servicio de canciones
     */
    public CancionController(CancionService cancionService) {
        this.cancionService = cancionService;
    }

    /**
     * Lista todas las canciones del catálogo.
     *
     * @return colección de todas las canciones
     */
    @GetMapping
    public Collection<Cancion> listarCanciones() {
        return cancionService.listarCanciones();
    }

    /**
     * Obtiene una canción específica por su identificador.
     *
     * @param id el identificador de la canción
     * @return la canción si existe, {@code null} en caso contrario
     */
    @GetMapping("/{id}")
    public Cancion obtenerCancion(@PathVariable String id) {
        return cancionService.buscarPorId(id);
    }

    /**
     * Agrega una nueva canción al catálogo.
     * <p>
     * Valida que no exista otra canción con el mismo ID antes de agregar.
     * </p>
     *
     * @param nuevaCancion la canción a agregar
     * @return mensaje de confirmación o advertencia
     */
    @PostMapping
    public String agregarCancion(@RequestBody Cancion nuevaCancion) {
        if (cancionService.buscarPorId(nuevaCancion.getId()) != null) {
            return "⚠️ Ya existe una canción con ese ID.";
        }
        cancionService.agregarCancion(nuevaCancion);
        return "✅ Canción agregada correctamente.";
    }

    /**
     * Actualiza una canción existente.
     * <p>
     * Modifica todos los atributos de la canción identificada por el ID de la ruta.
     * </p>
     *
     * @param id el identificador de la canción a actualizar
     * @param cancionActualizada los nuevos datos de la canción
     * @return mensaje de confirmación o error
     */
    @PutMapping("/{id}")
    public String actualizarCancion(@PathVariable String id, @RequestBody Cancion cancionActualizada) {
        cancionActualizada.setId(id);
        boolean actualizada = cancionService.actualizarCancion(cancionActualizada);
        return actualizada
                ? "✅ Canción actualizada correctamente."
                : "❌ No se encontró la canción con ID " + id;
    }

    /**
     * Elimina una canción del catálogo.
     *
     * @param id el identificador de la canción a eliminar
     * @return mensaje de confirmación o error
     */
    @DeleteMapping("/{id}")
    public String eliminarCancion(@PathVariable String id) {
        boolean eliminada = cancionService.eliminarCancion(id);
        return eliminada
                ? "🗑️ Canción eliminada correctamente."
                : "❌ No se encontró la canción con ID " + id;
    }

    /**
     * Realiza una búsqueda simple de canciones por título y/o género.
     * <p>
     * Ambos parámetros son opcionales. Ejemplos:
     * <ul>
     *   <li>{@code GET /api/canciones/buscar?titulo=Imagine}</li>
     *   <li>{@code GET /api/canciones/buscar?genero=Rock}</li>
     *   <li>{@code GET /api/canciones/buscar?titulo=Love&genero=Pop}</li>
     * </ul>
     * </p>
     *
     * @param titulo el título o parte del título a buscar (opcional)
     * @param genero el género o parte del género a buscar (opcional)
     * @return lista de canciones que coinciden con los criterios
     */
    @GetMapping("/buscar")
    public List<Cancion> buscarCanciones(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String genero) {
        return cancionService.buscarPorFiltro(titulo, genero);
    }

    /**
     * Realiza una búsqueda avanzada y concurrente de canciones con múltiples criterios.
     * <p>
     * Implementa RF-004 y RF-030. Todos los parámetros son opcionales.
     * Ejemplo:
     * {@code GET /api/canciones/buscar/avanzado?titulo=love&artista=queen&genero=rock&anioFrom=1970&anioTo=1990&op=OR}
     * </p>
     *
     * @param titulo título o parte del título (opcional)
     * @param artista artista o parte del nombre (opcional)
     * @param genero género o parte del género (opcional)
     * @param anioFrom año inicial del rango (opcional)
     * @param anioTo año final del rango (opcional)
     * @param op operador de combinación: "AND" o "OR" (por defecto "AND")
     * @return lista de canciones que cumplen los criterios
     */
    @GetMapping("/buscar/avanzado")
    public List<Cancion> buscarAvanzado(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String artista,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) Integer anioFrom,
            @RequestParam(required = false) Integer anioTo,
            @RequestParam(required = false, defaultValue = "AND") String op
    ) {
        return cancionService.buscarAvanzada(titulo, artista, genero, anioFrom, anioTo, op);
    }

    /**
     * Carga un lote de canciones desde un archivo CSV/TXT.
     * <p>
     * El archivo debe contener líneas con 6 o 7 campos separados por punto y coma.
     * </p>
     *
     * @param archivo el archivo MultipartFile a procesar
     * @return mensaje con la cantidad de canciones cargadas o error
     */
    @PostMapping("/cargar")
    public String cargarCancionesMasivamente(@RequestParam("archivo") MultipartFile archivo) {
        try {
            int cantidad = cancionService.cargarCancionesMasivamente(archivo);
            return "✅ Se cargaron " + cantidad + " canciones correctamente.";
        } catch (Exception e) {
            return "❌ Error al cargar canciones: " + e.getMessage();
        }
    }

    /**
     * Genera sugerencias de autocompletado basadas en un prefijo de título.
     *
     * @param prefijo el prefijo para autocompletar
     * @return lista de títulos que comienzan con el prefijo
     */
    @GetMapping("/autocompletar")
    public List<String> autocompletar(@RequestParam String prefijo) {
        return cancionService.autocompletarTitulo(prefijo);
    }

    /**
     * Obtiene las canciones más similares a una canción específica.
     * <p>
     * La similitud se calcula basándose en género, artista y año.
     * </p>
     *
     * @param id el identificador de la canción
     * @param limite la cantidad máxima de canciones similares a retornar (por defecto 5)
     * @return respuesta con la lista de canciones similares
     */
    @GetMapping("/{id}/similares")
    public ResponseEntity<List<Cancion>> obtenerSimilares(@PathVariable String id,
                                                          @RequestParam(defaultValue = "5") int limite) {
        List<Cancion> similares = cancionService.obtenerCancionesSimilares(id, limite);
        return ResponseEntity.ok(similares);
    }

    /**
     * Inicia una radio personalizada a partir de una canción.
     * <p>
     * Genera una cola de reproducción con canciones similares.
     * </p>
     *
     * @param id el identificador de la canción semilla
     * @param limite la cantidad máxima de canciones para la cola (por defecto 10)
     * @return respuesta con la lista de canciones para la radio
     */
    @GetMapping("/{id}/radio")
    public ResponseEntity<List<Cancion>> iniciarRadio(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limite) {

        List<Cancion> cola = cancionService.iniciarRadio(id, limite);
        return ResponseEntity.ok(cola);
    }

    /**
     * Actualiza solamente el nombre del archivo MP3 asociado a una canción.
     * <p>
     * Útil para enlazar la canción con el archivo de audio ubicado en
     * {@code public/music} del frontend.
     * </p>
     *
     * @param id el identificador de la canción
     * @param fileName el nombre del archivo MP3 (ej: "song1.mp3")
     * @return respuesta de confirmación o error
     */
    @PutMapping("/{id}/file")
    public ResponseEntity<?> actualizarFileName(@PathVariable String id, @RequestParam String fileName) {
        Cancion c = cancionService.buscarPorId(id);
        if (c == null) return ResponseEntity.notFound().build();
        c.setFileName(fileName);
        boolean ok = cancionService.actualizarCancion(c);
        return ok ? ResponseEntity.ok("✅ fileName actualizado") :
                ResponseEntity.status(500).body("❌ No se pudo actualizar fileName");
    }


}
