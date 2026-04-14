package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.CursoServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/curso")
public class CursoControlador {

    @Autowired
    private CursoServicio cursoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/registrar")
    public ResponseEntity<?> registrar() {
        return ResponseEntity.ok(Map.of("mensaje", "Endpoint para registrar curso"));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registro(
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam Integer capacidadMaxima,
            @RequestParam Integer creditos,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaTermino,
            @RequestParam(required = false) String profesorAsignado,
            @RequestParam(required = false) String profesorId) {

        try {
            String profesorReferencia = (profesorId != null && !profesorId.isBlank()) ? profesorId : profesorAsignado;
            cursoServicio.crearCurso(nombre, descripcion, capacidadMaxima, creditos, fechaTermino, profesorReferencia);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Curso registrado correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    //@PreAuthorize("hasAnyRole('ROLE_ADMIN')")
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

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/modificar/{id}")
    public ResponseEntity<Curso> modificar(@PathVariable String id) {
        return ResponseEntity.ok(cursoServicio.buscarPorId(id));
    }

    @PostMapping("/modificar/{id}")
    public ResponseEntity<?> modificar(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam Integer capacidadMaxima,
            @RequestParam Integer creditos,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaTermino,
            @RequestParam(required = false) String profesorAsignado,
            @RequestParam(required = false) String profesorId) {

        try {
            String profesorReferencia = (profesorId != null && !profesorId.isBlank()) ? profesorId : profesorAsignado;
            cursoServicio.modificarCurso(id, nombre, descripcion, capacidadMaxima, creditos, fechaTermino, profesorReferencia);
            return ResponseEntity.ok(cursoServicio.buscarPorId(id));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        try {
            cursoServicio.eliminarCurso(id);
            return ResponseEntity.ok(Map.of("mensaje", "Curso eliminado correctamente"));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/listaDisponiblesProfesor")
    public ResponseEntity<List<Curso>> listarCursosDisponiblesProfesor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        List<Curso> cursos = cursoServicio.listarCursosDisponiblesProfesor(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/listaInscritosProfesor")
    public ResponseEntity<List<Curso>> listarCursosInscritosProfesor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        List<Curso> cursos = cursoServicio.listarCursosInscritosProfesor(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ROLE_ALUMNO')")
    @GetMapping("/listaDisponiblesAlumno")
    public ResponseEntity<List<Curso>> listarCursosDisponiblesAlumno() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        List<Curso> cursos = cursoServicio.listarCursosDisponiblesAlumno(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ROLE_ALUMNO')")
    @GetMapping("/listaInscritosAlumno")
    public ResponseEntity<List<Curso>> listarCursosInscritosAlumno() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        List<Curso> cursos = cursoServicio.listarCursosInscritosAlumno(usuario.getId());
        return ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR', 'ROLE_ALUMNO')")
    @GetMapping("/inscribir/{id}")
    public ResponseEntity<?> inscribirCurso(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);

        try {
            cursoServicio.inscribirCurso(usuario.getId(), id);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Curso inscrito correctamente",
                    "rol", usuario.getRol().name()));
        } catch (MyException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }

    }

}
