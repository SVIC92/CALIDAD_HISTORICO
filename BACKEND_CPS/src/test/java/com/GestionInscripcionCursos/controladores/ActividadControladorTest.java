package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ActividadServicio;
import com.GestionInscripcionCursos.servicios.CursoServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ActividadControlador}: registro, modificacion,
 * listado por curso (con el rol del usuario autenticado) y eliminacion de
 * actividades.
 */
@ExtendWith(MockitoExtension.class)
class ActividadControladorTest {

    @Mock
    private ActividadServicio actividadServicio;

    @Mock
    private CursoServicio cursoServicio;

    @Mock
    private UsuarioServicio usuarioServicio;

    @InjectMocks
    private ActividadControlador actividadControlador;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Date fechaFutura() {
        return new Date(System.currentTimeMillis() + 3_600_000);
    }

    private Usuario usuario(String id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        return u;
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("devuelve el curso encontrado por id")
        void devuelveCurso() {
            Curso c = new Curso();
            c.setId("c-1");
            when(cursoServicio.buscarPorId("c-1")).thenReturn(c);

            ResponseEntity<?> respuesta = actividadControlador.registrar("c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(c, respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("con datos validos registra la actividad y devuelve 201")
        void registroValidoDevuelveCreated() throws MyException {
            Date vencimiento = fechaFutura();

            ResponseEntity<?> respuesta = actividadControlador.registro(
                    "c-1", "Tarea 1", "Descripcion", vencimiento, 2);

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Actividad registrada correctamente"), respuesta.getBody());
            verify(actividadServicio).crearActividad("Tarea 1", "Descripcion", vencimiento, 2, "c-1");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void registroConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Los intentos permitidos deben estar entre 1 y 3"))
                    .when(actividadServicio).crearActividad(anyString(), anyString(), any(Date.class), anyInt(), anyString());

            ResponseEntity<?> respuesta = actividadControlador.registro(
                    "c-1", "Tarea 1", "Descripcion", fechaFutura(), 9);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Los intentos permitidos deben estar entre 1 y 3"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("modificar (POST)")
    class ModificarPost {

        @Test
        @DisplayName("con datos validos modifica la actividad")
        void modificarValido() throws MyException {
            ResponseEntity<?> respuesta = actividadControlador.modificar(
                    "a-1", "Tarea mod", "Descripcion mod", fechaFutura(), 3);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Actividad modificada correctamente"), respuesta.getBody());
            verify(actividadServicio).modificarActividad(eq("a-1"), eq("Tarea mod"), eq("Descripcion mod"), any(Date.class), eq(3));
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void modificarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Actividad no encontrada"))
                    .when(actividadServicio).modificarActividad(anyString(), anyString(), anyString(), any(Date.class), anyInt());

            ResponseEntity<?> respuesta = actividadControlador.modificar(
                    "a-1", "Tarea mod", "Descripcion mod", fechaFutura(), 3);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Actividad no encontrada"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("devuelve el rol del usuario autenticado y las actividades del curso")
        void listarDevuelveRolYActividades() {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(usuario("u-1", Rol.ALUMNO));
            List<Actividad> actividades = List.of(new Actividad());
            when(actividadServicio.listarActividadesPorIdCurso("c-1")).thenReturn(actividades);

            ResponseEntity<?> respuesta = actividadControlador.listar("c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("rol", Rol.ALUMNO, "actividades", actividades), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("modificar (GET)")
    class ModificarGet {

        @Test
        @DisplayName("devuelve la actividad encontrada por id")
        void devuelveActividad() {
            Actividad a = new Actividad();
            a.setId("a-1");
            when(actividadServicio.buscarPorId("a-1")).thenReturn(a);

            ResponseEntity<?> respuesta = actividadControlador.modificar("a-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(a, respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("elimina la actividad correctamente")
        void eliminaCorrectamente() throws MyException {
            ResponseEntity<?> respuesta = actividadControlador.eliminar("a-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Actividad eliminada correctamente"), respuesta.getBody());
            verify(actividadServicio).eliminarActividad("a-1");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void eliminarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Actividad no encontrada")).when(actividadServicio).eliminarActividad("a-1");

            ResponseEntity<?> respuesta = actividadControlador.eliminar("a-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Actividad no encontrada"), respuesta.getBody());
        }
    }
}
