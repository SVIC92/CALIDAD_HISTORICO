import axios from '../API/axios';

const ReporteService = {
  registrar: async (actividadId) => {
    const respuesta = await axios.get(`/reporte/registrar/${actividadId}`);
    return respuesta.data;
  },
  registro: async (actividadId, payloadOrRespuesta, archivo) => {
    const payload =
      typeof payloadOrRespuesta === "object"
        ? payloadOrRespuesta
        : { respuesta: payloadOrRespuesta };
    const archivoFinal = archivo ?? payload.archivo;

    const formData = new FormData();
    formData.append("respuesta", payload.respuesta ?? "");
    if (archivoFinal) {
      formData.append("archivo", archivoFinal);
    }

    const respuesta = await axios.post(`/reporte/registro/${actividadId}`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return respuesta.data;
  },
  modificar: async (actividadId, payloadOrNombre, descripcion) => {
    const payload =
      typeof payloadOrNombre === "object"
        ? payloadOrNombre
        : { nombre: payloadOrNombre, descripcion };
    const respuesta = await axios.post(
      `/actividad/modificar/${actividadId}`,
      null,
      {
        params: {
          nombre: payload.nombre,
          descripcion: payload.descripcion,
          fechaVencimiento: payload.fechaVencimiento,
          intentosPermitidos: payload.intentosPermitidos || 1, // <<< NUEVO
        },
      },
    );
    return respuesta.data;
  },

  listar: async (actividadId) => {
    const respuesta = await axios.get(`/reporte/listar/${actividadId}`);
    return respuesta.data;
  },

  calificarView: async (reporteId) => {
    const respuesta = await axios.get(`/reporte/calificar/${reporteId}`);
    return respuesta.data;
  },

  calificar: async (reporteId, payloadOrNota, comentario) => {
    const payload =
      typeof payloadOrNota === "object"
        ? payloadOrNota
        : { nota: payloadOrNota, comentario };

    const respuesta = await axios.post(
      `/reporte/calificar/${reporteId}`,
      null,
      {
        params: {
          nota: payload.nota,
          comentario: payload.comentario,
        },
      },
    );
    return respuesta.data;
  },

  detalle: async (actividadId) => {
    const respuesta = await axios.get(`/reporte/detalle/${actividadId}`);
    return respuesta.data;
  },
};

export default ReporteService;
