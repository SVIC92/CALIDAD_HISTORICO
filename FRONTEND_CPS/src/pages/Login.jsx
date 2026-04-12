import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../API/axios'; 

const Login = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate(); // Hook para cambiar de página

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            // 1. Mandamos las credenciales a Spring Boot
            const response = await api.post('/auth/login', { email, password });

            // 2. Si es exitoso, guardamos el token y el rol en el navegador
            localStorage.setItem('token', response.data.token);
            localStorage.setItem('rol', response.data.rol);

            // 3. Redirigimos al usuario a la página principal (ej. panel de cursos)
            navigate('/curso');
        } catch (err) {
            console.error('Error en login:', err);
            setError('Credenciales incorrectas. Intenta de nuevo.');
        }
    };

    return (
        <div style={{ padding: '50px', maxWidth: '400px', margin: '0 auto' }}>
            <h2>Iniciar Sesión</h2>
            <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                <input
                    type="email"
                    placeholder="Correo electrónico"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                />
                <input
                    type="password"
                    placeholder="Contraseña"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />
                <button type="submit">Ingresar</button>
            </form>
            {error && <p style={{ color: 'red' }}>{error}</p>}
        </div>
    );
};

export default Login;