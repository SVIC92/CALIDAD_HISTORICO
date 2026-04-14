import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Slider,
  Stack,
  Switch,
  Typography,
} from '@mui/material';
import { ArrowBack, RestartAlt } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useUISettings } from '../context/UISettingsContext';

const Configuracion = () => {
  const navigate = useNavigate();
  const { preferences, updatePreferences, resetAllPreferences } = useUISettings();
  const [successMsg, setSuccessMsg] = useState('');

  const handleRestaurar = () => {
    resetAllPreferences();
    setSuccessMsg('Se restauraron los valores por defecto.');
  };

  const previewTextSize = `${preferences.fontScale}%`;

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ mb: 3, alignItems: 'center' }}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/dashboard')}>
          Volver
        </Button>
        <Typography variant="h4" sx={{ flexGrow: 1 }}>
          Configuración de Interfaz
        </Typography>
        <Button variant="outlined" color="warning" startIcon={<RestartAlt />} onClick={handleRestaurar}>
          Restaurar
        </Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Los cambios se aplican automaticamente.
      </Alert>

      {successMsg && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {successMsg}
        </Alert>
      )}

      <Paper sx={{ p: 3, mb: 3 }}>
        <Stack spacing={3}>
          <FormControl fullWidth>
            <InputLabel id="theme-select-label">Tema</InputLabel>
            <Select
              labelId="theme-select-label"
              label="Tema"
              value={preferences.theme}
              onChange={(e) => {
                updatePreferences({ theme: e.target.value });
                setSuccessMsg('');
              }}
            >
              <MenuItem value="light">Claro</MenuItem>
              <MenuItem value="dark">Oscuro</MenuItem>
            </Select>
          </FormControl>

          <Box>
            <Typography gutterBottom>Tamaño de letra global: {previewTextSize}</Typography>
            <Slider
              value={preferences.fontScale}
              min={90}
              max={120}
              step={5}
              marks
              valueLabelDisplay="auto"
              onChange={(_, value) => {
                const fontScale = Array.isArray(value) ? value[0] : value;
                updatePreferences({ fontScale });
                setSuccessMsg('');
              }}
            />
          </Box>

          <FormControlLabel
            control={(
              <Switch
                checked={preferences.compact}
                onChange={(e) => {
                  updatePreferences({ compact: e.target.checked });
                  setSuccessMsg('');
                }}
              />
            )}
            label="Modo compacto (reduce espacios en listas y tarjetas)"
          />
        </Stack>
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 1 }}>
          Vista previa rápida
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Aplica tus preferencias para personalizar el entorno: tema, densidad y legibilidad.
        </Typography>
        <Typography variant="body1">Texto de prueba con la escala actual: {previewTextSize}</Typography>
      </Paper>
    </Box>
  );
};

export default Configuracion;
