import React, { useState, useEffect } from 'react';
import { 
  AppIcon, 
  DashboardIcon, 
  ListIcon, 
  SettingsIcon 
} from '../components/Icons';
import { db } from '../utils/db';
import packageJson from '../../package.json';
import './Landing.css';

// SVG Icons matching the real app components
const CalendarIcon = () => (
  <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
    <path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z" />
  </svg>
);

const LinkIcon = () => (
  <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
    <path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z" />
  </svg>
);

const FileIcon = () => (
  <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
    <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" />
  </svg>
);

const CheckIcon = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="var(--accent-green)">
    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
  </svg>
);

// Semantic Feature Icons (Consistent 22x22 vector paths tailored to each feature)
const PipelineFeatureIcon = () => (
  <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
    <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 19H5V5h4v14zm5-4h-3V5h3v10zm5-6h-3V5h3v4z" />
  </svg>
);

const VaultFeatureIcon = () => (
  <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
    <path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" />
  </svg>
);

const SyncDevicesFeatureIcon = () => (
  <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
    <path d="M4 6h18V4H4c-1.1 0-2 .9-2 2v11H0v3h14v-3H4V6zm19 2h-6c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h6c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1zm-1 9h-4v-7h4v7z" />
  </svg>
);

const MetricsFeatureIcon = () => (
  <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
    <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zM7 10h2v7H7zm4-3h2v10h-2zm4 6h2v4h-2z" />
  </svg>
);

const SearchFilterFeatureIcon = () => (
  <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
    <path d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z" />
  </svg>
);

const ArchiveExportFeatureIcon = () => (
  <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
    <path d="M20.54 5.23l-1.39-1.68C18.88 3.21 18.47 3 18 3H6c-.47 0-.88.21-1.16.55L3.46 5.23C3.17 5.57 3 6.02 3 6.5V19c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6.5c0-.48-.17-.93-.46-1.27zM6.24 5h11.52l.83 1H5.42l.82-1zM5 19V8h14v11H5zm7-9l-4 4h2.55v3h2.9v-3H16l-4-4z" />
  </svg>
);

export default function Landing({ setActiveTab, user }) {
  const [currentTheme, setCurrentTheme] = useState(
    document.documentElement.getAttribute('data-theme') || 'system'
  );
  const [copiedEmail, setCopiedEmail] = useState(false);
  const [selectedSampleIndex, setSelectedSampleIndex] = useState(0);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Sync theme changes with the rest of ApplyTrack
  useEffect(() => {
    const handleThemeChange = () => {
      const active = document.documentElement.getAttribute('data-theme') || 'system';
      setCurrentTheme(active);
    };
    window.addEventListener('applytrack_theme_change', handleThemeChange);
    return () => window.removeEventListener('applytrack_theme_change', handleThemeChange);
  }, []);

  // Close mobile drawer on Escape key or window resize to desktop
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') setIsMobileMenuOpen(false);
    };
    const handleResize = () => {
      if (window.innerWidth >= 768) setIsMobileMenuOpen(false);
    };
    if (isMobileMenuOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('resize', handleResize);
    };
  }, [isMobileMenuOpen]);

  const toggleTheme = () => {
    const saved = db.getTheme();
    const nextTheme = saved === 'dark' ? 'light' : 'dark';
    db.setTheme(nextTheme);
    setCurrentTheme(nextTheme);
  };

  const handleCopyEmail = (e) => {
    e.preventDefault();
    navigator.clipboard.writeText('unarmudasir@gmail.com');
    setCopiedEmail(true);
    setTimeout(() => setCopiedEmail(false), 2500);
  };

  const scrollToSection = (id) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  // Authentic demo applications matching the real ApplyTrack schema
  const realAppSamples = [
    {
      companyName: 'Stripe',
      role: 'Staff Frontend Engineer',
      status: 'Interview',
      statusColor: 'var(--accent-green)',
      statusBg: 'var(--accent-green-tint)',
      dateText: 'Interview on Oct 2, 2026',
      platform: 'LinkedIn',
      resumeName: 'Resume_Staff_Frontend.pdf',
      salary: '$185,000 / yr',
      notes: 'System design with engineering manager completed. Next: Team technical discussion.'
    },
    {
      companyName: 'Spotify',
      role: 'Senior Android Developer',
      status: 'Offer',
      statusColor: 'var(--link-blue)',
      statusBg: 'var(--link-blue-tint)',
      dateText: 'Offer received Sep 20, 2026',
      platform: 'Company Careers',
      resumeName: 'Resume_Android_Kotlin.pdf',
      salary: '$195,000 / yr',
      notes: 'Official offer package received. Evaluating stock grants and benefits.'
    },
    {
      companyName: 'Google',
      role: 'Software Engineer III',
      status: 'Applied',
      statusColor: 'var(--warning-amber)',
      statusBg: 'var(--warning-amber-tint)',
      dateText: 'Applied on Sep 22, 2026',
      platform: 'Google Careers',
      resumeName: 'Resume_FullStack.pdf',
      salary: '$170,000 / yr',
      notes: 'Referred by senior software engineer. Waiting for recruiter screening reach-out.'
    }
  ];

  return (
    <div className="landing-page">
      {/* HEADER / NAVIGATION */}
      <header className="landing-header">
        <div className="landing-header-inner">
          <div className="landing-brand" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
            <AppIcon size={34} className="landing-brand-logo" />
            <span className="landing-brand-title">ApplyTrack</span>
          </div>

          <nav className="landing-nav-links" aria-label="Main Navigation">
            <button type="button" onClick={() => scrollToSection('features')} className="landing-nav-link">
              Features
            </button>
            <button type="button" onClick={() => scrollToSection('how-it-works')} className="landing-nav-link">
              How It Works
            </button>
            <button type="button" onClick={() => scrollToSection('ecosystem')} className="landing-nav-link">
              Platforms
            </button>
            <button type="button" onClick={() => scrollToSection('faq')} className="landing-nav-link">
              FAQ
            </button>
            <button type="button" onClick={() => scrollToSection('creator')} className="landing-nav-link">
              Creator
            </button>
          </nav>

          <div className="landing-header-actions">
            {/* Theme Toggle */}
            <button 
              type="button" 
              onClick={toggleTheme} 
              className="landing-theme-btn" 
              title="Toggle Theme"
              aria-label="Toggle Theme"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M12 3c-4.97 0-9 4.03-9 9 0 2.12.74 4.07 1.97 5.61L4.35 18.23c-.39.39-.39 1.02 0 1.41.39.39 1.02.39 1.41 0l.62-.62C8.01 20.25 9.92 21 12 21c4.97 0 9-4.03 9-9 0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-2.98 0-5.4-2.42-5.4-5.4 0-1.81.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z" />
              </svg>
            </button>

            {user ? (
              <button 
                type="button" 
                onClick={() => setActiveTab('dashboard')} 
                className="btn-primary landing-cta-btn"
              >
                <span>Dashboard</span>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M5 13h11.86l-5.43 5.43 1.42 1.42L21.14 12l-8.29-8.29-1.42 1.42L16.86 11H5v2z" />
                </svg>
              </button>
            ) : (
              <button 
                type="button" 
                onClick={() => setActiveTab('login')} 
                className="btn-primary landing-cta-btn"
              >
                Sign In
              </button>
            )}

            {/* Mobile Hamburger Toggle Button */}
            <button 
              type="button" 
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)} 
              className="landing-mobile-menu-btn"
              aria-label={isMobileMenuOpen ? 'Close Navigation Menu' : 'Open Navigation Menu'}
              aria-expanded={isMobileMenuOpen}
            >
              {isMobileMenuOpen ? (
                <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
                </svg>
              ) : (
                <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
                  <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z" />
                </svg>
              )}
            </button>
          </div>
        </div>

        {/* Mobile Dropdown Navigation Drawer & Backdrop */}
        {isMobileMenuOpen && (
          <>
            <div 
              className="landing-mobile-backdrop" 
              onClick={() => setIsMobileMenuOpen(false)} 
              aria-hidden="true" 
            />
            <div className="landing-mobile-drawer card-base animate-scale-in">
            <nav className="mobile-drawer-nav" aria-label="Mobile Navigation Menu">
              <button 
                type="button" 
                onClick={() => { scrollToSection('features'); setIsMobileMenuOpen(false); }} 
                className="mobile-drawer-link"
              >
                Features
              </button>
              <button 
                type="button" 
                onClick={() => { scrollToSection('how-it-works'); setIsMobileMenuOpen(false); }} 
                className="mobile-drawer-link"
              >
                How It Works
              </button>
              <button 
                type="button" 
                onClick={() => { scrollToSection('ecosystem'); setIsMobileMenuOpen(false); }} 
                className="mobile-drawer-link"
              >
                Platforms
              </button>
              <button 
                type="button" 
                onClick={() => { scrollToSection('faq'); setIsMobileMenuOpen(false); }} 
                className="mobile-drawer-link"
              >
                FAQ
              </button>
              <button 
                type="button" 
                onClick={() => { scrollToSection('creator'); setIsMobileMenuOpen(false); }} 
                className="mobile-drawer-link"
              >
                Creator
              </button>
            </nav>
            <div className="mobile-drawer-cta">
              <button 
                type="button" 
                onClick={() => { setActiveTab(user ? 'dashboard' : 'login'); setIsMobileMenuOpen(false); }}
                className="btn-primary"
                style={{ width: '100%', justifyContent: 'center' }}
              >
                {user ? 'Open Dashboard' : 'Sign In with Google'}
              </button>
            </div>
          </div>
        </>
      )}
      </header>

      {/* HERO SECTION */}
      <section className="landing-hero-section">
        <div className="landing-container">
          <div className="landing-hero-content">
            <div className="landing-eyebrow-pill">
              <span className="status-indicator-dot" />
              <span>Cross-Platform Job Application Tracker</span>
            </div>

            <h1 className="landing-hero-title">
              Track Applications, Resumes &amp; Progress in One Secure Place
            </h1>

            <p className="landing-hero-subtitle">
              A private, focused career companion for modern developers and job seekers. Manage every application stage,
              store custom resume PDFs with Supabase Storage, and sync effortlessly across Android and Web.
            </p>

            <div className="landing-hero-cta-group">
              <button 
                type="button" 
                onClick={() => setActiveTab(user ? 'dashboard' : 'login')} 
                className="btn-primary hero-main-btn"
              >
                <span>{user ? 'Open Dashboard' : 'Start Tracking Free'}</span>
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                  <path d="M5 13h11.86l-5.43 5.43 1.42 1.42L21.14 12l-8.29-8.29-1.42 1.42L16.86 11H5v2z" />
                </svg>
              </button>

              <button 
                type="button" 
                onClick={() => scrollToSection('how-it-works')} 
                className="btn-secondary hero-secondary-btn"
              >
                <span>See How It Works</span>
              </button>
            </div>

            {/* Quick App Pillars */}
            <div className="landing-hero-pillars">
              <div className="pillar-item">
                <CheckIcon />
                <span>100% Free &amp; Ad-Free</span>
              </div>
              <div className="pillar-item">
                <CheckIcon />
                <span>Supabase Cloud Attachments</span>
              </div>
              <div className="pillar-item">
                <CheckIcon />
                <span>Native Android Sync</span>
              </div>
              <div className="pillar-item">
                <CheckIcon />
                <span>Full ZIP Data Export</span>
              </div>
            </div>
          </div>

          {/* REAL APP UI SHOWCASE (Authentic Dashboard + Card view) */}
          <div className="real-app-showcase-container">
            <div className="real-app-window card-base">
              {/* Browser Window Header */}
              <div className="real-window-header">
                <div className="window-dots">
                  <span className="w-dot w-dot-close" />
                  <span className="w-dot w-dot-min" />
                  <span className="w-dot w-dot-max" />
                </div>
                <div className="window-address-bar">
                  <span className="window-url">applytrack.app/dashboard</span>
                </div>
                <div className="window-sync-status">
                  <span className="sync-pulse-dot" />
                  <span>Synced</span>
                </div>
              </div>

              {/* Realistic Dashboard Top Row */}
              <div className="showcase-dashboard-body">
                <div className="showcase-metrics-bar">
                  <div className="showcase-stat-box card-base">
                    <span className="stat-box-label">Applications</span>
                    <span className="stat-box-val">42</span>
                    <span className="stat-box-sub text-secondary">+5 this month</span>
                  </div>

                  <div className="showcase-stat-box card-base">
                    <span className="stat-box-label">Interview Rate</span>
                    <span className="stat-box-val text-green">28%</span>
                    <span className="stat-box-sub text-green">12 Active stages</span>
                  </div>

                  <div className="showcase-stat-box card-base">
                    <span className="stat-box-label">Job Offers</span>
                    <span className="stat-box-val text-blue">3</span>
                    <span className="stat-box-sub text-blue">Compensation review</span>
                  </div>

                  <div className="showcase-stat-box card-base">
                    <span className="stat-box-label">Response Rate</span>
                    <span className="stat-box-val text-amber">64%</span>
                    <span className="stat-box-sub text-secondary">27 Total responses</span>
                  </div>
                </div>

                {/* Application Card Showcase with Interactive Selector */}
                <div className="showcase-applications-section">
                  <div className="showcase-section-header">
                    <div className="showcase-tabs-left">
                      <span className="section-label">Interactive Application Card:</span>
                      <div className="sample-selector-pills">
                        {realAppSamples.map((s, idx) => (
                          <button
                            key={s.companyName}
                            type="button"
                            onClick={() => setSelectedSampleIndex(idx)}
                            className={`sample-pill-btn ${selectedSampleIndex === idx ? 'active' : ''}`}
                          >
                            {s.companyName}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Real JobCard Markup */}
                  {(() => {
                    const sample = realAppSamples[selectedSampleIndex];
                    return (
                      <div className="job-card card-base showcase-job-card" key={sample.companyName}>
                        <div className="job-card-main">
                          <div className="job-card-header">
                            <div>
                              <h4 className="job-card-role">{sample.role}</h4>
                              <span className="job-card-company">{sample.companyName}</span>
                            </div>
                            <span 
                              className="status-badge"
                              style={{ color: sample.statusColor, backgroundColor: sample.statusBg }}
                            >
                              {sample.status}
                            </span>
                          </div>

                          <div className="job-card-footer">
                            <div className="job-card-details">
                              <div className="job-card-detail-item">
                                <CalendarIcon />
                                <span>{sample.dateText}</span>
                              </div>
                              <div className="job-card-detail-item">
                                <span style={{ fontWeight: 700, color: 'var(--text-secondary)' }}>Salary:</span>
                                <span>{sample.salary}</span>
                              </div>
                            </div>
                          </div>

                          <hr className="job-card-divider" />

                          <div className="job-card-extra-row">
                            <div className="job-card-detail-item">
                              <LinkIcon />
                              <span>{sample.platform}</span>
                            </div>
                            <div className="job-card-detail-item" style={{ flex: 1, minWidth: '160px' }}>
                              <FileIcon />
                              <span style={{ fontWeight: 600 }}>{sample.resumeName}</span>
                            </div>
                          </div>

                          {sample.notes && (
                            <div className="job-card-notes-row">
                              <span className="notes-bullet">📝</span>
                              <span className="notes-text">{sample.notes}</span>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })()}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CORE FEATURES */}
      <section id="features" className="landing-section">
        <div className="landing-container">
          <div className="section-title-center">
            <span className="section-tag">Key Features</span>
            <h2 className="section-title-text">Designed Exactly for How People Apply to Jobs</h2>
            <p className="section-title-sub">
              No bloated enterprise CRM features. Just the essential, polished tools you need to stay on top of your search.
            </p>
          </div>

          <div className="features-card-grid">
            <div className="feature-item-card card-base">
              <div className="feature-icon-wrapper" aria-hidden="true">
                <PipelineFeatureIcon />
              </div>
              <h3 className="feature-card-heading">Structured Pipeline Tracking</h3>
              <p className="feature-card-desc">
                Log applications into 5 clear states: <strong>Saved, Applied, Interview, Offer,</strong> and <strong>Rejected</strong>. Track application dates, follow-up reminders, and salaries without messy sheets.
              </p>
            </div>

            <div className="feature-item-card card-base">
              <div className="feature-icon-wrapper" aria-hidden="true">
                <VaultFeatureIcon />
              </div>
              <h3 className="feature-card-heading">Supabase Document Vault</h3>
              <p className="feature-card-desc">
                Know exactly which resume version you sent to each company. Upload CVs, cover letters, and offer PDFs directly to Supabase Storage with instant in-app document viewing.
              </p>
            </div>

            <div className="feature-item-card card-base">
              <div className="feature-icon-wrapper" aria-hidden="true">
                <SyncDevicesFeatureIcon />
              </div>
              <h3 className="feature-card-heading">Android &amp; Web Synchronization</h3>
              <p className="feature-card-desc">
                Built from the ground up for two-way sync. Job metadata synchronizes via Firebase Firestore while attachments sync via Supabase Storage. Use Android on the go, Web at your desk.
              </p>
            </div>

            <div className="feature-item-card card-base">
              <div className="feature-icon-wrapper" aria-hidden="true">
                <MetricsFeatureIcon />
              </div>
              <h3 className="feature-card-heading">Resume Effectiveness Metrics</h3>
              <p className="feature-card-desc">
                Inspect which resume variations generate the most interview callbacks and offers. View monthly activity trends, platform breakdowns, and conversion percentages.
              </p>
            </div>

            <div className="feature-item-card card-base">
              <div className="feature-icon-wrapper" aria-hidden="true">
                <SearchFilterFeatureIcon />
              </div>
              <h3 className="feature-card-heading">Fast Search &amp; Sub-Filters</h3>
              <p className="feature-card-desc">
                Filter your applications by specific platforms (LinkedIn, Indeed, Email, Direct), resume variations, or timeframes (This Month, Custom Range) with instant search results.
              </p>
            </div>

            <div className="feature-item-card card-base">
              <div className="feature-icon-wrapper" aria-hidden="true">
                <ArchiveExportFeatureIcon />
              </div>
              <h3 className="feature-card-heading">Complete ZIP Archive Export</h3>
              <p className="feature-card-desc">
                Your data is yours. Export a single ZIP package containing your entire structured JSON database plus all physical binary attachments with one click, or restore on a new device.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* HOW IT WORKS / USAGE GUIDE */}
      <section id="how-it-works" className="landing-section section-alternate">
        <div className="landing-container">
          <div className="section-title-center">
            <span className="section-tag">Usage Guide</span>
            <h2 className="section-title-text">How ApplyTrack Works</h2>
            <p className="section-title-sub">
              Three simple steps to transform your job search from stressful chaos to an organized pipeline.
            </p>
          </div>

          <div className="steps-container-grid">
            <div className="step-card card-base">
              <div className="step-badge">Step 1</div>
              <h3 className="step-heading">Add Opportunity &amp; Attach CV</h3>
              <p className="step-body">
                Whenever you apply for a job, create an entry with company name, position, salary range, and platform. Attach the exact PDF resume you used for that application.
              </p>
            </div>

            <div className="step-card card-base">
              <div className="step-badge">Step 2</div>
              <h3 className="step-heading">Automatic Multi-Device Sync</h3>
              <p className="step-body">
                ApplyTrack saves your metadata to Firestore and binary documents to Supabase Storage. Whether you check your applications on your Android phone or your laptop, everything stays in sync.
              </p>
            </div>

            <div className="step-card card-base">
              <div className="step-badge">Step 3</div>
              <h3 className="step-heading">Follow Up &amp; Measure Success</h3>
              <p className="step-body">
                Update statuses when recruiters reach out. Check your conversion funnel to understand which resume or platform performs best, and review your notes right before interview rounds.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* PLATFORMS / ECOSYSTEM */}
      <section id="ecosystem" className="landing-section">
        <div className="landing-container">
          <div className="section-title-center">
            <span className="section-tag">Platforms</span>
            <h2 className="section-title-text">Available On Web &amp; Android</h2>
            <p className="section-title-sub">
              Enjoy a dedicated client on both desktop and mobile, sharing the exact same data model.
            </p>
          </div>

          <div className="platforms-cards-grid">
            <div className="platform-box card-base">
              <div className="platform-tag">Desktop &amp; Browser</div>
              <h3 className="platform-name">ApplyTrack WebApp</h3>
              <p className="platform-desc">
                High-performance React 19 single-page app. Perfect for managing applications while searching on your computer.
              </p>
              <ul className="platform-points">
                <li><CheckIcon /> Full analytics dashboard with donut &amp; activity charts</li>
                <li><CheckIcon /> In-browser document &amp; attachment viewer</li>
                <li><CheckIcon /> Bulk selection, batch status updates, and ZIP export</li>
              </ul>
              <button 
                type="button" 
                onClick={() => setActiveTab(user ? 'dashboard' : 'login')} 
                className="btn-primary"
                style={{ width: '100%', marginTop: '16px' }}
              >
                Launch Web App
              </button>
            </div>

            <div className="platform-box card-base">
              <div className="platform-tag platform-tag-android">Mobile Client</div>
              <h3 className="platform-name">ApplyTrack Android App</h3>
              <p className="platform-desc">
                Native Android app built with Jetpack Compose, Material Design 3, and offline-first Room database.
              </p>
              <ul className="platform-points">
                <li><CheckIcon /> Offline-first with local SQLite cache</li>
                <li><CheckIcon /> Background WorkManager sync when network returns</li>
                <li><CheckIcon /> Open-source Kotlin codebase on GitHub</li>
              </ul>
              <a 
                href="https://github.com/mudasirunar/ApplyTrack" 
                target="_blank" 
                rel="noopener noreferrer"
                className="btn-secondary"
                style={{ width: '100%', marginTop: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', textDecoration: 'none' }}
              >
                <span>View Android Repository</span>
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* FREQUENTLY ASKED QUESTIONS (REAL APP SPECS & SEO/GEO) */}
      <section id="faq" className="landing-section" itemScope itemType="https://schema.org/FAQPage">
        <div className="landing-container">
          <div className="section-title-center">
            <span className="section-tag">Frequently Asked Questions</span>
            <h2 className="section-title-text">Everything You Need to Know About ApplyTrack</h2>
            <p className="section-title-sub">
              Clear, transparent answers about privacy, Android synchronization, Supabase document storage, and data ownership.
            </p>
          </div>

          <div className="faq-accordion-list">
            <details className="faq-item card-base" itemScope itemProp="mainEntity" itemType="https://schema.org/Question" open>
              <summary className="faq-question">
                <span itemProp="name">How does the cloud synchronization between Android and the WebApp work?</span>
                <span className="faq-toggle-icon">▾</span>
              </summary>
              <div className="faq-answer" itemScope itemProp="acceptedAnswer" itemType="https://schema.org/Answer">
                <p itemProp="text">
                  ApplyTrack uses an integrated dual-cloud synchronization model. Application metadata (job title, company, status pipeline, salary, platform, and notes) synchronizes instantly through <strong>Firebase Firestore</strong>, while physical document attachments (PDF resumes, cover letters, and document snapshots) are stored securely in <strong>Supabase Storage</strong>. When you log or update an application on your native Android app or desktop web browser, changes reflect seamlessly across both platforms in real-time.
                </p>
              </div>
            </details>

            <details className="faq-item card-base" itemScope itemProp="mainEntity" itemType="https://schema.org/Question">
              <summary className="faq-question">
                <span itemProp="name">Where are my uploaded resumes stored, and are they private?</span>
                <span className="faq-toggle-icon">▾</span>
              </summary>
              <div className="faq-answer" itemScope itemProp="acceptedAnswer" itemType="https://schema.org/Answer">
                <p itemProp="text">
                  Your uploaded attachments are stored in your authenticated user directory within <strong>Supabase Storage</strong> (<code>/users/{'{'}userId{'}'}/{'{'}type{'}'}/{'{'}fileName{'}'}</code>). Access is strictly isolated to your authenticated account via secure token policies. ApplyTrack respects your privacy: your personal documents are never indexed, shared, sold, or parsed by third-party recruitment brokers.
                </p>
              </div>
            </details>

            <details className="faq-item card-base" itemScope itemProp="mainEntity" itemType="https://schema.org/Question">
              <summary className="faq-question">
                <span itemProp="name">Can I use the Android app without an internet connection?</span>
                <span className="faq-toggle-icon">▾</span>
              </summary>
              <div className="faq-answer" itemScope itemProp="acceptedAnswer" itemType="https://schema.org/Answer">
                <p itemProp="text">
                  Yes. The native ApplyTrack Android client is engineered with an <strong>offline-first architecture</strong> powered by local SQLite with Android Jetpack Room. You can record new applications, review interview notes, and search existing entries even when offline or in airplane mode. As soon as network connectivity is restored, Android WorkManager background tasks automatically push changes to Firestore and upload attachments to Supabase.
                </p>
              </div>
            </details>

            <details className="faq-item card-base" itemScope itemProp="mainEntity" itemType="https://schema.org/Question">
              <summary className="faq-question">
                <span itemProp="name">How does ApplyTrack measure resume effectiveness?</span>
                <span className="faq-toggle-icon">▾</span>
              </summary>
              <div className="faq-answer" itemScope itemProp="acceptedAnswer" itemType="https://schema.org/Answer">
                <p itemProp="text">
                  Whenever you add or edit a job application, you can attach or tag the exact CV variation used (for example, <em>Staff_Frontend_Resume.pdf</em> versus <em>FullStack_Engineer_Resume.pdf</em>). The ApplyTrack analytics engine calculates interview callback rates, rejection ratios, and offer conversions for each resume file, enabling you to identify which CV tailoring strategy generates the strongest recruiter response.
                </p>
              </div>
            </details>

            <details className="faq-item card-base" itemScope itemProp="mainEntity" itemType="https://schema.org/Question">
              <summary className="faq-question">
                <span itemProp="name">Can I export or backup all my application records and attachments?</span>
                <span className="faq-toggle-icon">▾</span>
              </summary>
              <div className="faq-answer" itemScope itemProp="acceptedAnswer" itemType="https://schema.org/Answer">
                <p itemProp="text">
                  Yes, zero vendor lock-in. Under the Settings menu, ApplyTrack offers a comprehensive <strong>Full ZIP Archive Export</strong>. Unlike platforms that only give you a text CSV, ApplyTrack archives your complete structured application JSON dataset alongside every original PDF resume, cover letter, and binary attachment into an unencrypted ZIP archive that you can save locally or restore at any time.
                </p>
              </div>
            </details>

            <details className="faq-item card-base" itemScope itemProp="mainEntity" itemType="https://schema.org/Question">
              <summary className="faq-question">
                <span itemProp="name">Is ApplyTrack free to use, and is it open source?</span>
                <span className="faq-toggle-icon">▾</span>
              </summary>
              <div className="faq-answer" itemScope itemProp="acceptedAnswer" itemType="https://schema.org/Answer">
                <p itemProp="text">
                  Yes, ApplyTrack is 100% free and open source. There are no paid tiers, hidden subscriptions, or advertising trackers. Both the native Android application (Kotlin + Jetpack Compose) and the Web dashboard (React 19 + Vite) are created and actively maintained by Mudasir Ali.
                </p>
              </div>
            </details>
          </div>
        </div>
      </section>

      {/* CALL TO ACTION */}
      <section className="landing-section section-alternate">
        <div className="landing-container">
          <div className="cta-card card-base">
            <h2 className="cta-heading">Ready to Organize Your Job Search?</h2>
            <p className="cta-sub">
              Join job seekers who manage their career journey with ApplyTrack. Free to use, no credit card required.
            </p>
            <div className="cta-buttons">
              <button 
                type="button" 
                onClick={() => setActiveTab(user ? 'dashboard' : 'login')} 
                className="btn-primary hero-main-btn"
              >
                <span>{user ? 'Open Dashboard' : 'Get Started with Google'}</span>
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                  <path d="M5 13h11.86l-5.43 5.43 1.42 1.42L21.14 12l-8.29-8.29-1.42 1.42L16.86 11H5v2z" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* FOOTER & CREATOR SECTION */}
      <footer id="creator" className="landing-footer">
        <div className="landing-container">
          <div className="footer-layout-grid">
            {/* About ApplyTrack */}
            <div className="footer-col-about">
              <div className="footer-brand-header">
                <AppIcon size={30} />
                <span className="footer-brand-title">ApplyTrack</span>
                <span className="footer-version-tag">v{packageJson.version}</span>
              </div>
              <p className="footer-brand-sub">
                A private, cross-platform job application and resume manager built for developers and job seekers.
              </p>
              <a 
                href="https://github.com/mudasirunar/ApplyTrack" 
                target="_blank" 
                rel="noopener noreferrer" 
                className="footer-oss-chip"
                title="View ApplyTrack source code & contribute on GitHub"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor">
                  <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
                </svg>
                <span>Contribute on GitHub</span>
                <span className="oss-chip-badge">Open Source</span>
                <svg viewBox="0 0 24 24" width="12" height="12" fill="currentColor" style={{ opacity: 0.65 }}>
                  <path d="M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z" />
                </svg>
              </a>
            </div>

            {/* Navigation links */}
            <div className="footer-col-nav">
              <h4 className="footer-heading">Navigation</h4>
              <ul className="footer-links">
                <li><button type="button" onClick={() => scrollToSection('features')} className="footer-link-action">Features</button></li>
                <li><button type="button" onClick={() => scrollToSection('how-it-works')} className="footer-link-action">How It Works</button></li>
                <li><button type="button" onClick={() => scrollToSection('ecosystem')} className="footer-link-action">Platforms</button></li>
                <li><button type="button" onClick={() => scrollToSection('faq')} className="footer-link-action">FAQs</button></li>
                <li><button type="button" onClick={() => setActiveTab('privacy')} className="footer-link-action">Privacy Policy</button></li>
                <li><button type="button" onClick={() => setActiveTab('login')} className="footer-link-action">Sign In</button></li>
              </ul>
            </div>

            {/* Creator & Contact Details */}
            <div className="footer-col-creator">
              <h4 className="footer-heading">Creator &amp; Contact</h4>
              <div className="creator-details-card card-base">
                <span className="creator-role-tag">Developer &amp; Designer</span>
                <h5 className="creator-fullname">Mudasir Ali</h5>
                <p className="creator-bio">
                  Software engineer specializing in cross-platform Android and modern web applications.
                </p>

                {/* Email with copy button */}
                <div className="creator-email-box">
                  <a 
                    href="mailto:unarmudasir@gmail.com" 
                    className="creator-email-anchor"
                    title="Send Email to Mudasir Ali"
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                      <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z" />
                    </svg>
                    <span>unarmudasir@gmail.com</span>
                  </a>
                  <button 
                    type="button" 
                    onClick={handleCopyEmail} 
                    className="email-copy-action"
                    title="Copy Email"
                  >
                    {copiedEmail ? 'Copied' : 'Copy'}
                  </button>
                </div>

                {/* Clean formatted links */}
                <div className="creator-links-group">
                  <a 
                    href="https://www.linkedin.com/in/mudasir-ali-442196261" 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="creator-link-chip"
                  >
                    <span>LinkedIn</span>
                  </a>

                  <a 
                    href="http://github.com/mudasirunar" 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="creator-link-chip"
                  >
                    <span>GitHub</span>
                  </a>

                  <a 
                    href="https://mudasir.tech/" 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="creator-link-chip creator-portfolio-link"
                  >
                    <span>Portfolio</span>
                  </a>
                </div>
              </div>
            </div>
          </div>

          <div className="footer-bottom-line">
            <span>&copy; {new Date().getFullYear()} ApplyTrack. Designed and engineered by Mudasir Ali.</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <button 
                type="button" 
                onClick={() => setActiveTab('privacy')} 
                className="scroll-top-link"
                style={{ opacity: 0.85 }}
              >
                Privacy Policy
              </button>
              <button 
                type="button" 
                onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })} 
                className="scroll-top-link"
              >
                Back to Top ↑
              </button>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
