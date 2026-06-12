import { Box, Card, CardActionArea, CardContent, Grid, Typography } from '@mui/material';
import { People, Wifi, ManageAccounts } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../components/PageHeader';

const UsuarioHub = () => {
  const navigate = useNavigate();

  const submodulos = [
    {
      title: 'Lista Usuarios',
      description: 'Visualiza, busca y exporta el listado completo de usuarios del sistema.',
      icon: <People sx={{ fontSize: 48, color: 'primary.main' }} />,
      path: '/usuarios/listado',
    },
    {
      title: 'Usuarios Conectados En Tiempo Real',
      description: 'Monitorea los usuarios activos actualmente con actualización periódica.',
      icon: <Wifi sx={{ fontSize: 48, color: 'success.main' }} />,
      path: '/usuarios/conectados',
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Gestión de Usuarios"
        subtitle="Selecciona el submódulo de usuarios para continuar."
        icon={<ManageAccounts />}
      />

      <Grid container spacing={3}>
        {submodulos.map((item) => (
          <Grid size={{ xs: 12, md: 6 }} key={item.title}>
            <Card sx={{ height: '100%', transition: '0.2s', '&:hover': { transform: 'translateY(-4px)', boxShadow: 6 } }}>
              <CardActionArea sx={{ p: 3, height: '100%' }} onClick={() => navigate(item.path)}>
                <Box sx={{ mb: 2 }}>{item.icon}</Box>
                <CardContent sx={{ px: 0, pb: 0 }}>
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                    {item.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {item.description}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default UsuarioHub;
