package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<?> registro(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String password2,
            @RequestParam(required = false) String carrera,
            @RequestParam(required = false) Integer cicloActual,
            @RequestBody(required = false) Map<String, Object> body
    ) {

        try {
            String nombreFinal = valorTexto(nombre, body, "nombre");
            String emailFinal = valorTexto(email, body, "email");
            String passwordFinal = valorTexto(password, body, "password");
            String password2Final = valorTexto(password2, body, "password2");
            String carreraFinal = valorTexto(carrera, body, "carrera");
            Integer cicloActualFinal = valorEntero(cicloActual, body, "cicloActual");

            usuarioServicio.registrar(nombreFinal, emailFinal, passwordFinal, password2Final, carreraFinal, cicloActualFinal);
            Usuario usuario = usuarioServicio.buscarEmail(emailFinal);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "id", usuario.getId(),
                            "nombre", usuario.getNombre(),
                            "email", usuario.getEmail(),
                            "rol", usuario.getRol(),
                            "carrera", usuario.getCarrera() != null ? usuario.getCarrera().getNombre() : "",
                            "cicloActual", usuario.getCicloActual() != null ? usuario.getCicloActual() : 0));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error de integridad en base de datos al registrar usuario. Verifica email único y datos obligatorios."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al registrar usuario", "detalle", ex.getMessage()));
        }
    }

    private String valorTexto(String requestParamValue, Map<String, Object> body, String key) {
        if (requestParamValue != null && !requestParamValue.isBlank()) {
            return requestParamValue;
        }
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer valorEntero(Integer requestParamValue, Map<String, Object> body, String key) {
        if (requestParamValue != null) {
            return requestParamValue;
        }
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object value = body.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
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
