package co.edu.uniquindio.application.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro de seguridad que valida tokens JWT en cada solicitud HTTP.
 * <p>
 * Intercepta las solicitudes entrantes, extrae el token JWT del encabezado Authorization,
 * valida su autenticidad y coloca la información del usuario (username y rol) en el contexto
 * de seguridad de Spring para su uso posterior.
 * </p>
 * <p>
 * Proceso:
 * </p>
 * <ol>
 *   <li>Extrae el token del encabezado Authorization (formato: "Bearer &lt;token&gt;")</li>
 *   <li>Obtiene el username y rol del token JWT</li>
 *   <li>Valida la integridad y vigencia del token</li>
 *   <li>Crea un contexto de autenticación con los datos del usuario</li>
 *   <li>Continúa con la cadena de filtros</li>
 * </ol>
 *
 * @author SyncUp
 * @version 1.0
 * @see JwtUtil
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    /**
     * Utilidad para operaciones con tokens JWT.
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Procesa cada solicitud HTTP para validar y establecer la autenticación JWT.
     * <p>
     * Este método se ejecuta una única vez por solicitud (OncePerRequestFilter).
     * Valida el token JWT y establece el contexto de seguridad si es válido.
     * </p>
     *
     * @param request la solicitud HTTP entrante
     * @param response la respuesta HTTP
     * @param filterChain la cadena de filtros de seguridad
     * @throws ServletException si ocurre un error de servlet
     * @throws IOException si ocurre un error de I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;
        String rol = null;

        // Extrae el token JWT del encabezado Authorization
        // Formato esperado: "Bearer <token>"
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            username = jwtUtil.obtenerUsernameDelToken(token);
            rol = jwtUtil.obtenerRolDelToken(token);
        }

        // Valida el token y registra el usuario en el contexto de seguridad si es válido
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validarToken(token)) {
                // Crear el objeto de usuario con su rol
                User userDetails = new User(username, "", Collections.singleton(new SimpleGrantedAuthority("ROLE_" + rol)));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
