import { Box, Typography, Container } from '@mui/material';

const Footer = () => (
  <Box
    component="footer"
    sx={{
      py: 2.5,
      px: 2,
      mt: 2,
      borderTop: (theme) => `1px solid ${theme.palette.divider}`,
      backgroundColor: (theme) => theme.palette.mode === 'dark' ? 'rgba(15, 23, 42, 0.65)' : 'rgba(255, 255, 255, 0.72)',
      backdropFilter: 'blur(12px)',
      borderRadius: 4,
    }}
  >
    <Container maxWidth="lg">
      <Typography variant="body2" color="text.secondary" align="center" sx={{ fontWeight: 500 }}>
        {'© '} {new Date().getFullYear()} Gestión de Inscripción de Cursos. Todos los derechos reservados.
      </Typography>
    </Container>
  </Box>
);

export default Footer;