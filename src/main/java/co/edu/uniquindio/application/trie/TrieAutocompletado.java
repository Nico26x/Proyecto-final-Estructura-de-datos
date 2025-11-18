package co.edu.uniquindio.application.trie;

import java.util.ArrayList;
import java.util.List;

/**
 * Estructura de datos Trie optimizada para autocompletado de palabras.
 * <p>
 * Implementa un árbol de prefijos que permite insertar palabras y buscar
 * todas las palabras que comiencen con un prefijo específico de forma eficiente.
 * </p>
 * <p>
 * Características:
 * </p>
 * <ul>
 *   <li>Búsqueda por prefijo en tiempo O(m + n), donde m es la longitud del prefijo y n es el número de nodos resultantes</li>
 *   <li>Inserción de palabras en tiempo O(k), donde k es la longitud de la palabra</li>
 *   <li>Manejo insensible a mayúsculas/minúsculas</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
public class TrieAutocompletado {

    private final TrieNode raiz = new TrieNode();

    /**
     * Inserta una palabra en el Trie.
     * <p>
     * La palabra se convierte a minúsculas antes de insertarse.
     * Si la palabra ya existe, se marca como palabra válida.
     * </p>
     *
     * @param palabra la palabra a insertar. No debe ser {@code null}
     */
    public void insertarPalabra(String palabra) {
        TrieNode nodo = raiz;
        for (char c : palabra.toLowerCase().toCharArray()) {
            nodo = nodo.hijos.computeIfAbsent(c, k -> new TrieNode());
        }
        nodo.esFinDePalabra = true;
    }

    /**
     * Busca todas las palabras que comienzan con un prefijo específico.
     * <p>
     * La búsqueda es insensible a mayúsculas/minúsculas.
     * </p>
     *
     * @param prefijo el prefijo a buscar. No debe ser {@code null}
     * @return una lista de todas las palabras que comienzan con el prefijo especificado.
     *         Retorna una lista vacía si no hay coincidencias
     */
    public List<String> buscarPorPrefijo(String prefijo) {
        List<String> resultados = new ArrayList<>();
        TrieNode nodo = raiz;

        for (char c : prefijo.toLowerCase().toCharArray()) {
            nodo = nodo.hijos.get(c);
            if (nodo == null) {
                return resultados; // Prefijo no encontrado
            }
        }
        buscarRecursivo(nodo, new StringBuilder(prefijo.toLowerCase()), resultados);
        return resultados;
    }

    /**
     * Recorre recursivamente el Trie para recolectar todas las palabras.
     * <p>
     * Este método es una operación interna utilizada por {@link #buscarPorPrefijo(String)}.
     * </p>
     *
     * @param nodo el nodo actual del Trie
     * @param prefijo el prefijo acumulado hasta el momento
     * @param resultados lista donde se almacenan las palabras encontradas
     */
    private void buscarRecursivo(TrieNode nodo, StringBuilder prefijo, List<String> resultados) {
        if (nodo.esFinDePalabra) {
            resultados.add(prefijo.toString());
        }

        for (var entry : nodo.hijos.entrySet()) {
            prefijo.append(entry.getKey());
            buscarRecursivo(entry.getValue(), prefijo, resultados);
            prefijo.deleteCharAt(prefijo.length() - 1);
        }
    }
}
