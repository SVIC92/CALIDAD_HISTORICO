import { useState, useEffect } from 'react';
import {
  Box, Typography, Paper, CircularProgress, Alert, Card, CardContent,
  Dialog, DialogTitle, DialogContent, DialogActions,
  IconButton, Button, Chip, Stack, Grid, Divider,
} from '@mui/material';
import { alpha, useTheme } from '@mui/material/styles';
import {
  CalendarMonth, AccessTime, ClassOutlined, Close,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../components/PageHeader';
import CursoService from '../services/CursoService';

const diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

const START_HOUR = 7;
const END_HOUR = 23;
const PIXELS_PER_MINUTE = 1.3;
const hoursList = Array.from({ length: END_HOUR - START_HOUR }, (_, i) => START_HOUR + i);

const timeToMinutes = (timeStr) => {
  if (!timeStr) return 0;
  const [hours, minutes] = timeStr.split(':').map(Number);
  return hours * 60 + minutes;
};

const formatHora = (t) => (t ? t.substring(0, 5) : '-');

const formatFecha = (dateStr) => {
  if (!dateStr) return null;
  try {
    return new Date(dateStr).toLocaleDateString('es-PE', { day: 'numeric', month: 'long', year: 'numeric' });
  } catch { return dateStr; }
};

const InfoItem = ({ etiqueta, valor }) => {
  if (valor == null || valor === '' || valor === 0) return null;
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', fontWeight: 600, textTransform: 'uppercase', fontSize: '0.65rem', mb: 0.2 }}>
        {etiqueta}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: 500 }}>
        {valor}
      </Typography>
    </Box>
  );
};

const HorarioAlumno = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const [horarios, setHorarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [claseActiva, setClaseActiva] = useState(null);

  useEffect(() => {
    const fetchMisHorarios = async () => {
      try {
        const data = await CursoService.listarMisHorarios();
        setHorarios(data || []);
      } catch {
        setErrorMsg('Error al cargar tu horario. Verifica que estés matriculado en algún curso.');
      } finally {
        setLoading(false);
      }
    };
    fetchMisHorarios();
  }, []);

  const getHorariosPorDia = (dia) =>
    horarios.filter((h) => h.diaSemana.toLowerCase() === dia.toLowerCase());

  const curso = claseActiva?.curso;

  return (
    <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', height: '100%', color: 'text.primary' }}>
      <PageHeader
        title="Mi Horario de Clases"
        icon={<CalendarMonth />}
        onBack={() => navigate('/dashboard/alumno')}
      />

      {errorMsg && <Alert severity="error" sx={{ mb: 3 }}>{errorMsg}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 5 }}>
          <CircularProgress />
        </Box>
      ) : horarios.length === 0 && !errorMsg ? (
        <Alert severity="info">Aún no tienes cursos asignados. Inscríbete en la sección de Cursos Disponibles.</Alert>
      ) : horarios.length > 0 ? (
        <Paper
          sx={{
            overflowX: 'auto',
            borderRadius: 2,
            boxShadow: isDark ? '0 20px 48px rgba(2, 6, 23, 0.38)' : 3,
            bgcolor: isDark ? alpha(theme.palette.background.paper, 0.9) : 'background.paper',
            border: `1px solid ${isDark ? 'rgba(148, 163, 184, 0.14)' : 'rgba(148, 163, 184, 0.18)'}`,
            backdropFilter: 'blur(18px)',
          }}
        >
          <Box sx={{ minWidth: 900 }}>
            {/* Cabecera días */}
            <Box sx={{ display: 'grid', gridTemplateColumns: '60px repeat(7, 1fr)', borderBottom: 1, borderColor: 'divider' }}>
              <Box />
              {diasSemana.map((dia) => (
                <Box
                  key={dia}
                  sx={{
                    textAlign: 'center', p: 1.5,
                    bgcolor: isDark ? alpha(theme.palette.primary.main, 0.24) : 'primary.main',
                    color: 'white', borderRight: 1,
                    borderColor: isDark ? 'rgba(148, 163, 184, 0.16)' : 'primary.dark',
                  }}
                >
                  <Typography variant="subtitle2" fontWeight="bold">{dia}</Typography>
                </Box>
              ))}
            </Box>

            {/* Cuerpo */}
            <Box sx={{ display: 'grid', gridTemplateColumns: '60px repeat(7, 1fr)', position: 'relative' }}>
              {/* Eje Y horas */}
              <Box sx={{ borderRight: 1, borderColor: 'divider', position: 'relative', bgcolor: isDark ? alpha(theme.palette.background.default, 0.96) : '#fafafa' }}>
                {hoursList.map((hour) => (
                  <Box key={hour} sx={{ height: `${60 * PIXELS_PER_MINUTE}px`, position: 'relative', borderBottom: 1, borderColor: 'divider' }}>
                    <Typography
                      variant="caption" color="text.secondary"
                      sx={{ position: 'absolute', top: -10, right: 8, bgcolor: isDark ? alpha(theme.palette.background.default, 0.96) : '#fafafa', px: 0.5 }}
                    >
                      {`${hour.toString().padStart(2, '0')}:00`}
                    </Typography>
                  </Box>
                ))}
              </Box>

              {/* Columnas días */}
              {diasSemana.map((dia) => (
                <Box key={dia} sx={{ position: 'relative', borderRight: 1, borderColor: 'divider' }}>
                  {hoursList.map((hour) => (
                    <Box
                      key={`bg-${hour}`}
                      sx={{ height: `${60 * PIXELS_PER_MINUTE}px`, borderBottom: 1, borderColor: isDark ? 'rgba(148, 163, 184, 0.14)' : 'divider', borderBottomStyle: 'dashed' }}
                    />
                  ))}

                  {getHorariosPorDia(dia).map((clase) => {
                    const topPos = (timeToMinutes(clase.horaInicio) - START_HOUR * 60) * PIXELS_PER_MINUTE;
                    const cardHeight = (timeToMinutes(clase.horaFin) - timeToMinutes(clase.horaInicio)) * PIXELS_PER_MINUTE;

                    return (
                      <Card
                        key={clase.id}
                        onClick={() => setClaseActiva(clase)}
                        sx={{
                          position: 'absolute',
                          top: `${topPos}px`,
                          height: `${cardHeight}px`,
                          left: 4, right: 4,
                          bgcolor: isDark ? alpha(theme.palette.info.main, 0.18) : 'info.light',
                          color: 'info.contrastText',
                          overflow: 'hidden',
                          boxShadow: isDark ? '0 12px 28px rgba(2, 6, 23, 0.32)' : 3,
                          borderRadius: 2,
                          borderLeft: '4px solid',
                          borderColor: isDark ? alpha(theme.palette.info.main, 0.75) : 'info.dark',
                          cursor: 'pointer',
                          transition: 'transform 0.15s, box-shadow 0.15s',
                          zIndex: 10,
                          '&:hover': { transform: 'scale(1.03)', zIndex: 20, boxShadow: isDark ? '0 18px 36px rgba(2,6,23,0.5)' : 5 },
                        }}
                      >
                        <CardContent sx={{ p: 1, '&:last-child': { pb: 1 } }}>
                          <Typography variant="caption" sx={{ fontWeight: 'bold', display: 'block', lineHeight: 1.1, mb: 0.5 }}>
                            {clase.curso?.nombre || 'Curso'}
                          </Typography>
                          <Box sx={{ display: 'flex', alignItems: 'center', opacity: 0.9, mb: 0.2 }}>
                            <AccessTime sx={{ fontSize: 12, mr: 0.5 }} />
                            <Typography variant="caption" sx={{ fontSize: '0.65rem' }}>
                              {formatHora(clase.horaInicio)} - {formatHora(clase.horaFin)}
                            </Typography>
                          </Box>
                          <Box sx={{ display: 'flex', alignItems: 'center', opacity: 0.9 }}>
                            <ClassOutlined sx={{ fontSize: 12, mr: 0.5 }} />
                            <Typography variant="caption" sx={{ fontSize: '0.65rem' }}>
                              {clase.modalidad}{clase.aula ? ` | ${clase.aula}` : ''}
                            </Typography>
                          </Box>
                        </CardContent>
                      </Card>
                    );
                  })}
                </Box>
              ))}
            </Box>
          </Box>
        </Paper>
      ) : null}

      {/* Modal detalle del curso */}
      <Dialog
        open={Boolean(claseActiva)}
        onClose={() => setClaseActiva(null)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3,
            borderTop: '4px solid',
            borderColor: 'info.main',
          },
        }}
      >
        <DialogTitle sx={{ pb: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 1 }}>
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="h6" fontWeight={700} noWrap>
                {curso?.nombre || 'Detalle del curso'}
              </Typography>
              {curso?.codigoCurso && (
                <Typography variant="body2" color="text.secondary">
                  {curso.codigoCurso}
                </Typography>
              )}
            </Box>
            <IconButton size="small" onClick={() => setClaseActiva(null)} sx={{ flexShrink: 0, mt: -0.5 }}>
              <Close fontSize="small" />
            </IconButton>
          </Box>
        </DialogTitle>

        <DialogContent dividers>
          {/* Chips de estado */}
          <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 2, gap: 0.5 }}>
            {claseActiva?.diaSemana && (
              <Chip label={claseActiva.diaSemana} color="primary" size="small" />
            )}
            {claseActiva?.modalidad && (
              <Chip label={claseActiva.modalidad} color="info" size="small" variant="outlined" />
            )}
            {curso?.estado && (
              <Chip label={curso.estado} color="success" size="small" variant="outlined" />
            )}
          </Stack>

          {/* Info principal de la sesión */}
          <Typography variant="overline" color="text.secondary" sx={{ fontWeight: 700 }}>
            Sesión
          </Typography>
          <Divider sx={{ mb: 1.5 }} />
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid size={{ xs: 6 }}>
              <InfoItem etiqueta="Horario" valor={`${formatHora(claseActiva?.horaInicio)} – ${formatHora(claseActiva?.horaFin)}`} />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <InfoItem etiqueta="Aula / Sala" valor={claseActiva?.aula || 'No asignada'} />
            </Grid>
          </Grid>

          {/* Info del curso */}
          <Typography variant="overline" color="text.secondary" sx={{ fontWeight: 700 }}>
            Curso
          </Typography>
          <Divider sx={{ mb: 1.5 }} />
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid size={{ xs: 6 }}>
              <InfoItem etiqueta="Créditos" valor={curso?.creditos} />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <InfoItem etiqueta="Ciclo" valor={curso?.ciclo} />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <InfoItem etiqueta="Carrera" valor={typeof curso?.carrera === 'object' ? curso?.carrera?.nombre : curso?.carrera} />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <InfoItem etiqueta="Profesor" valor={
                typeof curso?.profesorAsignado === 'object'
                  ? curso?.profesorAsignado?.nombre
                  : curso?.profesorAsignadoNombre || curso?.profesorAsignado
              } />
            </Grid>
            {(curso?.horasTeoricas || curso?.horasPracticas || curso?.horasLaboratorio) && (
              <Grid size={{ xs: 12 }}>
                <InfoItem
                  etiqueta="Carga horaria"
                  valor={[
                    curso?.horasTeoricas ? `${curso.horasTeoricas}h Teóricas` : null,
                    curso?.horasPracticas ? `${curso.horasPracticas}h Prácticas` : null,
                    curso?.horasLaboratorio ? `${curso.horasLaboratorio}h Lab` : null,
                  ].filter(Boolean).join(' · ')}
                />
              </Grid>
            )}
            {(curso?.fechaInicio || curso?.fechaTermino) && (
              <Grid size={{ xs: 12 }}>
                <InfoItem
                  etiqueta="Período"
                  valor={[formatFecha(curso?.fechaInicio), formatFecha(curso?.fechaTermino)].filter(Boolean).join(' → ')}
                />
              </Grid>
            )}
            <Grid size={{ xs: 6 }}>
              <InfoItem etiqueta="Capacidad máx." valor={curso?.capacidadMaxima ? `${curso.capacidadMaxima} estudiantes` : null} />
            </Grid>
          </Grid>

          {/* Descripción */}
          {curso?.descripcion && (
            <>
              <Typography variant="overline" color="text.secondary" sx={{ fontWeight: 700 }}>
                Descripción
              </Typography>
              <Divider sx={{ mb: 1 }} />
              <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                {curso.descripcion}
              </Typography>
            </>
          )}
        </DialogContent>

        <DialogActions>
          <Button onClick={() => setClaseActiva(null)} variant="contained" color="info" size="small">
            Cerrar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default HorarioAlumno;
