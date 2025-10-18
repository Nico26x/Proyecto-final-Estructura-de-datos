package co.edu.uniquindio.application.model;

import java.util.LinkedList;
import java.util.Objects;

public class Usuario {
    private String username;
    private String password;
    private String nombre;
    private LinkedList<Cancion> favoritos;

    public Usuario(String username, String password, String nombre) {
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.favoritos = new LinkedList<>();
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNombre() { return nombre; }
    public LinkedList<Cancion> getFavoritos() { return favoritos; }

    public void setPassword(String password) { this.password = password; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public void agregarFavorito(Cancion cancion) {
        favoritos.add(cancion);
    }

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
