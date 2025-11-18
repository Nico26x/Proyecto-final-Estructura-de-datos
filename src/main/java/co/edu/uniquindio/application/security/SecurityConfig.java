package co.edu.uniquindio.application.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad basada en JWT para la aplicación.
 * <p>
 * Define la cadena de filtros de seguridad, reglas de autorización por rol,
 * CORS y configuración de sesiones sin estado (stateless) para una API REST.
 * </p>
 * <p>
 * Características de seguridad:
 * </p>
 * <ul>
 *   <li>Autenticación mediante tokens JWT</li>
 *   <li>Autorización basada en roles (ADMIN, USER)</li>
 *   <li>CORS habilitado para comunicación con frontend en localhost:3000</li>
 *   <li>CSRF deshabilitado para API REST stateless</li>
 *   <li>Encriptación de contraseñas con BCrypt</li>
 * </ul>
 * <p>
 * Roles y permisos:
 * </p>
 * <ul>
 *   <li><strong>Sin autenticación:</strong> Login, registro, consultas públicas</li>
 *   <li><strong>USER/ADMIN:</strong> Gestión de favoritos, social, recomendaciones</li>
 *   <li><strong>ADMIN:</strong> Gestión completa de usuarios y canciones</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@Configuration
public class SecurityConfig {

    /**
     * Filtro JWT para validar tokens en cada solicitud.
     */
    @Autowired
    private JwtFilter jwtFilter;

    /**
     * Configura la cadena de filtros de seguridad con reglas de autorización.
     * <p>
     * Define:
     * </p>
     * <ul>
     *   <li>CORS: permite solicitudes desde localhost:3000</li>
     *   <li>CSRF: deshabilitado para API REST stateless</li>
     *   <li>Autorización: reglas por rol para cada endpoint</li>
     *   <li>Sesiones: stateless (sin cookies de sesión)</li>
     * </ul>
     *
     * @param http la configuración de seguridad HTTP
     * @return la cadena de filtros de seguridad configurada
     * @throws Exception si ocurre un error en la configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos comunes del back
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // Endpoints públicos de autenticación
                        .requestMatchers("/api/usuarios/login", "/api/usuarios/registrar", "/api/usuarios/auth/**").permitAll()

                        // Lectura de canciones sin autenticación
                        .requestMatchers(HttpMethod.GET,
                                "/api/canciones", "/api/canciones/*",
                                "/api/canciones/buscar", "/api/canciones/buscar/**",
                                "/api/canciones/*/similares", "/api/canciones/*/radio"
                        ).permitAll()

                        // Favoritos: requieren rol USER o ADMIN
                        .requestMatchers(HttpMethod.GET,    "/api/usuarios/*/favoritos").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/usuarios/*/favoritos/agregar").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/*/favoritos/eliminar").hasAnyRole("USER","ADMIN")

                        // Archivos de música públicos
                        .requestMatchers(HttpMethod.GET, "/music/**").permitAll()

                        // Solicitudes CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ======== SOLO ADMIN ========
                        // Gestión de usuarios
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/listar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/eliminar").hasRole("ADMIN")

                        // Gestión de canciones
                        .requestMatchers(HttpMethod.POST, "/api/canciones/cargar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/canciones").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/canciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/canciones/**").hasRole("ADMIN")

                        // Funcionalidades sociales y recomendaciones para USER/ADMIN
                        .requestMatchers(
                                "/api/usuarios/seguir",
                                "/api/usuarios/dejar-seguir",
                                "/api/usuarios/*/seguidos",
                                "/api/usuarios/*/sugerencias",
                                "/api/usuarios/*/descubrimiento"
                        ).hasAnyRole("USER", "ADMIN")

                        // Resto de endpoints requieren autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Configura CORS (Cross-Origin Resource Sharing) para permitir solicitudes desde el frontend.
     * <p>
     * Permite solicitudes desde localhost:3000 (frontend React) con métodos HTTP comunes
     * y todos los encabezados.
     * </p>
     *
     * @return la fuente de configuración CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.setAllowedOrigins(List.of("http://localhost:3000"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * Proporciona el codificador de contraseñas usando BCrypt.
     * <p>
     * BCrypt es un algoritmo de hash seguro que incluye salt automático
     * y es resistente a ataques de fuerza bruta.
     * </p>
     *
     * @return codificador de contraseñas BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Proporciona el gestor de autenticación de Spring Security.
     * <p>
     * Se utiliza para autenticar credenciales de usuario en el proceso de login.
     * </p>
     *
     * @param configuration la configuración de autenticación
     * @return gestor de autenticación configurado
     * @throws Exception si ocurre un error en la configuración
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
