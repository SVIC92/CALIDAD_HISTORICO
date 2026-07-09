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

    private static final String CLAVE_MENSAJE = "mensaje";
    private static final String CLAVE_ERROR = "error";

    @Autowired
    private ActividadServicio actividadServicio;

    @Autowired
    private CursoServicio cursoServicio;
    
    @Autowired
    private UsuarioServicio usuarioServicio;

    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping("/registrar/{id}")
    public ResponseEntity<Object> registrar(@PathVariable String id) {
        return ResponseEntity.ok(cursoServicio.buscarPorId(id));
    }

    @PostMapping("/registro/{id}")
    public ResponseEntity<Object> registro(@PathVariable String id, 
            @RequestParam String nombre, 
            @RequestParam String descripcion, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date fechaVencimiento,
            @RequestParam(required = false, defaultValue = "1") Integer intentosPermitidos) {
        try {
            actividadServicio.crearActividad(nombre, descripcion, fechaVencimiento, intentosPermitidos, id);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(CLAVE_MENSAJE, "Actividad registrada correctamente"));
        } catch (MyException e) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/modificar/{id}")
    public ResponseEntity<Object> modificar(@PathVariable String id, 
            @RequestParam String nombre, 
            @RequestParam String descripcion, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date fechaVencimiento,
            @RequestParam(required = false, defaultValue = "1") Integer intentosPermitidos) {
        try {
            actividadServicio.modificarActividad(id, nombre, descripcion, fechaVencimiento, intentosPermitidos);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Actividad modificada correctamente"));
        } catch (MyException e) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR', 'ALUMNO')")
    @GetMapping("/listar/{id}")
    public ResponseEntity<Object> listar(@PathVariable String id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        Rol rol = usuario.getRol();

        List<Actividad> actividades = actividadServicio.listarActividadesPorIdCurso(id);

        return ResponseEntity.ok(Map.of("rol", rol, "actividades", actividades));
    }

    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping("/modificar/{id}")
    public ResponseEntity<Object> modificar(@PathVariable String id) {
        return ResponseEntity.ok(actividadServicio.buscarPorId(id));
    }

    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping("/eliminar/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {

        try {
            actividadServicio.eliminarActividad(id);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Actividad eliminada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

}
