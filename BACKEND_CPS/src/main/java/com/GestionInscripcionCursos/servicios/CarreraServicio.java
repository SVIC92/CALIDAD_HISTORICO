package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CarreraRepositorio;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

@Service
public class CarreraServicio {

    @Autowired
    private CarreraRepositorio carreraRepositorio;

    @Transactional
    public Carrera crear(String codigo, String nombre, String descripcion) throws MyException {
        if (codigo == null || codigo.isBlank()) {
            throw new MyException("El codigo de carrera es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new MyException("El nombre de carrera es obligatorio");
        }

        String codigoNorm = codigo.trim().toUpperCase();
        if (carreraRepositorio.findByCodigoIgnoreCase(codigoNorm).isPresent()) {
            throw new MyException("Ya existe una carrera con el codigo " + codigoNorm);
        }

        Carrera carrera = new Carrera(codigoNorm, nombre.trim(), descripcion);
        return carreraRepositorio.save(carrera);
    }

    public List<Carrera> listar() {
        return carreraRepositorio.findAll().stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .toList();
    }

    @Transactional
    public Carrera modificar(String id, String codigo, String nombre, String descripcion) throws MyException {
        Optional<Carrera> opt = carreraRepositorio.findById(id);
        if (opt.isEmpty()) {
            throw new MyException("Carrera no encontrada");
        }

        Carrera carrera = opt.get();

        if (codigo != null && !codigo.isBlank()) {
            String codigoNorm = codigo.trim().toUpperCase();
            Optional<Carrera> porCodigo = carreraRepositorio.findByCodigoIgnoreCase(codigoNorm);
            if (porCodigo.isPresent() && !porCodigo.get().getId().equals(id)) {
                throw new MyException("Ya existe otra carrera con ese codigo");
            }
            carrera.setCodigo(codigoNorm);
        }

        if (nombre != null && !nombre.isBlank()) {
            carrera.setNombre(nombre.trim());
        }

        carrera.setDescripcion(descripcion);
        return carreraRepositorio.save(carrera);
    }

    @Transactional
    public void eliminar(String id) throws MyException {
        Optional<Carrera> opt = carreraRepositorio.findById(id);
        if (opt.isEmpty()) {
            throw new MyException("Carrera no encontrada");
        }

        Carrera carrera = opt.get();
        if (carrera.getCursos() != null && !carrera.getCursos().isEmpty()) {
            throw new MyException("No puedes eliminar una carrera con cursos asociados");
        }

        carreraRepositorio.delete(carrera);
    }
}
