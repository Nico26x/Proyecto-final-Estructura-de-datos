package co.edu.uniquindio.application.model;

import java.util.*;

/**
 * Grafo no dirigido que modela las relaciones entre usuarios.
 * Permite seguir/dejar de seguir y obtener sugerencias mediante BFS.
 */
public class GrafoSocial {

    private final Map<String, Set<String>> relaciones = new HashMap<>();

    /**
     * Agrega un nuevo usuario al grafo (sin conexiones iniciales).
     */
    public void agregarUsuario(String username) {
        relaciones.putIfAbsent(username, new HashSet<>());
    }

    /**
     * Crea una conexión bidireccional entre dos usuarios.
     */
    public boolean seguirUsuario(String origen, String destino) {
        if (origen.equals(destino)) return false;
        if (!relaciones.containsKey(origen) || !relaciones.containsKey(destino)) return false;

        relaciones.get(origen).add(destino);
        relaciones.get(destino).add(origen);
        return true;
    }

    /**
     * Elimina la conexión entre dos usuarios (dejar de seguir).
     */
    public boolean dejarDeSeguir(String origen, String destino) {
        if (!relaciones.containsKey(origen) || !relaciones.containsKey(destino)) return false;

        relaciones.get(origen).remove(destino);
        relaciones.get(destino).remove(origen);
        return true;
    }

    /**
     * Obtiene los usuarios seguidos por un usuario.
     */
    public Set<String> obtenerAmigos(String username) {
        return relaciones.getOrDefault(username, Collections.emptySet());
    }

    /**
     * Sugerencias de amistad usando BFS (amigos de amigos no seguidos aún).
     */
    public List<String> sugerirUsuarios(String username, int limite) {
        if (!relaciones.containsKey(username)) return Collections.emptyList();

        Set<String> visitados = new HashSet<>();
        Queue<String> cola = new LinkedList<>();
        List<String> sugerencias = new ArrayList<>();

        cola.add(username);
        visitados.add(username);

        while (!cola.isEmpty() && sugerencias.size() < limite) {
            String actual = cola.poll();
            for (String vecino : relaciones.getOrDefault(actual, Collections.emptySet())) {
                if (!visitados.contains(vecino)) {
                    visitados.add(vecino);
                    cola.add(vecino);

                    if (!relaciones.get(username).contains(vecino) && !vecino.equals(username)) {
                        sugerencias.add(vecino);
                        if (sugerencias.size() >= limite) break;
                    }
                }
            }
        }

        return sugerencias;
    }
}
