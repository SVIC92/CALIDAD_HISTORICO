import axios from "axios";

const DEFAULT_REMOTE_API_BASE_URL = "https://calidad-historico.onrender.com/api";
const DEFAULT_LOCAL_API_BASE_URL = "http://localhost:8080/api";

const isLocalRuntime = () => {
  if (import.meta.env.DEV) return true;
  if (typeof window === "undefined") return false;

  return ["localhost", "127.0.0.1", "::1"].includes(window.location.hostname);
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

const resolvedBaseUrl = isLocalRuntime()
  ? normalizeApiBaseUrl(import.meta.env.VITE_LOCAL_API_BASE_URL, DEFAULT_LOCAL_API_BASE_URL)
  : normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL, DEFAULT_REMOTE_API_BASE_URL);

const api = axios.create({
  baseURL: resolvedBaseUrl,
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
export default api;