package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ActividadServicio;
import com.GestionInscripcionCursos.servicios.ArchivoServicio;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reporte")
public class ReporteControlador {

    @Autowired
    private ActividadServicio actividadServicio;

    @Autowired
    private ReporteServicio reporteServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private ArchivoServicio archivoServicio;

    @PreAuthorize("hasAnyRole('ROLE_ALUMNO')")
    @GetMapping("/registrar/{id}")
    public ResponseEntity<?> registrar(@PathVariable String id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        Actividad actividad = actividadServicio.buscarPorId(id);
        
        try {
            // Reemplazamos la validación anterior por la de límites de intentos
            reporteServicio.validarLimitesReporte(usuario.getId(), actividad.getId());
            
            return ResponseEntity.ok(actividad);

        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/registro/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ALUMNO')")
    public ResponseEntity<?> registro(
            @PathVariable String id, 
            @RequestParam("respuesta") String respuesta,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        try {
            String emailUser = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioServicio.buscarEmail(emailUser);

            String archivoUrl = null;
            if (archivo != null && !archivo.isEmpty()) {
                // Se sube a la carpeta 'reportes_archivos'
                archivoUrl = archivoServicio.subirArchivo(archivo, "reportes_archivos");
            }

            reporteServicio.crearReporte(respuesta, id, usuario.getId(), archivoUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mensaje", "Reporte registrado correctamente"));
        } catch (MyException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_PROFESOR')")
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

    @GetMapping("/detalle/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ALUMNO')")
    public ResponseEntity<?> verDetalle(@PathVariable String id) {
        try {
            String emailUser = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioServicio.buscarEmail(emailUser);
            // Ahora devolvemos la lista completa de intentos del alumno
            List<Reporte> reportes = reporteServicio.listarReportesAlumno(usuario.getId(), id);
            return ResponseEntity.ok(reportes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
