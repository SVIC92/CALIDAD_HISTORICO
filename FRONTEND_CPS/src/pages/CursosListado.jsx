import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Button,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
} from '@mui/material';
import { Add, Edit, Delete, Search, ArrowBack, HowToReg } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import DataTable from '../components/DataTable';
import CursoService from '../services/CursoService';
import ProfesorService from '../services/ProfesorService';

const CursosListado = () => {
  const [cursos, setCursos] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [openModal, setOpenModal] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [selectedCursoId, setSelectedCursoId] = useState(null);
  const [profesores, setProfesores] = useState([]);
  const [adminEstadoFilter, setAdminEstadoFilter] = useState('todos');
  const [formData, setFormData] = useState({
    nombre: '',
    descripcion: '',
    capacidadMaxima: '',
    creditos: '',
    fechaTermino: '',
    profesorId: '',
    profesorAsignado: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol');
  const canManageCursos = rol === 'ROLE_ADMIN';
  const canSelfEnroll = rol === 'ROLE_PROFESOR' || rol === 'ROLE_ALUMNO';

  const toDateInputValue = (value) => {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  };

  const todayDateInputValue = () => {
    const date = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  };

  const cursoTieneProfesorAsignado = (curso) => {
    const profesorAsignado = curso?.profesorAsignado;
    const profesorId = curso?.profesorId;

    if (typeof profesorAsignado === 'string' && profesorAsignado.trim()) return true;
    if (typeof profesorId === 'string' && profesorId.trim()) return true;

    if (profesorAsignado && typeof profesorAsignado === 'object') {
      if (profesorAsignado.id || profesorAsignado.nombre || profesorAsignado.email) return true;
    }

    if (profesorId && typeof profesorId === 'object') {
      if (profesorId.id || profesorId.nombre || profesorId.email) return true;
    }

    return false;
  };

  const fetchCursos = useCallback(async () => {
    try {
      let data = [];

      if (rol === 'ROLE_PROFESOR') {
        const disponibles = await CursoService.listarDisponiblesProfesor();
        data = (Array.isArray(disponibles) ? disponibles : []).filter((curso) => !cursoTieneProfesorAsignado(curso));
      } else if (rol === 'ROLE_ALUMNO') {
        const disponibles = await CursoService.listarDisponiblesAlumno();
        const disponiblesData = Array.isArray(disponibles) ? disponibles : [];

        if (disponiblesData.length > 0) {
          data = disponiblesData;
        } else {
          // Fallback defensivo: algunos cambios de query en backend pueden devolver []
          // aunque existan cursos con profesor asignado disponibles para alumno.
          const [todosResult, inscritosResult] = await Promise.allSettled([
            CursoService.listar(),
            CursoService.listarInscritosAlumno(),
          ]);

          const todos = todosResult.status === 'fulfilled' && Array.isArray(todosResult.value)
            ? todosResult.value
            : [];

          const inscritos = inscritosResult.status === 'fulfilled' && Array.isArray(inscritosResult.value)
            ? inscritosResult.value
            : [];

          if (todos.length > 0) {
            const inscritosIds = new Set(
              inscritos
                .map((curso) => curso?.id || curso?._id)
                .filter(Boolean)
            );

            const filtrados = todos.filter((curso) => {
              const cursoId = curso?.id || curso?._id;
              if (!cursoId) return false;
              return !inscritosIds.has(cursoId);
            });

            data = filtrados.length > 0 ? filtrados : todos;
          } else {
            data = [];
          }
        }
      } else {
        if (adminEstadoFilter === 'activos') {
          data = await CursoService.listarActivos();
        } else if (adminEstadoFilter === 'caducados') {
          data = await CursoService.listarCaducados();
        } else {
          data = await CursoService.listar();
        }
      }

      setCursos(data);
    } catch (error) {
      console.error("Error al obtener cursos", error);
    }
  }, [rol, adminEstadoFilter]);

  const fetchProfesores = useCallback(async () => {
    if (!canManageCursos) return;

    try {
      const data = await ProfesorService.listar();
      setProfesores(data || []);
    } catch (error) {
      console.error('Error al obtener profesores', error);
      setProfesores([]);
    }
  }, [canManageCursos]);

  const getCursoId = (curso) => curso.id || curso._id;

  const normalizeProfesorData = (curso) => {
    const profesorFromAsignado = curso.profesorAsignado;
    const profesorFromId = curso.profesorId;

    if (profesorFromAsignado && typeof profesorFromAsignado === 'object') {
      return {
        profesorId: profesorFromAsignado.id || '',
        profesorAsignado: profesorFromAsignado.nombre || profesorFromAsignado.email || '',
      };
    }

    if (profesorFromId && typeof profesorFromId === 'object') {
      return {
        profesorId: profesorFromId.id || '',
        profesorAsignado: profesorFromId.nombre || profesorFromId.email || '',
      };
    }

    return {
      profesorId: typeof profesorFromId === 'string' ? profesorFromId : '',
      profesorAsignado: typeof profesorFromAsignado === 'string' ? profesorFromAsignado : '',
    };
  };

  const resetForm = () => {
    setFormData({
      nombre: '',
      descripcion: '',
      capacidadMaxima: '',
      creditos: '',
      fechaTermino: '',
      profesorId: '',
      profesorAsignado: '',
    });
    setSelectedCursoId(null);
    setIsEditMode(false);
  };

  const handleOpenCreate = () => {
    if (!canManageCursos) return;
    resetForm();
    setOpenModal(true);
  };

  const handleOpenEdit = (curso) => {
    if (!canManageCursos) return;
    const cursoId = getCursoId(curso);
    const profesorData = normalizeProfesorData(curso);

    if (!cursoId) {
      alert('No se pudo identificar el curso a editar.');
      return;
    }

    setIsEditMode(true);
    setSelectedCursoId(cursoId);
    setFormData({
      nombre: curso.nombre || '',
      descripcion: curso.descripcion || '',
      capacidadMaxima: curso.capacidadMaxima ?? '',
      creditos: curso.creditos ?? '',
      fechaTermino: toDateInputValue(curso.fechaTermino),
      profesorId: profesorData.profesorId,
      profesorAsignado: profesorData.profesorAsignado,
    });
    setOpenModal(true);
  };

  const handleCloseModal = () => {
    if (isSubmitting) {
      return;
    }
    setOpenModal(false);
    resetForm();
  };

  const handleChangeForm = (event) => {
    const { name, value } = event.target;

    if (name === 'profesorId') {
      const profesorSeleccionado = profesores.find((p) => p.id === value);
      setFormData((prev) => ({
        ...prev,
        profesorId: value,
        profesorAsignado: profesorSeleccionado?.nombre || prev.profesorAsignado,
      }));
      return;
    }

    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmitForm = async () => {
    if (!canManageCursos) return;

    const nombre = formData.nombre.trim();
    const descripcion = formData.descripcion.trim();
    const profesorAsignado = formData.profesorAsignado.trim();
    const profesorId = formData.profesorId.trim();
    const fechaTermino = formData.fechaTermino;
    const capacidadMaxima = Number(formData.capacidadMaxima);
    const creditos = Number(formData.creditos);

    if (!nombre || !descripcion) {
      alert('Nombre y descripción son obligatorios.');
      return;
    }

    if (!Number.isInteger(capacidadMaxima) || capacidadMaxima <= 0) {
      alert('La capacidad máxima debe ser un número entero mayor a 0.');
      return;
    }

    if (!Number.isInteger(creditos) || creditos <= 0) {
      alert('Los créditos deben ser un número entero mayor a 0.');
      return;
    }

    if (!fechaTermino) {
      alert('La fecha de término es obligatoria.');
      return;
    }

    const today = todayDateInputValue();
    if (fechaTermino < today) {
      alert('La fecha de término no puede ser anterior a la fecha actual.');
      return;
    }

    try {
      setIsSubmitting(true);

      const payload = {
        nombre,
        descripcion,
        capacidadMaxima,
        creditos,
        fechaTermino,
        profesorId: profesorId || undefined,
        profesorAsignado: profesorAsignado || undefined,
      };

      if (isEditMode) {
        await CursoService.modificar(selectedCursoId, payload);
      } else {
        await CursoService.registro(payload);
      }

      await fetchCursos();
      handleCloseModal();
    } catch (error) {
      console.error('Error al guardar curso', error);
      alert('No se pudo guardar el curso.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteCurso = async (curso) => {
    if (!canManageCursos) return;

    const cursoId = getCursoId(curso);

    if (!cursoId) {
      alert('No se pudo identificar el curso a eliminar.');
      return;
    }

    const confirmar = window.confirm(`¿Eliminar el curso "${curso.nombre}"?`);
    if (!confirmar) {
      return;
    }

    try {
      await CursoService.eliminar(cursoId);
      await fetchCursos();
    } catch (error) {
      console.error('Error al eliminar curso', error);
      alert('No se pudo eliminar el curso.');
    }
  };

  const handleInscribirCurso = async (curso) => {
    if (!canSelfEnroll) return;

    const cursoId = getCursoId(curso);
    if (!cursoId) {
      alert('No se pudo identificar el curso para inscripción.');
      return;
    }

    const confirmar = window.confirm(`¿Inscribirte al curso "${curso.nombre}"?`);
    if (!confirmar) return;

    try {
      await CursoService.inscribirCurso(cursoId);
      await fetchCursos();
      alert('Inscripción registrada correctamente.');
    } catch (error) {
      console.error('Error al inscribirse en curso', error);
      alert('No se pudo completar la inscripción.');
    }
  };

  useEffect(() => {
    fetchCursos();
    fetchProfesores();
  }, [fetchCursos, fetchProfesores]);

  // Definición de columnas para este módulo específico
  const columns = [
    { id: 'nombre', label: 'Nombre del Curso' },
    { id: 'descripcion', label: 'Descripción' },
    { id: 'capacidadMaxima', label: 'Capacidad', align: 'center' },
    { id: 'creditos', label: 'Créditos', align: 'center' },
    { id: 'fechaTermino', label: 'Fecha de Término' },
    { id: 'estado', label: 'Estado', align: 'center' },
    { id: 'profesorAsignado', label: 'Profesor' },
  ];

  // Definición de acciones con iconos
  const actions = canManageCursos
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
          onClick: handleDeleteCurso,
        },
      ]
    : canSelfEnroll
      ? [
          {
            label: rol === 'ROLE_PROFESOR' ? 'Inscribirme a dictar' : 'Inscribirme',
            icon: <HowToReg />,
            color: 'success',
            onClick: handleInscribirCurso,
          },
        ]
      : undefined;

  const filteredData = cursos
    .map((curso) => ({
      ...curso,
      profesorAsignado:
        curso?.profesorAsignadoNombre
        || (typeof curso?.profesorAsignado === 'string'
          ? curso.profesorAsignado
          : curso?.profesorAsignado?.nombre || curso?.profesorAsignado?.email || '-'),
      fechaTermino: toDateInputValue(curso?.fechaTermino) || '-',
      estado: curso?.estado || '-',
    }))
    .filter((c) => (c.nombre || '').toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/cursos')} sx={{ mr: 2 }}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          {rol === 'ROLE_ADMIN' && 'Listado de Cursos'}
          {rol === 'ROLE_PROFESOR' && 'Cursos Disponibles para Dictar'}
          {rol === 'ROLE_ALUMNO' && 'Cursos Disponibles'}
        </Typography>
        {canManageCursos && (
          <Button variant="contained" startIcon={<Add />} onClick={handleOpenCreate}>
            Nuevo Curso
          </Button>
        )}
      </Box>

      <TextField
        fullWidth
        placeholder="Buscar por nombre..."
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

      {canManageCursos && (
        <TextField
          select
          fullWidth
          label="Estado de cursos"
          value={adminEstadoFilter}
          onChange={(e) => setAdminEstadoFilter(e.target.value)}
          sx={{ mb: 3, bgcolor: 'background.paper' }}
        >
          <MenuItem value="todos">Todos</MenuItem>
          <MenuItem value="activos">Activos</MenuItem>
          <MenuItem value="caducados">Caducados</MenuItem>
        </TextField>
      )}

      <DataTable 
        columns={columns} 
        data={filteredData} 
        actions={actions} 
      />

      {canManageCursos && (
        <Dialog open={openModal} onClose={handleCloseModal} fullWidth maxWidth="sm">
          <DialogTitle>{isEditMode ? 'Editar Curso' : 'Registrar Curso'}</DialogTitle>
          <DialogContent>
            <TextField
              margin="dense"
              label="Nombre"
              name="nombre"
              value={formData.nombre}
              onChange={handleChangeForm}
              fullWidth
              required
            />
            <TextField
              margin="dense"
              label="Descripción"
              name="descripcion"
              value={formData.descripcion}
              onChange={handleChangeForm}
              fullWidth
              required
              multiline
              minRows={3}
            />
            <TextField
              margin="dense"
              label="Capacidad máxima"
              name="capacidadMaxima"
              type="number"
              value={formData.capacidadMaxima}
              onChange={handleChangeForm}
              fullWidth
              required
            />
            <TextField
              margin="dense"
              label="Créditos"
              name="creditos"
              type="number"
              value={formData.creditos}
              onChange={handleChangeForm}
              fullWidth
              required
            />
            <TextField
              margin="dense"
              label="Fecha de término"
              name="fechaTermino"
              type="date"
              value={formData.fechaTermino}
              onChange={handleChangeForm}
              fullWidth
              required
              slotProps={{
                inputLabel: { shrink: true },
                htmlInput: { min: todayDateInputValue() },
              }}
            />
            {profesores.length > 0 ? (
              <TextField
                margin="dense"
                label="Profesor"
                name="profesorId"
                value={formData.profesorId}
                onChange={handleChangeForm}
                fullWidth
                select
              >
                <MenuItem value="">Sin profesor</MenuItem>
                {profesores.map((profesor) => (
                  <MenuItem key={profesor.id} value={profesor.id}>
                    {profesor.nombre} ({profesor.email})
                  </MenuItem>
                ))}
              </TextField>
            ) : null}
            <TextField
              margin="dense"
              label={profesores.length > 0 ? 'Profesor asignado (referencia)' : 'Profesor asignado'}
              name="profesorAsignado"
              value={formData.profesorAsignado}
              onChange={handleChangeForm}
              fullWidth
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseModal} disabled={isSubmitting}>Cancelar</Button>
            <Button onClick={handleSubmitForm} variant="contained" disabled={isSubmitting}>
              {isEditMode ? 'Guardar Cambios' : 'Registrar'}
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </Box>
  );
};

export default CursosListado;