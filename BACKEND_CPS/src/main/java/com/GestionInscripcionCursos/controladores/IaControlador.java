package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.IaChatRequestDto;
import com.GestionInscripcionCursos.dto.IaChatResponseDto;
import com.GestionInscripcionCursos.dto.RubricaGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.dto.SilaboGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.SilaboGeneradoDto;
import com.GestionInscripcionCursos.servicios.IaServicio;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(IaControlador.class);

    private final IaServicio iaServicio;

    public IaControlador(IaServicio iaServicio) {
        this.iaServicio = iaServicio;
    }

    @PreAuthorize("hasRole('ALUMNO')")
    @PostMapping("/chat/alumno")
    public ResponseEntity<?> chatAlumno(@RequestBody IaChatRequestDto request, Authentication auth) {
        return ejecutarChatSeguro(request, auth, "ALUMNO");
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping("/chat/profesor")
    public ResponseEntity<?> chatProfesor(@RequestBody IaChatRequestDto request, Authentication auth) {
        return ejecutarChatSeguro(request, auth, "PROFESOR");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/chat/admin")
    public ResponseEntity<?> chatAdmin(@RequestBody IaChatRequestDto request, Authentication auth) {
        return ejecutarChatSeguro(request, auth, "ADMIN");
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
    @PostMapping("/silabo/generar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    public ResponseEntity<?> generarSilabo(
            @RequestBody SilaboGeneracionRequestDto request) {
        try {
            SilaboGeneradoDto silabo = iaServicio.generarSilabo(request);
            return ResponseEntity.ok(silabo);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            LOGGER.error("Error de configuración/servicio IA al generar sílabo: {}", ex.getMessage());
            String detalle = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "Sin detalle disponible"
                : ex.getMessage();
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "El servicio de IA no esta disponible temporalmente");
            body.put("detalle", detalle);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        } catch (Exception ex) {
            LOGGER.error("Error no controlado al generar sílabo con IA", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al generar el sílabo"));
        }
    }

    private ResponseEntity<?> ejecutarChatSeguro(IaChatRequestDto request, Authentication auth, String rol) {
        try {
            if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No hay usuario autenticado para procesar el chat"));
            }
            if (request == null || request.mensaje() == null || request.mensaje().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El mensaje es obligatorio"));
            }
            IaChatResponseDto respuesta = iaServicio.chatearSegunRol(auth.getName(), rol, request.mensaje());
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            LOGGER.error("Error de configuración/servicio IA para rol {}: {}", rol, ex.getMessage());
            String detalle = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "Sin detalle disponible"
                : ex.getMessage();
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "El servicio de IA no esta disponible temporalmente");
            body.put("detalle", detalle);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        } catch (Exception ex) {
            LOGGER.error("Error no controlado en chat IA para rol {}", rol, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al procesar el chat IA"));
        }
    }
}
