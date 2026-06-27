import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { AutoAwesome, AutoStories } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import IaService from '../services/IaService';
import CursoService from '../services/CursoService';
import CarreraService from '../services/CarreraService';
import PageHeader from '../components/PageHeader';
import useIaGeneracionStore from '../store/useIaGeneracionStore';

const defaultForm = {
  nombreCurso: '',
  carrera: '',
  ciclo: 1,
  creditos: 3,
  semanas: 16,
  descripcionBreve: '',
};

const resolveCarreraNombre = (carreraValue, fallback = '') => {
  if (typeof carreraValue === 'string') return carreraValue;
  if (carreraValue && typeof carreraValue === 'object') {
    return carreraValue?.nombre || carreraValue?.name || fallback;
  }
  return fallback;
};

const SilaboIA = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol') || 'ROLE_ALUMNO';
  const canGenerate = rol === 'ROLE_PROFESOR' || rol === 'ROLE_ADMIN';

  const [cursos, setCursos] = useState([]);
  const [carreras, setCarreras] = useState([]);
  const [cursoId, setCursoId] = useState('');
  const [form, setForm] = useState(defaultForm);
  const [validacionError, setValidacionError] = useState('');

  const { silabo, iniciarSilabo, completarSilabo, fallarSilabo, limpiarSilabo, marcarSilaboVisto } =
    useIaGeneracionStore();

  const cargando = silabo.estado === 'cargando';
  const listo = silabo.estado === 'listo';
  const conError = silabo.estado === 'error';

  // Marcar como visto al entrar a la página si ya estaba listo
  useEffect(() => {
    if ((listo || conError) && !silabo.notificacionVista) {
      marcarSilaboVisto();
    }
  }, [listo, conError, silabo.notificacionVista, marcarSilaboVisto]);

  useEffect(() => {
    if (!canGenerate) return;
    let active = true;

    const cargarCursos = async () => {
      try {
        let data = [];
        if (rol === 'ROLE_PROFESOR') data = await CursoService.listarInscritosProfesor();
        else if (rol === 'ROLE_ADMIN') data = await CursoService.listarActivos();
        if (!active) return;
        const norm = Array.isArray(data) ? data : [];
        setCursos(norm);
        if (norm.length > 0) setCursoId(norm[0]?.id || norm[0]?._id || '');
      } catch {
        if (active) { setCursos([]); setCursoId(''); }
      }
    };

    const cargarCarreras = async () => {
      try {
        const data = await CarreraService.listar();
        const norm = Array.isArray(data)
          ? data
              .map((item) => ({
                id: item?.id || item?._id || item?.codigo || item?.nombre,
                nombre: item?.nombre || '',
              }))
              .filter((item) => item.nombre)
          : [];
        if (!active) return;
        setCarreras(norm);
        setForm((prev) => ({ ...prev, carrera: prev.carrera || norm[0]?.nombre || '' }));
      } catch {
        if (active) setCarreras([]);
      }
    };

    cargarCursos();
    cargarCarreras();
    return () => { active = false; };
  }, [canGenerate, rol]);

  useEffect(() => {
    if (!canGenerate || !cursoId) return;
    const curso = cursos.find((c) => (c?.id || c?._id) === cursoId);
    if (!curso) return;
    setForm((prev) => ({
      ...prev,
      nombreCurso: curso?.nombre || prev.nombreCurso,
      carrera: resolveCarreraNombre(curso?.carrera, curso?.nombreCarrera) || prev.carrera,
      ciclo: Number(curso?.ciclo ?? prev.ciclo),
      creditos: Number(curso?.creditos ?? prev.creditos),
      descripcionBreve: curso?.descripcion || prev.descripcionBreve,
    }));
  }, [canGenerate, cursoId, cursos]);

  const handleGenerar = () => {
    if (!canGenerate) return;
    if (!form.nombreCurso.trim() || !form.descripcionBreve.trim()) {
      setValidacionError('Completa al menos Nombre del curso y Descripción breve.');
      return;
    }
    setValidacionError('');

    const payload = {
      cursoId: cursoId || undefined,
      nombreCurso: form.nombreCurso.trim(),
      carrera: form.carrera.trim(),
      ciclo: Number(form.ciclo),
      creditos: Number(form.creditos),
      semanas: Number(form.semanas),
      descripcionBreve: form.descripcionBreve.trim(),
    };

    iniciarSilabo();

    IaService.generarSilabo(payload)
      .then((result) => completarSilabo(result))
      .catch((err) => {
        const msg = err?.response?.data?.error || err?.response?.data?.mensaje || err?.message || 'Error al generar el sílabo.';
        fallarSilabo(msg);
      });
  };

  if (!canGenerate) {
    return (
      <Alert severity="info">
        El generador de sílabo está disponible para perfiles Profesor y Administrador.
      </Alert>
    );
  }

  return (
    <Box>
      <PageHeader
        title="Generador de Sílabo IA"
        subtitle="Genera un sílabo completo según el formato del backend."
        icon={<AutoStories />}
        onBack={() => navigate('/modulo/ia')}
      />

      {/* Banner de generación en segundo plano */}
      {cargando && (
        <Alert
          severity="info"
          icon={<CircularProgress size={18} />}
          sx={{ mb: 2 }}
        >
          Tu sílabo se está generando en segundo plano. Puedes navegar por el sistema
          con normalidad y recibirás una notificación cuando esté listo.
        </Alert>
      )}

      {conError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={limpiarSilabo}>
          {silabo.error || 'No se pudo generar el sílabo. Intenta nuevamente.'}
        </Alert>
      )}

      {validacionError && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setValidacionError('')}>
          {validacionError}
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack spacing={1.2}>
          {cursos.length > 0 && (
            <TextField
              select
              fullWidth
              label="Curso (autocompleta datos)"
              value={cursoId}
              onChange={(e) => setCursoId(e.target.value)}
            >
              {cursos.map((curso) => {
                const id = curso?.id || curso?._id;
                return (
                  <MenuItem key={id} value={id}>
                    {curso?.nombre || 'Curso'}
                  </MenuItem>
                );
              })}
            </TextField>
          )}

          <TextField
            label="Nombre del curso"
            value={form.nombreCurso}
            onChange={(e) => setForm((p) => ({ ...p, nombreCurso: e.target.value }))}
            fullWidth
          />

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.2}>
            <TextField
              select
              label="Carrera"
              value={form.carrera}
              onChange={(e) => setForm((p) => ({ ...p, carrera: e.target.value }))}
              helperText={carreras.length === 0 ? 'No hay carreras registradas.' : ''}
              fullWidth
            >
              {carreras.map((c) => (
                <MenuItem key={c.id} value={c.nombre}>
                  {c.nombre}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              type="number"
              label="Ciclo"
              value={form.ciclo}
              onChange={(e) => setForm((p) => ({ ...p, ciclo: e.target.value }))}
              slotProps={{ htmlInput: { min: 1, max: 15 } }}
              fullWidth
            />
            <TextField
              type="number"
              label="Créditos"
              value={form.creditos}
              onChange={(e) => setForm((p) => ({ ...p, creditos: e.target.value }))}
              slotProps={{ htmlInput: { min: 1, max: 10 } }}
              fullWidth
            />
            <TextField
              type="number"
              label="Semanas"
              value={form.semanas}
              onChange={(e) => setForm((p) => ({ ...p, semanas: e.target.value }))}
              slotProps={{ htmlInput: { min: 4, max: 20 } }}
              fullWidth
            />
          </Stack>

          <TextField
            label="Descripción breve"
            value={form.descripcionBreve}
            onChange={(e) => setForm((p) => ({ ...p, descripcionBreve: e.target.value }))}
            multiline
            minRows={3}
            fullWidth
          />

          <Stack direction="row" spacing={1}>
            {listo && (
              <Button variant="outlined" onClick={limpiarSilabo}>
                Generar otro
              </Button>
            )}
            <Button
              variant="contained"
              onClick={handleGenerar}
              disabled={cargando}
              startIcon={cargando ? <CircularProgress size={16} color="inherit" /> : <AutoAwesome />}
            >
              {cargando ? 'Generando...' : 'Generar Sílabo'}
            </Button>
          </Stack>
        </Stack>
      </Paper>

      {/* Resultado */}
      {listo && silabo.datos && (
        <Paper sx={{ p: 2 }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
            <AutoStories color="primary" />
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              Sílabo generado
            </Typography>
            <AutoAwesome color="primary" fontSize="small" />
          </Stack>

          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.2 }}>
            Curso: {silabo.datos?.informacionGeneral?.curso || '-'} | Carrera:{' '}
            {silabo.datos?.informacionGeneral?.carrera || '-'} | Ciclo:{' '}
            {silabo.datos?.informacionGeneral?.ciclo ?? '-'} | Créditos:{' '}
            {silabo.datos?.informacionGeneral?.creditos ?? '-'}
          </Typography>

          <Typography variant="subtitle2">Sumilla</Typography>
          <Typography variant="body2" sx={{ mb: 1.2, whiteSpace: 'pre-wrap' }}>
            {silabo.datos?.sumilla || '-'}
          </Typography>

          <Typography variant="subtitle2">Logro del curso</Typography>
          <Typography variant="body2" sx={{ mb: 1.2, whiteSpace: 'pre-wrap' }}>
            {silabo.datos?.logroCurso || '-'}
          </Typography>

          <Typography variant="subtitle2">Competencias generales</Typography>
          <Box component="ul" sx={{ mt: 0.5, mb: 1.2 }}>
            {(silabo.datos?.competenciasGenerales || []).map((item, idx) => (
              <Typography component="li" variant="body2" key={`cg-${idx}`}>
                {item}
              </Typography>
            ))}
          </Box>

          <Typography variant="subtitle2">Competencias específicas</Typography>
          <Box component="ul" sx={{ mt: 0.5, mb: 1.2 }}>
            {(silabo.datos?.competenciasEspecificas || []).map((item, idx) => (
              <Typography component="li" variant="body2" key={`ce-${idx}`}>
                {item}
              </Typography>
            ))}
          </Box>

          <Divider sx={{ my: 1.5 }} />

          <Typography variant="subtitle1" sx={{ mb: 1 }}>
            Unidades y semanas
          </Typography>

          <Stack spacing={1.5}>
            {(silabo.datos?.unidades || []).map((unidad, idxUnidad) => (
              <Paper key={`unidad-${idxUnidad}`} variant="outlined" sx={{ p: 1.2 }}>
                <Typography variant="subtitle2">
                  Unidad {idxUnidad + 1}: {unidad?.tituloUnidad || '-'}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 0.8 }}>
                  Logro: {unidad?.logroUnidad || '-'}
                </Typography>
                {(unidad?.semanas || []).map((semana, idxSemana) => (
                  <Box key={`semana-${idxSemana}`} sx={{ mb: 0.8 }}>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      Semana {semana?.numeroSemana ?? idxSemana + 1}
                    </Typography>
                    <Typography variant="caption" display="block">
                      Temas: {semana?.temas || '-'}
                    </Typography>
                    <Typography variant="caption" display="block">
                      Actividades: {semana?.actividadesPracticas || '-'}
                    </Typography>
                    <Typography variant="caption" display="block">
                      Evaluación: {semana?.evaluacion || '-'}
                    </Typography>
                  </Box>
                ))}
              </Paper>
            ))}
          </Stack>

          <Divider sx={{ my: 1.5 }} />
          <Typography variant="subtitle2">Sistema de evaluación</Typography>
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
            {silabo.datos?.sistemaEvaluacion || '-'}
          </Typography>
        </Paper>
      )}
    </Box>
  );
};

export default SilaboIA;
