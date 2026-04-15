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
import { ArrowBack, Description, PictureAsPdf } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { jsPDF } from 'jspdf';
import IaService from '../services/IaService';
import CursoService from '../services/CursoService';

const defaultForm = {
  tema: '',
  nivelEducativo: 'Universitario',
  asignatura: '',
  tipoTarea: 'Proyecto',
  cantidadCriterios: 4,
  cantidadNiveles: 4,
  puntajeMaximo: 20,
};

const escapeHtml = (value) => String(value || '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#39;');

const RubricaIA = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol') || 'ROLE_ALUMNO';
  const canGenerateRubric = rol === 'ROLE_PROFESOR' || rol === 'ROLE_ADMIN';

  const [errorMsg, setErrorMsg] = useState('');
  const [rubricLoading, setRubricLoading] = useState(false);
  const [rubrica, setRubrica] = useState(null);
  const [cursosRubrica, setCursosRubrica] = useState([]);
  const [cursoRubricaId, setCursoRubricaId] = useState('');
  const [rubricaForm, setRubricaForm] = useState(defaultForm);

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

  const exportRubricaPdf = () => {
    if (!rubrica) return;

    const doc = new jsPDF({ unit: 'pt', format: 'a4' });
    const margin = 42;
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    const maxWidth = pageWidth - margin * 2;
    let y = margin;

    const ensureSpace = (needed = 18) => {
      if (y + needed > pageHeight - margin) {
        doc.addPage();
        y = margin;
      }
    };

    const addLine = (text, size = 11, isBold = false, extra = 6) => {
      const safeText = String(text || '-');
      doc.setFont('helvetica', isBold ? 'bold' : 'normal');
      doc.setFontSize(size);
      const lines = doc.splitTextToSize(safeText, maxWidth);
      const height = lines.length * (size + 1) + extra;
      ensureSpace(height);
      doc.text(lines, margin, y);
      y += height;
    };

    addLine(rubrica.titulo || 'Rúbrica generada por IA', 16, true, 10);
    addLine(`Descripción: ${rubrica.descripcion || '-'}`, 10, false, 8);
    addLine(`Modelo: ${rubrica.modelo || '-'} | Generada por IA: ${rubrica.generadaPorIa ? 'Sí' : 'No'}`, 10, false, 12);

    (rubrica.criterios || []).forEach((criterio, index) => {
      addLine(`${index + 1}. ${criterio?.nombre || 'Criterio'} (Peso: ${criterio?.peso ?? 0})`, 12, true, 4);
      addLine(`Descripción: ${criterio?.descripcion || '-'}`, 10, false, 6);

      (criterio?.niveles || []).forEach((nivel, idxNivel) => {
        addLine(`- ${nivel?.nombre || `Nivel ${idxNivel + 1}`} (${nivel?.puntaje ?? 0} pts): ${nivel?.descriptor || '-'}`, 10, false, 4);
      });

      y += 6;
    });

    const fileName = `${(rubrica.titulo || 'rubrica-ia').toLowerCase().replace(/[^a-z0-9]+/gi, '-')}.pdf`;
    doc.save(fileName);
  };

  const exportRubricaWord = () => {
    if (!rubrica) return;

    const rows = (rubrica.criterios || [])
      .map((criterio, index) => {
        const niveles = (criterio?.niveles || [])
          .map((nivel) => `
            <li><b>${escapeHtml(nivel?.nombre || 'Nivel')}</b> (${escapeHtml(nivel?.puntaje ?? 0)} pts): ${escapeHtml(nivel?.descriptor || '-')}</li>
          `)
          .join('');

        return `
          <tr>
            <td style="border:1px solid #d0d7de; padding:8px; vertical-align:top;">${index + 1}</td>
            <td style="border:1px solid #d0d7de; padding:8px; vertical-align:top;">${escapeHtml(criterio?.nombre || 'Criterio')}</td>
            <td style="border:1px solid #d0d7de; padding:8px; vertical-align:top;">${escapeHtml(criterio?.descripcion || '-')}</td>
            <td style="border:1px solid #d0d7de; padding:8px; vertical-align:top;">${escapeHtml(criterio?.peso ?? 0)}</td>
            <td style="border:1px solid #d0d7de; padding:8px; vertical-align:top;"><ul>${niveles}</ul></td>
          </tr>
        `;
      })
      .join('');

    const html = `
      <html>
        <head>
          <meta charset="utf-8" />
          <title>${escapeHtml(rubrica.titulo || 'Rúbrica IA')}</title>
        </head>
        <body style="font-family:Arial,sans-serif;">
          <h1>${escapeHtml(rubrica.titulo || 'Rúbrica generada por IA')}</h1>
          <p><b>Descripción:</b> ${escapeHtml(rubrica.descripcion || '-')}</p>
          <p><b>Modelo:</b> ${escapeHtml(rubrica.modelo || '-')} | <b>Generada por IA:</b> ${rubrica.generadaPorIa ? 'Sí' : 'No'}</p>
          <table style="border-collapse:collapse; width:100%;">
            <thead>
              <tr>
                <th style="border:1px solid #d0d7de; padding:8px; text-align:left;">#</th>
                <th style="border:1px solid #d0d7de; padding:8px; text-align:left;">Criterio</th>
                <th style="border:1px solid #d0d7de; padding:8px; text-align:left;">Descripción</th>
                <th style="border:1px solid #d0d7de; padding:8px; text-align:left;">Peso</th>
                <th style="border:1px solid #d0d7de; padding:8px; text-align:left;">Niveles</th>
              </tr>
            </thead>
            <tbody>
              ${rows}
            </tbody>
          </table>
        </body>
      </html>
    `;

    const blob = new Blob([html], { type: 'application/msword;charset=utf-8' });
    const fileName = `${(rubrica.titulo || 'rubrica-ia').toLowerCase().replace(/[^a-z0-9]+/gi, '-')}.doc`;
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  };

  if (!canGenerateRubric) {
    return (
      <Alert severity="info">
        El generador de rúbricas está disponible para perfiles Profesor y Administrador.
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
          Generador de Rúbricas IA
        </Typography>
      </Stack>

      {errorMsg && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {String(errorMsg)}
        </Alert>
      )}

      <Paper sx={{ p: 2 }}>
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
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' }, mb: 1 }}>
              <Typography variant="h6">{rubrica.titulo || 'Rúbrica generada'}</Typography>
              <Stack direction="row" spacing={1}>
                <Button startIcon={<PictureAsPdf />} variant="outlined" onClick={exportRubricaPdf}>
                  Exportar PDF
                </Button>
                <Button startIcon={<Description />} variant="outlined" onClick={exportRubricaWord}>
                  Exportar Word
                </Button>
              </Stack>
            </Stack>

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
    </Box>
  );
};

export default RubricaIA;
