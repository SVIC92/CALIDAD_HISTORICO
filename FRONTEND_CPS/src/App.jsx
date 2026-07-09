import { lazy, Suspense, useEffect, useRef } from 'react';
import { Backdrop, CircularProgress, Typography } from '@mui/material';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import DashboardLayout from './layouts/DashboardLayout';
import RutasProtegidas from './components/RutasProtegidas';
import { useLoadingScreen } from './context/LoadingScreenContext';
import AccessibilityFloatingMenu from './components/AccessibilityFloatingMenu';
import ReadingAidOverlay from './components/ReadingAidOverlay';

// Paginas cargadas de forma perezosa: evita que el primer request descargue
// el codigo de todos los modulos (videoconferencia, chat, IA, admin, etc.)
// cuando el usuario solo necesita, por ejemplo, la pantalla de login.
const Login = lazy(() => import('./pages/Login'));
const Registro = lazy(() => import('./pages/Registro'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'));
const ResetPassword = lazy(() => import('./pages/ResetPassword'));
const PerfilUsuario = lazy(() => import('./pages/PerfilUsuario'));
const DashboardAdmin = lazy(() => import('./pages/DashboardAdmin'));
const DashboardProfesor = lazy(() => import('./pages/DashboardProfesor'));
const DashboardAlumno = lazy(() => import('./pages/DashboardAlumno'));
const CursosHub = lazy(() => import('./pages/CursosHub'));
const CursosListado = lazy(() => import('./pages/CursosListado'));
const CursosDictadosProfesor = lazy(() => import('./pages/CursosDictadosProfesor'));
const Carreras = lazy(() => import('./pages/Carreras'));
const Actividades = lazy(() => import('./pages/Actividades'));
const Inscripciones = lazy(() => import('./pages/Inscripciones'));
const Reportes = lazy(() => import('./pages/Reportes'));
const Configuracion = lazy(() => import('./pages/Configuracion'));
const EstudIA = lazy(() => import('./pages/EstudIA'));
const IAHub = lazy(() => import('./pages/IAHub'));
const RubricaIA = lazy(() => import('./pages/RubricaIA'));
const SilaboIA = lazy(() => import('./pages/SilaboIA'));
const ListadoUsuarios = lazy(() => import('./pages/ListadoUsuarios'));
const UsuarioHub = lazy(() => import('./pages/UsuarioHub'));
const UsuariosConectados = lazy(() => import('./pages/UsuariosConectados'));
const ModuloPlaceholder = lazy(() => import('./pages/ModuloPlaceholder'));
const NoAutorizado = lazy(() => import('./pages/NoAutorizado'));
const HorarioProfesor = lazy(() => import('./pages/HorarioProfesor'));
const HorarioAlumno = lazy(() => import('./pages/HorarioAlumno'));
const ChatInstitucional = lazy(() => import('./pages/ChatInstitucional'));
const SalaEstudioMUI = lazy(() => import('./pages/SalaEstudioMUI'));

const PageFallback = () => (
  <Backdrop
    open
    sx={{
      zIndex: (theme) => theme.zIndex.drawer + 2000,
      color: '#fff',
      flexDirection: 'column',
      gap: 2,
      backdropFilter: 'blur(2px)',
      backgroundColor: 'rgba(0, 0, 0, 0.45)',
    }}
  >
    <CircularProgress color="inherit" />
    <Typography variant="h6" sx={{ fontWeight: 600 }}>
      Cargando...
    </Typography>
  </Backdrop>
);

const DashboardRedirect = () => {
  const rol = localStorage.getItem('rol');

  if (rol === 'ROLE_ADMIN') return <Navigate to="/dashboard/admin" replace />;
  if (rol === 'ROLE_PROFESOR') return <Navigate to="/dashboard/profesor" replace />;
  if (rol === 'ROLE_ALUMNO') return <Navigate to="/dashboard/alumno" replace />;

  return <Navigate to="/" replace />;
};

const RouteChangeLoader = () => {
  const location = useLocation();
  const { startLoading, stopLoading } = useLoadingScreen();
  const firstRenderRef = useRef(true);

  useEffect(() => {
    if (firstRenderRef.current) {
      firstRenderRef.current = false;
      return;
    }

    startLoading();
    let isStopped = false;
    const timeoutId = window.setTimeout(() => {
      stopLoading();
      isStopped = true;
    }, 350);

    return () => {
      window.clearTimeout(timeoutId);
      if (!isStopped) {
        stopLoading();
      }
    };
  }, [location.pathname, location.search, location.hash, startLoading, stopLoading]);

  return null;
};

function App() {
  return (
    <>
      <RouteChangeLoader />
      <Suspense fallback={<PageFallback />}>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/registro" element={<Registro />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route element={<RutasProtegidas />}>
          <Route element={<DashboardLayout />}>
            <Route path="/dashboard" element={<DashboardRedirect />} />
            <Route path="/no-autorizado" element={<NoAutorizado />} />
            <Route path="/perfil-usuario" element={<PerfilUsuario />} />

            <Route element={<RutasProtegidas allowedRoles={['ROLE_ADMIN']} />}>
              <Route path="/dashboard/admin" element={<DashboardAdmin />} />
              <Route path="/usuarios" element={<UsuarioHub />} />
              <Route path="/usuarios/listado" element={<ListadoUsuarios />} />
              <Route path="/usuarios/conectados" element={<UsuariosConectados />} />
              <Route path="/carreras" element={<Carreras />} />
            </Route>

            <Route element={<RutasProtegidas allowedRoles={['ROLE_PROFESOR']} />}>
              <Route path="/dashboard/profesor" element={<DashboardProfesor />} />
              <Route path="/cursos/dictados" element={<CursosDictadosProfesor />} />
            </Route>

            <Route element={<RutasProtegidas allowedRoles={['ROLE_ALUMNO']} />}>
              <Route path="/dashboard/alumno" element={<DashboardAlumno />} />
              <Route path="/modulo/mi-horario" element={<HorarioAlumno />} />
            </Route>

            <Route element={<RutasProtegidas allowedRoles={['ROLE_ADMIN', 'ROLE_PROFESOR', 'ROLE_ALUMNO']} />}>
            {/* Grupo de rutas de Cursos */}
              <Route path="/cursos" element={<CursosHub />} />
              <Route path="/cursos/listado" element={<CursosListado />} />
              <Route path="/modulo/inscripciones" element={<Inscripciones />} />
              <Route path="/modulo/actividades" element={<Actividades />} />
              <Route path="/modulo/reportes" element={<Reportes />} />
              <Route path="/modulo/ia" element={<IAHub />} />
              <Route path="/modulo/ia/chat" element={<EstudIA />} />
              <Route path="/modulo/ia/rubricas" element={<RubricaIA />} />
              <Route path="/modulo/ia/silabo" element={<SilaboIA />} />
              <Route path="/modulo/estudia" element={<Navigate to="/modulo/ia" replace />} />
              <Route path="/modulo/configuracion" element={<Configuracion />} />
              <Route path="/videoconferencia" element={<SalaEstudioMUI />} />
              <Route path="/videoconferencia/:salaUuid" element={<SalaEstudioMUI />} />
              <Route path="/modulo/:moduloNombre" element={<ModuloPlaceholder />} />
              <Route path="/modulo/horarios-profesor" element={<HorarioProfesor />} />
              <Route path="/modulo/chat" element={<ChatInstitucional />} />
            </Route>
            {/* <Route path="/cursos/inscripcion" element={<div>Próximamente: Inscripciones</div>} /> */}
          </Route>
        </Route>
      </Routes>
      </Suspense>
      <AccessibilityFloatingMenu />
      <ReadingAidOverlay />
    </>
  )
}

export default App
