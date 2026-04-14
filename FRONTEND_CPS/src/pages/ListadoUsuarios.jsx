import { useEffect, useMemo, useState } from 'react';
import { Box, Typography, TextField, InputAdornment, Alert, Button } from '@mui/material';
import { ArrowBack, Search } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import DataTable from '../components/DataTable';
import UsuarioService from '../services/UsuarioService';

const normalizeUsuario = (usuario) => ({
  id: usuario.id || usuario._id || '-',
  nombre: usuario.nombre || '-',
  email: usuario.email || '-',
  rol: usuario.rol || '-',
  fechaCreacion: usuario.fechaCreacion || usuario.createdAt || '-',
});

const ListadoUsuarios = () => {
  const [usuarios, setUsuarios] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUsuarios = async () => {
      try {
        setErrorMsg('');
        const data = await UsuarioService.listar();
        const normalized = Array.isArray(data)
          ? data.map(normalizeUsuario)
          : [];
        setUsuarios(normalized);
      } catch (error) {
        console.error('Error al obtener usuarios', error);
        const backendMessage = error?.response?.data?.error || error?.message;
        setErrorMsg(backendMessage || 'No se pudo cargar el listado de usuarios.');
        setUsuarios([]);
      }
    };

    fetchUsuarios();
  }, []);

  const columns = [
    { id: 'nombre', label: 'Nombre' },
    { id: 'email', label: 'Email' },
    { id: 'rol', label: 'Rol', align: 'center' },
    { id: 'fechaCreacion', label: 'Fecha de Creación' },
  ];

  const filteredData = useMemo(() => {
    const term = searchTerm.toLowerCase();
    return usuarios.filter((u) =>
      `${u.nombre} ${u.email} ${u.rol}`.toLowerCase().includes(term)
    );
  }, [usuarios, searchTerm]);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/usuarios')} sx={{ mr: 2 }}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Listado de Usuarios
        </Typography>
      </Box>

      {errorMsg && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {errorMsg}
        </Alert>
      )}

      <TextField
        fullWidth
        placeholder="Buscar por nombre, email o rol..."
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
        exportFileName="usuarios"
      />
    </Box>
  );
};

export default ListadoUsuarios;
