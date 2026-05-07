import { useState, useEffect } from 'react';
import {
    Box, Typography, Button, Paper, CircularProgress, Alert, Card, CardContent
} from '@mui/material';
import { alpha, useTheme } from '@mui/material/styles';
import { ArrowBack, AccessTime, ClassOutlined } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import CursoService from '../services/CursoService';

const diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

// Configuración del calendario
const START_HOUR = 7;
const END_HOUR = 23;
const PIXELS_PER_MINUTE = 1.3;
const hoursList = Array.from({ length: END_HOUR - START_HOUR }, (_, i) => START_HOUR + i);

const timeToMinutes = (timeStr) => {
    if (!timeStr) return 0;
    const [hours, minutes] = timeStr.split(':').map(Number);
    return (hours * 60) + minutes;
};

const HorarioAlumno = () => {
    const navigate = useNavigate();
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const [horarios, setHorarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState('');

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

    const getHorariosPorDia = (dia) => {
        return horarios.filter((h) => h.diaSemana.toLowerCase() === dia.toLowerCase());
    };

    return (
        <Box
            sx={{
                flexGrow: 1,
                display: 'flex',
                flexDirection: 'column',
                height: '100%',
                color: 'text.primary',
            }}
        >
            {/* Cabecera */}
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <Button startIcon={<ArrowBack />} onClick={() => navigate('/dashboard/alumno')} sx={{ mr: 2 }}>
                    Volver
                </Button>
                <Typography variant="h4" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
                    Mi Horario de Clases
                </Typography>
            </Box>

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

                        <Box sx={{ display: 'grid', gridTemplateColumns: '60px repeat(7, 1fr)', borderBottom: 1, borderColor: 'divider' }}>
                            <Box />
                            {diasSemana.map((dia) => (
                                <Box
                                    key={dia}
                                    sx={{
                                        textAlign: 'center',
                                        p: 1.5,
                                        bgcolor: isDark ? alpha(theme.palette.primary.main, 0.24) : 'primary.main',
                                        color: 'white',
                                        borderRight: 1,
                                        borderColor: isDark ? 'rgba(148, 163, 184, 0.16)' : 'primary.dark',
                                    }}
                                >
                                    <Typography variant="subtitle2" fontWeight="bold">{dia}</Typography>
                                </Box>
                            ))}
                        </Box>

                        <Box sx={{ display: 'grid', gridTemplateColumns: '60px repeat(7, 1fr)', position: 'relative' }}>

                            <Box
                                sx={{
                                    borderRight: 1,
                                    borderColor: 'divider',
                                    position: 'relative',
                                    bgcolor: isDark ? alpha(theme.palette.background.default, 0.96) : '#fafafa',
                                }}
                            >
                                {hoursList.map((hour) => (
                                    <Box key={hour} sx={{ height: `${60 * PIXELS_PER_MINUTE}px`, position: 'relative', borderBottom: 1, borderColor: 'divider' }}>
                                        <Typography
                                            variant="caption"
                                            color="text.secondary"
                                            sx={{
                                                position: 'absolute',
                                                top: -10,
                                                right: 8,
                                                bgcolor: isDark ? alpha(theme.palette.background.default, 0.96) : '#fafafa',
                                                px: 0.5,
                                            }}
                                        >
                                            {`${hour.toString().padStart(2, '0')}:00`}
                                        </Typography>
                                    </Box>
                                ))}
                            </Box>

                            {diasSemana.map((dia) => (
                                <Box key={dia} sx={{ position: 'relative', borderRight: 1, borderColor: 'divider' }}>
                                    {hoursList.map((hour) => (
                                        <Box
                                            key={`bg-${hour}`}
                                            sx={{
                                                height: `${60 * PIXELS_PER_MINUTE}px`,
                                                borderBottom: 1,
                                                borderColor: isDark ? 'rgba(148, 163, 184, 0.14)' : 'divider',
                                                borderBottomStyle: 'dashed',
                                            }}
                                        />
                                    ))}

                                    {getHorariosPorDia(dia).map((clase) => {
                                        const startMins = timeToMinutes(clase.horaInicio);
                                        const endMins = timeToMinutes(clase.horaFin);

                                        const topPos = (startMins - (START_HOUR * 60)) * PIXELS_PER_MINUTE;
                                        const cardHeight = (endMins - startMins) * PIXELS_PER_MINUTE;

                                        return (
                                            <Card
                                                key={clase.id}
                                                sx={{
                                                    position: 'absolute',
                                                    top: `${topPos}px`,
                                                    height: `${cardHeight}px`,
                                                    left: 4,
                                                    right: 4,
                                                    bgcolor: isDark ? alpha(theme.palette.info.main, 0.18) : 'info.light',
                                                    color: 'info.contrastText',
                                                    overflow: 'hidden',
                                                    boxShadow: isDark ? '0 12px 28px rgba(2, 6, 23, 0.32)' : 3,
                                                    borderRadius: 2,
                                                    borderLeft: '4px solid',
                                                    borderColor: isDark ? alpha(theme.palette.info.main, 0.75) : 'info.dark',
                                                    transition: 'transform 0.2s',
                                                    zIndex: 10,
                                                    '&:hover': { transform: 'scale(1.02)', zIndex: 20 }
                                                }}
                                            >
                                                <CardContent sx={{ p: 1, '&:last-child': { pb: 1 } }}>
                                                    <Typography variant="caption" sx={{ fontWeight: 'bold', display: 'block', lineHeight: 1.1, mb: 0.5 }}>
                                                        {clase.curso?.nombre || 'Curso'}
                                                    </Typography>

                                                    <Box sx={{ display: 'flex', alignItems: 'center', opacity: 0.9, mb: 0.2 }}>
                                                        <AccessTime sx={{ fontSize: 12, mr: 0.5 }} />
                                                        <Typography variant="caption" sx={{ fontSize: '0.65rem' }}>
                                                            {clase.horaInicio.substring(0, 5)} - {clase.horaFin.substring(0, 5)}
                                                        </Typography>
                                                    </Box>

                                                    <Box sx={{ display: 'flex', alignItems: 'center', opacity: 0.9 }}>
                                                        <ClassOutlined sx={{ fontSize: 12, mr: 0.5 }} />
                                                        <Typography variant="caption" sx={{ fontSize: '0.65rem' }}>
                                                            {clase.modalidad} {clase.aula ? `| ${clase.aula}` : ''}
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
        </Box>
    );
};

export default HorarioAlumno;