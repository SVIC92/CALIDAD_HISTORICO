package com.GestionInscripcionCursos.servicios;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link CorreoServicio}.
 *
 * <p>{@code JavaMailSender} se mockea y {@code createMimeMessage()} se stubea para
 * devolver un {@link MimeMessage} real (creado con una {@link Session} en memoria),
 * ya que {@code MimeMessageHelper} necesita un mensaje JavaMail funcional para
 * poder setear destinatario, asunto y cuerpo. Se captura el mensaje enviado con
 * {@link ArgumentCaptor} para verificar asunto y destinatario; el contenido HTML
 * escapado se valida directamente sobre el metodo privado {@code escaparHtml} via
 * reflexion, evitando depender de la estructura interna del multipart generado
 * por MimeMessageHelper.
 */
@ExtendWith(MockitoExtension.class)
class CorreoServicioTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private CorreoServicio correoServicio;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(correoServicio, "from", "no-reply@gci.pe");
        ReflectionTestUtils.setField(correoServicio, "fromName", "GCI+");
    }

    private void stubMimeMessageReal() {
        Session session = Session.getDefaultInstance(new Properties());
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(session));
    }

    @SuppressWarnings("unchecked")
    private <T> T invocarPrivado(String metodo, Class<?>[] tipos, Object... args) throws Exception {
        Method m = CorreoServicio.class.getDeclaredMethod(metodo, tipos);
        m.setAccessible(true);
        return (T) m.invoke(correoServicio, args);
    }

    // =====================================================================
    // enviarCorreo
    // =====================================================================
    @Nested
    @DisplayName("enviarCorreo")
    class EnviarCorreo {

        @Test
        @DisplayName("con datos validos arma el mensaje con destinatario y asunto correctos y lo envia")
        void enviaCorreoConDatosValidos() throws Exception {
            stubMimeMessageReal();

            correoServicio.enviarCorreo("alumno@dominio.com", "Aviso importante", "Contenido del aviso");

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            verify(mailSender).send(captor.capture());
            MimeMessage enviado = captor.getValue();
            assertEquals("Aviso importante", enviado.getSubject());
            assertEquals(1, enviado.getAllRecipients().length);
            assertTrue(enviado.getAllRecipients()[0].toString().contains("alumno@dominio.com"));
        }

        @Test
        @DisplayName("sin remitente configurado igual envia el correo")
        void sinRemitenteConfiguradoIgualEnvia() {
            ReflectionTestUtils.setField(correoServicio, "from", "");
            stubMimeMessageReal();

            assertDoesNotThrow(() -> correoServicio.enviarCorreo("alumno@dominio.com", "Aviso", "Cuerpo"));

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("si el envio falla en el servidor de correo, propaga MailPreparationException")
        void falloDeEnvioLanzaMailPreparationException() {
            stubMimeMessageReal();
            doThrow(new MailSendException("SMTP caido")).when(mailSender).send(any(MimeMessage.class));

            assertThrows(MailPreparationException.class, () ->
                    correoServicio.enviarCorreo("alumno@dominio.com", "Aviso", "Cuerpo"));
        }

        @Test
        @DisplayName("si no se puede crear el MimeMessage, propaga MailPreparationException")
        void falloAlCrearMimeMessageLanzaMailPreparationException() {
            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("sin memoria"));

            assertThrows(MailPreparationException.class, () ->
                    correoServicio.enviarCorreo("alumno@dominio.com", "Aviso", "Cuerpo"));
        }
    }

    // =====================================================================
    // enviarCorreoRecuperacion
    // =====================================================================
    @Nested
    @DisplayName("enviarCorreoRecuperacion")
    class EnviarCorreoRecuperacion {

        @Test
        @DisplayName("arma el correo con el asunto de reestablecimiento y lo envia")
        void enviaCorreoDeRecuperacion() throws Exception {
            stubMimeMessageReal();

            correoServicio.enviarCorreoRecuperacion("ana@dominio.com", "Ana Torres", "https://gci.pe/reset?token=abc");

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            verify(mailSender).send(captor.capture());
            assertEquals("Reestablecer contraseña", captor.getValue().getSubject());
            assertTrue(captor.getValue().getAllRecipients()[0].toString().contains("ana@dominio.com"));
        }
    }

    // =====================================================================
    // enviarCorreoInvitacionVideoconferencia
    // =====================================================================
    @Nested
    @DisplayName("enviarCorreoInvitacionVideoconferencia")
    class EnviarCorreoInvitacion {

        @Test
        @DisplayName("arma el correo de invitacion con el asunto correcto y lo envia")
        void enviaCorreoDeInvitacion() throws Exception {
            stubMimeMessageReal();

            correoServicio.enviarCorreoInvitacionVideoconferencia(
                    "profe@dominio.com", "Carlos Ruiz", "Sala de Algebra", "MODERADOR", "https://gci.pe/sala/123");

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            verify(mailSender).send(captor.capture());
            assertEquals("Invitación a reunión", captor.getValue().getSubject());
            assertTrue(captor.getValue().getAllRecipients()[0].toString().contains("profe@dominio.com"));
        }
    }

    // =====================================================================
    // escaparHtml (probado via reflexion: logica pura sin dependencias)
    // =====================================================================
    @Nested
    @DisplayName("escaparHtml")
    class EscaparHtml {

        @Test
        @DisplayName("escapa los caracteres especiales de HTML")
        void escapaCaracteresEspeciales() throws Exception {
            String resultado = invocarPrivado("escaparHtml", new Class<?>[]{String.class},
                    "<script>alert(\"hola\" & 'mundo')</script>");

            assertEquals("&lt;script&gt;alert(&quot;hola&quot; &amp; &#39;mundo&#39;)&lt;/script&gt;", resultado);
        }

        @Test
        @DisplayName("con texto nulo retorna cadena vacia")
        void conTextoNuloRetornaVacio() throws Exception {
            String resultado = invocarPrivado("escaparHtml", new Class<?>[]{String.class}, new Object[]{null});
            assertEquals("", resultado);
        }
    }

    // =====================================================================
    // convertirTextoAHtmlSimple (probado via reflexion)
    // =====================================================================
    @Nested
    @DisplayName("convertirTextoAHtmlSimple")
    class ConvertirTextoAHtmlSimple {

        @Test
        @DisplayName("envuelve el texto escapado en un bloque <pre>")
        void envuelveTextoEscapadoEnPre() throws Exception {
            String resultado = invocarPrivado("convertirTextoAHtmlSimple", new Class<?>[]{String.class}, "Hola <mundo>");

            assertTrue(resultado.startsWith("<pre"));
            assertTrue(resultado.contains("Hola &lt;mundo&gt;"));
        }
    }
}
