package com.GestionInscripcionCursos.servicios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CohereServicio {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CohereServicio.class);
    // Usamos el endpoint oficial de Cohere de forma directa
    private static final String URL_COHERE = "https://api.cohere.com/v1/chat";

    // Aseguramos leer exactamente la misma variable de tu properties
    @Value("${COHERE_API_KEY:}")
    private String apiKey;

    @Value("${cohere.model:command-r-08-2024}")
    private String modelo;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CohereServicio(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public boolean estaConfigurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generarTexto(String prompt) {
        if (!estaConfigurado()) {
            throw new IllegalStateException("Cohere API key no configurada");
        }

        try {
            Map<String, Object> cuerpoRequest = new LinkedHashMap<>();
            cuerpoRequest.put("message", prompt);
            
            // Si el modelo viene vacío, forzamos command-r
            String modeloAUsar = (modelo == null || modelo.isBlank()) ? "command-r" : modelo;
            cuerpoRequest.put("model", modeloAUsar);
            
            // Aumentamos los tokens. Un JSON de rúbrica es pesado.
            cuerpoRequest.put("max_tokens", 4000); 
            cuerpoRequest.put("temperature", 0.7);

            String jsonCuerpo = objectMapper.writeValueAsString(cuerpoRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_COHERE))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonCuerpo))
                    .build();

            HttpResponse<String> respuesta = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() != 200) {
                // Este log ahora te mostrará el mensaje EXACTO de Cohere si algo falla
                LOGGER.error("Error en Cohere API. Status: {}, Body: {}", respuesta.statusCode(), respuesta.body());
                throw new IllegalStateException("Cohere respondio con status " + respuesta.statusCode());
            }

            JsonNode raiz = objectMapper.readTree(respuesta.body());
            JsonNode textNode = raiz.path("text");

            if (textNode.isNull() || textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new IllegalStateException("Respuesta vacia de Cohere");
            }

            return textNode.asText();

        } catch (IOException | InterruptedException ex) {
            LOGGER.error("Error comunicando con Cohere: {}", ex.getMessage());
            throw new IllegalStateException("Error al comunicarse con Cohere", ex);
        }
    }
}