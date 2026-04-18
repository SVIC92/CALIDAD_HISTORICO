package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.IaChatResponseDto;
import com.GestionInscripcionCursos.dto.IaHistorialDto;
import com.GestionInscripcionCursos.dto.CriterioRubricaDto;
import com.GestionInscripcionCursos.dto.NivelRubricaDto;
import com.GestionInscripcionCursos.dto.RubricaGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.dto.SilaboGeneracionRequestDto;
import com.GestionInscripcionCursos.dto.SilaboGeneradoDto;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.IaHistorial;
import com.GestionInscripcionCursos.entidades.Silabo;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.IaHistorialRepositorio;
import com.GestionInscripcionCursos.repositorios.SilaboRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
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
    private static final String URL_GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final String geminiApiKey;
    private final String geminiModelo;

    private final UsuarioServicio usuarioServicio;
    private final IaHistorialRepositorio iaHistorialRepositorio;
    private final CursoRepositorio cursoRepositorio;
    private final SilaboRepositorio silaboRepositorio;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String groqApiKey;
    private final String modeloPorDefecto;

    public IaServicio(
            UsuarioServicio usuarioServicio,
            IaHistorialRepositorio iaHistorialRepositorio,
            CursoRepositorio cursoRepositorio,
            SilaboRepositorio silaboRepositorio,
            ObjectMapper objectMapper,
            @Value("${groq.api.key:}") String groqApiKey,
            @Value("${groq.model:llama-3.1-8b-instant}") String modeloPorDefecto,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.model:gemini-1.5-flash}") String geminiModelo
    ) {
        this.usuarioServicio = usuarioServicio;
        this.iaHistorialRepositorio = iaHistorialRepositorio;
        this.cursoRepositorio = cursoRepositorio;
        this.silaboRepositorio = silaboRepositorio;
        this.objectMapper = objectMapper;
        this.groqApiKey = groqApiKey;
        this.modeloPorDefecto = modeloPorDefecto;
        this.geminiApiKey = geminiApiKey;
        this.geminiModelo = geminiModelo;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Transactional
    public IaChatResponseDto chatearSegunRol(String email, String rol, String mensaje) {
        validarEntrada(mensaje);

        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new IllegalStateException("No se encontro la API key de Groq. Configura GROQ_API_KEY.");
        }

        Usuario usuario = usuarioServicio.buscarEmail(email);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        String promptSistema = construirPromptSistema(rol);
        String respuesta = llamarGroq(promptSistema, mensaje);

        IaHistorial historial = iaHistorialRepositorio.findByUsuario(usuario).orElseGet(IaHistorial::new);
        historial.setUsuario(usuario);
        historial.setUltimoMensaje(mensaje);
        historial.setUltimaRespuesta(respuesta);
        historial.setRol(rol);
        historial.setModelo(modeloPorDefecto);
        historial.setFechaActualizacion(Instant.now());
        iaHistorialRepositorio.save(historial);

        return new IaChatResponseDto(respuesta, rol, modeloPorDefecto, Instant.now());
    }

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

    public RubricaGeneradaDto generarRubrica(String email, RubricaGeneracionRequestDto request) {
        Usuario usuario = usuarioServicio.buscarEmail(email);
        if (usuario == null) {
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

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return construirRubricaFallback(tema, nivel, asignatura, tipoTarea, cantidadCriterios, cantidadNiveles, puntajeMaximo);
        }

        try {
            // Llamar a Gemini en lugar de Groq
            String jsonRubrica = llamarGeminiParaRubrica(tema, nivel, asignatura, tipoTarea, cantidadCriterios, cantidadNiveles, puntajeMaximo);
            
            RubricaGeneradaDto rubrica = parsearRubricaDesdeJson(jsonRubrica, tema, nivel, asignatura, tipoTarea, puntajeMaximo);
            
            if (rubrica == null || rubrica.criterios() == null || rubrica.criterios().isEmpty()) {
                return construirRubricaFallback(tema, nivel, asignatura, tipoTarea, cantidadCriterios, cantidadNiveles, puntajeMaximo);
            }
            return rubrica;
        } catch (Exception ex) {
            return construirRubricaFallback(tema, nivel, asignatura, tipoTarea, cantidadCriterios, cantidadNiveles, puntajeMaximo);
        }
    }

    private String llamarGroq(String promptSistema, String mensajeUsuario) {
        try {
            Map<String, Object> payload = Map.of(
                    "model", modeloPorDefecto,
                    "temperature", 0.3,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", promptSistema),
                            Map.of("role", "user", "content", mensajeUsuario)
                    }
            );

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_GROQ))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Groq devolvio error: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("Respuesta de Groq sin contenido util");
            }

            String texto = choices.get(0).path("message").path("content").asText();
            if (texto == null || texto.isBlank()) {
                throw new IllegalStateException("Respuesta de Groq vacia");
            }
            return texto.trim();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("No se pudo completar la consulta a Groq", e);
        }
    }

    private String llamarGeminiParaRubrica(String tema, String nivel, String asignatura, String tipoTarea,int cantidadCriterios, int cantidadNiveles, int puntajeMaximo) throws IOException, InterruptedException 
    {
        
        String promptSistema = "Eres un experto en evaluacion educativa. Debes generar una rubrica completa y coherente.";
        
        String mensajeUsuario = "Genera una rubrica con estos datos: "
                + "tema='" + tema + "', nivelEducativo='" + nivel + "', asignatura='" + asignatura + "', tipoTarea='" + tipoTarea + "'. "
                + "Cantidad de criterios=" + cantidadCriterios + ", niveles por criterio=" + cantidadNiveles + ", puntaje maximo total=" + puntajeMaximo + ". "
                + "Devuelve estrictamente este esquema JSON: "
                + "{"
                + "\"titulo\":\"...\"," 
                + "\"descripcion\":\"...\"," 
                + "\"tema\":\"...\"," 
                + "\"nivelEducativo\":\"...\"," 
                + "\"asignatura\":\"...\"," 
                + "\"tipoTarea\":\"...\"," 
                + "\"puntajeMaximo\":numero," 
                + "\"criterios\":[{\"nombre\":\"...\",\"descripcion\":\"...\",\"peso\":numero,\"niveles\":[{\"nombre\":\"...\",\"puntaje\":numero,\"descriptor\":\"...\"}]}]"
                + "}. "
                + "Asegura que la suma de pesos de criterios sea 100.";

        // Estructura específica para Gemini
        Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of(
                        "parts", Map.of("text", promptSistema)
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", mensajeUsuario)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json" // <-- ESTO ES CLAVE
                )
        );

        String body = objectMapper.writeValueAsString(payload);
        String urlConApiKey = String.format(URL_GEMINI_BASE, geminiModelo, geminiApiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlConApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Gemini devolvió error: " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Respuesta de Gemini sin contenido útil");
        }

        // Navegar por la estructura de respuesta de Gemini
        String textoJson = candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        if (textoJson == null || textoJson.isBlank()) {
            throw new IllegalStateException("Respuesta de Gemini vacía");
        }

        // Ya no necesitas limpiar la respuesta con regex porque Gemini garantiza un JSON puro
        return textoJson;
    }

    @Transactional
    public SilaboGeneradoDto generarSilabo(SilaboGeneracionRequestDto request) {
        if (request == null || ((request.nombreCurso() == null || request.nombreCurso().isBlank())
                && (request.cursoId() == null || request.cursoId().isBlank()))) {
            throw new IllegalArgumentException("Debes enviar nombreCurso o cursoId para generar el sílabo");
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("No se encontro la API key de Gemini. Configura GEMINI_API_KEY.");
        }

        String nombreCurso = request.nombreCurso();
        if ((nombreCurso == null || nombreCurso.isBlank()) && request.cursoId() != null && !request.cursoId().isBlank()) {
            nombreCurso = cursoRepositorio.findById(request.cursoId())
                    .map(Curso::getNombre)
                    .orElse("Curso sin nombre");
        }

        int semanas = normalizarEntero(request.semanas(), 16, 4, 20); // Por defecto 16 semanas

        try {
            String jsonSilabo = llamarGeminiParaSilabo(
                    nombreCurso,
                    request.carrera(), 
                    request.ciclo(), 
                    request.creditos(), 
                    semanas, 
                    request.descripcionBreve()
            );
            
            // Usamos ObjectMapper para convertir el JSON puro de Gemini a nuestro DTO
            SilaboGeneradoDto silaboGenerado = objectMapper.readValue(jsonSilabo, SilaboGeneradoDto.class);
            try {
                guardarSilaboGenerado(request, silaboGenerado);
            } catch (Exception persistEx) {
                LOGGER.warn("No se pudo persistir el sílabo generado por IA: {}", persistEx.getMessage());
            }
            return silaboGenerado;
            
        } catch (Exception ex) {
            throw new IllegalStateException("Error al generar el sílabo con IA", ex);
        }
    }

    private void guardarSilaboGenerado(SilaboGeneracionRequestDto request, SilaboGeneradoDto silaboGenerado) {
        Curso curso = resolverCursoParaSilabo(request);
        Silabo silabo = silaboRepositorio.findByCursoId(curso.getId()).orElseGet(Silabo::new);

        silabo.setCurso(curso);
        silabo.setSumilla(silaboGenerado.sumilla());
        silabo.setLogroCurso(silaboGenerado.logroCurso());
        silabo.setSistemaEvaluacion(silaboGenerado.sistemaEvaluacion());
        silabo.setVersion("IA-" + geminiModelo + "-" + Instant.now().toString());
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

    private String llamarGeminiParaSilabo(
            String curso, String carrera, int ciclo, int creditos, int semanas, String descripcion
    ) throws IOException, InterruptedException {
        
        String promptSistema = "Eres un director académico experto en diseño curricular universitario. "
                + "Tu objetivo es diseñar un sílabo estructurado, coherente y moderno.";

        String esquemaJson = "{"
                + "\"informacionGeneral\": {\"curso\":\"...\",\"carrera\":\"...\",\"ciclo\":0,\"creditos\":0},"
                + "\"competenciasGenerales\": [\"...\"],"
                + "\"competenciasEspecificas\": [\"...\"],"
                + "\"sumilla\": \"Resumen del curso...\","
                + "\"logroCurso\": \"Al finalizar el curso, el estudiante...\","
                + "\"unidades\": [{"
                + "  \"tituloUnidad\": \"Unidad I: ...\","
                + "  \"logroUnidad\": \"Al finalizar la unidad...\","
                + "  \"semanas\": [{"
                + "    \"numeroSemana\": 1,"
                + "    \"temas\": \"Saberes teóricos\","
                + "    \"actividadesPracticas\": \"Práctica de laboratorio o campo\","
                + "    \"evaluacion\": \"Ej: Evaluación Continua 1\""
                + "  }]"
                + "}],"
                + "\"sistemaEvaluacion\": \"Descripción breve del sistema de calificación\""
                + "}";

        String mensajeUsuario = String.format(
                "Genera un sílabo universitario para el curso '%s' de la carrera '%s', "
                + "ciclo %d, de %d créditos. El curso debe durar exactamente %d semanas. "
                + "Descripción general del curso: %s. "
                + "Distribuye las %d semanas equitativamente en 3 o 4 Unidades de Aprendizaje. "
                + "Debes devolver ESTRICTAMENTE la respuesta bajo este esquema JSON exacto: %s",
                curso, valorPorDefecto(carrera, "Ingeniería"), ciclo, creditos, semanas, 
                valorPorDefecto(descripcion, "Fundamentos teóricos y prácticos"), semanas, esquemaJson
        );

        Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of("parts", Map.of("text", promptSistema)),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", mensajeUsuario)))),
                "generationConfig", Map.of(
                        "temperature", 0.3, 
                        "responseMimeType", "application/json"
                )
        );

        String body = objectMapper.writeValueAsString(payload);
        String urlConApiKey = String.format(URL_GEMINI_BASE, geminiModelo, geminiApiKey); 

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlConApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Gemini devolvió error: " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
    }

    private String construirPromptSistema(String rol) {
        String base = "Eres un asistente del sistema de Gestion de Inscripcion de Cursos. "
                + "Responde de forma clara y breve en espanol. "
                + "No inventes datos de base de datos. Si no hay datos suficientes, dilo."
                + "Siempre saludar e indicar el rol.";

        return switch (rol) {
            case "ADMIN" -> base + " Puedes sugerir acciones administrativas, gestion de usuarios, cursos y reportes.";
            case "PROFESOR" -> base + " Solo puedes orientar sobre cursos, actividades, evaluacion y seguimiento academico.";
            case "ALUMNO" -> base + " Solo puedes orientar sobre inscripciones, actividades, requisitos y progreso del alumno.";
            default -> base + " Si el rol no es reconocido, limita la respuesta a orientacion general del sistema.";
        };
    }

    private void validarEntrada(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacio");
        }
        if (mensaje.length() > 4000) {
            throw new IllegalArgumentException("El mensaje excede el maximo permitido");
        }
    }

    private RubricaGeneradaDto parsearRubricaDesdeJson(
            String contenido,
            String temaDefault,
            String nivelDefault,
            String asignaturaDefault,
            String tipoTareaDefault,
            int puntajeMaximoDefault
    ) throws IOException {
        JsonNode root = objectMapper.readTree(contenido);

        String titulo = texto(root, "titulo", "Rubrica de evaluacion");
        String descripcion = texto(root, "descripcion", "Rubrica generada automaticamente para evaluar desempeno.");
        String tema = texto(root, "tema", temaDefault);
        String nivel = texto(root, "nivelEducativo", nivelDefault);
        String asignatura = texto(root, "asignatura", asignaturaDefault);
        String tipoTarea = texto(root, "tipoTarea", tipoTareaDefault);
        int puntajeMaximo = numero(root, "puntajeMaximo", puntajeMaximoDefault);

        List<CriterioRubricaDto> criterios = new ArrayList<>();
        JsonNode criteriosNode = root.path("criterios");
        if (criteriosNode.isArray()) {
            for (JsonNode criterioNode : criteriosNode) {
                String nombre = texto(criterioNode, "nombre", "Criterio");
                String descripcionCriterio = texto(criterioNode, "descripcion", "Evaluacion del criterio");
                int peso = numero(criterioNode, "peso", 25);

                List<NivelRubricaDto> niveles = new ArrayList<>();
                JsonNode nivelesNode = criterioNode.path("niveles");
                if (nivelesNode.isArray()) {
                    for (JsonNode nivelNode : nivelesNode) {
                        niveles.add(new NivelRubricaDto(
                                texto(nivelNode, "nombre", "Nivel"),
                                numero(nivelNode, "puntaje", 0),
                                texto(nivelNode, "descriptor", "Descripcion del nivel")
                        ));
                    }
                }

                criterios.add(new CriterioRubricaDto(nombre, descripcionCriterio, peso, niveles));
            }
        }

        RubricaGeneradaDto rubrica = new RubricaGeneradaDto(
                titulo,
                descripcion,
                tema,
                nivel,
                asignatura,
                tipoTarea,
                puntajeMaximo,
                criterios,
                true,
                modeloPorDefecto,
                Instant.now()
        );

            return normalizarRubricaSinRepeticiones(rubrica);
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

    private String texto(JsonNode node, String key, String fallback) {
        String value = node.path(key).asText();
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private int numero(JsonNode node, String key, int fallback) {
        JsonNode child = node.path(key);
        if (child.isInt() || child.isLong()) {
            return child.asInt();
        }
        if (child.isTextual()) {
            try {
                return Integer.parseInt(child.asText().trim());
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }
        return fallback;
    }
}
