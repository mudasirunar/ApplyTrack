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
    let syncingTimeout;

    // Safety auto-dismiss if mounted while state is SYNCING
    if (state === 'SYNCING') {
      syncingTimeout = setTimeout(() => {
        setVisible(false);
        setState('IDLE');
      }, 3500);
    }

    const handleSyncState = (e) => {
      const { state: newState, message } = e.detail || {};
      setState(newState || 'IDLE');
      if (message) setErrorMsg(message);

      if (hideTimeout) clearTimeout(hideTimeout);
      if (syncingTimeout) clearTimeout(syncingTimeout);

      if (newState === 'IDLE') {
        setVisible(false);
      } else {
        setVisible(true);
      }

      // Safety timeout: Never let 'Syncing...' hang on screen for more than 3.5 seconds
      if (newState === 'SYNCING') {
        syncingTimeout = setTimeout(() => {
          setVisible(false);
          setState('IDLE');
        }, 3500);
      }

      // Auto-hide success or error messages promptly after 2 seconds
      if (newState === 'SUCCESS' || newState === 'ERROR') {
        hideTimeout = setTimeout(() => {
          setVisible(false);
        }, 2000);
      }
    };

    window.addEventListener('applytrack_sync_state', handleSyncState);
    return () => {
      window.removeEventListener('applytrack_sync_state', handleSyncState);
      if (hideTimeout) clearTimeout(hideTimeout);
      if (syncingTimeout) clearTimeout(syncingTimeout);
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
