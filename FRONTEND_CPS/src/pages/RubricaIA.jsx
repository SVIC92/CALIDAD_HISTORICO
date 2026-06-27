import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { AutoAwesome, Description, PictureAsPdf } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { jsPDF } from 'jspdf';
import IaService from '../services/IaService';
import CursoService from '../services/CursoService';
import PageHeader from '../components/PageHeader';
import useIaGeneracionStore from '../store/useIaGeneracionStore';

const defaultForm = {
  tema: '',
  nivelEducativo: 'Universitario',
  asignatura: '',
  tipoTarea: 'Proyecto',
  cantidadCriterios: 4,
  cantidadNiveles: 4,
  puntajeMaximo: 20,
};

const escapeHtml = (value) =>
  String(value || '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');

const RubricaIA = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol') || 'ROLE_ALUMNO';
  const canGenerate = rol === 'ROLE_PROFESOR' || rol === 'ROLE_ADMIN';

  const [cursosRubrica, setCursosRubrica] = useState([]);
  const [cursoRubricaId, setCursoRubricaId] = useState('');
  const [rubricaForm, setRubricaForm] = useState(defaultForm);
  const [validacionError, setValidacionError] = useState('');

  const { rubrica, iniciarRubrica, completarRubrica, fallarRubrica, limpiarRubrica, marcarRubricaVista } =
    useIaGeneracionStore();

  const cargando = rubrica.estado === 'cargando';
  const lista = rubrica.estado === 'listo';
  const conError = rubrica.estado === 'error';

  // Marcar como vista al entrar a la página si ya estaba lista
  useEffect(() => {
    if ((lista || conError) && !rubrica.notificacionVista) {
      marcarRubricaVista();
    }
  }, [lista, conError, rubrica.notificacionVista, marcarRubricaVista]);

  useEffect(() => {
    if (!canGenerate) return;
    let active = true;

    const cargar = async () => {
      try {
        let data = [];
        if (rol === 'ROLE_PROFESOR') data = await CursoService.listarInscritosProfesor();
        else if (rol === 'ROLE_ADMIN') data = await CursoService.listarActivos();
        if (!active) return;
        const norm = Array.isArray(data) ? data : [];
        setCursosRubrica(norm);
        if (norm.length > 0) setCursoRubricaId(norm[0]?.id || norm[0]?._id || '');
      } catch {
        if (active) { setCursosRubrica([]); setCursoRubricaId(''); }
      }
    };

    cargar();
    return () => { active = false; };
  }, [canGenerate, rol]);

  useEffect(() => {
    if (!canGenerate || !cursoRubricaId) return;
    const curso = cursosRubrica.find((c) => (c?.id || c?._id) === cursoRubricaId);
    if (!curso) return;
    setRubricaForm((prev) => ({
      ...prev,
      tema: curso.nombre || prev.tema,
      asignatura: prev.asignatura?.trim() ? prev.asignatura : (curso.nombre || prev.asignatura),
    }));
  }, [canGenerate, cursoRubricaId, cursosRubrica]);

  const handleGenerar = () => {
    if (!canGenerate) return;
    if (!rubricaForm.tema.trim() || !rubricaForm.asignatura.trim()) {
      setValidacionError('Completa Tema y Asignatura para continuar.');
      return;
    }
    setValidacionError('');

    const payload = {
      ...rubricaForm,
      tema: rubricaForm.tema.trim(),
      asignatura: rubricaForm.asignatura.trim(),
      cantidadCriterios: Number(rubricaForm.cantidadCriterios),
      cantidadNiveles: Number(rubricaForm.cantidadNiveles),
      puntajeMaximo: Number(rubricaForm.puntajeMaximo),
    };

    iniciarRubrica();

    IaService.generarRubrica(payload)
      .then((result) => completarRubrica(result))
      .catch((err) => {
        const msg = err?.response?.data?.error || err?.message || 'Error al generar la rúbrica.';
        fallarRubrica(msg);
      });
  };

  const exportPdf = () => {
    const r = rubrica.datos;
    if (!r) return;

    const doc = new jsPDF({ unit: 'pt', format: 'a4' });
    const margin = 42;
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    const maxWidth = pageWidth - margin * 2;
    let y = margin;

    const ensureSpace = (needed = 18) => {
      if (y + needed > pageHeight - margin) { doc.addPage(); y = margin; }
    };
    const addLine = (text, size = 11, bold = false, extra = 6) => {
      const safeText = String(text || '-');
      doc.setFont('helvetica', bold ? 'bold' : 'normal');
      doc.setFontSize(size);
      const lines = doc.splitTextToSize(safeText, maxWidth);
      const height = lines.length * (size + 1) + extra;
      ensureSpace(height);
      doc.text(lines, margin, y);
      y += height;
    };

    addLine(r.titulo || 'Rúbrica generada por IA', 16, true, 10);
    addLine(`Descripción: ${r.descripcion || '-'}`, 10, false, 8);
    addLine(`Modelo: ${r.modelo || '-'} | Generada por IA: ${r.generadaPorIa ? 'Sí' : 'No'}`, 10, false, 12);

    (r.criterios || []).forEach((criterio, index) => {
      addLine(`${index + 1}. ${criterio?.nombre || 'Criterio'} (Peso: ${criterio?.peso ?? 0})`, 12, true, 4);
      addLine(`Descripción: ${criterio?.descripcion || '-'}`, 10, false, 6);
      (criterio?.niveles || []).forEach((nivel, i) => {
        addLine(`- ${nivel?.nombre || `Nivel ${i + 1}`} (${nivel?.puntaje ?? 0} pts): ${nivel?.descriptor || '-'}`, 10, false, 4);
      });
      y += 6;
    });

    doc.save(`${(r.titulo || 'rubrica-ia').toLowerCase().replace(/[^a-z0-9]+/gi, '-')}.pdf`);
  };

  const exportWord = () => {
    const r = rubrica.datos;
    if (!r) return;

    const rows = (r.criterios || [])
      .map((criterio, index) => {
        const niveles = (criterio?.niveles || [])
          .map((n) => `<li><b>${escapeHtml(n?.nombre || 'Nivel')}</b> (${escapeHtml(n?.puntaje ?? 0)} pts): ${escapeHtml(n?.descriptor || '-')}</li>`)
          .join('');
        return `<tr>
          <td style="border:1px solid #d0d7de;padding:8px;vertical-align:top;">${index + 1}</td>
          <td style="border:1px solid #d0d7de;padding:8px;vertical-align:top;">${escapeHtml(criterio?.nombre || 'Criterio')}</td>
          <td style="border:1px solid #d0d7de;padding:8px;vertical-align:top;">${escapeHtml(criterio?.descripcion || '-')}</td>
          <td style="border:1px solid #d0d7de;padding:8px;vertical-align:top;">${escapeHtml(criterio?.peso ?? 0)}</td>
          <td style="border:1px solid #d0d7de;padding:8px;vertical-align:top;"><ul>${niveles}</ul></td>
        </tr>`;
      })
      .join('');

    const html = `<html><head><meta charset="utf-8"/><title>${escapeHtml(r.titulo || 'Rúbrica IA')}</title></head>
      <body style="font-family:Arial,sans-serif;">
        <h1>${escapeHtml(r.titulo || 'Rúbrica generada por IA')}</h1>
        <p><b>Descripción:</b> ${escapeHtml(r.descripcion || '-')}</p>
        <p><b>Modelo:</b> ${escapeHtml(r.modelo || '-')} | <b>Generada por IA:</b> ${r.generadaPorIa ? 'Sí' : 'No'}</p>
        <table style="border-collapse:collapse;width:100%;">
          <thead><tr>
            <th style="border:1px solid #d0d7de;padding:8px;text-align:left;">#</th>
            <th style="border:1px solid #d0d7de;padding:8px;text-align:left;">Criterio</th>
            <th style="border:1px solid #d0d7de;padding:8px;text-align:left;">Descripción</th>
            <th style="border:1px solid #d0d7de;padding:8px;text-align:left;">Peso</th>
            <th style="border:1px solid #d0d7de;padding:8px;text-align:left;">Niveles</th>
          </tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </body></html>`;

    const blob = new Blob([html], { type: 'application/msword;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${(r.titulo || 'rubrica-ia').toLowerCase().replace(/[^a-z0-9]+/gi, '-')}.doc`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  };

  if (!canGenerate) {
    return (
      <Alert severity="info">
        El generador de rúbricas está disponible para perfiles Profesor y Administrador.
      </Alert>
    );
  }

  return (
    <Box>
      <PageHeader
        title="Generador de Rúbricas IA"
        subtitle="Define el contexto y genera una rúbrica de evaluación automáticamente."
        icon={<Description />}
        onBack={() => navigate('/modulo/ia')}
      />

      {/* Banner de generación en segundo plano */}
      {cargando && (
        <Alert
          severity="info"
          icon={<CircularProgress size={18} />}
          sx={{ mb: 2 }}
        >
          Tu rúbrica se está generando en segundo plano. Puedes navegar por el sistema
          con normalidad y recibirás una notificación cuando esté lista.
        </Alert>
      )}

      {conError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={limpiarRubrica}>
          {rubrica.error || 'No se pudo generar la rúbrica. Intenta nuevamente.'}
        </Alert>
      )}

      {validacionError && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setValidacionError('')}>
          {validacionError}
        </Alert>
      )}

      <Paper sx={{ p: 2 }}>
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
            label="Tema"
            value={rubricaForm.tema}
            onChange={(e) => setRubricaForm((p) => ({ ...p, tema: e.target.value }))}
            fullWidth
          />

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.2}>
            <TextField
              select
              fullWidth
              label="Nivel educativo"
              value={rubricaForm.nivelEducativo}
              onChange={(e) => setRubricaForm((p) => ({ ...p, nivelEducativo: e.target.value }))}
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
              onChange={(e) => setRubricaForm((p) => ({ ...p, asignatura: e.target.value }))}
            />
          </Stack>

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.2}>
            <TextField
              select
              fullWidth
              label="Tipo de tarea"
              value={rubricaForm.tipoTarea}
              onChange={(e) => setRubricaForm((p) => ({ ...p, tipoTarea: e.target.value }))}
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
              label="Criterios"
              value={rubricaForm.cantidadCriterios}
              onChange={(e) => setRubricaForm((p) => ({ ...p, cantidadCriterios: e.target.value }))}
              slotProps={{ htmlInput: { min: 2, max: 10 } }}
            />

            <TextField
              type="number"
              fullWidth
              label="Niveles"
              value={rubricaForm.cantidadNiveles}
              onChange={(e) => setRubricaForm((p) => ({ ...p, cantidadNiveles: e.target.value }))}
              slotProps={{ htmlInput: { min: 2, max: 6 } }}
            />

            <TextField
              type="number"
              fullWidth
              label="Puntaje máx."
              value={rubricaForm.puntajeMaximo}
              onChange={(e) => setRubricaForm((p) => ({ ...p, puntajeMaximo: e.target.value }))}
              slotProps={{ htmlInput: { min: 1, max: 100 } }}
            />
          </Stack>

          <Stack direction="row" spacing={1} justifyContent="flex-end">
            {lista && (
              <Button variant="outlined" onClick={limpiarRubrica}>
                Generar otra
              </Button>
            )}
            <Button
              variant="contained"
              onClick={handleGenerar}
              disabled={cargando}
              startIcon={cargando ? <CircularProgress size={16} color="inherit" /> : <AutoAwesome />}
            >
              {cargando ? 'Generando...' : 'Generar Rúbrica'}
            </Button>
          </Stack>
        </Stack>

        {/* Resultado */}
        {lista && rubrica.datos && (
          <Box sx={{ mt: 2 }}>
            <Divider sx={{ mb: 2 }} />
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={1}
              sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' }, mb: 1 }}
            >
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography variant="h5" sx={{ fontWeight: 700 }}>
                  {rubrica.datos.titulo || 'Rúbrica generada'}
                </Typography>
                {rubrica.datos.generadaPorIa && (
                  <Chip label="IA" size="small" color="primary" icon={<AutoAwesome />} />
                )}
              </Stack>
              <Stack direction="row" spacing={1}>
                <Button startIcon={<PictureAsPdf />} variant="outlined" onClick={exportPdf}>
                  PDF
                </Button>
                <Button startIcon={<Description />} variant="outlined" onClick={exportWord}>
                  Word
                </Button>
              </Stack>
            </Stack>

            <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
              {rubrica.datos.descripcion}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Modelo: {rubrica.datos.modelo || '-'} | Generada por IA:{' '}
              {rubrica.datos.generadaPorIa ? 'Sí' : 'No'}
            </Typography>

            <Stack spacing={1.5} sx={{ mt: 2 }}>
              {(rubrica.datos.criterios || []).map((criterio, index) => (
                <Paper
                  key={`${criterio?.nombre || 'criterio'}-${index}`}
                  variant="outlined"
                  sx={{ p: 1.5 }}
                >
                  <Typography variant="subtitle2">
                    {index + 1}. {criterio?.nombre || 'Criterio'} (Peso: {criterio?.peso ?? 0})
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    {criterio?.descripcion || '-'}
                  </Typography>
                  <Stack spacing={0.6}>
                    {(criterio?.niveles || []).map((nivel, i) => (
                      <Typography
                        key={`${nivel?.nombre || 'nivel'}-${i}`}
                        variant="caption"
                        color="text.secondary"
                      >
                        - {nivel?.nombre || 'Nivel'} ({nivel?.puntaje ?? 0} pts):{' '}
                        {nivel?.descriptor || '-'}
                      </Typography>
                    ))}
                  </Stack>
                </Paper>
              ))}
            </Stack>
          </Box>
        )}
      </Paper>
    </Box>
  );
};

export default RubricaIA;
