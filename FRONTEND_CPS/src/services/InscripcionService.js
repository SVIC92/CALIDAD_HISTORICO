import axios from '../API/axios';

const InscripcionService = {
    listaPendientesProfesor: async () => {
        const respuesta = await axios.get('/inscripcion/listaPendientesProfesor');
        return respuesta.data;
    },

    listaRealizadasProfesor: async () => {
        const respuesta = await axios.get('/inscripcion/listaRealizadasProfesor');
        return respuesta.data;
    },

    listaPendientesAlumno: async () => {
        const respuesta = await axios.get('/inscripcion/listaPendientesAlumno');
        return respuesta.data;
    },

    listaRealizadasAlumno: async () => {
        const respuesta = await axios.get('/inscripcion/listaRealizadasAlumno');
        return respuesta.data;
    },

    aprobar: async (inscripcionId) => {
        const respuesta = await axios.get(`/inscripcion/aprobar/${inscripcionId}`);
        return respuesta.data;
    },

    rechazar: async (inscripcionId) => {
        const respuesta = await axios.get(`/inscripcion/rechazar/${inscripcionId}`);
        return respuesta.data;
    },

    aprobarProfesor: async (inscripcionId) => {
        const respuesta = await axios.get(`/inscripcion/aprobarProfesor/${inscripcionId}`);
        return respuesta.data;
    },

    rechazarProfesor: async (inscripcionId) => {
        const respuesta = await axios.get(`/inscripcion/rechazarProfesor/${inscripcionId}`);
        return respuesta.data;
    },
};

export default InscripcionService;
