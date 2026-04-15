package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.RecuperacionPasswordRequestDto;
import com.GestionInscripcionCursos.dto.ResetPasswordRequestDto;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.seguridad.JwtUtil;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.servicios.RecuperacionPasswordServicio;
import com.GestionInscripcionCursos.servicios.TwoFactorServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RecuperacionPasswordServicio recuperacionPasswordServicio;

    @Autowired
    private TwoFactorServicio twoFactorServicio;

    @PostMapping("/login")
    public ResponseEntity<?> crearTokenAutenticacion(@RequestBody Map<String, String> credenciales) throws Exception {
        try {
            // Verificamos email y contraseña
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(credenciales.get("email"), credenciales.get("password"))
            );
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(401).body("Credenciales incorrectas: " + e.getMessage());
        }

        // Si es correcto, generamos el token
        Usuario usuario = usuarioServicio.buscarEmail(credenciales.get("email"));
        if (usuario != null && usuario.isTwoFactorEnabled()) {
            String otp = credenciales.get("otp");
            boolean otpValido = twoFactorServicio.validarCodigo(usuario.getTwoFactorSecret(), otp);
            if (!otpValido) {
                return ResponseEntity.status(401).body(Map.of(
                        "twoFactorRequired", true,
                        "mensaje", "Codigo de autenticacion invalido o ausente"
                ));
            }
        }

        final UserDetails userDetails = usuarioServicio.loadUserByUsername(credenciales.get("email"));
        final String jwt = jwtUtil.generateToken(userDetails);

        // Preparamos la respuesta para React
        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("token", jwt);
        respuesta.put("rol", userDetails.getAuthorities().iterator().next().getAuthority());
        respuesta.put("twoFactorEnabled", String.valueOf(usuario != null && usuario.isTwoFactorEnabled()));
        
        return ResponseEntity.ok(respuesta);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/2fa/status")
    public ResponseEntity<?> twoFactorStatus(Authentication authentication) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }
        return ResponseEntity.ok(Map.of("enabled", usuario.isTwoFactorEnabled()));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setupTwoFactor(Authentication authentication) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
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
                "enabled", usuario.isTwoFactorEnabled()
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/2fa/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> twoFactorQr(Authentication authentication,
            @RequestParam(defaultValue = "280") int size) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
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
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo generar el QR"));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(qr);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enableTwoFactor(Authentication authentication, @RequestBody Map<String, String> body) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }

        String secret = usuario.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Primero debes generar la configuracion 2FA"));
        }

        String codigo = body != null ? body.get("code") : null;
        if (!twoFactorServicio.validarCodigo(secret, codigo)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Codigo de autenticacion invalido"));
        }

        usuario.setTwoFactorEnabled(true);
        usuarioServicio.guardar(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Two factor activado correctamente", "enabled", true));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disableTwoFactor(Authentication authentication, @RequestBody Map<String, String> body) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }

        if (usuario.isTwoFactorEnabled()) {
            String codigo = body != null ? body.get("code") : null;
            if (!twoFactorServicio.validarCodigo(usuario.getTwoFactorSecret(), codigo)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Codigo de autenticacion invalido"));
            }
        }

        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret(null);
        usuarioServicio.guardar(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Two factor desactivado correctamente", "enabled", false));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> solicitarRecuperacion(@RequestBody RecuperacionPasswordRequestDto request) {
        try {
            recuperacionPasswordServicio.solicitarRecuperacion(request != null ? request.getEmail() : null);
            return ResponseEntity.ok(Map.of(
                    "mensaje",
                    "Si el correo existe en el sistema, recibiras un enlace para reestablecer tu contraseña"
            ));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        try {
            recuperacionPasswordServicio.reestablecerPassword(
                    request.getToken(),
                    request.getPassword(),
                    request.getPassword2()
            );
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> perfilActual(Authentication authentication) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("mensaje", "Usuario no encontrado"));
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
                "rol", rol != null ? rol : "SIN_ROL"
        ));
    }
}