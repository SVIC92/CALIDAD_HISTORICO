package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.*;
import com.GestionInscripcionCursos.enumeraciones.RolSala;
import com.GestionInscripcionCursos.repositorios.*;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VideoconferenciaServicio {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoconferenciaServicio.class);

    private final VideoconferenciaRepositorio videoRepo;
    private final VideoconferenciaParticipanteRepositorio participanteRepo;
    private final UsuarioRepositorio usuarioRepo;
    private final CorreoServicio correoServicio;

    @Value("${app.frontend.url:${FRONTEND_URL:https://calidad-historico.vercel.app}}")
    private String frontendUrl;

    public VideoconferenciaServicio(
            VideoconferenciaRepositorio videoRepo,
            VideoconferenciaParticipanteRepositorio participanteRepo,
            UsuarioRepositorio usuarioRepo,
            CorreoServicio correoServicio
    ) {
        this.videoRepo = videoRepo;
        this.participanteRepo = participanteRepo;
        this.usuarioRepo = usuarioRepo;
        this.correoServicio = correoServicio;
    }

    public Videoconferencia crearSala(String titulo, int capacidad, boolean esPublica, Usuario creador) {
        Videoconferencia sala = new Videoconferencia();
        sala.setTitulo(titulo);
        sala.setSalaUuid("Sala-" + UUID.randomUUID().toString().substring(0, 8));
        sala.setCapacidadMaxima(capacidad);
        sala.setEsPublica(esPublica);
        sala.setCreador(creador);
        
        Videoconferencia salaGuardada = videoRepo.save(sala);
        asignarRolASala(salaGuardada, creador, RolSala.ADMIN, false);
        return salaGuardada;
    }

    public void invitarUsuarios(String salaUuid, List<String> usuarioIds, RolSala rolAsignado) {
        if (salaUuid == null || salaUuid.isBlank()) {
            throw new IllegalArgumentException("El identificador de la sala es obligatorio");
        }
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un usuario a invitar");
        }
        if (rolAsignado == null) {
            throw new IllegalArgumentException("El rol a asignar es obligatorio");
        }

        Videoconferencia sala = videoRepo.findBySalaUuid(salaUuid)
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada"));

        String enlace = construirEnlaceSala(salaUuid);
        for (String id : usuarioIds) {
            if (id == null || id.isBlank()) {
                continue;
            }

            Usuario usuario = usuarioRepo.findById(id).orElse(null);
            if (usuario == null) {
                LOGGER.warn("No se encontró el usuario {} para invitar a la sala {}", id, salaUuid);
                continue;
            }

            asignarRolASala(sala, usuario, rolAsignado, true);

            try {
                correoServicio.enviarCorreo(
                        usuario.getEmail(),
                        "Invitación a Reunión",
                        construirMensajeInvitacion(sala, rolAsignado, enlace)
                );
            } catch (RuntimeException ex) {
                LOGGER.warn("No se pudo enviar la invitación por correo a {}: {}", usuario.getEmail(), ex.getMessage());
            }
        }
    }

    private void asignarRolASala(Videoconferencia sala, Usuario usuario, RolSala rol, boolean esInvitado) {
        VideoconferenciaParticipante p = participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId(sala.getSalaUuid(), usuario.getId())
                .orElse(new VideoconferenciaParticipante());
        p.setVideoconferencia(sala);
        p.setUsuario(usuario);
        p.setRolSala(rol);
        p.setInvitado(esInvitado);
        participanteRepo.save(p);
    }

    private String construirEnlaceSala(String salaUuid) {
        String baseUrl = frontendUrl == null ? "" : frontendUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/videoconferencia/" + salaUuid;
    }

    private String construirMensajeInvitacion(Videoconferencia sala, RolSala rolAsignado, String enlace) {
        return "Has sido invitado a la reunión '" + sala.getTitulo() + "' con el rol de "
                + rolAsignado + ".\nÚnete aquí: " + enlace;
    }
}