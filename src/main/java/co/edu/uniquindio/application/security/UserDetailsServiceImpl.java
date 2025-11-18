package co.edu.uniquindio.application.security;

import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementación de UserDetailsService para integración con Spring Security.
 * <p>
 * Carga los detalles del usuario desde el repositorio de base de datos
 * basándose en el nombre de usuario. Esta clase es utilizada por Spring Security
 * durante el proceso de autenticación.
 * </p>
 *
 * @author SyncUp
 * @version 1.0
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * Repositorio de usuarios para acceso a datos.
     */
    private final UsuarioRepository usuarioRepository;

    /**
     * Constructor que inyecta el repositorio de usuarios.
     *
     * @param usuarioRepository el repositorio de usuarios
     */
    @Autowired
    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carga los detalles del usuario desde el repositorio utilizando su nombre de usuario.
     * <p>
     * Implementa el método requerido por la interfaz UserDetailsService de Spring Security.
     * Se utiliza durante la autenticación para recuperar la información del usuario.
     * </p>
     *
     * @param username el nombre de usuario a buscar
     * @return los detalles del usuario (UserDetails) para Spring Security
     * @throws UsernameNotFoundException si el usuario no existe en el repositorio
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.buscarPorUsername(username);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities("USER")
                .build();
    }
}
