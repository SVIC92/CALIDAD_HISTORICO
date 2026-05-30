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
  IconButton,
  Tooltip,
} from '@mui/material';
import { Add, Edit, Delete, Search, ArrowBack, HowToReg, CalendarMonth, DateRange} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import DataTable from '../components/DataTable';
import FloatingConfirmModal from '../components/FloatingConfirmModal';
import FloatingMessageModal from '../components/FloatingMessageModal';
import CursoService from '../services/CursoService';
import ProfesorService from '../services/ProfesorService';
import { extractBackendValidationMessage } from '../utils/backendValidation';

const CursosListado = () => {
  const [cursos, setCursos] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [openModal, setOpenModal] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [selectedCursoId, setSelectedCursoId] = useState(null);
  const [profesores, setProfesores] = useState([]);
  const [adminEstadoFilter, setAdminEstadoFilter] = useState('todos');
  const [fieldErrors, setFieldErrors] = useState({
    nombre: '',
    codigoCurso: '',
    descripcion: '',
    carrera: '',
    ciclo: '',
    capacidadMaxima: '',
    creditos: '',
    horasTeoricas: '',
    horasPracticas: '',
    horasLaboratorio: '',
    fechaInicio: '',
    fechaTermino: '',
  });
  const [formData, setFormData] = useState({
    nombre: '',
    codigoCurso: '',
    descripcion: '',
    ciclo: 1,
    modalidad: 'PRESENCIAL',
    capacidadMaxima: '',
    creditos: '',
    fechaInicio: '',
    fechaTermino: '',
    horasTeoricas: 1,
    horasPracticas: 0,
    horasLaboratorio: 0,
    estado: 'ACTIVO',
    carrera: '',
    profesorId: '',
    profesorAsignado: '',
  });
  
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol');
  const canManageCursos = rol === 'ROLE_ADMIN';
  const canSelfEnroll = rol === 'ROLE_PROFESOR' || rol === 'ROLE_ALUMNO';
  const [openHorarioModal, setOpenHorarioModal] = useState(false);
  const [horariosCurso, setHorariosCurso] = useState([]);
  const [confirmDialog, setConfirmDialog] = useState(null);
  const [horarioFieldErrors, setHorarioFieldErrors] = useState({ horaInicio: '', horaFin: '', aula: '' });
  const [horarioFormData, setHorarioFormData] = useState({
    diaSemana: 'Lunes',
    horaInicio: '',
    horaFin: '',
    aula: '',
    modalidad: 'PRESENCIAL'
  });

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

  const nombreCursoRegex = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\s'\-]+$/;
  const codigoCursoRegex = /^[A-Za-z0-9_-]+$/;
  const textoCarreraRegex = /^[A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ\s'\-]+$/;

  const sanitizeLettersOnly = (value) => value.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ\s'\-]/g, '');
  const sanitizeCourseCode = (value) => value.replace(/[^A-Za-z0-9_-]/g, '').toUpperCase();
  const sanitizeCareerValue = (value) => value.replace(/[^A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ\s'\-]/g, '');

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
      codigoCurso: '',
      descripcion: '',
      ciclo: 1,
      modalidad: 'PRESENCIAL',
      capacidadMaxima: '',
      creditos: '',
      fechaInicio: todayDateInputValue(),
      fechaTermino: '',
      horasTeoricas: 1,
      horasPracticas: 0,
      horasLaboratorio: 0,
      estado: 'ACTIVO',
      carrera: '',
      profesorId: '',
      profesorAsignado: '',
    });
    setSelectedCursoId(null);
    setIsEditMode(false);
    setFieldErrors({
      nombre: '',
      codigoCurso: '',
      descripcion: '',
      carrera: '',
      ciclo: '',
      capacidadMaxima: '',
      creditos: '',
      horasTeoricas: '',
      horasPracticas: '',
      horasLaboratorio: '',
      fechaInicio: '',
      fechaTermino: '',
    });
  };

  const handleOpenCreate = () => {
    if (!canManageCursos) return;
    setErrorMsg('');
    setSuccessMsg('');
    setFieldErrors({
      nombre: '',
      codigoCurso: '',
      descripcion: '',
      carrera: '',
      ciclo: '',
      capacidadMaxima: '',
      creditos: '',
      horasTeoricas: '',
      horasPracticas: '',
      horasLaboratorio: '',
      fechaInicio: '',
      fechaTermino: '',
    });
    resetForm();
    setOpenModal(true);
  };

  const handleOpenEdit = (curso) => {
    if (!canManageCursos) return;
    const cursoId = getCursoId(curso);
    const profesorData = normalizeProfesorData(curso);

    if (!cursoId) {
      setErrorMsg('No se pudo identificar el curso a editar.');
      return;
    }

    setIsEditMode(true);
    setErrorMsg('');
    setSuccessMsg('');
    setFieldErrors({
      nombre: '',
      codigoCurso: '',
      descripcion: '',
      carrera: '',
      ciclo: '',
      capacidadMaxima: '',
      creditos: '',
      horasTeoricas: '',
      horasPracticas: '',
      horasLaboratorio: '',
      fechaInicio: '',
      fechaTermino: '',
    });
    setSelectedCursoId(cursoId);
    setFormData({
      nombre: curso.nombre || '',
      codigoCurso: curso.codigoCurso || '',
      descripcion: curso.descripcion || '',
      ciclo: curso.ciclo ?? 1,
      modalidad: curso.modalidad || 'PRESENCIAL',
      capacidadMaxima: curso.capacidadMaxima ?? '',
      creditos: curso.creditos ?? '',
      fechaInicio: toDateInputValue(curso.fechaInicio),
      fechaTermino: toDateInputValue(curso.fechaTermino),
      horasTeoricas: curso.horasTeoricas ?? 0,
      horasPracticas: curso.horasPracticas ?? 0,
      horasLaboratorio: curso.horasLaboratorio ?? 0,
      estado: curso.estado || 'ACTIVO',
      carrera: curso?.carrera?.nombre || '',
      profesorId: profesorData.profesorId,
      profesorAsignado: profesorData.profesorAsignado,
    });
    setOpenModal(true);
  };

  const handleCloseModal = () => {
    if (isSubmitting) {
      return;
    }
    setErrorMsg('');
    setOpenModal(false);
    resetForm();
  };

  const setValidationError = (field, message) => {
    setFieldErrors((prev) => ({
      ...prev,
      [field]: message,
    }));
    setErrorMsg(message);
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

    setFieldErrors({
      nombre: '',
      codigoCurso: '',
      descripcion: '',
      carrera: '',
      ciclo: '',
      capacidadMaxima: '',
      creditos: '',
      horasTeoricas: '',
      horasPracticas: '',
      horasLaboratorio: '',
      fechaInicio: '',
      fechaTermino: '',
    });

    const nombre = formData.nombre.trim();
    const codigoCurso = formData.codigoCurso.trim();
    const descripcion = formData.descripcion.trim();
    const carrera = formData.carrera.trim();
    const profesorAsignado = formData.profesorAsignado.trim();
    const profesorId = formData.profesorId.trim();
    const ciclo = Number(formData.ciclo);
    const modalidad = String(formData.modalidad || 'PRESENCIAL').toUpperCase();
    const fechaInicio = formData.fechaInicio || todayDateInputValue();
    const fechaTermino = formData.fechaTermino;
    const capacidadMaxima = Number(formData.capacidadMaxima);
    const creditos = Number(formData.creditos);
    const horasTeoricas = Number(formData.horasTeoricas || 0);
    const horasPracticas = Number(formData.horasPracticas || 0);
    const horasLaboratorio = Number(formData.horasLaboratorio || 0);

    if (!nombre || !descripcion) {
      if (!nombre) setValidationError('nombre', 'El nombre del curso es obligatorio.');
      if (!descripcion) setValidationError('descripcion', 'La descripción del curso es obligatoria.');
      return;
    }

    if (!nombreCursoRegex.test(nombre)) {
      setValidationError('nombre', 'El nombre solo puede contener letras, espacios, apóstrofes y guiones.');
      return;
    }

    if (nombre.length > 150) {
      setValidationError('nombre', 'El nombre no debe superar 150 caracteres.');
      return;
    }

    if (codigoCurso && codigoCurso.length > 30) {
      setValidationError('codigoCurso', 'El código no debe superar 30 caracteres.');
      return;
    }

    if (codigoCurso && !codigoCursoRegex.test(codigoCurso)) {
      setValidationError('codigoCurso', 'El código solo puede contener letras, números, guiones y guiones bajos.');
      return;
    }

    if (descripcion.length > 1000) {
      setValidationError('descripcion', 'La descripción no debe superar 1000 caracteres.');
      return;
    }

    if (!Number.isInteger(ciclo) || ciclo < 1 || ciclo > 14) {
      setValidationError('ciclo', 'El ciclo debe estar entre 1 y 14.');
      return;
    }

    if (!Number.isInteger(capacidadMaxima) || capacidadMaxima <= 0 || capacidadMaxima > 40) {
      setValidationError('capacidadMaxima', 'La capacidad máxima debe estar entre 1 y 40.');
      return;
    }

    if (!Number.isInteger(creditos) || creditos <= 0 || creditos > 8) {
      setValidationError('creditos', 'Los créditos deben estar entre 1 y 8.');
      return;
    }

    if (!Number.isInteger(horasTeoricas) || horasTeoricas < 0 || horasTeoricas > 20
      || !Number.isInteger(horasPracticas) || horasPracticas < 0 || horasPracticas > 20
      || !Number.isInteger(horasLaboratorio) || horasLaboratorio < 0 || horasLaboratorio > 20) {
      setValidationError('horasTeoricas', 'Las horas deben estar entre 0 y 20.');
      setValidationError('horasPracticas', 'Las horas deben estar entre 0 y 20.');
      setValidationError('horasLaboratorio', 'Las horas deben estar entre 0 y 20.');
      return;
    }

    if ((horasTeoricas + horasPracticas + horasLaboratorio) <= 0) {
      setValidationError('horasTeoricas', 'Debes registrar al menos una hora académica entre HT, HP o HL.');
      return;
    }

    if (!fechaInicio || !fechaTermino) {
      if (!fechaInicio) setValidationError('fechaInicio', 'La fecha de inicio es obligatoria.');
      if (!fechaTermino) setValidationError('fechaTermino', 'La fecha de término es obligatoria.');
      return;
    }

    if (fechaTermino < fechaInicio) {
      setValidationError('fechaTermino', 'La fecha de término no puede ser anterior a la fecha de inicio.');
      return;
    }

    if (!carrera) {
      setValidationError('carrera', 'La carrera es obligatoria.');
      return;
    }

    if (!textoCarreraRegex.test(carrera)) {
      setValidationError('carrera', 'La carrera solo puede contener letras, números, espacios, apóstrofes y guiones.');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg('');
      setSuccessMsg('');

      const payload = {
        nombre,
        codigoCurso: codigoCurso || undefined,
        descripcion,
        ciclo,
        modalidad,
        capacidadMaxima,
        creditos,
        fechaInicio,
        fechaTermino,
        horasTeoricas,
        horasPracticas,
        horasLaboratorio,
        estado: formData.estado,
        carrera,
        profesorId: profesorId || undefined,
        profesorAsignado: profesorAsignado || undefined,
      };

      if (isEditMode) {
        await CursoService.modificar(selectedCursoId, payload);
      } else {
        await CursoService.registro(payload);
      }

      await fetchCursos();
      setSuccessMsg(isEditMode ? 'Curso actualizado correctamente.' : 'Curso registrado correctamente.');
      handleCloseModal();
    } catch (error) {
      console.error('Error al guardar curso', error);
      setErrorMsg(extractBackendValidationMessage(error, 'No se pudo guardar el curso.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteCurso = async (curso) => {
    if (!canManageCursos) return;

    const cursoId = getCursoId(curso);

    if (!cursoId) {
      setErrorMsg('No se pudo identificar el curso a eliminar.');
      return;
    }

    setConfirmDialog({
      title: 'Eliminar curso',
      message: `¿Eliminar el curso "${curso.nombre}"?`,
      confirmText: 'Eliminar',
      severity: 'error',
      onConfirm: async () => {
        try {
          setErrorMsg('');
          setSuccessMsg('');
          await CursoService.eliminar(cursoId);
          await fetchCursos();
          setSuccessMsg('Curso eliminado correctamente.');
        } catch (error) {
          console.error('Error al eliminar curso', error);
          setErrorMsg(extractBackendValidationMessage(error, 'No se pudo eliminar el curso.'));
        }
      },
    });
  };

  const handleInscribirCurso = async (curso) => {
    if (!canSelfEnroll) return;

    const cursoId = getCursoId(curso);
    if (!cursoId) {
      setErrorMsg('No se pudo identificar el curso para inscripción.');
      return;
    }

    setConfirmDialog({
      title: rol === 'ROLE_PROFESOR' ? 'Inscribirme a dictar' : 'Inscribirme al curso',
      message: `¿Inscribirte al curso "${curso.nombre}"?`,
      confirmText: 'Inscribirme',
      severity: 'warning',
      onConfirm: async () => {
        try {
          setErrorMsg('');
          setSuccessMsg('');
          await CursoService.inscribirCurso(cursoId);
          await fetchCursos();
          setSuccessMsg('Inscripción registrada correctamente.');
        } catch (error) {
          console.error('Error al inscribirse en curso', error);
          setErrorMsg(extractBackendValidationMessage(error, 'No se pudo completar la inscripción.'));
        }
      },
    });
  };

  useEffect(() => {
    fetchCursos();
    fetchProfesores();
  }, [fetchCursos, fetchProfesores]);

  const handleOpenHorarios = async (curso) => {
    const cursoId = getCursoId(curso);
    setSelectedCursoId(cursoId);
    try {
      const horarios = await CursoService.listarHorarios(cursoId);
      setHorariosCurso(Array.isArray(horarios) ? horarios : []);
    } catch (error) {
      console.error("Error al cargar horarios:", error);
      setHorariosCurso([]);
    }
    setHorarioFormData({
      diaSemana: 'Lunes', horaInicio: '', horaFin: '', aula: '', modalidad: 'PRESENCIAL'
    });
    setHorarioFieldErrors({ horaInicio: '', horaFin: '', aula: '' });
    setOpenHorarioModal(true);
  };

  const handleGuardarHorario = async () => {
    setHorarioFieldErrors({ horaInicio: '', horaFin: '', aula: '' });
    if (!horarioFormData.horaInicio || !horarioFormData.horaFin) {
      setHorarioFieldErrors((prev) => ({
        ...prev,
        horaInicio: !horarioFormData.horaInicio ? 'La hora de inicio es obligatoria.' : '',
        horaFin: !horarioFormData.horaFin ? 'La hora de fin es obligatoria.' : '',
      }));
      setErrorMsg('Las horas son obligatorias.');
      return;
    }
    try {
      setIsSubmitting(true);
      await CursoService.agregarHorario(selectedCursoId, horarioFormData);
      // Recargar la lista de horarios
      const horarios = await CursoService.listarHorarios(selectedCursoId);
      setHorariosCurso(Array.isArray(horarios) ? horarios : []);
      // Limpiar el formulario parcial
      setHorarioFormData({
        diaSemana: 'Lunes', horaInicio: '', horaFin: '', aula: '', modalidad: 'PRESENCIAL'
      });
      setSuccessMsg("Horario agregado correctamente");
    } catch (error) {
      setErrorMsg(extractBackendValidationMessage(error, "No se pudo agregar el horario"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEliminarHorario = async (horario) => {
    if (!canManageCursos) return;

    const horarioId = horario?.id || horario?._id;
    if (!selectedCursoId || !horarioId) {
      setErrorMsg('No se pudo identificar el horario a eliminar.');
      return;
    }

    setConfirmDialog({
      title: 'Eliminar horario',
      message: `¿Eliminar el horario ${horario.diaSemana} de ${horario.horaInicio} a ${horario.horaFin}?`,
      confirmText: 'Eliminar',
      severity: 'error',
      onConfirm: async () => {
        try {
          setIsSubmitting(true);
          setErrorMsg('');
          setSuccessMsg('');

          await CursoService.eliminarHorario(selectedCursoId, horarioId);
          const horarios = await CursoService.listarHorarios(selectedCursoId);
          setHorariosCurso(Array.isArray(horarios) ? horarios : []);
          setSuccessMsg('Horario eliminado correctamente.');
        } catch (error) {
          console.error('Error al eliminar horario', error);
          setErrorMsg(extractBackendValidationMessage(error, 'No se pudo eliminar el horario.'));
        } finally {
          setIsSubmitting(false);
        }
      },
    });
  };


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
          label: 'Horarios',
          icon: <CalendarMonth />,
          color: 'secondary',
          onClick: handleOpenHorarios,
        },
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
          <Button
            variant="outlined"
            color="secondary"
            startIcon={<CalendarMonth />}
            onClick={() => navigate('/modulo/horarios-profesor')}
            sx={{ mr: 2 }}
          >
            Ver Horarios por Profesor
          </Button>
        )}
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
              slotProps={{ htmlInput: { maxLength: 150 } }}
              helperText={fieldErrors.nombre || 'Máximo 150 caracteres. Solo letras, espacios, apóstrofes y guiones.'}
              error={Boolean(fieldErrors.nombre)}
            />
            <TextField
              margin="dense"
              label="Código de curso"
              name="codigoCurso"
              value={formData.codigoCurso}
              onChange={handleChangeForm}
              fullWidth
              slotProps={{ htmlInput: { maxLength: 30 } }}
              helperText={fieldErrors.codigoCurso || 'Opcional, máximo 30 caracteres. Solo letras, números, guiones y guiones bajos.'}
              error={Boolean(fieldErrors.codigoCurso)}
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
              slotProps={{ htmlInput: { maxLength: 1000 } }}
              helperText={fieldErrors.descripcion || 'Máximo 1000 caracteres.'}
              error={Boolean(fieldErrors.descripcion)}
            />
            <TextField
              margin="dense"
              label="Carrera"
              name="carrera"
              value={formData.carrera}
              onChange={handleChangeForm}
              fullWidth
              required
              helperText={fieldErrors.carrera || 'ID o nombre de carrera.'}
              error={Boolean(fieldErrors.carrera)}
            />
            <TextField
              margin="dense"
              label="Ciclo"
              name="ciclo"
              type="number"
              value={formData.ciclo}
              onChange={handleChangeForm}
              fullWidth
              required
              slotProps={{ htmlInput: { min: 1, max: 14 } }}
              helperText={fieldErrors.ciclo || 'Rango permitido: 1 a 14.'}
              error={Boolean(fieldErrors.ciclo)}
            />
            <TextField
              margin="dense"
              label="Modalidad"
              name="modalidad"
              value={formData.modalidad}
              onChange={handleChangeForm}
              fullWidth
              select
            >
              <MenuItem value="PRESENCIAL">PRESENCIAL</MenuItem>
              <MenuItem value="VIRTUAL">VIRTUAL</MenuItem>
              <MenuItem value="HIBRIDO">HIBRIDO</MenuItem>
            </TextField>
            <TextField
              margin="dense"
              label="Estado"
              name="estado"
              value={formData.estado}
              onChange={handleChangeForm}
              fullWidth
              select
            >
              <MenuItem value="ACTIVO">ACTIVO</MenuItem>
              <MenuItem value="INACTIVO">INACTIVO</MenuItem>
              <MenuItem value="CERRADO">CERRADO</MenuItem>
            </TextField>
            <TextField
              margin="dense"
              label="Fecha de inicio"
              name="fechaInicio"
              type="date"
              value={formData.fechaInicio}
              onChange={handleChangeForm}
              fullWidth
              required
              slotProps={{
                inputLabel: { shrink: true },
                htmlInput: { max: formData.fechaTermino || undefined },
              }}
              helperText={fieldErrors.fechaInicio || 'Debe ser menor o igual a la fecha de término.'}
              error={Boolean(fieldErrors.fechaInicio)}
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
              slotProps={{ htmlInput: { min: 1, max: 40 } }}
              helperText={fieldErrors.capacidadMaxima || 'Rango permitido: 1 a 40.'}
              error={Boolean(fieldErrors.capacidadMaxima)}
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
              slotProps={{ htmlInput: { min: 1, max: 8 } }}
              helperText={fieldErrors.creditos || 'Rango permitido: 1 a 8.'}
              error={Boolean(fieldErrors.creditos)}
            />
            <TextField
              margin="dense"
              label="Horas teóricas"
              name="horasTeoricas"
              type="number"
              value={formData.horasTeoricas}
              onChange={handleChangeForm}
              fullWidth
              required
              slotProps={{ htmlInput: { min: 0, max: 20 } }}
              helperText={fieldErrors.horasTeoricas || 'Rango permitido: 0 a 20.'}
              error={Boolean(fieldErrors.horasTeoricas)}
            />
            <TextField
              margin="dense"
              label="Horas prácticas"
              name="horasPracticas"
              type="number"
              value={formData.horasPracticas}
              onChange={handleChangeForm}
              fullWidth
              required
              slotProps={{ htmlInput: { min: 0, max: 20 } }}
              helperText={fieldErrors.horasPracticas || 'Rango permitido: 0 a 20.'}
              error={Boolean(fieldErrors.horasPracticas)}
            />
            <TextField
              margin="dense"
              label="Horas laboratorio"
              name="horasLaboratorio"
              type="number"
              value={formData.horasLaboratorio}
              onChange={handleChangeForm}
              fullWidth
              required
              slotProps={{ htmlInput: { min: 0, max: 20 } }}
              helperText={fieldErrors.horasLaboratorio || 'Rango permitido: 0 a 20.'}
              error={Boolean(fieldErrors.horasLaboratorio)}
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
                htmlInput: { min: formData.fechaInicio || todayDateInputValue() },
              }}
              helperText={fieldErrors.fechaTermino || 'Debe ser mayor o igual a la fecha de inicio.'}
              error={Boolean(fieldErrors.fechaTermino)}
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
      {/* Modal para Gestión de Horarios */}
      <Dialog open={openHorarioModal} onClose={() => setOpenHorarioModal(false)} fullWidth maxWidth="sm">
        <DialogTitle>Gestión de Horarios</DialogTitle>
        <DialogContent>
          <Typography variant="subtitle1" sx={{ mt: 1, mb: 1, fontWeight: 'bold' }}>
            Horarios Registrados
          </Typography>
          {horariosCurso.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              No hay horarios registrados.
            </Typography>
          ) : (
            <Box sx={{ mb: 3 }}>
              {horariosCurso.map((h, i) => (
                <Box
                  key={h.id || i}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 1,
                    mb: 0.75,
                    p: 1,
                    borderRadius: 1,
                    bgcolor: 'action.hover',
                  }}
                >
                  <Typography variant="body2">
                    • {h.diaSemana}: {h.horaInicio} - {h.horaFin} ({h.modalidad} | Aula: {h.aula || 'N/A'})
                  </Typography>
                  <Tooltip title="Eliminar horario">
                    <span>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleEliminarHorario(h)}
                        disabled={isSubmitting}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </Box>
              ))}
            </Box>
          )}

          <Typography variant="subtitle1" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            Agregar Nuevo Horario
          </Typography>

          <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
            <TextField
              select
              label="Día"
              fullWidth
              value={horarioFormData.diaSemana}
              onChange={(e) => setHorarioFormData({ ...horarioFormData, diaSemana: e.target.value })}
            >
              {['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'].map(dia => (
                <MenuItem key={dia} value={dia}>{dia}</MenuItem>
              ))}
            </TextField>
            <TextField
              label="Modalidad"
              select
              fullWidth
              value={horarioFormData.modalidad}
              onChange={(e) => setHorarioFormData({ ...horarioFormData, modalidad: e.target.value })}
            >
              <MenuItem value="PRESENCIAL">Presencial</MenuItem>
              <MenuItem value="VIRTUAL">Virtual</MenuItem>
              <MenuItem value="HIBRIDO">Híbrido</MenuItem>
            </TextField>
          </Box>

          <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
            <TextField
              label="Hora Inicio"
              type="time"
              fullWidth
              InputLabelProps={{ shrink: true }}
              inputProps={{ step: 300 }}
              value={horarioFormData.horaInicio}
              onChange={(e) => setHorarioFormData({ ...horarioFormData, horaInicio: e.target.value })}
              error={Boolean(horarioFieldErrors.horaInicio)}
              helperText={horarioFieldErrors.horaInicio || ''}
            />
            <TextField
              label="Hora Fin"
              type="time"
              fullWidth
              InputLabelProps={{ shrink: true }}
              inputProps={{ step: 300 }}
              value={horarioFormData.horaFin}
              onChange={(e) => setHorarioFormData({ ...horarioFormData, horaFin: e.target.value })}
              error={Boolean(horarioFieldErrors.horaFin)}
              helperText={horarioFieldErrors.horaFin || ''}
            />
          </Box>
          <TextField
            label="Aula (Opcional)"
            fullWidth
            value={horarioFormData.aula}
            onChange={(e) => setHorarioFormData({ ...horarioFormData, aula: e.target.value })}
            error={Boolean(horarioFieldErrors.aula)}
            helperText={horarioFieldErrors.aula || ''}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenHorarioModal(false)}>Cerrar</Button>
          <Button onClick={handleGuardarHorario} variant="contained" disabled={isSubmitting}>
            {isSubmitting ? 'Guardando...' : 'Agregar Horario'}
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

export default CursosListado;