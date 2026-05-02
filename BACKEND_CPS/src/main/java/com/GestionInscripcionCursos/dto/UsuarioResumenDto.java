package com.GestionInscripcionCursos.dto;

public record UsuarioResumenDto(
        String id,
        String nombre,
        String email,
        String rol,
        Boolean activo
) {
}
