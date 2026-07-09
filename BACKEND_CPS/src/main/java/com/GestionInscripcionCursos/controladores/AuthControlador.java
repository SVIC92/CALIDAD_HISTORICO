package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.RecuperacionPasswordRequestDto;
import com.GestionInscripcionCursos.dto.ResetPasswordRequestDto;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.seguridad.JwtUtil;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.servicios.AuditoriaServicio;
import com.GestionInscripcionCursos.servicios.RecuperacionPasswordServicio;
import com.GestionInscripcionCursos.servicios.TwoFactorServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    private static final Logger log = LoggerFactory.getLogger(AuthControlador.class);

    private static final String CLAVE_ERROR = "error";
    private static final String CLAVE_MENSAJE = "mensaje";
    private static final String CLAVE_ENABLED = "enabled";
    private static final String MSG_USUARIO_NO_ENCONTRADO = "Usuario no encontrado";

    private final AuthenticationManager authenticationManager;
    private final UsuarioServicio usuarioServicio;
    private final JwtUtil jwtUtil;
    private final RecuperacionPasswordServicio recuperacionPasswordServicio;
    private final TwoFactorServicio twoFactorServicio;
    private final AuditoriaServicio auditoriaServicio;

    public AuthControlador(
            AuthenticationManager authenticationManager,
            UsuarioServicio usuarioServicio,
            JwtUtil jwtUtil,
            RecuperacionPasswordServicio recuperacionPasswordServicio,
            TwoFactorServicio twoFactorServicio,
            AuditoriaServicio auditoriaServicio
    ) {
        this.authenticationManager = authenticationManager;
        this.usuarioServicio = usuarioServicio;
        this.jwtUtil = jwtUtil;
        this.recuperacionPasswordServicio = recuperacionPasswordServicio;
        this.twoFactorServicio = twoFactorServicio;
        this.auditoriaServicio = auditoriaServicio;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> crearTokenAutenticacion(@RequestBody Map<String, String> credenciales) throws Exception {
        String email = credenciales.get("email");
        try {
            // Verificamos email y contraseña
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, credenciales.get("password"))
            );
        } catch (Exception e) {
            log.warn("Intento de login fallido para el email: {}", email);
            auditoriaServicio.registrar(AuditoriaServicio.LOGIN_FALLIDO, email, "Credenciales incorrectas", false);
            return ResponseEntity.status(401).body(Map.of(CLAVE_ERROR, "Credenciales incorrectas"));
        }

        // Si es correcto, generamos el token
        Usuario usuario = usuarioServicio.buscarEmail(email);
        if (usuario != null && usuario.isTwoFactorEnabled()) {
            String otp = credenciales.get("otp");
            boolean otpValido = twoFactorServicio.validarCodigo(usuario.getTwoFactorSecret(), otp);
            if (!otpValido) {
                auditoriaServicio.registrar(AuditoriaServicio.LOGIN_FALLIDO, email, "Codigo 2FA invalido o ausente", false);
                return ResponseEntity.status(401).body(Map.of(
                        "twoFactorRequired", true,
                        CLAVE_MENSAJE, "Codigo de autenticacion invalido o ausente"
                ));
            }
        }

        final UserDetails userDetails = usuarioServicio.loadUserByUsername(email);
        final String jwt = jwtUtil.generateToken(userDetails);
        auditoriaServicio.registrar(AuditoriaServicio.LOGIN_EXITOSO, email, "Inicio de sesion correcto", true);

        // Preparamos la respuesta para React
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("token", jwt);
        respuesta.put("rol", userDetails.getAuthorities().iterator().next().getAuthority());
        respuesta.put("twoFactorEnabled", usuario != null && usuario.isTwoFactorEnabled());

        return ResponseEntity.ok(respuesta);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/2fa/status")
    public ResponseEntity<Object> twoFactorStatus(Authentication authentication) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of(CLAVE_ERROR, MSG_USUARIO_NO_ENCONTRADO));
        }
        return ResponseEntity.ok(Map.of(CLAVE_ENABLED, usuario.isTwoFactorEnabled()));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/2fa/setup")
    public ResponseEntity<Object> setupTwoFactor(Authentication authentication) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of(CLAVE_ERROR, MSG_USUARIO_NO_ENCONTRADO));
        }

        String secret = usuario.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            secret = twoFactorServicio.generarSecreto();
            usuario.setTwoFactorSecret(secret);
            usuarioServicio.guardar(usuario);
        }

        String otpAuthUrl = twoFactorServicio.construirOtpAuthUrl(usuario.getEmail(), secret);
        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "otpAuthUrl", otpAuthUrl,
                CLAVE_ENABLED, usuario.isTwoFactorEnabled()
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/2fa/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Object> twoFactorQr(Authentication authentication,
            @RequestParam(defaultValue = "280") int size) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of(CLAVE_ERROR, MSG_USUARIO_NO_ENCONTRADO));
        }

        String secret = usuario.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            secret = twoFactorServicio.generarSecreto();
            usuario.setTwoFactorSecret(secret);
            usuarioServicio.guardar(usuario);
        }

        String otpAuthUrl = twoFactorServicio.construirOtpAuthUrl(usuario.getEmail(), secret);
        byte[] qr = twoFactorServicio.generarQrPng(otpAuthUrl, size);

        if (qr.length == 0) {
            return ResponseEntity.internalServerError().body(Map.of(CLAVE_ERROR, "No se pudo generar el QR"));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(qr);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/2fa/enable")
    public ResponseEntity<Object> enableTwoFactor(Authentication authentication, @RequestBody Map<String, String> body) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of(CLAVE_ERROR, MSG_USUARIO_NO_ENCONTRADO));
        }

        String secret = usuario.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, "Primero debes generar la configuracion 2FA"));
        }

        String codigo = body != null ? body.get("code") : null;
        if (!twoFactorServicio.validarCodigo(secret, codigo)) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, "Codigo de autenticacion invalido"));
        }

        usuario.setTwoFactorEnabled(true);
        usuarioServicio.guardar(usuario);

        return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Two factor activado correctamente", CLAVE_ENABLED, true));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/2fa/disable")
    public ResponseEntity<Object> disableTwoFactor(Authentication authentication, @RequestBody Map<String, String> body) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of(CLAVE_ERROR, MSG_USUARIO_NO_ENCONTRADO));
        }

        if (usuario.isTwoFactorEnabled()) {
            String codigo = body != null ? body.get("code") : null;
            if (!twoFactorServicio.validarCodigo(usuario.getTwoFactorSecret(), codigo)) {
                return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, "Codigo de autenticacion invalido"));
            }
        }

        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret(null);
        usuarioServicio.guardar(usuario);

        return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Two factor desactivado correctamente", CLAVE_ENABLED, false));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Object> solicitarRecuperacion(@RequestBody RecuperacionPasswordRequestDto request) {
        try {
            recuperacionPasswordServicio.solicitarRecuperacion(request != null ? request.getEmail() : null);
            return ResponseEntity.ok(Map.of(
                    CLAVE_MENSAJE,
                    "Si el correo existe en el sistema, recibiras un enlace para reestablecer tu contraseña"
            ));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        try {
            recuperacionPasswordServicio.reestablecerPassword(
                    request.getToken(),
                    request.getPassword(),
                    request.getPassword2()
            );
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Contraseña actualizada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<Object> perfilActual(Authentication authentication) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of(CLAVE_MENSAJE, MSG_USUARIO_NO_ENCONTRADO));
        }

        String rol = usuario.getRol() != null ? usuario.getRol().name() : null;
        if (rol == null && authentication != null && authentication.getAuthorities() != null) {
            rol = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                    .orElse(null);
        }

        return ResponseEntity.ok(Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "email", usuario.getEmail(),
            "rol", rol != null ? rol : "SIN_ROL",
            "carrera", usuario.getCarrera() != null ? usuario.getCarrera().getNombre() : "",
            "cicloActual", usuario.getCicloActual() != null ? usuario.getCicloActual() : 0
        ));
    }
}