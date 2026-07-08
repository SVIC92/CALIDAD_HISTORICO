package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.SubtituloDto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias de {@link SubtitulosControlador}: distribucion de
 * subtitulos generados por el microservicio de transcripcion via STOMP.
 */
@ExtendWith(MockitoExtension.class)
class SubtitulosControladorTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SubtitulosControlador subtitulosControlador;

    private SubtituloDto subtitulo(String salaUuid, String texto) {
        SubtituloDto dto = new SubtituloDto();
        dto.setSalaUuid(salaUuid);
        dto.setUsuarioId("user-1");
        dto.setTexto(texto);
        return dto;
    }

    @Nested
    @DisplayName("recibirSubtituloDeIA")
    class RecibirSubtituloDeIA {

        @Test
        @DisplayName("con sala y texto validos difunde el subtitulo al topico de la sala")
        void difundeSubtitulo() {
            SubtituloDto dto = subtitulo("sala-1", "Hola a todos");

            subtitulosControlador.recibirSubtituloDeIA(dto);

            verify(messagingTemplate).convertAndSend("/topic/sala/sala-1/subtitulos", dto);
        }

        @Test
        @DisplayName("sin salaUuid no envia nada")
        void sinSalaUuidNoEnvia() {
            SubtituloDto dto = subtitulo(null, "Hola a todos");

            subtitulosControlador.recibirSubtituloDeIA(dto);

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("con texto vacio no envia nada")
        void conTextoVacioNoEnvia() {
            SubtituloDto dto = subtitulo("sala-1", "");

            subtitulosControlador.recibirSubtituloDeIA(dto);

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }
}
