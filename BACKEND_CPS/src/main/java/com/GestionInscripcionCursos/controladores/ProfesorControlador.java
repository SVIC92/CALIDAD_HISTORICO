/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.ProfesorResumenDto;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesor")
public class ProfesorControlador {

    @Autowired
    private UsuarioServicio usuarioServicio;
    
    @GetMapping("/dashboard")
    public ResponseEntity<?> panelAdministrativo() {
        return ResponseEntity.ok(Map.of("mensaje", "Dashboard profesor"));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/lista")
    public ResponseEntity<List<ProfesorResumenDto>> listarProfesores() {
        return ResponseEntity.ok(usuarioServicio.listarProfesores());
    }
}
