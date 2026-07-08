package com.GestionInscripcionCursos.servicios;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link CohereServicio}.
 *
 * <p>El {@code HttpClient} usado para llamar a la API de Cohere se crea internamente
 * en el constructor ({@code HttpClient.newHttpClient()}), no se inyecta, por lo que
 * no puede mockearse con Mockito puro y una llamada exitosa u con error HTTP real
 * requeriria red. Siguiendo el mismo criterio aplicado a otros servicios de IA del
 * proyecto, estas pruebas se limitan a las ramas que no requieren red: la
 * validacion de configuracion de la API key, la resolucion del modelo por defecto,
 * y el armado/serializacion del cuerpo del request (que ocurre antes de la llamada
 * HTTP), forzando el fallo de serializacion para verificar el mapeo de errores sin
 * llegar a abrir una conexion real.
 */
@ExtendWith(MockitoExtension.class)
class CohereServicioTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CohereServicio cohereServicio;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cohereServicio, "apiKey", "");
        ReflectionTestUtils.setField(cohereServicio, "modelo", "command-r-08-2024");
    }

    // =====================================================================
    // estaConfigurado
    // =====================================================================
    @Nested
    @DisplayName("estaConfigurado")
    class EstaConfigurado {

        @Test
        @DisplayName("con apiKey nula retorna false")
        void conApiKeyNulaRetornaFalse() {
            ReflectionTestUtils.setField(cohereServicio, "apiKey", null);
            assertFalse(cohereServicio.estaConfigurado());
        }

        @Test
        @DisplayName("con apiKey en blanco retorna false")
        void conApiKeyEnBlancoRetornaFalse() {
            ReflectionTestUtils.setField(cohereServicio, "apiKey", "   ");
            assertFalse(cohereServicio.estaConfigurado());
        }

        @Test
        @DisplayName("con apiKey presente retorna true")
        void conApiKeyPresenteRetornaTrue() {
            ReflectionTestUtils.setField(cohereServicio, "apiKey", "clave-valida");
            assertTrue(cohereServicio.estaConfigurado());
        }
    }

    // =====================================================================
    // getModelo
    // =====================================================================
    @Nested
    @DisplayName("getModelo")
    class GetModelo {

        @Test
        @DisplayName("con modelo configurado retorna ese valor")
        void conModeloConfiguradoRetornaValor() {
            ReflectionTestUtils.setField(cohereServicio, "modelo", "command-r-plus");
            assertEquals("command-r-plus", cohereServicio.getModelo());
        }

        @Test
        @DisplayName("con modelo nulo retorna command-r por defecto")
        void conModeloNuloRetornaDefault() {
            ReflectionTestUtils.setField(cohereServicio, "modelo", null);
            assertEquals("command-r", cohereServicio.getModelo());
        }

        @Test
        @DisplayName("con modelo en blanco retorna command-r por defecto")
        void conModeloEnBlancoRetornaDefault() {
            ReflectionTestUtils.setField(cohereServicio, "modelo", "  ");
            assertEquals("command-r", cohereServicio.getModelo());
        }
    }

    // =====================================================================
    // generarTexto
    // =====================================================================
    @Nested
    @DisplayName("generarTexto")
    class GenerarTexto {

        @Test
        @DisplayName("sin apiKey configurada lanza IllegalStateException y no intenta serializar ni llamar a la red")
        void sinApiKeyLanzaExcepcion() {
            ReflectionTestUtils.setField(cohereServicio, "apiKey", "");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> cohereServicio.generarTexto("Genera una rubrica"));

            assertEquals("Cohere API key no configurada", ex.getMessage());
            verifyNoInteractions(objectMapper);
        }

        @Test
        @DisplayName("con apiKey en blanco (solo espacios) tambien lanza IllegalStateException")
        void conApiKeyDeEspaciosLanzaExcepcion() {
            ReflectionTestUtils.setField(cohereServicio, "apiKey", "   ");

            assertThrows(IllegalStateException.class, () -> cohereServicio.generarTexto("prompt"));
            verifyNoInteractions(objectMapper);
        }

        @Test
        @DisplayName("con modelo en blanco arma el cuerpo del request usando command-r por defecto")
        void modeloEnBlancoUsaCommandRPorDefecto() throws JsonProcessingException {
            ReflectionTestUtils.setField(cohereServicio, "apiKey", "clave-valida");
            ReflectionTestUtils.setField(cohereServicio, "modelo", "   ");

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            when(objectMapper.writeValueAsString(captor.capture()))
                    .thenThrow(new JsonProcessingException("fallo de serializacion simulado") {});

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> cohereServicio.generarTexto("Hola Cohere"));
            assertEquals("Error al comunicarse con Cohere", ex.getMessage());

            @SuppressWarnings("unchecked")
            Map<String, Object> cuerpo = (Map<String, Object>) captor.getValue();
            assertEquals("command-r", cuerpo.get("model"));
            assertEquals("Hola Cohere", cuerpo.get("message"));
            assertEquals(4000, cuerpo.get("max_tokens"));
            assertEquals(0.7, cuerpo.get("temperature"));
        }

        @Test
        @DisplayName("con modelo configurado lo incluye tal cual en el cuerpo del request")
        void modeloConfiguradoSeIncluyeEnElCuerpo() throws JsonProcessingException {
            ReflectionTestUtils.setField(cohereServicio, "apiKey", "clave-valida");
            ReflectionTestUtils.setField(cohereServicio, "modelo", "command-r-plus");

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            when(objectMapper.writeValueAsString(captor.capture()))
                    .thenThrow(new JsonProcessingException("fallo de serializacion simulado") {});

            assertThrows(IllegalStateException.class, () -> cohereServicio.generarTexto("prompt"));

            @SuppressWarnings("unchecked")
            Map<String, Object> cuerpo = (Map<String, Object>) captor.getValue();
            assertEquals("command-r-plus", cuerpo.get("model"));
        }

        @Test
        @DisplayName("si la serializacion del cuerpo falla, envuelve el error en IllegalStateException con causa")
        void falloDeSerializacionEnvuelveEnIllegalStateException() throws JsonProcessingException {
            ReflectionTestUtils.setField(cohereServicio, "apiKey", "clave-valida");
            JsonProcessingException causa = new JsonProcessingException("boom") {};
            when(objectMapper.writeValueAsString(any())).thenThrow(causa);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> cohereServicio.generarTexto("prompt"));

            assertEquals("Error al comunicarse con Cohere", ex.getMessage());
            assertEquals(causa, ex.getCause());
        }
    }
}
