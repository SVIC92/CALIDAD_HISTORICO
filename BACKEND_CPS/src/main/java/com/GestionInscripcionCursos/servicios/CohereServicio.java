package com.GestionInscripcionCursos.servicios;

import com.cohere.api.Cohere;
import com.cohere.api.requests.ChatRequest;
import com.cohere.api.types.NonStreamedChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CohereServicio {

    @Value("${COHERE_API_KEY:}")
    private String apiKey;

    @Value("${cohere.model:command-r}")
    private String modelo;

    public boolean estaConfigurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generarTexto(String prompt) {
        try {
            if (!estaConfigurado()) {
                throw new IllegalStateException("Cohere API key no configurada");
            }
            Cohere cohere = Cohere.builder()
                    .token(apiKey)
                    .build();

            NonStreamedChatResponse response = cohere.chat(
                    ChatRequest.builder()
                            .message(prompt)
                        .model(modelo)
                            .maxTokens(300)
                            .temperature(0.7f)
                            .build()
            );

            return response.getText();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar texto con Cohere", e);
        }
    }
}