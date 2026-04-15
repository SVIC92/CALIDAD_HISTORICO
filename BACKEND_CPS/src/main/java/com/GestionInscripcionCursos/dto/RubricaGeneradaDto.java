package com.GestionInscripcionCursos.dto;

import java.time.Instant;
import java.util.List;

public record RubricaGeneradaDto(
        String titulo,
        String descripcion,
        String tema,
        String nivelEducativo,
        String asignatura,
        String tipoTarea,
        Integer puntajeMaximo,
        List<CriterioRubricaDto> criterios,
        boolean generadaPorIa,
        String modelo,
        Instant fechaGeneracion
) {
}
