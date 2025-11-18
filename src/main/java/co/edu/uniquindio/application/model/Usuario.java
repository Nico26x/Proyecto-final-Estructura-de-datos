package co.edu.uniquindio.application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Modelo que representa un usuario en el sistema.
 * <p>
 * Proporciona gestión de autenticación, roles, favoritos y serialización JSON.
 * Implementa controles de seguridad para no exponer datos sensibles en respuestas API.
 * </p>
 * <p>
 * Características de seguridad:
 * </p>
 * <ul>
 *   <li>Password nunca se expone por JSON</li>
 *   <li>Lista de favoritos no se expone directamente; solo el contador</li>
 *   <li>Identificación única basada en username (RF-017)</li>
 *   <li>Compatible con serialización Jackson</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "username", "nombre", "rol", "favoritosCount" })
public class Usuario {

    /**
     * Nombre de usuario único en el sistema.
     */
    private String username;

    /**
     * Contraseña del usuario (nunca se expone por JSON).
     * Se almacena codificada con BCrypt en la persistencia.
     */
    @JsonIgnore
    private String password;

    /**
     * Nombre completo del usuario.
     */
    private String nombre;

    /**
     * Rol de autorización del usuario (ADMIN o USER).
     */
    private Rol rol;

    /**
     * Lista de canciones favoritas del usuario.
     * No se expone directamente por JSON para evitar payloads grandes.
     * El frontend recibe solo el contador de favoritos.
     */
    @JsonIgnore
    private List<Cancion> listaFavoritos;

    /**
     * Constructor sin argumentos requerido por Jackson para serialización/deserialización.
     */
    public Usuario() {
        this.listaFavoritos = new LinkedList<>();
    }

    /**
     * Constructor que inicializa un usuario con todos sus datos.
     *
     * @param username el nombre de usuario único
     * @param password la contraseña (será codificada con BCrypt en la persistencia)
     * @param nombre el nombre completo del usuario
     * @param rol el rol de autorización (ADMIN o USER)
     */
    public Usuario(String username, String password, String nombre, Rol rol) {
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.rol = rol;
        this.listaFavoritos = new LinkedList<>();
    }

    /**
     * Obtiene el nombre de usuario (identificador único).
     *
     * @return el nombre de usuario
     */
    public String getUsername() { return username; }

    /**
     * Obtiene la contraseña del usuario (nunca se expone por JSON).
     *
     * @return la contraseña
     */
    public String getPassword() { return password; }

    /**
     * Obtiene el nombre completo del usuario.
     *
     * @return el nombre
     */
    public String getNombre() { return nombre; }

    /**
     * Obtiene el rol de autorización del usuario.
     *
     * @return el rol (ADMIN o USER)
     */
    public Rol getRol() { return rol; }

    /**
     * Obtiene la lista de canciones favoritas del usuario.
     * <p>
     * Implementa defensa contra referencia nula para evitar NullPointerException
     * en código existente.
     * </p>
     *
     * @return lista de canciones favoritas
     */
    public List<Cancion> getListaFavoritos() {
        // defensa para evitar NPE en código existente
        if (listaFavoritos == null) listaFavoritos = new LinkedList<>();
        return listaFavoritos;
    }

    /**
     * Establece el nombre completo del usuario.
     *
     * @param nombre el nuevo nombre
     */
    public void setNombre(String nombre) { this.nombre = nombre; }

    /**
     * Establece la contraseña del usuario.
     *
     * @param password la nueva contraseña
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Establece el rol del usuario (nombre con typo, se mantiene por compatibilidad).
     *
     * @param rol el nuevo rol
     * @deprecated Usar {@link #setRol(Rol)} en su lugar
     */
    public void serRol(Rol rol) { this.rol = rol; }

    /**
     * Establece el rol de autorización del usuario.
     *
     * @param rol el nuevo rol (ADMIN o USER)
     */
    public void setRol(Rol rol) { this.rol = rol; }

    /**
     * Obtiene el número de canciones en favoritos del usuario.
     * <p>
     * Expuesto en JSON como {@code favoritosCount} para proporcionar
     * información al frontend sin transmitir la lista completa.
     * </p>
     *
     * @return la cantidad de favoritos
     */
    @JsonProperty("favoritosCount")
    public int getFavoritosCount() {
        return (listaFavoritos == null) ? 0 : listaFavoritos.size();
    }

    /**
     * Agrega una canción a la lista de favoritos del usuario.
     * <p>
     * No permite agregar duplicados.
     * </p>
     *
     * @param cancion la canción a agregar
     * @return {@code true} si la canción fue agregada, {@code false} si ya estaba en favoritos
     */
    public boolean agregarFavorito(Cancion cancion) {
        if (getListaFavoritos().contains(cancion)) return false;
        getListaFavoritos().add(cancion);
        return true;
    }

    /**
     * Elimina una canción de la lista de favoritos del usuario por su ID.
     *
     * @param idCancion el identificador de la canción a eliminar
     * @return {@code true} si la canción fue eliminada, {@code false} si no estaba en favoritos
     */
    public boolean eliminarFavorito(String idCancion) {
        return getListaFavoritos().removeIf(c -> c.getId().equals(idCancion));
    }

    /**
     * Verifica si el usuario tiene una canción específica en sus favoritos.
     *
     * @param idCancion el identificador de la canción a verificar
     * @return {@code true} si la canción está en favoritos, {@code false} en caso contrario
     */
    public boolean tieneEnFavoritos(String idCancion) {
        if (listaFavoritos == null) return false;
        return listaFavoritos.stream().anyMatch(c -> c.getId().equals(idCancion));
    }

    /**
     * Compara dos usuarios basándose en su nombre de usuario (RF-017).
     * <p>
     * Dos usuarios son iguales si tienen el mismo username.
     * </p>
     *
     * @param o el objeto a comparar
     * @return {@code true} si ambos usuarios tienen el mismo username, {@code false} en caso contrario
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return username != null && username.equals(usuario.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    /**
     * Genera una representación en texto segura del usuario.
     * <p>
     * No incluye password ni la lista de favoritos completa.
     * Incluye el contador de favoritos en su lugar.
     * </p>
     *
     * @return representación en texto del usuario
     */
    @Override
    public String toString() {
        return "Usuario{" +
                "username='" + username + '\'' +
                ", nombre='" + nombre + '\'' +
                ", rol=" + rol +
                ", favoritosCount=" + getFavoritosCount() +
                '}';
    }
}
