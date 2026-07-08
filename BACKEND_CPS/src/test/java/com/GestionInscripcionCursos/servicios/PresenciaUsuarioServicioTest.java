package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.UsuarioConectadoDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link PresenciaUsuarioServicio}: registro de
 * actividad/presencia de usuarios, listado ordenado, limpieza de sesiones
 * inactivas y notificacion via SSE.
 */
@ExtendWith(MockitoExtension.class)
class PresenciaUsuarioServicioTest {

    private PresenciaUsuarioServicio servicio;

    @BeforeEach
    void setUp() {
        servicio = new PresenciaUsuarioServicio(90);
    }

    // =====================================================================
    // registrarActividad
    // =====================================================================
    @Nested
    @DisplayName("registrarActividad")
    class RegistrarActividad {

        @Test
        @DisplayName("con usuario nuevo lo agrega con rol sin el prefijo ROLE_")
        void usuarioNuevoSeAgregaConRolNormalizado() {
            servicio.registrarActividad("alumno@dominio.com", List.of(new SimpleGrantedAuthority("ROLE_ALUMNO")));

            List<UsuarioConectadoDto> conectados = servicio.listarConectados();

            assertEquals(1, conectados.size());
            assertEquals("alumno@dominio.com", conectados.get(0).email());
            assertEquals("ALUMNO", conectados.get(0).rol());
            assertNotNull(conectados.get(0).conectadoDesde());
            assertNotNull(conectados.get(0).ultimaActividad());
        }

        @Test
        @DisplayName("sin authorities asigna rol nulo")
        void sinAuthoritiesAsignaRolNulo() {
            servicio.registrarActividad("usuario@dominio.com", List.of());

            List<UsuarioConectadoDto> conectados = servicio.listarConectados();

            assertEquals(1, conectados.size());
            assertNull(conectados.get(0).rol());
        }

        @Test
        @DisplayName("con authorities nulas asigna rol nulo")
        void authoritiesNulasAsignaRolNulo() {
            servicio.registrarActividad("usuario@dominio.com", null);

            List<UsuarioConectadoDto> conectados = servicio.listarConectados();

            assertNull(conectados.get(0).rol());
        }

        @Test
        @DisplayName("con usuario ya conectado mantiene conectadoDesde y actualiza ultimaActividad")
        void usuarioExistenteMantieneConectadoDesde() {
            servicio.registrarActividad("alumno@dominio.com", List.of(new SimpleGrantedAuthority("ROLE_ALUMNO")));
            var primero = servicio.listarConectados().get(0);

            servicio.registrarActividad("alumno@dominio.com", List.of(new SimpleGrantedAuthority("ROLE_ALUMNO")));
            var segundo = servicio.listarConectados().get(0);

            assertEquals(primero.conectadoDesde(), segundo.conectadoDesde());
            assertTrue(segundo.ultimaActividad().isAfter(primero.ultimaActividad())
                    || segundo.ultimaActividad().equals(primero.ultimaActividad()));
        }

        @Test
        @DisplayName("con usuario existente y sin authorities conserva el rol anterior")
        void usuarioExistenteSinAuthoritiesConservaRol() {
            servicio.registrarActividad("alumno@dominio.com", List.of(new SimpleGrantedAuthority("ROLE_ALUMNO")));

            servicio.registrarActividad("alumno@dominio.com", List.of());

            assertEquals("ALUMNO", servicio.listarConectados().get(0).rol());
        }

        @Test
        @DisplayName("authority sin prefijo ROLE_ se conserva tal cual")
        void authoritySinPrefijoSeConservaTalCual() {
            servicio.registrarActividad("alumno@dominio.com", List.of(new SimpleGrantedAuthority("ADMIN")));

            assertEquals("ADMIN", servicio.listarConectados().get(0).rol());
        }
    }

    // =====================================================================
    // listarConectados
    // =====================================================================
    @Nested
    @DisplayName("listarConectados")
    class ListarConectados {

        @Test
        @DisplayName("ordena a los usuarios por email ignorando mayusculas/minusculas")
        void ordenaPorEmailIgnorandoCase() {
            servicio.registrarActividad("Zeta@dominio.com", List.of());
            servicio.registrarActividad("alfa@dominio.com", List.of());
            servicio.registrarActividad("Beta@dominio.com", List.of());

            List<UsuarioConectadoDto> conectados = servicio.listarConectados();

            assertEquals(3, conectados.size());
            assertEquals("alfa@dominio.com", conectados.get(0).email());
            assertEquals("Beta@dominio.com", conectados.get(1).email());
            assertEquals("Zeta@dominio.com", conectados.get(2).email());
        }

        @Test
        @DisplayName("sin usuarios registrados retorna lista vacia")
        void sinUsuariosRetornaListaVacia() {
            assertTrue(servicio.listarConectados().isEmpty());
        }

        @Test
        @DisplayName("descarta sesiones inactivas segun el timeout configurado")
        void descartaSesionesInactivas() {
            PresenciaUsuarioServicio servicioTimeoutCorto = new PresenciaUsuarioServicio(0);
            Instant momentoRegistro = Instant.now();
            servicioTimeoutCorto.establecerClockParaPruebas(Clock.fixed(momentoRegistro, ZoneId.systemDefault()));
            servicioTimeoutCorto.registrarActividad("alumno@dominio.com", List.of());

            // Simula el paso del tiempo sin usar Thread.sleep: adelantamos el reloj interno.
            servicioTimeoutCorto.establecerClockParaPruebas(
                    Clock.fixed(momentoRegistro.plusMillis(50), ZoneId.systemDefault()));

            assertTrue(servicioTimeoutCorto.listarConectados().isEmpty());
        }
    }

    // =====================================================================
    // registrarEmisor / notificacion SSE
    // =====================================================================
    @Nested
    @DisplayName("registrarEmisor y notificacion SSE")
    class RegistrarEmisor {

        @Test
        @DisplayName("al registrar un emisor le envia el snapshot inicial")
        void registrarEmisorEnviaSnapshotInicial() throws IOException {
            SseEmitter emisor = mock(SseEmitter.class);

            servicio.registrarEmisor(emisor);

            verify(emisor, times(1)).send(any(SseEmitter.SseEventBuilder.class));
            verify(emisor).onCompletion(any());
            verify(emisor).onTimeout(any());
            verify(emisor).onError(any());
        }

        @Test
        @DisplayName("al registrar actividad notifica a los emisores registrados")
        void registrarActividadNotificaEmisores() throws IOException {
            SseEmitter emisor = mock(SseEmitter.class);
            servicio.registrarEmisor(emisor);
            reset(emisor);

            servicio.registrarActividad("alumno@dominio.com", List.of());

            verify(emisor, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("si el envio del snapshot falla con IOException, completa y remueve el emisor")
        void fallaEnvioSnapshotRemueveEmisor() throws IOException {
            SseEmitter emisor = mock(SseEmitter.class);
            doThrow(new IOException("desconectado")).when(emisor).send(any(SseEmitter.SseEventBuilder.class));

            servicio.registrarEmisor(emisor);

            verify(emisor).complete();

            // Al no estar mas en la lista de emisores, una nueva notificacion no le vuelve a enviar nada.
            reset(emisor);
            servicio.registrarActividad("otro@dominio.com", List.of());
            verify(emisor, never()).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("si el envio de la notificacion falla con IOException, completa y remueve el emisor")
        void fallaEnvioNotificacionRemueveEmisor() throws IOException {
            SseEmitter emisor = mock(SseEmitter.class);
            servicio.registrarEmisor(emisor);
            reset(emisor);
            doThrow(new IOException("desconectado")).when(emisor).send(any(SseEmitter.SseEventBuilder.class));

            servicio.registrarActividad("alumno@dominio.com", List.of());

            verify(emisor).complete();
        }
    }
}
