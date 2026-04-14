package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.IaChatResponseDto;
import com.GestionInscripcionCursos.dto.IaHistorialDto;
import com.GestionInscripcionCursos.entidades.IaHistorial;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.repositorios.IaHistorialRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IaServicio {

    private static final String URL_GROQ = "https://api.groq.com/openai/v1/chat/completions";

    private final UsuarioServicio usuarioServicio;
    private final IaHistorialRepositorio iaHistorialRepositorio;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String groqApiKey;
    private final String modeloPorDefecto;

    public IaServicio(
            UsuarioServicio usuarioServicio,
            IaHistorialRepositorio iaHistorialRepositorio,
            ObjectMapper objectMapper,
            @Value("${groq.api.key:}") String groqApiKey,
            @Value("${groq.model:llama-3.1-8b-instant}") String modeloPorDefecto
    ) {
        this.usuarioServicio = usuarioServicio;
        this.iaHistorialRepositorio = iaHistorialRepositorio;
        this.objectMapper = objectMapper;
        this.groqApiKey = groqApiKey;
        this.modeloPorDefecto = modeloPorDefecto;
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
}
