package co.edu.uniquindio.application;

import co.edu.uniquindio.application.model.GrafoSocial;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GrafoSocialTest {

    @Test
    void seguirYDejarDeSeguir_creaYEliminaConexionBidireccional() {
        GrafoSocial g = new GrafoSocial();
        g.agregarUsuario("alice");
        g.agregarUsuario("bob");

        assertTrue(g.seguirUsuario("alice", "bob"));
        assertTrue(g.obtenerAmigos("alice").contains("bob"));
        assertTrue(g.obtenerAmigos("bob").contains("alice"));

        assertTrue(g.dejarDeSeguir("alice", "bob"));
        assertFalse(g.obtenerAmigos("alice").contains("bob"));
        assertFalse(g.obtenerAmigos("bob").contains("alice"));
    }

    @Test
    void seguirUsuario_noPermiteSeguirseASiMismoNiUsuariosInexistentes() {
        GrafoSocial g = new GrafoSocial();
        g.agregarUsuario("nico");

        assertFalse(g.seguirUsuario("nico", "nico")); // mismo usuario
        assertFalse(g.seguirUsuario("nico", "deivid")); // destino no existe
        assertFalse(g.seguirUsuario("ghost", "nico"));  // origen no existe
    }

    @Test
    void sugerirUsuarios_bfsAmigosDeAmigos() {
        GrafoSocial g = new GrafoSocial();
        g.agregarUsuario("a");
        g.agregarUsuario("b");
        g.agregarUsuario("c");
        g.agregarUsuario("d");
        g.agregarUsuario("e");

        // a-b-c-d-e cadena
        g.seguirUsuario("a", "b");
        g.seguirUsuario("b", "c");
        g.seguirUsuario("c", "d");
        g.seguirUsuario("d", "e");

        // Para "a": amigos directos = {b}. Sugerencias esperadas: {c} (amigo de amigo)
        List<String> sug = g.sugerirUsuarios("a", 10);
        assertTrue(sug.contains("c"));
        assertFalse(sug.contains("a"));
        assertFalse(sug.contains("b")); // ya es amigo
    }
}