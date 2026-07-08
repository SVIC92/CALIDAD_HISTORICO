package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.IaChatRequestDto;
import com.GestionInscripcionCursos.dto.IaChatResponseDto;
import com.GestionInscripcionCursos.dto.IaConversacionDto;
import com.GestionInscripcionCursos.dto.IaHistorialDto;
import com.GestionInscripcionCursos.dto.IaSugerenciasDto;
import com.GestionInscripcionCursos.dto.RubricaGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.dto.SilaboGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.SilaboGeneradoDto;
import com.GestionInscripcionCursos.servicios.IaServicio;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link IaControlador}: chat asistido por IA,
 * historial de conversacion, sugerencias y generacion de rubricas/silabos.
 */
@ExtendWith(MockitoExtension.class)
class IaControladorTest {

    @Mock
    private IaServicio iaServicio;

    @InjectMocks
    private IaControlador iaControlador;

    private Authentication authValida(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    // =====================================================================
    // chatAlumno / chatProfesor / chatAdmin (ejecutarChatSeguro)
    // =====================================================================
    @Nested
    @DisplayName("chat por rol")
    class ChatPorRol {

        @Test
        @DisplayName("chatAlumno con datos validos responde 200 con la respuesta del servicio")
        void chatAlumnoValido() {
            Authentication auth = authValida("alumno@test.com");
            IaChatRequestDto request = new IaChatRequestDto("Hola, tengo una duda");
            IaChatResponseDto respuesta = new IaChatResponseDto("Respuesta IA", "ALUMNO", "modelo-x", Instant.now());
            when(iaServicio.chatearSegunRol("alumno@test.com", "ALUMNO", "Hola, tengo una duda")).thenReturn(respuesta);

            ResponseEntity<?> resultado = iaControlador.chatAlumno(request, auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(respuesta, resultado.getBody());
        }

        @Test
        @DisplayName("chatProfesor delega en el servicio con el rol PROFESOR")
        void chatProfesorValido() {
            Authentication auth = authValida("profesor@test.com");
            IaChatRequestDto request = new IaChatRequestDto("Necesito ayuda con una rubrica");
            IaChatResponseDto respuesta = new IaChatResponseDto("Respuesta IA", "PROFESOR", "modelo-x", Instant.now());
            when(iaServicio.chatearSegunRol("profesor@test.com", "PROFESOR", "Necesito ayuda con una rubrica")).thenReturn(respuesta);

            ResponseEntity<?> resultado = iaControlador.chatProfesor(request, auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(respuesta, resultado.getBody());
        }

        @Test
        @DisplayName("chatAdmin delega en el servicio con el rol ADMIN")
        void chatAdminValido() {
            Authentication auth = authValida("admin@test.com");
            IaChatRequestDto request = new IaChatRequestDto("Dame un resumen del sistema");
            IaChatResponseDto respuesta = new IaChatResponseDto("Respuesta IA", "ADMIN", "modelo-x", Instant.now());
            when(iaServicio.chatearSegunRol("admin@test.com", "ADMIN", "Dame un resumen del sistema")).thenReturn(respuesta);

            ResponseEntity<?> resultado = iaControlador.chatAdmin(request, auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(respuesta, resultado.getBody());
        }

        @Test
        @DisplayName("sin usuario autenticado responde 401")
        void sinAutenticacionResponde401() {
            IaChatRequestDto request = new IaChatRequestDto("Hola");

            ResponseEntity<?> resultado = iaControlador.chatAlumno(request, null);

            assertEquals(HttpStatus.UNAUTHORIZED, resultado.getStatusCode());
        }

        @Test
        @DisplayName("con nombre de usuario en blanco responde 401")
        void nombreUsuarioEnBlancoResponde401() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("   ");
            IaChatRequestDto request = new IaChatRequestDto("Hola");

            ResponseEntity<?> resultado = iaControlador.chatAlumno(request, auth);

            assertEquals(HttpStatus.UNAUTHORIZED, resultado.getStatusCode());
        }

        @Test
        @DisplayName("con mensaje en blanco responde 400")
        void mensajeEnBlancoResponde400() {
            Authentication auth = authValida("alumno@test.com");
            IaChatRequestDto request = new IaChatRequestDto("   ");

            ResponseEntity<?> resultado = iaControlador.chatAlumno(request, auth);

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
        }

        @Test
        @DisplayName("con request nulo responde 400")
        void requestNuloResponde400() {
            Authentication auth = authValida("alumno@test.com");

            ResponseEntity<?> resultado = iaControlador.chatAlumno(null, auth);

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
        }

        @Test
        @DisplayName("cuando el servicio lanza IllegalArgumentException responde 400")
        void servicioLanzaIllegalArgumentResponde400() {
            Authentication auth = authValida("alumno@test.com");
            IaChatRequestDto request = new IaChatRequestDto("Hola");
            when(iaServicio.chatearSegunRol(anyString(), anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException("mensaje invalido"));

            ResponseEntity<?> resultado = iaControlador.chatAlumno(request, auth);

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
            assertEquals(Map.of("error", "mensaje invalido"), resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza IllegalStateException responde 503 con detalle")
        void servicioLanzaIllegalStateResponde503() {
            Authentication auth = authValida("alumno@test.com");
            IaChatRequestDto request = new IaChatRequestDto("Hola");
            when(iaServicio.chatearSegunRol(anyString(), anyString(), anyString()))
                    .thenThrow(new IllegalStateException("sin api key"));

            ResponseEntity<?> resultado = iaControlador.chatAlumno(request, auth);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resultado.getStatusCode());
            assertTrue(resultado.getBody() instanceof Map);
            assertEquals("sin api key", ((Map<?, ?>) resultado.getBody()).get("detalle"));
        }

        @Test
        @DisplayName("cuando el servicio lanza una excepcion generica responde 500")
        void servicioLanzaExcepcionGenericaResponde500() {
            Authentication auth = authValida("alumno@test.com");
            IaChatRequestDto request = new IaChatRequestDto("Hola");
            when(iaServicio.chatearSegunRol(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("boom"));

            ResponseEntity<?> resultado = iaControlador.chatAlumno(request, auth);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resultado.getStatusCode());
        }
    }

    // =====================================================================
    // historial
    // =====================================================================
    @Nested
    @DisplayName("historial")
    class Historial {

        @Test
        @DisplayName("ultimoHistorial con datos presentes responde 200 con el dto")
        void ultimoHistorialPresente() {
            Authentication auth = authValida("alumno@test.com");
            IaHistorialDto dto = new IaHistorialDto("hola", "respuesta", "ALUMNO", "modelo-x", Instant.now());
            when(iaServicio.obtenerUltimoHistorial("alumno@test.com")).thenReturn(Optional.of(dto));

            ResponseEntity<?> resultado = iaControlador.ultimoHistorial(auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(dto, resultado.getBody());
        }

        @Test
        @DisplayName("ultimoHistorial sin datos responde 200 con mensaje informativo")
        void ultimoHistorialVacio() {
            Authentication auth = authValida("alumno@test.com");
            when(iaServicio.obtenerUltimoHistorial("alumno@test.com")).thenReturn(Optional.empty());

            ResponseEntity<?> resultado = iaControlador.ultimoHistorial(auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(Map.of("mensaje", "No hay historial disponible"), resultado.getBody());
        }

        @Test
        @DisplayName("conversacion responde 200 con la conversacion del servicio")
        void conversacionOk() {
            Authentication auth = authValida("alumno@test.com");
            IaConversacionDto conversacion = new IaConversacionDto(List.of(), 0);
            when(iaServicio.obtenerConversacion("alumno@test.com")).thenReturn(conversacion);

            ResponseEntity<IaConversacionDto> resultado = iaControlador.conversacion(auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(conversacion, resultado.getBody());
        }

        @Test
        @DisplayName("limpiarHistorial invoca al servicio y responde 200")
        void limpiarHistorialOk() {
            Authentication auth = authValida("alumno@test.com");

            ResponseEntity<?> resultado = iaControlador.limpiarHistorial(auth);

            verify(iaServicio).limpiarHistorial("alumno@test.com");
            assertEquals(HttpStatus.OK, resultado.getStatusCode());
        }
    }

    // =====================================================================
    // sugerencias
    // =====================================================================
    @Nested
    @DisplayName("sugerencias")
    class Sugerencias {

        @Test
        @DisplayName("normaliza el rol recibido a mayusculas antes de consultar el servicio")
        void normalizaRolAMayusculas() {
            IaSugerenciasDto dto = new IaSugerenciasDto("ALUMNO", List.of("sugerencia 1"));
            when(iaServicio.obtenerSugerencias("ALUMNO")).thenReturn(dto);

            ResponseEntity<IaSugerenciasDto> resultado = iaControlador.sugerencias("alumno");

            verify(iaServicio).obtenerSugerencias("ALUMNO");
            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(dto, resultado.getBody());
        }

        @Test
        @DisplayName("con rol nulo consulta el servicio con cadena vacia")
        void rolNuloConsultaConCadenaVacia() {
            IaSugerenciasDto dto = new IaSugerenciasDto("", List.of());
            when(iaServicio.obtenerSugerencias("")).thenReturn(dto);

            iaControlador.sugerencias(null);

            verify(iaServicio).obtenerSugerencias("");
        }
    }

    // =====================================================================
    // generarRubrica
    // =====================================================================
    @Nested
    @DisplayName("generarRubrica")
    class GenerarRubrica {

        private RubricaGeneracionRequestDto requestValido() {
            return new RubricaGeneracionRequestDto("Sumas", "Primaria", "Matematica", "Individual", 4, 3, 20);
        }

        @Test
        @DisplayName("con datos validos responde 200 con la rubrica generada")
        void generaRubricaValida() {
            Authentication auth = authValida("profesor@test.com");
            RubricaGeneradaDto dto = new RubricaGeneradaDto(
                    "Titulo", "Desc", "Sumas", "Primaria", "Matematica", "Individual",
                    20, List.of(), true, "modelo-x", Instant.now());
            when(iaServicio.generarRubrica(eq("profesor@test.com"), any(RubricaGeneracionRequestDto.class)))
                    .thenReturn(dto);

            ResponseEntity<?> resultado = iaControlador.generarRubrica(requestValido(), auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(dto, resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza IllegalArgumentException responde 400")
        void illegalArgumentResponde400() {
            Authentication auth = authValida("profesor@test.com");
            when(iaServicio.generarRubrica(anyString(), any())).thenThrow(new IllegalArgumentException("datos invalidos"));

            ResponseEntity<?> resultado = iaControlador.generarRubrica(requestValido(), auth);

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
        }

        @Test
        @DisplayName("cuando el servicio lanza una excepcion generica responde 500")
        void excepcionGenericaResponde500() {
            Authentication auth = authValida("profesor@test.com");
            when(iaServicio.generarRubrica(anyString(), any())).thenThrow(new RuntimeException("boom"));

            ResponseEntity<?> resultado = iaControlador.generarRubrica(requestValido(), auth);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resultado.getStatusCode());
        }
    }

    // =====================================================================
    // generarSilabo
    // =====================================================================
    @Nested
    @DisplayName("generarSilabo")
    class GenerarSilabo {

        private SilaboGeneracionRequestDto requestValido() {
            return new SilaboGeneracionRequestDto("curso1", "Curso X", "Sistemas", 3, 4, 16, "Descripcion breve");
        }

        @Test
        @DisplayName("con datos validos responde 200 con el silabo generado")
        void generaSilaboValido() {
            SilaboGeneradoDto dto = new SilaboGeneradoDto(null, List.of(), List.of(), "sumilla", "logro", List.of(), "sistema evaluacion");
            when(iaServicio.generarSilabo(requestValido())).thenReturn(dto);

            ResponseEntity<?> resultado = iaControlador.generarSilabo(requestValido());

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(dto, resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza IllegalArgumentException responde 400")
        void illegalArgumentResponde400() {
            when(iaServicio.generarSilabo(any())).thenThrow(new IllegalArgumentException("curso invalido"));

            ResponseEntity<?> resultado = iaControlador.generarSilabo(requestValido());

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
        }

        @Test
        @DisplayName("cuando el servicio lanza IllegalStateException responde 503 con detalle")
        void illegalStateResponde503() {
            when(iaServicio.generarSilabo(any())).thenThrow(new IllegalStateException("sin api key"));

            ResponseEntity<?> resultado = iaControlador.generarSilabo(requestValido());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resultado.getStatusCode());
            assertTrue(resultado.getBody() instanceof Map);
            assertEquals("sin api key", ((Map<?, ?>) resultado.getBody()).get("detalle"));
        }

        @Test
        @DisplayName("cuando IllegalStateException no trae mensaje usa detalle por defecto")
        void illegalStateSinMensajeUsaDetallePorDefecto() {
            when(iaServicio.generarSilabo(any())).thenThrow(new IllegalStateException());

            ResponseEntity<?> resultado = iaControlador.generarSilabo(requestValido());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resultado.getStatusCode());
            assertEquals("Sin detalle disponible", ((Map<?, ?>) resultado.getBody()).get("detalle"));
        }

        @Test
        @DisplayName("cuando el servicio lanza una excepcion generica responde 500")
        void excepcionGenericaResponde500() {
            when(iaServicio.generarSilabo(any())).thenThrow(new RuntimeException("boom"));

            ResponseEntity<?> resultado = iaControlador.generarSilabo(requestValido());

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resultado.getStatusCode());
        }
    }
}
