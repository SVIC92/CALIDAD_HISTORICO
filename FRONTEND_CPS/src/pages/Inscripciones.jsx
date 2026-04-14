import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  MenuItem,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { ArrowBack, CheckCircle, Close, Refresh, ListAlt } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import DataTable from '../components/DataTable';
import InscripcionService from '../services/InscripcionService';
import CursoService from '../services/CursoService';

const formatDate = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString();
};

const normalizeInscripcion = (inscripcion) => ({
  id: inscripcion.id || '-',
  usuarioNombre: inscripcion.usuario?.nombre || '-',
  usuarioEmail: inscripcion.usuario?.email || '-',
  cursoNombre: inscripcion.curso?.nombre || '-',
  estado: inscripcion.estado || '-',
  fechaCreacion: formatDate(inscripcion.fechaCreacion),
  raw: inscripcion,
});

const normalizeCursoInscrito = (curso) => ({
  id: curso.id || curso._id || '-',
  cursoNombre: curso.nombre || '-',
  descripcion: curso.descripcion || '-',
  creditos: curso.creditos ?? '-',
  profesorAsignado:
    typeof curso.profesorAsignado === 'string'
      ? curso.profesorAsignado
      : curso.profesorAsignado?.nombre || curso.profesorAsignado?.email || '-',
  estado: 'INSCRITO',
  fechaCreacion: formatDate(curso.fechaCreacion || curso.fechaRegistro),
  raw: curso,
});

const Inscripciones = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol');

  const [pendientes, setPendientes] = useState([]);
  const [realizadas, setRealizadas] = useState([]);
  const [tab, setTab] = useState('pendientes');
  const [cursoFilter, setCursoFilter] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const headersByRole = {
    ROLE_ADMIN: 'Inscripciones de Profesores',
    ROLE_PROFESOR: 'Inscripciones de Alumnos',
    ROLE_ALUMNO: 'Mis Cursos Inscritos',
  };

  const loadInscripciones = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMsg('');
      setSuccessMsg('');

      if (rol === 'ROLE_ADMIN') {
        const [pendientesData, realizadasData] = await Promise.all([
          InscripcionService.listaPendientesProfesor(),
          InscripcionService.listaRealizadasProfesor(),
        ]);

        setPendientes((pendientesData || []).map(normalizeInscripcion));
        setRealizadas((realizadasData || []).map(normalizeInscripcion));
        return;
      }

      if (rol === 'ROLE_PROFESOR') {
        const realizadasData = await InscripcionService.listaRealizadasAlumno();

        setPendientes([]);
        setRealizadas((realizadasData || []).map(normalizeInscripcion));
        setTab('realizadas');
        return;
      }

      if (rol === 'ROLE_ALUMNO') {
        try {
          const cursosInscritos = await CursoService.listarInscritosAlumno();
          setPendientes([]);
          setRealizadas((cursosInscritos || []).map(normalizeCursoInscrito));
          setTab('realizadas');
        } catch {
          setPendientes([]);
          setRealizadas([]);
          setTab('realizadas');
          setErrorMsg(
            'No se pudieron cargar tus cursos inscritos. Verifica el endpoint de cursos inscritos en backend.'
          );
        }
      }
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data;
      setErrorMsg(backendMessage || 'No se pudo cargar el listado de inscripciones.');
      setPendientes([]);
      setRealizadas([]);
    } finally {
      setIsLoading(false);
    }
  }, [rol]);

  useEffect(() => {
    loadInscripciones();
  }, [loadInscripciones]);

  useEffect(() => {
    if (rol === 'ROLE_PROFESOR') {
      setTab('realizadas');
    }
    setCursoFilter('');
  }, [rol]);

  const handleAprobar = async (item) => {
    try {
      setErrorMsg('');
      setSuccessMsg('');
      if (rol === 'ROLE_ADMIN') {
        await InscripcionService.aprobarProfesor(item.id);
      } else {
        await InscripcionService.aprobar(item.id);
      }
      setSuccessMsg('Inscripción aprobada correctamente.');
      await loadInscripciones();
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data;
      setErrorMsg(backendMessage || 'No se pudo aprobar la inscripción.');
    }
  };

  const handleRechazar = async (item) => {
    try {
      setErrorMsg('');
      setSuccessMsg('');
      if (rol === 'ROLE_ADMIN') {
        await InscripcionService.rechazarProfesor(item.id);
      } else {
        await InscripcionService.rechazar(item.id);
      }
      setSuccessMsg('Inscripción rechazada correctamente.');
      await loadInscripciones();
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data;
      setErrorMsg(backendMessage || 'No se pudo rechazar la inscripción.');
    }
  };

  const columns =
    rol === 'ROLE_ALUMNO'
      ? [
          { id: 'cursoNombre', label: 'Curso' },
          { id: 'descripcion', label: 'Descripción' },
          { id: 'creditos', label: 'Créditos', align: 'center' },
          { id: 'profesorAsignado', label: 'Profesor' },
          { id: 'estado', label: 'Estado', align: 'center' },
        ]
      : [
          { id: 'usuarioNombre', label: 'Usuario' },
          { id: 'usuarioEmail', label: 'Email' },
          { id: 'cursoNombre', label: 'Curso' },
          { id: 'estado', label: 'Estado', align: 'center' },
          { id: 'fechaCreacion', label: 'Fecha' },
        ];

  const pendingActions = rol !== 'ROLE_ADMIN'
    ? undefined
    : [
        {
          label: 'Aprobar',
          icon: <CheckCircle />,
          color: 'success',
          onClick: handleAprobar,
        },
        {
          label: 'Rechazar',
          icon: <Close />,
          color: 'error',
          onClick: handleRechazar,
        },
      ];

  const cursosProfesor = useMemo(() => {
    if (rol !== 'ROLE_PROFESOR') return [];
    const unique = new Set();
    realizadas.forEach((item) => {
      const nombre = item?.cursoNombre || '-';
      if (nombre && nombre !== '-') unique.add(nombre);
    });
    return Array.from(unique).sort((a, b) => a.localeCompare(b));
  }, [rol, realizadas]);

  const alumnoActions = useMemo(() => {
    if (rol !== 'ROLE_ALUMNO') return undefined;

    return [
      {
        label: 'Ver Actividades',
        icon: <ListAlt />,
        color: 'primary',
        onClick: (item) => {
          if (!item?.id || item.id === '-') return;
          navigate(`/modulo/actividades?cursoId=${item.id}`);
        },
      },
    ];
  }, [rol, navigate]);

  const realizadasFiltradas = useMemo(() => {
    if (rol !== 'ROLE_PROFESOR') return realizadas;
    if (!cursoFilter) return realizadas;
    return realizadas.filter((item) => item?.cursoNombre === cursoFilter);
  }, [rol, cursoFilter, realizadas]);

  const tabData = tab === 'pendientes' ? pendientes : rol === 'ROLE_PROFESOR' ? realizadasFiltradas : realizadas;
  const tabActions = tab === 'pendientes' ? pendingActions : undefined;
  const tableActions = rol === 'ROLE_ALUMNO' ? alumnoActions : tabActions;

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 3, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/cursos')}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          {headersByRole[rol] || 'Inscripciones'}
        </Typography>
        <Button variant="outlined" startIcon={<Refresh />} onClick={loadInscripciones} disabled={isLoading}>
          Recargar
        </Button>
      </Stack>

      <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
        {rol === 'ROLE_ADMIN' && (
          <Chip label={`Pendientes: ${pendientes.length}`} color="warning" variant="outlined" />
        )}
        <Chip
          label={
            rol === 'ROLE_ALUMNO'
              ? `Cursos inscritos: ${realizadas.length}`
              : rol === 'ROLE_PROFESOR'
                ? `Realizadas: ${realizadasFiltradas.length}`
                : `Realizadas: ${realizadas.length}`
          }
          color="primary"
          variant="outlined"
        />
      </Stack>

      {rol === 'ROLE_PROFESOR' && (
        <TextField
          select
          fullWidth
          label="Filtrar por curso"
          value={cursoFilter}
          onChange={(e) => setCursoFilter(e.target.value)}
          sx={{ mb: 2, bgcolor: 'background.paper' }}
        >
          <MenuItem value="">Todos los cursos</MenuItem>
          {cursosProfesor.map((curso) => (
            <MenuItem key={curso} value={curso}>
              {curso}
            </MenuItem>
          ))}
        </TextField>
      )}

      {errorMsg && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {errorMsg}
        </Alert>
      )}

      {successMsg && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {successMsg}
        </Alert>
      )}

      {rol === 'ROLE_ADMIN' && (
        <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
          <Tab label="Pendientes" value="pendientes" />
          <Tab label="Realizadas" value="realizadas" />
        </Tabs>
      )}

      <DataTable
        columns={columns}
        data={tabData}
        actions={tableActions}
        exportFileName={
          rol === 'ROLE_ALUMNO'
            ? 'mis-cursos-inscritos'
            : rol === 'ROLE_PROFESOR'
              ? 'inscripciones-realizadas-profesor'
              : `inscripciones-${tab}`
        }
      />
    </Box>
  );
};

export default Inscripciones;
