import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Avatar,
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
import { ArrowBack, Send, SmartToy } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import IaService from '../services/IaService';
import CursoService from '../services/CursoService';

const roleLabelByCode = {
  ROLE_ADMIN: 'Administrador',
  ROLE_PROFESOR: 'Profesor',
  ROLE_ALUMNO: 'Alumno',
};

const formatDateTime = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleString();
};

const createMsg = (type, text, fecha) => ({
  id: `${type}-${Date.now()}-${Math.random().toString(36).slice(2)}`,
  type,
  text,
  fecha,
});

const EstudIA = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol') || 'ROLE_ALUMNO';
  const canGenerateRubric = rol === 'ROLE_PROFESOR' || rol === 'ROLE_ADMIN';
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [loadingHistorial, setLoadingHistorial] = useState(true);
  const [sending, setSending] = useState(false);
  const [rubricLoading, setRubricLoading] = useState(false);
  const [rubrica, setRubrica] = useState(null);
  const [cursosRubrica, setCursosRubrica] = useState([]);
  const [cursoRubricaId, setCursoRubricaId] = useState('');
  const [rubricaForm, setRubricaForm] = useState({
    tema: '',
    nivelEducativo: 'Universitario',
    asignatura: '',
    tipoTarea: 'Proyecto',
    cantidadCriterios: 4,
    cantidadNiveles: 4,
    puntajeMaximo: 20,
  });
  const bottomRef = useRef(null);

  const rolLabel = useMemo(() => roleLabelByCode[rol] || 'Usuario', [rol]);

  useEffect(() => {
    const cargarHistorial = async () => {
      try {
        setLoadingHistorial(true);
        setErrorMsg('');

        const historial = await IaService.obtenerUltimoHistorial();

        if (historial?.ultimoMensaje && historial?.ultimaRespuesta) {
          setMessages([
            createMsg('user', historial.ultimoMensaje, historial.fechaActualizacion),
            createMsg('assistant', historial.ultimaRespuesta, historial.fechaActualizacion),
          ]);
        } else {
          setMessages([
            createMsg(
              'assistant',
              `Hola, soy EstudIA. Estoy listo para ayudarte con consultas de ${rolLabel.toLowerCase()}.`,
              new Date().toISOString(),
            ),
          ]);
        }
      } catch (error) {
        const backendMessage = error?.response?.data?.error || error?.response?.data || error?.message;
        setErrorMsg(backendMessage || 'No se pudo cargar el historial de conversación.');
        setMessages([
          createMsg(
            'assistant',
            `Hola, soy EstudIA. Puedes empezar una nueva conversación para el rol ${rolLabel.toLowerCase()}.`,
            new Date().toISOString(),
          ),
        ]);
      } finally {
        setLoadingHistorial(false);
      }
    };

    cargarHistorial();
  }, [rolLabel]);

  useEffect(() => {
    if (!canGenerateRubric) return;

    let active = true;

    const cargarCursosParaRubrica = async () => {
      try {
        let data = [];
        if (rol === 'ROLE_PROFESOR') {
          data = await CursoService.listarInscritosProfesor();
        } else if (rol === 'ROLE_ADMIN') {
          data = await CursoService.listarActivos();
        }

        const normalizados = Array.isArray(data) ? data : [];
        if (!active) return;

        setCursosRubrica(normalizados);

        if (normalizados.length > 0) {
          const firstId = normalizados[0]?.id || normalizados[0]?._id || '';
          setCursoRubricaId(firstId);
        }
      } catch {
        if (active) {
          setCursosRubrica([]);
          setCursoRubricaId('');
        }
      }
    };

    cargarCursosParaRubrica();

    return () => {
      active = false;
    };
  }, [canGenerateRubric, rol]);

  useEffect(() => {
    if (!canGenerateRubric || !cursoRubricaId) return;

    const curso = cursosRubrica.find((c) => (c?.id || c?._id) === cursoRubricaId);
    const nombreCurso = curso?.nombre || '';
    if (!nombreCurso) return;

    setRubricaForm((prev) => ({
      ...prev,
      tema: nombreCurso,
      asignatura: prev.asignatura?.trim() ? prev.asignatura : nombreCurso,
    }));
  }, [canGenerateRubric, cursoRubricaId, cursosRubrica]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, sending]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || sending) return;

    const userMsg = createMsg('user', text, new Date().toISOString());
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setSending(true);
    setErrorMsg('');

    try {
      const result = await IaService.enviarMensaje(text, rol);
      const assistantMsg = createMsg(
        'assistant',
        result?.respuesta || 'No hubo respuesta de EstudIA.',
        result?.fecha || new Date().toISOString(),
      );
      setMessages((prev) => [...prev, assistantMsg]);
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data || error?.message;
      setErrorMsg(backendMessage || 'No se pudo enviar tu consulta a EstudIA.');
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSend();
    }
  };

  const handleGenerateRubric = async () => {
    if (!canGenerateRubric) return;

    if (!rubricaForm.tema.trim() || !rubricaForm.asignatura.trim()) {
      setErrorMsg('Para generar la rúbrica debes completar Tema y Asignatura.');
      return;
    }

    try {
      setRubricLoading(true);
      setErrorMsg('');

      const payload = {
        ...rubricaForm,
        tema: rubricaForm.tema.trim(),
        asignatura: rubricaForm.asignatura.trim(),
        cantidadCriterios: Number(rubricaForm.cantidadCriterios),
        cantidadNiveles: Number(rubricaForm.cantidadNiveles),
        puntajeMaximo: Number(rubricaForm.puntajeMaximo),
      };

      const generated = await IaService.generarRubrica(payload);
      setRubrica(generated || null);
    } catch (error) {
      const backendMessage = error?.response?.data?.error || error?.response?.data?.mensaje || error?.message;
      setErrorMsg(backendMessage || 'No se pudo generar la rúbrica por IA.');
    } finally {
      setRubricLoading(false);
    }
  };

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 2, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/dashboard')}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          EstudIA
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Rol activo: {rolLabel}
        </Typography>
      </Stack>

      {errorMsg && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {String(errorMsg)}
        </Alert>
      )}

      {canGenerateRubric && (
        <Paper sx={{ p: 2, mb: 2 }}>
          <Typography variant="h6" sx={{ mb: 1 }}>
            Generador de Rúbrica por IA
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Define el contexto y genera una rúbrica de evaluación automáticamente.
          </Typography>

          <Stack spacing={1.2}>
            {cursosRubrica.length > 0 && (
              <TextField
                select
                fullWidth
                label="Curso (autocompleta tema)"
                value={cursoRubricaId}
                onChange={(e) => setCursoRubricaId(e.target.value)}
              >
                {cursosRubrica.map((curso) => {
                  const cursoId = curso?.id || curso?._id;
                  return (
                    <MenuItem key={cursoId} value={cursoId}>
                      {curso?.nombre || 'Curso'}
                    </MenuItem>
                  );
                })}
              </TextField>
            )}

            <TextField
              label="Tema"
              value={rubricaForm.tema}
              onChange={(e) => setRubricaForm((prev) => ({ ...prev, tema: e.target.value }))}
              fullWidth
            />

            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.2}>
              <TextField
                select
                fullWidth
                label="Nivel educativo"
                value={rubricaForm.nivelEducativo}
                onChange={(e) => setRubricaForm((prev) => ({ ...prev, nivelEducativo: e.target.value }))}
              >
                <MenuItem value="Primaria">Primaria</MenuItem>
                <MenuItem value="Secundaria">Secundaria</MenuItem>
                <MenuItem value="Universitario">Universitario</MenuItem>
                <MenuItem value="Posgrado">Posgrado</MenuItem>
              </TextField>

              <TextField
                fullWidth
                label="Asignatura"
                value={rubricaForm.asignatura}
                onChange={(e) => setRubricaForm((prev) => ({ ...prev, asignatura: e.target.value }))}
              />
            </Stack>

            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.2}>
              <TextField
                select
                fullWidth
                label="Tipo de tarea"
                value={rubricaForm.tipoTarea}
                onChange={(e) => setRubricaForm((prev) => ({ ...prev, tipoTarea: e.target.value }))}
              >
                <MenuItem value="Proyecto">Proyecto</MenuItem>
                <MenuItem value="Ensayo">Ensayo</MenuItem>
                <MenuItem value="Exposición">Exposición</MenuItem>
                <MenuItem value="Práctica">Práctica</MenuItem>
                <MenuItem value="Investigación">Investigación</MenuItem>
              </TextField>

              <TextField
                type="number"
                fullWidth
                label="Cantidad de criterios"
                value={rubricaForm.cantidadCriterios}
                onChange={(e) => setRubricaForm((prev) => ({ ...prev, cantidadCriterios: e.target.value }))}
                slotProps={{ htmlInput: { min: 2, max: 10 } }}
              />

              <TextField
                type="number"
                fullWidth
                label="Cantidad de niveles"
                value={rubricaForm.cantidadNiveles}
                onChange={(e) => setRubricaForm((prev) => ({ ...prev, cantidadNiveles: e.target.value }))}
                slotProps={{ htmlInput: { min: 2, max: 6 } }}
              />

              <TextField
                type="number"
                fullWidth
                label="Puntaje máximo"
                value={rubricaForm.puntajeMaximo}
                onChange={(e) => setRubricaForm((prev) => ({ ...prev, puntajeMaximo: e.target.value }))}
                slotProps={{ htmlInput: { min: 1, max: 100 } }}
              />
            </Stack>

            <Stack direction="row" justifyContent="flex-end">
              <Button variant="contained" onClick={handleGenerateRubric} disabled={rubricLoading}>
                {rubricLoading ? 'Generando...' : 'Generar Rúbrica'}
              </Button>
            </Stack>
          </Stack>

          {rubrica && (
            <Box sx={{ mt: 2 }}>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="h6">{rubrica.titulo || 'Rúbrica generada'}</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                {rubrica.descripcion}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Modelo: {rubrica.modelo || '-'} | Generada por IA: {rubrica.generadaPorIa ? 'Sí' : 'No'}
              </Typography>

              <Stack spacing={1.5} sx={{ mt: 2 }}>
                {(rubrica.criterios || []).map((criterio, index) => (
                  <Paper key={`${criterio?.nombre || 'criterio'}-${index}`} variant="outlined" sx={{ p: 1.5 }}>
                    <Typography variant="subtitle2">
                      {index + 1}. {criterio?.nombre || 'Criterio'} (Peso: {criterio?.peso ?? 0})
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      {criterio?.descripcion || '-'}
                    </Typography>
                    <Stack spacing={0.6}>
                      {(criterio?.niveles || []).map((nivel, idxNivel) => (
                        <Typography key={`${nivel?.nombre || 'nivel'}-${idxNivel}`} variant="caption" color="text.secondary">
                          - {nivel?.nombre || 'Nivel'} ({nivel?.puntaje ?? 0} pts): {nivel?.descriptor || '-'}
                        </Typography>
                      ))}
                    </Stack>
                  </Paper>
                ))}
              </Stack>
            </Box>
          )}
        </Paper>
      )}

      <Paper sx={{ p: 2, height: '65vh', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ flex: 1, overflowY: 'auto', p: 1, borderRadius: 1, bgcolor: 'background.default' }}>
          {loadingHistorial ? (
            <Stack direction="row" spacing={1} sx={{ p: 2, justifyContent: 'center', alignItems: 'center' }}>
              <CircularProgress size={20} />
              <Typography variant="body2" color="text.secondary">Cargando conversación...</Typography>
            </Stack>
          ) : (
            messages.map((msg) => (
              <Box
                key={msg.id}
                sx={{
                  display: 'flex',
                  justifyContent: msg.type === 'user' ? 'flex-end' : 'flex-start',
                  mb: 1.5,
                }}
              >
                <Box
                  sx={{
                    maxWidth: '80%',
                    px: 1.5,
                    py: 1,
                    borderRadius: 2,
                    bgcolor: msg.type === 'user' ? 'primary.main' : 'background.paper',
                    color: msg.type === 'user' ? 'primary.contrastText' : 'text.primary',
                    border: msg.type === 'assistant' ? (theme) => `1px solid ${theme.palette.divider}` : 'none',
                  }}
                >
                  {msg.type === 'assistant' && (
                    <Stack direction="row" spacing={1} sx={{ mb: 0.5, alignItems: 'center' }}>
                      <Avatar sx={{ width: 20, height: 20, bgcolor: 'secondary.main' }}>
                        <SmartToy sx={{ fontSize: 14 }} />
                      </Avatar>
                      <Typography variant="caption" color="text.secondary">EstudIA</Typography>
                    </Stack>
                  )}
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {msg.text}
                  </Typography>
                  <Typography
                    variant="caption"
                    sx={{
                      display: 'block',
                      mt: 0.5,
                      opacity: 0.8,
                      textAlign: msg.type === 'user' ? 'right' : 'left',
                    }}
                  >
                    {formatDateTime(msg.fecha)}
                  </Typography>
                </Box>
              </Box>
            ))
          )}

          {sending && (
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', px: 1, py: 0.5 }}>
              <CircularProgress size={16} />
              <Typography variant="caption" color="text.secondary">
                EstudIA está escribiendo...
              </Typography>
            </Stack>
          )}
          <div ref={bottomRef} />
        </Box>

        <Stack direction="row" spacing={1.2} sx={{ mt: 1.5 }}>
          <TextField
            fullWidth
            multiline
            maxRows={3}
            placeholder="Escribe tu consulta..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={sending}
          />
          <Button
            variant="contained"
            endIcon={<Send />}
            onClick={handleSend}
            disabled={sending || !input.trim()}
          >
            Enviar
          </Button>
        </Stack>
      </Paper>
    </Box>
  );
};

export default EstudIA;
