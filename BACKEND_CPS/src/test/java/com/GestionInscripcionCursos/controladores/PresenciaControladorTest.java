package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.UsuarioConectadoDto;
import com.GestionInscripcionCursos.dto.UsuarioResumenDto;
import com.GestionInscripcionCursos.servicios.PresenciaUsuarioServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link PresenciaControlador}: registro de actividad
 * ("ping"), listado de usuarios conectados/todos y el stream SSE.
 */
@ExtendWith(MockitoExtension.class)
class PresenciaControladorTest {

    @Mock
    private PresenciaUsuarioServicio presenciaUsuarioServicio;

    @Mock
    private UsuarioServicio usuarioServicio;

    @InjectMocks
    private PresenciaControlador presenciaControlador;

    @Nested
    @DisplayName("ping")
    class Ping {

        @Test
        @DisplayName("registra la actividad del usuario autenticado y responde 200")
        void registraActividadYResponde200() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("usuario@test.com");
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ALUMNO"));
            doReturn(authorities).when(auth).getAuthorities();

            ResponseEntity<?> resultado = presenciaControlador.ping(auth);

            verify(presenciaUsuarioServicio).registrarActividad("usuario@test.com", authorities);
            assertEquals(200, resultado.getStatusCode().value());
        }
    }

    @Nested
    @DisplayName("listarConectados")
    class ListarConectados {

        @Test
        @DisplayName("responde 200 con los usuarios conectados")
        void listaConectados() {
            List<UsuarioConectadoDto> conectados = List.of(
                    new UsuarioConectadoDto("usuario@test.com", "ALUMNO", Instant.now(), Instant.now()));
            when(presenciaUsuarioServicio.listarConectados()).thenReturn(conectados);

            ResponseEntity<List<UsuarioConectadoDto>> resultado = presenciaControlador.listarConectados();

            assertEquals(200, resultado.getStatusCode().value());
            assertEquals(conectados, resultado.getBody());
        }
    }

    @Nested
    @DisplayName("listarTodos")
    class ListarTodos {

        @Test
        @DisplayName("responde 200 con el resumen de todos los usuarios")
        void listaTodos() {
            List<UsuarioResumenDto> usuarios = List.of(
                    new UsuarioResumenDto("id-1", "Usuario Uno", "uno@test.com", "ALUMNO", true, null));
            when(usuarioServicio.listarUsuarios()).thenReturn(usuarios);

            ResponseEntity<List<UsuarioResumenDto>> resultado = presenciaControlador.listarTodos();

            assertEquals(200, resultado.getStatusCode().value());
            assertEquals(usuarios, resultado.getBody());
        }
    }

    @Nested
    @DisplayName("streamConectados")
    class StreamConectados {

        @Test
        @DisplayName("crea un SseEmitter sin timeout y lo registra en el servicio de presencia")
        void creaYRegistraEmisor() {
            SseEmitter emisor = presenciaControlador.streamConectados();

            assertNotNull(emisor);
            ArgumentCaptor<SseEmitter> captor = ArgumentCaptor.forClass(SseEmitter.class);
            verify(presenciaUsuarioServicio).registrarEmisor(captor.capture());
            assertSame(emisor, captor.getValue());
        }
    }
}
