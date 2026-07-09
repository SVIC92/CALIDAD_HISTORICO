import axios from "axios";

const DEFAULT_REMOTE_API_BASE_URL = "https://calidad-historico.onrender.com/api";
const DEFAULT_LOCAL_API_BASE_URL = "http://localhost:8080/api";

const isLocalRuntime = () => {
  if (typeof window === "undefined") return false;

  const isLocalHost = ["localhost", "127.0.0.1", "::1"].includes(window.location.hostname);
  if (!isLocalHost) return false;

  return Boolean(import.meta.env.DEV || isLocalHost);
};

const normalizeApiBaseUrl = (url, fallbackUrl) => {
  let clean = String(url || "").trim().replace(/\/+$/, "");
  if (!clean) return fallbackUrl;

  if (!/^https?:\/\//i.test(clean)) {
    const isLocalHost = /^(localhost|127\.0\.0\.1|::1)(:\d+)?$/i.test(clean);
    clean = `${isLocalHost ? "http" : "https"}://${clean}`;
  }

  return clean.endsWith("/api") ? clean : `${clean}/api`;
};

const isLocalHostUrl = (url) => {
  try {
    const parsed = new URL(url);
    return ["localhost", "127.0.0.1", "::1"].includes(parsed.hostname);
  } catch {
    return false;
  }
};

const resolveBaseUrl = () => {
  const localBaseUrl = normalizeApiBaseUrl(import.meta.env.VITE_LOCAL_API_BASE_URL, DEFAULT_LOCAL_API_BASE_URL);
  const remoteBaseUrl = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL, DEFAULT_REMOTE_API_BASE_URL);

  if (isLocalRuntime()) {
    return localBaseUrl;
  }

  if (isLocalHostUrl(remoteBaseUrl)) {
    return DEFAULT_REMOTE_API_BASE_URL;
  }

  return remoteBaseUrl;
};

const resolvedBaseUrl = resolveBaseUrl();

// Render (free tier) suspende el backend por inactividad y puede tardar
// hasta ~60s en "despertar" en el primer request. El timeout se deja holgado
// para no cortar ese arranque en frio; solo evita que un request quede
// colgado para siempre ante una falla real de red.
const REQUEST_TIMEOUT_MS = 60000;

const api = axios.create({
  baseURL: resolvedBaseUrl,
  timeout: REQUEST_TIMEOUT_MS,
});
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === "ECONNABORTED" || error.message === "Network Error") {
      error.message =
        "No se pudo conectar con el servidor. Si es la primera visita en un rato, puede tardar hasta un minuto en iniciar — intenta de nuevo en unos segundos.";
    }
    return Promise.reject(error);
  },
);
export default api;