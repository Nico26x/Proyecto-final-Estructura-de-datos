package co.edu.uniquindio.application.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 🔐 Configuración de seguridad basada en JWT.
 * Controla qué endpoints son públicos y cuáles requieren autenticación o rol específico.
 */
@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 🚫 Deshabilitar CSRF (no necesario con JWT)
                .csrf(csrf -> csrf.disable())

                // ⚙️ Configurar autorizaciones por endpoint y rol
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas (sin token)
                        .requestMatchers(
                                "/api/usuarios/login",
                                "/api/usuarios/registrar"
                        ).permitAll()

                        // Endpoints accesibles solo por ADMIN
                        .requestMatchers(
                                "/api/usuarios/listar",
                                "/api/usuarios/**/eliminar",
                                "/api/canciones/cargar",
                                "/api/canciones/**/eliminar"
                        ).hasRole("ADMIN")

                        // Todos los demás requieren estar autenticados (user o admin)
                        .anyRequest().authenticated()
                )

                // 🧩 Política de sesión sin estado (JWT)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 🔄 Insertar el filtro JWT antes del filtro estándar de autenticación
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🔑 Encriptación de contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ⚙️ Manejador de autenticación para compatibilidad con Spring
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
