package co.edu.uniquindio.application;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.repository.CancionRepository;
import co.edu.uniquindio.application.service.CancionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancionServiceTest {

    CancionRepository cancionRepository;
    CancionService cancionService;

    @BeforeEach
    void setUp() {
        cancionRepository = mock(CancionRepository.class);
        
        // Mock de canciones para inicialización
        when(cancionRepository.listarCanciones()).thenReturn(List.of(
            new Cancion("1", "Despacito", "Luis Fonsi", "Pop", 2017, 3.5),
            new Cancion("2", "Shape of You", "Ed Sheeran", "Pop", 2017, 4.0),
            new Cancion("3", "Bohemian Rhapsody", "Queen", "Rock", 1975, 6.0)
        ));
        
        cancionService = new CancionService(cancionRepository);
    }

    @Test
    void autocompletarTitulo_encuentraCoindicencias() {
        // Buscar por prefijo "De"
        List<String> resultados = cancionService.autocompletarTitulo("De");
        
        // Debería encontrar "Despacito"
        assertTrue(resultados.size() > 0);
        assertTrue(resultados.stream().anyMatch(t -> t.equalsIgnoreCase("Despacito")));
    }

    @Test
    void autocompletarTitulo_noCaseSensitive() {
        // Buscar con mayúsculas y minúsculas
        List<String> resultados1 = cancionService.autocompletarTitulo("sha");
        List<String> resultados2 = cancionService.autocompletarTitulo("SHA");
        
        // Ambos deberían encontrar "Shape of You"
        assertFalse(resultados1.isEmpty());
        assertFalse(resultados2.isEmpty());
    }

    @Test
    void buscarPorId_devuelveCancionCorrecta() {
        Cancion esperada = new Cancion("10", "Test Song", "Artist", "Pop", 2020, 3.0);
        when(cancionRepository.buscarPorId("10")).thenReturn(esperada);
        
        Cancion resultado = cancionService.buscarPorId("10");
        
        assertNotNull(resultado);
        assertEquals("Test Song", resultado.getTitulo());
        assertEquals("Artist", resultado.getArtista());
    }

    @Test
    void agregarCancion_actualizaTrieYGrafo() {
        Cancion nueva = new Cancion("20", "Nueva Cancion", "Artista", "Jazz", 2023, 4.2);
        
        cancionService.agregarCancion(nueva);
        
        // Verificar que se agregó al repositorio
        verify(cancionRepository, times(1)).agregarCancion(nueva);
        
        // Verificar que se puede autocompletar (el Trie devuelve en minúsculas)
        List<String> resultados = cancionService.autocompletarTitulo("Nueva");
        assertTrue(resultados.stream().anyMatch(t -> t.equalsIgnoreCase("Nueva Cancion")));
    }

    @Test
    void obtenerCancionesSimilares_retornaListaVacia_cuandoNoExisteCancion() {
        when(cancionRepository.buscarPorId("999")).thenReturn(null);
        
        List<Cancion> similares = cancionService.obtenerCancionesSimilares("999", 5);
        
        assertTrue(similares.isEmpty());
    }

    @Test
    void iniciarRadio_incluyeCancionOriginalAlInicio() {
        Cancion origen = new Cancion("1", "Despacito", "Luis Fonsi", "Pop", 2017, 3.5);
        when(cancionRepository.buscarPorId("1")).thenReturn(origen);
        
        List<Cancion> radio = cancionService.iniciarRadio("1", 5);
        
        // La primera canción debe ser la original
        assertFalse(radio.isEmpty());
        assertEquals("Despacito", radio.get(0).getTitulo());
    }

    @Test
    void buscarAvanzada_llamaRepositorioConParametrosCorrectos() {
        cancionService.buscarAvanzada("Test", "Artist", "Rock", 2000, 2020, "AND");
        
        verify(cancionRepository, times(1))
            .buscarAvanzadaConcurrente("Test", "Artist", "Rock", 2000, 2020, "AND");
    }

    @Test
    void eliminarCancion_actualizaTrieYGrafo() {
        when(cancionRepository.eliminarCancion("1")).thenReturn(true);
        
        boolean resultado = cancionService.eliminarCancion("1");
        
        assertTrue(resultado);
        verify(cancionRepository, times(1)).eliminarCancion("1");
        // El Trie debería reconstruirse después de la eliminación
        verify(cancionRepository, atLeast(2)).listarCanciones();
    }
}
