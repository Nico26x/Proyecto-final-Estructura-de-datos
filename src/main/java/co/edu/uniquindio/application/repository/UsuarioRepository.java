package co.edu.uniquindio.application.repository;

import co.edu.uniquindio.application.model.Usuario;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio con persistencia en archivo usuarios.txt
 */
@Repository
public class UsuarioRepository {

    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    private static final String FILE_PATH = "src/main/resources/data/usuarios.txt";

    public UsuarioRepository() {
        cargarUsuariosDesdeArchivo();
    }

    public Usuario buscarPorUsername(String username) {
        return usuarios.get(username);
    }

    public void guardarUsuario(Usuario usuario) {
        usuarios.put(usuario.getUsername(), usuario);
        guardarUsuariosEnArchivo();
    }

    public Map<String, Usuario> listarUsuarios() {
        return Collections.unmodifiableMap(usuarios);
    }

    public boolean existe(String username) {
        return usuarios.containsKey(username);
    }

    public Usuario eliminarUsuario(String username) {
        Usuario eliminado = usuarios.remove(username);
        if (eliminado != null) {
            guardarUsuariosEnArchivo();
        }
        return eliminado;
    }

    /**
     * Cargar usuarios desde usuarios.txt al iniciar
     */
    private void cargarUsuariosDesdeArchivo() {
        File archivo = new File(FILE_PATH);
        if (!archivo.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length == 3) {
                    String username = partes[0];
                    String password = partes[1];
                    String nombre = partes[2];
                    usuarios.put(username, new Usuario(username, password, nombre));
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error al cargar usuarios: " + e.getMessage());
        }
    }

    /**
     * Guardar usuarios en usuarios.txt cada vez que hay cambios
     */
    private void guardarUsuariosEnArchivo() {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FILE_PATH), StandardCharsets.UTF_8))) {
            for (Usuario u : usuarios.values()) {
                bw.write(u.getUsername() + ";" + u.getPassword() + ";" + u.getNombre());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("❌ Error al guardar usuarios: " + e.getMessage());
        }
    }
}
