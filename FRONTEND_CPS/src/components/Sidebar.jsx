import { useEffect, useState } from 'react';
import { Avatar, Box, Divider, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Toolbar, Typography, Chip } from '@mui/material';
import { alpha } from '@mui/material/styles';
import { Dashboard, Book, People, Assessment, SmartToy, AssignmentTurnedIn, School, Settings, AccountCircle, Apartment, CalendarMonth, Circle, VideoCall } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useLocation } from 'react-router-dom';
import { getDisplayNameFromToken, getRoleFromToken } from '../utils/authIdentity';
import AuthService from '../services/AuthService';

const drawerWidth = 240;

const normalizeRole = (roleValue) => {
  const raw = String(roleValue || '').trim().toUpperCase();
  if (!raw) return '';
  if (raw.startsWith('ROLE_')) return raw;

  if (raw.includes('ADMIN')) return 'ROLE_ADMIN';
  if (raw.includes('PROF')) return 'ROLE_PROFESOR';
  if (raw.includes('ALUM')) return 'ROLE_ALUMNO';

  return '';
};

const Sidebar = ({ open }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const token = localStorage.getItem('token');
  const rolGuardado = localStorage.getItem('rol');
  const nombreGuardado = localStorage.getItem('nombre') || '';
  const nombreToken = getDisplayNameFromToken(token);
  const [perfilUsuario, setPerfilUsuario] = useState(null);

  useEffect(() => {
    let active = true;

    const cargarUsuarioConectado = async () => {
      try {
        const perfil = await AuthService.usuarioConectado();
        if (!active || !perfil) return;

        setPerfilUsuario(perfil);

        if (perfil?.nombre) {
          localStorage.setItem('nombre', perfil.nombre);
        }
        if (perfil?.rol) {
          const rolNormalizado = normalizeRole(perfil.rol);
          if (rolNormalizado) {
            localStorage.setItem('rol', rolNormalizado);
          }
        }
      } catch {
        // Si falla /auth/me, mantenemos fallback local/token.
      }
    };

    if (token) {
      cargarUsuarioConectado();
    }

    return () => {
      active = false;
    };
  }, [token]);

  const nombrePerfil = perfilUsuario?.nombre || '';
  const nombre = nombrePerfil
    || (nombreGuardado && !nombreGuardado.includes('@') ? nombreGuardado : '')
    || nombreToken
    || nombreGuardado
    || 'Usuario';

  const rolLabelByCode = {
    ROLE_ADMIN: 'Administrador',
    ROLE_PROFESOR: 'Profesor',
    ROLE_ALUMNO: 'Alumno',
  };

  const rolFinal = normalizeRole(perfilUsuario?.rol)
    || normalizeRole(rolGuardado)
    || normalizeRole(getRoleFromToken(token));
  const rolLabel = rolLabelByCode[rolFinal] || 'Usuario';

  const menuByRole = {
    ROLE_ADMIN: [
      { text: 'Panel de Control', icon: <Dashboard />, path: '/dashboard/admin' },
      { text: 'IA', icon: <SmartToy />, path: '/modulo/ia' },
      { text: 'Videoconferencia', icon: <VideoCall />, path: '/videoconferencia' },
      { text: 'Cursos', icon: <Book />, path: '/cursos' },
      { text: 'Carreras', icon: <Apartment />, path: '/carreras' },
      { text: 'Usuarios', icon: <People />, path: '/usuarios' },
      { text: 'Reportes', icon: <Assessment />, path: '/modulo/reportes' },
      { text: 'Configuración', icon: <Settings />, path: '/modulo/configuracion' },
    ],
    ROLE_PROFESOR: [
      { text: 'Panel de Control', icon: <Dashboard />, path: '/dashboard/profesor' },
      { text: 'IA', icon: <SmartToy />, path: '/modulo/ia' },
      { text: 'Videoconferencia', icon: <VideoCall />, path: '/videoconferencia' },
      { text: 'Mis Cursos', icon: <School />, path: '/cursos' },
      { text: 'Actividades', icon: <Book />, path: '/modulo/actividades' },
      { text: 'Inscripciones', icon: <AssignmentTurnedIn />, path: '/modulo/inscripciones' },
      { text: 'Reportes', icon: <Assessment />, path: '/modulo/reportes' },
      { text: 'Configuración', icon: <Settings />, path: '/modulo/configuracion' },
    ],
    ROLE_ALUMNO: [
      { text: 'Panel de Control', icon: <Dashboard />, path: '/dashboard/alumno' },
      { text: 'Mi Horario', icon: <CalendarMonth />, path: '/modulo/mi-horario' },
      { text: 'Cursos', icon: <Book />, path: '/cursos' },
      { text: 'Videoconferencia', icon: <VideoCall />, path: '/videoconferencia' },
      { text: 'Mis Cursos Inscritos', icon: <AssignmentTurnedIn />, path: '/modulo/inscripciones' },
      { text: 'Mis Reportes', icon: <Assessment />, path: '/modulo/reportes' },
      { text: 'IA', icon: <SmartToy />, path: '/modulo/ia' },
      { text: 'Configuración', icon: <Settings />, path: '/modulo/configuracion' },
    ],
  };

  const menuItems = menuByRole[rolFinal] || menuByRole.ROLE_ALUMNO;

  return (
    <Box
      component="aside"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        display: open ? 'block' : 'none',
        // Sticky a altura completa del viewport: el fondo cubre siempre de arriba
        // a abajo (sin hueco al scrollear) y la navegacion queda fija a la vista.
        position: 'sticky',
        top: 0,
        alignSelf: 'flex-start',
        height: '100vh',
        overflowY: 'auto',
        bgcolor: 'background.paper',
        boxShadow: '18px 0 40px rgba(15, 23, 42, 0.12)',
        borderRight: 'none',
        boxSizing: 'border-box',
      }}
    >
      <Toolbar /> {/* Espacio para que no choque con el Navbar */}

      <Box
        sx={{
          px: 2,
          py: 3,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          textAlign: 'center',
          gap: 1.25,
        }}
      >
        <Avatar
          sx={{ width: 64, height: 64, bgcolor: 'primary.main', cursor: 'pointer', boxShadow: '0 12px 28px rgba(37, 99, 235, 0.32)' }}
          onClick={() => navigate('/perfil-usuario')}
          title="Ir a perfil"
        >
          <AccountCircle sx={{ fontSize: 36 }} />
        </Avatar>
        <Typography variant="subtitle1" sx={{ fontWeight: 800, maxWidth: '100%' }} noWrap title={nombre}>
          {nombre}
        </Typography>
        <Chip
          icon={<Circle sx={{ fontSize: 10 }} />}
          label={rolLabel}
          size="small"
          sx={{
            borderRadius: 999,
            fontWeight: 700,
            px: 0.5,
            '& .MuiChip-icon': { fontSize: 8 },
          }}
        />
      </Box>

      <Divider />
      <List sx={{ px: 1.5, py: 1 }}>
        {menuItems.map((item) => {
          const isActive = item.path === '/'
            ? location.pathname === '/'
            : location.pathname === item.path || location.pathname.startsWith(`${item.path}/`);

          return (
          <ListItem disablePadding key={item.text}>
            <ListItemButton
              selected={isActive}
              onClick={() => {
                if (location.pathname.startsWith('/videoconferencia') && item.path !== location.pathname) {
                  window.dispatchEvent(new Event('app:leave-videoconference'));
                }
                navigate(item.path);
              }}
              sx={{
                mb: 0.5,
                borderRadius: 3,
                '&:hover': {
                  bgcolor: 'action.hover',
                },
                '&.Mui-selected': {
                  bgcolor: (theme) => alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.24 : 0.12),
                  color: 'primary.main',
                  '& .MuiListItemIcon-root': { color: 'primary.main' },
                  '&:hover': {
                    bgcolor: (theme) => alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.3 : 0.18),
                  },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 38, color: 'text.secondary' }}>{item.icon}</ListItemIcon>
              <ListItemText
                primary={<Typography variant="body2" sx={{ fontWeight: isActive ? 800 : 600 }}>{item.text}</Typography>}
              />
            </ListItemButton>
          </ListItem>
          );
        })}
      </List>
        </Box>
  );
};

export default Sidebar;