package com.GestionInscripcionCursos.controladores;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class WebSocketMusicaControlador {

    // Recibe el estado del reproductor de un cliente y lo esparce a toda la sala
    @MessageMapping("/sala/{salaUuid}/musica")
    @SendTo("/topic/sala/{salaUuid}/musica")
    public Map<String, Object> sincronizarMusica(@DestinationVariable String salaUuid, Map<String, Object> mensajeMusica) {
        // mensajeMusica contendrá: { accion: 'PLAY'|'PAUSE', segundosActuales: 45.2, videoId: '...' }
        return mensajeMusica;
    }
}