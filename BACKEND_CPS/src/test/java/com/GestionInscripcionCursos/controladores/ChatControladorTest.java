package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.GifResultadoDto;
import com.GestionInscripcionCursos.dto.MensajeRequestDto;
import com.GestionInscripcionCursos.entidades.Mensaje;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ArchivoServicio;
import com.GestionInscripcionCursos.servicios.ChatServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ChatControlador}: historial de chat, envio de
 * mensajes por websocket, envio de archivos, contador de no leidos y busqueda
 * de gifs.
 */
@ExtendWith(MockitoExtension.class)
class ChatControladorTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChatServicio chatServicio;

    @Mock
    private UsuarioServicio usuarioServicio;

    @Mock
    private ArchivoServicio archivoServicio;

    @InjectMocks
    private ChatControlador chatControlador;

    private Usuario usuario(String id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre("Usuario " + id);
        return u;
    }

    @Nested
    @DisplayName("obtenerHistorial")
    class ObtenerHistorial {

        @Test
        @DisplayName("responde 200 con el historial entre el emisor autenticado y el receptor")
        void devuelveHistorial() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("emisor@test.com");
            Usuario emisor = usuario("id-emisor", "emisor@test.com");
            when(usuarioServicio.buscarEmail("emisor@test.com")).thenReturn(emisor);
            List<Mensaje> historial = List.of(new Mensaje());
            when(chatServicio.obtenerHistorial("id-emisor", "id-receptor")).thenReturn(historial);

            ResponseEntity<List<Mensaje>> resultado = chatControlador.obtenerHistorial("id-receptor", auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(historial, resultado.getBody());
        }
    }

    @Nested
    @DisplayName("enviarMensaje (websocket)")
    class EnviarMensaje {

        @Test
        @DisplayName("guarda el mensaje y lo envia al canal privado del receptor")
        void envioExitoso() throws MyException {
            Principal principal = mock(Principal.class);
            when(principal.getName()).thenReturn("emisor@test.com");
            Usuario emisor = usuario("id-emisor", "emisor@test.com");
            Usuario receptor = usuario("id-receptor", "receptor@test.com");
            when(usuarioServicio.buscarEmail("emisor@test.com")).thenReturn(emisor);
            when(usuarioServicio.buscarPorId("id-receptor")).thenReturn(receptor);
            Mensaje guardado = new Mensaje();
            when(chatServicio.guardarMensaje(emisor, receptor, "Hola")).thenReturn(guardado);
            MensajeRequestDto dto = new MensajeRequestDto("id-receptor", "Hola");

            chatControlador.enviarMensaje(dto, principal);

            verify(messagingTemplate).convertAndSend("/queue/mensajes/id-receptor", guardado);
        }

        @Test
        @DisplayName("si ocurre un error no propaga la excepcion y no envia el mensaje")
        void errorNoPropagaExcepcion() {
            Principal principal = mock(Principal.class);
            when(principal.getName()).thenReturn("emisor@test.com");
            when(usuarioServicio.buscarEmail("emisor@test.com")).thenThrow(new RuntimeException("usuario no encontrado"));
            MensajeRequestDto dto = new MensajeRequestDto("id-receptor", "Hola");

            assertDoesNotThrow(() -> chatControlador.enviarMensaje(dto, principal));

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("enviarArchivo")
    class EnviarArchivo {

        @Test
        @DisplayName("sube el archivo, guarda el mensaje y responde 200")
        void envioExitoso() throws Exception {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("emisor@test.com");
            Usuario emisor = usuario("id-emisor", "emisor@test.com");
            Usuario receptor = usuario("id-receptor", "receptor@test.com");
            when(usuarioServicio.buscarEmail("emisor@test.com")).thenReturn(emisor);
            when(usuarioServicio.buscarPorId("id-receptor")).thenReturn(receptor);
            MultipartFile archivo = mock(MultipartFile.class);
            when(archivo.getOriginalFilename()).thenReturn("foto.png");
            when(archivoServicio.subirArchivo(archivo, "chat_archivos")).thenReturn("http://cloud/foto.png");
            Mensaje guardado = new Mensaje();
            when(chatServicio.guardarMensajeArchivo(emisor, receptor, "http://cloud/foto.png", "IMAGEN", "foto.png"))
                    .thenReturn(guardado);

            ResponseEntity<?> resultado = chatControlador.enviarArchivo(archivo, "id-receptor", "IMAGEN", auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(guardado, resultado.getBody());
        }

        @Test
        @DisplayName("si ocurre un error responde 400 con el mensaje de error")
        void errorResponde400() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("emisor@test.com");
            when(usuarioServicio.buscarEmail("emisor@test.com")).thenThrow(new RuntimeException("fallo de subida"));
            MultipartFile archivo = mock(MultipartFile.class);

            ResponseEntity<?> resultado = chatControlador.enviarArchivo(archivo, "id-receptor", "IMAGEN", auth);

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
            assertEquals(Map.of("error", "fallo de subida"), resultado.getBody());
        }
    }

    @Nested
    @DisplayName("obtenerNoLeidos")
    class ObtenerNoLeidos {

        @Test
        @DisplayName("responde 200 con el conteo de mensajes no leidos")
        void devuelveConteo() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("receptor@test.com");
            Usuario receptor = usuario("id-receptor", "receptor@test.com");
            when(usuarioServicio.buscarEmail("receptor@test.com")).thenReturn(receptor);
            Map<String, Long> conteo = Map.of("id-emisor", 3L);
            when(chatServicio.obtenerConteoNoLeidos("id-receptor")).thenReturn(conteo);

            ResponseEntity<Map<String, Long>> resultado = chatControlador.obtenerNoLeidos(auth);

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(conteo, resultado.getBody());
        }
    }

    @Nested
    @DisplayName("marcarLeidos")
    class MarcarLeidos {

        @Test
        @DisplayName("marca los mensajes como leidos y responde 200")
        void marcaLeidosOk() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("receptor@test.com");
            Usuario receptor = usuario("id-receptor", "receptor@test.com");
            when(usuarioServicio.buscarEmail("receptor@test.com")).thenReturn(receptor);

            ResponseEntity<?> resultado = chatControlador.marcarLeidos("id-emisor", auth);

            verify(chatServicio).marcarMensajesLeidos("id-emisor", "id-receptor");
            assertEquals(HttpStatus.OK, resultado.getStatusCode());
        }
    }

    @Nested
    @DisplayName("buscarGifs")
    class BuscarGifs {

        @Test
        @DisplayName("responde 200 con los resultados de la busqueda")
        void buscaGifsOk() throws MyException {
            List<GifResultadoDto> resultados = List.of(new GifResultadoDto("1", "titulo", "url", "preview"));
            when(chatServicio.buscarGifs("gato")).thenReturn(resultados);

            ResponseEntity<?> resultado = chatControlador.buscarGifs("gato");

            assertEquals(HttpStatus.OK, resultado.getStatusCode());
            assertEquals(resultados, resultado.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException responde 400")
        void errorServicioResponde400() throws MyException {
            when(chatServicio.buscarGifs("gato")).thenThrow(new MyException("API de gifs no disponible"));

            ResponseEntity<?> resultado = chatControlador.buscarGifs("gato");

            assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatusCode());
            assertEquals(Map.of("error", "API de gifs no disponible"), resultado.getBody());
        }
    }
}
