package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.EstadoEntrega;
import com.GestionInscripcionCursos.repositorios.ActividadRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.ReporteRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link EvaluacionAutomaticaTask}. Se invoca
 * directamente el metodo anotado con {@code @Scheduled} como un metodo
 * normal, sin simular el scheduler de Spring.
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionAutomaticaTaskTest {

    @Mock
    private ActividadRepositorio actividadRepositorio;

    @Mock
    private InscripcionRepositorio inscripcionRepositorio;

    @Mock
    private ReporteRepositorio reporteRepositorio;

    @InjectMocks
    private EvaluacionAutomaticaTask evaluacionAutomaticaTask;

    private Curso curso(String id) {
        Curso c = new Curso();
        c.setId(id);
        return c;
    }

    private Actividad actividad(String id, Curso curso) {
        Actividad a = new Actividad();
        a.setId(id);
        a.setCurso(curso);
        a.setFechaVencimiento(new Date(System.currentTimeMillis() - 86_400_000L));
        return a;
    }

    private Usuario alumno(String id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Nested
    @DisplayName("calificarActividadesVencidas")
    class CalificarActividadesVencidas {

        @Test
        @DisplayName("sin actividades vencidas no genera reportes ni consulta alumnos")
        void sinActividadesVencidasNoHaceNada() {
            when(actividadRepositorio.buscarActividadesVencidas(any(Date.class))).thenReturn(List.of());

            evaluacionAutomaticaTask.calificarActividadesVencidas();

            verify(inscripcionRepositorio, never()).buscarAlumnosAprobadosPorCurso(any());
            verify(reporteRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("alumno sin reportes enviados recibe calificacion automatica de 00")
        void alumnoSinReportesRecibeCalificacionAutomatica() {
            Curso c = curso("curso-1");
            Actividad act = actividad("act-1", c);
            Usuario al = alumno("alu-1");

            when(actividadRepositorio.buscarActividadesVencidas(any(Date.class))).thenReturn(List.of(act));
            when(inscripcionRepositorio.buscarAlumnosAprobadosPorCurso("curso-1")).thenReturn(List.of(al));
            when(reporteRepositorio.contarReportesPorUsuarioYActividad("alu-1", "act-1")).thenReturn(0L);

            evaluacionAutomaticaTask.calificarActividadesVencidas();

            verify(reporteRepositorio).save(argThat(r ->
                    "00".equals(r.getNota())
                    && r.getEstado() == EstadoEntrega.CALIFICADO
                    && r.getUsuario() == al
                    && r.getActividad() == act));
        }

        @Test
        @DisplayName("alumno con reportes ya enviados no recibe calificacion automatica")
        void alumnoConReportesNoRecibeCalificacionAutomatica() {
            Curso c = curso("curso-1");
            Actividad act = actividad("act-1", c);
            Usuario al = alumno("alu-1");

            when(actividadRepositorio.buscarActividadesVencidas(any(Date.class))).thenReturn(List.of(act));
            when(inscripcionRepositorio.buscarAlumnosAprobadosPorCurso("curso-1")).thenReturn(List.of(al));
            when(reporteRepositorio.contarReportesPorUsuarioYActividad("alu-1", "act-1")).thenReturn(1L);

            evaluacionAutomaticaTask.calificarActividadesVencidas();

            verify(reporteRepositorio, never()).save(any(Reporte.class));
        }

        @Test
        @DisplayName("procesa multiples actividades y alumnos, calificando solo a quienes no entregaron")
        void procesaMultiplesActividadesYAlumnos() {
            Curso c1 = curso("curso-1");
            Curso c2 = curso("curso-2");
            Actividad act1 = actividad("act-1", c1);
            Actividad act2 = actividad("act-2", c2);
            Usuario alSinEntrega = alumno("alu-1");
            Usuario alConEntrega = alumno("alu-2");

            when(actividadRepositorio.buscarActividadesVencidas(any(Date.class))).thenReturn(List.of(act1, act2));
            when(inscripcionRepositorio.buscarAlumnosAprobadosPorCurso("curso-1")).thenReturn(List.of(alSinEntrega, alConEntrega));
            when(inscripcionRepositorio.buscarAlumnosAprobadosPorCurso("curso-2")).thenReturn(List.of());
            when(reporteRepositorio.contarReportesPorUsuarioYActividad("alu-1", "act-1")).thenReturn(0L);
            when(reporteRepositorio.contarReportesPorUsuarioYActividad("alu-2", "act-1")).thenReturn(3L);

            evaluacionAutomaticaTask.calificarActividadesVencidas();

            verify(reporteRepositorio, times(1)).save(any(Reporte.class));
            verify(reporteRepositorio).save(argThat(r -> r.getUsuario() == alSinEntrega));
        }
    }
}
