import React, { useEffect } from 'react';
import { AppIcon } from '../components/Icons';
import packageJson from '../../package.json';
import './Privacy.css';

export default function Privacy({ setActiveTab, user, fromTab }) {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' });
    document.title = 'Privacy Policy | ApplyTrack';
    return () => {
      document.title = 'ApplyTrack | Modern Job Application, Resume & Career Tracker';
    };
  }, []);

  const getSourceTab = () => {
    if (fromTab && fromTab !== 'privacy') return fromTab;
    try {
      const stored = sessionStorage.getItem('applytrack_privacy_from');
      if (stored && stored !== 'privacy') return stored;
    } catch {
      // ignore
    }
    return user ? 'dashboard' : 'landing';
  };

  const sourceTab = getSourceTab();

  const getReturnLabel = () => {
    switch (sourceTab) {
      case 'landing':
        return 'Back to Home';
      case 'settings':
        return 'Back to Settings';
      case 'dashboard':
        return 'Back to Dashboard';
      case 'applications':
        return 'Back to Applications';
      default:
        return user ? 'Back to Dashboard' : 'Back to Home';
    }
  };

  const handleReturn = () => {
    if (setActiveTab) {
      setActiveTab(sourceTab);
    } else if (window.history.length > 1) {
      window.history.back();
    } else {
      window.location.href = sourceTab === 'landing' ? '/' : (user ? '/dashboard' : '/');
    }
  };

  const handleBrandClick = () => {
    if (setActiveTab) {
      setActiveTab(user ? 'dashboard' : 'landing');
    } else {
      window.location.href = user ? '/dashboard' : '/';
    }
  };

  return (
    <div className="privacy-page">
      {/* Top Header */}
      <header className="privacy-header">
        <div className="privacy-header-inner">
          <div 
            className="privacy-brand" 
            onClick={handleBrandClick}
            role="button"
            tabIndex={0}
          >
            <AppIcon size={32} />
            <span className="privacy-brand-name">ApplyTrack</span>
          </div>

          <button 
            type="button" 
            onClick={handleReturn}
            className="privacy-return-btn"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
            </svg>
            <span>{getReturnLabel()}</span>
          </button>
        </div>
      </header>

      {/* Content Container */}
      <main className="privacy-main-container">
        <div className="privacy-content-card card-base">
          {/* Document Header */}
          <div className="privacy-doc-header">
            <span className="privacy-badge">Privacy &amp; Data Governance</span>
            <h1 className="privacy-title">Privacy Policy</h1>
            <p className="privacy-meta">
              <strong>Effective Date:</strong> September 24, 2026 • <strong>Policy Revision:</strong> v1.0 • <em>Applies to Web &amp; Android</em>
            </p>
          </div>

          <hr className="privacy-divider" />

          {/* Section 1 */}
          <section className="privacy-section">
            <h2 className="privacy-section-heading">1. Overview &amp; Privacy Commitment</h2>
            <p>
              ApplyTrack is a free, privacy-first career and job application tracker designed and engineered by Mudasir Ali. We believe your job hunt is sensitive, personal, and should remain completely under your control.
            </p>
            <p>
              ApplyTrack is <strong>100% free and open-source</strong>. We do <strong>not</strong> display advertisements, do <strong>not</strong> track you across third-party websites, and do <strong>not</strong> sell, rent, monetize, or broker your personal resumes or application history to recruitment agencies or data brokers.
            </p>
          </section>

          {/* Section 2 */}
          <section className="privacy-section">
            <h2 className="privacy-section-heading">2. Information We Collect</h2>
            <p>We collect only the essential data necessary to operate your application tracking dashboard:</p>
            <ul>
              <li>
                <strong>Account Information:</strong> When you sign in using Google Authentication, we receive your Google user identifier (UID), email address, and display name. We do not receive or store your Google password.
              </li>
              <li>
                <strong>Job Application Records:</strong> Data you manually enter, including company names, job titles, application statuses (Saved, Applied, Interview, Offer, Rejected), submission dates, platform sources (e.g. LinkedIn, Indeed), compensation details, and notes.
              </li>
              <li>
                <strong>Document Attachments:</strong> Resumes, cover letters, and document snapshots (PDF and image files) that you explicitly upload to attach to specific job applications.
              </li>
            </ul>
          </section>

          {/* Section 3 */}
          <section className="privacy-section">
            <h2 className="privacy-section-heading">3. Storage Providers &amp; Architecture</h2>
            <p>ApplyTrack utilizes a dual-cloud architecture to provide synchronization between the native Android app and the Web dashboard:</p>
            <ul>
              <li>
                <strong>Google Firebase Authentication &amp; Cloud Firestore:</strong> Used for managing secure user authentication sessions and synchronizing structured application metadata. All records in Firestore are strictly partitioned by authenticated user ID (under <code>/users/{'{'}userId{'}'}/job_applications</code>).
              </li>
              <li>
                <strong>Supabase Storage:</strong> Physical resume PDFs and document attachments are stored securely in Supabase Storage. File storage is strictly isolated to your authenticated account directory (under <code>/users/{'{'}userId{'}'}/{'{'}type{'}'}/{'{'}fileName{'}'}</code>).
              </li>
              <li>
                <strong>Local Device Cache:</strong> To provide instant UI responsiveness and offline capability, copies of your data are cached locally on your device (using browser <code>localStorage</code> on the Web and Room SQLite database on Android).
              </li>
            </ul>
          </section>

          {/* Section 4 */}
          <section className="privacy-section">
            <h2 className="privacy-section-heading">4. How Your Information Is Used</h2>
            <p>Your data is used solely for the following purposes:</p>
            <ul>
              <li>Displaying and managing your personal job application pipeline.</li>
              <li>Computing resume effectiveness metrics and interview callback percentages.</li>
              <li>Synchronizing your career data seamlessly between your Android device and web browser.</li>
              <li>Enabling you to view, download, or export your uploaded resumes and application records.</li>
            </ul>
          </section>

          {/* Section 5 */}
          <section className="privacy-section">
            <h2 className="privacy-section-heading">5. Data Ownership, Export &amp; Deletion Rights</h2>
            <p>You retain 100% ownership of your data at all times (Zero Vendor Lock-in):</p>
            <ul>
              <li>
                <strong>Full Data Export:</strong> Under the Settings tab, you can generate and download a complete ZIP archive containing your entire structured JSON database plus all physical binary attachments (PDF resumes and documents).
              </li>
              <li>
                <strong>Complete Account &amp; Data Erasure:</strong> You can permanently wipe your entire application database and all associated Supabase document attachments at any time via the Settings screen. Once confirmed, your records are deleted from Firestore and Supabase Storage immediately.
              </li>
            </ul>
          </section>

          {/* Section 6 */}
          <section className="privacy-section">
            <h2 className="privacy-section-heading">6. Open Source Transparency</h2>
            <p>
              ApplyTrack is open-source software licensed under the <strong>MIT License</strong>. The complete source code for both the Web application and the Android application is publicly inspectable and auditable on GitHub:
            </p>
            <p>
              <a 
                href="https://github.com/mudasirunar/ApplyTrack" 
                target="_blank" 
                rel="noopener noreferrer" 
                className="privacy-inline-link"
              >
                https://github.com/mudasirunar/ApplyTrack &rarr;
              </a>
            </p>
          </section>

          {/* Section 7 */}
          <section className="privacy-section">
            <h2 className="privacy-section-heading">7. Contact Information</h2>
            <p>
              If you have any questions, feedback, or data requests regarding this Privacy Policy, you may contact the developer directly:
            </p>
            <div className="privacy-contact-card">
              <span className="privacy-contact-name">Mudasir Ali</span>
              <span className="privacy-contact-role">Creator &amp; Maintainer of ApplyTrack</span>
              <a href="mailto:unarmudasir@gmail.com" className="privacy-contact-email">
                unarmudasir@gmail.com
              </a>
              <div className="privacy-contact-links">
                <a href="https://mudasir.tech/" target="_blank" rel="noopener noreferrer">Portfolio</a>
                <span>•</span>
                <a href="https://www.linkedin.com/in/mudasir-ali-442196261" target="_blank" rel="noopener noreferrer">LinkedIn</a>
                <span>•</span>
                <a href="http://github.com/mudasirunar" target="_blank" rel="noopener noreferrer">GitHub</a>
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
