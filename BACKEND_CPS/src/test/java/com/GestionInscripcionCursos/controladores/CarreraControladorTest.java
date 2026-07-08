package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.CarreraServicio;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CarreraControlador}: alta, listado, edicion y
 * baja de carreras.
 */
@ExtendWith(MockitoExtension.class)
class CarreraControladorTest {

    @Mock
    private CarreraServicio carreraServicio;

    @InjectMocks
    private CarreraControlador carreraControlador;

    private Carrera carrera(String id, String codigo, String nombre) {
        Carrera c = new Carrera();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre(nombre);
        return c;
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("con datos validos responde 201 con la carrera creada")
        void registraCarreraValida() throws MyException {
            Carrera nueva = carrera("car-1", "SIS", "Ingenieria de Sistemas");
            when(carreraServicio.crear("SIS", "Ingenieria de Sistemas", "descripcion")).thenReturn(nueva);

            ResponseEntity<?> resultado = carreraControlador.registrar("SIS", "Ingenieria de Sistemas", "descripcion");

            assertEquals(HttpStatus.CREATED, resultado.getStatusCode());
            assertEquals(nueva, resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException responde 400")
        void errorServicioResponde400() throws MyException {
            when(carreraServicio.crear("SIS", "Ingenieria de Sistemas", null))
                    .thenThrow(new MyException("El codigo ya existe"));

            ResponseEntity<?> resultado = carreraControlador.registrar("SIS", "Ingenieria de Sistemas", null);

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
            assertEquals(Map.of("error", "El codigo ya existe"), resultado.getBody());
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("responde 200 con todas las carreras")
        void listaCarreras() {
            List<Carrera> carreras = List.of(carrera("car-1", "SIS", "Ingenieria de Sistemas"));
            when(carreraServicio.listar()).thenReturn(carreras);

            ResponseEntity<List<Carrera>> resultado = carreraControlador.listar();

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(carreras, resultado.getBody());
        }
    }

    @Nested
    @DisplayName("modificar")
    class Modificar {

        @Test
        @DisplayName("con datos validos responde 200 con la carrera modificada")
        void modificaCarreraValida() throws MyException {
            Carrera modificada = carrera("car-1", "SIS", "Ingenieria de Software");
            when(carreraServicio.modificar("car-1", "SIS", "Ingenieria de Software", null)).thenReturn(modificada);

            ResponseEntity<?> resultado = carreraControlador.modificar("car-1", "SIS", "Ingenieria de Software", null);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(modificada, resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException responde 400")
        void errorServicioResponde400() throws MyException {
            when(carreraServicio.modificar("car-x", null, null, null))
                    .thenThrow(new MyException("Carrera no encontrada"));

            ResponseEntity<?> resultado = carreraControlador.modificar("car-x", null, null, null);

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
            assertEquals(Map.of("error", "Carrera no encontrada"), resultado.getBody());
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("elimina la carrera y responde 200 con un mensaje de confirmacion")
        void eliminaCorrectamente() throws MyException {
            ResponseEntity<?> resultado = carreraControlador.eliminar("car-1");

            verify(carreraServicio).eliminar("car-1");
            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(Map.of("mensaje", "Carrera eliminada correctamente"), resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException responde 400")
        void errorEliminandoResponde400() throws MyException {
            org.mockito.Mockito.doThrow(new MyException("Carrera con cursos asociados"))
                    .when(carreraServicio).eliminar("car-x");

            ResponseEntity<?> resultado = carreraControlador.eliminar("car-x");

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
            assertEquals(Map.of("error", "Carrera con cursos asociados"), resultado.getBody());
        }
    }
}
