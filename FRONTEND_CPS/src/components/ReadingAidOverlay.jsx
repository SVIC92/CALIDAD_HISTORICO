import { useEffect, useMemo, useRef, useState } from 'react';
import { useUISettings } from '../context/UISettingsContext';

const CURSOR_SIZE = 64;
const MASK_HEIGHT = 96;
const GUIDE_HEIGHT = 52;
const GUIDE_WIDTH = 420;
const GUIDE_PADDING = 40;

const getInitialPoint = () => ({
  x: typeof window !== 'undefined' ? window.innerWidth / 2 : 0,
  y: typeof window !== 'undefined' ? window.innerHeight / 2 : 0,
});

const ReadingAidOverlay = () => {
  const { preferences } = useUISettings();
  const mode = preferences.readingAidMode || 'normal';
  const [point, setPoint] = useState(getInitialPoint);
  const frameRef = useRef(0);

  const isCursorMode = mode === 'cursor';
  const isMaskMode = mode === 'mask';
  const isGuideMode = mode === 'guide';

  useEffect(() => {
    if (mode === 'normal') return undefined;

    const updatePoint = (event) => {
      if (frameRef.current) return;

      frameRef.current = window.requestAnimationFrame(() => {
        frameRef.current = 0;
        setPoint({ x: event.clientX, y: event.clientY });
      });
    };

    const handleLeave = () => {
      setPoint(getInitialPoint());
    };

    window.addEventListener('pointermove', updatePoint, { passive: true });
    window.addEventListener('pointerleave', handleLeave);
    window.addEventListener('blur', handleLeave);

    return () => {
      window.removeEventListener('pointermove', updatePoint);
      window.removeEventListener('pointerleave', handleLeave);
      window.removeEventListener('blur', handleLeave);

      if (frameRef.current) {
        window.cancelAnimationFrame(frameRef.current);
        frameRef.current = 0;
      }
    };
  }, [mode]);

  const maskStyle = useMemo(() => ({
    top: Math.max(8, point.y - MASK_HEIGHT / 2),
  }), [point.y]);

  const guideStyle = useMemo(() => ({
    left: Math.min(Math.max(point.x, GUIDE_PADDING + GUIDE_WIDTH / 2), (typeof window !== 'undefined' ? window.innerWidth - GUIDE_PADDING - GUIDE_WIDTH / 2 : point.x)),
    top: Math.max(8, point.y - GUIDE_HEIGHT / 2),
  }), [point.y]);

  if (mode === 'normal') return null;

  return (
    <div
      aria-hidden="true"
      style={{
        position: 'fixed',
        inset: 0,
        pointerEvents: 'none',
        zIndex: 5000,
      }}
    >
      {isCursorMode ? (
        <div
          style={{
            position: 'fixed',
            left: 0,
            top: 0,
            width: CURSOR_SIZE,
            height: CURSOR_SIZE,
            transform: `translate3d(${point.x}px, ${point.y}px, 0) translate(-6px, -4px)`,
            filter: 'drop-shadow(0 4px 10px rgba(15, 23, 42, 0.35))',
          }}
        >
          <svg viewBox="0 0 64 64" width="100%" height="100%" aria-hidden="true">
            <path
              d="M14 10L14 50L25 38L31 56L37 54L31 36L49 36Z"
              fill="#0f172a"
              stroke="#ffffff"
              strokeWidth="2.5"
              strokeLinejoin="round"
            />
          </svg>
        </div>
      ) : null}

      {isMaskMode ? (
        <>
          <div
            style={{
              position: 'fixed',
              left: 0,
              right: 0,
              top: 0,
              height: maskStyle.top,
              background: 'rgba(3, 7, 18, 0.64)',
            }}
          />
          <div
            style={{
              position: 'fixed',
              left: 0,
              right: 0,
              top: maskStyle.top,
              height: MASK_HEIGHT,
              background: 'rgba(255, 255, 255, 0.08)',
              borderTop: '2px solid rgba(255, 255, 255, 0.55)',
              borderBottom: '2px solid rgba(255, 255, 255, 0.55)',
              boxShadow: '0 8px 18px rgba(15, 23, 42, 0.16)',
            }}
          />
          <div
            style={{
              position: 'fixed',
              left: 0,
              right: 0,
              top: maskStyle.top + MASK_HEIGHT,
              bottom: 0,
              background: 'rgba(3, 7, 18, 0.64)',
            }}
          />
        </>
      ) : null}

      {isGuideMode ? (
        <div
          style={{
            position: 'fixed',
            left: guideStyle.left,
            top: guideStyle.top,
            height: GUIDE_HEIGHT,
            width: `min(${GUIDE_WIDTH}px, calc(100vw - ${GUIDE_PADDING * 2}px))`,
            transform: 'translateX(-50%)',
            background: 'rgba(59, 130, 246, 0.94)',
            border: '2px solid #0b3d91',
            borderRadius: 12,
            boxShadow: '0 8px 18px rgba(15, 23, 42, 0.24), inset 0 0 0 1px rgba(255, 255, 255, 0.28)',
            backdropFilter: 'none',
            overflow: 'visible',
          }}
        >
          <div
            style={{
              position: 'absolute',
              left: '50%',
              top: -20,
              transform: 'translateX(-50%)',
              width: 0,
              height: 0,
              borderLeft: '16px solid transparent',
              borderRight: '16px solid transparent',
              borderBottom: '16px solid #0b3d91',
            }}
          />
          <div
            style={{
              position: 'absolute',
              left: '50%',
              top: -14,
              transform: 'translateX(-50%)',
              width: 0,
              height: 0,
              borderLeft: '12px solid transparent',
              borderRight: '12px solid transparent',
              borderBottom: '12px solid rgba(59, 130, 246, 0.98)',
            }}
          />
        </div>
      ) : null}
    </div>
  );
};

export default ReadingAidOverlay;