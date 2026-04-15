package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.IaChatRequestDto;
import com.GestionInscripcionCursos.dto.IaChatResponseDto;
import com.GestionInscripcionCursos.dto.IaHistorialDto;
import com.GestionInscripcionCursos.dto.RubricaGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.servicios.IaServicio;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
public class IaControlador {

    private final IaServicio iaServicio;

    public IaControlador(IaServicio iaServicio) {
        this.iaServicio = iaServicio;
    }

    @PreAuthorize("hasRole('ALUMNO')")
    @PostMapping("/chat/alumno")
    public ResponseEntity<IaChatResponseDto> chatAlumno(@RequestBody IaChatRequestDto request, Authentication auth) {
        return ResponseEntity.ok(iaServicio.chatearSegunRol(auth.getName(), "ALUMNO", request.mensaje()));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping("/chat/profesor")
    public ResponseEntity<IaChatResponseDto> chatProfesor(@RequestBody IaChatRequestDto request, Authentication auth) {
        return ResponseEntity.ok(iaServicio.chatearSegunRol(auth.getName(), "PROFESOR", request.mensaje()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/chat/admin")
    public ResponseEntity<IaChatResponseDto> chatAdmin(@RequestBody IaChatRequestDto request, Authentication auth) {
        return ResponseEntity.ok(iaServicio.chatearSegunRol(auth.getName(), "ADMIN", request.mensaje()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/historial/ultimo")
    public ResponseEntity<?> ultimoHistorial(Authentication auth) {
        return iaServicio.obtenerUltimoHistorial(auth.getName())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("mensaje", "No hay historial disponible")));
    }

    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    @PostMapping("/rubricas/generar")
    public ResponseEntity<RubricaGeneradaDto> generarRubrica(
            @RequestBody RubricaGeneracionRequestDto request,
            Authentication auth
    ) {
        return ResponseEntity.ok(iaServicio.generarRubrica(auth.getName(), request));
    }
}
