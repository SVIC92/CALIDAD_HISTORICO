import { useState, useEffect, useRef } from 'react';
import {
    Box, Paper, Typography, List, ListItemButton, ListItemAvatar,
    Avatar, ListItemText, TextField, IconButton, Stack, Divider, Badge, CircularProgress, Alert, Button
} from '@mui/material';
import { Send, AttachFile, Close, InsertDriveFile, Image as ImageIcon, AccountCircle } from '@mui/icons-material';
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

const ChatInstitucional = () => {
    const [miPerfil, setMiPerfil] = useState(null);
    const [contactos, setContactos] = useState([]);
    const [conectados, setConectados] = useState([]);
    const [noLeidos, setNoLeidos] = useState({});

    const [contactoActivo, setContactoActivo] = useState(null);
    const contactoActivoRef = useRef(null); // ¡CLAVE PARA WEBSOCKETS! Mantiene la referencia fresca

    const [mensajes, setMensajes] = useState([]);
    const [mensajeTexto, setMensajeTexto] = useState('');
    const [archivoAdjunto, setArchivoAdjunto] = useState(null);

    const [isLoading, setIsLoading] = useState(true);
    const [isSending, setIsSending] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    const stompClientRef = useRef(null);
    const messagesEndRef = useRef(null);
    const fileInputRef = useRef(null);

    // Mantiene el Ref sincronizado con el state
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

                setContactos(todosUsuarios.filter(u => u.id !== perfil.id));
                setConectados(usuariosConectados);
                setNoLeidos(conteoNoLeidos || {});
            } catch (error) {
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
            } catch (e) { /* Silencioso */ }
        }, 15000);

        return () => clearInterval(intervalId);
    }, []);

    // 2. Conectar a WebSockets
    useEffect(() => {
        // Solo nos conectamos si ya terminó de cargar nuestro perfil
        if (!miPerfil?.id) return;

        const token = localStorage.getItem('token');
        if (!token) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(getWsUrl()),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5000,
            onConnect: () => {
                // SUSCRIPCIÓN PERSONALIZADA DIRECTA
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
        // Limpiamos el contador rojo en tiempo real y en BD
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
        } catch (error) {
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
    // Formateador de Fechas ("Hoy", "Ayer", "12/05/2026")
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

    // UI Helpers
    const isOnline = (email) => conectados.some(c => c.email === email);
    const getRolBadgeColor = (rol) => {
        if (rol?.includes('ADMIN')) return 'error';
        if (rol?.includes('PROFESOR')) return 'primary';
        return 'success';
    };

    if (isLoading) return <CircularProgress sx={{ display: 'block', margin: '40px auto' }} />;

    let fechaActualRenderizada = null;

    return (
        <Box sx={{ display: 'flex', height: 'calc(100vh - 120px)', gap: 2 }}>

            {/* PANEL IZQUIERDO */}
            <Paper sx={{ width: { xs: '100px', sm: '320px' }, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                <Typography variant="h6" sx={{ p: 2, display: { xs: 'none', sm: 'block' }, fontWeight: 'bold' }}>
                    Chats Institucionales
                </Typography>
                <Divider />
                <List sx={{ flex: 1, overflowY: 'auto', p: 0 }}>
                    {contactos.map(contacto => {
                        const online = isOnline(contacto.email);
                        return (
                            <ListItemButton
                                key={contacto.id}
                                selected={contactoActivo?.id === contacto.id}
                                onClick={() => handleSeleccionarContacto(contacto)}
                                sx={{ py: 1.5 }}
                            >
                                <ListItemAvatar>
                                    <Badge color="success" variant="dot" invisible={!online} overlap="circular" anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
                                        <Avatar sx={{ bgcolor: getRolBadgeColor(contacto.rol) + '.main' }}>
                                            {contacto.nombre ? contacto.nombre.charAt(0).toUpperCase() : <AccountCircle />}
                                        </Avatar>
                                    </Badge>
                                </ListItemAvatar>
                                <ListItemText
                                    primary={<Typography sx={{ fontWeight: noLeidos[contacto.id] > 0 || contactoActivo?.id === contacto.id ? 'bold' : 'normal' }}>{contacto.nombre}</Typography>}
                                    secondary={contacto.rol?.replace('ROLE_', '')}
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
                </List>
            </Paper>

            {/* PANEL DERECHO */}
            <Paper sx={{ flex: 1, display: 'flex', flexDirection: 'column', position: 'relative', overflow: 'hidden' }}>
                {contactoActivo ? (
                    <>
                        {/* Cabecera */}
                        <Box sx={{ p: 2, bgcolor: 'background.default', borderBottom: '1px solid', borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 2 }}>
                            <Avatar sx={{ bgcolor: getRolBadgeColor(contactoActivo.rol) + '.main' }}>
                                {contactoActivo.nombre?.charAt(0).toUpperCase()}
                            </Avatar>
                            <Box>
                                <Typography variant="subtitle1" fontWeight="bold">{contactoActivo.nombre}</Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {isOnline(contactoActivo.email) ? 'En línea' : 'Desconectado'}
                                </Typography>
                            </Box>
                        </Box>

                        {/* Mensajes */}
                        <Box sx={{ flex: 1, overflowY: 'auto', p: 3, display: 'flex', flexDirection: 'column', gap: 1.5, bgcolor: 'action.hover' }}>
                            {errorMsg && <Alert severity="error" sx={{ mb: 2 }}>{errorMsg}</Alert>}

                            {mensajes.length === 0 && (
                                <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 4 }}>
                                    No hay mensajes previos. ¡Inicia la conversación!
                                </Typography>
                            )}

                            {mensajes.map((msg) => {
                                const soyYo = msg.emisor.id === miPerfil.id;
                                const etiquetaFecha = obtenerEtiquetaFecha(msg.fechaEnvio);

                                // Lógica para mostrar la etiqueta de fecha solo si cambió de día
                                const mostrarEtiqueta = fechaActualRenderizada !== etiquetaFecha;
                                if (mostrarEtiqueta) {
                                    fechaActualRenderizada = etiquetaFecha;
                                }

                                return (
                                    <Box key={msg.id} sx={{ display: 'flex', flexDirection: 'column' }}>
                                        {/* Renderizamos la fecha agrupadora si corresponde */}
                                        {mostrarEtiqueta && (
                                            <Box sx={{ display: 'flex', justifyContent: 'center', my: 2 }}>
                                                <Typography variant="caption" sx={{ bgcolor: 'background.paper', px: 2, py: 0.5, borderRadius: 4, boxShadow: 1, color: 'text.secondary', fontWeight: 'bold' }}>
                                                    {etiquetaFecha}
                                                </Typography>
                                            </Box>
                                        )}

                                        <Box sx={{ display: 'flex', justifyContent: soyYo ? 'flex-end' : 'flex-start', mb: 1 }}>
                                            <Box sx={{
                                                maxWidth: '75%',
                                                p: 1.5,
                                                borderRadius: 2,
                                                bgcolor: soyYo ? 'primary.main' : 'background.paper',
                                                color: soyYo ? 'primary.contrastText' : 'text.primary',
                                                borderBottomRightRadius: soyYo ? 0 : 8,
                                                borderBottomLeftRadius: soyYo ? 8 : 0,
                                                boxShadow: 1
                                            }}>
                                                {msg.tipo === 'IMAGEN' ? (
                                                    <Box component="img" src={msg.urlArchivo} alt="Adjunto" sx={{ maxWidth: '100%', borderRadius: 1, mb: 1 }} />
                                                ) : msg.tipo !== 'TEXTO' && msg.urlArchivo ? (
                                                    <Button
                                                        variant="contained"
                                                        color={soyYo ? "secondary" : "primary"}
                                                        href={msg.urlArchivo}
                                                        target="_blank"
                                                        startIcon={<InsertDriveFile />}
                                                        sx={{ mb: 1, textTransform: 'none' }}
                                                    >
                                                        Ver Documento
                                                    </Button>
                                                ) : null}

                                                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                                                    {msg.contenido}
                                                </Typography>
                                                <Typography variant="caption" sx={{ display: 'block', mt: 0.5, opacity: 0.7, textAlign: 'right' }}>
                                                    {new Date(msg.fechaEnvio).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
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
                            <Box sx={{ p: 1, bgcolor: 'background.paper', borderTop: '1px solid', borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 1 }}>
                                {archivoAdjunto.type.startsWith('image/') ? <ImageIcon color="primary" /> : <InsertDriveFile color="primary" />}
                                <Typography variant="body2" sx={{ flex: 1 }} noWrap>{archivoAdjunto.name}</Typography>
                                <IconButton size="small" onClick={() => setArchivoAdjunto(null)}>
                                    <Close fontSize="small" />
                                </IconButton>
                            </Box>
                        )}

                        <Box sx={{ p: 2, bgcolor: 'background.default', borderTop: '1px solid', borderColor: 'divider' }}>
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
                                    sx={{ bgcolor: 'background.paper', borderRadius: 1 }}
                                />

                                <IconButton color="primary" onClick={handleSend} disabled={isSending || (!mensajeTexto.trim() && !archivoAdjunto)}>
                                    {isSending ? <CircularProgress size={24} /> : <Send />}
                                </IconButton>
                            </Stack>
                        </Box>
                    </>
                ) : (
                    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', bgcolor: 'action.hover' }}>
                        <Avatar sx={{ width: 80, height: 80, mb: 2, bgcolor: 'primary.light' }}>
                            <Send sx={{ fontSize: 40 }} />
                        </Avatar>
                        <Typography variant="h6" color="text.secondary">
                            Selecciona un contacto para iniciar la conversación
                        </Typography>
                    </Box>
                )}
            </Paper>
        </Box>
    );
};

export default ChatInstitucional;