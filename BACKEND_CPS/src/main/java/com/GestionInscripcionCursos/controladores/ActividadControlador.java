package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ActividadServicio;
import com.GestionInscripcionCursos.servicios.CursoServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actividad")
public class ActividadControlador {

    @Autowired
    private ActividadServicio actividadServicio;

    @Autowired
    private CursoServicio cursoServicio;
    
    @Autowired
    private UsuarioServicio usuarioServicio;

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/registrar/{id}")
    public ResponseEntity<?> registrar(@PathVariable String id) {
        return ResponseEntity.ok(cursoServicio.buscarPorId(id));
    }

    @PostMapping("/registro/{id}")
    public ResponseEntity<?> registro(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Date fechaVencimiento) {

        try {
            actividadServicio.crearActividad(nombre, descripcion, fechaVencimiento, id);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Actividad registrada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR', 'ALUMNO')")
    @GetMapping("/listar/{id}")
    public ResponseEntity<?> listar(@PathVariable String id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        Rol rol = usuario.getRol();

        List<Actividad> actividades = actividadServicio.listarActividadesPorIdCurso(id);

        return ResponseEntity.ok(Map.of("rol", rol, "actividades", actividades));
    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/modificar/{id}")
    public ResponseEntity<?> modificar(@PathVariable String id) {
        return ResponseEntity.ok(actividadServicio.buscarPorId(id));
    }

    @PostMapping("/modificar/{id}")
    public ResponseEntity<?> modificar(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Date fechaVencimiento) {

        try {
            actividadServicio.modificarActividad(id, nombre, descripcion, fechaVencimiento);
            return ResponseEntity.ok(actividadServicio.buscarPorId(id));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {

        try {
            actividadServicio.eliminarActividad(id);
            return ResponseEntity.ok(Map.of("mensaje", "Actividad eliminada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

}
