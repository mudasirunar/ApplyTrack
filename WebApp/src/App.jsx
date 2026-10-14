import React, { useState, useEffect, useRef } from 'react';
import { db } from './utils/db';
import MainLayout from './components/MainLayout';
import Login from './pages/Login';
import SyncToast from './components/SyncToast';
import { logEvent } from 'firebase/analytics';
import { analytics } from './utils/firebase';
import { Analytics } from '@vercel/analytics/react';

const Landing = React.lazy(() => import('./pages/Landing'));
const Privacy = React.lazy(() => import('./pages/Privacy'));
const Dashboard = React.lazy(() => import('./pages/Dashboard'));
const Applications = React.lazy(() => import('./pages/Applications'));
const JobAddEdit = React.lazy(() => import('./pages/JobAddEdit'));
const JobDetail = React.lazy(() => import('./pages/JobDetail'));
const Settings = React.lazy(() => import('./pages/Settings'));

const getLocalDateString = (timestampOrDate = new Date()) => {
  const date = new Date(timestampOrDate);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const getLocalFirstOfMonth = () => {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}-01`;
};

// Custom Path Parser for Single Page Application URL routing
const parsePath = (path) => {
  const clean = (path || '/').split('?')[0].split('#')[0];
  const p = (clean.replace(/\/+$/, '') || '/').toLowerCase();
  
  if (p === '/') {
    return { tab: 'landing', jobId: null };
  }
  if (p === '/login') {
    return { tab: 'login', jobId: null };
  }
  if (p === '/privacy') {
    return { tab: 'privacy', jobId: null };
  }
  if (p === '/dashboard') {
    return { tab: 'dashboard', jobId: null };
  }
  if (p === '/applications') {
    return { tab: 'applications', jobId: null };
  }
  if (p === '/applications/new') {
    return { tab: 'add-job', jobId: null };
  }
  if (p === '/settings') {
    return { tab: 'settings', jobId: null };
  }
  
  const editMatch = p.match(/^\/applications\/([^/]+)\/edit$/);
  if (editMatch) {
    return { tab: 'edit-job', jobId: editMatch[1] };
  }
  
  const detailMatch = p.match(/^\/applications\/([^/]+)$/);
  if (detailMatch) {
    return { tab: 'job-detail', jobId: detailMatch[1] };
  }
  
  return { tab: 'not-found', jobId: null };
};

export default function App() {
  const initialUser = db.getCurrentUser();
  const initialPath = window.location.pathname;
  const parsedInitial = parsePath(initialPath);
  
  let initialTab = parsedInitial.tab;
  let initialJobId = parsedInitial.jobId;

  const isStandalone = typeof window !== 'undefined' && (
    window.navigator.standalone === true || 
    window.matchMedia('(display-mode: standalone)').matches
  );
  
  // Resolve protected routes on initial load
  if (!initialUser) {
    if (initialTab === 'landing' || initialTab === 'login' || initialTab === 'privacy' || initialTab === 'not-found') {
      // Public routes allowed without auth
      initialJobId = null;
    } else {
      // Protected routes require login
      initialTab = 'login';
      initialJobId = null;
      if (window.location.pathname !== '/login') {
        window.history.replaceState(null, '', '/login');
      }
    }
  } else if (initialTab === 'login' || (isStandalone && initialTab === 'landing')) {
    initialTab = 'dashboard';
    initialJobId = null;
    window.history.replaceState(null, '', '/dashboard');
  }

  const [user, setUser] = useState(initialUser);
  const [activeTab, setActiveTabState] = useState(initialTab);
  const [fromTab, setFromTab] = useState(() => {
    try {
      return sessionStorage.getItem('applytrack_privacy_from') || null;
    } catch {
      return null;
    }
  });
  const [isAuthReady, setIsAuthReady] = useState(db.isAuthReady());
  const [selectedJobId, setSelectedJobIdState] = useState(initialJobId);
  const [editSource, setEditSource] = useState('applications');
  const [isSelectionMode, setIsSelectionMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);
  
  // Ref to bypass React state-batching delays in synchronous navigation clicks
  const selectedJobIdRef = useRef(initialJobId);
  const [loadingProgress, setLoadingProgress] = useState(0);

  const renderFullScreenSpinner = () => (
    <div 
      style={{ 
        position: 'fixed',
        inset: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--bg-primary)',
        zIndex: 99999
      }}
    >
      <div className="signin-spinner-container" style={{ width: '50px', height: '50px' }}>
        <div className="signin-spinner-ring" style={{ borderWidth: '3px', borderColor: 'var(--brand-primary) transparent transparent transparent' }} />
      </div>
    </div>
  );

  const [filters, setFilters] = useState({
    searchQuery: '',
    statusFilter: 'All',
    selectedResume: 'Select---',
    selectedPlatform: 'LinkedIn',
    dateBasis: 'created',
    dateFilterMode: 'Month',
    dateMonth: (new Date().getMonth() + 1).toString(),
    dateYear: new Date().getFullYear().toString(),
    dateSpecificDay: getLocalDateString(),
    dateStartRange: getLocalFirstOfMonth(),
    dateEndRange: getLocalDateString()
  });

  // Track user session changes to reset UI state on login/logout/switch transitions
  const lastUserEmailRef = useRef(initialUser ? initialUser.email : null);

  const resetFilters = () => {
    setFilters({
      searchQuery: '',
      statusFilter: 'All',
      selectedResume: 'Select---',
      selectedPlatform: 'LinkedIn',
      dateBasis: 'created',
      dateFilterMode: 'Month',
      dateMonth: (new Date().getMonth() + 1).toString(),
      dateYear: new Date().getFullYear().toString(),
      dateSpecificDay: getLocalDateString(),
      dateStartRange: getLocalFirstOfMonth(),
      dateEndRange: getLocalDateString()
    });
  };

  const resetUIState = () => {
    resetFilters();
    setSelectedJobId(null);
    setEditSource('applications');
    setIsSelectionMode(false);
    setSelectedIds([]);
  };

  const loaderIntervalRef = useRef(null);
  const loaderTimeoutRef = useRef(null);

  // Premium glowing progress loader animator
  const triggerLoader = () => {
    if (loaderIntervalRef.current) clearInterval(loaderIntervalRef.current);
    if (loaderTimeoutRef.current) clearTimeout(loaderTimeoutRef.current);

    setLoadingProgress(10);
    let currentProgress = 10;

    loaderIntervalRef.current = setInterval(() => {
      currentProgress += (90 - currentProgress) * 0.15;
      setLoadingProgress(Math.min(currentProgress, 90));
    }, 60);

    loaderTimeoutRef.current = setTimeout(() => {
      clearInterval(loaderIntervalRef.current);
      setLoadingProgress(100);
      loaderTimeoutRef.current = setTimeout(() => {
        setLoadingProgress(0);
      }, 350);
    }, 200);
  };

  // Synchronous setSelectedJobId to update ref and state
  const setSelectedJobId = (id) => {
    selectedJobIdRef.current = id;
    setSelectedJobIdState(id);
  };

  // Custom setActiveTab wrapper to push URL path to browser history
  const setActiveTab = (tab) => {
    const jobId = selectedJobIdRef.current;
    
    if (tab === 'privacy') {
      setFromTab(activeTab);
      try {
        sessionStorage.setItem('applytrack_privacy_from', activeTab);
      } catch {}
    } else if (activeTab === 'privacy') {
      try {
        sessionStorage.removeItem('applytrack_privacy_from');
      } catch {}
    }

    if (tab === 'edit-job') {
      if (activeTab === 'applications' || activeTab === 'job-detail') {
        setEditSource(activeTab);
      }
    }
    
    let path = '/dashboard';
    if (tab === 'landing') path = '/';
    else if (tab === 'login') path = '/login';
    else if (tab === 'privacy') path = '/privacy';
    else if (tab === 'dashboard') path = '/dashboard';
    else if (tab === 'applications') path = '/applications';
    else if (tab === 'settings') path = '/settings';
    else if (tab === 'add-job') path = '/applications/new';
    else if (tab === 'job-detail') path = `/applications/${jobId}`;
    else if (tab === 'edit-job') path = `/applications/${jobId}/edit`;
    else if (tab === 'not-found') path = window.location.pathname;

    if (window.location.pathname !== path) {
      triggerLoader();
      window.history.pushState(null, '', path);
    }

    // Reset scroll position so each page starts at the top
    window.scrollTo(0, 0);
    setIsSelectionMode(false);
    setSelectedIds([]);
    setActiveTabState(tab);
  };

  // Listen to Auth, Theme, and PopState history changes
  useEffect(() => {
    // 1. Auth Change Listener with protected redirects
    const handleAuth = () => {
      setIsAuthReady(db.isAuthReady());
      const currentUser = db.getCurrentUser();
      
      const prevEmail = lastUserEmailRef.current;
      const currentEmail = currentUser ? currentUser.email : null;

      if (currentEmail !== prevEmail) {
        resetUIState();
        lastUserEmailRef.current = currentEmail;
      }

      setUser(currentUser);
      if (currentUser) {
        const currentPath = window.location.pathname;
        const isStandaloneMode = typeof window !== 'undefined' && (
          window.navigator.standalone === true || 
          window.matchMedia('(display-mode: standalone)').matches
        );
        if (parsed.tab === 'login' || (isStandaloneMode && parsed.tab === 'landing')) {
          triggerLoader();
          window.history.replaceState(null, '', '/dashboard');
          setActiveTabState('dashboard');
        } else {
          setActiveTabState(parsed.tab);
          selectedJobIdRef.current = parsed.jobId;
          setSelectedJobIdState(parsed.jobId);
        }
      } else {
        const currentPath = window.location.pathname;
        const parsed = parsePath(currentPath);
        if (parsed.tab === 'landing' || parsed.tab === 'login' || parsed.tab === 'privacy' || parsed.tab === 'not-found') {
          setActiveTabState(parsed.tab);
          selectedJobIdRef.current = null;
          setSelectedJobIdState(null);
        } else {
          if (window.location.pathname !== '/login') {
            window.history.replaceState(null, '', '/login');
          }
          setActiveTabState('login');
          selectedJobIdRef.current = null;
          setSelectedJobIdState(null);
        }
      }
    };
    
    // 2. Theme Applier
    const applyTheme = () => {
      const savedTheme = db.getTheme();
      let themeToSet = savedTheme;
      
      if (savedTheme === 'system') {
        const isSystemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        themeToSet = isSystemDark ? 'dark' : 'light';
      }
      
      document.documentElement.setAttribute('data-theme', themeToSet);
      const bgColor = themeToSet === 'dark' ? '#0E141C' : '#F7F8FA';
      document.documentElement.style.backgroundColor = bgColor;
    };

    // 3. Browser Back/Forward Popstate Listener
    const handlePopState = () => {
      const path = window.location.pathname;
      const parsed = parsePath(path);
      
      triggerLoader();
      
      // Reset scroll position on back/forward navigation
      window.scrollTo(0, 0);
      setActiveTabState(parsed.tab);
      selectedJobIdRef.current = parsed.jobId;
      setSelectedJobIdState(parsed.jobId);
    };

    // Initial run
    handleAuth();
    applyTheme();

    // Remove no-transitions class after initial paint
    const timer = setTimeout(() => {
      document.documentElement.classList.remove('no-transitions');
    }, 150);

    // Event listeners
    window.addEventListener('applytrack_auth_change', handleAuth);
    window.addEventListener('applytrack_theme_change', applyTheme);
    window.addEventListener('popstate', handlePopState);
    
    // System dark mode change listener
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleSystemThemeChange = () => {
      if (db.getTheme() === 'system') {
        applyTheme();
      }
    };
    
    mediaQuery.addEventListener('change', handleSystemThemeChange);

    return () => {
      clearTimeout(timer);
      window.removeEventListener('applytrack_auth_change', handleAuth);
      window.removeEventListener('applytrack_theme_change', applyTheme);
      window.removeEventListener('popstate', handlePopState);
      mediaQuery.removeEventListener('change', handleSystemThemeChange);
      if (loaderIntervalRef.current) clearInterval(loaderIntervalRef.current);
      if (loaderTimeoutRef.current) clearTimeout(loaderTimeoutRef.current);
    };
  }, []);

  // Log page view to Firebase Analytics on activeTab change
  useEffect(() => {
    if (analytics) {
      logEvent(analytics, 'page_view', {
        page_title: activeTab,
        page_location: window.location.href,
        page_path: window.location.pathname
      });
    }

    // Dynamically synchronize SEO metadata (canonical, description, og:url, og:title, twitter:title, robots, title) per route
    let canonical = document.querySelector('link[rel="canonical"]');
    if (!canonical) {
      canonical = document.createElement('link');
      canonical.setAttribute('rel', 'canonical');
      document.head.appendChild(canonical);
    }
    const currentPath = window.location.pathname.replace(/\/+$/, '') || '/';
    canonical.setAttribute('href', `https://apply-track-web.vercel.app${currentPath === '/' ? '/' : currentPath}`);

    let ogUrl = document.querySelector('meta[property="og:url"]');
    if (ogUrl) {
      ogUrl.setAttribute('content', `https://apply-track-web.vercel.app${currentPath === '/' ? '/' : currentPath}`);
    }

    let metaDesc = document.querySelector('meta[name="description"]');
    let robotsMeta = document.querySelector('meta[name="robots"]');
    let ogTitle = document.querySelector('meta[property="og:title"]');
    let twitterTitle = document.querySelector('meta[name="twitter:title"]');

    if (activeTab === 'privacy') {
      document.title = 'Privacy Policy | ApplyTrack';
      if (metaDesc) metaDesc.setAttribute('content', 'ApplyTrack Privacy Policy - Understand how your personal job application data, resumes, and document attachments are stored securely with zero data monetization.');
      if (robotsMeta) robotsMeta.setAttribute('content', 'index, follow');
      if (ogTitle) ogTitle.setAttribute('content', 'Privacy Policy | ApplyTrack');
      if (twitterTitle) twitterTitle.setAttribute('content', 'Privacy Policy | ApplyTrack');
    } else if (activeTab === 'login') {
      document.title = 'Sign In | ApplyTrack';
      if (metaDesc) metaDesc.setAttribute('content', 'Sign in to your ApplyTrack dashboard to track your job applications, tailored resumes, and review interview conversion analytics.');
      if (robotsMeta) robotsMeta.setAttribute('content', 'index, follow');
      if (ogTitle) ogTitle.setAttribute('content', 'Sign In | ApplyTrack');
      if (twitterTitle) twitterTitle.setAttribute('content', 'Sign In | ApplyTrack');
    } else if (activeTab === 'not-found') {
      document.title = '404: Page Not Found | ApplyTrack';
      if (metaDesc) metaDesc.setAttribute('content', 'The requested page could not be found on ApplyTrack.');
      if (robotsMeta) robotsMeta.setAttribute('content', 'noindex, nofollow');
      if (ogTitle) ogTitle.setAttribute('content', '404: Page Not Found | ApplyTrack');
      if (twitterTitle) twitterTitle.setAttribute('content', '404: Page Not Found | ApplyTrack');
    } else {
      if (activeTab === 'landing') {
        document.title = 'ApplyTrack | Modern Job Application, Resume & Career Tracker';
      }
      if (metaDesc) metaDesc.setAttribute('content', 'Track job applications, tailored resume variations, interview milestones, and conversion rate analytics with real-time Android and Web cloud sync. Free, private, and secure.');
      if (robotsMeta) robotsMeta.setAttribute('content', 'index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1');
      if (ogTitle) ogTitle.setAttribute('content', 'ApplyTrack | Modern Job Application & Resume Tracker');
      if (twitterTitle) twitterTitle.setAttribute('content', 'ApplyTrack | Modern Job Application & Resume Tracker');
    }
  }, [activeTab]);

  // Render correct page content
  const renderContent = () => {
    if (activeTab === 'landing') {
      return (
        <React.Suspense fallback={renderFullScreenSpinner()}>
          <Landing setActiveTab={setActiveTab} user={user} />
        </React.Suspense>
      );
    }

    if (activeTab === 'privacy') {
      return (
        <React.Suspense fallback={renderFullScreenSpinner()}>
          <Privacy setActiveTab={setActiveTab} user={user} fromTab={fromTab} />
        </React.Suspense>
      );
    }

    if (activeTab === 'not-found') {
      return (
        <div style={{
          minHeight: '70vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          padding: '40px 20px'
        }}>
          <h1 style={{ fontSize: '4.5rem', fontWeight: 900, color: 'var(--brand-primary)', margin: 0, letterSpacing: '-0.04em' }}>404</h1>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, margin: '12px 0 8px 0' }}>Page Not Found</h2>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '420px', marginBottom: '24px', lineHeight: 1.5 }}>
            The page you are looking for doesn't exist or has been moved.
          </p>
          <a
            href="/"
            onClick={(e) => { e.preventDefault(); setActiveTab('landing'); }}
            className="btn-primary"
            style={{ textDecoration: 'none', padding: '10px 22px' }}
          >
            Back to Home
          </a>
        </div>
      );
    }

    if (!user) {
      return <Login setActiveTab={setActiveTab} />;
    }

    return (
      <React.Suspense fallback={renderFullScreenSpinner()}>
        {(() => {
          switch (activeTab) {
            case 'dashboard':
              return (
                <Dashboard 
                  setActiveTab={setActiveTab} 
                  setFilters={setFilters} 
                />
              );
            case 'applications':
              return (
                <Applications 
                  filters={filters} 
                  setFilters={setFilters} 
                  setActiveTab={setActiveTab} 
                  setSelectedJobId={setSelectedJobId} 
                  isSelectionMode={isSelectionMode}
                  setIsSelectionMode={setIsSelectionMode}
                  selectedIds={selectedIds}
                  setSelectedIds={setSelectedIds}
                />
              );
            case 'job-detail':
              return (
                <JobDetail 
                  jobId={selectedJobId} 
                  setActiveTab={setActiveTab} 
                  setSelectedJobId={setSelectedJobId} 
                />
              );
            case 'add-job':
              return (
                <JobAddEdit 
                  jobId={null} 
                  setActiveTab={setActiveTab} 
                  setSelectedJobId={setSelectedJobId} 
                />
              );
            case 'edit-job':
              return (
                <JobAddEdit 
                  jobId={selectedJobId} 
                  setActiveTab={setActiveTab} 
                  setSelectedJobId={setSelectedJobId} 
                  editSource={editSource}
                />
              );
            case 'settings':
              return <Settings setActiveTab={setActiveTab} />;
            case 'privacy':
              return <Privacy setActiveTab={setActiveTab} user={user} fromTab={fromTab} />;
            default:
              return <Dashboard setActiveTab={setActiveTab} setFilters={setFilters} />;
          }
        })()}
      </React.Suspense>
    );
  };

  if (!isAuthReady) {
    return renderFullScreenSpinner();
  }

  return (
    <>
      {loadingProgress > 0 && (
        <div 
          className="app-loader-bar" 
          style={{ 
            width: `${loadingProgress}%`,
            opacity: loadingProgress === 100 ? 0 : 1,
            transition: loadingProgress === 100 ? 'width 0.15s ease-out, opacity 0.25s ease-in 0.1s' : 'width 0.2s ease-out'
          }}
        />
      )}
      {user && activeTab !== 'login' && activeTab !== 'landing' && activeTab !== 'privacy' && activeTab !== 'not-found' && <SyncToast />}
      <MainLayout 
        activeTab={user ? activeTab : (activeTab === 'landing' || activeTab === 'privacy' || activeTab === 'not-found' ? activeTab : 'login')} 
        setActiveTab={setActiveTab}
        isSelectionMode={isSelectionMode}
      >
        <div key={user ? user.email : 'guest'} style={{ display: 'contents' }}>
          {renderContent()}
        </div>
      </MainLayout>
      <Analytics />
    </>
  );
}
