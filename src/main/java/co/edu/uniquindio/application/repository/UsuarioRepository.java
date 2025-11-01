package co.edu.uniquindio.application.repository;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.Usuario;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private void cargarUsuariosDesdeArchivo() {
        File archivo = new File(FILE_PATH);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length >= 3) {
                    String username = partes[0];
                    String password = partes[1];
                    String nombre = partes[2];

                    Usuario usuario = new Usuario(username, password, nombre);

                    // Si tiene favoritos (posición 3)
                    if (partes.length == 4 && !partes[3].isBlank()) {
                        String[] ids = partes[3].split(",");
                        for (String id : ids) {
                            Cancion c = cancionRepository.buscarPorId(id);
                            if (c != null) {
                                usuario.agregarFavorito(c);
                            }
                        }
                    }

                    usuarios.put(username, usuario);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error al cargar usuarios: " + e.getMessage());
        }
    }

    /**
     * 💾 Guardar usuarios y sus favoritos en usuarios.txt
     */
    private void guardarUsuariosEnArchivo() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_PATH), StandardCharsets.UTF_8))) {

            for (Usuario u : usuarios.values()) {
                StringBuilder linea = new StringBuilder();
                linea.append(u.getUsername()).append(";")
                        .append(u.getPassword()).append(";")
                        .append(u.getNombre()).append(";");

                // Agregar IDs de favoritos separados por coma
                List<Cancion> favs = u.getListaFavoritos();
                if (!favs.isEmpty()) {
                    String favoritos = String.join(",",
                            favs.stream().map(Cancion::getId).toList());
                    linea.append(favoritos);
                }

                bw.write(linea.toString());
                bw.newLine();
            }

        } catch (IOException e) {
            System.err.println("❌ Error al guardar usuarios: " + e.getMessage());
        }
    }
}
