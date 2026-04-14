import axios from '../API/axios';

const AuthService = {
    login: async (email, password) => {
        const respuesta = await axios.post('/auth/login', { email, password });
        return respuesta.data;
    },

    usuarioConectado: async () => {
        const respuesta = await axios.get('/auth/me');
        return respuesta.data;
    },
};

export default AuthService;
