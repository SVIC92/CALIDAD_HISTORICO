package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.EstadoEntrega;
import com.GestionInscripcionCursos.repositorios.ActividadRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.ReporteRepositorio;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EvaluacionAutomaticaTask {

    @Autowired
    private ActividadRepositorio actividadRepositorio;

    @Autowired
    private InscripcionRepositorio inscripcionRepositorio;

    @Autowired
    private ReporteRepositorio reporteRepositorio;
    // En producción podrías cambiarlo a "@Scheduled(cron = "0 0 0 * * *")" para que corra a medianoche.
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void calificarActividadesVencidas() {
        Date ahora = new Date();
        List<Actividad> vencidas = actividadRepositorio.buscarActividadesVencidas(ahora);

        for (Actividad actividad : vencidas) {
            List<Usuario> alumnos = inscripcionRepositorio.buscarAlumnosAprobadosPorCurso(actividad.getCurso().getId());
            if (alumnos.isEmpty()) {
                continue;
            }

            // Una sola consulta por actividad (en vez de una por cada alumno) para saber
            // quienes ya entregaron algo.
            Set<String> alumnosConReporte = new HashSet<>();
            for (Reporte reporte : reporteRepositorio.buscarReportesPorIdActividad(actividad.getId())) {
                alumnosConReporte.add(reporte.getUsuario().getId());
            }

            for (Usuario alumno : alumnos) {
                // Si el alumno no envió ningún reporte, se le asigna 00 automáticamente
                if (!alumnosConReporte.contains(alumno.getId())) {
                    Reporte reporteAuto = new Reporte(
                        "No entregado (Cierre automático)",
                        "00",
                        "Actividad vencida. Calificación asignada por el sistema.",
                        EstadoEntrega.CALIFICADO,
                        ahora,
                        alumno,
                        actividad
                    );
                    reporteRepositorio.save(reporteAuto);
                }
            }
        }
    }
}