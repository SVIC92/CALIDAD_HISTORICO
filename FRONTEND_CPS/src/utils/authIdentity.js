const decodeTokenPayload = (token) => {
  try {
    if (!token) return null;
    const [, payloadBase64] = token.split('.');
    if (!payloadBase64) return null;

    const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    return JSON.parse(window.atob(padded));
  } catch {
    return null;
  }
};

const formatFromEmail = (email) => {
  if (!email || !email.includes('@')) return '';
  const local = email.split('@')[0].replace(/[._-]+/g, ' ').trim();
  if (!local) return '';
  return local
    .split(' ')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
};

export const getDisplayNameFromToken = (token) => {
  const payload = decodeTokenPayload(token);
  if (!payload) return '';

  const candidate = payload?.nombre
    || payload?.name
    || payload?.fullName
    || payload?.given_name
    || payload?.preferred_username
    || payload?.username
    || '';

  if (candidate && !String(candidate).includes('@')) {
    return String(candidate);
  }

  const byEmail = formatFromEmail(payload?.email || payload?.sub || '');
  return byEmail || '';
};

export const getRoleFromToken = (token) => {
  const payload = decodeTokenPayload(token);
  if (!payload) return '';

  const roles = payload?.roles || payload?.authorities || payload?.role || payload?.rol;
  if (Array.isArray(roles) && roles.length > 0) return String(roles[0]);
  return roles ? String(roles) : '';
};
