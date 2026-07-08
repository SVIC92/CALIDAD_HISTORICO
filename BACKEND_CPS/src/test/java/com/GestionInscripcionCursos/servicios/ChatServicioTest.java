package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Mensaje;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.MensajeRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ChatServicio}: validacion de permisos de chat entre
 * roles, guardado de mensajes (texto y archivo), historial, conteo de no leidos
 * y marcado de lectura.
 *
 * <p>El metodo {@code buscarGifs} usa un {@code HttpClient} creado internamente
 * (no inyectado por constructor), por lo que no es mockeable con Mockito puro y
 * requeriria una llamada de red real a Tenor. Siguiendo el mismo criterio usado
 * para los servicios de IA del proyecto, se omite probar el flujo de red completo
 * y en su lugar se cubren mediante reflexion los metodos privados de logica pura
 * (construccion de URL, filtro de formato y construccion de titulo) que si son
 * deterministas y no dependen de la red.
 */
@ExtendWith(MockitoExtension.class)
class ChatServicioTest {

    @Mock
    private MensajeRepositorio mensajeRepositorio;

    @InjectMocks
    private ChatServicio chatServicio;

    private Usuario usuario(String id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        u.setNombre("Usuario " + id);
        return u;
    }

    @SuppressWarnings("unchecked")
    private <T> T invocarPrivado(String metodo, Class<?>[] tipos, Object... args) throws Exception {
        Method m = ChatServicio.class.getDeclaredMethod(metodo, tipos);
        m.setAccessible(true);
        return (T) m.invoke(chatServicio, args);
    }

    // =====================================================================
    // guardarMensaje (incluye validarPermisoChat)
    // =====================================================================
    @Nested
    @DisplayName("guardarMensaje")
    class GuardarMensaje {

        @Test
        @DisplayName("entre PROFESOR y ADMIN esta permitido y guarda el mensaje")
        void profesorConAdminPermitido() throws MyException {
            Usuario profesor = usuario("p-1", Rol.PROFESOR);
            Usuario admin = usuario("a-1", Rol.ADMIN);
            when(mensajeRepositorio.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

            Mensaje guardado = chatServicio.guardarMensaje(profesor, admin, "Hola admin");

            assertEquals("Hola admin", guardado.getContenido());
            assertEquals(profesor, guardado.getEmisor());
            assertEquals(admin, guardado.getReceptor());
            verify(mensajeRepositorio).save(guardado);
        }

        @Test
        @DisplayName("entre ADMIN y PROFESOR (orden inverso) esta permitido")
        void adminConProfesorPermitido() {
            Usuario admin = usuario("a-1", Rol.ADMIN);
            Usuario profesor = usuario("p-1", Rol.PROFESOR);
            when(mensajeRepositorio.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> chatServicio.guardarMensaje(admin, profesor, "Hola profesor"));
        }

        @Test
        @DisplayName("entre ALUMNO y PROFESOR esta permitido")
        void alumnoConProfesorPermitido() {
            Usuario alumno = usuario("al-1", Rol.ALUMNO);
            Usuario profesor = usuario("p-1", Rol.PROFESOR);
            when(mensajeRepositorio.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> chatServicio.guardarMensaje(alumno, profesor, "Consulta"));
        }

        @Test
        @DisplayName("entre PROFESOR y ALUMNO (orden inverso) esta permitido")
        void profesorConAlumnoPermitido() {
            Usuario profesor = usuario("p-1", Rol.PROFESOR);
            Usuario alumno = usuario("al-1", Rol.ALUMNO);
            when(mensajeRepositorio.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> chatServicio.guardarMensaje(profesor, alumno, "Respuesta"));
        }

        @Test
        @DisplayName("entre ALUMNO y ALUMNO esta permitido")
        void alumnoConAlumnoPermitido() {
            Usuario alumno1 = usuario("al-1", Rol.ALUMNO);
            Usuario alumno2 = usuario("al-2", Rol.ALUMNO);
            when(mensajeRepositorio.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> chatServicio.guardarMensaje(alumno1, alumno2, "Hola compa"));
        }

        @Test
        @DisplayName("entre ADMIN y ADMIN esta permitido")
        void adminConAdminPermitido() {
            Usuario admin1 = usuario("a-1", Rol.ADMIN);
            Usuario admin2 = usuario("a-2", Rol.ADMIN);
            when(mensajeRepositorio.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> chatServicio.guardarMensaje(admin1, admin2, "Coordinacion"));
        }

        @Test
        @DisplayName("entre ALUMNO y ADMIN no esta permitido y lanza MyException")
        void alumnoConAdminNoPermitido() {
            Usuario alumno = usuario("al-1", Rol.ALUMNO);
            Usuario admin = usuario("a-1", Rol.ADMIN);

            MyException ex = assertThrows(MyException.class, () -> chatServicio.guardarMensaje(alumno, admin, "Hola"));
            assertTrue(ex.getMessage().contains("No tienes permiso"));
            verify(mensajeRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("entre ADMIN y ALUMNO (orden inverso) no esta permitido y lanza MyException")
        void adminConAlumnoNoPermitido() {
            Usuario admin = usuario("a-1", Rol.ADMIN);
            Usuario alumno = usuario("al-1", Rol.ALUMNO);

            assertThrows(MyException.class, () -> chatServicio.guardarMensaje(admin, alumno, "Hola"));
            verify(mensajeRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("entre PROFESOR y PROFESOR no esta permitido y lanza MyException")
        void profesorConProfesorNoPermitido() {
            Usuario profesor1 = usuario("p-1", Rol.PROFESOR);
            Usuario profesor2 = usuario("p-2", Rol.PROFESOR);

            assertThrows(MyException.class, () -> chatServicio.guardarMensaje(profesor1, profesor2, "Hola"));
            verify(mensajeRepositorio, never()).save(any());
        }
    }

    // =====================================================================
    // guardarMensajeArchivo
    // =====================================================================
    @Nested
    @DisplayName("guardarMensajeArchivo")
    class GuardarMensajeArchivo {

        @Test
        @DisplayName("con permiso valido guarda el mensaje con metadatos del archivo")
        void guardaArchivoConPermisoValido() throws MyException {
            Usuario alumno = usuario("al-1", Rol.ALUMNO);
            Usuario profesor = usuario("p-1", Rol.PROFESOR);
            when(mensajeRepositorio.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

            Mensaje guardado = chatServicio.guardarMensajeArchivo(
                    alumno, profesor, "https://cloudinary.com/archivo.pdf", "PDF", "tarea.pdf");

            assertEquals("Archivo adjunto: tarea.pdf", guardado.getContenido());
            assertEquals("PDF", guardado.getTipo());
            assertEquals("https://cloudinary.com/archivo.pdf", guardado.getUrlArchivo());
            verify(mensajeRepositorio).save(guardado);
        }

        @Test
        @DisplayName("sin permiso de chat lanza MyException y no guarda nada")
        void sinPermisoLanzaExcepcion() {
            Usuario alumno = usuario("al-1", Rol.ALUMNO);
            Usuario admin = usuario("a-1", Rol.ADMIN);

            assertThrows(MyException.class, () -> chatServicio.guardarMensajeArchivo(
                    alumno, admin, "url", "IMAGEN", "foto.png"));
            verify(mensajeRepositorio, never()).save(any());
        }
    }

    // =====================================================================
    // obtenerHistorial
    // =====================================================================
    @Nested
    @DisplayName("obtenerHistorial")
    class ObtenerHistorial {

        @Test
        @DisplayName("delega en el repositorio y retorna la lista de mensajes")
        void retornaHistorialDelRepositorio() {
            Mensaje m1 = new Mensaje();
            List<Mensaje> esperado = List.of(m1);
            when(mensajeRepositorio.obtenerHistorialChat("u1", "u2")).thenReturn(esperado);

            List<Mensaje> resultado = chatServicio.obtenerHistorial("u1", "u2");

            assertEquals(esperado, resultado);
            verify(mensajeRepositorio).obtenerHistorialChat("u1", "u2");
        }
    }

    // =====================================================================
    // obtenerConteoNoLeidos
    // =====================================================================
    @Nested
    @DisplayName("obtenerConteoNoLeidos")
    class ObtenerConteoNoLeidos {

        @Test
        @DisplayName("convierte las filas del repositorio en un mapa emisor->cantidad")
        void convierteFilasAMapa() {
            Object[] fila1 = new Object[]{"emisor-1", 3L};
            Object[] fila2 = new Object[]{"emisor-2", 5L};
            when(mensajeRepositorio.contarNoLeidosPorEmisor("receptor-1"))
                    .thenReturn(List.of(fila1, fila2));

            Map<String, Long> resultado = chatServicio.obtenerConteoNoLeidos("receptor-1");

            assertEquals(2, resultado.size());
            assertEquals(3L, resultado.get("emisor-1"));
            assertEquals(5L, resultado.get("emisor-2"));
        }

        @Test
        @DisplayName("sin mensajes no leidos retorna un mapa vacio")
        void sinFilasRetornaMapaVacio() {
            when(mensajeRepositorio.contarNoLeidosPorEmisor("receptor-1")).thenReturn(List.of());

            Map<String, Long> resultado = chatServicio.obtenerConteoNoLeidos("receptor-1");

            assertTrue(resultado.isEmpty());
        }
    }

    // =====================================================================
    // marcarMensajesLeidos
    // =====================================================================
    @Nested
    @DisplayName("marcarMensajesLeidos")
    class MarcarMensajesLeidos {

        @Test
        @DisplayName("delega en el repositorio con los ids de emisor y receptor")
        void delegaEnRepositorio() {
            chatServicio.marcarMensajesLeidos("emisor-1", "receptor-1");

            verify(mensajeRepositorio).marcarComoLeidos("emisor-1", "receptor-1");
        }
    }

    // =====================================================================
    // Metodos privados de soporte de buscarGifs (probados via reflexion,
    // no requieren red)
    // =====================================================================
    @Nested
    @DisplayName("logica interna de busqueda de GIFs (sin red)")
    class LogicaInternaGifs {

        @Test
        @DisplayName("construirUrlTenor normaliza el texto de busqueda a formato slug")
        void construirUrlTenorNormalizaTexto() throws Exception {
            String url = invocarPrivado("construirUrlTenor", new Class<?>[]{String.class}, "Gato Saltando!");
            assertEquals("https://tenor.com/search/gato-saltando-gifs", url);
        }

        @Test
        @DisplayName("construirUrlTenor con texto en blanco usa 'reactions' por defecto")
        void construirUrlTenorConBlancoUsaReactions() throws Exception {
            String url = invocarPrivado("construirUrlTenor", new Class<?>[]{String.class}, "   ");
            assertEquals("https://tenor.com/search/reactions-gifs", url);
        }

        @Test
        @DisplayName("esUrlGifCompatible acepta .gif y .webp")
        void esUrlGifCompatibleAceptaFormatosValidos() throws Exception {
            boolean gif = invocarPrivado("esUrlGifCompatible", new Class<?>[]{String.class},
                    "https://media.tenor.com/abc.GIF");
            boolean webp = invocarPrivado("esUrlGifCompatible", new Class<?>[]{String.class},
                    "https://media.tenor.com/abc.webp");
            boolean invalido = invocarPrivado("esUrlGifCompatible", new Class<?>[]{String.class},
                    "https://media.tenor.com/abc.mp4");

            assertTrue(gif);
            assertTrue(webp);
            assertFalse(invalido);
        }

        @Test
        @DisplayName("construirTituloDesdeUrl obtiene el nombre legible del archivo")
        void construirTituloDesdeUrlObtieneNombreLegible() throws Exception {
            String titulo = invocarPrivado("construirTituloDesdeUrl", new Class<?>[]{String.class},
                    "https://media.tenor.com/carpeta/gato-feliz_saltando.gif");
            assertEquals("gato feliz saltando", titulo);
        }

        @Test
        @DisplayName("construirTituloDesdeUrl con path vacio retorna 'GIF'")
        void construirTituloDesdeUrlConPathVacioRetornaGif() throws Exception {
            String titulo = invocarPrivado("construirTituloDesdeUrl", new Class<?>[]{String.class},
                    "https://media.tenor.com/");
            assertEquals("GIF", titulo);
        }

        @Test
        @DisplayName("construirTituloDesdeUrl con URL sin path (path vacio directo) retorna 'GIF'")
        void construirTituloDesdeUrlSinPathRetornaGifDirectamente() throws Exception {
            String titulo = invocarPrivado("construirTituloDesdeUrl", new Class<?>[]{String.class},
                    "https://media.tenor.com");
            assertEquals("GIF", titulo);
        }

        @Test
        @DisplayName("construirTituloDesdeUrl con url invalida retorna 'GIF'")
        void construirTituloDesdeUrlConUrlInvalidaRetornaGif() throws Exception {
            String titulo = invocarPrivado("construirTituloDesdeUrl", new Class<?>[]{String.class},
                    "no es una url valida ::: <>");
            assertEquals("GIF", titulo);
        }
    }
}
