import axios from '../API/axios';

const AuthService = {
    login: async (email, password) => {
        const respuesta = await axios.post('/auth/login', { email, password });
        return respuesta.data;
    },

    solicitarRecuperacion: async (email) => {
        const respuesta = await axios.post('/auth/forgot-password', { email });
        return respuesta.data;
    },

    reestablecerPassword: async ({ token, password, password2 }) => {
        const respuesta = await axios.post('/auth/reset-password', { token, password, password2 });
        return respuesta.data;
    },

    usuarioConectado: async () => {
        const respuesta = await axios.get('/auth/me');
        return respuesta.data;
    },
};

export default AuthService;
