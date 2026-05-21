package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.MensajeRequestDto; 
import com.GestionInscripcionCursos.dto.GifResultadoDto;
import com.GestionInscripcionCursos.entidades.Mensaje;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ArchivoServicio;
import com.GestionInscripcionCursos.servicios.ChatServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatControlador {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatServicio chatServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    // --- Endpoint REST para obtener el historial ---
    @GetMapping("/historial/{idReceptor}")
    public ResponseEntity<List<Mensaje>> obtenerHistorial(@PathVariable String idReceptor, Authentication authentication) {
        Usuario emisor = usuarioServicio.buscarEmail(authentication.getName());
        return ResponseEntity.ok(chatServicio.obtenerHistorial(emisor.getId(), idReceptor));
    }

    // --- Endpoint WebSocket para procesar y enviar ---
    @MessageMapping("/enviar")
    public void enviarMensaje(@Payload MensajeRequestDto mensajeDto, Principal principal) {
        try {
            Usuario emisor = usuarioServicio.buscarEmail(principal.getName());
            
            // CORRECCIÓN AQUÍ: usamos .idReceptor() en lugar de .getIdReceptor()
            Usuario receptor = usuarioServicio.buscarPorId(mensajeDto.idReceptor());

            // CORRECCIÓN AQUÍ: usamos .contenido() en lugar de .getContenido()
            Mensaje mensajeGuardado = chatServicio.guardarMensaje(emisor, receptor, mensajeDto.contenido());

            // Enviar al canal privado del receptor
            messagingTemplate.convertAndSend(
            "/queue/mensajes/" + receptor.getId(),
            mensajeGuardado
    );
        } catch (Exception e) {
            // Manejar error (puedes enviar un mensaje de error de vuelta al emisor)
            System.err.println("Error enviando mensaje: " + e.getMessage());
        }
    }
    @Autowired
    private ArchivoServicio archivoServicio;

    // --- Endpoint REST para subir y enviar archivos ---
    @PostMapping("/enviar-archivo")
    public ResponseEntity<?> enviarArchivo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("idReceptor") String idReceptor,
            @RequestParam("tipo") String tipo,
            Authentication authentication) {
        try {
            String emailUser = authentication.getName();
            Usuario emisor = usuarioServicio.buscarEmail(emailUser);
            Usuario receptor = usuarioServicio.buscarPorId(idReceptor);

            // Especificar la carpeta "chat_archivos" aquí
            String urlArchivo = archivoServicio.subirArchivo(archivo, "chat_archivos");

            Mensaje mensajeGuardado = chatServicio.guardarMensajeArchivo(emisor, receptor, urlArchivo, tipo, archivo.getOriginalFilename());
            return ResponseEntity.ok(mensajeGuardado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/no-leidos")
    public ResponseEntity<Map<String, Long>> obtenerNoLeidos(Authentication authentication) {
        Usuario receptor = usuarioServicio.buscarEmail(authentication.getName());
        return ResponseEntity.ok(chatServicio.obtenerConteoNoLeidos(receptor.getId()));
    }

    @PutMapping("/marcar-leidos/{idEmisor}")
    public ResponseEntity<?> marcarLeidos(@PathVariable String idEmisor, Authentication authentication) {
        Usuario receptor = usuarioServicio.buscarEmail(authentication.getName());
        chatServicio.marcarMensajesLeidos(idEmisor, receptor.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/gifs")
    public ResponseEntity<?> buscarGifs(@RequestParam(defaultValue = "") String query) {
        try {
            return ResponseEntity.ok(chatServicio.buscarGifs(query));
        } catch (MyException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}