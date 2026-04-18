package com.GestionInscripcionCursos.dto;

import java.util.List;

public record UnidadSilaboDto(
        String tituloUnidad,
        String logroUnidad,
        List<SemanaSilaboDto> semanas
) {
}
