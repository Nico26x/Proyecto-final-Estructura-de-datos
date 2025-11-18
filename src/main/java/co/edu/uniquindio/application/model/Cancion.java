package co.edu.uniquindio.application.model;

import java.util.Objects;

/**
 * Modelo que representa una canción en el sistema.
 * <p>
 * Contiene información sobre una canción incluyendo metadatos como título, artista,
 * género, año y duración. Opcionalmente puede almacenar una referencia al archivo MP3
 * correspondiente en el frontend.
 * </p>
 * <p>
 * La clase soporta serialización a formato de texto con 6 o 7 columnas separadas por
 * punto y coma para persistencia en archivo.
 * </p>
 *
 * @author SyncUp
 * @version 1.0
 */
public class Cancion {
    /**
     * Identificador único de la canción.
     */
    private String id;

    /**
     * Título de la canción.
     */
    private String titulo;

    /**
     * Nombre del artista que interpreta la canción.
     */
    private String artista;

    /**
     * Género musical de la canción.
     */
    private String genero;

    /**
     * Año de lanzamiento o creación de la canción.
     */
    private int anio;

    /**
     * Duración de la canción en minutos.
     */
    private double duracion;

    /**
     * Nombre del archivo MP3 asociado (ej: "song1.mp3").
     * Este campo es opcional y referencia archivos ubicados en la carpeta
     * {@code public/music} del frontend.
     */
    private String fileName;

    /**
     * Constructor sin argumentos (para deserialización).
     */
    public Cancion() {}

    /**
     * Constructor para crear una canción con 6 atributos (sin fileName).
     *
     * @param id el identificador único de la canción
     * @param titulo el título de la canción
     * @param artista el nombre del artista
     * @param genero el género musical
     * @param anio el año de lanzamiento
     * @param duracion la duración en minutos
     */
    public Cancion(String id, String titulo, String artista, String genero, int anio, double duracion) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.genero = genero;
        this.anio = anio;
        this.duracion = duracion;
    }

    /**
     * Constructor para crear una canción con 7 atributos (incluyendo fileName).
     *
     * @param id el identificador único de la canción
     * @param titulo el título de la canción
     * @param artista el nombre del artista
     * @param genero el género musical
     * @param anio el año de lanzamiento
     * @param duracion la duración en minutos
     * @param fileName el nombre del archivo MP3 asociado
     */
    public Cancion(String id, String titulo, String artista, String genero, int anio, double duracion, String fileName) {
        this(id, titulo, artista, genero, anio, duracion);
        this.fileName = fileName;
    }

    /**
     * Obtiene el identificador único de la canción.
     *
     * @return el ID de la canción
     */
    public String getId() { return id; }

    /**
     * Establece el identificador de la canción.
     *
     * @param id el nuevo ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Obtiene el título de la canción.
     *
     * @return el título
     */
    public String getTitulo() { return titulo; }

    /**
     * Establece el título de la canción.
     *
     * @param titulo el nuevo título
     */
    public void setTitulo(String titulo) { this.titulo = titulo; }

    /**
     * Obtiene el nombre del artista.
     *
     * @return el nombre del artista
     */
    public String getArtista() { return artista; }

    /**
     * Establece el nombre del artista.
     *
     * @param artista el nuevo nombre del artista
     */
    public void setArtista(String artista) { this.artista = artista; }

    /**
     * Obtiene el género musical de la canción.
     *
     * @return el género
     */
    public String getGenero() { return genero; }

    /**
     * Establece el género musical de la canción.
     *
     * @param genero el nuevo género
     */
    public void setGenero(String genero) { this.genero = genero; }

    /**
     * Obtiene el año de lanzamiento de la canción.
     *
     * @return el año
     */
    public int getAnio() { return anio; }

    /**
     * Establece el año de lanzamiento de la canción.
     *
     * @param anio el nuevo año
     */
    public void setAnio(int anio) { this.anio = anio; }

    /**
     * Obtiene la duración de la canción.
     *
     * @return la duración en minutos
     */
    public double getDuracion() { return duracion; }

    /**
     * Establece la duración de la canción.
     *
     * @param duracion la nueva duración en minutos
     */
    public void setDuracion(double duracion) { this.duracion = duracion; }

    /**
     * Obtiene el nombre del archivo MP3 asociado.
     *
     * @return el nombre del archivo, o {@code null} si no está definido
     */
    public String getFileName() { return fileName; }

    /**
     * Establece el nombre del archivo MP3 asociado.
     *
     * @param fileName el nuevo nombre del archivo
     */
    public void setFileName(String fileName) { this.fileName = fileName; }

    /**
     * Compara dos objetos Cancion basándose en su identificador único.
     *
     * @param o el objeto a comparar
     * @return {@code true} si ambos objetos tienen el mismo ID, {@code false} en caso contrario
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cancion)) return false;
        Cancion cancion = (Cancion) o;
        return Objects.equals(id, cancion.id);
    }

    /**
     * Genera el código hash de la canción basado en su identificador único.
     *
     * @return el código hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Convierte la canción a su representación en formato texto.
     * <p>
     * Genera una línea compatible con el archivo canciones.txt:
     * <ul>
     *   <li>Con fileName: {@code id;titulo;artista;genero;anio;duracion;fileName}</li>
     *   <li>Sin fileName: {@code id;titulo;artista;genero;anio;duracion}</li>
     * </ul>
     * </p>
     *
     * @return la representación en texto de la canción
     */
    @Override
    public String toString() {
        // Compatibilidad: si hay fileName, guardamos 7 columnas; si no, las 6 de siempre
        if (fileName != null && !fileName.isBlank()) {
            return id + ";" + titulo + ";" + artista + ";" + genero + ";" + anio + ";" + duracion + ";" + fileName;
        }
        return id + ";" + titulo + ";" + artista + ";" + genero + ";" + anio + ";" + duracion;
    }

    /**
     * Convierte una línea del archivo canciones.txt a un objeto Cancion.
     * <p>
     * Soporta dos formatos:
     * <ul>
     *   <li>6 campos: {@code id;titulo;artista;genero;anio;duracion}</li>
     *   <li>7 campos: {@code id;titulo;artista;genero;anio;duracion;fileName}</li>
     * </ul>
     * </p>
     * <p>
     * Si algún campo numérico no puede ser parseado, retorna {@code null} y registra
     * un error en la consola.
     * </p>
     *
     * @param linea la línea de texto a convertir
     * @return un objeto Cancion si la conversión es exitosa, {@code null} en caso contrario
     */
    public static Cancion fromString(String linea) {
        if (linea == null || linea.trim().isEmpty()) return null;
        String[] partes = linea.split(";");
        if (partes.length < 6) return null;

        try {
            String id = partes[0].trim();
            String titulo = partes[1].trim();
            String artista = partes[2].trim();
            String genero = partes[3].trim();
            int anio = Integer.parseInt(partes[4].trim());
            double duracion = Double.parseDouble(partes[5].trim().replace(",", "."));

            Cancion c = new Cancion(id, titulo, artista, genero, anio, duracion);
            if (partes.length >= 7) {
                c.setFileName(partes[6].trim());
            }
            return c;
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
