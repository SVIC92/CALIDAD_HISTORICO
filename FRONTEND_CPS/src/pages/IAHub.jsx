import { Box, Button, Paper, Stack, Typography } from '@mui/material';
import { ArrowBack, AutoAwesome, Chat, Grading } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

const IAHub = () => {
  const navigate = useNavigate();

  const modules = [
    {
      title: 'Chat IA',
      description: 'Conversa con EstudIA según el rol activo y obtén respuestas contextuales.',
      icon: <Chat color="primary" />,
      action: () => navigate('/modulo/ia/chat'),
      buttonLabel: 'Ir al Chat',
    },
    {
      title: 'Generador de Rúbricas',
      description: 'Crea rúbricas de evaluación por IA y expórtalas en PDF o Word.',
      icon: <Grading color="primary" />,
      action: () => navigate('/modulo/ia/rubricas'),
      buttonLabel: 'Ir al Generador',
    },
  ];

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 2, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/dashboard')}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          IA
        </Typography>
      </Stack>

      <Paper sx={{ p: 2.5, mb: 2, bgcolor: 'primary.50' }}>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 0.5 }}>
          <AutoAwesome color="primary" />
          <Typography variant="h6">Centro de herramientas IA</Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary">
          Selecciona un submódulo para conversar con la IA o generar rúbricas automáticamente.
        </Typography>
      </Paper>

      <Stack spacing={2}>
        {modules.map((module) => (
          <Paper key={module.title} sx={{ p: 2.2 }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ alignItems: { sm: 'center' } }}>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexGrow: 1 }}>
                {module.icon}
                <Box>
                  <Typography variant="h6">{module.title}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {module.description}
                  </Typography>
                </Box>
              </Stack>
              <Button variant="contained" onClick={module.action}>
                {module.buttonLabel}
              </Button>
            </Stack>
          </Paper>
        ))}
      </Stack>
    </Box>
  );
};

export default IAHub;
