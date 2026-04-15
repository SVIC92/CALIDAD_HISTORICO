package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.PasswordResetToken;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.PasswordResetTokenRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.mail.MailException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RecuperacionPasswordServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordResetTokenRepositorio tokenRepositorio;
    private final CorreoServicio correoServicio;

    @Value("${app.frontend.reset-password-url:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    public RecuperacionPasswordServicio(
            UsuarioRepositorio usuarioRepositorio,
            PasswordResetTokenRepositorio tokenRepositorio,
            CorreoServicio correoServicio
    ) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.tokenRepositorio = tokenRepositorio;
        this.correoServicio = correoServicio;
    }

    @Transactional
    public void solicitarRecuperacion(String email) throws MyException {
        if (email == null || email.isBlank()) {
            throw new MyException("El email es obligatorio");
        }

        Usuario usuario = usuarioRepositorio.buscarPorEmail(email.trim());
        if (usuario == null) {
            return;
        }

        tokenRepositorio.deleteByUsuarioId(usuario.getId());

        String tokenPlano = UUID.randomUUID().toString();
        LocalDateTime ahora = LocalDateTime.now();
        PasswordResetToken token = new PasswordResetToken(
                tokenPlano,
                ahora.plusMinutes(30),
                false,
                ahora,
                usuario
        );

        tokenRepositorio.save(token);

        String link = resetPasswordUrl + "?token=" + tokenPlano;
        try {
            correoServicio.enviarCorreoRecuperacion(usuario.getEmail(), usuario.getNombre(), link);
        } catch (MailException ex) {
            throw new MyException("No se pudo enviar el correo de recuperacion. Revisa la configuracion SMTP (MAIL_USERNAME/MAIL_PASSWORD)");
        }
    }

    @Transactional
    public void reestablecerPassword(String tokenPlano, String password, String password2) throws MyException {
        validarNuevaPassword(tokenPlano, password, password2);

        Optional<PasswordResetToken> respuesta = tokenRepositorio.findByToken(tokenPlano);
        if (respuesta.isEmpty()) {
            throw new MyException("El enlace de recuperación es inválido");
        }

        PasswordResetToken token = respuesta.get();

        if (token.isUsed()) {
            throw new MyException("El enlace de recuperación ya fue utilizado");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new MyException("El enlace de recuperación expiró");
        }

        Usuario usuario = token.getUsuario();
        usuario.setPassword(new BCryptPasswordEncoder().encode(password));
        usuarioRepositorio.save(usuario);

        token.setUsed(true);
        tokenRepositorio.save(token);
    }

    private void validarNuevaPassword(String token, String password, String password2) throws MyException {
        if (token == null || token.isBlank()) {
            throw new MyException("El token es obligatorio");
        }

        if (password == null || password.isBlank() || password.length() <= 5) {
            throw new MyException("La contraseña debe tener más de 5 caracteres");
        }

        if (password2 == null || password2.isBlank()) {
            throw new MyException("La confirmación de contraseña es obligatoria");
        }

        if (!password.equals(password2)) {
            throw new MyException("Las contraseñas ingresadas deben ser iguales");
        }

        if (password.length() > 255 || password2.length() > 255) {
            throw new MyException("La contraseña no debe pasar de 255 caracteres");
        }
    }
}
