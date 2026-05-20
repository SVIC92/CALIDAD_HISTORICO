import React, { useEffect, useRef, useState } from 'react';
import { Box, Typography, IconButton, Tooltip } from '@mui/material';
import { alpha, useTheme } from '@mui/material/styles';
import ClosedCaptionIcon from '@mui/icons-material/ClosedCaption';
import ClosedCaptionOffIcon from '@mui/icons-material/ClosedCaptionOff';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import api from '../API/axios';

const getWsUrl = () => {
    const apiUrl = api.defaults.baseURL || 'http://localhost:8080/api';
    return apiUrl.replace(/\/api\/?$/, '') + '/ws-chat';
};

const DEFAULT_LOCAL_PYTHON_WS_BASE_URL = 'ws://localhost:8000';
const DEFAULT_PROD_PYTHON_WS_BASE_URL = 'wss://svic92-subtitulos-vosk-api.hf.space';

const getPythonWsUrl = () => {
    const envBaseUrl = import.meta.env.VITE_PYTHON_WS_BASE_URL;
    const fallbackBaseUrl = import.meta.env.DEV
        ? DEFAULT_LOCAL_PYTHON_WS_BASE_URL
        : DEFAULT_PROD_PYTHON_WS_BASE_URL;

    return String(envBaseUrl || fallbackBaseUrl).trim().replace(/\/+$/, '');
};

const buildSubtitleText = (mensaje) => {
    if (!mensaje) return '';

    if (typeof mensaje === 'string') {
        return mensaje.trim();
    }

    return String(mensaje.texto || mensaje.contenido || mensaje.transcripcion || '').trim();
};

const SubtitulosVideoconferencia = ({ salaUuid, usuarioId }) => {
    const [activado, setActivado] = useState(false);
    const [subtituloActual, setSubtituloActual] = useState('');
    const theme = useTheme();

    const stompClientRef = useRef(null);
    const wsPythonRef = useRef(null);
    const audioContextRef = useRef(null);
    const streamRef = useRef(null);
    const sourceRef = useRef(null);
    const processorRef = useRef(null);
    const clearSubtitleTimerRef = useRef(null);

    useEffect(() => {
        if (!salaUuid) return undefined;

        const token = localStorage.getItem('token');
        if (!token) return undefined;

        const client = new Client({
            webSocketFactory: () => new SockJS(getWsUrl()),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5000,
            onConnect: () => {
                client.subscribe(`/topic/sala/${salaUuid}/subtitulos`, (mensaje) => {
                    try {
                        const data = JSON.parse(mensaje.body);
                        const texto = buildSubtitleText(data);

                        if (!texto) return;

                        setSubtituloActual(texto);

                        if (clearSubtitleTimerRef.current) {
                            window.clearTimeout(clearSubtitleTimerRef.current);
                        }

                        clearSubtitleTimerRef.current = window.setTimeout(() => {
                            setSubtituloActual('');
                        }, 4500);
                    } catch (error) {
                        console.error('Error al procesar subtítulo recibido:', error);
                    }
                });
            },
            onStompError: (frame) => {
                console.error('Error STOMP en subtítulos:', frame.headers['message'] || frame.body);
            },
            onWebSocketError: (event) => {
                console.error('Error de WebSocket en subtítulos:', event);
            },
        });

        client.activate();
        stompClientRef.current = client;

        return () => {
            if (clearSubtitleTimerRef.current) {
                window.clearTimeout(clearSubtitleTimerRef.current);
                clearSubtitleTimerRef.current = null;
            }

            client.deactivate();
            stompClientRef.current = null;
        };
    }, [salaUuid]);

    const detenerGrabacion = async () => {
        setActivado(false);

        if (processorRef.current) {
            try {
                processorRef.current.disconnect();
            } catch {
                // Ignoramos cierres transitorios.
            }
            processorRef.current = null;
        }

        if (sourceRef.current) {
            try {
                sourceRef.current.disconnect();
            } catch {
                // Ignoramos cierres transitorios.
            }
            sourceRef.current = null;
        }

        if (audioContextRef.current) {
            try {
                await audioContextRef.current.close();
            } catch {
                // Ignoramos cierres transitorios.
            }
            audioContextRef.current = null;
        }

        if (streamRef.current) {
            streamRef.current.getTracks().forEach((track) => track.stop());
            streamRef.current = null;
        }

        if (wsPythonRef.current) {
            try {
                wsPythonRef.current.close();
            } catch {
                // Ignoramos cierres transitorios.
            }
            wsPythonRef.current = null;
        }
    };

    const iniciarGrabacion = async () => {
        if (!salaUuid || !usuarioId) {
            console.error('No se puede iniciar la transcripción sin salaUuid y usuarioId.');
            return;
        }

        if (!navigator.mediaDevices?.getUserMedia) {
            console.error('El navegador no soporta acceso al micrófono.');
            return;
        }

        try {
            const audioStream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 });
            const source = audioContext.createMediaStreamSource(audioStream);
            const processor = audioContext.createScriptProcessor(4096, 1, 1);
            const silencio = audioContext.createGain();
            silencio.gain.value = 0;

            const pythonWsUrl = `${getPythonWsUrl()}/ws/transcribir/${encodeURIComponent(salaUuid)}/${encodeURIComponent(usuarioId)}`;
            const socketPython = new WebSocket(pythonWsUrl);

            socketPython.binaryType = 'arraybuffer';

            socketPython.onopen = () => {
                streamRef.current = audioStream;
                audioContextRef.current = audioContext;
                sourceRef.current = source;
                processorRef.current = processor;
                wsPythonRef.current = socketPython;
                setActivado(true);

                processor.onaudioprocess = (evento) => {
                    const inputData = evento.inputBuffer.getChannelData(0);
                    const pcm16 = new Int16Array(inputData.length);

                    for (let i = 0; i < inputData.length; i += 1) {
                        const muestra = Math.max(-1, Math.min(1, inputData[i]));
                        pcm16[i] = muestra < 0 ? muestra * 0x8000 : muestra * 0x7FFF;
                    }

                    if (socketPython.readyState === WebSocket.OPEN) {
                        socketPython.send(pcm16.buffer);
                    }
                };

                source.connect(processor);
                processor.connect(silencio);
                silencio.connect(audioContext.destination);
            };

            socketPython.onerror = (error) => {
                console.error('Error en el WebSocket del microservicio de transcripción:', error);
            };

            socketPython.onclose = () => {
                if (wsPythonRef.current === socketPython) {
                    wsPythonRef.current = null;
                }
            };
        } catch (error) {
            console.error('Error al iniciar la transcripción:', error);
            await detenerGrabacion();
        }
    };

    const toggleSubtitulos = async () => {
        if (activado) {
            await detenerGrabacion();
            return;
        }

        await iniciarGrabacion();
    };

    useEffect(() => {
        return () => {
            detenerGrabacion();
        };
    }, []);

    return (
        <Box sx={{ position: 'relative', width: '100%', height: '100%', pointerEvents: 'none' }}>
            <Box sx={{ position: 'absolute', top: 16, right: 16, zIndex: 10, pointerEvents: 'auto' }}>
                <Tooltip title={activado ? 'Desactivar transcripción' : 'Activar transcripción'}>
                    <IconButton
                        onClick={toggleSubtitulos}
                        color={activado ? 'primary' : 'default'}
                        sx={{
                            bgcolor: activado
                                ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.28 : 0.14)
                                : theme.palette.background.paper,
                            color: activado ? theme.palette.primary.main : theme.palette.text.primary,
                            border: '1px solid',
                            borderColor: activado ? alpha(theme.palette.primary.main, 0.35) : theme.palette.divider,
                            boxShadow: theme.shadows[2],
                            transition: theme.transitions.create(['background-color', 'box-shadow', 'transform'], {
                                duration: theme.transitions.duration.shortest,
                            }),
                            '&:hover': {
                                bgcolor: activado
                                    ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.4 : 0.2)
                                    : theme.palette.background.paper,
                                boxShadow: theme.shadows[4],
                                transform: 'translateY(-1px)',
                            },
                        }}
                    >
                        {activado ? <ClosedCaptionIcon /> : <ClosedCaptionOffIcon />}
                    </IconButton>
                </Tooltip>
            </Box>

            {subtituloActual && (
                <Box
                    sx={{
                        position: 'absolute',
                        bottom: '10%',
                        left: '50%',
                        transform: 'translateX(-50%)',
                        backgroundColor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.72 : 0.88),
                        padding: '10px 18px',
                        borderRadius: '10px',
                        maxWidth: '80%',
                        textAlign: 'center',
                        zIndex: 20,
                        pointerEvents: 'none'
                    }}
                >
                    <Typography
                        variant="h6"
                        sx={{
                            color: theme.palette.text.primary,
                            textShadow: theme.palette.mode === 'dark' ? '1px 1px 2px rgba(0,0,0,0.85)' : 'none',
                            fontWeight: 500,
                            lineHeight: 1.35
                        }}
                    >
                        {subtituloActual}
                    </Typography>
                </Box>
            )}
        </Box>
    );
};

export default SubtitulosVideoconferencia;