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
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import com.GestionInscripcionCursos.util.HorarioUtil;

import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Transactional(readOnly = true)
    public List<Inscripcion> listaPendientesProfesor() {
        return inscripcionRepositorio.listarPendientesProfesor();
    }

    @Transactional(readOnly = true)
    public List<Inscripcion> listaRealizadasProfesor() {
        return inscripcionRepositorio.listarRealizadasProfesor();
    }

    @Transactional(readOnly = true)
    public List<Inscripcion> listaPendientesAlumno() {
        return inscripcionRepositorio.listarPendientesAlumno();
    }

    @Transactional(readOnly = true)
    public List<Inscripcion> listaRealizadasAlumno() {
        return inscripcionRepositorio.listarRealizadasAlumno();
    }

    @Transactional
    public void inscribirAlumnoDirecto(String usuarioId, String cursoId) throws MyException {
        Usuario alumno = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new MyException("Usuario no encontrado"));
            
        if (!Rol.ALUMNO.equals(alumno.getRol())) {
            throw new MyException("El usuario no es un alumno");
        }

        Curso curso = cursoRepositorio.findById(cursoId)
                .orElseThrow(() -> new MyException("Curso no encontrado"));

        // Comprobación de aforo: contamos directamente en BD en lugar de cargar toda la tabla
        if (curso.getCapacidadMaxima() != null) {
            Long alumnosInscritosActualmente = inscripcionRepositorio.contarAlumnosAprobadosPorCurso(cursoId);

            if (alumnosInscritosActualmente >= curso.getCapacidadMaxima()) {
                throw new MyException("El cupo/aforo del curso ya se encuentra lleno. Capacidad máxima: " + curso.getCapacidadMaxima());
            }
        }

        // Validación de cruces de horario del alumno
        validarCruceHorariosAlumno(alumno, curso);

        // Registro automático de la inscripción
        Inscripcion nuevaInscripcion = new Inscripcion();
        nuevaInscripcion.setUsuario(alumno);
        nuevaInscripcion.setCurso(curso);
        nuevaInscripcion.setEstado("APROBADO");  

        inscripcionRepositorio.save(nuevaInscripcion);
    }

    private void validarCruceHorariosAlumno(Usuario alumno, Curso nuevoCurso) throws MyException {
        List<HorarioSesion> horariosNuevoCurso = horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(nuevoCurso.getId());

        if (horariosNuevoCurso == null || horariosNuevoCurso.isEmpty()) {
            return;
        }

        // Horarios de los cursos aprobados/activos de ESTE alumno, en una sola consulta
        List<HorarioSesion> horariosExistentes = horarioSesionRepositorio.buscarHorariosPorAlumno(alumno.getId());

        HorarioSesion cruce = HorarioUtil.primerCruce(horariosNuevoCurso, horariosExistentes);
        if (cruce != null) {
            throw new MyException("Cruce de horarios detectado. El alumno ya está inscrito en el curso '"
                + cruce.getCurso().getNombre() + "' los días " + cruce.getDiaSemana()
                + " de " + cruce.getHoraInicio() + " a " + cruce.getHoraFin());
        }
    }

    @Transactional
    public void aprobar(String id) throws MyException {

        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new MyException("Inscripcion no encontrada"));

        validarInscripcionAlumno(inscripcion);

        inscripcion.setEstado("APROBADO");

        inscripcionRepositorio.save(inscripcion);

    }
    
    @Transactional
    public void rechazar(String id) throws MyException {

        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new MyException("Inscripcion no encontrada"));

        validarInscripcionAlumno(inscripcion);

        inscripcion.setEstado("RECHAZADO");

        inscripcionRepositorio.save(inscripcion);

    }

    @Transactional
    public void aprobarInscripcionProfesor(String id) throws MyException {
    Inscripcion inscripcion = inscripcionRepositorio.findById(id)
            .orElseThrow(() -> new MyException("Inscripcion no encontrada"));
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
        Inscripcion inscripcion = inscripcionRepositorio.findById(id)
                .orElseThrow(() -> new MyException("Inscripcion no encontrada"));

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
        List<HorarioSesion> horariosNuevoCurso = horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(nuevoCurso.getId());

        // Si el curso no tiene horarios aún, no hay cruce posible
        if (horariosNuevoCurso == null || horariosNuevoCurso.isEmpty()) {
            return;
        }

        // Horarios de los cursos activos que ya dicta el profesor, en una sola consulta
        List<HorarioSesion> horariosExistentes = horarioSesionRepositorio.buscarHorariosPorProfesor(profesor.getId());

        HorarioSesion cruce = HorarioUtil.primerCruce(horariosNuevoCurso, horariosExistentes);
        if (cruce != null) {
            throw new MyException("Cruce de horarios detectado. El profesor ya dicta el curso '"
                + cruce.getCurso().getNombre() + "' los días " + cruce.getDiaSemana()
                + " de " + cruce.getHoraInicio() + " a " + cruce.getHoraFin());
        }
    }
}
