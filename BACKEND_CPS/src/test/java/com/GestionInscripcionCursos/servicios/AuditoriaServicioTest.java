package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.EventoAuditoria;
import com.GestionInscripcionCursos.repositorios.EventoAuditoriaRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link AuditoriaServicio} (OWASP A09 - Security
 * Logging and Monitoring Failures).
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaServicioTest {

    @Mock
    private EventoAuditoriaRepositorio eventoAuditoriaRepositorio;

    @InjectMocks
    private AuditoriaServicio auditoriaServicio;

    @Test
    @DisplayName("registrar guarda un evento con tipo, usuario, detalle y resultado")
    void registrarGuardaElEvento() {
        auditoriaServicio.registrar(AuditoriaServicio.LOGIN_EXITOSO, "ana@dominio.com", "Inicio de sesion correcto", true);

        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(eventoAuditoriaRepositorio).save(captor.capture());

        EventoAuditoria evento = captor.getValue();
        assertEquals(AuditoriaServicio.LOGIN_EXITOSO, evento.getTipoEvento());
        assertEquals("ana@dominio.com", evento.getUsuarioEmail());
        assertTrue(evento.isExitoso());
        assertNotNull(evento.getFecha());
    }

    @Test
    @DisplayName("registrar no propaga la excepcion si el repositorio falla")
    void registrarNoPropagaExcepcionDelRepositorio() {
        when(eventoAuditoriaRepositorio.save(any(EventoAuditoria.class))).thenThrow(new RuntimeException("BD caida"));

        assertDoesNotThrow(() ->
                auditoriaServicio.registrar(AuditoriaServicio.LOGIN_FALLIDO, "ana@dominio.com", "Credenciales incorrectas", false));
    }

    @Test
    @DisplayName("listarRecientes delega en el repositorio ordenado por fecha descendente")
    void listarRecientesDelegaEnElRepositorio() {
        EventoAuditoria evento = new EventoAuditoria();
        when(eventoAuditoriaRepositorio.findTop200ByOrderByFechaDesc()).thenReturn(List.of(evento));

        List<EventoAuditoria> resultado = auditoriaServicio.listarRecientes();

        assertEquals(1, resultado.size());
        assertSame(evento, resultado.get(0));
    }
}
