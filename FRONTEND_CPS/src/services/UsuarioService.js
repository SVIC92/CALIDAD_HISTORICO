import axios from '../API/axios';

const userCandidates = [
    '/admin/usuarios',
    '/usuario/lista',
    '/usuarios/lista',
    '/admin/listaUsuarios',
];

const connectedUserCandidates = [
    '/usuarios/conectados',
    '/usuarios/conectados/',
    '/admin/usuarios-conectados',
    '/admin/usuariosConectados',
    '/usuario/conectados',
];

let connectedUsersEndpointCache = null; // string | false | null

const UsuarioService = {
    listar: async () => {
        for (const endpoint of userCandidates) {
            try {
                const respuesta = await axios.get(endpoint);
                return respuesta.data;
            } catch (error) {
                if (error?.response?.status && error.response.status !== 404) {
                    throw error;
                }
            }
        }

        throw new Error('No se encontro un endpoint de listado de usuarios en el backend.');
    },

    listarConectados: async () => {
        if (connectedUsersEndpointCache === false) {
            throw new Error('No se encontro un endpoint de usuarios conectados en el backend.');
        }

        if (typeof connectedUsersEndpointCache === 'string') {
            const respuesta = await axios.get(connectedUsersEndpointCache);
            return respuesta.data;
        }

        for (const endpoint of connectedUserCandidates) {
            try {
                const respuesta = await axios.get(endpoint);
                connectedUsersEndpointCache = endpoint;
                return respuesta.data;
            } catch (error) {
                if (error?.response?.status && error.response.status !== 404) {
                    throw error;
                }
            }
        }

        connectedUsersEndpointCache = false;

        throw new Error('No se encontro un endpoint de usuarios conectados en el backend.');
    },

    pingPresencia: async () => {
        const respuesta = await axios.post('/usuarios/ping');
        return respuesta.data;
    },

    getConectadosStreamUrl: () => `${axios.defaults.baseURL}/usuarios/conectados/stream`,
};

export default UsuarioService;
