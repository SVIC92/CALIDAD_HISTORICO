package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.UsuarioConectadoDto;
import com.GestionInscripcionCursos.servicios.PresenciaUsuarioServicio;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/usuarios")
public class PresenciaControlador {

    @Autowired
    private PresenciaUsuarioServicio presenciaUsuarioServicio;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/ping")
    public ResponseEntity<?> ping(Authentication authentication) {
        presenciaUsuarioServicio.registrarActividad(authentication.getName(), authentication.getAuthorities());
        return ResponseEntity.ok(Map.of("mensaje", "pong"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/conectados")
    public ResponseEntity<List<UsuarioConectadoDto>> listarConectados() {
        return ResponseEntity.ok(presenciaUsuarioServicio.listarConectados());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/conectados/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConectados() {
        SseEmitter emisor = new SseEmitter(0L);
        presenciaUsuarioServicio.registrarEmisor(emisor);
        return emisor;
    }
}
