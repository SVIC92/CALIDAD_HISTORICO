import axios from '../API/axios';

const PortalService = {
    index: async () => {
        const respuesta = await axios.get('/portal/');
        return respuesta.data;
    },

    registrar: async () => {
        const respuesta = await axios.get('/portal/registrar');
        return respuesta.data;
    },

    registro: async (payloadOrNombre, email, password, password2) => {
        const payload = typeof payloadOrNombre === 'object'
            ? payloadOrNombre
            : { nombre: payloadOrNombre, email, password, password2 };

        const carrera = payload.carrera?.trim?.() || payload.carrera;
        const cicloActual = payload.cicloActual === '' || payload.cicloActual === null || payload.cicloActual === undefined
            ? undefined
            : Number(payload.cicloActual);

        const respuesta = await axios.post('/portal/registro', null, {
            params: {
                nombre: payload.nombre,
                email: payload.email,
                password: payload.password,
                password2: payload.password2,
                carrera: carrera || undefined,
                cicloActual: Number.isNaN(cicloActual) ? undefined : cicloActual,
            },
        });
        return respuesta.data;
    },

    login: async (error) => {
        const respuesta = await axios.get('/portal/login', {
            params: error ? { error } : undefined,
        });
        return respuesta.data;
    },

    inicio: async () => {
        const respuesta = await axios.get('/portal/inicio');
        return respuesta.data;
    },
};

export default PortalService;
