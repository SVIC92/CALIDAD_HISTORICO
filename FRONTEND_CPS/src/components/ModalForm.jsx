// src/components/ModalForm.jsx
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, Box } from '@mui/material';
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
    <Dialog open={open} onClose={handleCancel} fullWidth maxWidth="sm">
      <DialogTitle>{title}</DialogTitle>
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
            />
          ))}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={handleCancel}>Cancelar</Button>
          <Button type="submit" variant="contained" color="primary">
            Guardar
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
};

export default ModalForm;