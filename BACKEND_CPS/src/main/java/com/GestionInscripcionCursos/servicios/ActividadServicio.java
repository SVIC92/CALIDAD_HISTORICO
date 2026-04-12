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
    public void crearActividad(String nombre, String descripcion, String idCurso) throws MyException {

        validarActividad(nombre, descripcion);

        Optional<Curso> respuesta = cursoRepositorio.findById(idCurso);

        Curso curso = respuesta.get();

        Actividad actividad = new Actividad(nombre, descripcion, curso);

        actividadRepositorio.save(actividad);
    }

    public List<Actividad> listarActividadesPorIdCurso(String idCurso) {
        return actividadRepositorio.buscarActividadesPorIdCurso(idCurso);
    }

    @Transactional
    public void modificarActividad(String id, String nombre, String descripcion) throws MyException {

        validarActividad(nombre, descripcion);


        Optional<Actividad> respuesta = actividadRepositorio.findById(id);

        if (respuesta.isPresent()) {

            Actividad actividad = respuesta.get();

            actividad.setNombre(nombre);

            actividad.setDescripcion(descripcion);

            actividadRepositorio.save(actividad);

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

    private void validarActividad(String nombre, String descripcion) throws MyException {

        if (nombre.isEmpty() || nombre == null) {
            throw new MyException("El nombre no puede ser nulo o estar vacio");
        }

        if (descripcion.isEmpty() || descripcion == null) {
            throw new MyException("La descripcion no puede ser nulo o estar vacio");
        }
    }

}
