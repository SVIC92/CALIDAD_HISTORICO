# CLAUDE.md

Este archivo proporciona guía a Claude Code (claude.ai/code) al trabajar con código en este repositorio.

## Descripción general

Plataforma full-stack de inscripción de cursos / gestión académica ("Gestión de Inscripción de Cursos" / GCI+). El repositorio es un monorepo con tres partes desplegadas de forma independiente:

- **`BACKEND_CPS/`** — API REST + WebSocket con Spring Boot 3.4 (Java 17, Maven). Desplegado en Render mediante el Dockerfile.
- **`FRONTEND_CPS/`** — SPA en React 19 construida con Vite 8 + Material-UI v9. Desplegado en Vercel.
- **`MICRO-SERVICES/transcripcion/`** — Servicio Python FastAPI por WebSocket que hace speech-to-text en vivo con Vosk (modelo Kaldi), usado para subtitular videoconferencias.

**El código está escrito en español** — nombres de paquetes, clases, variables, rutas y comentarios. Respeta esta convención (p. ej. `controladores`, `servicios`, `inscribirAlumnoDirecto`).

## Comandos

### Backend (`BACKEND_CPS/`)
```bash
./mvnw spring-boot:run            # levanta el servidor de desarrollo en :8080 (usa mvnw.cmd en cmd.exe/PowerShell)
./mvnw clean package              # genera el jar (target/GestionInscripcionCursos-0.0.1-SNAPSHOT.jar)
./mvnw test                       # ejecuta todos los tests
./mvnw test -Dtest=InscripcionServicioTest                     # una sola clase de test
./mvnw test -Dtest=InscripcionServicioTest#metodoDePrueba      # un solo método de test
./mvnw verify -Pe2e -De2e.admin.email=... -De2e.admin.password=... -De2e.alumno.email=... -De2e.alumno.password=...
                                   # pruebas E2E con Selenium (requiere backend+frontend corriendo); ver docs/E2E_SELENIUM.md
```
Requiere variables de entorno para funcionar (los placeholders quedan vacíos por defecto en `application.properties`): `DB_PASSWORD`, `JWT_SECRET`, `GROQ_API_KEY`, `COHERE_API_KEY`, `CLOUDINARY_*`, `MAIL_PASSWORD`. El plugin de Sonar scanner está configurado en `pom.xml`.

### Frontend (`FRONTEND_CPS/`)
```bash
npm install
npm start          # servidor de desarrollo de Vite (nota: es "start", no "dev") en :5173
npm run build      # build de producción a dist/
npm run lint       # ESLint
npm run preview    # sirve el build de producción
```
No hay test runner configurado en el frontend (sin script `test` en `package.json`); los únicos tests automatizados del repo son los del backend.
La URL base de la API se resuelve automáticamente en `src/API/axios.js`: localhost → `VITE_LOCAL_API_BASE_URL` (por defecto `http://localhost:8080/api`), en otro caso → `VITE_API_BASE_URL` (por defecto la URL de Render). Para backend local, define `VITE_LOCAL_API_BASE_URL` en `.env.local`.

### Microservicio de transcripción (`MICRO-SERVICES/transcripcion/`)
```bash
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 7860
```
Necesita un directorio de modelo Vosk en `./model` (o `VOSK_MODEL_PATH`). Envía los resultados al backend en `SPRING_BOOT_URL` (por defecto `/api/subtitulos/interno`).

## Arquitectura

### Capas del backend
Módulo Maven único, paquete raíz `com.GestionInscripcionCursos`, capas convencionales:
`controladores` (endpoints REST + WebSocket) → `servicios` (lógica de negocio) → `repositorios` (Spring Data JPA) → `entidades` (entidades JPA). Además `dto`, `enumeraciones`, `excepciones` (`MyException` es la excepción de negocio checked de la app), `seguridad`, `configuracion`, `util`.

- **Persistencia:** PostgreSQL (Neon cloud) con `spring.jpa.hibernate.ddl-auto=update` — el esquema lo gestiona Hibernate a partir de las entidades, no hay migraciones. `configuracion/SchemaBootstrap.java` ejecuta `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` en crudo al iniciar para parchar BDs antiguas (actualmente las columnas de 2FA) antes de que carguen las entidades. El conector de MySQL también está en el classpath, pero PostgreSQL es el driver activo.
- **Seguridad:** `SeguridadWeb.java` (en la raíz del paquete, *no* en `seguridad/`) define el `SecurityFilterChain` — JWT stateless, CSRF desactivado, CORS desde `app.cors.allowed-origin-patterns`. `seguridad/JwtFiltro` autentica cada request a partir del header `Authorization: Bearer` y registra la presencia vía `PresenciaUsuarioServicio`. **Nota:** `/api/**` está actualmente `permitAll()` ("temporalmente abierto"); la autorización real depende de `@PreAuthorize` (la seguridad a nivel de método está habilitada vía `@EnableMethodSecurity`) y de los guards de rol del frontend. El enum de roles es `ADMIN, PROFESOR, ALUMNO`, expuesto a los clientes como authorities `ROLE_*`.
- **Tiempo real (WebSocket/STOMP):** `configuracion/WebSocketConfig.java` expone el endpoint SockJS `/ws-chat` con un broker simple en `/queue`, `/topic`, `/user`. Los frames STOMP CONNECT se autentican por JWT en un channel interceptor (independiente del filtro HTTP). Da soporte al chat institucional, presencia de usuarios, señalización de videoconferencia y música sincronizada.
- **Funciones de IA (`servicios/IaServicio`, `CohereServicio`):** el asistente de chat llama a la API de **Groq** (compatible con OpenAI, modelo por defecto `llama-3.1-8b-instant`); la generación de rúbricas/sílabos prioriza **Cohere** (`command-r-08-2024`) con un fallback local. Todas las claves son opcionales — los servicios degradan a fallbacks cuando no están configuradas.
- **Otras integraciones:** Cloudinary (subida de archivos/imágenes, `ArchivoServicio`), Brevo SMTP (correo / recuperación de contraseña vía `CorreoServicio` + `RecuperacionPasswordServicio`), TOTP 2FA con códigos QR de ZXing (`TwoFactorServicio`), calificación automática programada (`EvaluacionAutomaticaTask`).
- **Regla de dominio transversal:** la validación de cruce de horarios vive en `util/HorarioUtil` (`primerCruce`) — el cruce es `inicioA < finB && finA > inicioB` (los extremos que se tocan **no** se consideran cruce). Este es el requerimiento RF05 y es el foco de `InscripcionServicioTest`; ver `docs/TDD_RF05_CruceHorarios.md` para la matriz de casos Red→Green→Refactor detrás de esos tests.

### Estructura del frontend
`src/` está organizado en `pages/` (pantallas de rutas), `components/` (UI compartida), `layouts/DashboardLayout.jsx`, `services/` (un módulo por dominio del backend, p. ej. `CursoService.js`), `API/axios.js` (la instancia de axios configurada con un interceptor de request que inyecta el bearer token), `context/`, y `utils/`.

- **Ruteo y autenticación:** todas las rutas se declaran en `src/App.jsx`. `components/RutasProtegidas.jsx` es el guard de autenticación/rol; lee `token` y `rol` de `localStorage`. Las rutas están anidadas por rol (`ROLE_ADMIN` / `ROLE_PROFESOR` / `ROLE_ALUMNO`); `/dashboard` redirige al dashboard específico del rol.
- **Datos y estado:** TanStack React Query para estado del servidor, Zustand para estado del cliente, React Hook Form para formularios. La UI es Material-UI v9 (`@mui/material`), con ayudas de accesibilidad (fuente OpenDyslexic, overlay de asistencia de lectura, menú flotante de accesibilidad).
- **Integraciones de dominio:** STOMP sobre SockJS (`@stomp/stompjs`, `sockjs-client`) para chat/tiempo real, Jitsi (`@jitsi/react-sdk`) para videoconferencia, `jspdf` + `xlsx` para exportar reportes, `react-youtube` para la sala de estudio sincronizada.
- **La generación con IA sobrevive a la navegación:** la generación de rúbricas/sílabos (`RubricaIA.jsx`, `SilaboIA.jsx`) es síncrona en el backend, pero el frontend guarda el estado `idle/cargando/listo/error` en `store/useIaGeneracionStore.js` (Zustand) en lugar de estado de componente, de modo que la petición sobrevive aunque el usuario navegue a otra página. `components/GeneracionIaNotificador.jsx` está montado globalmente y muestra un Snackbar con un enlace "Ver ahora" cuando la generación termina en una ruta distinta.

### Cómo se conectan las tres partes
El frontend se conecta al backend por REST (`/api/...`) autenticado con el bearer token JWT, y por STOMP/SockJS (`/ws-chat`) para tiempo real. El microservicio de transcripción recibe audio crudo del navegador por su propio WebSocket (`/ws/transcribir/{salaUuid}/{usuarioId}`), lo transcribe con Vosk, y envía el texto de los subtítulos de vuelta al endpoint interno del backend (`/api/subtitulos/interno`), que lo distribuye a los clientes vía STOMP.
