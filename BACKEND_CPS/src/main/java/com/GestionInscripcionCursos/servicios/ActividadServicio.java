/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.ActividadRepositorio;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActividadServicio {

    @Autowired
    private ActividadRepositorio actividadRepositorio;

    @Autowired
    private CursoRepositorio cursoRepositorio;

    @Transactional
    public void crearActividad(String nombre, String descripcion, Date fechaVencimiento, Integer intentosPermitidos, String idCurso) throws MyException {
        validarActividad(nombre, descripcion, fechaVencimiento, intentosPermitidos);
        Optional<Curso> respuesta = cursoRepositorio.findById(idCurso);
        Curso curso = respuesta.get();
        Actividad actividad = new Actividad(nombre, descripcion, fechaVencimiento, curso);
        actividad.setIntentosPermitidos(intentosPermitidos);
        actividadRepositorio.save(actividad);
    }

    @Transactional
    public void modificarActividad(String id, String nombre, String descripcion, Date fechaVencimiento, Integer intentosPermitidos) throws MyException {
        validarActividad(nombre, descripcion, fechaVencimiento, intentosPermitidos);
        Optional<Actividad> respuesta = actividadRepositorio.findById(id);
        if (respuesta.isPresent()) {
            Actividad actividad = respuesta.get();
            actividad.setNombre(nombre);
            actividad.setDescripcion(descripcion);
            actividad.setFechaVencimiento(fechaVencimiento);
            actividad.setIntentosPermitidos(intentosPermitidos);
            actividadRepositorio.save(actividad);
        }
    }

    public List<Actividad> listarActividadesPorIdCurso(String idCurso) {
        return actividadRepositorio.buscarActividadesPorIdCurso(idCurso);
    }
    private void validarActividad(String nombre, String descripcion, Date fechaVencimiento, Integer intentosPermitidos) throws MyException {
        // ... (tus validaciones previas de nombre, desc y fecha) ...
        if (intentosPermitidos == null || intentosPermitidos < 1 || intentosPermitidos > 3) {
            throw new MyException("Los intentos permitidos deben estar entre 1 y 3");
        }
    }

    @Transactional
    public void eliminarActividad(String id) throws MyException {

        Actividad actividad = actividadRepositorio.getById(id);

        actividadRepositorio.delete(actividad);

    }

    public Actividad buscarPorId(String id) {
        return actividadRepositorio.buscarPorId(id);
    }

    private void validarActividad(String nombre, String descripcion, Date fechaVencimiento) throws MyException {

        if (nombre == null || nombre.isEmpty()) {
            throw new MyException("El nombre no puede ser nulo o estar vacio");
        }

        if (descripcion == null || descripcion.isEmpty()) {
            throw new MyException("La descripcion no puede ser nulo o estar vacio");
        }

        if (fechaVencimiento == null) {
            throw new MyException("La fecha de vencimiento es obligatoria");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime vencimiento = fechaVencimiento.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        if (vencimiento.isBefore(ahora)) {
            throw new MyException("La fecha y hora de vencimiento no puede ser anterior a la fecha y hora de creacion");
        }
    }

}
