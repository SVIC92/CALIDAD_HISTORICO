package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.Rubrica;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.ActividadRepositorio;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.RubricaRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ActividadServicio}, incluida la asignacion de
 * una rubrica guardada a una actividad (RF-09/RF-10).
 */
@ExtendWith(MockitoExtension.class)
class ActividadServicioTest {

    @Mock
    private ActividadRepositorio actividadRepositorio;

    @Mock
    private CursoRepositorio cursoRepositorio;

    @Mock
    private RubricaRepositorio rubricaRepositorio;

    @InjectMocks
    private ActividadServicio actividadServicio;

    private Curso curso(String id) {
        Curso c = new Curso();
        c.setId(id);
        return c;
    }

    private Actividad actividad(String id, Curso curso) {
        Actividad a = new Actividad();
        a.setId(id);
        a.setCurso(curso);
        return a;
    }

    private Date fechaFutura() {
        return new Date(System.currentTimeMillis() + 3_600_000);
    }

    @Nested
    @DisplayName("crearActividad")
    class CrearActividad {

        @Test
        @DisplayName("con datos validos guarda la actividad")
        void creaActividadValida() throws MyException {
            Curso c = curso("c-1");
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));

            actividadServicio.crearActividad("Tarea 1", "Descripcion", fechaFutura(), 2, "c-1");

            verify(actividadRepositorio).save(argThat(a -> "Tarea 1".equals(a.getNombre()) && a.getCurso() == c));
        }

        @Test
        @DisplayName("con intentos permitidos fuera de rango lanza MyException")
        void intentosFueraDeRangoLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    actividadServicio.crearActividad("Tarea 1", "Descripcion", fechaFutura(), 5, "c-1"));
            verify(actividadRepositorio, never()).save(any());
        }
    }

    @Nested
    @DisplayName("asignarRubrica")
    class AsignarRubrica {

        @Test
        @DisplayName("actividad inexistente lanza MyException")
        void actividadInexistenteLanzaExcepcion() {
            when(actividadRepositorio.findById("a-x")).thenReturn(Optional.empty());
            assertThrows(MyException.class, () -> actividadServicio.asignarRubrica("a-x", "r-1"));
        }

        @Test
        @DisplayName("rubrica inexistente lanza MyException")
        void rubricaInexistenteLanzaExcepcion() {
            Actividad a = actividad("a-1", curso("c-1"));
            when(actividadRepositorio.findById("a-1")).thenReturn(Optional.of(a));
            when(rubricaRepositorio.findById("r-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> actividadServicio.asignarRubrica("a-1", "r-x"));
        }

        @Test
        @DisplayName("rubrica de otro curso lanza MyException")
        void rubricaDeOtroCursoLanzaExcepcion() {
            Actividad a = actividad("a-1", curso("c-1"));
            Rubrica r = new Rubrica();
            r.setCurso(curso("c-2"));
            when(actividadRepositorio.findById("a-1")).thenReturn(Optional.of(a));
            when(rubricaRepositorio.findById("r-1")).thenReturn(Optional.of(r));

            MyException ex = assertThrows(MyException.class, () -> actividadServicio.asignarRubrica("a-1", "r-1"));
            assertTrue(ex.getMessage().contains("otro curso"));
            verify(actividadRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con rubrica del mismo curso la asigna correctamente")
        void asignaRubricaDelMismoCurso() throws MyException {
            Curso c = curso("c-1");
            Actividad a = actividad("a-1", c);
            Rubrica r = new Rubrica();
            r.setCurso(c);
            when(actividadRepositorio.findById("a-1")).thenReturn(Optional.of(a));
            when(rubricaRepositorio.findById("r-1")).thenReturn(Optional.of(r));

            actividadServicio.asignarRubrica("a-1", "r-1");

            assertSame(r, a.getRubrica());
            verify(actividadRepositorio).save(a);
        }
    }

    @Nested
    @DisplayName("listarActividadesPorIdCurso y buscarPorId")
    class Consultas {

        @Test
        @DisplayName("listarActividadesPorIdCurso delega en el repositorio")
        void listarDelega() {
            Actividad a = actividad("a-1", curso("c-1"));
            when(actividadRepositorio.buscarActividadesPorIdCurso("c-1")).thenReturn(List.of(a));

            assertEquals(List.of(a), actividadServicio.listarActividadesPorIdCurso("c-1"));
        }

        @Test
        @DisplayName("buscarPorId delega en el repositorio")
        void buscarPorIdDelega() {
            Actividad a = actividad("a-1", curso("c-1"));
            when(actividadRepositorio.buscarPorId("a-1")).thenReturn(a);

            assertSame(a, actividadServicio.buscarPorId("a-1"));
        }
    }
}
