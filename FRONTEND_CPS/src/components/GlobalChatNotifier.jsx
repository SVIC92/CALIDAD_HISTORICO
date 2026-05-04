import { useEffect, useState, useRef } from 'react';
import { Snackbar, Alert, Typography } from '@mui/material';
import { useLocation, useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
// Aseguramos que importe tu instancia de axios correctamente
import api from '../API/axios';
import AuthService from '../services/AuthService';

const getWsUrl = () => {
    const apiUrl = api.defaults.baseURL;
    return apiUrl.replace('/api', '') + '/ws-chat';
};

const GlobalChatNotifier = () => {
    const location = useLocation();
    const navigate = useNavigate();

    const [open, setOpen] = useState(false);
    const [mensajeInfo, setMensajeInfo] = useState(null);
    const stompClientRef = useRef(null);

    const isChatPage = location.pathname.includes('/modulo/chat');

    // No olvides importar AuthService arriba:
    // import AuthService from '../services/AuthService';

    useEffect(() => {
        if (isChatPage) {
            if (stompClientRef.current) stompClientRef.current.deactivate();
            return;
        }

        const conectarWebSocket = async () => {
            const token = localStorage.getItem('token');
            if (!token) return;

            try {
                // 1. Obtenemos nuestro propio perfil para saber nuestro ID
                const miPerfil = await AuthService.usuarioConectado();

                const client = new Client({
                    webSocketFactory: () => new SockJS(getWsUrl()),
                    connectHeaders: { Authorization: `Bearer ${token}` },
                    reconnectDelay: 5000,
                    onConnect: () => {
                        console.log("✅ Notificador conectado. Escuchando a: ", miPerfil.id);

                        // 2. Nos suscribimos DIRECTO a nuestra cola personal
                        client.subscribe(`/queue/mensajes/${miPerfil.id}`, (msg) => {
                            const nuevoMensaje = JSON.parse(msg.body);
                            setMensajeInfo(nuevoMensaje);
                            setOpen(true);
                        });
                    }
                });

                client.activate();
                stompClientRef.current = client;
            } catch (error) {
                console.error("Error conectando notificador:", error);
            }
        };

        conectarWebSocket();

        return () => {
            if (stompClientRef.current) stompClientRef.current.deactivate();
        };
    }, [isChatPage]);

    const handleClose = (event, reason) => {
        if (reason === 'clickaway') return;
        setOpen(false);
    };

    const handleClickAlerta = () => {
        setOpen(false);
        navigate('/modulo/chat');
    };

    return (
        <Snackbar
            open={open}
            autoHideDuration={6000}
            onClose={handleClose}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            sx={{ zIndex: 9999 }} // <-- ¡FORZAMOS QUE ESTÉ POR ENCIMA DE TODO!
        >
            <Alert
                onClose={handleClose}
                severity="info"
                onClick={handleClickAlerta}
                sx={{
                    width: '100%',
                    cursor: 'pointer',
                    bgcolor: 'primary.main',
                    color: 'white',
                    '& .MuiAlert-icon': { color: 'white' },
                    boxShadow: 6 // Sombra más fuerte
                }}
            >
                <Typography variant="subtitle2" fontWeight="bold">
                    Nuevo mensaje de {mensajeInfo?.emisor?.nombre || 'Usuario'}
                </Typography>
                <Typography variant="body2" sx={{
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    maxWidth: 250
                }}>
                    {mensajeInfo?.tipo === 'TEXTO' ? mensajeInfo.contenido : '📎 Archivo adjunto'}
                </Typography>
            </Alert>
        </Snackbar>
    );
};

export default GlobalChatNotifier;