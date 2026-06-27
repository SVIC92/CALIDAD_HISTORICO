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
  LinearProgress,
} from '@mui/material';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import SpaceDashboardRoundedIcon from '@mui/icons-material/SpaceDashboardRounded';
import { useNavigate } from 'react-router-dom';
import CursoService from '../services/CursoService';
import ActividadService from '../services/ActividadService';
import ReporteService from '../services/ReporteService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';

const normalizeReportes = (detalle) => {
  if (!detalle) return [];
  if (Array.isArray(detalle)) return detalle;
  if (Array.isArray(detalle.reportes)) return detalle.reportes;
  if (Array.isArray(detalle.items)) return detalle.items;
  return [detalle];
};

const hasRespuestaEntregada = (reporte) => {
  const respuesta = reporte?.respuesta || reporte?.contenido || reporte?.detalle;
  return typeof respuesta === 'string' ? respuesta.trim().length > 0 : Boolean(respuesta);
};

const isEstadoEntregado = (estadoRaw) => {
  const estado = String(estadoRaw || '').toUpperCase();
  return estado.includes('ENTREG') || estado.includes('ENVIAD') || estado.includes('CALIFIC') || estado.includes('APROBAD');
};

const getEstadoBackend = (reportes) => {
  if (!Array.isArray(reportes) || reportes.length === 0) return '';
  return reportes.find((r) => r?.estado)?.estado || '';
};

const isSameDay = (a, b) => (
  a.getFullYear() === b.getFullYear()
  && a.getMonth() === b.getMonth()
  && a.getDate() === b.getDate()
);

const getSaludo = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Buenos días';
  if (h < 18) return 'Buenas tardes';
  return 'Buenas noches';
};

const chipUrgencia = (fecha, entregada) => {
  if (entregada) return { label: 'Entregada', color: 'success' };
  const now = new Date();
  if (isSameDay(fecha, now)) return { label: 'Vence hoy', color: 'error' };
  const diffDias = Math.ceil((fecha.getTime() - now.getTime()) / 86400000);
  if (diffDias === 1) return { label: 'Mañana', color: 'warning' };
  if (diffDias <= 3) return { label: `En ${diffDias} días`, color: 'warning' };
  return { label: `En ${diffDias} días`, color: 'default' };
};

const DashboardAlumno = () => {
  const navigate = useNavigate();
  const nombre = localStorage.getItem('nombre') || '';

  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [kpis, setKpis] = useState({
    cursosInscritos: 0,
    actividadesEntregadas: 0,
    actividadesPendientes: 0,
    promedio: '-',
  });
  const [upcoming, setUpcoming] = useState([]);
  const [cursosList, setCursosList] = useState([]);

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setIsLoading(true);
        setErrorMsg('');

        const cursosData = await CursoService.listarInscritosAlumno();
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
            if (!actividadId) return { actividadId: '', reportes: [] };
            try {
              const detalle = await ReporteService.detalle(actividadId);
              return { actividadId, reportes: normalizeReportes(detalle) };
            } catch {
              return { actividadId, reportes: [] };
            }
          })
        );

        const reportes = reportesPorActividad.flatMap((item) => item.reportes);

        const actividadesEntregadasIds = new Set(
          reportesPorActividad
            .filter((item) => {
              const estadoBackend = getEstadoBackend(item.reportes);
              return isEstadoEntregado(estadoBackend) || item.reportes.some(hasRespuestaEntregada);
            })
            .map((item) => item.actividadId)
            .filter(Boolean)
        );

        const estadoBackendPorActividad = new Map(
          reportesPorActividad.map((item) => [item.actividadId, getEstadoBackend(item.reportes)])
        );

        const notas = reportes.map((r) => Number(r?.nota)).filter((n) => !Number.isNaN(n));
        const promedioNum = notas.length > 0
          ? notas.reduce((acc, n) => acc + n, 0) / notas.length
          : null;
        const promedioStr = promedioNum != null ? promedioNum.toFixed(1) : '-';

        const now = new Date();
        const upcomingData = actividades
          .map((actividad) => {
            const rawDate = actividad?.fechaVencimiento || actividad?.fechaEntrega || actividad?.fechaLimite;
            const fecha = rawDate ? new Date(rawDate) : null;
            if (!fecha || Number.isNaN(fecha.getTime())) return null;
            const actId = actividad?.id || actividad?._id;
            const entregada = actividadesEntregadasIds.has(actId);
            return {
              id: actId || String(Math.random()),
              nombre: actividad?.nombre || 'Actividad',
              cursoNombre: actividad?.cursoNombre || 'Curso',
              fecha,
              entregada,
              estado: estadoBackendPorActividad.get(actId) || (entregada ? 'ENTREGADA' : 'PENDIENTE'),
            };
          })
          .filter((item) => item && item.fecha >= now)
          .sort((a, b) => a.fecha - b.fecha)
          .slice(0, 4);

        const pendientes = Math.max(0, actividades.length - actividadesEntregadasIds.size);

        setCursosList(cursos.slice(0, 4));
        setUpcoming(upcomingData);
        setKpis({
          cursosInscritos: cursos.length,
          actividadesEntregadas: actividadesEntregadasIds.size,
          actividadesPendientes: pendientes,
          promedio: promedioStr,
        });
      } catch (error) {
        setErrorMsg(error?.response?.data?.error || 'No se pudieron cargar los datos del dashboard.');
      } finally {
        setIsLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const promedioNum = Number(kpis.promedio);
  const promedioValido = !Number.isNaN(promedioNum);
  const promedioPercent = promedioValido ? Math.min(100, (promedioNum / 20) * 100) : 0;
  const promedioColor = promedioValido
    ? promedioNum >= 17 ? 'success' : promedioNum >= 13 ? 'warning' : 'error'
    : 'inherit';

  const mensajeAcademico = useMemo(() => {
    if (!promedioValido) return 'Entrega tus primeras actividades para calcular tu promedio.';
    if (promedioNum >= 17) return '¡Excelente rendimiento! Mantén el ritmo de entregas puntuales.';
    if (promedioNum >= 13) return 'Buen avance académico. Puedes subir tu promedio con entregas constantes.';
    return 'Tu promedio requiere atención. Prioriza actividades pendientes y consulta a tus profesores.';
  }, [promedioValido, promedioNum]);

  const stats = useMemo(() => [
    {
      title: 'Cursos Inscritos',
      value: String(kpis.cursosInscritos),
      icon: <MenuBookIcon />,
      color: 'primary',
      subtitle: 'actualmente cursando',
      onClick: () => navigate('/cursos'),
    },
    {
      title: 'Actividades Entregadas',
      value: String(kpis.actividadesEntregadas),
      icon: <AssignmentTurnedInIcon />,
      color: 'success',
      subtitle: 'reportes enviados',
      onClick: () => navigate('/modulo/reportes'),
    },
    {
      title: 'Pendientes',
      value: String(kpis.actividadesPendientes),
      icon: <PendingActionsIcon />,
      color: kpis.actividadesPendientes > 0 ? 'warning' : 'success',
      subtitle: 'actividades por entregar',
      onClick: () => navigate('/modulo/actividades'),
    },
    {
      title: 'Promedio General',
      value: kpis.promedio,
      icon: <EmojiEventsIcon />,
      color: promedioValido
        ? promedioNum >= 17 ? 'success' : promedioNum >= 13 ? 'warning' : 'error'
        : 'secondary',
      subtitle: 'en escala 0–20',
      onClick: () => navigate('/modulo/reportes'),
    },
  ], [kpis, navigate, promedioValido, promedioNum]);

  return (
    <Box>
      <PageHeader
        title="Dashboard de Alumno"
        subtitle={
          nombre
            ? `${getSaludo()}, ${nombre.split(' ')[0]}. Aquí tienes tu resumen académico.`
            : 'Tu progreso académico y próximas entregas'
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

          {/* Próximas Entregas */}
          <Grid size={{ xs: 12, md: 8 }}>
            <Paper sx={{ p: 3, minHeight: 320 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="h6">Próximas Entregas</Typography>
                <Button size="small" onClick={() => navigate('/modulo/actividades')}>
                  Ver actividades
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
                    border: 1,
                    borderColor: (theme) => `${theme.palette.success.main}40`,
                    bgcolor: (theme) => `${theme.palette.success.main}10`,
                  }}
                >
                  <CheckCircleOutlinedIcon color="success" />
                  <Typography variant="body2">
                    No hay actividades con vencimiento próximo.
                  </Typography>
                </Box>
              ) : (
                <Stack spacing={1.5}>
                  {upcoming.map((item) => {
                    const chip = chipUrgencia(item.fecha, item.entregada);
                    const esCritico = chip.color === 'error';
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
                          borderColor: esCritico ? 'error.main' : chip.color === 'warning' ? 'warning.main' : 'divider',
                          borderLeftWidth: 3,
                          bgcolor: esCritico
                            ? (theme) => `${theme.palette.error.main}08`
                            : 'background.paper',
                        }}
                      >
                        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                          <Typography
                            variant="body2"
                            sx={{ fontWeight: 600, color: esCritico ? 'error.main' : 'text.primary' }}
                            noWrap
                          >
                            {item.nombre}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {item.cursoNombre} · {item.fecha.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
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

          {/* Estado Académico */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Paper sx={{ p: 3, minHeight: 320, display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" gutterBottom>
                Estado Académico
              </Typography>
              <Divider sx={{ mb: 2 }} />

              {/* Promedio visual */}
              <Box sx={{ textAlign: 'center', mb: 2 }}>
                <Typography
                  variant="h2"
                  sx={{ fontWeight: 900, color: `${promedioColor}.main`, lineHeight: 1 }}
                >
                  {kpis.promedio}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  promedio general
                </Typography>
                {promedioValido && (
                  <LinearProgress
                    variant="determinate"
                    value={promedioPercent}
                    color={promedioColor}
                    sx={{ mt: 1, height: 6, borderRadius: 3 }}
                  />
                )}
              </Box>

              <Typography variant="body2" color="text.secondary" sx={{ mb: 2, textAlign: 'center' }}>
                {mensajeAcademico}
              </Typography>

              {/* Mis cursos */}
              {cursosList.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, mb: 0.5, display: 'block' }}>
                    MIS CURSOS
                  </Typography>
                  <Stack spacing={0.5}>
                    {cursosList.map((c) => (
                      <Typography
                        key={c?.id || c?._id}
                        variant="body2"
                        noWrap
                        sx={{ color: 'text.secondary', fontSize: '0.8rem' }}
                        title={c?.nombre}
                      >
                        · {c?.nombre || 'Curso'}
                      </Typography>
                    ))}
                    {kpis.cursosInscritos > 4 && (
                      <Typography variant="caption" color="text.disabled">
                        y {kpis.cursosInscritos - 4} más...
                      </Typography>
                    )}
                  </Stack>
                </Box>
              )}

              <Stack spacing={1} sx={{ mt: 'auto' }}>
                <Button variant="outlined" size="small" fullWidth onClick={() => navigate('/modulo/reportes')}>
                  Ver mis reportes
                </Button>
                <Button variant="text" size="small" fullWidth onClick={() => navigate('/modulo/ia/chat')}>
                  Consultar al asistente IA
                </Button>
              </Stack>
            </Paper>
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default DashboardAlumno;
