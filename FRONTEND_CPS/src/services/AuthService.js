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

const getAuthBlobWithFallback = async (path, config = {}) => {
    try {
        const response = await axios.get(`/auth/${path}`, { ...config, responseType: 'blob' });
        return response.data;
    } catch (error) {
        if (error?.response?.status === 404) {
            const fallback = await axios.get(`/${path}`, { ...config, responseType: 'blob' });
            return fallback.data;
        }
        throw error;
    }
};

const AuthService = {
    login: async (email, password, otp) => {
        const payload = { email, password };
        if (otp && String(otp).trim()) {
            payload.otp = String(otp).trim();
        }
        return postAuthWithFallback('login', payload);
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

    twoFactorStatus: async () => {
        return getAuthWithFallback('2fa/status');
    },

    twoFactorSetup: async () => {
        return postAuthWithFallback('2fa/setup');
    },

    twoFactorQr: async (size = 280) => {
        return getAuthBlobWithFallback('2fa/qr', { params: { size } });
    },

    twoFactorEnable: async (code) => {
        return postAuthWithFallback('2fa/enable', { code });
    },

    twoFactorDisable: async (code) => {
        return postAuthWithFallback('2fa/disable', { code });
    },
};

export default AuthService;
