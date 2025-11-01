package co.edu.uniquindio.application.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Usuario {

    private String username;
    private String password;
    private String nombre;
    private List<Cancion> listaFavoritos;

    public Usuario(String username, String password, String nombre) {
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.listaFavoritos = new LinkedList<>();
    }

    // ✅ Getters y setters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNombre() { return nombre; }
    public List<Cancion> getListaFavoritos() { return listaFavoritos; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPassword(String password) { this.password = password; }

    // ✅ Métodos de favoritos
    public boolean agregarFavorito(Cancion cancion) {
        if (!listaFavoritos.contains(cancion)) {
            listaFavoritos.add(cancion);
            return true;
        }
        return false;
    }

    public boolean eliminarFavorito(String idCancion) {
        return listaFavoritos.removeIf(c -> c.getId().equals(idCancion));
    }

    // ✅ equals/hashCode basados en username (RF-017)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return username.equals(usuario.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
