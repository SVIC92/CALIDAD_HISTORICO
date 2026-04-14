package com.GestionInscripcionCursos.dto;

import java.time.Instant;

public record UsuarioConectadoDto(
        String email,
        String rol,
        Instant conectadoDesde,
        Instant ultimaActividad
) {
}
