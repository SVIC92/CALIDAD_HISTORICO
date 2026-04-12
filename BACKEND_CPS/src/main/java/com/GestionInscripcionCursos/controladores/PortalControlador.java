package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/portal")
public class PortalControlador {
    
    @Autowired
    private UsuarioServicio usuarioServicio;

    @GetMapping("/")
    public ResponseEntity<?> index() {
        return ResponseEntity.ok(Map.of("mensaje", "API de GestionInscripcionCursos"));
    }
    
    @GetMapping("/registrar")
    public ResponseEntity<?> registrar() {
        return ResponseEntity.ok(Map.of("mensaje", "Endpoint para registrar usuarios"));
    }
    
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestParam String nombre, @RequestParam String email,
            @RequestParam String password, @RequestParam String password2) {

        try {
            usuarioServicio.registrar(nombre, email, password, password2);
            Usuario usuario = usuarioServicio.buscarEmail(email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "id", usuario.getId(),
                            "nombre", usuario.getNombre(),
                            "email", usuario.getEmail(),
                            "rol", usuario.getRol()));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
    
    @GetMapping("/login")
    public ResponseEntity<?> login(@RequestParam(required = false) String error) {
        if (error != null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email o Contraseña invalidos"));
        }
        return ResponseEntity.ok(Map.of("mensaje", "Login endpoint"));
    }

    @PreAuthorize("hasAnyRole('ROLE_ALUMNO', 'ROLE_PROFESOR', 'ROLE_ADMIN')")
    @GetMapping("/inicio")
    public ResponseEntity<?> inicio(HttpSession session) {

        Usuario logueado = (Usuario) session.getAttribute("usuariosession");

        if (logueado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No hay usuario autenticado en sesión"));
        }
        
        if (logueado.getRol().toString().equals("ADMIN")) {
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Logueo exitoso",
                    "rol", logueado.getRol(),
                    "redirectTo", "/admin/dashboard"));
        }
        
        if (logueado.getRol().toString().equals("PROFESOR")) {
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Logueo exitoso",
                    "rol", logueado.getRol(),
                    "redirectTo", "/profesor/dashboard"));
        }

        return ResponseEntity.ok(Map.of(
                "mensaje", "Logueo exitoso",
                "rol", logueado.getRol(),
                "redirectTo", "/inicio"));
    }

    
    
    
    
}
