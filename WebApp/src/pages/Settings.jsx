import React, { useState, useEffect } from 'react';
import { db } from '../utils/db';
import { SunIcon, MoonIcon, DeviceIcon, LogoutIcon } from '../components/Icons';

export default function Settings() {
  const [user, setUser] = useState(db.getCurrentUser());
  const [activeTheme, setActiveTheme] = useState(db.getTheme());
  const [appsCount, setAppsCount] = useState(db.getApplications().length);

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

  const handleLogout = () => {
    if (window.confirm('Are you sure you want to sign out?')) {
      db.logout();
    }
  };

  const handleReset = () => {
    if (window.confirm('WARNING: This will reset all your tracked job applications to the default mock dataset. Are you sure?')) {
      db.resetDatabase();
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: { message: 'Database reset to default mock applications successfully.' }
      }));
    }
  };

  const handleExport = () => {
    db.exportData();
    window.dispatchEvent(new CustomEvent('applytrack_toast', {
      detail: { message: 'Data exported successfully. Check your downloads folder.' }
    }));
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
    // Clear input so same file can be uploaded again
    e.target.value = '';
  };

  return (
    <div className="content-container animate-fade-in">
      <div className="settings-container">
        <h2 className="dashboard-greeting" style={{ marginBottom: '8px' }}>Settings</h2>

        {/* 1. Account Settings Card */}
        {user && (
          <div className="card-base settings-card">
            <h3 className="section-title">Google Account Details</h3>
            <div className="profile-settings-content">
              <img 
                src={user.photoURL} 
                alt={user.displayName} 
                className="profile-avatar-large"
                onError={(e) => {
                  e.target.src = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y';
                }}
              />
              <div className="profile-details-large">
                <span className="profile-name-large">{user.displayName}</span>
                <span className="profile-email-large">{user.email}</span>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                  Sync Status: <strong>Local Cloud Backup Mocked</strong>
                </span>
              </div>
              <button 
                onClick={handleLogout} 
                className="backup-btn reset"
                style={{ alignSelf: 'center', display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 16px' }}
              >
                <LogoutIcon style={{ width: '16px', height: '16px' }} />
                <span>Sign Out</span>
              </button>
            </div>
          </div>
        )}

        {/* 2. Theme Settings Card */}
        <div className="card-base settings-card">
          <h3 className="section-title">Appearance Theme</h3>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '16px', marginTop: '-8px' }}>
            Choose how ApplyTrack looks on your screen. Supports Light and Dark modes.
          </p>
          <div className="theme-options-grid">
            <button
              onClick={() => handleThemeChange('light')}
              className={`theme-option-btn ${activeTheme === 'light' ? 'active' : ''}`}
            >
              <SunIcon />
              <span>Light Mode</span>
            </button>
            <button
              onClick={() => handleThemeChange('dark')}
              className={`theme-option-btn ${activeTheme === 'dark' ? 'active' : ''}`}
            >
              <MoonIcon />
              <span>Dark Mode</span>
            </button>
            <button
              onClick={() => handleThemeChange('system')}
              className={`theme-option-btn ${activeTheme === 'system' ? 'active' : ''}`}
            >
              <DeviceIcon />
              <span>System Default</span>
            </button>
          </div>
        </div>

        {/* 3. Data Management Card */}
        <div className="card-base settings-card">
          <h3 className="section-title">Data Backup & Management</h3>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '16px', marginTop: '-8px' }}>
            Export your database backup as a JSON file, import a previous backup, or reset the storage.
          </p>
          
          <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--brand-primary)', marginBottom: '16px' }}>
            Currently Stored: <span style={{ color: 'var(--link-blue)' }}>{appsCount} applications</span>
          </div>

          <div className="backup-actions-grid">
            <button onClick={handleExport} className="backup-btn">
              <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: '18px', height: '18px' }}>
                <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM17 13l-5 5-5-5h3V9h4v4h3z" />
              </svg>
              <span>Export Backup (.json)</span>
            </button>

            <label className="backup-btn" style={{ cursor: 'pointer' }}>
              <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: '18px', height: '18px' }}>
                <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z" />
              </svg>
              <span>Import Backup (.json)</span>
              <input 
                type="file" 
                accept=".json" 
                onChange={handleImportClick} 
                style={{ display: 'none' }} 
              />
            </label>

            <button onClick={handleReset} className="backup-btn reset" style={{ gridColumn: '1 / -1' }}>
              <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: '18px', height: '18px' }}>
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
              </svg>
              <span>Reset to Default Mock Database</span>
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
