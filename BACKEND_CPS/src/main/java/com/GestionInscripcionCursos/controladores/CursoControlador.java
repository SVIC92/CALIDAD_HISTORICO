package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.dto.CursoRequestDto;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.CursoPrerequisito;
import com.GestionInscripcionCursos.entidades.HorarioSesion;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.CursoServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/curso")
public class CursoControlador {

    private static final String CLAVE_MENSAJE = "mensaje";
    private static final String CLAVE_ERROR = "error";

    @Autowired
    private CursoServicio cursoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/registrar")
    public ResponseEntity<Object> registrar() {
        return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Endpoint para registrar curso"));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/registro")
    public ResponseEntity<Object> registro(
            @RequestParam String nombre,
            @RequestParam(required = false) String codigoCurso,
            @RequestParam String descripcion,
            @RequestParam Integer capacidadMaxima,
            @RequestParam Integer creditos,
            @RequestParam(required = false) Integer ciclo,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaInicio,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaTermino,
            @RequestParam(required = false) Integer horasTeoricas,
            @RequestParam(required = false) Integer horasPracticas,
            @RequestParam(required = false) Integer horasLaboratorio,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String carrera,
            @RequestParam(required = false) String profesorAsignado,
            @RequestParam(required = false) String profesorId) {

        try {
            String profesorReferencia = (profesorId != null && !profesorId.isBlank()) ? profesorId : profesorAsignado;
            cursoServicio.crearCurso(new CursoRequestDto(
                nombre,
                codigoCurso,
                descripcion,
                capacidadMaxima,
                creditos,
                ciclo,
                modalidad,
                fechaInicio,
                fechaTermino,
                horasTeoricas,
                horasPracticas,
                horasLaboratorio,
                estado,
                profesorReferencia,
                carrera
            ));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(CLAVE_MENSAJE, "Curso registrado correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    //@PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/lista")
    public ResponseEntity<List<Curso>> listar() {
        return ResponseEntity.ok(cursoServicio.listarCursos());
    }

    @GetMapping("/lista/activos")
    public ResponseEntity<List<Curso>> listarActivos() {
        return ResponseEntity.ok(cursoServicio.listarCursosActivos());
    }

    @GetMapping("/lista/caducados")
    public ResponseEntity<List<Curso>> listarCaducados() {
        return ResponseEntity.ok(cursoServicio.listarCursosCaducados());
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/modificar/{id}")
    public ResponseEntity<Curso> modificar(@PathVariable String id) {
        return ResponseEntity.ok(cursoServicio.buscarPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/modificar/{id}")
    public ResponseEntity<Object> modificar(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam(required = false) String codigoCurso,
            @RequestParam String descripcion,
            @RequestParam Integer capacidadMaxima,
            @RequestParam Integer creditos,
            @RequestParam(required = false) Integer ciclo,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaInicio,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaTermino,
            @RequestParam(required = false) Integer horasTeoricas,
            @RequestParam(required = false) Integer horasPracticas,
            @RequestParam(required = false) Integer horasLaboratorio,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String carrera,
            @RequestParam(required = false) String profesorAsignado,
            @RequestParam(required = false) String profesorId) {

        try {
            String profesorReferencia = (profesorId != null && !profesorId.isBlank()) ? profesorId : profesorAsignado;
            cursoServicio.modificarCurso(id, new CursoRequestDto(
                    nombre,
                    codigoCurso,
                    descripcion,
                    capacidadMaxima,
                    creditos,
                    ciclo,
                    modalidad,
                    fechaInicio,
                    fechaTermino,
                    horasTeoricas,
                    horasPracticas,
                    horasLaboratorio,
                    estado,
                    profesorReferencia,
                    carrera
            ));
            return ResponseEntity.ok(cursoServicio.buscarPorId(id));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }

    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable String id) {
        try {
            cursoServicio.eliminarCurso(id);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Curso eliminado correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping("/listaDisponiblesProfesor")
    public ResponseEntity<List<Curso>> listarCursosDisponiblesProfesor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        List<Curso> cursos = cursoServicio.listarCursosDisponiblesProfesor(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping("/listaInscritosProfesor")
    public ResponseEntity<List<Curso>> listarCursosInscritosProfesor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        List<Curso> cursos = cursoServicio.listarCursosInscritosProfesor(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ALUMNO')")
    @GetMapping("/listaDisponiblesAlumno")
    public ResponseEntity<List<Curso>> listarCursosDisponiblesAlumno() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        List<Curso> cursos = cursoServicio.listarCursosDisponiblesAlumno(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ALUMNO')")
    @GetMapping("/listaInscritosAlumno")
    public ResponseEntity<List<Curso>> listarCursosInscritosAlumno() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        List<Curso> cursos = cursoServicio.listarCursosInscritosAlumno(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('PROFESOR', 'ALUMNO')")
    @PostMapping("/inscribir/{id}")
    public ResponseEntity<Object> inscribirCurso(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        try {
            cursoServicio.inscribirCurso(usuario.getId(), id);
            return ResponseEntity.ok(Map.of(
                    CLAVE_MENSAJE, "Curso inscrito correctamente",
                    "rol", usuario.getRol().name()));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @PostMapping("/{idCurso}/horarios/agregar")
    public ResponseEntity<Object> agregarHorario(
            @PathVariable String idCurso,
            @RequestParam String diaSemana,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin,
            @RequestParam(required = false) String aula,
            @RequestParam(required = false) String modalidad
    ) {
        try {
            HorarioSesion horario = cursoServicio.agregarHorario(idCurso, diaSemana, horaInicio, horaFin, aula, modalidad);
            return ResponseEntity.status(HttpStatus.CREATED).body(horario);
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @GetMapping("/{idCurso}/horarios")
    public ResponseEntity<List<HorarioSesion>> listarHorarios(@PathVariable String idCurso) {
        return ResponseEntity.ok(cursoServicio.listarHorariosCurso(idCurso));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @DeleteMapping("/{idCurso}/horarios/{idHorario}")
    public ResponseEntity<Object> eliminarHorario(
            @PathVariable String idCurso,
            @PathVariable String idHorario
    ) {
        try {
            cursoServicio.eliminarHorario(idCurso, idHorario);
            return ResponseEntity.ok(Map.of(CLAVE_MENSAJE, "Horario eliminado correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    @PostMapping("/{idCurso}/prerequisitos/agregar")
    public ResponseEntity<Object> agregarPrerequisito(
            @PathVariable String idCurso,
            @RequestParam String idCursoPrerequisito,
            @RequestParam(required = false) Boolean obligatorio,
            @RequestParam(required = false) String observacion
    ) {
        try {
            CursoPrerequisito prerequisito = cursoServicio.agregarPrerequisito(idCurso, idCursoPrerequisito, obligatorio, observacion);
            return ResponseEntity.status(HttpStatus.CREATED).body(prerequisito);
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
    }

    @GetMapping("/{idCurso}/prerequisitos")
    public ResponseEntity<List<CursoPrerequisito>> listarPrerequisitos(@PathVariable String idCurso) {
        return ResponseEntity.ok(cursoServicio.listarPrerequisitosCurso(idCurso));
    }

    @GetMapping("/horarios/profesor/{profesorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    public ResponseEntity<List<HorarioSesion>> listarHorariosProfesor(@PathVariable String profesorId) {
        return ResponseEntity.ok(cursoServicio.listarHorariosPorProfesor(profesorId));
    }
    @GetMapping("/horarios/alumno")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<List<HorarioSesion>> listarMisHorarios(Authentication authentication) {
        Usuario usuario = usuarioServicio.buscarEmail(authentication.getName());
        return ResponseEntity.ok(cursoServicio.listarHorariosPorAlumno(usuario.getId()));
    }
}
