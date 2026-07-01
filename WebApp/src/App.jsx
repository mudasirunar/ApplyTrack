import React, { useState, useEffect, useRef } from 'react';
import { db } from './utils/db';
import MainLayout from './components/MainLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Applications from './pages/Applications';
import JobAddEdit from './pages/JobAddEdit';
import JobDetail from './pages/JobDetail';
import Settings from './pages/Settings';

// Custom Path Parser for Single Page Application URL routing
const parsePath = (path) => {
  const p = path.replace(/\/$/, '') || '/';
  
  if (p === '/login') {
    return { tab: 'login', jobId: null };
  }
  if (p === '/dashboard' || p === '/') {
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
  
  const editMatch = p.match(/^\/applications\/([^\/]+)\/edit$/);
  if (editMatch) {
    return { tab: 'edit-job', jobId: editMatch[1] };
  }
  
  const detailMatch = p.match(/^\/applications\/([^\/]+)$/);
  if (detailMatch) {
    return { tab: 'job-detail', jobId: detailMatch[1] };
  }
  
  return { tab: 'dashboard', jobId: null };
};

export default function App() {
  const initialUser = db.getCurrentUser();
  const initialPath = window.location.pathname;
  const parsedInitial = parsePath(initialPath);
  
  let initialTab = parsedInitial.tab;
  let initialJobId = parsedInitial.jobId;
  
  // Resolve protected routes on initial load
  if (!initialUser) {
    initialTab = 'login';
    initialJobId = null;
    if (window.location.pathname !== '/login') {
      window.history.replaceState(null, '', '/login');
    }
  } else if (initialTab === 'login') {
    initialTab = 'dashboard';
    initialJobId = null;
    window.history.replaceState(null, '', '/dashboard');
  }

  const [user, setUser] = useState(initialUser);
  const [activeTab, setActiveTabState] = useState(initialTab);
  const [selectedJobId, setSelectedJobIdState] = useState(initialJobId);
  const [editSource, setEditSource] = useState('applications');
  const [isSelectionMode, setIsSelectionMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);
  
  // Ref to bypass React state-batching delays in synchronous navigation clicks
  const selectedJobIdRef = useRef(initialJobId);
  const [loadingProgress, setLoadingProgress] = useState(0);

  // Global Applications Filters State
  const [filters, setFilters] = useState({
    searchQuery: '',
    statusFilter: 'All',
    selectedResume: 'Select---',
    selectedPlatform: 'LinkedIn',
    dateFilterMode: 'Month',
    dateMonth: (new Date().getMonth() + 1).toString(),
    dateYear: new Date().getFullYear().toString(),
    dateSpecificDay: new Date().toISOString().split('T')[0],
    dateStartRange: (() => { const d = new Date(); d.setDate(1); return d.toISOString().split('T')[0]; })(),
    dateEndRange: new Date().toISOString().split('T')[0]
  });

  // Track user session changes to reset UI state on login/logout/switch transitions
  const lastUserEmailRef = useRef(initialUser ? initialUser.email : null);

  const resetFilters = () => {
    setFilters({
      searchQuery: '',
      statusFilter: 'All',
      selectedResume: 'Select---',
      selectedPlatform: 'LinkedIn',
      dateFilterMode: 'Month',
      dateMonth: (new Date().getMonth() + 1).toString(),
      dateYear: new Date().getFullYear().toString(),
      dateSpecificDay: new Date().toISOString().split('T')[0],
      dateStartRange: (() => { const d = new Date(); d.setDate(1); return d.toISOString().split('T')[0]; })(),
      dateEndRange: new Date().toISOString().split('T')[0]
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
    
    if (tab === 'edit-job') {
      if (activeTab === 'applications' || activeTab === 'job-detail') {
        setEditSource(activeTab);
      }
    }
    
    let path = '/dashboard';
    if (tab === 'login') path = '/login';
    else if (tab === 'applications') path = '/applications';
    else if (tab === 'settings') path = '/settings';
    else if (tab === 'add-job') path = '/applications/new';
    else if (tab === 'job-detail') path = `/applications/${jobId}`;
    else if (tab === 'edit-job') path = `/applications/${jobId}/edit`;

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
        const parsed = parsePath(currentPath);
        if (parsed.tab === 'login') {
          triggerLoader();
          window.history.replaceState(null, '', '/dashboard');
          setActiveTabState('dashboard');
        } else {
          setActiveTabState(parsed.tab);
          selectedJobIdRef.current = parsed.jobId;
          setSelectedJobIdState(parsed.jobId);
        }
      } else {
        if (window.location.pathname !== '/login') {
          window.history.replaceState(null, '', '/login');
        }
        setActiveTabState('login');
        selectedJobIdRef.current = null;
        setSelectedJobIdState(null);
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

  // Render correct page content
  const renderContent = () => {
    if (!user) {
      return <Login />;
    }

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
        return <Settings />;
      default:
        return <Dashboard setActiveTab={setActiveTab} setFilters={setFilters} />;
    }
  };

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
      <MainLayout 
        activeTab={user ? activeTab : 'login'} 
        setActiveTab={setActiveTab}
        isSelectionMode={isSelectionMode}
      >
        <div key={user ? user.email : 'none'} style={{ display: 'contents' }}>
          {renderContent()}
        </div>
      </MainLayout>
    </>
  );
}
