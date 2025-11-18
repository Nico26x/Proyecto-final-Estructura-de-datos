package co.edu.uniquindio.application.model;

/**
 * Enumeración que define los roles de usuario en el sistema.
 * <p>
 * Los roles determinan el nivel de autorización y permisos de un usuario
 * en la plataforma SyncUp.
 * </p>
 *
 * @author SyncUp
 * @version 1.0
 */
public enum Rol {
    /**
     * Rol de administrador.
     * <p>
     * Permisos completos para gestionar usuarios, canciones, métricas y configuración del sistema.
     * </p>
     */
    ADMIN,

    /**
     * Rol de usuario regular.
     * <p>
     * Permisos limitados: reproducción de música, gestión de favoritos, redes sociales
     * y acceso a funcionalidades estándar de la aplicación.
     * </p>
     */
    USER
}
