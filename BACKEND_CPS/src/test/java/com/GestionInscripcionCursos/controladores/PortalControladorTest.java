package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link PortalControlador}: endpoints publicos del
 * portal (indice, registro, login informativo e inicio autenticado).
 */
@ExtendWith(MockitoExtension.class)
class PortalControladorTest {

    @Mock
    private UsuarioServicio usuarioServicio;

    @InjectMocks
    private PortalControlador portalControlador;

    private Usuario usuario(String email, Rol rol) {
        Usuario u = new Usuario();
        u.setId("u-1");
        u.setNombre("Ana Torres");
        u.setEmail(email);
        u.setRol(rol);
        return u;
    }

    // =====================================================================
    // GET /api/portal/
    // =====================================================================
    @Nested
    @DisplayName("index")
    class Index {

        @Test
        @DisplayName("devuelve el mensaje de bienvenida de la API")
        void devuelveMensaje() {
            ResponseEntity<?> respuesta = portalControlador.index();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "API de GestionInscripcionCursos"), respuesta.getBody());
        }
    }

    // =====================================================================
    // GET /api/portal/registrar
    // =====================================================================
    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("devuelve el mensaje informativo del endpoint")
        void devuelveMensaje() {
            ResponseEntity<?> respuesta = portalControlador.registrar();

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Endpoint para registrar usuarios"), respuesta.getBody());
        }
    }

    // =====================================================================
    // POST /api/portal/registro
    // =====================================================================
    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("con parametros de request completos registra el usuario y devuelve 201")
        void conParametrosDeRequestCompletos() throws MyException {
            when(usuarioServicio.buscarEmail("ana@dominio.com")).thenReturn(usuario("ana@dominio.com", Rol.ALUMNO));

            ResponseEntity<?> respuesta = portalControlador.registro(
                    "Ana Torres", "ana@dominio.com", "clave123", "clave123", "SIS", 3, null);

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            verify(usuarioServicio).registrar("Ana Torres", "ana@dominio.com", "clave123", "clave123", "SIS", 3);
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("ana@dominio.com", body.get("email"));
        }

        @Test
        @DisplayName("con parametros de request nulos toma los datos del cuerpo JSON")
        void tomaDatosDelBodyCuandoNoHayRequestParams() throws MyException {
            when(usuarioServicio.buscarEmail("beto@dominio.com")).thenReturn(usuario("beto@dominio.com", Rol.ALUMNO));
            Map<String, Object> body = Map.of(
                    "nombre", "Beto Ruiz",
                    "email", "beto@dominio.com",
                    "password", "clave123",
                    "password2", "clave123",
                    "carrera", "SIS",
                    "cicloActual", 2);

            ResponseEntity<?> respuesta = portalControlador.registro(null, null, null, null, null, null, body);

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            verify(usuarioServicio).registrar("Beto Ruiz", "beto@dominio.com", "clave123", "clave123", "SIS", 2);
        }

        @Test
        @DisplayName("con parametros de request en blanco recurre al cuerpo JSON")
        void parametrosEnBlancoRecurrenAlBody() throws MyException {
            when(usuarioServicio.buscarEmail("carla@dominio.com")).thenReturn(usuario("carla@dominio.com", Rol.ALUMNO));
            Map<String, Object> body = Map.of("nombre", "Carla Diaz");

            ResponseEntity<?> respuesta = portalControlador.registro(
                    "", "carla@dominio.com", "clave123", "clave123", null, null, body);

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            verify(usuarioServicio).registrar("Carla Diaz", "carla@dominio.com", "clave123", "clave123", null, null);
        }

        @Test
        @DisplayName("con cicloActual como texto numerico en el body lo convierte a entero")
        void cicloActualComoTextoEnElBody() throws MyException {
            when(usuarioServicio.buscarEmail("dario@dominio.com")).thenReturn(usuario("dario@dominio.com", Rol.ALUMNO));
            Map<String, Object> body = Map.of("cicloActual", "4");

            portalControlador.registro("Dario Leon", "dario@dominio.com", "clave123", "clave123", "SIS", null, body);

            verify(usuarioServicio).registrar("Dario Leon", "dario@dominio.com", "clave123", "clave123", "SIS", 4);
        }

        @Test
        @DisplayName("con cicloActual no numerico en el body lo ignora sin lanzar error")
        void cicloActualNoNumericoEnElBody() throws MyException {
            when(usuarioServicio.buscarEmail("eva@dominio.com")).thenReturn(usuario("eva@dominio.com", Rol.ALUMNO));
            Map<String, Object> body = Map.of("cicloActual", "no-es-numero");

            portalControlador.registro("Eva Ponce", "eva@dominio.com", "clave123", "clave123", "SIS", null, body);

            verify(usuarioServicio).registrar("Eva Ponce", "eva@dominio.com", "clave123", "clave123", "SIS", null);
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve 400")
        void servicioLanzaMyException() throws MyException {
            doThrow(new MyException("El email está en uso"))
                    .when(usuarioServicio).registrar(anyString(), anyString(), anyString(), anyString(), any(), any());

            ResponseEntity<?> respuesta = portalControlador.registro(
                    "Ana Torres", "ana@dominio.com", "clave123", "clave123", "SIS", 3, null);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "El email está en uso"), respuesta.getBody());
        }

        @Test
        @DisplayName("cuando hay una violacion de integridad de datos devuelve 400 con el detalle")
        void servicioLanzaDataIntegrityViolationException() throws MyException {
            DataIntegrityViolationException ex = new DataIntegrityViolationException(
                    "duplicate key", new RuntimeException("Llave duplicada en email"));
            doThrow(ex).when(usuarioServicio)
                    .registrar(anyString(), anyString(), anyString(), anyString(), any(), any());

            ResponseEntity<?> respuesta = portalControlador.registro(
                    "Ana Torres", "ana@dominio.com", "clave123", "clave123", "SIS", 3, null);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("Llave duplicada en email", body.get("detalle"));
            assertTrue(((String) body.get("error")).contains("integridad"));
        }

        @Test
        @DisplayName("cuando ocurre un error inesperado devuelve 500 con el detalle")
        void servicioLanzaErrorInesperado() throws MyException {
            doThrow(new RuntimeException("fallo de conexion"))
                    .when(usuarioServicio).registrar(anyString(), anyString(), anyString(), anyString(), any(), any());

            ResponseEntity<?> respuesta = portalControlador.registro(
                    "Ana Torres", "ana@dominio.com", "clave123", "clave123", "SIS", 3, null);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("fallo de conexion", body.get("detalle"));
        }
    }

    // =====================================================================
    // GET /api/portal/login
    // =====================================================================
    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("sin parametro de error devuelve el mensaje informativo")
        void sinError() {
            ResponseEntity<?> respuesta = portalControlador.login(null);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Login endpoint"), respuesta.getBody());
        }

        @Test
        @DisplayName("con parametro de error devuelve 401")
        void conError() {
            ResponseEntity<?> respuesta = portalControlador.login("true");

            assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Email o Contraseña invalidos"), respuesta.getBody());
        }
    }

    // =====================================================================
    // GET /api/portal/inicio
    // =====================================================================
    @Nested
    @DisplayName("inicio")
    class Inicio {

        @Test
        @DisplayName("sin autenticacion devuelve 401")
        void sinAutenticacion() {
            ResponseEntity<?> respuesta = portalControlador.inicio(null);

            assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("con Authentication no autenticado devuelve 401")
        void noAutenticado() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);

            ResponseEntity<?> respuesta = portalControlador.inicio(auth);

            assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("con usuario no encontrado en base de datos devuelve 401")
        void usuarioNoEncontrado() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn("fantasma@dominio.com");
            when(usuarioServicio.buscarEmail("fantasma@dominio.com")).thenReturn(null);

            ResponseEntity<?> respuesta = portalControlador.inicio(auth);

            assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("con rol ADMIN redirige al dashboard de administracion")
        void rolAdmin() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn("admin@dominio.com");
            when(usuarioServicio.buscarEmail("admin@dominio.com")).thenReturn(usuario("admin@dominio.com", Rol.ADMIN));

            ResponseEntity<?> respuesta = portalControlador.inicio(auth);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("/admin/dashboard", body.get("redirectTo"));
        }

        @Test
        @DisplayName("con rol PROFESOR redirige al dashboard de profesor")
        void rolProfesor() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn("prof@dominio.com");
            when(usuarioServicio.buscarEmail("prof@dominio.com")).thenReturn(usuario("prof@dominio.com", Rol.PROFESOR));

            ResponseEntity<?> respuesta = portalControlador.inicio(auth);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("/profesor/dashboard", body.get("redirectTo"));
        }

        @Test
        @DisplayName("con rol ALUMNO redirige al inicio")
        void rolAlumno() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn("alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(usuario("alumno@dominio.com", Rol.ALUMNO));

            ResponseEntity<?> respuesta = portalControlador.inicio(auth);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("/inicio", body.get("redirectTo"));
        }
    }
}
