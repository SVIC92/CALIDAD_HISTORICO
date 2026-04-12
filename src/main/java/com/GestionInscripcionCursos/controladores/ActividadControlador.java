package com.GestionInscripcionCursos.controladores;

import com.GestionInscripcionCursos.entidades.Actividad;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.servicios.ActividadServicio;
import com.GestionInscripcionCursos.servicios.CursoServicio;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//prueba
@Controller
@RequestMapping("/actividad")
public class ActividadControlador {

    @Autowired
    private ActividadServicio actividadServicio;

    @Autowired
    private CursoServicio cursoServicio;
    
    @Autowired
    private UsuarioServicio usuarioServicio;

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/registrar/{id}")
    public String registrar(
            @PathVariable String id,
            ModelMap modelo) {
        modelo.put("curso", cursoServicio.buscarPorId(id));
        return "VistaRegistrarActividad.html";
    }

    @PostMapping("/registro/{id}")
    public String registro(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            RedirectAttributes redirectAttributes) {

        try {
            actividadServicio.crearActividad(nombre, descripcion, id);
            redirectAttributes.addFlashAttribute("exito", "Actividad Registrada Correctamente!");
            return "redirect:/curso/listaInscritosProfesor";
        } catch (MyException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/actividad/registrar/" + id;
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR', 'ROLE_ALUMNO')")
    @GetMapping("/listar/{id}")
    public String listar(
            @PathVariable String id,
            ModelMap modelo) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String emailUser = authentication.getName();

        Usuario usuario = usuarioServicio.buscarEmail(emailUser);
        
        Rol rol = usuario.getRol();
        
        modelo.addAttribute("rol", rol);

        List<Actividad> actividades = actividadServicio.listarActividadesPorIdCurso(id);
        modelo.addAttribute("actividades", actividades);

        return "VistaListarActividades.html";
    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/modificar/{id}")
    public String modificar(
            @PathVariable String id,
            ModelMap modelo) {

        modelo.put("actividad", actividadServicio.buscarPorId(id));

        return "VistaModificarActividad.html";
    }

    @PostMapping("/modificar/{id}")
    public String modificar(
            @PathVariable String id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            RedirectAttributes redirectAttributes) {

        Actividad actividad = actividadServicio.buscarPorId(id);

        try {

            actividadServicio.modificarActividad(id, nombre, descripcion);

            redirectAttributes.addFlashAttribute("exito", "Actividad Modificado Correctamente!");

            return "redirect:/actividad/listar/" + actividad.getCurso().getId();

        } catch (MyException ex) {

            redirectAttributes.addFlashAttribute("error", ex.getMessage());

            return "redirect:/actividad/modificar/" + actividad.getId();
        }

    }

    @PreAuthorize("hasAnyRole('ROLE_PROFESOR')")
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable String id, RedirectAttributes redirectAttributes) {

        Actividad actividad = actividadServicio.buscarPorId(id);

        try {
            actividadServicio.eliminarActividad(id);

            redirectAttributes.addFlashAttribute("exito", "Actividad Eliminado Correctamente!");

        } catch (MyException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/actividad/listar/" + actividad.getCurso().getId();
    }

}
