package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.ProfesorResumenDto;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;

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
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ProfesorControlador}: panel administrativo
 * estatico y listado de profesores.
 */
@ExtendWith(MockitoExtension.class)
class ProfesorControladorTest {

    @Mock
    private UsuarioServicio usuarioServicio;

    @InjectMocks
    private ProfesorControlador profesorControlador;

    @Nested
    @DisplayName("panelAdministrativo")
    class PanelAdministrativo {

        @Test
        @DisplayName("responde 200 con el mensaje del dashboard de profesor")
        void devuelveMensajeDashboard() {
            ResponseEntity<?> resultado = profesorControlador.panelAdministrativo();

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(Map.of("mensaje", "Dashboard profesor"), resultado.getBody());
        }
    }

    @Nested
    @DisplayName("listarProfesores")
    class ListarProfesores {

        @Test
        @DisplayName("responde 200 con el resumen de profesores del servicio")
        void devuelveListaProfesores() {
            List<ProfesorResumenDto> profesores = List.of(new ProfesorResumenDto("id-1", "Prof Uno", "uno@test.com"));
            when(usuarioServicio.listarProfesores()).thenReturn(profesores);

            ResponseEntity<List<ProfesorResumenDto>> resultado = profesorControlador.listarProfesores();

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(profesores, resultado.getBody());
        }
    }
}
