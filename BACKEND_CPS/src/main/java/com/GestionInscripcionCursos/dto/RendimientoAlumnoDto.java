package com.GestionInscripcionCursos.dto;

/**
 * Reporte academico agregado de un alumno dentro de un curso (RF-12):
 * cuantas actividades tiene pendientes, entregadas, atrasadas o calificadas,
 * y su promedio de notas numericas.
 */
public record RendimientoAlumnoDto(
        String alumnoId,
        String nombreAlumno,
        String email,
        int actividadesTotales,
        int entregadas,
        int pendientes,
        int atrasadas,
        int calificadas,
        Double promedioNotas
) {
}
