const STORAGE_KEY = 'uiPreferences';

const DEFAULT_PREFERENCES = {
  theme: 'light', // light | dark
  compact: false,
  fontScale: 100, // 90 - 120
};

const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

export const getDefaultPreferences = () => ({ ...DEFAULT_PREFERENCES });

export const loadPreferences = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return getDefaultPreferences();

    const parsed = JSON.parse(raw);
    return {
      theme: parsed?.theme === 'dark' ? 'dark' : 'light',
      compact: Boolean(parsed?.compact),
      fontScale: clamp(Number(parsed?.fontScale) || DEFAULT_PREFERENCES.fontScale, 90, 120),
    };
  } catch {
    return getDefaultPreferences();
  }
};

export const savePreferences = (preferences) => {
  const normalized = {
    theme: preferences?.theme === 'dark' ? 'dark' : 'light',
    compact: Boolean(preferences?.compact),
    fontScale: clamp(Number(preferences?.fontScale) || DEFAULT_PREFERENCES.fontScale, 90, 120),
  };

  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
  return normalized;
};

export const applyPreferences = (preferences) => {
  const normalized = savePreferences(preferences);

  const root = document.documentElement;
  const body = document.body;

  root.setAttribute('data-theme', normalized.theme);
  root.style.fontSize = `${normalized.fontScale}%`;

  body.classList.toggle('compact-mode', normalized.compact);

  return normalized;
};

export const resetPreferences = () => {
  const defaults = getDefaultPreferences();
  return applyPreferences(defaults);
};
