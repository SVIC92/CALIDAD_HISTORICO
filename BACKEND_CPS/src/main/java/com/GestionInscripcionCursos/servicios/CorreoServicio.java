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

    public void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        enviarCorreoFormatoMixto(destinatario, asunto, cuerpo, convertirTextoAHtmlSimple(cuerpo));
    }

    public void enviarCorreoRecuperacion(String destinatario, String nombreUsuario, String link) {
        String nombreSeguro = escaparHtml(nombreUsuario);
        String linkSeguro = escaparHtml(link);
        String asunto = "Reestablecer contraseña";

        String cuerpoPlano = "Hola " + nombreUsuario + ",\n\n"
                + "Recibimos una solicitud para reestablecer tu contraseña.\n"
                + "Haz clic en el siguiente enlace para continuar:\n"
                + link + "\n\n"
                + "Este enlace expira en 30 minutos.\n"
                + "Si no solicitaste este cambio, ignora este correo.";

        String cuerpoHtml = construirCorreoHtml(
                "Recuperación de contraseña",
                "Hola " + nombreSeguro + ",",
                "Recibimos una solicitud para reestablecer tu contraseña. Haz clic en el botón para continuar.",
                "Reestablecer contraseña",
                linkSeguro,
                "Este enlace expira en 30 minutos. Si no solicitaste este cambio, puedes ignorar este correo.",
                "Si el botón no funciona, copia y pega este enlace en tu navegador:"
        );

        enviarCorreoFormatoMixto(destinatario, asunto, cuerpoPlano, cuerpoHtml);
    }

    public void enviarCorreoInvitacionVideoconferencia(
            String destinatario,
            String nombreUsuario,
            String tituloSala,
            String rolAsignado,
            String enlace
    ) {
        String nombreSeguro = escaparHtml(nombreUsuario);
        String salaSeguro = escaparHtml(tituloSala);
        String rolSeguro = escaparHtml(rolAsignado);
        String enlaceSeguro = escaparHtml(enlace);
        String asunto = "Invitación a reunión";

        String cuerpoPlano = "Has sido invitado a la reunión '" + tituloSala + "' con el rol de "
                + rolAsignado + ".\nÚnete aquí: " + enlace;

        String cuerpoHtml = construirCorreoHtml(
                "Invitación a videoconferencia",
                "Hola " + nombreSeguro + ",",
                "Has sido invitado a la reunión <strong>" + salaSeguro + "</strong> con el rol de <strong>" + rolSeguro + "</strong>.",
                "Unirme a la reunión",
                enlaceSeguro,
                "Podrás acceder al enlace en cualquier momento mientras la sala esté activa.",
                "Si el botón no funciona, usa este enlace:"
        );

        enviarCorreoFormatoMixto(destinatario, asunto, cuerpoPlano, cuerpoHtml);
    }

    private void enviarCorreoFormatoMixto(String destinatario, String asunto, String cuerpoPlano, String cuerpoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);

            if (from != null && !from.isBlank()) {
                helper.setFrom(new InternetAddress(from, fromName).toString());
            }

            helper.setSubject(asunto);
            helper.setText(cuerpoPlano, cuerpoHtml);

            mailSender.send(mensaje);
        } catch (Exception ex) {
            throw new MailPreparationException("No se pudo preparar el correo", ex);
        }
    }

    private String construirCorreoHtml(
            String encabezado,
            String saludo,
            String descripcion,
            String textoBoton,
            String enlace,
            String pie,
            String textoEnlace
    ) {
        return "<!doctype html>"
                + "<html lang='es'>"
                + "<body style='margin:0;padding:0;background:#f3f6fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;'>"
                + "<div style='max-width:640px;margin:0 auto;padding:32px 16px;'>"
                + "<div style='background:linear-gradient(135deg,#0f172a,#2563eb);color:#fff;padding:28px 32px;border-radius:20px 20px 0 0;'>"
                + "<div style='font-size:12px;letter-spacing:.12em;text-transform:uppercase;opacity:.85;'>GCI+</div>"
                + "<h1 style='margin:10px 0 0;font-size:26px;line-height:1.2;'>" + escaparHtml(encabezado) + "</h1>"
                + "</div>"
                + "<div style='background:#ffffff;padding:32px;border-radius:0 0 20px 20px;box-shadow:0 12px 40px rgba(15,23,42,.08);'>"
                + "<p style='margin:0 0 16px;font-size:16px;line-height:1.6;'>" + saludo + "</p>"
                + "<p style='margin:0 0 24px;font-size:15px;line-height:1.7;color:#475569;'>" + descripcion + "</p>"
                + "<div style='text-align:center;margin:28px 0;'>"
                + "<a href='" + enlace + "' style='display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:14px 24px;border-radius:12px;font-weight:700;font-size:15px;'>" + escaparHtml(textoBoton) + "</a>"
                + "</div>"
                + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;padding:16px 18px;margin:0 0 20px;'>"
                + "<p style='margin:0 0 6px;font-size:13px;color:#64748b;'>" + escaparHtml(textoEnlace) + "</p>"
                + "<a href='" + enlace + "' style='word-break:break-all;color:#1d4ed8;text-decoration:none;font-size:14px;'>" + enlace + "</a>"
                + "</div>"
                + "<p style='margin:0;font-size:13px;line-height:1.6;color:#64748b;'>" + escaparHtml(pie) + "</p>"
                + "</div>"
                + "<p style='text-align:center;color:#94a3b8;font-size:12px;line-height:1.5;margin:16px 0 0;'>"
                + "Mensaje automático de GCI+. No respondas a este correo."
                + "</p>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private String convertirTextoAHtmlSimple(String texto) {
        return "<pre style='margin:0;font-family:Arial,Helvetica,sans-serif;white-space:pre-wrap;color:#1f2937;'>"
                + escaparHtml(texto)
                + "</pre>";
    }

    private String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
