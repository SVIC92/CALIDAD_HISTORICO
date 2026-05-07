import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { createTheme, CssBaseline, ThemeProvider } from '@mui/material';
import { applyPreferences, getDefaultPreferences, loadPreferences } from '../utils/uiPreferences';

const UISettingsContext = createContext(null);

export const UISettingsProvider = ({ children }) => {
  const [preferences, setPreferences] = useState(() => loadPreferences());
  const isDark = preferences.theme === 'dark';
  const contrastMode = preferences.contrastMode || 'normal';
  const isContrastActive = contrastMode !== 'normal';
  const isDarkContrast = contrastMode === 'oscuro';
  const isLightHigh = contrastMode === 'luz-alta';
  const themeMode = isDarkContrast ? 'dark' : isLightHigh ? 'light' : preferences.theme;
  const readableFontMode = preferences.readableFontMode || 'normal';
  const isReadableFont = readableFontMode === 'readable';
  const isDyslexiaFont = readableFontMode === 'dyslexia';
  const spacingMode = preferences.spacingMode || 'normal';
  const isSpacingLow = spacingMode === 'low';
  const isSpacingMedium = spacingMode === 'medium';
  const isSpacingHigh = spacingMode === 'high';

  useEffect(() => {
    applyPreferences(preferences);
  }, [preferences]);

  const updatePreferences = useCallback((patch) => {
    setPreferences((prev) => {
      const changes = typeof patch === 'function' ? patch(prev) : patch;
      return { ...prev, ...changes };
    });
  }, []);

  const resetAllPreferences = useCallback(() => {
    setPreferences(getDefaultPreferences());
  }, []);

  const theme = createTheme({
    palette: {
      mode: themeMode,
      primary: isContrastActive
        ? {
            main: isLightHigh ? '#0b57d0' : isDark ? '#90caf9' : '#0b57d0',
            light: isLightHigh ? '#4285f4' : isDark ? '#bbdefb' : '#4285f4',
            dark: isLightHigh ? '#083b9a' : isDark ? '#64b5f6' : '#083b9a',
            contrastText: isLightHigh || !isDark ? '#ffffff' : '#000000',
          }
        : {
            main: '#2563eb',
            light: '#60a5fa',
            dark: '#1d4ed8',
            contrastText: '#ffffff',
          },
      secondary: isContrastActive
        ? {
            main: isLightHigh ? '#146c43' : isDark ? '#a5d6a7' : '#146c43',
            light: isLightHigh ? '#2e7d32' : isDark ? '#c8e6c9' : '#2e7d32',
            dark: isLightHigh ? '#0b4f31' : isDark ? '#81c784' : '#0b4f31',
          }
        : {
            main: '#0f766e',
            light: '#14b8a6',
            dark: '#115e59',
          },
      background: {
        default: isContrastActive
          ? (isDarkContrast ? '#000000' : '#ffffff')
          : (isDark ? '#0b1220' : '#eef3fb'),
        paper: isContrastActive
          ? (isDarkContrast ? '#111111' : '#ffffff')
          : (isDark ? '#111a2e' : '#ffffff'),
      },
      divider: isContrastActive
        ? (isDarkContrast ? 'rgba(255,255,255,0.42)' : 'rgba(0,0,0,0.38)')
        : (isDark ? 'rgba(148, 163, 184, 0.18)' : 'rgba(148, 163, 184, 0.24)'),
      ...(isContrastActive ? {
        text: {
          primary: isDarkContrast ? '#ffffff' : '#000000',
          secondary: isDarkContrast ? '#f5f5f5' : '#1f2937',
        },
      } : {}),
    },
    typography: {
      fontFamily: isDyslexiaFont
        ? 'OpenDyslexicRegular, sans-serif'
        : isReadableFont
          ? 'Verdana, "Segoe UI", Arial, sans-serif'
          : '"Segoe UI Variable", "Segoe UI", "Aptos", "Helvetica Neue", Arial, sans-serif',
      fontSize: Math.round((14 * preferences.fontScale) / 100),
      h1: { fontWeight: 800, letterSpacing: isSpacingHigh ? '0.035em' : isSpacingMedium ? '0.02em' : isSpacingLow ? '0.008em' : '-0.04em', lineHeight: 1.25 },
      h2: { fontWeight: 800, letterSpacing: isSpacingHigh ? '0.03em' : isSpacingMedium ? '0.016em' : isSpacingLow ? '0.006em' : '-0.035em', lineHeight: 1.25 },
      h3: { fontWeight: 750, letterSpacing: isSpacingHigh ? '0.022em' : isSpacingMedium ? '0.012em' : isSpacingLow ? '0.005em' : '-0.03em', lineHeight: 1.3 },
      h4: { fontWeight: 750, letterSpacing: isSpacingHigh ? '0.018em' : isSpacingMedium ? '0.01em' : isSpacingLow ? '0.004em' : '-0.025em', lineHeight: 1.3 },
      h5: { fontWeight: 700, letterSpacing: isSpacingHigh ? '0.015em' : isSpacingMedium ? '0.008em' : isSpacingLow ? '0.003em' : '-0.02em', lineHeight: 1.35 },
      h6: { fontWeight: 700 },
      subtitle1: { fontWeight: 650, lineHeight: isDyslexiaFont ? 1.8 : 1.6, letterSpacing: isDyslexiaFont ? '0.03em' : isSpacingHigh ? '0.02em' : isSpacingMedium ? '0.012em' : isSpacingLow ? '0.005em' : undefined },
      body1: { lineHeight: isDyslexiaFont ? 1.95 : 1.7, letterSpacing: isDyslexiaFont ? '0.04em' : isSpacingHigh ? '0.04em' : isSpacingMedium ? '0.02em' : isSpacingLow ? '0.008em' : undefined },
      body2: { lineHeight: isDyslexiaFont ? 1.85 : 1.6, letterSpacing: isDyslexiaFont ? '0.035em' : isSpacingHigh ? '0.03em' : isSpacingMedium ? '0.015em' : isSpacingLow ? '0.006em' : undefined },
    },
    shape: {
      borderRadius: 16,
    },
    spacing: preferences.compact ? 6 : 8,
    shadows: [
      'none',
      '0 1px 2px rgba(15, 23, 42, 0.06)',
      '0 2px 8px rgba(15, 23, 42, 0.08)',
      '0 12px 28px rgba(15, 23, 42, 0.10)',
      '0 16px 34px rgba(15, 23, 42, 0.12)',
      '0 22px 50px rgba(15, 23, 42, 0.14)',
      '0 28px 60px rgba(15, 23, 42, 0.16)',
      '0 34px 72px rgba(15, 23, 42, 0.18)',
      ...Array(17).fill('0 36px 80px rgba(15, 23, 42, 0.20)'),
    ],
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: {
            backgroundImage: themeMode === 'dark'
              ? 'radial-gradient(circle at top, rgba(59, 130, 246, 0.10), transparent 35%), linear-gradient(180deg, #0b1220 0%, #0f172a 100%)'
              : 'radial-gradient(circle at top, rgba(37, 99, 235, 0.08), transparent 32%), linear-gradient(180deg, #eef3fb 0%, #f8fbff 100%)',
            backgroundAttachment: 'fixed',
            WebkitFontSmoothing: 'antialiased',
            MozOsxFontSmoothing: 'grayscale',
            ...(isContrastActive ? {
              color: isDarkContrast ? '#ffffff' : '#000000',
              backgroundImage: 'none',
            } : {}),
            ...(preferences.reduceMotion ? {
              scrollBehavior: 'auto',
            } : {}),
          },
          'html[data-motion="reduced"] *, html[data-motion="reduced"] *::before, html[data-motion="reduced"] *::after': {
            animationDuration: '0.001ms !important',
            animationIterationCount: '1 !important',
            transitionDuration: '0.001ms !important',
            scrollBehavior: 'auto !important',
          },
          'html[data-spacing-mode="low"] body': {
            letterSpacing: '0.008em',
          },
          'html[data-spacing-mode="medium"] body': {
            letterSpacing: '0.02em',
          },
          'html[data-spacing-mode="high"] body': {
            letterSpacing: '0.06em',
          },
          'html[data-spacing-mode="normal"] body': {
            letterSpacing: 'normal',
          },
          'html[data-font-style="readable"] body': {
            letterSpacing: '0.01em',
          },
          'html[data-font-style="dyslexia"] body': {
            letterSpacing: '0.04em',
            wordSpacing: '0.08em',
          },
          'html[data-font-style="dyslexia"] .MuiTypography-root, html[data-font-style="dyslexia"] .MuiButton-root, html[data-font-style="dyslexia"] .MuiInputBase-input, html[data-font-style="dyslexia"] .MuiMenuItem-root': {
            letterSpacing: '0.04em',
            wordSpacing: '0.08em',
          },
          'html[data-saturation="low"] #root': {
            filter: 'saturate(85%)',
          },
          'html[data-saturation="normal"] #root': {
            filter: 'saturate(100%)',
          },
          'html[data-saturation="high"] #root': {
            filter: 'saturate(130%)',
          },
          'html[data-links="underlined"] a, html[data-links="underlined"] .MuiLink-root': {
            textDecoration: 'underline !important',
            textUnderlineOffset: '0.14em',
          },
          'html[data-contrast-mode="invertido"] body': {
            filter: 'invert(1) hue-rotate(180deg) saturate(var(--app-saturation, 100%))',
          },
          'html[data-contrast-mode="oscuro"] body': {
            filter: 'saturate(var(--app-saturation, 100%))',
          },
          'html[data-contrast-mode="luz-alta"] body': {
            filter: 'saturate(var(--app-saturation, 100%)) contrast(1.12) brightness(1.03)',
          },
          'html[data-contrast-mode="normal"] body': {
            filter: 'saturate(var(--app-saturation, 100%))',
          },
          'html[data-contrast="high"] .MuiPaper-root, html[data-contrast="high"] .MuiCard-root, html[data-contrast="high"] .MuiDrawer-paper': {
            borderWidth: '2px !important',
          },
          '*': {
            boxSizing: 'border-box',
          },
          '*::-webkit-scrollbar': {
            width: 10,
            height: 10,
          },
          '*::-webkit-scrollbar-thumb': {
            background: 'rgba(100, 116, 139, 0.45)',
            borderRadius: 999,
            border: '2px solid transparent',
            backgroundClip: 'content-box',
          },
          '*::-webkit-scrollbar-track': {
            background: 'transparent',
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundImage: isContrastActive
              ? 'none'
              : themeMode === 'dark'
              ? 'linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(30, 41, 59, 0.92))'
              : 'linear-gradient(135deg, rgba(255,255,255,0.92), rgba(248,250,252,0.92))',
            backdropFilter: 'blur(18px)',
            borderBottom: themeMode === 'dark'
              ? '1px solid rgba(148, 163, 184, 0.16)'
              : '1px solid rgba(148, 163, 184, 0.18)',
            boxShadow: '0 12px 36px rgba(15, 23, 42, 0.10)',
            color: isContrastActive ? (isDarkContrast ? '#ffffff' : '#000000') : (themeMode === 'dark' ? '#e2e8f0' : '#0f172a'),
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            backgroundImage: 'none',
          },
          rounded: {
            borderRadius: 18,
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 20,
            border: isContrastActive
              ? (isDarkContrast ? '2px solid rgba(255,255,255,0.55)' : '2px solid rgba(0,0,0,0.55)')
              : (themeMode === 'dark'
                ? '1px solid rgba(148, 163, 184, 0.14)'
                : '1px solid rgba(148, 163, 184, 0.16)'),
            boxShadow: isContrastActive
              ? 'none'
              : (themeMode === 'dark'
                ? '0 18px 42px rgba(2, 6, 23, 0.32)'
                : '0 16px 36px rgba(15, 23, 42, 0.08)'),
          },
        },
      },
      MuiDrawer: {
        styleOverrides: {
          paper: {
            backgroundImage: isContrastActive
              ? 'none'
              : themeMode === 'dark'
              ? 'linear-gradient(180deg, rgba(15, 23, 42, 0.98), rgba(17, 24, 39, 0.98))'
              : 'linear-gradient(180deg, rgba(255,255,255,0.96), rgba(248,250,252,0.98))',
            backdropFilter: 'blur(18px)',
            borderRight: isContrastActive
              ? (isDarkContrast ? '2px solid rgba(255,255,255,0.55)' : '2px solid rgba(0,0,0,0.55)')
              : (themeMode === 'dark'
                ? '1px solid rgba(148, 163, 184, 0.14)'
                : '1px solid rgba(148, 163, 184, 0.18)'),
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 999,
            textTransform: 'none',
            fontWeight: 700,
            boxShadow: 'none',
          },
          contained: {
            boxShadow: '0 14px 30px rgba(37, 99, 235, 0.22)',
            '&:hover': {
              boxShadow: '0 18px 36px rgba(37, 99, 235, 0.28)',
            },
          },
          outlined: {
            borderColor: isContrastActive
              ? (isDarkContrast ? 'rgba(255,255,255,0.55)' : 'rgba(0,0,0,0.55)')
              : (themeMode === 'dark' ? 'rgba(148, 163, 184, 0.28)' : 'rgba(37, 99, 235, 0.18)'),
            backgroundColor: isContrastActive
              ? (isDarkContrast ? 'rgba(0,0,0,0.55)' : 'rgba(255,255,255,0.92)')
              : (themeMode === 'dark' ? 'rgba(15, 23, 42, 0.4)' : 'rgba(255,255,255,0.72)'),
            backdropFilter: 'blur(10px)',
          },
        },
      },
      MuiTextField: {
        defaultProps: {
          variant: 'outlined',
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            borderRadius: 16,
            backgroundColor: isContrastActive
              ? (isDarkContrast ? 'rgba(0,0,0,0.6)' : 'rgba(255,255,255,0.98)')
              : (themeMode === 'dark' ? 'rgba(15, 23, 42, 0.45)' : 'rgba(255, 255, 255, 0.92)'),
            backdropFilter: 'blur(10px)',
          },
          notchedOutline: {
            borderColor: isContrastActive
              ? (isDarkContrast ? 'rgba(255,255,255,0.65)' : 'rgba(0,0,0,0.65)')
              : (themeMode === 'dark' ? 'rgba(148, 163, 184, 0.24)' : 'rgba(148, 163, 184, 0.28)'),
          },
        },
      },
      MuiTableContainer: {
        styleOverrides: {
          root: {
            borderRadius: 20,
            border: isContrastActive
              ? (isDarkContrast ? '2px solid rgba(255,255,255,0.55)' : '2px solid rgba(0,0,0,0.55)')
              : (themeMode === 'dark'
                ? '1px solid rgba(148, 163, 184, 0.14)'
                : '1px solid rgba(148, 163, 184, 0.16)'),
            overflow: 'hidden',
          },
        },
      },
      MuiTableHead: {
        styleOverrides: {
          root: {
            background: preferences.theme === 'dark'
              ? 'linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(14, 116, 144, 0.95))'
              : 'linear-gradient(135deg, #2563eb, #0f766e)',
          },
        },
      },
      MuiCardContent: {
        styleOverrides: preferences.compact
          ? {
              root: {
                paddingTop: 12,
                paddingBottom: 12,
                '&:last-child': { paddingBottom: 12 },
              },
            }
          : {},
      },
      MuiListItem: {
        styleOverrides: preferences.compact
          ? {
              root: {
                paddingTop: 4,
                paddingBottom: 4,
              },
            }
          : {},
      },
      MuiTableCell: {
        styleOverrides: preferences.compact
          ? {
              root: {
                paddingTop: 6,
                paddingBottom: 6,
              },
            }
          : {},
      },
    },
  });

  const value = useMemo(
    () => ({
      preferences,
      updatePreferences,
      resetAllPreferences,
    }),
    [preferences, updatePreferences, resetAllPreferences],
  );

  return (
    <UISettingsContext.Provider value={value}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </UISettingsContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useUISettings = () => {
  const context = useContext(UISettingsContext);
  if (!context) {
    throw new Error('useUISettings debe usarse dentro de UISettingsProvider');
  }
  return context;
};
