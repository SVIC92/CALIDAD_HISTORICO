import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import AuthService from '../services/AuthService';
import {
    Alert,
    Box,
    Button,
    Container,
    Divider,
    InputAdornment,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import Avatar from '@mui/material/Avatar';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { alpha, useTheme } from '@mui/material/styles';
import { useLoadingScreen } from '../context/LoadingScreenContext';
import { getDisplayNameFromToken } from '../utils/authIdentity';
import api from '../API/axios';

const Login = () => {
    const { register, handleSubmit, formState: { errors } } = useForm();
    const [serverError, setServerError] = useState('');
    const [twoFactorRequired, setTwoFactorRequired] = useState(false);
    const [otpCode, setOtpCode] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const navigate = useNavigate();
    const { startLoading, stopLoading } = useLoadingScreen();
    const theme = useTheme();

    const isBackendSleeping = (error) => {
        const status = error?.response?.status;
        if ([502, 503, 504, 521, 522, 524].includes(status)) return true;

        if (!error?.response) {
            const message = String(error?.message || '').toLowerCase();
            if (message.includes('network')) return true;
            if (message.includes('timeout')) return true;
            if (error?.code === 'ERR_NETWORK') return true;
        }

        return false;
    };

    const wakeUpBackend = async () => {
        try {
            await api.get('/', { timeout: 12000 });
        } catch (wakeError) {
            console.info('Intento de despertar backend:', wakeError?.message || wakeError);
        }
    };

    const getRedirectByRole = (rol) => {
        if (rol === 'ROLE_ADMIN') return '/dashboard/admin';
        if (rol === 'ROLE_PROFESOR') return '/dashboard/profesor';
        if (rol === 'ROLE_ALUMNO') return '/dashboard/alumno';
        return '/dashboard';
    };

    const onSubmit = async (data) => {
        try {
            setServerError('');
            setTwoFactorRequired(false);
            setIsSubmitting(true);
            startLoading();

            const respuesta = await AuthService.login(data.email, data.password, otpCode);
            localStorage.setItem('token', respuesta.token);
            localStorage.setItem('rol', respuesta.rol);
            const nameFromToken = getDisplayNameFromToken(respuesta.token);
            localStorage.setItem('nombre', respuesta.nombre || respuesta.username || nameFromToken || data.email || 'Usuario');
            navigate(getRedirectByRole(respuesta.rol));
        } catch (err) {
            console.error('Error en login:', err);
            if (isBackendSleeping(err)) {
                setServerError('Servidor dormido en Render. Lo estamos despertando, intenta de nuevo en unos segundos.');
                await wakeUpBackend();
                return;
            }
            const body = err?.response?.data;
            if (body?.twoFactorRequired) {
                setTwoFactorRequired(true);
                if (otpCode) {
                    const mensajeOtp = 'Código 2FA incorrecto. Intenta de nuevo.';
                    setServerError(mensajeOtp);
                } else {
                    setOtpCode('');
                    setServerError('');
                }
            } else {
                const backendMessage = typeof body === 'string'
                    ? body
                    : body?.error || body?.mensaje;
                setServerError(backendMessage || 'Credenciales incorrectas. Intenta de nuevo.');
            }
        } finally {
            stopLoading();
            setIsSubmitting(false);
        }
    };

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                position: 'relative',
                overflow: 'hidden',
                px: 2,
                py: 4,
                background: `radial-gradient(circle at top left, ${alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.22 : 0.18)}, transparent 32%), radial-gradient(circle at top right, ${alpha(theme.palette.info.main, theme.palette.mode === 'dark' ? 0.2 : 0.16)}, transparent 28%), linear-gradient(135deg, ${alpha(theme.palette.background.default, theme.palette.mode === 'dark' ? 0.92 : 0.96)} 0%, ${alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.86 : 0.9)} 45%, ${alpha(theme.palette.background.default, theme.palette.mode === 'dark' ? 0.94 : 0.98)} 100%)`,
            }}
        >
            <Box
                sx={{
                    position: 'absolute',
                    inset: 0,
                    background: theme.palette.mode === 'dark'
                        ? `linear-gradient(120deg, ${alpha(theme.palette.background.paper, 0.22)}, ${alpha(theme.palette.background.paper, 0.08)})`
                        : `linear-gradient(120deg, ${alpha(theme.palette.common.white, 0.72)}, ${alpha(theme.palette.common.white, 0.32)})`,
                    backdropFilter: 'blur(2px)',
                    pointerEvents: 'none',
                }}
            />

            <Container component="main" maxWidth="lg" sx={{ position: 'relative' }}>
                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                    <Paper
                        elevation={0}
                        sx={{
                            width: '100%',
                            maxWidth: 1080,
                            display: 'grid',
                            gridTemplateColumns: { xs: '1fr', md: '1fr 1.05fr' },
                            overflow: 'hidden',
                            borderRadius: 5,
                            border: `1px solid ${alpha(theme.palette.divider, 0.28)}`,
                            boxShadow: `0 24px 80px ${alpha(theme.palette.common.black, 0.12)}`,
                            background: alpha(theme.palette.background.paper, 0.86),
                            backdropFilter: 'blur(18px)',
                        }}
                    >
                        <Box
                            sx={{
                                display: { xs: 'none', md: 'flex' },
                                flexDirection: 'column',
                                justifyContent: 'space-between',
                                p: 5,
                                color: theme.palette.primary.contrastText,
                                background: `linear-gradient(160deg, ${theme.palette.primary.dark} 0%, ${theme.palette.primary.main} 52%, ${theme.palette.info.main} 100%)`,
                                position: 'relative',
                                minHeight: 620,
                            }}
                        >
                            <Box
                                sx={{
                                    position: 'absolute',
                                    inset: 0,
                                    background: `radial-gradient(circle at top right, ${alpha(theme.palette.common.white, 0.18)}, transparent 28%), radial-gradient(circle at bottom left, ${alpha(theme.palette.common.white, 0.12)}, transparent 24%)`,
                                }}
                            />
                            <Box sx={{ position: 'relative', zIndex: 1 }}>
                                <Avatar sx={{ width: 56, height: 56, mb: 3, bgcolor: alpha(theme.palette.common.white, 0.14), color: theme.palette.common.white }}>
                                    <LockOutlinedIcon />
                                </Avatar>
                                <Typography variant="overline" sx={{ letterSpacing: 3, color: alpha(theme.palette.common.white, 0.75) }}>
                                    Portal académico
                                </Typography>
                                <Typography variant="h3" sx={{ mt: 1, fontWeight: 800, lineHeight: 1.05 }}>
                                    Accede a tu espacio de gestión con una experiencia clara y moderna.
                                </Typography>
                                <Typography sx={{ mt: 2, maxWidth: 420, color: alpha(theme.palette.common.white, 0.78), fontSize: '1rem', lineHeight: 1.7 }}>
                                    Centraliza cursos, horarios, reportes y comunicación institucional en un entorno pensado para trabajar rápido.
                                </Typography>
                            </Box>

                            <Stack spacing={1.5} sx={{ position: 'relative', zIndex: 1 }}>
                                {['Acceso seguro con soporte 2FA', 'Vista optimizada para docentes y alumnos', 'Navegación fluida y consistente'].map((item) => (
                                    <Box
                                        key={item}
                                        sx={{
                                            px: 2,
                                            py: 1.5,
                                            borderRadius: 3,
                                            background: alpha(theme.palette.common.white, 0.1),
                                            border: `1px solid ${alpha(theme.palette.common.white, 0.12)}`,
                                            color: alpha(theme.palette.common.white, 0.9),
                                            backdropFilter: 'blur(10px)',
                                        }}
                                    >
                                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                            {item}
                                        </Typography>
                                    </Box>
                                ))}
                            </Stack>
                        </Box>

                        <Box
                            sx={{
                                p: { xs: 3, sm: 4, md: 5 },
                                display: 'flex',
                                flexDirection: 'column',
                                justifyContent: 'center',
                                minHeight: { md: 620 },
                                background: `linear-gradient(180deg, ${alpha(theme.palette.background.paper, 0.98)} 0%, ${alpha(theme.palette.background.default, 0.98)} 100%)`,
                            }}
                        >
                            <Stack spacing={1.5} sx={{ mb: 3 }}>
                                <Avatar sx={{ width: 52, height: 52, bgcolor: 'primary.main', boxShadow: `0 12px 28px ${alpha(theme.palette.primary.main, 0.35)}` }}>
                                    <LockOutlinedIcon />
                                </Avatar>

                                <Box>
                                    <Typography component="h1" variant="h4" sx={{ fontWeight: 800, letterSpacing: '-0.03em' }}>
                                        Iniciar sesión
                                    </Typography>
                                    <Typography variant="body1" color="text.secondary" sx={{ mt: 1, maxWidth: 420 }}>
                                        Ingresa con tu correo institucional para continuar en el portal.
                                    </Typography>
                                </Box>
                            </Stack>

                            <Divider sx={{ mb: 3, borderColor: alpha(theme.palette.divider, 0.28) }} />

                            <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate sx={{ width: '100%' }}>
                                <Stack spacing={2.2}>
                                    <TextField
                                        required
                                        fullWidth
                                        id="email"
                                        label="Correo electrónico"
                                        autoComplete="email"
                                        autoFocus
                                        {...register('email', { required: 'El correo es obligatorio' })}
                                        error={!!errors.email}
                                        helperText={errors.email ? errors.email.message : ''}
                                        slotProps={{
                                            input: {
                                                startAdornment: (
                                                    <InputAdornment position="start">
                                                        <EmailOutlinedIcon fontSize="small" />
                                                    </InputAdornment>
                                                ),
                                            },
                                        }}
                                    />

                                    <TextField
                                        required
                                        fullWidth
                                        label="Contraseña"
                                        type="password"
                                        id="password"
                                        autoComplete="current-password"
                                        {...register('password', { required: 'La contraseña es obligatoria' })}
                                        error={!!errors.password}
                                        helperText={errors.password ? errors.password.message : ''}
                                    />

                                    {twoFactorRequired && (
                                        <TextField
                                            required
                                            fullWidth
                                            label="Código de autenticación (2FA)"
                                            value={otpCode}
                                            onChange={(e) => setOtpCode(e.target.value)}
                                            inputProps={{ maxLength: 8 }}
                                        />
                                    )}

                                    {serverError && (
                                        <Alert severity="error" sx={{ borderRadius: 2 }}>
                                            {serverError}
                                        </Alert>
                                    )}

                                    <Button
                                        type="submit"
                                        fullWidth
                                        variant="contained"
                                        disabled={isSubmitting}
                                        endIcon={<ArrowForwardRoundedIcon />}
                                        sx={{
                                            mt: 1,
                                            py: 1.5,
                                            borderRadius: 999,
                                            textTransform: 'none',
                                            fontSize: '1rem',
                                            fontWeight: 700,
                                            boxShadow: `0 16px 32px ${alpha(theme.palette.primary.main, 0.28)}`,
                                        }}
                                    >
                                        {isSubmitting ? 'Ingresando...' : 'Ingresar'}
                                    </Button>

                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ pt: 0.5 }}>
                                        <Button
                                            type="button"
                                            fullWidth
                                            variant="outlined"
                                            onClick={() => navigate('/registro')}
                                            sx={{
                                                borderRadius: 999,
                                                textTransform: 'none',
                                                py: 1.2,
                                                fontWeight: 600,
                                            }}
                                        >
                                            Crear cuenta
                                        </Button>

                                        <Button
                                            type="button"
                                            fullWidth
                                            variant="text"
                                            onClick={() => navigate('/forgot-password')}
                                            sx={{
                                                borderRadius: 999,
                                                textTransform: 'none',
                                                py: 1.2,
                                                fontWeight: 600,
                                            }}
                                        >
                                            ¿Olvidaste tu contraseña?
                                        </Button>
                                    </Stack>
                                </Stack>
                            </Box>
                        </Box>
                    </Paper>
                </Box>
            </Container>
        </Box>
    );
};

export default Login;