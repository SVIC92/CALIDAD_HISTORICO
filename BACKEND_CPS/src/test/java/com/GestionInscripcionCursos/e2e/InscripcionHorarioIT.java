package com.GestionInscripcionCursos.e2e;

import com.GestionInscripcionCursos.e2e.paginas.CursosPage;
import com.GestionInscripcionCursos.e2e.paginas.LoginPage;
import com.GestionInscripcionCursos.e2e.paginas.NavbarPage;
import com.GestionInscripcionCursos.e2e.soporte.ConfiguracionE2E;
import com.GestionInscripcionCursos.e2e.soporte.Interacciones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E de RF05 (validación de cruce de horarios) de punta a punta por la UI:
 * un ADMIN crea dos cursos con horarios que se solapan un mismo día, y un
 * ALUMNO logra inscribirse en el primero pero es bloqueado en el segundo.
 *
 * Réplica a nivel de interfaz del caso CP_RF05_03 cubierto a nivel de
 * servicio en InscripcionServicioTest (ver docs/TDD_RF05_CruceHorarios.md).
 *
 * Requiere tres cuentas ya existentes en la base de datos local/dev:
 *   -De2e.admin.email=...    -De2e.admin.password=...
 *   -De2e.alumno.email=...   -De2e.alumno.password=...
 *   -De2e.profesor.email=...
 * Si faltan las propiedades, el test se OMITE (no falla).
 *
 * Los dos cursos se crean con esa cuenta PROFESOR ya asignada como docente: desde que
 * CursoServicio.inscribirCurso valida profesorAsignado != null para alumnos, un curso
 * sin docente ya no admite autoinscripción y el test fallaría al llamar inscribirme().
 *
 * La hora de los cursos se sortea en cada corrida (ver
 * Interacciones.inicioAleatorioDeMadrugada) porque la cuenta ALUMNO no tiene forma de
 * desinscribirse desde la UI: sus inscripciones de corridas anteriores quedan
 * acumuladas, y un horario fijo terminaría chocando con un curso ya inscrito de una
 * ejecución previa.
 */
class InscripcionHorarioIT extends ConfiguracionE2E {

    @Test
    @DisplayName("RF05: alumno se inscribe en un curso y es bloqueado en otro con horario cruzado")
    void bloqueaInscripcionPorCruceDeHorario() {
        String adminEmail = propiedadRequerida("e2e.admin.email");
        String adminPassword = propiedadRequerida("e2e.admin.password");
        String alumnoEmail = propiedadRequerida("e2e.alumno.email");
        String alumnoPassword = propiedadRequerida("e2e.alumno.password");
        String profesorEmail = propiedadRequerida("e2e.profesor.email");

        String cursoA = Interacciones.nombreUnico("Curso Horario Base");
        String cursoB = Interacciones.nombreUnico("Curso Horario Cruzado");

        // Día + horario aleatorios por corrida: la cuenta ALUMNO acumula inscripciones de
        // corridas anteriores (no hay forma de desinscribirse desde la UI), así que un
        // horario fijo -o con pocos valores posibles- terminaría chocando con un curso ya
        // inscrito de una ejecución previa.
        int diaPosicion = Interacciones.diaAleatorioPosicion();
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime inicioA = Interacciones.inicioAleatorioDeMadrugada();
        LocalTime finA = inicioA.plusHours(2);
        LocalTime inicioB = inicioA.plusMinutes(30);
        LocalTime finB = inicioA.plusHours(1).plusMinutes(30);
        String horaInicioA = inicioA.format(formatoHora);
        String horaFinA = finA.format(formatoHora);
        String horaInicioB = inicioB.format(formatoHora);
        String horaFinB = finB.format(formatoHora);

        new LoginPage(driver, espera, baseUrl).abrir().iniciarSesion(adminEmail, adminPassword);
        capturar("login-admin");
        CursosPage cursosAdmin = new CursosPage(driver, espera, baseUrl).navegar();

        crearCursoConHorario(cursosAdmin, cursoA, diaPosicion, horaInicioA, horaFinA, "curso-a", profesorEmail);
        crearCursoConHorario(cursosAdmin, cursoB, diaPosicion, horaInicioB, horaFinB, "curso-b", profesorEmail); // se solapa con A

        new NavbarPage(driver, espera).cerrarSesion();

        new LoginPage(driver, espera, baseUrl).abrir().iniciarSesion(alumnoEmail, alumnoPassword);
        capturar("login-alumno");
        CursosPage cursosAlumno = new CursosPage(driver, espera, baseUrl).navegar();
        capturar("cursos-disponibles-alumno");

        cursosAlumno.inscribirme(cursoA);
        String mensajeExito = cursosAlumno.esperarMensajeExito();
        capturar("inscripcion-exitosa-curso-a");
        assertTrue(mensajeExito.contains("Inscripción registrada correctamente"),
                "La inscripción al curso A (sin cruce) debería completarse. Mensaje: " + mensajeExito);

        cursosAlumno.inscribirme(cursoB);
        String mensajeError = cursosAlumno.esperarMensajeError();
        capturar("cruce-horario-bloqueado-curso-b");
        assertTrue(mensajeError.contains("Cruce de horarios"),
                "La inscripción al curso B (cruza con A) debería bloquearse por RF05. Mensaje: " + mensajeError);
    }

    private void crearCursoConHorario(CursosPage cursosAdmin, String nombre, int diaPosicion,
                                       String horaInicio, String horaFin, String etiquetaCaptura,
                                       String profesorEmail) {
        cursosAdmin.abrirNuevoCurso();
        cursosAdmin.registrarCurso(
                nombre,
                Interacciones.codigoUnico("SEL-RF05"),
                "Curso generado por la suite Selenium E2E para validar RF05 (cruce de horarios).",
                "Ingenieria de Sistemas",
                30,
                4,
                LocalDate.now().plusMonths(4),
                profesorEmail);
        capturar(etiquetaCaptura + "-registrado");

        cursosAdmin.abrirHorarios(nombre);
        cursosAdmin.agregarHorario(diaPosicion, horaInicio, horaFin);
        capturar(etiquetaCaptura + "-horario-agregado");
        cursosAdmin.cerrarHorarios();
    }
}
