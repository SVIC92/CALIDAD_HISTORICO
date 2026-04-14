import axios from '../API/axios';

const AuthService = {
    login: async (email, password) => {
        const respuesta = await axios.post('/auth/login', { email, password });
        return respuesta.data;
    },

    crearPrueba: async () => {
        const respuesta = await axios.get('/auth/crear-prueba');
        return respuesta.data;
    },
};

export default AuthService;
