package co.edu.uniquindio.application.trie;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa un nodo en la estructura de datos Trie.
 * <p>
 * Cada nodo contiene una referencia a sus nodos hijo y un indicador que especifica
 * si el nodo actual marca el fin de una palabra válida en el Trie.
 * </p>
 *
 * @author SyncUp
 * @version 1.0
 */
public class TrieNode {
    /**
     * Mapa que almacena los nodos hijo de este nodo.
     * <p>
     * La clave es un carácter y el valor es el nodo hijo correspondiente.
     * </p>
     */
    Map<Character, TrieNode> hijos = new HashMap<>();

    /**
     * Indicador que especifica si este nodo marca el final de una palabra válida.
     * <p>
     * {@code true} si el camino desde la raíz hasta este nodo forma una palabra válida,
     * {@code false} en caso contrario.
     * </p>
     */
    boolean esFinDePalabra = false;
}
