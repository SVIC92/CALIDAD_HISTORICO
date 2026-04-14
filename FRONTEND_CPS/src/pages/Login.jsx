import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import AuthService from '../services/AuthService';
import { 
    Container, 
    Box, 
    Typography, 
    TextField, 
    Button, 
    Paper, 
    Alert 
} from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import Avatar from '@mui/material/Avatar';
import { useLoadingScreen } from '../context/LoadingScreenContext';
import { getDisplayNameFromToken } from '../utils/authIdentity';

const Login = () => {
    const { register, handleSubmit, formState: { errors } } = useForm();
    const [serverError, setServerError] = useState('');
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
            setIsSubmitting(true);
            startLoading();

            const respuesta = await AuthService.login(data.email, data.password);
            localStorage.setItem('token', respuesta.token);
            localStorage.setItem('rol', respuesta.rol);
            const nameFromToken = getDisplayNameFromToken(respuesta.token);
            localStorage.setItem('nombre', respuesta.nombre || respuesta.username || nameFromToken || data.email || 'Usuario');
            navigate(getRedirectByRole(respuesta.rol));
        } catch (err) {
            console.error('Error en login:', err);
            const backendMessage = typeof err?.response?.data === 'string'
                ? err.response.data
                : err?.response?.data?.error;
            setServerError(backendMessage || 'Credenciales incorrectas. Intenta de nuevo.');
        } finally {
            stopLoading();
            setIsSubmitting(false);
        }
    };

    return (
        <Container component="main" maxWidth="xs">
            {/* Box actúa como un div flexible que centra el contenido */}
            <Box
                sx={{
                    marginTop: 8,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                }}
            >
                {/* Paper le da el aspecto de tarjeta con sombra */}
                <Paper elevation={3} sx={{ padding: 4, width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', borderRadius: 2 }}>
                    
                    <Avatar sx={{ m: 1, bgcolor: 'primary.main' }}>
                        <LockOutlinedIcon />
                    </Avatar>
                    
                    <Typography component="h1" variant="h5" sx={{ mb: 1 }}>
                        Iniciar Sesión
                    </Typography>
                    
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                        Portal de Gestión de Cursos
                    </Typography>

                    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate sx={{ mt: 1, width: '100%' }}>
                        
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            id="email"
                            label="Correo electrónico"
                            autoComplete="email"
                            autoFocus
                            {...register("email", { required: "El correo es obligatorio" })}
                            error={!!errors.email} // MUI cambia a color rojo si hay error
                            helperText={errors.email ? errors.email.message : ""} // Muestra el mensaje de react-hook-form
                        />

                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            label="Contraseña"
                            type="password"
                            id="password"
                            autoComplete="current-password"
                            {...register("password", { required: "La contraseña es obligatoria" })}
                            error={!!errors.password}
                            helperText={errors.password ? errors.password.message : ""}
                        />

                        {serverError && (
                            <Alert severity="error" sx={{ mt: 2 }}>
                                {serverError}
                            </Alert>
                        )}

                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            disabled={isSubmitting}
                            sx={{ mt: 3, mb: 2, py: 1.5 }}
                        >
                            {isSubmitting ? 'Ingresando...' : 'Ingresar'}
                        </Button>

                        <Button
                            type="button"
                            fullWidth
                            variant="text"
                            onClick={() => navigate('/registro')}
                        >
                            Crear cuenta
                        </Button>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default Login;