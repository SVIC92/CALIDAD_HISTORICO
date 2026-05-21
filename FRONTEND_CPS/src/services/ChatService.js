import axios from "../API/axios";

const ChatService = {
  obtenerHistorial: async (idReceptor) => {
    const respuesta = await axios.get(`/chat/historial/${idReceptor}`);
    return respuesta.data;
  },

  enviarArchivo: async (archivo, idReceptor, tipo) => {
    const formData = new FormData();
    formData.append("archivo", archivo);
    formData.append("idReceptor", idReceptor);
    formData.append("tipo", tipo);

    const respuesta = await axios.post("/chat/enviar-archivo", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return respuesta.data;
  },
  obtenerNoLeidos: async () => {
    const respuesta = await axios.get("/chat/no-leidos");
    return respuesta.data;
  },

  marcarLeidos: async (idEmisor) => {
    const respuesta = await axios.put(`/chat/marcar-leidos/${idEmisor}`);
    return respuesta.data;
  },

  buscarGifs: async (query) => {
    const respuesta = await axios.get('/chat/gifs', {
      params: { query },
    });
    return respuesta.data;
  },
};

export default ChatService;
