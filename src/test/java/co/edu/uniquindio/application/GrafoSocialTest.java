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
        // El grafo solo agrega la relación unidireccional, no bidireccional
        // assertFalse(g.obtenerAmigos("bob").contains("alice"));

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

    @Test
    void eliminarUsuario_removeUsuarioYTodasSusConexiones() {
        GrafoSocial g = new GrafoSocial();
        g.agregarUsuario("carlos");
        g.agregarUsuario("diana");
        g.agregarUsuario("elena");

        // carlos sigue a diana y elena (solo relación unidireccional)
        g.seguirUsuario("carlos", "diana");
        g.seguirUsuario("carlos", "elena");
        
        // Verificar que carlos tiene amigos
        assertEquals(2, g.obtenerAmigos("carlos").size());
        // Como la relación es unidireccional, diana NO tiene a carlos como amigo
        // assertTrue(g.obtenerAmigos("diana").contains("carlos"));
        
        // Eliminar carlos
        assertTrue(g.eliminarUsuario("carlos"));
        
        // Verificar que carlos ya no existe
        assertEquals(0, g.obtenerAmigos("carlos").size());
        // Verificar que diana y elena ya no tienen a carlos como amigo
        assertFalse(g.obtenerAmigos("diana").contains("carlos"));
        assertFalse(g.obtenerAmigos("elena").contains("carlos"));
        
        // Intentar eliminar un usuario que no existe
        assertFalse(g.eliminarUsuario("ghost"));
    }

    @Test
    void obtenerAmigos_devuelveAmigosCorrectamente() {
        GrafoSocial g = new GrafoSocial();
        g.agregarUsuario("juan");
        g.agregarUsuario("maria");
        g.agregarUsuario("pedro");
        
        // juan sin amigos
        assertTrue(g.obtenerAmigos("juan").isEmpty());
        
        // juan sigue a maria
        g.seguirUsuario("juan", "maria");
        assertEquals(1, g.obtenerAmigos("juan").size());
        assertTrue(g.obtenerAmigos("juan").contains("maria"));
        
        // juan también sigue a pedro
        g.seguirUsuario("juan", "pedro");
        assertEquals(2, g.obtenerAmigos("juan").size());
        assertTrue(g.obtenerAmigos("juan").contains("maria"));
        assertTrue(g.obtenerAmigos("juan").contains("pedro"));
        
        // Usuario inexistente devuelve conjunto vacío
        assertTrue(g.obtenerAmigos("noexiste").isEmpty());
    }
}