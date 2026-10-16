import React, { useEffect } from 'react';
import { AppIcon } from '../components/Icons';
import './NotFound.css';

export default function NotFound({ 
  setActiveTab, 
  setSelectedJobId,
  user,
  title = "Page Not Found",
  description = "The page you are looking for doesn't exist, has been removed, or the link might be broken.",
  isMissingJob = false
}) {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' });
    document.title = `${isMissingJob ? 'Application Not Found' : '404: Page Not Found'} | ApplyTrack`;
    
    const robotsMeta = document.querySelector('meta[name="robots"]');
    if (robotsMeta) {
      robotsMeta.setAttribute('content', 'noindex, nofollow');
    }

    return () => {
      if (robotsMeta) {
        robotsMeta.setAttribute('content', 'index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1');
      }
    };
  }, [isMissingJob]);

  const handlePrimaryAction = () => {
    if (setSelectedJobId) setSelectedJobId(null);
    if (user) {
      if (setActiveTab) setActiveTab('applications');
      else window.location.href = '/applications';
    } else {
      if (setActiveTab) setActiveTab('landing');
      else window.location.href = '/';
    }
  };

  const handleSecondaryAction = () => {
    if (setSelectedJobId) setSelectedJobId(null);
    if (user) {
      if (setActiveTab) setActiveTab('dashboard');
      else window.location.href = '/dashboard';
    } else {
      if (setActiveTab) setActiveTab('login');
      else window.location.href = '/login';
    }
  };

  return (
    <div className="not-found-page animate-fade-in">
      <header className="not-found-header">
        <div className="not-found-header-inner">
          <div 
            className="not-found-brand" 
            onClick={handleSecondaryAction}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handleSecondaryAction()}
          >
            <AppIcon size={28} className="not-found-logo" />
            <span className="not-found-brand-name">ApplyTrack</span>
          </div>
        </div>
      </header>

      <main className="not-found-main">
        <div className="not-found-card">
          {isMissingJob ? (
            <div className="not-found-missing-icon-badge">
              <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <line x1="9" y1="15" x2="15" y2="15"></line>
              </svg>
            </div>
          ) : (
            <div className="not-found-badge">404</div>
          )}
          
          <h1 className="not-found-title">{title}</h1>
          <p className="not-found-description">{description}</p>

          <div className="not-found-actions">
            <button 
              type="button" 
              onClick={handlePrimaryAction} 
              className="not-found-btn not-found-btn-primary"
            >
              {user ? (isMissingJob ? 'Go to Applications' : 'View Applications') : 'Back to Home'}
            </button>
            <button 
              type="button" 
              onClick={handleSecondaryAction} 
              className="not-found-btn not-found-btn-secondary"
            >
              {user ? (isMissingJob ? 'Back to Dashboard' : 'Return to Dashboard') : 'Sign In'}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
