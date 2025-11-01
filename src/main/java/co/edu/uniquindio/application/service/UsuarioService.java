package co.edu.uniquindio.application.service;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.repository.CancionRepository;
import co.edu.uniquindio.application.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CancionRepository cancionRepository;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, CancionRepository cancionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cancionRepository = cancionRepository;
    }

    // ✅ Registrar nuevo usuario
    public boolean registrarUsuario(String username, String password, String nombre) {
        if (usuarioRepository.buscarPorUsername(username) != null) {
            return false;
        }
        Usuario usuario = new Usuario(username, password, nombre);
        usuarioRepository.guardarUsuario(usuario);
        return true;
    }

    // ✅ Iniciar sesión
    public Usuario login(String username, String password) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario != null && usuario.getPassword().equals(password)) {
            return usuario;
        }
        return null;
    }

    // ✅ Listar todos los usuarios
    public Collection<Usuario> listarUsuarios() {
        return usuarioRepository.listarUsuarios().values();
    }

    // ✅ Agregar canción a favoritos
    public String agregarFavorito(String username, String idCancion) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        Cancion cancion = cancionRepository.buscarPorId(idCancion);

        if (usuario == null) return "❌ Usuario no encontrado";
        if (cancion == null) return "❌ Canción no encontrada";

        boolean agregado = usuarioRepository.agregarFavorito(username, cancion);
        return agregado ? "✅ Canción agregada a favoritos" : "⚠️ Ya estaba en favoritos";
    }

    // ✅ Eliminar canción de favoritos
    public String eliminarFavorito(String username, String idCancion) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return "❌ Usuario no encontrado";

        boolean eliminado = usuarioRepository.eliminarFavorito(username, idCancion);
        return eliminado ? "🗑️ Canción eliminada de favoritos" : "⚠️ No estaba en favoritos";
    }

    // ✅ Listar canciones favoritas
    public Collection<Cancion> listarFavoritos(String username) {
        return usuarioRepository.listarFavoritos(username);
    }
}
