import { useState, useEffect, useRef } from 'react';
import {
    Box, Paper, Typography, List, ListItemButton, ListItemAvatar,
    Avatar, ListItemText, TextField, IconButton, Stack, Divider, Badge, CircularProgress, Alert, Button, InputAdornment, Chip
} from '@mui/material';
import { alpha, useTheme } from '@mui/material/styles';
import { Send, AttachFile, Close, InsertDriveFile, Image as ImageIcon, AccountCircle, Search, MoreVert, CheckCircle, AccessTime } from '@mui/icons-material';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../API/axios';
import AuthService from '../services/AuthService';
import UsuarioService from '../services/UsuarioService';
import ChatService from '../services/ChatService';
import { extractBackendValidationMessage } from '../utils/backendValidation';

// Función para obtener la URL de WebSockets a partir de la de Axios
const getWsUrl = () => {
    const apiUrl = axios.defaults.baseURL;
    return apiUrl.replace('/api', '') + '/ws-chat';
};

// Función para normalizar el rol recibido del backend
const normalizeRol = (rol) => String(rol || '').replace('ROLE_', '').toUpperCase();

const ChatInstitucional = () => {
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';

    const [miPerfil, setMiPerfil] = useState(null);
    const [contactos, setContactos] = useState([]);
    const [conectados, setConectados] = useState([]);
    const [noLeidos, setNoLeidos] = useState({});
    const [contactoActivo, setContactoActivo] = useState(null);
    const [filtroContacto, setFiltroContacto] = useState('');

    const contactoActivoRef = useRef(null);
    const [mensajes, setMensajes] = useState([]);
    const [mensajeTexto, setMensajeTexto] = useState('');
    const [archivoAdjunto, setArchivoAdjunto] = useState(null);

    const [isLoading, setIsLoading] = useState(true);
    const [isSending, setIsSending] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    const stompClientRef = useRef(null);
    const messagesEndRef = useRef(null);
    const fileInputRef = useRef(null);

    useEffect(() => {
        contactoActivoRef.current = contactoActivo;
    }, [contactoActivo]);

    // 1. Cargar datos iniciales
    useEffect(() => {
        const inicializarChat = async () => {
            try {
                const perfil = await AuthService.usuarioConectado();
                setMiPerfil(perfil);

                const [todosUsuarios, usuariosConectados, conteoNoLeidos] = await Promise.all([
                    UsuarioService.listarParaChat(),
                    UsuarioService.listarConectados().catch(() => []),
                    ChatService.obtenerNoLeidos().catch(() => ({}))
                ]);

                // --- FILTRADO CORREGIDO SEGÚN EL ROL NORMALIZADO ---
                const miRol = normalizeRol(perfil.rol);
                let contactosFiltrados = todosUsuarios.filter(u => u.id !== perfil.id);

                if (miRol === 'ALUMNO') {
                    contactosFiltrados = contactosFiltrados.filter(u =>
                        normalizeRol(u.rol) === 'PROFESOR' || normalizeRol(u.rol) === 'ALUMNO'
                    );
                } else if (miRol === 'PROFESOR') {
                    contactosFiltrados = contactosFiltrados.filter(u =>
                        normalizeRol(u.rol) === 'ADMIN' || normalizeRol(u.rol) === 'ALUMNO'
                    );
                } else if (miRol === 'ADMIN') {
                    contactosFiltrados = contactosFiltrados.filter(u =>
                        normalizeRol(u.rol) === 'ADMIN' || normalizeRol(u.rol) === 'PROFESOR'
                    );
                }

                setContactos(contactosFiltrados);
                setConectados(usuariosConectados);
                setNoLeidos(conteoNoLeidos || {});
            } catch {
                setErrorMsg('Error al cargar la información del chat.');
            } finally {
                setIsLoading(false);
            }
        };

        inicializarChat();

        const intervalId = setInterval(async () => {
            try {
                const activos = await UsuarioService.listarConectados();
                setConectados(activos || []);
            } catch { /* Silencioso */ }
        }, 15000);

        return () => clearInterval(intervalId);
    }, []);

    // 2. Conectar a WebSockets
    useEffect(() => {
        if (!miPerfil?.id) return;
        const token = localStorage.getItem('token');
        if (!token) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(getWsUrl()),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5000,
            onConnect: () => {
                client.subscribe(`/queue/mensajes/${miPerfil.id}`, (msg) => {
                    const nuevoMensaje = JSON.parse(msg.body);
                    const chatActual = contactoActivoRef.current;

                    if (chatActual && chatActual.id === nuevoMensaje.emisor.id) {
                        setMensajes(prev => {
                            if (prev.some(m => m.id === nuevoMensaje.id)) return prev;
                            return [...prev, nuevoMensaje];
                        });
                        ChatService.marcarLeidos(nuevoMensaje.emisor.id).catch(() => { });
                    } else {
                        setNoLeidos(prev => ({
                            ...prev,
                            [nuevoMensaje.emisor.id]: (prev[nuevoMensaje.emisor.id] || 0) + 1
                        }));
                    }
                });
            }
        });

        client.activate();
        stompClientRef.current = client;

        return () => client.deactivate();
    }, [miPerfil?.id]);

    // 3. Cargar historial cuando se selecciona un contacto
    useEffect(() => {
        if (!contactoActivo) return;

        const cargarHistorial = async () => {
            try {
                const historial = await ChatService.obtenerHistorial(contactoActivo.id);
                setMensajes(historial || []);
                setErrorMsg('');
            } catch (error) {
                setErrorMsg(extractBackendValidationMessage(error, 'No se pudo cargar el historial.'));
            }
        };

        cargarHistorial();
    }, [contactoActivo]);

    // 4. Auto-scroll
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [mensajes]);

    const handleSeleccionarContacto = (contacto) => {
        setContactoActivo(contacto);
        if (noLeidos[contacto.id] > 0) {
            setNoLeidos(prev => ({ ...prev, [contacto.id]: 0 }));
            ChatService.marcarLeidos(contacto.id).catch(() => { });
        }
    };

    const handleSend = async () => {
        if ((!mensajeTexto.trim() && !archivoAdjunto) || !contactoActivo || !miPerfil) return;

        setIsSending(true);
        try {
            if (archivoAdjunto) {
                const isImage = archivoAdjunto.type.startsWith('image/');
                const tipo = isImage ? 'IMAGEN' : archivoAdjunto.name.endsWith('.pdf') ? 'PDF' : 'WORD';
                const mensajeGuardado = await ChatService.enviarArchivo(archivoAdjunto, contactoActivo.id, tipo);

                setMensajes(prev => [...prev, mensajeGuardado]);
                setArchivoAdjunto(null);
            } else {
                const payload = {
                    idReceptor: contactoActivo.id,
                    contenido: mensajeTexto.trim()
                };

                stompClientRef.current.publish({
                    destination: '/app/enviar',
                    body: JSON.stringify(payload)
                });

                setMensajes(prev => [...prev, {
                    id: `temp-${Date.now()}`,
                    emisor: { id: miPerfil.id, nombre: miPerfil.nombre },
                    receptor: { id: contactoActivo.id },
                    contenido: mensajeTexto.trim(),
                    tipo: 'TEXTO',
                    fechaEnvio: new Date().toISOString()
                }]);
            }
            setMensajeTexto('');
            setErrorMsg('');
        } catch {
            setErrorMsg('Error al enviar el mensaje. Verifica tus permisos.');
        } finally {
            setIsSending(false);
        }
    };

    const handleFileChange = (e) => {
        if (e.target.files && e.target.files.length > 0) {
            setArchivoAdjunto(e.target.files[0]);
        }
    };

    const obtenerEtiquetaFecha = (fechaString) => {
        const fechaMsg = new Date(fechaString);
        const hoy = new Date();
        const ayer = new Date(hoy);
        ayer.setDate(hoy.getDate() - 1);
        const formato = { day: '2-digit', month: '2-digit', year: 'numeric' };

        if (fechaMsg.toLocaleDateString() === hoy.toLocaleDateString()) return "Hoy";
        if (fechaMsg.toLocaleDateString() === ayer.toLocaleDateString()) return "Ayer";
        return fechaMsg.toLocaleDateString('es-PE', formato);
    };

    const isOnline = (email) => conectados.some(c => c.email === email);

    const getRolBadgeColor = (rol) => {
        const rolLimpio = normalizeRol(rol);
        if (rolLimpio === 'ADMIN') return 'error';
        if (rolLimpio === 'PROFESOR') return 'primary';
        return 'success';
    };

    const contactosMostrados = contactos.filter(c =>
        c.nombre?.toLowerCase().includes(filtroContacto.toLowerCase())
    );

    if (isLoading) return <CircularProgress sx={{ display: 'block', margin: '40px auto' }} />;

    let fechaActualRenderizada = null;

    return (
        <Box
            sx={{
                display: 'flex',
                height: 'calc(100vh - 120px)',
                gap: 2,
                p: 0.5,
                background: isDark
                    ? 'radial-gradient(circle at top left, rgba(37, 99, 235, 0.16), transparent 28%), radial-gradient(circle at top right, rgba(14, 116, 144, 0.14), transparent 26%), linear-gradient(180deg, rgba(11,18,32,0.9), rgba(15,23,42,0.92))'
                    : 'radial-gradient(circle at top left, rgba(37, 99, 235, 0.10), transparent 28%), radial-gradient(circle at top right, rgba(14, 116, 144, 0.08), transparent 26%), linear-gradient(180deg, rgba(255,255,255,0.35), rgba(255,255,255,0.08))',
            }}
        >
            {/* PANEL IZQUIERDO */}
            <Paper
                sx={{
                    width: { xs: '110px', sm: '330px' },
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                    borderRadius: 2,
                    border: `1px solid ${isDark ? 'rgba(148, 163, 184, 0.18)' : 'rgba(148, 163, 184, 0.18)'}`,
                    bgcolor: isDark ? alpha(theme.palette.background.paper, 0.92) : 'rgba(255,255,255,0.82)',
                    backdropFilter: 'blur(18px)',
                    boxShadow: isDark ? '0 18px 48px rgba(2, 6, 23, 0.45)' : '0 18px 48px rgba(15, 23, 42, 0.10)',
                }}
            >
                <Box
                    sx={{
                        p: 2,
                        color: 'white',
                        background: isDark
                            ? 'linear-gradient(135deg, #0f172a 0%, #1e293b 55%, #0f766e 100%)'
                            : 'linear-gradient(135deg, #1d4ed8 0%, #0f766e 52%, #0284c7 100%)',
                    }}
                >
                    <Typography variant="overline" sx={{ letterSpacing: 2, opacity: 0.85 }}>
                        Mensajería
                    </Typography>
                    <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.15 }}>
                        Chats Institucionales
                    </Typography>
                </Box>

                <Box sx={{ px: 2, py: 1.5, display: { xs: 'none', sm: 'block' } }}>
                    <TextField
                        fullWidth
                        size="small"
                        placeholder="Buscar contacto..."
                        value={filtroContacto}
                        onChange={(e) => setFiltroContacto(e.target.value)}
                        slotProps={{
                            input: {
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <Search fontSize="small" />
                                    </InputAdornment>
                                ),
                            },
                        }}
                        sx={{
                            '& .MuiOutlinedInput-root': {
                                borderRadius: 2,
                                bgcolor: isDark ? alpha(theme.palette.background.default, 0.9) : 'background.paper',
                            },
                        }}
                    />
                </Box>
                <Divider />

                <List sx={{ flex: 1, overflowY: 'auto', p: 1 }}>
                    {contactosMostrados.map(contacto => {
                        const online = isOnline(contacto.email);
                        return (
                            <ListItemButton
                                key={contacto.id}
                                selected={contactoActivo?.id === contacto.id}
                                onClick={() => handleSeleccionarContacto(contacto)}
                                sx={{
                                    py: 1.25,
                                    px: 1.5,
                                    mb: 0.75,
                                    borderRadius: 2,
                                    alignItems: 'center',
                                    transition: 'all 180ms ease',
                                    '&.Mui-selected': {
                                        bgcolor: isDark ? 'rgba(37, 99, 235, 0.22)' : 'rgba(37, 99, 235, 0.10)',
                                        '&:hover': { bgcolor: isDark ? 'rgba(37, 99, 235, 0.28)' : 'rgba(37, 99, 235, 0.14)' },
                                    },
                                    '&:hover': {
                                        bgcolor: isDark ? 'rgba(148, 163, 184, 0.14)' : 'rgba(148, 163, 184, 0.10)',
                                    },
                                }}
                            >
                                <ListItemAvatar>
                                    <Badge color="success" variant="dot" invisible={!online} overlap="circular" anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
                                        <Avatar sx={{ bgcolor: getRolBadgeColor(contacto.rol) + '.main', boxShadow: '0 8px 18px rgba(15,23,42,0.12)' }}>
                                            {contacto.nombre ? contacto.nombre.charAt(0).toUpperCase() : <AccountCircle />}
                                        </Avatar>
                                    </Badge>
                                </ListItemAvatar>
                                <ListItemText
                                    primary={
                                        <Typography sx={{ fontWeight: noLeidos[contacto.id] > 0 || contactoActivo?.id === contacto.id ? 800 : 600 }} noWrap>
                                            {contacto.nombre}
                                        </Typography>
                                    }
                                    secondary={
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.25 }}>
                                            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'capitalize' }}>
                                                {normalizeRol(contacto.rol)}
                                            </Typography>
                                            {online && (
                                                <Chip
                                                    label="En línea"
                                                    size="small"
                                                    sx={{
                                                        height: 20,
                                                        fontSize: '0.65rem',
                                                        bgcolor: isDark ? 'rgba(37, 99, 235, 0.22)' : 'rgba(37, 99, 235, 0.10)',
                                                        color: 'primary.main',
                                                    }}
                                                />
                                            )}
                                        </Box>
                                    }
                                    sx={{ display: { xs: 'none', sm: 'block' } }}
                                />
                                {noLeidos[contacto.id] > 0 && (
                                    <Badge
                                        badgeContent={noLeidos[contacto.id]}
                                        color="error"
                                        sx={{
                                            mr: 2,
                                            display: { xs: 'none', sm: 'flex' },
                                            '& .MuiBadge-badge': { fontWeight: 'bold' }
                                        }}
                                    />
                                )}
                            </ListItemButton>
                        );
                    })}
                    {contactosMostrados.length === 0 && (
                        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', mt: 3, display: { xs: 'none', sm: 'block' } }}>
                            No se encontraron contactos.
                        </Typography>
                    )}
                </List>
            </Paper>

            {/* PANEL DERECHO */}
            <Paper
                sx={{
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    position: 'relative',
                    overflow: 'hidden',
                    borderRadius: 2,
                    border: `1px solid ${isDark ? 'rgba(148, 163, 184, 0.18)' : 'rgba(148, 163, 184, 0.18)'}`,
                    bgcolor: isDark ? alpha(theme.palette.background.paper, 0.88) : 'rgba(255,255,255,0.70)',
                    backdropFilter: 'blur(18px)',
                    boxShadow: isDark ? '0 18px 48px rgba(2, 6, 23, 0.45)' : '0 18px 48px rgba(15, 23, 42, 0.10)',
                }}
            >
                {contactoActivo ? (
                    <>
                        {/* Cabecera */}
                        <Box
                            sx={{
                                p: 2,
                                bgcolor: isDark ? alpha(theme.palette.primary.main, 0.32) : 'primary.dark',
                                color: 'white',
                                borderBottom: '1px solid rgba(255,255,255,0.08)',
                                display: 'flex',
                                alignItems: 'center',
                                gap: 2,
                                justifyContent: 'space-between',
                            }}
                        >
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, minWidth: 0 }}>
                            <Avatar sx={{ bgcolor: getRolBadgeColor(contactoActivo.rol) + '.main' }}>
                                {contactoActivo.nombre?.charAt(0).toUpperCase()}
                            </Avatar>
                            <Box>
                                <Typography variant="subtitle1" fontWeight="800" noWrap>
                                    {contactoActivo.nombre}
                                </Typography>
                                <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.78)' }}>
                                    {isOnline(contactoActivo.email) ? 'En línea' : 'Desconectado'}
                                </Typography>
                            </Box>
                            </Box>
                            <IconButton sx={{ color: 'white', bgcolor: 'rgba(255,255,255,0.08)' }}>
                                <MoreVert />
                            </IconButton>
                        </Box>

                        {/* Mensajes */}
                        <Box
                            sx={{
                                flex: 1,
                                overflowY: 'auto',
                                p: 3,
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 1.5,
                                    backgroundColor: isDark ? alpha(theme.palette.background.default, 0.96) : '#f6f8fc',
                                    backgroundImage: isDark
                                        ? 'radial-gradient(rgba(148, 163, 184, 0.09) 1px, transparent 1px)'
                                        : 'radial-gradient(rgba(37, 99, 235, 0.05) 1px, transparent 1px)',
                                backgroundSize: '22px 22px',
                            }}
                        >
                            {errorMsg && <Alert severity="error" sx={{ mb: 2 }}>{errorMsg}</Alert>}

                            {mensajes.length === 0 && (
                                    <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 4, bgcolor: isDark ? alpha(theme.palette.background.paper, 0.85) : 'rgba(255,255,255,0.75)', px: 2, py: 1, borderRadius: 2, alignSelf: 'center' }}>
                                    No hay mensajes previos. ¡Inicia la conversación!
                                </Typography>
                            )}

                            {mensajes.map((msg) => {
                                const soyYo = msg.emisor.id === miPerfil.id;
                                const etiquetaFecha = obtenerEtiquetaFecha(msg.fechaEnvio);
                                const mostrarEtiqueta = fechaActualRenderizada !== etiquetaFecha;

                                if (mostrarEtiqueta) {
                                    fechaActualRenderizada = etiquetaFecha;
                                }

                                return (
                                    <Box key={msg.id} sx={{ display: 'flex', flexDirection: 'column' }}>
                                        {mostrarEtiqueta && (
                                            <Box sx={{ display: 'flex', justifyContent: 'center', my: 2 }}>
                                                <Typography variant="caption" sx={{ bgcolor: isDark ? alpha(theme.palette.background.paper, 0.92) : 'rgba(255,255,255,0.82)', px: 2, py: 0.6, borderRadius: 2, boxShadow: isDark ? '0 8px 20px rgba(2,6,23,0.28)' : '0 8px 20px rgba(15,23,42,0.08)', color: 'text.secondary', fontWeight: 'bold' }}>
                                                    {etiquetaFecha}
                                                </Typography>
                                            </Box>
                                        )}

                                        <Box sx={{ display: 'flex', justifyContent: soyYo ? 'flex-end' : 'flex-start', mb: 1 }}>
                                            <Box sx={{
                                                maxWidth: '75%',
                                                p: 1.5,
                                                position: 'relative',
                                                borderRadius: '18px 18px 18px 6px',
                                                bgcolor: soyYo
                                                    ? (isDark ? alpha(theme.palette.primary.main, 0.32) : 'rgba(37, 99, 235, 0.12)')
                                                    : (isDark ? alpha(theme.palette.background.paper, 0.94) : 'rgba(255,255,255,0.96)'),
                                                color: 'text.primary',
                                                minHeight: 56,
                                                display: 'flex',
                                                flexDirection: 'column',
                                                justifyContent: 'center',
                                                borderTopLeftRadius: 18,
                                                borderTopRightRadius: 18,
                                                borderBottomRightRadius: soyYo ? 6 : 18,
                                                borderBottomLeftRadius: soyYo ? 18 : 6,
                                                boxShadow: isDark ? '0 8px 20px rgba(2, 6, 23, 0.28)' : '0 8px 20px rgba(15, 23, 42, 0.08)',
                                                border: soyYo
                                                    ? `1px solid ${isDark ? 'rgba(96, 165, 250, 0.24)' : 'rgba(37, 99, 235, 0.18)'}`
                                                    : `1px solid ${isDark ? 'rgba(148, 163, 184, 0.18)' : 'rgba(148, 163, 184, 0.18)'}`,
                                            }}>
                                                {msg.tipo === 'IMAGEN' ? (
                                                    <Box component="img" src={msg.urlArchivo} alt="Adjunto" sx={{ maxWidth: '100%', borderRadius: 1, mb: 1 }} />
                                                ) : msg.tipo !== 'TEXTO' && msg.urlArchivo ? (
                                                    <Button
                                                        variant="contained"
                                                        color={soyYo ? 'primary' : 'secondary'}
                                                        href={msg.urlArchivo}
                                                        target="_blank"
                                                        startIcon={<InsertDriveFile />}
                                                        sx={{ mb: 1, textTransform: 'none', borderRadius: 2 }}
                                                    >
                                                        Ver Documento
                                                    </Button>
                                                ) : null}

                                                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                                                    {msg.contenido}
                                                </Typography>
                                                <Typography variant="caption" sx={{ display: 'flex', mt: 0.7, opacity: 0.78, textAlign: 'right', alignItems: 'center', justifyContent: 'flex-end', gap: 0.5 }}>
                                                    {new Date(msg.fechaEnvio).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                                    {soyYo ? <CheckCircle sx={{ fontSize: 14, color: 'success.main' }} /> : <AccessTime sx={{ fontSize: 14 }} />}
                                                </Typography>
                                            </Box>
                                        </Box>
                                    </Box>
                                );
                            })}
                            <div ref={messagesEndRef} />
                        </Box>

                        {/* Adjuntos e Input */}
                        {archivoAdjunto && (
                            <Box sx={{ p: 1.25, bgcolor: isDark ? alpha(theme.palette.background.paper, 0.9) : 'rgba(255,255,255,0.88)', borderTop: '1px solid', borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 1.25 }}>
                                {archivoAdjunto.type.startsWith('image/') ? <ImageIcon color="primary" /> : <InsertDriveFile color="primary" />}
                                <Typography variant="body2" sx={{ flex: 1 }} noWrap>{archivoAdjunto.name}</Typography>
                                <IconButton size="small" onClick={() => setArchivoAdjunto(null)}>
                                    <Close fontSize="small" />
                                </IconButton>
                            </Box>
                        )}

                        <Box sx={{ p: 2, bgcolor: isDark ? alpha(theme.palette.background.paper, 0.92) : 'rgba(255,255,255,0.92)', borderTop: '1px solid', borderColor: 'divider' }}>
                            <Stack direction="row" spacing={1} alignItems="center">
                                <IconButton color="primary" component="label" disabled={isSending}>
                                    <AttachFile />
                                    <input type="file" hidden ref={fileInputRef} onChange={handleFileChange} accept="image/*,.pdf,.doc,.docx" />
                                </IconButton>

                                <TextField
                                    fullWidth
                                    size="small"
                                    placeholder="Escribe un mensaje..."
                                    value={mensajeTexto}
                                    onChange={(e) => setMensajeTexto(e.target.value)}
                                    onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), handleSend())}
                                    disabled={isSending}
                                    multiline
                                    maxRows={3}
                                    sx={{
                                        bgcolor: isDark ? alpha(theme.palette.background.default, 0.9) : 'background.paper',
                                        '& .MuiOutlinedInput-root': {
                                            borderRadius: 2,
                                            bgcolor: isDark ? alpha(theme.palette.background.default, 0.92) : 'rgba(255,255,255,0.98)',
                                        },
                                    }}
                                />

                                <IconButton
                                    color="primary"
                                    onClick={handleSend}
                                    disabled={isSending || (!mensajeTexto.trim() && !archivoAdjunto)}
                                    sx={{
                                        width: 48,
                                        height: 48,
                                        bgcolor: 'primary.main',
                                        color: 'white',
                                        '&:hover': { bgcolor: 'primary.dark' },
                                        '&.Mui-disabled': { bgcolor: 'action.disabledBackground', color: 'action.disabled' },
                                    }}
                                >
                                    {isSending ? <CircularProgress size={24} /> : <Send />}
                                </IconButton>
                            </Stack>
                        </Box>
                    </>
                ) : (
                    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', bgcolor: isDark ? alpha(theme.palette.background.default, 0.96) : '#f6f8fc' }}>
                        <Avatar sx={{ width: 92, height: 92, mb: 2, bgcolor: 'primary.main', boxShadow: '0 18px 36px rgba(37, 99, 235, 0.22)' }}>
                            <Send sx={{ fontSize: 40 }} />
                        </Avatar>
                        <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 700 }}>
                            Selecciona un contacto para iniciar la conversación
                        </Typography>
                    </Box>
                )}
            </Paper>
        </Box>
    );
};

export default ChatInstitucional;