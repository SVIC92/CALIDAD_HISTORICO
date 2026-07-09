package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.RendimientoAlumnoDto;
import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.CriterioRubrica;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.NivelRubrica;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Rubrica;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.EstadoEntrega;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.ActividadRepositorio;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.ReporteRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ReporteServicio}: entrega de tareas, estado
 * de entrega (RF-11), calificacion por rubrica (RF-09) y rendimiento
 * academico agregado por curso (RF-12).
 */
@ExtendWith(MockitoExtension.class)
class ReporteServicioTest {

    @Mock
    private ActividadRepositorio actividadRepositorio;

    @Mock
    private ReporteRepositorio reporteRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private CursoRepositorio cursoRepositorio;

    @Mock
    private InscripcionRepositorio inscripcionRepositorio;

    @InjectMocks
    private ReporteServicio reporteServicio;

    private Usuario alumno(String id, String nombre, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        return u;
    }

    private Curso curso(String id) {
        Curso c = new Curso();
        c.setId(id);
        return c;
    }

    private Actividad actividad(String id, Curso curso, Date fechaVencimiento) {
        Actividad a = new Actividad();
        a.setId(id);
        a.setCurso(curso);
        a.setFechaVencimiento(fechaVencimiento);
        a.setIntentosPermitidos(3);
        return a;
    }

    private Reporte reporte(String id, Actividad actividad, EstadoEntrega estado, String nota) {
        Reporte r = new Reporte();
        r.setId(id);
        r.setActividad(actividad);
        r.setEstado(estado);
        r.setNota(nota);
        return r;
    }

    private Date fechaFutura() {
        return new Date(System.currentTimeMillis() + 3_600_000);
    }

    private Date fechaPasada() {
        return new Date(System.currentTimeMillis() - 3_600_000);
    }

    // =====================================================================
    // crearReporte
    // =====================================================================
    @Nested
    @DisplayName("crearReporte")
    class CrearReporte {

        @Test
        @DisplayName("antes del vencimiento queda ENVIADO")
        void antesDelVencimientoQuedaEnviado() throws MyException {
            Curso c = curso("c-1");
            Actividad a = actividad("a-1", c, fechaFutura());
            Usuario u = alumno("u-1", "Ana", "ana@dominio.com");

            when(actividadRepositorio.findById("a-1")).thenReturn(Optional.of(a));
            when(usuarioRepositorio.findById("u-1")).thenReturn(Optional.of(u));
            when(reporteRepositorio.contarReportesPorUsuarioYActividad("u-1", "a-1")).thenReturn(0L);

            reporteServicio.crearReporte("mi respuesta", "a-1", "u-1", null);

            verify(reporteRepositorio).save(argThat(r -> r.getEstado() == EstadoEntrega.ENVIADO));
        }

        @Test
        @DisplayName("despues del vencimiento queda ATRASADO con nota 00")
        void despuesDelVencimientoQuedaAtrasado() throws MyException {
            Curso c = curso("c-1");
            Actividad a = actividad("a-1", c, fechaPasada());
            Usuario u = alumno("u-1", "Ana", "ana@dominio.com");

            when(actividadRepositorio.findById("a-1")).thenReturn(Optional.of(a));
            when(usuarioRepositorio.findById("u-1")).thenReturn(Optional.of(u));
            when(reporteRepositorio.contarReportesPorUsuarioYActividad("u-1", "a-1")).thenReturn(0L);

            reporteServicio.crearReporte("mi respuesta", "a-1", "u-1", null);

            verify(reporteRepositorio).save(argThat(r -> r.getEstado() == EstadoEntrega.ATRASADO && "00".equals(r.getNota())));
        }

        @Test
        @DisplayName("al alcanzar el limite de intentos lanza MyException")
        void limiteDeIntentosLanzaExcepcion() {
            Actividad a = actividad("a-1", curso("c-1"), fechaFutura());
            when(actividadRepositorio.findById("a-1")).thenReturn(Optional.of(a));
            when(reporteRepositorio.contarReportesPorUsuarioYActividad("u-1", "a-1")).thenReturn(3L);

            assertThrows(MyException.class, () -> reporteServicio.crearReporte("resp", "a-1", "u-1", null));
            verify(reporteRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con respuesta vacia lanza MyException")
        void respuestaVaciaLanzaExcepcion() {
            Actividad a = actividad("a-1", curso("c-1"), fechaFutura());
            when(actividadRepositorio.findById("a-1")).thenReturn(Optional.of(a));
            when(reporteRepositorio.contarReportesPorUsuarioYActividad("u-1", "a-1")).thenReturn(0L);

            assertThrows(MyException.class, () -> reporteServicio.crearReporte("", "a-1", "u-1", null));
        }
    }

    // =====================================================================
    // calificarReporte (manual, sin rubrica)
    // =====================================================================
    @Nested
    @DisplayName("calificarReporte")
    class CalificarReporte {

        @Test
        @DisplayName("con datos validos actualiza nota, comentario y estado CALIFICADO")
        void calificaCorrectamente() throws MyException {
            Reporte r = reporte("r-1", actividad("a-1", curso("c-1"), fechaFutura()), EstadoEntrega.ENVIADO, "Por Calificar");
            when(reporteRepositorio.findById("r-1")).thenReturn(Optional.of(r));

            reporteServicio.calificarReporte("r-1", "18", "Buen trabajo");

            assertEquals("18", r.getNota());
            assertEquals("Buen trabajo", r.getComentario());
            assertEquals(EstadoEntrega.CALIFICADO, r.getEstado());
            verify(reporteRepositorio).save(r);
        }

        @Test
        @DisplayName("con nota vacia lanza MyException")
        void notaVaciaLanzaExcepcion() {
            assertThrows(MyException.class, () -> reporteServicio.calificarReporte("r-1", "", "comentario"));
            verify(reporteRepositorio, never()).save(any());
        }
    }

    // =====================================================================
    // calificarConRubrica (RF-09)
    // =====================================================================
    @Nested
    @DisplayName("calificarConRubrica")
    class CalificarConRubrica {

        private CriterioRubrica criterio(String id, String nombre, int puntajeMaximo) {
            CriterioRubrica c = new CriterioRubrica();
            c.setId(id);
            c.setNombre(nombre);
            c.setNiveles(List.of(new NivelRubrica("Excelente", puntajeMaximo, "desc")));
            return c;
        }

        @Test
        @DisplayName("reporte inexistente lanza MyException")
        void reporteInexistenteLanzaExcepcion() {
            when(reporteRepositorio.findById("r-x")).thenReturn(Optional.empty());
            assertThrows(MyException.class, () -> reporteServicio.calificarConRubrica("r-x", Map.of(), ""));
        }

        @Test
        @DisplayName("actividad sin rubrica asignada lanza MyException")
        void sinRubricaAsignadaLanzaExcepcion() {
            Actividad a = actividad("a-1", curso("c-1"), fechaFutura());
            Reporte r = reporte("r-1", a, EstadoEntrega.ENVIADO, "Por Calificar");
            when(reporteRepositorio.findById("r-1")).thenReturn(Optional.of(r));

            MyException ex = assertThrows(MyException.class, () -> reporteServicio.calificarConRubrica("r-1", Map.of(), ""));
            assertTrue(ex.getMessage().contains("rúbrica"));
        }

        @Test
        @DisplayName("falta el puntaje de un criterio lanza MyException")
        void faltaPuntajeDeCriterioLanzaExcepcion() {
            Rubrica rubrica = new Rubrica();
            CriterioRubrica c1 = criterio("crit-1", "Claridad", 10);
            rubrica.setCriterios(List.of(c1));

            Actividad a = actividad("a-1", curso("c-1"), fechaFutura());
            a.setRubrica(rubrica);
            Reporte r = reporte("r-1", a, EstadoEntrega.ENVIADO, "Por Calificar");
            when(reporteRepositorio.findById("r-1")).thenReturn(Optional.of(r));

            assertThrows(MyException.class, () -> reporteServicio.calificarConRubrica("r-1", Map.of(), ""));
        }

        @Test
        @DisplayName("puntaje fuera de rango lanza MyException")
        void puntajeFueraDeRangoLanzaExcepcion() {
            Rubrica rubrica = new Rubrica();
            CriterioRubrica c1 = criterio("crit-1", "Claridad", 10);
            rubrica.setCriterios(List.of(c1));

            Actividad a = actividad("a-1", curso("c-1"), fechaFutura());
            a.setRubrica(rubrica);
            Reporte r = reporte("r-1", a, EstadoEntrega.ENVIADO, "Por Calificar");
            when(reporteRepositorio.findById("r-1")).thenReturn(Optional.of(r));

            assertThrows(MyException.class, () -> reporteServicio.calificarConRubrica("r-1", Map.of("crit-1", 15), ""));
        }

        @Test
        @DisplayName("con puntajes validos calcula la nota como la suma y marca CALIFICADO")
        void calificaConRubricaCorrectamente() throws MyException {
            Rubrica rubrica = new Rubrica();
            CriterioRubrica c1 = criterio("crit-1", "Claridad", 10);
            CriterioRubrica c2 = criterio("crit-2", "Fondo", 10);
            rubrica.setCriterios(List.of(c1, c2));

            Actividad a = actividad("a-1", curso("c-1"), fechaFutura());
            a.setRubrica(rubrica);
            Reporte r = reporte("r-1", a, EstadoEntrega.ENVIADO, "Por Calificar");
            when(reporteRepositorio.findById("r-1")).thenReturn(Optional.of(r));
            when(reporteRepositorio.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

            Reporte calificado = reporteServicio.calificarConRubrica("r-1", Map.of("crit-1", 8, "crit-2", 6), "Buen trabajo");

            assertEquals("14", calificado.getNota());
            assertEquals(EstadoEntrega.CALIFICADO, calificado.getEstado());
            assertEquals(2, calificado.getPuntajesCriterio().size());
        }
    }

    // =====================================================================
    // calcularRendimientoCurso (RF-12)
    // =====================================================================
    @Nested
    @DisplayName("calcularRendimientoCurso")
    class CalcularRendimientoCurso {

        @Test
        @DisplayName("curso inexistente lanza MyException")
        void cursoInexistenteLanzaExcepcion() {
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());
            assertThrows(MyException.class, () -> reporteServicio.calcularRendimientoCurso("c-x"));
        }

        @Test
        @DisplayName("agrega pendientes, entregadas, atrasadas, calificadas y promedio por alumno")
        void agregaRendimientoPorAlumno() throws MyException {
            Curso c = curso("c-1");
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));

            Actividad act1 = actividad("a-1", c, fechaFutura());
            Actividad act2 = actividad("a-2", c, fechaFutura());
            Actividad act3 = actividad("a-3", c, fechaFutura());
            when(actividadRepositorio.buscarActividadesPorIdCurso("c-1")).thenReturn(List.of(act1, act2, act3));

            Usuario ana = alumno("u-1", "Ana", "ana@dominio.com");
            when(inscripcionRepositorio.buscarAlumnosAprobadosPorCurso("c-1")).thenReturn(List.of(ana));

            // act1: calificada con nota numerica 18
            Reporte reporteAct1 = reporte("r-1", act1, EstadoEntrega.CALIFICADO, "18");
            reporteAct1.setUsuario(ana);
            // act2: atrasada
            Reporte reporteAct2 = reporte("r-2", act2, EstadoEntrega.ATRASADO, "00");
            reporteAct2.setUsuario(ana);
            // act3: sin entrega (pendiente) -> no se incluye ningun reporte para esa actividad
            when(reporteRepositorio.buscarReportesPorIdsActividad(List.of("a-1", "a-2", "a-3")))
                    .thenReturn(List.of(reporteAct1, reporteAct2));

            List<RendimientoAlumnoDto> resultado = reporteServicio.calcularRendimientoCurso("c-1");

            assertEquals(1, resultado.size());
            RendimientoAlumnoDto dto = resultado.get(0);
            assertEquals(3, dto.actividadesTotales());
            assertEquals(2, dto.entregadas());
            assertEquals(1, dto.pendientes());
            assertEquals(1, dto.atrasadas());
            assertEquals(1, dto.calificadas());
            assertEquals(18.0, dto.promedioNotas());
        }

        @Test
        @DisplayName("nota no numerica no participa del promedio")
        void notaNoNumericaNoParticipaDelPromedio() throws MyException {
            Curso c = curso("c-1");
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));

            Actividad act1 = actividad("a-1", c, fechaFutura());
            when(actividadRepositorio.buscarActividadesPorIdCurso("c-1")).thenReturn(List.of(act1));

            Usuario ana = alumno("u-1", "Ana", "ana@dominio.com");
            when(inscripcionRepositorio.buscarAlumnosAprobadosPorCurso("c-1")).thenReturn(List.of(ana));

            Reporte reporteAct1 = reporte("r-1", act1, EstadoEntrega.CALIFICADO, "Por Calificar");
            reporteAct1.setUsuario(ana);
            when(reporteRepositorio.buscarReportesPorIdsActividad(List.of("a-1")))
                    .thenReturn(List.of(reporteAct1));

            RendimientoAlumnoDto dto = reporteServicio.calcularRendimientoCurso("c-1").get(0);

            assertEquals(1, dto.calificadas());
            assertNull(dto.promedioNotas());
        }
    }
}
