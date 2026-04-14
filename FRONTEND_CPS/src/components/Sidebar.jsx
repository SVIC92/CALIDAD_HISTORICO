import { Avatar, Box, Divider, Drawer, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Toolbar, Typography } from '@mui/material';
import { Dashboard, Book, People, Assessment, SmartToy, AssignmentTurnedIn, School, Settings, AccountCircle } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { getDisplayNameFromToken, getRoleFromToken } from '../utils/authIdentity';

const drawerWidth = 240;

const Sidebar = ({ open, variant }) => {
  const navigate = useNavigate();
  const token = localStorage.getItem('token');
  const rol = localStorage.getItem('rol');
  const nombreGuardado = localStorage.getItem('nombre') || '';
  const nombreToken = getDisplayNameFromToken(token);
  const nombre = (nombreGuardado && !nombreGuardado.includes('@') ? nombreGuardado : nombreToken) || nombreGuardado || 'Usuario';

  const rolLabelByCode = {
    ROLE_ADMIN: 'Administrador',
    ROLE_PROFESOR: 'Profesor',
    ROLE_ALUMNO: 'Alumno',
  };

  const rolFinal = rol || getRoleFromToken(token);
  const rolLabel = rolLabelByCode[rolFinal] || 'Usuario';

  const menuByRole = {
    ROLE_ADMIN: [
      { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard/admin' },
      { text: 'EstudIA', icon: <SmartToy />, path: '/modulo/estudia' },
      { text: 'Cursos', icon: <Book />, path: '/cursos' },
      { text: 'Usuarios', icon: <People />, path: '/usuarios' },
      { text: 'Reportes', icon: <Assessment />, path: '/modulo/reportes' },
      { text: 'Configuración', icon: <Settings />, path: '/modulo/configuracion' },
    ],
    ROLE_PROFESOR: [
      { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard/profesor' },
      { text: 'EstudIA', icon: <SmartToy />, path: '/modulo/estudia' },
      { text: 'Mis Cursos', icon: <School />, path: '/cursos' },
      { text: 'Actividades', icon: <Book />, path: '/modulo/actividades' },
      { text: 'Inscripciones', icon: <AssignmentTurnedIn />, path: '/modulo/inscripciones' },
      { text: 'Reportes', icon: <Assessment />, path: '/modulo/reportes' },
      { text: 'Configuración', icon: <Settings />, path: '/modulo/configuracion' },
    ],
    ROLE_ALUMNO: [
      { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard/alumno' },
      { text: 'Cursos', icon: <Book />, path: '/cursos' },
      { text: 'Mis Cursos Inscritos', icon: <AssignmentTurnedIn />, path: '/modulo/inscripciones' },
      { text: 'Mis Reportes', icon: <Assessment />, path: '/modulo/reportes' },
      { text: 'EstudIA', icon: <SmartToy />, path: '/modulo/estudia' },
      { text: 'Configuración', icon: <Settings />, path: '/modulo/configuracion' },
    ],
  };

  const menuItems = menuByRole[rol] || menuByRole.ROLE_ALUMNO;

  return (
    <Drawer
      variant={variant}
      open={open}
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' },
      }}
    >
      <Toolbar /> {/* Espacio para que no choque con el Navbar */}

      <Box
        sx={{
          px: 2,
          py: 2.5,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          textAlign: 'center',
          gap: 1,
        }}
      >
        <Avatar sx={{ width: 56, height: 56, bgcolor: 'primary.main' }}>
          <AccountCircle sx={{ fontSize: 36 }} />
        </Avatar>
        <Typography variant="subtitle1" sx={{ fontWeight: 700, maxWidth: '100%' }} noWrap title={nombre}>
          {nombre}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ letterSpacing: 0.5 }}>
          {rolLabel}
        </Typography>
      </Box>

      <Divider />
      <List>
        {menuItems.map((item) => (
          <ListItem disablePadding key={item.text}>
            <ListItemButton onClick={() => navigate(item.path)}>
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Drawer>
  );
};

export default Sidebar;