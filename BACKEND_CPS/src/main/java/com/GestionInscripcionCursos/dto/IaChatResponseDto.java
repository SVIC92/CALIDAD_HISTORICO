package com.GestionInscripcionCursos.dto;

import java.time.Instant;

public record IaChatResponseDto(
        String respuesta,
        String rol,
        String modelo,
        Instant fecha
) {
}
