import { useEffect, useMemo, useState } from 'react';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Divider,
  Alert,
  CircularProgress,
  Button,
  Chip,
  Stack,
  Avatar,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import PeopleIcon from '@mui/icons-material/People';
import BookIcon from '@mui/icons-material/Book';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import SchoolIcon from '@mui/icons-material/School';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';
import SpaceDashboardRoundedIcon from '@mui/icons-material/SpaceDashboardRounded';
import { useNavigate } from 'react-router-dom';
import UsuarioService from '../services/UsuarioService';
import CursoService from '../services/CursoService';
import InscripcionService from '../services/InscripcionService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';

const toDate = (value) => {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
};

const isToday = (date) => {
  if (!date) return false;
  const now = new Date();
  return (
    date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
  );
};

const formatRelativo = (date) => {
  if (!date) return '';
  const min = Math.max(1, Math.floor((Date.now() - date.getTime()) / 60000));
  if (min < 60) return `Hace ${min} min`;
  const h = Math.floor(min / 60);
  if (h < 24) return `Hace ${h} h`;
  return `Hace ${Math.floor(h / 24)} d`;
};

const getSaludo = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Buenos días';
  if (h < 18) return 'Buenas tardes';
  return 'Buenas noches';
};

const ACCIONES = [
  { label: 'Usuarios', descripcion: 'Gestionar usuarios y roles', icono: <ManageAccountsIcon />, path: '/usuarios/listado', color: 'primary' },
  { label: 'Cursos', descripcion: 'Administrar cursos activos', icono: <BookIcon />, path: '/cursos/listado', color: 'success' },
  { label: 'Carreras', descripcion: 'Programas académicos', icono: <AccountBalanceIcon />, path: '/carreras', color: 'info' },
  { label: 'Inscripciones', descripcion: 'Solicitudes pendientes', icono: <AssignmentTurnedInIcon />, path: '/modulo/inscripciones', color: 'warning' },
];

const DashboardAdmin = () => {
  const navigate = useNavigate();
  const nombre = localStorage.getItem('nombre') || '';

  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [kpis, setKpis] = useState({
    totalAlumnos: 0,
    totalProfesores: 0,
    cursosActivos: 0,
    inscripcionesPendientes: 0,
  });
  const [actividadReciente, setActividadReciente] = useState([]);

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setIsLoading(true);
        setErrorMsg('');

        const [usuariosResult, cursosResult, pendientesResult, realizadasResult] = await Promise.allSettled([
          UsuarioService.listar(),
          CursoService.listarActivos(),
          InscripcionService.listaPendientesProfesor(),
          InscripcionService.listaRealizadasProfesor(),
        ]);

        const usuarios = usuariosResult.status === 'fulfilled' && Array.isArray(usuariosResult.value)
          ? usuariosResult.value : [];
        const cursos = cursosResult.status === 'fulfilled' && Array.isArray(cursosResult.value)
          ? cursosResult.value : [];
        const pendientes = pendientesResult.status === 'fulfilled' && Array.isArray(pendientesResult.value)
          ? pendientesResult.value : [];
        const realizadas = realizadasResult.status === 'fulfilled' && Array.isArray(realizadasResult.value)
          ? realizadasResult.value : [];

        const totalAlumnos = usuarios.filter((u) => String(u?.rol || '').includes('ALUMNO')).length;
        const totalProfesores = usuarios.filter((u) => String(u?.rol || '').includes('PROFESOR')).length;

        const inscripcionesHoy = realizadas.filter((i) => isToday(toDate(i?.fechaCreacion))).length;

        const actividad = realizadas
          .map((i) => ({
            id: i?.id || String(Math.random()),
            titulo: `${i?.usuario?.nombre || 'Usuario'} inscrito en "${i?.curso?.nombre || '-'}"`,
            tiempo: formatRelativo(toDate(i?.fechaCreacion)),
            fecha: toDate(i?.fechaCreacion),
          }))
          .filter((a) => a.fecha)
          .sort((a, b) => (b.fecha?.getTime?.() || 0) - (a.fecha?.getTime?.() || 0))
          .slice(0, 8);

        setActividadReciente(actividad);
        setKpis({
          totalAlumnos,
          totalProfesores,
          cursosActivos: cursos.length,
          inscripcionesPendientes: pendientes.length,
          inscripcionesHoy,
        });

        if ([usuariosResult, cursosResult].some((r) => r.status === 'rejected')) {
          setErrorMsg('Algunos indicadores no pudieron cargarse.');
        }
      } catch (error) {
        setErrorMsg(error?.response?.data?.error || 'No se pudo cargar el dashboard.');
      } finally {
        setIsLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const stats = useMemo(() => [
    {
      title: 'Total Alumnos',
      value: String(kpis.totalAlumnos),
      icon: <PeopleIcon />,
      color: 'primary',
      subtitle: 'estudiantes registrados',
      onClick: () => navigate('/usuarios/listado'),
    },
    {
      title: 'Total Profesores',
      value: String(kpis.totalProfesores),
      icon: <SchoolIcon />,
      color: 'info',
      subtitle: 'docentes activos',
      onClick: () => navigate('/usuarios/listado'),
    },
    {
      title: 'Cursos Activos',
      value: String(kpis.cursosActivos),
      icon: <BookIcon />,
      color: 'success',
      subtitle: 'en oferta académica',
      onClick: () => navigate('/cursos/listado'),
    },
    {
      title: 'Inscripciones Pendientes',
      value: String(kpis.inscripcionesPendientes),
      icon: <PendingActionsIcon />,
      color: kpis.inscripcionesPendientes > 0 ? 'warning' : 'secondary',
      subtitle: 'por revisar',
      onClick: () => navigate('/modulo/inscripciones'),
    },
  ], [kpis, navigate]);

  return (
    <Box>
      <PageHeader
        title="Panel de Administración"
        subtitle={
          nombre
            ? `${getSaludo()}, ${nombre.split(' ')[0]}. Aquí tienes el resumen de la plataforma.`
            : 'Indicadores y actividad general de la plataforma'
        }
        icon={<SpaceDashboardRoundedIcon />}
      />

      {errorMsg && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {errorMsg}
        </Alert>
      )}

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={3}>
          {stats.map((stat) => (
            <Grid size={{ xs: 12, sm: 6, md: 3 }} key={stat.title}>
              <StatCard {...stat} />
            </Grid>
          ))}

          {/* Acciones Rápidas */}
          <Grid size={{ xs: 12, md: 7 }}>
            <Paper sx={{ p: 3, height: '100%' }}>
              <Typography variant="h6" gutterBottom>
                Acciones Rápidas
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                {ACCIONES.map((accion) => (
                  <Grid size={{ xs: 12, sm: 6 }} key={accion.label}>
                    <Paper
                      variant="outlined"
                      onClick={() => navigate(accion.path)}
                      sx={{
                        p: 2,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                        transition: 'all 0.15s ease',
                        '&:hover': {
                          boxShadow: 3,
                          borderColor: `${accion.color}.main`,
                          transform: 'translateY(-2px)',
                        },
                      }}
                    >
                      <Avatar
                        variant="rounded"
                        sx={{
                          bgcolor: (theme) => alpha(theme.palette[accion.color === 'info' ? 'info' : accion.color].main, 0.14),
                          color: `${accion.color}.main`,
                          width: 44,
                          height: 44,
                        }}
                      >
                        {accion.icono}
                      </Avatar>
                      <Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                          {accion.label}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {accion.descripcion}
                        </Typography>
                      </Box>
                    </Paper>
                  </Grid>
                ))}
              </Grid>

              {kpis.inscripcionesHoy > 0 && (
                <Box sx={{ mt: 2, pt: 2, borderTop: 1, borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Chip label={`${kpis.inscripcionesHoy} inscripción(es) hoy`} size="small" color="success" variant="outlined" />
                </Box>
              )}
            </Paper>
          </Grid>

          {/* Actividad Reciente */}
          <Grid size={{ xs: 12, md: 5 }}>
            <Paper sx={{ p: 3, height: '100%', maxHeight: 380, overflowY: 'auto' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="h6">Actividad Reciente</Typography>
                <Button size="small" onClick={() => navigate('/modulo/inscripciones')}>
                  Ver más
                </Button>
              </Box>
              <Divider sx={{ mb: 2 }} />

              {actividadReciente.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No hay actividad reciente registrada.
                </Typography>
              ) : (
                <Stack spacing={1.5}>
                  {actividadReciente.map((item) => (
                    <Box key={item.id} sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}>
                      <FiberManualRecordIcon
                        sx={{ fontSize: 10, mt: 0.75, color: 'primary.main', flexShrink: 0 }}
                      />
                      <Box sx={{ minWidth: 0, flexGrow: 1 }}>
                        <Typography variant="body2" noWrap title={item.titulo}>
                          {item.titulo}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {item.tiempo}
                        </Typography>
                      </Box>
                    </Box>
                  ))}
                </Stack>
              )}
            </Paper>
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default DashboardAdmin;
