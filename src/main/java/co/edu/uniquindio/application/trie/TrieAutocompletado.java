package co.edu.uniquindio.application.trie;

import java.util.ArrayList;
import java.util.List;

public class TrieAutocompletado {

    private final TrieNode raiz = new TrieNode();

    // ✅ Inserta una palabra en el Trie
    public void insertarPalabra(String palabra) {
        TrieNode nodo = raiz;
        for (char c : palabra.toLowerCase().toCharArray()) {
            nodo = nodo.hijos.computeIfAbsent(c, k -> new TrieNode());
        }
        nodo.esFinDePalabra = true;
    }

    // ✅ Devuelve todas las palabras que empiecen por un prefijo
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

    // 🔍 Recorre el Trie para recolectar palabras
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
