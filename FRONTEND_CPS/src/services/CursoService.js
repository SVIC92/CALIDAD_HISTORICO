import axios from '../API/axios';

const CursoService = {
    registrar: async () => {
        const respuesta = await axios.get('/curso/registrar');
        return respuesta.data;
    },

    registro: async (payloadOrNombre, descripcion) => {
        const payload = typeof payloadOrNombre === 'object'
            ? payloadOrNombre
            : { nombre: payloadOrNombre, descripcion };

        const profesorId = payload.profesorId?.trim?.() || payload.profesorId;
        const profesorAsignado = payload.profesorAsignado?.trim?.() || payload.profesorAsignado;
        const codigoCurso = payload.codigoCurso?.trim?.() || payload.codigoCurso;
        const carrera = payload.carrera?.trim?.() || payload.carrera;

        const respuesta = await axios.post('/curso/registro', null, {
            params: {
                nombre: payload.nombre,
                codigoCurso: codigoCurso || undefined,
                descripcion: payload.descripcion,
                capacidadMaxima: payload.capacidadMaxima,
                creditos: payload.creditos,
                ciclo: payload.ciclo,
                modalidad: payload.modalidad,
                fechaInicio: payload.fechaInicio,
                fechaTermino: payload.fechaTermino,
                horasTeoricas: payload.horasTeoricas,
                horasPracticas: payload.horasPracticas,
                horasLaboratorio: payload.horasLaboratorio,
                estado: payload.estado,
                carrera: carrera || undefined,
                profesorAsignado: profesorAsignado || undefined,
                profesorId: profesorId || undefined,
            },
        });
        return respuesta.data;
    },

    listar: async () => {
        const respuesta = await axios.get('/curso/lista');
        return respuesta.data;
    },

    getAll: async () => {
        const respuesta = await axios.get('/curso/lista');
        return respuesta.data;
    },

    listarActivos: async () => {
        const respuesta = await axios.get('/curso/lista/activos');
        return respuesta.data;
    },

    listarCaducados: async () => {
        const respuesta = await axios.get('/curso/lista/caducados');
        return respuesta.data;
    },

    obtenerPorIdParaModificar: async (id) => {
        const respuesta = await axios.get(`/curso/modificar/${id}`);
        return respuesta.data;
    },

    modificar: async (id, payloadOrNombre, descripcion) => {
        const payload = typeof payloadOrNombre === 'object'
            ? payloadOrNombre
            : { nombre: payloadOrNombre, descripcion };

        const profesorId = payload.profesorId?.trim?.() || payload.profesorId;
        const profesorAsignado = payload.profesorAsignado?.trim?.() || payload.profesorAsignado;
        const codigoCurso = payload.codigoCurso?.trim?.() || payload.codigoCurso;
        const carrera = payload.carrera?.trim?.() || payload.carrera;

        const respuesta = await axios.post(`/curso/modificar/${id}`, null, {
            params: {
                nombre: payload.nombre,
                codigoCurso: codigoCurso || undefined,
                descripcion: payload.descripcion,
                capacidadMaxima: payload.capacidadMaxima,
                creditos: payload.creditos,
                ciclo: payload.ciclo,
                modalidad: payload.modalidad,
                fechaInicio: payload.fechaInicio,
                fechaTermino: payload.fechaTermino,
                horasTeoricas: payload.horasTeoricas,
                horasPracticas: payload.horasPracticas,
                horasLaboratorio: payload.horasLaboratorio,
                estado: payload.estado,
                carrera: carrera || undefined,
                profesorAsignado: profesorAsignado || undefined,
                profesorId: profesorId || undefined,
            },
        });
        return respuesta.data;
    },

    eliminar: async (id) => {
        const respuesta = await axios.get(`/curso/eliminar/${id}`);
        return respuesta.data;
    },

    listarDisponiblesProfesor: async () => {
        const respuesta = await axios.get('/curso/listaDisponiblesProfesor');
        return respuesta.data;
    },

    listarInscritosProfesor: async () => {
        const respuesta = await axios.get('/curso/listaInscritosProfesor');
        return respuesta.data;
    },

    listarDisponiblesAlumno: async () => {
        const respuesta = await axios.get('/curso/listaDisponiblesAlumno');
        return respuesta.data;
    },

    listarInscritosAlumno: async () => {
        const respuesta = await axios.get('/curso/listaInscritosAlumno');
        return respuesta.data;
    },

    inscribirCurso: async (id) => {
        const respuesta = await axios.get(`/curso/inscribir/${id}`);
        return respuesta.data;
    },
};

export const obtenerCursos = CursoService.listar;

export default CursoService;