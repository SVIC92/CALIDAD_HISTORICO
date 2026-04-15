import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Container,
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import LockResetIcon from '@mui/icons-material/LockReset';
import AuthService from '../services/AuthService';
import { useLoadingScreen } from '../context/LoadingScreenContext';

const ForgotPassword = () => {
  const navigate = useNavigate();
  const { startLoading, stopLoading } = useLoadingScreen();

  const [email, setEmail] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const getBackendMessage = (error) => {
    const data = error?.response?.data;
    if (typeof data === 'string') return data;
    return data?.error || data?.mensaje || error?.message || '';
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!email.trim()) {
      setErrorMsg('Ingresa un correo electrónico.');
      return;
    }

    const emailValue = email.trim().toLowerCase();
    const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue);
    if (!isValidEmail) {
      setErrorMsg('Ingresa un correo válido.');
      return;
    }

    try {
      setSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');
      startLoading();

      const response = await AuthService.solicitarRecuperacion(emailValue);
      setSuccessMsg(response?.mensaje || 'Si el correo existe, recibirás instrucciones para reestablecer tu contraseña.');
    } catch (error) {
      const backendMessage = getBackendMessage(error);
      setErrorMsg(backendMessage || 'No se pudo procesar la solicitud.');
    } finally {
      stopLoading();
      setSubmitting(false);
    }
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Paper elevation={3} sx={{ p: 4, width: '100%', borderRadius: 2 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <Avatar sx={{ m: 1, bgcolor: 'primary.main' }}>
              <LockResetIcon />
            </Avatar>
            <Typography component="h1" variant="h5" sx={{ mb: 1 }}>
              Recuperar Contraseña
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3, textAlign: 'center' }}>
              Ingresa tu correo para recibir el enlace de reestablecimiento.
            </Typography>

            <Box component="form" onSubmit={handleSubmit} sx={{ width: '100%' }}>
              <TextField
                fullWidth
                required
                type="email"
                label="Correo electrónico"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />

              {errorMsg && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  {errorMsg}
                </Alert>
              )}

              {successMsg && (
                <Alert severity="success" sx={{ mt: 2 }}>
                  {successMsg}
                </Alert>
              )}

              <Button
                type="submit"
                fullWidth
                variant="contained"
                disabled={submitting}
                sx={{ mt: 3, mb: 1, py: 1.3 }}
              >
                {submitting ? 'Enviando...' : 'Enviar Enlace'}
              </Button>

              <Button fullWidth variant="text" onClick={() => navigate('/')}>
                Volver al Login
              </Button>
            </Box>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default ForgotPassword;
