package co.edu.uniquindio.application.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utilidad para la gestión de tokens JWT (JSON Web Tokens).
 * <p>
 * Proporciona métodos para generar, validar y extraer información de tokens JWT
 * utilizando la librería JJWT. Los tokens contienen información del usuario (username)
 * y su rol de autorización.
 * </p>
 * <p>
 * Características:
 * </p>
 * <ul>
 *   <li>Generación de tokens JWT firmados con HMAC-SHA256</li>
 *   <li>Validación de integridad y vigencia de tokens</li>
 *   <li>Extracción de claims del token (username, rol)</li>
 *   <li>Tiempo de expiración configurable (24 horas por defecto)</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@Component
public class JwtUtil {

    /**
     * Clave secreta para firmar los tokens JWT.
     * <p>
     * En producción, esto debería obtenerse de variables de entorno o archivos de configuración seguros.
     * </p>
     */
    private static final String SECRET_KEY = "mi_clave_secreta_segura_para_jwt_de_prueba_123456789";

    /**
     * Tiempo de expiración de los tokens en milisegundos.
     * <p>
     * Por defecto: 86400000 ms = 24 horas
     * </p>
     */
    private static final long EXPIRATION_TIME = 86400000;

    /**
     * Clave criptográfica derivada del SECRET_KEY para operaciones de firma.
     */
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * Genera un nuevo token JWT para un usuario con su rol asociado.
     * <p>
     * El token contiene:
     * </p>
     * <ul>
     *   <li>subject: el username del usuario</li>
     *   <li>claim "rol": el rol del usuario</li>
     *   <li>issuedAt: marca de tiempo de creación</li>
     *   <li>expiration: marca de tiempo de expiración (24 horas)</li>
     *   <li>firma: HMAC-SHA256 para garantizar integridad</li>
     * </ul>
     *
     * @param username el nombre de usuario a incluir en el token
     * @param rol el rol del usuario a incluir en el token
     * @return el token JWT codificado y firmado
     */
    public String generarToken(String username, String rol) {
        return Jwts.builder()
                .setSubject(username)
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extrae el nombre de usuario (subject) del token JWT.
     *
     * @param token el token JWT a procesar
     * @return el username contenido en el token
     */
    public String obtenerUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extrae el rol del usuario del claim "rol" del token JWT.
     *
     * @param token el token JWT a procesar
     * @return el rol contenido en el token
     */
    public String obtenerRol(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("rol", String.class);
    }

    /**
     * Valida la integridad y vigencia de un token JWT.
     * <p>
     * Verifica que:
     * </p>
     * <ul>
     *   <li>La firma sea válida (coincide con la clave secreta)</li>
     *   <li>El token no haya expirado</li>
     *   <li>El formato del token sea correcto</li>
     * </ul>
     *
     * @param token el token JWT a validar
     * @return {@code true} si el token es válido, {@code false} en caso contrario
     */
    public boolean validarToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Token inválido: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el nombre de usuario (subject) del token JWT.
     * <p>
     * Alias de {@link #obtenerUsername(String)} con nombre más descriptivo.
     * </p>
     *
     * @param token el token JWT a procesar
     * @return el username contenido en el token
     */
    public String obtenerUsernameDelToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extrae el rol del usuario del claim "rol" del token JWT.
     * <p>
     * Alias de {@link #obtenerRol(String)} con nombre más descriptivo.
     * </p>
     *
     * @param token el token JWT a procesar
     * @return el rol contenido en el token
     */
    public String obtenerRolDelToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("rol", String.class);
    }

}
