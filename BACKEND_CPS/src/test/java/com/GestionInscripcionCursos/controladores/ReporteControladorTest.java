package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.RendimientoAlumnoDto;
import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ActividadServicio;
import com.GestionInscripcionCursos.servicios.ArchivoServicio;
import com.GestionInscripcionCursos.servicios.ReporteServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ReporteControlador}: entrega y calificacion
 * (tradicional y por rubrica, RF-09) de reportes, detalle del alumno y
 * reporte de rendimiento academico por curso (RF-12).
 */
@ExtendWith(MockitoExtension.class)
class ReporteControladorTest {

    @Mock
    private ActividadServicio actividadServicio;

    @Mock
    private ReporteServicio reporteServicio;

    @Mock
    private UsuarioServicio usuarioServicio;

    @Mock
    private ArchivoServicio archivoServicio;

    @InjectMocks
    private ReporteControlador reporteControlador;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Usuario alumno(String id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private Actividad actividad(String id) {
        Actividad a = new Actividad();
        a.setId(id);
        return a;
    }

    private Reporte reporte(String id) {
        Reporte r = new Reporte();
        r.setId(id);
        return r;
    }

    // =====================================================================
    // registrar (GET) - validacion de limites antes de mostrar la actividad
    // =====================================================================
    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("dentro del limite de intentos devuelve la actividad")
        void dentroDeLimiteDevuelveActividad() {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            Usuario u = alumno("u-1", "alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(u);
            Actividad a = actividad("a-1");
            when(actividadServicio.buscarPorId("a-1")).thenReturn(a);

            ResponseEntity<?> respuesta = reporteControlador.registrar("a-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(a, respuesta.getBody());
        }

        @Test
        @DisplayName("al alcanzar el limite de intentos devuelve badRequest")
        void limiteAlcanzadoDevuelveBadRequest() throws MyException {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            Usuario u = alumno("u-1", "alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(u);
            Actividad a = actividad("a-1");
            when(actividadServicio.buscarPorId("a-1")).thenReturn(a);
            doThrow(new MyException("Has alcanzado el limite maximo de intentos"))
                    .when(reporteServicio).validarLimitesReporte("u-1", "a-1");

            ResponseEntity<?> respuesta = reporteControlador.registrar("a-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Has alcanzado el limite maximo de intentos"), respuesta.getBody());
        }
    }

    // =====================================================================
    // registro (POST) - entrega de la actividad, con o sin archivo
    // =====================================================================
    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("sin archivo adjunto registra el reporte y devuelve 201")
        void sinArchivoRegistraReporte() throws Exception {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(alumno("u-1", "alumno@dominio.com"));

            ResponseEntity<?> respuesta = reporteControlador.registro("a-1", "mi respuesta", null);

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            assertEquals(Map.of("mensaje", "Reporte registrado correctamente"), respuesta.getBody());
            verify(archivoServicio, never()).subirArchivo(any(), anyString());
            verify(reporteServicio).crearReporte("mi respuesta", "a-1", "u-1", null);
        }

        @Test
        @DisplayName("con archivo adjunto lo sube y registra el reporte con la url")
        void conArchivoSubeYRegistraReporte() throws Exception {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(alumno("u-1", "alumno@dominio.com"));
            MockMultipartFile archivo = new MockMultipartFile("archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
            when(archivoServicio.subirArchivo(archivo, "reportes_archivos")).thenReturn("https://cloudinary/informe.pdf");

            ResponseEntity<?> respuesta = reporteControlador.registro("a-1", "mi respuesta", archivo);

            assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
            verify(reporteServicio).crearReporte("mi respuesta", "a-1", "u-1", "https://cloudinary/informe.pdf");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void servicioLanzaMyExceptionDevuelveBadRequest() throws Exception {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(alumno("u-1", "alumno@dominio.com"));
            doThrow(new MyException("La respuesta no puede ser nulo o estar vacio"))
                    .when(reporteServicio).crearReporte(anyString(), anyString(), anyString(), any());

            ResponseEntity<?> respuesta = reporteControlador.registro("a-1", "mi respuesta", null);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "La respuesta no puede ser nulo o estar vacio"), respuesta.getBody());
        }

        @Test
        @DisplayName("cuando falla la subida del archivo devuelve badRequest")
        void fallaSubidaDeArchivoDevuelveBadRequest() throws Exception {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(alumno("u-1", "alumno@dominio.com"));
            MockMultipartFile archivo = new MockMultipartFile("archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
            when(archivoServicio.subirArchivo(any(), anyString())).thenThrow(new IOException("Error al subir a Cloudinary"));

            ResponseEntity<?> respuesta = reporteControlador.registro("a-1", "mi respuesta", archivo);

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Error al subir a Cloudinary"), respuesta.getBody());
            verify(reporteServicio, never()).crearReporte(anyString(), anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("devuelve los reportes de la actividad")
        void devuelveReportes() {
            when(reporteServicio.listarReportesPorIdActividad("a-1")).thenReturn(List.of(reporte("r-1")));

            ResponseEntity<List<Reporte>> respuesta = reporteControlador.listar("a-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(1, respuesta.getBody().size());
        }
    }

    @Nested
    @DisplayName("calificar")
    class Calificar {

        @Test
        @DisplayName("GET devuelve el reporte buscado por id")
        void obtenerReporteParaCalificar() {
            Reporte r = reporte("r-1");
            when(reporteServicio.buscarPorId("r-1")).thenReturn(r);

            ResponseEntity<Reporte> respuesta = reporteControlador.calificar("r-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(r, respuesta.getBody());
        }

        @Test
        @DisplayName("POST con datos validos califica y devuelve el reporte actualizado")
        void calificaCorrectamente() throws MyException {
            Reporte calificado = reporte("r-1");
            when(reporteServicio.buscarPorId("r-1")).thenReturn(calificado);

            ResponseEntity<?> respuesta = reporteControlador.calificar("r-1", "18", "Buen trabajo");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(calificado, respuesta.getBody());
            verify(reporteServicio).calificarReporte("r-1", "18", "Buen trabajo");
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void calificarConExcepcionDevuelveBadRequest() throws MyException {
            doThrow(new MyException("La nota no puede ser nulo o estar vacio"))
                    .when(reporteServicio).calificarReporte("r-1", "", "comentario");

            ResponseEntity<?> respuesta = reporteControlador.calificar("r-1", "", "comentario");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "La nota no puede ser nulo o estar vacio"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("verDetalle")
    class VerDetalle {

        @Test
        @DisplayName("devuelve todos los intentos del alumno para la actividad")
        void devuelveIntentosDelAlumno() {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenReturn(alumno("u-1", "alumno@dominio.com"));
            when(reporteServicio.listarReportesAlumno("u-1", "a-1")).thenReturn(List.of(reporte("r-1"), reporte("r-2")));

            ResponseEntity<?> respuesta = reporteControlador.verDetalle("a-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(2, ((List<?>) respuesta.getBody()).size());
        }

        @Test
        @DisplayName("ante un error inesperado devuelve badRequest")
        void errorInesperadoDevuelveBadRequest() {
            when(authentication.getName()).thenReturn("alumno@dominio.com");
            when(usuarioServicio.buscarEmail("alumno@dominio.com")).thenThrow(new RuntimeException("fallo inesperado"));

            ResponseEntity<?> respuesta = reporteControlador.verDetalle("a-1");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "fallo inesperado"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("calificarConRubrica")
    class CalificarConRubrica {

        @Test
        @DisplayName("con puntajes validos devuelve el reporte calificado")
        void calificaConRubricaCorrectamente() throws MyException {
            Reporte calificado = reporte("r-1");
            Map<String, Integer> puntajes = Map.of("crit-1", 5);
            when(reporteServicio.calificarConRubrica("r-1", puntajes, "Buen trabajo")).thenReturn(calificado);

            ResponseEntity<Object> respuesta = reporteControlador.calificarConRubrica("r-1", puntajes, "Buen trabajo");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(calificado, respuesta.getBody());
        }

        @Test
        @DisplayName("cuando el servicio lanza MyException devuelve badRequest")
        void calificarConRubricaConExcepcionDevuelveBadRequest() throws MyException {
            when(reporteServicio.calificarConRubrica(anyString(), anyMap(), anyString()))
                    .thenThrow(new MyException("La actividad de este reporte no tiene una rubrica asignada"));

            ResponseEntity<Object> respuesta = reporteControlador.calificarConRubrica("r-1", Map.of("crit-1", 5), "");

            assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
            assertEquals(Map.of("error", "La actividad de este reporte no tiene una rubrica asignada"), respuesta.getBody());
        }
    }

    @Nested
    @DisplayName("rendimientoPorCurso")
    class RendimientoPorCurso {

        @Test
        @DisplayName("devuelve el rendimiento de los alumnos del curso")
        void devuelveRendimiento() throws MyException {
            RendimientoAlumnoDto dto = new RendimientoAlumnoDto("u-1", "Ana", "ana@dominio.com", 5, 3, 2, 0, 3, 15.0);
            when(reporteServicio.calcularRendimientoCurso("c-1")).thenReturn(List.of(dto));

            ResponseEntity<Object> respuesta = reporteControlador.rendimientoPorCurso("c-1");

            assertEquals(HttpStatus.OK, respuesta.getStatusCode());
            assertEquals(List.of(dto), respuesta.getBody());
        }

        @Test
        @DisplayName("cuando el curso no existe devuelve NOT_FOUND")
        void cursoInexistenteDevuelveNotFound() throws MyException {
            when(reporteServicio.calcularRendimientoCurso("c-inexistente"))
                    .thenThrow(new MyException("Curso no encontrado"));

            ResponseEntity<Object> respuesta = reporteControlador.rendimientoPorCurso("c-inexistente");

            assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
            assertEquals(Map.of("error", "Curso no encontrado"), respuesta.getBody());
        }
    }
}
