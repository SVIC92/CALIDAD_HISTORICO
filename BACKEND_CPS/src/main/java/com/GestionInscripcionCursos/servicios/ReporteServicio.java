package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Reporte;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.ActividadRepositorio;
import com.GestionInscripcionCursos.repositorios.ReporteRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import jakarta.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReporteServicio {

    @Autowired
    private ActividadRepositorio actividadRepositorio;

    @Autowired
    private ReporteRepositorio reporteRepositorio;
    
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Transactional
    public void crearReporte(String respuesta, String idActividad, String idUser) throws MyException {

        validarReporte(respuesta);

        Optional<Actividad> respuesta1 = actividadRepositorio.findById(idActividad);

        Actividad actividad = respuesta1.get();
        
        Optional<Usuario> respuesta2 = usuarioRepositorio.findById(idUser);

        Usuario usuario = respuesta2.get();

        Reporte reporte = new Reporte(respuesta, "Por Calificar", "Ningun Comentario", "ENVIADO", new Date(),usuario, actividad);

        reporteRepositorio.save(reporte);
    }

    public List<Reporte> listarReportesPorIdActividad(String idActividad) {
        return reporteRepositorio.buscarReportesPorIdActividad(idActividad);
    }
    
    
    public void validarDobleReporte(String idUser, String idActividad)throws MyException{
        
        Reporte reporte = reporteRepositorio.buscarReportePorIdUserIdActividad(idUser,idActividad);
        
        if (reporte!=null) {

            throw new MyException("No puede realizar doble reporte");
        }
        
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
            
            reporte.setEstado("CALIFICADO");
            
           

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
}
