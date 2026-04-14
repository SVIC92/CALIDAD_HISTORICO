import { useCallback, useEffect, useMemo, useState } from 'react';
import { Box, Button, InputAdornment, TextField, Typography } from '@mui/material';
import { ArrowBack, Search } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import DataTable from '../components/DataTable';
import CursoService from '../services/CursoService';

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

const hasProfesorAsignado = (curso, email) => {
  const profesor = curso?.profesorAsignado;
  if (!profesor) return false;

  if (typeof profesor === 'string') {
    return profesor.toLowerCase() === email;
  }

  return (profesor?.email || '').toLowerCase() === email;
};

const CursosDictadosProfesor = () => {
  const [cursos, setCursos] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const navigate = useNavigate();

  const fetchCursos = useCallback(async () => {
    try {
      const inscritos = await CursoService.listarInscritosProfesor();
      setCursos(Array.isArray(inscritos) ? inscritos : []);
    } catch (error) {
      console.error('Error al obtener cursos dictados', error);
      setCursos([]);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchCursos();
  }, [fetchCursos]);

  const columns = [
    { id: 'nombre', label: 'Nombre del Curso' },
    { id: 'descripcion', label: 'Descripción' },
    { id: 'capacidadMaxima', label: 'Capacidad', align: 'center' },
    { id: 'creditos', label: 'Créditos', align: 'center' },
    { id: 'profesorAsignado', label: 'Profesor' },
  ];

  const filteredData = useMemo(() => {
    const term = searchTerm.toLowerCase();
    return cursos.filter((c) => (c.nombre || '').toLowerCase().includes(term));
  }, [cursos, searchTerm]);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/cursos')} sx={{ mr: 2 }}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Cursos Aprobados para Dictar
        </Typography>
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

      <DataTable
        columns={columns}
        data={filteredData}
        exportFileName="cursos-dictados-profesor"
      />
    </Box>
  );
};

export default CursosDictadosProfesor;
