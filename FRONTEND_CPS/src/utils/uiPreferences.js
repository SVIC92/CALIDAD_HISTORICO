const STORAGE_KEY = 'uiPreferences';

const DEFAULT_PREFERENCES = {
  theme: 'light', // light | dark
  compact: false,
  fontScale: 100, // 100 - 140
  saturationMode: 'normal', // low | normal | high
  contrastMode: 'normal', // invertido | oscuro | luz-alta | normal
  reduceMotion: false,
  spacingMode: 'normal', // low | medium | high | normal
  readableFontMode: 'normal', // normal | readable | dyslexia
  readingAidMode: 'normal', // normal | cursor | mask | guide
  hideImages: false,
  textAlignMode: 'normal', // left | center | right | justify | normal
  lineHeightMode: 'normal', // low | medium | high | normal
  underlineLinks: false,
};

const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
const normalizeSaturationMode = (value) => {
  if (value === 'low' || value === 'high' || value === 'normal') return value;
  return DEFAULT_PREFERENCES.saturationMode;
};

const normalizeContrastMode = (value) => {
  if (value === 'invertido' || value === 'oscuro' || value === 'luz-alta' || value === 'normal') return value;
  return DEFAULT_PREFERENCES.contrastMode;
};

const normalizeSpacingMode = (value) => {
  if (value === 'low' || value === 'medium' || value === 'high' || value === 'normal') return value;
  return DEFAULT_PREFERENCES.spacingMode;
};

const normalizeReadableFontMode = (value) => {
  if (value === 'normal' || value === 'readable' || value === 'dyslexia') return value;
  return DEFAULT_PREFERENCES.readableFontMode;
};

const normalizeReadingAidMode = (value) => {
  if (value === 'normal' || value === 'cursor' || value === 'mask' || value === 'guide') return value;
  return DEFAULT_PREFERENCES.readingAidMode;
};

const normalizeTextAlignMode = (value) => {
  if (value === 'left' || value === 'center' || value === 'right' || value === 'justify' || value === 'normal') return value;
  return DEFAULT_PREFERENCES.textAlignMode;
};

const normalizeLineHeightMode = (value) => {
  if (value === 'low' || value === 'medium' || value === 'high' || value === 'normal') return value;
  return DEFAULT_PREFERENCES.lineHeightMode;
};

const getSaturationValue = (mode) => {
  if (mode === 'low') return 85;
  if (mode === 'high') return 130;
  return 100;
};

export const getDefaultPreferences = () => ({ ...DEFAULT_PREFERENCES });

export const loadPreferences = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return getDefaultPreferences();

    const parsed = JSON.parse(raw);
    return {
      theme: parsed?.theme === 'dark' ? 'dark' : 'light',
      compact: Boolean(parsed?.compact),
      fontScale: clamp(Number(parsed?.fontScale) || DEFAULT_PREFERENCES.fontScale, 100, 140),
      saturationMode: normalizeSaturationMode(parsed?.saturationMode),
      contrastMode: normalizeContrastMode(parsed?.contrastMode || (parsed?.highContrast ? 'oscuro' : 'normal')),
      reduceMotion: Boolean(parsed?.reduceMotion),
      spacingMode: normalizeSpacingMode(parsed?.spacingMode || (parsed?.wideSpacing ? 'medium' : 'normal')),
      readableFontMode: normalizeReadableFontMode(parsed?.readableFontMode || (parsed?.readableFont ? 'readable' : 'normal')),
      readingAidMode: normalizeReadingAidMode(parsed?.readingAidMode || (parsed?.readingAid ? 'cursor' : 'normal')),
      hideImages: Boolean(parsed?.hideImages),
      textAlignMode: normalizeTextAlignMode(parsed?.textAlignMode || 'normal'),
      lineHeightMode: normalizeLineHeightMode(parsed?.lineHeightMode || 'normal'),
      underlineLinks: Boolean(parsed?.underlineLinks),
    };
  } catch {
    return getDefaultPreferences();
  }
};

export const savePreferences = (preferences) => {
  const normalized = {
    theme: preferences?.theme === 'dark' ? 'dark' : 'light',
    compact: Boolean(preferences?.compact),
    fontScale: clamp(Number(preferences?.fontScale) || DEFAULT_PREFERENCES.fontScale, 100, 140),
    saturationMode: normalizeSaturationMode(preferences?.saturationMode),
    contrastMode: normalizeContrastMode(preferences?.contrastMode),
    reduceMotion: Boolean(preferences?.reduceMotion),
    spacingMode: normalizeSpacingMode(preferences?.spacingMode),
    readableFontMode: normalizeReadableFontMode(preferences?.readableFontMode),
    readingAidMode: normalizeReadingAidMode(preferences?.readingAidMode),
    hideImages: Boolean(preferences?.hideImages),
    textAlignMode: normalizeTextAlignMode(preferences?.textAlignMode),
    lineHeightMode: normalizeLineHeightMode(preferences?.lineHeightMode),
    underlineLinks: Boolean(preferences?.underlineLinks),
  };

  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
  return normalized;
};

export const applyPreferences = (preferences) => {
  const normalized = savePreferences(preferences);

  const root = document.documentElement;
  const body = document.body;
  const saturationValue = getSaturationValue(normalized.saturationMode);
  const isContrastActive = normalized.contrastMode !== 'normal';

  root.setAttribute('data-theme', normalized.theme);
  root.setAttribute('data-saturation', normalized.saturationMode);
  root.setAttribute('data-contrast-mode', normalized.contrastMode);
  root.setAttribute('data-contrast', isContrastActive ? 'high' : 'normal');
  root.setAttribute('data-motion', normalized.reduceMotion ? 'reduced' : 'normal');
  root.setAttribute('data-spacing', normalized.spacingMode === 'normal' ? 'normal' : 'wide');
  root.setAttribute('data-spacing-mode', normalized.spacingMode);
  root.setAttribute('data-font-style', normalized.readableFontMode);
  root.setAttribute('data-reading-aid-mode', normalized.readingAidMode);
  root.setAttribute('data-images', normalized.hideImages ? 'hidden' : 'visible');
  root.setAttribute('data-text-align-mode', normalized.textAlignMode);
  root.setAttribute('data-line-height-mode', normalized.lineHeightMode);
  root.setAttribute('data-links', normalized.underlineLinks ? 'underlined' : 'default');
  body.style.setProperty('--app-saturation', `${saturationValue}%`);
  root.style.fontSize = `${normalized.fontScale}%`;

  body.classList.toggle('compact-mode', normalized.compact);

  return normalized;
};

export const resetPreferences = () => {
  const defaults = getDefaultPreferences();
  return applyPreferences(defaults);
};
