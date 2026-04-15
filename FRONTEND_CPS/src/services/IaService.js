import axios from '../API/axios';

const roleEndpointByCode = {
  ROLE_ALUMNO: '/ia/chat/alumno',
  ROLE_PROFESOR: '/ia/chat/profesor',
  ROLE_ADMIN: '/ia/chat/admin',
};

const IaService = {
  enviarMensaje: async (mensaje, rol) => {
    const endpoint = roleEndpointByCode[rol] || roleEndpointByCode.ROLE_ALUMNO;
    const respuesta = await axios.post(endpoint, { mensaje });
    return respuesta.data;
  },

  obtenerUltimoHistorial: async () => {
    const respuesta = await axios.get('/ia/historial/ultimo');
    return respuesta.data;
  },

  generarRubrica: async (payload) => {
    const respuesta = await axios.post('/ia/rubricas/generar', payload);
    return respuesta.data;
  },
};

export default IaService;
