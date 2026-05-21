import React, { useEffect, useMemo, useState, useRef, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import YouTube from 'react-youtube';
import SockJS from 'sockjs-client';
import { alpha, useTheme } from '@mui/material/styles';

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
    FormControl,
    InputLabel,
    Chip,
    Grid,
    CircularProgress,
    MenuItem,
    Select,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    IconButton,
    Tooltip,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Popover,
    List,
    ListItem,
    ListItemText,
    ListItemAvatar,
    Avatar
} from '@mui/material';
import { PlayArrow, Pause, Sync, LibraryMusic, VideoCall, Add, People, Public, ExpandMore, KeyboardArrowLeft, KeyboardArrowRight } from '@mui/icons-material';
import api from '../API/axios';
import AuthService from '../services/AuthService';
import UsuarioService from '../services/UsuarioService';
import VideoconferenciaService from '../services/VideoconferenciaService';
import SubtitulosVideoconferencia from '../components/SubtitulosVideoconferencia';

const getWsUrl = () => {
    const apiUrl = api.defaults.baseURL;
    return apiUrl.replace('/api', '') + '/ws-chat';
};

const normalizeRol = (rol) => String(rol || '').replace('ROLE_', '').toUpperCase();

const formatearRolSalaVisible = (rol) => {
    switch (normalizeRol(rol)) {
        case 'ADMIN':
            return 'Administrador';
        case 'PRESENTADOR':
            return 'Presentador';
        case 'PARTICIPANTE':
        default:
            return 'Participante';
    }
};

const normalizarParticipanteSala = (participante, perfilActualId) => {
    const usuarioId = participante?.usuarioId || participante?.id || '';

    return {
        id: usuarioId,
        nombre: participante?.nombreUsuario || participante?.nombre || participante?.displayName || participante?.email || 'Usuario',
        email: participante?.email || '',
        rol: normalizeRol(participante?.rolSala || participante?.rol || (usuarioId && usuarioId === perfilActualId ? 'ADMIN' : 'PARTICIPANTE')),
        invitado: Boolean(participante?.invitado),
        dentroDeSala: Boolean(participante?.dentroDeSala),
    };
};

const obtenerIdParticipanteJitsi = (participante) => {
    if (!participante) return '';

    return String(
        participante.participantId
        || participante.id
        || participante.userId
        || participante.email
        || participante.displayName
        || participante.name
        || ''
    );
};

const normalizarParticipanteJitsi = (participante) => {
    const id = obtenerIdParticipanteJitsi(participante);
    if (!id) return null;

    return {
        id,
        nombre: participante?.displayName || participante?.name || 'Usuario',
        email: participante?.email || '',
        rol: 'PARTICIPANTE',
        invitado: false,
        dentroDeSala: true,
    };
};

const obtenerToolbarPorRolSala = (rolSala) => {
    switch (normalizeRol(rolSala)) {
        case 'ADMIN':
            return undefined;
        case 'PRESENTADOR':
            return [
                'microphone',
                'camera',
                'desktop',
                'chat',
                'hangup',
            ];
        default:
            return [
                'chat',
                'hangup',
            ];
    }
};

const obtenerConfigJitsiPorRolSala = (rolSala) => {
    const rolNormalizado = normalizeRol(rolSala);

    if (rolNormalizado === 'ADMIN') {
        return {
            startWithAudioMuted: false,
            startWithVideoMuted: false,
            prejoinPageEnabled: false,
        };
    }

    if (rolNormalizado === 'PRESENTADOR') {
        return {
            startWithAudioMuted: false,
            startWithVideoMuted: false,
            prejoinPageEnabled: false,
            remoteVideoMenu: {
                disableKick: true,
                disableMute: true,
                disableGrantModerator: true,
            },
            disableRemoteMute: true,
        };
    }

    return {
        startWithAudioMuted: true,
        startWithVideoMuted: true,
        prejoinPageEnabled: false,
        remoteVideoMenu: {
            disableKick: true,
            disableMute: true,
            disableGrantModerator: true,
        },
        disableRemoteMute: true,
        disableTileView: false,
    };
};

const JITSI_EXTERNAL_API_URL = 'https://meet.jit.si/external_api.js';
let jitsiExternalApiLoader = null;
const EMPTY_CONNECTED_USERS = [];

const loadJitsiExternalApi = () => {
    if (typeof window === 'undefined') {
        return Promise.reject(new Error('Jitsi solo puede cargarse en el navegador.'));
    }

    if (window.JitsiMeetExternalAPI) {
        return Promise.resolve(window.JitsiMeetExternalAPI);
    }

    if (!jitsiExternalApiLoader) {
        jitsiExternalApiLoader = new Promise((resolve, reject) => {
            const existingScript = document.querySelector(`script[src="${JITSI_EXTERNAL_API_URL}"]`);
            if (existingScript && window.JitsiMeetExternalAPI) {
                resolve(window.JitsiMeetExternalAPI);
                return;
            }

            const script = document.createElement('script');
            script.src = JITSI_EXTERNAL_API_URL;
            script.async = true;
            script.onload = () => resolve(window.JitsiMeetExternalAPI);
            script.onerror = () => reject(new Error('No se pudo cargar el script de Jitsi.'));
            document.head.appendChild(script);
        });
    }

    return jitsiExternalApiLoader;
};

export const SalaEstudioMUI = ({ usuarioAutenticado, listaUsuariosConectados }) => {
    const { salaUuid } = useParams();
    const navigate = useNavigate();
    const theme = useTheme();
    const usuariosConectadosExternos = listaUsuariosConectados ?? EMPTY_CONNECTED_USERS;

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
    const [enSalaDeEspera, setEnSalaDeEspera] = useState(false);
    const [accesoRechazado, setAccesoRechazado] = useState(false);
    const [solicitudesPendientes, setSolicitudesPendientes] = useState([]);
    const [openDialogSolicitud, setOpenDialogSolicitud] = useState(false);
    const [rolesSeleccionados, setRolesSeleccionados] = useState({});
    const [rosterAnchorEl, setRosterAnchorEl] = useState(null);
    const [miRolEnSala, setMiRolEnSala] = useState('PARTICIPANTE');
    const [rolesUsuarios, setRolesUsuarios] = useState({});
    const [participantesSala, setParticipantesSala] = useState([]);
    const [participantesJitsi, setParticipantesJitsi] = useState([]);

    const openRoster = Boolean(rosterAnchorEl);
    const handleOpenRoster = (event) => {
        setRosterAnchorEl(event.currentTarget);
    };
    const handleCloseRoster = () => {
        setRosterAnchorEl(null);
    };

    const rolUsuario = useMemo(() => normalizeRol(perfil?.rol), [perfil?.rol]);
    const rolSala = rolUsuario === 'ADMIN' ? 'ADMIN' : 'PARTICIPANTE';
    const rolSalaEnVivo = useMemo(() => normalizeRol(miRolEnSala), [miRolEnSala]);
    const esModerador = rolSalaEnVivo === 'ADMIN';

    const [videoId, setVideoId] = useState('4xDzrJKXOOY');
    const [inputVideoId, setInputVideoId] = useState('4xDzrJKXOOY');

    const stompClient = useRef(null);
    const reproductorRef = useRef(null);
    const bloqueoBucle = useRef(false);
    const comandoPendienteRef = useRef(null);
    const jitsiContainerRef = useRef(null);
    const jitsiApiRef = useRef(null);

    const limpiarJitsi = useCallback(() => {
        if (jitsiApiRef.current) {
            try {
                jitsiApiRef.current.destroy();
            } catch {
                // Ignoramos errores de destrucción al salir de la sala.
            }
            jitsiApiRef.current = null;
        }

        if (jitsiContainerRef.current) {
            jitsiContainerRef.current.innerHTML = '';
        }

        document.body.style.removeProperty('overflow');
        document.documentElement.style.removeProperty('overflow');

        document.querySelectorAll('iframe[src*="meet.jit.si"], iframe[src*="jitsi"]').forEach((node) => node.remove());
    }, []);

    useEffect(() => {
        setMiRolEnSala((rolActual) => {
            if (rolActual === 'ADMIN' || rolActual === 'PRESENTADOR') {
                return rolActual;
            }

            return rolSala;
        });
    }, [rolSala]);

    useEffect(() => {
        setRolesUsuarios((prev) => {
            const siguiente = { ...prev };
            let huboCambios = false;

            usuariosConectadosExternos.forEach((usuario) => {
                if (!usuario?.id) return;

                if (!siguiente[usuario.id]) {
                    siguiente[usuario.id] = usuario.id === perfil?.id ? miRolEnSala : 'PARTICIPANTE';
                    huboCambios = true;
                }
            });

            if (!huboCambios) {
                return prev;
            }

            return siguiente;
        });
    }, [usuariosConectadosExternos, perfil?.id, miRolEnSala]);

    useEffect(() => {
        if (!perfil?.id || participantesSala.length === 0) return;

        const participanteActual = participantesSala.find((participante) => {
            const idParticipante = String(participante?.usuarioId || participante?.id || '').trim();
            const emailParticipante = String(participante?.email || '').trim().toLowerCase();
            const idPerfil = String(perfil?.id || '').trim();
            const emailPerfil = String(perfil?.email || '').trim().toLowerCase();

            return (idParticipante && idParticipante === idPerfil)
                || (emailParticipante && emailParticipante === emailPerfil);
        });

        if (!participanteActual?.rolSala) return;

        const rolNormalizado = normalizeRol(participanteActual.rolSala);
        setMiRolEnSala((rolAnterior) => (rolAnterior === rolNormalizado ? rolAnterior : rolNormalizado));
    }, [participantesSala, perfil?.id, perfil?.email]);

    useEffect(() => {
        if (!salaUuid || !perfil?.id) return;

        setEnSalaDeEspera(rolUsuario !== 'ADMIN');
        setAccesoRechazado(false);
        setSolicitudesPendientes([]);
        setOpenDialogSolicitud(false);
        setRolesUsuarios({});
        setParticipantesSala([]);
        setParticipantesJitsi([]);
    }, [salaUuid, perfil?.id]);

    const cargarParticipantesSala = useCallback(async () => {
        if (!salaUuid) return;

        try {
            const participantes = await VideoconferenciaService.listarParticipantes(salaUuid);
            setParticipantesSala(Array.isArray(participantes) ? participantes : []);
        } catch (error) {
            console.error('No se pudo cargar el roster de participantes de la sala:', error);
            setParticipantesSala([]);
        }
    }, [salaUuid]);

    useEffect(() => {
        cargarParticipantesSala();
    }, [cargarParticipantesSala]);

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
                    resultado.catch(() => { });
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

    const ejecutarCambioRol = useCallback((usuarioId, nuevoRol) => {
        if (!salaUuid || !stompClient.current?.connected || !esModerador) return;

        stompClient.current.publish({
            destination: `/app/sala/${salaUuid}/cambiar-rol-en-vivo`,
            body: JSON.stringify({
                salaUuid,
                usuarioId,
                nuevoRol,
            })
        });
    }, [esModerador, salaUuid]);

    const manejarRespuestaSolicitud = useCallback((solicitud, aprobado) => {
        if (!salaUuid || !stompClient.current?.connected) return;

        const rolAsignado = rolesSeleccionados[solicitud.usuarioId] || 'PARTICIPANTE';

        stompClient.current.publish({
            destination: `/app/sala/${salaUuid}/responder-acceso`,
            body: JSON.stringify({
                ...solicitud,
                aprobado,
                rolAsignado
            })
        });

        setSolicitudesPendientes((prev) => {
            const restantes = prev.filter((item) => item.usuarioId !== solicitud.usuarioId);
            if (restantes.length === 0) {
                setOpenDialogSolicitud(false);
            }
            return restantes;
        });
    }, [salaUuid, rolesSeleccionados]);

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

                client.subscribe(`/topic/sala/${salaUuid}/participantes`, (mensaje) => {
                    try {
                        const participantes = JSON.parse(mensaje.body);
                        setParticipantesSala(Array.isArray(participantes) ? participantes : []);
                    } catch (error) {
                        console.error('No se pudo procesar el roster de participantes:', error);
                    }
                });

                if (esModerador) {
                    client.subscribe(`/topic/sala/${salaUuid}/lobby-admin`, (mensaje) => {
                        try {
                            const nuevaSolicitud = JSON.parse(mensaje.body);
                            setSolicitudesPendientes((prev) => [...prev, nuevaSolicitud]);
                            setOpenDialogSolicitud(true);
                        } catch (error) {
                            console.error('No se pudo procesar la solicitud de acceso:', error);
                        }
                    });
                } else if (enSalaDeEspera && perfil?.id) {
                    client.subscribe(`/topic/sala/${salaUuid}/espera/${perfil.id}`, (mensaje) => {
                        try {
                            const respuesta = JSON.parse(mensaje.body);
                            if (respuesta.aprobado) {
                                setAccesoRechazado(false);
                                setErrorMsg('');
                                setEnSalaDeEspera(false);
                            } else {
                                setErrorMsg('Tu solicitud de acceso ha sido rechazada por el moderador.');
                                setAccesoRechazado(true);
                                setEnSalaDeEspera(false);
                            }
                        } catch (error) {
                            console.error('No se pudo procesar la respuesta de acceso:', error);
                        }
                    });

                    client.publish({
                        destination: `/app/sala/${salaUuid}/solicitar-acceso`,
                        body: JSON.stringify({
                            salaUuid,
                            usuarioId: perfil.id,
                            nombreUsuario: perfil.nombre || usuarioAutenticado?.nombre || 'Usuario'
                        })
                    });
                }

                client.subscribe(`/topic/sala/${salaUuid}/roles-en-vivo`, (mensaje) => {
                    try {
                        const cambio = JSON.parse(mensaje.body);
                        if (!cambio?.usuarioId || !cambio?.nuevoRol) return;

                        setRolesUsuarios((prev) => ({
                            ...prev,
                            [cambio.usuarioId]: cambio.nuevoRol,
                        }));

                        if (cambio.usuarioId === perfil?.id) {
                            setMiRolEnSala(cambio.nuevoRol);
                        }
                    } catch (error) {
                        console.error('No se pudo procesar el cambio de rol en vivo:', error);
                    }
                });
            },
        });

        client.activate();
        stompClient.current = client;

        return () => {
            client.deactivate();
            stompClient.current = null;
        };
    }, [salaUuid, ejecutarComandoSincronizado, esModerador, enSalaDeEspera, perfil?.id, perfil?.nombre, usuarioAutenticado?.nombre]);

    useEffect(() => {
        if (enSalaDeEspera) {
            limpiarJitsi();
            return undefined;
        }

        if (!salaUuid || !perfil?.nombre || !jitsiContainerRef.current) return undefined;

        let isMounted = true;

        const inicializarJitsi = async () => {
            try {
                const JitsiExternalAPI = await loadJitsiExternalApi();
                if (!isMounted || !jitsiContainerRef.current) return;

                if (jitsiApiRef.current) {
                    jitsiApiRef.current.destroy();
                    jitsiApiRef.current = null;
                }

                jitsiContainerRef.current.innerHTML = '';

                const opcionesConfig = obtenerConfigJitsiPorRolSala(rolSalaEnVivo);

                const toolbarButtons = obtenerToolbarPorRolSala(rolSalaEnVivo);
                const opcionesInterfaz = toolbarButtons
                    ? { TOOLBAR_BUTTONS: toolbarButtons }
                    : {};

                const apiJitsi = new JitsiExternalAPI('meet.jit.si', {
                    roomName: salaUuid,
                    width: '100%',
                    height: '100%',
                    parentNode: jitsiContainerRef.current,
                    userInfo: {
                        displayName: perfil?.nombre || usuarioAutenticado?.nombre || 'Usuario'
                    },
                    configOverwrite: opcionesConfig,
                    interfaceConfigOverwrite: opcionesInterfaz,
                });

                apiJitsi.addEventListener('videoConferenceJoined', () => {
                    setParticipantesJitsi((prev) => {
                        const selfParticipant = normalizarParticipanteJitsi({
                            participantId: perfil?.id || perfil?.email || perfil?.nombre,
                            displayName: perfil?.nombre || usuarioAutenticado?.nombre || 'Usuario',
                            email: perfil?.email || usuarioAutenticado?.email || '',
                        });

                        if (!selfParticipant) return prev;

                        if (prev.some((item) => item.id === selfParticipant.id)) {
                            return prev;
                        }

                        return [...prev, selfParticipant];
                    });

                    if (esModerador) {
                        try {
                            apiJitsi.executeCommand('toggleLobby', true);
                        } catch {
                            // Si la versión de Jitsi no soporta el comando, seguimos sin bloquear la sala.
                        }
                    }
                });

                apiJitsi.addEventListener('participantJoined', (evento) => {
                    const participante = normalizarParticipanteJitsi(evento?.participant || evento);
                    if (!participante) return;

                    setParticipantesJitsi((prev) => {
                        if (prev.some((item) => item.id === participante.id)) {
                            return prev;
                        }

                        return [...prev, participante];
                    });
                });

                apiJitsi.addEventListener('participantLeft', (evento) => {
                    const participanteId = obtenerIdParticipanteJitsi(evento?.participant || evento);
                    if (!participanteId) return;

                    setParticipantesJitsi((prev) => prev.filter((item) => item.id !== participanteId));
                });

                apiJitsi.addEventListener('videoConferenceLeft', () => {
                    setParticipantesJitsi([]);
                });

                jitsiApiRef.current = apiJitsi;
            } catch (error) {
                if (isMounted) {
                    setErrorMsg('No se pudo cargar la videoconferencia de Jitsi.');
                    console.error(error);
                }
            }
        };

        inicializarJitsi();

        return () => {
            isMounted = false;
            limpiarJitsi();
            setParticipantesJitsi([]);
        };
    }, [enSalaDeEspera, salaUuid, perfil?.nombre, usuarioAutenticado?.nombre, rolSalaEnVivo, esModerador, limpiarJitsi]);

    useEffect(() => {
        const handleLeaveVideoconference = () => {
            limpiarJitsi();
        };

        window.addEventListener('app:leave-videoconference', handleLeaveVideoconference);

        return () => {
            window.removeEventListener('app:leave-videoconference', handleLeaveVideoconference);
        };
    }, [limpiarJitsi]);

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

    const participantesConectados = useMemo(() => {
        if (participantesJitsi.length === 0) {
            return [];
        }

        const perfilIdNormalizado = String(perfil?.id || '').trim().toLowerCase();
        const perfilEmailNormalizado = String(perfil?.email || '').trim().toLowerCase();
        const perfilNombreNormalizado = String(perfil?.nombre || usuarioAutenticado?.nombre || '').trim().toLowerCase();

        const participantePersistidoDelPerfil = participantesSala.find((participante) => {
            const idPersistido = String(participante?.usuarioId || participante?.id || '').trim().toLowerCase();
            const emailPersistido = String(participante?.email || '').trim().toLowerCase();
            const nombrePersistido = String(participante?.nombreUsuario || participante?.nombre || '').trim().toLowerCase();

            return (perfilIdNormalizado && perfilIdNormalizado === idPersistido)
                || (perfilEmailNormalizado && perfilEmailNormalizado === emailPersistido)
                || (perfilNombreNormalizado && perfilNombreNormalizado === nombrePersistido)
                || (perfilNombreNormalizado && perfilNombreNormalizado === emailPersistido);
        });

        return participantesJitsi
            .map((participanteJitsi) => {
                const candidatoId = String(participanteJitsi?.id || '').trim().toLowerCase();
                const candidatoEmail = String(participanteJitsi?.email || '').trim().toLowerCase();
                const candidatoNombre = String(participanteJitsi?.nombre || '').trim().toLowerCase();

                const coincideConPerfilActual = Boolean(
                    (perfilIdNormalizado && (candidatoId === perfilIdNormalizado || candidatoEmail === perfilIdNormalizado || candidatoNombre === perfilIdNormalizado))
                    || (perfilEmailNormalizado && (candidatoId === perfilEmailNormalizado || candidatoEmail === perfilEmailNormalizado || candidatoNombre === perfilEmailNormalizado))
                    || (perfilNombreNormalizado && (candidatoId === perfilNombreNormalizado || candidatoEmail === perfilNombreNormalizado || candidatoNombre === perfilNombreNormalizado))
                );

                const participantePersistido = participantesSala.find((participante) => {
                    const idPersistido = String(participante?.usuarioId || participante?.id || '').trim().toLowerCase();
                    const emailPersistido = String(participante?.email || '').trim().toLowerCase();
                    const nombrePersistido = String(participante?.nombreUsuario || participante?.nombre || '').trim().toLowerCase();

                    return (candidatoId && (candidatoId === idPersistido || candidatoId === emailPersistido || candidatoId === nombrePersistido))
                        || (candidatoEmail && (candidatoEmail === idPersistido || candidatoEmail === emailPersistido || candidatoEmail === nombrePersistido))
                        || (candidatoNombre && (candidatoNombre === idPersistido || candidatoNombre === emailPersistido || candidatoNombre === nombrePersistido));
                });

                if (coincideConPerfilActual && participantePersistidoDelPerfil) {
                    return normalizarParticipanteSala(participantePersistidoDelPerfil, perfil?.id);
                }

                if (participantePersistido) {
                    return normalizarParticipanteSala(participantePersistido, perfil?.id);
                }

                return normalizarParticipanteSala(participanteJitsi, perfil?.id);
            })
            .filter((participante) => Boolean(participante?.id));
    }, [participantesSala, participantesJitsi, perfil?.id]);

    if (!salaUuid) {
        return (
            <Box
                sx={{
                    minHeight: 'calc(100vh - 160px)',
                    width: '100%',
                    maxWidth: '100%',
                    overflowX: 'hidden',
                    bgcolor: theme.palette.background.default,
                    p: { xs: 2, md: 4 },
                    mt: 2,
                    mb: 4
                }}
            >
                <Box sx={{ maxWidth: 1280, mx: 'auto' }}>
                    <Stack spacing={1} sx={{ mb: 3 }}>
                        <Typography variant="overline" color="primary.main" sx={{ letterSpacing: 1.8, opacity: 0.9 }}>
                            Videoconferencias
                        </Typography>
                        <Typography variant="h4" fontWeight="bold">
                            Salas de estudio y reuniones
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                            Crea una sala nueva o entra a una pública disponible, con UUID propio.
                        </Typography>
                    </Stack>

                    {errorMsg && (
                        <Alert severity="warning" sx={{ mb: 2, bgcolor: alpha(theme.palette.warning.main, theme.palette.mode === 'dark' ? 0.16 : 0.08) }}>
                            {errorMsg}
                        </Alert>
                    )}

                    <Grid container spacing={3}>
                        <Grid size={{ xs: 12, md: 5 }}>
                            <Paper sx={{ p: 3, borderRadius: 3, bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.92 : 0.96), border: '1px solid', borderColor: 'divider' }}>
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
                            <Paper sx={{ p: 3, borderRadius: 3, minHeight: 360, bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.92 : 0.96), border: '1px solid', borderColor: 'divider' }}>
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
                                                <Card variant="outlined" sx={{ height: '100%', bgcolor: alpha(theme.palette.background.default, theme.palette.mode === 'dark' ? 0.56 : 0.5), borderColor: 'divider' }}>
                                                    <CardContent>
                                                        <Stack spacing={1.2}>
                                                            <Typography variant="h6" fontWeight="bold">
                                                                {sala.titulo || 'Sala sin título'}
                                                            </Typography>
                                                            <Typography variant="body2" color="text.secondary">
                                                                {sala.salaUuid}
                                                            </Typography>
                                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                                                <Chip size="small" label={`Capacidad: ${sala.capacidadMaxima ?? '-'}`} variant="outlined" />
                                                                <Chip size="small" label={sala.esPublica ? 'Pública' : 'Privada'} color={sala.esPublica ? 'success' : 'default'} variant={sala.esPublica ? 'filled' : 'outlined'} />
                                                                <Chip size="small" label={sala.modoEstudioFlexible ? 'Flexible' : 'Estándar'} variant="outlined" />
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
                                    <Alert severity="info" sx={{ bgcolor: alpha(theme.palette.info.main, theme.palette.mode === 'dark' ? 0.16 : 0.08) }}>
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
                // Eliminamos maxWidth: 100% que puede causar conflictos
                minWidth: 0,
                boxSizing: 'border-box',
                height: { xs: 'calc(100vh - 140px)', lg: 'calc(100vh - 160px)' },
                mt: 2,
                mb: 4,
                // CORRECCIÓN CLAVE: Eliminamos el padding derecho del contenedor principal
                pl: { xs: 1.5, md: 2.5 },
                pt: { xs: 1.5, md: 2.5 },
                pb: { xs: 1.5, md: 2.5 },
                pr: 0, // Aseguramos que no haya padding derecho general
                gap: { xs: 1.25, md: 0 }, // Quitamos el gap en pantallas grandes para pegarlo al borde
                borderRadius: 2,
                boxShadow: 3,
                overflow: 'hidden',
                bgcolor: theme.palette.background.default,
                position: 'relative',
            }}
        >

            <Box sx={{
                flex: '1 1 auto',
                minWidth: 0,
                minHeight: 0,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                height: '100%',
                position: 'relative',
                mr: { lg: panelLateralOpen ? 2 : 0 }
            }}>
                <Box sx={{ flex: 1, minWidth: 0, minHeight: 0, overflow: 'hidden', height: '100%', position: 'relative', borderRadius: 2 }}>
                    {!enSalaDeEspera && !accesoRechazado && (
                        <Box sx={{ position: 'absolute', top: 16, left: 16, zIndex: 15 }}>
                            <Tooltip title="Gestionar participantes">
                                <span>
                                    <IconButton
                                        onClick={handleOpenRoster}
                                        disabled={!esModerador}
                                        sx={{
                                            bgcolor: alpha(theme.palette.background.paper, 0.92),
                                            boxShadow: 2,
                                            '&:hover': { bgcolor: alpha(theme.palette.background.paper, 1) },
                                        }}
                                    >
                                        <People color={esModerador ? 'primary' : 'disabled'} />
                                    </IconButton>
                                </span>
                            </Tooltip>
                        </Box>
                    )}

                    {enSalaDeEspera ? (
                        <Card sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', p: 4, textAlign: 'center', bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.92 : 0.96) }}>
                            <CircularProgress size={60} sx={{ mb: 3 }} />
                            <Typography variant="h5" color="text.secondary" gutterBottom>
                                Esperando aprobación del organizador...
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Entrarás automáticamente a la reunión de "{tituloSala}" cuando el administrador acepte tu solicitud.
                            </Typography>
                            {errorMsg && (
                                <Alert severity="error" sx={{ mt: 3, width: '100%' }}>
                                    {errorMsg}
                                </Alert>
                            )}
                        </Card>
                    ) : accesoRechazado ? (
                        <Card sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', p: 4, textAlign: 'center', bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.92 : 0.96) }}>
                            <Typography variant="h5" color="error" gutterBottom>
                                Acceso rechazado
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                                Tu solicitud para unirte a "{tituloSala}" fue rechazada por el moderador, refresque la página para volver a intentarlo.
                            </Typography>
                            <Alert severity="error" sx={{ mb: 3, width: '100%' }}>
                                {errorMsg || 'No tienes permiso para entrar a esta videoconferencia.'}
                            </Alert>
                            <Button variant="contained" onClick={() => navigate('/videoconferencia')}>
                                Volver a videoconferencias
                            </Button>
                        </Card>
                    ) : (
                        <Box ref={jitsiContainerRef} sx={{ width: '100%', height: '100%', backgroundColor: '#000' }} />
                    )}
                    <Box sx={{ position: 'absolute', inset: 0, zIndex: 2, pointerEvents: 'none' }}>
                        <SubtitulosVideoconferencia
                            salaUuid={salaUuid}
                            usuarioId={perfil?.id || usuarioAutenticado?.id}
                        />
                    </Box>
                </Box>
            </Box>

            <Popover
                open={openRoster}
                anchorEl={rosterAnchorEl}
                onClose={handleCloseRoster}
                anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                disableScrollLock
                PaperProps={{
                    sx: {
                        width: 384,
                        maxHeight: 'calc(100vh - 200px)',
                        p: 1.25,
                        color: theme.palette.text.primary,
                        bgcolor: theme.palette.mode === 'dark'
                            ? alpha(theme.palette.primary.dark, 0.18)
                            : alpha(theme.palette.primary.main, 0.06),
                        backgroundImage: theme.palette.mode === 'dark'
                            ? `radial-gradient(circle at top left, ${alpha(theme.palette.primary.main, 0.32)} 0, transparent 26%), radial-gradient(circle at top right, ${alpha(theme.palette.secondary.main, 0.2)} 0, transparent 24%), linear-gradient(180deg, ${alpha(theme.palette.background.paper, 0.94)} 0%, ${alpha(theme.palette.background.paper, 0.98)} 36%, ${alpha(theme.palette.background.paper, 1)} 100%)`
                            : `radial-gradient(circle at top left, ${alpha(theme.palette.primary.main, 0.18)} 0, transparent 26%), radial-gradient(circle at top right, ${alpha(theme.palette.secondary.main, 0.12)} 0, transparent 24%), linear-gradient(180deg, ${alpha(theme.palette.primary.main, 0.1)} 0%, ${alpha(theme.palette.background.paper, 0.95)} 20%, ${alpha(theme.palette.background.paper, 0.99)} 100%)`,
                        borderTopLeftRadius: 28,
                        borderBottomLeftRadius: 28,
                        borderLeft: `1px solid ${alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.35 : 0.65)}`,
                        borderTop: `1px solid ${alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.18 : 0.55)}`,
                        borderBottom: `1px solid ${alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.18 : 0.55)}`,
                        borderRight: `1px solid ${alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.12 : 0.18)}`,
                        boxShadow: theme.palette.mode === 'dark'
                            ? `0 26px 60px ${alpha('#000', 0.55)}`
                            : `0 26px 58px ${alpha(theme.palette.primary.main, 0.18)}`,
                        backdropFilter: 'blur(18px)',
                        transform: 'translateZ(0)',
                        willChange: 'transform',
                        overflow: 'hidden',
                        transition: theme.transitions.create(['transform', 'opacity'], {
                            duration: theme.transitions.duration.standard,
                            easing: theme.transitions.easing.easeOut,
                        }),
                        position: 'relative',
                        '&::after': {
                            content: '""',
                            position: 'absolute',
                            left: 0,
                            top: 0,
                            bottom: 0,
                            width: 8,
                            background: `linear-gradient(180deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
                            boxShadow: `0 0 24px ${alpha(theme.palette.primary.main, 0.35)}`,
                            pointerEvents: 'none',
                        },
                        '&::before': {
                            content: '""',
                            position: 'absolute',
                            inset: 0,
                            pointerEvents: 'none',
                            backgroundImage: `linear-gradient(135deg, ${alpha(theme.palette.primary.main, 0.15)}, transparent 38%), linear-gradient(315deg, ${alpha(theme.palette.secondary.main, 0.08)}, transparent 34%)`,
                        },
                        '&::-webkit-scrollbar': {
                            width: 10,
                        },
                        '&::-webkit-scrollbar-track': {
                            background: alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.12 : 0.22),
                        },
                        '&::-webkit-scrollbar-thumb': {
                            background: alpha(theme.palette.primary.main, 0.45),
                            borderRadius: 999,
                            border: `2px solid ${alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.9 : 0.96)}`,
                        },
                    },
                }}
            >
                <Box
                    sx={{
                        width: '100%',
                        height: '100%',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 1.5,
                        pl: 1.4,
                        pr: 0.75,
                        py: 0.75,
                        position: 'relative',
                        zIndex: 1,
                        backgroundImage: theme.palette.mode === 'dark'
                            ? `linear-gradient(180deg, ${alpha(theme.palette.background.paper, 0.1)} 0%, transparent 24%), radial-gradient(circle at bottom right, ${alpha(theme.palette.primary.main, 0.18)} 0, transparent 26%), radial-gradient(circle at top right, ${alpha(theme.palette.secondary.main, 0.1)} 0, transparent 20%)`
                            : `linear-gradient(180deg, ${alpha(theme.palette.primary.main, 0.08)} 0%, transparent 24%), radial-gradient(circle at bottom right, ${alpha(theme.palette.primary.main, 0.14)} 0, transparent 26%), radial-gradient(circle at top right, ${alpha(theme.palette.secondary.main, 0.08)} 0, transparent 20%)`,
                    }}
                >
                    <Box
                        sx={{
                            position: 'sticky',
                            top: 0,
                            zIndex: 2,
                            px: 0.5,
                            pt: 0.25,
                            pb: 1,
                            backdropFilter: 'none',
                            background: 'transparent',
                            borderBottom: `1px solid ${alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.28 : 0.48)}`,
                            borderRadius: 0,
                        }}
                    >
                        <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1.5}>
                            <Box sx={{ minWidth: 0, flex: 1, pr: 1 }}>
                                <Typography variant="overline" color="text.secondary" sx={{ display: 'block', letterSpacing: 1.8, fontWeight: 800, lineHeight: 1.1 }}>
                                    Sala en vivo
                                </Typography>
                                <Typography
                                    variant="h6"
                                    fontWeight={900}
                                    noWrap
                                    sx={{ lineHeight: 1.05, mt: 0.25, maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis' }}
                                >
                                    Participantes
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.4, maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                                    Controla roles y presencia en tiempo real.
                                </Typography>
                            </Box>
                            <Chip
                                label={`${participantesConectados.length} conectados`}
                                color="primary"
                                variant="filled"
                                sx={{ fontWeight: 900, borderRadius: 999, boxShadow: `0 10px 20px ${alpha(theme.palette.primary.main, 0.22)}`, flexShrink: 0, mt: 0.25 }}
                            />
                        </Stack>
                    </Box>

                    <List disablePadding sx={{ display: 'flex', flexDirection: 'column', gap: 1, flex: 1, overflowY: 'auto', pr: 0.5, pb: 0.5 }}>
                        {participantesConectados.map((usuario) => {
                            if (!usuario?.id) return null;

                            const rolActual = normalizeRol(
                                rolesUsuarios[usuario.id]
                                || usuario.rol
                                || (usuario.id === perfil?.id ? miRolEnSala : 'PARTICIPANTE')
                            );
                            const nombreVisible = usuario.nombre || usuario.email || 'Usuario';
                            const inicial = String(nombreVisible).trim().charAt(0).toUpperCase() || '?';
                            const chipColor = rolActual === 'ADMIN' ? 'error' : rolActual === 'PRESENTADOR' ? 'warning' : 'default';
                            const roleBarColor = rolActual === 'ADMIN'
                                ? theme.palette.error.main
                                : rolActual === 'PRESENTADOR'
                                    ? theme.palette.warning.main
                                    : theme.palette.primary.main;

                            return (
                                <ListItem
                                    key={usuario.id}
                                    disableGutters
                                    sx={{
                                        display: 'block',
                                        borderRadius: 3,
                                        overflow: 'hidden',
                                        border: `1px solid ${alpha(theme.palette.divider, 0.74)}`,
                                        borderLeft: `4px solid ${alpha(roleBarColor, 0.85)}`,
                                        bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.76 : 0.98),
                                        boxShadow: `0 8px 20px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.14 : 0.04)}`,
                                        transition: theme.transitions.create(['transform', 'box-shadow', 'border-color', 'background-color'], {
                                            duration: theme.transitions.duration.shorter,
                                            easing: theme.transitions.easing.easeOut,
                                        }),
                                        '&:hover': {
                                            transform: 'translateY(-2px)',
                                            borderColor: alpha(roleBarColor, 0.35),
                                            boxShadow: `0 16px 30px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.2 : 0.08)}`,
                                            bgcolor: alpha(roleBarColor, theme.palette.mode === 'dark' ? 0.1 : 0.05),
                                        },
                                    }}
                                >
                                    <Box sx={{ p: 1.35 }}>
                                        <Stack direction="row" spacing={1.5} alignItems={esModerador ? 'flex-start' : 'center'}>
                                            <ListItemAvatar sx={{ minWidth: 0, mt: 0.15 }}>
                                                <Avatar
                                                    sx={{
                                                        width: 40,
                                                        height: 40,
                                                        fontWeight: 900,
                                                        bgcolor: usuario.id === perfil?.id
                                                            ? 'primary.main'
                                                            : alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                                                        color: usuario.id === perfil?.id ? 'primary.contrastText' : 'text.primary',
                                                        boxShadow: `0 8px 18px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.22 : 0.08)}`,
                                                    }}
                                                >
                                                    {inicial}
                                                </Avatar>
                                            </ListItemAvatar>

                                            <Box sx={{ minWidth: 0, flex: 1 }}>
                                                <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 0.35 }}>
                                                    <Typography variant="subtitle2" fontWeight={900} noWrap>
                                                        {nombreVisible}
                                                    </Typography>
                                                    {usuario.id === perfil?.id && (
                                                        <Chip label="Tú" size="small" color="primary" variant="filled" sx={{ fontWeight: 800, height: 24 }} />
                                                    )}
                                                </Stack>
                                                <Typography variant="body2" color="text.secondary" noWrap sx={{ lineHeight: 1.3 }}>
                                                    {usuario.email || 'Sin correo visible'}
                                                </Typography>
                                                <Chip
                                                    label={formatearRolSalaVisible(rolActual)}
                                                    size="small"
                                                    color={chipColor}
                                                    variant={rolActual === 'PARTICIPANTE' ? 'outlined' : 'filled'}
                                                    sx={{ mt: 1, fontWeight: 800, borderRadius: 999, height: 26 }}
                                                />
                                            </Box>

                                            {esModerador ? (
                                                <FormControl size="small" sx={{ minWidth: 138, alignSelf: 'center' }}>
                                                    <InputLabel id={`rol-vivo-${usuario.id}`}>Rol</InputLabel>
                                                    <Select
                                                        labelId={`rol-vivo-${usuario.id}`}
                                                        value={rolActual}
                                                        onChange={(e) => ejecutarCambioRol(usuario.id, e.target.value)}
                                                        label="Rol"
                                                        sx={{
                                                            bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.9 : 1),
                                                            borderRadius: 2.5,
                                                            '& .MuiSelect-select': {
                                                                py: 1,
                                                                fontWeight: 700,
                                                            },
                                                        }}
                                                    >
                                                        <MenuItem value="PARTICIPANTE">Participante</MenuItem>
                                                        <MenuItem value="PRESENTADOR">Presentador</MenuItem>
                                                        <MenuItem value="ADMIN">Administrador</MenuItem>
                                                    </Select>
                                                </FormControl>
                                            ) : (
                                                <Typography variant="caption" color="text.secondary" sx={{ pl: 0.5, whiteSpace: 'nowrap', alignSelf: 'center' }}>
                                                    Rol activo
                                                </Typography>
                                            )}
                                        </Stack>
                                    </Box>
                                </ListItem>
                            );
                        })}
                        {participantesConectados.length === 0 && (
                            <Box
                                sx={{
                                    mt: 1,
                                    p: 2,
                                    borderRadius: 3,
                                    textAlign: 'center',
                                    border: `1px dashed ${alpha(theme.palette.divider, 0.9)}`,
                                    bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.72 : 0.9),
                                }}
                            >
                                <Typography variant="body2" color="text.secondary">
                                    No hay participantes conectados en este momento.
                                </Typography>
                            </Box>
                        )}
                    </List>

                    <Box
                        sx={{
                            mt: 'auto',
                            position: 'sticky',
                            bottom: 0,
                            px: 0.25,
                            pt: 1.25,
                            pb: 0.5,
                            background: `linear-gradient(180deg, transparent 0%, ${alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.94 : 0.98)} 18%)`,
                        }}
                    >
                        <Box
                            sx={{
                                px: 1.5,
                                py: 1,
                                borderTop: `1px solid ${alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.3 : 0.45)}`,
                                borderRadius: 0,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                gap: 2,
                                bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.92 : 0.96),
                            }}
                        >
                            <Box sx={{ minWidth: 0 }}>
                                <Typography variant="subtitle2" fontWeight={800} sx={{ lineHeight: 1.15 }}>
                                    Vista en vivo
                                </Typography>
                                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.2, lineHeight: 1.2 }}>
                                    Los cambios de rol se reflejan al instante.
                                </Typography>
                            </Box>
                            <Chip
                                label={esModerador ? 'Moderación activa' : 'Acceso restringido'}
                                size="small"
                                color={esModerador ? 'success' : 'default'}
                                variant={esModerador ? 'filled' : 'outlined'}
                                sx={{ fontWeight: 700, borderRadius: 999, flexShrink: 0 }}
                            />
                        </Box>
                    </Box>
                </Box>
            </Popover>

            {!panelLateralOpen && (
                <Box sx={{ position: 'absolute', top: 20, right: 12, zIndex: 12, display: { xs: 'none', lg: 'inline-flex' } }}>
                    <Tooltip title="Expandir menú">
                        <IconButton
                            size="small"
                            onClick={() => setPanelLateralOpen(true)}
                            sx={{
                                border: '1px solid',
                                borderColor: 'divider',
                                bgcolor: 'background.paper',
                                boxShadow: 2,
                            }}
                        >
                            <KeyboardArrowLeft />
                        </IconButton>
                    </Tooltip>
                </Box>
            )}

            <Dialog open={openDialogSolicitud} maxWidth="xs" fullWidth onClose={() => setOpenDialogSolicitud(false)}>
                <DialogTitle>Solicitud de acceso a la reunión</DialogTitle>
                <DialogContent dividers>
                    <Stack spacing={1.5}>
                        {solicitudesPendientes.map((solicitud) => (
                            <Paper key={solicitud.usuarioId} variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
                                <Stack spacing={1.25}>
                                    <Box>
                                        <Typography variant="body1" fontWeight="bold">
                                            {solicitud.nombreUsuario || 'Usuario'}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            desea unirse a la videoconferencia.
                                        </Typography>
                                    </Box>

                                    <FormControl size="small" fullWidth>
                                        <InputLabel id={`rol-asignado-${solicitud.usuarioId}`}>Asignar rol</InputLabel>
                                        <Select
                                            labelId={`rol-asignado-${solicitud.usuarioId}`}
                                            value={rolesSeleccionados[solicitud.usuarioId] || 'PARTICIPANTE'}
                                            label="Asignar rol"
                                            onChange={(e) => setRolesSeleccionados((prev) => ({
                                                ...prev,
                                                [solicitud.usuarioId]: e.target.value,
                                            }))}
                                        >
                                            <MenuItem value="PARTICIPANTE">Participante</MenuItem>
                                            <MenuItem value="PRESENTADOR">Presentador</MenuItem>
                                            <MenuItem value="ADMIN">Administrador</MenuItem>
                                        </Select>
                                    </FormControl>

                                    <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                                        <Button onClick={() => manejarRespuestaSolicitud(solicitud, false)} color="error" variant="outlined" size="small">
                                            Rechazar
                                        </Button>
                                        <Button onClick={() => manejarRespuestaSolicitud(solicitud, true)} color="primary" variant="contained" size="small">
                                            Permitir entrada
                                        </Button>
                                    </Box>
                                </Stack>
                            </Paper>
                        ))}
                        {solicitudesPendientes.length === 0 && (
                            <Typography variant="body2" color="text.secondary">
                                No hay solicitudes pendientes.
                            </Typography>
                        )}
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpenDialogSolicitud(false)}>Cerrar</Button>
                </DialogActions>
            </Dialog>

            {/* PANEL DERECHO: Controles de MUI y Reproductor (25% aprox - Ancho fijo) */}
            <Paper
                elevation={4}
                square
                sx={{
                    flex: { xs: '0 0 auto', lg: panelLateralOpen ? '0 0 clamp(300px, 30vw, 380px)' : '0 0 0' },
                    width: { xs: '100%', lg: panelLateralOpen ? 'clamp(300px, 30vw, 380px)' : 0 },
                    // CORRECCIÓN CLAVE: Aseguramos que no intente crecer más de la cuenta
                    minWidth: { xs: '100%', lg: panelLateralOpen ? '300px' : 0 },
                    maxWidth: { xs: '100%', lg: panelLateralOpen ? '380px' : 0 },
                    height: '100%',
                    maxHeight: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    p: panelLateralOpen ? 2 : 0,
                    zIndex: 10,
                    overflowY: 'auto',
                    overflowX: 'hidden',
                    bgcolor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.92 : 0.94),
                    borderLeft: panelLateralOpen ? '1px solid' : 'none',
                    borderColor: 'divider',
                    transition: theme.transitions.create(['width', 'flex-basis', 'min-width'], {
                        duration: 320,
                        easing: theme.transitions.easing.easeInOut,
                    }),
                    willChange: 'width, flex-basis',
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

                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        flex: 1,
                        minHeight: 0,
                        opacity: panelLateralOpen ? 1 : 0,
                        transform: panelLateralOpen ? 'translateX(0)' : 'translateX(10px)',
                        pointerEvents: panelLateralOpen ? 'auto' : 'none',
                        overflow: 'hidden',
                        maxHeight: panelLateralOpen ? 'none' : 0,
                        transition: theme.transitions.create(['opacity', 'transform', 'max-height'], {
                            duration: 240,
                            easing: theme.transitions.easing.easeInOut,
                        }),
                    }}
                >
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
                </Box>
            </Paper>
        </Box>
    );
};

export default SalaEstudioMUI;