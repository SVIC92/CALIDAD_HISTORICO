import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Box, Button, InputAdornment, TextField, Typography } from '@mui/material';
import { ArrowBack, Refresh, Search } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import DataTable from '../components/DataTable';
import UsuarioService from '../services/UsuarioService';

const normalizeConectado = (usuario) => ({
  id: usuario.id || usuario._id || usuario.email || Math.random().toString(36),
  nombre: usuario.nombre || usuario.username || usuario.email || '-',
  email: usuario.email || '-',
  rol: usuario.rol || '-',
  estadoConexion: usuario.estadoConexion || usuario.estado || 'CONECTADO',
  ultimoAcceso: usuario.ultimaActividad || usuario.ultimoAcceso || usuario.lastSeen || usuario.fechaActualizacion || '-',
});

const formatDateTime = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
};

const UsuariosConectados = () => {
  const navigate = useNavigate();
  const [usuarios, setUsuarios] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [endpointDisponible, setEndpointDisponible] = useState(true);

  const cargar = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMsg('');
      const data = await UsuarioService.listarConectados();
      const normalizados = Array.isArray(data) ? data.map(normalizeConectado) : [];
      setUsuarios(normalizados);
      setEndpointDisponible(true);
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.message;
      const notFoundEndpoint = String(backendMessage || '').toLowerCase().includes('no se encontro un endpoint de usuarios conectados');
      if (notFoundEndpoint) {
        setEndpointDisponible(false);
        setErrorMsg('Backend sin endpoint de listado de conectados. Crea un GET como /api/usuarios/conectados o un stream SSE para habilitar este módulo.');
      } else {
        setErrorMsg(backendMessage || 'No se pudo cargar el listado de usuarios conectados.');
      }
      setUsuarios([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    cargar();
    if (!endpointDisponible) return undefined;

    const intervalId = window.setInterval(cargar, 15000);
    return () => window.clearInterval(intervalId);
  }, [cargar, endpointDisponible]);

  useEffect(() => {
    if (!endpointDisponible) return undefined;

    const token = localStorage.getItem('token');
    if (!token) return undefined;

    const streamUrl = `${UsuarioService.getConectadosStreamUrl()}?token=${encodeURIComponent(token)}`;
    const eventSource = new EventSource(streamUrl);

    const onMessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        const data = Array.isArray(payload) ? payload : [];
        const normalizados = data.map(normalizeConectado);
        setUsuarios(normalizados);
        setErrorMsg('');
      } catch {
        // Ignorar paquetes mal formados sin romper el stream.
      }
    };

    const onUsuariosConectados = (event) => {
      onMessage(event);
    };

    eventSource.addEventListener('usuarios-conectados', onUsuariosConectados);
    eventSource.onmessage = onMessage;
    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.removeEventListener('usuarios-conectados', onUsuariosConectados);
      eventSource.close();
    };
  }, [endpointDisponible]);

  const columns = [
    { id: 'nombre', label: 'Nombre' },
    { id: 'email', label: 'Email' },
    { id: 'rol', label: 'Rol', align: 'center' },
    { id: 'estadoConexion', label: 'Estado', align: 'center' },
    { id: 'ultimoAcceso', label: 'Último Acceso' },
  ];

  const filteredData = useMemo(() => {
    const term = searchTerm.toLowerCase();
    return usuarios
      .map((u) => ({
        ...u,
        ultimoAcceso: formatDateTime(u.ultimoAcceso),
      }))
      .filter((u) => `${u.nombre} ${u.email} ${u.rol} ${u.estadoConexion}`.toLowerCase().includes(term));
  }, [usuarios, searchTerm]);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/usuarios')} sx={{ mr: 2 }}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Usuarios Conectados En Tiempo Real
        </Typography>
        <Button variant="outlined" startIcon={<Refresh />} onClick={cargar} disabled={isLoading || !endpointDisponible}>
          Recargar
        </Button>
      </Box>

      {errorMsg && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {errorMsg}
        </Alert>
      )}

      <TextField
        fullWidth
        placeholder="Buscar por nombre, email, rol o estado..."
        sx={{ mb: 3, bgcolor: 'background.paper' }}
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <Search />
              </InputAdornment>
            ),
          },
        }}
      />

      <DataTable columns={columns} data={filteredData} exportFileName="usuarios-conectados" />
    </Box>
  );
};

export default UsuariosConectados;
