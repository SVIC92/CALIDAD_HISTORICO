# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Full-stack course-enrollment / academic-management platform ("Gestión de Inscripción de Cursos" / GCI+). The repo is a monorepo with three independently-deployed parts:

- **`BACKEND_CPS/`** — Spring Boot 3.4 REST + WebSocket API (Java 17, Maven). Deployed on Render via the Dockerfile.
- **`FRONTEND_CPS/`** — React 19 SPA built with Vite 8 + Material-UI v9. Deployed on Vercel.
- **`MICRO-SERVICES/transcripcion/`** — Python FastAPI WebSocket service doing live speech-to-text with Vosk (Kaldi model), used to caption videoconferences.

**The codebase is written in Spanish** — package names, classes, variables, routes, and comments. Match this convention (e.g. `controladores`, `servicios`, `inscribirAlumnoDirecto`).

## Commands

### Backend (`BACKEND_CPS/`)
```bash
./mvnw spring-boot:run            # run dev server on :8080 (use mvnw.cmd in cmd.exe/PowerShell)
./mvnw clean package              # build the jar (target/GestionInscripcionCursos-0.0.1-SNAPSHOT.jar)
./mvnw test                       # run all tests
./mvnw test -Dtest=InscripcionServicioTest                     # single test class
./mvnw test -Dtest=InscripcionServicioTest#metodoDePrueba      # single test method
```
Requires env vars to function (placeholders default to empty in `application.properties`): `DB_PASSWORD`, `JWT_SECRET`, `GROQ_API_KEY`, `COHERE_API_KEY`, `CLOUDINARY_*`, `MAIL_PASSWORD`. Sonar scanner plugin is wired in `pom.xml`.

### Frontend (`FRONTEND_CPS/`)
```bash
npm install
npm start          # Vite dev server (note: "start", not "dev") on :5173
npm run build      # production build to dist/
npm run lint       # ESLint
npm run preview    # serve the production build
```
API base URL resolves automatically in `src/API/axios.js`: localhost → `VITE_LOCAL_API_BASE_URL` (default `http://localhost:8080/api`), otherwise → `VITE_API_BASE_URL` (default the Render URL). For local backend, set `VITE_LOCAL_API_BASE_URL` in `.env.local`.

### Transcription microservice (`MICRO-SERVICES/transcripcion/`)
```bash
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 7860
```
Needs a Vosk model directory at `./model` (or `VOSK_MODEL_PATH`). Posts results to the backend at `SPRING_BOOT_URL` (defaults to `/api/subtitulos/interno`).

## Architecture

### Backend layering
Single Maven module, package root `com.GestionInscripcionCursos`, conventional layers:
`controladores` (REST + WebSocket endpoints) → `servicios` (business logic) → `repositorios` (Spring Data JPA) → `entidades` (JPA entities). Plus `dto`, `enumeraciones`, `excepciones` (`MyException` is the app's checked business exception), `seguridad`, `configuracion`, `util`.

- **Persistence:** PostgreSQL (Neon cloud) with `spring.jpa.hibernate.ddl-auto=update` — schema is managed by Hibernate from the entities, not migrations. `configuracion/SchemaBootstrap.java` runs raw `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` at startup to patch legacy DBs (currently the 2FA columns) before entities load. MySQL connector is also on the classpath but PostgreSQL is the active driver.
- **Security:** `SeguridadWeb.java` (at the package root, *not* in `seguridad/`) defines the `SecurityFilterChain` — stateless JWT, CSRF off, CORS from `app.cors.allowed-origin-patterns`. `seguridad/JwtFiltro` authenticates each request from the `Authorization: Bearer` header and records presence via `PresenciaUsuarioServicio`. **Note:** `/api/**` is currently `permitAll()` ("temporalmente abierto"); real authorization relies on `@PreAuthorize` (method security is enabled via `@EnableMethodSecurity`) and the frontend role guards. Roles enum is `ADMIN, PROFESOR, ALUMNO`, surfaced to clients as `ROLE_*` authorities.
- **Realtime (WebSocket/STOMP):** `configuracion/WebSocketConfig.java` exposes the `/ws-chat` SockJS endpoint with a simple broker on `/queue`, `/topic`, `/user`. STOMP CONNECT frames are authenticated by JWT in a channel interceptor (separate from the HTTP filter). Powers institutional chat, user presence, videoconference signaling, and synced music.
- **AI features (`servicios/IaServicio`, `CohereServicio`):** chat assistant calls the **Groq** API (OpenAI-compatible, default model `llama-3.1-8b-instant`); rubric/syllabus generation prefers **Cohere** (`command-r-08-2024`) with a local fallback. All keys are optional — services degrade to fallbacks when unset.
- **Other integrations:** Cloudinary (file/image upload, `ArchivoServicio`), Brevo SMTP (mail / password reset via `CorreoServicio` + `RecuperacionPasswordServicio`), TOTP 2FA with ZXing QR codes (`TwoFactorServicio`), scheduled auto-grading (`EvaluacionAutomaticaTask`).
- **Cross-cutting domain rule:** schedule-overlap validation lives in `util/HorarioUtil` (`primerCruce`) — overlap is `inicioA < finB && finA > inicioB` (touching endpoints do **not** overlap). This is the RF05 requirement and is the focus of `InscripcionServicioTest`.

### Frontend structure
`src/` is organized as `pages/` (route screens), `components/` (shared UI), `layouts/DashboardLayout.jsx`, `services/` (one module per backend domain, e.g. `CursoService.js`), `API/axios.js` (the configured axios instance with a request interceptor that injects the bearer token), `context/`, and `utils/`.

- **Routing & auth:** all routes are declared in `src/App.jsx`. `components/RutasProtegidas.jsx` is the auth/role gate; it reads `token` and `rol` from `localStorage`. Routes are nested by role (`ROLE_ADMIN` / `ROLE_PROFESOR` / `ROLE_ALUMNO`); `/dashboard` redirects to the role-specific dashboard.
- **Data & state:** TanStack React Query for server state, Zustand for client state, React Hook Form for forms. UI is Material-UI v9 (`@mui/material`), with accessibility helpers (OpenDyslexic font, reading-aid overlay, floating accessibility menu).
- **Domain integrations:** STOMP over SockJS (`@stomp/stompjs`, `sockjs-client`) for chat/realtime, Jitsi (`@jitsi/react-sdk`) for videoconferencing, `jspdf` + `xlsx` for report exports, `react-youtube` for the synced study room.

### How the three parts connect
Frontend → backend over REST (`/api/...`) authenticated with the JWT bearer token, and over STOMP/SockJS (`/ws-chat`) for realtime. The transcription microservice receives raw audio from the browser over its own WebSocket (`/ws/transcribir/{salaUuid}/{usuarioId}`), transcribes with Vosk, and POSTs caption text back to the backend's internal endpoint (`/api/subtitulos/interno`), which fan-outs to clients via STOMP.
