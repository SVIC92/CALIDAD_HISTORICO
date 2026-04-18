package com.GestionInscripcionCursos.servicios;


import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.CursoPrerequisito;
import com.GestionInscripcionCursos.entidades.HorarioSesion;
import com.GestionInscripcionCursos.entidades.Inscripcion;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.EstadoCurso;
import com.GestionInscripcionCursos.enumeraciones.ModalidadCurso;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CarreraRepositorio;
import com.GestionInscripcionCursos.repositorios.CursoPrerequisitoRepositorio;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.HorarioSesionRepositorio;
import com.GestionInscripcionCursos.repositorios.InscripcionRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
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

    @Autowired
    private CarreraRepositorio carreraRepositorio;

    @Autowired
    private CursoPrerequisitoRepositorio cursoPrerequisitoRepositorio;

    @Autowired
    private HorarioSesionRepositorio horarioSesionRepositorio;

    @Transactional
    public void crearCurso(
            String nombre,
            String codigoCurso,
            String descripcion,
            Integer capacidadMaxima,
            Integer creditos,
            Integer ciclo,
            String modalidad,
            Date fechaInicio,
            Date fechaTermino,
            Integer horasTeoricas,
            Integer horasPracticas,
            Integer horasLaboratorio,
            String estado,
            String profesorReferencia,
            String carreraReferencia
    ) throws MyException {

        String codigoNormalizado = normalizarCodigoCurso(codigoCurso, nombre);
        if (cursoRepositorio.existsByCodigoCursoIgnoreCase(codigoNormalizado)) {
            throw new MyException("Ya existe un curso con el codigo " + codigoNormalizado);
        }

        Integer cicloNormalizado = (ciclo == null || ciclo <= 0) ? 1 : ciclo;
        Date fechaInicioNormalizada = fechaInicio == null ? new Date() : fechaInicio;

        Usuario profesor = resolverProfesorSiExiste(profesorReferencia);
        Carrera carrera = resolverCarreraSiExiste(carreraReferencia);

        ModalidadCurso modalidadCurso = parsearModalidad(modalidad);
        EstadoCurso estadoCurso = parsearEstado(estado);
        validarCurso(
                nombre,
                codigoNormalizado,
                descripcion,
                capacidadMaxima,
                creditos,
                cicloNormalizado,
                fechaInicioNormalizada,
                fechaTermino,
                horasTeoricas,
                horasPracticas,
                horasLaboratorio
        );

        Curso curso = new Curso(
                nombre,
                codigoNormalizado,
                descripcion,
                cicloNormalizado,
                modalidadCurso,
                capacidadMaxima,
                creditos,
                fechaInicioNormalizada,
                fechaTermino,
                normalizarHoras(horasTeoricas),
                normalizarHoras(horasPracticas),
                normalizarHoras(horasLaboratorio),
                estadoCurso,
                profesor,
                carrera
        );

        cursoRepositorio.save(curso);
    }

    public List<Curso> listarCursos() {
        return cursoRepositorio.buscarCursos();
    }

    public List<Curso> listarCursosActivos() {
        return cursoRepositorio.buscarCursosActivosPorEstado(new Date(), EstadoCurso.ACTIVO);
    }

    public List<Curso> listarCursosCaducados() {
        return cursoRepositorio.buscarCursosCaducados(new Date());
    }

    @Transactional
    public void modificarCurso(
            String id,
            String nombre,
            String codigoCurso,
            String descripcion,
            Integer capacidadMaxima,
            Integer creditos,
            Integer ciclo,
            String modalidad,
            Date fechaInicio,
            Date fechaTermino,
            Integer horasTeoricas,
            Integer horasPracticas,
            Integer horasLaboratorio,
            String estado,
            String profesorReferencia,
            String carreraReferencia
    ) throws MyException {

        Usuario profesor = resolverProfesorSiExiste(profesorReferencia);
        Carrera carrera = resolverCarreraSiExiste(carreraReferencia);

        ModalidadCurso modalidadCurso = parsearModalidad(modalidad);
        EstadoCurso estadoCurso = parsearEstado(estado);
        String codigoNormalizado = normalizarCodigoCurso(codigoCurso, nombre);
        Integer cicloNormalizado = (ciclo == null || ciclo <= 0) ? 1 : ciclo;
        Date fechaInicioNormalizada = fechaInicio == null ? new Date() : fechaInicio;

        validarCurso(
                nombre,
                codigoNormalizado,
                descripcion,
                capacidadMaxima,
                creditos,
            cicloNormalizado,
            fechaInicioNormalizada,
                fechaTermino,
                horasTeoricas,
                horasPracticas,
                horasLaboratorio
        );

        Optional<Curso> respuesta = cursoRepositorio.findById(id);

        if (respuesta.isPresent()) {

            Curso curso = respuesta.get();

            if (!curso.getCodigoCurso().equalsIgnoreCase(codigoNormalizado)
                    && cursoRepositorio.existsByCodigoCursoIgnoreCase(codigoNormalizado)) {
                throw new MyException("Ya existe otro curso con el codigo " + codigoNormalizado);
            }

            curso.setNombre(nombre);

            curso.setCodigoCurso(codigoNormalizado);

            curso.setDescripcion(descripcion);

            curso.setCiclo(cicloNormalizado);

            curso.setModalidad(modalidadCurso);

            curso.setCapacidadMaxima(capacidadMaxima);

            curso.setCreditos(creditos);

            curso.setFechaInicio(fechaInicioNormalizada);

            curso.setFechaTermino(fechaTermino);

            curso.setHorasTeoricas(normalizarHoras(horasTeoricas));

            curso.setHorasPracticas(normalizarHoras(horasPracticas));

            curso.setHorasLaboratorio(normalizarHoras(horasLaboratorio));

            curso.setEstado(estadoCurso);

            curso.setCarrera(carrera);

            if (profesor != null) {
                curso.setProfesorAsignado(profesor);
            }

            cursoRepositorio.save(curso);

        }
    }

    @Transactional
    public void eliminarCurso(String id) throws MyException {

        Curso curso = cursoRepositorio.getById(id);
        curso.setEstado(EstadoCurso.INACTIVO);
        cursoRepositorio.save(curso);

    }

    public Curso buscarPorId(String id) {
        return cursoRepositorio.buscarPorId(id);
    }

        private void validarCurso(
            String nombre,
            String codigoCurso,
            String descripcion,
            Integer capacidadMaxima,
            Integer creditos,
            Integer ciclo,
            Date fechaInicio,
            Date fechaTermino,
            Integer horasTeoricas,
            Integer horasPracticas,
            Integer horasLaboratorio
        ) throws MyException {

        if (nombre == null || nombre.isEmpty()) {
            throw new MyException("El nombre no puede ser nulo o estar vacio");
        }

        if (codigoCurso == null || codigoCurso.isBlank()) {
            throw new MyException("El codigo de curso es obligatorio");
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

        if (ciclo == null || ciclo <= 0) {
            throw new MyException("El ciclo debe ser mayor a 0");
        }

        if (ciclo > 14) {
            throw new MyException("El ciclo no puede ser mayor a 14");
        }

        if (fechaInicio == null) {
            throw new MyException("La fecha de inicio es obligatoria");
        }

        if (fechaTermino == null) {
            throw new MyException("La fecha de termino es obligatoria");
        }

        LocalDate inicio = fechaInicio.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate termino = fechaTermino.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (termino.isBefore(inicio)) {
            throw new MyException("La fecha de termino no puede ser anterior a la fecha de inicio");
        }

        int sumaHoras = normalizarHoras(horasTeoricas) + normalizarHoras(horasPracticas) + normalizarHoras(horasLaboratorio);
        if (sumaHoras <= 0) {
            throw new MyException("Debe registrar al menos una hora academica entre HT/HP/HL");
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

    private Carrera resolverCarreraSiExiste(String carreraReferencia) {
        if (carreraReferencia == null || carreraReferencia.isBlank()) {
            return null;
        }

        Optional<Carrera> porId = carreraRepositorio.findById(carreraReferencia);
        if (porId.isPresent()) {
            return porId.get();
        }

        return carreraRepositorio.findByNombreIgnoreCase(carreraReferencia)
                .orElseGet(() -> {
                    Carrera nuevaCarrera = new Carrera();
                    nuevaCarrera.setNombre(carreraReferencia.trim());
                    nuevaCarrera.setCodigo(generarCodigoCarreraUnico(carreraReferencia));
                    nuevaCarrera.setDescripcion("Programa academico generado automaticamente");
                    return carreraRepositorio.save(nuevaCarrera);
                });
    }

    private String generarCodigoCarreraUnico(String carreraReferencia) {
        String base = generarCodigoCarrera(carreraReferencia);
        String candidato = base;
        int indice = 1;
        while (carreraRepositorio.findByCodigoIgnoreCase(candidato).isPresent()) {
            candidato = (base + indice);
            if (candidato.length() > 10) {
                candidato = candidato.substring(0, 10);
            }
            indice++;
        }
        return candidato;
    }

    private String generarCodigoCarrera(String carreraReferencia) {
        String texto = carreraReferencia.trim().toUpperCase().replaceAll("[^A-Z0-9 ]", "");
        String[] partes = texto.split("\\s+");
        StringBuilder codigo = new StringBuilder();
        for (String parte : partes) {
            if (!parte.isBlank()) {
                codigo.append(parte.charAt(0));
            }
            if (codigo.length() == 6) {
                break;
            }
        }
        if (codigo.length() < 3) {
            codigo.append("CAR");
        }
        return codigo.substring(0, Math.min(codigo.length(), 10));
    }

    private String normalizarCodigoCurso(String codigoCurso, String nombreCurso) {
        if (codigoCurso != null && !codigoCurso.isBlank()) {
            return codigoCurso.trim().toUpperCase();
        }
        String base = nombreCurso == null ? "CURSO" : nombreCurso.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (base.length() < 4) {
            base = (base + "CURSO").substring(0, 5);
        }
        return base.substring(0, Math.min(base.length(), 10));
    }

    private ModalidadCurso parsearModalidad(String modalidad) throws MyException {
        if (modalidad == null || modalidad.isBlank()) {
            return ModalidadCurso.PRESENCIAL;
        }

        try {
            return ModalidadCurso.valueOf(modalidad.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new MyException("Modalidad invalida. Usa PRESENCIAL, VIRTUAL o HIBRIDO");
        }
    }

    private EstadoCurso parsearEstado(String estado) throws MyException {
        if (estado == null || estado.isBlank()) {
            return EstadoCurso.ACTIVO;
        }

        try {
            return EstadoCurso.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new MyException("Estado invalido. Usa ACTIVO, INACTIVO o CERRADO");
        }
    }

    private int normalizarHoras(Integer horas) throws MyException {
        if (horas == null) {
            return 0;
        }
        if (horas < 0) {
            throw new MyException("Las horas no pueden ser negativas");
        }
        if (horas > 20) {
            throw new MyException("Las horas por bloque no pueden ser mayores a 20");
        }
        return horas;
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

    public List<HorarioSesion> listarHorariosCurso(String idCurso) {
        return horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(idCurso);
    }

    public List<CursoPrerequisito> listarPrerequisitosCurso(String idCurso) {
        return cursoPrerequisitoRepositorio.findByCursoId(idCurso);
    }

    @Transactional
    public HorarioSesion agregarHorario(
            String idCurso,
            String diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            String aula,
            String modalidad
    ) throws MyException {
        if (diaSemana == null || diaSemana.isBlank()) {
            throw new MyException("El dia de la semana es obligatorio");
        }
        if (horaInicio == null || horaFin == null) {
            throw new MyException("La hora de inicio y fin son obligatorias");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new MyException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Curso curso = cursoRepositorio.findById(idCurso)
                .orElseThrow(() -> new MyException("Curso no encontrado"));

        HorarioSesion horario = new HorarioSesion();
        horario.setCurso(curso);
        horario.setDiaSemana(diaSemana.trim());
        horario.setHoraInicio(horaInicio);
        horario.setHoraFin(horaFin);
        horario.setAula(aula);
        horario.setModalidad(parsearModalidad(modalidad));

        return horarioSesionRepositorio.save(horario);
    }

    @Transactional
    public CursoPrerequisito agregarPrerequisito(
            String idCurso,
            String idCursoPrerequisito,
            Boolean obligatorio,
            String observacion
    ) throws MyException {
        if (idCurso.equals(idCursoPrerequisito)) {
            throw new MyException("Un curso no puede ser prerrequisito de si mismo");
        }

        Curso curso = cursoRepositorio.findById(idCurso)
                .orElseThrow(() -> new MyException("Curso destino no encontrado"));
        Curso prerequisito = cursoRepositorio.findById(idCursoPrerequisito)
                .orElseThrow(() -> new MyException("Curso prerrequisito no encontrado"));

        List<CursoPrerequisito> existentes = cursoPrerequisitoRepositorio.findByCursoId(idCurso);
        boolean repetido = existentes.stream()
                .anyMatch(cp -> cp.getPrerrequisito() != null
                        && cp.getPrerrequisito().getId().equals(idCursoPrerequisito));
        if (repetido) {
            throw new MyException("El prerrequisito ya fue registrado para este curso");
        }

        CursoPrerequisito relacion = new CursoPrerequisito();
        relacion.setCurso(curso);
        relacion.setPrerrequisito(prerequisito);
        relacion.setObligatorio(obligatorio == null ? Boolean.TRUE : obligatorio);
        relacion.setObservacion(observacion);
        return cursoPrerequisitoRepositorio.save(relacion);
    }

    @Transactional
    public void inscribirCurso(String idUser, String idCurso) throws MyException {

        Optional<Usuario> respuesta1 = usuarioRepositorio.findById(idUser);
        Usuario usuario = respuesta1.get();

        Optional<Curso> respuesta2 = cursoRepositorio.findById(idCurso);
        Curso curso = respuesta2.get();

        if (!EstadoCurso.ACTIVO.equals(curso.getEstado())) {
            throw new MyException("Solo puedes inscribirte en cursos con estado ACTIVO");
        }

        Inscripcion inscripcion = inscripcionRepositorio.buscarInscripcionPorIdUserIdCurso(idUser, idCurso);

        String estadoObjetivo = Rol.ALUMNO.equals(usuario.getRol()) ? "APROBADO" : "PENDIENTE";

        if (Rol.ALUMNO.equals(usuario.getRol())) {
            validarCapacidadCurso(curso);
            validarPrerequisitosAlumno(idUser, idCurso);
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

    private void validarPrerequisitosAlumno(String idUser, String idCurso) throws MyException {
        List<CursoPrerequisito> prerequisitos = cursoPrerequisitoRepositorio.findByCursoId(idCurso);
        for (CursoPrerequisito prerequisito : prerequisitos) {
            if (Boolean.FALSE.equals(prerequisito.getObligatorio())) {
                continue;
            }

            Curso cursoRequerido = prerequisito.getPrerrequisito();
            if (cursoRequerido == null) {
                continue;
            }

            Boolean aprobado = inscripcionRepositorio.existeInscripcionAprobada(idUser, cursoRequerido.getId());
            if (!Boolean.TRUE.equals(aprobado)) {
                throw new MyException("Debes aprobar el prerrequisito " + cursoRequerido.getCodigoCurso() + " - " + cursoRequerido.getNombre() + " antes de inscribirte.");
            }
        }
    }
}
