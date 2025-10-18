package co.edu.uniquindio.application.model;

import java.util.Objects;

public class Cancion {
    private String id;
    private String titulo;
    private String artista;
    private String genero;
    private int anio;
    private double duracion;

    public Cancion(String id, String titulo, String artista, String genero, int anio, double duracion) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.genero = genero;
        this.anio = anio;
        this.duracion = duracion;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getArtista() { return artista; }
    public String getGenero() { return genero; }
    public int getAnio() { return anio; }
    public double getDuracion() { return duracion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cancion)) return false;
        Cancion cancion = (Cancion) o;
        return id.equals(cancion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
