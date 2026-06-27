# TDD — RF05: Validación de Cruce de Horarios

Desarrollo del requerimiento **RF05** siguiendo el ciclo **Red → Green → Refactor**, aplicado
al proyecto real (`com.GestionInscripcionCursos`).

A diferencia del ejemplo didáctico (paquete `com.sistema.academico`), aquí:

- El día/horas viven en la entidad **`HorarioSesion`** (no en `Curso`).
- El "cruce" se reporta lanzando **`MyException`** (no `CruceHorarioException`).
- Los datos provienen de **`HorarioSesionRepositorio`** vía Mockito.

## Matriz de Casos de Prueba

| ID Caso | Escenario | Datos (Día, Inicio, Fin) | Resultado Esperado |
|---|---|---|---|
| **CP_RF05_01** | Sin cruce — horario posterior | LUNES, 10:00, 12:00 (existente 08:00–10:00) | Permitir inscripción (Exitosa) |
| **CP_RF05_02** | Sin cruce — horario previo | LUNES, 06:00, 08:00 (existente 08:00–10:00) | Permitir inscripción (Exitosa) |
| **CP_RF05_03** | Cruce — solapamiento total interno | LUNES, 08:30, 09:30 (existente 08:00–10:00) | Lanzar `MyException` |
| **CP_RF05_04** | Cruce — valor límite (1 min) | LUNES, 09:59, 12:00 (existente 08:00–10:00) | Lanzar `MyException` |

---

## PASO 1: FASE ROJA (Escribir la prueba que va a fallar)

Escribimos primero el test unitario con **JUnit 5 + Mockito**. En esta fase el método
`inscribirAlumnoDirecto` **todavía no contiene la validación de cruce**, por lo que el caso
`CP_RF05_03` no lanza la excepción esperada y la prueba termina en **Fallo (Rojo)**.

```java
@ExtendWith(MockitoExtension.class)
class InscripcionServicioTest {

    @Mock private InscripcionRepositorio inscripcionRepositorio;
    @Mock private CursoRepositorio cursoRepositorio;
    @Mock private UsuarioRepositorio usuarioRepositorio;
    @Mock private HorarioSesionRepositorio horarioSesionRepositorio;
    @InjectMocks private InscripcionServicio inscripcionServicio;

    @Test
    @DisplayName("CP_RF05_03: solapamiento total interno -> lanza MyException")
    void testInscribirConCruceHorario_CP_RF05_03() {
        // ARRANGE — preparar el escenario según el caso de prueba
        Usuario alumno = new Usuario();
        alumno.setId("alu-1");
        alumno.setRol(Rol.ALUMNO);

        Curso cursoNuevo = new Curso();
        cursoNuevo.setId("curso-nuevo");
        cursoNuevo.setNombre("Algoritmos");
        cursoNuevo.setCapacidadMaxima(40);

        Curso cursoExistente = new Curso();
        cursoExistente.setId("curso-existente");
        cursoExistente.setNombre("Calculo I");

        when(usuarioRepositorio.findById("alu-1")).thenReturn(Optional.of(alumno));
        when(cursoRepositorio.findById("curso-nuevo")).thenReturn(Optional.of(cursoNuevo));
        when(inscripcionRepositorio.contarAlumnosAprobadosPorCurso("curso-nuevo")).thenReturn(0L);

        // Curso a inscribir: LUNES 08:30 - 09:30
        when(horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc("curso-nuevo"))
                .thenReturn(List.of(horario(cursoNuevo, "LUNES", LocalTime.of(8, 30), LocalTime.of(9, 30))));
        // Curso ya inscrito del alumno: LUNES 08:00 - 10:00
        when(horarioSesionRepositorio.buscarHorariosPorAlumno("alu-1"))
                .thenReturn(List.of(horario(cursoExistente, "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0))));

        // ACT & ASSERT — debe lanzar la excepción por solapamiento
        MyException ex = assertThrows(MyException.class,
                () -> inscripcionServicio.inscribirAlumnoDirecto("alu-1", "curso-nuevo"));

        assertTrue(ex.getMessage().contains("Cruce de horarios detectado"));
        verify(inscripcionRepositorio, never()).save(any());
    }

    private HorarioSesion horario(Curso c, String dia, LocalTime ini, LocalTime fin) {
        HorarioSesion h = new HorarioSesion();
        h.setCurso(c); h.setDiaSemana(dia); h.setHoraInicio(ini); h.setHoraFin(fin);
        return h;
    }
}
```

> 🔴 **Resultado:** `AssertionError` / no se lanza `MyException` → la prueba **FALLA**, porque
> el servicio aún no valida cruces de horario.

---

## PASO 2: FASE VERDE (Solución mínima para que la prueba pase)

Escribimos la lógica **justa y necesaria** en la capa de servicio para que el test se ponga en
**Verde**. La fórmula de solapamiento se resuelve con un doble bucle imperativo dentro del propio
`InscripcionServicio`.

```java
@Service
public class InscripcionServicio {

    @Autowired private UsuarioRepositorio usuarioRepositorio;
    @Autowired private CursoRepositorio cursoRepositorio;
    @Autowired private InscripcionRepositorio inscripcionRepositorio;
    @Autowired private HorarioSesionRepositorio horarioSesionRepositorio;

    @Transactional
    public void inscribirAlumnoDirecto(String usuarioId, String cursoId) throws MyException {
        Usuario alumno = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new MyException("Usuario no encontrado"));
        if (!Rol.ALUMNO.equals(alumno.getRol())) {
            throw new MyException("El usuario no es un alumno");
        }
        Curso curso = cursoRepositorio.findById(cursoId)
                .orElseThrow(() -> new MyException("Curso no encontrado"));

        // Aforo
        if (curso.getCapacidadMaxima() != null
                && inscripcionRepositorio.contarAlumnosAprobadosPorCurso(cursoId) >= curso.getCapacidadMaxima()) {
            throw new MyException("El cupo/aforo del curso ya se encuentra lleno.");
        }

        // --- Validación de cruce: lógica mínima inline ---
        List<HorarioSesion> nuevos =
            horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(cursoId);
        if (nuevos != null && !nuevos.isEmpty()) {
            List<HorarioSesion> existentes =
                horarioSesionRepositorio.buscarHorariosPorAlumno(alumno.getId());
            for (HorarioSesion nuevo : nuevos) {
                for (HorarioSesion existente : existentes) {
                    // Fórmula de solapamiento de intervalos: inicioA < finB && finA > inicioB
                    if (nuevo.getDiaSemana().equalsIgnoreCase(existente.getDiaSemana())
                            && nuevo.getHoraInicio().isBefore(existente.getHoraFin())
                            && nuevo.getHoraFin().isAfter(existente.getHoraInicio())) {
                        throw new MyException("Cruce de horarios detectado con el curso '"
                                + existente.getCurso().getNombre() + "'.");
                    }
                }
            }
        }

        Inscripcion nueva = new Inscripcion();
        nueva.setUsuario(alumno);
        nueva.setCurso(curso);
        nueva.setEstado("APROBADO");
        inscripcionRepositorio.save(nueva);
    }
}
```

> 🟢 **Resultado:** la prueba `CP_RF05_03` (y las demás) **PASAN**. El código funciona, pero el
> servicio quedó "saturado" con la lógica de comparación de tiempos.

---

## PASO 3: FASE REFACTOR (Optimizar y limpiar)

El código funciona, pero bajo buenas prácticas arquitectónicas **delegamos la responsabilidad de
la comparación de tiempos** a una clase de utilidad de dominio (`HorarioUtil`), evitando saturar
el `Service` y mejorando la legibilidad y la reutilización (la misma regla la usa también la
validación de cruce de profesores).

**Utilidad de dominio reutilizable (`HorarioUtil.java`):**

```java
public final class HorarioUtil {

    private HorarioUtil() { }

    /** Dos sesiones se cruzan si caen el mismo día y sus rangos se solapan:
     *  (InicioA < FinB) && (FinA > InicioB). */
    public static boolean seCruzan(HorarioSesion a, HorarioSesion b) {
        return a.getDiaSemana().equalsIgnoreCase(b.getDiaSemana())
                && a.getHoraInicio().isBefore(b.getHoraFin())
                && a.getHoraFin().isAfter(b.getHoraInicio());
    }

    /** Primera sesión existente que choca con alguna nueva, o null si no hay cruce. */
    public static HorarioSesion primerCruce(List<HorarioSesion> nuevos, List<HorarioSesion> existentes) {
        if (nuevos == null || existentes == null) return null;
        for (HorarioSesion nuevo : nuevos) {
            for (HorarioSesion existente : existentes) {
                if (seCruzan(nuevo, existente)) return existente;
            }
        }
        return null;
    }
}
```

**Servicio refactorizado y limpio (`InscripcionServicio.java`):**

```java
@Transactional
public void inscribirAlumnoDirecto(String usuarioId, String cursoId) throws MyException {
    Usuario alumno = usuarioRepositorio.findById(usuarioId)
            .orElseThrow(() -> new MyException("Usuario no encontrado"));
    if (!Rol.ALUMNO.equals(alumno.getRol())) {
        throw new MyException("El usuario no es un alumno");
    }
    Curso curso = cursoRepositorio.findById(cursoId)
            .orElseThrow(() -> new MyException("Curso no encontrado"));

    if (curso.getCapacidadMaxima() != null
            && inscripcionRepositorio.contarAlumnosAprobadosPorCurso(cursoId) >= curso.getCapacidadMaxima()) {
        throw new MyException("El cupo/aforo del curso ya se encuentra lleno. Capacidad máxima: "
                + curso.getCapacidadMaxima());
    }

    validarCruceHorariosAlumno(alumno, curso); // responsabilidad delegada y legible

    Inscripcion nueva = new Inscripcion();
    nueva.setUsuario(alumno);
    nueva.setCurso(curso);
    nueva.setEstado("APROBADO");
    inscripcionRepositorio.save(nueva);
}

private void validarCruceHorariosAlumno(Usuario alumno, Curso nuevoCurso) throws MyException {
    List<HorarioSesion> nuevos =
        horarioSesionRepositorio.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(nuevoCurso.getId());
    if (nuevos == null || nuevos.isEmpty()) return;

    List<HorarioSesion> existentes =
        horarioSesionRepositorio.buscarHorariosPorAlumno(alumno.getId());

    HorarioSesion cruce = HorarioUtil.primerCruce(nuevos, existentes);
    if (cruce != null) {
        throw new MyException("Cruce de horarios detectado. El alumno ya está inscrito en el curso '"
            + cruce.getCurso().getNombre() + "' los días " + cruce.getDiaSemana()
            + " de " + cruce.getHoraInicio() + " a " + cruce.getHoraFin());
    }
}
```

> ♻️ **Resultado:** los tests **siguen en Verde** (la red de seguridad confirma que el refactor no
> rompió el comportamiento), el servicio quedó limpio y la regla de solapamiento es reutilizable.

---

## Evidencias de ejecución de las 3 fases

Cada fase se ejecutó con Maven (`./mvnw ... test`) sobre el servicio real. La salida real
del runner es la evidencia del ciclo.

### 🔴 FASE ROJA — la prueba falla (lógica de cruce ausente)

Comando:
```bash
./mvnw -Dtest='InscripcionServicioTest$CruceHorarioAlumno#cp03_cruceTotalInterno_lanzaExcepcion' test
```
Salida:
```text
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0  <<< FAILURE!
  -- in InscripcionServicioTest$CruceHorarioAlumno

[ERROR] cp03_cruceTotalInterno_lanzaExcepcion
org.opentest4j.AssertionFailedError:
   Expected com.GestionInscripcionCursos.excepciones.MyException to be thrown, but nothing was thrown.

[INFO] BUILD FAILURE
```
> La inscripción se registró sin validar el cruce → el `assertThrows` no recibe la excepción esperada.

### 🟢 FASE VERDE — la prueba pasa (validación mínima inline)

Comando:
```bash
./mvnw -Dtest='InscripcionServicioTest$CruceHorarioAlumno' test
```
Salida:
```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
   -- in InscripcionServicioTest$CruceHorarioAlumno
[INFO] BUILD SUCCESS
```
> Los 6 casos del escenario de cruce (CP_RF05_01..04 + distinto día + sin horarios) quedan en verde.

### ♻️ FASE REFACTOR — sigue pasando (delegado a `HorarioUtil`)

Comando:
```bash
./mvnw -Dtest='InscripcionServicioTest' test
```
Salida:
```text
[INFO] Tests run: 6, Failures: 0, ... -- in InscripcionServicioTest$CruceHorarioAlumno
[INFO] Tests run: 5, Failures: 0, ... -- in InscripcionServicioTest$InscripcionProfesor
[INFO] Tests run: 4, Failures: 0, ... -- in InscripcionServicioTest$ValidacionesAlumno
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
> Tras mover la lógica a `HorarioUtil.primerCruce`, la suite completa (15 tests) sigue verde:
> el refactor no cambió el comportamiento.

| Fase | Comando | Resultado |
|---|---|---|
| 🔴 Rojo | `...#cp03_cruceTotalInterno_lanzaExcepcion` | `Tests run: 1, Failures: 1` → **BUILD FAILURE** |
| 🟢 Verde | `...$CruceHorarioAlumno` | `Tests run: 6, Failures: 0` → **BUILD SUCCESS** |
| ♻️ Refactor | `InscripcionServicioTest` | `Tests run: 15, Failures: 0` → **BUILD SUCCESS** |

> 💡 **Cómo reproducir la evidencia visual (capturas):** para mostrar el Rojo, comenta la línea
> `validarCruceHorariosAlumno(alumno, curso);` en `InscripcionServicio.java` y ejecuta el comando
> de la fase roja; verás el `BUILD FAILURE`. Restaura la línea (o `git checkout`) y ejecuta de
> nuevo para el verde. Toma captura de cada terminal.

---

## Suite de pruebas completa

El conjunto completo de los 4 casos de la matriz (más casos de aforo, rol y flujo de profesor)
está implementado y pasando en:

[`src/test/java/com/GestionInscripcionCursos/servicios/InscripcionServicioTest.java`](../src/test/java/com/GestionInscripcionCursos/servicios/InscripcionServicioTest.java)

```bash
./mvnw -Dtest=InscripcionServicioTest test
# Tests run: 15, Failures: 0, Errors: 0  -> BUILD SUCCESS
```

> **Nota sobre el límite (CP_RF05_01 / CP_RF05_02):** con la fórmula `inicio < fin && fin > inicio`,
> dos sesiones que apenas *se tocan* (p. ej. una termina 10:00 y la otra empieza 10:00) **no** se
> consideran cruce → inscripción permitida, tal como exige la matriz.
