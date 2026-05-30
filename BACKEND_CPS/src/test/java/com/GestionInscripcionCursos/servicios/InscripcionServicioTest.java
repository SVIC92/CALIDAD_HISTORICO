package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.*;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.*;
import com.GestionInscripcionCursos.servicios.InscripcionServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InscripcionServicioTest {

    @Mock
    private InscripcionRepositorio inscripcionRepositorio;

    @Mock
    private CursoRepositorio cursoRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private HorarioSesionRepositorio horarioSesionRepositorio;

    @InjectMocks
    private InscripcionServicio inscripcionServicio;

    private Usuario profesor;
    private Curso curso;
    private Inscripcion inscripcion;

    @BeforeEach
    void setUp() {
        profesor = new Usuario();
        profesor.setId("prof-1");
        profesor.setRol(Rol.PROFESOR);

        curso = new Curso();
        curso.setId("curso-1");
        curso.setNombre("Matemáticas");

        inscripcion = new Inscripcion(new Date(), "PENDIENTE", profesor, curso);
        inscripcion.setId("insc-1");
    }

    @Test
    void probarAprobarInscripcionProfesor_Exito() throws MyException {
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);
        when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-1"))
                .thenReturn(new ArrayList<>());

        inscripcionServicio.aprobarInscripcionProfesor("insc-1");

        assertEquals("APROBADO", inscripcion.getEstado());
        assertEquals(profesor, curso.getProfesorAsignado());
        verify(cursoRepositorio, times(1)).save(curso);
        verify(inscripcionRepositorio, times(1)).save(inscripcion);
    }

    @Test
    void probarAprobarInscripcionProfesor_RolIncorrecto() {
        profesor.setRol(Rol.ALUMNO);
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);

        MyException exception = assertThrows(MyException.class, () -> inscripcionServicio.aprobarInscripcionProfesor("insc-1"));

        assertEquals("La inscripcion no corresponde a un profesor", exception.getMessage());
        verify(cursoRepositorio, never()).save(any());
    }

    @Test
    void probarAprobarInscripcionProfesor_CruceHorarios() {
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);

        HorarioSesion horarioNuevo = new HorarioSesion();
        horarioNuevo.setDiaSemana("Lunes");
        horarioNuevo.setHoraInicio(LocalTime.of(9, 0));
        horarioNuevo.setHoraFin(LocalTime.of(11, 0));
        
        Curso cursoExistente = new Curso();
        cursoExistente.setId("curso-2");
        cursoExistente.setNombre("Física");
        HorarioSesion horarioExistente = new HorarioSesion();
        horarioExistente.setDiaSemana("Lunes");
        horarioExistente.setHoraInicio(LocalTime.of(8, 0));
        horarioExistente.setHoraFin(LocalTime.of(10, 0));

        when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-1"))
                .thenReturn(List.of(horarioNuevo));
        when(cursoRepositorio.buscarCursosInscritosProfesor("prof-1"))
                .thenReturn(List.of(cursoExistente));
        when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-2"))
                .thenReturn(List.of(horarioExistente));

        MyException exception = assertThrows(MyException.class, () -> inscripcionServicio.aprobarInscripcionProfesor("insc-1"));

        assertTrue(exception.getMessage().contains("Cruce de horarios detectado"));
        verify(inscripcionRepositorio, never()).save(any());
    }

    @Test
    void probarAprobarInscripcionProfesor_CursoYaTieneOtroProfesor() {
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);
        
        Usuario otroProfesor = new Usuario();
        otroProfesor.setId("prof-999");
        curso.setProfesorAsignado(otroProfesor);

        MyException exception = assertThrows(MyException.class, () -> inscripcionServicio.aprobarInscripcionProfesor("insc-1"));

        assertEquals("El curso ya tiene otro profesor asignado", exception.getMessage());
        verify(inscripcionRepositorio, never()).save(any());
    }

    @Test
    void probarAprobarInscripcionProfesor_MismoProfesorYaAsignado_Exito() throws MyException {
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);
        curso.setProfesorAsignado(profesor);
        
        when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-1")).thenReturn(new ArrayList<>());

        inscripcionServicio.aprobarInscripcionProfesor("insc-1");

        assertEquals("APROBADO", inscripcion.getEstado());
        verify(cursoRepositorio, times(1)).save(curso);
        verify(inscripcionRepositorio, times(1)).save(inscripcion);
    }

    @Test
    void probarAprobarInscripcionProfesor_CursoNuevoSinHorarios_Exito() throws MyException {
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);
        
        when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-1")).thenReturn(new ArrayList<>());

        inscripcionServicio.aprobarInscripcionProfesor("insc-1");

        assertEquals("APROBADO", inscripcion.getEstado());
        verify(cursoRepositorio, never()).buscarCursosInscritosProfesor(anyString());
        verify(inscripcionRepositorio, times(1)).save(inscripcion);
    }

    @Test
    void probarRechazarInscripcionProfesor_Exito() throws MyException {
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);

        inscripcionServicio.rechazarInscripcionProfesor("insc-1");

        assertEquals("RECHAZADO", inscripcion.getEstado());
        verify(inscripcionRepositorio, times(1)).save(inscripcion);
        verify(cursoRepositorio, never()).save(any());
    }

    @Test
    void probarInscribirAlumnoDirecto_Exito() throws MyException {
        Usuario alumno = new Usuario();
        alumno.setId("alu-10");
        alumno.setRol(Rol.ALUMNO);

        Curso cursoDestino = new Curso();
        cursoDestino.setId("curso-10");
        cursoDestino.setNombre("Algoritmos");
        cursoDestino.setCapacidadMaxima(40);
        when(usuarioRepositorio.findById("alu-10")).thenReturn(java.util.Optional.of(alumno));
        when(cursoRepositorio.findById("curso-10")).thenReturn(java.util.Optional.of(cursoDestino));
        when(inscripcionRepositorio.findAll()).thenReturn(new ArrayList<>());
        when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-10")).thenReturn(new ArrayList<>());

        inscripcionServicio.inscribirAlumnoDirecto("alu-10", "curso-10");

        verify(inscripcionRepositorio, times(1)).save(argThat(nuevaInscripcion ->
            nuevaInscripcion.getUsuario().getId().equals("alu-10") &&
            nuevaInscripcion.getCurso().getId().equals("curso-10") &&
            "APROBADO".equals(nuevaInscripcion.getEstado())
        ));
    }

    @Test
    void probarInscribirAlumnoDirecto_AforoLleno_LanzaExcepcion() {
        Usuario alumno = new Usuario();
        alumno.setId("alu-10");
        alumno.setRol(Rol.ALUMNO);

        Curso cursoLleno = new Curso();
        cursoLleno.setId("curso-20");
        cursoLleno.setCapacidadMaxima(2);

        Inscripcion ins1 = new Inscripcion();
        ins1.setCurso(cursoLleno);
        ins1.setEstado("APROBADO");

        Inscripcion ins2 = new Inscripcion();
        ins2.setCurso(cursoLleno);
        ins2.setEstado("APROBADO");

        when(usuarioRepositorio.findById("alu-10")).thenReturn(java.util.Optional.of(alumno));
        when(cursoRepositorio.findById("curso-20")).thenReturn(java.util.Optional.of(cursoLleno));
        when(inscripcionRepositorio.findAll()).thenReturn(List.of(ins1, ins2));

        MyException exception = assertThrows(MyException.class, () -> inscripcionServicio.inscribirAlumnoDirecto("alu-10", "curso-20"));

        assertTrue(exception.getMessage().contains("El cupo/aforo del curso ya se encuentra lleno"));
        verify(inscripcionRepositorio, never()).save(any());
    }
}