import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
    Container,
    Box,
    Typography,
    TextField,
    Button,
    Paper,
    Alert,
} from '@mui/material';
import PersonAddAlt1Icon from '@mui/icons-material/PersonAddAlt1';
import Avatar from '@mui/material/Avatar';
import PortalService from '../services/PortalService';
import { useLoadingScreen } from '../context/LoadingScreenContext';

const Registro = () => {
    const {
        register,
        handleSubmit,
        watch,
        formState: { errors },
    } = useForm();

    const [serverError, setServerError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const navigate = useNavigate();
    const { startLoading, stopLoading } = useLoadingScreen();

    const passwordValue = watch('password');

    const onSubmit = async (data) => {
        try {
            setServerError('');
            setSuccessMessage('');
            setIsSubmitting(true);
            startLoading();

            await PortalService.registro({
                nombre: data.nombre,
                email: data.email,
                password: data.password,
                password2: data.password2,
            });

            setSuccessMessage('Usuario registrado correctamente. Ahora puedes iniciar sesión.');
            setTimeout(() => {
                startLoading();
                navigate('/');
            }, 1200);
        } catch (err) {
            console.error('Error en registro:', err);
            const backendMessage = typeof err?.response?.data === 'string'
                ? err.response.data
                : err?.response?.data?.error;
            setServerError(backendMessage || 'No se pudo registrar el usuario.');
        } finally {
            stopLoading();
            setIsSubmitting(false);
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
                <Paper
                    elevation={3}
                    sx={{
                        padding: 4,
                        width: '100%',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        borderRadius: 2,
                    }}
                >
                    <Avatar sx={{ m: 1, bgcolor: 'primary.main' }}>
                        <PersonAddAlt1Icon />
                    </Avatar>

                    <Typography component="h1" variant="h5" sx={{ mb: 1 }}>
                        Crear Cuenta
                    </Typography>

                    <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                        Regístrate en el portal de Gestión de Cursos
                    </Typography>

                    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate sx={{ mt: 1, width: '100%' }}>
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            label="Nombre completo"
                            autoFocus
                            {...register('nombre', {
                                required: 'El nombre es obligatorio',
                                minLength: {
                                    value: 3,
                                    message: 'El nombre debe tener al menos 3 caracteres',
                                },
                            })}
                            error={!!errors.nombre}
                            helperText={errors.nombre ? errors.nombre.message : ''}
                        />

                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            label="Correo electrónico"
                            autoComplete="email"
                            {...register('email', {
                                required: 'El correo es obligatorio',
                                pattern: {
                                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                                    message: 'Ingresa un correo válido',
                                },
                            })}
                            error={!!errors.email}
                            helperText={errors.email ? errors.email.message : ''}
                        />

                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            label="Contraseña"
                            type="password"
                            autoComplete="new-password"
                            {...register('password', {
                                required: 'La contraseña es obligatoria',
                                minLength: {
                                    value: 6,
                                    message: 'La contraseña debe tener al menos 6 caracteres',
                                },
                            })}
                            error={!!errors.password}
                            helperText={errors.password ? errors.password.message : ''}
                        />

                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            label="Repetir contraseña"
                            type="password"
                            autoComplete="new-password"
                            {...register('password2', {
                                required: 'Debes repetir la contraseña',
                                validate: (value) => value === passwordValue || 'Las contraseñas no coinciden',
                            })}
                            error={!!errors.password2}
                            helperText={errors.password2 ? errors.password2.message : ''}
                        />

                        {serverError && (
                            <Alert severity="error" sx={{ mt: 2 }}>
                                {serverError}
                            </Alert>
                        )}

                        {successMessage && (
                            <Alert severity="success" sx={{ mt: 2 }}>
                                {successMessage}
                            </Alert>
                        )}

                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            disabled={isSubmitting}
                            sx={{ mt: 3, mb: 1, py: 1.5 }}
                        >
                            {isSubmitting ? 'Registrando...' : 'Registrarme'}
                        </Button>

                        <Button
                            type="button"
                            fullWidth
                            variant="text"
                            onClick={() => navigate('/')}
                        >
                            Volver al Login
                        </Button>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default Registro;
