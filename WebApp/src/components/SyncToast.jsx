import React, { useState, useEffect } from 'react';
import { db } from '../utils/db';
import './SyncToast.css';

export default function SyncToast() {
  const [visible, setVisible] = useState(() => {
    const initialState = db.getSyncState();
    return initialState !== 'IDLE';
  });
  const [state, setState] = useState(() => db.getSyncState());
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    let hideTimeout;

    const handleSyncState = (e) => {
      const { state: newState, message } = e.detail || {};
      setState(newState || 'IDLE');
      if (message) setErrorMsg(message);

      if (newState === 'IDLE') {
        setVisible(false);
      } else {
        setVisible(true);
      }

      // Auto-hide success or error messages after 2.5 seconds
      if (newState === 'SUCCESS' || newState === 'ERROR') {
        if (hideTimeout) clearTimeout(hideTimeout);
        hideTimeout = setTimeout(() => {
          setVisible(false);
        }, 2500);
      }
    };

    window.addEventListener('applytrack_sync_state', handleSyncState);
    return () => {
      window.removeEventListener('applytrack_sync_state', handleSyncState);
      if (hideTimeout) clearTimeout(hideTimeout);
    };
  }, []);

  if (!visible) return null;

  let icon = null;
  let text = '';
  let statusClass = '';

  if (state === 'SYNCING') {
    icon = <div className="sync-spinner" />;
    text = 'Syncing...';
    statusClass = 'syncing';
  } else if (state === 'SUCCESS') {
    icon = <div className="sync-icon success">✓</div>;
    text = 'Sync successful';
    statusClass = 'success';
  } else if (state === 'ERROR') {
    icon = <div className="sync-icon error">✕</div>;
    text = errorMsg || 'Sync failed';
    statusClass = 'error';
  }

  return (
    <div className={`sync-toast-container animate-slide-in ${statusClass}`}>
      {icon}
      <span className="sync-toast-text">{text}</span>
    </div>
  );
}
