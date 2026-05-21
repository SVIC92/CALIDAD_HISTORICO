package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.CambioRolDinamicoDto;
import com.GestionInscripcionCursos.dto.VideoconferenciaParticipanteDto;
import com.GestionInscripcionCursos.dto.SolicitudEntradaDto;
import com.GestionInscripcionCursos.servicios.VideoconferenciaServicio;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class VideoconferenciaLobbyControlador {

    private final SimpMessagingTemplate messagingTemplate;
    private final VideoconferenciaServicio videoconferenciaServicio;

    public VideoconferenciaLobbyControlador(
            SimpMessagingTemplate messagingTemplate,
            VideoconferenciaServicio videoconferenciaServicio
    ) {
        this.messagingTemplate = messagingTemplate;
        this.videoconferenciaServicio = videoconferenciaServicio;
    }

    @MessageMapping("/sala/{salaUuid}/solicitar-acceso")
    public void solicitarAcceso(@DestinationVariable String salaUuid, @Payload SolicitudEntradaDto solicitud) {
        messagingTemplate.convertAndSend("/topic/sala/" + salaUuid + "/lobby-admin", solicitud);
    }
    @MessageMapping("/sala/{salaUuid}/responder-acceso")
    public void responderAcceso(@DestinationVariable String salaUuid, @Payload SolicitudEntradaDto respuesta) {
        if (respuesta.isAprobado()) {
            videoconferenciaServicio.cambiarRolParticipanteEnSala(
                    salaUuid,
                    respuesta.getUsuarioId(),
                    respuesta.getRolAsignado()
            );

            messagingTemplate.convertAndSend(
                    "/topic/sala/" + salaUuid + "/participantes",
                    videoconferenciaServicio.listarParticipantes(salaUuid)
            );
        }

        messagingTemplate.convertAndSend("/topic/sala/" + salaUuid + "/espera/" + respuesta.getUsuarioId(), respuesta);
    }
    @MessageMapping("/sala/{salaUuid}/cambiar-rol-en-vivo")
    public void cambiarRolEnVivo(@DestinationVariable String salaUuid, @Payload CambioRolDinamicoDto cambioRol) {
        VideoconferenciaParticipanteDto participanteActualizado = videoconferenciaServicio.cambiarRolParticipanteEnSala(
                salaUuid,
                cambioRol.getUsuarioId(),
                cambioRol.getNuevoRol()
        );

        CambioRolDinamicoDto cambioNormalizado = new CambioRolDinamicoDto();
        cambioNormalizado.setSalaUuid(salaUuid);
        cambioNormalizado.setUsuarioId(participanteActualizado.getUsuarioId());
        cambioNormalizado.setNuevoRol(participanteActualizado.getRolSala());

        messagingTemplate.convertAndSend("/topic/sala/" + salaUuid + "/roles-en-vivo", cambioNormalizado);
        messagingTemplate.convertAndSend("/topic/sala/" + salaUuid + "/participantes", videoconferenciaServicio.listarParticipantes(salaUuid));
    }
}