import axios from '../API/axios';

export const obtenerCursos = async () => {
    const respuesta = await axios.get('/curso/lista');
    return respuesta.data;
};