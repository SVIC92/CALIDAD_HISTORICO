import { useCallback, useEffect, useMemo, useState } from 'react';
import {
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
import { Add, ArrowBack, Delete, Edit, Assessment } from '@mui/icons-material';
import { useNavigate, useSearchParams } from 'react-router-dom';
import DataTable from '../components/DataTable';
import FloatingConfirmModal from '../components/FloatingConfirmModal';
import FloatingMessageModal from '../components/FloatingMessageModal';
import ActividadService from '../services/ActividadService';
import CursoService from '../services/CursoService';
import { extractBackendValidationMessage } from '../utils/backendValidation';

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

const toDateTimeLocalValue = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';

  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const todayReference = () => {
  const now = new Date();
  return now.toLocaleString();
};

const nowDateTimeLocal = () => {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
};

const nombreActividadRegex = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\s'\-]+$/;
const sanitizeNombreActividad = (value) => value.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ\s'\-]/g, '');

const Actividades = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const cursoIdParam = searchParams.get('cursoId') || '';
  const rol = localStorage.getItem('rol');
  const canManage = rol === 'ROLE_PROFESOR';

  const [cursos, setCursos] = useState([]);
  const [selectedCursoId, setSelectedCursoId] = useState('');
  const [actividades, setActividades] = useState([]);
  const [errorMsg, setErrorMsg] = useState('');
  const [fieldErrors, setFieldErrors] = useState({ nombre: '', descripcion: '', fechaVencimiento: '', intentosPermitidos: '' });
  const [successMsg, setSuccessMsg] = useState('');
  const [confirmDialog, setConfirmDialog] = useState(null);

  const [openModal, setOpenModal] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [selectedActividadId, setSelectedActividadId] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    nombre: '',
    descripcion: '',
    fechaVencimiento: '',
    intentosPermitidos: 1,
  });

  const resetForm = () => {
    setFormData({ nombre: '', descripcion: '', fechaVencimiento: '', intentosPermitidos: 1 });
    setSelectedActividadId('');
    setIsEditMode(false);
    setFieldErrors({ nombre: '', descripcion: '', fechaVencimiento: '', intentosPermitidos: '' });
  };

  const fetchCursosByRole = useCallback(async () => {
    try {
      let data = [];
      if (rol === 'ROLE_PROFESOR') {
        data = await CursoService.listarInscritosProfesor();
      } else if (rol === 'ROLE_ALUMNO') {
        data = await CursoService.listarInscritosAlumno();
      } else {
        setErrorMsg('Este módulo está disponible para Profesor y Alumno.');
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
      }
    } catch (error) {
      console.error('Error al cargar cursos', error);
      setErrorMsg('No se pudieron cargar los cursos para actividades.');
      setCursos([]);
    }
  }, [rol, cursoIdParam]);

  const fetchActividades = useCallback(async (cursoId) => {
    if (!cursoId) {
      setActividades([]);
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
    } catch (error) {
      console.error('Error al obtener actividades', error);
      setErrorMsg(extractBackendValidationMessage(error, 'No se pudieron cargar las actividades.'));
      setActividades([]);
    }
  }, []);

  useEffect(() => {
    fetchCursosByRole();
  }, [fetchCursosByRole]);

  useEffect(() => {
    if (!selectedCursoId) return;
    fetchActividades(selectedCursoId);
  }, [selectedCursoId, fetchActividades]);

  const handleOpenCreate = () => {
    if (!canManage || !selectedCursoId) return;
    resetForm();
    setOpenModal(true);
  };

  const handleOpenEdit = async (actividad) => {
    if (!canManage) return;
    const id = actividad?.id || actividad?._id;
    if (!id) return;

    try {
      const data = await ActividadService.obtenerPorIdParaModificar(id);
      setIsEditMode(true);
      setSelectedActividadId(id);
      setFormData({
        nombre: data?.nombre || actividad?.nombre || '',
        descripcion: data?.descripcion || actividad?.descripcion || '',
        fechaVencimiento: toDateTimeLocalValue(data?.fechaVencimiento || actividad?.fechaVencimiento),
        intentosPermitidos: data?.intentosPermitidos || actividad?.intentosPermitidos || 1, // <<< NUEVO
      });
      setFieldErrors({ nombre: '', descripcion: '', fechaVencimiento: '', intentosPermitidos: '' });
      setOpenModal(true);
    } catch (error) {
      console.error('Error al cargar actividad para edición', error);
      setErrorMsg('No se pudo cargar la actividad para edición.');
    }
  };

  const handleDelete = async (actividad) => {
    if (!canManage) return;
    const id = actividad?.id || actividad?._id;
    if (!id) return;

    setConfirmDialog({
      title: 'Eliminar actividad',
      message: `¿Eliminar la actividad "${actividad.nombre}"?`,
      confirmText: 'Eliminar',
      severity: 'error',
      onConfirm: async () => {
        try {
          await ActividadService.eliminar(id);
          setSuccessMsg('Actividad eliminada correctamente.');
          await fetchActividades(selectedCursoId);
        } catch (error) {
          console.error('Error al eliminar actividad', error);
          setErrorMsg('No se pudo eliminar la actividad.');
        }
      },
    });
  };

  const handleSubmit = async () => {
    if (!canManage || !selectedCursoId) return;

    setFieldErrors({ nombre: '', descripcion: '', fechaVencimiento: '', intentosPermitidos: '' });

    const nombre = formData.nombre.trim();
    const descripcion = formData.descripcion.trim();
    const fechaVencimiento = formData.fechaVencimiento;

    if (!nombre || !descripcion) {
      const message = 'Nombre y descripción son obligatorios.';
      setFieldErrors((prev) => ({ ...prev, nombre: !nombre ? message : '', descripcion: !descripcion ? message : '' }));
      setErrorMsg(message);
      return;
    }

    if (!nombreActividadRegex.test(nombre)) {
      setFieldErrors((prev) => ({ ...prev, nombre: 'El nombre de la actividad solo puede contener letras, espacios, apóstrofes y guiones.' }));
      setErrorMsg('El nombre de la actividad solo puede contener letras, espacios, apóstrofes y guiones.');
      return;
    }

    if (nombre.length > 120) {
      setFieldErrors((prev) => ({ ...prev, nombre: 'El nombre de la actividad no debe superar 120 caracteres.' }));
      setErrorMsg('El nombre de la actividad no debe superar 120 caracteres.');
      return;
    }

    if (descripcion.length > 1000) {
      setFieldErrors((prev) => ({ ...prev, descripcion: 'La descripción no debe superar 1000 caracteres.' }));
      setErrorMsg('La descripción no debe superar 1000 caracteres.');
      return;
    }

    if (!fechaVencimiento) {
      setFieldErrors((prev) => ({ ...prev, fechaVencimiento: 'La fecha y hora de vencimiento es obligatoria.' }));
      setErrorMsg('La fecha y hora de vencimiento es obligatoria.');
      return;
    }

    const vencimientoDate = new Date(fechaVencimiento);
    const ahora = new Date();
    if (Number.isNaN(vencimientoDate.getTime()) || vencimientoDate < ahora) {
      setFieldErrors((prev) => ({ ...prev, fechaVencimiento: 'La fecha y hora de vencimiento debe ser mayor o igual al momento actual.' }));
      setErrorMsg('La fecha y hora de vencimiento debe ser mayor o igual al momento actual.');
      return;
    }
    const intentosPermitidos = Number(formData.intentosPermitidos);
    if (intentosPermitidos < 1 || intentosPermitidos > 3) {
      setFieldErrors((prev) => ({ ...prev, intentosPermitidos: 'Los intentos permitidos deben ser entre 1 y 3.' }));
      setErrorMsg('Los intentos permitidos deben ser entre 1 y 3.');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');

      if (isEditMode) {
        await ActividadService.modificar(selectedActividadId, {
          nombre,
          descripcion,
          fechaVencimiento: new Date(fechaVencimiento).toISOString(),
        });
      } else {
        await ActividadService.registro(selectedCursoId, {
          nombre,
          descripcion,
          fechaVencimiento: new Date(fechaVencimiento).toISOString(),
        });
      }

      setSuccessMsg(isEditMode ? 'Actividad actualizada correctamente.' : 'Actividad registrada correctamente.');
      setOpenModal(false);
      resetForm();
      await fetchActividades(selectedCursoId);
    } catch (error) {
      console.error('Error al guardar actividad', error);
      setErrorMsg(extractBackendValidationMessage(error, 'No se pudo guardar la actividad.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const actions = canManage
    ? [
        {
          label: 'Editar',
          icon: <Edit />,
          color: 'primary',
          onClick: handleOpenEdit,
        },
        {
          label: 'Eliminar',
          icon: <Delete />,
          color: 'error',
          onClick: handleDelete,
        },
      ]
    : undefined;

  const columns = [
    { id: 'nombre', label: 'Actividad' },
    { id: 'descripcion', label: 'Descripción' },
    { id: 'intentosPermitidos', label: 'Intentos Max', align: 'center' }, // <<< NUEVA COLUMNA
    { id: 'fechaCreacion', label: 'Creación' },
    { id: 'fechaVencimiento', label: 'Vencimiento' },
  ];

  const selectedCurso = useMemo(
    () => cursos.find((c) => (c.id || c._id) === selectedCursoId),
    [cursos, selectedCursoId]
  );

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 3, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/cursos')}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Actividades
        </Typography>
        <Button
          variant="outlined"
          startIcon={<Assessment />}
          onClick={() => navigate(`/modulo/reportes?cursoId=${selectedCursoId}`)}
          disabled={!selectedCursoId}
        >
          Ir a Reportes
        </Button>
        {canManage && (
          <Button variant="contained" startIcon={<Add />} onClick={handleOpenCreate} disabled={!selectedCursoId}>
            Nueva Actividad
          </Button>
        )}
      </Stack>

      <TextField
        select
        fullWidth
        label="Curso"
        value={selectedCursoId}
        onChange={(e) => setSelectedCursoId(e.target.value)}
        sx={{ mb: 2, bgcolor: 'background.paper' }}
      >
        {cursos.map((curso) => {
          const cursoId = curso.id || curso._id;
          return (
            <MenuItem key={cursoId} value={cursoId}>
              {curso.nombre}
            </MenuItem>
          );
        })}
      </TextField>

      {selectedCurso && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Curso seleccionado: {selectedCurso.nombre}
        </Typography>
      )}

      <FloatingMessageModal
        open={Boolean(errorMsg)}
        severity="error"
        title="Error"
        message={errorMsg}
        onClose={() => setErrorMsg('')}
      />

      <FloatingMessageModal
        open={Boolean(successMsg)}
        severity="success"
        title="Operación completada"
        message={successMsg}
        onClose={() => setSuccessMsg('')}
      />

      <DataTable
        columns={columns}
        data={actividades.map((a) => ({
          ...a,
          fechaCreacion: formatDateTimeDisplay(a.fechaCreacion),
          fechaVencimiento: formatDateTimeDisplay(a.fechaVencimiento),
        }))}
        actions={actions}
        exportFileName={`actividades-${selectedCurso?.nombre || 'curso'}`}
      />

      {canManage && (
        <Dialog open={openModal} onClose={() => !isSubmitting && setOpenModal(false)} fullWidth maxWidth="sm">
          <DialogTitle>{isEditMode ? 'Editar Actividad' : 'Nueva Actividad'}</DialogTitle>
          <DialogContent>
            <TextField
              margin="dense"
              label="Nombre"
              value={formData.nombre}
              onChange={(e) => setFormData((prev) => ({ ...prev, nombre: e.target.value }))}
              fullWidth
              required
              slotProps={{ htmlInput: { maxLength: 120, pattern: "[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\s'\-]+" } }}
              error={Boolean(fieldErrors.nombre)}
              helperText={fieldErrors.nombre || ''}
            />
            <TextField
              margin="dense"
              label="Descripción"
              value={formData.descripcion}
              onChange={(e) => setFormData((prev) => ({ ...prev, descripcion: e.target.value }))}
              fullWidth
              required
              multiline
              minRows={3}
              slotProps={{ htmlInput: { maxLength: 1000 } }}
              error={Boolean(fieldErrors.descripcion)}
              helperText={fieldErrors.descripcion || ''}
            />
            <TextField
              margin="dense"
              label="Intentos Permitidos (Max 3)"
              type="number"
              value={formData.intentosPermitidos}
              onChange={(e) => setFormData((prev) => ({ ...prev, intentosPermitidos: e.target.value }))}
              fullWidth
              required
              slotProps={{ htmlInput: { min: 1, max: 3 } }}
              error={Boolean(fieldErrors.intentosPermitidos)}
              helperText={fieldErrors.intentosPermitidos || 'Rango permitido: 1 a 3'}
            />
            <TextField
              margin="dense"
              label="Fecha de creación (referencial)"
              value={todayReference()}
              fullWidth
              disabled
            />
            <TextField
              margin="dense"
              label="Fecha y hora de vencimiento"
              type="datetime-local"
              value={formData.fechaVencimiento}
              onChange={(e) => setFormData((prev) => ({ ...prev, fechaVencimiento: e.target.value }))}
              fullWidth
              required
              slotProps={{
                inputLabel: { shrink: true },
                input: { min: nowDateTimeLocal() },
              }}
              error={Boolean(fieldErrors.fechaVencimiento)}
              helperText={fieldErrors.fechaVencimiento || 'Debe ser mayor o igual al momento actual'}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenModal(false)} disabled={isSubmitting}>Cancelar</Button>
            <Button onClick={handleSubmit} variant="contained" disabled={isSubmitting}>
              {isEditMode ? 'Guardar Cambios' : 'Registrar'}
            </Button>
          </DialogActions>
        </Dialog>
      )}

      <FloatingConfirmModal
        open={Boolean(confirmDialog)}
        title={confirmDialog?.title || 'Confirmar acción'}
        message={confirmDialog?.message || ''}
        confirmText={confirmDialog?.confirmText || 'Confirmar'}
        severity={confirmDialog?.severity || 'warning'}
        onClose={() => setConfirmDialog(null)}
        onConfirm={async () => {
          const action = confirmDialog?.onConfirm;
          setConfirmDialog(null);
          if (action) {
            await action();
          }
        }}
      />
    </Box>
  );
};

export default Actividades;
