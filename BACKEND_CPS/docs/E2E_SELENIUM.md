# Pruebas E2E con Selenium

Suite de pruebas de extremo a extremo que maneja un navegador Chrome real contra
el frontend (`FRONTEND_CPS`, Vite en `:5173`) y el backend (`BACKEND_CPS`, Spring
Boot en `:8080`) corriendo localmente. Viven aparte de las pruebas unitarias /
`MockMvc` existentes:

```
src/test/java/com/GestionInscripcionCursos/e2e/
├── soporte/
│   ├── ConfiguracionE2E.java   # setup/teardown de ChromeDriver, helpers comunes
│   └── Interacciones.java      # utilidades (fijar valores de <input type=date/time>, nombres únicos)
├── paginas/                    # Page Objects (uno por pantalla/componente)
│   ├── LoginPage.java
│   ├── NavbarPage.java         # cerrar sesión (barra superior, visible en todo el dashboard)
│   └── CursosPage.java         # /cursos/listado — CRUD (ADMIN) e inscripción (ALUMNO/PROFESOR)
├── CursoCrudIT.java             # crear/editar/eliminar un curso como ADMIN
└── InscripcionHorarioIT.java    # RF05: cruce de horarios, de punta a punta por la UI
```

Los tests terminan en **`IT`** (Integration Test), no en `Test`, a propósito:
Surefire (el que corre `mvn test`) no los recoge por convención, así que **no**
rompen el build normal ni el flujo de CI existente. Corren con **Failsafe**
mediante el perfil Maven `e2e`.

## Prerrequisitos

1. Backend levantado: `./mvnw spring-boot:run` (puerto 8080).
2. Frontend levantado: `cd ../FRONTEND_CPS && npm start` (puerto 5173).
3. Google Chrome instalado (WebDriverManager descarga el driver automáticamente
   la primera vez; no hace falta instalar `chromedriver` a mano).
4. **Cuentas de prueba ya existentes** en la base de datos contra la que corre
   tu backend local (ver sección siguiente) — la suite no crea usuarios por sí
   sola.

## Cuentas de prueba requeridas

No existe seed/data loader en el proyecto (`SchemaBootstrap` solo parcha
columnas, no crea usuarios), y el auto-registro (`/registro`) siempre crea rol
`ALUMNO`. Por eso las cuentas se pasan como propiedades del sistema:

| Propiedad             | Usada por                          |
|------------------------|-------------------------------------|
| `e2e.admin.email`      | `CursoCrudIT`, `InscripcionHorarioIT` |
| `e2e.admin.password`   | `CursoCrudIT`, `InscripcionHorarioIT` |
| `e2e.alumno.email`     | `InscripcionHorarioIT`              |
| `e2e.alumno.password`  | `InscripcionHorarioIT`              |
| `e2e.profesor.email`   | `InscripcionHorarioIT`              |

Si falta alguna, el test que la necesita se **omite** (`Assumptions`, no
falla) con un mensaje indicando qué propiedad falta.

**Cuenta ALUMNO**: créala normalmente desde `/registro` en el frontend (necesita
que exista al menos una Carrera cargada — créala antes desde el panel de admin
si tu base está vacía). No hace falta que esté "limpia": `InscripcionHorarioIT`
programa sus cursos de prueba en un horario aleatorio de madrugada (00:00–05:59)
específicamente para no chocar con cursos reales ya inscritos en horario normal
(ver `Interacciones.inicioAleatorioDeMadrugada`).

**Cuenta PROFESOR** (`e2e.profesor.email`, no hace falta su contraseña — el test
no inicia sesión con ella, solo la usa como referencia para el campo "Profesor
asignado" al crear los cursos): créala por `/registro` y ascendela a rol
`PROFESOR` en la base de datos, igual que con la cuenta ADMIN. Es obligatoria
desde que `CursoServicio.inscribirCurso` rechaza la autoinscripción de un
ALUMNO en un curso sin `profesorAsignado`; sin esta propiedad, `cursosAlumno
.inscribirme(cursoA)` fallaría con "El curso aún no tiene un docente asignado".

**Cuenta ADMIN** (no hay forma de autorregistrarse como admin — es
intencional): la manera más simple de crear la primera es registrar una cuenta
normal por `/registro` y luego ascenderla directamente en la base de datos:

```sql
UPDATE usuario SET rol = 'ADMIN' WHERE email = 'tu-admin-de-prueba@ejemplo.com';
```

(Tabla `usuario`, columna `rol` almacenada como texto — ver
`entidades/Usuario.java`.) Hazlo solo contra tu base de datos local/dev, nunca
contra producción.

## Cómo ejecutar

```bash
# desde BACKEND_CPS/
./mvnw verify -Pe2e -De2e.admin.email=admin@test.com -De2e.admin.password=Admin123! -De2e.alumno.email=alumno@test.com -De2e.alumno.password=Alumno123! -De2e.profesor.email=profesor@test.com

# una sola clase
./mvnw verify -Pe2e -Dit.test=CursoCrudIT -De2e.admin.email=... -De2e.admin.password=...
```

Parámetros opcionales:

| Propiedad         | Default                  | Descripción |
|--------------------|---------------------------|-------------|
| `e2e.baseUrl`      | `http://localhost:5173`   | URL del frontend a probar |
| `e2e.headless`     | `false`                   | `true` corre Chrome sin ventana (útil en CI) |
| `e2e.evidencia`    | `false`                   | `true` guarda una captura numerada por cada paso clave (login, formulario, resultado...) en `target/e2e-screenshots/`, útil para armar un informe |

Por defecto el navegador es **visible** (`e2e.headless=false`) para poder ver
el flujo mientras se desarrollan/depuran tests. Cambia a `true` para correrlos
desatendidos.

## Notas de implementación

Estos tests se escribieron leyendo el código fuente del frontend y luego se
ajustaron corriéndolos de verdad contra la app — varios de los puntos siguientes
son bugs/comportamientos reales que solo aparecieron al ejecutar, no cosas
anticipables solo leyendo el JSX:

- **`Interacciones.fijarValor`**: los `<input type="date">`/`type="time">`
  representan su valor internamente en formato fijo (`yyyy-MM-dd` / `HH:mm`),
  pero `sendKeys()` escribe sobre la representación visual, que depende del
  locale del sistema operativo (ej. AM/PM en inglés). Además, `WebElement.clear()`
  demostró no ser confiable contra los `TextField` controlados por React de esta
  app (el DOM se vacía pero React no se entera, y un re-render posterior restaura
  el valor viejo, quedando el texto nuevo *concatenado* en vez de reemplazado —
  visto en el buscador y en el campo "Nombre" al editar). Por eso todo campo de
  texto se fija por JavaScript (`fijarValor`, funciona con `<input>` y
  `<textarea>`) en vez de `clear()+sendKeys()`.
- **Clicks por JavaScript**: los botones de acción por fila (Horarios/Editar/
  Eliminar/Inscribirme) a veces no abrían su diálogo pese a que Selenium los
  reportaba como "clickable" y sin lanzar ninguna excepción — probablemente el
  ripple/Tooltip de MUI interceptando el punto de clic. `CursosPage.clic()`
  dispara el evento por JS directo sobre el elemento en vez de usar
  `WebElement.click()`.
- **Mensajes flotantes bloquean la siguiente acción**: `FloatingMessageModal`
  (éxito/error) no se cierra solo — quedan abiertos con un backdrop de pantalla
  completa hasta que el usuario hace click en su "X", bloqueando cualquier otra
  interacción (buscador, otro botón). `CursosPage` los descarta defensivamente
  antes de la siguiente acción (`descartarMensajesFlotantes`).
- **"Eliminar" es soft-delete**: `CursoServicio` marca el curso `estado=INACTIVO`
  en vez de borrar la fila — la fila sigue en la tabla. `CursoCrudIT` verifica el
  estado, no la ausencia de la fila.
- **Editar un curso sin profesor puede fallar**: el campo "Profesor asignado" se
  precarga con el texto sentinela `"Sin docente"` cuando el curso no tiene uno; si
  se reenvía tal cual al guardar, el backend lo trata como una referencia real y
  responde "No se encontró el profesor asignado". `CursosPage.editarNombre` limpia
  ese campo antes de guardar.
- **Timeouts generosos**: el backend habla con Postgres en la nube (Neon, con
  cold-start); varias operaciones (guardar edición, eliminar, inscribir) llegaron
  a tardar más de 20s en pruebas reales. La espera explícita es de 30s.
- **Nombres y códigos de curso únicos**: el campo "Nombre" solo acepta letras,
  espacios, apóstrofes y guiones (`nombreCursoRegex`), así que el sufijo único
  para evitar colisiones entre corridas es alfabético (`Interacciones.nombreUnico`),
  no un timestamp. El "Código de curso" se envía siempre explícito y único
  (`Interacciones.codigoUnico`): si se omite, el backend lo autogenera truncando
  el nombre, y como todos los nombres de esta suite comparten prefijo, ese código
  autogenerado siempre coincide y choca con cursos huérfanos de corridas
  anteriores fallidas ("Ya existe un curso con el codigo ...").
- **Sin limpieza automática**: `CursoCrudIT` elimina (soft-delete) el curso que
  crea, pero `InscripcionHorarioIT` deja los dos cursos y la inscripción creados
  en la base de datos (para mantener el test enfocado en RF05). Si te importa
  mantener la base local limpia, bórralos manualmente desde el panel de admin
  después de correr la suite.
- **Capturas de pantalla al fallar**: `CapturaPantallaAlFallar` (JUnit
  `AfterTestExecutionCallback`, no `TestWatcher` — este último corre después de
  `@AfterEach`, cuando el navegador ya se cerró) guarda un PNG en
  `target/e2e-screenshots/` con el estado exacto de la pantalla al momento del
  fallo. Revísalo primero si un test falla; ahorra tener que reproducir a ciegas.
