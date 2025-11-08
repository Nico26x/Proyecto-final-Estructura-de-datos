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
 * Modelo de usuario del sistema.
 * - No expone password ni la lista completa de favoritos por JSON.
 * - Expone un contador de favoritos (favoritosCount) útil para el front.
 * - Incluye constructor vacío para compatibilidad con Jackson.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "username", "nombre", "rol", "favoritosCount" })
public class Usuario {

    private String username;

    /** Nunca exponer por JSON */
    @JsonIgnore
    private String password;

    private String nombre;
    private Rol rol;

    /** No exponer la lista por JSON para evitar payloads grandes y datos innecesarios */
    @JsonIgnore
    private List<Cancion> listaFavoritos;

    /** Constructor vacío requerido por Jackson/serialización */
    public Usuario() {
        this.listaFavoritos = new LinkedList<>();
    }

    public Usuario(String username, String password, String nombre, Rol rol) {
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.rol = rol;
        this.listaFavoritos = new LinkedList<>();
    }

    // ========= Getters y setters =========
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNombre() { return nombre; }
    public Rol getRol() { return rol; }

    public List<Cancion> getListaFavoritos() {
        // defensa para evitar NPE en código existente
        if (listaFavoritos == null) listaFavoritos = new LinkedList<>();
        return listaFavoritos;
    }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPassword(String password) { this.password = password; }

    // Mantengo tu método original (posible typo) para no romper llamadas antiguas
    public void serRol(Rol rol) { this.rol = rol; }

    // Nuevo setter correcto (no rompe compatibilidad)
    public void setRol(Rol rol) { this.rol = rol; }

    // ========= Proyección segura para el front =========

    /** Contador expuesto al front en lugar de la lista completa */
    @JsonProperty("favoritosCount")
    public int getFavoritosCount() {
        return (listaFavoritos == null) ? 0 : listaFavoritos.size();
    }

    // ========= Métodos de favoritos =========
    public boolean agregarFavorito(Cancion cancion) {
        if (getListaFavoritos().contains(cancion)) return false;
        getListaFavoritos().add(cancion);
        return true;
    }

    public boolean eliminarFavorito(String idCancion) {
        return getListaFavoritos().removeIf(c -> c.getId().equals(idCancion));
    }

    public boolean tieneEnFavoritos(String idCancion) {
        if (listaFavoritos == null) return false;
        return listaFavoritos.stream().anyMatch(c -> c.getId().equals(idCancion));
    }

    // ✅ equals/hashCode basados en username (RF-017)
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

    // toString seguro (no incluye password ni favoritos)
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
