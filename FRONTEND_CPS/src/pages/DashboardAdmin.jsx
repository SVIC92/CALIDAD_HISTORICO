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
import PeopleIcon from '@mui/icons-material/People';
import BookIcon from '@mui/icons-material/Book';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import { useNavigate } from 'react-router-dom';
import UsuarioService from '../services/UsuarioService';
import CursoService from '../services/CursoService';
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

const toDate = (value) => {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
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

const formatRelativeMinutes = (date) => {
  if (!date) return 'Fecha no disponible';
  const diffMs = Date.now() - date.getTime();
  const minutes = Math.max(1, Math.floor(diffMs / 60000));
  if (minutes < 60) return `Hace ${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `Hace ${hours} h`;
  const days = Math.floor(hours / 24);
  return `Hace ${days} d`;
};

const DashboardAdmin = () => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [stats, setStats] = useState([
    { title: 'Total Alumnos', value: '0', icon: <PeopleIcon />, color: '#1976d2' },
    { title: 'Cursos Activos', value: '0', icon: <BookIcon />, color: '#2e7d32' },
    { title: 'Inscripciones Hoy', value: '0', icon: <AssignmentTurnedInIcon />, color: '#ed6c02' },
    { title: 'Tasa de Crecimiento', value: '0%', icon: <TrendingUpIcon />, color: '#9c27b0' },
  ]);
  const [actividadReciente, setActividadReciente] = useState([]);

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setIsLoading(true);
        setErrorMsg('');

        const [usuariosResult, cursosActivosResult, inscripcionesResult] = await Promise.allSettled([
          UsuarioService.listar(),
          CursoService.listarActivos(),
          InscripcionService.listaRealizadasProfesor(),
        ]);

        const usuarios = usuariosResult.status === 'fulfilled' && Array.isArray(usuariosResult.value)
          ? usuariosResult.value
          : [];

        const cursosActivos = cursosActivosResult.status === 'fulfilled' && Array.isArray(cursosActivosResult.value)
          ? cursosActivosResult.value
          : [];

        const inscripciones = inscripcionesResult.status === 'fulfilled' && Array.isArray(inscripcionesResult.value)
          ? inscripcionesResult.value
          : [];

        const totalAlumnos = usuarios.filter((u) => String(u?.rol || '').includes('ALUMNO')).length;

        const inscripcionesHoy = inscripciones.filter((i) => isToday(toDate(i?.fechaCreacion))).length;

        const now = new Date();
        const startCurrent = new Date(now.getFullYear(), now.getMonth(), 1);
        const startPrevious = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        const endPrevious = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999);

        const currentMonthUsers = usuarios.filter((u) => {
          const d = toDate(u?.fechaCreacion);
          return d && d >= startCurrent;
        }).length;

        const previousMonthUsers = usuarios.filter((u) => {
          const d = toDate(u?.fechaCreacion);
          return d && d >= startPrevious && d <= endPrevious;
        }).length;

        let crecimiento = '0%';
        if (previousMonthUsers > 0) {
          const rate = ((currentMonthUsers - previousMonthUsers) / previousMonthUsers) * 100;
          const sign = rate > 0 ? '+' : '';
          crecimiento = `${sign}${Math.round(rate)}%`;
        } else if (currentMonthUsers > 0) {
          crecimiento = '+100%';
        }

        const actividad = inscripciones
          .map((i) => ({
            id: i?.id || Math.random().toString(36),
            titulo: `${i?.usuario?.nombre || 'Usuario'} ${String(i?.estado || '').toUpperCase() === 'APROBADO' ? 'inscrito' : 'registrado'} en curso`,
            detalle: `${formatRelativeMinutes(toDate(i?.fechaCreacion))} - Curso: ${i?.curso?.nombre || '-'}`,
            fecha: toDate(i?.fechaCreacion),
          }))
          .sort((a, b) => (b.fecha?.getTime?.() || 0) - (a.fecha?.getTime?.() || 0))
          .slice(0, 6);

        setActividadReciente(actividad);
        setStats([
          { title: 'Total Alumnos', value: String(totalAlumnos), icon: <PeopleIcon />, color: '#1976d2' },
          { title: 'Cursos Activos', value: String(cursosActivos.length), icon: <BookIcon />, color: '#2e7d32' },
          { title: 'Inscripciones Hoy', value: String(inscripcionesHoy), icon: <AssignmentTurnedInIcon />, color: '#ed6c02' },
          { title: 'Tasa de Crecimiento', value: crecimiento, icon: <TrendingUpIcon />, color: '#9c27b0' },
        ]);

        if (usuariosResult.status === 'rejected' || cursosActivosResult.status === 'rejected' || inscripcionesResult.status === 'rejected') {
          setErrorMsg('Algunos indicadores no pudieron cargarse por cambios o errores en backend.');
        }
      } catch (error) {
        const backendMessage = error?.response?.data?.error || error?.response?.data;
        setErrorMsg(backendMessage || 'No se pudo cargar el dashboard de administración.');
      } finally {
        setIsLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const resumenMensual = useMemo(() => {
    return [
      { mes: 'Cursos activos', valor: stats[1]?.value || '0' },
      { mes: 'Inscripciones hoy', valor: stats[2]?.value || '0' },
      { mes: 'Crecimiento', valor: stats[3]?.value || '0%' },
    ];
  }, [stats]);

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 'medium' }}>
        Panel de Administración
      </Typography>

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
          {stats.map((stat, index) => (
            <Grid size={{ xs: 12, sm: 6, md: 3 }} key={index}>
              <KpiCard {...stat} />
            </Grid>
          ))}

          <Grid size={{ xs: 12, md: 8 }}>
            <Paper sx={{ p: 3, height: '400px', display: 'flex', flexDirection: 'column' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6" gutterBottom>
                  Resumen Operativo
                </Typography>
                <Button size="small" onClick={() => navigate('/usuarios/listado')}>
                  Ver usuarios
                </Button>
              </Box>
              <Divider sx={{ mb: 2 }} />
              <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: 'repeat(1, minmax(0, 1fr))' }}>
                {resumenMensual.map((item) => (
                  <Box
                    key={item.mes}
                    sx={{
                      p: 2,
                      borderRadius: 1,
                      bgcolor: 'action.hover',
                      border: (theme) => `1px solid ${theme.palette.divider}`,
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                      {item.mes}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {item.valor}
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Paper>
          </Grid>

          <Grid size={{ xs: 12, md: 4 }}>
            <Paper sx={{ p: 3, height: '400px', overflowY: 'auto' }}>
              <Typography variant="h6" gutterBottom>
                Actividad Reciente
              </Typography>
              <Divider sx={{ mb: 2 }} />
              {actividadReciente.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No hay actividad reciente para mostrar.
                </Typography>
              ) : (
                actividadReciente.map((item) => (
                  <Box key={item.id} sx={{ mb: 2 }}>
                    <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                      {item.titulo}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {item.detalle}
                    </Typography>
                    <Divider sx={{ mt: 1 }} />
                  </Box>
                ))
              )}
            </Paper>
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default DashboardAdmin;