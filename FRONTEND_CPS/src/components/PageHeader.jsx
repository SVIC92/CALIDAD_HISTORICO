import { Box, Typography, IconButton, Tooltip } from '@mui/material';
import { alpha } from '@mui/material/styles';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';

/**
 * Cabecera de pagina unificada para toda la aplicacion.
 *
 * Estandariza tipografia (h4 / 800), espaciado y el patron de
 * icono + titulo + subtitulo + boton volver + acciones, de modo que
 * todas las paginas compartan el mismo lenguaje visual.
 *
 * Props:
 *  - title (string | node): titulo principal (obligatorio).
 *  - subtitle (string | node): texto de apoyo opcional bajo el titulo.
 *  - icon (node): icono opcional mostrado dentro de un recuadro tematizado.
 *  - onBack (fn): si se entrega, muestra un boton circular de "volver".
 *  - actions (node): contenido alineado a la derecha (botones, filtros...).
 *  - sx: estilos extra para el contenedor.
 */
const PageHeader = ({ title, subtitle, icon, onBack, actions, sx }) => {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: { xs: 'flex-start', sm: 'center' },
        flexDirection: { xs: 'column', sm: 'row' },
        gap: 2,
        mb: { xs: 3, md: 4 },
        ...sx,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 0, flexGrow: 1 }}>
        {onBack && (
          <Tooltip title="Volver">
            <IconButton
              onClick={onBack}
              aria-label="Volver"
              sx={{
                bgcolor: 'action.hover',
                '&:hover': { bgcolor: 'action.selected' },
              }}
            >
              <ArrowBackRoundedIcon />
            </IconButton>
          </Tooltip>
        )}

        {icon && (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 52,
              height: 52,
              flexShrink: 0,
              borderRadius: 3,
              color: 'primary.main',
              bgcolor: (theme) => alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.22 : 0.12),
              '& .MuiSvgIcon-root': { fontSize: 28 },
            }}
          >
            {icon}
          </Box>
        )}

        <Box sx={{ minWidth: 0 }}>
          <Typography variant="h4" component="h1" sx={{ fontWeight: 800, lineHeight: 1.2 }}>
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
      </Box>

      {actions && (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            flexWrap: 'wrap',
            flexShrink: 0,
            width: { xs: '100%', sm: 'auto' },
          }}
        >
          {actions}
        </Box>
      )}
    </Box>
  );
};

export default PageHeader;
