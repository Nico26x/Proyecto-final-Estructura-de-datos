package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.security.JwtUtil;
import co.edu.uniquindio.application.service.MetricasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/metricas")
@CrossOrigin(origins = "*")
public class MetricasController {

    private final MetricasService metricasService;
    private final JwtUtil jwtUtil;

    @Autowired
    public MetricasController(MetricasService metricasService, JwtUtil jwtUtil) {
        this.metricasService = metricasService;
        this.jwtUtil = jwtUtil;
    }

    // 📊 DESCARGAS DE FAVORITOS POR DÍA
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

    // 🏆 TOP USUARIOS EXPORTADORES
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

    // 🎤 (Opcional) TOP ARTISTAS desde favoritos_*.csv
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

    // 🎧 (Opcional) TOP GÉNEROS desde favoritos_*.csv
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
