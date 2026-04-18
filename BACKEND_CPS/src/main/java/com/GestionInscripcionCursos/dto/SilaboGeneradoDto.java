package com.GestionInscripcionCursos.dto;

import java.util.List;

public record SilaboGeneradoDto(
        InformacionGeneralDto informacionGeneral,
        List<String> competenciasGenerales,
        List<String> competenciasEspecificas,
        String sumilla,
        String logroCurso,
        List<UnidadSilaboDto> unidades,
        String sistemaEvaluacion
) {
}