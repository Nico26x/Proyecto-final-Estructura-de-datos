package co.edu.uniquindio.application.model;

import java.util.*;

public class GrafoDeSimilitud {

    private final Map<Cancion, Map<Cancion, Double>> grafo = new HashMap<>();

    // ✅ Construir el grafo basándose en similitud de género, artista o año cercano
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

    // ✅ Regla de similitud (ajustable)
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

    // ✅ Obtener las canciones más similares a una canción dada
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
