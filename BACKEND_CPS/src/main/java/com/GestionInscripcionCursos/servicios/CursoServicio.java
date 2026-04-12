package com.GestionInscripcionCursos.servicios;


import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import jakarta.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CursoServicio {

    @Autowired
    private CursoRepositorio cursoRepositorio;

    @Autowired
    private InscripcionRepositorio inscripcionRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Transactional
    public void crearCurso(String nombre, String descripcion) throws MyException {

        validarCurso(nombre, descripcion);

        Curso curso = new Curso(nombre, descripcion);

        cursoRepositorio.save(curso);
    }

    public List<Curso> listarCursos() {
        return cursoRepositorio.buscarCursos();
    }

    @Transactional
    public void modificarCurso(String id, String nombre, String descripcion) throws MyException {

        validarCurso(nombre, descripcion);

        Optional<Curso> respuesta = cursoRepositorio.findById(id);

        if (respuesta.isPresent()) {

            Curso curso = respuesta.get();

            curso.setNombre(nombre);

            curso.setDescripcion(descripcion);

            cursoRepositorio.save(curso);

        }
    }

    @Transactional
    public void eliminarCurso(String id) throws MyException {

        Curso curso = cursoRepositorio.getById(id);

        cursoRepositorio.delete(curso);

    }

    public Curso buscarPorId(String id) {
        return cursoRepositorio.buscarPorId(id);
    }

    private void validarCurso(String nombre, String descripcion) throws MyException {

        if (nombre.isEmpty() || nombre == null) {
            throw new MyException("El nombre no puede ser nulo o estar vacio");
        }

        if (descripcion.isEmpty() || descripcion == null) {
            throw new MyException("La descripcion no puede ser nulo o estar vacio");
        }
    }

    public List<Curso> listarCursosDisponiblesProfesor(String idUser) {
       return cursoRepositorio.buscarCursosDisponiblesProfesor(idUser);

    }

    public List<Curso> listarCursosInscritosProfesor(String idUser) {
        return cursoRepositorio.buscarCursosInscritosProfesor(idUser);
    }
    
    public List<Curso> listarCursosDisponiblesAlumno(String idUser) {
       return cursoRepositorio.buscarCursosDisponiblesAlumno(idUser);

    }

    public List<Curso> listarCursosInscritosAlumno(String idUser) {
        return cursoRepositorio.buscarCursosInscritosAlumno(idUser);
    }

    @Transactional
    public void inscribirCurso(String idUser, String idCurso) throws MyException {

        Inscripcion inscripcion = inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso(idUser, idCurso);

        if (inscripcion != null) {

            if (inscripcion.getEstado().equals("PENDIENTE")) {
                throw new MyException("ESTADO PENDIENTE.");
            }

            inscripcion.setEstado("PENDIENTE");
            inscripcion.setFechaCreacion(new Date());
            inscripcionRepositorio.save(inscripcion);
        } else {
            Optional<Usuario> respuesta1 = usuarioRepositorio.findById(idUser);
            Usuario usuario = respuesta1.get();

            Optional<Curso> respuesta2 = cursoRepositorio.findById(idCurso);
            Curso curso = respuesta2.get();

            Inscripcion inscripcionNuevo = new Inscripcion(new Date(), "PENDIENTE", usuario, curso);

            inscripcionRepositorio.save(inscripcionNuevo);

        }

    }
}
