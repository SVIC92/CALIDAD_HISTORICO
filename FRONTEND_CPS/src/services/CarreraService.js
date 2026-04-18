import axios from '../API/axios';

const CarreraService = {
  listar: async () => {
    const respuesta = await axios.get('/carrera/lista');
    return respuesta.data;
  },

  registro: async (payload) => {
    const respuesta = await axios.post('/carrera/registro', null, {
      params: {
        codigo: payload.codigo,
        nombre: payload.nombre,
        descripcion: payload.descripcion || undefined,
      },
    });
    return respuesta.data;
  },

  modificar: async (id, payload) => {
    const respuesta = await axios.post(`/carrera/modificar/${id}`, null, {
      params: {
        codigo: payload.codigo || undefined,
        nombre: payload.nombre || undefined,
        descripcion: payload.descripcion,
      },
    });
    return respuesta.data;
  },

  eliminar: async (id) => {
    const respuesta = await axios.get(`/carrera/eliminar/${id}`);
    return respuesta.data;
  },
};

export default CarreraService;
