package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.security.JwtUtil;
import co.edu.uniquindio.application.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de usuarios y sus favoritos.
 * Usa UsuarioService para la lógica de negocio.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    // ✅ Inyección de dependencias
    @Autowired
    public UsuarioController(UsuarioService usuarioService, JwtUtil jwtUtil) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
    }

    // 📌 Registrar usuario (ahora con contraseña encriptada)
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

    // 📌 Iniciar sesión (genera y devuelve el JWT)
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam String username,
                                                     @RequestParam String password) {
        Map<String, Object> respuesta = new HashMap<>();
        Usuario usuario = usuarioService.autenticarUsuario(username, password);

        if (usuario != null) {
            String token = jwtUtil.generarToken(username, usuario.getRol().name());
            usuarioService.iniciarSesion(usuario);

            respuesta.put("mensaje", "✅ Inicio de sesión exitoso");
            respuesta.put("token", token);
            respuesta.put("usuario", usuario);

            // Devuelve 200 OK con JSON
            return ResponseEntity.ok(respuesta);
        } else {
            respuesta.put("error", "❌ Credenciales incorrectas");

            // Devuelve 401 Unauthorized con JSON
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
        }
    }

    // 📌 Cerrar sesión
    @PostMapping("/logout")
    public String cerrarSesion() {
        usuarioService.logout();
        return "👋 Sesión cerrada correctamente.";
    }

    // 📌 Obtener sesión actual
    @GetMapping("/sesion")
    public ResponseEntity<?> obtenerSesion(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "🚫 Token no proporcionado"));
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.obtenerUsername(token);

        Usuario usuario = usuarioService.buscarPorUsername(username);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "❌ Usuario no encontrado"));
        }

        return ResponseEntity.ok(usuario);
    }


    // 📌 Listar usuarios
    @GetMapping("/listar")
    public ResponseEntity<?> listar(@RequestHeader("Authorization") String token) {
        if (!jwtUtil.validarToken(token.replace("Bearer ", ""))) {
            return ResponseEntity.status(401).body("❌ Token inválido");
        }
        String rol = jwtUtil.obtenerRol(token.replace("Bearer ", ""));
        if (!"ADMIN".equals(rol)) {
            return ResponseEntity.status(403).body("🚫 Acceso denegado: solo administradores");
        }
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }


    // 🎵 FAVORITOS — Agregar canción
    @PostMapping("/{username}/favoritos/agregar")
    public String agregarFavorito(@PathVariable String username,
                                  @RequestParam String idCancion,
                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "🚫 Debes enviar un token JWT válido.";
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return "🚫 Token inválido o expirado.";
        }

        return usuarioService.agregarFavorito(username, idCancion);
    }

    // 🎵 FAVORITOS — Eliminar canción
    @DeleteMapping("/{username}/favoritos/eliminar")
    public String eliminarFavorito(@PathVariable String username,
                                   @RequestParam String idCancion,
                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "🚫 Debes enviar un token JWT válido.";
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return "🚫 Token inválido o expirado.";
        }

        return usuarioService.eliminarFavorito(username, idCancion);
    }

    // 🎵 FAVORITOS — Listar canciones favoritas
    @GetMapping("/{username}/favoritos")
    public Collection<Cancion> listarFavoritos(@PathVariable String username,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("🚫 Debes enviar un token JWT válido.");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            throw new RuntimeException("🚫 Token inválido o expirado.");
        }

        return usuarioService.listarFavoritos(username);
    }

    // ✏️ Actualizar nombre del usuario
    @PutMapping("/{username}/actualizar-nombre")
    public String actualizarNombre(@PathVariable String username,
                                   @RequestParam String nuevoNombre) {
        return usuarioService.actualizarNombre(username, nuevoNombre);
    }

    // 🔒 Cambiar contraseña del usuario
    @PutMapping("/{username}/cambiar-password")
    public String cambiarPassword(@PathVariable String username,
                                  @RequestParam String nuevaPassword) {
        return usuarioService.cambiarPassword(username, nuevaPassword);
    }

    // 🎧 Generar playlist "Descubrimiento Semanal"
    @GetMapping("/{username}/descubrimiento")
    public ResponseEntity<?> generarDescubrimientoSemanal(
            @PathVariable String username,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader) {

        // Validar token JWT
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("🚫 Token no proporcionado.");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return ResponseEntity.status(403).body("❌ Token inválido o expirado.");
        }

        List<Cancion> playlist = usuarioService.generarPlaylistDescubrimiento(username, size);

        if (playlist.isEmpty()) {
            return ResponseEntity.ok("⚠️ No se encontraron recomendaciones para el usuario.");
        }

        return ResponseEntity.ok(playlist);
    }

}
