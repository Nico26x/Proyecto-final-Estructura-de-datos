package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Controlador REST para la gestión de usuarios y sus favoritos.
 * Usa UsuarioService para la lógica de negocio.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ✅ Inyección de dependencias por constructor
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 📌 Registrar usuario
    @PostMapping("/registrar")
    public String registrar(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String nombre) {
        boolean registrado = usuarioService.registrarUsuario(username, password, nombre);
        if (registrado) {
            return "✅ Usuario registrado correctamente";
        } else {
            return "⚠️ El usuario ya existe";
        }
    }

    // 📌 Iniciar sesión
    @PostMapping("/login")
    public Usuario login(@RequestParam String username,
                         @RequestParam String password) {
        Usuario usuario = usuarioService.login(username, password);
        if (usuario != null) {
            return usuario;
        }
        throw new RuntimeException("❌ Credenciales incorrectas");
    }

    // 📌 Listar usuarios
    @GetMapping("/listar")
    public Collection<Usuario> listar() {
        return usuarioService.listarUsuarios();
    }

    // 🎵 FAVORITOS — Agregar canción
    @PostMapping("/{username}/favoritos/agregar")
    public String agregarFavorito(@PathVariable String username,
                                  @RequestParam String idCancion) {
        return usuarioService.agregarFavorito(username, idCancion);
    }

    // 🎵 FAVORITOS — Eliminar canción
    @DeleteMapping("/{username}/favoritos/eliminar")
    public String eliminarFavorito(@PathVariable String username,
                                   @RequestParam String idCancion) {
        return usuarioService.eliminarFavorito(username, idCancion);
    }

    // 🎵 FAVORITOS — Listar canciones favoritas
    @GetMapping("/{username}/favoritos")
    public Collection<Cancion> listarFavoritos(@PathVariable String username) {
        return usuarioService.listarFavoritos(username);
    }
}
