package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.UsuarioAdminRequestDto;
import com.GestionInscripcionCursos.dto.UsuarioResumenDto;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminControlador {

    @Autowired
    private UsuarioServicio usuarioServicio;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<?> panelAdministrativo() {
        return ResponseEntity.ok(Map.of("mensaje", "Dashboard admin"));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResumenDto>> listarUsuarios() {
        return ResponseEntity.ok(usuarioServicio.listarUsuarios());
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<?> obtenerUsuario(@PathVariable String id) {
        try {
            Usuario usuario = usuarioServicio.buscarPorId(id);
            return ResponseEntity.ok(mapearUsuario(usuario));
        } catch (MyException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioAdminRequestDto request) {
        try {
            Usuario usuario = usuarioServicio.crearUsuarioAdmin(
                    request.nombre(),
                    request.email(),
                    request.password(),
                    request.rol(),
                    request.carrera(),
                    request.cicloActual()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(mapearUsuario(usuario));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable String id, @RequestBody UsuarioAdminRequestDto request) {
        try {
            Usuario usuario = usuarioServicio.actualizarUsuarioAdmin(
                    id,
                    request.nombre(),
                    request.email(),
                    request.password(),
                    request.rol(),
                    request.carrera(),
                    request.cicloActual()
            );
            return ResponseEntity.ok(mapearUsuario(usuario));
        } catch (MyException ex) {
            HttpStatus status = esNoEncontrado(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PatchMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<?> desactivarUsuario(@PathVariable String id) {
        try {
            Usuario usuario = usuarioServicio.desactivarUsuario(id);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario desactivado correctamente",
                    "usuario", mapearUsuario(usuario)
            ));
        } catch (MyException ex) {
            HttpStatus status = esNoEncontrado(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PatchMapping("/usuarios/{id}/activar")
    public ResponseEntity<?> activarUsuario(@PathVariable String id) {
        try {
            Usuario usuario = usuarioServicio.activarUsuario(id);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario activado correctamente",
                    "usuario", mapearUsuario(usuario)
            ));
        } catch (MyException ex) {
            HttpStatus status = esNoEncontrado(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
        }
    }

    private Map<String, Object> mapearUsuario(Usuario usuario) {
        return Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "email", usuario.getEmail(),
                "rol", usuario.getRol(),
                "carrera", usuario.getCarrera() != null ? usuario.getCarrera().getNombre() : "",
                "cicloActual", usuario.getCicloActual() != null ? usuario.getCicloActual() : 0,
                "activo", usuario.isActivo()
        );
    }

    private boolean esNoEncontrado(String mensaje) {
        return mensaje != null && mensaje.toLowerCase().contains("no encontrado");
    }

}
