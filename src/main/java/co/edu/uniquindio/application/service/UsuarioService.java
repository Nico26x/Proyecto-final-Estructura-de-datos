package co.edu.uniquindio.application.service;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.GrafoSocial;
import co.edu.uniquindio.application.model.Rol;
import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.repository.CancionRepository;
import co.edu.uniquindio.application.repository.UsuarioRepository;
import co.edu.uniquindio.application.security.JwtUtil;
import co.edu.uniquindio.application.utils.CsvUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CancionRepository cancionRepository;
    private final PasswordEncoder passwordEncoder;
    private final GrafoSocial grafoSocial = new GrafoSocial();
    private static final String RUTA_GRAFO = "src/main/resources/data/grafo_social.txt";

    // === NUEVO: rutas para reportes y métricas ===
    private static final String RUTA_REPORTES = "src/main/resources/data/reportes";
    private static final String RUTA_METRICAS = "src/main/resources/data/metricas";
    private static final String ARCHIVO_METRICAS = "metricas_export_favoritos.csv";

    // ✅ Referencia al servicio de canciones
    private final CancionService cancionService;

    // 🔹 Usuario en sesión (almacenado temporalmente)
    private Usuario usuarioLogueado;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MetricasService metricasService;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          CancionRepository cancionRepository,
                          CancionService cancionService,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cancionRepository = cancionRepository;
        this.cancionService = cancionService;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ Cargar el grafo desde archivo al iniciar
    @PostConstruct
    public void inicializarGrafo() {
        grafoSocial.cargarRelacionesDesdeArchivo(RUTA_GRAFO);
    }

    @PostConstruct
    public void inicializarAdmin() {
        if (usuarioRepository.buscarPorUsername("admin") == null) {
            String passwordEncriptada = passwordEncoder.encode("admin123");
            Usuario admin = new Usuario("admin", passwordEncriptada, "Administrador", Rol.ADMIN);
            usuarioRepository.guardarUsuario(admin);
            System.out.println("👑 Usuario administrador creado por defecto (admin / admin123)");
        }
    }

    // ✅ Registrar nuevo usuario con contraseña cifrada
    public boolean registrarUsuario(String username, String password, String nombre) {
        if (usuarioRepository.buscarPorUsername(username) != null) {
            return false;
        }
        String passwordEncriptada = passwordEncoder.encode(password);
        Usuario usuario = new Usuario(username, passwordEncriptada, nombre, Rol.USER);
        usuarioRepository.guardarUsuario(usuario);

        // ➕ También lo agregamos al grafo y persistimos
        grafoSocial.agregarUsuario(username);
        grafoSocial.guardarRelacionesEnArchivo(RUTA_GRAFO);

        return true;
    }

    // ✅ Login que devuelve un JWT
    public String login(String username, String password) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
            return jwtUtil.generarToken(username, usuario.getRol().name());
        }
        return null;
    }

    // ✅ Autenticar usuario
    public Usuario autenticarUsuario(String username, String password) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
            return usuario;
        }
        return null;
    }

    public void iniciarSesion(Usuario usuario) {
        this.usuarioLogueado = usuario;
    }

    public void logout() {
        usuarioLogueado = null;
    }

    public Usuario obtenerUsuarioActual() {
        return usuarioLogueado;
    }

    public Collection<Usuario> listarUsuarios() {
        return usuarioRepository.listarUsuarios().values();
    }

    // 🎵 Favoritos
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

    public boolean haySesionActiva() {
        return usuarioLogueado != null;
    }

    public String actualizarNombre(String username, String nuevoNombre) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return "❌ Usuario no encontrado";

        usuario.setNombre(nuevoNombre);
        usuarioRepository.guardarUsuario(usuario);
        return "✅ Nombre actualizado correctamente";
    }

    // 👑 Eliminar usuario (acción de administrador)
    public boolean eliminarUsuarioAdmin(String username) {
        // opcional: proteger al admin por defecto
        if ("admin".equalsIgnoreCase(username)) {
            return false; // no permitir borrar el admin base
        }

        // El repositorio devuelve Usuario; úsalo como boolean comparando con null
        Usuario eliminado = usuarioRepository.eliminarUsuario(username);
        boolean ok = (eliminado != null);

        if (ok) {
            // Mantener consistencia del grafo social
            try {
                grafoSocial.eliminarUsuario(username); // asegúrate de tener este método; si no, bórralo de sus listas
                grafoSocial.guardarRelacionesEnArchivo(RUTA_GRAFO);
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo sincronizar el grafo social tras eliminar usuario: " + e.getMessage());
            }

            // (Opcional) Si estás guardando reportes CSV por usuario, puedes limpiar el archivo:
            // try {
            //     Path reporte = getReporteFavoritosPath(username);
            //     java.nio.file.Files.deleteIfExists(reporte);
            // } catch (Exception e) {
            //     System.err.println("⚠️ No se pudo borrar el CSV de reportes del usuario: " + e.getMessage());
            // }
        }

        return ok;
    }


    public String cambiarPassword(String username, String nuevaPassword) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return "❌ Usuario no encontrado";

        String passwordEncriptada = passwordEncoder.encode(nuevaPassword);
        usuario.setPassword(passwordEncriptada);
        usuarioRepository.guardarUsuario(usuario);
        return "🔑 Contraseña actualizada correctamente";
    }

    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.buscarPorUsername(username);
    }

    public String obtenerUsernameActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // 🎧 Playlist de descubrimiento semanal (RF-005)
    public List<Cancion> generarPlaylistDescubrimiento(String username, int size) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return Collections.emptyList();

        Collection<Cancion> favoritos = usuario.getListaFavoritos();
        if (favoritos == null || favoritos.isEmpty()) {
            return cancionRepository.listarCanciones().stream()
                    .limit(size)
                    .collect(Collectors.toList());
        }

        Map<String, Double> scoreMap = new HashMap<>();
        int kPorFavorito = 10;

        for (Cancion fav : favoritos) {
            List<Cancion> similares = cancionService.obtenerCancionesSimilares(fav.getId(), kPorFavorito);
            int rank = 1;
            for (Cancion s : similares) {
                if (usuario.tieneEnFavoritos(s.getId())) continue;
                double score = (kPorFavorito - rank + 1);
                scoreMap.merge(s.getId(), score, Double::sum);
                rank++;
            }
        }

        if (scoreMap.isEmpty()) {
            return cancionRepository.listarCanciones().stream()
                    .filter(c -> !usuario.tieneEnFavoritos(c.getId()))
                    .limit(size)
                    .collect(Collectors.toList());
        }

        List<String> orderedIds = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(size)
                .collect(Collectors.toList());

        return orderedIds.stream()
                .map(cancionRepository::buscarPorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 👥 Seguir usuario
    public String seguirUsuario(String username, String destino) {
        Usuario origen = usuarioRepository.buscarPorUsername(username);
        Usuario objetivo = usuarioRepository.buscarPorUsername(destino);

        if (origen == null || objetivo == null) return "❌ Usuario no encontrado";

        grafoSocial.agregarUsuario(username);
        grafoSocial.agregarUsuario(destino);

        boolean exito = grafoSocial.seguirUsuario(username, destino);
        if (exito) grafoSocial.guardarRelacionesEnArchivo(RUTA_GRAFO);

        return exito ? "✅ Ahora sigues a " + destino : "⚠️ No se pudo seguir al usuario.";
    }

    // 🚫 Dejar de seguir
    public String dejarDeSeguir(String username, String destino) {
        boolean exito = grafoSocial.dejarDeSeguir(username, destino);
        if (exito) grafoSocial.guardarRelacionesEnArchivo(RUTA_GRAFO);
        return exito ? "🗑️ Has dejado de seguir a " + destino : "⚠️ No seguías a ese usuario.";
    }

    // 📜 Listar seguidos
    public Set<String> listarSeguidos(String username) {
        return grafoSocial.obtenerAmigos(username);
    }

    // 💡 Sugerir usuarios (amigos de amigos)
    public List<String> sugerirUsuarios(String username, int limite) {
        return grafoSocial.sugerirUsuarios(username, limite);
    }

    // ---------------------------
    // RF-009: Exportar Favoritos a CSV
    // ---------------------------

    /**
     * Genera el contenido CSV de los favoritos del usuario (en memoria).
     */
    public byte[] exportarFavoritosCsv(String username) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado: " + username);
        }

        Collection<Cancion> favoritos = listarFavoritos(username);
        StringBuilder sb = new StringBuilder();

        // Encabezado CSV
        sb.append(CsvUtils.joinRow(List.of(
                "id", "titulo", "artista", "genero", "anio", "duracion_seg"
        ))).append("\n");

        // Filas
        for (Cancion c : favoritos) {
            sb.append(CsvUtils.joinRow(List.of(
                    c.getId(),
                    c.getTitulo(),
                    c.getArtista(),
                    c.getGenero(),
                    String.valueOf(c.getAnio()),
                    String.valueOf(c.getDuracion())
            ))).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Nombre de archivo para DESCARGA (incluye fecha, útil para el front).
     * Ej: favoritos_deivid_20251108.csv
     */
    public String buildFavoritosFilename(String username) {
        String fecha = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        return "favoritos_" + username + "_" + fecha + ".csv";
    }

    // ========= Helpers de RUTA y guardado en DATA =========

    /** Directorio donde se guardan/actualizan los CSV por usuario. */
    public Path getReportesDir() {
        // Dentro de la misma carpeta "data" de persistencia
        return Paths.get(RUTA_REPORTES);
    }

    /** Nombre de archivo FIJO en disco (uno por usuario, se sobrescribe). */
    public String buildFavoritosStorageName(String username) {
        // Sin fecha → un solo CSV por usuario, se ACTUALIZA
        return "favoritos_" + username + ".csv";
    }

    /** Ruta completa del CSV de un usuario dentro de /data/reportes. */
    public Path getReporteFavoritosPath(String username) {
        return getReportesDir().resolve(buildFavoritosStorageName(username));
    }

    /** Directorio de métricas. */
    public Path getMetricasDir() {
        return Paths.get(RUTA_METRICAS);
    }

    /** Ruta del archivo de métricas de exportación de favoritos. */
    public Path getMetricasFavoritosPath() {
        return getMetricasDir().resolve(ARCHIVO_METRICAS);
    }

    /** Guarda (crea o sobrescribe) el CSV en /data/reportes y devuelve la ruta absoluta. */
    public String guardarFavoritosCsvEnData(String username, byte[] csvBytes) {
        try {
            Path dir = getReportesDir();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = getReporteFavoritosPath(username);
            Files.write(file, csvBytes); // sobrescribe
            return file.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("Error guardando CSV en data/reportes: " + e.getMessage(), e);
        }
    }

    /**
     * Cuenta favoritos actuales del usuario (útil para métricas).
     */
    public int contarFavoritosUsuario(String username) {
        Collection<Cancion> favs = listarFavoritos(username);
        return (favs == null) ? 0 : favs.size();
    }

    /**
     * Registra una línea de métrica en CSV:
     * columnas: fecha_iso,username,total_favoritos,archivo_reporte
     */
    public void registrarMetricaExportFavoritos(String username, int totalFavoritos, Path archivoReporte) {
        try {
            Path dir = getMetricasDir();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path metricas = getMetricasFavoritosPath();

            boolean existe = Files.exists(metricas);
            if (!existe) {
                // encabezado
                String header = CsvUtils.joinRow(List.of("fecha_iso", "username", "total_favoritos", "archivo_reporte")) + "\n";
                Files.write(metricas, header.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            String fechaIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String rutaRelativa = archivoReporte != null ? getReportesDir().relativize(archivoReporte).toString().replace("\\","/") : "";

            String row = CsvUtils.joinRow(List.of(
                    fechaIso,
                    username,
                    String.valueOf(totalFavoritos),
                    rutaRelativa
            )) + "\n";

            Files.write(metricas, row.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            // No romper el flujo por métricas; solo loguear
            System.err.println("⚠️ No se pudo registrar métrica de exportación: " + e.getMessage());
        }
    }

    /**
     * Flujo completo: genera CSV en memoria, lo guarda/actualiza en /data/reportes,
     * registra la métrica y retorna bytes + nombre sugerido de descarga + ruta guardada.
     */
    public ExportResultado exportarYGuardarFavoritosCsv(String username) {
        byte[] csv = exportarFavoritosCsv(username);
        String savedPath = guardarFavoritosCsvEnData(username, csv);
        String downloadName = buildFavoritosFilename(username);

        // 👇 REGISTRA el evento de exportación en tu servicio de métricas (para endpoints /metricas)
        int items = usuarioRepository.listarFavoritos(username).size();
        metricasService.registrarExportFavoritos(username, items);

        // 👇 (Opcional, adicional) deja traza de auditoría local en /data/metricas/metricas_export_favoritos.csv
        registrarMetricaExportFavoritos(username, items, Paths.get(savedPath));

        return new ExportResultado(csv, downloadName, savedPath);
    }

    /** DTO simple para devolver info del export. */
    public static class ExportResultado {
        public final byte[] csv;
        public final String downloadName;
        public final String savedAbsolutePath;

        public ExportResultado(byte[] csv, String downloadName, String savedAbsolutePath) {
            this.csv = csv;
            this.downloadName = downloadName;
            this.savedAbsolutePath = savedAbsolutePath;
        }
    }

    /** Exportar usando el usuario del SecurityContext (si lo necesitas). */
    public byte[] exportarFavoritosCsvUsuarioActual() {
        String username = obtenerUsernameActual();
        return exportarFavoritosCsv(username);
    }

    // Método para sugerir usuarios basados en canciones favoritas
    public List<String> sugerirUsuariosPorFavoritos(String username, int limite) {
        // Obtener el usuario autenticado
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return Collections.emptyList(); // Si no se encuentra el usuario, retornamos vacío

        // Obtener lista de canciones favoritas del usuario
        Collection<Cancion> favoritosUsuario = usuario.getListaFavoritos();
        if (favoritosUsuario.isEmpty()) return Collections.emptyList(); // Si no tiene favoritos, no hay sugerencias

        // Crear un mapa que almacene el número de canciones favoritas comunes
        Map<String, Integer> coincidencias = new HashMap<>();

        // Recorrer todos los usuarios y comparar sus favoritos
        for (Usuario otroUsuario : usuarioRepository.listarUsuarios().values()) {
            if (otroUsuario.getUsername().equals(username)) continue; // No comparar consigo mismo

            Collection<Cancion> favoritosOtroUsuario = otroUsuario.getListaFavoritos();
            if (favoritosOtroUsuario.isEmpty()) continue;

            // Comparar las canciones favoritas: contar las coincidencias
            long comunes = favoritosUsuario.stream()
                    .filter(cancion -> favoritosOtroUsuario.stream()
                            .anyMatch(c -> c.getId().equals(cancion.getId()))) // Comparar por ID o por otro atributo
                    .count();

            // Si hay coincidencias, agregar al mapa
            if (comunes > 0) {
                coincidencias.put(otroUsuario.getUsername(), (int) comunes);
            }
        }

        // Ordenar a los usuarios por el número de coincidencias y devolver los más relevantes
        return coincidencias.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limite)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

}
