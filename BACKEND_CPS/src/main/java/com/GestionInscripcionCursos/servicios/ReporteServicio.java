package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.RendimientoAlumnoDto;
import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.CriterioRubrica;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.PuntajeCriterio;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Rubrica;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.EstadoEntrega;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.ActividadRepositorio;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.ReporteRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ReporteServicio {

    private final ActividadRepositorio actividadRepositorio;
    private final ReporteRepositorio reporteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final CursoRepositorio cursoRepositorio;
    private final InscripcionRepositorio inscripcionRepositorio;

    public ReporteServicio(
            ActividadRepositorio actividadRepositorio,
            ReporteRepositorio reporteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            CursoRepositorio cursoRepositorio,
            InscripcionRepositorio inscripcionRepositorio
    ) {
        this.actividadRepositorio = actividadRepositorio;
        this.reporteRepositorio = reporteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.cursoRepositorio = cursoRepositorio;
        this.inscripcionRepositorio = inscripcionRepositorio;
    }

    @Transactional
    public void crearReporte(String respuesta, String idActividad, String idUser, String archivoUrl) throws MyException {
        validarLimitesReporte(idUser, idActividad);
        validarReporte(respuesta);

        Actividad actividad = actividadRepositorio.findById(idActividad).get();
        Usuario usuario = usuarioRepositorio.findById(idUser).get();
        Date ahora = new Date();

        Reporte reporte;

        // Evaluar si es un envío atrasado
        if (ahora.after(actividad.getFechaVencimiento())) {
            reporte = new Reporte(respuesta, "00", "Entrega fuera de plazo.", EstadoEntrega.ATRASADO, ahora, usuario, actividad);
        } else {
            reporte = new Reporte(respuesta, "Por Calificar", "Ningun Comentario", EstadoEntrega.ENVIADO, ahora, usuario, actividad);
        }

        reporte.setArchivoUrl(archivoUrl);
        reporteRepositorio.save(reporte);
    }

    public List<Reporte> listarReportesPorIdActividad(String idActividad) {
        return reporteRepositorio.buscarReportesPorIdActividad(idActividad);
    }


    public void validarLimitesReporte(String idUser, String idActividad) throws MyException {
        Actividad actividad = actividadRepositorio.findById(idActividad).get();
        Long totalReportes = reporteRepositorio.contarReportesPorUsuarioYActividad(idUser, idActividad);

        if (totalReportes >= actividad.getIntentosPermitidos()) {
            throw new MyException("Has alcanzado el límite máximo de " + actividad.getIntentosPermitidos() + " intento(s) para esta actividad.");
        }
    }

    // Nuevo método para devolver la LISTA de reportes del alumno
    public List<Reporte> listarReportesAlumno(String idUser, String idActividad) {
        return reporteRepositorio.buscarReportesPorUsuarioYActividad(idUser, idActividad);
    }


    public Reporte buscarPorId(String id) {
        return reporteRepositorio.buscarPorId(id);
    }

    public Reporte buscarPorIdCategoriaIdUsuario(String idUser, String idActividad) {
        return reporteRepositorio.buscarReportePorIdUserIdActividad(idUser,idActividad);
    }

    private void validarReporte(String respuesta) throws MyException {

        if (respuesta.isEmpty() || respuesta == null) {
            throw new MyException("La respuesta no puede ser nulo o estar vacio");
        }

    }



    @Transactional
    public void calificarReporte(String id, String nota, String comentario) throws MyException {

        validarReporte( nota, comentario);


        Optional<Reporte> respuesta1 = reporteRepositorio.findById(id);

        if (respuesta1.isPresent()) {

            Reporte reporte = respuesta1.get();

            reporte.setNota(nota);

            reporte.setComentario(comentario);

            reporte.setEstado(EstadoEntrega.CALIFICADO);



            reporteRepositorio.save(reporte);

        }
    }

    private void validarReporte( String nota, String comentario) throws MyException {


        if (nota.isEmpty() || nota == null) {
            throw new MyException("La nota no puede ser nulo o estar vacio");
        }
        if (comentario.isEmpty() || comentario == null) {
            throw new MyException("El comentario no puede ser nulo o estar vacio");
        }

    }

    /**
     * Califica una entrega usando la rubrica asignada a su actividad (RF-09):
     * recibe el puntaje obtenido por criterio, valida que este dentro del
     * rango permitido y calcula la nota total como la suma de los puntajes.
     */
    @Transactional
    public Reporte calificarConRubrica(String idReporte, Map<String, Integer> puntajesPorCriterioId, String comentario) throws MyException {
        Reporte reporte = reporteRepositorio.findById(idReporte)
                .orElseThrow(() -> new MyException("Reporte no encontrado"));

        Rubrica rubrica = reporte.getActividad().getRubrica();
        if (rubrica == null) {
            throw new MyException("La actividad de este reporte no tiene una rúbrica asignada");
        }

        int total = 0;
        List<PuntajeCriterio> nuevosPuntajes = new ArrayList<>();
        for (CriterioRubrica criterio : rubrica.getCriterios()) {
            Integer puntaje = puntajesPorCriterioId != null ? puntajesPorCriterioId.get(criterio.getId()) : null;
            if (puntaje == null) {
                throw new MyException("Falta el puntaje del criterio '" + criterio.getNombre() + "'");
            }
            int maximo = criterio.puntajeMaximoCriterio();
            if (puntaje < 0 || puntaje > maximo) {
                throw new MyException("El puntaje del criterio '" + criterio.getNombre() + "' debe estar entre 0 y " + maximo);
            }
            nuevosPuntajes.add(new PuntajeCriterio(reporte, criterio, puntaje));
            total += puntaje;
        }

        reporte.getPuntajesCriterio().clear();
        reporte.getPuntajesCriterio().addAll(nuevosPuntajes);
        reporte.setNota(String.valueOf(total));
        reporte.setComentario(comentario);
        reporte.setEstado(EstadoEntrega.CALIFICADO);

        return reporteRepositorio.save(reporte);
    }

    /**
     * Reporte academico agregado por curso (RF-12): para cada alumno inscrito
     * calcula cuantas actividades tiene pendientes, entregadas, atrasadas o
     * calificadas, y su promedio de notas numericas.
     */
    public List<RendimientoAlumnoDto> calcularRendimientoCurso(String cursoId) throws MyException {
        Curso curso = cursoRepositorio.findById(cursoId)
                .orElseThrow(() -> new MyException("Curso no encontrado"));

        List<Actividad> actividades = actividadRepositorio.buscarActividadesPorIdCurso(curso.getId());
        List<Usuario> alumnos = inscripcionRepositorio.buscarAlumnosAprobadosPorCurso(curso.getId());

        List<RendimientoAlumnoDto> resultado = new ArrayList<>();
        for (Usuario alumno : alumnos) {
            resultado.add(calcularRendimientoAlumno(alumno, actividades));
        }

        return resultado;
    }

    private RendimientoAlumnoDto calcularRendimientoAlumno(Usuario alumno, List<Actividad> actividades) {
        Set<String> actividadesConEntrega = new HashSet<>();
        int atrasadas = 0;
        int calificadas = 0;
        double sumaNotas = 0;
        int cantidadNotas = 0;

        for (Actividad actividad : actividades) {
            Reporte ultimoIntento = ultimoIntentoDe(alumno, actividad);
            if (ultimoIntento == null) {
                continue;
            }

            actividadesConEntrega.add(actividad.getId());
            if (ultimoIntento.getEstado() == EstadoEntrega.ATRASADO) {
                atrasadas++;
            } else if (ultimoIntento.getEstado() == EstadoEntrega.CALIFICADO) {
                calificadas++;
                Double nota = notaComoNumero(ultimoIntento.getNota());
                if (nota != null) {
                    sumaNotas += nota;
                    cantidadNotas++;
                }
            }
        }

        int entregadas = actividadesConEntrega.size();
        int pendientes = actividades.size() - entregadas;
        Double promedio = cantidadNotas > 0 ? sumaNotas / cantidadNotas : null;

        return new RendimientoAlumnoDto(
                alumno.getId(), alumno.getNombre(), alumno.getEmail(),
                actividades.size(), entregadas, pendientes, atrasadas, calificadas, promedio);
    }

    private Reporte ultimoIntentoDe(Usuario alumno, Actividad actividad) {
        List<Reporte> intentos = reporteRepositorio.buscarReportesPorUsuarioYActividad(alumno.getId(), actividad.getId());
        return intentos.isEmpty() ? null : intentos.get(0);
    }

    private Double notaComoNumero(String nota) {
        try {
            return Double.parseDouble(nota);
        } catch (NumberFormatException ignorado) {
            // Notas no numericas (p.ej. "Por Calificar") no participan del promedio
            return null;
        }
    }
}
