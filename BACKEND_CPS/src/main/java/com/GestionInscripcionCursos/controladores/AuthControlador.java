package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.RecuperacionPasswordRequestDto;
import com.GestionInscripcionCursos.dto.ResetPasswordRequestDto;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.seguridad.JwtUtil;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.servicios.RecuperacionPasswordServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
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
        final UserDetails userDetails = usuarioServicio.loadUserByUsername(credenciales.get("email"));
        final String jwt = jwtUtil.generateToken(userDetails);

        // Preparamos la respuesta para React
        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("token", jwt);
        respuesta.put("rol", userDetails.getAuthorities().iterator().next().getAuthority());
        
        return ResponseEntity.ok(respuesta);
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