package com.GestionInscripcionCursos.controladores;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminControlador {

    @GetMapping("/dashboard")
    public ResponseEntity<?> panelAdministrativo() {
        return ResponseEntity.ok(Map.of("mensaje", "Dashboard admin"));
    }

}
