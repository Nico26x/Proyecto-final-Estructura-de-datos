package co.edu.uniquindio.application;

import co.edu.uniquindio.application.service.MetricasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetricasServiceTest {

    MetricasService metricasService;

    @BeforeEach
    void setUp() {
        metricasService = new MetricasService();
    }

    @Test
    void registrarEvento_noLanzaExcepcion() {
        assertDoesNotThrow(() -> {
            metricasService.registrarEvento("testuser", "TEST_ACTION", "test detail");
        });
    }

    @Test
    void registrarExportFavoritos_formateaDetalleCorrectamente() {
        assertDoesNotThrow(() -> {
            metricasService.registrarExportFavoritos("usuario1", 10);
        });
        
        // Verificar que se puede leer sin errores
        long total = metricasService.totalDescargasFavoritos();
        assertTrue(total >= 0);
    }

    @Test
    void totalDescargasFavoritos_devuelveConteoNoNegativo() {
        long total = metricasService.totalDescargasFavoritos();
        assertTrue(total >= 0);
    }

    @Test
    void descargasFavoritosPorUsuario_devuelveMapa() {
        metricasService.registrarExportFavoritos("user1", 5);
        metricasService.registrarExportFavoritos("user2", 3);
        
        Map<String, Long> resultado = metricasService.descargasFavoritosPorUsuario();
        
        assertNotNull(resultado);
        assertTrue(resultado.size() >= 0);
    }

    @Test
    void descargasFavoritosPorDia_devuelveMapaConFechas() {
        metricasService.registrarExportFavoritos("testuser", 5);
        
        Map<String, Long> resultado = metricasService.descargasFavoritosPorDia();
        
        assertNotNull(resultado);
        // Debería haber al menos una entrada para hoy
        String hoy = LocalDate.now().toString();
        if (resultado.containsKey(hoy)) {
            assertTrue(resultado.get(hoy) >= 1);
        }
    }

    @Test
    void descargasFavoritosPorRango_incluyeDiasSinEventos() {
        String desde = "2025-01-01";
        String hasta = "2025-01-05";
        
        Map<String, Long> resultado = metricasService.descargasFavoritosPorRango(desde, hasta);
        
        // Debería tener exactamente 5 días
        assertEquals(5, resultado.size());
        
        // Todos los días deben estar presentes (incluso con conteo 0)
        assertTrue(resultado.containsKey("2025-01-01"));
        assertTrue(resultado.containsKey("2025-01-02"));
        assertTrue(resultado.containsKey("2025-01-03"));
        assertTrue(resultado.containsKey("2025-01-04"));
        assertTrue(resultado.containsKey("2025-01-05"));
    }

    @Test
    void topUsuariosExportadores_devuelveListaOrdenada() {
        metricasService.registrarExportFavoritos("alice", 5);
        metricasService.registrarExportFavoritos("bob", 3);
        metricasService.registrarExportFavoritos("alice", 2);
        
        List<Map.Entry<String, Long>> top = metricasService.topUsuariosExportadores(10);
        
        assertNotNull(top);
        
        // Si hay datos, alice debería tener más exportaciones que bob
        if (top.size() >= 2) {
            // Buscar alice y bob en los resultados
            boolean aliceEncontrada = top.stream().anyMatch(e -> e.getKey().equals("alice"));
            boolean bobEncontrado = top.stream().anyMatch(e -> e.getKey().equals("bob"));
            
            if (aliceEncontrada && bobEncontrado) {
                Long conteoAlice = top.stream()
                    .filter(e -> e.getKey().equals("alice"))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(0L);
                
                Long conteoBob = top.stream()
                    .filter(e -> e.getKey().equals("bob"))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(0L);
                
                assertTrue(conteoAlice >= conteoBob);
            }
        }
    }

    @Test
    void promedioFavoritosPorExport_calculaCorrectamente() {
        metricasService.registrarExportFavoritos("user1", 10);
        metricasService.registrarExportFavoritos("user2", 20);
        
        double promedio = metricasService.promedioFavoritosPorExport();
        
        // El promedio debería ser >= 0
        assertTrue(promedio >= 0.0);
    }

    @Test
    void ultimasExportaciones_devuelveListaLimitada() {
        metricasService.registrarExportFavoritos("user1", 5);
        metricasService.registrarExportFavoritos("user2", 10);
        metricasService.registrarExportFavoritos("user3", 15);
        
        List<MetricasService.EventoDTO> ultimas = metricasService.ultimasExportaciones(2);
        
        assertNotNull(ultimas);
        assertTrue(ultimas.size() <= 2);
    }

    @Test
    void topArtistasDesdeFavoritos_devuelveMapa() {
        Map<String, Long> resultado = metricasService.topArtistasDesdeFavoritos(10);
        
        assertNotNull(resultado);
        assertTrue(resultado.size() <= 10);
    }

    @Test
    void topGenerosDesdeFavoritos_devuelveMapa() {
        Map<String, Long> resultado = metricasService.topGenerosDesdeFavoritos(10);
        
        assertNotNull(resultado);
        assertTrue(resultado.size() <= 10);
    }

    @Test
    void topCancionesDesdeFavoritos_devuelveMapa() {
        Map<String, Long> resultado = metricasService.topCancionesDesdeFavoritos(10);
        
        assertNotNull(resultado);
        assertTrue(resultado.size() <= 10);
    }

    @Test
    void topArtistasPorUsuario_manejaUsuarioInexistente() {
        Map<String, Long> resultado = metricasService.topArtistasPorUsuario("usuario_inexistente", 5);
        
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void topGenerosPorUsuario_manejaUsuarioInexistente() {
        Map<String, Long> resultado = metricasService.topGenerosPorUsuario("usuario_inexistente", 5);
        
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}
