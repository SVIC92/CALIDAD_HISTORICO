import { create } from 'zustand';

const estadoInicial = () => ({
  estado: 'idle',       // 'idle' | 'cargando' | 'listo' | 'error'
  datos: null,
  error: null,
  notificacionVista: false,
});

const useIaGeneracionStore = create((set) => ({
  rubrica: estadoInicial(),
  silabo: estadoInicial(),

  iniciarRubrica: () =>
    set({ rubrica: { ...estadoInicial(), estado: 'cargando' } }),
  completarRubrica: (datos) =>
    set({ rubrica: { estado: 'listo', datos, error: null, notificacionVista: false } }),
  fallarRubrica: (error) =>
    set({ rubrica: { estado: 'error', datos: null, error, notificacionVista: false } }),
  limpiarRubrica: () =>
    set({ rubrica: estadoInicial() }),
  marcarRubricaVista: () =>
    set((s) => ({ rubrica: { ...s.rubrica, notificacionVista: true } })),

  iniciarSilabo: () =>
    set({ silabo: { ...estadoInicial(), estado: 'cargando' } }),
  completarSilabo: (datos) =>
    set({ silabo: { estado: 'listo', datos, error: null, notificacionVista: false } }),
  fallarSilabo: (error) =>
    set({ silabo: { estado: 'error', datos: null, error, notificacionVista: false } }),
  limpiarSilabo: () =>
    set({ silabo: estadoInicial() }),
  marcarSilaboVisto: () =>
    set((s) => ({ silabo: { ...s.silabo, notificacionVista: true } })),
}));

export default useIaGeneracionStore;
