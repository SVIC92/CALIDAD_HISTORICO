import { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  Divider,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { ArrowBack, Security, QrCode2, VerifiedUser, Person } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import AuthService from '../services/AuthService';

const PerfilUsuario = () => {
  const navigate = useNavigate();

  const [perfil, setPerfil] = useState(null);
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(false);
  const [setupData, setSetupData] = useState(null);
  const [qrUrl, setQrUrl] = useState('');
  const [enableCode, setEnableCode] = useState('');
  const [disableCode, setDisableCode] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const qrObjectUrlRef = useRef('');

  useEffect(() => {
    const load = async () => {
      try {
        setIsLoading(true);
        setErrorMsg('');

        const [perfilRes, statusRes] = await Promise.all([
          AuthService.usuarioConectado(),
          AuthService.twoFactorStatus(),
        ]);

        setPerfil(perfilRes || null);
        setTwoFactorEnabled(Boolean(statusRes?.enabled));
      } catch (error) {
        const backendMessage = error?.response?.data?.error || error?.response?.data?.mensaje || error?.message;
        setErrorMsg(backendMessage || 'No se pudo cargar la información del perfil.');
      } finally {
        setIsLoading(false);
      }
    };

    load();

    return () => {
      if (qrObjectUrlRef.current) {
        URL.revokeObjectURL(qrObjectUrlRef.current);
      }
    };
  }, []);

  const handleGenerateSetup = async () => {
    try {
      setIsSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');

      const setup = await AuthService.twoFactorSetup();
      setSetupData(setup || null);

      const qrBlob = await AuthService.twoFactorQr(280);
      if (qrObjectUrlRef.current) {
        URL.revokeObjectURL(qrObjectUrlRef.current);
      }

      const objectUrl = URL.createObjectURL(qrBlob);
      qrObjectUrlRef.current = objectUrl;
      setQrUrl(objectUrl);
      setSuccessMsg('Configuración 2FA generada. Escanea el QR y valida con tu código.');
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data?.mensaje || error?.message;
      setErrorMsg(backendMessage || 'No se pudo generar la configuración de 2FA.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEnable = async () => {
    if (!enableCode.trim()) {
      setErrorMsg('Ingresa el código de autenticación para activar 2FA.');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');

      const response = await AuthService.twoFactorEnable(enableCode.trim());
      setTwoFactorEnabled(Boolean(response?.enabled));
      setEnableCode('');
      setSuccessMsg(response?.mensaje || 'Two factor activado correctamente.');
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data?.mensaje || error?.message;
      setErrorMsg(backendMessage || 'No se pudo activar 2FA.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDisable = async () => {
    if (!disableCode.trim()) {
      setErrorMsg('Ingresa el código de autenticación para desactivar 2FA.');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');

      const response = await AuthService.twoFactorDisable(disableCode.trim());
      setTwoFactorEnabled(Boolean(response?.enabled));
      setDisableCode('');
      setSetupData(null);
      setQrUrl('');
      if (qrObjectUrlRef.current) {
        URL.revokeObjectURL(qrObjectUrlRef.current);
        qrObjectUrlRef.current = '';
      }
      setSuccessMsg(response?.mensaje || 'Two factor desactivado correctamente.');
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data?.mensaje || error?.message;
      setErrorMsg(backendMessage || 'No se pudo desactivar 2FA.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 3, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/dashboard')}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Perfil de Usuario
        </Typography>
        <Chip
          icon={twoFactorEnabled ? <VerifiedUser /> : <Security />}
          label={twoFactorEnabled ? '2FA Activo' : '2FA Inactivo'}
          color={twoFactorEnabled ? 'success' : 'default'}
          variant={twoFactorEnabled ? 'filled' : 'outlined'}
        />
      </Stack>

      {errorMsg && <Alert severity="error" sx={{ mb: 2 }}>{errorMsg}</Alert>}
      {successMsg && <Alert severity="success" sx={{ mb: 2 }}>{successMsg}</Alert>}

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>Datos de la Cuenta</Typography>
        {isLoading ? (
          <Typography color="text.secondary">Cargando datos...</Typography>
        ) : (
          <Stack direction="row" spacing={3} alignItems="center">
            <Avatar sx={{ width: 80, height: 80, bgcolor: 'primary.main' }}>
              <Person sx={{ fontSize: 50 }} />
            </Avatar>
            <Stack spacing={1}>
              <Typography><strong>Nombre:</strong> {perfil?.nombre || '-'}</Typography>
              <Typography><strong>Email:</strong> {perfil?.email || '-'}</Typography>
              <Typography><strong>Rol:</strong> {perfil?.rol || '-'}</Typography>
            </Stack>
          </Stack>
        )}
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>Seguridad - Two Factor Authentication</Typography>

        {!twoFactorEnabled ? (
          <Stack spacing={2}>
            <Typography variant="body2" color="text.secondary">
              Para activar 2FA, genera tu configuración, escanea el QR con Google Authenticator o similar y confirma con el código de 6 dígitos.
            </Typography>

            <Button
              variant="outlined"
              startIcon={<QrCode2 />}
              onClick={handleGenerateSetup}
              disabled={isSubmitting}
            >
              Generar configuración 2FA
            </Button>

            {qrUrl && (
              <Box sx={{ textAlign: 'center' }}>
                <img src={qrUrl} alt="QR 2FA" style={{ width: 220, height: 220, borderRadius: 8 }} />
              </Box>
            )}

            {setupData?.secret && (
              <Typography variant="caption" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
                Clave manual: {setupData.secret}
              </Typography>
            )}

            <Divider />

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField
                fullWidth
                label="Código de autenticación"
                placeholder="123456"
                value={enableCode}
                onChange={(e) => setEnableCode(e.target.value)}
                inputProps={{ maxLength: 8 }}
              />
              <Button variant="contained" onClick={handleEnable} disabled={isSubmitting || !enableCode.trim()}>
                Activar 2FA
              </Button>
            </Stack>
          </Stack>
        ) : (
          <Stack spacing={2}>
            <Typography variant="body2" color="text.secondary">
              2FA está activo. Para desactivarlo, confirma con un código válido de tu aplicación autenticadora.
            </Typography>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <TextField
                fullWidth
                label="Código de autenticación"
                placeholder="123456"
                value={disableCode}
                onChange={(e) => setDisableCode(e.target.value)}
                inputProps={{ maxLength: 8 }}
              />
              <Button color="error" variant="contained" onClick={handleDisable} disabled={isSubmitting || !disableCode.trim()}>
                Desactivar 2FA
              </Button>
            </Stack>
          </Stack>
        )}
      </Paper>
    </Box>
  );
};

export default PerfilUsuario;
