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
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import AssignmentIcon from '@mui/icons-material/Assignment';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import AutoGraphIcon from '@mui/icons-material/AutoGraph';
import GroupAddIcon from '@mui/icons-material/GroupAdd';
import SpaceDashboardRoundedIcon from '@mui/icons-material/SpaceDashboardRounded';
import { useNavigate } from 'react-router-dom';
import CursoService from '../services/CursoService';
import ActividadService from '../services/ActividadService';
import ReporteService from '../services/ReporteService';
import InscripcionService from '../services/InscripcionService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';

const normalizeReportes = (data) => {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (Array.isArray(data.reportes)) return data.reportes;
  if (Array.isArray(data.items)) return data.items;
  return [data];
};

const getSaludo = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Buenos días';
  if (h < 18) return 'Buenas tardes';
  return 'Buenas noches';
};

const chipUrgencia = (diffDias) => {
  if (diffDias === 0) return { label: 'Vence hoy', color: 'error' };
  if (diffDias === 1) return { label: 'Mañana', color: 'warning' };
  if (diffDias <= 3) return { label: `En ${diffDias} días`, color: 'warning' };
  return { label: `En ${diffDias} días`, color: 'default' };
};

const DashboardProfesor = () => {
  const navigate = useNavigate();
  const nombre = localStorage.getItem('nombre') || '';

  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [kpis, setKpis] = useState({ cursos: 0, actividades: 0, pendientesCalificar: 0, cumplimiento: 100 });
  const [upcoming, setUpcoming] = useState([]);
  const [alertas, setAlertas] = useState({ pendientesCalificar: 0, solicitudesPendientes: 0 });

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setIsLoading(true);
        setErrorMsg('');

        const cursosData = await CursoService.listarInscritosProfesor();
        const cursos = Array.isArray(cursosData) ? cursosData : [];

        const actividadesPorCurso = await Promise.all(
          cursos.map(async (curso) => {
            const cursoId = curso?.id || curso?._id;
            if (!cursoId) return [];
            try {
              const data = await ActividadService.listar(cursoId);
              const actividades = Array.isArray(data)
                ? data
                : Array.isArray(data?.actividades)
                  ? data.actividades
                  : [];
              return actividades.map((a) => ({ ...a, cursoNombre: curso?.nombre || 'Curso' }));
            } catch { return []; }
          })
        );

        const actividades = actividadesPorCurso.flat();

        const reportesPorActividad = await Promise.all(
          actividades.map(async (actividad) => {
            const actividadId = actividad?.id || actividad?._id;
            if (!actividadId) return [];
            try { return normalizeReportes(await ReporteService.listar(actividadId)); }
            catch { return []; }
          })
        );

        const reportes = reportesPorActividad.flat();

        const pendientesCalificar = reportes.filter((r) => {
          const estado = String(r?.estado || '').toUpperCase();
          return estado.includes('PENDIENTE') || estado.includes('ENTREGADO') || r?.nota == null;
        }).length;

        const calificados = reportes.length - pendientesCalificar;
        const cumplimiento = reportes.length > 0
          ? Math.round((calificados / reportes.length) * 100)
          : 100;

        const now = new Date();
        const proximasActividades = actividades
          .map((a) => {
            const rawDate = a?.fechaVencimiento || a?.fechaEntrega || a?.fechaLimite;
            const fecha = rawDate ? new Date(rawDate) : null;
            if (!fecha || Number.isNaN(fecha.getTime())) return null;
            const diffDias = Math.ceil((fecha.getTime() - now.getTime()) / 86400000);
            return {
              id: a?.id || a?._id,
              nombre: a?.nombre || 'Actividad',
              cursoNombre: a?.cursoNombre || 'Curso',
              diffDias,
            };
          })
          .filter((item) => item && item.diffDias >= 0)
          .sort((a, b) => a.diffDias - b.diffDias)
          .slice(0, 5);

        let solicitudesPendientes = 0;
        try {
          const pend = await InscripcionService.listaPendientesAlumno();
          solicitudesPendientes = Array.isArray(pend) ? pend.length : 0;
        } catch { /* silent */ }

        setUpcoming(proximasActividades);
        setAlertas({ pendientesCalificar, solicitudesPendientes });
        setKpis({ cursos: cursos.length, actividades: actividades.length, pendientesCalificar, cumplimiento });
      } catch (error) {
        setErrorMsg(error?.response?.data?.error || 'No se pudieron cargar los datos del dashboard.');
      } finally {
        setIsLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const stats = useMemo(() => [
    {
      title: 'Cursos Asignados',
      value: String(kpis.cursos),
      icon: <SchoolIcon />,
      color: 'primary',
      subtitle: 'a tu cargo',
      onClick: () => navigate('/cursos/dictados'),
    },
    {
      title: 'Actividades',
      value: String(kpis.actividades),
      icon: <AssignmentIcon />,
      color: 'success',
      subtitle: 'en todos tus cursos',
      onClick: () => navigate('/modulo/actividades'),
    },
    {
      title: 'Por Calificar',
      value: String(kpis.pendientesCalificar),
      icon: <FactCheckIcon />,
      color: kpis.pendientesCalificar > 0 ? 'warning' : 'success',
      subtitle: 'reportes pendientes',
      onClick: () => navigate('/modulo/reportes'),
    },
    {
      title: 'Cumplimiento',
      value: `${kpis.cumplimiento}%`,
      icon: <AutoGraphIcon />,
      color: kpis.cumplimiento >= 80 ? 'success' : 'warning',
      subtitle: 'reportes calificados',
    },
  ], [kpis, navigate]);

  return (
    <Box>
      <PageHeader
        title="Dashboard de Profesor"
        subtitle={
          nombre
            ? `${getSaludo()}, ${nombre.split(' ')[0]}. Resumen de tus cursos y pendientes.`
            : 'Resumen de tus cursos, actividades y pendientes'
        }
        icon={<SpaceDashboardRoundedIcon />}
      />

      {errorMsg && (
        <Alert severity="error" sx={{ mb: 2 }}>
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

          {/* Actividades Próximas */}
          <Grid size={{ xs: 12, md: 7 }}>
            <Paper sx={{ p: 3, minHeight: 320 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="h6">Actividades Próximas</Typography>
                <Button size="small" onClick={() => navigate('/modulo/actividades')}>
                  Ver todas
                </Button>
              </Box>
              <Divider sx={{ mb: 2 }} />

              {upcoming.length === 0 ? (
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    p: 2,
                    borderRadius: 2,
                    bgcolor: (theme) => theme.palette.mode === 'dark' ? 'success.900' : 'success.50',
                    border: 1,
                    borderColor: (theme) => `${theme.palette.success.main}40`,
                  }}
                >
                  <CheckCircleOutlinedIcon color="success" />
                  <Typography variant="body2">No hay actividades próximas por vencer.</Typography>
                </Box>
              ) : (
                <Stack spacing={1.5}>
                  {upcoming.map((item) => {
                    const chip = chipUrgencia(item.diffDias);
                    return (
                      <Box
                        key={item.id}
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 2,
                          p: 1.5,
                          borderRadius: 2,
                          border: 1,
                          borderColor: chip.color === 'error' ? 'error.main' : chip.color === 'warning' ? 'warning.main' : 'divider',
                          borderLeftWidth: 3,
                          bgcolor: 'background.paper',
                        }}
                      >
                        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                          <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                            {item.nombre}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {item.cursoNombre}
                          </Typography>
                        </Box>
                        <Chip
                          label={chip.label}
                          color={chip.color}
                          size="small"
                          sx={{ flexShrink: 0 }}
                        />
                      </Box>
                    );
                  })}
                </Stack>
              )}
            </Paper>
          </Grid>

          {/* Alertas y Acciones */}
          <Grid size={{ xs: 12, md: 5 }}>
            <Paper sx={{ p: 3, minHeight: 320, display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" gutterBottom>
                Alertas y Acciones
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Stack spacing={1.5} sx={{ flexGrow: 1 }}>
                {alertas.pendientesCalificar > 0 ? (
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1.5,
                      p: 1.5,
                      borderRadius: 2,
                      border: 1,
                      borderColor: (theme) => `${theme.palette.warning.main}50`,
                      bgcolor: (theme) => `${theme.palette.warning.main}12`,
                    }}
                  >
                    <FactCheckIcon color="warning" />
                    <Typography variant="body2" sx={{ flexGrow: 1 }}>
                      Reportes pendientes de calificación
                    </Typography>
                    <Chip label={alertas.pendientesCalificar} color="warning" size="small" />
                  </Box>
                ) : (
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1.5,
                      p: 1.5,
                      borderRadius: 2,
                      border: 1,
                      borderColor: (theme) => `${theme.palette.success.main}50`,
                      bgcolor: (theme) => `${theme.palette.success.main}12`,
                    }}
                  >
                    <CheckCircleOutlinedIcon color="success" />
                    <Typography variant="body2">
                      Sin reportes pendientes. ¡Al día con tus calificaciones!
                    </Typography>
                  </Box>
                )}

                {alertas.solicitudesPendientes > 0 && (
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1.5,
                      p: 1.5,
                      borderRadius: 2,
                      border: 1,
                      borderColor: (theme) => `${theme.palette.info.main}50`,
                      bgcolor: (theme) => `${theme.palette.info.main}12`,
                    }}
                  >
                    <GroupAddIcon color="info" />
                    <Typography variant="body2" sx={{ flexGrow: 1 }}>
                      Solicitudes de inscripción pendientes
                    </Typography>
                    <Chip label={alertas.solicitudesPendientes} color="info" size="small" />
                  </Box>
                )}

                {alertas.pendientesCalificar === 0 && alertas.solicitudesPendientes === 0 && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    No tienes alertas pendientes por el momento.
                  </Typography>
                )}
              </Stack>

              <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
                <Button
                  size="small"
                  variant="outlined"
                  fullWidth
                  onClick={() => navigate('/modulo/inscripciones')}
                >
                  Inscripciones
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  fullWidth
                  onClick={() => navigate('/modulo/reportes')}
                >
                  Reportes
                </Button>
              </Stack>
            </Paper>
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default DashboardProfesor;
