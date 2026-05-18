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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InscripcionServicioTest {

    @Mock
    private InscripcionRepositorio inscripcionRepositorio;

    @Mock
    private CursoRepositorio cursoRepositorio;

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

    // CP-01: Aprobación exitosa de inscripción de profesor
    @Test
    void probarAprobarInscripcionProfesor_Exito() throws MyException {
        // Entradas y comportamiento simulado (Mock)
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);
        when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-1"))
                .thenReturn(new ArrayList<>()); 

        // Proceso
        inscripcionServicio.aprobarInscripcionProfesor("insc-1");

        // Salidas esperadas
        assertEquals("APROBADO", inscripcion.getEstado());
        assertEquals(profesor, curso.getProfesorAsignado());
        verify(cursoRepositorio, times(1)).save(curso);
        verify(inscripcionRepositorio, times(1)).save(inscripcion);
    }

    // CP-02: Rechazo por rol de Alumno
    @Test
    void probarAprobarInscripcionProfesor_RolIncorrecto() {
        // Entradas
        profesor.setRol(Rol.ALUMNO);
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);

        // Proceso y Salida esperada (Excepción)
        MyException exception = assertThrows(MyException.class, () -> {
            inscripcionServicio.aprobarInscripcionProfesor("insc-1");
        });

        assertEquals("La inscripcion no corresponde a un profesor", exception.getMessage());
        verify(cursoRepositorio, never()).save(any()); 
    }

    // CP-03: Cruce de horarios
    @Test
    void probarAprobarInscripcionProfesor_CruceHorarios() {
        // Entradas
        when(inscripcionRepositorio.getById("insc-1")).thenReturn(inscripcion);

        // Horario del curso NUEVO (Lunes 09:00 - 11:00)
        HorarioSesion horarioNuevo = new HorarioSesion();
        horarioNuevo.setDiaSemana("Lunes");
        horarioNuevo.setHoraInicio(LocalTime.of(9, 0));
        horarioNuevo.setHoraFin(LocalTime.of(11, 0));
        
        // Horario del curso EXISTENTE (Lunes 08:00 - 10:00)
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

        // Proceso y Salida esperada (Excepción por cruce)
        MyException exception = assertThrows(MyException.class, () -> {
            inscripcionServicio.aprobarInscripcionProfesor("insc-1");
        });

        assertTrue(exception.getMessage().contains("Cruce de horarios detectado"));
        verify(inscripcionRepositorio, never()).save(any());
    }
}