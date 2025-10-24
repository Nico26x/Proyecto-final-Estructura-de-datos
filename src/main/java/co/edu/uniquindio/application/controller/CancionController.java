package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.repository.CancionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/canciones")
@CrossOrigin(origins = "http://localhost:3000") // permite peticiones desde tu frontend React
public class CancionController {

    @Autowired
    private CancionRepository cancionRepository;

    /**
     * 🔹 Obtener todas las canciones
     * Ejemplo: GET http://localhost:8080/api/canciones
     */
    @GetMapping
    public List<Cancion> listarCanciones() {
        return cancionRepository.listarCanciones();
    }

    /**
     * 🔹 Buscar una canción por ID
     * Ejemplo: GET http://localhost:8080/api/canciones/1
     */
    @GetMapping("/{id}")
    public Cancion obtenerCancion(@PathVariable String id) {
        return cancionRepository.buscarPorId(id);
    }

    /**
     * 🔹 Agregar una nueva canción
     * Ejemplo:
     * POST http://localhost:8080/api/canciones
     * Body (JSON):
     * {
     *   "id": "3",
     *   "titulo": "Yesterday",
     *   "artista": "The Beatles",
     *   "genero": "Pop",
     *   "anio": 1965,
     *   "duracion": 2.5
     * }
     */
    @PostMapping
    public String agregarCancion(@RequestBody Cancion nuevaCancion) {
        boolean agregada = cancionRepository.agregarCancion(nuevaCancion);
        if (agregada) {
            return "✅ Canción agregada correctamente.";
        } else {
            return "⚠️ Ya existe una canción con ese ID.";
        }
    }

    /**
     * 🔹 Eliminar una canción por ID
     * Ejemplo: DELETE http://localhost:8080/api/canciones/2
     */
    @DeleteMapping("/{id}")
    public String eliminarCancion(@PathVariable String id) {
        boolean eliminada = cancionRepository.eliminarCancion(id);
        if (eliminada) {
            return "🗑️ Canción eliminada correctamente.";
        } else {
            return "❌ No se encontró la canción con ID " + id;
        }
    }
}
