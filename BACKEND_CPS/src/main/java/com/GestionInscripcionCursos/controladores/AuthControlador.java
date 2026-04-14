package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.seguridad.JwtUtil;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthControlador {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private JwtUtil jwtUtil;

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
    @GetMapping("/crear-prueba")
    public ResponseEntity<String> crearUsuarioPrueba() {
        try {
            // Crea o actualiza el usuario admin de prueba.
            usuarioServicio.crearOActualizarAdminPrueba("Admin Prueba", "admin@prueba.com", "123456");
            return ResponseEntity.ok("Usuario admin de prueba creado/actualizado con exito en NeonDB");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear: " + e.getMessage());
        }
    }
}