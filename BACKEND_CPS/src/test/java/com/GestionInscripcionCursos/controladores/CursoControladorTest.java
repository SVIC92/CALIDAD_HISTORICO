package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.CursoRequestDto;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.CursoPrerequisito;
import com.GestionInscripcionCursos.entidades.HorarioSesion;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.CursoServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link CursoControlador}: registro, listados,
 * modificacion/eliminacion, inscripcion, horarios y prerrequisitos de cursos.
 */
@ExtendWith(MockitoExtension.class)
class CursoControladorTest {

    @Mock
    private CursoServicio cursoServicio;

    @Mock
    private UsuarioServicio usuarioServicio;

    @InjectMocks
    private CursoControlador cursoControlador;

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

    private Usuario usuario(String id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        u.setNombre("Usuario " + id);
        return u;
    }

    private Curso curso(String id) {
        Curso c = new Curso();
        c.setId(id);
        c.setNombre("Curso " + id);
        return c;
    }

    // =====================================================================
    // registrar / registro
    // =====================================================================
    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("devuelve el mensaje del endpoint placeholder")
        void devuelveMensaje() {
            ResponseEntity<?> respuesta = cursoControlador.registrar();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Endpoint para registrar curso"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("con datos validos crea el curso y devuelve 201")
        void registroValidoDevuelveCreated() throws MyException {
            ResponseEntity<?> respuesta = cursoControlador.registro(
                    "Base de Datos II", "BD2", "Descripcion", 30, 4, 5, "PRESENCIAL",
                    new Date(), new Date(), 3, 2, 0, "ACTIVO", "Carrera X", "profe@dominio.com", null);

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            verify(cursoServicio).crearCurso(any(CursoRequestDto.class));
        }

        @Test
        @DisplayName("usa profesorId por encima de profesorAsignado cuando ambos estan presentes")
        void priorizaProfesorId() throws MyException {
            cursoControlador.registro(
                    "Curso", "COD", "Descripcion", 30, 4, 5, "PRESENCIAL",
                    new Date(), new Date(), 3, 2, 0, "ACTIVO", "Carrera X", "correoAsignado@dominio.com", "id-profesor-1");

            ArgumentCaptor<CursoRequestDto> captor = ArgumentCaptor.forClass(CursoRequestDto.class);
            verify(cursoServicio).crearCurso(captor.capture());
            assertEquals("id-profesor-1", captor.getValue().profesorReferencia());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void servicioLanzaExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Ya existe un curso con el codigo BD2"))
                    .when(cursoServicio).crearCurso(any(CursoRequestDto.class));

            ResponseEntity<?> respuesta = cursoControlador.registro(
                    "Base de Datos II", "BD2", "Descripcion", 30, 4, 5, "PRESENCIAL",
                    new Date(), new Date(), 3, 2, 0, "ACTIVO", null, null, null);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Ya existe un curso con el codigo BD2"), respuesta.getBody());
        }
    }

    // =====================================================================
    // listados
    // =====================================================================
    @Nested
    @DisplayName("listados")
    class Listados {

        @Test
        @DisplayName("listar devuelve la lista completa de cursos")
        void listarDevuelveCursos() {
            when(cursoServicio.listarCursos()).thenReturn(List.of(curso("c-1")));

            ResponseEntity<List<Curso>> respuesta = cursoControlador.listar();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listarActivos devuelve solo cursos activos")
        void listarActivosDevuelveCursos() {
            when(cursoServicio.listarCursosActivos()).thenReturn(List.of(curso("c-1")));

            ResponseEntity<List<Curso>> respuesta = cursoControlador.listarActivos();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listarCaducados devuelve cursos caducados")
        void listarCaducadosDevuelveCursos() {
            when(cursoServicio.listarCursosCaducados()).thenReturn(List.of(curso("c-1")));

            ResponseEntity<List<Curso>> respuesta = cursoControlador.listarCaducados();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }
    }

    // =====================================================================
    // modificar
    // =====================================================================
    @Nested
    @DisplayName("modificar")
    class Modificar {

        @Test
        @DisplayName("GET devuelve el curso buscado por id")
        void obtenerCursoParaModificar() {
            Curso c = curso("c-1");
            when(cursoServicio.buscarPorId("c-1")).thenReturn(c);

            ResponseEntity<Curso> respuesta = cursoControlador.modificar("c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(c, respuesta.getBody());
        }

        @Test
        @DisplayName("PUT con datos validos modifica y devuelve el curso actualizado")
        void modificarValidoDevuelveCursoActualizado() throws MyException {
            Curso actualizado = curso("c-1");
            when(cursoServicio.buscarPorId("c-1")).thenReturn(actualizado);

            ResponseEntity<?> respuesta = cursoControlador.modificar(
                    "c-1", "Curso mod", "COD", "Desc", 30, 4, 5, "PRESENCIAL",
                    new Date(), new Date(), 3, 2, 0, "ACTIVO", null, null, null);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(actualizado, respuesta.getBody());
            verify(cursoServicio).modificarCurso(eq("c-1"), any(CursoRequestDto.class));
        }

        @Test
        @DisplayName("PUT cuando el servicio lanza MyException devuelve badRequest")
        void modificarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Curso no encontrado"))
                    .when(cursoServicio).modificarCurso(eq("c-1"), any(CursoRequestDto.class));

            ResponseEntity<?> respuesta = cursoControlador.modificar(
                    "c-1", "Curso mod", "COD", "Desc", 30, 4, 5, "PRESENCIAL",
                    new Date(), new Date(), 3, 2, 0, "ACTIVO", null, null, null);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Curso no encontrado"), respuesta.getBody());
        }
    }

    // =====================================================================
    // eliminar
    // =====================================================================
    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("elimina el curso correctamente")
        void eliminaCorrectamente() {
            ResponseEntity<?> respuesta = cursoControlador.eliminar("c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Curso eliminado correctamente"), respuesta.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void eliminarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Curso no encontrado")).when(cursoServicio).eliminarCurso("c-1");

            ResponseEntity<?> respuesta = cursoControlador.eliminar("c-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Curso no encontrado"), respuesta.getBody());
        }
    }

    // =====================================================================
    // listados personalizados por rol (via SecurityContextHolder)
    // =====================================================================
    @Nested
    @DisplayName("listados por rol autenticado")
    class ListadosPorRol {

        @Test
        @DisplayName("listarCursosDisponiblesProfesor usa el usuario autenticado")
        void listarCursosDisponiblesProfesor() {
            when(authentication.getName()).thenReturn("profe@dominio.com");
            Usuario u = usuario("u-1", Rol.PROFESOR);
            when(usuarioServicio.buscarEmail("profe@dominio.com")).thenReturn(u);
            when(cursoServicio.listarCursosDisponiblesProfesor("u-1")).thenReturn(List.of(curso("c-1")));

            ResponseEntity<List<Curso>> respuesta = cursoControlador.listarCursosDisponiblesProfesor();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listarCursosInscritosProfesor usa el usuario autenticado")
        void listarCursosInscritosProfesor() {
            when(authentication.getName()).thenReturn("profe@dominio.com");
            Usuario u = usuario("u-1", Rol.PROFESOR);
            when(usuarioServicio.buscarEmail("profe@dominio.com")).thenReturn(u);
            when(cursoServicio.listarCursosInscritosProfesor("u-1")).thenReturn(List.of(curso("c-1")));

            ResponseEntity<List<Curso>> respuesta = cursoControlador.listarCursosInscritosProfesor();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listarCursosDisponiblesAlumno usa el usuario autenticado")
        void listarCursosDisponiblesAlumno() {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            Usuario u = usuario("u-2", Rol.ALUMNO);
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(u);
            when(cursoServicio.listarCursosDisponiblesAlumno("u-2")).thenReturn(List.of(curso("c-1")));

            ResponseEntity<List<Curso>> respuesta = cursoControlador.listarCursosDisponiblesAlumno();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listarCursosInscritosAlumno usa el usuario autenticado")
        void listarCursosInscritosAlumno() {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            Usuario u = usuario("u-2", Rol.ALUMNO);
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(u);
            when(cursoServicio.listarCursosInscritosAlumno("u-2")).thenReturn(List.of(curso("c-1")));

            ResponseEntity<List<Curso>> respuesta = cursoControlador.listarCursosInscritosAlumno();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }
    }

    // =====================================================================
    // inscribirCurso
    // =====================================================================
    @Nested
    @DisplayName("inscribirCurso")
    class InscribirCurso {

        @Test
        @DisplayName("inscribe correctamente y devuelve el rol del usuario")
        void inscribeCorrectamente() throws MyException {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            Usuario u = usuario("u-2", Rol.ALUMNO);
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(u);

            ResponseEntity<?> respuesta = cursoControlador.inscribirCurso("c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Curso inscrito correctamente", "rol", "ALUMNO"), respuesta.getBody());
            verify(cursoServicio).inscribirCurso("u-2", "c-1");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void inscribirConExcepcionDevuelveBadRequest() throws MyException {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            Usuario u = usuario("u-2", Rol.ALUMNO);
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(u);
            doThrow(new MyException("Salon lleno")).when(cursoServicio).inscribirCurso("u-2", "c-1");

            ResponseEntity<?> respuesta = cursoControlador.inscribirCurso("c-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Salon lleno"), respuesta.getBody());
        }
    }

    // =====================================================================
    // horarios
    // =====================================================================
    @Nested
    @DisplayName("horarios")
    class Horarios {

        @Test
        @DisplayName("agregarHorario con datos validos devuelve 201")
        void agregarHorarioValido() throws MyException {
            HorarioSesion horario = new HorarioSesion();
            horario.setId("h-1");
            when(cursoServicio.agregarHorario(eq("c-1"), eq("LUNES"), any(LocalTime.class), any(LocalTime.class), eq("A-101"), eq("PRESENCIAL")))
                    .thenReturn(horario);

            ResponseEntity<?> respuesta = cursoControlador.agregarHorario(
                    "c-1", "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0), "A-101", "PRESENCIAL");

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            assertEquals(horario, respuesta.getBody());
        }

        @Test
        @DisplayName("agregarHorario cuando el servicio lanza MyException devuelve badRequest")
        void agregarHorarioConExcepcionDevuelveBadRequest() throws MyException {
            when(cursoServicio.agregarHorario(anyString(), anyString(), any(LocalTime.class), any(LocalTime.class), any(), any()))
                    .thenThrow(new MyException("El aula ya esta ocupada"));

            ResponseEntity<?> respuesta = cursoControlador.agregarHorario(
                    "c-1", "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0), "A-101", "PRESENCIAL");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "El aula ya esta ocupada"), respuesta.getBody());
        }

        @Test
        @DisplayName("listarHorarios devuelve los horarios del curso")
        void listarHorariosDevuelveLista() {
            when(cursoServicio.listarHorariosCurso("c-1")).thenReturn(List.of(new HorarioSesion()));

            ResponseEntity<List<HorarioSesion>> respuesta = cursoControlador.listarHorarios("c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("eliminarHorario elimina correctamente")
        void eliminarHorarioCorrectamente() {
            ResponseEntity<?> respuesta = cursoControlador.eliminarHorario("c-1", "h-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Horario eliminado correctamente"), respuesta.getBody());
        }

        @Test
        @DisplayName("eliminarHorario cuando el servicio lanza MyException devuelve badRequest")
        void eliminarHorarioConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("Horario no encontrado")).when(cursoServicio).eliminarHorario("c-1", "h-1");

            ResponseEntity<?> respuesta = cursoControlador.eliminarHorario("c-1", "h-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Horario no encontrado"), respuesta.getBody());
        }

        @Test
        @DisplayName("listarHorariosProfesor devuelve los horarios del profesor")
        void listarHorariosProfesorDevuelveLista() {
            when(cursoServicio.listarHorariosPorProfesor("p-1")).thenReturn(List.of(new HorarioSesion()));

            ResponseEntity<List<HorarioSesion>> respuesta = cursoControlador.listarHorariosProfesor("p-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }

        @Test
        @DisplayName("listarMisHorarios usa el Authentication recibido como parametro")
        void listarMisHorariosUsaAuthenticationParametro() {
            Authentication authParam = mock(Authentication.class);
            when(authParam.getName()).thenReturn("alumno@dominio.com");
            Usuario u = usuario("u-2", Rol.ALUMNO);
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(u);
            when(cursoServicio.listarHorariosPorAlumno("u-2")).thenReturn(List.of(new HorarioSesion()));

            ResponseEntity<List<HorarioSesion>> respuesta = cursoControlador.listarMisHorarios(authParam);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }
    }

    // =====================================================================
    // prerequisitos
    // =====================================================================
    @Nested
    @DisplayName("prerequisitos")
    class Prerequisitos {

        @Test
        @DisplayName("agregarPrerequisito con datos validos devuelve 201")
        void agregarPrerequisitoValido() throws MyException {
            CursoPrerequisito prerequisito = new CursoPrerequisito();
            prerequisito.setId("pr-1");
            when(cursoServicio.agregarPrerequisito("c-1", "c-2", true, "obs"))
                    .thenReturn(prerequisito);

            ResponseEntity<?> respuesta = cursoControlador.agregarPrerequisito("c-1", "c-2", true, "obs");

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            assertEquals(prerequisito, respuesta.getBody());
        }

        @Test
        @DisplayName("agregarPrerequisito cuando el servicio lanza MyException devuelve badRequest")
        void agregarPrerequisitoConExcepcionDevuelveBadRequest() throws MyException {
            when(cursoServicio.agregarPrerequisito(anyString(), anyString(), any(), any()))
                    .thenThrow(new MyException("El prerrequisito ya fue registrado para este curso"));

            ResponseEntity<?> respuesta = cursoControlador.agregarPrerequisito("c-1", "c-2", true, "obs");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "El prerrequisito ya fue registrado para este curso"), respuesta.getBody());
        }

        @Test
        @DisplayName("listarPrerequisitos devuelve la lista de prerrequisitos del curso")
        void listarPrerequisitosDevuelveLista() {
            when(cursoServicio.listarPrerequisitosCurso("c-1")).thenReturn(List.of(new CursoPrerequisito()));

            ResponseEntity<List<CursoPrerequisito>> respuesta = cursoControlador.listarPrerequisitos("c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }
    }
}
