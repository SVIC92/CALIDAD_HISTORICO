package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.InscripcionServicio;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inscripcion")
public class InscripcionControlador {

    private static final String CLAVE_MENSAJE = "mensaje";
    private static final String CLAVE_ERROR = "error";

    @Autowired
    private InscripcionServicio inscripcionServicio;
    
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/listaPendientesProfesor")
    public ResponseEntity<List<Inscripcion>> listaPendientesProfesor() {
        return ResponseEntity.ok(inscripcionServicio.listaPendientesProfesor());
    }
    
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/listaRealizadasProfesor")
    public ResponseEntity<List<Inscripcion>> listaRealizadasProfesor() {
        return ResponseEntity.ok(inscripcionServicio.listaRealizadasProfesor());
    }
    
    
    
    
    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping("/listaPendientesAlumno")
    public ResponseEntity<List<Inscripcion>> listaPendientesAlumno() {
        return ResponseEntity.ok(inscripcionServicio.listaPendientesAlumno());
    }
    
    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping("/listaRealizadasAlumno")
    public ResponseEntity<List<Inscripcion>> listaRealizadasAlumno() {
        return ResponseEntity.ok(inscripcionServicio.listaRealizadasAlumno());
    }
    
    
    
    
    
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @PostMapping("/aprobar/{id}")
    public ResponseEntity<Object> aprobar(@PathVariable String id) {
        try {
            inscripcionServicio.aprobar(id);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Inscripcion aprobada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @PostMapping("/rechazar/{id}")
    public ResponseEntity<Object> rechazar(@PathVariable String id) {
        try {
            inscripcionServicio.rechazar(id);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Inscripcion rechazada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/aprobarProfesor/{id}")
    public ResponseEntity<Object> aprobarProfesor(@PathVariable String id) {
        try {
            inscripcionServicio.aprobarInscripcionProfesor(id);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Inscripcion de profesor aprobada y asignada al curso"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/rechazarProfesor/{id}")
    public ResponseEntity<Object> rechazarProfesor(@PathVariable String id) {
        try {
            inscripcionServicio.rechazarInscripcionProfesor(id);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Inscripcion de profesor rechazada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/inscribirAlumnoDirecto")
    public ResponseEntity<Object> inscribirAlumnoDirecto(
            @RequestParam String usuarioId,
            @RequestParam String cursoId) {
        try {
            inscripcionServicio.inscribirAlumnoDirecto(usuarioId, cursoId);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Alumno inscrito directamente al curso"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }


}
