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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inscripcion")
public class InscripcionControlador {
    
    @Autowired
    private InscripcionServicio inscripcionServicio;
    
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/listaPendientesProfesor")
    public ResponseEntity<List<Inscripcion>> listaPendientesProfesor() {
        return ResponseEntity.ok(inscripcionServicio.listaPendientesProfesor());
    }
    
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/listaRealizadasProfesor")
    public ResponseEntity<List<Inscripcion>> listaRealizadasProfesor() {
        return ResponseEntity.ok(inscripcionServicio.listaRealizadasProfesor());
    }
    
    
    
    
    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/listaPendientesAlumno")
    public ResponseEntity<List<Inscripcion>> listaPendientesAlumno() {
        return ResponseEntity.ok(inscripcionServicio.listaPendientesAlumno());
    }
    
    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/listaRealizadasAlumno")
    public ResponseEntity<List<Inscripcion>> listaRealizadasAlumno() {
        return ResponseEntity.ok(inscripcionServicio.listaRealizadasAlumno());
    }
    
    
    
    
    
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_PROFESOR')")
    @GetMapping("/aprobar/{id}")
    public ResponseEntity<?> aprobar(@PathVariable String id) {
        try {
            inscripcionServicio.aprobar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Inscripcion aprobada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
    
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_PROFESOR')")
    @GetMapping("/rechazar/{id}")
    public ResponseEntity<?> rechazar(@PathVariable String id) {
        try {
            inscripcionServicio.rechazar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Inscripcion rechazada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
    
    
}
