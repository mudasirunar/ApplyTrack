import React, { useEffect } from 'react';
import { AppIcon } from '../components/Icons';
import './NotFound.css';

export default function NotFound({ setActiveTab, user }) {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' });
    document.title = '404: Page Not Found | ApplyTrack';
    
    const robotsMeta = document.querySelector('meta[name="robots"]');
    if (robotsMeta) {
      robotsMeta.setAttribute('content', 'noindex, nofollow');
    }

    return () => {
      if (robotsMeta) {
        robotsMeta.setAttribute('content', 'index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1');
      }
    };
  }, []);

  const handlePrimaryAction = () => {
    if (user) {
      if (setActiveTab) setActiveTab('dashboard');
      else window.location.href = '/dashboard';
    } else {
      if (setActiveTab) setActiveTab('landing');
      else window.location.href = '/';
    }
  };

  const handleSecondaryAction = () => {
    if (user) {
      if (setActiveTab) setActiveTab('applications');
      else window.location.href = '/applications';
    } else {
      if (setActiveTab) setActiveTab('login');
      else window.location.href = '/login';
    }
  };

  return (
    <div className="not-found-page animate-fade-in">
      <header className="not-found-header">
        <div 
          className="not-found-brand" 
          onClick={handlePrimaryAction}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handlePrimaryAction()}
        >
          <AppIcon className="not-found-logo" />
          <span className="not-found-brand-name">ApplyTrack</span>
        </div>
      </header>

      <main className="not-found-main">
        <div className="not-found-card">
          <div className="not-found-badge">404</div>
          <h1 className="not-found-title">Page Not Found</h1>
          <p className="not-found-description">
            The page you are looking for doesn't exist, has been removed, or the link might be broken.
          </p>

          <div className="not-found-actions">
            <button 
              type="button" 
              onClick={handlePrimaryAction} 
              className="not-found-btn not-found-btn-primary"
            >
              {user ? 'Return to Dashboard' : 'Back to Home'}
            </button>
            <button 
              type="button" 
              onClick={handleSecondaryAction} 
              className="not-found-btn not-found-btn-secondary"
            >
              {user ? 'View Applications' : 'Sign In'}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
