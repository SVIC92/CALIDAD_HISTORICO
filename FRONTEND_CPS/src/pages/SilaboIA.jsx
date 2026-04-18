import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Divider,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { ArrowBack, AutoStories } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import IaService from '../services/IaService';
import CursoService from '../services/CursoService';

const defaultForm = {
  nombreCurso: '',
  carrera: '',
  ciclo: 1,
  creditos: 3,
  semanas: 16,
  descripcionBreve: '',
};

const SilaboIA = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol') || 'ROLE_ALUMNO';
  const canGenerateSilabo = rol === 'ROLE_PROFESOR' || rol === 'ROLE_ADMIN';

  const [errorMsg, setErrorMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const [silabo, setSilabo] = useState(null);
  const [cursos, setCursos] = useState([]);
  const [cursoId, setCursoId] = useState('');
  const [form, setForm] = useState(defaultForm);

  useEffect(() => {
    if (!canGenerateSilabo) return;

    let active = true;

    const cargarCursos = async () => {
      try {
        let data = [];
        if (rol === 'ROLE_PROFESOR') {
          data = await CursoService.listarInscritosProfesor();
        } else if (rol === 'ROLE_ADMIN') {
          data = await CursoService.listarActivos();
        }

        const normalizados = Array.isArray(data) ? data : [];
        if (!active) return;

        setCursos(normalizados);

        if (normalizados.length > 0) {
          const firstId = normalizados[0]?.id || normalizados[0]?._id || '';
          setCursoId(firstId);
        }
      } catch {
        if (!active) return;
        setCursos([]);
        setCursoId('');
      }
    };

    cargarCursos();

    return () => {
      active = false;
    };
  }, [canGenerateSilabo, rol]);

  useEffect(() => {
    if (!canGenerateSilabo || !cursoId) return;

    const cursoSeleccionado = cursos.find((c) => (c?.id || c?._id) === cursoId);
    if (!cursoSeleccionado) return;

    setForm((prev) => ({
      ...prev,
      nombreCurso: cursoSeleccionado?.nombre || prev.nombreCurso,
      carrera: cursoSeleccionado?.carrera || cursoSeleccionado?.nombreCarrera || prev.carrera,
      ciclo: Number(cursoSeleccionado?.ciclo ?? prev.ciclo),
      creditos: Number(cursoSeleccionado?.creditos ?? prev.creditos),
      descripcionBreve: cursoSeleccionado?.descripcion || prev.descripcionBreve,
    }));
  }, [canGenerateSilabo, cursoId, cursos]);

  const handleGenerate = async () => {
    if (!canGenerateSilabo) return;

    if (!form.nombreCurso.trim() || !form.descripcionBreve.trim()) {
      setErrorMsg('Completa al menos Nombre del curso y Descripcion breve.');
      return;
    }

    try {
      setLoading(true);
      setErrorMsg('');

      const payload = {
        nombreCurso: form.nombreCurso.trim(),
        carrera: form.carrera.trim(),
        ciclo: Number(form.ciclo),
        creditos: Number(form.creditos),
        semanas: Number(form.semanas),
        descripcionBreve: form.descripcionBreve.trim(),
      };

      const result = await IaService.generarSilabo(payload);
      setSilabo(result || null);
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data?.mensaje || error?.message;
      setErrorMsg(backendMessage || 'No se pudo generar el silabo por IA.');
    } finally {
      setLoading(false);
    }
  };

  if (!canGenerateSilabo) {
    return (
      <Alert severity="info">
        El generador de silabo esta disponible para perfiles Profesor y Administrador.
      </Alert>
    );
  }

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 2, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/modulo/ia')}>
          Volver al IAHub
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Generador de Silabo IA
        </Typography>
      </Stack>

      {errorMsg && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {String(errorMsg)}
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Genera un silabo completo segun el formato del backend.
        </Typography>

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
            onChange={(e) => setForm((prev) => ({ ...prev, nombreCurso: e.target.value }))}
            fullWidth
          />

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.2}>
            <TextField
              label="Carrera"
              value={form.carrera}
              onChange={(e) => setForm((prev) => ({ ...prev, carrera: e.target.value }))}
              fullWidth
            />
            <TextField
              type="number"
              label="Ciclo"
              value={form.ciclo}
              onChange={(e) => setForm((prev) => ({ ...prev, ciclo: e.target.value }))}
              slotProps={{ htmlInput: { min: 1, max: 15 } }}
              fullWidth
            />
            <TextField
              type="number"
              label="Creditos"
              value={form.creditos}
              onChange={(e) => setForm((prev) => ({ ...prev, creditos: e.target.value }))}
              slotProps={{ htmlInput: { min: 1, max: 10 } }}
              fullWidth
            />
            <TextField
              type="number"
              label="Semanas"
              value={form.semanas}
              onChange={(e) => setForm((prev) => ({ ...prev, semanas: e.target.value }))}
              slotProps={{ htmlInput: { min: 4, max: 20 } }}
              fullWidth
            />
          </Stack>

          <TextField
            label="Descripcion breve"
            value={form.descripcionBreve}
            onChange={(e) => setForm((prev) => ({ ...prev, descripcionBreve: e.target.value }))}
            multiline
            minRows={3}
            fullWidth
          />

          <Stack direction="row" spacing={1}>
            <Button variant="contained" onClick={handleGenerate} disabled={loading}>
              {loading ? 'Generando...' : 'Generar Silabo'}
            </Button>
            <Button
              variant="outlined"
              onClick={() => {
                setForm(defaultForm);
                setSilabo(null);
                setErrorMsg('');
              }}
            >
              Limpiar
            </Button>
          </Stack>
        </Stack>
      </Paper>

      {silabo && (
        <Paper sx={{ p: 2 }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
            <AutoStories color="primary" />
            <Typography variant="h6">Silabo generado</Typography>
          </Stack>

          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.2 }}>
            Curso: {silabo?.informacionGeneral?.curso || '-'} | Carrera: {silabo?.informacionGeneral?.carrera || '-'} | Ciclo: {silabo?.informacionGeneral?.ciclo ?? '-'} | Creditos: {silabo?.informacionGeneral?.creditos ?? '-'}
          </Typography>

          <Typography variant="subtitle2">Sumilla</Typography>
          <Typography variant="body2" sx={{ mb: 1.2, whiteSpace: 'pre-wrap' }}>
            {silabo?.sumilla || '-'}
          </Typography>

          <Typography variant="subtitle2">Logro del curso</Typography>
          <Typography variant="body2" sx={{ mb: 1.2, whiteSpace: 'pre-wrap' }}>
            {silabo?.logroCurso || '-'}
          </Typography>

          <Typography variant="subtitle2">Competencias generales</Typography>
          <Box component="ul" sx={{ mt: 0.5, mb: 1.2 }}>
            {(silabo?.competenciasGenerales || []).map((item, idx) => (
              <Typography component="li" variant="body2" key={`cg-${idx}`}>
                {item}
              </Typography>
            ))}
          </Box>

          <Typography variant="subtitle2">Competencias especificas</Typography>
          <Box component="ul" sx={{ mt: 0.5, mb: 1.2 }}>
            {(silabo?.competenciasEspecificas || []).map((item, idx) => (
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
            {(silabo?.unidades || []).map((unidad, idxUnidad) => (
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
                    <Typography variant="caption" display="block">Temas: {semana?.temas || '-'}</Typography>
                    <Typography variant="caption" display="block">Actividades: {semana?.actividadesPracticas || '-'}</Typography>
                    <Typography variant="caption" display="block">Evaluacion: {semana?.evaluacion || '-'}</Typography>
                  </Box>
                ))}
              </Paper>
            ))}
          </Stack>

          <Divider sx={{ my: 1.5 }} />
          <Typography variant="subtitle2">Sistema de evaluacion</Typography>
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
            {silabo?.sistemaEvaluacion || '-'}
          </Typography>
        </Paper>
      )}
    </Box>
  );
};

export default SilaboIA;
