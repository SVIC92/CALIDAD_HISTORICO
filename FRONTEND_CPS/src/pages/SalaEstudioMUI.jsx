import React, { useEffect, useMemo, useState, useRef, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { JitsiMeeting } from '@jitsi/react-sdk';
import { Client } from '@stomp/stompjs';
import YouTube from 'react-youtube';
import SockJS from 'sockjs-client';

import {
    Box,
    Stack,
    Paper,
    Typography,
    Button,
    TextField,
    Divider,
    Alert,
    Card,
    CardContent,
    Switch,
    FormControlLabel,
    Chip,
    Grid,
    CircularProgress,
    MenuItem,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    IconButton,
    Tooltip
} from '@mui/material';
import { PlayArrow, Pause, Sync, LibraryMusic, VideoCall, Add, People, Public, ExpandMore, KeyboardArrowLeft, KeyboardArrowRight } from '@mui/icons-material';
import api from '../API/axios';
import AuthService from '../services/AuthService';
import UsuarioService from '../services/UsuarioService';
import VideoconferenciaService from '../services/VideoconferenciaService';

const getWsUrl = () => {
    const apiUrl = api.defaults.baseURL;
    return apiUrl.replace('/api', '') + '/ws-chat';
};

const normalizeRol = (rol) => String(rol || '').replace('ROLE_', '').toUpperCase();

export const SalaEstudioMUI = ({ usuarioAutenticado }) => {
    const { salaUuid } = useParams();
    const navigate = useNavigate();

    const [perfil, setPerfil] = useState(usuarioAutenticado || null);
    const [salasPublicas, setSalasPublicas] = useState([]);
    const [usuariosInvitables, setUsuariosInvitables] = useState([]);
    const [isLoadingLobby, setIsLoadingLobby] = useState(false);
    const [isLoadingUsuarios, setIsLoadingUsuarios] = useState(false);
    const [isCreatingRoom, setIsCreatingRoom] = useState(false);
    const [isInvitingUsers, setIsInvitingUsers] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');
    const [successMsg, setSuccessMsg] = useState('');

    const [tituloSala, setTituloSala] = useState('Sala de estudio');
    const [capacidadSala, setCapacidadSala] = useState(12);
    const [esPublica, setEsPublica] = useState(true);
    const [usuariosSeleccionados, setUsuariosSeleccionados] = useState([]);
    const [rolInvitacion, setRolInvitacion] = useState('PARTICIPANTE');
    const [youtubePanelOpen, setYoutubePanelOpen] = useState(true);
    const [invitePanelOpen, setInvitePanelOpen] = useState(false);
    const [panelLateralOpen, setPanelLateralOpen] = useState(true);

    const rolUsuario = useMemo(() => normalizeRol(perfil?.rol), [perfil?.rol]);
    const rolSala = rolUsuario === 'ADMIN' ? 'ADMIN' : 'PARTICIPANTE';

    const [videoId, setVideoId] = useState('4xDzrJKXOOY');
    const [inputVideoId, setInputVideoId] = useState('4xDzrJKXOOY');

    const stompClient = useRef(null);
    const reproductorRef = useRef(null);
    const bloqueoBucle = useRef(false);
    const comandoPendienteRef = useRef(null);

    const aplicarComandoReproductor = useCallback((datos) => {
        if (!reproductorRef.current) return;

        bloqueoBucle.current = true;

        if (datos.accion === 'PLAY') {
            const tiempo = Number(datos.segundosActuales ?? datos.tiempo ?? 0);
            if (!Number.isNaN(tiempo) && tiempo > 0) {
                reproductorRef.current.seekTo(tiempo, true);
            }

            try {
                const resultado = reproductorRef.current.playVideo();
                if (resultado?.catch) {
                    resultado.catch(() => {});
                }
            } catch {
                // Ignoramos interrupciones transitorias del iframe.
            }
        } else if (datos.accion === 'PAUSE') {
            try {
                reproductorRef.current.pauseVideo();
            } catch {
                // Ignoramos interrupciones transitorias del iframe.
            }
        }

        window.setTimeout(() => {
            bloqueoBucle.current = false;
        }, 500);
    }, []);

    useEffect(() => {
        let isMounted = true;

        const cargarPerfil = async () => {
            if (perfil?.id) return;

            try {
                const perfilBackend = await AuthService.usuarioConectado();
                if (isMounted) {
                    setPerfil(perfilBackend);
                }
            } catch {
                if (isMounted) {
                    setErrorMsg('No se pudo cargar el perfil del usuario autenticado.');
                }
            }
        };

        const cargarSalasPublicas = async () => {
            try {
                setIsLoadingLobby(true);
                const salas = await VideoconferenciaService.listarPublicas();
                if (isMounted) {
                    setSalasPublicas(Array.isArray(salas) ? salas : []);
                }
            } catch {
                if (isMounted) {
                    setErrorMsg('No se pudo cargar la lista de salas públicas.');
                }
            } finally {
                if (isMounted) {
                    setIsLoadingLobby(false);
                }
            }
        };

        const cargarUsuariosInvitables = async () => {
            if (rolSala !== 'ADMIN') return;

            try {
                setIsLoadingUsuarios(true);
                const usuarios = await UsuarioService.listar();
                const normalizados = Array.isArray(usuarios) ? usuarios : [];
                setUsuariosInvitables(
                    normalizados.filter((usuario) => usuario.id && usuario.id !== perfil?.id)
                );
            } catch {
                if (isMounted) {
                    setErrorMsg('No se pudo cargar el listado de usuarios para invitar.');
                }
            } finally {
                if (isMounted) {
                    setIsLoadingUsuarios(false);
                }
            }
        };

        cargarPerfil();
        cargarSalasPublicas();
        cargarUsuariosInvitables();

        return () => {
            isMounted = false;
        };
    }, [perfil?.id, rolSala]);

    const alEstarListo = (evento) => {
        reproductorRef.current = evento.target;
        evento.target.setVolume(30);

        if (comandoPendienteRef.current) {
            const comandoPendiente = comandoPendienteRef.current;
            comandoPendienteRef.current = null;
            aplicarComandoReproductor(comandoPendiente);
        }
    };

    const ejecutarComandoSincronizado = useCallback((datos) => {
        if (datos.accion === 'PLAY' && datos.videoId && datos.videoId !== videoId) {
            comandoPendienteRef.current = datos;
            setVideoId(datos.videoId);
            return;
        }

        if (!reproductorRef.current) {
            comandoPendienteRef.current = datos;
            return;
        }

        aplicarComandoReproductor(datos);
    }, [aplicarComandoReproductor, videoId]);

    useEffect(() => {
        if (!salaUuid) return undefined;

        const token = localStorage.getItem('token');
        if (!token) return undefined;

        const client = new Client({
            webSocketFactory: () => new SockJS(getWsUrl()),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5000,
            onConnect: () => {
                client.subscribe(`/topic/sala/${salaUuid}/musica`, (mensaje) => {
                    const datos = JSON.parse(mensaje.body);
                    ejecutarComandoSincronizado(datos);
                });
            },
        });

        client.activate();
        stompClient.current = client;

        return () => {
            client.deactivate();
            stompClient.current = null;
        };
    }, [salaUuid, ejecutarComandoSincronizado]);

    const emitirComando = (accion, nuevoVideoId = null) => {
        if (rolSala !== 'ADMIN') return;

        let segundosActuales = 0;
        if (reproductorRef.current && accion === 'PLAY') {
            segundosActuales = reproductorRef.current.getCurrentTime();
        }

        const idAEnviar = nuevoVideoId || videoId;

        stompClient.current.publish({
            destination: `/app/sala/${salaUuid}/musica`,
            body: JSON.stringify({
                accion,
                videoId: idAEnviar,
                segundosActuales
            })
        });

        if (nuevoVideoId) setVideoId(nuevoVideoId);
    };

    const handleCambiarCancion = () => {
        emitirComando('PLAY', inputVideoId);
    };

    const handleCrearSala = async () => {
        if (!perfil?.id) {
            setErrorMsg('No se pudo identificar el usuario para crear la sala.');
            return;
        }

        try {
            setIsCreatingRoom(true);
            setErrorMsg('');
            const salaCreada = await VideoconferenciaService.crearSala({
                titulo: tituloSala.trim() || 'Sala de estudio',
                capacidad: Number(capacidadSala) || 12,
                esPublica,
                creador: perfil,
            });

            if (salaCreada?.salaUuid) {
                navigate(`/videoconferencia/${salaCreada.salaUuid}`);
            }
        } catch (error) {
            setErrorMsg(error?.response?.data?.error || 'No se pudo crear la sala de videoconferencia.');
        } finally {
            setIsCreatingRoom(false);
        }
    };

    const handleInvitarUsuarios = async () => {
        if (!salaUuid) return;
        if (!usuariosSeleccionados.length) {
            setErrorMsg('Selecciona al menos un usuario para invitar.');
            return;
        }

        try {
            setIsInvitingUsers(true);
            setErrorMsg('');
            setSuccessMsg('');
            await VideoconferenciaService.invitarUsuarios(salaUuid, usuariosSeleccionados, rolInvitacion);
            setSuccessMsg('Invitaciones enviadas correctamente.');
            setUsuariosSeleccionados([]);
        } catch (error) {
            setErrorMsg(error?.response?.data?.error || 'No se pudieron enviar las invitaciones.');
        } finally {
            setIsInvitingUsers(false);
        }
    };

    if (!salaUuid) {
        return (
            <Box sx={{ minHeight: '100vh', width: '100%', maxWidth: '100%', overflowX: 'hidden', bgcolor: '#f4f6f8', p: { xs: 2, md: 4 } }}>
                <Box sx={{ maxWidth: 1280, mx: 'auto' }}>
                    <Stack spacing={1} sx={{ mb: 3 }}>
                        <Typography variant="overline" color="primary.main" sx={{ letterSpacing: 1.8 }}>
                            Videoconferencias
                        </Typography>
                        <Typography variant="h4" fontWeight="bold">
                            Salas de estudio y reuniones
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                            Crea una sala nueva o entra a una pública disponible. El backend ahora expone salas persistidas con UUID propio.
                        </Typography>
                    </Stack>

                    {errorMsg && (
                        <Alert severity="warning" sx={{ mb: 2 }}>
                            {errorMsg}
                        </Alert>
                    )}

                    <Grid container spacing={3}>
                        <Grid size={{ xs: 12, md: 5 }}>
                            <Paper sx={{ p: 3, borderRadius: 3 }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
                                    <Add color="primary" />
                                    <Typography variant="h6" fontWeight="bold">
                                        Crear sala
                                    </Typography>
                                </Box>

                                <Stack spacing={2}>
                                    <TextField
                                        fullWidth
                                        label="Título de la sala"
                                        value={tituloSala}
                                        onChange={(e) => setTituloSala(e.target.value)}
                                    />
                                    <TextField
                                        fullWidth
                                        type="number"
                                        label="Capacidad máxima"
                                        value={capacidadSala}
                                        slotProps={{ htmlInput: { min: 2, max: 500 } }}
                                        onChange={(e) => setCapacidadSala(e.target.value)}
                                    />
                                    <FormControlLabel
                                        control={<Switch checked={esPublica} onChange={(e) => setEsPublica(e.target.checked)} />}
                                        label="Sala pública"
                                    />
                                    <Button
                                        variant="contained"
                                        size="large"
                                        startIcon={<VideoCall />}
                                        onClick={handleCrearSala}
                                        disabled={isCreatingRoom}
                                    >
                                        {isCreatingRoom ? 'Creando sala...' : 'Crear y entrar'}
                                    </Button>
                                </Stack>
                            </Paper>
                        </Grid>

                        <Grid size={{ xs: 12, md: 7 }}>
                            <Paper sx={{ p: 3, borderRadius: 3, minHeight: 360 }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                                        <Public color="primary" />
                                        <Typography variant="h6" fontWeight="bold">
                                            Salas públicas disponibles
                                        </Typography>
                                    </Box>
                                    <Chip label={`${salasPublicas.length} salas`} size="small" />
                                </Box>

                                {isLoadingLobby ? (
                                    <Box sx={{ display: 'grid', placeItems: 'center', minHeight: 220 }}>
                                        <CircularProgress />
                                    </Box>
                                ) : salasPublicas.length > 0 ? (
                                    <Grid container spacing={2}>
                                        {salasPublicas.map((sala) => (
                                            <Grid size={{ xs: 12, sm: 6 }} key={sala.id || sala.salaUuid}>
                                                <Card variant="outlined" sx={{ height: '100%' }}>
                                                    <CardContent>
                                                        <Stack spacing={1.2}>
                                                            <Typography variant="h6" fontWeight="bold">
                                                                {sala.titulo || 'Sala sin título'}
                                                            </Typography>
                                                            <Typography variant="body2" color="text.secondary">
                                                                {sala.salaUuid}
                                                            </Typography>
                                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                                                <Chip size="small" label={`Capacidad: ${sala.capacidadMaxima ?? '-'}`} />
                                                                <Chip size="small" label={sala.esPublica ? 'Pública' : 'Privada'} color={sala.esPublica ? 'success' : 'default'} />
                                                                <Chip size="small" label={sala.modoEstudioFlexible ? 'Flexible' : 'Estándar'} />
                                                            </Stack>
                                                            <Button
                                                                variant="contained"
                                                                onClick={() => navigate(`/videoconferencia/${sala.salaUuid}`)}
                                                                sx={{ alignSelf: 'flex-start', mt: 1 }}
                                                            >
                                                                Entrar
                                                            </Button>
                                                        </Stack>
                                                    </CardContent>
                                                </Card>
                                            </Grid>
                                        ))}
                                    </Grid>
                                ) : (
                                    <Alert severity="info">
                                        No hay salas públicas disponibles todavía.
                                    </Alert>
                                )}
                            </Paper>
                        </Grid>
                    </Grid>
                </Box>
            </Box>
        );
    }

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: { xs: 'column', lg: 'row' },
                width: '100%',
                maxWidth: '100%',
                minWidth: 0,
                height: { xs: 'auto', lg: 'calc(100dvh - 180px)' },
                maxHeight: { xs: 'none', lg: 'calc(100dvh - 180px)' },
                overflow: 'hidden',
                bgcolor: '#f4f6f8'
            }}
        >

            <Box sx={{ flex: '1 1 auto', minWidth: 0, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden', height: '100%' }}>
                <Box sx={{ flex: 1, minWidth: 0, minHeight: 0, overflow: 'hidden', height: '100%' }}>
                    <JitsiMeeting
                        domain="meet.jit.si"
                        roomName={salaUuid}
                        userInfo={{ displayName: perfil?.nombre || usuarioAutenticado?.nombre || 'Usuario' }}
                        configOverwrite={{
                            startWithAudioMuted: true,
                            disableModeratorIndicator: false,
                            lobby: { autoKnock: true, enableLobby: true }
                        }}
                        interfaceConfigOverwrite={{
                            // Ocultamos la barra lateral de Jitsi si queremos usar la nuestra de MUI
                            TOOLBAR_BUTTONS: rolSala === 'ADMIN'
                                ? ['microphone', 'camera', 'desktop', 'chat', 'participants-pane']
                                : ['microphone', 'camera', 'chat']
                        }}
                        getIFrameRef={(iframeRef) => {
                            iframeRef.style.height = '100%';
                            iframeRef.style.width = '100%';
                            iframeRef.style.border = 'none';
                        }}
                    />
                </Box>
            </Box>

            {/* PANEL DERECHO: Controles de MUI y Reproductor (25% aprox - Ancho fijo) */}
            <Paper
                elevation={4}
                square
                sx={{
                    flex: { xs: '0 0 auto', lg: panelLateralOpen ? '0 0 clamp(300px, 30vw, 380px)' : '0 0 64px' },
                    width: { xs: '100%', lg: panelLateralOpen ? 'clamp(300px, 30vw, 380px)' : '64px' },
                    maxWidth: '100%',
                    height: '100%',
                    maxHeight: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    p: 2,
                    zIndex: 10,
                    overflowY: 'auto',
                    overflowX: 'hidden',
                }}
            >
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: panelLateralOpen ? 'space-between' : 'center', mb: panelLateralOpen ? 2 : 0, gap: 1 }}>
                    {panelLateralOpen && (
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <LibraryMusic color="primary" />
                            <Typography variant="h6" fontWeight="bold">
                                Sala {salaUuid}
                            </Typography>
                        </Box>
                    )}

                    <Tooltip title={panelLateralOpen ? 'Contraer menú' : 'Expandir menú'}>
                        <IconButton
                            size="small"
                            onClick={() => setPanelLateralOpen((prev) => !prev)}
                            sx={{
                                alignSelf: 'flex-start',
                                display: { xs: 'none', lg: 'inline-flex' },
                                border: '1px solid',
                                borderColor: 'divider',
                                bgcolor: 'background.paper',
                            }}
                        >
                            {panelLateralOpen ? <KeyboardArrowRight /> : <KeyboardArrowLeft />}
                        </IconButton>
                    </Tooltip>
                </Box>

                {panelLateralOpen && (
                    <>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                            {rolSala === 'ADMIN' ? 'Moderación de videoconferencia y música sincronizada.' : 'Participando en la videoconferencia y escuchando la pista sincronizada.'}
                        </Typography>

                        <Accordion
                            disableGutters
                            elevation={0}
                            expanded={youtubePanelOpen}
                            onChange={(_, expanded) => setYoutubePanelOpen(expanded)}
                            sx={{
                                bgcolor: 'transparent',
                                '&:before': { display: 'none' },
                                borderTop: '1px solid',
                                borderColor: 'divider',
                            }}
                        >
                            <AccordionSummary expandIcon={<ExpandMore />} sx={{ px: 0 }}>
                                <Typography variant="subtitle2" color="text.secondary">
                                    YouTube y música
                                </Typography>
                            </AccordionSummary>
                            <AccordionDetails sx={{ px: 0, pt: 0 }}>
                                <Box sx={{
                                    width: '100%',
                                    borderRadius: 2,
                                    overflow: 'hidden',
                                    mb: 2,
                                    pointerEvents: rolSala === 'ADMIN' ? 'auto' : 'none'
                                }}>
                                    <YouTube
                                        videoId={videoId}
                                        opts={{
                                            width: '100%',
                                            height: '200',
                                            playerVars: { autoplay: 0, controls: rolSala === 'ADMIN' ? 1 : 0, disablekb: 1 }
                                        }}
                                        onReady={alEstarListo}
                                    />
                                </Box>

                                {rolSala === 'ADMIN' ? (
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                        <Typography variant="subtitle2" color="text.secondary">
                                            Controles de Moderador
                                        </Typography>

                                        <Box sx={{ display: 'flex', gap: 1 }}>
                                            <Button
                                                variant="contained"
                                                color="success"
                                                fullWidth
                                                startIcon={<PlayArrow />}
                                                onClick={() => emitirComando('PLAY')}
                                            >
                                                Play a Todos
                                            </Button>
                                            <Button
                                                variant="outlined"
                                                color="error"
                                                fullWidth
                                                startIcon={<Pause />}
                                                onClick={() => emitirComando('PAUSE')}
                                            >
                                                Pausar
                                            </Button>
                                        </Box>

                                        <Divider sx={{ my: 1 }} />

                                        <Typography variant="body2" color="text.secondary">
                                            Cambiar pista de fondo:
                                        </Typography>
                                        <TextField
                                            size="small"
                                            fullWidth
                                            label="ID del video de YouTube"
                                            variant="outlined"
                                            value={inputVideoId}
                                            onChange={(e) => setInputVideoId(e.target.value)}
                                            helperText="Ej: jfKfPfyJRdk"
                                        />
                                        <Button
                                            variant="contained"
                                            color="primary"
                                            startIcon={<Sync />}
                                            onClick={handleCambiarCancion}
                                        >
                                            Sincronizar Nueva Pista
                                        </Button>
                                    </Box>
                                ) : (
                                    <Alert severity="info" sx={{ mt: 2 }}>
                                        La música de estudio está siendo sincronizada por el administrador de la sala.
                                    </Alert>
                                )}
                            </AccordionDetails>
                        </Accordion>

                        <Accordion
                            disableGutters
                            elevation={0}
                            expanded={invitePanelOpen}
                            onChange={(_, expanded) => setInvitePanelOpen(expanded)}
                            sx={{
                                bgcolor: 'transparent',
                                '&:before': { display: 'none' },
                                borderTop: '1px solid',
                                borderColor: 'divider',
                            }}
                        >
                            <AccordionSummary expandIcon={<ExpandMore />} sx={{ px: 0 }}>
                                <Typography variant="subtitle2" color="text.secondary">
                                    Invitar usuarios a la sala
                                </Typography>
                            </AccordionSummary>
                            <AccordionDetails sx={{ px: 0, pt: 0 }}>
                                {rolSala === 'ADMIN' ? (
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                        <TextField
                                            select
                                            size="small"
                                            fullWidth
                                            label="Rol asignado"
                                            value={rolInvitacion}
                                            onChange={(e) => setRolInvitacion(e.target.value)}
                                        >
                                            <MenuItem value="PARTICIPANTE">PARTICIPANTE</MenuItem>
                                            <MenuItem value="PRESENTADOR">PRESENTADOR</MenuItem>
                                            <MenuItem value="ADMIN">ADMIN</MenuItem>
                                        </TextField>

                                        <TextField
                                            select
                                            size="small"
                                            fullWidth
                                            label="Usuarios"
                                            value={usuariosSeleccionados}
                                            onChange={(e) => setUsuariosSeleccionados(typeof e.target.value === 'string' ? e.target.value.split(',') : e.target.value)}
                                            SelectProps={{
                                                multiple: true,
                                                renderValue: (selected) => {
                                                    const etiquetas = usuariosInvitables
                                                        .filter((usuario) => selected.includes(usuario.id))
                                                        .map((usuario) => usuario.nombre || usuario.email);
                                                    return etiquetas.length > 0 ? etiquetas.join(', ') : 'Selecciona usuarios';
                                                },
                                            }}
                                        >
                                            {isLoadingUsuarios ? (
                                                <MenuItem value="">Cargando usuarios...</MenuItem>
                                            ) : (
                                                usuariosInvitables.map((usuario) => (
                                                    <MenuItem key={usuario.id} value={usuario.id}>
                                                        {usuario.nombre || usuario.email} - {normalizeRol(usuario.rol)}
                                                    </MenuItem>
                                                ))
                                            )}
                                        </TextField>

                                        <Button
                                            variant="outlined"
                                            onClick={handleInvitarUsuarios}
                                            disabled={isInvitingUsers || usuariosSeleccionados.length === 0}
                                        >
                                            {isInvitingUsers ? 'Enviando invitaciones...' : 'Invitar seleccionados'}
                                        </Button>
                                    </Box>
                                ) : (
                                    <Alert severity="info">
                                        No tienes permisos para invitar usuarios a esta sala.
                                    </Alert>
                                )}
                            </AccordionDetails>
                        </Accordion>

                        {successMsg && (
                            <Alert severity="success" sx={{ mt: 2 }}>
                                {successMsg}
                            </Alert>
                        )}

                        <Divider sx={{ my: 2 }} />

                        <Alert severity="success" icon={<People />}>
                            {normalizeRol(perfil?.rol) === 'ADMIN' ? 'Tienes permisos para crear salas y moderar audio.' : 'Tu acceso a la sala depende del rol asignado en backend.'}
                        </Alert>
                    </>
                )}
            </Paper>
        </Box>
    );
};

export default SalaEstudioMUI;