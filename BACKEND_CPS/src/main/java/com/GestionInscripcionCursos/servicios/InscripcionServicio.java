/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InscripcionServicio {

    @Autowired
    private InscripcionRepositorio inscripcionRepositorio;

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

        inscripcion.setEstado("APROBADO");

        inscripcionRepositorio.save(inscripcion);

    }
    
    @Transactional
    public void rechazar(String id) throws MyException {

        Inscripcion inscripcion = inscripcionRepositorio.getById(id);

        inscripcion.setEstado("RECHAZADO");

        inscripcionRepositorio.save(inscripcion);

    }

}
