// src/components/ModalForm.jsx
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, Box, IconButton, Typography } from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import { useForm } from 'react-hook-form';
import { useEffect } from 'react';

const ModalForm = ({ open, handleClose, title, fields, onSubmit, defaultValues }) => {
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: defaultValues || {}
  });

  // Efecto para limpiar o llenar el formulario cuando cambian los datos (Ej: al editar)
  useEffect(() => {
    reset(defaultValues || {});
  }, [defaultValues, reset]);

  const submitHandler = async (data) => {
    await onSubmit(data);
    reset(); // Limpia el formulario después de guardar con éxito
    handleClose();
  };

  const handleCancel = () => {
    reset();
    handleClose();
  };

  return (
    <Dialog open={open} onClose={handleCancel} fullWidth maxWidth="sm" PaperProps={{ sx: { borderRadius: 4, boxShadow: '0 28px 70px rgba(15, 23, 42, 0.20)' } }}>
      <DialogTitle sx={{ pb: 1.25, pr: 6 }}>
        <Typography variant="h6" component="div" sx={{ fontWeight: 800 }}>
          {title}
        </Typography>
        <IconButton onClick={handleCancel} sx={{ position: 'absolute', right: 12, top: 12 }}>
          <CloseRoundedIcon />
        </IconButton>
      </DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submitHandler)}>
        <DialogContent dividers>
          {fields.map((field) => (
            <TextField
              key={field.name}
              fullWidth
              margin="normal"
              label={field.label}
              type={field.type || 'text'}
              multiline={field.multiline || false}
              rows={field.rows || 1}
              {...register(field.name, field.rules)}
              error={!!errors[field.name]}
              helperText={errors[field.name]?.message}
              sx={{ mb: 0.5 }}
            />
          ))}
        </DialogContent>
        <DialogActions sx={{ p: 2, gap: 1 }}>
          <Button onClick={handleCancel} variant="outlined">
            Cancelar
          </Button>
          <Button type="submit" variant="contained" color="primary">
            Guardar
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
};

export default ModalForm;