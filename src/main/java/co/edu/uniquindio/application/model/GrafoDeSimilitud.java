package co.edu.uniquindio.application.model;

import java.util.*;

/**
 * Grafo ponderado que representa la similitud entre canciones.
 * <p>
 * Implementa un modelo de recomendación basado en similitud de metadatos
 * (género, artista y año). Las aristas del grafo tienen pesos que indican
 * el grado de similitud entre dos canciones.
 * </p>
 * <p>
 * La similitud se calcula mediante una función configurable que considera:
 * <ul>
 *   <li>Mismo género: +0.6</li>
 *   <li>Mismo artista: +0.3</li>
 *   <li>Año cercano (±2): +0.1</li>
 * </ul>
 * </p>
 *
 * @author SyncUp
 * @version 1.0
 */
public class GrafoDeSimilitud {

    /**
     * Almacenamiento del grafo: mapa de canciones a mapas de adyacentes con sus pesos.
     * Estructura: {@code grafo[nodo] = {nodo_adyacente -> peso_similitud, ...}}
     */
    private final Map<Cancion, Map<Cancion, Double>> grafo = new HashMap<>();

    /**
     * Construye el grafo de similitud a partir de una colección de canciones.
     * <p>
     * Realiza comparaciones entre todos los pares de canciones y establece aristas
     * bidireccionales para aquellos pares cuya similitud sea mayor a 0.
     * </p>
     * <p>
     * Complejidad: O(n²) donde n es la cantidad de canciones.
     * </p>
     *
     * @param canciones la colección de canciones para construir el grafo
     */
    public void construirGrafo(Collection<Cancion> canciones) {
        grafo.clear();

        List<Cancion> lista = new ArrayList<>(canciones);

        for (int i = 0; i < lista.size(); i++) {
            Cancion a = lista.get(i);
            grafo.putIfAbsent(a, new HashMap<>());

            for (int j = i + 1; j < lista.size(); j++) {
                Cancion b = lista.get(j);

                double peso = calcularSimilitud(a, b);
                if (peso > 0) {
                    grafo.get(a).put(b, peso);
                    grafo.putIfAbsent(b, new HashMap<>());
                    grafo.get(b).put(a, peso);
                }
            }
        }
    }

    /**
     * Calcula el peso de similitud entre dos canciones.
     * <p>
     * Utiliza una función de similitud basada en:
     * <ul>
     *   <li>Coincidencia de género (case-insensitive): 0.6</li>
     *   <li>Coincidencia de artista (case-insensitive): 0.3</li>
     *   <li>Años cercanos (diferencia ≤ 2): 0.1</li>
     * </ul>
     * </p>
     * <p>
     * La similitud total es la suma de todos los criterios que se cumplen,
     * con un valor máximo de 1.0.
     * </p>
     *
     * @param a la primera canción
     * @param b la segunda canción
     * @return el peso de similitud entre 0.0 y 1.0
     */
    private double calcularSimilitud(Cancion a, Cancion b) {
        double similitud = 0.0;

        // Mismo género: +0.6
        if (a.getGenero().equalsIgnoreCase(b.getGenero())) similitud += 0.6;

        // Mismo artista: +0.3
        if (a.getArtista().equalsIgnoreCase(b.getArtista())) similitud += 0.3;

        // Año cercano (±2): +0.1
        if (Math.abs(a.getAnio() - b.getAnio()) <= 2) similitud += 0.1;

        return similitud;
    }

    /**
     * Obtiene las canciones más similares a una canción dada, ordenadas por similitud descendente.
     * <p>
     * Busca la canción en el grafo y retorna sus adyacentes ordenados por peso (similitud)
     * de mayor a menor, limitando el resultado a una cantidad especificada.
     * </p>
     * <p>
     * Complejidad: O(k log k) donde k es el número de vecinos de la canción origen.
     * </p>
     *
     * @param origen la canción de referencia
     * @param limite la cantidad máxima de canciones similares a retornar
     * @return lista de canciones similares ordenadas de mayor a menor similitud,
     *         o lista vacía si la canción no existe en el grafo
     */
    public List<Cancion> obtenerSimilares(Cancion origen, int limite) {
        if (!grafo.containsKey(origen)) return List.of();

        Map<Cancion, Double> adyacentes = grafo.get(origen);

        return adyacentes.entrySet()
                .stream()
                .sorted(Map.Entry.<Cancion, Double>comparingByValue().reversed())
                .limit(limite)
                .map(Map.Entry::getKey)
                .toList();
    }
}
