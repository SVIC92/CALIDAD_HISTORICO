package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.entidades.Rubrica;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ActividadServicio;
import com.GestionInscripcionCursos.servicios.RubricaServicio;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link RubricaControlador}: alta de rubricas generadas
 * por IA, consulta por curso/id y asignacion a una actividad.
 */
@ExtendWith(MockitoExtension.class)
class RubricaControladorTest {

    @Mock
    private RubricaServicio rubricaServicio;

    @Mock
    private ActividadServicio actividadServicio;

    @InjectMocks
    private RubricaControlador rubricaControlador;

    private RubricaGeneradaDto dtoValido() {
        return new RubricaGeneradaDto(
                "Titulo", "Desc", "Sumas", "Primaria", "Matematica", "Individual",
                20, List.of(), true, "modelo-x", Instant.now());
    }

    @Nested
    @DisplayName("guardar")
    class Guardar {

        @Test
        @DisplayName("con datos validos responde 201 con la rubrica guardada")
        void guardaRubricaValida() throws MyException {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("profesor@test.com");
            Rubrica guardada = new Rubrica();
            guardada.setId("rub-1");
            RubricaGeneradaDto dto = dtoValido();
            when(rubricaServicio.guardarDesdeGeneracion(dto, "curso-1", "profesor@test.com")).thenReturn(guardada);

            ResponseEntity<Object> resultado = rubricaControlador.guardar(dto, "curso-1", auth);

            assertEquals(HttpStatus.CREATED, resultado.getStatusCode());
            assertEquals(guardada, resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException responde 400")
        void errorServicioResponde400() throws MyException {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("profesor@test.com");
            RubricaGeneradaDto dto = dtoValido();
            when(rubricaServicio.guardarDesdeGeneracion(dto, "curso-1", "profesor@test.com"))
                    .thenThrow(new MyException("Curso no encontrado"));

            ResponseEntity<Object> resultado = rubricaControlador.guardar(dto, "curso-1", auth);

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
            assertEquals(Map.of("error", "Curso no encontrado"), resultado.getBody());
        }
    }

    @Nested
    @DisplayName("listarPorCurso")
    class ListarPorCurso {

        @Test
        @DisplayName("responde 200 con las rubricas del curso")
        void listaRubricas() {
            List<Rubrica> rubricas = List.of(new Rubrica());
            when(rubricaServicio.listarPorCurso("curso-1")).thenReturn(rubricas);

            ResponseEntity<List<Rubrica>> resultado = rubricaControlador.listarPorCurso("curso-1");

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(rubricas, resultado.getBody());
        }
    }

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("con id existente responde 200 con la rubrica")
        void encuentraRubrica() throws MyException {
            Rubrica rubrica = new Rubrica();
            rubrica.setId("rub-1");
            when(rubricaServicio.buscarPorId("rub-1")).thenReturn(rubrica);

            ResponseEntity<Object> resultado = rubricaControlador.buscarPorId("rub-1");

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(rubrica, resultado.getBody());
        }

        @Test
        @DisplayName("con id inexistente responde 404")
        void noEncuentraRubrica() throws MyException {
            when(rubricaServicio.buscarPorId("rub-x")).thenThrow(new MyException("Rubrica no encontrada"));

            ResponseEntity<Object> resultado = rubricaControlador.buscarPorId("rub-x");

            assertEquals(HttpStatus.NOT_FOUND, resultado.getStatusCode());
            assertEquals(Map.of("error", "Rubrica no encontrada"), resultado.getBody());
        }
    }

    @Nested
    @DisplayName("asignarAActividad")
    class AsignarAActividad {

        @Test
        @DisplayName("asigna la rubrica a la actividad y responde 200")
        void asignaCorrectamente() throws MyException {
            ResponseEntity<Object> resultado = rubricaControlador.asignarAActividad("rub-1", "act-1");

            verify(actividadServicio).asignarRubrica("act-1", "rub-1");
            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(Map.of("mensaje", "Rúbrica asignada a la actividad correctamente"), resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException responde 400")
        void errorAsignandoResponde400() throws MyException {
            org.mockito.Mockito.doThrow(new MyException("Actividad no encontrada"))
                    .when(actividadServicio).asignarRubrica("act-x", "rub-1");

            ResponseEntity<Object> resultado = rubricaControlador.asignarAActividad("rub-1", "act-x");

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
            assertEquals(Map.of("error", "Actividad no encontrada"), resultado.getBody());
        }
    }
}
