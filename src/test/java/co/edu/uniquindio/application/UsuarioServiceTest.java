package co.edu.uniquindio.application;

import co.edu.uniquindio.application.model.Cancion;
import co.edu.uniquindio.application.model.Rol;
import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.repository.CancionRepository;
import co.edu.uniquindio.application.repository.UsuarioRepository;
import co.edu.uniquindio.application.security.JwtUtil;
import co.edu.uniquindio.application.service.CancionService;
import co.edu.uniquindio.application.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    UsuarioRepository usuarioRepository;
    CancionRepository cancionRepository;
    CancionService cancionService;
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;

    UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        cancionRepository = mock(CancionRepository.class);
        cancionService = mock(CancionService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);

        usuarioService = new UsuarioService(
                usuarioRepository,
                cancionRepository,
                cancionService,
                passwordEncoder
        );
        // inyectar jwtUtil por @Autowired field
        // (usamos reflexión por simplicidad; también podrías exponer setter)
        try {
            var f = UsuarioService.class.getDeclaredField("jwtUtil");
            f.setAccessible(true);
            f.set(usuarioService, jwtUtil);
        } catch (Exception ignored) {}
    }

    @Test
    void generarPlaylistDescubrimiento_conFavoritos_usaSimilaresYFiltraYaFavoritos() {
        String user = "nico";
        Usuario u = new Usuario(user, "pass", "Nico", Rol.USER);

        // Favorito del usuario
        Cancion fav = new Cancion("1", "Song A", "X", "Pop", 2020, 3.2);
        u.agregarFavorito(fav);

        when(usuarioRepository.buscarPorUsername(user)).thenReturn(u);

        // Similares devueltos por el servicio (uno de ellos coincide con favorito y debe filtrarse)
        Cancion s1 = new Cancion("2", "Song B", "Y", "Pop", 2021, 3.0);
        Cancion s2 = new Cancion("1", "Song A", "X", "Pop", 2020, 3.2); // ya favorito → debe excluirse
        when(cancionService.obtenerCancionesSimilares("1", 10)).thenReturn(List.of(s1, s2));

        // Repositorio de canciones para mapear por id
        when(cancionRepository.buscarPorId("2")).thenReturn(s1);
        when(cancionRepository.buscarPorId("1")).thenReturn(fav);

        List<Cancion> res = usuarioService.generarPlaylistDescubrimiento(user, 5);

        assertFalse(res.isEmpty());
        assertTrue(res.stream().noneMatch(c -> c.getId().equals("1")), "No debe incluir ya-favoritos");
        assertTrue(res.stream().anyMatch(c -> c.getId().equals("2")));
    }

    @Test
    void generarPlaylistDescubrimiento_sinFavoritos_devuelvePrimerasDelRepositorio() {
        String user = "deivid";
        Usuario u = new Usuario(user, "pass", "Deivid", Rol.USER);
        // sin favoritos
        when(usuarioRepository.buscarPorUsername(user)).thenReturn(u);

        // Repositorio devuelve catálogo
        List<Cancion> catalogo = List.of(
                new Cancion("10", "C1", "A", "Pop", 2020, 3.0),
                new Cancion("11", "C2", "B", "Rock", 2019, 4.0),
                new Cancion("12", "C3", "C", "Jazz", 2018, 5.0)
        );
        when(cancionRepository.listarCanciones()).thenReturn(catalogo);

        List<Cancion> res = usuarioService.generarPlaylistDescubrimiento(user, 2);
        assertEquals(2, res.size());
        assertEquals("10", res.get(0).getId());
        assertEquals("11", res.get(1).getId());
    }

    @Test
    void sugerirUsuariosPorFavoritos_encuentraUsuariosConFavoritosComunes() {
        // Usuario principal
        String user = "ana";
        Usuario ana = new Usuario(user, "pass", "Ana", Rol.USER);
        
        // Otros usuarios
        Usuario bob = new Usuario("bob", "pass", "Bob", Rol.USER);
        Usuario carl = new Usuario("carl", "pass", "Carl", Rol.USER);
        Usuario dave = new Usuario("dave", "pass", "Dave", Rol.USER);
        
        // Canciones
        Cancion c1 = new Cancion("1", "Song1", "Artist1", "Pop", 2020, 3.0);
        Cancion c2 = new Cancion("2", "Song2", "Artist2", "Rock", 2021, 4.0);
        Cancion c3 = new Cancion("3", "Song3", "Artist3", "Jazz", 2019, 3.5);
        
        // ana tiene c1 y c2 como favoritos
        ana.agregarFavorito(c1);
        ana.agregarFavorito(c2);
        
        // bob tiene c1 y c2 (2 coincidencias con ana)
        bob.agregarFavorito(c1);
        bob.agregarFavorito(c2);
        
        // carl tiene solo c1 (1 coincidencia con ana)
        carl.agregarFavorito(c1);
        
        // dave tiene c3 (0 coincidencias con ana)
        dave.agregarFavorito(c3);
        
        // Mock del repositorio
        when(usuarioRepository.buscarPorUsername(user)).thenReturn(ana);
        when(usuarioRepository.listarUsuarios()).thenReturn(
            Map.of("ana", ana, "bob", bob, "carl", carl, "dave", dave)
        );
        
        // Ejecutar
        List<String> sugerencias = usuarioService.sugerirUsuariosPorFavoritos(user, 3);
        
        // Verificar: bob debería estar primero (2 coincidencias), luego carl (1 coincidencia)
        assertTrue(sugerencias.size() >= 2);
        assertEquals("bob", sugerencias.get(0)); // más coincidencias
        assertEquals("carl", sugerencias.get(1)); // menos coincidencias
        assertFalse(sugerencias.contains("dave")); // sin coincidencias
        assertFalse(sugerencias.contains("ana")); // no se sugiere a sí mismo
    }

    @Test
    void exportarFavoritosCsv_generaCsvConFormatoCorrect() {
        String user = "laura";
        Usuario laura = new Usuario(user, "pass", "Laura", Rol.USER);
        
        Cancion c1 = new Cancion("101", "Himno", "Artista A", "Pop", 2020, 3.5);
        Cancion c2 = new Cancion("102", "Melodía", "Artista B", "Rock", 2021, 4.2);
        laura.agregarFavorito(c1);
        laura.agregarFavorito(c2);
        
        when(usuarioRepository.buscarPorUsername(user)).thenReturn(laura);
        // Mock para listarFavoritos que es llamado por exportarFavoritosCsv
        when(usuarioRepository.listarFavoritos(user)).thenReturn(laura.getListaFavoritos());
        
        byte[] csv = usuarioService.exportarFavoritosCsv(user);
        String contenido = new String(csv);
        
        // Verificar encabezado
        assertTrue(contenido.contains("id,titulo,artista,genero,anio,duracion_seg"));
        // Verificar datos
        assertTrue(contenido.contains("101,Himno,Artista A,Pop,2020,3.5"));
        assertTrue(contenido.contains("102,Melodía,Artista B,Rock,2021,4.2"));
    }
}
