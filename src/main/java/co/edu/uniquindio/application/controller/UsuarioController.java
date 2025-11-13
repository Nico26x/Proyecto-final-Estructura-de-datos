package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.api.ApiResponse;
import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.security.JwtUtil;
import co.edu.uniquindio.application.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

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
    public ResponseEntity<String> registrar(@RequestParam String username,
                                            @RequestParam String password,
                                            @RequestParam String nombre) {
        boolean registrado = usuarioService.registrarUsuario(username, password, nombre);
        if (registrado) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("✅ Usuario registrado correctamente");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("⚠️ El usuario ya existe");
        }
    }

    // ===== NUEVO: versión “envelope” del registro (opcional, no rompe lo anterior)
    @PostMapping("/auth/registrar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrarStd(@RequestParam String username,
                                                                         @RequestParam String password,
                                                                         @RequestParam String nombre) {
        boolean registrado = usuarioService.registrarUsuario(username, password, nombre);
        Map<String, Object> payload = new HashMap<>();
        if (registrado) {
            payload.put("mensaje", "✅ Usuario registrado correctamente");
            payload.put("username", username);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("⚠️ El usuario ya existe"));
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

    // ===== NUEVO: versión “envelope” del login (opcional)
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginStd(@RequestParam String username,
                                                                     @RequestParam String password) {
        Usuario usuario = usuarioService.autenticarUsuario(username, password);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("❌ Credenciales incorrectas"));
        }

        String token = jwtUtil.generarToken(username, usuario.getRol().name());
        usuarioService.iniciarSesion(usuario);

        Map<String, Object> data = new HashMap<>();
        data.put("mensaje", "✅ Inicio de sesión exitoso");
        data.put("token", token);
        data.put("usuario", usuario); // ya viene sin password por @JsonIgnore

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
    // ===== fin nuevo

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

    // Endpoint para seguir a un usuario
    @PostMapping("/seguir")
    public ResponseEntity<Map<String, String>> seguirUsuario(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String destino = body.get("destino");

        // Llamamos al servicio para seguir al usuario
        String resultado = usuarioService.seguirUsuario(username, destino);

        // Preparamos la respuesta en formato JSON
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", resultado);

        // Si la respuesta empieza con "✅", significa que fue exitosa
        if (resultado.startsWith("✅")) {
            return ResponseEntity.ok(response);  // Si fue exitoso, retornamos OK
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);  // Si hubo un error, retornamos BAD_REQUEST con el mensaje
        }
    }




    // 🚫 Dejar de seguir
    @PostMapping("/dejar-seguir")
    public ResponseEntity<Map<String, String>> dejarDeSeguir(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String destino = body.get("destino");

        // Llamamos al servicio para dejar de seguir al usuario
        String resultado = usuarioService.dejarDeSeguir(username, destino);

        // Preparamos la respuesta en formato JSON
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", resultado);

        // Si la respuesta empieza con "✅", significa que fue exitoso
        if (resultado.startsWith("✅")) {
            return ResponseEntity.ok(response);  // Si fue exitoso, retornamos OK
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);  // Si hubo un error, retornamos BAD_REQUEST con el mensaje
        }
    }


    // 📜 Listar seguidos
    @GetMapping("/{username}/seguidos")
    public ResponseEntity<Set<String>> listarSeguidos(
            @PathVariable String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Validación del token JWT (asegurarse de que el usuario está autenticado)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        // Listar seguidos
        Set<String> seguidos = usuarioService.listarSeguidos(username);
        return ResponseEntity.ok(seguidos);
    }


    // Endpoint para sugerir usuarios basados en canciones favoritas
    @PostMapping("/{username}/sugerir-usuarios")
    public ResponseEntity<List<String>> sugerirUsuariosPorFavoritos(
            @PathVariable String username,
            @RequestParam(defaultValue = "5") int limite) {

        List<String> sugerencias = usuarioService.sugerirUsuariosPorFavoritos(username, limite);

        if (sugerencias.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(sugerencias); // Si no hay sugerencias, retornamos vacío
        }

        return ResponseEntity.ok(sugerencias); // Devolvemos la lista de usuarios sugeridos
    }


    // =========================
    // RF-009 — Exportar y GUARDAR CSV (depende del usuario logueado)
    // =========================
    @GetMapping("/{username}/favoritos/export")
    public ResponseEntity<byte[]> exportarFavoritosCsv(
            @PathVariable String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Validación de token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Sólo el dueño o ADMIN
        String userFromToken = jwtUtil.obtenerUsername(token);
        String rol = jwtUtil.obtenerRol(token);
        boolean esAdmin = "ADMIN".equalsIgnoreCase(rol);
        if (!esAdmin && !userFromToken.equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Exportar y guardar (un archivo por usuario, se sobreescribe)
        UsuarioService.ExportResultado res = usuarioService.exportarYGuardarFavoritosCsv(username);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.downloadName + "\"")
                .header("X-Saved-At", res.savedAbsolutePath) // útil para depurar/ver dónde se guardó
                .contentType(MediaType.valueOf("text/csv"))
                .body(res.csv);
    }

    // (Endpoint alterno que exporta por usuario actual, si quisieras mantenerlo)
    @GetMapping("/favoritos/export")
    public ResponseEntity<byte[]> exportarFavoritosCsvUsuarioActual() {
        String username = usuarioService.obtenerUsernameActual();
        byte[] csv = usuarioService.exportarFavoritosCsvUsuarioActual();
        String filename = usuarioService.buildFavoritosFilename(username);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.valueOf("text/csv"))
                .body(csv);
    }

    // Dentro de UsuarioController (ajusta imports si hace falta)
    @DeleteMapping("/eliminar")
    public ResponseEntity<?> eliminarUsuarioAdmin(
            @RequestParam String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 1) Validar header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("🚫 Token no proporcionado.");
        }

        String token = authHeader.substring(7);

        // 2) Validar token
        if (!jwtUtil.validarToken(token)) {
            return ResponseEntity.status(403).body("❌ Token inválido o expirado.");
        }

        // 3) Validar rol ADMIN (si tu JwtUtil guarda 'ADMIN')
        String rol = jwtUtil.obtenerRol(token);
        if (!"ADMIN".equalsIgnoreCase(rol) && !"ROLE_ADMIN".equalsIgnoreCase(rol)) {
            return ResponseEntity.status(403).body("🚫 Acceso denegado: solo administradores.");
        }

        // 4) Eliminar usuario via service (retorna boolean)
        boolean ok = usuarioService.eliminarUsuarioAdmin(username);
        if (ok) {
            return ResponseEntity.ok("✅ Usuario '" + username + "' eliminado correctamente.");
        } else {
            return ResponseEntity.status(404).body("❌ Usuario '" + username + "' no existe.");
        }
    }

}
