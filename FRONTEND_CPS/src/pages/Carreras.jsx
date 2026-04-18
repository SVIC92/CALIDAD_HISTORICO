import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { Add, ArrowBack, Delete, Edit, Search } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import DataTable from '../components/DataTable';
import CarreraService from '../services/CarreraService';
import { extractBackendValidationMessage } from '../utils/backendValidation';

const normalizeCarrera = (carrera) => ({
  id: carrera?.id || '-',
  codigo: carrera?.codigo || '-',
  nombre: carrera?.nombre || '-',
  descripcion: carrera?.descripcion || '-',
});

const initialForm = {
  codigo: '',
  nombre: '',
  descripcion: '',
};

const Carreras = () => {
  const navigate = useNavigate();
  const [carreras, setCarreras] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [openModal, setOpenModal] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [selectedCarreraId, setSelectedCarreraId] = useState('');
  const [formData, setFormData] = useState(initialForm);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const cargarCarreras = useCallback(async () => {
    try {
      setErrorMsg('');
      const data = await CarreraService.listar();
      const normalizadas = Array.isArray(data) ? data.map(normalizeCarrera) : [];
      setCarreras(normalizadas);
    } catch (error) {
      setCarreras([]);
      setErrorMsg(extractBackendValidationMessage(error, 'No se pudo cargar el listado de carreras.'));
    }
  }, []);

  useEffect(() => {
    cargarCarreras();
  }, [cargarCarreras]);

  const closeModal = () => {
    if (isSubmitting) return;
    setOpenModal(false);
    setIsEditMode(false);
    setSelectedCarreraId('');
    setFormData(initialForm);
  };

  const openCreate = () => {
    setErrorMsg('');
    setSuccessMsg('');
    setIsEditMode(false);
    setSelectedCarreraId('');
    setFormData(initialForm);
    setOpenModal(true);
  };

  const openEdit = (row) => {
    setErrorMsg('');
    setSuccessMsg('');
    setIsEditMode(true);
    setSelectedCarreraId(row.id);
    setFormData({
      codigo: row.codigo === '-' ? '' : row.codigo,
      nombre: row.nombre === '-' ? '' : row.nombre,
      descripcion: row.descripcion === '-' ? '' : row.descripcion,
    });
    setOpenModal(true);
  };

  const handleSave = async () => {
    const codigo = formData.codigo.trim().toUpperCase();
    const nombre = formData.nombre.trim();
    const descripcion = formData.descripcion.trim();

    if (!codigo) {
      setErrorMsg('El código de carrera es obligatorio.');
      return;
    }

    if (!nombre) {
      setErrorMsg('El nombre de carrera es obligatorio.');
      return;
    }

    if (codigo.length > 20) {
      setErrorMsg('El código no debe superar 20 caracteres.');
      return;
    }

    if (nombre.length > 120) {
      setErrorMsg('El nombre no debe superar 120 caracteres.');
      return;
    }

    if (descripcion.length > 1000) {
      setErrorMsg('La descripción no debe superar 1000 caracteres.');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');

      const payload = { codigo, nombre, descripcion };

      if (isEditMode) {
        await CarreraService.modificar(selectedCarreraId, payload);
      } else {
        await CarreraService.registro(payload);
      }

      await cargarCarreras();
      setSuccessMsg(isEditMode ? 'Carrera actualizada correctamente.' : 'Carrera registrada correctamente.');
      closeModal();
    } catch (error) {
      setErrorMsg(extractBackendValidationMessage(error, 'No se pudo guardar la carrera.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (row) => {
    if (!row?.id || row.id === '-') {
      setErrorMsg('No se pudo identificar la carrera a eliminar.');
      return;
    }

    const confirmar = window.confirm(`¿Eliminar la carrera "${row.nombre}"?`);
    if (!confirmar) return;

    try {
      setErrorMsg('');
      setSuccessMsg('');
      await CarreraService.eliminar(row.id);
      await cargarCarreras();
      setSuccessMsg('Carrera eliminada correctamente.');
    } catch (error) {
      setErrorMsg(extractBackendValidationMessage(error, 'No se pudo eliminar la carrera.'));
    }
  };

  const filteredData = useMemo(() => {
    const term = searchTerm.toLowerCase();
    return carreras.filter((c) => `${c.codigo} ${c.nombre} ${c.descripcion}`.toLowerCase().includes(term));
  }, [carreras, searchTerm]);

  const columns = [
    { id: 'codigo', label: 'Código' },
    { id: 'nombre', label: 'Nombre' },
    { id: 'descripcion', label: 'Descripción' },
  ];

  const actions = [
    {
      label: 'Editar',
      icon: <Edit />,
      color: 'primary',
      onClick: openEdit,
    },
    {
      label: 'Eliminar',
      icon: <Delete />,
      color: 'error',
      onClick: handleDelete,
    },
  ];

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 3, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/cursos')}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Gestión de Carreras
        </Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreate}>
          Nueva Carrera
        </Button>
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

      <TextField
        fullWidth
        placeholder="Buscar por código, nombre o descripción..."
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

      <DataTable
        columns={columns}
        data={filteredData}
        actions={actions}
        exportFileName="carreras"
      />

      <Dialog open={openModal} onClose={closeModal} fullWidth maxWidth="sm">
        <DialogTitle>{isEditMode ? 'Editar Carrera' : 'Registrar Carrera'}</DialogTitle>
        <DialogContent>
          <TextField
            margin="dense"
            label="Código"
            value={formData.codigo}
            onChange={(e) => setFormData((prev) => ({ ...prev, codigo: e.target.value.toUpperCase() }))}
            fullWidth
            required
            slotProps={{ htmlInput: { maxLength: 20 } }}
            helperText="Máximo 20 caracteres"
          />
          <TextField
            margin="dense"
            label="Nombre"
            value={formData.nombre}
            onChange={(e) => setFormData((prev) => ({ ...prev, nombre: e.target.value }))}
            fullWidth
            required
            slotProps={{ htmlInput: { maxLength: 120 } }}
            helperText="Máximo 120 caracteres"
          />
          <TextField
            margin="dense"
            label="Descripción"
            value={formData.descripcion}
            onChange={(e) => setFormData((prev) => ({ ...prev, descripcion: e.target.value }))}
            fullWidth
            multiline
            minRows={3}
            slotProps={{ htmlInput: { maxLength: 1000 } }}
            helperText="Opcional, máximo 1000 caracteres"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={closeModal} disabled={isSubmitting}>Cancelar</Button>
          <Button onClick={handleSave} variant="contained" disabled={isSubmitting}>
            {isEditMode ? 'Guardar Cambios' : 'Registrar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Carreras;
