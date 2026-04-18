package com.GestionInscripcionCursos.dto;

public record InformacionGeneralDto(
        String curso,
        String carrera,
        int ciclo,
        int creditos
) {
}
