import axios from '../API/axios';

const postAuthWithFallback = async (path, body) => {
    try {
        const response = await axios.post(`/auth/${path}`, body);
        return response.data;
    } catch (error) {
        if (error?.response?.status === 404) {
            const fallback = await axios.post(`/${path}`, body);
            return fallback.data;
        }
        throw error;
    }
};

const getAuthWithFallback = async (path) => {
    try {
        const response = await axios.get(`/auth/${path}`);
        return response.data;
    } catch (error) {
        if (error?.response?.status === 404) {
            const fallback = await axios.get(`/${path}`);
            return fallback.data;
        }
        throw error;
    }
};

const AuthService = {
    login: async (email, password) => {
        return postAuthWithFallback('login', { email, password });
    },

    solicitarRecuperacion: async (email) => {
        return postAuthWithFallback('forgot-password', { email });
    },

    reestablecerPassword: async ({ token, password, password2 }) => {
        return postAuthWithFallback('reset-password', { token, password, password2 });
    },

    usuarioConectado: async () => {
        return getAuthWithFallback('me');
    },
};

export default AuthService;
