package com.GestionInscripcionCursos.dto;

public record RubricaGeneracionRequestDto(
        String tema,
        String nivelEducativo,
        String asignatura,
        String tipoTarea,
        Integer cantidadCriterios,
        Integer cantidadNiveles,
        Integer puntajeMaximo
) {
}
