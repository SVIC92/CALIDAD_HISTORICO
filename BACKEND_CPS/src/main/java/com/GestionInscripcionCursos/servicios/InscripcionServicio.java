/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.HorarioSesion;
import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.HorarioSesionRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InscripcionServicio {

    @Autowired
    private InscripcionRepositorio inscripcionRepositorio;

    @Autowired
    private CursoRepositorio cursoRepositorio;

    @Autowired
    private HorarioSesionRepositorio horarioSesionRepositorio;

    public List<Inscripcion> listaPendientesProfesor() {
        return inscripcionRepositorio.listarPendientesProfesor();
    }

    public List<Inscripcion> listaRealizadasProfesor() {
        return inscripcionRepositorio.listarRealizadasProfesor();
    }
    
    public List<Inscripcion> listaPendientesAlumno() {
        return inscripcionRepositorio.listarPendientesAlumno();
    }

    public List<Inscripcion> listaRealizadasAlumno() {
        return inscripcionRepositorio.listarRealizadasAlumno();
    }

    @Transactional
    public void aprobar(String id) throws MyException {

        Inscripcion inscripcion = inscripcionRepositorio.getById(id);

        validarInscripcionAlumno(inscripcion);

        inscripcion.setEstado("APROBADO");

        inscripcionRepositorio.save(inscripcion);

    }
    
    @Transactional
    public void rechazar(String id) throws MyException {

        Inscripcion inscripcion = inscripcionRepositorio.getById(id);

        validarInscripcionAlumno(inscripcion);

        inscripcion.setEstado("RECHAZADO");

        inscripcionRepositorio.save(inscripcion);

    }

    @Transactional
    public void aprobarInscripcionProfesor(String id) throws MyException {
    Inscripcion inscripcion = inscripcionRepositorio.getById(id);
    validarInscripcionProfesor(inscripcion);

    Usuario usuario = inscripcion.getUsuario();
    Curso curso = inscripcion.getCurso();

    // Valida si el curso ya tiene otro profesor (lógica original)
    if (curso.getProfesorAsignado() != null
            && !curso.getProfesorAsignado().getId().equals(usuario.getId())) {
        throw new MyException("El curso ya tiene otro profesor asignado");
    }

    // NUEVO: Validar que el profesor no tenga cruces de horario
    validarCruceHorarios(usuario, curso);

    inscripcion.setEstado("APROBADO");
    curso.setProfesorAsignado(usuario);
    cursoRepositorio.save(curso);
    inscripcionRepositorio.save(inscripcion);
}

    @Transactional
    public void rechazarInscripcionProfesor(String id) throws MyException {
        Inscripcion inscripcion = inscripcionRepositorio.getById(id);

        validarInscripcionProfesor(inscripcion);

        inscripcion.setEstado("RECHAZADO");
        inscripcionRepositorio.save(inscripcion);
    }

    private void validarInscripcionProfesor(Inscripcion inscripcion) throws MyException {
        if (inscripcion.getUsuario() == null || !Rol.PROFESOR.equals(inscripcion.getUsuario().getRol())) {
            throw new MyException("La inscripcion no corresponde a un profesor");
        }
    }

    private void validarInscripcionAlumno(Inscripcion inscripcion) throws MyException {
        if (inscripcion.getUsuario() == null || !Rol.ALUMNO.equals(inscripcion.getUsuario().getRol())) {
            throw new MyException("Para profesores use aprobarProfesor/rechazarProfesor");
        }
    }

    private void validarCruceHorarios(Usuario profesor, Curso nuevoCurso) throws MyException {
    // 1. Obtener los horarios del curso al que el profesor quiere ser asignado
    List<HorarioSesion> horariosNuevoCurso = horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(nuevoCurso.getId());
    
    // Si el curso no tiene horarios aún, no hay cruce posible
    if (horariosNuevoCurso == null || horariosNuevoCurso.isEmpty()) {
        return; 
    }

    // 2. Obtener los cursos donde el profesor ya está aprobado/asignado
    List<Curso> cursosAprobados = cursoRepositorio.buscarCursosInscritosProfesor(profesor.getId());

    // 3. Comparar los horarios para buscar solapamientos
    for (Curso cursoExistente : cursosAprobados) {
        List<HorarioSesion> horariosExistente = horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(cursoExistente.getId());

        for (HorarioSesion horarioNuevo : horariosNuevoCurso) {
            for (HorarioSesion horarioExistente : horariosExistente) {
                
                // Si coinciden en el mismo día de la semana
                if (horarioNuevo.getDiaSemana().equalsIgnoreCase(horarioExistente.getDiaSemana())) {
                    
                    // Lógica de solapamiento de tiempo: (InicioA < FinB) && (FinA > InicioB)
                    if (horarioNuevo.getHoraInicio().isBefore(horarioExistente.getHoraFin()) && 
                        horarioNuevo.getHoraFin().isAfter(horarioExistente.getHoraInicio())) {
                        
                        throw new MyException("Cruce de horarios detectado. El profesor ya dicta el curso '" 
                            + cursoExistente.getNombre() + "' los días " + horarioExistente.getDiaSemana() 
                            + " de " + horarioExistente.getHoraInicio() + " a " + horarioExistente.getHoraFin());
                    }
                }
            }
        }
    }
    }
}
