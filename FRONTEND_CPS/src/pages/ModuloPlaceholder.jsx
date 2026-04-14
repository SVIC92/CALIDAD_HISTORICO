import { Box, Typography, Paper, Button } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';

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
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 'medium', textTransform: 'capitalize' }}>
        {modulo}
      </Typography>

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
