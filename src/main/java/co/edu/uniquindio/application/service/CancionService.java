package co.edu.uniquindio.application.service;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.repository.CancionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

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

    // 🎵 Nuevo: búsqueda por título/género
    public List<Cancion> buscarPorFiltro(String titulo, String genero) {
        return cancionRepository.buscarPorFiltro(titulo, genero);
    }
}
