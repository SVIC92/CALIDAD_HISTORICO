import { useState } from 'react';
import { AppBar, Toolbar, Typography, IconButton, Button, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Box, Avatar } from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import LogoutIcon from '@mui/icons-material/Logout';
import SchoolRoundedIcon from '@mui/icons-material/SchoolRounded';
import { useNavigate } from 'react-router-dom';

const Navbar = ({ onToggleSidebar }) => {
  const navigate = useNavigate();
  const [openLogoutDialog, setOpenLogoutDialog] = useState(false);

  const handleConfirmLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('rol');
    localStorage.removeItem('nombre');
    setOpenLogoutDialog(false);
    navigate('/');
  };

  return (
    <>
      <AppBar 
        position="fixed" 
        sx={{ 
          zIndex: (theme) => theme.zIndex.drawer + 1,
          bgcolor: 'transparent',
          px: { xs: 0.5, sm: 1.5 },
          py: 1,
          boxShadow: 'none',
        }}
      >
        <Toolbar
          sx={{
            minHeight: 72,
            borderRadius: 4,
            mx: { xs: 0, sm: 1, md: 2 },
            border: (theme) => `1px solid ${theme.palette.divider}`,
            backgroundColor: (theme) => theme.palette.mode === 'dark' ? 'rgba(15, 23, 42, 0.78)' : 'rgba(255, 255, 255, 0.84)',
            backdropFilter: 'blur(18px)',
            boxShadow: '0 16px 40px rgba(15, 23, 42, 0.12)',
          }}
        >
          <IconButton color="inherit" edge="start" onClick={onToggleSidebar} sx={{ mr: 1.5 }}>
            <MenuIcon />
          </IconButton>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexGrow: 1, minWidth: 0 }}>
            <Avatar sx={{ width: 38, height: 38, bgcolor: 'primary.main' }}>
              <SchoolRoundedIcon fontSize="small" />
            </Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="overline" sx={{ display: 'block', lineHeight: 1.1, color: 'text.secondary', letterSpacing: 1.6 }}>
                CPS
              </Typography>
              <Typography variant="subtitle1" component="div" sx={{ fontWeight: 800 }} noWrap>
                Gestión de Cursos
              </Typography>
            </Box>
          </Box>

          <Button
            variant="outlined"
            onClick={() => setOpenLogoutDialog(true)}
            startIcon={<LogoutIcon />}
            sx={{ px: 2 }}
          >
            Salir
          </Button>
        </Toolbar>
      </AppBar>

      <Dialog open={openLogoutDialog} onClose={() => setOpenLogoutDialog(false)}>
        <DialogTitle>Confirmar cierre de sesión</DialogTitle>
        <DialogContent>
          <DialogContentText>
            ¿Seguro que deseas cerrar sesión?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenLogoutDialog(false)} color="inherit">
            Cancelar
          </Button>
          <Button onClick={handleConfirmLogout} color="error" variant="contained" autoFocus>
            Cerrar sesión
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default Navbar;