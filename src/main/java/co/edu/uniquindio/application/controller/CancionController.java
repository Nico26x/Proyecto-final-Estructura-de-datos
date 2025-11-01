package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.service.CancionService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * Controlador REST para la gestión del catálogo de canciones.
 * Cumple RF-010 (CRUD) y RF-003 (búsqueda por título/género).
 */
@RestController
@RequestMapping("/api/canciones")
@CrossOrigin(origins = "http://localhost:3000") // Permite peticiones desde React
public class CancionController {

    private final CancionService cancionService;

    public CancionController(CancionService cancionService) {
        this.cancionService = cancionService;
    }

    // 📋 Obtener todas las canciones
    @GetMapping
    public Collection<Cancion> listarCanciones() {
        return cancionService.listarCanciones();
    }

    // 🔎 Buscar una canción por ID
    @GetMapping("/{id}")
    public Cancion obtenerCancion(@PathVariable String id) {
        return cancionService.buscarPorId(id);
    }

    // ➕ Agregar una nueva canción (con validación)
    @PostMapping
    public String agregarCancion(@RequestBody Cancion nuevaCancion) {
        if (cancionService.buscarPorId(nuevaCancion.getId()) != null) {
            return "⚠️ Ya existe una canción con ese ID.";
        }
        cancionService.agregarCancion(nuevaCancion);
        return "✅ Canción agregada correctamente.";
    }

    // ✏️ Actualizar una canción existente
    @PutMapping("/{id}")
    public String actualizarCancion(@PathVariable String id, @RequestBody Cancion cancionActualizada) {
        cancionActualizada.setId(id);
        boolean actualizada = cancionService.actualizarCancion(cancionActualizada);
        return actualizada
                ? "✅ Canción actualizada correctamente."
                : "❌ No se encontró la canción con ID " + id;
    }

    // 🗑️ Eliminar una canción por ID
    @DeleteMapping("/{id}")
    public String eliminarCancion(@PathVariable String id) {
        boolean eliminada = cancionService.eliminarCancion(id);
        return eliminada
                ? "🗑️ Canción eliminada correctamente."
                : "❌ No se encontró la canción con ID " + id;
    }

    // 🎵 Buscar canciones por título o género
    // Ejemplos:
    //   /api/canciones/buscar?titulo=Imagine
    //   /api/canciones/buscar?genero=Rock
    //   /api/canciones/buscar?titulo=Love&genero=Pop
    @GetMapping("/buscar")
    public List<Cancion> buscarCanciones(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String genero) {
        return cancionService.buscarPorFiltro(titulo, genero);
    }
}
