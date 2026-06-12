package com.GestionInscripcionCursos.util;

import com.GestionInscripcionCursos.entidades.HorarioSesion;
import java.util.List;

/**
 * Utilidades para la deteccion de cruces de horarios entre sesiones de clase.
 * Centraliza la logica que antes estaba duplicada en CursoServicio e InscripcionServicio.
 */
public final class HorarioUtil {

    private HorarioUtil() {
    }

    /**
     * Dos sesiones se cruzan si caen el mismo dia de la semana y sus rangos horarios se solapan.
     * Regla de solapamiento: (InicioA < FinB) && (FinA > InicioB).
     */
    public static boolean seCruzan(HorarioSesion a, HorarioSesion b) {
        return a.getDiaSemana().equalsIgnoreCase(b.getDiaSemana())
                && a.getHoraInicio().isBefore(b.getHoraFin())
                && a.getHoraFin().isAfter(b.getHoraInicio());
    }

    /**
     * Devuelve la primera sesion de {@code existentes} que se cruza con alguna de {@code nuevos},
     * o {@code null} si no hay ningun cruce.
     */
    public static HorarioSesion primerCruce(List<HorarioSesion> nuevos, List<HorarioSesion> existentes) {
        if (nuevos == null || existentes == null) {
            return null;
        }
        for (HorarioSesion nuevo : nuevos) {
            for (HorarioSesion existente : existentes) {
                if (seCruzan(nuevo, existente)) {
                    return existente;
                }
            }
        }
        return null;
    }
}
