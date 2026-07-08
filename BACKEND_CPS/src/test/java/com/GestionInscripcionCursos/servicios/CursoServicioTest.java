package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.CursoRequestDto;
import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.CursoPrerequisito;
import com.GestionInscripcionCursos.entidades.HorarioSesion;
import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.EstadoCurso;
import com.GestionInscripcionCursos.enumeraciones.ModalidadCurso;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CarreraRepositorio;
import com.GestionInscripcionCursos.repositorios.CursoPrerequisitoRepositorio;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.HorarioSesionRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link CursoServicio}: alta/edicion de cursos,
 * inscripcion de alumnos y profesores, horarios y prerrequisitos.
 */
@ExtendWith(MockitoExtension.class)
class CursoServicioTest {

    @Mock
    private CursoRepositorio cursoRepositorio;

    @Mock
    private InscripcionRepositorio inscripcionRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private CarreraRepositorio carreraRepositorio;

    @Mock
    private CursoPrerequisitoRepositorio cursoPrerequisitoRepositorio;

    @Mock
    private HorarioSesionRepositorio horarioSesionRepositorio;

    @InjectMocks
    private CursoServicio cursoServicio;

    private static Date hoy() {
        return new Date();
    }

    private static Date enDias(int dias) {
        return new Date(System.currentTimeMillis() + dias * 86_400_000L);
    }

    private CursoRequestDto requestValido() {
        return new CursoRequestDto(
                "Base de Datos II", "BD2", "Curso avanzado de BD", 30, 4, 5,
                "PRESENCIAL", hoy(), enDias(120), 3, 2, 0, "ACTIVO", null, null);
    }

    private Usuario usuario(String id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        u.setNombre("Usuario " + id);
        return u;
    }

    private Curso curso(String id, EstadoCurso estado, Integer capacidad) {
        Curso c = new Curso();
        c.setId(id);
        c.setNombre("Curso " + id);
        c.setCodigoCurso("C-" + id);
        c.setEstado(estado);
        c.setCapacidadMaxima(capacidad);
        return c;
    }

    static Stream<Arguments> solicitudesInvalidasVariasCausas() {
        return Stream.of(
                Arguments.of("capacidad maxima excesiva", new CursoRequestDto(
                        "Curso X", "BD2", "Desc", 50, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null)),
                Arguments.of("fecha de termino anterior a la de inicio", new CursoRequestDto(
                        "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(-10), 3, 0, 0, "ACTIVO", null, null)),
                Arguments.of("sin horas academicas (HT+HP+HL=0)", new CursoRequestDto(
                        "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 0, 0, 0, "ACTIVO", null, null))
        );
    }

    // =====================================================================
    // crearCurso
    // =====================================================================
    @Nested
    @DisplayName("crearCurso")
    class CrearCurso {

        @Test
        @DisplayName("con datos validos guarda el curso normalizando el codigo a mayusculas")
        void creaCursoValido() throws MyException {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);

            cursoServicio.crearCurso(requestValido());

            verify(cursoRepositorio).save(argThat(c -> "BD2".equals(c.getCodigoCurso()) && c.getEstado() == EstadoCurso.ACTIVO));
        }

        @Test
        @DisplayName("con codigo ya existente lanza MyException")
        void codigoDuplicadoLanzaExcepcion() {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(true);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(requestValido()));
            verify(cursoRepositorio, never()).save(any());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.GestionInscripcionCursos.servicios.CursoServicioTest#solicitudesInvalidasVariasCausas")
        @DisplayName("con datos invalidos en distintos campos lanza MyException")
        void datosInvalidosLanzanExcepcion(String descripcion, CursoRequestDto request) {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(request));
        }

        @Test
        @DisplayName("con modalidad invalida lanza MyException")
        void modalidadInvalidaLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "TELEPATICO", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("resuelve el profesor por email cuando la referencia no es un id")
        void resuelveProfesorPorEmail() throws MyException {
            Usuario profesor = usuario("prof-1", Rol.PROFESOR);
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            when(usuarioRepositorio.findById("prof@dominio.com")).thenReturn(Optional.empty());
            when(usuarioRepositorio.buscarPorEmail("prof@dominio.com")).thenReturn(profesor);

            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", "prof@dominio.com", null);

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getProfesorAsignado() == profesor));
        }

        @Test
        @DisplayName("con referencia de profesor que no tiene rol PROFESOR lanza MyException")
        void profesorConRolIncorrectoLanzaExcepcion() {
            Usuario noProfesor = usuario("u-1", Rol.ALUMNO);
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            when(usuarioRepositorio.findById("u-1")).thenReturn(Optional.of(noProfesor));

            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", "u-1", null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        // -----------------------------------------------------------
        // Rama faltante: validaciones de capacidad maxima
        // -----------------------------------------------------------
        @Test
        @DisplayName("con capacidad maxima nula lanza MyException")
        void capacidadMaximaNulaLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", null, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con capacidad maxima igual a 0 lanza MyException")
        void capacidadMaximaCeroLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 0, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con capacidad maxima igual a 40 (limite valido) no lanza excepcion")
        void capacidadMaximaEnLimite40NoLanza() {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 40, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertDoesNotThrow(() -> cursoServicio.crearCurso(req));
            verify(cursoRepositorio).save(argThat(c -> c.getCapacidadMaxima() == 40));
        }

        @Test
        @DisplayName("con capacidad maxima igual a 41 (limite invalido) lanza MyException")
        void capacidadMaximaEnLimite41LanzaExcepcion() {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 41, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        // -----------------------------------------------------------
        // Rama faltante: validaciones de creditos
        // -----------------------------------------------------------
        @Test
        @DisplayName("con creditos nulos lanza MyException")
        void creditosNulosLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, null, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con creditos igual a 0 lanza MyException")
        void creditosCeroLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 0, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con creditos mayor a 8 lanza MyException")
        void creditosMayorA8LanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 9, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con creditos igual a 8 (limite valido) no lanza excepcion")
        void creditosEnLimite8NoLanza() {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 8, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertDoesNotThrow(() -> cursoServicio.crearCurso(req));
            verify(cursoRepositorio).save(argThat(c -> c.getCreditos() == 8));
        }

        // -----------------------------------------------------------
        // Rama faltante: normalizacion y validacion de ciclo
        // -----------------------------------------------------------
        @Test
        @DisplayName("con ciclo mayor a 14 lanza MyException")
        void cicloMayorA14LanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 15, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con ciclo igual a 14 (limite valido) no lanza excepcion")
        void cicloEnLimite14NoLanza() {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 14, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertDoesNotThrow(() -> cursoServicio.crearCurso(req));
            verify(cursoRepositorio).save(argThat(c -> c.getCiclo() == 14));
        }

        @Test
        @DisplayName("con ciclo nulo se normaliza a 1 en vez de lanzar excepcion")
        void cicloNuloSeNormalizaAUno() throws MyException {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, null, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getCiclo() == 1));
        }

        @Test
        @DisplayName("con ciclo negativo se normaliza a 1 en vez de lanzar excepcion")
        void cicloNegativoSeNormalizaAUno() throws MyException {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, -3, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getCiclo() == 1));
        }

        // -----------------------------------------------------------
        // Rama faltante: fechas
        // -----------------------------------------------------------
        @Test
        @DisplayName("con fecha de inicio nula se normaliza a la fecha actual")
        void fechaInicioNulaSeNormalizaAFechaActual() throws MyException {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", null, enDias(120), 3, 0, 0, "ACTIVO", null, null);

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getFechaInicio() != null));
        }

        @Test
        @DisplayName("con fecha de termino nula lanza MyException")
        void fechaTerminoNulaLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), null, 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con fecha de termino igual a la de inicio no lanza excepcion")
        void fechaTerminoIgualAFechaInicioNoLanza() {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            Date mismaFecha = hoy();
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", mismaFecha, mismaFecha, 3, 0, 0, "ACTIVO", null, null);

            assertDoesNotThrow(() -> cursoServicio.crearCurso(req));
        }

        // -----------------------------------------------------------
        // Rama faltante: nombre / descripcion
        // -----------------------------------------------------------
        @Test
        @DisplayName("con nombre nulo lanza MyException")
        void nombreNuloLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    null, "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con descripcion vacia lanza MyException")
        void descripcionVaciaLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        // -----------------------------------------------------------
        // Rama faltante: normalizarHoras (negativas / mayores a 20)
        // -----------------------------------------------------------
        @Test
        @DisplayName("con horas negativas lanza MyException")
        void horasNegativasLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), -1, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("con horas mayores a 20 en un bloque lanza MyException")
        void horasMayorA20LanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 21, 0, 0, "ACTIVO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        // -----------------------------------------------------------
        // Rama faltante: defaults de modalidad y estado
        // -----------------------------------------------------------
        @Test
        @DisplayName("con modalidad nula usa PRESENCIAL por defecto")
        void modalidadNulaUsaPresencialPorDefecto() throws MyException {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, null, hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getModalidad() == ModalidadCurso.PRESENCIAL));
        }

        @Test
        @DisplayName("con estado nulo usa ACTIVO por defecto")
        void estadoNuloUsaActivoPorDefecto() throws MyException {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, null, null, null);

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getEstado() == EstadoCurso.ACTIVO));
        }

        @Test
        @DisplayName("con estado invalido lanza MyException")
        void estadoInvalidoLanzaExcepcion() {
            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "DESCONOCIDO", null, null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        // -----------------------------------------------------------
        // Rama faltante: resolverProfesorSiExiste / resolverCarreraSiExiste
        // -----------------------------------------------------------
        @Test
        @DisplayName("resuelve el profesor directamente por id cuando findById lo encuentra")
        void profesorPorIdDirectoLoAsigna() throws MyException {
            Usuario profesor = usuario("prof-1", Rol.PROFESOR);
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            when(usuarioRepositorio.findById("prof-1")).thenReturn(Optional.of(profesor));

            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", "prof-1", null);

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getProfesorAsignado() == profesor));
            verify(usuarioRepositorio, never()).buscarPorEmail(anyString());
        }

        @Test
        @DisplayName("profesor no encontrado ni por id ni por email lanza MyException")
        void profesorNoEncontradoLanzaExcepcion() {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            when(usuarioRepositorio.findById("fantasma")).thenReturn(Optional.empty());
            when(usuarioRepositorio.buscarPorEmail("fantasma")).thenReturn(null);

            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", "fantasma", null);

            assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
        }

        @Test
        @DisplayName("resuelve la carrera directamente por id cuando findById la encuentra")
        void carreraPorIdDirectoLaAsigna() throws MyException {
            Carrera carrera = new Carrera();
            carrera.setId("car-1");
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            when(carreraRepositorio.findById("car-1")).thenReturn(Optional.of(carrera));

            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, "car-1");

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getCarrera() == carrera));
        }

        @Test
        @DisplayName("resuelve la carrera por nombre cuando no se encuentra por id")
        void carreraPorNombreLaAsigna() throws MyException {
            Carrera carrera = new Carrera();
            carrera.setId("car-2");
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            when(carreraRepositorio.findById("Ingenieria de Sistemas")).thenReturn(Optional.empty());
            when(carreraRepositorio.findByNombreIgnoreCase("Ingenieria de Sistemas")).thenReturn(Optional.of(carrera));

            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, "Ingenieria de Sistemas");

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> c.getCarrera() == carrera));
        }

        @Test
        @DisplayName("carrera no encontrada ni por id ni por nombre lanza MyException")
        void carreraNoEncontradaLanzaExcepcion() {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);
            when(carreraRepositorio.findById("Inexistente")).thenReturn(Optional.empty());
            when(carreraRepositorio.findByNombreIgnoreCase("Inexistente")).thenReturn(Optional.empty());

            CursoRequestDto req = new CursoRequestDto(
                    "Curso X", "BD2", "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, "Inexistente");

            MyException ex = assertThrows(MyException.class, () -> cursoServicio.crearCurso(req));
            assertTrue(ex.getMessage().contains("Carrera no encontrada"));
        }

        // -----------------------------------------------------------
        // Rama faltante: normalizarCodigoCurso genera codigo desde el nombre
        // -----------------------------------------------------------
        @Test
        @DisplayName("con codigo de curso nulo genera el codigo a partir del nombre")
        void codigoCursoNuloGeneraCodigoDesdeNombre() throws MyException {
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BASESDEDAT")).thenReturn(false);

            CursoRequestDto req = new CursoRequestDto(
                    "Bases De Datos", null, "Desc", 30, 4, 5, "PRESENCIAL", hoy(), enDias(120), 3, 0, 0, "ACTIVO", null, null);

            cursoServicio.crearCurso(req);

            verify(cursoRepositorio).save(argThat(c -> "BASESDEDAT".equals(c.getCodigoCurso())));
        }
    }

    // =====================================================================
    // modificarCurso / eliminarCurso
    // =====================================================================
    @Nested
    @DisplayName("modificarCurso y eliminarCurso")
    class ModificarEliminar {

        @Test
        @DisplayName("modificarCurso con curso inexistente lanza MyException")
        void modificarCursoInexistenteLanzaExcepcion() {
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());
            assertThrows(MyException.class, () -> cursoServicio.modificarCurso("c-x", requestValido()));
        }

        @Test
        @DisplayName("modificarCurso con nuevo codigo ya usado por otro curso lanza MyException")
        void modificarConCodigoDuplicadoLanzaExcepcion() {
            Curso existente = curso("c-1", EstadoCurso.ACTIVO, 30);
            existente.setCodigoCurso("ANTIGUO");
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(existente));
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(true);

            assertThrows(MyException.class, () -> cursoServicio.modificarCurso("c-1", requestValido()));
        }

        @Test
        @DisplayName("modificarCurso con datos validos actualiza los campos")
        void modificarCursoValido() throws MyException {
            Curso existente = curso("c-1", EstadoCurso.ACTIVO, 30);
            existente.setCodigoCurso("BD2");
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(existente));

            cursoServicio.modificarCurso("c-1", requestValido());

            assertEquals("Base de Datos II", existente.getNombre());
            verify(cursoRepositorio).save(existente);
        }

        @Test
        @DisplayName("eliminarCurso marca el curso como INACTIVO")
        void eliminarCursoLoMarcaInactivo() throws MyException {
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));

            cursoServicio.eliminarCurso("c-1");

            assertEquals(EstadoCurso.INACTIVO, c.getEstado());
            verify(cursoRepositorio).save(c);
        }

        @Test
        @DisplayName("eliminarCurso con curso inexistente lanza MyException")
        void eliminarCursoInexistenteLanzaExcepcion() {
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> cursoServicio.eliminarCurso("c-x"));
        }

        @Test
        @DisplayName("modificarCurso con profesor referenciado le asigna el profesor")
        void modificarCursoConProfesorAsignadoLoActualiza() throws MyException {
            Curso existente = curso("c-1", EstadoCurso.ACTIVO, 30);
            existente.setCodigoCurso("BD2");
            Usuario profesor = usuario("prof-1", Rol.PROFESOR);
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(existente));
            when(usuarioRepositorio.findById("prof-1")).thenReturn(Optional.of(profesor));

            CursoRequestDto req = new CursoRequestDto(
                    "Base de Datos II", "BD2", "Curso avanzado de BD", 30, 4, 5,
                    "PRESENCIAL", hoy(), enDias(120), 3, 2, 0, "ACTIVO", "prof-1", null);

            cursoServicio.modificarCurso("c-1", req);

            assertEquals(profesor, existente.getProfesorAsignado());
        }

        @Test
        @DisplayName("modificarCurso con nuevo codigo distinto pero disponible actualiza sin lanzar")
        void modificarConCodigoDiferenteYNoExistenteActualiza() {
            Curso existente = curso("c-1", EstadoCurso.ACTIVO, 30);
            existente.setCodigoCurso("ANTIGUO");
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(existente));
            when(cursoRepositorio.existsByCodigoCursoIgnoreCase("BD2")).thenReturn(false);

            assertDoesNotThrow(() -> cursoServicio.modificarCurso("c-1", requestValido()));
            assertEquals("BD2", existente.getCodigoCurso());
        }
    }

    // =====================================================================
    // inscribirCurso
    // =====================================================================
    @Nested
    @DisplayName("inscribirCurso")
    class InscribirCurso {

        @Test
        @DisplayName("curso no activo lanza MyException")
        void cursoNoActivoLanzaExcepcion() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.INACTIVO, 30);
            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));

            assertThrows(MyException.class, () -> cursoServicio.inscribirCurso("a-1", "c-1"));
        }

        @Test
        @DisplayName("alumno nuevo se inscribe directo como APROBADO")
        void alumnoNuevoSeInscribeAprobado() throws MyException {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of());
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(List.of());

            cursoServicio.inscribirCurso("a-1", "c-1");

            verify(inscripcionRepositorio).save(argThat(i -> "APROBADO".equals(i.getEstado())));
        }

        @Test
        @DisplayName("profesor nuevo se inscribe como PENDIENTE sin validar capacidad/prerequisitos")
        void profesorNuevoSeInscribePendiente() throws MyException {
            Usuario profesor = usuario("p-1", Rol.PROFESOR);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            when(usuarioRepositorio.findById("p-1")).thenReturn(Optional.of(profesor));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("p-1", "c-1")).thenReturn(null);

            cursoServicio.inscribirCurso("p-1", "c-1");

            verify(inscripcionRepositorio).save(argThat(i -> "PENDIENTE".equals(i.getEstado())));
            verify(inscripcionRepositorio, never()).contarAlumnosAprobadosPorCurso(anyString());
        }

        @Test
        @DisplayName("alumno con capacidad llena lanza MyException")
        void capacidadLlenaLanzaExcepcion() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 2);
            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(2L);

            assertThrows(MyException.class, () -> cursoServicio.inscribirCurso("a-1", "c-1"));
            verify(inscripcionRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("alumno sin prerrequisito aprobado lanza MyException")
        void prerequisitoNoAprobadoLanzaExcepcion() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            Curso requerido = curso("c-0", EstadoCurso.ACTIVO, 30);
            CursoPrerequisito prereq = new CursoPrerequisito();
            prereq.setPrerrequisito(requerido);
            prereq.setObligatorio(true);

            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of(prereq));
            when(inscripcionRepositorio.existeInscripcionAprobada("a-1", "c-0")).thenReturn(false);

            assertThrows(MyException.class, () -> cursoServicio.inscribirCurso("a-1", "c-1"));
        }

        @Test
        @DisplayName("reinscribirse con el mismo estado ya existente lanza MyException")
        void mismoEstadoYaExistenteLanzaExcepcion() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            Inscripcion existente = new Inscripcion(new Date(), "APROBADO", alumno, c);

            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(existente);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of());
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(List.of());

            assertThrows(MyException.class, () -> cursoServicio.inscribirCurso("a-1", "c-1"));
            verify(inscripcionRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("usuario no encontrado lanza MyException")
        void usuarioNoEncontradoLanzaExcepcion() {
            when(usuarioRepositorio.findById("a-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> cursoServicio.inscribirCurso("a-x", "c-1"));
        }

        @Test
        @DisplayName("curso no encontrado lanza MyException")
        void cursoNoEncontradoLanzaExcepcion() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> cursoServicio.inscribirCurso("a-1", "c-x"));
        }

        @Test
        @DisplayName("alumno con inscripcion previa en estado diferente la actualiza en vez de lanzar")
        void inscripcionExistenteConEstadoDiferenteActualiza() throws MyException {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            Inscripcion existente = new Inscripcion(new Date(), "PENDIENTE", alumno, c);

            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(existente);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of());
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(List.of());

            cursoServicio.inscribirCurso("a-1", "c-1");

            assertEquals("APROBADO", existente.getEstado());
            verify(inscripcionRepositorio).save(existente);
        }

        @Test
        @DisplayName("curso con capacidad maxima nula omite la validacion de cupo")
        void capacidadMaximaNulaOmiteValidacionCapacidad() throws MyException {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, null);
            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of());
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(List.of());

            cursoServicio.inscribirCurso("a-1", "c-1");

            verify(inscripcionRepositorio, never()).contarAlumnosAprobadosPorCurso(anyString());
            verify(inscripcionRepositorio).save(any());
        }

        @Test
        @DisplayName("prerrequisito no obligatorio se omite y permite la inscripcion")
        void prerequisitoNoObligatorioSeOmite() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            Curso requerido = curso("c-0", EstadoCurso.ACTIVO, 30);
            CursoPrerequisito prereq = new CursoPrerequisito();
            prereq.setPrerrequisito(requerido);
            prereq.setObligatorio(false);

            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of(prereq));
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(List.of());

            assertDoesNotThrow(() -> cursoServicio.inscribirCurso("a-1", "c-1"));
            verify(inscripcionRepositorio, never()).existeInscripcionAprobada(anyString(), anyString());
        }

        @Test
        @DisplayName("prerrequisito sin curso requerido asociado se omite")
        void prerequisitoSinCursoRequeridoSeOmite() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            CursoPrerequisito prereq = new CursoPrerequisito();
            prereq.setPrerrequisito(null);
            prereq.setObligatorio(true);

            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of(prereq));
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(List.of());

            assertDoesNotThrow(() -> cursoServicio.inscribirCurso("a-1", "c-1"));
        }

        @Test
        @DisplayName("alumno con prerrequisito aprobado se inscribe sin lanzar excepcion")
        void prerequisitoAprobadoNoLanza() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            Curso requerido = curso("c-0", EstadoCurso.ACTIVO, 30);
            CursoPrerequisito prereq = new CursoPrerequisito();
            prereq.setPrerrequisito(requerido);
            prereq.setObligatorio(true);

            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of(prereq));
            when(inscripcionRepositorio.existeInscripcionAprobada("a-1", "c-0")).thenReturn(true);
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(List.of());

            assertDoesNotThrow(() -> cursoServicio.inscribirCurso("a-1", "c-1"));
            verify(inscripcionRepositorio).save(any());
        }

        @Test
        @DisplayName("alumno con cruce de horarios respecto a un curso ya inscrito lanza MyException")
        void cruceDeHorariosLanzaExcepcion() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            Curso otroCursoInscrito = curso("c-2", EstadoCurso.ACTIVO, 30);

            HorarioSesion nuevoHorario = new HorarioSesion();
            nuevoHorario.setCurso(c);
            nuevoHorario.setDiaSemana("LUNES");
            nuevoHorario.setHoraInicio(LocalTime.of(9, 0));
            nuevoHorario.setHoraFin(LocalTime.of(11, 0));

            HorarioSesion horarioExistente = new HorarioSesion();
            horarioExistente.setCurso(otroCursoInscrito);
            horarioExistente.setDiaSemana("LUNES");
            horarioExistente.setHoraInicio(LocalTime.of(10, 0));
            horarioExistente.setHoraFin(LocalTime.of(12, 0));

            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of());
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(List.of(nuevoHorario));
            when(horarioSesionRepositorio.buscarHorariosPorAlumno("a-1")).thenReturn(List.of(horarioExistente));

            MyException ex = assertThrows(MyException.class, () -> cursoServicio.inscribirCurso("a-1", "c-1"));
            assertTrue(ex.getMessage().contains("Cruce de horarios"));
        }

        @Test
        @DisplayName("horarios del nuevo curso nulos omiten la validacion de cruce")
        void horariosNuevoCursoNuloNoValidaCruce() {
            Usuario alumno = usuario("a-1", Rol.ALUMNO);
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            when(usuarioRepositorio.findById("a-1")).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso("a-1", "c-1")).thenReturn(null);
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("c-1")).thenReturn(0L);
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of());
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("c-1")).thenReturn(null);

            assertDoesNotThrow(() -> cursoServicio.inscribirCurso("a-1", "c-1"));
            verify(horarioSesionRepositorio, never()).buscarHorariosPorAlumno(anyString());
        }
    }

    // =====================================================================
    // agregarHorario
    // =====================================================================
    @Nested
    @DisplayName("agregarHorario")
    class AgregarHorario {

        @Test
        @DisplayName("con hora fin anterior o igual a hora inicio lanza MyException")
        void horaFinInvalidaLanzaExcepcion() {
            assertThrows(MyException.class, () -> cursoServicio.agregarHorario(
                    "c-1", "LUNES", LocalTime.of(10, 0), LocalTime.of(9, 0), "A101", "PRESENCIAL"));
        }

        @Test
        @DisplayName("con cruce de aula en el mismo dia y horario lanza MyException")
        void cruceDeAulaLanzaExcepcion() {
            Curso otroCurso = curso("c-2", EstadoCurso.ACTIVO, 30);
            HorarioSesion existente = new HorarioSesion();
            existente.setCurso(otroCurso);
            existente.setHoraInicio(LocalTime.of(8, 0));
            existente.setHoraFin(LocalTime.of(10, 0));

            when(horarioSesionRepositorio.buscarPorAulaYDia("A101", "LUNES")).thenReturn(List.of(existente));

            MyException ex = assertThrows(MyException.class, () -> cursoServicio.agregarHorario(
                    "c-1", "LUNES", LocalTime.of(9, 0), LocalTime.of(11, 0), "A101", "PRESENCIAL"));
            assertTrue(ex.getMessage().contains("ya está ocupada"));
        }

        @Test
        @DisplayName("sin cruce de aula guarda el horario correctamente")
        void sinCruceGuardaHorario() throws MyException {
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            when(horarioSesionRepositorio.buscarPorAulaYDia("A101", "LUNES")).thenReturn(List.of());
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(horarioSesionRepositorio.save(any(HorarioSesion.class))).thenAnswer(inv -> inv.getArgument(0));

            HorarioSesion guardado = cursoServicio.agregarHorario(
                    "c-1", "LUNES", LocalTime.of(9, 0), LocalTime.of(11, 0), "A101", "PRESENCIAL");

            assertEquals(c, guardado.getCurso());
            assertEquals(ModalidadCurso.PRESENCIAL, guardado.getModalidad());
        }

        @Test
        @DisplayName("con dia de la semana nulo lanza MyException")
        void diaSemanaNuloLanzaExcepcion() {
            assertThrows(MyException.class, () -> cursoServicio.agregarHorario(
                    "c-1", null, LocalTime.of(9, 0), LocalTime.of(11, 0), "A101", "PRESENCIAL"));
        }

        @Test
        @DisplayName("con hora de inicio nula lanza MyException")
        void horaInicioNulaLanzaExcepcion() {
            assertThrows(MyException.class, () -> cursoServicio.agregarHorario(
                    "c-1", "LUNES", null, LocalTime.of(11, 0), "A101", "PRESENCIAL"));
        }

        @Test
        @DisplayName("con aula nula omite la validacion de cruce de aula")
        void aulaNulaOmiteValidacionDeCruce() throws MyException {
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(horarioSesionRepositorio.save(any(HorarioSesion.class))).thenAnswer(inv -> inv.getArgument(0));

            HorarioSesion guardado = cursoServicio.agregarHorario(
                    "c-1", "LUNES", LocalTime.of(9, 0), LocalTime.of(11, 0), null, "PRESENCIAL");

            assertNull(guardado.getAula());
            verify(horarioSesionRepositorio, never()).buscarPorAulaYDia(anyString(), anyString());
        }

        @Test
        @DisplayName("agregarHorario con curso inexistente lanza MyException")
        void cursoNoEncontradoAlAgregarHorarioLanzaExcepcion() {
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> cursoServicio.agregarHorario(
                    "c-x", "LUNES", LocalTime.of(9, 0), LocalTime.of(11, 0), null, "PRESENCIAL"));
        }

        @Test
        @DisplayName("con sesiones existentes en la misma aula pero sin solape de horas guarda el horario")
        void conAulaSinSolapeEntreHorariosExistentesGuardaHorario() throws MyException {
            Curso otroCurso = curso("c-2", EstadoCurso.ACTIVO, 30);
            HorarioSesion existente = new HorarioSesion();
            existente.setCurso(otroCurso);
            existente.setHoraInicio(LocalTime.of(7, 0));
            existente.setHoraFin(LocalTime.of(9, 0));

            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            when(horarioSesionRepositorio.buscarPorAulaYDia("A101", "LUNES")).thenReturn(List.of(existente));
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(horarioSesionRepositorio.save(any(HorarioSesion.class))).thenAnswer(inv -> inv.getArgument(0));

            HorarioSesion guardado = cursoServicio.agregarHorario(
                    "c-1", "LUNES", LocalTime.of(9, 0), LocalTime.of(11, 0), "A101", "PRESENCIAL");

            assertEquals(c, guardado.getCurso());
        }
    }

    // =====================================================================
    // agregarPrerequisito
    // =====================================================================
    @Nested
    @DisplayName("agregarPrerequisito")
    class AgregarPrerequisito {

        @Test
        @DisplayName("un curso no puede ser prerrequisito de si mismo")
        void cursoComoPrerequisitoDeSiMismoLanzaExcepcion() {
            assertThrows(MyException.class, () -> cursoServicio.agregarPrerequisito("c-1", "c-1", true, null));
        }

        @Test
        @DisplayName("prerequisito ya registrado lanza MyException")
        void prerequisitoYaRegistradoLanzaExcepcion() {
            Curso destino = curso("c-1", EstadoCurso.ACTIVO, 30);
            Curso prereq = curso("c-0", EstadoCurso.ACTIVO, 30);
            CursoPrerequisito existente = new CursoPrerequisito();
            existente.setPrerrequisito(prereq);

            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(destino));
            when(cursoRepositorio.findById("c-0")).thenReturn(Optional.of(prereq));
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of(existente));

            assertThrows(MyException.class, () -> cursoServicio.agregarPrerequisito("c-1", "c-0", true, null));
        }

        @Test
        @DisplayName("con datos validos guarda la relacion de prerrequisito")
        void agregaPrerequisitoValido() throws MyException {
            Curso destino = curso("c-1", EstadoCurso.ACTIVO, 30);
            Curso prereq = curso("c-0", EstadoCurso.ACTIVO, 30);

            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(destino));
            when(cursoRepositorio.findById("c-0")).thenReturn(Optional.of(prereq));
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of());
            when(cursoPrerequisitoRepositorio.save(any(CursoPrerequisito.class))).thenAnswer(inv -> inv.getArgument(0));

            CursoPrerequisito guardado = cursoServicio.agregarPrerequisito("c-1", "c-0", null, "obs");

            assertEquals(destino, guardado.getCurso());
            assertEquals(prereq, guardado.getPrerrequisito());
            assertTrue(guardado.getObligatorio());
        }

        @Test
        @DisplayName("curso destino no encontrado lanza MyException")
        void cursoDestinoNoEncontradoLanzaExcepcion() {
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> cursoServicio.agregarPrerequisito("c-x", "c-0", true, null));
        }

        @Test
        @DisplayName("curso prerrequisito no encontrado lanza MyException")
        void cursoPrerequisitoNoEncontradoLanzaExcepcion() {
            Curso destino = curso("c-1", EstadoCurso.ACTIVO, 30);
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(destino));
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> cursoServicio.agregarPrerequisito("c-1", "c-x", true, null));
        }

        @Test
        @DisplayName("con obligatorio explicito en false lo respeta en vez de usar el default")
        void obligatorioFalseSeRespeta() throws MyException {
            Curso destino = curso("c-1", EstadoCurso.ACTIVO, 30);
            Curso prereq = curso("c-0", EstadoCurso.ACTIVO, 30);

            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(destino));
            when(cursoRepositorio.findById("c-0")).thenReturn(Optional.of(prereq));
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of());
            when(cursoPrerequisitoRepositorio.save(any(CursoPrerequisito.class))).thenAnswer(inv -> inv.getArgument(0));

            CursoPrerequisito guardado = cursoServicio.agregarPrerequisito("c-1", "c-0", false, null);

            assertFalse(guardado.getObligatorio());
        }

        @Test
        @DisplayName("una relacion existente con prerrequisito nulo no se cuenta como repetida")
        void prerequisitoConEntradaSinPrerrequisitoAsociadoNoCuentaComoRepetido() {
            Curso destino = curso("c-1", EstadoCurso.ACTIVO, 30);
            Curso prereq = curso("c-0", EstadoCurso.ACTIVO, 30);
            CursoPrerequisito conPrerrequisitoNulo = new CursoPrerequisito();
            conPrerrequisitoNulo.setPrerrequisito(null);

            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(destino));
            when(cursoRepositorio.findById("c-0")).thenReturn(Optional.of(prereq));
            when(cursoPrerequisitoRepositorio.findByCursoId("c-1")).thenReturn(List.of(conPrerrequisitoNulo));
            when(cursoPrerequisitoRepositorio.save(any(CursoPrerequisito.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> cursoServicio.agregarPrerequisito("c-1", "c-0", true, null));
        }
    }

    // =====================================================================
    // eliminarHorario
    // =====================================================================
    @Nested
    @DisplayName("eliminarHorario")
    class EliminarHorario {

        @Test
        @DisplayName("horario no encontrado lanza MyException")
        void horarioNoEncontradoLanzaExcepcion() {
            when(horarioSesionRepositorio.findById("h-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> cursoServicio.eliminarHorario("c-1", "h-x"));
        }

        @Test
        @DisplayName("horario sin curso asociado lanza MyException")
        void horarioConCursoNuloLanzaExcepcion() {
            HorarioSesion horario = new HorarioSesion();
            horario.setCurso(null);
            when(horarioSesionRepositorio.findById("h-1")).thenReturn(Optional.of(horario));

            assertThrows(MyException.class, () -> cursoServicio.eliminarHorario("c-1", "h-1"));
        }

        @Test
        @DisplayName("horario cuyo curso tiene id nulo lanza MyException")
        void horarioConIdCursoNuloLanzaExcepcion() {
            Curso cursoSinId = new Curso();
            HorarioSesion horario = new HorarioSesion();
            horario.setCurso(cursoSinId);
            when(horarioSesionRepositorio.findById("h-1")).thenReturn(Optional.of(horario));

            assertThrows(MyException.class, () -> cursoServicio.eliminarHorario("c-1", "h-1"));
        }

        @Test
        @DisplayName("horario que pertenece a otro curso lanza MyException")
        void horarioConIdCursoDiferenteLanzaExcepcion() {
            Curso otroCurso = curso("c-2", EstadoCurso.ACTIVO, 30);
            HorarioSesion horario = new HorarioSesion();
            horario.setCurso(otroCurso);
            when(horarioSesionRepositorio.findById("h-1")).thenReturn(Optional.of(horario));

            assertThrows(MyException.class, () -> cursoServicio.eliminarHorario("c-1", "h-1"));
            verify(horarioSesionRepositorio, never()).delete(any());
        }

        @Test
        @DisplayName("horario valido perteneciente al curso indicado se elimina")
        void horarioValidoSeElimina() throws MyException {
            Curso c = curso("c-1", EstadoCurso.ACTIVO, 30);
            HorarioSesion horario = new HorarioSesion();
            horario.setCurso(c);
            when(horarioSesionRepositorio.findById("h-1")).thenReturn(Optional.of(horario));

            cursoServicio.eliminarHorario("c-1", "h-1");

            verify(horarioSesionRepositorio).delete(horario);
        }
    }
}
