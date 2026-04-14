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
import SchoolIcon from '@mui/icons-material/School';
import AssignmentIcon from '@mui/icons-material/Assignment';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import { useNavigate } from 'react-router-dom';
import CursoService from '../services/CursoService';
import ActividadService from '../services/ActividadService';
import ReporteService from '../services/ReporteService';
import InscripcionService from '../services/InscripcionService';

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

const getUserEmailFromToken = () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) return '';
    const [, payloadBase64] = token.split('.');
    if (!payloadBase64) return '';

    const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const payload = JSON.parse(window.atob(padded));
    return (payload?.sub || payload?.email || '').toLowerCase();
  } catch {
    return '';
  }
};

const hasProfesorAsignado = (curso, email) => {
  const profesor = curso?.profesorAsignado;
  if (!profesor || !email) return false;

  if (typeof profesor === 'string') {
    return profesor.toLowerCase() === email;
  }

  return (profesor?.email || '').toLowerCase() === email;
};

const normalizeReportes = (data) => {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (Array.isArray(data.reportes)) return data.reportes;
  if (Array.isArray(data.items)) return data.items;
  return [data];
};

const DashboardProfesor = () => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [stats, setStats] = useState([
    { title: 'Cursos Asignados', value: '0', icon: <SchoolIcon />, color: '#1565c0' },
    { title: 'Actividades Activas', value: '0', icon: <AssignmentIcon />, color: '#2e7d32' },
    { title: 'Pendientes por Calificar', value: '0', icon: <FactCheckIcon />, color: '#ef6c00' },
    { title: 'Cumplimiento Semanal', value: '0%', icon: <TrendingUpIcon />, color: '#6a1b9a' },
  ]);
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
            if (!actividadId) return [];
            try {
              const listData = await ReporteService.listar(actividadId);
              return normalizeReportes(listData);
            } catch {
              return [];
            }
          })
        );

        const reportes = reportesPorActividad.flat();

        const pendientesCalificar = reportes.filter((reporte) => {
          const estado = String(reporte?.estado || '').toUpperCase();
          if (estado.includes('PENDIENTE')) return true;
          if (estado.includes('ENTREGADO')) return true;
          return reporte?.nota === null || reporte?.nota === undefined;
        }).length;

        const totalReportes = reportes.length;
        const calificados = totalReportes - pendientesCalificar;
        const cumplimientoSemanal = totalReportes > 0
          ? `${Math.round((calificados / totalReportes) * 100)}%`
          : '0%';

        const now = new Date();
        const proximasActividades = actividades
          .map((actividad) => {
            const rawDate = actividad?.fechaVencimiento || actividad?.fechaEntrega || actividad?.fechaLimite;
            const fecha = rawDate ? new Date(rawDate) : null;
            if (!fecha || Number.isNaN(fecha.getTime())) return null;

            const diffMs = fecha.getTime() - now.getTime();
            const diffDias = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

            return {
              id: actividad?.id || actividad?._id,
              nombre: actividad?.nombre || 'Actividad',
              cursoNombre: actividad?.cursoNombre || 'Curso',
              fecha,
              diffDias,
            };
          })
          .filter((item) => item && item.diffDias >= 0)
          .sort((a, b) => a.fecha - b.fecha)
          .slice(0, 4);

        let solicitudesPendientes = 0;
        try {
          const pendientesInscripcion = await InscripcionService.listaPendientesAlumno();
          solicitudesPendientes = Array.isArray(pendientesInscripcion) ? pendientesInscripcion.length : 0;
        } catch {
          solicitudesPendientes = 0;
        }

        setUpcoming(proximasActividades);
        setAlertas({ pendientesCalificar, solicitudesPendientes });
        setStats([
          { title: 'Cursos Asignados', value: String(cursos.length), icon: <SchoolIcon />, color: '#1565c0' },
          { title: 'Actividades Activas', value: String(actividades.length), icon: <AssignmentIcon />, color: '#2e7d32' },
          { title: 'Pendientes por Calificar', value: String(pendientesCalificar), icon: <FactCheckIcon />, color: '#ef6c00' },
          { title: 'Cumplimiento Semanal', value: cumplimientoSemanal, icon: <TrendingUpIcon />, color: '#6a1b9a' },
        ]);
      } catch (error) {
        const backendMessage = error?.response?.data?.error || error?.response?.data;
        setErrorMsg(backendMessage || 'No se pudieron cargar los datos del dashboard de profesor.');
      } finally {
        setIsLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const resumenAlertas = useMemo(() => {
    const mensajes = [];
    if (alertas.pendientesCalificar > 0) {
      mensajes.push(`${alertas.pendientesCalificar} reporte(s) pendientes de calificacion.`);
    }
    if (alertas.solicitudesPendientes > 0) {
      mensajes.push(`${alertas.solicitudesPendientes} solicitud(es) de inscripcion en espera.`);
    }
    if (mensajes.length === 0) {
      mensajes.push('No tienes alertas pendientes por el momento.');
    }
    return mensajes;
  }, [alertas]);

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 'medium' }}>
        Dashboard de Profesor
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

          <Grid size={{ xs: 12, md: 7 }}>
            <Paper sx={{ p: 3, height: '360px', display: 'flex', flexDirection: 'column' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6" gutterBottom>
                  Actividades Proximas
                </Typography>
                <Button size="small" onClick={() => navigate('/modulo/actividades')}>
                  Ver actividades
                </Button>
              </Box>
              <Divider sx={{ mb: 2 }} />

              {upcoming.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No hay actividades proximas por vencer.
                </Typography>
              ) : (
                upcoming.map((item) => (
                  <Box key={item.id} sx={{ mb: 2 }}>
                    <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                      {item.nombre}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Curso {item.cursoNombre} - Vence en {item.diffDias} dia(s)
                    </Typography>
                  </Box>
                ))
              )}
            </Paper>
          </Grid>

          <Grid size={{ xs: 12, md: 5 }}>
            <Paper sx={{ p: 3, height: '360px', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" gutterBottom>
                Alertas
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, flexGrow: 1 }}>
                {resumenAlertas.map((alerta, idx) => (
                  <Typography key={idx} variant="body2" color="text.secondary">
                    {alerta}
                  </Typography>
                ))}
              </Box>
              <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
                <Button size="small" variant="outlined" onClick={() => navigate('/modulo/inscripciones')}>
                  Ver inscripciones
                </Button>
                <Button size="small" variant="outlined" onClick={() => navigate('/modulo/reportes')}>
                  Ver reportes
                </Button>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default DashboardProfesor;
