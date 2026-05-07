package com.GestionInscripcionCursos.dto;

import java.util.Date;

public record UsuarioResumenDto(
        String id,
        String nombre,
        String email,
        String rol,
        Boolean activo,
        Date fechaCreacion
) {
}
