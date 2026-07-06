package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.entidades.Rubrica;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ActividadServicio;
import com.GestionInscripcionCursos.servicios.RubricaServicio;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rubricas")
public class RubricaControlador {

    private static final String CLAVE_ERROR = "error";

    private final RubricaServicio rubricaServicio;
    private final ActividadServicio actividadServicio;

    public RubricaControlador(RubricaServicio rubricaServicio, ActividadServicio actividadServicio) {
        this.rubricaServicio = rubricaServicio;
        this.actividadServicio = actividadServicio;
    }

    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @PostMapping
    public ResponseEntity<Object> guardar(
            @RequestBody RubricaGeneradaDto dto,
            @RequestParam String cursoId,
            Authentication auth
    ) {
        try {
            Rubrica guardada = rubricaServicio.guardarDesdeGeneracion(dto, cursoId, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @GetMapping("/curso/{cursoId}")
    public ResponseEntity<List<Rubrica>> listarPorCurso(@PathVariable String cursoId) {
        return ResponseEntity.ok(rubricaServicio.listarPorCurso(cursoId));
    }

    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable String id) {
        try {
            return ResponseEntity.ok(rubricaServicio.buscarPorId(id));
        } catch (MyException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @PatchMapping("/{rubricaId}/asignar/{actividadId}")
    public ResponseEntity<Object> asignarAActividad(@PathVariable String rubricaId, @PathVariable String actividadId) {
        try {
            actividadServicio.asignarRubrica(actividadId, rubricaId);
            return ResponseEntity.ok(Map.of("mensaje", "Rúbrica asignada a la actividad correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }
}
