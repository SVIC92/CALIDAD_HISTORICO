package com.GestionInscripcionCursos.dto;

public record UsuarioAdminRequestDto(
        String nombre,
        String email,
        String password,
        String rol,
        String carrera,
        Integer cicloActual
) {
}
