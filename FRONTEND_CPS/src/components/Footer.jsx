import { Box, Typography, Container } from '@mui/material';

const Footer = () => (
  <Box
    component="footer"
    sx={{
      py: 3,
      px: 2,
      mt: 'auto',
      backgroundColor: 'background.paper',
      borderTop: (theme) => `1px solid ${theme.palette.divider}`,
    }}
  >
    <Container maxWidth="lg">
      <Typography variant="body2" color="text.secondary" align="center">
        {'© '} {new Date().getFullYear()} Gestión de Inscripción de Cursos. Todos los derechos reservados.
      </Typography>
    </Container>
  </Box>
);

export default Footer;