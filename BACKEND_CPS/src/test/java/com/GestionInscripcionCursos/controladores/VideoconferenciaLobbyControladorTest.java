package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.CambioRolDinamicoDto;
import com.GestionInscripcionCursos.dto.SolicitudEntradaDto;
import com.GestionInscripcionCursos.dto.VideoconferenciaParticipanteDto;
import com.GestionInscripcionCursos.servicios.VideoconferenciaServicio;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link VideoconferenciaLobbyControlador}: mensajes
 * STOMP de lobby (solicitar acceso, responder acceso y cambio de rol en
 * vivo).
 */
@ExtendWith(MockitoExtension.class)
class VideoconferenciaLobbyControladorTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private VideoconferenciaServicio videoconferenciaServicio;

    @InjectMocks
    private VideoconferenciaLobbyControlador lobbyControlador;

    @Nested
    @DisplayName("solicitarAcceso")
    class SolicitarAcceso {

        @Test
        @DisplayName("reenvia la solicitud al topico de administracion del lobby de la sala")
        void reenviaSolicitud() {
            SolicitudEntradaDto solicitud = new SolicitudEntradaDto();
            solicitud.setSalaUuid("sala-1");
            solicitud.setUsuarioId("user-1");

            lobbyControlador.solicitarAcceso("sala-1", solicitud);

            verify(messagingTemplate).convertAndSend("/topic/sala/sala-1/lobby-admin", solicitud);
        }
    }

    @Nested
    @DisplayName("responderAcceso")
    class ResponderAcceso {

        @Test
        @DisplayName("si es aprobado cambia el rol, difunde la lista de participantes y notifica al usuario")
        void aprobadoActualizaRolYNotifica() {
            SolicitudEntradaDto respuesta = new SolicitudEntradaDto();
            respuesta.setSalaUuid("sala-1");
            respuesta.setUsuarioId("user-1");
            respuesta.setAprobado(true);
            respuesta.setRolAsignado("PARTICIPANTE");
            List<VideoconferenciaParticipanteDto> participantes = List.of(new VideoconferenciaParticipanteDto());
            when(videoconferenciaServicio.listarParticipantes("sala-1")).thenReturn(participantes);

            lobbyControlador.responderAcceso("sala-1", respuesta);

            verify(videoconferenciaServicio).cambiarRolParticipanteEnSala("sala-1", "user-1", "PARTICIPANTE");
            verify(messagingTemplate).convertAndSend("/topic/sala/sala-1/participantes", participantes);
            verify(messagingTemplate).convertAndSend("/topic/sala/sala-1/espera/user-1", respuesta);
        }

        @Test
        @DisplayName("si no es aprobado solo notifica al usuario en espera sin cambiar roles")
        void noAprobadoSoloNotifica() {
            SolicitudEntradaDto respuesta = new SolicitudEntradaDto();
            respuesta.setSalaUuid("sala-1");
            respuesta.setUsuarioId("user-1");
            respuesta.setAprobado(false);

            lobbyControlador.responderAcceso("sala-1", respuesta);

            verify(videoconferenciaServicio, never()).cambiarRolParticipanteEnSala(anyString(), anyString(), anyString());
            verify(messagingTemplate, never()).convertAndSend(eq("/topic/sala/sala-1/participantes"), any(Object.class));
            verify(messagingTemplate, times(1)).convertAndSend("/topic/sala/sala-1/espera/user-1", respuesta);
        }
    }

    @Nested
    @DisplayName("cambiarRolEnVivo")
    class CambiarRolEnVivo {

        @Test
        @DisplayName("actualiza el rol, difunde el cambio normalizado y la lista de participantes")
        void actualizaYDifundeCambioDeRol() {
            CambioRolDinamicoDto cambioRol = new CambioRolDinamicoDto();
            cambioRol.setSalaUuid("sala-1");
            cambioRol.setUsuarioId("user-1");
            cambioRol.setNuevoRol("PRESENTADOR");

            VideoconferenciaParticipanteDto actualizado = new VideoconferenciaParticipanteDto();
            actualizado.setUsuarioId("user-1");
            actualizado.setRolSala("PRESENTADOR");
            when(videoconferenciaServicio.cambiarRolParticipanteEnSala("sala-1", "user-1", "PRESENTADOR"))
                    .thenReturn(actualizado);
            List<VideoconferenciaParticipanteDto> participantes = List.of(actualizado);
            when(videoconferenciaServicio.listarParticipantes("sala-1")).thenReturn(participantes);

            lobbyControlador.cambiarRolEnVivo("sala-1", cambioRol);

            ArgumentCaptor<CambioRolDinamicoDto> captor = ArgumentCaptor.forClass(CambioRolDinamicoDto.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/sala/sala-1/roles-en-vivo"), captor.capture());
            assertEquals("sala-1", captor.getValue().getSalaUuid());
            assertEquals("user-1", captor.getValue().getUsuarioId());
            assertEquals("PRESENTADOR", captor.getValue().getNuevoRol());
            verify(messagingTemplate).convertAndSend("/topic/sala/sala-1/participantes", participantes);
        }
    }
}
