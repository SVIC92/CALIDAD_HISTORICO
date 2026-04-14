import axios from '../API/axios';

const ReporteService = {
    registrar: async (actividadId) => {
        const respuesta = await axios.get(`/reporte/registrar/${actividadId}`);
        return respuesta.data;
    },

    registro: async (actividadId, payloadOrRespuesta) => {
        const payload = typeof payloadOrRespuesta === 'object'
            ? payloadOrRespuesta
            : { respuesta: payloadOrRespuesta };

        const respuesta = await axios.post(`/reporte/registro/${actividadId}`, null, {
            params: {
                respuesta: payload.respuesta,
            },
        });
        return respuesta.data;
    },

    listar: async (actividadId) => {
        const respuesta = await axios.get(`/reporte/listar/${actividadId}`);
        return respuesta.data;
    },

    calificarView: async (reporteId) => {
        const respuesta = await axios.get(`/reporte/calificar/${reporteId}`);
        return respuesta.data;
    },

    calificar: async (reporteId, payloadOrNota, comentario) => {
        const payload = typeof payloadOrNota === 'object'
            ? payloadOrNota
            : { nota: payloadOrNota, comentario };

        const respuesta = await axios.post(`/reporte/calificar/${reporteId}`, null, {
            params: {
                nota: payload.nota,
                comentario: payload.comentario,
            },
        });
        return respuesta.data;
    },

    detalle: async (actividadId) => {
        const respuesta = await axios.get(`/reporte/detalle/${actividadId}`);
        return respuesta.data;
    },
};

export default ReporteService;
