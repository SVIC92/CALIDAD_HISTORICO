package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CarreraRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link CarreraServicio}: alta, listado, modificacion
 * y eliminacion de carreras.
 */
@ExtendWith(MockitoExtension.class)
class CarreraServicioTest {

    @Mock
    private CarreraRepositorio carreraRepositorio;

    @InjectMocks
    private CarreraServicio carreraServicio;

    private Carrera carrera(String id, String codigo, String nombre) {
        Carrera c = new Carrera(codigo, nombre, "descripcion");
        c.setId(id);
        return c;
    }

    // =====================================================================
    // crear
    // =====================================================================
    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("con codigo nulo lanza MyException")
        void codigoNuloLanzaExcepcion() {
            assertThrows(MyException.class, () -> carreraServicio.crear(null, "Ingenieria", "desc"));
            verify(carreraRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con codigo en blanco lanza MyException")
        void codigoEnBlancoLanzaExcepcion() {
            assertThrows(MyException.class, () -> carreraServicio.crear("   ", "Ingenieria", "desc"));
            verify(carreraRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con nombre nulo lanza MyException")
        void nombreNuloLanzaExcepcion() {
            assertThrows(MyException.class, () -> carreraServicio.crear("ISI", null, "desc"));
            verify(carreraRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con nombre en blanco lanza MyException")
        void nombreEnBlancoLanzaExcepcion() {
            assertThrows(MyException.class, () -> carreraServicio.crear("ISI", "   ", "desc"));
            verify(carreraRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con codigo ya existente (normalizado) lanza MyException")
        void codigoDuplicadoLanzaExcepcion() {
            when(carreraRepositorio.findByCodigoIgnoreCase("ISI")).thenReturn(Optional.of(carrera("c-1", "ISI", "Ing. de Sistemas")));

            assertThrows(MyException.class, () -> carreraServicio.crear("isi", "Otra Carrera", "desc"));
            verify(carreraRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con datos validos normaliza el codigo a mayusculas y guarda")
        void creaCarreraValida() throws MyException {
            when(carreraRepositorio.findByCodigoIgnoreCase("ISI")).thenReturn(Optional.empty());
            when(carreraRepositorio.save(any(Carrera.class))).thenAnswer(inv -> inv.getArgument(0));

            Carrera creada = carreraServicio.crear(" isi ", " Ingenieria de Sistemas ", "desc");

            assertEquals("ISI", creada.getCodigo());
            assertEquals("Ingenieria de Sistemas", creada.getNombre());
            verify(carreraRepositorio).save(any(Carrera.class));
        }
    }

    // =====================================================================
    // listar
    // =====================================================================
    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("retorna las carreras ordenadas por nombre ignorando mayusculas/minusculas")
        void listaOrdenadaPorNombre() {
            Carrera zeta = carrera("c-1", "ZET", "Zeta Carrera");
            Carrera alfa = carrera("c-2", "ALF", "alfa Carrera");
            when(carreraRepositorio.findAll()).thenReturn(List.of(zeta, alfa));

            List<Carrera> resultado = carreraServicio.listar();

            assertEquals(2, resultado.size());
            assertEquals("alfa Carrera", resultado.get(0).getNombre());
            assertEquals("Zeta Carrera", resultado.get(1).getNombre());
        }

        @Test
        @DisplayName("sin carreras registradas retorna lista vacia")
        void sinCarrerasRetornaListaVacia() {
            when(carreraRepositorio.findAll()).thenReturn(List.of());

            assertTrue(carreraServicio.listar().isEmpty());
        }
    }

    // =====================================================================
    // modificar
    // =====================================================================
    @Nested
    @DisplayName("modificar")
    class Modificar {

        @Test
        @DisplayName("con id inexistente lanza MyException")
        void idInexistenteLanzaExcepcion() {
            when(carreraRepositorio.findById("c-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> carreraServicio.modificar("c-x", "ISI", "Nombre", "desc"));
            verify(carreraRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con nuevo codigo perteneciente a otra carrera lanza MyException")
        void codigoUsadoPorOtraCarreraLanzaExcepcion() {
            Carrera existente = carrera("c-1", "ISI", "Ing. de Sistemas");
            Carrera otra = carrera("c-2", "ISW", "Ing. de Software");
            when(carreraRepositorio.findById("c-1")).thenReturn(Optional.of(existente));
            when(carreraRepositorio.findByCodigoIgnoreCase("ISW")).thenReturn(Optional.of(otra));

            assertThrows(MyException.class, () -> carreraServicio.modificar("c-1", "ISW", null, null));
            verify(carreraRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con nuevo codigo perteneciente a la misma carrera no lanza excepcion")
        void codigoPerteneceALaMismaCarreraNoLanzaExcepcion() throws MyException {
            Carrera existente = carrera("c-1", "ISI", "Ing. de Sistemas");
            when(carreraRepositorio.findById("c-1")).thenReturn(Optional.of(existente));
            when(carreraRepositorio.findByCodigoIgnoreCase("ISI")).thenReturn(Optional.of(existente));
            when(carreraRepositorio.save(any(Carrera.class))).thenAnswer(inv -> inv.getArgument(0));

            carreraServicio.modificar("c-1", "isi", null, null);

            verify(carreraRepositorio).save(existente);
        }

        @Test
        @DisplayName("con codigo y nombre en blanco no los modifica, pero actualiza la descripcion")
        void codigoYNombreEnBlancoNoSeModifican() throws MyException {
            Carrera existente = carrera("c-1", "ISI", "Ing. de Sistemas");
            when(carreraRepositorio.findById("c-1")).thenReturn(Optional.of(existente));
            when(carreraRepositorio.save(any(Carrera.class))).thenAnswer(inv -> inv.getArgument(0));

            carreraServicio.modificar("c-1", "   ", "   ", "nueva descripcion");

            assertEquals("ISI", existente.getCodigo());
            assertEquals("Ing. de Sistemas", existente.getNombre());
            assertEquals("nueva descripcion", existente.getDescripcion());
            verify(carreraRepositorio).save(existente);
        }

        @Test
        @DisplayName("con datos validos actualiza codigo, nombre y descripcion")
        void modificaCarreraValida() throws MyException {
            Carrera existente = carrera("c-1", "ISI", "Ing. de Sistemas");
            when(carreraRepositorio.findById("c-1")).thenReturn(Optional.of(existente));
            when(carreraRepositorio.findByCodigoIgnoreCase("ISW")).thenReturn(Optional.empty());
            when(carreraRepositorio.save(any(Carrera.class))).thenAnswer(inv -> inv.getArgument(0));

            Carrera resultado = carreraServicio.modificar("c-1", " isw ", " Ing. de Software ", "desc nueva");

            assertEquals("ISW", resultado.getCodigo());
            assertEquals("Ing. de Software", resultado.getNombre());
            assertEquals("desc nueva", resultado.getDescripcion());
        }
    }

    // =====================================================================
    // eliminar
    // =====================================================================
    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("con id inexistente lanza MyException")
        void idInexistenteLanzaExcepcion() {
            when(carreraRepositorio.findById("c-x")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () -> carreraServicio.eliminar("c-x"));
            verify(carreraRepositorio, never()).delete(any());
        }

        @Test
        @DisplayName("con cursos asociados lanza MyException")
        void conCursosAsociadosLanzaExcepcion() {
            Carrera existente = carrera("c-1", "ISI", "Ing. de Sistemas");
            existente.setCursos(new ArrayList<>(List.of(new Curso())));
            when(carreraRepositorio.findById("c-1")).thenReturn(Optional.of(existente));

            assertThrows(MyException.class, () -> carreraServicio.eliminar("c-1"));
            verify(carreraRepositorio, never()).delete(any());
        }

        @Test
        @DisplayName("sin cursos asociados elimina la carrera")
        void sinCursosAsociadosEliminaCarrera() throws MyException {
            Carrera existente = carrera("c-1", "ISI", "Ing. de Sistemas");
            when(carreraRepositorio.findById("c-1")).thenReturn(Optional.of(existente));

            carreraServicio.eliminar("c-1");

            verify(carreraRepositorio).delete(existente);
        }
    }
}
