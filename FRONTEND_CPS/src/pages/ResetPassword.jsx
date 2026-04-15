import { useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
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
import KeyIcon from '@mui/icons-material/Key';
import AuthService from '../services/AuthService';
import { useLoadingScreen } from '../context/LoadingScreenContext';

const ResetPassword = () => {
  const navigate = useNavigate();
  const { startLoading, stopLoading } = useLoadingScreen();
  const [searchParams] = useSearchParams();

  const tokenFromUrl = useMemo(() => searchParams.get('token') || '', [searchParams]);

  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
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

    if (!tokenFromUrl.trim()) {
      setErrorMsg('El token de recuperación es obligatorio.');
      return;
    }

    if (!password || !password2) {
      setErrorMsg('Debes completar ambas contraseñas.');
      return;
    }

    if (password.length <= 5) {
      setErrorMsg('La contraseña debe tener más de 5 caracteres.');
      return;
    }

    if (password !== password2) {
      setErrorMsg('Las contraseñas ingresadas deben ser iguales.');
      return;
    }

    try {
      setSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');
      startLoading();

      const response = await AuthService.reestablecerPassword({
        token: tokenFromUrl.trim(),
        password,
        password2,
      });

      setSuccessMsg(response?.mensaje || 'Contraseña actualizada correctamente.');
      setPassword('');
      setPassword2('');
    } catch (error) {
      const backendMessage = getBackendMessage(error);
      setErrorMsg(backendMessage || 'No se pudo reestablecer la contraseña.');
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
              <KeyIcon />
            </Avatar>
            <Typography component="h1" variant="h5" sx={{ mb: 1 }}>
              Reestablecer Contraseña
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3, textAlign: 'center' }}>
              Define tu nueva contraseña usando el token recibido.
            </Typography>

            {!tokenFromUrl && (
              <Alert severity="error" sx={{ width: '100%', mb: 2 }}>
                Enlace inválido: falta el token de recuperación. Abre el enlace enviado a tu correo o solicita uno nuevo.
              </Alert>
            )}

            <Box component="form" onSubmit={handleSubmit} sx={{ width: '100%' }}>
              <TextField
                fullWidth
                required
                type="password"
                label="Nueva contraseña"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                sx={{ mb: 2 }}
                disabled={!tokenFromUrl}
              />

              <TextField
                fullWidth
                required
                type="password"
                label="Repetir nueva contraseña"
                value={password2}
                onChange={(e) => setPassword2(e.target.value)}
                disabled={!tokenFromUrl}
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
                disabled={submitting || !tokenFromUrl}
                sx={{ mt: 3, mb: 1, py: 1.3 }}
              >
                {submitting ? 'Actualizando...' : 'Actualizar Contraseña'}
              </Button>

              <Button fullWidth variant="text" onClick={() => navigate('/')}>
                Ir al Login
              </Button>
            </Box>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default ResetPassword;
