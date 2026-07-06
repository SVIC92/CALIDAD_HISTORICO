package com.GestionInscripcionCursos.enumeraciones;

/**
 * Estados persistidos de la entrega de una actividad (RF-11). El estado
 * "pendiente" no se modela aqui: se infiere por la ausencia de un
 * {@link com.GestionInscripcionCursos.entidades.Reporte} para esa actividad.
 */
public enum EstadoEntrega {
    ENVIADO,
    ATRASADO,
    CALIFICADO
}
