package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.RecuperacionPasswordRequestDto;
import com.GestionInscripcionCursos.dto.ResetPasswordRequestDto;
import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.seguridad.JwtUtil;
import com.GestionInscripcionCursos.servicios.AuditoriaServicio;
import com.GestionInscripcionCursos.servicios.RecuperacionPasswordServicio;
import com.GestionInscripcionCursos.servicios.TwoFactorServicio;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link AuthControlador}: login (con y sin 2FA),
 * gestion de 2FA (setup/enable/disable/qr/status), recuperacion de
 * contrasena y perfil del usuario autenticado.
 */
@ExtendWith(MockitoExtension.class)
class AuthControladorTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioServicio usuarioServicio;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RecuperacionPasswordServicio recuperacionPasswordServicio;

    @Mock
    private TwoFactorServicio twoFactorServicio;

    @Mock
    private AuditoriaServicio auditoriaServicio;

    @InjectMocks
    private AuthControlador authControlador;

    private Usuario usuario(String email, Rol rol, boolean twoFactorEnabled) {
        Usuario u = new Usuario();
        u.setId("u-1");
        u.setNombre("Juan Perez");
        u.setEmail(email);
        u.setRol(rol);
        u.setTwoFactorEnabled(twoFactorEnabled);
        return u;
    }

    private UserDetails userDetails(String email, String rol) {
        return new User(email, "hash", List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private Map<String, String> credenciales(String email, String password) {
        Map<String, String> map = new HashMap<>();
        map.put("email", email);
        map.put("password", password);
        return map;
    }

    // =====================================================================
    // POST /api/auth/login
    // =====================================================================
    @Nested
    @DisplayName("crearTokenAutenticacion (login)")
    class Login {

        @Test
        @DisplayName("con credenciales validas y sin 2FA devuelve token y rol")
        void loginExitosoSin2fa() throws Exception {
            String email = "juan@dominio.com";
            Usuario u = usuario(email, Rol.ALUMNO, false);
            when(usuarioServicio.buscarEmail(email)).thenReturn(u);
            when(usuarioServicio.loadUserByUsername(email)).thenReturn(userDetails(email, "ALUMNO"));
            when(jwtUtil.generateToken(any())).thenReturn("token-123");

            ResponseEntity<?> respuesta = authControlador.crearTokenAutenticacion(credenciales(email, "clave123"));

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("token-123", body.get("token"));
            assertEquals("ROLE_ALUMNO", body.get("rol"));
            assertEquals(false, body.get("twoFactorEnabled"));
            verify(auditoriaServicio).registrar(eq(AuditoriaServicio.LOGIN_EXITOSO), eq(email), anyString(), eq(true));
        }

        @Test
        @DisplayName("con credenciales incorrectas devuelve 401 y registra auditoria de login fallido")
        void loginConCredencialesIncorrectas() throws Exception {
            String email = "juan@dominio.com";
            doThrow(new BadCredentialsException("Credenciales incorrectas"))
                    .when(authenticationManager).authenticate(any());

            ResponseEntity<?> respuesta = authControlador.crearTokenAutenticacion(credenciales(email, "malaClave"));

            assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("Credenciales incorrectas", body.get("error"));
            verify(auditoriaServicio).registrar(eq(AuditoriaServicio.LOGIN_FALLIDO), eq(email), anyString(), eq(false));
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("con 2FA habilitado y codigo valido devuelve el token")
        void loginCon2faValido() throws Exception {
            String email = "juan@dominio.com";
            Usuario u = usuario(email, Rol.ALUMNO, true);
            u.setTwoFactorSecret("SECRETO");
            when(usuarioServicio.buscarEmail(email)).thenReturn(u);
            when(twoFactorServicio.validarCodigo("SECRETO", "123456")).thenReturn(true);
            when(usuarioServicio.loadUserByUsername(email)).thenReturn(userDetails(email, "ALUMNO"));
            when(jwtUtil.generateToken(any())).thenReturn("token-2fa");

            Map<String, String> creds = credenciales(email, "clave123");
            creds.put("otp", "123456");

            ResponseEntity<?> respuesta = authControlador.crearTokenAutenticacion(creds);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("token-2fa", body.get("token"));
            assertEquals(true, body.get("twoFactorEnabled"));
        }

        @Test
        @DisplayName("con 2FA habilitado y codigo invalido o ausente devuelve 401 con twoFactorRequired")
        void loginCon2faInvalido() throws Exception {
            String email = "juan@dominio.com";
            Usuario u = usuario(email, Rol.ALUMNO, true);
            u.setTwoFactorSecret("SECRETO");
            when(usuarioServicio.buscarEmail(email)).thenReturn(u);
            when(twoFactorServicio.validarCodigo(eq("SECRETO"), any())).thenReturn(false);

            ResponseEntity<?> respuesta = authControlador.crearTokenAutenticacion(credenciales(email, "clave123"));

            assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals(true, body.get("twoFactorRequired"));
            verify(auditoriaServicio).registrar(eq(AuditoriaServicio.LOGIN_FALLIDO), eq(email), anyString(), eq(false));
            verifyNoInteractions(jwtUtil);
        }
    }

    // =====================================================================
    // GET /api/auth/2fa/status
    // =====================================================================
    @Nested
    @DisplayName("twoFactorStatus")
    class TwoFactorStatus {

        @Test
        @DisplayName("con usuario existente devuelve el estado habilitado")
        void devuelveEstado() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(usuario("juan@dominio.com", Rol.ALUMNO, true));

            ResponseEntity<?> respuesta = authControlador.twoFactorStatus(auth);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(Map.of("enabled", true), respuesta.getBody());
        }

        @Test
        @DisplayName("con usuario inexistente devuelve 404")
        void usuarioNoEncontrado() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("fantasma@dominio.com");
            when(usuarioServicio.buscarEmail("fantasma@dominio.com")).thenReturn(null);

            ResponseEntity<?> respuesta = authControlador.twoFactorStatus(auth);

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        }
    }

    // =====================================================================
    // POST /api/auth/2fa/setup
    // =====================================================================
    @Nested
    @DisplayName("setupTwoFactor")
    class SetupTwoFactor {

        @Test
        @DisplayName("con usuario inexistente devuelve 404")
        void usuarioNoEncontrado() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("fantasma@dominio.com");
            when(usuarioServicio.buscarEmail("fantasma@dominio.com")).thenReturn(null);

            ResponseEntity<?> respuesta = authControlador.setupTwoFactor(auth);

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("sin secreto previo genera uno nuevo y lo guarda")
        void generaSecretoNuevo() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            when(twoFactorServicio.generarSecreto()).thenReturn("NUEVO_SECRETO");
            when(twoFactorServicio.construirOtpAuthUrl("juan@dominio.com", "NUEVO_SECRETO")).thenReturn("otpauth://totp/x");

            ResponseEntity<?> respuesta = authControlador.setupTwoFactor(auth);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("NUEVO_SECRETO", body.get("secret"));
            assertEquals("otpauth://totp/x", body.get("otpAuthUrl"));
            verify(usuarioServicio).guardar(u);
        }

        @Test
        @DisplayName("con secreto ya existente no genera uno nuevo")
        void reutilizaSecretoExistente() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            u.setTwoFactorSecret("SECRETO_VIEJO");
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            when(twoFactorServicio.construirOtpAuthUrl("juan@dominio.com", "SECRETO_VIEJO")).thenReturn("otpauth://totp/y");

            ResponseEntity<?> respuesta = authControlador.setupTwoFactor(auth);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("SECRETO_VIEJO", body.get("secret"));
            verify(twoFactorServicio, never()).generarSecreto();
            verify(usuarioServicio, never()).guardar(any());
        }
    }

    // =====================================================================
    // GET /api/auth/2fa/qr
    // =====================================================================
    @Nested
    @DisplayName("twoFactorQr")
    class TwoFactorQr {

        @Test
        @DisplayName("con usuario inexistente devuelve 404")
        void usuarioNoEncontrado() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("fantasma@dominio.com");
            when(usuarioServicio.buscarEmail("fantasma@dominio.com")).thenReturn(null);

            ResponseEntity<?> respuesta = authControlador.twoFactorQr(auth, 280);

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("con generacion exitosa devuelve la imagen PNG")
        void generaQrExitosamente() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            u.setTwoFactorSecret("SECRETO");
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            when(twoFactorServicio.construirOtpAuthUrl("juan@dominio.com", "SECRETO")).thenReturn("otpauth://totp/z");
            byte[] qrBytes = {1, 2, 3};
            when(twoFactorServicio.generarQrPng("otpauth://totp/z", 280)).thenReturn(qrBytes);

            ResponseEntity<?> respuesta = authControlador.twoFactorQr(auth, 280);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertArrayEquals(qrBytes, (byte[]) respuesta.getBody());
        }

        @Test
        @DisplayName("cuando el servicio no puede generar el QR devuelve 500")
        void fallaGeneracionDevuelve500() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            u.setTwoFactorSecret("SECRETO");
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            when(twoFactorServicio.construirOtpAuthUrl(anyString(), anyString())).thenReturn("otpauth://totp/z");
            when(twoFactorServicio.generarQrPng(anyString(), anyInt())).thenReturn(new byte[0]);

            ResponseEntity<?> respuesta = authControlador.twoFactorQr(auth, 280);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        }
    }

    // =====================================================================
    // POST /api/auth/2fa/enable
    // =====================================================================
    @Nested
    @DisplayName("enableTwoFactor")
    class EnableTwoFactor {

        @Test
        @DisplayName("con usuario inexistente devuelve 404")
        void usuarioNoEncontrado() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("fantasma@dominio.com");
            when(usuarioServicio.buscarEmail("fantasma@dominio.com")).thenReturn(null);

            ResponseEntity<?> respuesta = authControlador.enableTwoFactor(auth, Map.of("code", "123456"));

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("sin configuracion 2FA previa devuelve 400")
        void sinSecretoDevuelveBadRequest() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);

            ResponseEntity<?> respuesta = authControlador.enableTwoFactor(auth, Map.of("code", "123456"));

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("con codigo invalido devuelve 400")
        void codigoInvalidoDevuelveBadRequest() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            u.setTwoFactorSecret("SECRETO");
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            when(twoFactorServicio.validarCodigo("SECRETO", "000000")).thenReturn(false);

            ResponseEntity<?> respuesta = authControlador.enableTwoFactor(auth, Map.of("code", "000000"));

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            verify(usuarioServicio, never()).guardar(any());
        }

        @Test
        @DisplayName("con codigo valido activa el 2FA")
        void codigoValidoActivaTwoFactor() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            u.setTwoFactorSecret("SECRETO");
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            when(twoFactorServicio.validarCodigo("SECRETO", "123456")).thenReturn(true);

            ResponseEntity<?> respuesta = authControlador.enableTwoFactor(auth, Map.of("code", "123456"));

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertTrue(u.isTwoFactorEnabled());
            verify(usuarioServicio).guardar(u);
        }
    }

    // =====================================================================
    // POST /api/auth/2fa/disable
    // =====================================================================
    @Nested
    @DisplayName("disableTwoFactor")
    class DisableTwoFactor {

        @Test
        @DisplayName("con usuario inexistente devuelve 404")
        void usuarioNoEncontrado() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("fantasma@dominio.com");
            when(usuarioServicio.buscarEmail("fantasma@dominio.com")).thenReturn(null);

            ResponseEntity<?> respuesta = authControlador.disableTwoFactor(auth, Map.of());

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        }

        @Test
        @DisplayName("con 2FA habilitado y codigo invalido devuelve 400")
        void codigoInvalidoDevuelveBadRequest() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, true);
            u.setTwoFactorSecret("SECRETO");
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            when(twoFactorServicio.validarCodigo("SECRETO", "000000")).thenReturn(false);

            ResponseEntity<?> respuesta = authControlador.disableTwoFactor(auth, Map.of("code", "000000"));

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertTrue(u.isTwoFactorEnabled());
        }

        @Test
        @DisplayName("con 2FA habilitado y codigo valido lo desactiva")
        void codigoValidoDesactiva() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, true);
            u.setTwoFactorSecret("SECRETO");
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            when(twoFactorServicio.validarCodigo("SECRETO", "123456")).thenReturn(true);

            ResponseEntity<?> respuesta = authControlador.disableTwoFactor(auth, Map.of("code", "123456"));

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertFalse(u.isTwoFactorEnabled());
            assertNull(u.getTwoFactorSecret());
            verify(usuarioServicio).guardar(u);
        }

        @Test
        @DisplayName("con 2FA ya deshabilitado lo desactiva sin pedir codigo")
        void yaDeshabilitadoNoValidaCodigo() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);

            ResponseEntity<?> respuesta = authControlador.disableTwoFactor(auth, null);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            verify(twoFactorServicio, never()).validarCodigo(anyString(), any());
        }
    }

    // =====================================================================
    // POST /api/auth/forgot-password
    // =====================================================================
    @Nested
    @DisplayName("solicitarRecuperacion (forgot-password)")
    class ForgotPassword {

        @Test
        @DisplayName("con solicitud valida devuelve mensaje generico")
        void solicitudValida() throws MyException {
            RecuperacionPasswordRequestDto req = new RecuperacionPasswordRequestDto();
            req.setEmail("juan@dominio.com");

            ResponseEntity<?> respuesta = authControlador.solicitarRecuperacion(req);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            verify(recuperacionPasswordServicio).solicitarRecuperacion("juan@dominio.com");
        }

        @Test
        @DisplayName("con request nulo pasa email nulo al servicio")
        void requestNulo() throws MyException {
            ResponseEntity<?> respuesta = authControlador.solicitarRecuperacion(null);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            verify(recuperacionPasswordServicio).solicitarRecuperacion(null);
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve 400")
        void servicioLanzaExcepcion() throws MyException {
            RecuperacionPasswordRequestDto req = new RecuperacionPasswordRequestDto();
            req.setEmail("juan@dominio.com");
            doThrow(new MyException("El email es obligatorio"))
                    .when(recuperacionPasswordServicio).solicitarRecuperacion("juan@dominio.com");

            ResponseEntity<?> respuesta = authControlador.solicitarRecuperacion(req);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "El email es obligatorio"), respuesta.getBody());
        }
    }

    // =====================================================================
    // POST /api/auth/reset-password
    // =====================================================================
    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        private ResetPasswordRequestDto request(String token, String password, String password2) {
            ResetPasswordRequestDto dto = new ResetPasswordRequestDto();
            dto.setToken(token);
            dto.setPassword(password);
            dto.setPassword2(password2);
            return dto;
        }

        @Test
        @DisplayName("con datos validos actualiza la contrasena")
        void datosValidos() throws MyException {
            ResponseEntity<?> respuesta = authControlador.resetPassword(request("tok-1", "clave123", "clave123"));

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            verify(recuperacionPasswordServicio).reestablecerPassword("tok-1", "clave123", "clave123");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve 400")
        void servicioLanzaExcepcion() throws MyException {
            doThrow(new MyException("El enlace de recuperación expiró"))
                    .when(recuperacionPasswordServicio).reestablecerPassword(anyString(), anyString(), anyString());

            ResponseEntity<?> respuesta = authControlador.resetPassword(request("tok-1", "clave123", "clave123"));

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "El enlace de recuperación expiró"), respuesta.getBody());
        }
    }

    // =====================================================================
    // GET /api/auth/me
    // =====================================================================
    @Nested
    @DisplayName("perfilActual (/me)")
    class PerfilActual {

        @Test
        @DisplayName("con usuario inexistente devuelve 404")
        void usuarioNoEncontrado() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("fantasma@dominio.com");
            when(usuarioServicio.buscarEmail("fantasma@dominio.com")).thenReturn(null);

            ResponseEntity<?> respuesta = authControlador.perfilActual(auth);

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Usuario no encontrado"), respuesta.getBody());
        }

        @Test
        @DisplayName("con usuario existente y rol definido devuelve el perfil completo")
        void usuarioConRolDefinido() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.PROFESOR, false);
            Carrera carrera = new Carrera("SIS", "Ingenieria de Sistemas", "desc");
            u.setCarrera(carrera);
            u.setCicloActual(5);
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);

            ResponseEntity<?> respuesta = authControlador.perfilActual(auth);

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("PROFESOR", body.get("rol"));
            assertEquals("Ingenieria de Sistemas", body.get("carrera"));
            assertEquals(5, body.get("cicloActual"));
        }

        @Test
        @DisplayName("sin carrera ni ciclo asignados devuelve valores por defecto")
        void usuarioSinCarreraNiCiclo() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", Rol.ALUMNO, false);
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);

            ResponseEntity<?> respuesta = authControlador.perfilActual(auth);

            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("", body.get("carrera"));
            assertEquals(0, body.get("cicloActual"));
        }

        @Test
        @DisplayName("sin rol en el usuario lo deriva de las authorities del Authentication")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void derivaRolDeAuthorities() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("juan@dominio.com");
            Usuario u = usuario("juan@dominio.com", null, false);
            when(usuarioServicio.buscarEmail("juan@dominio.com")).thenReturn(u);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ALUMNO"));
            doReturn(authorities).when(auth).getAuthorities();

            ResponseEntity<?> respuesta = authControlador.perfilActual(auth);

            Map<?, ?> body = (Map<?, ?>) respuesta.getBody();
            assertEquals("ALUMNO", body.get("rol"));
        }
    }
}
