import { Card, CardContent, Box, Typography, Avatar } from '@mui/material';
import { alpha } from '@mui/material/styles';

/**
 * Tarjeta de indicador (KPI) unificada para los dashboards.
 *
 * Usa SIEMPRE colores de la paleta del tema (no hex sueltos), de modo
 * que los indicadores sean coherentes entre Admin / Profesor / Alumno.
 *
 * Props:
 *  - title (string): etiqueta del indicador.
 *  - value (string | number): valor destacado.
 *  - icon (node): icono mostrado en el avatar.
 *  - color (string): clave de la paleta -> 'primary' | 'secondary' | 'info' |
 *      'success' | 'warning' | 'error'. Por defecto 'primary'.
 *  - subtitle (string): texto secundario opcional bajo el valor.
 *  - onClick (fn): si se entrega, la tarjeta es interactiva.
 */
const PALETTE_KEYS = ['primary', 'secondary', 'info', 'success', 'warning', 'error'];

const StatCard = ({ title, value, icon, color = 'primary', subtitle, onClick }) => {
  const paletteKey = PALETTE_KEYS.includes(color) ? color : 'primary';

  return (
    <Card
      onClick={onClick}
      sx={{
        height: '100%',
        cursor: onClick ? 'pointer' : 'default',
        transition: 'transform 0.2s ease, box-shadow 0.2s ease',
        ...(onClick && {
          '&:hover': { transform: 'translateY(-4px)', boxShadow: 4 },
        }),
      }}
    >
      <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Avatar
          variant="rounded"
          sx={{
            width: 56,
            height: 56,
            borderRadius: 3,
            color: `${paletteKey}.main`,
            bgcolor: (theme) => alpha(theme.palette[paletteKey].main, theme.palette.mode === 'dark' ? 0.24 : 0.14),
          }}
        >
          {icon}
        </Avatar>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }} noWrap>
            {title}
          </Typography>
          <Typography variant="h4" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
            {value}
          </Typography>
          {subtitle && (
            <Typography variant="caption" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
      </CardContent>
    </Card>
  );
};

export default StatCard;
