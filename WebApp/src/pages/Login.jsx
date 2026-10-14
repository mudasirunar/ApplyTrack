import React, { useState, useEffect } from 'react';
import { 
  signInWithPopup, 
  signInWithRedirect, 
  getRedirectResult, 
  GoogleAuthProvider, 
  setPersistence, 
  browserLocalPersistence 
} from 'firebase/auth';
import { auth } from '../utils/firebase';
import { useBodyScrollLock } from '../utils/useBodyScrollLock';
import { AppIcon, GoogleIcon } from '../components/Icons';
import loginBgMobileJpeg from '../assets/login_bg_mobile.jpeg';
import loginBgDesktopJpeg from '../assets/login_bg_desktop.jpeg';
import loginBgMobileAvif from '../assets/login_bg_mobile.avif';
import loginBgDesktopAvif from '../assets/login_bg_desktop.avif';
import './Login.css';

export default function Login({ setActiveTab }) {
  const [isLoading, setIsLoading] = useState(false);
  useBodyScrollLock(isLoading);

  useEffect(() => {
    document.title = 'Sign In | ApplyTrack';

    // Handle incoming redirect from Google sign-in (e.g. on iOS standalone PWA)
    let isMounted = true;
    getRedirectResult(auth)
      .then((result) => {
        if (!isMounted) return;
        if (result && result.user) {
          // Firebase onAuthStateChanged in db.js handles session setup
        }
      })
      .catch((error) => {
        if (!isMounted) return;
        console.error('Redirect sign-in error:', error);
        if (error.code !== 'auth/credential-already-in-use') {
          alert(error.message || 'Failed to complete sign in');
        }
      });

    return () => {
      isMounted = false;
      document.title = 'ApplyTrack | Modern Job Application, Resume & Career Tracker';
    };
  }, []);

  const handleGoogleSignInClick = () => {
    setIsLoading(true);
    try {
      const provider = new GoogleAuthProvider();
      provider.addScope('profile');
      provider.addScope('email');
      provider.setCustomParameters({ prompt: 'select_account' });

      // In iOS / WebKit, signInWithPopup MUST be initiated synchronously in the direct click turn.
      // Any preceding 'await' drops the browser's User Gesture token, causing iOS to block the popup.
      signInWithPopup(auth, provider)
        .then(() => {
          setIsLoading(false);
          // onAuthStateChanged in db.js automatically transitions to dashboard
        })
        .catch((error) => {
          setIsLoading(false);
          console.error('Google sign in failed:', error);
          if (error.code !== 'auth/popup-closed-by-user' && error.code !== 'auth/cancelled-popup-request') {
            alert(error.message || 'Failed to sign in with Google');
          }
        });
    } catch (e) {
      setIsLoading(false);
      console.error('Sign in trigger error:', e);
    }
  };

  return (
    <main className="login-container">
      {/* Top Header Navigation */}
      {setActiveTab && (
        <header className="login-header-bar">
          <a 
            href="/" 
            onClick={(e) => { e.preventDefault(); setActiveTab('landing'); }}
            className="login-header-brand"
            title="ApplyTrack Overview"
            style={{ textDecoration: 'none' }}
          >
            <AppIcon size={28} />
            <span className="login-header-brand-name">ApplyTrack</span>
          </a>

          <a 
            href="/" 
            onClick={(e) => { e.preventDefault(); setActiveTab('landing'); }}
            className="login-header-explore-btn"
            title="Explore features and overview"
            style={{ textDecoration: 'none' }}
          >
            <span>Explore ApplyTrack</span>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M5 13h11.86l-5.43 5.43 1.42 1.42L21.14 12l-8.29-8.29-1.42 1.42L16.86 11H5v2z" />
            </svg>
          </a>
        </header>
      )}

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

        {/* Action Button & Exploration Link */}
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '14px', alignItems: 'center' }}>
          <button 
            onClick={handleGoogleSignInClick} 
            className="google-login-btn"
            disabled={isLoading}
          >
            <GoogleIcon size={22} />
            <span>Sign in with Google</span>
          </button>

          {setActiveTab && (
            <a 
              href="/" 
              onClick={(e) => { e.preventDefault(); setActiveTab('landing'); }}
              className="login-card-learn-more"
              style={{ textDecoration: 'none' }}
            >
              <span>New to ApplyTrack?</span>
              <span className="learn-more-accent">See how it works &rarr;</span>
            </a>
          )}
        </div>
      </div>

      {/* Dedicated Bottom Footer Bar */}
      <footer className="login-footer-bar">
        <a 
          href="/" 
          onClick={(e) => { e.preventDefault(); setActiveTab('landing'); }}
          className="login-footer-link"
        >
          Home
        </a>
        <span className="login-footer-dot">•</span>
        <a 
          href="/privacy" 
          onClick={(e) => { e.preventDefault(); setActiveTab('privacy'); }}
          className="login-footer-link"
        >
          Privacy Policy
        </a>
        <span className="login-footer-dot">•</span>
        <a 
          href="https://github.com/mudasirunar/ApplyTrack" 
          target="_blank" 
          rel="noopener noreferrer"
          className="login-footer-link"
        >
          GitHub
        </a>
      </footer>

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
