package com.GestionInscripcionCursos.dto;

import java.util.Date;

/**
 * Datos de entrada para crear o modificar un curso.
 * Agrupa los parametros que antes se pasaban sueltos a CursoServicio.
 */
public record CursoRequestDto(
        String nombre,
        String codigoCurso,
        String descripcion,
        Integer capacidadMaxima,
        Integer creditos,
        Integer ciclo,
        String modalidad,
        Date fechaInicio,
        Date fechaTermino,
        Integer horasTeoricas,
        Integer horasPracticas,
        Integer horasLaboratorio,
        String estado,
        String profesorReferencia,
        String carreraReferencia
) {
}
