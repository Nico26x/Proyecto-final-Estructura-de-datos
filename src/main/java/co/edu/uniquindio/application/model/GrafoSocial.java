package co.edu.uniquindio.application.model;

import java.io.*;
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
        // Evitar que un usuario se siga a sí mismo
        if (origen.equals(destino)) return false;

        // Verificar que ambos usuarios existen en el grafo
        if (!relaciones.containsKey(origen) || !relaciones.containsKey(destino)) return false;

        // Si ya sigue al destino, podemos retornar true, para actualizar la relación y proceder normalmente.
        if (relaciones.get(origen).contains(destino)) {
            return true;  // No hace falta agregarlo nuevamente, pero la relación ya está presente
        }

        // Agregar la relación bidireccional en el grafo
        relaciones.get(origen).add(destino);

        return true;  // La relación se creó correctamente
    }



    /**
     * Elimina la conexión entre dos usuarios (dejar de seguir).
     */
    public boolean dejarDeSeguir(String origen, String destino) {
        // Evitar que un usuario se deje de seguir a sí mismo
        if (origen.equals(destino)) return false;

        // Verificar que ambos usuarios existen en el grafo
        if (!relaciones.containsKey(origen) || !relaciones.containsKey(destino)) return false;

        // Verificar si existe una relación de "seguir"
        if (!relaciones.get(origen).contains(destino)) {
            return false;  // No existe una relación de "seguir", por lo que no se puede eliminar
        }

        // Eliminar la relación bidireccional en el grafo
        relaciones.get(origen).remove(destino);
        relaciones.get(destino).remove(origen);

        return true;  // La relación se eliminó correctamente
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

    // 🔽🔽🔽 NUEVO: Eliminar usuario del grafo 🔽🔽🔽

    /**
     * Elimina por completo un usuario del grafo y todas sus conexiones.
     * @param username usuario a eliminar
     * @return true si existía y fue eliminado; false si no estaba presente
     */
    public boolean eliminarUsuario(String username) {
        if (!relaciones.containsKey(username)) {
            return false;
        }
        // Quitar la referencia del resto de usuarios
        for (Set<String> amigos : relaciones.values()) {
            amigos.remove(username);
        }
        // Remover el nodo del usuario
        relaciones.remove(username);
        return true;
    }

    // 🔽🔽🔽 Métodos de persistencia 🔽🔽🔽

    /**
     * Guarda todas las relaciones en un archivo de texto.
     * Cada línea representa una relación bidireccional: usuario1;usuario2
     */
    public void guardarRelacionesEnArchivo(String rutaArchivo) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(rutaArchivo))) {
            for (String usuario : relaciones.keySet()) {
                for (String amigo : relaciones.get(usuario)) {
                    // Guardar todas las relaciones, sin excluir ninguna
                    bw.write(usuario + ";" + amigo);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error al guardar grafo social: " + e.getMessage());
        }
    }


    /**
     * Carga las relaciones desde un archivo existente.
     * Si el archivo no existe, no hace nada.
     */
    public void cargarRelacionesDesdeArchivo(String rutaArchivo) {
        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(";");
                if (partes.length == 2) {
                    String u1 = partes[0].trim();
                    String u2 = partes[1].trim();

                    // Crear usuarios si no existen
                    agregarUsuario(u1);
                    agregarUsuario(u2);

                    // Reconstruir la relación
                    seguirUsuario(u1, u2);
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error al cargar grafo social: " + e.getMessage());
        }
    }

    // ✅ (Opcional) Método auxiliar para inspeccionar el grafo
    public void imprimirRelaciones() {
        relaciones.forEach((usuario, amigos) -> {
            System.out.println(usuario + " -> " + amigos);
        });
    }
}
