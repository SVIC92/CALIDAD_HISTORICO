import axios from "axios";

const ensureApiBasePath = (url) => {
  let clean = String(url || "").trim().replace(/\/+$/, "");
  if (!clean) return "https://calidad-historico.onrender.com/api";

  // Avoid accidental relative URLs in production when protocol is omitted.
  if (!/^https?:\/\//i.test(clean)) {
    clean = `https://${clean}`;
  }

  return clean.endsWith("/api") ? clean : `${clean}/api`;
};

const resolvedBaseUrl = ensureApiBasePath(import.meta.env.VITE_API_BASE_URL);

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