import axios from '../API/axios';

const VideoconferenciaService = {
  listarPublicas: async () => {
    const respuesta = await axios.get('/videoconferencias/publicas');
    return respuesta.data;
  },

  crearSala: async ({ titulo, capacidad, esPublica, creador }) => {
    const respuesta = await axios.post('/videoconferencias/crear', null, {
      params: {
        titulo,
        capacidad,
        esPublica,
        creadorId: creador?.id,
      },
    });
    return respuesta.data;
  },

  invitarUsuarios: async (salaUuid, usuarioIds, rol) => {
    const respuesta = await axios.post(`/videoconferencias/${salaUuid}/invitar`, usuarioIds, {
      params: { rol },
    });
    return respuesta.data;
  },

  listarParticipantes: async (salaUuid) => {
    const respuesta = await axios.get(`/videoconferencias/${salaUuid}/participantes`);
    return respuesta.data;
  },
};

export default VideoconferenciaService;