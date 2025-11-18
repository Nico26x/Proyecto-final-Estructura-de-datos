package co.edu.uniquindio.application.model;

import java.io.*;
import java.util.*;

/**
 * Grafo no dirigido que modela las relaciones sociales entre usuarios.
 * <p>
 * Permite gestionar conexiones bidireccionales entre usuarios (seguir/dejar de seguir),
 * obtener amigos de un usuario y generar sugerencias de amistad mediante búsqueda en anchura (BFS).
 * </p>
 * <p>
 * Características:
 * </p>
 * <ul>
 *   <li>Operaciones de seguir/dejar de seguir con validación</li>
 *   <li>Sugerencias de amigos mediante BFS (amigos de amigos no seguidos)</li>
 *   <li>Persistencia en archivo de texto</li>
 *   <li>Eliminación completa de usuarios y sus relaciones</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
public class GrafoSocial {

    /**
     * Almacenamiento del grafo no dirigido: mapa de usuarios a sus conjuntos de amigos.
     */
    private final Map<String, Set<String>> relaciones = new HashMap<>();

    /**
     * Agrega un nuevo usuario al grafo sin conexiones iniciales.
     * <p>
     * Si el usuario ya existe, no realiza cambios.
     * </p>
     *
     * @param username el nombre del usuario a agregar
     */
    public void agregarUsuario(String username) {
        relaciones.putIfAbsent(username, new HashSet<>());
    }

    /**
     * Crea una conexión bidireccional entre dos usuarios (seguir).
     * <p>
     * Validaciones:
     * <ul>
     *   <li>Un usuario no puede seguirse a sí mismo</li>
     *   <li>Ambos usuarios deben existir en el grafo</li>
     *   <li>Si ya existe la relación, devuelve {@code true} sin cambios</li>
     * </ul>
     * </p>
     *
     * @param origen el usuario que sigue
     * @param destino el usuario a seguir
     * @return {@code true} si la relación se creó o ya existía, {@code false} si falló la validación
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
     * Elimina la conexión bidireccional entre dos usuarios (dejar de seguir).
     * <p>
     * Validaciones:
     * <ul>
     *   <li>Un usuario no puede dejar de seguirse a sí mismo</li>
     *   <li>Ambos usuarios deben existir en el grafo</li>
     *   <li>Debe existir una relación previa para poder eliminarla</li>
     * </ul>
     * </p>
     *
     * @param origen el usuario que deja de seguir
     * @param destino el usuario al que se deja de seguir
     * @return {@code true} si la relación fue eliminada exitosamente, {@code false} si falló la validación
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
     * Obtiene el conjunto de usuarios seguidos por un usuario.
     *
     * @param username el nombre del usuario
     * @return conjunto no modificable de amigos, o conjunto vacío si el usuario no existe
     */
    public Set<String> obtenerAmigos(String username) {
        return relaciones.getOrDefault(username, Collections.emptySet());
    }

    /**
     * Genera sugerencias de amistad para un usuario mediante búsqueda en anchura (BFS).
     * <p>
     * Busca usuarios que estén a dos niveles de distancia (amigos de amigos) y que
     * el usuario no sigue actualmente. El resultado se limita a una cantidad especificada.
     * </p>
     * <p>
     * Complejidad: O(V + E) donde V es la cantidad de usuarios y E es la cantidad de relaciones.
     * </p>
     *
     * @param username el usuario para el cual generar sugerencias
     * @param limite la cantidad máxima de sugerencias a retornar
     * @return lista de usuarios sugeridos, o lista vacía si el usuario no existe
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

    /**
     * Elimina un usuario completamente del grafo y todas sus conexiones.
     * <p>
     * Busca el usuario en el mapa de relaciones y lo elimina junto con todas
     * las referencias que otros usuarios tienen hacia él.
     * </p>
     *
     * @param username el nombre del usuario a eliminar
     * @return {@code true} si el usuario existía y fue eliminado, {@code false} si no estaba presente
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

    /**
     * Guarda todas las relaciones del grafo en un archivo de texto.
     * <p>
     * Cada línea representa una relación unidireccional en formato:
     * <code>usuario1;usuario2</code>
     * </p>
     * <p>
     * Si ocurre un error de I/O, registra un mensaje de error pero no lanza excepción.
     * </p>
     *
     * @param rutaArchivo la ruta del archivo donde guardar las relaciones
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
     * Carga las relaciones del grafo desde un archivo de persistencia.
     * <p>
     * Lee líneas del archivo en formato {@code usuario1;usuario2} y reconstruye
     * el grafo. Crea automáticamente los usuarios si no existen.
     * Si el archivo no existe, no realiza cambios.
     * </p>
     * <p>
     * Si ocurre un error de I/O, registra un mensaje de error pero no lanza excepción.
     * </p>
     *
     * @param rutaArchivo la ruta del archivo a cargar
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

    /**
     * Método auxiliar para imprimir el estado actual del grafo en consola.
     * <p>
     * Útil para depuración. Imprime cada usuario y sus amigos en el formato:
     * <code>usuario -&gt; [amigo1, amigo2, ...]</code>
     * </p>
     */
    public void imprimirRelaciones() {
        relaciones.forEach((usuario, amigos) -> {
            System.out.println(usuario + " -> " + amigos);
        });
    }
}
