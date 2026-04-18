package com.GestionInscripcionCursos.dto;

public record SilaboGeneracionRequestDto(
        String cursoId,
        String nombreCurso,
        String carrera,
        int ciclo,
        int creditos,
        int semanas,
        String descripcionBreve 
) {}