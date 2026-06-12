import { Box, Paper, Typography, Button } from '@mui/material';
import { LockOutlined } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../components/PageHeader';

const NoAutorizado = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol');

  const getDashboardPath = () => {
    if (rol === 'ROLE_ADMIN') return '/dashboard/admin';
    if (rol === 'ROLE_PROFESOR') return '/dashboard/profesor';
    if (rol === 'ROLE_ALUMNO') return '/dashboard/alumno';
    return '/';
  };

  return (
    <Box>
      <PageHeader
        title="Acceso denegado"
        icon={<LockOutlined />}
      />

      <Paper sx={{ p: 4 }}>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          No tienes permisos para ingresar a esta sección con tu rol actual.
        </Typography>

        <Button variant="contained" onClick={() => navigate(getDashboardPath())}>
          Ir a mi dashboard
        </Button>
      </Paper>
    </Box>
  );
};

export default NoAutorizado;
