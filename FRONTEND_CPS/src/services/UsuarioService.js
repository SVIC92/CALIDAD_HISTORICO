import axios from '../API/axios';

const userCandidates = [
    '/usuario/listado',
    '/usuario/listadoUsuarios',
    '/admin/usuarios',
    '/usuario/lista',
    '/usuarios/lista',
    '/admin/listaUsuarios',
];

const userCreateCandidates = [
    '/usuario/registro',
    '/usuarios/registro',
    '/admin/usuario/registro',
    '/admin/usuarios/registro',
];

const userUpdateCandidates = [
    '/usuario/modificar',
    '/usuarios/modificar',
    '/admin/usuario/modificar',
    '/admin/usuarios/modificar',
];

const userDeleteCandidates = [
    '/usuario/eliminar',
    '/usuarios/eliminar',
    '/admin/usuario/eliminar',
    '/admin/usuarios/eliminar',
];

const connectedUserCandidates = [
    '/usuarios/conectados',
    '/usuarios/conectados/',
    '/admin/usuarios-conectados',
    '/admin/usuariosConectados',
    '/usuario/conectados',
];

let connectedUsersEndpointCache = null; // string | false | null

const requestWithFallback = async (candidates, requestBuilder, notFoundErrorMessage) => {
    for (const endpoint of candidates) {
        try {
            const respuesta = await requestBuilder(endpoint);
            return respuesta.data;
        } catch (error) {
            if (error?.response?.status && error.response.status !== 404) {
                throw error;
            }
        }
    }

    throw new Error(notFoundErrorMessage);
};

const UsuarioService = {
    listar: async () => {
        return requestWithFallback(
            userCandidates,
            (endpoint) => axios.get(endpoint),
            'No se encontro un endpoint de listado de usuarios en el backend.'
        );
    },

    registro: async (payload) => {
        return requestWithFallback(
            userCreateCandidates,
            (endpoint) => axios.post(endpoint, null, {
                params: {
                    nombre: payload?.nombre,
                    email: payload?.email,
                    password: payload?.password,
                    password2: payload?.password2,
                    rol: payload?.rol,
                    activo: payload?.activo,
                },
            }),
            'No se encontro un endpoint de registro de usuarios en el backend.'
        );
    },

    modificar: async (id, payload) => {
        return requestWithFallback(
            userUpdateCandidates,
            (baseEndpoint) => axios.post(`${baseEndpoint}/${id}`, null, {
                params: {
                    nombre: payload?.nombre || undefined,
                    email: payload?.email || undefined,
                    password: payload?.password || undefined,
                    password2: payload?.password2 || undefined,
                    rol: payload?.rol || undefined,
                    activo: payload?.activo,
                },
            }),
            'No se encontro un endpoint de modificacion de usuarios en el backend.'
        );
    },

    eliminar: async (id) => {
        return requestWithFallback(
            userDeleteCandidates,
            (baseEndpoint) => axios.get(`${baseEndpoint}/${id}`),
            'No se encontro un endpoint de eliminacion de usuarios en el backend.'
        );
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
};

export default UsuarioService;
