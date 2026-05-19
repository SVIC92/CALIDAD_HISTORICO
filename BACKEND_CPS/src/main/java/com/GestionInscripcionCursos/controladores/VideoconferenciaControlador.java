package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.*;
import com.GestionInscripcionCursos.repositorios.VideoconferenciaRepositorio;
import com.GestionInscripcionCursos.servicios.VideoconferenciaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/videoconferencias")
public class VideoconferenciaControlador {

    @Autowired
    private VideoconferenciaServicio videoServicio;

    @Autowired
    private VideoconferenciaRepositorio videoRepo;

    @GetMapping("/publicas")
    public List<Videoconferencia> listarPublicasDisponibles() {
        return videoRepo.findSalasPublicasDisponibles();
    }

    // Aquí puedes capturar el usuario actual autenticado desde tu lógica de sesión o JWT
    @PostMapping("/crear")
    public ResponseEntity<Videoconferencia> crear(@RequestParam String titulo, @RequestParam int capacidad, @RequestParam boolean esPublica, @RequestBody Usuario creador) {
        return ResponseEntity.ok(videoServicio.crearSala(titulo, capacidad, esPublica, creador));
    }

    @PostMapping("/{salaUuid}/invitar")
    public ResponseEntity<String> invitar(@PathVariable String salaUuid, @RequestBody List<String> usuarioIds, @RequestParam com.GestionInscripcionCursos.enumeraciones.RolSala rol) {
        videoServicio.invitarUsuarios(salaUuid, usuarioIds, rol);
        return ResponseEntity.ok("Invitaciones enviadas correctamente");
    }
}