package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.IaChatResponseDto;
import com.GestionInscripcionCursos.dto.IaConversacionDto;
import com.GestionInscripcionCursos.dto.IaHistorialDto;
import com.GestionInscripcionCursos.dto.IaMensajeDto;
import com.GestionInscripcionCursos.dto.IaSugerenciasDto;
import com.GestionInscripcionCursos.dto.CriterioRubricaDto;
import com.GestionInscripcionCursos.dto.RubricaGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.dto.SilaboGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.SilaboGeneradoDto;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.IaHistorial;
import com.GestionInscripcionCursos.entidades.Silabo;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.IaHistorialRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.SilaboRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link IaServicio}: chat con fallback local, historial de
 * conversacion, sugerencias por rol, generacion de rubricas (Cohere + fallback) y
 * generacion de silabos (Cohere + fallback).
 *
 * <p>El servicio se construye siempre con {@code groqApiKey = ""} para forzar la rama de
 * fallback local en {@code chatearSegunRol} y evitar cualquier llamada real de red a Groq
 * (el {@code HttpClient} interno no es inyectable y por tanto no se puede mockear).</p>
 */
@ExtendWith(MockitoExtension.class)
class IaServicioTest {

    @Mock
    private UsuarioServicio usuarioServicio;

    @Mock
    private IaHistorialRepositorio iaHistorialRepositorio;

    @Mock
    private CursoRepositorio cursoRepositorio;

    @Mock
    private InscripcionRepositorio inscripcionRepositorio;

    @Mock
    private SilaboRepositorio silaboRepositorio;

    @Mock
    private CohereServicio cohereServicio;

    private ObjectMapper objectMapper;
    private IaServicio iaServicio;

    private static final String RUBRICA_JSON_VALIDA = """
            {"titulo":"Rubrica de Ensayo","descripcion":"Evalua un ensayo argumentativo",
            "tema":"Cambio climatico","nivelEducativo":"Universitario","asignatura":"Ciencias",
            "tipoTarea":"Ensayo","puntajeMaximo":20,"criterios":[
            {"nombre":"Contenido","descripcion":"Calidad del contenido","peso":50,
            "niveles":[{"nombre":"Bajo","puntaje":5,"descriptor":"Insuficiente"},
            {"nombre":"Alto","puntaje":15,"descriptor":"Sobresaliente"}]},
            {"nombre":"Redaccion","descripcion":"Calidad de la redaccion","peso":50,
            "niveles":[{"nombre":"Bajo","puntaje":5,"descriptor":"Insuficiente"},
            {"nombre":"Alto","puntaje":15,"descriptor":"Sobresaliente"}]}]}
            """;

    private static final String SILABO_JSON_VALIDO = """
            {"informacionGeneral":{"curso":"Base de Datos II","carrera":"Ing. de Sistemas","ciclo":5,"creditos":4},
            "competenciasGenerales":["Analiza sistemas de informacion"],
            "competenciasEspecificas":["Disena bases de datos relacionales"],
            "sumilla":"Curso de bases de datos avanzadas",
            "logroCurso":"El estudiante disena bases de datos normalizadas",
            "unidades":[{"tituloUnidad":"Unidad 1","logroUnidad":"Introduccion",
            "semanas":[{"numeroSemana":1,"temas":"Introduccion","actividadesPracticas":"Laboratorio","evaluacion":"Ninguna"}]}],
            "sistemaEvaluacion":"Evaluacion continua y examenes parciales"}
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        iaServicio = new IaServicio(
                usuarioServicio,
                iaHistorialRepositorio,
                cursoRepositorio,
                inscripcionRepositorio,
                silaboRepositorio,
                objectMapper,
                "",
                "llama-3.1-8b-instant",
                cohereServicio);
    }

    private Usuario usuario(String id, Rol rol, String nombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        u.setNombre(nombre);
        u.setEmail(id == null ? "sin-id@test.com" : id + "@test.com");
        return u;
    }

    private Curso curso(String id, String nombre, String codigo) {
        Curso c = new Curso();
        c.setId(id);
        c.setNombre(nombre);
        c.setCodigoCurso(codigo);
        return c;
    }

    private String jsonArrayDeMensajes(int cantidad) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cantidad; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"role\":\"user\",\"content\":\"msg").append(i).append("\"}");
        }
        return sb.append("]").toString();
    }

    // =====================================================================
    // chatearSegunRol
    // =====================================================================
    @Nested
    @DisplayName("chatearSegunRol")
    class ChatearSegunRol {

        @Test
        @DisplayName("con mensaje nulo lanza excepcion")
        void mensajeNuloLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class,
                    () -> iaServicio.chatearSegunRol("a@test.com", "ADMIN", null));
        }

        @Test
        @DisplayName("con mensaje en blanco lanza excepcion")
        void mensajeEnBlancoLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class,
                    () -> iaServicio.chatearSegunRol("a@test.com", "ADMIN", "   "));
        }

        @Test
        @DisplayName("con mensaje que excede el maximo permitido lanza excepcion")
        void mensajeExcesivoLanzaExcepcion() {
            String mensajeLargo = "a".repeat(4001);
            assertThrows(IllegalArgumentException.class,
                    () -> iaServicio.chatearSegunRol("a@test.com", "ADMIN", mensajeLargo));
        }

        @Test
        @DisplayName("con usuario inexistente lanza excepcion")
        void usuarioInexistenteLanzaExcepcion() {
            when(usuarioServicio.buscarEmail("noexiste@test.com")).thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> iaServicio.chatearSegunRol("noexiste@test.com", "ADMIN", "Hola"));
        }

        @Test
        @DisplayName("sin historial previo crea uno nuevo y responde con fallback local para ADMIN")
        void sinHistorialPrevioAdmin() {
            Usuario admin = usuario("admin-1", Rol.ADMIN, "Ana Admin");
            when(usuarioServicio.buscarEmail("admin@test.com")).thenReturn(admin);
            when(iaHistorialRepositorio.findByUsuario(admin)).thenReturn(Optional.empty());
            when(cursoRepositorio.buscarCursosActivos(any(Date.class))).thenReturn(List.of());

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("admin@test.com", "ADMIN", "Hola IA");

            assertEquals("ADMIN", respuesta.rol());
            assertEquals("fallback-local", respuesta.modelo());
            assertTrue(respuesta.respuesta().contains("no esta disponible"));
            assertTrue(respuesta.respuesta().contains("Hola IA"));

            ArgumentCaptor<IaHistorial> captor = ArgumentCaptor.forClass(IaHistorial.class);
            verify(iaHistorialRepositorio).save(captor.capture());
            IaHistorial guardado = captor.getValue();
            assertEquals(admin, guardado.getUsuario());
            assertEquals("Hola IA", guardado.getUltimoMensaje());
            assertNotNull(guardado.getHistorialConversacion());
        }

        @Test
        @DisplayName("con historial previo valido en JSON agrega los nuevos mensajes para PROFESOR")
        void conHistorialPrevioValidoProfesor() throws Exception {
            Usuario profesor = usuario("prof-1", Rol.PROFESOR, "Pedro Profesor");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion(jsonArrayDeMensajes(1));

            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(profesor);
            when(iaHistorialRepositorio.findByUsuario(profesor)).thenReturn(Optional.of(historial));
            when(cursoRepositorio.buscarCursosInscritosProfesor("prof-1")).thenReturn(List.of());
            when(cursoRepositorio.buscarCursosDisponiblesProfesor("prof-1")).thenReturn(List.of());

            iaServicio.chatearSegunRol("prof@test.com", "PROFESOR", "Como creo una rubrica?");

            ArgumentCaptor<IaHistorial> captor = ArgumentCaptor.forClass(IaHistorial.class);
            verify(iaHistorialRepositorio).save(captor.capture());
            JsonNode array = objectMapper.readTree(captor.getValue().getHistorialConversacion());
            assertEquals(3, array.size());
        }

        @Test
        @DisplayName("con historial previo con JSON invalido descarta el historial corrupto para ALUMNO")
        void conHistorialPrevioInvalidoAlumno() throws Exception {
            Usuario alumno = usuario("alu-1", Rol.ALUMNO, "Alicia Alumna");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion("esto-no-es-json-valido");

            when(usuarioServicio.buscarEmail("alu@test.com")).thenReturn(alumno);
            when(iaHistorialRepositorio.findByUsuario(alumno)).thenReturn(Optional.of(historial));
            when(cursoRepositorio.buscarCursosInscritosAlumno("alu-1")).thenReturn(List.of());
            when(cursoRepositorio.buscarCursosDisponiblesAlumno("alu-1")).thenReturn(List.of());

            iaServicio.chatearSegunRol("alu@test.com", "ALUMNO", "En que cursos estoy inscrito?");

            ArgumentCaptor<IaHistorial> captor = ArgumentCaptor.forClass(IaHistorial.class);
            verify(iaHistorialRepositorio).save(captor.capture());
            JsonNode array = objectMapper.readTree(captor.getValue().getHistorialConversacion());
            assertEquals(2, array.size());
        }

        @Test
        @DisplayName("con historial previo cuyo JSON no es un arreglo lo descarta")
        void conHistorialPrevioNoEsArreglo() throws Exception {
            Usuario alumno = usuario("alu-2", Rol.ALUMNO, "Beto Alumno");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion("{\"foo\":1}");

            when(usuarioServicio.buscarEmail("alu2@test.com")).thenReturn(alumno);
            when(iaHistorialRepositorio.findByUsuario(alumno)).thenReturn(Optional.of(historial));
            when(cursoRepositorio.buscarCursosInscritosAlumno("alu-2")).thenReturn(List.of());
            when(cursoRepositorio.buscarCursosDisponiblesAlumno("alu-2")).thenReturn(List.of());

            iaServicio.chatearSegunRol("alu2@test.com", "ALUMNO", "Hola");

            ArgumentCaptor<IaHistorial> captor = ArgumentCaptor.forClass(IaHistorial.class);
            verify(iaHistorialRepositorio).save(captor.capture());
            JsonNode array = objectMapper.readTree(captor.getValue().getHistorialConversacion());
            assertEquals(2, array.size());
        }

        @Test
        @DisplayName("cuando el historial supera el maximo permitido lo recorta a 50 mensajes")
        void historialSuperaMaximoSeRecorta() throws Exception {
            Usuario alumno = usuario("alu-3", Rol.ALUMNO, "Carla Alumna");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion(jsonArrayDeMensajes(50));

            when(usuarioServicio.buscarEmail("alu3@test.com")).thenReturn(alumno);
            when(iaHistorialRepositorio.findByUsuario(alumno)).thenReturn(Optional.of(historial));
            when(cursoRepositorio.buscarCursosInscritosAlumno("alu-3")).thenReturn(List.of());
            when(cursoRepositorio.buscarCursosDisponiblesAlumno("alu-3")).thenReturn(List.of());

            iaServicio.chatearSegunRol("alu3@test.com", "ALUMNO", "Mensaje nuevo");

            ArgumentCaptor<IaHistorial> captor = ArgumentCaptor.forClass(IaHistorial.class);
            verify(iaHistorialRepositorio).save(captor.capture());
            JsonNode array = objectMapper.readTree(captor.getValue().getHistorialConversacion());
            assertEquals(50, array.size());
            // El mensaje mas antiguo (msg0) debe haber sido descartado.
            assertNotEquals("msg0", array.get(0).path("content").asText());
        }

        @Test
        @DisplayName("con rol no reconocido responde con orientacion general")
        void rolNoReconocido() {
            Usuario usuarioSinRolConocido = usuario("u-1", null, "Usuario Generico");
            when(usuarioServicio.buscarEmail("otro@test.com")).thenReturn(usuarioSinRolConocido);
            when(iaHistorialRepositorio.findByUsuario(usuarioSinRolConocido)).thenReturn(Optional.empty());

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("otro@test.com", "OTRO", "Hola");

            assertTrue(respuesta.respuesta().contains("rol no reconocido"));
        }

        @Test
        @DisplayName("con usuario sin id no puede resolver el contexto de datos")
        void usuarioSinIdNoResuelveContexto() {
            Usuario sinId = usuario(null, Rol.ADMIN, "Sin Id");
            when(usuarioServicio.buscarEmail("sinid@test.com")).thenReturn(sinId);
            when(iaHistorialRepositorio.findByUsuario(sinId)).thenReturn(Optional.empty());

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("sinid@test.com", "ADMIN", "Hola");

            assertTrue(respuesta.respuesta().contains("no se pudo resolver el usuario autenticado"));
        }

        @Test
        @DisplayName("cuando falla la consulta de contexto de datos responde con mensaje de error controlado")
        void errorConstruyendoContextoDatos() {
            Usuario admin = usuario("admin-2", Rol.ADMIN, "Admin Con Error");
            when(usuarioServicio.buscarEmail("admin2@test.com")).thenReturn(admin);
            when(iaHistorialRepositorio.findByUsuario(admin)).thenReturn(Optional.empty());
            when(cursoRepositorio.buscarCursosActivos(any(Date.class))).thenThrow(new RuntimeException("fallo bd"));

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("admin2@test.com", "ADMIN", "Hola");

            assertTrue(respuesta.respuesta().contains("no disponible temporalmente"));
        }

        @Test
        @DisplayName("resume la lista de cursos activos truncando y usando valores por defecto")
        void resumenDeCursosActivosAdmin() {
            Usuario admin = usuario("admin-3", Rol.ADMIN, "Admin Resumen");
            when(usuarioServicio.buscarEmail("admin3@test.com")).thenReturn(admin);
            when(iaHistorialRepositorio.findByUsuario(admin)).thenReturn(Optional.empty());

            List<Curso> cursos = Arrays.asList(
                    null,
                    curso("c-1", "Base de Datos", "BD1"),
                    curso("c-2", "Programacion", "PR1"),
                    curso("c-3", "Redes", "RD1"),
                    curso("c-4", "Algoritmos", "AL1"),
                    curso("c-5", null, null),
                    curso("c-6", "Extra1", "EX1"),
                    curso("c-7", "Extra2", "EX2"));
            when(cursoRepositorio.buscarCursosActivos(any(Date.class))).thenReturn(cursos);

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("admin3@test.com", "ADMIN", "Cuantos cursos hay?");

            assertTrue(respuesta.respuesta().contains("Cursos activos: 8"));
            assertTrue(respuesta.respuesta().contains("Curso (sin-codigo)"));
            assertTrue(respuesta.respuesta().contains("..."));
        }

        @Test
        @DisplayName("con contexto de PROFESOR sin cursos responde 'sin cursos'")
        void contextoProfesorSinCursos() {
            Usuario profesor = usuario("prof-2", Rol.PROFESOR, "Profesor Sin Cursos");
            when(usuarioServicio.buscarEmail("prof2@test.com")).thenReturn(profesor);
            when(iaHistorialRepositorio.findByUsuario(profesor)).thenReturn(Optional.empty());

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("prof2@test.com", "PROFESOR", "Hola");

            assertTrue(respuesta.respuesta().contains("sin cursos"));
        }

        @Test
        @DisplayName("con mensaje de longitud exactamente 4000 no lanza excepcion (limite exacto permitido)")
        void mensajeEnLimiteExactoNoLanzaExcepcion() {
            Usuario admin = usuario("admin-4", Rol.ADMIN, "Admin Limite");
            when(usuarioServicio.buscarEmail("limite@test.com")).thenReturn(admin);
            when(iaHistorialRepositorio.findByUsuario(admin)).thenReturn(Optional.empty());
            when(cursoRepositorio.buscarCursosActivos(any(Date.class))).thenReturn(List.of());

            String mensajeLimite = "a".repeat(4000);

            assertDoesNotThrow(() -> iaServicio.chatearSegunRol("limite@test.com", "ADMIN", mensajeLimite));
        }

        @Test
        @DisplayName("con groqApiKey nula tambien usa el fallback local (rama null del OR)")
        void groqApiKeyNulaUsaFallback() {
            IaServicio servicioConKeyNula = new IaServicio(
                    usuarioServicio,
                    iaHistorialRepositorio,
                    cursoRepositorio,
                    inscripcionRepositorio,
                    silaboRepositorio,
                    objectMapper,
                    null,
                    "llama-3.1-8b-instant",
                    cohereServicio);

            Usuario admin = usuario("admin-5", Rol.ADMIN, "Admin Key Nula");
            when(usuarioServicio.buscarEmail("keynula@test.com")).thenReturn(admin);
            when(iaHistorialRepositorio.findByUsuario(admin)).thenReturn(Optional.empty());
            when(cursoRepositorio.buscarCursosActivos(any(Date.class))).thenReturn(List.of());

            IaChatResponseDto respuesta = servicioConKeyNula.chatearSegunRol("keynula@test.com", "ADMIN", "Hola");

            assertEquals("fallback-local", respuesta.modelo());
        }

        @Test
        @DisplayName("con usuario cuyo id esta en blanco (no nulo) no resuelve el contexto")
        void usuarioConIdEnBlancoNoResuelveContexto() {
            Usuario idBlanco = usuario("   ", Rol.ADMIN, "Id En Blanco");
            when(usuarioServicio.buscarEmail("idblanco@test.com")).thenReturn(idBlanco);
            when(iaHistorialRepositorio.findByUsuario(idBlanco)).thenReturn(Optional.empty());

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("idblanco@test.com", "ADMIN", "Hola");

            assertTrue(respuesta.respuesta().contains("no se pudo resolver el usuario autenticado"));
        }

        @Test
        @DisplayName("cuando todos los cursos activos son nulos el resumen retorna 'sin cursos'")
        void resumenDeCursosActivosSoloNulos() {
            Usuario admin = usuario("admin-6", Rol.ADMIN, "Admin Solo Nulos");
            when(usuarioServicio.buscarEmail("admin6@test.com")).thenReturn(admin);
            when(iaHistorialRepositorio.findByUsuario(admin)).thenReturn(Optional.empty());
            when(cursoRepositorio.buscarCursosActivos(any(Date.class))).thenReturn(Arrays.asList(null, null));

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("admin6@test.com", "ADMIN", "Hola");

            assertTrue(respuesta.respuesta().contains("Cursos activos: 2"));
            assertTrue(respuesta.respuesta().contains("Cursos activos (muestra): sin cursos"));
        }

        @Test
        @DisplayName("resume los cursos sin truncar cuando la cantidad es menor o igual al limite")
        void resumenSinTruncarCuandoCantidadMenorOIgualLimite() {
            Usuario profesor = usuario("prof-3", Rol.PROFESOR, "Profesor Pocos Cursos");
            when(usuarioServicio.buscarEmail("prof3@test.com")).thenReturn(profesor);
            when(iaHistorialRepositorio.findByUsuario(profesor)).thenReturn(Optional.empty());
            when(cursoRepositorio.buscarCursosInscritosProfesor("prof-3")).thenReturn(
                    List.of(curso("c-10", "Curso Uno", "U1"), curso("c-11", "Curso Dos", "U2")));
            when(cursoRepositorio.buscarCursosDisponiblesProfesor("prof-3")).thenReturn(List.of());

            IaChatResponseDto respuesta = iaServicio.chatearSegunRol("prof3@test.com", "PROFESOR", "Hola");

            assertTrue(respuesta.respuesta().contains("Curso Uno (U1), Curso Dos (U2)"));
            assertFalse(respuesta.respuesta().contains("..."));
        }
    }

    // =====================================================================
    // obtenerUltimoHistorial
    // =====================================================================
    @Nested
    @DisplayName("obtenerUltimoHistorial")
    class ObtenerUltimoHistorial {

        @Test
        @DisplayName("con usuario inexistente retorna vacio")
        void usuarioInexistenteRetornaVacio() {
            when(usuarioServicio.buscarEmail("noexiste@test.com")).thenReturn(null);

            Optional<IaHistorialDto> resultado = iaServicio.obtenerUltimoHistorial("noexiste@test.com");

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("con usuario sin historial retorna vacio")
        void usuarioSinHistorialRetornaVacio() {
            Usuario u = usuario("u-1", Rol.ALUMNO, "Usuario Sin Historial");
            when(usuarioServicio.buscarEmail("u1@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.empty());

            Optional<IaHistorialDto> resultado = iaServicio.obtenerUltimoHistorial("u1@test.com");

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("con usuario e historial existentes retorna el dto mapeado")
        void usuarioConHistorialRetornaDto() {
            Usuario u = usuario("u-2", Rol.ALUMNO, "Usuario Con Historial");
            IaHistorial historial = new IaHistorial();
            historial.setUltimoMensaje("Hola");
            historial.setUltimaRespuesta("Respuesta");
            historial.setRol("ALUMNO");
            historial.setModelo("fallback-local");

            when(usuarioServicio.buscarEmail("u2@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.of(historial));

            Optional<IaHistorialDto> resultado = iaServicio.obtenerUltimoHistorial("u2@test.com");

            assertTrue(resultado.isPresent());
            assertEquals("Hola", resultado.get().ultimoMensaje());
            assertEquals("Respuesta", resultado.get().ultimaRespuesta());
            assertEquals("ALUMNO", resultado.get().rol());
            assertEquals("fallback-local", resultado.get().modelo());
        }
    }

    // =====================================================================
    // obtenerConversacion
    // =====================================================================
    @Nested
    @DisplayName("obtenerConversacion")
    class ObtenerConversacion {

        @Test
        @DisplayName("con usuario inexistente retorna conversacion vacia")
        void usuarioInexistenteConversacionVacia() {
            when(usuarioServicio.buscarEmail("noexiste@test.com")).thenReturn(null);

            IaConversacionDto resultado = iaServicio.obtenerConversacion("noexiste@test.com");

            assertEquals(0, resultado.totalMensajes());
            assertTrue(resultado.mensajes().isEmpty());
        }

        @Test
        @DisplayName("con usuario sin historial retorna conversacion vacia")
        void usuarioSinHistorialConversacionVacia() {
            Usuario u = usuario("u-3", Rol.ALUMNO, "Sin Historial");
            when(usuarioServicio.buscarEmail("u3@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.empty());

            IaConversacionDto resultado = iaServicio.obtenerConversacion("u3@test.com");

            assertEquals(0, resultado.totalMensajes());
        }

        @Test
        @DisplayName("con historial en JSON valido retorna los mensajes parseados")
        void historialValidoRetornaMensajes() {
            Usuario u = usuario("u-4", Rol.ALUMNO, "Con Historial");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion("[{\"role\":\"user\",\"content\":\"Hola\"},"
                    + "{\"role\":\"assistant\",\"content\":\"Hola, en que te ayudo?\"}]");

            when(usuarioServicio.buscarEmail("u4@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.of(historial));

            IaConversacionDto resultado = iaServicio.obtenerConversacion("u4@test.com");

            assertEquals(2, resultado.totalMensajes());
            IaMensajeDto primero = resultado.mensajes().get(0);
            assertEquals("user", primero.rol());
            assertEquals("Hola", primero.contenido());
        }

        @Test
        @DisplayName("con historial en JSON invalido retorna conversacion vacia")
        void historialInvalidoRetornaVacio() {
            Usuario u = usuario("u-5", Rol.ALUMNO, "Historial Corrupto");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion("no-es-json");

            when(usuarioServicio.buscarEmail("u5@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.of(historial));

            IaConversacionDto resultado = iaServicio.obtenerConversacion("u5@test.com");

            assertEquals(0, resultado.totalMensajes());
        }

        @Test
        @DisplayName("con historial nulo o en blanco retorna conversacion vacia")
        void historialNuloRetornaVacio() {
            Usuario u = usuario("u-6", Rol.ALUMNO, "Historial Nulo");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion(null);

            when(usuarioServicio.buscarEmail("u6@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.of(historial));

            IaConversacionDto resultado = iaServicio.obtenerConversacion("u6@test.com");

            assertEquals(0, resultado.totalMensajes());
        }

        @Test
        @DisplayName("con historial JSON que no es un arreglo retorna conversacion vacia")
        void historialNoEsArregloRetornaVacio() {
            Usuario u = usuario("u-7", Rol.ALUMNO, "Historial Objeto");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion("{\"a\":1}");

            when(usuarioServicio.buscarEmail("u7@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.of(historial));

            IaConversacionDto resultado = iaServicio.obtenerConversacion("u7@test.com");

            assertEquals(0, resultado.totalMensajes());
        }
    }

    // =====================================================================
    // limpiarHistorial
    // =====================================================================
    @Nested
    @DisplayName("limpiarHistorial")
    class LimpiarHistorial {

        @Test
        @DisplayName("con usuario inexistente no hace nada")
        void usuarioInexistenteNoHaceNada() {
            when(usuarioServicio.buscarEmail("noexiste@test.com")).thenReturn(null);

            iaServicio.limpiarHistorial("noexiste@test.com");

            verify(iaHistorialRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con usuario sin historial no guarda nada")
        void usuarioSinHistorialNoGuardaNada() {
            Usuario u = usuario("u-8", Rol.ALUMNO, "Sin Historial");
            when(usuarioServicio.buscarEmail("u8@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.empty());

            iaServicio.limpiarHistorial("u8@test.com");

            verify(iaHistorialRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con usuario e historial existentes reinicia el historial")
        void conHistorialExistenteLoReinicia() {
            Usuario u = usuario("u-9", Rol.ALUMNO, "Con Historial");
            IaHistorial historial = new IaHistorial();
            historial.setHistorialConversacion("[{\"role\":\"user\",\"content\":\"Hola\"}]");
            historial.setUltimoMensaje("Hola");
            historial.setUltimaRespuesta("Respuesta anterior");

            when(usuarioServicio.buscarEmail("u9@test.com")).thenReturn(u);
            when(iaHistorialRepositorio.findByUsuario(u)).thenReturn(Optional.of(historial));

            iaServicio.limpiarHistorial("u9@test.com");

            ArgumentCaptor<IaHistorial> captor = ArgumentCaptor.forClass(IaHistorial.class);
            verify(iaHistorialRepositorio).save(captor.capture());
            IaHistorial guardado = captor.getValue();
            assertNull(guardado.getHistorialConversacion());
            assertEquals("Historial limpiado por el usuario", guardado.getUltimoMensaje());
            assertEquals("El historial de conversacion ha sido reiniciado.", guardado.getUltimaRespuesta());
        }
    }

    // =====================================================================
    // obtenerSugerencias
    // =====================================================================
    @Nested
    @DisplayName("obtenerSugerencias")
    class ObtenerSugerencias {

        @Test
        @DisplayName("para ADMIN retorna sugerencias administrativas")
        void sugerenciasAdmin() {
            IaSugerenciasDto resultado = iaServicio.obtenerSugerencias("ADMIN");

            assertEquals("ADMIN", resultado.rol());
            assertEquals(5, resultado.sugerencias().size());
            assertTrue(resultado.sugerencias().get(0).contains("cursos activos"));
        }

        @Test
        @DisplayName("para PROFESOR retorna sugerencias de aula")
        void sugerenciasProfesor() {
            IaSugerenciasDto resultado = iaServicio.obtenerSugerencias("PROFESOR");

            assertEquals("PROFESOR", resultado.rol());
            assertEquals(5, resultado.sugerencias().size());
            assertTrue(resultado.sugerencias().stream().anyMatch(s -> s.contains("rúbrica")));
        }

        @Test
        @DisplayName("para ALUMNO retorna sugerencias academicas")
        void sugerenciasAlumno() {
            IaSugerenciasDto resultado = iaServicio.obtenerSugerencias("ALUMNO");

            assertEquals("ALUMNO", resultado.rol());
            assertEquals(5, resultado.sugerencias().size());
            assertTrue(resultado.sugerencias().stream().anyMatch(s -> s.contains("matriculado")));
        }

        @Test
        @DisplayName("para rol no reconocido retorna sugerencias genericas")
        void sugerenciasRolDesconocido() {
            IaSugerenciasDto resultado = iaServicio.obtenerSugerencias("INVITADO");

            assertEquals("INVITADO", resultado.rol());
            assertEquals(3, resultado.sugerencias().size());
        }
    }

    // =====================================================================
    // generarRubrica
    // =====================================================================
    @Nested
    @DisplayName("generarRubrica")
    class GenerarRubrica {

        private RubricaGeneracionRequestDto requestValido() {
            return new RubricaGeneracionRequestDto("Cambio climatico", "Secundaria", "Ciencias", "Ensayo", 5, 4, 25);
        }

        @Test
        @DisplayName("con usuario inexistente lanza excepcion")
        void usuarioInexistenteLanzaExcepcion() {
            when(usuarioServicio.buscarEmail("noexiste@test.com")).thenReturn(null);
            RubricaGeneracionRequestDto request = requestValido();

            assertThrows(IllegalArgumentException.class,
                    () -> iaServicio.generarRubrica("noexiste@test.com", request));
        }

        @Test
        @DisplayName("con request nulo lanza excepcion")
        void requestNuloLanzaExcepcion() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);

            assertThrows(IllegalArgumentException.class,
                    () -> iaServicio.generarRubrica("prof@test.com", null));
        }

        @Test
        @DisplayName("con tema nulo lanza excepcion")
        void temaNuloLanzaExcepcion() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            RubricaGeneracionRequestDto req = new RubricaGeneracionRequestDto(null, null, null, null, null, null, null);

            assertThrows(IllegalArgumentException.class, () -> iaServicio.generarRubrica("prof@test.com", req));
        }

        @Test
        @DisplayName("con tema en blanco lanza excepcion")
        void temaEnBlancoLanzaExcepcion() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            RubricaGeneracionRequestDto req = new RubricaGeneracionRequestDto("   ", null, null, null, null, null, null);

            assertThrows(IllegalArgumentException.class, () -> iaServicio.generarRubrica("prof@test.com", req));
        }

        @Test
        @DisplayName("cuando Cohere no esta configurado usa el fallback local")
        void cohereNoConfiguradoUsaFallback() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(false);

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertFalse(resultado.generadaPorIa());
            assertEquals("fallback-local", resultado.modelo());
            assertEquals(5, resultado.criterios().size());
            resultado.criterios().forEach(c -> assertEquals(4, c.niveles().size()));
            int sumaPesos = resultado.criterios().stream().mapToInt(CriterioRubricaDto::peso).sum();
            assertEquals(100, sumaPesos);
        }

        @Test
        @DisplayName("con valores por defecto (campos nulos) normaliza cantidad y puntaje")
        void conValoresPorDefectoNormaliza() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            RubricaGeneracionRequestDto req = new RubricaGeneracionRequestDto("Tema libre", null, null, null, null, null, null);

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", req);

            assertEquals("Secundaria", resultado.nivelEducativo());
            assertEquals("General", resultado.asignatura());
            assertEquals("Trabajo escrito", resultado.tipoTarea());
            assertEquals(20, resultado.puntajeMaximo());
            assertEquals(4, resultado.criterios().size());
        }

        @Test
        @DisplayName("con cantidades fuera de rango las ajusta a los limites minimos")
        void cantidadesPorDebajoDelMinimoSeAjustan() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            RubricaGeneracionRequestDto req = new RubricaGeneracionRequestDto("Tema", null, null, null, 1, 1, 1);

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", req);

            assertEquals(3, resultado.criterios().size());
            assertEquals(3, resultado.criterios().get(0).niveles().size());
            assertEquals(10, resultado.puntajeMaximo());
        }

        @Test
        @DisplayName("con cantidades fuera de rango las ajusta a los limites maximos")
        void cantidadesPorEncimaDelMaximoSeAjustan() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            RubricaGeneracionRequestDto req = new RubricaGeneracionRequestDto("Tema", null, null, null, 100, 100, 1000);

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", req);

            assertEquals(8, resultado.criterios().size());
            assertEquals(5, resultado.criterios().get(0).niveles().size());
            assertEquals(100, resultado.puntajeMaximo());
        }

        @Test
        @DisplayName("cuando Cohere esta configurado y responde JSON valido usa la respuesta de Cohere")
        void cohereConfiguradoRespuestaValida() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenReturn(RUBRICA_JSON_VALIDA);
            when(cohereServicio.getModelo()).thenReturn("command-r-08-2024");

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertTrue(resultado.generadaPorIa());
            assertEquals("command-r-08-2024", resultado.modelo());
            assertEquals("Rubrica de Ensayo", resultado.titulo());
            assertEquals(2, resultado.criterios().size());
        }

        @Test
        @DisplayName("cuando Cohere esta configurado pero responde envuelta en markdown la parsea igual")
        void cohereConfiguradoRespuestaConMarkdown() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenReturn("```json\n" + RUBRICA_JSON_VALIDA + "\n```");
            when(cohereServicio.getModelo()).thenReturn("command-r-08-2024");

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertTrue(resultado.generadaPorIa());
            assertEquals("Rubrica de Ensayo", resultado.titulo());
        }

        @Test
        @DisplayName("cuando Cohere esta configurado pero lanza excepcion usa el fallback local")
        void cohereConfiguradoLanzaExcepcionUsaFallback() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenThrow(new RuntimeException("timeout"));

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertFalse(resultado.generadaPorIa());
            assertEquals("fallback-local", resultado.modelo());
        }

        @Test
        @DisplayName("cuando Cohere responde vacio usa el fallback local")
        void cohereRespuestaVaciaUsaFallback() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenReturn("   ");

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertFalse(resultado.generadaPorIa());
        }

        @Test
        @DisplayName("cuando Cohere responde sin JSON valido usa el fallback local")
        void cohereRespuestaSinJsonUsaFallback() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenReturn("respuesta sin llaves");

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertFalse(resultado.generadaPorIa());
        }

        @Test
        @DisplayName("cuando Cohere lanza una excepcion con causa anidada registra el mensaje raiz")
        void cohereLanzaExcepcionConCausaAnidada() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString()))
                    .thenThrow(new RuntimeException("fallo externo", new IllegalStateException("causa raiz")));

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertFalse(resultado.generadaPorIa());
        }

        @Test
        @DisplayName("cuando Cohere lanza una excepcion sin mensaje usa el nombre de la clase")
        void cohereLanzaExcepcionSinMensaje() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            Exception sinMensaje = new Exception() {
                @Override
                public String getMessage() {
                    return null;
                }
            };
            when(cohereServicio.generarTexto(anyString())).thenThrow(new RuntimeException("wrap", sinMensaje));

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertFalse(resultado.generadaPorIa());
        }

        @Test
        @DisplayName("con campos de texto en blanco (no nulos) usa los valores por defecto")
        void conCamposEnBlancoUsaValoresPorDefecto() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            RubricaGeneracionRequestDto req = new RubricaGeneracionRequestDto("Tema", "   ", "   ", "   ", null, null, null);

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", req);

            assertEquals("Secundaria", resultado.nivelEducativo());
            assertEquals("General", resultado.asignatura());
            assertEquals("Trabajo escrito", resultado.tipoTarea());
        }

        @Test
        @DisplayName("con cantidades exactamente en los limites minimos y maximos no las ajusta")
        void cantidadesEnLimiteExactoNoSeAjustan() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            RubricaGeneracionRequestDto req = new RubricaGeneracionRequestDto("Tema", null, null, null, 3, 5, 10);

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", req);

            assertEquals(3, resultado.criterios().size());
            assertEquals(5, resultado.criterios().get(0).niveles().size());
            assertEquals(10, resultado.puntajeMaximo());
        }

        @Test
        @DisplayName("cuando Cohere lanza una excepcion con mensaje en blanco usa el nombre de la clase")
        void cohereLanzaExcepcionConMensajeEnBlanco() {
            Usuario u = usuario("prof-x", Rol.PROFESOR, "Profesor");
            when(usuarioServicio.buscarEmail("prof@test.com")).thenReturn(u);
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            Exception mensajeBlanco = new Exception() {
                @Override
                public String getMessage() {
                    return "   ";
                }
            };
            when(cohereServicio.generarTexto(anyString())).thenThrow(new RuntimeException("wrap", mensajeBlanco));

            RubricaGeneradaDto resultado = iaServicio.generarRubrica("prof@test.com", requestValido());

            assertFalse(resultado.generadaPorIa());
        }
    }

    // =====================================================================
    // generarRubricaConCohere (invocacion directa)
    // =====================================================================
    @Nested
    @DisplayName("generarRubricaConCohere")
    class GenerarRubricaConCohereDirecto {

        @Test
        @DisplayName("con respuesta valida de Cohere retorna la rubrica generada por IA")
        void respuestaValidaRetornaRubrica() {
            when(cohereServicio.generarTexto("prompt")).thenReturn(RUBRICA_JSON_VALIDA);
            when(cohereServicio.getModelo()).thenReturn("command-r-08-2024");

            RubricaGeneradaDto resultado = iaServicio.generarRubricaConCohere("prompt");

            assertTrue(resultado.generadaPorIa());
            assertEquals("command-r-08-2024", resultado.modelo());
            assertEquals("Cambio climatico", resultado.tema());
        }

        @Test
        @DisplayName("con respuesta invalida de Cohere lanza IllegalStateException")
        void respuestaInvalidaLanzaExcepcion() {
            when(cohereServicio.generarTexto("prompt")).thenReturn("sin json aqui");

            assertThrows(IllegalStateException.class, () -> iaServicio.generarRubricaConCohere("prompt"));
        }
    }

    // =====================================================================
    // generarSilabo
    // =====================================================================
    @Nested
    @DisplayName("generarSilabo")
    class GenerarSilabo {

        @Test
        @DisplayName("con request nulo lanza excepcion")
        void requestNuloLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () -> iaServicio.generarSilabo(null));
        }

        @Test
        @DisplayName("sin nombreCurso ni cursoId lanza excepcion")
        void sinNombreCursoNiCursoIdLanzaExcepcion() {
            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "  ", "Carrera", 1, 4, 16, "desc");

            assertThrows(IllegalArgumentException.class, () -> iaServicio.generarSilabo(req));
        }

        @Test
        @DisplayName("con cursoId valido y sin nombreCurso resuelve el nombre desde el repositorio")
        void resuelveNombreDesdeCursoId() {
            Curso c = curso("c-1", "Curso Real", "CR1");
            when(cursoRepositorio.findById("c-1")).thenReturn(Optional.of(c));
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            when(silaboRepositorio.findByCursoId("c-1")).thenReturn(Optional.empty());
            when(silaboRepositorio.save(any(Silabo.class))).thenAnswer(inv -> inv.getArgument(0));

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto("c-1", null, "Carrera", 1, 4, 16, "desc");
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals("Curso Real", resultado.informacionGeneral().curso());
            verify(silaboRepositorio).save(any(Silabo.class));
        }

        @Test
        @DisplayName("con cursoId no encontrado usa 'Curso sin nombre' y continua con fallback")
        void cursoIdNoEncontradoUsaNombrePorDefecto() {
            when(cursoRepositorio.findById("c-x")).thenReturn(Optional.empty());
            when(cohereServicio.estaConfigurado()).thenReturn(false);

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto("c-x", null, null, 0, 0, 4, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals("Curso sin nombre", resultado.informacionGeneral().curso());
            assertEquals(1, resultado.informacionGeneral().ciclo());
            assertEquals(4, resultado.informacionGeneral().creditos());
            verify(silaboRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con semanas por debajo del minimo las ajusta a 4 y usa 3 unidades")
        void semanasPorDebajoDelMinimoUsaTresUnidades() {
            when(cohereServicio.estaConfigurado()).thenReturn(false);

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Corto", "Ing", 0, 0, 1, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals(4, resultado.unidades().stream().mapToInt(u -> u.semanas().size()).sum());
            assertEquals(3, resultado.unidades().size());
        }

        @Test
        @DisplayName("con semanas mayores o iguales a 12 usa 4 unidades")
        void semanasMayoresIgualA12UsaCuatroUnidades() {
            when(cohereServicio.estaConfigurado()).thenReturn(false);

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Largo", "Ing", 3, 5, 16, "Mi descripcion");
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals(4, resultado.unidades().size());
            boolean hayEvaluacionContinua = resultado.unidades().stream()
                    .flatMap(u -> u.semanas().stream())
                    .anyMatch(s -> s.evaluacion().startsWith("Evaluacion continua"));
            boolean haySeguimientoFormativo = resultado.unidades().stream()
                    .flatMap(u -> u.semanas().stream())
                    .anyMatch(s -> s.evaluacion().equals("Seguimiento formativo"));
            assertTrue(hayEvaluacionContinua);
            assertTrue(haySeguimientoFormativo);
        }

        @Test
        @DisplayName("con semanas mayores al maximo las ajusta a 20")
        void semanasMayoresAlMaximoSeAjustan() {
            when(cohereServicio.estaConfigurado()).thenReturn(false);

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Muy Largo", "Ing", 1, 4, 40, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals(20, resultado.unidades().stream().mapToInt(u -> u.semanas().size()).sum());
        }

        @Test
        @DisplayName("cuando Cohere esta configurado y responde JSON valido persiste y retorna el silabo generado")
        void cohereConfiguradoRespuestaValidaPersiste() {
            Curso c = curso("c-2", "Base de Datos II", "BD2");
            when(cursoRepositorio.findById("c-2")).thenReturn(Optional.of(c));
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenReturn(SILABO_JSON_VALIDO);
            when(silaboRepositorio.findByCursoId("c-2")).thenReturn(Optional.empty());
            when(silaboRepositorio.save(any(Silabo.class))).thenAnswer(inv -> inv.getArgument(0));

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto("c-2", "Base de Datos II", "Ing. de Sistemas", 5, 4, 1, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals("Base de Datos II", resultado.informacionGeneral().curso());
            verify(silaboRepositorio).save(any(Silabo.class));
        }

        @Test
        @DisplayName("cuando Cohere esta configurado y ya existe un silabo lo actualiza")
        void cohereConfiguradoActualizaSilaboExistente() {
            Curso c = curso("c-3", "Curso Existente", "CE1");
            Silabo existente = new Silabo();
            when(cursoRepositorio.findById("c-3")).thenReturn(Optional.of(c));
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenReturn(SILABO_JSON_VALIDO);
            when(silaboRepositorio.findByCursoId("c-3")).thenReturn(Optional.of(existente));
            when(silaboRepositorio.save(any(Silabo.class))).thenAnswer(inv -> inv.getArgument(0));

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto("c-3", "Curso Existente", "Ing", 1, 4, 1, null);
            iaServicio.generarSilabo(req);

            verify(silaboRepositorio).save(existente);
        }

        @Test
        @DisplayName("cuando Cohere esta configurado pero lanza excepcion usa el fallback local")
        void cohereConfiguradoLanzaExcepcionUsaFallback() {
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenThrow(new RuntimeException("fallo cohere"));

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Fallback", "Ing", 1, 4, 4, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals("Curso Fallback", resultado.informacionGeneral().curso());
        }

        @Test
        @DisplayName("cuando Cohere esta configurado pero responde JSON invalido usa el fallback local")
        void cohereConfiguradoRespuestaInvalidaUsaFallback() {
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenReturn("no es json");

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Invalido", "Ing", 1, 4, 4, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals("Curso Invalido", resultado.informacionGeneral().curso());
        }

        @Test
        @DisplayName("resuelve el curso por nombre cuando no se envia cursoId")
        void resuelveCursoPorNombre() {
            Curso c = curso("c-4", "Curso Por Nombre", "CN1");
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            when(cursoRepositorio.findFirstByNombreIgnoreCase("Curso Por Nombre")).thenReturn(Optional.of(c));
            when(silaboRepositorio.findByCursoId("c-4")).thenReturn(Optional.empty());
            when(silaboRepositorio.save(any(Silabo.class))).thenAnswer(inv -> inv.getArgument(0));

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Por Nombre", "Ing", 1, 4, 4, null);
            iaServicio.generarSilabo(req);

            verify(silaboRepositorio).save(any(Silabo.class));
        }

        @Test
        @DisplayName("si no encuentra el curso por nombre no falla, solo omite la persistencia")
        void noEncuentraCursoPorNombreNoFalla() {
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            when(cursoRepositorio.findFirstByNombreIgnoreCase("Curso Inexistente")).thenReturn(Optional.empty());

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Inexistente", "Ing", 1, 4, 4, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals("Curso Inexistente", resultado.informacionGeneral().curso());
            verify(silaboRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con nombreCurso en blanco (no nulo) y sin cursoId lanza excepcion")
        void nombreCursoEnBlancoSinCursoIdLanzaExcepcion() {
            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "   ", "Carrera", 1, 4, 16, "desc");

            assertThrows(IllegalArgumentException.class, () -> iaServicio.generarSilabo(req));
        }

        @Test
        @DisplayName("con nombreCurso en blanco (no nulo) y cursoId valido resuelve el nombre desde el repositorio")
        void nombreCursoEnBlancoConCursoIdValidoResuelveNombre() {
            Curso c = curso("c-9", "Curso Resuelto", "CR9");
            when(cursoRepositorio.findById("c-9")).thenReturn(Optional.of(c));
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            when(silaboRepositorio.findByCursoId("c-9")).thenReturn(Optional.empty());
            when(silaboRepositorio.save(any(Silabo.class))).thenAnswer(inv -> inv.getArgument(0));

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto("c-9", "   ", "Carrera", 1, 4, 16, "desc");
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals("Curso Resuelto", resultado.informacionGeneral().curso());
        }

        @Test
        @DisplayName("con cursoId en blanco (no nulo) resuelve el curso por nombre para persistir")
        void cursoIdEnBlancoResuelveCursoPorNombre() {
            Curso c = curso("c-8", "Curso Con Id Blanco", "CB8");
            when(cohereServicio.estaConfigurado()).thenReturn(false);
            when(cursoRepositorio.findFirstByNombreIgnoreCase("Curso Con Id Blanco")).thenReturn(Optional.of(c));
            when(silaboRepositorio.findByCursoId("c-8")).thenReturn(Optional.empty());
            when(silaboRepositorio.save(any(Silabo.class))).thenAnswer(inv -> inv.getArgument(0));

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto("   ", "Curso Con Id Blanco", "Ing", 1, 4, 4, null);
            iaServicio.generarSilabo(req);

            verify(silaboRepositorio).save(any(Silabo.class));
        }

        @Test
        @DisplayName("cuando Cohere responde JSON valido pero no puede resolver el curso usa el fallback sin persistir")
        void cohereConfiguradoPeroNoResuelveElCursoUsaFallback() {
            when(cohereServicio.estaConfigurado()).thenReturn(true);
            when(cohereServicio.generarTexto(anyString())).thenReturn(SILABO_JSON_VALIDO);
            when(cursoRepositorio.findFirstByNombreIgnoreCase("Curso Sin Match")).thenReturn(Optional.empty());

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Sin Match", "Ing", 1, 4, 4, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals("Curso Sin Match", resultado.informacionGeneral().curso());
            assertTrue(resultado.sistemaEvaluacion().contains("La calificacion integra"));
            verify(silaboRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con semanas exactamente en 12 usa cuatro unidades (limite exacto)")
        void semanasExactamente12UsaCuatroUnidades() {
            when(cohereServicio.estaConfigurado()).thenReturn(false);

            SilaboGeneracionRequestDto req = new SilaboGeneracionRequestDto(null, "Curso Limite", "Ing", 1, 4, 12, null);
            SilaboGeneradoDto resultado = iaServicio.generarSilabo(req);

            assertEquals(4, resultado.unidades().size());
        }
    }
}
