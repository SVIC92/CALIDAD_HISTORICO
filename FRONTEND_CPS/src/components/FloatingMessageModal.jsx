import {
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Alert,
  Box,
  Typography,
} from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';

const FloatingMessageModal = ({
  open,
  severity = 'info',
  title,
  message,
  onClose,
}) => {
  const resolvedTitle = title || (severity === 'error'
    ? 'Error'
    : severity === 'success'
      ? 'Éxito'
      : severity === 'warning'
        ? 'Advertencia'
        : 'Información');

  return (
    <Dialog
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: {
          position: 'fixed',
          right: 24,
          bottom: 24,
          m: 0,
          width: 'min(440px, calc(100vw - 32px))',
          borderRadius: 3,
          boxShadow: '0 24px 60px rgba(15, 23, 42, 0.24)',
          overflow: 'hidden',
        },
      }}
    >
      <DialogTitle sx={{ pr: 6, pb: 1 }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 800 }}>
          {resolvedTitle}
        </Typography>
        <IconButton onClick={onClose} sx={{ position: 'absolute', right: 12, top: 12 }}>
          <CloseRoundedIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent sx={{ pt: 0, pb: 2 }}>
        <Box>
          <Alert severity={severity} variant="outlined" sx={{ alignItems: 'center' }}>
            {message}
          </Alert>
        </Box>
      </DialogContent>
    </Dialog>
  );
};

export default FloatingMessageModal;