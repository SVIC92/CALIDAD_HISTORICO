import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { ArrowBack, Assignment, Grading, Refresh } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import DataTable from '../components/DataTable';
import CursoService from '../services/CursoService';
import ActividadService from '../services/ActividadService';
import ReporteService from '../services/ReporteService';

const getUserEmailFromToken = () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) return '';
    const [, payloadBase64] = token.split('.');
    if (!payloadBase64) return '';

    const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const payload = JSON.parse(window.atob(padded));
    return (payload?.sub || payload?.email || '').toLowerCase();
  } catch {
    return '';
  }
};

const isCursoAsignadoAProfesor = (curso, email) => {
  const profesor = curso?.profesorAsignado;
  if (!profesor || !email) return false;

  if (typeof profesor === 'string') {
    return profesor.toLowerCase() === email;
  }

  return (profesor?.email || '').toLowerCase() === email;
};

const formatDateTimeDisplay = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
};

const normalizeReporte = (reporte, rol) => {
  const usuario = reporte?.usuario || reporte?.alumno || reporte?.estudiante || reporte?.user || null;
  const nota = reporte?.nota;
  const comentario = reporte?.comentario || reporte?.observacion || '';

  return {
    id: reporte?.id || reporte?._id || reporte?.reporteId || '',
    alumnoNombre: usuario?.nombre || reporte?.alumnoNombre || (rol === 'ROLE_ALUMNO' ? 'Yo' : '-'),
    alumnoEmail: usuario?.email || reporte?.alumnoEmail || '-',
    respuesta: reporte?.respuesta || reporte?.contenido || reporte?.detalle || '-',
    nota: nota === null || nota === undefined ? '-' : nota,
    comentario: comentario || '-',
    estado: reporte?.estado || (nota === null || nota === undefined ? 'PENDIENTE' : 'CALIFICADO'),
    fechaRegistro: formatDateTimeDisplay(reporte?.fechaRegistro || reporte?.fechaCreacion || reporte?.fechaEnvio),
    raw: reporte,
  };
};

const hasReporteEnviado = (reporte) => {
  const estado = String(reporte?.estado || '').toUpperCase();
  if (estado.includes('ENTREG') || estado.includes('ENVIAD') || estado.includes('CALIFIC') || estado.includes('APROBAD')) {
    return true;
  }

  const respuesta = reporte?.respuesta;
  return typeof respuesta === 'string' ? respuesta.trim().length > 0 : Boolean(respuesta);
};

const Reportes = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const cursoIdParam = searchParams.get('cursoId') || '';
  const actividadIdParam = searchParams.get('actividadId') || '';
  const rol = localStorage.getItem('rol');
  const canCalificar = rol === 'ROLE_PROFESOR' || rol === 'ROLE_ADMIN';
  const canResponder = rol === 'ROLE_ALUMNO';

  const [cursos, setCursos] = useState([]);
  const [selectedCursoId, setSelectedCursoId] = useState('');
  const [actividades, setActividades] = useState([]);
  const [selectedActividadId, setSelectedActividadId] = useState('');
  const [reportes, setReportes] = useState([]);

  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const [openRespuesta, setOpenRespuesta] = useState(false);
  const [respuesta, setRespuesta] = useState('');
  const [isSavingRespuesta, setIsSavingRespuesta] = useState(false);

  const [openCalificacion, setOpenCalificacion] = useState(false);
  const [selectedReporteId, setSelectedReporteId] = useState('');
  const [nota, setNota] = useState('');
  const [comentario, setComentario] = useState('');
  const [isSavingCalificacion, setIsSavingCalificacion] = useState(false);

  const loadCursosByRole = useCallback(async () => {
    try {
      setErrorMsg('');
      let data = [];

      if (rol === 'ROLE_PROFESOR') {
        data = await CursoService.listarInscritosProfesor();
      } else if (rol === 'ROLE_ALUMNO') {
        data = await CursoService.listarInscritosAlumno();
      } else if (rol === 'ROLE_ADMIN') {
        data = await CursoService.listarActivos();
      }

      const normalized = Array.isArray(data) ? data : [];
      setCursos(normalized);

      if (normalized.length > 0) {
        const existsFromParam = normalized.some((curso) => (curso?.id || curso?._id) === cursoIdParam);
        if (cursoIdParam && existsFromParam) {
          setSelectedCursoId(cursoIdParam);
        } else {
          const firstId = normalized[0]?.id || normalized[0]?._id || '';
          setSelectedCursoId(firstId);
        }
      } else {
        setSelectedCursoId('');
      }
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data;
      setErrorMsg(backendMessage || 'No se pudieron cargar los cursos para reportes.');
      setCursos([]);
      setSelectedCursoId('');
    }
  }, [rol, cursoIdParam]);

  const loadActividades = useCallback(async (cursoId) => {
    if (!cursoId) {
      setActividades([]);
      setSelectedActividadId('');
      return;
    }

    try {
      setErrorMsg('');
      const data = await ActividadService.listar(cursoId);
      const actividadesData = Array.isArray(data)
        ? data
        : Array.isArray(data?.actividades)
          ? data.actividades
          : [];

      setActividades(actividadesData);
      if (actividadesData.length > 0) {
        const existsFromParam = actividadesData.some((actividad) => (actividad?.id || actividad?._id) === actividadIdParam);
        if (actividadIdParam && existsFromParam) {
          setSelectedActividadId(actividadIdParam);
        } else {
          const firstActividadId = actividadesData[0]?.id || actividadesData[0]?._id || '';
          setSelectedActividadId(firstActividadId);
        }
      } else {
        setSelectedActividadId('');
      }
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data;
      setErrorMsg(backendMessage || 'No se pudieron cargar las actividades del curso.');
      setActividades([]);
      setSelectedActividadId('');
    }
  }, [actividadIdParam]);

  const loadReportes = useCallback(async (actividadId) => {
    if (!actividadId) {
      setReportes([]);
      return;
    }

    try {
      setIsLoading(true);
      setErrorMsg('');
      let data;

      if (rol === 'ROLE_ALUMNO') {
        try {
          data = await ReporteService.detalle(actividadId);
        } catch (detailError) {
          const status = detailError?.response?.status;
          if (status === 403) {
            setErrorMsg('No tienes permisos para ver reportes de esta actividad.');
            setReportes([]);
            return;
          }
          const backendMessage = detailError?.response?.data?.error || detailError?.response?.data;
          setErrorMsg(backendMessage || 'No se pudieron cargar los reportes de esta actividad.');
          setReportes([]);
          return;
        }
      } else {
        data = await ReporteService.listar(actividadId);
      }

      const rawList = Array.isArray(data)
        ? data
        : Array.isArray(data?.reportes)
          ? data.reportes
          : data
            ? [data]
            : [];

      setReportes(rawList.map((item) => normalizeReporte(item, rol)));
    } catch (error) {
      const status = error?.response?.status;
      const backendMessage = error?.response?.data?.error || error?.response?.data;
      if (status === 403) {
        setErrorMsg('No tienes permisos para listar reportes en esta actividad.');
      } else {
        setErrorMsg(backendMessage || 'No se pudieron cargar los reportes.');
      }
      setReportes([]);
    } finally {
      setIsLoading(false);
    }
  }, [rol]);

  useEffect(() => {
    loadCursosByRole();
  }, [loadCursosByRole]);

  useEffect(() => {
    loadActividades(selectedCursoId);
  }, [selectedCursoId, loadActividades]);

  useEffect(() => {
    loadReportes(selectedActividadId);
  }, [selectedActividadId, loadReportes]);

  const columns = [
    { id: 'alumnoNombre', label: 'Alumno' },
    { id: 'alumnoEmail', label: 'Email' },
    { id: 'respuesta', label: 'Respuesta' },
    { id: 'nota', label: 'Nota', align: 'center' },
    { id: 'estado', label: 'Estado', align: 'center' },
    { id: 'comentario', label: 'Comentario' },
    { id: 'fechaRegistro', label: 'Fecha' },
  ];

  const alumnoYaEnvioReporte = useMemo(() => {
    if (!canResponder) return false;
    return reportes.some(hasReporteEnviado);
  }, [canResponder, reportes]);

  const handleRecargar = async () => {
    await loadReportes(selectedActividadId);
  };

  const handleAbrirResponder = async () => {
    if (!canResponder || !selectedActividadId) return;

    if (alumnoYaEnvioReporte) {
      setErrorMsg('Ya registraste un reporte para esta actividad. No puedes enviar otro.');
      return;
    }

    try {
      setErrorMsg('');
      const pref = await ReporteService.registrar(selectedActividadId);
      const prefRespuesta = pref?.respuesta || pref?.contenido || '';
      setRespuesta(prefRespuesta);
    } catch {
      setRespuesta('');
    }

    setOpenRespuesta(true);
  };

  const handleGuardarRespuesta = async () => {
    if (!canResponder || !selectedActividadId) return;

    const texto = respuesta.trim();
    if (!texto) {
      setErrorMsg('La respuesta no puede estar vacia.');
      return;
    }

    try {
      setIsSavingRespuesta(true);
      setErrorMsg('');
      setSuccessMsg('');
      await ReporteService.registro(selectedActividadId, { respuesta: texto });
      setOpenRespuesta(false);
      setSuccessMsg('Reporte enviado correctamente.');
      await loadReportes(selectedActividadId);
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data;
      setErrorMsg(backendMessage || 'No se pudo enviar el reporte.');
    } finally {
      setIsSavingRespuesta(false);
    }
  };

  const handleAbrirCalificar = (reporte) => {
    if (!canCalificar) return;

    const reporteId = reporte?.id || reporte?.raw?.id || reporte?.raw?._id;
    if (!reporteId) return;

    setSelectedReporteId(reporteId);
    setNota(reporte?.nota === '-' ? '' : String(reporte?.nota ?? ''));
    setComentario(reporte?.comentario === '-' ? '' : String(reporte?.comentario ?? ''));
    setOpenCalificacion(true);
  };

  const handleGuardarCalificacion = async () => {
    if (!canCalificar || !selectedReporteId) return;

    const notaNumero = Number(nota);
    if (Number.isNaN(notaNumero) || notaNumero < 0 || notaNumero > 20) {
      setErrorMsg('La nota debe ser un numero entre 0 y 20.');
      return;
    }

    try {
      setIsSavingCalificacion(true);
      setErrorMsg('');
      setSuccessMsg('');
      await ReporteService.calificar(selectedReporteId, {
        nota: notaNumero,
        comentario: comentario.trim(),
      });
      setOpenCalificacion(false);
      setSuccessMsg('Reporte calificado correctamente.');
      await loadReportes(selectedActividadId);
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data;
      setErrorMsg(backendMessage || 'No se pudo calificar el reporte.');
    } finally {
      setIsSavingCalificacion(false);
    }
  };

  const actions = canCalificar
    ? [
        {
          label: 'Calificar',
          icon: <Grading />,
          color: 'primary',
          onClick: handleAbrirCalificar,
        },
      ]
    : undefined;

  const titulo = useMemo(() => {
    if (rol === 'ROLE_ADMIN') return 'Reportes de Cursos';
    if (rol === 'ROLE_PROFESOR') return 'Reportes de Actividades';
    return 'Mis Reportes';
  }, [rol]);

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 3, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/cursos')}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          {titulo}
        </Typography>
        <Button variant="outlined" startIcon={<Refresh />} onClick={handleRecargar} disabled={isLoading || !selectedActividadId}>
          Recargar
        </Button>
        {canResponder && (
          <Button
            variant="contained"
            startIcon={<Assignment />}
            onClick={handleAbrirResponder}
            disabled={!selectedActividadId || alumnoYaEnvioReporte}
          >
            {alumnoYaEnvioReporte ? 'Reporte ya enviado' : 'Responder Actividad'}
          </Button>
        )}
      </Stack>

      <Stack spacing={2} sx={{ mb: 2 }}>
        <TextField
          select
          fullWidth
          label="Curso"
          value={selectedCursoId}
          onChange={(e) => setSelectedCursoId(e.target.value)}
          sx={{ bgcolor: 'background.paper' }}
        >
          {cursos.map((curso) => {
            const cursoId = curso?.id || curso?._id;
            return (
              <MenuItem key={cursoId} value={cursoId}>
                {curso?.nombre || 'Curso'}
              </MenuItem>
            );
          })}
        </TextField>

        <TextField
          select
          fullWidth
          label="Actividad"
          value={selectedActividadId}
          onChange={(e) => setSelectedActividadId(e.target.value)}
          sx={{ bgcolor: 'background.paper' }}
        >
          {actividades.map((actividad) => {
            const actividadId = actividad?.id || actividad?._id;
            return (
              <MenuItem key={actividadId} value={actividadId}>
                {actividad?.nombre || 'Actividad'}
              </MenuItem>
            );
          })}
        </TextField>
      </Stack>

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

      <DataTable
        columns={columns}
        data={reportes}
        actions={actions}
        exportFileName={`reportes-${selectedActividadId || 'actividad'}`}
      />

      <Dialog open={openRespuesta} onClose={() => !isSavingRespuesta && setOpenRespuesta(false)} fullWidth maxWidth="sm">
        <DialogTitle>Responder Actividad</DialogTitle>
        <DialogContent>
          <TextField
            margin="dense"
            label="Respuesta"
            value={respuesta}
            onChange={(e) => setRespuesta(e.target.value)}
            fullWidth
            required
            multiline
            minRows={4}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenRespuesta(false)} disabled={isSavingRespuesta}>Cancelar</Button>
          <Button onClick={handleGuardarRespuesta} variant="contained" disabled={isSavingRespuesta}>
            Enviar
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={openCalificacion} onClose={() => !isSavingCalificacion && setOpenCalificacion(false)} fullWidth maxWidth="sm">
        <DialogTitle>Calificar Reporte</DialogTitle>
        <DialogContent>
          <TextField
            margin="dense"
            label="Nota (0 a 20)"
            type="number"
            value={nota}
            onChange={(e) => setNota(e.target.value)}
            fullWidth
            required
            slotProps={{ htmlInput: { min: 0, max: 20, step: 1 } }}
          />
          <TextField
            margin="dense"
            label="Comentario"
            value={comentario}
            onChange={(e) => setComentario(e.target.value)}
            fullWidth
            multiline
            minRows={3}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCalificacion(false)} disabled={isSavingCalificacion}>Cancelar</Button>
          <Button onClick={handleGuardarCalificacion} variant="contained" disabled={isSavingCalificacion}>
            Guardar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Reportes;
