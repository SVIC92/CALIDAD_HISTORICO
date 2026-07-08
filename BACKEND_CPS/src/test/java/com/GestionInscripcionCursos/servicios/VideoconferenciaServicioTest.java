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

import java.util.Arrays;
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
            List<String> usuarioIds = List.of("u-1");
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios(" ", usuarioIds, RolSala.PARTICIPANTE));
        }

        @Test
        @DisplayName("con lista de usuarios vacia lanza IllegalArgumentException")
        void listaUsuariosVaciaLanzaExcepcion() {
            List<String> usuarioIds = List.of();
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", usuarioIds, RolSala.PARTICIPANTE));
        }

        @Test
        @DisplayName("con sala inexistente lanza IllegalArgumentException")
        void salaInexistenteLanzaExcepcion() {
            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.empty());
            List<String> usuarioIds = List.of("u-1");

            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", usuarioIds, RolSala.PARTICIPANTE));
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

        @Test
        @DisplayName("con salaUuid nulo lanza IllegalArgumentException")
        void salaUuidNuloLanzaExcepcion() {
            List<String> usuarioIds = List.of("u-1");
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios(null, usuarioIds, RolSala.PARTICIPANTE));
        }

        @Test
        @DisplayName("con lista de usuarios nula lanza IllegalArgumentException")
        void listaUsuariosNulaLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", null, RolSala.PARTICIPANTE));
        }

        @Test
        @DisplayName("con rolAsignado nulo lanza IllegalArgumentException")
        void rolAsignadoNuloLanzaExcepcion() {
            List<String> usuarioIds = List.of("u-1");
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", usuarioIds, null));
        }

        @Test
        @DisplayName("ignora ids nulos o en blanco dentro de la lista sin consultar el repositorio de usuarios")
        void ignoraIdsNulosOEnBlancoDentroDeLaLista() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setTitulo("Clase 1");
            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));

            List<String> ids = Arrays.asList(null, "   ");

            assertDoesNotThrow(() ->
                    videoconferenciaServicio.invitarUsuarios("Sala-1", ids, RolSala.PARTICIPANTE));

            verifyNoInteractions(usuarioRepo);
            verify(participanteRepo, never()).save(any());
            verifyNoInteractions(correoServicio);
        }

        @Test
        @DisplayName("invita con rol ADMIN y formatea el rol como 'Administrador' en el correo")
        void invitaConRolAdminFormateaCorreo() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setTitulo("Clase 1");
            Usuario invitado = usuario("u-3", "Marta", "marta@dominio.com");

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-3")).thenReturn(Optional.of(invitado));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-3"))
                    .thenReturn(Optional.empty());

            videoconferenciaServicio.invitarUsuarios("Sala-1", List.of("u-3"), RolSala.ADMIN);

            verify(correoServicio).enviarCorreoInvitacionVideoconferencia(
                    eq("marta@dominio.com"), eq("Marta"), eq("Clase 1"), eq("Administrador"), contains("Sala-1"));
        }

        @Test
        @DisplayName("cuando el frontendUrl termina en '/' no duplica la barra en el enlace generado")
        void frontendUrlConBarraFinalNoDuplicaBarra() {
            ReflectionTestUtils.setField(videoconferenciaServicio, "frontendUrl", "https://app.test/");
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setTitulo("Clase 1");
            Usuario invitado = usuario("u-4", "Carlos", "carlos@dominio.com");

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-4")).thenReturn(Optional.of(invitado));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-4"))
                    .thenReturn(Optional.empty());

            videoconferenciaServicio.invitarUsuarios("Sala-1", List.of("u-4"), RolSala.PARTICIPANTE);

            verify(correoServicio).enviarCorreoInvitacionVideoconferencia(
                    anyString(), anyString(), anyString(), anyString(), eq("https://app.test/videoconferencia/Sala-1"));
        }

        @Test
        @DisplayName("cuando el frontendUrl es nulo el enlace se construye solo con la ruta relativa")
        void frontendUrlNuloConstruyeRutaRelativa() {
            ReflectionTestUtils.setField(videoconferenciaServicio, "frontendUrl", null);
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setTitulo("Clase 1");
            Usuario invitado = usuario("u-5", "Diana", "diana@dominio.com");

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-5")).thenReturn(Optional.of(invitado));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-5"))
                    .thenReturn(Optional.empty());

            videoconferenciaServicio.invitarUsuarios("Sala-1", List.of("u-5"), RolSala.PARTICIPANTE);

            verify(correoServicio).enviarCorreoInvitacionVideoconferencia(
                    anyString(), anyString(), anyString(), anyString(), eq("/videoconferencia/Sala-1"));
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

        @Test
        @DisplayName("con salaUuid nulo lanza IllegalArgumentException")
        void salaUuidNuloLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.listarParticipantes(null));
        }

        @Test
        @DisplayName("con salaUuid en blanco lanza IllegalArgumentException")
        void salaUuidEnBlancoLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.listarParticipantes("   "));
        }

        @Test
        @DisplayName("participante sin usuario asignado usa cadena vacia para ordenar y valores nulos en el dto")
        void participanteSinUsuarioUsaValoresPorDefecto() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-2");
            when(videoRepo.findBySalaUuid("Sala-2")).thenReturn(Optional.of(sala));

            VideoconferenciaParticipante sinUsuario = new VideoconferenciaParticipante();
            sinUsuario.setRolSala(null);

            VideoconferenciaParticipante conUsuario = new VideoconferenciaParticipante();
            conUsuario.setUsuario(usuario("u-6", "Bruno", "bruno@dominio.com"));
            conUsuario.setRolSala(RolSala.PARTICIPANTE);

            when(participanteRepo.findByVideoconferenciaSalaUuid("Sala-2")).thenReturn(List.of(sinUsuario, conUsuario));

            var resultado = videoconferenciaServicio.listarParticipantes("Sala-2");

            assertEquals(2, resultado.size());
            assertNull(resultado.get(0).getUsuarioId());
            assertNull(resultado.get(0).getNombreUsuario());
            assertNull(resultado.get(0).getEmail());
            assertEquals(RolSala.PARTICIPANTE.name(), resultado.get(0).getRolSala());
            assertEquals("Bruno", resultado.get(1).getNombreUsuario());
        }
    }

    // =====================================================================
    // cambiarRolParticipanteEnSala
    // =====================================================================
    @Nested
    @DisplayName("cambiarRolParticipanteEnSala")
    class CambiarRolParticipanteEnSala {

        @Test
        @DisplayName("con salaUuid nulo lanza IllegalArgumentException")
        void salaUuidNuloLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala(null, "u-1", "ADMIN"));
        }

        @Test
        @DisplayName("con salaUuid en blanco lanza IllegalArgumentException")
        void salaUuidEnBlancoLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala("   ", "u-1", "ADMIN"));
        }

        @Test
        @DisplayName("con usuarioId nulo lanza IllegalArgumentException")
        void usuarioIdNuloLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", null, "ADMIN"));
        }

        @Test
        @DisplayName("con usuarioId en blanco lanza IllegalArgumentException")
        void usuarioIdEnBlancoLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "   ", "ADMIN"));
        }

        @Test
        @DisplayName("con nuevoRol nulo lanza IllegalArgumentException")
        void nuevoRolNuloLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", null));
        }

        @Test
        @DisplayName("con nuevoRol en blanco lanza IllegalArgumentException")
        void nuevoRolEnBlancoLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "   "));
        }

        @Test
        @DisplayName("con sala inexistente lanza IllegalArgumentException")
        void salaInexistenteLanzaExcepcion() {
            when(videoRepo.findBySalaUuid("Sala-x")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-x", "u-1", "ADMIN"));
        }

        @Test
        @DisplayName("con usuario inexistente lanza IllegalArgumentException")
        void usuarioInexistenteLanzaExcepcion() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-1")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "ADMIN"));
        }

        @Test
        @DisplayName("con nuevoRol invalido (no existe en el enum) lanza IllegalArgumentException")
        void nuevoRolInvalidoLanzaExcepcion() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            Usuario u = usuario("u-1", "Ana", "ana@dominio.com");
            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-1")).thenReturn(Optional.of(u));

            assertThrows(IllegalArgumentException.class, () ->
                    videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "SUPERADMIN"));
        }

        @Test
        @DisplayName("con participante existente conserva su valor de invitado (true)")
        void participanteExistenteInvitadoTrueConservaValor() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            Usuario u = usuario("u-1", "Ana", "ana@dominio.com");
            VideoconferenciaParticipante existente = new VideoconferenciaParticipante();
            existente.setId(1L);
            existente.setInvitado(true);

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-1")).thenReturn(Optional.of(u));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-1"))
                    .thenReturn(Optional.of(existente));
            when(participanteRepo.save(any(VideoconferenciaParticipante.class))).thenAnswer(inv -> inv.getArgument(0));

            var resultado = videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "presentador");

            assertEquals(RolSala.PRESENTADOR.name(), resultado.getRolSala());
            assertTrue(resultado.isInvitado());
            assertTrue(resultado.isDentroDeSala());
        }

        @Test
        @DisplayName("con participante existente conserva su valor de invitado (false)")
        void participanteExistenteInvitadoFalseConservaValor() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            Usuario u = usuario("u-1", "Ana", "ana@dominio.com");
            VideoconferenciaParticipante existente = new VideoconferenciaParticipante();
            existente.setId(1L);
            existente.setInvitado(false);

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-1")).thenReturn(Optional.of(u));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-1"))
                    .thenReturn(Optional.of(existente));
            when(participanteRepo.save(any(VideoconferenciaParticipante.class))).thenAnswer(inv -> inv.getArgument(0));

            var resultado = videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "ADMIN");

            assertFalse(resultado.isInvitado());
        }

        @Test
        @DisplayName("con participante nuevo y sala sin creador queda marcado como invitado")
        void participanteNuevoSalaSinCreadorQuedaInvitado() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setCreador(null);
            Usuario u = usuario("u-1", "Ana", "ana@dominio.com");

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-1")).thenReturn(Optional.of(u));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-1"))
                    .thenReturn(Optional.empty());
            when(participanteRepo.save(any(VideoconferenciaParticipante.class))).thenAnswer(inv -> inv.getArgument(0));

            var resultado = videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "ADMIN");

            assertTrue(resultado.isInvitado());
        }

        @Test
        @DisplayName("con participante nuevo y sala con creador distinto queda marcado como invitado")
        void participanteNuevoSalaConCreadorDistintoQuedaInvitado() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            sala.setCreador(usuario("creador-1", "Creador", "creador@dominio.com"));
            Usuario u = usuario("u-1", "Ana", "ana@dominio.com");

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-1")).thenReturn(Optional.of(u));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-1"))
                    .thenReturn(Optional.empty());
            when(participanteRepo.save(any(VideoconferenciaParticipante.class))).thenAnswer(inv -> inv.getArgument(0));

            var resultado = videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "ADMIN");

            assertTrue(resultado.isInvitado());
        }

        @Test
        @DisplayName("con participante nuevo y sala cuyo creador es el propio usuario no queda marcado como invitado")
        void participanteNuevoEsElCreadorNoQuedaInvitado() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            Usuario creador = usuario("u-1", "Ana", "ana@dominio.com");
            sala.setCreador(creador);

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-1")).thenReturn(Optional.of(creador));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-1"))
                    .thenReturn(Optional.empty());
            when(participanteRepo.save(any(VideoconferenciaParticipante.class))).thenAnswer(inv -> inv.getArgument(0));

            var resultado = videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "ADMIN");

            assertFalse(resultado.isInvitado());
        }

        @Test
        @DisplayName("el nuevoRol se normaliza recortando espacios y a mayusculas")
        void nuevoRolSeNormalizaTrimYMayusculas() {
            Videoconferencia sala = new Videoconferencia();
            sala.setSalaUuid("Sala-1");
            Usuario u = usuario("u-1", "Ana", "ana@dominio.com");

            when(videoRepo.findBySalaUuid("Sala-1")).thenReturn(Optional.of(sala));
            when(usuarioRepo.findById("u-1")).thenReturn(Optional.of(u));
            when(participanteRepo.findByVideoconferenciaSalaUuidAndUsuarioId("Sala-1", "u-1"))
                    .thenReturn(Optional.empty());
            when(participanteRepo.save(any(VideoconferenciaParticipante.class))).thenAnswer(inv -> inv.getArgument(0));

            var resultado = videoconferenciaServicio.cambiarRolParticipanteEnSala("Sala-1", "u-1", "  participante  ");

            assertEquals(RolSala.PARTICIPANTE.name(), resultado.getRolSala());
        }
    }
}
