package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.CambioRolDinamicoDto;
import com.GestionInscripcionCursos.dto.SolicitudEntradaDto;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class VideoconferenciaLobbyControlador {

    private final SimpMessagingTemplate messagingTemplate;

    public VideoconferenciaLobbyControlador(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/sala/{salaUuid}/solicitar-acceso")
    public void solicitarAcceso(@DestinationVariable String salaUuid, @Payload SolicitudEntradaDto solicitud) {
        messagingTemplate.convertAndSend("/topic/sala/" + salaUuid + "/lobby-admin", solicitud);
    }
    @MessageMapping("/sala/{salaUuid}/responder-acceso")
    public void responderAcceso(@DestinationVariable String salaUuid, @Payload SolicitudEntradaDto respuesta) {
        messagingTemplate.convertAndSend("/topic/sala/" + salaUuid + "/espera/" + respuesta.getUsuarioId(), respuesta);
    }
    @MessageMapping("/sala/{salaUuid}/cambiar-rol-en-vivo")
    public void cambiarRolEnVivo(@DestinationVariable String salaUuid, @Payload CambioRolDinamicoDto cambioRol) {
        messagingTemplate.convertAndSend("/topic/sala/" + salaUuid + "/roles-en-vivo", cambioRol);
    }
}