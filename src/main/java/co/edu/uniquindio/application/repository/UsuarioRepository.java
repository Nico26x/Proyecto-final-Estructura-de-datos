package co.edu.uniquindio.application.repository;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.Rol;
import co.edu.uniquindio.application.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repositorio de usuarios con persistencia en archivo.
 * <p>
 * Gestiona la lectura, escritura y búsqueda de usuarios utilizando un archivo
 * de texto (usuarios.txt) como almacenamiento. Proporciona operaciones CRUD,
 * gestión de favoritos y carga de dependencias con CancionRepository.
 * </p>
 * <p>
 * Formato de línea en archivo:
 * <code>username;password;nombre;ROL;id1,id2,id3</code>
 * </p>
 * <p>
 * Soporta retrocompatibilidad con formatos legacy sin rol o sin favoritos.
 * </p>
 *
 * @author SyncUp
 * @version 1.0
 */
@Repository
public class UsuarioRepository {

    /**
     * Almacenamiento en memoria de usuarios (thread-safe).
     */
    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();

    /**
     * Ruta del archivo de persistencia de usuarios.
     */
    private static final String FILE_PATH = "src/main/resources/data/usuarios.txt";

    /**
     * Referencia al repositorio de canciones para resolver IDs de favoritos.
     */
    private final CancionRepository cancionRepository;

    /**
     * Constructor que inyecta el repositorio de canciones y carga los usuarios desde archivo.
     *
     * @param cancionRepository el repositorio de canciones inyectado
     */
    @Autowired
    public UsuarioRepository(CancionRepository cancionRepository) {
        this.cancionRepository = cancionRepository;
        cargarUsuariosDesdeArchivo();
    }

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username el nombre de usuario a buscar
     * @return el usuario si existe, {@code null} en caso contrario
     */
    public Usuario buscarPorUsername(String username) {
        return usuarios.get(username);
    }

    /**
     * Guarda un nuevo usuario o actualiza uno existente.
     *
     * @param usuario el usuario a guardar o actualizar
     */
    public void guardarUsuario(Usuario usuario) {
        usuarios.put(usuario.getUsername(), usuario);
        guardarUsuariosEnArchivo();
    }

    /**
     * Lista todos los usuarios registrados en el repositorio.
     *
     * @return mapa no modificable con todos los usuarios
     */
    public Map<String, Usuario> listarUsuarios() {
        return Collections.unmodifiableMap(usuarios);
    }

    /**
     * Verifica si un usuario existe en el repositorio.
     *
     * @param username el nombre de usuario a verificar
     * @return {@code true} si el usuario existe, {@code false} en caso contrario
     */
    public boolean existe(String username) {
        return usuarios.containsKey(username);
    }

    /**
     * Elimina un usuario del repositorio.
     *
     * @param username el nombre del usuario a eliminar
     * @return el usuario eliminado, o {@code null} si no existía
     */
    public Usuario eliminarUsuario(String username) {
        Usuario eliminado = usuarios.remove(username);
        if (eliminado != null) {
            guardarUsuariosEnArchivo();
        }
        return eliminado;
    }

    /**
     * Auxiliar para eliminar un usuario si existe, devolviendo un booleano.
     *
     * @param username el nombre del usuario a eliminar
     * @return {@code true} si el usuario fue eliminado, {@code false} si no existía
     */
    public boolean eliminarUsuarioSiExiste(String username) {
        return eliminarUsuario(username) != null;
    }

    /**
     * Auxiliar para eliminar múltiples usuarios en una sola operación.
     *
     * @param usernames colección con los nombres de usuarios a eliminar
     * @return la cantidad de usuarios que fueron efectivamente eliminados
     */
    public int eliminarUsuarios(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) return 0;
        int count = 0;
        for (String u : usernames) {
            if (usuarios.remove(u) != null) {
                count++;
            }
        }
        if (count > 0) {
            guardarUsuariosEnArchivo();
        }
        return count;
    }

    /**
     * Agrega una canción a la lista de favoritos de un usuario.
     *
     * @param username el nombre del usuario
     * @param cancion la canción a agregar a favoritos
     * @return {@code true} si la canción fue agregada, {@code false} si ya estaba o usuario no existe
     */
    public boolean agregarFavorito(String username, Cancion cancion) {
        Usuario usuario = usuarios.get(username);
        if (usuario != null && cancion != null) {
            boolean agregado = usuario.agregarFavorito(cancion);
            guardarUsuariosEnArchivo();
            return agregado;
        }
        return false;
    }

    /**
     * Elimina una canción de la lista de favoritos de un usuario.
     *
     * @param username el nombre del usuario
     * @param idCancion el identificador de la canción a eliminar
     * @return {@code true} si la canción fue eliminada, {@code false} si no existía o usuario no existe
     */
    public boolean eliminarFavorito(String username, String idCancion) {
        Usuario usuario = usuarios.get(username);
        if (usuario != null) {
            boolean eliminado = usuario.eliminarFavorito(idCancion);
            guardarUsuariosEnArchivo();
            return eliminado;
        }
        return false;
    }

    /**
     * Lista todas las canciones favoritas de un usuario.
     *
     * @param username el nombre del usuario
     * @return colección de canciones favoritas, o lista vacía si el usuario no existe
     */
    public Collection<Cancion> listarFavoritos(String username) {
        Usuario usuario = usuarios.get(username);
        return usuario != null ? usuario.getListaFavoritos() : List.of();
    }

    /**
     * Carga todos los usuarios desde el archivo de persistencia.
     * <p>
     * Soporta múltiples formatos para retrocompatibilidad:
     * <ul>
     *   <li>username;password;nombre</li>
     *   <li>username;password;nombre;ROL</li>
     *   <li>username;password;nombre;ROL;id1,id2,...</li>
     *   <li>username;password;nombre;id1,id2,... (legacy sin rol)</li>
     * </ul>
     * </p>
     * <p>
     * Resuelve los IDs de canciones en favoritos a través del CancionRepository inyectado.
     * Si una canción no existe, se registra una advertencia pero no detiene la carga.
     * </p>
     */
    private void cargarUsuariosDesdeArchivo() {
        File archivo = new File(FILE_PATH);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] partes = linea.split(";");
                // formato esperado (retrocompatible):
                // 1) username;password;nombre
                // 2) username;password;nombre;ROL
                // 3) username;password;nombre;ROL;id1,id2,...
                // 4) username;password;nombre;id1,id2,...   (legacy donde no hay rol)
                if (partes.length >= 3) {
                    String username = partes[0].trim();
                    String password = partes[1].trim();
                    String nombre = partes[2].trim();

                    // valor por defecto
                    String rolStr = "USER";
                    String favoritosPart = null;

                    if (partes.length == 4) {
                        // si el cuarto campo coincide con ADMIN/USER lo tomamos como rol,
                        // si no, lo interpretamos como lista de favoritos (legacy)
                        String cuarto = partes[3].trim();
                        if ("ADMIN".equalsIgnoreCase(cuarto) || "USER".equalsIgnoreCase(cuarto)) {
                            rolStr = cuarto.toUpperCase();
                        } else {
                            favoritosPart = cuarto;
                        }
                    } else if (partes.length >= 5) {
                        rolStr = partes[3].trim().toUpperCase();
                        favoritosPart = partes[4].trim();
                    }

                    // Crear usuario considerando el constructor que acepta rol
                    Usuario usuario = new Usuario(username, password, nombre, Rol.valueOf(rolStr));

                    // cargar favoritos si vienen
                    if (favoritosPart != null && !favoritosPart.isBlank()) {
                        String[] ids = favoritosPart.split(",");
                        for (String id : ids) {
                            String idTrim = id.trim();
                            if (idTrim.isEmpty()) continue;
                            Cancion c = cancionRepository.buscarPorId(idTrim);
                            if (c != null) {
                                usuario.agregarFavorito(c); // usa tu método existente
                            } else {
                                // opcional: registrar aviso si id no existe
                                // System.err.println("⚠️ Canción con id " + idTrim + " no encontrada al cargar favoritos de " + username);
                            }
                        }
                    }

                    usuarios.put(username, usuario);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error al cargar usuarios: " + e.getMessage());
        } catch (IllegalArgumentException iae) {
            System.err.println("❌ Error al interpretar rol de usuario: " + iae.getMessage());
        }
    }

    /**
     * Guarda todos los usuarios en el archivo de persistencia.
     * <p>
     * Escribe cada usuario con el formato:
     * <code>username;password;nombre;ROL;id1,id2,...</code>
     * </p>
     * <p>
     * Si un usuario no tiene favoritos, se escribe un campo vacío al final.
     * La codificación utilizada es UTF-8.
     * </p>
     */
    private void guardarUsuariosEnArchivo() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_PATH), StandardCharsets.UTF_8))) {

            for (Usuario u : usuarios.values()) {
                // formar la lista de ids de favoritos
                String favIds = "";
                if (u.getListaFavoritos() != null && !u.getListaFavoritos().isEmpty()) {
                    favIds = u.getListaFavoritos().stream()
                            .map(Cancion::getId)
                            .collect(Collectors.joining(","));
                }

                // escribimos: username;password;nombre;ROL;id1,id2,...
                // si no hay favoritos escribimos campo vacío al final
                bw.write(String.format("%s;%s;%s;%s;%s",
                        u.getUsername(),
                        u.getPassword(),
                        u.getNombre(),
                        (u.getRol() != null ? u.getRol().name() : "USER"),
                        favIds));
                bw.newLine();
            }

        } catch (IOException e) {
            System.err.println("❌ Error al guardar usuarios: " + e.getMessage());
        }
    }

}
