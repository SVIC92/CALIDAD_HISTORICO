package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.CarreraServicio;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carrera")
public class CarreraControlador {

    @Autowired
    private CarreraServicio carreraServicio;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(
            @RequestParam String codigo,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion
    ) {
        try {
            Carrera carrera = carreraServicio.crear(codigo, nombre, descripcion);
            return ResponseEntity.status(HttpStatus.CREATED).body(carrera);
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/lista")
    public ResponseEntity<List<Carrera>> listar() {
        return ResponseEntity.ok(carreraServicio.listar());
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/modificar/{id}")
    public ResponseEntity<?> modificar(
            @PathVariable String id,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion
    ) {
        try {
            return ResponseEntity.ok(carreraServicio.modificar(id, codigo, nombre, descripcion));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        try {
            carreraServicio.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Carrera eliminada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
