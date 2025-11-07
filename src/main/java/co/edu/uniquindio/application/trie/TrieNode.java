package co.edu.uniquindio.application.trie;

import java.util.HashMap;
import java.util.Map;

public class TrieNode {
    Map<Character, TrieNode> hijos = new HashMap<>();
    boolean esFinDePalabra = false;
}
