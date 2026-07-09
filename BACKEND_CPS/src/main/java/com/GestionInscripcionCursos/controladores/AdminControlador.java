package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.UsuarioAdminRequestDto;
import com.GestionInscripcionCursos.dto.UsuarioResumenDto;
import com.GestionInscripcionCursos.entidades.EventoAuditoria;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.AuditoriaServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import java.util.List;
import java.util.Map;
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

    private static final String CLAVE_MENSAJE = "mensaje";
    private static final String CLAVE_ERROR = "error";

    private final UsuarioServicio usuarioServicio;
    private final AuditoriaServicio auditoriaServicio;

    public AdminControlador(UsuarioServicio usuarioServicio, AuditoriaServicio auditoriaServicio) {
        this.usuarioServicio = usuarioServicio;
        this.auditoriaServicio = auditoriaServicio;
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<Object> panelAdministrativo() {
        return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Dashboard admin"));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/auditoria")
    public ResponseEntity<List<EventoAuditoria>> listarAuditoria() {
        return ResponseEntity.ok(auditoriaServicio.listarRecientes());
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResumenDto>> listarUsuarios() {
        return ResponseEntity.ok(usuarioServicio.listarUsuarios());
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Object> obtenerUsuario(@PathVariable String id) {
        try {
            Usuario usuario = usuarioServicio.buscarPorId(id);
            return ResponseEntity.ok(mapearUsuario(usuario));
        } catch (MyException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/usuarios")
    public ResponseEntity<Object> crearUsuario(@RequestBody UsuarioAdminRequestDto request) {
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
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Object> actualizarUsuario(@PathVariable String id, @RequestBody UsuarioAdminRequestDto request) {
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
            return ResponseEntity.status(status).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PatchMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<Object> desactivarUsuario(@PathVariable String id) {
        try {
            Usuario usuario = usuarioServicio.desactivarUsuario(id);
            return ResponseEntity.ok(Map.of(
                    CLAVE_MENSAJE, "Usuario desactivado correctamente",
                    "usuario", mapearUsuario(usuario)
            ));
        } catch (MyException ex) {
            HttpStatus status = esNoEncontrado(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PatchMapping("/usuarios/{id}/activar")
    public ResponseEntity<Object> activarUsuario(@PathVariable String id) {
        try {
            Usuario usuario = usuarioServicio.activarUsuario(id);
            return ResponseEntity.ok(Map.of(
                    CLAVE_MENSAJE, "Usuario activado correctamente",
                    "usuario", mapearUsuario(usuario)
            ));
        } catch (MyException ex) {
            HttpStatus status = esNoEncontrado(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of(CLAVE_ERROR, ex.getMessage()));
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
