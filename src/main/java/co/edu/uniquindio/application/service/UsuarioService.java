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

/**
 * Servicio de gestión de usuarios del sistema.
 * <p>
 * Proporciona funcionalidades de autenticación, gestión de sesiones, favoritos,
 * relaciones sociales (seguir/dejar de seguir) y exportación de datos.
 * </p>
 * <p>
 * Características principales:
 * </p>
 * <ul>
 *   <li>Registro y autenticación de usuarios con contraseñas cifradas</li>
 *   <li>Generación de tokens JWT para sesiones</li>
 *   <li>Gestión de canciones favoritas por usuario</li>
 *   <li>Red social con seguimiento de usuarios (grafo social)</li>
 *   <li>Sugerencias de usuarios basadas en favoritos comunes</li>
 *   <li>Exportación de favoritos a CSV con métricas</li>
 *   <li>Generación de playlists de descubrimiento personalizadas</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CancionRepository cancionRepository;
    private final PasswordEncoder passwordEncoder;
    /**
     * Grafo social que representa las relaciones de seguimiento entre usuarios.
     */
    private final GrafoSocial grafoSocial = new GrafoSocial();
    private static final String RUTA_GRAFO = "src/main/resources/data/grafo_social.txt";

    // === NUEVO: rutas para reportes y métricas ===
    private static final String RUTA_REPORTES = "src/main/resources/data/reportes";
    private static final String RUTA_METRICAS = "src/main/resources/data/metricas";
    private static final String ARCHIVO_METRICAS = "metricas_export_favoritos.csv";

    /**
     * Referencia al servicio de canciones para obtener recomendaciones y similares.
     */
    private final CancionService cancionService;

    /**
     * Usuario actualmente autenticado en sesión.
     */
    private Usuario usuarioLogueado;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MetricasService metricasService;

    /**
     * Constructor del servicio de usuarios.
     *
     * @param usuarioRepository el repositorio de usuarios
     * @param cancionRepository el repositorio de canciones
     * @param cancionService el servicio de canciones
     * @param passwordEncoder el codificador de contraseñas
     */
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

    /**
     * Inicializa el grafo social cargando las relaciones guardadas desde archivo.
     * <p>
     * Se ejecuta al iniciar la aplicación (@PostConstruct).
     * </p>
     */
    @PostConstruct
    public void inicializarGrafo() {
        grafoSocial.cargarRelacionesDesdeArchivo(RUTA_GRAFO);
    }

    /**
     * Inicializa el usuario administrador por defecto si no existe.
     * <p>
     * Se ejecuta al iniciar la aplicación (@PostConstruct).
     * Crea un usuario admin/admin123 con rol ADMIN si aún no existe.
     * </p>
     */
    @PostConstruct
    public void inicializarAdmin() {
        if (usuarioRepository.buscarPorUsername("admin") == null) {
            String passwordEncriptada = passwordEncoder.encode("admin123");
            Usuario admin = new Usuario("admin", passwordEncriptada, "Administrador", Rol.ADMIN);
            usuarioRepository.guardarUsuario(admin);
            System.out.println("👑 Usuario administrador creado por defecto (admin / admin123)");
        }
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * <p>
     * Encripta la contraseña, crea un nuevo usuario con rol USER,
     * lo añade al repositorio y también al grafo social.
     * </p>
     *
     * @param username el nombre de usuario único
     * @param password la contraseña en texto plano
     * @param nombre el nombre completo del usuario
     * @return {@code true} si el registro fue exitoso, {@code false} si el usuario ya existe
     */
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

    /**
     * Autentica un usuario y genera un token JWT.
     * <p>
     * Valida las credenciales contra la contraseña encriptada del usuario.
     * </p>
     *
     * @param username el nombre de usuario
     * @param password la contraseña en texto plano
     * @return token JWT si la autenticación es exitosa, {@code null} en caso contrario
     */
    public String login(String username, String password) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
            return jwtUtil.generarToken(username, usuario.getRol().name());
        }
        return null;
    }

    /**
     * Autentica un usuario verificando sus credenciales.
     *
     * @param username el nombre de usuario
     * @param password la contraseña en texto plano
     * @return el objeto Usuario si la autenticación es exitosa, {@code null} en caso contrario
     */
    public Usuario autenticarUsuario(String username, String password) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
            return usuario;
        }
        return null;
    }

    /**
     * Inicia una sesión de usuario almacenándolo en memoria.
     *
     * @param usuario el usuario a establecer como autenticado
     */
    public void iniciarSesion(Usuario usuario) {
        this.usuarioLogueado = usuario;
    }

    /**
     * Cierra la sesión actual del usuario.
     */
    public void logout() {
        usuarioLogueado = null;
    }

    /**
     * Obtiene el usuario actualmente autenticado en sesión.
     *
     * @return el usuario en sesión, o {@code null} si no hay sesión activa
     */
    public Usuario obtenerUsuarioActual() {
        return usuarioLogueado;
    }

    /**
     * Lista todos los usuarios registrados en el sistema.
     *
     * @return colección de todos los usuarios
     */
    public Collection<Usuario> listarUsuarios() {
        return usuarioRepository.listarUsuarios().values();
    }

    /**
     * Agrega una canción a los favoritos de un usuario.
     *
     * @param username el nombre del usuario
     * @param idCancion el identificador de la canción
     * @return mensaje indicando el resultado de la operación
     */
    public String agregarFavorito(String username, String idCancion) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        Cancion cancion = cancionRepository.buscarPorId(idCancion);

        if (usuario == null) return "❌ Usuario no encontrado";
        if (cancion == null) return "❌ Canción no encontrada";

        boolean agregado = usuarioRepository.agregarFavorito(username, cancion);
        return agregado ? "✅ Canción agregada a favoritos" : "⚠️ Ya estaba en favoritos";
    }

    /**
     * Elimina una canción de los favoritos de un usuario.
     *
     * @param username el nombre del usuario
     * @param idCancion el identificador de la canción
     * @return mensaje indicando el resultado de la operación
     */
    public String eliminarFavorito(String username, String idCancion) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return "❌ Usuario no encontrado";

        boolean eliminado = usuarioRepository.eliminarFavorito(username, idCancion);
        return eliminado ? "🗑️ Canción eliminada de favoritos" : "⚠️ No estaba en favoritos";
    }

    /**
     * Obtiene la lista de canciones favoritas de un usuario.
     *
     * @param username el nombre del usuario
     * @return colección de canciones favoritas
     */
    public Collection<Cancion> listarFavoritos(String username) {
        return usuarioRepository.listarFavoritos(username);
    }

    /**
     * Verifica si hay una sesión de usuario activa.
     *
     * @return {@code true} si hay un usuario autenticado, {@code false} en caso contrario
     */
    public boolean haySesionActiva() {
        return usuarioLogueado != null;
    }

    /**
     * Actualiza el nombre completo de un usuario.
     *
     * @param username el nombre de usuario
     * @param nuevoNombre el nuevo nombre completo
     * @return mensaje indicando el resultado de la operación
     */
    public String actualizarNombre(String username, String nuevoNombre) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return "❌ Usuario no encontrado";

        usuario.setNombre(nuevoNombre);
        usuarioRepository.guardarUsuario(usuario);
        return "✅ Nombre actualizado correctamente";
    }

    /**
     * Elimina un usuario del sistema (acción administrativa).
     * <p>
     * No permite eliminar el usuario administrador por defecto.
     * Mantiene la consistencia del grafo social.
     * </p>
     *
     * @param username el nombre del usuario a eliminar
     * @return {@code true} si la eliminación fue exitosa, {@code false} en caso contrario
     */
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


    /**
     * Cambia la contraseña de un usuario.
     *
     * @param username el nombre del usuario
     * @param nuevaPassword la nueva contraseña en texto plano
     * @return mensaje indicando el resultado de la operación
     */
    public String cambiarPassword(String username, String nuevaPassword) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) return "❌ Usuario no encontrado";

        String passwordEncriptada = passwordEncoder.encode(nuevaPassword);
        usuario.setPassword(passwordEncriptada);
        usuarioRepository.guardarUsuario(usuario);
        return "🔑 Contraseña actualizada correctamente";
    }

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username el nombre de usuario a buscar
     * @return el usuario encontrado, o {@code null} si no existe
     */
    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.buscarPorUsername(username);
    }

    /**
     * Obtiene el nombre de usuario del usuario actualmente autenticado.
     *
     * @return el nombre de usuario del contexto de seguridad actual
     */
    public String obtenerUsernameActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * Genera una playlist de descubrimiento semanal personalizada para un usuario.
     * <p>
     * Implementa RF-005. Utiliza canciones similares a los favoritos del usuario
     * con un sistema de ranking para sugerencias.
     * </p>
     *
     * @param username el nombre del usuario
     * @param size el tamaño máximo de la playlist
     * @return lista de canciones recomendadas
     */
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

    /**
     * Agrega una relación de seguimiento entre dos usuarios.
     * <p>
     * El usuario especificado en {@code username} comenzará a seguir al usuario {@code destino}.
     * Actualiza el grafo social y lo persiste en archivo.
     * </p>
     *
     * @param username el nombre del usuario que seguirá
     * @param destino el nombre del usuario a seguir
     * @return mensaje indicando el resultado de la operación
     */
    public String seguirUsuario(String username, String destino) {
        Usuario origen = usuarioRepository.buscarPorUsername(username);
        Usuario objetivo = usuarioRepository.buscarPorUsername(destino);

        if (origen == null || objetivo == null) {
            return "❌ Usuario no encontrado";
        }

        // Asegurarse de que los usuarios estén registrados en el grafo
        grafoSocial.agregarUsuario(username);
        grafoSocial.agregarUsuario(destino);

        // Intentar seguir al usuario
        boolean exito = grafoSocial.seguirUsuario(username, destino);

        if (exito) {
            // Guardar las relaciones actualizadas en el archivo del grafo
            grafoSocial.guardarRelacionesEnArchivo(RUTA_GRAFO);
            return "✅ Ahora sigues a " + destino;
        } else {
            return "⚠️ Ya estás siguiendo a " + destino; // O un mensaje adecuado si no se agregó la relación
        }
    }


    /**
     * Elimina una relación de seguimiento entre dos usuarios.
     * <p>
     * El usuario especificado en {@code username} dejaráde seguir al usuario {@code destino}.
     * Actualiza el grafo social y lo persiste en archivo.
     * </p>
     *
     * @param username el nombre del usuario que dejaráde seguir
     * @param destino el nombre del usuario a dejar de seguir
     * @return mensaje indicando el resultado de la operación
     */
    public String dejarDeSeguir(String username, String destino) {
        Usuario origen = usuarioRepository.buscarPorUsername(username);
        Usuario objetivo = usuarioRepository.buscarPorUsername(destino);

        if (origen == null || objetivo == null) {
            return "❌ Usuario no encontrado";
        }

        // Asegurarse de que los usuarios estén registrados en el grafo
        grafoSocial.agregarUsuario(username);
        grafoSocial.agregarUsuario(destino);

        // Intentar dejar de seguir al usuario
        boolean exito = grafoSocial.dejarDeSeguir(username, destino);

        if (exito) {
            // Guardar las relaciones actualizadas en el archivo del grafo
            grafoSocial.guardarRelacionesEnArchivo(RUTA_GRAFO);
            return "✅ Has dejado de seguir a " + destino;
        } else {
            return "⚠️ No seguías a " + destino; // O un mensaje adecuado si no se eliminó la relación
        }
    }


    /**
     * Lista los usuarios que un usuario específico está siguiendo.
     *
     * @param username el nombre del usuario
     * @return conjunto de nombres de usuarios que está siguiendo
     */
    public Set<String> listarSeguidos(String username) {
        return grafoSocial.obtenerAmigos(username);
    }

    /**
     * Obtiene sugerencias de usuarios basadas en amigos de amigos.
     *
     * @param username el nombre del usuario
     * @param limite el número máximo de sugerencias
     * @return lista de nombres de usuarios sugeridos
     */
    public List<String> sugerirUsuarios(String username, int limite) {
        return grafoSocial.sugerirUsuarios(username, limite);
    }

    /* ---------------------------
       RF-009: Exportar Favoritos a CSV
       --------------------------- */

    /**
     * Genera el contenido CSV de los favoritos del usuario en formato bytes.
     * <p>
     * Incluye encabezado con columnas estándar (id, titulo, artista, genero, anio, duracion_seg).
     * </p>
     *
     * @param username el nombre del usuario
     * @return bytes del contenido CSV
     * @throws IllegalArgumentException si el usuario no existe
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
     * Construye el nombre de archivo para descarga del CSV de favoritos.
     * <p>
     * Incluye fecha en formato YYYYMMDD para diferenciar descargas.
     * Ejemplo: favoritos_deivid_20251108.csv
     * </p>
     *
     * @param username el nombre del usuario
     * @return nombre de archivo sugerido para descargar
     */
    public String buildFavoritosFilename(String username) {
        String fecha = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        return "favoritos_" + username + "_" + fecha + ".csv";
    }

    /* ========= Helpers de RUTA y guardado en DATA ========= */

    /**
     * Obtiene la ruta del directorio donde se guardan reportes CSV por usuario.
     *
     * @return path del directorio de reportes
     */
    public Path getReportesDir() {
        // Dentro de la misma carpeta "data" de persistencia
        return Paths.get(RUTA_REPORTES);
    }

    /**
     * Construye el nombre de archivo fijo de almacenamiento para un usuario.
     * <p>
     * Sin fecha, un solo CSV por usuario, se sobrescribe en cada actualización.
     * </p>
     *
     * @param username el nombre del usuario
     * @return nombre de archivo (ej: favoritos_deivid.csv)
     */
    public String buildFavoritosStorageName(String username) {
        // Sin fecha → un solo CSV por usuario, se ACTUALIZA
        return "favoritos_" + username + ".csv";
    }

    /**
     * Obtiene la ruta completa del archivo CSV de favoritos de un usuario.
     * <p>
     * Ubicado en /data/reportes/favoritos_username.csv
     * </p>
     *
     * @param username el nombre del usuario
     * @return path completo del archivo de reportes
     */
    public Path getReporteFavoritosPath(String username) {
        return getReportesDir().resolve(buildFavoritosStorageName(username));
    }

    /**
     * Obtiene la ruta del directorio de métricas.
     *
     * @return path del directorio de métricas
     */
    public Path getMetricasDir() {
        return Paths.get(RUTA_METRICAS);
    }

    /**
     * Obtiene la ruta del archivo de métricas de exportación de favoritos.
     *
     * @return path del archivo de métricas
     */
    public Path getMetricasFavoritosPath() {
        return getMetricasDir().resolve(ARCHIVO_METRICAS);
    }

    /**
     * Guarda el CSV de favoritos en el directorio de datos.
     * <p>
     * Crea o sobrescribe el archivo en /data/reportes.
     * </p>
     *
     * @param username el nombre del usuario
     * @param csvBytes el contenido del CSV en bytes
     * @return ruta absoluta del archivo guardado
     * @throws RuntimeException si ocurre un error al guardar
     */
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
     * Cuenta el número de canciones favoritas actuales del usuario.
     * <p>
     * Útil para registrar en métricas.
     * </p>
     *
     * @param username el nombre del usuario
     * @return cantidad de favoritos, o 0 si la colección es nula
     */
    public int contarFavoritosUsuario(String username) {
        Collection<Cancion> favs = listarFavoritos(username);
        return (favs == null) ? 0 : favs.size();
    }

    /**
     * Registra una métrica de exportación de favoritos en archivo CSV.
     * <p>
     * Columnas: fecha_iso, username, total_favoritos, archivo_reporte
     * Crea el archivo con encabezado si no existe.
     * </p>
     *
     * @param username el nombre del usuario
     * @param totalFavoritos la cantidad de favoritos exportados
     * @param archivoReporte la ruta del archivo de reporte guardado
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
     * Flujo completo de exportación y guardado de favoritos a CSV.
     * <p>
     * Genera el CSV en memoria, lo guarda/actualiza en /data/reportes,
     * registra la métrica de exportación y retorna información completa.
     * </p>
     *
     * @param username el nombre del usuario
     * @return objeto con bytes del CSV, nombre de descarga y ruta guardada
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

    /**
     * DTO que encapsula la información de un resultado de exportación.
     * <p>
     * Contiene los bytes del CSV, el nombre sugerido para descarga
     * y la ruta absoluta donde fue guardado.
     * </p>
     */
    public static class ExportResultado {
        /**
         * Contenido del CSV en formato bytes.
         */
        public final byte[] csv;

        /**
         * Nombre de archivo sugerido para la descarga.
         */
        public final String downloadName;

        /**
         * Ruta absoluta del archivo guardado en el servidor.
         */
        public final String savedAbsolutePath;

        /**
         * Constructor del DTO.
         *
         * @param csv el contenido en bytes
         * @param downloadName el nombre para descargar
         * @param savedAbsolutePath la ruta absoluta guardada
         */
        public ExportResultado(byte[] csv, String downloadName, String savedAbsolutePath) {
            this.csv = csv;
            this.downloadName = downloadName;
            this.savedAbsolutePath = savedAbsolutePath;
        }
    }

    /**
     * Exporta los favoritos del usuario actualmente autenticado en el contexto de seguridad.
     *
     * @return bytes del contenido CSV
     */
    public byte[] exportarFavoritosCsvUsuarioActual() {
        String username = obtenerUsernameActual();
        return exportarFavoritosCsv(username);
    }

    /**
     * Obtiene sugerencias de usuarios basadas en canciones favoritas comunes.
     * <p>
     * Compara los favoritos del usuario con todos los demás usuarios
     * y retorna aquellos con más canciones en común.
     * </p>
     *
     * @param username el nombre del usuario
     * @param limite el número máximo de sugerencias
     * @return lista de nombres de usuarios sugeridos ordenados por similitud
     */
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
