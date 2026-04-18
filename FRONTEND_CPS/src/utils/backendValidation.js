export const extractBackendValidationMessage = (error, fallback = 'No se pudo completar la operación.') => {
  const body = error?.response?.data;

  if (typeof body === 'string' && body.trim()) {
    return body;
  }

  if (body && typeof body === 'object') {
    if (typeof body.error === 'string' && body.error.trim()) {
      return body.error;
    }

    if (typeof body.mensaje === 'string' && body.mensaje.trim()) {
      return body.mensaje;
    }

    if (Array.isArray(body.errors) && body.errors.length > 0) {
      const first = body.errors[0];
      if (typeof first === 'string' && first.trim()) {
        return first;
      }
      if (first && typeof first.message === 'string' && first.message.trim()) {
        return first.message;
      }
    }
  }

  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message;
  }

  return fallback;
};
