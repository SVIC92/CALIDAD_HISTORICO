package com.GestionInscripcionCursos.servicios;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class CorreoServicio {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String from;

    @Value("${app.mail.from-name:GCI+}")
    private String fromName;

    public CorreoServicio(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreoRecuperacion(String destinatario, String nombreUsuario, String link) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");

            helper.setTo(destinatario);

            if (from != null && !from.isBlank()) {
                helper.setFrom(new InternetAddress(from, fromName).toString());
            }

            helper.setSubject("Reestablecer contraseña");
            helper.setText(
                    "Hola " + nombreUsuario + ",\n\n"
                    + "Recibimos una solicitud para reestablecer tu contraseña.\n"
                    + "Haz clic en el siguiente enlace para continuar:\n"
                    + link + "\n\n"
                    + "Este enlace expira en 30 minutos.\n"
                    + "Si no solicitaste este cambio, ignora este correo."
            );

            mailSender.send(mensaje);
        } catch (Exception ex) {
            throw new MailPreparationException("No se pudo preparar el correo de recuperacion", ex);
        }
    }
}
