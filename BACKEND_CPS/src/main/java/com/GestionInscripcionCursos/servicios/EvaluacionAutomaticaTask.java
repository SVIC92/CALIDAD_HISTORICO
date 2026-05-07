package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.repositorios.ActividadRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.ReporteRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

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
            
            for (Usuario alumno : alumnos) {
                Long cantidadReportes = reporteRepositorio.contarReportesPorUsuarioYActividad(alumno.getId(), actividad.getId());
                
                // Si el alumno no envió ningún reporte (0 intentos usados), se le asigna 00 automáticamente
                if (cantidadReportes == 0) {
                    Reporte reporteAuto = new Reporte(
                        "No entregado (Cierre automático)", 
                        "00", 
                        "Actividad vencida. Calificación asignada por el sistema.", 
                        "CALIFICADO", 
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