package co.edu.uniquindio.application.controller;

import co.edu.uniquindio.application.model.Usuario;
import co.edu.uniquindio.application.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:3000")  // para conectar con React
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    public String registrar(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String nombre) {
        boolean creado = usuarioService.registrarUsuario(username, password, nombre);
        return creado ? "Usuario registrado correctamente" : "El usuario ya existe";
    }

    @PostMapping("/login")
    public Usuario login(@RequestParam String username,
                         @RequestParam String password) {
        return usuarioService.login(username, password);
    }
}
