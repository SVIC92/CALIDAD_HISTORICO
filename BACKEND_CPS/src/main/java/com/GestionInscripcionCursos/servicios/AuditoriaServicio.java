package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.EventoAuditoria;
import com.GestionInscripcionCursos.repositorios.EventoAuditoriaRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Registra eventos de seguridad relevantes (autenticacion, cambios de
 * contrasena y gestion de usuarios) para cubrir OWASP A09 (Security Logging
 * and Monitoring Failures). Un fallo al auditar nunca debe interrumpir el
 * flujo de negocio que la origino.
 */
@Service
public class AuditoriaServicio {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServicio.class);

    public static final String LOGIN_EXITOSO = "LOGIN_EXITOSO";
    public static final String LOGIN_FALLIDO = "LOGIN_FALLIDO";
    public static final String CAMBIO_PASSWORD = "CAMBIO_PASSWORD";
    public static final String USUARIO_CREADO = "USUARIO_CREADO";
    public static final String USUARIO_ACTUALIZADO = "USUARIO_ACTUALIZADO";
    public static final String USUARIO_DESACTIVADO = "USUARIO_DESACTIVADO";
    public static final String USUARIO_ACTIVADO = "USUARIO_ACTIVADO";

    private final EventoAuditoriaRepositorio eventoAuditoriaRepositorio;

    public AuditoriaServicio(EventoAuditoriaRepositorio eventoAuditoriaRepositorio) {
        this.eventoAuditoriaRepositorio = eventoAuditoriaRepositorio;
    }

    public void registrar(String tipoEvento, String usuarioEmail, String detalle, boolean exitoso) {
        try {
            eventoAuditoriaRepositorio.save(
                    new EventoAuditoria(tipoEvento, usuarioEmail, detalle, exitoso, LocalDateTime.now()));
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar el evento de auditoria {} para {}: {}", tipoEvento, usuarioEmail, ex.getMessage());
        }
    }

    public List<EventoAuditoria> listarRecientes() {
        return eventoAuditoriaRepositorio.findTop200ByOrderByFechaDesc();
    }
}
