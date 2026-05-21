import axios from '../API/axios';

const VideoconferenciaService = {
  listarPublicas: async () => {
    const respuesta = await axios.get('/videoconferencias/publicas');
    return respuesta.data;
  },

  crearSala: async ({ titulo, capacidad, esPublica, creador }) => {
    const creadorMinimo = creador
      ? {
          id: creador.id,
          nombre: creador.nombre,
          email: creador.email,
          rol: String(creador.rol || '').replace('ROLE_', ''),
          activo: creador.activo ?? true,
        }
      : {};

    const respuesta = await axios.post('/videoconferencias/crear', creadorMinimo, {
      params: {
        titulo,
        capacidad,
        esPublica,
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