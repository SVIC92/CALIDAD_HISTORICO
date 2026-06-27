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
        const respuesta = await axios.post(`/inscripcion/aprobar/${inscripcionId}`);
        return respuesta.data;
    },

    rechazar: async (inscripcionId) => {
        const respuesta = await axios.post(`/inscripcion/rechazar/${inscripcionId}`);
        return respuesta.data;
    },

    aprobarProfesor: async (inscripcionId) => {
        const respuesta = await axios.post(`/inscripcion/aprobarProfesor/${inscripcionId}`);
        return respuesta.data;
    },

    rechazarProfesor: async (inscripcionId) => {
        const respuesta = await axios.post(`/inscripcion/rechazarProfesor/${inscripcionId}`);
        return respuesta.data;
    },

    inscribirAlumnoDirecto: async (usuarioId, cursoId) => {
        const respuesta = await axios.post('/inscripcion/inscribirAlumnoDirecto', null, {
            params: { usuarioId, cursoId },
        });
        return respuesta.data;
    },
};

export default InscripcionService;
