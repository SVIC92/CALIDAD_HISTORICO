import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // Añade esta línea para solucionar el error de sockjs-client
    global: "window",
  },
});
