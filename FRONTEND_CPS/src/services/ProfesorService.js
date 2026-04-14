import axios from '../API/axios';

const ProfesorService = {
    dashboard: async () => {
        const respuesta = await axios.get('/profesor/dashboard');
        return respuesta.data;
    },

    listar: async () => {
        const respuesta = await axios.get('/profesor/lista');
        return respuesta.data;
    },
};

export default ProfesorService;
