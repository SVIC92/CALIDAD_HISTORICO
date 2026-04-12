package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ActividadServicio;
import com.GestionInscripcionCursos.servicios.ReporteServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reporte")
public class ReporteControlador {

    @Autowired
    private ActividadServicio actividadServicio;

    @Autowired
    private ReporteServicio reporteServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @PreAuthorize("hasAnyRole('ROLE_ALUMNO')")
    @GetMapping("/registrar/{id}")
    public ResponseEntity<?> registrar(@PathVariable String id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        Actividad actividad = actividadServicio.buscarPorId(id);
        try {
            reporteServicio.validarDobleReporte(usuario.getId(), actividad.getId());
            return ResponseEntity.ok(actividad);

        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

    }

    @PostMapping("/registro/{id}")
    public ResponseEntity<?> registro(
            @PathVariable String id,
            @RequestParam String respuesta) {

        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            String emailUser = authentication.getName();

            Usuario usuario = usuarioServicio.buscarEmail(emailUser);

            reporteServicio.crearReporte(respuesta, id, usuario.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Reporte registrado correctamente"));

        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/listar/{id}")
    public ResponseEntity<List<Reporte>> listar(@PathVariable String id) {

        List<Reporte> reportes = reporteServicio.listarReportesPorIdActividad(id);
        return ResponseEntity.ok(reportes);
    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/calificar/{id}")
    public ResponseEntity<Reporte> calificar(@PathVariable String id) {
        return ResponseEntity.ok(reporteServicio.buscarPorId(id));
    }

    @PostMapping("/calificar/{id}")
    public ResponseEntity<?> calificar(
            @PathVariable String id,
            @RequestParam String nota,
            @RequestParam String comentario) {

        try {
            reporteServicio.calificarReporte(id, nota, comentario);
            return ResponseEntity.ok(reporteServicio.buscarPorId(id));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

    }

    @PreAuthorize("hasAnyRole('ROLE_ALUMNO')")
    @GetMapping("/detalle/{id}")
    public ResponseEntity<Reporte> verDetalle(@PathVariable String id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        Actividad actividad = actividadServicio.buscarPorId(id);

        Reporte reporte = reporteServicio.buscarPorIdCategoriaIdUsuario(usuario.getId(), actividad.getId());
        return ResponseEntity.ok(reporte);

    }

}
