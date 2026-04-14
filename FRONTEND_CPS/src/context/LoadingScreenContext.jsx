import { Backdrop, CircularProgress, Typography } from '@mui/material';
import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const LoadingScreenContext = createContext(null);

export const LoadingScreenProvider = ({ children }) => {
  const [pendingCount, setPendingCount] = useState(0);

  const startLoading = useCallback(() => {
    setPendingCount((prev) => prev + 1);
  }, []);

  const stopLoading = useCallback(() => {
    setPendingCount((prev) => Math.max(0, prev - 1));
  }, []);

  const value = useMemo(
    () => ({
      startLoading,
      stopLoading,
      isLoading: pendingCount > 0,
    }),
    [pendingCount, startLoading, stopLoading],
  );

  return (
    <LoadingScreenContext.Provider value={value}>
      {children}
      <Backdrop
        open={pendingCount > 0}
        sx={{
          zIndex: (theme) => theme.zIndex.drawer + 2000,
          color: '#fff',
          flexDirection: 'column',
          gap: 2,
          backdropFilter: 'blur(2px)',
          backgroundColor: 'rgba(0, 0, 0, 0.45)',
        }}
      >
        <CircularProgress color="inherit" />
        <Typography variant="h6" sx={{ fontWeight: 600 }}>
          Cargando...
        </Typography>
      </Backdrop>
    </LoadingScreenContext.Provider>
  );
};

export const useLoadingScreen = () => {
  const context = useContext(LoadingScreenContext);
  if (!context) {
    throw new Error('useLoadingScreen debe usarse dentro de LoadingScreenProvider');
  }
  return context;
};
