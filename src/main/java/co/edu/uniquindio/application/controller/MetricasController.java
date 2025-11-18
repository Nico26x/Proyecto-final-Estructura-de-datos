package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.security.JwtUtil;
import co.edu.uniquindio.application.service.MetricasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controlador REST para la gestión y consulta de métricas del sistema.
 * <p>
 * Proporciona endpoints para analizar datos de exportación, favoritos y actividad de usuarios.
 * Todos los endpoints requieren autenticación mediante JWT token.
 * </p>
 * <p>
 * Métricas disponibles:
 * </p>
 * <ul>
 *   <li>Descargas de favoritos por día</li>
 *   <li>Top usuarios exportadores</li>
 *   <li>Top artistas por favoritos</li>
 *   <li>Top géneros por favoritos</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@RestController
@RequestMapping("/api/metricas")
@CrossOrigin(origins = "*")
public class MetricasController {

    /**
     * Servicio de métricas inyectado.
     */
    private final MetricasService metricasService;

    /**
     * Utilidad JWT para validación de tokens.
     */
    private final JwtUtil jwtUtil;

    /**
     * Constructor que inyecta las dependencias de servicio y seguridad.
     *
     * @param metricasService el servicio de métricas
     * @param jwtUtil la utilidad JWT para validación de tokens
     */
    @Autowired
    public MetricasController(MetricasService metricasService, JwtUtil jwtUtil) {
        this.metricasService = metricasService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Obtiene el conteo de descargas de favoritos agrupadas por día.
     * <p>
     * Requiere autenticación mediante JWT token en el header Authorization.
     * Retorna un mapa con fechas como clave y cantidad de descargas como valor.
     * </p>
     *
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con el mapa de descargas por día, o error 401/403 si falta o es inválido el token
     */
    @GetMapping("/descargas-favoritos/dia")
    public ResponseEntity<?> descargasFavoritosPorDia(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("🚫 Token no proporcionado.");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Token inválido o expirado.");
        }

        Map<String, Long> data = metricasService.descargasFavoritosPorDia();
        return ResponseEntity.ok(data);
    }

    /**
     * Obtiene el ranking de los usuarios que más han exportado favoritos.
     * <p>
     * Requiere autenticación mediante JWT token. Retorna una lista de objetos con
     * username y cantidad total de exportaciones.
     * </p>
     *
     * @param limit la cantidad máxima de usuarios a retornar (por defecto 5)
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con la lista de top usuarios exportadores, o error 401/403 si falta o es inválido el token
     */
    @GetMapping("/usuarios/top-exportadores")
    public ResponseEntity<?> topUsuariosExportadores(
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("🚫 Token no proporcionado.");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Token inválido o expirado.");
        }

        var top = metricasService.topUsuariosExportadores(limit);
        // Devuelve como lista de objetos {username, total}
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Map.Entry<String, Long> e : top) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", e.getKey());
            row.put("total", e.getValue());
            payload.add(row);
        }
        return ResponseEntity.ok(payload);
    }

    /**
     * Obtiene el ranking de los artistas más favoritos de todos los usuarios.
     * <p>
     * Requiere autenticación mediante JWT token. Analiza los archivos CSV de favoritos
     * para contar apariciones de artistas.
     * </p>
     *
     * @param limit la cantidad máxima de artistas a retornar (por defecto 10)
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con el ranking de artistas, o error 401/403 si falta o es inválido el token
     */
    @GetMapping("/favoritos/top-artistas")
    public ResponseEntity<?> topArtistas(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("🚫 Token no proporcionado.");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Token inválido o expirado.");
        }

        var data = metricasService.topArtistasDesdeFavoritos(limit);
        return ResponseEntity.ok(data);
    }

    /**
     * Obtiene el ranking de los géneros musicales más favoritos de todos los usuarios.
     * <p>
     * Requiere autenticación mediante JWT token. Analiza los archivos CSV de favoritos
     * para contar apariciones de géneros.
     * </p>
     *
     * @param limit la cantidad máxima de géneros a retornar (por defecto 10)
     * @param authHeader el header de autorización con el JWT token
     * @return respuesta con el ranking de géneros, o error 401/403 si falta o es inválido el token
     */
    @GetMapping("/favoritos/top-generos")
    public ResponseEntity<?> topGeneros(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("🚫 Token no proporcionado.");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validarToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Token inválido o expirado.");
        }

        var data = metricasService.topGenerosDesdeFavoritos(limit);
        return ResponseEntity.ok(data);
    }
}
