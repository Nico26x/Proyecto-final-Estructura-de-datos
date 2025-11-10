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
 * 🔐 Configuración de seguridad basada en JWT.
 */
@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ✅ Recursos estáticos comunes del back (si los usas)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // ✅ Endpoints públicos de auth
                        .requestMatchers("/api/usuarios/login", "/api/usuarios/registrar", "/api/usuarios/auth/**").permitAll()

                        // ✅ (Opcional) Permitir leer canciones sin token (lista/detalle/búsquedas)
                        .requestMatchers(HttpMethod.GET,
                                "/api/canciones", "/api/canciones/*",
                                "/api/canciones/buscar", "/api/canciones/buscar/**",
                                "/api/canciones/*/similares", "/api/canciones/*/radio"
                        ).permitAll()

                        // ✅ Favoritos: permitir GET/POST/DELETE con rol USER o ADMIN
                        .requestMatchers(HttpMethod.GET,    "/api/usuarios/*/favoritos").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/usuarios/*/favoritos/agregar").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/*/favoritos/eliminar").hasAnyRole("USER","ADMIN")

                        // ✅ (Opcional) si expones /music en el back algún día
                        .requestMatchers(HttpMethod.GET, "/music/**").permitAll()

                        // ✅ Preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ======== SOLO ADMIN ========
                        // Usuarios (listar y eliminar)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/listar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/eliminar").hasRole("ADMIN") // coincide con @DeleteMapping("/eliminar")

                        // Canciones (carga masiva, crear, actualizar, eliminar)
                        .requestMatchers(HttpMethod.POST, "/api/canciones/cargar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/canciones").hasRole("ADMIN")       // crear canción
                        .requestMatchers(HttpMethod.PUT, "/api/canciones/**").hasRole("ADMIN")     // actualizar canción
                        .requestMatchers(HttpMethod.DELETE, "/api/canciones/**").hasRole("ADMIN")  // eliminar canción

                        // USER/ADMIN (social y recomendaciones)
                        .requestMatchers(
                                "/api/usuarios/seguir",
                                "/api/usuarios/dejar-seguir",
                                "/api/usuarios/*/seguidos",
                                "/api/usuarios/*/sugerencias",
                                "/api/usuarios/*/descubrimiento"
                        ).hasAnyRole("USER", "ADMIN")

                        // Resto autenticado
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // ✅ CORS para el front
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

    // 🔑 Encriptación
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ⚙️ Manager de autenticación
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
