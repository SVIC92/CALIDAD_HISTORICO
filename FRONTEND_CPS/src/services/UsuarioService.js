import axios from '../API/axios';

const ADMIN_USERS_ENDPOINT = '/admin/usuarios';

const connectedUserCandidates = [
    '/usuarios/conectados',
    '/usuarios/conectados/',
    '/admin/usuarios-conectados',
    '/admin/usuariosConectados',
    '/usuario/conectados',
];

let connectedUsersEndpointCache = null; // string | false | null

const buildAdminUsuarioPayload = (payload = {}) => ({
    nombre: payload?.nombre,
    email: payload?.email,
    password: payload?.password,
    rol: payload?.rol,
    carrera: payload?.carrera ?? null,
    cicloActual: payload?.cicloActual ?? null,
});

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
        const respuesta = await axios.get(ADMIN_USERS_ENDPOINT);
        return respuesta.data;
    },

    registro: async (payload) => {
        const respuesta = await axios.post(ADMIN_USERS_ENDPOINT, buildAdminUsuarioPayload(payload));

        // Si el backend crea activo por defecto y el frontend solicita inactivo, lo desactivamos luego.
        if (payload?.activo === false && respuesta?.data?.id) {
            await axios.patch(`${ADMIN_USERS_ENDPOINT}/${respuesta.data.id}/desactivar`);
        }

        return respuesta.data;
    },

    modificar: async (id, payload) => {
        const respuesta = await axios.put(
            `${ADMIN_USERS_ENDPOINT}/${id}`,
            buildAdminUsuarioPayload(payload)
        );

        if (typeof payload?.activo === 'boolean') {
            const estadoPath = payload.activo ? 'activar' : 'desactivar';
            await axios.patch(`${ADMIN_USERS_ENDPOINT}/${id}/${estadoPath}`);
        }

        return respuesta.data;
    },

    eliminar: async (id) => {
        const respuesta = await axios.patch(`${ADMIN_USERS_ENDPOINT}/${id}/desactivar`);
        return respuesta.data;
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
