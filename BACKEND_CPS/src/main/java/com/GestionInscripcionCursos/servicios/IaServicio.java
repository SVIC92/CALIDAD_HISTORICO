package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.IaChatResponseDto;
import com.GestionInscripcionCursos.dto.IaConversacionDto;
import com.GestionInscripcionCursos.dto.IaHistorialDto;
import com.GestionInscripcionCursos.dto.IaMensajeDto;
import com.GestionInscripcionCursos.dto.IaSugerenciasDto;
import com.GestionInscripcionCursos.dto.CriterioRubricaDto;
import com.GestionInscripcionCursos.dto.NivelRubricaDto;
import com.GestionInscripcionCursos.dto.RubricaGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.dto.InformacionGeneralDto;
import com.GestionInscripcionCursos.dto.SemanaSilaboDto;
import com.GestionInscripcionCursos.dto.SilaboGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.SilaboGeneradoDto;
import com.GestionInscripcionCursos.dto.UnidadSilaboDto;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.IaHistorial;
import com.GestionInscripcionCursos.entidades.Silabo;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.IaHistorialRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.SilaboRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IaServicio {

    private static final Logger LOGGER = LoggerFactory.getLogger(IaServicio.class);

    private static final String URL_GROQ = "https://api.groq.com/openai/v1/chat/completions";

    private static final String ESQUEMA_RUBRICA =
            "{\"titulo\":\"...\",\"descripcion\":\"...\",\"tema\":\"...\",\"nivelEducativo\":\"...\","
            + "\"asignatura\":\"...\",\"tipoTarea\":\"...\",\"puntajeMaximo\":0,"
            + "\"criterios\":[{\"nombre\":\"...\",\"descripcion\":\"...\",\"peso\":0,"
            + "\"niveles\":[{\"nombre\":\"...\",\"puntaje\":0,\"descriptor\":\"...\"}]}]}";

    private static final String ESQUEMA_SILABO =
            "{\"informacionGeneral\":{\"curso\":\"...\",\"carrera\":\"...\",\"ciclo\":0,\"creditos\":0},"
            + "\"competenciasGenerales\":[\"...\"],\"competenciasEspecificas\":[\"...\"],"
            + "\"sumilla\":\"...\",\"logroCurso\":\"...\","
            + "\"unidades\":[{\"tituloUnidad\":\"...\",\"logroUnidad\":\"...\","
            + "\"semanas\":[{\"numeroSemana\":1,\"temas\":\"...\","
            + "\"actividadesPracticas\":\"...\",\"evaluacion\":\"...\"}]}],"
            + "\"sistemaEvaluacion\":\"...\"}";

    private final UsuarioServicio usuarioServicio;
    private final IaHistorialRepositorio iaHistorialRepositorio;
    private final CursoRepositorio cursoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;
    private final SilaboRepositorio silaboRepositorio;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String groqApiKey;
    private final String modeloPorDefecto;
    private final CohereServicio cohereServicio;

    public IaServicio(
            UsuarioServicio usuarioServicio,
            IaHistorialRepositorio iaHistorialRepositorio,
            CursoRepositorio cursoRepositorio,
            InscripcionRepositorio inscripcionRepositorio,
            SilaboRepositorio silaboRepositorio,
            ObjectMapper objectMapper,
            @Value("${groq.api.key:}") String groqApiKey,
            @Value("${groq.model:llama-3.1-8b-instant}") String modeloPorDefecto,
            CohereServicio cohereServicio
    ) {
        this.usuarioServicio = usuarioServicio;
        this.iaHistorialRepositorio = iaHistorialRepositorio;
        this.cursoRepositorio = cursoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
        this.silaboRepositorio = silaboRepositorio;
        this.objectMapper = objectMapper;
        this.groqApiKey = groqApiKey;
        this.modeloPorDefecto = modeloPorDefecto;
        this.httpClient = HttpClient.newHttpClient();
        this.cohereServicio = cohereServicio;
    }

    private static final int MAX_MENSAJES_CONTEXTO = 10;
    private static final int MAX_MENSAJES_HISTORIAL = 50;

    // ==========================================
    // CHAT EXCLUSIVO CON GROQ (multi-turno)
    // ==========================================
    @Transactional
    public IaChatResponseDto chatearSegunRol(String email, String rol, String mensaje) {
        validarEntrada(mensaje);

        Usuario usuario = usuarioServicio.buscarEmail(email);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        IaHistorial historial = iaHistorialRepositorio.findByUsuario(usuario).orElseGet(IaHistorial::new);

        String respuesta;
        String modeloRespuesta;

        if (groqApiKey == null || groqApiKey.isBlank()) {
            LOGGER.warn("GROQ_API_KEY no configurada. Se respondera con fallback local para rol {}", rol);
            respuesta = construirRespuestaFallback(rol, mensaje, usuario);
            modeloRespuesta = "fallback-local";
        } else {
            String promptSistema = construirPromptSistema(rol, usuario);
            List<Map<String, Object>> historialPrevio = cargarUltimosMensajes(historial, MAX_MENSAJES_CONTEXTO);
            respuesta = llamarGroq(promptSistema, mensaje, historialPrevio);
            modeloRespuesta = modeloPorDefecto;
        }

        actualizarHistorialConversacion(historial, mensaje, respuesta);

        historial.setUsuario(usuario);
        historial.setUltimoMensaje(mensaje);
        historial.setUltimaRespuesta(respuesta);
        historial.setRol(rol);
        historial.setModelo(modeloRespuesta);
        historial.setFechaActualizacion(Instant.now());
        iaHistorialRepositorio.save(historial);

        return new IaChatResponseDto(respuesta, rol, modeloRespuesta, Instant.now());
    }

    @Transactional(readOnly = true)
    public Optional<IaHistorialDto> obtenerUltimoHistorial(String email) {
        Usuario usuario = usuarioServicio.buscarEmail(email);
        if (usuario == null) {
            return Optional.empty();
        }

        return iaHistorialRepositorio.findByUsuario(usuario)
                .map(h -> new IaHistorialDto(
                        h.getUltimoMensaje(),
                        h.getUltimaRespuesta(),
                        h.getRol(),
                        h.getModelo(),
                        h.getFechaActualizacion()
                ));
    }

    @Transactional(readOnly = true)
    public IaConversacionDto obtenerConversacion(String email) {
        Usuario usuario = usuarioServicio.buscarEmail(email);
        if (usuario == null) {
            return new IaConversacionDto(List.of(), 0);
        }
        return iaHistorialRepositorio.findByUsuario(usuario)
                .map(h -> {
                    List<IaMensajeDto> mensajes = parsearConversacion(h.getHistorialConversacion());
                    return new IaConversacionDto(mensajes, mensajes.size());
                })
                .orElseGet(() -> new IaConversacionDto(List.of(), 0));
    }

    @Transactional
    public void limpiarHistorial(String email) {
        Usuario usuario = usuarioServicio.buscarEmail(email);
        if (usuario == null) {
            return;
        }
        iaHistorialRepositorio.findByUsuario(usuario).ifPresent(h -> {
            h.setHistorialConversacion(null);
            h.setUltimoMensaje("Historial limpiado por el usuario");
            h.setUltimaRespuesta("El historial de conversacion ha sido reiniciado.");
            h.setFechaActualizacion(Instant.now());
            iaHistorialRepositorio.save(h);
        });
    }

    public IaSugerenciasDto obtenerSugerencias(String rol) {
        List<String> sugerencias = switch (rol) {
            case "ADMIN" -> List.of(
                "¿Cuántos cursos activos hay actualmente en el sistema?",
                "¿Cuántas inscripciones están pendientes de aprobación?",
                "¿Cuáles son los cursos con mayor demanda de inscritos?",
                "¿Cómo puedo generar un reporte de inscripciones?",
                "¿Qué acciones administrativas puedo realizar hoy?"
            );
            case "PROFESOR" -> List.of(
                "¿En qué cursos estoy inscrito como profesor?",
                "¿Cómo creo una actividad para mis alumnos?",
                "¿Cómo genero una rúbrica de evaluación con IA?",
                "¿Cómo creo un sílabo con ayuda de la IA?",
                "¿Qué herramientas de evaluación tengo disponibles?"
            );
            case "ALUMNO" -> List.of(
                "¿En qué cursos estoy matriculado?",
                "¿Qué cursos están disponibles para inscribirme?",
                "¿Cómo me inscribo en un nuevo curso?",
                "¿Cuáles son mis actividades pendientes?",
                "¿Cómo puedo ver mi progreso académico?"
            );
            default -> List.of(
                "¿Qué funciones tiene el sistema?",
                "¿Cómo inicio sesión?",
                "¿Cómo recupero mi contraseña?"
            );
        };
        return new IaSugerenciasDto(rol, sugerencias);
    }

    private List<Map<String, Object>> cargarUltimosMensajes(IaHistorial historial, int limite) {
        if (historial == null || historial.getHistorialConversacion() == null
                || historial.getHistorialConversacion().isBlank()) {
            return List.of();
        }
        try {
            JsonNode array = objectMapper.readTree(historial.getHistorialConversacion());
            if (!array.isArray()) {
                return List.of();
            }
            List<Map<String, Object>> mensajes = new ArrayList<>();
            array.forEach(nodo -> {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("role", nodo.path("role").asText("user"));
                msg.put("content", nodo.path("content").asText(""));
                mensajes.add(msg);
            });
            int desde = Math.max(0, mensajes.size() - limite);
            return mensajes.subList(desde, mensajes.size());
        } catch (Exception e) {
            LOGGER.warn("No se pudo deserializar el historial de conversacion: {}", e.getMessage());
            return List.of();
        }
    }

    private void actualizarHistorialConversacion(IaHistorial historial, String mensaje, String respuesta) {
        List<Map<String, Object>> conversacion = new ArrayList<>(cargarUltimosMensajes(historial, MAX_MENSAJES_HISTORIAL));

        Map<String, Object> msgUsuario = new LinkedHashMap<>();
        msgUsuario.put("role", "user");
        msgUsuario.put("content", mensaje);
        conversacion.add(msgUsuario);

        Map<String, Object> msgAsistente = new LinkedHashMap<>();
        msgAsistente.put("role", "assistant");
        msgAsistente.put("content", respuesta);
        conversacion.add(msgAsistente);

        if (conversacion.size() > MAX_MENSAJES_HISTORIAL) {
            conversacion = conversacion.subList(conversacion.size() - MAX_MENSAJES_HISTORIAL, conversacion.size());
        }

        try {
            historial.setHistorialConversacion(objectMapper.writeValueAsString(conversacion));
        } catch (IOException e) {
            LOGGER.warn("No se pudo serializar el historial de conversacion: {}", e.getMessage());
        }
    }

    private List<IaMensajeDto> parsearConversacion(String historialJson) {
        if (historialJson == null || historialJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode array = objectMapper.readTree(historialJson);
            if (!array.isArray()) {
                return List.of();
            }
            List<IaMensajeDto> mensajes = new ArrayList<>();
            array.forEach(nodo -> mensajes.add(new IaMensajeDto(
                    nodo.path("role").asText("user"),
                    nodo.path("content").asText("")
            )));
            return mensajes;
        } catch (Exception e) {
            LOGGER.warn("Error parseando conversacion guardada: {}", e.getMessage());
            return List.of();
        }
    }

    // ==========================================
    // RÚBRICAS EXCLUSIVAS CON COHERE
    // ==========================================
    public RubricaGeneradaDto generarRubrica(String email, RubricaGeneracionRequestDto request) {
        if (usuarioServicio.buscarEmail(email) == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        if (request == null || request.tema() == null || request.tema().isBlank()) {
            throw new IllegalArgumentException("El tema es obligatorio para generar la rubrica");
        }

        String tema = request.tema().trim();
        String nivel = valorPorDefecto(request.nivelEducativo(), "Secundaria");
        String asignatura = valorPorDefecto(request.asignatura(), "General");
        String tipoTarea = valorPorDefecto(request.tipoTarea(), "Trabajo escrito");
        int cantidadCriterios = normalizarEntero(request.cantidadCriterios(), 4, 3, 8);
        int cantidadNiveles = normalizarEntero(request.cantidadNiveles(), 4, 3, 5);
        int puntajeMaximo = normalizarEntero(request.puntajeMaximo(), 20, 10, 100);

        if (cohereServicio.estaConfigurado()) {
            try {
                LOGGER.info("Intentando generar rúbrica con Cohere");
                String prompt = construirPromptRubrica(tema, nivel, asignatura, tipoTarea, cantidadCriterios, cantidadNiveles, puntajeMaximo);
                return generarRubricaConCohere(prompt);
            } catch (Exception e) {
                LOGGER.warn("Error generando rúbrica con Cohere: {}. Usando fallback local.", extraerMensajeRaiz(e));
            }
        } else {
            LOGGER.info("CohereServicio no disponible, usando fallback local para rúbrica");
        }
        return construirRubricaFallback(tema, nivel, asignatura, tipoTarea, cantidadCriterios, cantidadNiveles, puntajeMaximo);
    }

    private String construirPromptRubrica(String tema, String nivel, String asignatura, String tipoTarea,
                                          int cantidadCriterios, int cantidadNiveles, int puntajeMaximo) {
        return String.format(
            "Eres un experto en evaluacion educativa. Genera una rubrica completa y devuelve SOLO JSON valido, "
                + "sin markdown ni texto adicional. "
                + "Si no puedes completar un campo, usa un valor razonable por defecto. "
                + "Usa este esquema exacto: %s. "
                + "Datos: tema='%s', nivelEducativo='%s', asignatura='%s', tipoTarea='%s', "
                + "cantidadCriterios=%d, cantidadNiveles=%d, puntajeMaximo=%d.",
            ESQUEMA_RUBRICA, tema, nivel, asignatura, tipoTarea, cantidadCriterios, cantidadNiveles, puntajeMaximo
        );
    }

    public RubricaGeneradaDto generarRubricaConCohere(String prompt) {
        try {
            String respuesta = cohereServicio.generarTexto(prompt);
            RubricaGeneradaDto parsed = parsearJsonIa(respuesta, RubricaGeneradaDto.class);
            // Cohere no incluye generadaPorIa ni modelo en su JSON de salida;
            // hay que fijarlos explícitamente después de parsear.
            return new RubricaGeneradaDto(
                    parsed.titulo(),
                    parsed.descripcion(),
                    parsed.tema(),
                    parsed.nivelEducativo(),
                    parsed.asignatura(),
                    parsed.tipoTarea(),
                    parsed.puntajeMaximo(),
                    parsed.criterios(),
                    true,
                    cohereServicio.getModelo(),
                    Instant.now()
            );
        } catch (Exception e) {
            LOGGER.warn("Error parseando respuesta de Cohere: {}", extraerMensajeRaiz(e));
            throw new IllegalStateException("No se pudo generar o parsear la rúbrica con Cohere", e);
        }
    }

    // ==========================================
    // SÍLABOS EXCLUSIVOS CON COHERE
    // ==========================================
    @Transactional
    public SilaboGeneradoDto generarSilabo(SilaboGeneracionRequestDto request) {
        if (request == null || ((request.nombreCurso() == null || request.nombreCurso().isBlank())
                && (request.cursoId() == null || request.cursoId().isBlank()))) {
            throw new IllegalArgumentException("Debes enviar nombreCurso o cursoId para generar el sílabo");
        }

        String nombreCurso = request.nombreCurso();
        if ((nombreCurso == null || nombreCurso.isBlank()) && request.cursoId() != null && !request.cursoId().isBlank()) {
            nombreCurso = cursoRepositorio.findById(request.cursoId())
                    .map(Curso::getNombre)
                    .orElse("Curso sin nombre");
        }

        int semanas = normalizarEntero(request.semanas(), 16, 4, 20);
        int ciclo = request.ciclo() <= 0 ? 1 : request.ciclo();
        int creditos = request.creditos() <= 0 ? 4 : request.creditos();

        if (cohereServicio.estaConfigurado()) {
            try {
                String prompt = String.format(
                    "Eres un director academico experto en diseno curricular. Genera un silabo completo y devuelve SOLO JSON valido, sin markdown ni texto adicional. "
                        + "Si falta algun dato, completa con una propuesta razonable. "
                        + "Usa este esquema exacto: %s. "
                        + "Datos: curso='%s', carrera='%s', ciclo=%d, creditos=%d, total_semanas=%d, descripcion='%s'. "
                        + "REGLA CRÍTICA Y OBLIGATORIA: El curso dura exactamente %d semanas. DEBES generar un desglose detallado para EXACTAMENTE %d semanas en el JSON. "
                        + "Bajo ninguna circunstancia puedes generar menos semanas. No resumas, no omitas, ni saltes números. Escribe desde la semana 1 hasta la semana %d.",
                    ESQUEMA_SILABO,
                    nombreCurso,
                    valorPorDefecto(request.carrera(), "Ingenieria"),
                    ciclo,
                    creditos,
                    semanas,
                    valorPorDefecto(request.descripcionBreve(), ""),
                    semanas, // Se inyecta para la regla crítica
                    semanas, // Se inyecta para la regla crítica
                    semanas  // Se inyecta para la regla crítica
                );
                String respuesta = cohereServicio.generarTexto(prompt);
                SilaboGeneradoDto silaboGenerado = parsearJsonIa(respuesta, SilaboGeneradoDto.class);
                guardarSilaboGenerado(request, silaboGenerado);
                return silaboGenerado;
            } catch (Exception e) {
                LOGGER.warn("Error generando sílabo con Cohere: {}. Usando fallback local.", extraerMensajeRaiz(e));
            }
        } else {
             LOGGER.info("CohereServicio no disponible o no configurado, usando fallback local para sílabo");
        }

        // Fallback local (Se ejecuta si Cohere falla o no está configurado)
        SilaboGeneradoDto silaboFallback = construirSilaboFallback(
                nombreCurso,
                request.carrera(),
                ciclo,
                creditos,
                semanas,
                request.descripcionBreve()
        );
        try {
            guardarSilaboGenerado(request, silaboFallback);
        } catch (Exception persistEx) {
            LOGGER.warn("Error guardando sílabo fallback: {}", extraerMensajeRaiz(persistEx));
        }
        return silaboFallback;
    }

    private void guardarSilaboGenerado(SilaboGeneracionRequestDto request, SilaboGeneradoDto silaboGenerado) {
        Curso curso = resolverCursoParaSilabo(request);
        Silabo silabo = silaboRepositorio.findByCursoId(curso.getId()).orElseGet(Silabo::new);

        silabo.setCurso(curso);
        silabo.setSumilla(silaboGenerado.sumilla());
        silabo.setLogroCurso(silaboGenerado.logroCurso());
        silabo.setSistemaEvaluacion(silaboGenerado.sistemaEvaluacion());
        silabo.setVersion("IA-local-" + Instant.now());
        silabo.setFechaAprobacion(new Date());

        try {
            silabo.setCompetenciasGenerales(objectMapper.writeValueAsString(silaboGenerado.competenciasGenerales()));
            silabo.setCompetenciasEspecificas(objectMapper.writeValueAsString(silaboGenerado.competenciasEspecificas()));
            silabo.setContenidoSemanal(objectMapper.writeValueAsString(silaboGenerado.unidades()));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo serializar el contenido del sílabo para guardar en base de datos", ex);
        }

        silaboRepositorio.save(silabo);
    }

    private Curso resolverCursoParaSilabo(SilaboGeneracionRequestDto request) {
        if (request.cursoId() != null && !request.cursoId().isBlank()) {
            return cursoRepositorio.findById(request.cursoId())
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el curso para guardar el sílabo (cursoId inválido)"));
        }

        if (request.nombreCurso() == null || request.nombreCurso().isBlank()) {
            throw new IllegalArgumentException("No se puede asociar el sílabo: envia cursoId o nombreCurso válido");
        }

        return cursoRepositorio.findFirstByNombreIgnoreCase(request.nombreCurso().trim())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un curso con ese nombre para guardar el sílabo. Envia cursoId en la solicitud."));
    }

    private String extraerMensajeRaiz(Throwable throwable) {
        if (throwable == null) {
            return "Sin detalle";
        }
        Throwable actual = throwable;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        if (actual.getMessage() == null || actual.getMessage().isBlank()) {
            return actual.getClass().getSimpleName();
        }
        return actual.getMessage();
    }

    private SilaboGeneradoDto construirSilaboFallback(
            String curso,
            String carrera,
            int ciclo,
            int creditos,
            int semanas,
            String descripcion
    ) {
        String carreraValor = valorPorDefecto(carrera, "Ingenieria");
        String descripcionValor = valorPorDefecto(descripcion, "Curso orientado al desarrollo progresivo de competencias teorico-practicas.");
        int cantidadUnidades = semanas >= 12 ? 4 : 3;

        List<UnidadSilaboDto> unidades = new ArrayList<>();
        for (int unidadIndex = 0; unidadIndex < cantidadUnidades; unidadIndex++) {
            List<SemanaSilaboDto> semanasUnidad = new ArrayList<>();
            for (int numeroSemana = unidadIndex + 1; numeroSemana <= semanas; numeroSemana += cantidadUnidades) {
                semanasUnidad.add(new SemanaSilaboDto(
                        numeroSemana,
                        "Desarrollo de contenidos clave de la Unidad " + (unidadIndex + 1),
                        "Practicas aplicadas, analisis de casos y trabajo colaborativo",
                        (numeroSemana % 4 == 0)
                                ? "Evaluacion continua " + (numeroSemana / 4)
                                : "Seguimiento formativo"
                ));
            }

            unidades.add(new UnidadSilaboDto(
                    "Unidad " + (unidadIndex + 1),
                    "Al finalizar la unidad, el estudiante aplica conceptos y herramientas de forma pertinente.",
                    semanasUnidad
            ));
        }

        return new SilaboGeneradoDto(
                new InformacionGeneralDto(curso, carreraValor, ciclo, creditos),
                List.of(
                        "Analiza fundamentos y principios de la disciplina",
                        "Resuelve problemas usando criterios tecnicos y eticos",
                        "Comunica resultados de forma clara y estructurada"
                ),
                List.of(
                        "Aplica procedimientos propios del curso en contextos reales",
                        "Integra teoria y practica para tomar decisiones academicas",
                        "Construye productos o evidencias con estandares de calidad"
                ),
                descripcionValor,
                "Al finalizar el curso, el estudiante demuestra dominio conceptual y practico en los ejes del curso.",
                unidades,
                "La calificacion integra evaluaciones diagnosticas, formativas y sumativas durante el periodo academico."
        );
    }

    private String construirPromptSistema(String rol, Usuario usuario) {
        String nombreUsuario = (usuario.getNombre() != null && !usuario.getNombre().isBlank())
                ? usuario.getNombre() : "usuario";
        String contextoDatos = construirContextoDatosUsuario(rol, usuario);

        String base = String.format(
            "Eres GCI-Asistente, el asistente inteligente del Sistema de Gestion de Inscripcion de Cursos (GCI+). "
            + "El usuario conectado se llama '%s' y tiene el rol %s. "
            + "Responde SIEMPRE en espanol, de forma clara, concisa y amigable. "
            + "Usa viñetas o numeracion para listas. "
            + "No inventes datos que no esten en el contexto adjunto; si no tienes un dato, dilo. "
            + "Recuerda el contexto de la conversacion anterior para dar respuestas coherentes y personalizadas.",
            nombreUsuario, rol
        );

        String instruccionesRol = switch (rol) {
            case "ADMIN" ->
                " Como ADMIN puedes: gestionar usuarios, aprobar/rechazar inscripciones, "
                + "administrar cursos y carreras, generar reportes y ver estadisticas. "
                + "Sugiere acciones concretas del panel administrativo cuando corresponda.";
            case "PROFESOR" ->
                " Como PROFESOR puedes: ver tus cursos asignados, crear y gestionar actividades, "
                + "evaluar alumnos, generar rubricas con IA, crear silabos y acceder al chat de aula. "
                + "Da instrucciones claras sobre como usar las herramientas del sistema.";
            case "ALUMNO" ->
                " Como ALUMNO puedes: ver tus cursos inscritos, consultar actividades pendientes, "
                + "inscribirte en cursos disponibles, ver tu progreso academico y acceder al chat de aula. "
                + "Orienta al alumno sobre sus proximos pasos y oportunidades academicas.";
            default ->
                " Brinda orientacion general sobre las funciones del sistema segun el perfil del usuario.";
        };

        return base + instruccionesRol + "\n\n" + contextoDatos;
    }

    private void validarEntrada(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacio");
        }
        if (mensaje.length() > 4000) {
            throw new IllegalArgumentException("El mensaje excede el maximo permitido");
        }
    }

    private String construirRespuestaFallback(String rol, String mensaje, Usuario usuario) {
        String contextoDatos = construirContextoDatosUsuario(rol, usuario);
        String recomendacion = switch (rol) {
            case "ADMIN" -> "Puedes revisar gestion de usuarios, cursos y reportes desde el panel administrativo.";
            case "PROFESOR" -> "Puedes revisar cursos, actividades, evaluacion y seguimiento academico del aula.";
            case "ALUMNO" -> "Puedes revisar tus inscripciones, actividades pendientes, requisitos y progreso academico.";
            default -> "Puedes revisar las opciones disponibles dentro del sistema segun tu perfil.";
        };

        return "En este momento el asistente IA externo no esta disponible. "
                + "Recibi tu consulta: '" + mensaje.trim() + "'. "
                + recomendacion
                + " Si deseas, intenta nuevamente en unos minutos.\n\n"
                + contextoDatos;
    }

    private String construirContextoDatosUsuario(String rol, Usuario usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getId().isBlank()) {
            return "Contexto de datos: no se pudo resolver el usuario autenticado.";
        }

        String idUsuario = usuario.getId();

        try {
            switch (rol) {
                case "ADMIN": {
                    List<Curso> cursosActivos = cursoRepositorio.buscarCursosActivos(new Date());
                    long totalInscripciones = inscripcionRepositorio.count();
                    int pendientesAlumno = inscripcionRepositorio.listarPendientesAlumno().size();
                    int pendientesProfesor = inscripcionRepositorio.listarPendientesProfesor().size();

                    return "Contexto de datos para ADMIN:\n"
                            + "- Cursos activos: " + cursosActivos.size() + "\n"
                            + "- Total de inscripciones: " + totalInscripciones + "\n"
                            + "- Inscripciones pendientes de alumnos: " + pendientesAlumno + "\n"
                            + "- Inscripciones pendientes de profesores: " + pendientesProfesor + "\n"
                            + "- Cursos activos (muestra): " + resumirCursos(cursosActivos, 6);
                }
                case "PROFESOR": {
                    List<Curso> cursosInscritos = cursoRepositorio.buscarCursosInscritosProfesor(idUsuario);
                    List<Curso> cursosDisponibles = cursoRepositorio.buscarCursosDisponiblesProfesor(idUsuario);

                    return "Contexto de datos para PROFESOR:\n"
                            + "- Cursos inscritos/aprobados: " + cursosInscritos.size() + "\n"
                            + "- Cursos disponibles para inscribirse: " + cursosDisponibles.size() + "\n"
                            + "- Inscritos (muestra): " + resumirCursos(cursosInscritos, 6) + "\n"
                            + "- Disponibles (muestra): " + resumirCursos(cursosDisponibles, 6);
                }
                case "ALUMNO": {
                    List<Curso> cursosInscritos = cursoRepositorio.buscarCursosInscritosAlumno(idUsuario);
                    List<Curso> cursosDisponibles = cursoRepositorio.buscarCursosDisponiblesAlumno(idUsuario);

                    return "Contexto de datos para ALUMNO:\n"
                            + "- Cursos inscritos/aprobados: " + cursosInscritos.size() + "\n"
                            + "- Cursos disponibles para inscribirse: " + cursosDisponibles.size() + "\n"
                            + "- Inscritos (muestra): " + resumirCursos(cursosInscritos, 6) + "\n"
                            + "- Disponibles (muestra): " + resumirCursos(cursosDisponibles, 6);
                }
                default:
                    return "Contexto de datos: rol no reconocido; solo se puede brindar orientacion general.";
            }
        } catch (Exception ex) {
            LOGGER.warn("No se pudo construir el contexto de datos para rol {}: {}", rol, ex.getMessage());
            return "Contexto de datos: no disponible temporalmente por un error interno al consultar cursos/inscripciones.";
        }
    }

    private String resumirCursos(List<Curso> cursos, int limite) {
        if (cursos == null || cursos.isEmpty()) {
            return "sin cursos";
        }

        List<String> resumen = new ArrayList<>();
        int maximo = Math.min(limite, cursos.size());
        for (int i = 0; i < maximo; i++) {
            Curso curso = cursos.get(i);
            if (curso == null) {
                continue;
            }

            String nombre = valorPorDefecto(curso.getNombre(), "Curso");
            String codigo = valorPorDefecto(curso.getCodigoCurso(), "sin-codigo");
            resumen.add(nombre + " (" + codigo + ")");
        }

        if (resumen.isEmpty()) {
            return "sin cursos";
        }

        if (cursos.size() > limite) {
            return String.join(", ", resumen) + ", ...";
        }
        return String.join(", ", resumen);
    }

    private RubricaGeneradaDto construirRubricaFallback(
            String tema,
            String nivel,
            String asignatura,
            String tipoTarea,
            int cantidadCriterios,
            int cantidadNiveles,
            int puntajeMaximo
    ) {
        List<String> nombresNivelesBase = List.of("Inicial", "En proceso", "Logrado", "Sobresaliente", "Experto");

        List<CriterioRubricaDto> criterios = new ArrayList<>();
        int pesoBase = Math.max(5, 100 / cantidadCriterios);

        for (int i = 1; i <= cantidadCriterios; i++) {
            List<NivelRubricaDto> niveles = new ArrayList<>();
            for (int j = 0; j < cantidadNiveles; j++) {
                int puntaje = (int) Math.round((double) puntajeMaximo * (j + 1) / cantidadNiveles / cantidadCriterios);
                String nombreNivel = nombresNivelesBase.get(Math.min(j, nombresNivelesBase.size() - 1));
                niveles.add(new NivelRubricaDto(
                        nombreNivel,
                        puntaje,
                        "El estudiante demuestra " + nombreNivel.toLowerCase()
                                + " en el criterio " + i + " del tema '" + tema + "'."
                ));
            }

            criterios.add(new CriterioRubricaDto(
                    "Criterio " + i,
                    "Evalua el desempeno del estudiante en un aspecto clave de la tarea.",
                    pesoBase,
                    niveles
            ));
        }

        RubricaGeneradaDto rubrica = new RubricaGeneradaDto(
                "Rubrica generica para " + tema,
                "Rubrica generada en modo fallback, editable para personalizacion.",
                tema,
                nivel,
                asignatura,
                tipoTarea,
                puntajeMaximo,
                criterios,
                false,
                "fallback-local",
                Instant.now()
        );

        return normalizarRubricaSinRepeticiones(rubrica);
    }

    private RubricaGeneradaDto normalizarRubricaSinRepeticiones(RubricaGeneradaDto rubrica) {
        if (rubrica == null || rubrica.criterios() == null || rubrica.criterios().isEmpty()) {
            return rubrica;
        }

        Map<String, CriterioRubricaDto> unicosPorNombre = new LinkedHashMap<>();

        for (CriterioRubricaDto criterio : rubrica.criterios()) {
            if (criterio == null) {
                continue;
            }

            String nombreBase = valorPorDefecto(criterio.nombre(), "Criterio").trim();
            String llave = nombreBase.toLowerCase();

            if (!unicosPorNombre.containsKey(llave)) {
                unicosPorNombre.put(llave, new CriterioRubricaDto(
                        nombreBase,
                        valorPorDefecto(criterio.descripcion(), "Evaluacion del criterio"),
                        criterio.peso() == null ? 0 : criterio.peso(),
                        normalizarNivelesSinRepeticiones(criterio.niveles())
                ));
            }
        }

        List<CriterioRubricaDto> criteriosNormalizados = new ArrayList<>(unicosPorNombre.values());

        int cantidad = criteriosNormalizados.size();
        if (cantidad == 0) {
            return rubrica;
        }

        int pesoBase = 100 / cantidad;
        int resto = 100 % cantidad;

        List<CriterioRubricaDto> criteriosConPesoRebalanceado = new ArrayList<>();
        for (int i = 0; i < criteriosNormalizados.size(); i++) {
            CriterioRubricaDto c = criteriosNormalizados.get(i);
            int peso = pesoBase + (i < resto ? 1 : 0);
            criteriosConPesoRebalanceado.add(new CriterioRubricaDto(
                    c.nombre(),
                    c.descripcion(),
                    peso,
                    c.niveles()
            ));
        }

        return new RubricaGeneradaDto(
                rubrica.titulo(),
                rubrica.descripcion(),
                rubrica.tema(),
                rubrica.nivelEducativo(),
                rubrica.asignatura(),
                rubrica.tipoTarea(),
                rubrica.puntajeMaximo(),
                criteriosConPesoRebalanceado,
                rubrica.generadaPorIa(),
                rubrica.modelo(),
                rubrica.fechaGeneracion()
        );
    }

    private List<NivelRubricaDto> normalizarNivelesSinRepeticiones(List<NivelRubricaDto> niveles) {
        if (niveles == null || niveles.isEmpty()) {
            return niveles;
        }

        Map<String, NivelRubricaDto> unicosPorNombre = new LinkedHashMap<>();
        int fallbackIndex = 1;

        for (NivelRubricaDto nivel : niveles) {
            if (nivel == null) {
                continue;
            }

            String nombreBase = valorPorDefecto(nivel.nombre(), "Nivel " + fallbackIndex).trim();
            String llave = nombreBase.toLowerCase();
            if (unicosPorNombre.containsKey(llave)) {
                fallbackIndex++;
                continue;
            }

            String descriptor = valorPorDefecto(nivel.descriptor(), "Descripcion del nivel").trim();
            int puntaje = nivel.puntaje() == null ? 0 : nivel.puntaje();

            unicosPorNombre.put(llave, new NivelRubricaDto(nombreBase, puntaje, descriptor));
            fallbackIndex++;
        }

        List<NivelRubricaDto> normalizados = new ArrayList<>(unicosPorNombre.values());

        for (int i = 0; i < normalizados.size(); i++) {
            NivelRubricaDto actual = normalizados.get(i);
            String nombre = actual.nombre();
            String descriptor = actual.descriptor();

            if (descriptor != null && descriptor.equalsIgnoreCase("Descripcion del nivel")) {
                descriptor = "Desempeno esperado para " + nombre + ".";
            }

            normalizados.set(i, new NivelRubricaDto(nombre, actual.puntaje(), descriptor));
        }

        return normalizados;
    }

    private String valorPorDefecto(String valor, String fallback) {
        if (valor == null || valor.isBlank()) {
            return fallback;
        }
        return valor.trim();
    }

    private int normalizarEntero(Integer valor, int fallback, int min, int max) {
        int v = valor == null ? fallback : valor;
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    private <T> T parsearJsonIa(String respuestaIa, Class<T> tipo) throws IOException {
        String json = extraerJsonValido(respuestaIa);
        return objectMapper.readValue(json, tipo);
    }

    private String extraerJsonValido(String respuestaIa) {
        if (respuestaIa == null || respuestaIa.isBlank()) {
            throw new IllegalStateException("La IA devolvio una respuesta vacia");
        }

        String texto = respuestaIa.trim();
        if (texto.startsWith("```") && texto.contains("\n")) {
            int primerSalto = texto.indexOf('\n');
            if (primerSalto >= 0 && primerSalto + 1 < texto.length()) {
                texto = texto.substring(primerSalto + 1).trim();
            }
            if (texto.endsWith("```")) {
                texto = texto.substring(0, texto.length() - 3).trim();
            }
        }

        int inicioObjeto = texto.indexOf('{');
        int finObjeto = texto.lastIndexOf('}');
        if (inicioObjeto >= 0 && finObjeto > inicioObjeto) {
            return texto.substring(inicioObjeto, finObjeto + 1).trim();
        }

        throw new IllegalStateException("No se encontro JSON valido en la respuesta de IA");
    }

    // ==========================================
    // METODO PARA EL CHAT CON GROQ
    // ==========================================
    private String llamarGroq(String promptSistema, String mensajeUsuario, List<Map<String, Object>> historialPrevio) {
        try {
            List<Map<String, Object>> mensajes = new ArrayList<>();

            Map<String, Object> mensajeDelSistema = new LinkedHashMap<>();
            mensajeDelSistema.put("role", "system");
            mensajeDelSistema.put("content", promptSistema);
            mensajes.add(mensajeDelSistema);

            if (historialPrevio != null && !historialPrevio.isEmpty()) {
                mensajes.addAll(historialPrevio);
            }

            Map<String, Object> mensajeDelUsuario = new LinkedHashMap<>();
            mensajeDelUsuario.put("role", "user");
            mensajeDelUsuario.put("content", mensajeUsuario);
            mensajes.add(mensajeDelUsuario);

            Map<String, Object> cuerpoRequest = new LinkedHashMap<>();
            cuerpoRequest.put("model", modeloPorDefecto);
            cuerpoRequest.put("messages", mensajes);
            cuerpoRequest.put("temperature", 0.7);
            cuerpoRequest.put("max_tokens", 2048);

            String jsonCuerpo = objectMapper.writeValueAsString(cuerpoRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_GROQ))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonCuerpo))
                    .build();

            HttpResponse<String> respuesta = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() != 200) {
                LOGGER.error("Error en llamada a Groq. Status: {}, Body: {}", respuesta.statusCode(), respuesta.body());
                throw new IllegalStateException("Groq respondió con status " + respuesta.statusCode());
            }

            JsonNode raiz = objectMapper.readTree(respuesta.body());
            JsonNode choices = raiz.path("choices");
            
            if (choices.isEmpty()) {
                throw new IllegalStateException("No se encontraron resultados en la respuesta de Groq");
            }
            
            JsonNode contenido = choices.get(0).path("message").path("content");

            if (contenido.isNull() || contenido.isMissingNode() || contenido.asText().isBlank()) {
                throw new IllegalStateException("Respuesta vacía de Groq");
            }

            return contenido.asText();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("Comunicación con Groq interrumpida");
            throw new IllegalStateException("Comunicación con Groq interrumpida", ex);
        } catch (IOException ex) {
            LOGGER.error("Error comunicando con Groq: {}", extraerMensajeRaiz(ex));
            throw new IllegalStateException("No se pudo comunicar con Groq", ex);
        }
    }
}