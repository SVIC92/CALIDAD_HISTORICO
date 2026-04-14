import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { createTheme, CssBaseline, ThemeProvider } from '@mui/material';
import { applyPreferences, getDefaultPreferences, loadPreferences } from '../utils/uiPreferences';

const UISettingsContext = createContext(null);

export const UISettingsProvider = ({ children }) => {
  const [preferences, setPreferences] = useState(() => loadPreferences());

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

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: preferences.theme,
        },
        typography: {
          fontSize: Math.round((14 * preferences.fontScale) / 100),
        },
        spacing: preferences.compact ? 6 : 8,
        components: {
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
      }),
    [preferences],
  );

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

export const useUISettings = () => {
  const context = useContext(UISettingsContext);
  if (!context) {
    throw new Error('useUISettings debe usarse dentro de UISettingsProvider');
  }
  return context;
};
