import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, CssBaseline, Toolbar } from '@mui/material';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import Footer from '../components/Footer';
import UsuarioService from '../services/UsuarioService';
import GlobalChatNotifier from '../components/GlobalChatNotifier';

const DashboardLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const toggleSidebar = () => setSidebarOpen(!sidebarOpen);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return undefined;

    const ping = async () => {
      try {
        await UsuarioService.pingPresencia();
      } catch {
        // Silencioso para no interferir con la UX si el endpoint aun no esta desplegado.
      }
    };

    ping();
    const intervalId = window.setInterval(ping, 30000);

    return () => window.clearInterval(intervalId);
  }, []);

  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100vh',
        background: 'transparent',
      }}
    >
      <CssBaseline />
      <Navbar onToggleSidebar={toggleSidebar} />
      
      <Sidebar open={sidebarOpen} variant="persistent" />

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          px: { xs: 2, sm: 3, md: 4 },
          py: { xs: 2, sm: 3 },
          display: 'flex',
          flexDirection: 'column',
          gap: 3,
        }}
      >
        <Toolbar /> {/* Espaciador para el AppBar fixed */}
        
        <Box
          sx={{
            flex: 1,
            width: '100%',
            maxWidth: '1600px',
            mx: 'auto',
            p: { xs: 0, sm: 0 },
          }}
        >
          <Outlet /> {/* Aquí se cargan las páginas (Cursos, Alumnos, etc.) */}
          <GlobalChatNotifier />
        </Box>

        <Footer />
      </Box>
    </Box>
  );
};

export default DashboardLayout;