package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CarreraRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link UsuarioServicio}: validaciones de registro,
 * autenticacion (loadUserByUsername) y gestion administrativa de usuarios.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private CarreraRepositorio carreraRepositorio;

    @Mock
    private AuditoriaServicio auditoriaServicio;

    @InjectMocks
    private UsuarioServicio usuarioServicio;

    private Usuario usuario(String id, String email, Rol rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre("Usuario " + id);
        u.setPassword("hash");
        u.setRol(rol);
        u.setActivo(activo);
        return u;
    }

    // =====================================================================
    // registrar
    // =====================================================================
    @Nested
    @DisplayName("registrar: validaciones del alumno")
    class Registrar {

        @Test
        @DisplayName("con datos validos guarda el usuario como ALUMNO activo")
        void registrarConDatosValidosGuardaAlumno() throws MyException {
            when(usuarioRepositorio.buscarPorEmail("nuevo@dominio.com")).thenReturn(null);

            usuarioServicio.registrar("Ana Torres", "nuevo@dominio.com", "clave123", "clave123");

            verify(usuarioRepositorio).save(argThatUsuario(u ->
                    u.getRol() == Rol.ALUMNO
                    && u.isActivo()
                    && !u.isTwoFactorEnabled()
                    && u.getCicloActual() == 1
                    && u.getEmail().equals("nuevo@dominio.com")));
        }

        @Test
        @DisplayName("normaliza email a minusculas y recorta espacios")
        void registrarNormalizaEmail() throws MyException {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            usuarioServicio.registrar("  Ana  ", "  ANA@DOMINIO.COM  ", "clave123", "clave123");

            verify(usuarioRepositorio).save(argThatUsuario(u -> u.getEmail().equals("ana@dominio.com")));
        }

        @Test
        @DisplayName("con email de formato invalido lanza MyException")
        void registrarConEmailInvalidoLanzaExcepcion() {
            MyException ex = assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "no-es-un-email", "clave123", "clave123"));
            assertTrue(ex.getMessage().contains("formato"));
            verify(usuarioRepositorio, never()).save(any());
        }

        @Test
        @DisplayName("con email ya registrado lanza MyException")
        void registrarConEmailEnUsoLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(usuario("1", "ana@dominio.com", Rol.ALUMNO, true));

            MyException ex = assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "clave123"));
            assertTrue(ex.getMessage().contains("en uso"));
        }

        @Test
        @DisplayName("con contrasenas distintas lanza MyException")
        void registrarConPasswordsDistintasLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            MyException ex = assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "otraClave"));
            assertTrue(ex.getMessage().contains("iguales"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1234"})
        @DisplayName("con contrasena vacia o muy corta lanza MyException")
        void registrarConPasswordInvalidaLanzaExcepcion(String password) {
            lenient().when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", password, password));
        }

        @Test
        @DisplayName("con ciclo fuera de rango [1,14] lanza MyException")
        void registrarConCicloFueraDeRangoLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            MyException ex = assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "clave123", null, 15));
            assertTrue(ex.getMessage().contains("ciclo"));
        }

        @Test
        @DisplayName("resuelve la carrera por id cuando existe")
        void registrarResuelveCarreraPorId() throws MyException {
            Carrera carrera = new Carrera();
            carrera.setId("car-1");
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);
            when(carreraRepositorio.findById("car-1")).thenReturn(Optional.of(carrera));

            usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "clave123", "car-1", 3);

            verify(usuarioRepositorio).save(argThatUsuario(u -> u.getCarrera() == carrera && u.getCicloActual() == 3));
        }

        @Test
        @DisplayName("email cerca del limite de 255 caracteres con formato valido es aceptado")
        void registrarAceptaEmailLargoValido() {
            String localPart = "a".repeat(64);
            String email = localPart + "@" + "b".repeat(60) + ".com";
            when(usuarioRepositorio.buscarPorEmail(email)).thenReturn(null);

            assertDoesNotThrow(() -> usuarioServicio.registrar("Ana", email, "clave123", "clave123"));
        }

        @Test
        @DisplayName("email que excede 255 caracteres es rechazado antes de evaluar el regex")
        void registrarRechazaEmailDemasiadoLargo() {
            String emailGigante = "a".repeat(300) + "@dominio.com";

            MyException ex = assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", emailGigante, "clave123", "clave123"));
            assertTrue(ex.getMessage().contains("255"));
            verify(usuarioRepositorio, never()).buscarPorEmail(anyString());
        }

        // -----------------------------------------------------------
        // Rama faltante: nombre / email nulos o en blanco
        // -----------------------------------------------------------
        @Test
        @DisplayName("con nombre nulo lanza MyException")
        void registrarConNombreNuloLanzaExcepcion() {
            MyException ex = assertThrows(MyException.class, () ->
                    usuarioServicio.registrar(null, "ana@dominio.com", "clave123", "clave123"));
            assertTrue(ex.getMessage().toLowerCase().contains("nombre"));
            verify(usuarioRepositorio, never()).buscarPorEmail(anyString());
        }

        @Test
        @DisplayName("con nombre en blanco lanza MyException")
        void registrarConNombreEnBlancoLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("   ", "ana@dominio.com", "clave123", "clave123"));
        }

        @Test
        @DisplayName("con email nulo lanza MyException")
        void registrarConEmailNuloLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", null, "clave123", "clave123"));
        }

        @Test
        @DisplayName("con email en blanco lanza MyException")
        void registrarConEmailEnBlancoLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "   ", "clave123", "clave123"));
        }

        // -----------------------------------------------------------
        // Rama faltante: password / password2 nulos, boundary y longitud maxima
        // -----------------------------------------------------------
        @Test
        @DisplayName("con password nula lanza MyException")
        void registrarConPasswordNulaLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", null, null));
        }

        @Test
        @DisplayName("con password de exactamente 5 caracteres (limite invalido) lanza MyException")
        void registrarConPasswordDe5CaracteresLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", "12345", "12345"));
        }

        @Test
        @DisplayName("con password de exactamente 6 caracteres (limite valido) es aceptada")
        void registrarConPasswordDe6CaracteresEsAceptada() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            assertDoesNotThrow(() ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", "123456", "123456"));
        }

        @Test
        @DisplayName("con password2 nula lanza MyException")
        void registrarConPassword2NulaLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            MyException ex = assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", null));
            assertTrue(ex.getMessage().contains("confirmar"));
        }

        @Test
        @DisplayName("con password2 en blanco lanza MyException")
        void registrarConPassword2EnBlancoLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "   "));
        }

        @Test
        @DisplayName("con password mayor a 255 caracteres lanza MyException")
        void registrarConPasswordDemasiadoLargaLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);
            String passwordGigante = "a1".repeat(150);

            MyException ex = assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", passwordGigante, passwordGigante));
            assertTrue(ex.getMessage().contains("255"));
        }

        // -----------------------------------------------------------
        // Rama faltante: boundary de cicloActual
        // -----------------------------------------------------------
        @Test
        @DisplayName("con ciclo actual igual a 0 (limite invalido) lanza MyException")
        void registrarConCicloActualCeroLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () ->
                    usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "clave123", null, 0));
        }

        @Test
        @DisplayName("con ciclo actual igual a 14 (limite valido) es aceptado")
        void registrarConCicloActualCatorceEsAceptado() throws MyException {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);

            usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "clave123", null, 14);

            verify(usuarioRepositorio).save(argThatUsuario(u -> u.getCicloActual() == 14));
        }

        // -----------------------------------------------------------
        // Rama faltante: resolverCarreraSiExiste (por nombre / no encontrada)
        // -----------------------------------------------------------
        @Test
        @DisplayName("resuelve la carrera por nombre cuando no se encuentra por id")
        void registrarResuelveCarreraPorNombreCuandoNoExistePorId() throws MyException {
            Carrera carrera = new Carrera();
            carrera.setId("car-2");
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);
            when(carreraRepositorio.findById("Ingenieria")).thenReturn(Optional.empty());
            when(carreraRepositorio.findByNombreIgnoreCase("Ingenieria")).thenReturn(Optional.of(carrera));

            usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "clave123", "Ingenieria", null);

            verify(usuarioRepositorio).save(argThatUsuario(u -> u.getCarrera() == carrera));
        }

        @Test
        @DisplayName("carrera no encontrada ni por id ni por nombre asigna carrera nula sin lanzar")
        void registrarConCarreraInexistenteAsignaNull() throws MyException {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com")).thenReturn(null);
            when(carreraRepositorio.findById("Fantasma")).thenReturn(Optional.empty());
            when(carreraRepositorio.findByNombreIgnoreCase("Fantasma")).thenReturn(Optional.empty());

            usuarioServicio.registrar("Ana", "ana@dominio.com", "clave123", "clave123", "Fantasma", null);

            verify(usuarioRepositorio).save(argThatUsuario(u -> u.getCarrera() == null));
        }
    }

    // =====================================================================
    // crearOActualizarAdminPrueba
    // =====================================================================
    @Nested
    @DisplayName("crearOActualizarAdminPrueba")
    class CrearOActualizarAdminPrueba {

        @Test
        @DisplayName("crea un admin nuevo cuando el email no existe")
        void creaAdminNuevo() throws MyException {
            when(usuarioRepositorio.buscarPorEmail("admin@dominio.com")).thenReturn(null);

            usuarioServicio.crearOActualizarAdminPrueba("Admin", "admin@dominio.com", "clave123");

            verify(usuarioRepositorio).save(argThatUsuario(u -> u.getRol() == Rol.ADMIN && u.isActivo()));
        }

        @Test
        @DisplayName("actualiza el admin existente reutilizando su id")
        void actualizaAdminExistente() throws MyException {
            Usuario existente = usuario("admin-1", "admin@dominio.com", Rol.ALUMNO, false);
            when(usuarioRepositorio.buscarPorEmail("admin@dominio.com")).thenReturn(existente);

            usuarioServicio.crearOActualizarAdminPrueba("Admin", "admin@dominio.com", "clave123");

            verify(usuarioRepositorio).save(argThatUsuario(u -> u.getId().equals("admin-1") && u.getRol() == Rol.ADMIN));
        }

        @Test
        @DisplayName("con password corta lanza MyException")
        void conPasswordCortaLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    usuarioServicio.crearOActualizarAdminPrueba("Admin", "admin@dominio.com", "123"));
        }

        @Test
        @DisplayName("con nombre nulo lanza MyException")
        void conNombreNuloLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    usuarioServicio.crearOActualizarAdminPrueba(null, "admin@dominio.com", "clave123"));
        }

        @Test
        @DisplayName("con email nulo lanza MyException")
        void conEmailNuloLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    usuarioServicio.crearOActualizarAdminPrueba("Admin", null, "clave123"));
        }

        @Test
        @DisplayName("con password nula lanza MyException")
        void conPasswordNulaLanzaExcepcion() {
            assertThrows(MyException.class, () ->
                    usuarioServicio.crearOActualizarAdminPrueba("Admin", "admin@dominio.com", null));
        }
    }

    // =====================================================================
    // loadUserByUsername (Spring Security)
    // =====================================================================
    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("usuario activo retorna UserDetails con authority ROLE_<rol>")
        void usuarioActivoRetornaUserDetails() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com"))
                    .thenReturn(usuario("1", "ana@dominio.com", Rol.PROFESOR, true));

            UserDetails detalles = usuarioServicio.loadUserByUsername("ana@dominio.com");

            assertEquals("ana@dominio.com", detalles.getUsername());
            assertTrue(detalles.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PROFESOR")));
        }

        @Test
        @DisplayName("usuario inactivo lanza UsernameNotFoundException")
        void usuarioInactivoLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("ana@dominio.com"))
                    .thenReturn(usuario("1", "ana@dominio.com", Rol.ALUMNO, false));

            assertThrows(UsernameNotFoundException.class, () ->
                    usuarioServicio.loadUserByUsername("ana@dominio.com"));
        }

        @Test
        @DisplayName("usuario inexistente lanza UsernameNotFoundException")
        void usuarioInexistenteLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("nadie@dominio.com")).thenReturn(null);

            assertThrows(UsernameNotFoundException.class, () ->
                    usuarioServicio.loadUserByUsername("nadie@dominio.com"));
        }
    }

    // =====================================================================
    // buscarPorId
    // =====================================================================
    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("con id nulo o vacio lanza MyException")
        void idNuloLanzaExcepcion() {
            assertThrows(MyException.class, () -> usuarioServicio.buscarPorId(" "));
        }

        @Test
        @DisplayName("con id no encontrado lanza MyException")
        void idNoEncontradoLanzaExcepcion() {
            when(usuarioRepositorio.findById("x")).thenReturn(Optional.empty());
            assertThrows(MyException.class, () -> usuarioServicio.buscarPorId("x"));
        }

        @Test
        @DisplayName("con id existente retorna el usuario")
        void idExistenteRetornaUsuario() throws MyException {
            Usuario u = usuario("1", "ana@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(u));

            assertSame(u, usuarioServicio.buscarPorId("1"));
        }
    }

    // =====================================================================
    // crearUsuarioAdmin / actualizarUsuarioAdmin
    // =====================================================================
    @Nested
    @DisplayName("crearUsuarioAdmin y actualizarUsuarioAdmin")
    class GestionAdministrativa {

        @Test
        @DisplayName("crearUsuarioAdmin con rol invalido lanza MyException")
        void crearConRolInvalidoLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("nuevo@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () -> usuarioServicio.crearUsuarioAdmin(
                    "Nuevo", "nuevo@dominio.com", "clave123", "SUPERADMIN", null, null));
        }

        @Test
        @DisplayName("crearUsuarioAdmin con email en uso lanza MyException")
        void crearConEmailEnUsoLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("nuevo@dominio.com"))
                    .thenReturn(usuario("1", "nuevo@dominio.com", Rol.ALUMNO, true));

            assertThrows(MyException.class, () -> usuarioServicio.crearUsuarioAdmin(
                    "Nuevo", "nuevo@dominio.com", "clave123", "PROFESOR", null, null));
        }

        @Test
        @DisplayName("crearUsuarioAdmin sin rol asigna ALUMNO por defecto")
        void crearSinRolAsignaAlumnoPorDefecto() throws MyException {
            when(usuarioRepositorio.buscarPorEmail("nuevo@dominio.com")).thenReturn(null);
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario creado = usuarioServicio.crearUsuarioAdmin(
                    "Nuevo", "nuevo@dominio.com", "clave123", null, null, null);

            assertEquals(Rol.ALUMNO, creado.getRol());
            assertEquals(1, creado.getCicloActual());
            verify(auditoriaServicio).registrar(eq("USUARIO_CREADO"), eq("nuevo@dominio.com"), anyString(), eq(true));
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con email de otro usuario lanza MyException")
        void actualizarConEmailDeOtroUsuarioLanzaExcepcion() {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            Usuario otro = usuario("2", "ocupado@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(usuarioRepositorio.buscarPorEmail("ocupado@dominio.com")).thenReturn(otro);

            assertThrows(MyException.class, () -> usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, "ocupado@dominio.com", null, null, null, null));
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin permite reasignar el mismo email al propio usuario")
        void actualizarPermiteMismoEmailDelPropioUsuario() throws MyException {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(usuarioRepositorio.buscarPorEmail("actual@dominio.com")).thenReturn(objetivo);
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario actualizado = usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, "actual@dominio.com", null, null, null, null);

            assertEquals("actual@dominio.com", actualizado.getEmail());
        }

        @Test
        @DisplayName("desactivarUsuario y activarUsuario alternan el estado activo")
        void desactivarYActivarUsuario() throws MyException {
            Usuario u = usuario("1", "ana@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(u));
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario desactivado = usuarioServicio.desactivarUsuario("1");
            assertFalse(desactivado.isActivo());
            verify(auditoriaServicio).registrar(eq("USUARIO_DESACTIVADO"), eq("ana@dominio.com"), anyString(), eq(true));

            Usuario activado = usuarioServicio.activarUsuario("1");
            assertTrue(activado.isActivo());
            verify(auditoriaServicio).registrar(eq("USUARIO_ACTIVADO"), eq("ana@dominio.com"), anyString(), eq(true));
        }

        // -----------------------------------------------------------
        // Rama faltante: crearUsuarioAdmin (nombre/email nulos, password corta, ciclo)
        // -----------------------------------------------------------
        @Test
        @DisplayName("crearUsuarioAdmin con nombre nulo lanza MyException")
        void crearConNombreNuloLanzaExcepcion() {
            assertThrows(MyException.class, () -> usuarioServicio.crearUsuarioAdmin(
                    null, "nuevo@dominio.com", "clave123", "ALUMNO", null, null));
        }

        @Test
        @DisplayName("crearUsuarioAdmin con email nulo lanza MyException")
        void crearConEmailNuloLanzaExcepcion() {
            assertThrows(MyException.class, () -> usuarioServicio.crearUsuarioAdmin(
                    "Nuevo", null, "clave123", "ALUMNO", null, null));
        }

        @Test
        @DisplayName("crearUsuarioAdmin con password corta lanza MyException")
        void crearConPasswordCortaLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("nuevo@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () -> usuarioServicio.crearUsuarioAdmin(
                    "Nuevo", "nuevo@dominio.com", "123", "ALUMNO", null, null));
        }

        @Test
        @DisplayName("crearUsuarioAdmin con ciclo actual fuera de rango lanza MyException")
        void crearConCicloActualFueraDeRangoLanzaExcepcion() {
            when(usuarioRepositorio.buscarPorEmail("nuevo@dominio.com")).thenReturn(null);

            assertThrows(MyException.class, () -> usuarioServicio.crearUsuarioAdmin(
                    "Nuevo", "nuevo@dominio.com", "clave123", "ALUMNO", null, 0));
        }

        @Test
        @DisplayName("crearUsuarioAdmin con ciclo actual valido lo asigna")
        void crearConCicloActualValidoLoAsigna() throws MyException {
            when(usuarioRepositorio.buscarPorEmail("nuevo@dominio.com")).thenReturn(null);
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario creado = usuarioServicio.crearUsuarioAdmin(
                    "Nuevo", "nuevo@dominio.com", "clave123", "PROFESOR", null, 7);

            assertEquals(7, creado.getCicloActual());
        }

        // -----------------------------------------------------------
        // Rama faltante: actualizarUsuarioAdmin (ramas "!= null" de cada campo)
        // -----------------------------------------------------------
        @Test
        @DisplayName("actualizarUsuarioAdmin con nombre valido lo actualiza")
        void actualizarConNombreValidoLoActualiza() throws MyException {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario actualizado = usuarioServicio.actualizarUsuarioAdmin(
                    "1", "  Nuevo Nombre  ", null, null, null, null, null);

            assertEquals("Nuevo Nombre", actualizado.getNombre());
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con nombre en blanco lanza MyException")
        void actualizarConNombreEnBlancoLanzaExcepcion() {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));

            assertThrows(MyException.class, () -> usuarioServicio.actualizarUsuarioAdmin(
                    "1", "   ", null, null, null, null, null));
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con email en blanco lanza MyException")
        void actualizarConEmailEnBlancoLanzaExcepcion() {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));

            assertThrows(MyException.class, () -> usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, "   ", null, null, null, null));
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con password en blanco no modifica la contrasena existente")
        void actualizarConPasswordEnBlancoNoActualizaPassword() throws MyException {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            String passwordOriginal = objetivo.getPassword();
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario actualizado = usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, null, "   ", null, null, null);

            assertEquals(passwordOriginal, actualizado.getPassword());
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con password no vacia pero corta lanza MyException")
        void actualizarConPasswordCortaLanzaExcepcion() {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));

            assertThrows(MyException.class, () -> usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, null, "123", null, null, null));
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con password valida la actualiza")
        void actualizarConPasswordValidaLaActualiza() throws MyException {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            String passwordOriginal = objetivo.getPassword();
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario actualizado = usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, null, "nuevaClave123", null, null, null);

            assertNotEquals(passwordOriginal, actualizado.getPassword());
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con rolTexto lo actualiza")
        void actualizarConRolTextoLoActualiza() throws MyException {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario actualizado = usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, null, null, "PROFESOR", null, null);

            assertEquals(Rol.PROFESOR, actualizado.getRol());
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con carreraReferencia en blanco quita la carrera asignada")
        void actualizarConCarreraEnBlancoQuitaCarrera() throws MyException {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            Carrera carreraPrevia = new Carrera();
            carreraPrevia.setId("car-1");
            objetivo.setCarrera(carreraPrevia);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario actualizado = usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, null, null, null, "   ", null);

            assertNull(actualizado.getCarrera());
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con carreraReferencia valida la resuelve y asigna")
        void actualizarConCarreraValidaLaAsigna() throws MyException {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            Carrera carrera = new Carrera();
            carrera.setId("car-2");
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(carreraRepositorio.findById("car-2")).thenReturn(Optional.of(carrera));
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario actualizado = usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, null, null, null, "car-2", null);

            assertEquals(carrera, actualizado.getCarrera());
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con ciclo actual valido lo actualiza")
        void actualizarConCicloActualValidoLoActualiza() throws MyException {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));
            when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario actualizado = usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, null, null, null, null, 10);

            assertEquals(10, actualizado.getCicloActual());
        }

        @Test
        @DisplayName("actualizarUsuarioAdmin con ciclo actual fuera de rango lanza MyException")
        void actualizarConCicloActualFueraDeRangoLanzaExcepcion() {
            Usuario objetivo = usuario("1", "actual@dominio.com", Rol.ALUMNO, true);
            when(usuarioRepositorio.findById("1")).thenReturn(Optional.of(objetivo));

            assertThrows(MyException.class, () -> usuarioServicio.actualizarUsuarioAdmin(
                    "1", null, null, null, null, null, 15));
        }
    }

    private Usuario argThatUsuario(java.util.function.Predicate<Usuario> predicado) {
        return org.mockito.ArgumentMatchers.argThat(predicado::test);
    }
}
