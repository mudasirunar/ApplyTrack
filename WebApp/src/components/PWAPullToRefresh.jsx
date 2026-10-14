import React, { useState, useEffect, useRef } from 'react';
import './PWAPullToRefresh.css';

const PULL_THRESHOLD = 55; // Distance in px to trigger refresh
const PULL_MAX = 80;

export default function PWAPullToRefresh() {
  const [isStandalone, setIsStandalone] = useState(() => {
    if (typeof window === 'undefined') return false;
    return (
      window.navigator.standalone === true ||
      window.matchMedia('(display-mode: standalone)').matches
    );
  });

  const [pullDistance, setPullDistance] = useState(0);
  const [isRefreshing, setIsRefreshing] = useState(false);
  
  const startYRef = useRef(0);
  const startXRef = useRef(0);
  const isPullingRef = useRef(false);
  const isRefreshingRef = useRef(false);

  useEffect(() => {
    // Re-verify standalone display mode on media changes
    const mediaQuery = window.matchMedia('(display-mode: standalone)');
    const handleMediaChange = (e) => setIsStandalone(e.matches || window.navigator.standalone === true);
    
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleMediaChange);
    } else {
      mediaQuery.addListener(handleMediaChange);
    }

    return () => {
      if (mediaQuery.removeEventListener) {
        mediaQuery.removeEventListener('change', handleMediaChange);
      } else {
        mediaQuery.removeListener(handleMediaChange);
      }
    };
  }, []);

  useEffect(() => {
    // Strictly PWA-only: if not installed as standalone PWA, do nothing
    if (!isStandalone) return;

    // Apply overscroll containment in standalone mode to avoid native rubber-band fight
    const prevOverscroll = document.body.style.overscrollBehaviorY;
    document.body.style.overscrollBehaviorY = 'contain';

    const handleTouchStart = (e) => {
      if (isRefreshingRef.current) return;
      // Only initiate pull when user is at the very top of the page
      if (window.scrollY > 0 || document.documentElement.scrollTop > 0) return;

      if (e.touches && e.touches.length === 1) {
        startYRef.current = e.touches[0].clientY;
        startXRef.current = e.touches[0].clientX;
        isPullingRef.current = false;
      }
    };

    const handleTouchMove = (e) => {
      if (isRefreshingRef.current) return;
      if (!e.touches || e.touches.length !== 1) return;

      const currentY = e.touches[0].clientY;
      const currentX = e.touches[0].clientX;
      const diffY = currentY - startYRef.current;
      const diffX = Math.abs(currentX - startXRef.current);

      // Still at the very top and pulling downward more than sideways
      if ((window.scrollY <= 0 || document.documentElement.scrollTop <= 0) && diffY > 0) {
        if (diffY > diffX && diffY > 5) {
          isPullingRef.current = true;
          // Prevent browser scroll interference while pulling down
          if (e.cancelable) {
            e.preventDefault();
          }

          // Damped pull distance curve
          const distance = Math.min(diffY * 0.42, PULL_MAX);
          setPullDistance(distance);
        }
      } else if (isPullingRef.current && diffY <= 0) {
        isPullingRef.current = false;
        setPullDistance(0);
      }
    };

    const handleTouchEnd = () => {
      if (isRefreshingRef.current) return;

      if (isPullingRef.current) {
        isPullingRef.current = false;

        setPullDistance((currentDistance) => {
          if (currentDistance >= PULL_THRESHOLD) {
            isRefreshingRef.current = true;
            setIsRefreshing(true);

            // Subtle haptic tick on Android PWA if supported
            if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
              try {
                navigator.vibrate(15);
              } catch {
                // Ignore unsupported
              }
            }

            // Brief spin before triggering full web reload
            setTimeout(() => {
              window.location.reload();
            }, 380);

            return 50; // Pin spinner in view during reload
          }

          return 0; // Spring back
        });
      }
    };

    window.addEventListener('touchstart', handleTouchStart, { passive: true });
    window.addEventListener('touchmove', handleTouchMove, { passive: false });
    window.addEventListener('touchend', handleTouchEnd, { passive: true });
    window.addEventListener('touchcancel', handleTouchEnd, { passive: true });

    return () => {
      document.body.style.overscrollBehaviorY = prevOverscroll;
      window.removeEventListener('touchstart', handleTouchStart);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('touchend', handleTouchEnd);
      window.removeEventListener('touchcancel', handleTouchEnd);
    };
  }, [isStandalone]);

  // If not standalone PWA mode, render nothing
  if (!isStandalone) return null;

  const isActive = pullDistance > 0 || isRefreshing;
  const isReady = pullDistance >= PULL_THRESHOLD;
  const translateY = isRefreshing ? 48 : Math.max(pullDistance * 0.9 - 42, -50);
  const rotation = isRefreshing ? 0 : Math.min(pullDistance * 6, 360);

  return (
    <div
      className={`pwa-pull-indicator ${isActive ? 'active' : ''} ${isRefreshing ? 'refreshing' : ''}`}
      style={{
        transform: `translateX(-50%) translateY(${translateY}px)`
      }}
      aria-hidden="true"
    >
      <div
        className={`pwa-pull-spinner ${isRefreshing ? 'spinning' : ''} ${isReady ? 'ready' : ''}`}
        style={{
          transform: isRefreshing ? undefined : `rotate(${rotation}deg)`
        }}
      />
    </div>
  );
}
