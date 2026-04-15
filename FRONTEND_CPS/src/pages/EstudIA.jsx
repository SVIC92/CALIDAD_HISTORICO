import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Avatar,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { ArrowBack, Send, SmartToy } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import IaService from '../services/IaService';

const roleLabelByCode = {
  ROLE_ADMIN: 'Administrador',
  ROLE_PROFESOR: 'Profesor',
  ROLE_ALUMNO: 'Alumno',
};

const formatDateTime = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleString();
};

const createMsg = (type, text, fecha) => ({
  id: `${type}-${Date.now()}-${Math.random().toString(36).slice(2)}`,
  type,
  text,
  fecha,
});

const EstudIA = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol') || 'ROLE_ALUMNO';
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [loadingHistorial, setLoadingHistorial] = useState(true);
  const [sending, setSending] = useState(false);
  const bottomRef = useRef(null);

  const rolLabel = useMemo(() => roleLabelByCode[rol] || 'Usuario', [rol]);

  useEffect(() => {
    const cargarHistorial = async () => {
      try {
        setLoadingHistorial(true);
        setErrorMsg('');

        const historial = await IaService.obtenerUltimoHistorial();

        if (historial?.ultimoMensaje && historial?.ultimaRespuesta) {
          setMessages([
            createMsg('user', historial.ultimoMensaje, historial.fechaActualizacion),
            createMsg('assistant', historial.ultimaRespuesta, historial.fechaActualizacion),
          ]);
        } else {
          setMessages([
            createMsg(
              'assistant',
              `Hola, soy EstudIA. Estoy listo para ayudarte con consultas de ${rolLabel.toLowerCase()}.`,
              new Date().toISOString(),
            ),
          ]);
        }
      } catch (error) {
        const backendMessage = error?.response?.data?.error || error?.response?.data || error?.message;
        setErrorMsg(backendMessage || 'No se pudo cargar el historial de conversación.');
        setMessages([
          createMsg(
            'assistant',
            `Hola, soy EstudIA. Puedes empezar una nueva conversación para el rol ${rolLabel.toLowerCase()}.`,
            new Date().toISOString(),
          ),
        ]);
      } finally {
        setLoadingHistorial(false);
      }
    };

    cargarHistorial();
  }, [rolLabel]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, sending]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || sending) return;

    const userMsg = createMsg('user', text, new Date().toISOString());
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setSending(true);
    setErrorMsg('');

    try {
      const result = await IaService.enviarMensaje(text, rol);
      const assistantMsg = createMsg(
        'assistant',
        result?.respuesta || 'No hubo respuesta de EstudIA.',
        result?.fecha || new Date().toISOString(),
      );
      setMessages((prev) => [...prev, assistantMsg]);
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data || error?.message;
      setErrorMsg(backendMessage || 'No se pudo enviar tu consulta a EstudIA.');
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSend();
    }
  };

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 2, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/modulo/ia')}>
          Volver al IAHub
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Chat IA
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Rol activo: {rolLabel}
        </Typography>
      </Stack>

      {errorMsg && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {String(errorMsg)}
        </Alert>
      )}

      <Paper sx={{ p: 2, height: '65vh', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ flex: 1, overflowY: 'auto', p: 1, borderRadius: 1, bgcolor: 'background.default' }}>
          {loadingHistorial ? (
            <Stack direction="row" spacing={1} sx={{ p: 2, justifyContent: 'center', alignItems: 'center' }}>
              <CircularProgress size={20} />
              <Typography variant="body2" color="text.secondary">Cargando conversación...</Typography>
            </Stack>
          ) : (
            messages.map((msg) => (
              <Box
                key={msg.id}
                sx={{
                  display: 'flex',
                  justifyContent: msg.type === 'user' ? 'flex-end' : 'flex-start',
                  mb: 1.5,
                }}
              >
                <Box
                  sx={{
                    maxWidth: '80%',
                    px: 1.5,
                    py: 1,
                    borderRadius: 2,
                    bgcolor: msg.type === 'user' ? 'primary.main' : 'background.paper',
                    color: msg.type === 'user' ? 'primary.contrastText' : 'text.primary',
                    border: msg.type === 'assistant' ? (theme) => `1px solid ${theme.palette.divider}` : 'none',
                  }}
                >
                  {msg.type === 'assistant' && (
                    <Stack direction="row" spacing={1} sx={{ mb: 0.5, alignItems: 'center' }}>
                      <Avatar sx={{ width: 20, height: 20, bgcolor: 'secondary.main' }}>
                        <SmartToy sx={{ fontSize: 14 }} />
                      </Avatar>
                      <Typography variant="caption" color="text.secondary">EstudIA</Typography>
                    </Stack>
                  )}
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {msg.text}
                  </Typography>
                  <Typography
                    variant="caption"
                    sx={{
                      display: 'block',
                      mt: 0.5,
                      opacity: 0.8,
                      textAlign: msg.type === 'user' ? 'right' : 'left',
                    }}
                  >
                    {formatDateTime(msg.fecha)}
                  </Typography>
                </Box>
              </Box>
            ))
          )}

          {sending && (
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', px: 1, py: 0.5 }}>
              <CircularProgress size={16} />
              <Typography variant="caption" color="text.secondary">
                EstudIA está escribiendo...
              </Typography>
            </Stack>
          )}
          <div ref={bottomRef} />
        </Box>

        <Stack direction="row" spacing={1.2} sx={{ mt: 1.5 }}>
          <TextField
            fullWidth
            multiline
            maxRows={3}
            placeholder="Escribe tu consulta..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={sending}
          />
          <Button
            variant="contained"
            endIcon={<Send />}
            onClick={handleSend}
            disabled={sending || !input.trim()}
          >
            Enviar
          </Button>
        </Stack>
      </Paper>
    </Box>
  );
};

export default EstudIA;
