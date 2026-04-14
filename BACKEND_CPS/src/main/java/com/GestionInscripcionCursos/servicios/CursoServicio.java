package com.GestionInscripcionCursos.servicios;


import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
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
    public void crearCurso(String nombre, String descripcion, Integer capacidadMaxima, Integer creditos, Date fechaTermino, String profesorReferencia) throws MyException {

        Usuario profesor = resolverProfesorSiExiste(profesorReferencia);
        validarCurso(nombre, descripcion, capacidadMaxima, creditos, fechaTermino);

        Curso curso = new Curso(nombre, descripcion, capacidadMaxima, creditos, fechaTermino, profesor);

        cursoRepositorio.save(curso);
    }

    public List<Curso> listarCursos() {
        return cursoRepositorio.buscarCursos();
    }

    public List<Curso> listarCursosActivos() {
        return cursoRepositorio.buscarCursosActivos(new Date());
    }

    public List<Curso> listarCursosCaducados() {
        return cursoRepositorio.buscarCursosCaducados(new Date());
    }

    @Transactional
    public void modificarCurso(String id, String nombre, String descripcion, Integer capacidadMaxima, Integer creditos, Date fechaTermino, String profesorReferencia) throws MyException {

        Usuario profesor = resolverProfesorSiExiste(profesorReferencia);
        validarCurso(nombre, descripcion, capacidadMaxima, creditos, fechaTermino);

        Optional<Curso> respuesta = cursoRepositorio.findById(id);

        if (respuesta.isPresent()) {

            Curso curso = respuesta.get();

            curso.setNombre(nombre);

            curso.setDescripcion(descripcion);

            curso.setCapacidadMaxima(capacidadMaxima);

            curso.setCreditos(creditos);

            curso.setFechaTermino(fechaTermino);

            if (profesor != null) {
                curso.setProfesorAsignado(profesor);
            }

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

    private void validarCurso(String nombre, String descripcion, Integer capacidadMaxima, Integer creditos, Date fechaTermino) throws MyException {

        if (nombre == null || nombre.isEmpty()) {
            throw new MyException("El nombre no puede ser nulo o estar vacio");
        }

        if (descripcion == null || descripcion.isEmpty()) {
            throw new MyException("La descripcion no puede ser nulo o estar vacio");
        }

        if (capacidadMaxima == null || capacidadMaxima <= 0) {
            throw new MyException("La capacidad maxima debe ser mayor a 0");
        }

        if (capacidadMaxima > 40) {
            throw new MyException("La capacidad maxima no puede ser mayor a 40");
        }

        if (creditos == null || creditos <= 0) {
            throw new MyException("Los creditos deben ser mayor a 0");
        }

        if (creditos > 8) {
            throw new MyException("Los creditos no pueden ser mayor a 8");
        }

        if (fechaTermino == null) {
            throw new MyException("La fecha de termino es obligatoria");
        }

        LocalDate hoy = LocalDate.now();
        LocalDate termino = fechaTermino.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (termino.isBefore(hoy)) {
            throw new MyException("La fecha de termino no puede ser anterior a la fecha de creacion");
        }

    }

    private Usuario resolverProfesorSiExiste(String profesorReferencia) throws MyException {
        if (profesorReferencia == null || profesorReferencia.isBlank()) {
            return null;
        }

        Usuario profesor = null;
        Optional<Usuario> porId = usuarioRepositorio.findById(profesorReferencia);
        if (porId.isPresent()) {
            profesor = porId.get();
        } else {
            profesor = usuarioRepositorio.buscarPorEmail(profesorReferencia);
        }

        if (profesor == null) {
            throw new MyException("No se encontro el profesor asignado");
        }

        if (!Rol.PROFESOR.equals(profesor.getRol())) {
            throw new MyException("El usuario asignado no tiene rol PROFESOR");
        }

        return profesor;
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

        Optional<Usuario> respuesta1 = usuarioRepositorio.findById(idUser);
        Usuario usuario = respuesta1.get();

        Optional<Curso> respuesta2 = cursoRepositorio.findById(idCurso);
        Curso curso = respuesta2.get();

        Inscripcion inscripcion = inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso(idUser, idCurso);

        String estadoObjetivo = Rol.ALUMNO.equals(usuario.getRol()) ? "APROBADO" : "PENDIENTE";

        if (Rol.ALUMNO.equals(usuario.getRol())) {
            validarCapacidadCurso(curso);
        }

        if (inscripcion != null) {

            if (inscripcion.getEstado().equals(estadoObjetivo)) {
                throw new MyException("Ya tienes una inscripcion con estado " + estadoObjetivo + ".");
            }

            inscripcion.setEstado(estadoObjetivo);
            inscripcion.setFechaCreacion(new Date());
            inscripcionRepositorio.save(inscripcion);
        } else {
            Inscripcion inscripcionNuevo = new Inscripcion(new Date(), estadoObjetivo, usuario, curso);

            inscripcionRepositorio.save(inscripcionNuevo);

        }

    }

    private void validarCapacidadCurso(Curso curso) throws MyException {
        if (curso.getCapacidadMaxima() == null) {
            return;
        }

        Long alumnosAprobados = inscripcionRepositorio.contarAlumnosAprobadosPorCurso(curso.getId());
        if (alumnosAprobados >= curso.getCapacidadMaxima()) {
            throw new MyException("Salon lleno");
        }
    }
}
