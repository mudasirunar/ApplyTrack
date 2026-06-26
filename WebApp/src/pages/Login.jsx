import React, { useState } from 'react';
import { db } from '../utils/db';
import { AppIcon, GoogleIcon } from '../components/Icons';


export default function Login() {
  const [showPopup, setShowPopup] = useState(false);
  const [customName, setCustomName] = useState('');
  const [customEmail, setCustomEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const MOCK_ACCOUNTS = [
    {
      displayName: 'Alex Developer',
      email: 'alex.developer@gmail.com',
      photoURL: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=80&q=80'
    },
    {
      displayName: 'Sarah Coder',
      email: 'sarah.coder@gmail.com',
      photoURL: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=80&q=80'
    }
  ];

  const handleGoogleSignInClick = () => {
    setShowPopup(true);
  };

  const handleSelectAccount = (account) => {
    setIsLoading(true);
    setShowPopup(false);
    
    // Simulate slight network delay for premium feel
    setTimeout(() => {
      db.setCurrentUser(account);
      setIsLoading(false);
    }, 800);
  };

  const handleCustomSubmit = (e) => {
    e.preventDefault();
    if (!customName || !customEmail) return;

    const initials = customName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
    const mockAvatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(customName)}&background=2F3A4A&color=fff&bold=true`;

    handleSelectAccount({
      displayName: customName,
      email: customEmail,
      photoURL: mockAvatar
    });
  };

  return (
    <div className="login-container">
      <div className="login-card animate-scale-in">
        {/* Branding Area */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
          <div className="login-logo-wrapper">
            <AppIcon size={100} className="login-logo" />
          </div>
          <div>
            <h1 className="login-title">ApplyTrack</h1>
            <p className="login-subtitle">
              Track your career applications, resumes, and progress in one secure place.
            </p>
          </div>
        </div>

        {/* Action Button */}
        <div style={{ width: '100%' }}>
          <button 
            onClick={handleGoogleSignInClick} 
            className="google-login-btn"
            disabled={isLoading}
          >
            <GoogleIcon size={22} />
            <span>Sign in with Google</span>
          </button>
        </div>


      </div>

      {/* simulated Google OAuth Popup */}
      {showPopup && (
        <div className="modal-overlay" onClick={() => setShowPopup(false)}>
          <div className="google-popup-card animate-scale-in" onClick={(e) => e.stopPropagation()}>
            <div className="google-popup-header">
              <svg className="google-popup-logo" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56,12.25c0,-0.78 -0.07,-1.53 -0.2,-2.25H12v4.26h5.92c-0.26,1.37 -1.04,2.53 -2.21,3.31v2.77h3.57C21.36,18.42 22.56,15.6 22.56,12.25Z" />
                <path fill="#34A853" d="M12,23c2.97,0 5.46,-0.98 7.28,-2.66l-3.57,-2.77c-0.98,0.66 -2.23,1.06 -3.71,1.06c-2.86,0 -5.29,-1.93 -6.16,-4.53H2.18v2.84C3.99,20.53 7.7,23 12,23Z" />
                <path fill="#FBBC05" d="M5.84,14.09c-0.22,-0.66 -0.35,-1.36 -0.35,-2.09s0.13,-1.43 0.35,-2.09V7.07H2.18C1.43,8.55 1,10.22 1,12s0.43,3.45 1.18,4.93L5.84,14.09Z" />
                <path fill="#EA4335" d="M12,5.38c1.62,0 3.06,0.56 4.21,1.64l3.15,-3.15C17.45,2.09 14.97,1 12,1C7.7,1 3.99,3.47 2.18,7.07l3.66,2.84C6.71,7.31 9.14,5.38 12,5.38Z" />
              </svg>
              <span className="google-popup-headline">Sign in with Google</span>
            </div>
            
            <p className="google-popup-sub">
              to continue to <strong>ApplyTrack</strong>. Choose a mock developer account or type a custom name.
            </p>

            <div className="google-accounts-list">
              {MOCK_ACCOUNTS.map((account, idx) => (
                <div 
                  key={idx} 
                  className="google-account-row" 
                  onClick={() => handleSelectAccount(account)}
                >
                  <img src={account.photoURL} alt={account.displayName} className="google-account-avatar" />
                  <div className="google-account-details">
                    <span className="google-account-name">{account.displayName}</span>
                    <span className="google-account-email">{account.email}</span>
                  </div>
                </div>
              ))}
            </div>

            <form onSubmit={handleCustomSubmit} className="google-popup-custom-input">
              <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#3c4043' }}>Use a custom account:</span>
              <input 
                type="text" 
                placeholder="Full Name" 
                value={customName}
                onChange={(e) => setCustomName(e.target.value)}
                className="google-popup-input-field"
                required
              />
              <input 
                type="email" 
                placeholder="Email Address" 
                value={customEmail}
                onChange={(e) => setCustomEmail(e.target.value)}
                className="google-popup-input-field"
                required
              />
              <div className="google-popup-actions">
                <button 
                  type="button" 
                  onClick={() => setShowPopup(false)} 
                  className="google-popup-btn secondary"
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="google-popup-btn primary"
                >
                  Sign In
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* TRANSLUCENT OVERLAY LOADER */}
      {isLoading && (
        <div className="modal-overlay" style={{ backgroundColor: 'rgba(0, 0, 0, 0.4)' }}>
          <div className="modal-content-card animate-scale-in" style={{ maxWidth: '240px', textAlign: 'center', padding: '32px' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
              <div className="login-logo-wrapper" style={{ width: '48px', height: '48px', margin: 0 }}>
                <AppIcon size={32} className="login-logo" style={{ animationDuration: '1.5s' }} />
              </div>
              <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--brand-primary)' }}>
                Signing in...
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
