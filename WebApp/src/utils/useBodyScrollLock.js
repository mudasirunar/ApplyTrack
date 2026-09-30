import { useEffect } from 'react';

// Global counter to handle multiple / stacked modals safely
let activeModalCount = 0;
let previousBodyOverflow = '';
let previousHtmlOverflow = '';

/**
 * Universal hook to lock background scrolling across desktop and mobile devices.
 * Handles nested/stacked modals safely using a global reference counter.
 */
export function useBodyScrollLock(isLocked = true) {
  useEffect(() => {
    if (!isLocked) return;

    if (activeModalCount === 0) {
      previousBodyOverflow = document.body.style.overflow;
      previousHtmlOverflow = document.documentElement.style.overflow;
      document.body.style.overflow = 'hidden';
      document.documentElement.style.overflow = 'hidden';
    }
    activeModalCount++;

    return () => {
      activeModalCount = Math.max(0, activeModalCount - 1);
      if (activeModalCount === 0) {
        document.body.style.overflow = previousBodyOverflow;
        document.documentElement.style.overflow = previousHtmlOverflow;
      }
    };
  }, [isLocked]);
}
