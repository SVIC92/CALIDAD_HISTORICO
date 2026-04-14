import { useEffect, useMemo, useState } from 'react';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  Divider,
  Avatar,
  Alert,
  CircularProgress,
  Button,
} from '@mui/material';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import { useNavigate } from 'react-router-dom';
import CursoService from '../services/CursoService';
import ActividadService from '../services/ActividadService';
import ReporteService from '../services/ReporteService';

const KpiCard = ({ title, value, icon, color }) => (
  <Card sx={{ height: '100%', boxShadow: 3 }}>
    <CardContent>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <Avatar sx={{ bgcolor: color, mr: 2 }}>
          {icon}
        </Avatar>
        <Typography variant="h6" color="text.secondary">
          {title}
        </Typography>
      </Box>
      <Typography variant="h4" component="div" sx={{ fontWeight: 'bold' }}>
        {value}
      </Typography>
    </CardContent>
  </Card>
);

const normalizeReportesFromDetalle = (detalle) => {
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
  const reporteConEstado = reportes.find((r) => r?.estado);
  return reporteConEstado?.estado || '';
};

const isSameDay = (a, b) => (
  a.getFullYear() === b.getFullYear()
  && a.getMonth() === b.getMonth()
  && a.getDate() === b.getDate()
);

const DashboardAlumno = () => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [stats, setStats] = useState([
    { title: 'Cursos Inscritos', value: '0', icon: <MenuBookIcon />, color: '#1565c0' },
    { title: 'Inscripciones Aprobadas', value: '0', icon: <CheckCircleIcon />, color: '#2e7d32' },
    { title: 'Actividades Entregadas', value: '0', icon: <AssignmentTurnedInIcon />, color: '#ef6c00' },
    { title: 'Promedio General', value: '-', icon: <EmojiEventsIcon />, color: '#6a1b9a' },
  ]);
  const [upcoming, setUpcoming] = useState([]);

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setIsLoading(true);
        setErrorMsg('');

        const cursosData = await CursoService.listarInscritosAlumno();
        const cursos = Array.isArray(cursosData) ? cursosData : [];

        const cursosCount = cursos.length;
        const inscripcionesAprobadas = cursos.length;

        const actividadesPorCurso = await Promise.all(
          cursos.map(async (curso) => {
            const cursoId = curso?.id || curso?._id;
            if (!cursoId) return [];
            try {
              const actividadesData = await ActividadService.listar(cursoId);
              const actividades = Array.isArray(actividadesData)
                ? actividadesData
                : Array.isArray(actividadesData?.actividades)
                  ? actividadesData.actividades
                  : [];

              return actividades.map((actividad) => ({
                ...actividad,
                cursoNombre: curso?.nombre || 'Curso',
              }));
            } catch {
              return [];
            }
          })
        );

        const actividades = actividadesPorCurso.flat();

        const reportesPorActividad = await Promise.all(
          actividades.map(async (actividad) => {
            const actividadId = actividad?.id || actividad?._id;
            if (!actividadId) return { actividadId: '', reportes: [] };
            try {
              const detalle = await ReporteService.detalle(actividadId);
              return {
                actividadId,
                reportes: normalizeReportesFromDetalle(detalle),
              };
            } catch {
              return {
                actividadId,
                reportes: [],
              };
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

        const entregadas = reportes.filter(hasRespuestaEntregada).length;

        const notas = reportes
          .map((r) => Number(r?.nota))
          .filter((nota) => !Number.isNaN(nota));

        const promedio = notas.length > 0
          ? (notas.reduce((acc, n) => acc + n, 0) / notas.length).toFixed(1)
          : '-';

        const now = new Date();
        const upcomingData = actividades
          .map((actividad) => {
            const rawDate = actividad?.fechaVencimiento || actividad?.fechaEntrega || actividad?.fechaLimite;
            const fecha = rawDate ? new Date(rawDate) : null;
            return {
              id: actividad?.id || actividad?._id || Math.random().toString(36),
              nombre: actividad?.nombre || 'Actividad',
              cursoNombre: actividad?.cursoNombre || 'Curso',
              fecha,
              entregada: actividadesEntregadasIds.has(actividad?.id || actividad?._id),
              estado:
                estadoBackendPorActividad.get(actividad?.id || actividad?._id)
                || (actividadesEntregadasIds.has(actividad?.id || actividad?._id)
                  ? 'ENTREGADA'
                  : isSameDay(fecha, now)
                    ? 'VENCE HOY'
                    : 'PENDIENTE'),
            };
          })
          .filter((item) => item.fecha && !Number.isNaN(item.fecha.getTime()) && item.fecha >= now)
          .sort((a, b) => a.fecha - b.fecha)
          .slice(0, 3);

        setUpcoming(upcomingData);
        setStats([
          { title: 'Cursos Inscritos', value: String(cursosCount), icon: <MenuBookIcon />, color: '#1565c0' },
          { title: 'Inscripciones Aprobadas', value: String(inscripcionesAprobadas), icon: <CheckCircleIcon />, color: '#2e7d32' },
          { title: 'Actividades Entregadas', value: String(entregadas), icon: <AssignmentTurnedInIcon />, color: '#ef6c00' },
          { title: 'Promedio General', value: String(promedio), icon: <EmojiEventsIcon />, color: '#6a1b9a' },
        ]);
      } catch (error) {
        const backendMessage = error?.response?.data?.error || error?.response?.data;
        setErrorMsg(backendMessage || 'No se pudieron cargar los datos del dashboard de alumno.');
      } finally {
        setIsLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const estadoAcademico = useMemo(() => {
    const promedio = Number(stats[3]?.value);
    if (Number.isNaN(promedio)) return 'Aun no hay calificaciones suficientes para calcular tu promedio.';
    if (promedio >= 17) return 'Excelente rendimiento. Mantente constante en tus entregas.';
    if (promedio >= 13) return 'Buen avance academico. Puedes subir mas tu promedio con entregas puntuales.';
    return 'Tu promedio requiere atencion. Prioriza actividades pendientes y consulta a tus profesores.';
  }, [stats]);

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 'medium' }}>
        Dashboard de Alumno
      </Typography>

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
              <KpiCard {...stat} />
            </Grid>
          ))}

          <Grid size={{ xs: 12, md: 8 }}>
            <Paper sx={{ p: 3, height: '360px', display: 'flex', flexDirection: 'column' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6" gutterBottom>
                  Proximas Entregas
                </Typography>
                <Button size="small" onClick={() => navigate('/modulo/actividades')}>
                  Ver actividades
                </Button>
              </Box>
              <Divider sx={{ mb: 2 }} />

              {upcoming.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No hay actividades con vencimiento proximo.
                </Typography>
              ) : (
                upcoming.map((item) => (
                  <Box key={item.id} sx={{ mb: 2 }}>
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: 'bold',
                        color: isSameDay(item.fecha, new Date()) && !item.entregada ? 'error.main' : 'text.primary',
                      }}
                    >
                      {item.nombre} - {item.cursoNombre}
                    </Typography>
                    <Typography
                      variant="caption"
                      color={isSameDay(item.fecha, new Date()) && !item.entregada ? 'error.main' : 'text.secondary'}
                    >
                      Fecha limite: {item.fecha.toLocaleString()}
                      {isSameDay(item.fecha, new Date()) && !item.entregada ? ' (vence hoy, no entregada)' : ''}
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{
                        display: 'block',
                        mt: 0.5,
                        fontWeight: 600,
                        color: (() => {
                          const estadoUpper = String(item.estado || '').toUpperCase();
                          if (estadoUpper.includes('VENCE HOY')) return 'error.main';
                          if (isEstadoEntregado(estadoUpper)) return 'success.main';
                          return 'warning.main';
                        })(),
                      }}
                    >
                      Estado: {item.estado}
                    </Typography>
                  </Box>
                ))
              )}
            </Paper>
          </Grid>

          <Grid size={{ xs: 12, md: 4 }}>
            <Paper sx={{ p: 3, height: '360px', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" gutterBottom>
                Estado Academico
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {estadoAcademico}
              </Typography>
              <Button variant="outlined" onClick={() => navigate('/modulo/reportes')}>
                Ver mis reportes
              </Button>
            </Paper>
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default DashboardAlumno;
