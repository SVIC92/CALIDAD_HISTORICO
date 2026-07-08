package com.GestionInscripcionCursos.e2e;

import com.GestionInscripcionCursos.e2e.paginas.CursosPage;
import com.GestionInscripcionCursos.e2e.paginas.LoginPage;
import com.GestionInscripcionCursos.e2e.soporte.ConfiguracionE2E;
import com.GestionInscripcionCursos.e2e.soporte.Interacciones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E: CRUD de cursos desde la UI, solo disponible para ROLE_ADMIN
 * (ver canManageCursos en CursosListado.jsx).
 *
 * Requiere una cuenta ADMIN ya existente en la base de datos local/dev:
 *   -De2e.admin.email=... -De2e.admin.password=...
 * Si no se proveen, el test se OMITE (no falla).
 */
class CursoCrudIT extends ConfiguracionE2E {

    @Test
    @DisplayName("Un ADMIN puede crear, editar y eliminar un curso desde el listado")
    void crearEditarYEliminarCurso() {
        String adminEmail = propiedadRequerida("e2e.admin.email");
        String adminPassword = propiedadRequerida("e2e.admin.password");

        new LoginPage(driver, espera, baseUrl).abrir().iniciarSesion(adminEmail, adminPassword);
        capturar("login-admin");

        CursosPage cursos = new CursosPage(driver, espera, baseUrl).navegar();
        capturar("listado-cursos");

        String nombreCurso = Interacciones.nombreUnico("Curso Prueba Selenium");
        cursos.abrirNuevoCurso();
        capturar("formulario-nuevo-curso");
        cursos.registrarCurso(
                nombreCurso,
                Interacciones.codigoUnico("SEL-CRUD"),
                "Curso generado por la suite Selenium E2E para validar el CRUD.",
                "Ingenieria de Sistemas",
                30,
                4,
                LocalDate.now().plusMonths(4));

        assertTrue(cursos.esperarFila(nombreCurso).isDisplayed(),
                "El curso recién creado debería aparecer en la tabla");
        capturar("curso-registrado");

        String nombreEditado = nombreCurso + " Editado";
        cursos.editarNombre(nombreCurso, nombreEditado);

        assertTrue(cursos.esperarFila(nombreEditado).isDisplayed(),
                "El curso debería reflejar el nuevo nombre tras editar");
        capturar("curso-editado");

        cursos.eliminarCurso(nombreEditado);
        capturar("curso-eliminado"); // mensaje de éxito aún visible

        // "Eliminar" es soft-delete: la fila sigue en la tabla, pero pasa a estado INACTIVO.
        assertTrue(cursos.estadoDeFila(nombreEditado).equalsIgnoreCase("INACTIVO"),
                "El curso eliminado debería quedar en estado INACTIVO");
        capturar("curso-estado-inactivo");
    }
}
