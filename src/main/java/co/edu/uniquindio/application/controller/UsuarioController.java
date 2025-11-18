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
 * Controlador REST para la gestión integral de usuarios y sus funcionalidades.
 * <p>
 * Proporciona endpoints para autenticación, gestión de perfiles, favoritos,
 * redes sociales y exportación de datos.
 * </p>
 * <p>
 * Implementa:
 * </p>
 * <ul>
 *   <li>RF-001: Registro de usuarios con contraseña encriptada</li>
 *   <li>RF-002: Autenticación con JWT</li>
 *   <li>RF-005: Playlist de descubrimiento</li>
 *   <li>RF-008: Gestión de amigos (seguir/dejar de seguir)</li>
 *   <li>RF-009: Exportación de favoritos a CSV</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    /**
     * Servicio de gestión de usuarios inyectado.
     */
    private final UsuarioService usuarioService;

    /**
     * Utilidad JWT para generación y validación de tokens.
     */
    private final JwtUtil jwtUtil;

    /**
     * Constructor que inyecta las dependencias del servicio y seguridad.
     *
     * @param usuarioService el servicio de usuarios
     * @param jwtUtil la utilidad JWT para manejo de tokens
     */
    @Autowired
    public UsuarioController(UsuarioService usuarioService, JwtUtil jwtUtil) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registra un nuevo usuario en el sistema con contraseña encriptada.
     * <p>
     * Implementa RF-001. Valida que no exista un usuario con el mismo username.
     * </p>
     *
     * @param username el nombre de usuario (debe ser único)
     * @param password la contraseña (será encriptada con BCrypt)
     * @param nombre el nombre completo del usuario
     * @return respuesta 201 CREATED si el registro es exitoso, 409 CONFLICT si ya existe
     */
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

    /**
     * Registra un nuevo usuario usando formato de respuesta estándar (envelope).
     * <p>
     * Alternativa a {@link #registrar(String, String, String)} que devuelve
     * un objeto {@code ApiResponse} con estructura uniforme.
     * </p>
     *
     * @param username el nombre de usuario (debe ser único)
     * @param password la contraseña (será encriptada con BCrypt)
     * @param nombre el nombre completo del usuario
     * @return respuesta envuelta con los datos del usuario registrado o mensaje de error
     */
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

    /**
     * Autentica un usuario y genera un JWT token.
     * <p>
     * Implementa RF-002. Valida credenciales y devuelve un JWT válido por 24 horas.
     * </p>
     *
     * @param username el nombre de usuario
     * @param password la contraseña (será comparada con hash BCrypt)
     * @return respuesta 200 OK con token y datos del usuario si es válido, 401 si no
     */
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

    /**
     * Autentica un usuario usando formato de respuesta estándar (envelope).
     * <p>
     * Alternativa a {@link #login(String, String)} que devuelve un objeto
     * {@code ApiResponse} con estructura uniforme.
     * </p>
     *
     * @param username el nombre de usuario
     * @param password la contraseña
     * @return respuesta envuelta con token y datos del usuario, o mensaje de error
     */
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

    /**
     * Cierra la sesión del usuario actual.
     *
     * @return mensaje de confirmación
     */
    @PostMapping("/logout")
    public String cerrarSesion() {
        usuarioService.logout();
        return "👋 Sesión cerrada correctamente.";
    }

    /**
     * Obtiene los datos del usuario actual desde el JWT token.
     * <p>
     * Requiere autenticación mediante JWT en el header Authorization.
     * </p>
     *
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con los datos del usuario, o error 401/404 si falta token o usuario no existe
     */
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

    /**
     * Lista todos los usuarios del sistema (requiere rol ADMIN).
     *
     * @param token el JWT token del usuario autenticado
     * @return respuesta con la lista de usuarios, o error 401/403 si token inválido o no es ADMIN
     */
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

    /**
     * Agrega una canción a la lista de favoritos de un usuario.
     * <p>
     * Requiere autenticación mediante JWT token.
     * </p>
     *
     * @param username el nombre del usuario
     * @param idCancion el identificador de la canción a agregar
     * @param authHeader el header de autorización con el JWT token
     * @return mensaje de confirmación o error
     */
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

    /**
     * Elimina una canción de la lista de favoritos de un usuario.
     * <p>
     * Requiere autenticación mediante JWT token.
     * </p>
     *
     * @param username el nombre del usuario
     * @param idCancion el identificador de la canción a eliminar
     * @param authHeader el header de autorización con el JWT token
     * @return mensaje de confirmación o error
     */
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

    /**
     * Lista todas las canciones favoritas de un usuario.
     * <p>
     * Requiere autenticación mediante JWT token.
     * </p>
     *
     * @param username el nombre del usuario
     * @param authHeader el header de autorización con el JWT token
     * @return colección de canciones favoritas
     * @throws RuntimeException si falta o es inválido el token
     */
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

    /**
     * Actualiza el nombre completo del usuario.
     *
     * @param username el nombre del usuario a actualizar
     * @param nuevoNombre el nuevo nombre completo
     * @return mensaje de confirmación o error
     */
    @PutMapping("/{username}/actualizar-nombre")
    public String actualizarNombre(@PathVariable String username,
                                   @RequestParam String nuevoNombre) {
        return usuarioService.actualizarNombre(username, nuevoNombre);
    }

    /**
     * Cambia la contraseña de un usuario.
     * <p>
     * La nueva contraseña será encriptada con BCrypt antes de guardarse.
     * </p>
     *
     * @param username el nombre del usuario
     * @param nuevaPassword la nueva contraseña
     * @return mensaje de confirmación o error
     */
    @PutMapping("/{username}/cambiar-password")
    public String cambiarPassword(@PathVariable String username,
                                  @RequestParam String nuevaPassword) {
        return usuarioService.cambiarPassword(username, nuevaPassword);
    }

    /**
     * Genera una playlist de descubrimiento para un usuario.
     * <p>
     * Implementa RF-005. Utiliza el grafo de similitud de canciones y favoritos
     * del usuario para recomendar nuevas canciones.
     * </p>
     *
     * @param username el nombre del usuario
     * @param size la cantidad máxima de canciones a retornar (por defecto 10)
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con la playlist de descubrimiento, o error 401/403 si token inválido
     */
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

    /**
     * Permite que un usuario siga a otro usuario.
     * <p>
     * Implementa RF-008. Crea una conexión bidireccional en el grafo social.
     * </p>
     *
     * @param body mapa con campos "username" (origen) y "destino" (usuario a seguir)
     * @return respuesta con mensaje de confirmación o error
     */
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




    /**
     * Permite que un usuario deje de seguir a otro usuario.
     * <p>
     * Implementa RF-008. Elimina la conexión bidireccional en el grafo social.
     * </p>
     *
     * @param body mapa con campos "username" (origen) y "destino" (usuario a dejar de seguir)
     * @return respuesta con mensaje de confirmación o error
     */
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


    /**
     * Lista todos los usuarios que un usuario específico está siguiendo.
     * <p>
     * Requiere autenticación mediante JWT token.
     * </p>
     *
     * @param username el nombre del usuario
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con el conjunto de usuarios seguidos, o error 401/403 si token inválido
     */
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


    /**
     * Sugiere usuarios basándose en canciones favoritas comunes.
     * <p>
     * Implementa RF-008. Busca usuarios con intersecciones en favoritos.
     * </p>
     *
     * @param username el nombre del usuario
     * @param limite la cantidad máxima de sugerencias a retornar (por defecto 5)
     * @return respuesta con la lista de usuarios sugeridos, o 204 si no hay sugerencias
     */
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


    /**
     * Exporta los favoritos de un usuario en formato CSV.
     * <p>
     * Implementa RF-009. Requiere autenticación. Solo el dueño de la cuenta
     * o un ADMIN pueden descargar los favoritos.
     * </p>
     *
     * @param username el nombre del usuario cuyos favoritos se van a exportar
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con el archivo CSV o error 401/403 si no autorizado
     */
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

    /**
     * Exporta los favoritos del usuario actual en formato CSV (versión alternativa).
     * <p>
     * Utiliza el username del usuario autenticado en la sesión actual.
     * </p>
     *
     * @return respuesta con el archivo CSV descargable
     */
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

    /**
     * Elimina un usuario del sistema (requiere rol ADMIN).
     * <p>
     * Solo administradores pueden eliminar usuarios. El usuario a eliminar debe
     * ser especificado como parámetro de query.
     * </p>
     *
     * @param username el nombre del usuario a eliminar
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con mensaje de confirmación o error (401/403/404)
     */
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
