import { useEffect, useRef } from 'react';
import { Alert, Button, Snackbar, Typography } from '@mui/material';
import { AutoAwesome } from '@mui/icons-material';
import { useLocation, useNavigate } from 'react-router-dom';
import useIaGeneracionStore from '../store/useIaGeneracionStore';

const RUTAS = {
  rubrica: '/modulo/ia/rubricas',
  silabo: '/modulo/ia/silabo',
};

const useNotificacion = (tipo) => {
  const location = useLocation();
  const store = useIaGeneracionStore();
  const item = store[tipo];
  const marcar = tipo === 'rubrica' ? store.marcarRubricaVista : store.marcarSilaboVisto;
  const enPagina = location.pathname === RUTAS[tipo];

  // Cuando completa y el usuario YA está en la página, marcar como vista sin Snackbar
  useEffect(() => {
    if ((item.estado === 'listo' || item.estado === 'error') && !item.notificacionVista && enPagina) {
      marcar();
    }
  }, [item.estado, item.notificacionVista, enPagina, marcar]);

  const debeNotificar =
    (item.estado === 'listo' || item.estado === 'error') &&
    !item.notificacionVista &&
    !enPagina;

  return { debeNotificar, estado: item.estado, marcar };
};

const GeneracionIaNotificador = () => {
  const navigate = useNavigate();
  const { marcarRubricaVista, marcarSilaboVisto } = useIaGeneracionStore();

  const rubrica = useNotificacion('rubrica');
  const silabo = useNotificacion('silabo');

  // Mostrar solo una notificación a la vez; rubrica tiene prioridad
  const activa = rubrica.debeNotificar
    ? { tipo: 'rubrica', estado: rubrica.estado, marcar: marcarRubricaVista }
    : silabo.debeNotificar
    ? { tipo: 'silabo', estado: silabo.estado, marcar: marcarSilaboVisto }
    : null;

  // Ref para detectar cambio de "activa" y no cerrar si hay una nueva pendiente
  const tipoActivoRef = useRef(null);
  useEffect(() => {
    tipoActivoRef.current = activa?.tipo ?? null;
  });

  const handleClose = (_, reason) => {
    if (reason === 'clickaway') return;
    activa?.marcar();
  };

  const handleVerAhora = () => {
    activa?.marcar();
    navigate(RUTAS[activa.tipo]);
  };

  const esError = activa?.estado === 'error';
  const nombre = activa?.tipo === 'rubrica' ? 'Rúbrica' : 'Sílabo';

  return (
    <Snackbar
      open={Boolean(activa)}
      autoHideDuration={14000}
      onClose={handleClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      sx={{ zIndex: 9998 }}
    >
      <Alert
        severity={esError ? 'error' : 'success'}
        icon={<AutoAwesome />}
        onClose={handleClose}
        sx={{ width: '100%', boxShadow: 6 }}
        action={
          !esError && (
            <Button size="small" color="inherit" onClick={handleVerAhora} sx={{ whiteSpace: 'nowrap' }}>
              Ver ahora
            </Button>
          )
        }
      >
        <Typography variant="subtitle2" fontWeight="bold">
          {esError ? `Error al generar ${nombre}` : `${nombre} lista`}
        </Typography>
        <Typography variant="body2">
          {esError
            ? `No se pudo generar la ${nombre.toLowerCase()}. Intenta de nuevo.`
            : `Tu ${nombre.toLowerCase()} fue generada con IA. Haz clic en "Ver ahora".`}
        </Typography>
      </Alert>
    </Snackbar>
  );
};

export default GeneracionIaNotificador;
