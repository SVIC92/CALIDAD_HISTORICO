package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.UsuarioConectadoDto;
import com.GestionInscripcionCursos.dto.UsuarioResumenDto;
import com.GestionInscripcionCursos.servicios.PresenciaUsuarioServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class PresenciaControlador {

    @Autowired
    private PresenciaUsuarioServicio presenciaUsuarioServicio;

    // AÑADIMOS el UsuarioServicio para obtener la lista
    @Autowired
    private UsuarioServicio usuarioServicio;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/ping")
    public ResponseEntity<?> ping(Authentication authentication) {
        presenciaUsuarioServicio.registrarActividad(authentication.getName(), authentication.getAuthorities());
        return ResponseEntity.ok().build();
    }

    // CAMBIO 1: Permitimos que cualquier usuario autenticado vea a los conectados
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/conectados")
    public ResponseEntity<List<UsuarioConectadoDto>> listarConectados() {
        return ResponseEntity.ok(presenciaUsuarioServicio.listarConectados());
    }

    // CAMBIO 2: Creamos un endpoint público para la lista de contactos del chat
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/todos")
    public ResponseEntity<List<UsuarioResumenDto>> listarTodos() {
        return ResponseEntity.ok(usuarioServicio.listarUsuarios());
    }

    @GetMapping(value = "/conectados/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConectados() {
        SseEmitter emisor = new SseEmitter(0L); 
        presenciaUsuarioServicio.registrarEmisor(emisor);
        return emisor;
    }
}