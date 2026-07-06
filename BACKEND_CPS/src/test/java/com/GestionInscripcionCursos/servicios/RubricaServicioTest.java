package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.CriterioRubricaDto;
import com.GestionInscripcionCursos.dto.NivelRubricaDto;
import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.Rubrica;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.RubricaRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link RubricaServicio}: persistencia de rubricas
 * generadas por IA vinculadas a un curso (RF-10).
 */
@ExtendWith(MockitoExtension.class)
class RubricaServicioTest {

    @Mock
    private RubricaRepositorio rubricaRepositorio;

    @Mock
    private CursoRepositorio cursoRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    private RubricaServicio rubricaServicio;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        rubricaServicio = new RubricaServicio(rubricaRepositorio, cursoRepositorio, usuarioRepositorio);
    }

    private RubricaGeneradaDto dtoConCriterios() {
        NivelRubricaDto nivel = new NivelRubricaDto("Excelente", 10, "Cumple todo");
        CriterioRubricaDto criterio = new CriterioRubricaDto("Claridad", "Que tan claro es", 50, List.of(nivel));
        return new RubricaGeneradaDto(
                "Rubrica de ensayo", "Descripcion", "Cambio climatico", "Universitario",
                "Ciencias", "Ensayo", 10, List.of(criterio), true, "command-r", Instant.now());
    }

    @Nested
    @DisplayName("guardarDesdeGeneracion")
    class GuardarDesdeGeneracion {

        @Test
        @DisplayName("sin criterios lanza MyException")
        void sinCriteriosLanzaExcepcion() {
            RubricaGeneradaDto vacia = new RubricaGeneradaDto(
                    "T", "D", "T", "N", "A", "T", 10, List.of(), true, "m", Instant.now());

            assertThrows(MyException.class, () -> rubricaServicio.guardarDesdeGeneracion(vacia, "c-1", "prof@dominio.com"));
        }

        @Test
        @DisplayName("con cursoId vacio lanza MyException")
        void cursoIdVacioLanzaExcepcion() {
            assertThrows(MyException.class, () -> rubricaServicio.guardarDesdeGeneracion(dtoConCriterios(), " ", "prof@dominio.com"));
        }

        @Test
        @DisplayName("curso inexistente lanza MyException")
        void cursoInexistenteLanzaExcepcion() {
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());
            assertThrows(MyException.class, () -> rubricaServicio.guardarDesdeGeneracion(dtoConCriterios(), "c-x", "prof@dominio.com"));
        }

        @Test
        @DisplayName("usuario creador inexistente lanza MyException")
        void usuarioInexistenteLanzaExcepcion() {
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(new Curso()));
            when(usuarioRepositorio.buscarPorEmail("nadie@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () -> rubricaServicio.guardarDesdeGeneracion(dtoConCriterios(), "c-1", "nadie@dominio.com"));
        }

        @Test
        @DisplayName("con datos validos persiste la rubrica con sus criterios y niveles")
        void guardaCorrectamente() throws MyException {
            Curso curso = new Curso();
            curso.setId("c-1");
            Usuario profesor = new Usuario();
            profesor.setId("u-1");
            profesor.setEmail("prof@dominio.com");

            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(curso));
            when(usuarioRepositorio.buscarPorEmail("prof@dominio.com")).thenReturn(profesor);
            when(rubricaRepositorio.save(any(Rubrica.class))).thenAnswer(inv -> inv.getArgument(0));

            Rubrica guardada = rubricaServicio.guardarDesdeGeneracion(dtoConCriterios(), "c-1", "prof@dominio.com");

            assertEquals("Rubrica de ensayo", guardada.getTitulo());
            assertSame(curso, guardada.getCurso());
            assertSame(profesor, guardada.getCreadoPor());
            assertEquals(1, guardada.getCriterios().size());
            assertEquals(1, guardada.getCriterios().get(0).getNiveles().size());
            assertEquals("Excelente", guardada.getCriterios().get(0).getNiveles().get(0).getNombre());

            ArgumentCaptor<Rubrica> captor = ArgumentCaptor.forClass(Rubrica.class);
            verify(rubricaRepositorio).save(captor.capture());
            assertSame(guardada.getCriterios().get(0).getRubrica(), captor.getValue());
        }
    }

    @Nested
    @DisplayName("listarPorCurso y buscarPorId")
    class Consultas {

        @Test
        @DisplayName("listarPorCurso delega en el repositorio")
        void listarPorCursoDelega() {
            Rubrica r = new Rubrica();
            when(rubricaRepositorio.findByCursoIdOrderByFechaGeneracionDesc("c-1")).thenReturn(List.of(r));

            assertEquals(List.of(r), rubricaServicio.listarPorCurso("c-1"));
        }

        @Test
        @DisplayName("buscarPorId con id inexistente lanza MyException")
        void buscarPorIdInexistenteLanzaExcepcion() {
            when(rubricaRepositorio.findById("r-x")).thenReturn(Optional.empty());
            assertThrows(MyException.class, () -> rubricaServicio.buscarPorId("r-x"));
        }

        @Test
        @DisplayName("buscarPorId con id existente lo retorna")
        void buscarPorIdExistenteLoRetorna() throws MyException {
            Rubrica r = new Rubrica();
            when(rubricaRepositorio.findById("r-1")).thenReturn(Optional.of(r));

            assertSame(r, rubricaServicio.buscarPorId("r-1"));
        }
    }
}
