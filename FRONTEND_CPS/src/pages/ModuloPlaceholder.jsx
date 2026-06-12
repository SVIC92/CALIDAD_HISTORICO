import { Box, Typography, Paper, Button } from '@mui/material';
import { ViewModule } from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import PageHeader from '../components/PageHeader';

const ModuloPlaceholder = () => {
  const navigate = useNavigate();
  const { moduloNombre } = useParams();
  const rol = localStorage.getItem('rol');

  const dashboardPath = rol === 'ROLE_ADMIN'
    ? '/dashboard/admin'
    : rol === 'ROLE_PROFESOR'
      ? '/dashboard/profesor'
      : '/dashboard/alumno';

  const modulo = (moduloNombre || 'modulo').replaceAll('-', ' ');

  return (
    <Box>
      <PageHeader
        title={modulo}
        icon={<ViewModule />}
        onBack={() => navigate(dashboardPath)}
        sx={{ textTransform: 'capitalize' }}
      />

      <Paper sx={{ p: 4 }}>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
          Este submódulo aún no está implementado en el frontend.
        </Typography>
        <Button variant="contained" onClick={() => navigate(dashboardPath)}>
          Volver al Dashboard
        </Button>
      </Paper>
    </Box>
  );
};

export default ModuloPlaceholder;
