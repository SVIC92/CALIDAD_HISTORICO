package com.GestionInscripcionCursos.dto;

import java.util.List;

public record CriterioRubricaDto(
        String nombre,
        String descripcion,
        Integer peso,
        List<NivelRubricaDto> niveles
) {
}
