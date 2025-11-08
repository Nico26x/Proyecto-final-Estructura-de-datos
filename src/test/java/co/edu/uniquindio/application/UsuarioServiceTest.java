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
}
