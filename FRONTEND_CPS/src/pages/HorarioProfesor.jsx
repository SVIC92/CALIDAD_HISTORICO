import { useState, useEffect } from 'react';
import {
    Box, Typography, Button, TextField, MenuItem,
    Paper, CircularProgress, Alert, Card, CardContent
} from '@mui/material';
import { ArrowBack, AccessTime, ClassOutlined } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import ProfesorService from '../services/ProfesorService';
import CursoService from '../services/CursoService';

const diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

// Configuración del calendario
const START_HOUR = 7; // 7 AM
const END_HOUR = 23;  // 11 PM
const PIXELS_PER_MINUTE = 1.3; // Escala: 1 minuto = 1.3 píxeles (78px por hora)

// Genera un arreglo de horas [7, 8, 9, ..., 22]
const hoursList = Array.from({ length: END_HOUR - START_HOUR }, (_, i) => START_HOUR + i);

// Convierte "HH:mm" o "HH:mm:ss" a minutos totales desde las 00:00
const timeToMinutes = (timeStr) => {
    if (!timeStr) return 0;
    const [hours, minutes] = timeStr.split(':').map(Number);
    return (hours * 60) + minutes;
};

const HorarioProfesor = () => {
    const navigate = useNavigate();
    const [profesores, setProfesores] = useState([]);
    const [selectedProfesor, setSelectedProfesor] = useState('');
    const [horarios, setHorarios] = useState([]);
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    // 1. Cargar lista de profesores al montar
    useEffect(() => {
        const fetchProfesores = async () => {
            try {
                const data = await ProfesorService.listar();
                setProfesores(data || []);
            } catch (error) {
                setErrorMsg('Error al cargar la lista de profesores.',error);
            }
        };
        fetchProfesores();
    }, []);

    // 2. Cargar horarios cuando se selecciona un profesor
    useEffect(() => {
        if (!selectedProfesor) {
            setHorarios([]);
            return;
        }

        const fetchHorarios = async () => {
            setLoading(true);
            setErrorMsg('');
            try {
                const data = await CursoService.listarHorariosProfesor(selectedProfesor);
                setHorarios(data || []);
            } catch (error) {
                setErrorMsg('Error al cargar los horarios del profesor.',error);
            } finally {
                setLoading(false);
            }
        };

        fetchHorarios();
    }, [selectedProfesor]);

    // 3. Función para filtrar horarios por día
    const getHorariosPorDia = (dia) => {
        return horarios.filter((h) => h.diaSemana.toLowerCase() === dia.toLowerCase());
    };

    return (
        <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', height: '100%' }}>
            {/* Cabecera */}
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <Button startIcon={<ArrowBack />} onClick={() => navigate('/cursos/listado')} sx={{ mr: 2 }}>
                    Volver
                </Button>
                <Typography variant="h4" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
                    Horario de Profesores
                </Typography>
            </Box>

            {/* Filtro superior */}
            <Paper sx={{ p: 3, mb: 3, borderRadius: 2, boxShadow: 2 }}>
                <Typography variant="body1" sx={{ mb: 2, color: 'text.secondary' }}>
                    Selecciona un profesor para visualizar su carga horaria semanal.
                </Typography>
                <TextField
                    select
                    fullWidth
                    label="Seleccionar Profesor"
                    value={selectedProfesor}
                    onChange={(e) => setSelectedProfesor(e.target.value)}
                    sx={{ maxWidth: 500 }}
                >
                    <MenuItem value="" disabled>-- Seleccione --</MenuItem>
                    {profesores.map((prof) => (
                        <MenuItem key={prof.id} value={prof.id}>
                            {prof.nombre} ({prof.email})
                        </MenuItem>
                    ))}
                </TextField>
            </Paper>

            {errorMsg && <Alert severity="error" sx={{ mb: 3 }}>{errorMsg}</Alert>}

            {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 5 }}>
                    <CircularProgress />
                </Box>
            ) : selectedProfesor && horarios.length === 0 ? (
                <Alert severity="info">Este profesor no tiene horarios asignados actualmente.</Alert>
            ) : selectedProfesor ? (

                // Contenedor principal del calendario con scroll horizontal
                <Paper sx={{ overflowX: 'auto', borderRadius: 2, boxShadow: 3, bgcolor: 'background.paper' }}>
                    <Box sx={{ minWidth: 900 }}> {/* Garantiza que no se aplaste en móviles */}

                        {/* Cabecera de los días */}
                        <Box sx={{ display: 'grid', gridTemplateColumns: '60px repeat(7, 1fr)', borderBottom: 1, borderColor: 'divider' }}>
                            <Box /> {/* Espacio vacío arriba de las horas */}
                            {diasSemana.map((dia) => (
                                <Box key={dia} sx={{ textAlign: 'center', p: 1.5, bgcolor: 'primary.main', color: 'white', borderRight: 1, borderColor: 'primary.dark' }}>
                                    <Typography variant="subtitle2" fontWeight="bold">{dia}</Typography>
                                </Box>
                            ))}
                        </Box>

                        {/* Cuerpo del calendario */}
                        <Box sx={{ display: 'grid', gridTemplateColumns: '60px repeat(7, 1fr)', position: 'relative' }}>

                            {/* Columna de las Horas (Eje Y) */}
                            <Box sx={{ borderRight: 1, borderColor: 'divider', position: 'relative', bgcolor: '#fafafa' }}>
                                {hoursList.map((hour) => (
                                    <Box key={hour} sx={{ height: `${60 * PIXELS_PER_MINUTE}px`, position: 'relative', borderBottom: 1, borderColor: 'divider' }}>
                                        <Typography variant="caption" color="text.secondary" sx={{ position: 'absolute', top: -10, right: 8, bgcolor: '#fafafa', px: 0.5 }}>
                                            {`${hour.toString().padStart(2, '0')}:00`}
                                        </Typography>
                                    </Box>
                                ))}
                            </Box>

                            {/* Columnas de los Días */}
                            {diasSemana.map((dia) => (
                                <Box key={dia} sx={{ position: 'relative', borderRight: 1, borderColor: 'divider' }}>

                                    {/* Líneas horizontales de fondo para guiarse */}
                                    {hoursList.map((hour) => (
                                        <Box key={`bg-${hour}`} sx={{ height: `${60 * PIXELS_PER_MINUTE}px`, borderBottom: 1, borderColor: 'divider', borderBottomStyle: 'dashed' }} />
                                    ))}

                                    {/* Renderizado dinámico de las clases */}
                                    {getHorariosPorDia(dia).map((clase) => {
                                        const startMins = timeToMinutes(clase.horaInicio);
                                        const endMins = timeToMinutes(clase.horaFin);

                                        // Cálculo de posicionamiento absoluto
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
                                                    bgcolor: 'secondary.light',
                                                    color: 'secondary.contrastText',
                                                    overflow: 'hidden',
                                                    boxShadow: 3,
                                                    borderRadius: 2,
                                                    borderLeft: '4px solid',
                                                    borderColor: 'secondary.dark',
                                                    transition: 'transform 0.2s',
                                                    zIndex: 10,
                                                    '&:hover': { transform: 'scale(1.02)', zIndex: 20 }
                                                }}
                                            >
                                                <CardContent sx={{ p: 1, '&:last-child': { pb: 1 } }}>
                                                    <Typography variant="caption" sx={{ fontWeight: 'bold', display: 'block', lineHeight: 1.1, mb: 0.5 }}>
                                                        {clase.curso?.nombre || 'Curso Desconocido'}
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

export default HorarioProfesor;