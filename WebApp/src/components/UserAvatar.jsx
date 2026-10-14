import React, { useState, useEffect } from 'react';
import { UserIcon } from './Icons';

/**
 * Robust UserAvatar component that renders the Google profile photo
 * with referrerPolicy="no-referrer" and falls back to a clean, offline,
 * zero-network brand monogram or UserIcon if the image is missing or fails to load.
 */
export default function UserAvatar({ 
  user, 
  size = 40, 
  className = '', 
  style = {}, 
  onClick, 
  title 
}) {
  const [imgFailed, setImgFailed] = useState(false);
  const photoURL = user?.photoURL;
  const displayName = user?.displayName || 'User';
  const initial = displayName.trim().charAt(0).toUpperCase();

  // Reset failure state if photoURL changes
  useEffect(() => {
    setImgFailed(false);
  }, [photoURL]);

  if (photoURL && !imgFailed) {
    return (
      <img
        src={photoURL}
        alt={displayName}
        className={className}
        referrerPolicy="no-referrer"
        onClick={onClick}
        title={title}
        onError={() => setImgFailed(true)}
        style={{
          width: `${size}px`,
          height: `${size}px`,
          borderRadius: '50%',
          objectFit: 'cover',
          flexShrink: 0,
          ...style
        }}
      />
    );
  }

  // Pure inline offline fallback: brand-colored round badge with user initial or UserIcon
  return (
    <div
      className={className}
      onClick={onClick}
      title={title}
      style={{
        width: `${size}px`,
        height: `${size}px`,
        borderRadius: '50%',
        backgroundColor: 'var(--brand-primary, #2F3A4A)',
        color: '#FFFFFF',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontWeight: 800,
        fontSize: `${Math.max(14, Math.round(size * 0.42))}px`,
        textTransform: 'uppercase',
        flexShrink: 0,
        userSelect: 'none',
        boxSizing: 'border-box',
        ...style
      }}
    >
      {initial ? initial : <UserIcon size={Math.round(size * 0.55)} fill="#FFFFFF" />}
    </div>
  );
}
