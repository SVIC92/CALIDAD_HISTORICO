import { useEffect, useRef } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import DashboardLayout from './layouts/DashboardLayout';
import RutasProtegidas from './components/RutasProtegidas';
import Login from './pages/Login'
import Registro from './pages/Registro';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import PerfilUsuario from './pages/PerfilUsuario';
import DashboardAdmin from './pages/DashboardAdmin';
import DashboardProfesor from './pages/DashboardProfesor';
import DashboardAlumno from './pages/DashboardAlumno';
import CursosHub from './pages/CursosHub';
import CursosListado from './pages/CursosListado';
import CursosDictadosProfesor from './pages/CursosDictadosProfesor';
import Actividades from './pages/Actividades';
import Inscripciones from './pages/Inscripciones';
import Reportes from './pages/Reportes';
import Configuracion from './pages/Configuracion';
import EstudIA from './pages/EstudIA';
import ListadoUsuarios from './pages/ListadoUsuarios';
import UsuarioHub from './pages/UsuarioHub';
import UsuariosConectados from './pages/UsuariosConectados';
import ModuloPlaceholder from './pages/ModuloPlaceholder';
import NoAutorizado from './pages/NoAutorizado';
import { useLoadingScreen } from './context/LoadingScreenContext';

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
    const timeoutId = window.setTimeout(() => {
      stopLoading();
    }, 350);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [location.pathname, location.search, location.hash, startLoading, stopLoading]);

  return null;
};

function App() {
  return (
    <>
      <RouteChangeLoader />
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
            </Route>

            <Route element={<RutasProtegidas allowedRoles={['ROLE_PROFESOR']} />}>
              <Route path="/dashboard/profesor" element={<DashboardProfesor />} />
              <Route path="/cursos/dictados" element={<CursosDictadosProfesor />} />
            </Route>

            <Route element={<RutasProtegidas allowedRoles={['ROLE_ALUMNO']} />}>
              <Route path="/dashboard/alumno" element={<DashboardAlumno />} />
            </Route>

            <Route element={<RutasProtegidas allowedRoles={['ROLE_ADMIN', 'ROLE_PROFESOR', 'ROLE_ALUMNO']} />}>
            {/* Grupo de rutas de Cursos */}
              <Route path="/cursos" element={<CursosHub />} />
              <Route path="/cursos/listado" element={<CursosListado />} />
              <Route path="/modulo/inscripciones" element={<Inscripciones />} />
              <Route path="/modulo/actividades" element={<Actividades />} />
              <Route path="/modulo/reportes" element={<Reportes />} />
              <Route path="/modulo/estudia" element={<EstudIA />} />
              <Route path="/modulo/configuracion" element={<Configuracion />} />
              <Route path="/modulo/:moduloNombre" element={<ModuloPlaceholder />} />
            </Route>
            {/* <Route path="/cursos/inscripcion" element={<div>Próximamente: Inscripciones</div>} /> */}
          </Route>
        </Route>
      </Routes>
    </>
  )
}

export default App
