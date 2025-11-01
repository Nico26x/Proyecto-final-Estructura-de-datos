package co.edu.uniquindio.application.model;

import java.util.Objects;

public class Cancion {
    private String id;
    private String titulo;
    private String artista;
    private String genero;
    private int anio;
    private double duracion;

    public Cancion() {}

    public Cancion(String id, String titulo, String artista, String genero, int anio, double duracion) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.genero = genero;
        this.anio = anio;
        this.duracion = duracion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getArtista() { return artista; }
    public void setArtista(String artista) { this.artista = artista; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public double getDuracion() { return duracion; }
    public void setDuracion(double duracion) { this.duracion = duracion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cancion)) return false;
        Cancion cancion = (Cancion) o;
        return Objects.equals(id, cancion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + ";" + titulo + ";" + artista + ";" + genero + ";" + anio + ";" + duracion;
    }

    /**
     * Convierte una línea del archivo canciones.txt a un objeto Cancion.
     * Formato esperado: id;titulo;artista;genero;anio;duracion
     */
    public static Cancion fromString(String linea) {
        if (linea == null || linea.trim().isEmpty()) return null;
        String[] partes = linea.split(";");
        if (partes.length != 6) return null;

        try {
            String id = partes[0].trim();
            String titulo = partes[1].trim();
            String artista = partes[2].trim();
            String genero = partes[3].trim();
            int anio = Integer.parseInt(partes[4].trim());
            double duracion = Double.parseDouble(partes[5].trim());
            return new Cancion(id, titulo, artista, genero, anio, duracion);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
