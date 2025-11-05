package co.edu.uniquindio.application.service;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.repository.CancionRepository;
import co.edu.uniquindio.application.repository.UsuarioRepository;
import co.edu.uniquindio.application.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CancionRepository cancionRepository;
    private final PasswordEncoder passwordEncoder;

    // 🔹 Usuario en sesión (almacenado temporalmente)
    private Usuario usuarioLogueado;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          CancionRepository cancionRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cancionRepository = cancionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ Registrar nuevo usuario con contraseña cifrada
    public boolean registrarUsuario(String username, String password, String nombre) {
        if (usuarioRepository.buscarPorUsername(username) != null) {
            return false;
        }
        String passwordEncriptada = passwordEncoder.encode(password);
        Usuario usuario = new Usuario(username, passwordEncriptada, nombre);
        usuarioRepository.guardarUsuario(usuario);
        return true;
    }

    // ✅ Login que devuelve un JWT (para compatibilidad)
    public String login(String username, String password) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
            return jwtUtil.generarToken(username);
        }
        return null;
    }

    // ✅ NUEVO: Autenticar usuario (devuelve el objeto Usuario si las credenciales son correctas)
    public Usuario autenticarUsuario(String username, String password) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
            return usuario;
        }
        return null;
    }

    // ✅ Iniciar sesión manualmente desde el controlador
    public void iniciarSesion(Usuario usuario) {
        this.usuarioLogueado = usuario;
    }

    // ✅ Cerrar sesión
    public void logout() {
        usuarioLogueado = null;
    }

    // ✅ Obtener usuario actual
    public Usuario obtenerUsuarioActual() {
        return usuarioLogueado;
    }

    // ✅ Listar todos los usuarios
    public Collection<Usuario> listarUsuarios() {
        return usuarioRepository.listarUsuarios().values();
    }

    // ✅ Favoritos
    public String agregarFavorito(String username, String idCancion) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        Cancion cancion = cancionRepository.buscarPorId(idCancion);

        if (usuario == null) return "❌ Usuario no encontrado";
        if (cancion == null) return "❌ Canción no encontrada";

        boolean agregado = usuarioRepository.agregarFavorito(username, cancion);
        return agregado ? "✅ Canción agregada a favoritos" : "⚠️ Ya estaba en favoritos";
    }

    public String eliminarFavorito(String username, String idCancion) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return "❌ Usuario no encontrado";

        boolean eliminado = usuarioRepository.eliminarFavorito(username, idCancion);
        return eliminado ? "🗑️ Canción eliminada de favoritos" : "⚠️ No estaba en favoritos";
    }

    public Collection<Cancion> listarFavoritos(String username) {
        return usuarioRepository.listarFavoritos(username);
    }

    // ✅ Verificar si hay sesión activa
    public boolean haySesionActiva() {
        return usuarioLogueado != null;
    }

    // ✏️ NUEVO: Actualizar nombre del usuario
    public String actualizarNombre(String username, String nuevoNombre) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) {
            return "❌ Usuario no encontrado";
        }

        usuario.setNombre(nuevoNombre);
        usuarioRepository.guardarUsuario(usuario);
        return "✅ Nombre actualizado correctamente";
    }

    // 🔐 NUEVO: Cambiar contraseña (con encriptación)
    public String cambiarPassword(String username, String nuevaPassword) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) {
            return "❌ Usuario no encontrado";
        }

        String passwordEncriptada = passwordEncoder.encode(nuevaPassword);
        usuario.setPassword(passwordEncriptada);
        usuarioRepository.guardarUsuario(usuario);
        return "🔑 Contraseña actualizada correctamente";
    }
}
