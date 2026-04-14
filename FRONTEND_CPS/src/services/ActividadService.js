import axios from '../API/axios';

const ActividadService = {
    registrar: async (cursoId) => {
        const respuesta = await axios.get(`/actividad/registrar/${cursoId}`);
        return respuesta.data;
    },

    registro: async (cursoId, payloadOrNombre, descripcion) => {
        const payload = typeof payloadOrNombre === 'object'
            ? payloadOrNombre
            : { nombre: payloadOrNombre, descripcion };

        const respuesta = await axios.post(`/actividad/registro/${cursoId}`, null, {
            params: {
                nombre: payload.nombre,
                descripcion: payload.descripcion,
                fechaVencimiento: payload.fechaVencimiento,
            },
        });
        return respuesta.data;
    },

    listar: async (cursoId) => {
        const respuesta = await axios.get(`/actividad/listar/${cursoId}`);
        return respuesta.data;
    },

    obtenerPorIdParaModificar: async (actividadId) => {
        const respuesta = await axios.get(`/actividad/modificar/${actividadId}`);
        return respuesta.data;
    },

    modificar: async (actividadId, payloadOrNombre, descripcion) => {
        const payload = typeof payloadOrNombre === 'object'
            ? payloadOrNombre
            : { nombre: payloadOrNombre, descripcion };

        const respuesta = await axios.post(`/actividad/modificar/${actividadId}`, null, {
            params: {
                nombre: payload.nombre,
                descripcion: payload.descripcion,
                fechaVencimiento: payload.fechaVencimiento,
            },
        });
        return respuesta.data;
    },

    eliminar: async (actividadId) => {
        const respuesta = await axios.get(`/actividad/eliminar/${actividadId}`);
        return respuesta.data;
    },
};

export default ActividadService;
