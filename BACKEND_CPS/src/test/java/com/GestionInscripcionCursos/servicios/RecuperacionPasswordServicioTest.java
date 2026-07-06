package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.PasswordResetToken;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.PasswordResetTokenRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link RecuperacionPasswordServicio}.
 *
 * <p>Cubre el flujo completo de recuperacion de contrasena: solicitud del enlace
 * (incluyendo el caso en que {@code getMostSpecificCause()} nunca es null, ver
 * el fix de la condicion siempre verdadera detectada por SonarQube) y el
 * reestablecimiento de la contrasena con el token recibido.
 */
@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordServicioTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordResetTokenRepositorio tokenRepositorio;

    @Mock
    private CorreoServicio correoServicio;

    @Mock
    private AuditoriaServicio auditoriaServicio;

    @InjectMocks
    private RecuperacionPasswordServicio recuperacionServicio;

    private Usuario usuario(String id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre("Ana Torres");
        u.setPassword("hashViejo");
        return u;
    }

    // =====================================================================
    // solicitarRecuperacion
    // =====================================================================
    @Nested
    @DisplayName("solicitarRecuperacion")
    class SolicitarRecuperacion {

        @Test
        @DisplayName("con email nulo o vacio lanza MyException")
        void emailVacioLanzaExcepcion() {
            assertThrows(MyException.class, () -> recuperacionServicio.solicitarRecuperacion("  "));
            verifyNoInteractions(tokenRepositorio, correoServicio);
        }

        @Test
        @DisplayName("con usuario inexistente no lanza excepcion ni envia correo (no revela si el email existe)")
        void usuarioInexistenteNoHaceNada() {
            when(usuarioRepositorio.buscarPorEmail("nadie@dominio.com")).thenReturn(null);

            assertDoesNotThrow(() -> recuperacionServicio.solicitarRecuperacion("nadie@dominio.com"));

            verifyNoInteractions(tokenRepositorio, correoServicio);
        }

        @Test
        @DisplayName("con usuario existente borra tokens previos, guarda uno nuevo y envia el correo")
        void usuarioExistenteGeneraTokenYEnviaCorreo() throws MyException {
            Usuario u = usuario("1", "ana@dominio.com");
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(u);

            recuperacionServicio.solicitarRecuperacion("ana@dominio.com");

            verify(tokenRepositorio).deleteByUsuarioId("1");
            verify(tokenRepositorio).save(any(PasswordResetToken.class));
            verify(correoServicio).enviarCorreoRecuperacion(eq("ana@dominio.com"), eq("Ana Torres"), anyString());
        }

        @Test
        @DisplayName("si el envio de correo falla, propaga MyException con el detalle SMTP")
        void falloDeCorreoLanzaMyException() {
            Usuario u = usuario("1", "ana@dominio.com");
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(u);
            doThrow(new MailSendException("Fallo de conexion SMTP"))
                    .when(correoServicio).enviarCorreoRecuperacion(anyString(), anyString(), anyString());

            MyException ex = assertThrows(MyException.class, () ->
                    recuperacionServicio.solicitarRecuperacion("ana@dominio.com"));

            assertTrue(ex.getMessage().contains("Fallo de conexion SMTP"));
        }
    }

    // =====================================================================
    // reestablecerPassword
    // =====================================================================
    @Nested
    @DisplayName("reestablecerPassword")
    class ReestablecerPassword {

        private PasswordResetToken token(boolean usado, LocalDateTime expiracion, Usuario usuario) {
            return new PasswordResetToken("token-123", expiracion, usado, LocalDateTime.now().minusMinutes(1), usuario);
        }

        @Test
        @DisplayName("con token vacio lanza MyException")
        void tokenVacioLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    recuperacionServicio.reestablecerPassword(" ", "clave123", "clave123"));
        }

        @Test
        @DisplayName("con contrasenas que no coinciden lanza MyException")
        void passwordsNoCoincidenLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    recuperacionServicio.reestablecerPassword("token-123", "clave123", "otraClave"));
            verifyNoInteractions(tokenRepositorio);
        }

        @Test
        @DisplayName("con token no encontrado lanza MyException")
        void tokenNoEncontradoLanzaExcepcion() {
            when(tokenRepositorio.findByToken("token-123")).thenReturn(Optional.empty());

            assertThrows(MyException.class, () ->
                    recuperacionServicio.reestablecerPassword("token-123", "clave123", "clave123"));
        }

        @Test
        @DisplayName("con token ya utilizado lanza MyException")
        void tokenYaUsadoLanzaExcepcion() {
            Usuario u = usuario("1", "ana@dominio.com");
            when(tokenRepositorio.findByToken("token-123"))
                    .thenReturn(Optional.of(token(true, LocalDateTime.now().plusMinutes(10), u)));

            MyException ex = assertThrows(MyException.class, () ->
                    recuperacionServicio.reestablecerPassword("token-123", "clave123", "clave123"));
            assertTrue(ex.getMessage().contains("utilizado"));
        }

        @Test
        @DisplayName("con token expirado lanza MyException")
        void tokenExpiradoLanzaExcepcion() {
            Usuario u = usuario("1", "ana@dominio.com");
            when(tokenRepositorio.findByToken("token-123"))
                    .thenReturn(Optional.of(token(false, LocalDateTime.now().minusMinutes(1), u)));

            MyException ex = assertThrows(MyException.class, () ->
                    recuperacionServicio.reestablecerPassword("token-123", "clave123", "clave123"));
            assertTrue(ex.getMessage().contains("expir"));
        }

        @Test
        @DisplayName("con token valido actualiza la contrasena del usuario y marca el token como usado")
        void tokenValidoActualizaPassword() throws MyException {
            Usuario u = usuario("1", "ana@dominio.com");
            PasswordResetToken t = token(false, LocalDateTime.now().plusMinutes(10), u);
            when(tokenRepositorio.findByToken("token-123")).thenReturn(Optional.of(t));

            recuperacionServicio.reestablecerPassword("token-123", "claveNueva1", "claveNueva1");

            assertNotEquals("hashViejo", u.getPassword());
            assertTrue(t.isUsed());
            verify(usuarioRepositorio).save(u);
            verify(tokenRepositorio).save(t);
            verify(auditoriaServicio).registrar(eq(AuditoriaServicio.CAMBIO_PASSWORD), eq("ana@dominio.com"), anyString(), eq(true));
        }
    }
}
