package com.GestionInscripcionCursos.dto;

import java.time.Instant;

public record IaHistorialDto(
        String ultimoMensaje,
        String ultimaRespuesta,
        String rol,
        String modelo,
        Instant fechaActualizacion
) {
}
