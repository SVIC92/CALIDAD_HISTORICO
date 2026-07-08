package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.InscripcionServicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link InscripcionControlador}: listados por rol y
 * flujos de aprobacion/rechazo de inscripciones de alumnos y profesores.
 */
@ExtendWith(MockitoExtension.class)
class InscripcionControladorTest {

    @Mock
    private InscripcionServicio inscripcionServicio;

    @InjectMocks
    private InscripcionControlador inscripcionControlador;

    @Nested
    @DisplayName("listados")
    class Listados {

        @Test
        @DisplayName("listaPendientesProfesor devuelve la lista del servicio")
        void listaPendientesProfesor() {
            when(inscripcionServicio.listaPendientesProfesor()).thenReturn(List.of(new Inscripcion()));

            ResponseEntity<List<Inscripcion>> respuesta = inscripcionControlador.listaPendientesProfesor();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listaRealizadasProfesor devuelve la lista del servicio")
        void listaRealizadasProfesor() {
            when(inscripcionServicio.listaRealizadasProfesor()).thenReturn(List.of(new Inscripcion()));

            ResponseEntity<List<Inscripcion>> respuesta = inscripcionControlador.listaRealizadasProfesor();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listaPendientesAlumno devuelve la lista del servicio")
        void listaPendientesAlumno() {
            when(inscripcionServicio.listaPendientesAlumno()).thenReturn(List.of(new Inscripcion()));

            ResponseEntity<List<Inscripcion>> respuesta = inscripcionControlador.listaPendientesAlumno();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listaRealizadasAlumno devuelve la lista del servicio")
        void listaRealizadasAlumno() {
            when(inscripcionServicio.listaRealizadasAlumno()).thenReturn(List.of(new Inscripcion()));

            ResponseEntity<List<Inscripcion>> respuesta = inscripcionControlador.listaRealizadasAlumno();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("las listas vacias tambien se responden con OK")
        void listasVaciasDevuelvenOk() {
            when(inscripcionServicio.listaPendientesProfesor()).thenReturn(List.of());

            ResponseEntity<List<Inscripcion>> respuesta = inscripcionControlador.listaPendientesProfesor();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertTrue(respuesta.getBody().isEmpty());
        }
    }

    @Nested
    @DisplayName("aprobar")
    class Aprobar {

        @Test
        @DisplayName("aprueba correctamente la inscripcion del alumno")
        void apruebaCorrectamente() throws MyException {
            ResponseEntity<?> respuesta = inscripcionControlador.aprobar("i-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Inscripcion aprobada correctamente"), respuesta.getBody());
            verify(inscripcionServicio).aprobar("i-1");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void aprobarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Inscripcion no encontrada")).when(inscripcionServicio).aprobar("i-1");

            ResponseEntity<?> respuesta = inscripcionControlador.aprobar("i-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Inscripcion no encontrada"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("rechazar")
    class Rechazar {

        @Test
        @DisplayName("rechaza correctamente la inscripcion del alumno")
        void rechazaCorrectamente() throws MyException {
            ResponseEntity<?> respuesta = inscripcionControlador.rechazar("i-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Inscripcion rechazada correctamente"), respuesta.getBody());
            verify(inscripcionServicio).rechazar("i-1");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void rechazarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Inscripcion no encontrada")).when(inscripcionServicio).rechazar("i-1");

            ResponseEntity<?> respuesta = inscripcionControlador.rechazar("i-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Inscripcion no encontrada"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("aprobarProfesor")
    class AprobarProfesor {

        @Test
        @DisplayName("aprueba y asigna el profesor al curso")
        void apruebaCorrectamente() throws MyException {
            ResponseEntity<?> respuesta = inscripcionControlador.aprobarProfesor("i-2");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Inscripcion de profesor aprobada y asignada al curso"), respuesta.getBody());
            verify(inscripcionServicio).aprobarInscripcionProfesor("i-2");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void aprobarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("El curso ya tiene otro profesor asignado"))
                    .when(inscripcionServicio).aprobarInscripcionProfesor("i-2");

            ResponseEntity<?> respuesta = inscripcionControlador.aprobarProfesor("i-2");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "El curso ya tiene otro profesor asignado"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("rechazarProfesor")
    class RechazarProfesor {

        @Test
        @DisplayName("rechaza correctamente la inscripcion del profesor")
        void rechazaCorrectamente() throws MyException {
            ResponseEntity<?> respuesta = inscripcionControlador.rechazarProfesor("i-2");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Inscripcion de profesor rechazada correctamente"), respuesta.getBody());
            verify(inscripcionServicio).rechazarInscripcionProfesor("i-2");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void rechazarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Inscripcion no encontrada"))
                    .when(inscripcionServicio).rechazarInscripcionProfesor("i-2");

            ResponseEntity<?> respuesta = inscripcionControlador.rechazarProfesor("i-2");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Inscripcion no encontrada"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("inscribirAlumnoDirecto")
    class InscribirAlumnoDirecto {

        @Test
        @DisplayName("inscribe correctamente al alumno en el curso")
        void inscribeCorrectamente() throws MyException {
            ResponseEntity<?> respuesta = inscripcionControlador.inscribirAlumnoDirecto("u-1", "c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Alumno inscrito directamente al curso"), respuesta.getBody());
            verify(inscripcionServicio).inscribirAlumnoDirecto("u-1", "c-1");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void inscribirConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("El alumno ya esta inscrito en este curso"))
                    .when(inscripcionServicio).inscribirAlumnoDirecto("u-1", "c-1");

            ResponseEntity<?> respuesta = inscripcionControlador.inscribirAlumnoDirecto("u-1", "c-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "El alumno ya esta inscrito en este curso"), respuesta.getBody());
        }
    }
}
