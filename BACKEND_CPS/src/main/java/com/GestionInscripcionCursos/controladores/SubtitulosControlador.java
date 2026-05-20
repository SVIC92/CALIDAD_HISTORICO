package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.SubtituloDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subtitulos")
public class SubtitulosControlador {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/interno")
    public void recibirSubtituloDeIA(@RequestBody SubtituloDto subtitulo) {
        
        if(subtitulo.getSalaUuid() != null && !subtitulo.getTexto().isEmpty()){
            String destino = "/topic/sala/" + subtitulo.getSalaUuid() + "/subtitulos";
            messagingTemplate.convertAndSend(destino, subtitulo);
        }
    }
}