import { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Box, CssBaseline, Toolbar } from '@mui/material';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import Footer from '../components/Footer';
import UsuarioService from '../services/UsuarioService';
import GlobalChatNotifier from '../components/GlobalChatNotifier';

const DashboardLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const location = useLocation();
  const isVideoConferenceRoute = location.pathname.startsWith('/videoconferencia');

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
        minHeight: isVideoConferenceRoute ? '100vh' : 'auto',
        alignItems: isVideoConferenceRoute ? 'stretch' : 'flex-start',
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
          minHeight: isVideoConferenceRoute ? 0 : 'auto',
          px: isVideoConferenceRoute ? 0 : { xs: 2, sm: 3, md: 4 },
          py: isVideoConferenceRoute ? 0 : { xs: 2, sm: 3 },
          display: 'flex',
          flexDirection: 'column',
          gap: isVideoConferenceRoute ? 0 : 3,
          alignSelf: isVideoConferenceRoute ? 'stretch' : 'flex-start',
        }}
      >
        <Toolbar /> {/* Espaciador para el AppBar fixed */}
        
        <Box
          sx={{
            flex: isVideoConferenceRoute ? 1 : '0 0 auto',
            minHeight: isVideoConferenceRoute ? 0 : 'auto',
            width: '100%',
            maxWidth: isVideoConferenceRoute ? 'none' : '1600px',
            mx: isVideoConferenceRoute ? 0 : 'auto',
            display: 'flex',
            flexDirection: 'column',
            overflow: isVideoConferenceRoute ? 'hidden' : 'hidden',
            p: 0,
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