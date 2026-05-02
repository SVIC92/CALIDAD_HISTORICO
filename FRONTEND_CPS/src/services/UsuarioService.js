import axios from "../API/axios";

// Endpoints basados en tu AdminControlador.java
const ADMIN_USERS_ENDPOINT = "/admin/usuarios";

// Endpoints basados en tu PresenciaControlador.java
const PRESENCIA_ENDPOINT = "/usuarios";

const buildAdminUsuarioPayload = (payload = {}) => ({
  nombre: payload?.nombre,
  email: payload?.email,
  password: payload?.password,
  rol: payload?.rol,
  carrera: payload?.carrera ?? null,
  cicloActual: payload?.cicloActual ?? null,
});

const UsuarioService = {
  // Llama a @GetMapping("/usuarios") en AdminControlador
  listar: async () => {
    const respuesta = await axios.get(ADMIN_USERS_ENDPOINT);
    return respuesta.data;
  },

  // Llama a @PostMapping("/usuarios") en AdminControlador
  registro: async (payload) => {
    const respuesta = await axios.post(
      ADMIN_USERS_ENDPOINT,
      buildAdminUsuarioPayload(payload),
    );

    // Si el admin lo crea inactivo desde el formulario, llamamos al endpoint de desactivar
    if (payload?.activo === false && respuesta?.data?.id) {
      await axios.patch(
        `${ADMIN_USERS_ENDPOINT}/${respuesta.data.id}/desactivar`,
      );
    }

    return respuesta.data;
  },

  // Llama a @PutMapping("/usuarios/{id}") en AdminControlador
  modificar: async (id, payload) => {
    const respuesta = await axios.put(
      `${ADMIN_USERS_ENDPOINT}/${id}`,
      buildAdminUsuarioPayload(payload),
    );

    // Llama a @PatchMapping("/usuarios/{id}/activar" o "desactivar")
    if (typeof payload?.activo === "boolean") {
      const estadoPath = payload.activo ? "activar" : "desactivar";
      await axios.patch(`${ADMIN_USERS_ENDPOINT}/${id}/${estadoPath}`);
    }

    return respuesta.data;
  },

  // Llama a @PatchMapping("/usuarios/{id}/desactivar") en AdminControlador
  eliminar: async (id) => {
    const respuesta = await axios.patch(
      `${ADMIN_USERS_ENDPOINT}/${id}/desactivar`,
    );
    return respuesta.data;
  },

  // Llama a @GetMapping("/conectados") en PresenciaControlador
  listarConectados: async () => {
    const respuesta = await axios.get(`${PRESENCIA_ENDPOINT}/conectados`);
    return respuesta.data;
  },

  // Llama a @PostMapping("/ping") en PresenciaControlador
  pingPresencia: async () => {
    const respuesta = await axios.post(`${PRESENCIA_ENDPOINT}/ping`);
    return respuesta.data;
  },
};

export default UsuarioService;
