package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.entidades.Videoconferencia;
import com.GestionInscripcionCursos.entidades.VideoconferenciaParticipante;
import com.GestionInscripcionCursos.enumeraciones.RolSala;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import com.GestionInscripcionCursos.repositorios.VideoconferenciaParticipanteRepositorio;
import com.GestionInscripcionCursos.repositorios.VideoconferenciaRepositorio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link VideoconferenciaServicio}.
 *
 * <p>El endpoint que consume {@code crearSala} fue corregido para dejar de
 * recibir la entidad {@link Usuario} completa por request body (mass
 * assignment, vulnerabilidad detectada por SonarQube); estas pruebas cubren
 * la logica de negocio que queda del lado del servicio.
 */
@ExtendWith(MockitoExtension.class)
class VideoconferenciaServicioTest {

    @Mock
    private VideoconferenciaRepositorio videoRepo;

    @Mock
    private VideoconferenciaParticipanteRepositorio participanteRepo;

    @Mock
    private UsuarioRepositorio usuarioRepo;

    @Mock
    private CorreoServicio correoServicio;

    private VideoconferenciaServicio videoconferenciaServicio;

    @BeforeEach
    void setUp() {
        videoconferenciaServicio = new VideoconferenciaServicio(videoRepo, participanteRepo, usuarioRepo, correoServicio);
        ReflectionTestUtils.setField(videoconferenciaServicio, "frontendUrl", "https://app.test");
    }

    private Usuario usuario(String id, String nombre, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        return u;
    }

    // =====================================================================
    // crearSala
    // =====================================================================
    @Nested
    @DisplayName("crearSala")
    class CrearSala {

        @Test
        @DisplayName("guarda la sala con un salaUuid generado y asigna ADMIN al creador")
        void creaSalaYAsignaAdminAlCreador() {
            Usuario creador = usuario("u-1", "Ana", "ana@dominio.com");
            when(videoRepo.save(any(Videoconferencia.class))).thenAnswer(inv -> inv.getArgument(0));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId(anyString(), eq("u-1")))
                    .thenReturn(Optional.empty());

            Videoconferencia sala = videoconferenciaServicio.crearSala("Clase 1", 30, true, creador);

            assertEquals("Clase 1", sala.getTitulo());
            assertEquals(30, sala.getCapacidadMaxima());
            assertTrue(sala.isEsPublica());
            assertTrue(sala.getSalaUuid().startsWith("Sala-"));
            assertSame(creador, sala.getCreador());

            ArgumentCaptor<VideoconferenciaParticipante> captor = ArgumentCaptor.forClass(VideoconferenciaParticipante.class);
            verify(participanteRepo).save(captor.capture());
            assertEquals(RolSala.ADMIN, captor.getValue().getRolSala());
            assertFalse(captor.getValue().isInvitado());
        }
    }

    // =====================================================================
    // invitarUsuarios
    // =====================================================================
    @Nested
    @DisplayName("invitarUsuarios")
    class InvitarUsuarios {

        @Test
        @DisplayName("con salaUuid vacio lanza IllegalArgumentException")
        void salaUuidVacioLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios(" ", List.of("u-1"), RolSala.PARTICIPANTE));
        }

        @Test
        @DisplayName("con lista de usuarios vacia lanza IllegalArgumentException")
        void listaUsuariosVaciaLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", List.of(), RolSala.PARTICIPANTE));
        }

        @Test
        @DisplayName("con sala inexistente lanza IllegalArgumentException")
        void salaInexistenteLanzaExcepcion() {
            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", List.of("u-1"), RolSala.PARTICIPANTE));
        }

        @Test
        @DisplayName("ignora usuarios inexistentes sin interrumpir el resto de invitaciones")
        void ignoraUsuariosInexistentes() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setTitulo("Clase 1");
            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("fantasma")).thenReturn(Optional.empty());

            assertDoesNotThrow(() ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", List.of("fantasma"), RolSala.PARTICIPANTE));

            verify(participanteRepo, never()).save(any());
            verifyNoInteractions(correoServicio);
        }

        @Test
        @DisplayName("invita a un usuario existente, le asigna el rol y envia el correo")
        void invitaUsuarioExistenteYEnviaCorreo() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setTitulo("Clase 1");
            Usuario invitado = usuario("u-2", "Luis", "luis@dominio.com");

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-2")).thenReturn(Optional.of(invitado));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-2"))
                    .thenReturn(Optional.empty());

            videoconferenciaServicio.invitarUsuarios("Sala-1", List.of("u-2"), RolSala.PRESENTADOR);

            ArgumentCaptor<VideoconferenciaParticipante> captor = ArgumentCaptor.forClass(VideoconferenciaParticipante.class);
            verify(participanteRepo).save(captor.capture());
            assertEquals(RolSala.PRESENTADOR, captor.getValue().getRolSala());
            assertTrue(captor.getValue().isInvitado());

            verify(correoServicio).enviarCorreoInvitacionVideoconferencia(
                    eq("luis@dominio.com"), eq("Luis"), eq("Clase 1"), eq("Presentador"), contains("Sala-1"));
        }

        @Test
        @DisplayName("si el envio de correo falla, la invitacion no se interrumpe")
        void falloDeCorreoNoInterrumpeLaInvitacion() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setTitulo("Clase 1");
            Usuario invitado = usuario("u-2", "Luis", "luis@dominio.com");

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-2")).thenReturn(Optional.of(invitado));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-2"))
                    .thenReturn(Optional.empty());
            doThrow(new RuntimeException("smtp caido"))
                    .when(correoServicio).enviarCorreoInvitacionVideoconferencia(anyString(), anyString(), anyString(), anyString(), anyString());

            assertDoesNotThrow(() ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", List.of("u-2"), RolSala.PARTICIPANTE));

            verify(participanteRepo).save(any(VideoconferenciaParticipante.class));
        }
    }

    // =====================================================================
    // listarParticipantes
    // =====================================================================
    @Nested
    @DisplayName("listarParticipantes")
    class ListarParticipantes {

        @Test
        @DisplayName("con sala inexistente lanza IllegalArgumentException")
        void salaInexistenteLanzaExcepcion() {
            when(videoRepo.findBySalaUuid("Sala-x")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.listarParticipantes("Sala-x"));
        }

        @Test
        @DisplayName("retorna los participantes ordenados alfabeticamente por nombre")
        void retornaParticipantesOrdenados() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));

            VideoconferenciaParticipante p1 = new VideoconferenciaParticipante();
            p1.setUsuario(usuario("u-1", "Zoe", "zoe@dominio.com"));
            p1.setRolSala(RolSala.PARTICIPANTE);

            VideoconferenciaParticipante p2 = new VideoconferenciaParticipante();
            p2.setUsuario(usuario("u-2", "Ana", "ana@dominio.com"));
            p2.setRolSala(RolSala.ADMIN);

            when(participanteRepo.findByVideoconferenciaSalaUuid("Sala-1")).thenReturn(List.of(p1, p2));

            var resultado = videoconferenciaServicio.listarParticipantes("Sala-1");

            assertEquals(2, resultado.size());
            assertEquals("Ana", resultado.get(0).getNombreUsuario());
            assertEquals("Zoe", resultado.get(1).getNombreUsuario());
        }
    }
}
