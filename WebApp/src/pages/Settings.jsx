import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { db } from '../utils/db';
import { LogoutIcon } from '../components/Icons';
import packageJson from '../../package.json';
import './Settings.css';

function ConfirmationModal({ title, message, confirmLabel, isDestructive, onConfirm, onCancel }) {
  const modalContent = (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-content-card" style={{ maxWidth: '400px' }} onClick={(e) => e.stopPropagation()}>
        <h3 className="modal-title" style={{ margin: 0, color: isDestructive ? 'var(--error-red)' : 'var(--brand-primary)' }}>
          {title}
        </h3>
        <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', margin: '8px 0' }}></div>
        <p className="modal-text" style={{ fontSize: '0.85rem', lineHeight: '1.6', color: 'var(--text-primary)', margin: '12px 0 20px' }}>
          {message}
        </p>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <button onClick={onCancel} className="btn-secondary" style={{ padding: '8px 16px', fontSize: '0.85rem' }}>
            Cancel
          </button>
          <button 
            onClick={onConfirm} 
            className="btn-primary" 
            style={{ 
              padding: '8px 16px', 
              fontSize: '0.85rem', 
              backgroundColor: isDestructive ? 'var(--error-red)' : undefined,
              borderColor: isDestructive ? 'var(--error-red)' : undefined,
              color: '#FFFFFF'
            }}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
}

export default function Settings() {
  const [user, setUser] = useState(db.getCurrentUser());
  const [activeTheme, setActiveTheme] = useState(db.getTheme());
  const [appsCount, setAppsCount] = useState(db.getApplications().length);

  const [showWipeModal, setShowWipeModal] = useState(false);
  const [showSignOutModal, setShowSignOutModal] = useState(false);

  const appVersion = packageJson.version;

  useEffect(() => {
    const handleAuthChange = () => {
      setUser(db.getCurrentUser());
    };
    const handleDataChange = () => {
      setAppsCount(db.getApplications().length);
    };

    window.addEventListener('applytrack_auth_change', handleAuthChange);
    window.addEventListener('applytrack_data_change', handleDataChange);

    return () => {
      window.removeEventListener('applytrack_auth_change', handleAuthChange);
      window.removeEventListener('applytrack_data_change', handleDataChange);
    };
  }, []);

  const handleThemeChange = (theme) => {
    db.setTheme(theme);
    setActiveTheme(theme);
  };

  const handleImportClick = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const result = db.importData(event.target.result);
      if (result.success) {
        window.dispatchEvent(new CustomEvent('applytrack_toast', {
          detail: { message: `Import successful! Restored ${result.count} job applications.` }
        }));
      } else {
        window.dispatchEvent(new CustomEvent('applytrack_toast', {
          detail: { message: `Import failed: ${result.error}` }
        }));
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  return (
    <div className="content-container animate-fade-in">
      <div className="settings-container">
        <h2 className="dashboard-greeting" style={{ marginBottom: '8px' }}>Settings</h2>

        {/* 1. Account Settings Card */}
        <div className="card-base settings-card">
          <h3 className="section-title" style={{ fontWeight: 800, color: 'var(--brand-primary)', marginBottom: '12px' }}>Account</h3>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '16px' }}></div>
          
          <div className="profile-settings-content" style={{ marginBottom: '16px' }}>
            <img 
              src={user?.photoURL} 
              alt={user?.displayName} 
              className="profile-avatar-large"
              onError={(e) => {
                e.target.src = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y';
              }}
            />
            <div className="profile-details-large">
              <span className="profile-name-large">{user?.displayName}</span>
              <span className="profile-email-large">{user?.email}</span>
            </div>
          </div>

          <button 
            onClick={() => setShowSignOutModal(true)} 
            className="btn-primary"
            style={{ width: '100%', height: '44px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', backgroundColor: 'var(--error-red)', borderColor: 'var(--error-red)', color: '#FFFFFF' }}
          >
            <LogoutIcon style={{ width: '20px', height: '20px', fill: 'currentColor' }} />
            <span>Sign Out</span>
          </button>
        </div>

        {/* 2. Theme Settings Card */}
        <div className="card-base settings-card">
          <h3 className="section-title" style={{ fontWeight: 800, color: 'var(--brand-primary)', marginBottom: '12px' }}>Appearance</h3>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '16px' }}></div>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '16px' }}>
            Select how ApplyTrack looks on your device.
          </p>
          <div className="theme-options-grid">
            <button
              onClick={() => handleThemeChange('system')}
              className={`theme-option-btn ${activeTheme === 'system' ? 'active' : ''}`}
            >
              <span>System</span>
            </button>
            <button
              onClick={() => handleThemeChange('light')}
              className={`theme-option-btn ${activeTheme === 'light' ? 'active' : ''}`}
            >
              <span>Light</span>
            </button>
            <button
              onClick={() => handleThemeChange('dark')}
              className={`theme-option-btn ${activeTheme === 'dark' ? 'active' : ''}`}
            >
              <span>Dark</span>
            </button>
          </div>
        </div>

        {/* 3. Data Management Card */}
        <div className="card-base settings-card">
          <h3 className="section-title" style={{ fontWeight: 800, color: 'var(--brand-primary)', marginBottom: '12px' }}>Backup & Data Management</h3>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '16px' }}></div>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '16px' }}>
            Export all records along with attachments into a single ZIP archive, or restore them.
          </p>

          <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
            <button 
              onClick={() => db.exportData()} 
              className="btn-secondary" 
              style={{ flex: 1, padding: '10px', fontSize: '0.85rem', fontWeight: 700 }}
              disabled={appsCount === 0}
            >
              Export Backup
            </button>

            <label 
              className="btn-secondary" 
              style={{ 
                flex: 1, 
                padding: '10px', 
                fontSize: '0.85rem', 
                fontWeight: 700, 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                cursor: appsCount === 0 ? 'not-allowed' : 'pointer', 
                margin: 0,
                opacity: appsCount === 0 ? 0.5 : 1
              }}
            >
              <span>Import Backup</span>
              <input 
                type="file" 
                accept=".json" 
                onChange={handleImportClick} 
                style={{ display: 'none' }} 
                disabled={appsCount === 0}
              />
            </label>
          </div>

          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '16px' }}></div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '16px' }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 800, fontSize: '0.85rem', color: 'var(--error-red)', marginBottom: '2px' }}>Wipe All Records</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Clears all job applications and attachments locally & remotely.</div>
            </div>
            <button 
              onClick={() => setShowWipeModal(true)} 
              className="btn-primary" 
              style={{ backgroundColor: 'var(--error-red)', borderColor: 'var(--error-red)', padding: '8px 16px', fontSize: '0.85rem', flexShrink: 0, color: '#FFFFFF' }}
              disabled={appsCount === 0}
            >
              Wipe All
            </button>
          </div>
        </div>

        {/* 4. About Application Card */}
        <div className="card-base settings-card">
          <h3 className="section-title" style={{ fontWeight: 800, color: 'var(--brand-primary)', marginBottom: '12px' }}>About Application</h3>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '16px' }}></div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '16px' }}>
            <div 
              style={{ 
                width: '48px', 
                height: '48px', 
                borderRadius: '50%', 
                overflow: 'hidden', 
                backgroundColor: 'rgba(47, 58, 74, 0.08)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}
            >
              <img 
                src="/app_icon.png" 
                alt="App Logo" 
                style={{ width: '36px', height: '36px', objectFit: 'contain' }}
              />
            </div>
            <div>
              <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--brand-primary)' }}>ApplyTrack</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Version {appVersion}</div>
            </div>
          </div>
          
          <p style={{ fontSize: '0.85rem', lineHeight: '1.6', color: 'var(--text-secondary)', margin: 0 }}>
            ApplyTrack is a web dashboard designed to track job applications, manage resumes, and monitor application metrics.
          </p>
        </div>

      </div>

      {/* CUSTOM CONFIRMATION MODALS (No browser specific dialog popups) */}
      {showSignOutModal && (
        <ConfirmationModal 
          title="Sign Out"
          message="Are you sure you want to sign out of your Google account?"
          confirmLabel="Sign Out"
          isDestructive={true}
          onConfirm={() => {
            db.logout();
            setUser(null);
            setShowSignOutModal(false);
          }}
          onCancel={() => setShowSignOutModal(false)}
        />
      )}

      {showWipeModal && (
        <ConfirmationModal 
          title="Wipe All Records"
          message="WARNING: This will permanently delete all your job applications and attached documents. This action cannot be undone. Are you sure you want to proceed?"
          confirmLabel="Wipe All"
          isDestructive={true}
          onConfirm={() => {
            db.resetDatabase();
            setAppsCount(db.getApplications().length);
            setShowWipeModal(false);
            window.dispatchEvent(new CustomEvent('applytrack_toast', {
              detail: { message: 'Database reset to default mock applications successfully.' }
            }));
          }}
          onCancel={() => setShowWipeModal(false)}
        />
      )}

    </div>
  );
}
