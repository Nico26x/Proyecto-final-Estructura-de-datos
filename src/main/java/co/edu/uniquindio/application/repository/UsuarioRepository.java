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
 * Repositorio con persistencia en archivo usuarios.txt y manejo de favoritos.
 * Formato de línea:
 * username;password;nombre;id1,id2,id3
 */
@Repository
public class UsuarioRepository {

    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    private static final String FILE_PATH = "src/main/resources/data/usuarios.txt";

    private final CancionRepository cancionRepository;

    @Autowired
    public UsuarioRepository(CancionRepository cancionRepository) {
        this.cancionRepository = cancionRepository;
        cargarUsuariosDesdeArchivo();
    }

    // 📌 Buscar usuario por username
    public Usuario buscarPorUsername(String username) {
        return usuarios.get(username);
    }

    // 📌 Registrar o actualizar usuario
    public void guardarUsuario(Usuario usuario) {
        usuarios.put(usuario.getUsername(), usuario);
        guardarUsuariosEnArchivo();
    }

    // 📋 Listar todos los usuarios
    public Map<String, Usuario> listarUsuarios() {
        return Collections.unmodifiableMap(usuarios);
    }

    // 📌 Verificar si existe
    public boolean existe(String username) {
        return usuarios.containsKey(username);
    }

    // 🗑️ Eliminar usuario
    public Usuario eliminarUsuario(String username) {
        Usuario eliminado = usuarios.remove(username);
        if (eliminado != null) {
            guardarUsuariosEnArchivo();
        }
        return eliminado;
    }

    // 🎵 FAVORITOS
    public boolean agregarFavorito(String username, Cancion cancion) {
        Usuario usuario = usuarios.get(username);
        if (usuario != null && cancion != null) {
            boolean agregado = usuario.agregarFavorito(cancion);
            guardarUsuariosEnArchivo();
            return agregado;
        }
        return false;
    }

    public boolean eliminarFavorito(String username, String idCancion) {
        Usuario usuario = usuarios.get(username);
        if (usuario != null) {
            boolean eliminado = usuario.eliminarFavorito(idCancion);
            guardarUsuariosEnArchivo();
            return eliminado;
        }
        return false;
    }

    public Collection<Cancion> listarFavoritos(String username) {
        Usuario usuario = usuarios.get(username);
        return usuario != null ? usuario.getListaFavoritos() : List.of();
    }

    /**
     * 🔹 Cargar usuarios desde usuarios.txt
     */
    // dentro de UsuarioRepository (asegúrate de tener un campo:
// private final CancionRepository cancionRepository; y que esté inyectado)

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
