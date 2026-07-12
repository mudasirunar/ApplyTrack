import React, { useState } from 'react';
import { signInWithPopup, GoogleAuthProvider, setPersistence, browserLocalPersistence } from 'firebase/auth';
import { auth } from '../utils/firebase';
import { AppIcon, GoogleIcon } from '../components/Icons';
import loginBgMobileJpeg from '../assets/login_bg_mobile.jpeg';
import loginBgDesktopJpeg from '../assets/login_bg_desktop.jpeg';
import loginBgMobileAvif from '../assets/login_bg_mobile.avif';
import loginBgDesktopAvif from '../assets/login_bg_desktop.avif';
import './Login.css';

export default function Login() {
  const [isLoading, setIsLoading] = useState(false);

  const handleGoogleSignInClick = async () => {
    setIsLoading(true);
    try {
      // Explicitly set persistence to local to remember the user across sessions
      await setPersistence(auth, browserLocalPersistence);
      const provider = new GoogleAuthProvider();
      // Enforce account selection popup
      provider.setCustomParameters({ prompt: 'select_account' });
      await signInWithPopup(auth, provider);
    } catch (error) {
      console.error('Google sign in failed:', error);
      if (error.code !== 'auth/popup-closed-by-user') {
        alert(error.message || 'Failed to sign in with Google');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className="login-container">
      <picture className="login-bg-picture">
        <source media="(min-width: 768px)" srcSet={loginBgDesktopAvif} type="image/avif" />
        <source media="(min-width: 768px)" srcSet={loginBgDesktopJpeg} type="image/jpeg" />
        <source srcSet={loginBgMobileAvif} type="image/avif" />
        <img src={loginBgMobileJpeg} alt="" className="login-bg-image" fetchPriority="high" />
      </picture>
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

      {/* TRANSLUCENT OVERLAY LOADER */}
      {isLoading && (
        <div className="modal-overlay" style={{ backgroundColor: 'rgba(0, 0, 0, 0.4)' }}>
          <div className="modal-content-card animate-scale-in" style={{ maxWidth: '240px', textAlign: 'center', padding: '32px' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
              <div className="signin-spinner-container">
                <div className="signin-spinner-ring"></div>
                <div className="login-logo-wrapper" style={{ width: '48px', height: '48px', margin: 0 }}>
                  <AppIcon size={32} className="login-logo" style={{ animationDuration: '1.5s' }} />
                </div>
              </div>
              <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--brand-primary)' }}>
                Signing in...
              </span>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
