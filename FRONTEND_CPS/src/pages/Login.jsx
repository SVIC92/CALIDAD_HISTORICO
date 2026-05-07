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
import { useLoadingScreen } from '../context/LoadingScreenContext';
import { getDisplayNameFromToken } from '../utils/authIdentity';

const Login = () => {
    const { register, handleSubmit, formState: { errors } } = useForm();
    const [serverError, setServerError] = useState('');
    const [twoFactorRequired, setTwoFactorRequired] = useState(false);
    const [otpCode, setOtpCode] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const navigate = useNavigate();
    const { startLoading, stopLoading } = useLoadingScreen();

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
            const body = err?.response?.data;
            if (body?.twoFactorRequired) {
                setTwoFactorRequired(true);
                setServerError(body?.mensaje || 'Debes ingresar el código de autenticación (2FA).');
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
                background: 'radial-gradient(circle at top left, rgba(37, 99, 235, 0.16), transparent 32%), radial-gradient(circle at top right, rgba(14, 165, 233, 0.18), transparent 28%), linear-gradient(135deg, #f8fbff 0%, #eef4ff 45%, #f6f8fc 100%)',
            }}
        >
            <Box
                sx={{
                    position: 'absolute',
                    inset: 0,
                    background: 'linear-gradient(120deg, rgba(255,255,255,0.72), rgba(255,255,255,0.32))',
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
                            border: '1px solid rgba(148, 163, 184, 0.18)',
                            boxShadow: '0 24px 80px rgba(15, 23, 42, 0.14)',
                            background: 'rgba(255, 255, 255, 0.88)',
                            backdropFilter: 'blur(18px)',
                        }}
                    >
                        <Box
                            sx={{
                                display: { xs: 'none', md: 'flex' },
                                flexDirection: 'column',
                                justifyContent: 'space-between',
                                p: 5,
                                color: '#f8fafc',
                                background: 'linear-gradient(160deg, #0f172a 0%, #1d4ed8 52%, #0284c7 100%)',
                                position: 'relative',
                                minHeight: 620,
                            }}
                        >
                            <Box
                                sx={{
                                    position: 'absolute',
                                    inset: 0,
                                    background: 'radial-gradient(circle at top right, rgba(255,255,255,0.18), transparent 28%), radial-gradient(circle at bottom left, rgba(255,255,255,0.12), transparent 24%)',
                                }}
                            />
                            <Box sx={{ position: 'relative', zIndex: 1 }}>
                                <Avatar sx={{ width: 56, height: 56, mb: 3, bgcolor: 'rgba(255,255,255,0.14)', color: '#fff' }}>
                                    <LockOutlinedIcon />
                                </Avatar>
                                <Typography variant="overline" sx={{ letterSpacing: 3, color: 'rgba(255,255,255,0.75)' }}>
                                    Portal académico
                                </Typography>
                                <Typography variant="h3" sx={{ mt: 1, fontWeight: 800, lineHeight: 1.05 }}>
                                    Accede a tu espacio de gestión con una experiencia clara y moderna.
                                </Typography>
                                <Typography sx={{ mt: 2, maxWidth: 420, color: 'rgba(255,255,255,0.78)', fontSize: '1rem', lineHeight: 1.7 }}>
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
                                            background: 'rgba(255,255,255,0.1)',
                                            border: '1px solid rgba(255,255,255,0.12)',
                                            color: 'rgba(255,255,255,0.9)',
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
                                background: 'linear-gradient(180deg, rgba(255,255,255,0.98) 0%, rgba(248,250,252,0.98) 100%)',
                            }}
                        >
                            <Stack spacing={1.5} sx={{ mb: 3 }}>
                                <Avatar sx={{ width: 52, height: 52, bgcolor: 'primary.main', boxShadow: '0 12px 28px rgba(37, 99, 235, 0.35)' }}>
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

                            <Divider sx={{ mb: 3, borderColor: 'rgba(148, 163, 184, 0.18)' }} />

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
                                        InputProps={{
                                            startAdornment: (
                                                <InputAdornment position="start">
                                                    <EmailOutlinedIcon fontSize="small" />
                                                </InputAdornment>
                                            ),
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
                                            boxShadow: '0 16px 32px rgba(37, 99, 235, 0.28)',
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