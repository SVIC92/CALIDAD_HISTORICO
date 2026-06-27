package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.HorarioSesion;
import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.HorarioSesionRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link InscripcionServicio}.
 *
 * <p>El nucleo de estas pruebas es el requerimiento <b>RF05 - Validacion de cruce
 * de horarios</b> aplicado a la inscripcion directa de un alumno
 * ({@link InscripcionServicio#inscribirAlumnoDirecto}). La matriz de casos
 * CP_RF05_01..04 se materializa aqui adaptada al modelo real del proyecto
 * ({@link HorarioSesion} + {@code HorarioUtil.primerCruce}), donde el "cruce"
 * se reporta lanzando {@link MyException}.
 *
 * <p>Regla de solapamiento (HorarioUtil): {@code inicioA < finB && finA > inicioB}.
 * Los limites que apenas se tocan (fin == inicio) NO se consideran cruce.
 */
@ExtendWith(MockitoExtension.class)
class InscripcionServicioTest {

    @Mock
    private InscripcionRepositorio inscripcionRepositorio;

    @Mock
    private CursoRepositorio cursoRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private HorarioSesionRepositorio horarioSesionRepositorio;

    @InjectMocks
    private InscripcionServicio inscripcionServicio;

    // ---------------------------------------------------------------------
    // Helpers de construccion de datos de prueba
    // ---------------------------------------------------------------------

    private Usuario alumno(String id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(Rol.ALUMNO);
        return u;
    }

    private Usuario profesor(String id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(Rol.PROFESOR);
        return u;
    }

    private Curso curso(String id, String nombre, Integer capacidadMaxima) {
        Curso c = new Curso();
        c.setId(id);
        c.setNombre(nombre);
        c.setCapacidadMaxima(capacidadMaxima);
        return c;
    }

    private HorarioSesion horario(Curso curso, String dia, LocalTime inicio, LocalTime fin) {
        HorarioSesion h = new HorarioSesion();
        h.setCurso(curso);
        h.setDiaSemana(dia);
        h.setHoraInicio(inicio);
        h.setHoraFin(fin);
        return h;
    }

    // =====================================================================
    // RF05 - Inscripcion directa de alumno con validacion de cruce horario
    // =====================================================================
    @Nested
    @DisplayName("RF05 - inscribirAlumnoDirecto: validacion de cruce de horarios")
    class CruceHorarioAlumno {

        private static final String ALUMNO_ID = "alu-1";
        private static final String CURSO_NUEVO_ID = "curso-nuevo";

        private Usuario alumno;
        private Curso cursoNuevo;
        private Curso cursoExistente;

        @BeforeEach
        void setUp() {
            alumno = alumno(ALUMNO_ID);
            cursoNuevo = curso(CURSO_NUEVO_ID, "Algoritmos", 40);
            cursoExistente = curso("curso-existente", "Calculo I", 40);
        }

        /**
         * Prepara el camino feliz comun: alumno valido, curso encontrado y con cupo.
         * Los horarios se inyectan en cada caso.
         */
        private void stubBase() {
            when(usuarioRepositorio.findById(ALUMNO_ID)).thenReturn(Optional.of(alumno));
            when(cursoRepositorio.findById(CURSO_NUEVO_ID)).thenReturn(Optional.of(cursoNuevo));
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso(CURSO_NUEVO_ID)).thenReturn(0L);
        }

        @Test
        @DisplayName("CP_RF05_01: sin cruce (horario posterior) -> inscripcion exitosa")
        void cp01_sinCruceHorarioPosterior_permiteInscripcion() throws MyException {
            stubBase();
            // Existente: LUNES 08:00-10:00  |  Nuevo: LUNES 10:00-12:00 (se tocan, no cruzan)
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(CURSO_NUEVO_ID))
                    .thenReturn(List.of(horario(cursoNuevo, "LUNES", LocalTime.of(10, 0), LocalTime.of(12, 0))));
            when(horarioSesionRepositorio.buscarHorariosPorAlumno(ALUMNO_ID))
                    .thenReturn(List.of(horario(cursoExistente, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0))));

            inscripcionServicio.inscribirAlumnoDirecto(ALUMNO_ID, CURSO_NUEVO_ID);

            verify(inscripcionRepositorio).save(argThat(i ->
                    i.getUsuario().getId().equals(ALUMNO_ID)
                    && i.getCurso().getId().equals(CURSO_NUEVO_ID)
                    && "APROBADO".equals(i.getEstado())));
        }

        @Test
        @DisplayName("CP_RF05_02: sin cruce (horario previo) -> inscripcion exitosa")
        void cp02_sinCruceHorarioPrevio_permiteInscripcion() throws MyException {
            stubBase();
            // Existente: LUNES 08:00-10:00  |  Nuevo: LUNES 06:00-08:00 (se tocan, no cruzan)
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(CURSO_NUEVO_ID))
                    .thenReturn(List.of(horario(cursoNuevo, "LUNES", LocalTime.of(6, 0), LocalTime.of(8, 0))));
            when(horarioSesionRepositorio.buscarHorariosPorAlumno(ALUMNO_ID))
                    .thenReturn(List.of(horario(cursoExistente, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0))));

            inscripcionServicio.inscribirAlumnoDirecto(ALUMNO_ID, CURSO_NUEVO_ID);

            verify(inscripcionRepositorio).save(any(Inscripcion.class));
        }

        @Test
        @DisplayName("CP_RF05_03: solapamiento total interno -> lanza MyException (cruce)")
        void cp03_cruceTotalInterno_lanzaExcepcion() {
            stubBase();
            // Existente: LUNES 08:00-10:00  |  Nuevo: LUNES 08:30-09:30 (contenido => cruza)
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(CURSO_NUEVO_ID))
                    .thenReturn(List.of(horario(cursoNuevo, "LUNES", LocalTime.of(8, 30), LocalTime.of(9, 30))));
            when(horarioSesionRepositorio.buscarHorariosPorAlumno(ALUMNO_ID))
                    .thenReturn(List.of(horario(cursoExistente, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0))));

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.inscribirAlumnoDirecto(ALUMNO_ID, CURSO_NUEVO_ID));

            assertTrue(ex.getMessage().contains("Cruce de horarios detectado"));
            assertTrue(ex.getMessage().contains("Calculo I"), "Debe mencionar el curso en conflicto");
            verify(inscripcionRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("CP_RF05_04: valor limite / cruce por 1 min -> lanza MyException (cruce)")
        void cp04_crucePorUnMinuto_lanzaExcepcion() {
            stubBase();
            // Existente: LUNES 08:00-10:00  |  Nuevo: LUNES 09:59-12:00 (1 min de solape => cruza)
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(CURSO_NUEVO_ID))
                    .thenReturn(List.of(horario(cursoNuevo, "LUNES", LocalTime.of(9, 59), LocalTime.of(12, 0))));
            when(horarioSesionRepositorio.buscarHorariosPorAlumno(ALUMNO_ID))
                    .thenReturn(List.of(horario(cursoExistente, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0))));

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.inscribirAlumnoDirecto(ALUMNO_ID, CURSO_NUEVO_ID));

            assertTrue(ex.getMessage().contains("Cruce de horarios detectado"));
            verify(inscripcionRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("Cruce en distinto dia NO cuenta como cruce -> inscripcion exitosa")
        void distintoDia_noHayCruce_permiteInscripcion() throws MyException {
            stubBase();
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(CURSO_NUEVO_ID))
                    .thenReturn(List.of(horario(cursoNuevo, "MARTES", LocalTime.of(8, 30), LocalTime.of(9, 30))));
            when(horarioSesionRepositorio.buscarHorariosPorAlumno(ALUMNO_ID))
                    .thenReturn(List.of(horario(cursoExistente, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0))));

            inscripcionServicio.inscribirAlumnoDirecto(ALUMNO_ID, CURSO_NUEVO_ID);

            verify(inscripcionRepositorio).save(any(Inscripcion.class));
        }

        @Test
        @DisplayName("Curso nuevo sin horarios -> no se consultan horarios del alumno y se inscribe")
        void cursoSinHorarios_permiteInscripcionSinValidarCruce() throws MyException {
            stubBase();
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(CURSO_NUEVO_ID))
                    .thenReturn(new ArrayList<>());

            inscripcionServicio.inscribirAlumnoDirecto(ALUMNO_ID, CURSO_NUEVO_ID);

            verify(horarioSesionRepositorio, never()).buscarHorariosPorAlumno(anyString());
            verify(inscripcionRepositorio).save(any(Inscripcion.class));
        }
    }

    // =====================================================================
    // inscribirAlumnoDirecto - validaciones de aforo y rol
    // =====================================================================
    @Nested
    @DisplayName("inscribirAlumnoDirecto: aforo y rol")
    class ValidacionesAlumno {

        @Test
        @DisplayName("Aforo lleno -> lanza MyException y no guarda")
        void aforoLleno_lanzaExcepcion() {
            Usuario al = alumno("alu-1");
            Curso cursoLleno = curso("curso-lleno", "Base de Datos", 2);

            when(usuarioRepositorio.findById("alu-1")).thenReturn(Optional.of(al));
            when(cursoRepositorio.findById("curso-lleno")).thenReturn(Optional.of(cursoLleno));
            when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("curso-lleno")).thenReturn(2L);

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.inscribirAlumnoDirecto("alu-1", "curso-lleno"));

            assertTrue(ex.getMessage().contains("aforo del curso ya se encuentra lleno"));
            verify(inscripcionRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("Usuario no es alumno -> lanza MyException")
        void usuarioNoEsAlumno_lanzaExcepcion() {
            Usuario noAlumno = profesor("prof-1");
            when(usuarioRepositorio.findById("prof-1")).thenReturn(Optional.of(noAlumno));

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.inscribirAlumnoDirecto("prof-1", "curso-1"));

            assertEquals("El usuario no es un alumno", ex.getMessage());
            verify(inscripcionRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("Usuario inexistente -> lanza MyException")
        void usuarioInexistente_lanzaExcepcion() {
            when(usuarioRepositorio.findById("fantasma")).thenReturn(Optional.empty());

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.inscribirAlumnoDirecto("fantasma", "curso-1"));

            assertEquals("Usuario no encontrado", ex.getMessage());
        }

        @Test
        @DisplayName("Curso inexistente -> lanza MyException")
        void cursoInexistente_lanzaExcepcion() {
            Usuario al = alumno("alu-1");
            when(usuarioRepositorio.findById("alu-1")).thenReturn(Optional.of(al));
            when(cursoRepositorio.findById("curso-x")).thenReturn(Optional.empty());

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.inscribirAlumnoDirecto("alu-1", "curso-x"));

            assertEquals("Curso no encontrado", ex.getMessage());
        }
    }

    // =====================================================================
    // aprobarInscripcionProfesor / rechazarInscripcionProfesor
    // =====================================================================
    @Nested
    @DisplayName("aprobar/rechazar inscripcion de profesor")
    class InscripcionProfesor {

        private static final String INSC_ID = "insc-1";

        private Usuario prof;
        private Curso cursoProf;
        private Inscripcion inscripcion;

        @BeforeEach
        void setUp() {
            prof = profesor("prof-1");
            cursoProf = curso("curso-1", "Estructuras de Datos", 40);
            inscripcion = new Inscripcion(new Date(), "PENDIENTE", prof, cursoProf);
            inscripcion.setId(INSC_ID);
        }

        @Test
        @DisplayName("Aprobacion sin cruce -> estado APROBADO y profesor asignado")
        void aprobar_exito() throws MyException {
            when(inscripcionRepositorio.findById(INSC_ID)).thenReturn(Optional.of(inscripcion));
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-1"))
                    .thenReturn(new ArrayList<>());

            inscripcionServicio.aprobarInscripcionProfesor(INSC_ID);

            assertEquals("APROBADO", inscripcion.getEstado());
            assertEquals(prof, cursoProf.getProfesorAsignado());
            verify(cursoRepositorio).save(cursoProf);
            verify(inscripcionRepositorio).save(inscripcion);
        }

        @Test
        @DisplayName("Inscripcion no es de profesor -> lanza MyException")
        void rolIncorrecto_lanzaExcepcion() {
            prof.setRol(Rol.ALUMNO);
            when(inscripcionRepositorio.findById(INSC_ID)).thenReturn(Optional.of(inscripcion));

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.aprobarInscripcionProfesor(INSC_ID));

            assertEquals("La inscripcion no corresponde a un profesor", ex.getMessage());
            verify(cursoRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("Curso ya tiene otro profesor -> lanza MyException")
        void cursoConOtroProfesor_lanzaExcepcion() {
            cursoProf.setProfesorAsignado(profesor("prof-999"));
            when(inscripcionRepositorio.findById(INSC_ID)).thenReturn(Optional.of(inscripcion));

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.aprobarInscripcionProfesor(INSC_ID));

            assertEquals("El curso ya tiene otro profesor asignado", ex.getMessage());
            verify(inscripcionRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("Cruce de horarios del profesor -> lanza MyException")
        void cruceHorarioProfesor_lanzaExcepcion() {
            Curso otroCurso = curso("curso-2", "Fisica I", 40);
            when(inscripcionRepositorio.findById(INSC_ID)).thenReturn(Optional.of(inscripcion));
            when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-1"))
                    .thenReturn(List.of(horario(cursoProf, "LUNES", LocalTime.of(9, 0), LocalTime.of(11, 0))));
            when(horarioSesionRepositorio.buscarHorariosPorProfesor("prof-1"))
                    .thenReturn(List.of(horario(otroCurso, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0))));

            MyException ex = assertThrows(MyException.class,
                    () -> inscripcionServicio.aprobarInscripcionProfesor(INSC_ID));

            assertTrue(ex.getMessage().contains("Cruce de horarios detectado"));
            verify(inscripcionRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("Rechazo de inscripcion de profesor -> estado RECHAZADO")
        void rechazar_exito() throws MyException {
            when(inscripcionRepositorio.findById(INSC_ID)).thenReturn(Optional.of(inscripcion));

            inscripcionServicio.rechazarInscripcionProfesor(INSC_ID);

            assertEquals("RECHAZADO", inscripcion.getEstado());
            verify(inscripcionRepositorio).save(inscripcion);
            verify(cursoRepositorio, never()).save(any());
        }
    }
}
