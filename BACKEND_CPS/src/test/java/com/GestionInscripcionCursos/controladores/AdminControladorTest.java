package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.UsuarioAdminRequestDto;
import com.GestionInscripcionCursos.dto.UsuarioResumenDto;
import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.entidades.EventoAuditoria;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.AuditoriaServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link AdminControlador}: dashboard, auditoria y
 * CRUD administrativo de usuarios.
 */
@ExtendWith(MockitoExtension.class)
class AdminControladorTest {

    @Mock
    private UsuarioServicio usuarioServicio;

    @Mock
    private AuditoriaServicio auditoriaServicio;

    @InjectMocks
    private AdminControlador adminControlador;

    private Usuario usuario(String id, String nombre, String email, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    private UsuarioAdminRequestDto requestValido() {
        return new UsuarioAdminRequestDto("Ana Torres", "ana@dominio.com", "clave123", "PROFESOR", "SIS", 3);
    }

    // =====================================================================
    // Endpoint: dashboard administrativo
    // =====================================================================
    @Nested
    @DisplayName("panelAdministrativo")
    class PanelAdministrativo {

        @Test
        @DisplayName("devuelve el mensaje de dashboard")
        void devuelveMensaje() {
            ResponseEntity<?> respuesta = adminControlador.panelAdministrativo();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Dashboard admin"), respuesta.getBody());
        }
    }

    // =====================================================================
    // Endpoint: listar eventos de auditoria
    // =====================================================================
    @Nested
    @DisplayName("listarAuditoria")
    class ListarAuditoria {

        @Test
        @DisplayName("devuelve los eventos recientes del servicio de auditoria")
        void devuelveEventosRecientes() {
            EventoAuditoria evento = new EventoAuditoria(
                    AuditoriaServicio.LOGIN_EXITOSO, "ana@dominio.com", "detalle", true, LocalDateTime.now());
            when(auditoriaServicio.listarRecientes()).thenReturn(List.of(evento));

            ResponseEntity<List<EventoAuditoria>> respuesta = adminControlador.listarAuditoria();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
            assertSame(evento, respuesta.getBody().get(0));
        }
    }

    // =====================================================================
    // Endpoint: listar usuarios
    // =====================================================================
    @Nested
    @DisplayName("listarUsuarios")
    class ListarUsuarios {

        @Test
        @DisplayName("devuelve el listado de usuarios resumido")
        void devuelveListado() {
            UsuarioResumenDto dto = new UsuarioResumenDto("u-1", "Ana Torres", "ana@dominio.com", "PROFESOR", true, new Date());
            when(usuarioServicio.listarUsuarios()).thenReturn(List.of(dto));

            ResponseEntity<List<UsuarioResumenDto>> respuesta = adminControlador.listarUsuarios();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(List.of(dto), respuesta.getBody());
        }
    }

    // =====================================================================
    // Endpoint: obtener un usuario por id
    // =====================================================================
    @Nested
    @DisplayName("obtenerUsuario")
    class ObtenerUsuario {

        @Test
        @DisplayName("con id existente devuelve el usuario mapeado")
        void usuarioExistente() throws MyException {
            Usuario u = usuario("u-1", "Ana Torres", "ana@dominio.com", Rol.PROFESOR);
            when(usuarioServicio.buscarPorId("u-1")).thenReturn(u);

            ResponseEntity<?> respuesta = adminControlador.obtenerUsuario("u-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("u-1", body.get("id"));
            assertEquals("ana@dominio.com", body.get("email"));
            assertEquals(Rol.PROFESOR, body.get("rol"));
            assertEquals("", body.get("carrera"));
            assertEquals(0, body.get("cicloActual"));
            assertEquals(true, body.get("activo"));
        }

        @Test
        @DisplayName("con id inexistente devuelve 404")
        void usuarioInexistente() throws MyException {
            when(usuarioServicio.buscarPorId("u-x")).thenThrow(new MyException("Usuario no encontrado"));

            ResponseEntity<?> respuesta = adminControlador.obtenerUsuario("u-x");

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Usuario no encontrado"), respuesta.getBody());
        }
    }

    // =====================================================================
    // Endpoint: crear usuario
    // =====================================================================
    @Nested
    @DisplayName("crearUsuario")
    class CrearUsuario {

        @Test
        @DisplayName("con datos validos crea el usuario y devuelve 201")
        void datosValidos() throws MyException {
            Usuario creado = usuario("u-2", "Ana Torres", "ana@dominio.com", Rol.PROFESOR);
            Carrera carrera = new Carrera("SIS", "Ingenieria de Sistemas", "desc");
            creado.setCarrera(carrera);
            creado.setCicloActual(3);
            when(usuarioServicio.crearUsuarioAdmin("Ana Torres", "ana@dominio.com", "clave123", "PROFESOR", "SIS", 3))
                    .thenReturn(creado);

            ResponseEntity<?> respuesta = adminControlador.crearUsuario(requestValido());

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("u-2", body.get("id"));
            assertEquals("Ingenieria de Sistemas", body.get("carrera"));
            assertEquals(3, body.get("cicloActual"));
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve 400")
        void servicioLanzaExcepcion() throws MyException {
            when(usuarioServicio.crearUsuarioAdmin(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new MyException("El email está en uso"));

            ResponseEntity<?> respuesta = adminControlador.crearUsuario(requestValido());

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "El email está en uso"), respuesta.getBody());
        }
    }

    // =====================================================================
    // Endpoint: actualizar usuario
    // =====================================================================
    @Nested
    @DisplayName("actualizarUsuario")
    class ActualizarUsuario {

        @Test
        @DisplayName("con datos validos actualiza y devuelve 200")
        void datosValidos() throws MyException {
            Usuario actualizado = usuario("u-1", "Ana Torres", "ana@dominio.com", Rol.PROFESOR);
            when(usuarioServicio.actualizarUsuarioAdmin("u-1", "Ana Torres", "ana@dominio.com", "clave123", "PROFESOR", "SIS", 3))
                    .thenReturn(actualizado);

            ResponseEntity<?> respuesta = adminControlador.actualizarUsuario("u-1", requestValido());

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("u-1", body.get("id"));
        }

        @Test
        @DisplayName("cuando el usuario no existe devuelve 404")
        void usuarioNoEncontrado() throws MyException {
            when(usuarioServicio.actualizarUsuarioAdmin(any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new MyException("Usuario no encontrado"));

            ResponseEntity<?> respuesta = adminControlador.actualizarUsuario("u-x", requestValido());

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("con otro error de validacion devuelve 400")
        void errorDeValidacionDevuelveBadRequest() throws MyException {
            when(usuarioServicio.actualizarUsuarioAdmin(any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new MyException("El email está en uso"));

            ResponseEntity<?> respuesta = adminControlador.actualizarUsuario("u-1", requestValido());

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        }
    }

    // =====================================================================
    // Endpoint: desactivar usuario
    // =====================================================================
    @Nested
    @DisplayName("desactivarUsuario")
    class DesactivarUsuario {

        @Test
        @DisplayName("con id existente desactiva el usuario")
        void desactivaCorrectamente() throws MyException {
            Usuario u = usuario("u-1", "Ana Torres", "ana@dominio.com", Rol.PROFESOR);
            u.setActivo(false);
            when(usuarioServicio.desactivarUsuario("u-1")).thenReturn(u);

            ResponseEntity<?> respuesta = adminControlador.desactivarUsuario("u-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("Usuario desactivado correctamente", body.get("mensaje"));
        }

        @Test
        @DisplayName("con id inexistente devuelve 404")
        void usuarioNoEncontrado() throws MyException {
            when(usuarioServicio.desactivarUsuario("u-x")).thenThrow(new MyException("Usuario no encontrado"));

            ResponseEntity<?> respuesta = adminControlador.desactivarUsuario("u-x");

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        }
    }

    // =====================================================================
    // Endpoint: activar usuario
    // =====================================================================
    @Nested
    @DisplayName("activarUsuario")
    class ActivarUsuario {

        @Test
        @DisplayName("con id existente activa el usuario")
        void activaCorrectamente() throws MyException {
            Usuario u = usuario("u-1", "Ana Torres", "ana@dominio.com", Rol.PROFESOR);
            when(usuarioServicio.activarUsuario("u-1")).thenReturn(u);

            ResponseEntity<?> respuesta = adminControlador.activarUsuario("u-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("Usuario activado correctamente", body.get("mensaje"));
        }

        @Test
        @DisplayName("con id inexistente devuelve 404")
        void usuarioNoEncontrado() throws MyException {
            when(usuarioServicio.activarUsuario("u-x")).thenThrow(new MyException("Usuario no encontrado"));

            ResponseEntity<?> respuesta = adminControlador.activarUsuario("u-x");

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("con un error de validacion distinto de 'no encontrado' devuelve 400")
        void errorDeValidacionDevuelveBadRequest() throws MyException {
            when(usuarioServicio.activarUsuario("u-1")).thenThrow(new MyException("El usuario ya esta activo"));

            ResponseEntity<?> respuesta = adminControlador.activarUsuario("u-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        }
    }
}
