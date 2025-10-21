package co.edu.uniquindio.application.service;

import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioService() {
        this.usuarioRepository = new UsuarioRepository();
    }

    public boolean registrarUsuario(String username, String password, String nombre) {
        if (usuarioRepository.buscarPorUsername(username) != null) {
            return false;
        }
        Usuario usuario = new Usuario(username, password, nombre);
        usuarioRepository.guardarUsuario(usuario);
        return true;
    }

    public Usuario login(String username, String password) {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario != null && usuario.getPassword().equals(password)) {
            return usuario;
        }
        return null;
    }
}
