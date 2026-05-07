import { useMemo, useState } from 'react';
import {
  Fab,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Tooltip,
} from '@mui/material';
import {
  AccessibilityNew,
  TextIncrease,
  Palette,
  Contrast,
  DensitySmall,
  RestartAlt,
  CheckCircle,
  MotionPhotosAuto,
  FontDownload,
  Link,
  ContrastOutlined,
  DarkMode,
  LightMode,
  SpaceBar,
  Visibility,
  ImageNotSupported,
  FormatAlignLeft,
  FormatAlignCenter,
  FormatAlignRight,
  FormatAlignJustify,
  FormatLineSpacing,
} from '@mui/icons-material';
import { useUISettings } from '../context/UISettingsContext';

const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

const AccessibilityFloatingMenu = () => {
  const { preferences, updatePreferences, resetAllPreferences } = useUISettings();
  const [anchorEl, setAnchorEl] = useState(null);

  const open = Boolean(anchorEl);

  const fontScaleLabel = useMemo(() => {
    if (preferences.fontScale >= 140) return 'Nivel 4';
    if (preferences.fontScale >= 130) return 'Nivel 3';
    if (preferences.fontScale >= 120) return 'Nivel 2';
    if (preferences.fontScale >= 110) return 'Nivel 1';
    return 'Normal';
  }, [preferences.fontScale]);
  const saturationLabel = useMemo(() => {
    if (preferences.saturationMode === 'low') return 'Poca saturación';
    if (preferences.saturationMode === 'high') return 'Mucha saturación';
    return 'Normal';
  }, [preferences.saturationMode]);
  const contrastLabel = useMemo(() => {
    if (preferences.contrastMode === 'invertido') return 'Invertido';
    if (preferences.contrastMode === 'oscuro') return 'Oscuro';
    if (preferences.contrastMode === 'luz-alta') return 'Luz alta';
    return 'Normal';
  }, [preferences.contrastMode]);
  const readableFontLabel = useMemo(() => {
    if (preferences.readableFontMode === 'dyslexia') return 'Apto para dislexia';
    if (preferences.readableFontMode === 'readable') return 'Fuente más legible';
    return 'Normal';
  }, [preferences.readableFontMode]);
  const readingAidLabel = useMemo(() => {
    if (preferences.readingAidMode === 'cursor') return 'Cursor grande';
    if (preferences.readingAidMode === 'mask') return 'Máscara de lectura';
    if (preferences.readingAidMode === 'guide') return 'Guía de lectura';
    return 'Normal';
  }, [preferences.readingAidMode]);
  const imagesLabel = useMemo(() => (preferences.hideImages ? 'Ocultas' : 'Visibles'), [preferences.hideImages]);
  const textAlignLabel = useMemo(() => {
    if (preferences.textAlignMode === 'left') return 'Izquierda';
    if (preferences.textAlignMode === 'center') return 'Centrado';
    if (preferences.textAlignMode === 'right') return 'Derecha';
    if (preferences.textAlignMode === 'justify') return 'Justificado';
    return 'Normal';
  }, [preferences.textAlignMode]);
  const lineHeightLabel = useMemo(() => {
    if (preferences.lineHeightMode === 'low') return 'Baja';
    if (preferences.lineHeightMode === 'medium') return 'Media';
    if (preferences.lineHeightMode === 'high') return 'Alta';
    return 'Normal';
  }, [preferences.lineHeightMode]);
  const spacingLabel = useMemo(() => {
    if (preferences.spacingMode === 'low') return 'Poco espaciado';
    if (preferences.spacingMode === 'medium') return 'Medio espaciado';
    if (preferences.spacingMode === 'high') return 'Espaciado alto';
    return 'Normal';
  }, [preferences.spacingMode]);

  const handleOpen = (event) => setAnchorEl(event.currentTarget);
  const handleClose = () => setAnchorEl(null);

  const cycleFontScale = () => {
    updatePreferences((prev) => {
      const current = [100, 110, 120, 130, 140].includes(Number(prev.fontScale)) ? Number(prev.fontScale) : 100;
      const next = current === 100 ? 110 : current === 110 ? 120 : current === 120 ? 130 : current === 130 ? 140 : 100;
      return { fontScale: next };
    });
  };

  const cycleSaturation = () => {
    updatePreferences((prev) => {
      const current = prev.saturationMode === 'low' || prev.saturationMode === 'high' || prev.saturationMode === 'normal'
        ? prev.saturationMode
        : 'normal';

      const next = current === 'low' ? 'high' : current === 'high' ? 'normal' : 'low';
      return { saturationMode: next };
    });
  };

  const cycleContrast = () => {
    updatePreferences((prev) => {
      const current = prev.contrastMode === 'invertido' || prev.contrastMode === 'oscuro' || prev.contrastMode === 'luz-alta'
        ? prev.contrastMode
        : 'normal';

      const next = current === 'normal' ? 'invertido' : current === 'invertido' ? 'oscuro' : current === 'oscuro' ? 'luz-alta' : 'normal';
      return { contrastMode: next };
    });
  };

  const toggleCompact = () => {
    updatePreferences((prev) => ({ compact: !prev.compact }));
  };

  const toggleReduceMotion = () => {
    updatePreferences((prev) => ({ reduceMotion: !prev.reduceMotion }));
  };

  const cycleSpacing = () => {
    updatePreferences((prev) => {
      const current = prev.spacingMode === 'low' || prev.spacingMode === 'medium' || prev.spacingMode === 'high' || prev.spacingMode === 'normal'
        ? prev.spacingMode
        : 'normal';

      const next = current === 'normal' ? 'low' : current === 'low' ? 'medium' : current === 'medium' ? 'high' : 'normal';
      return { spacingMode: next };
    });
  };

  const cycleReadableFont = () => {
    updatePreferences((prev) => {
      const current = prev.readableFontMode === 'readable' || prev.readableFontMode === 'dyslexia' || prev.readableFontMode === 'normal'
        ? prev.readableFontMode
        : 'normal';

      const next = current === 'normal' ? 'readable' : current === 'readable' ? 'dyslexia' : 'normal';
      return { readableFontMode: next };
    });
  };

  const cycleReadingAid = () => {
    updatePreferences((prev) => {
      const current = prev.readingAidMode === 'cursor' || prev.readingAidMode === 'mask' || prev.readingAidMode === 'guide' || prev.readingAidMode === 'normal'
        ? prev.readingAidMode
        : 'normal';

      const next = current === 'normal' ? 'cursor' : current === 'cursor' ? 'mask' : current === 'mask' ? 'guide' : 'normal';
      return { readingAidMode: next };
    });
  };

  const toggleImages = () => {
    updatePreferences((prev) => ({ hideImages: !prev.hideImages }));
  };

  const cycleTextAlign = () => {
    updatePreferences((prev) => {
      const current = prev.textAlignMode === 'left' || prev.textAlignMode === 'center' || prev.textAlignMode === 'right' || prev.textAlignMode === 'justify' || prev.textAlignMode === 'normal'
        ? prev.textAlignMode
        : 'normal';

      const next = current === 'normal' ? 'left' : current === 'left' ? 'center' : current === 'center' ? 'right' : current === 'right' ? 'justify' : 'normal';
      return { textAlignMode: next };
    });
  };

  const cycleLineHeight = () => {
    updatePreferences((prev) => {
      const current = prev.lineHeightMode === 'low' || prev.lineHeightMode === 'medium' || prev.lineHeightMode === 'high' || prev.lineHeightMode === 'normal'
        ? prev.lineHeightMode
        : 'normal';

      const next = current === 'normal' ? 'low' : current === 'low' ? 'medium' : current === 'medium' ? 'high' : 'normal';
      return { lineHeightMode: next };
    });
  };

  const toggleUnderlineLinks = () => {
    updatePreferences((prev) => ({ underlineLinks: !prev.underlineLinks }));
  };

  const handleReset = () => {
    resetAllPreferences();
    handleClose();
  };

  return (
    <>
      <Tooltip title="Accesibilidad" placement="left">
        <Fab
          color="primary"
          aria-label="Accesibilidad"
          onClick={handleOpen}
          sx={{
            position: 'fixed',
            right: 24,
            bottom: 24,
            zIndex: (theme) => theme.zIndex.drawer + 3000,
            boxShadow: '0 16px 36px rgba(37, 99, 235, 0.35)',
          }}
        >
          <AccessibilityNew />
        </Fab>
      </Tooltip>

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        transformOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        slotProps={{
          paper: {
            sx: {
              minWidth: 280,
              borderRadius: 3,
              mt: 1,
              border: '1px solid',
              borderColor: 'divider',
              boxShadow: '0 20px 50px rgba(15, 23, 42, 0.22)',
            },
          },
        }}
      >
        <MenuItem onClick={cycleFontScale} selected={preferences.fontScale !== 100}>
          <ListItemIcon>
            <TextIncrease fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Tamaño de fuente" secondary={fontScaleLabel} />
        </MenuItem>

        <MenuItem onClick={cycleSaturation} selected={preferences.saturationMode !== 'normal'}>
          <ListItemIcon>
            <Palette fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Saturación" secondary={saturationLabel} />
        </MenuItem>

        <MenuItem onClick={cycleContrast} selected={preferences.contrastMode !== 'normal'}>
          <ListItemIcon>
            {preferences.contrastMode === 'invertido' ? <ContrastOutlined fontSize="small" /> : preferences.contrastMode === 'oscuro' ? <DarkMode fontSize="small" /> : preferences.contrastMode === 'luz-alta' ? <LightMode fontSize="small" /> : <Contrast fontSize="small" />}
          </ListItemIcon>
          <ListItemText primary="Contraste" secondary={contrastLabel} />
        </MenuItem>

        <MenuItem onClick={toggleCompact} selected={preferences.compact}>
          <ListItemIcon>
            {preferences.compact ? <CheckCircle fontSize="small" /> : <DensitySmall fontSize="small" />}
          </ListItemIcon>
          <ListItemText primary="Modo compacto" secondary={preferences.compact ? 'Activo' : 'Más espacio' } />
        </MenuItem>

        <MenuItem onClick={toggleReduceMotion} selected={preferences.reduceMotion}>
          <ListItemIcon>
            {preferences.reduceMotion ? <CheckCircle fontSize="small" /> : <MotionPhotosAuto fontSize="small" />}
          </ListItemIcon>
          <ListItemText primary="Reducir animaciones" secondary={preferences.reduceMotion ? 'Activo' : 'Menos movimiento visual'} />
        </MenuItem>

        <MenuItem onClick={cycleSpacing} selected={preferences.spacingMode !== 'normal'}>
          <ListItemIcon>
            <SpaceBar fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Espaciado" secondary={spacingLabel} />
        </MenuItem>

        <MenuItem onClick={cycleReadableFont} selected={preferences.readableFontMode !== 'normal'}>
          <ListItemIcon>
            {preferences.readableFontMode === 'dyslexia' ? <CheckCircle fontSize="small" /> : <FontDownload fontSize="small" />}
          </ListItemIcon>
          <ListItemText primary="Fuente más legible" secondary={readableFontLabel} />
        </MenuItem>

        <MenuItem onClick={cycleReadingAid} selected={preferences.readingAidMode !== 'normal'}>
          <ListItemIcon>
            {preferences.readingAidMode === 'normal' ? <Visibility fontSize="small" /> : <CheckCircle fontSize="small" />}
          </ListItemIcon>
          <ListItemText primary="Guía de lectura" secondary={readingAidLabel} />
        </MenuItem>

        <MenuItem onClick={toggleImages} selected={preferences.hideImages}>
          <ListItemIcon>
            {preferences.hideImages ? <CheckCircle fontSize="small" /> : <ImageNotSupported fontSize="small" />}
          </ListItemIcon>
          <ListItemText primary="No mostrar imágenes" secondary={imagesLabel} />
        </MenuItem>

        <MenuItem onClick={cycleTextAlign} selected={preferences.textAlignMode !== 'normal'}>
          <ListItemIcon>
            {preferences.textAlignMode === 'left'
              ? <FormatAlignLeft fontSize="small" />
              : preferences.textAlignMode === 'center'
                ? <FormatAlignCenter fontSize="small" />
                : preferences.textAlignMode === 'right'
                  ? <FormatAlignRight fontSize="small" />
                  : preferences.textAlignMode === 'justify'
                    ? <FormatAlignJustify fontSize="small" />
                    : <FormatAlignLeft fontSize="small" />}
          </ListItemIcon>
          <ListItemText primary="Texto alineado" secondary={textAlignLabel} />
        </MenuItem>

        <MenuItem onClick={cycleLineHeight} selected={preferences.lineHeightMode !== 'normal'}>
          <ListItemIcon>
            <FormatLineSpacing fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Altura de línea" secondary={lineHeightLabel} />
        </MenuItem>

        <MenuItem onClick={toggleUnderlineLinks} selected={preferences.underlineLinks}>
          <ListItemIcon>
            {preferences.underlineLinks ? <CheckCircle fontSize="small" /> : <Link fontSize="small" />}
          </ListItemIcon>
          <ListItemText primary="Subrayar enlaces" secondary={preferences.underlineLinks ? 'Activo' : 'Mejor identificación de links'} />
        </MenuItem>

        <Divider />

        <MenuItem onClick={handleReset}>
          <ListItemIcon>
            <RestartAlt fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Restablecer accesibilidad" />
        </MenuItem>
      </Menu>
    </>
  );
};

export default AccessibilityFloatingMenu;