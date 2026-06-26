import React, { useState, useEffect } from 'react';
import { db } from '../utils/db';
import { 
  AppIcon, 
  DashboardIcon, 
  ListIcon, 
  SettingsIcon, 
  LogoutIcon 
} from './Icons';

export default function MainLayout({ activeTab, setActiveTab, children }) {
  const [user, setUser] = useState(db.getCurrentUser());
  const [toast, setToast] = useState(null);

  useEffect(() => {
    const handleAuthChange = () => {
      setUser(db.getCurrentUser());
    };
    
    const handleToast = (e) => {
      setToast(e.detail);
      // Auto-dismiss after 4 seconds
      if (window.toastTimeout) clearTimeout(window.toastTimeout);
      window.toastTimeout = setTimeout(() => {
        setToast(null);
        if (e.detail.onDismiss) e.detail.onDismiss();
      }, 4000);
    };

    window.addEventListener('applytrack_auth_change', handleAuthChange);
    window.addEventListener('applytrack_toast', handleToast);

    return () => {
      window.removeEventListener('applytrack_auth_change', handleAuthChange);
      window.removeEventListener('applytrack_toast', handleToast);
      if (window.toastTimeout) clearTimeout(window.toastTimeout);
    };
  }, []);

  const handleLogout = () => {
    if (window.confirm('Are you sure you want to sign out?')) {
      db.logout();
    }
  };

  const handleToastAction = () => {
    if (toast && toast.onAction) {
      toast.onAction();
    }
    setToast(null);
    if (window.toastTimeout) clearTimeout(window.toastTimeout);
  };

  if (!user) return <>{children}</>;

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: <DashboardIcon /> },
    { id: 'applications', label: 'Applications', icon: <ListIcon /> },
    { id: 'settings', label: 'Settings', icon: <SettingsIcon /> },
  ];

  return (
    <div className="app-layout">
      {/* DESKTOP SIDEBAR */}
      <aside className="app-sidebar">
        <div className="app-sidebar-top">
          <div className="app-sidebar-logo-container">
            <AppIcon className="app-sidebar-logo" />
            <span className="app-sidebar-title">ApplyTrack</span>
          </div>
          
          <nav className="app-sidebar-nav">
            {navItems.map(item => (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`app-sidebar-link ${activeTab === item.id ? 'active' : ''}`}
              >
                {item.icon}
                <span>{item.label}</span>
              </button>
            ))}
          </nav>
        </div>

        <div className="app-sidebar-bottom">
          <div className="app-sidebar-footer">
            <img 
              src={user.photoURL || 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'} 
              alt={user.displayName} 
              className="user-avatar"
              onError={(e) => {
                e.target.src = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y';
              }}
            />
            <div className="user-info">
              <span className="user-name">{user.displayName}</span>
              <span className="user-email">{user.email}</span>
            </div>
            <button 
              onClick={handleLogout} 
              className="job-card-action-btn delete" 
              title="Sign Out"
              style={{ marginLeft: 'auto', padding: '8px' }}
            >
              <LogoutIcon style={{ width: '20px', height: '20px' }} />
            </button>
          </div>
        </div>
      </aside>

      {/* MAIN BODY */}
      <main className="app-main">
        {children}
      </main>

      {/* MOBILE BOTTOM NAVIGATION */}
      <nav className="app-mobile-nav">
        {navItems.map(item => (
          <div
            key={item.id}
            onClick={() => setActiveTab(item.id)}
            className={`mobile-nav-item ${activeTab === item.id ? 'active' : ''}`}
          >
            <div className="mobile-nav-icon-container">
              {item.icon}
            </div>
            <span className="mobile-nav-label">{item.label}</span>
          </div>
        ))}
      </nav>

      {/* GLOBAL TOAST NOTIFICATION */}
      {toast && (
        <div className="toast-host">
          <div className="toast-card animate-fade-in">
            <span className="toast-message">{toast.message}</span>
            {toast.action && (
              <button onClick={handleToastAction} className="toast-action-btn">
                {toast.action}
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
