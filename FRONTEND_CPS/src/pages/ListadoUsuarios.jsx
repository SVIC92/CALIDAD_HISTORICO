import { useEffect, useMemo, useState } from 'react';
import {
  Box, Typography, TextField, InputAdornment, Button,
  Dialog, DialogTitle, DialogContent, DialogActions, MenuItem, Stack,
} from '@mui/material';
import { ArrowBack, Search, Add, Edit, Block, CheckCircle } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import DataTable from '../components/DataTable';
import FloatingConfirmModal from '../components/FloatingConfirmModal';
import FloatingMessageModal from '../components/FloatingMessageModal';
import UsuarioService from '../services/UsuarioService';
import { extractBackendValidationMessage } from '../utils/backendValidation';
import axios from '../API/axios';

// Función para formatear la fecha
const formatDate = (value) => {
  if (!value || value === '-') return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleDateString(); // Devuelve formato DD/MM/YYYY
};

const normalizeUsuario = (usuario) => ({
  id: usuario.id || usuario._id || '-',
  nombre: usuario.nombre || '-',
  email: usuario.email || '-',
  rol: usuario.rol || '-',
  activo: typeof usuario.activo === 'boolean' ? usuario.activo : true,
  activoTexto: (typeof usuario.activo === 'boolean' ? usuario.activo : true) ? 'SI' : 'NO',
  // Se aplica el formato a la fecha recibida
  fechaCreacion: formatDate(usuario.fechaCreacion || usuario.createdAt),
});

const initialForm = {
  nombre: '',
  email: '',
  password: '',
  password2: '',
  rol: 'ALUMNO',
  activo: true,
};

const ListadoUsuarios = () => {
  const [usuarios, setUsuarios] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [openModal, setOpenModal] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedUsuarioId, setSelectedUsuarioId] = useState('');
  const [formData, setFormData] = useState(initialForm);
  const [confirmDialog, setConfirmDialog] = useState(null);

  const navigate = useNavigate();

  const fetchUsuarios = async () => {
    try {
      const data = await UsuarioService.listar();
      const normalized = Array.isArray(data) ? data.map(normalizeUsuario) : [];
      setUsuarios(normalized);
    } catch (error) {
      console.error('Error al obtener usuarios', error);
      setErrorMsg(extractBackendValidationMessage(error, 'No se pudo cargar el listado de usuarios.'));
      setUsuarios([]);
    }
  };

  useEffect(() => {
    fetchUsuarios();
  }, []);

  const closeModal = () => {
    if (isSubmitting) return;
    setOpenModal(false);
    setIsEditMode(false);
    setSelectedUsuarioId('');
    setFormData(initialForm);
    setErrorMsg('');
  };

  const openCreate = () => {
    setErrorMsg('');
    setSuccessMsg('');
    setIsEditMode(false);
    setSelectedUsuarioId('');
    setFormData(initialForm);
    setOpenModal(true);
  };

  const openEdit = (row) => {
    setErrorMsg('');
    setSuccessMsg('');
    setIsEditMode(true);
    setSelectedUsuarioId(row.id);
    setFormData({
      nombre: row.nombre === '-' ? '' : row.nombre,
      email: row.email === '-' ? '' : row.email,
      password: '',
      password2: '',
      rol: row.rol === '-' ? 'ALUMNO' : row.rol.replace('ROLE_', ''),
      activo: !!row.activo,
    });
    setOpenModal(true);
  };

  const handleToggleStatus = async (row, activar) => {
    setErrorMsg('');
    setSuccessMsg('');

    if (row.activo === activar) {
      setErrorMsg(`El usuario ya se encuentra ${activar ? 'activo' : 'inactivo'}.`);
      return;
    }

    const accionTexto = activar ? 'activar' : 'desactivar';

    setConfirmDialog({
      title: `${activar ? 'Activar' : 'Desactivar'} usuario`,
      message: `¿Estás seguro de ${accionTexto} el usuario "${row.nombre}"?`,
      confirmText: activar ? 'Activar' : 'Desactivar',
      severity: activar ? 'success' : 'warning',
      onConfirm: async () => {
        try {
          setIsSubmitting(true);
          await axios.patch(`/admin/usuarios/${row.id}/${accionTexto}`);
          await fetchUsuarios();
          setSuccessMsg(`Usuario ${activar ? 'activado' : 'desactivado'} correctamente.`);
        } catch (error) {
          setErrorMsg(extractBackendValidationMessage(error, `No se pudo ${accionTexto} el usuario.`));
        } finally {
          setIsSubmitting(false);
        }
      },
    });
  };

  const handleSave = async () => {
    const nombre = formData.nombre.trim();
    const email = formData.email.trim();
    const password = formData.password.trim();
    const password2 = formData.password2.trim();

    if (!nombre) { setErrorMsg('El nombre es obligatorio.'); return; }
    if (!email) { setErrorMsg('El email es obligatorio.'); return; }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) { setErrorMsg('Ingresa un email valido.'); return; }

    if (!isEditMode && !password) { setErrorMsg('La contraseña es obligatoria para registrar un usuario.'); return; }
    if (password && password.length < 6) { setErrorMsg('La contraseña debe tener al menos 6 caracteres.'); return; }
    if ((password || password2) && password !== password2) { setErrorMsg('Las contraseñas no coinciden.'); return; }

    try {
      setIsSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');

      const payload = {
        nombre,
        email,
        password: password || undefined,
        password2: password2 || undefined,
        rol: formData.rol.replace('ROLE_', ''),
        activo: formData.activo,
      };

      if (isEditMode) {
        await UsuarioService.modificar(selectedUsuarioId, payload);
      } else {
        await UsuarioService.registro(payload);
      }

      await fetchUsuarios();
      setSuccessMsg(isEditMode ? 'Usuario actualizado correctamente.' : 'Usuario registrado correctamente.');
      closeModal();
    } catch (error) {
      setErrorMsg(extractBackendValidationMessage(error, 'No se pudo guardar el usuario.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const columns = [
    { id: 'nombre', label: 'Nombre' },
    { id: 'email', label: 'Email' },
    { id: 'rol', label: 'Rol', align: 'center' },
    { id: 'activoTexto', label: 'Activo', align: 'center' },
    { id: 'fechaCreacion', label: 'Fecha de Creación', align: 'center' },
  ];

  const actions = [
    { label: 'Editar', icon: <Edit />, color: 'primary', onClick: openEdit },
    { label: 'Activar', icon: <CheckCircle />, color: 'success', onClick: (row) => handleToggleStatus(row, true) },
    { label: 'Desactivar', icon: <Block />, color: 'error', onClick: (row) => handleToggleStatus(row, false) },
  ];

  const filteredData = useMemo(() => {
    const term = searchTerm.toLowerCase();
    return usuarios.filter((u) =>
      `${u.nombre} ${u.email} ${u.rol} ${u.activoTexto} ${u.fechaCreacion}`.toLowerCase().includes(term)
    );
  }, [usuarios, searchTerm]);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/usuarios')} sx={{ mr: 2 }}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Gestión de Usuarios
        </Typography>
        <Button variant="contained" startIcon={<Add />} onClick={openCreate}>
          Nuevo Usuario
        </Button>
      </Box>

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

      <TextField
        fullWidth
        placeholder="Buscar por nombre, email o rol..."
        sx={{ mb: 3, bgcolor: 'background.paper' }}
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        slotProps={{
          input: { startAdornment: (<InputAdornment position="start"><Search /></InputAdornment>) },
        }}
      />

      <DataTable columns={columns} data={filteredData} actions={actions} exportFileName="usuarios" />

      <Dialog open={openModal} onClose={closeModal} fullWidth maxWidth="sm">
        <DialogTitle>{isEditMode ? 'Editar Usuario' : 'Nuevo Usuario'}</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 0.5 }}>
            <TextField label="Nombre" value={formData.nombre} onChange={(e) => setFormData((prev) => ({ ...prev, nombre: e.target.value }))} fullWidth />
            <TextField label="Email" value={formData.email} onChange={(e) => setFormData((prev) => ({ ...prev, email: e.target.value }))} fullWidth />

            <TextField select label="Rol" value={formData.rol} onChange={(e) => setFormData((prev) => ({ ...prev, rol: e.target.value }))} fullWidth >
              <MenuItem value="ALUMNO">Alumno</MenuItem>
              <MenuItem value="PROFESOR">Profesor</MenuItem>
              <MenuItem value="ADMIN">Administrador</MenuItem>
            </TextField>

            <TextField select label="Activo" value={formData.activo ? 'true' : 'false'} onChange={(e) => setFormData((prev) => ({ ...prev, activo: e.target.value === 'true' }))} fullWidth >
              <MenuItem value="true">SI</MenuItem>
              <MenuItem value="false">NO</MenuItem>
            </TextField>
            <TextField type="password" label={isEditMode ? 'Contraseña (opcional)' : 'Contraseña'} value={formData.password} onChange={(e) => setFormData((prev) => ({ ...prev, password: e.target.value }))} fullWidth />
            <TextField type="password" label={isEditMode ? 'Repetir contraseña (opcional)' : 'Repetir contraseña'} value={formData.password2} onChange={(e) => setFormData((prev) => ({ ...prev, password2: e.target.value }))} fullWidth />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeModal} disabled={isSubmitting}>Cancelar</Button>
          <Button variant="contained" onClick={handleSave} disabled={isSubmitting}>
            {isSubmitting ? 'Guardando...' : 'Guardar'}
          </Button>
        </DialogActions>
      </Dialog>

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

export default ListadoUsuarios;