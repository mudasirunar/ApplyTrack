import React, { useState, useEffect } from 'react';
import { db } from './utils/db';
import MainLayout from './components/MainLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Applications from './pages/Applications';
import JobAddEdit from './pages/JobAddEdit';
import JobDetail from './pages/JobDetail';
import Settings from './pages/Settings';

export default function App() {
  const [user, setUser] = useState(db.getCurrentUser());
  const [activeTab, setActiveTab] = useState('dashboard');
  const [selectedJobId, setSelectedJobId] = useState(null);

  // Global Applications Filters State
  const [filters, setFilters] = useState({
    searchQuery: '',
    statusFilter: 'All',
    selectedResume: 'All',
    selectedPlatform: 'All',
    dateFilterMode: 'All',
    dateMonth: '',
    dateYear: ''
  });

  // Listen to Auth and Theme changes
  useEffect(() => {
    // 1. Auth Change Listener
    const handleAuth = () => {
      const currentUser = db.getCurrentUser();
      setUser(currentUser);
      if (currentUser) {
        setActiveTab('dashboard');
      } else {
        setActiveTab('login');
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
    };

    // Initial run
    handleAuth();
    applyTheme();

    // Event listeners
    window.addEventListener('applytrack_auth_change', handleAuth);
    window.addEventListener('applytrack_theme_change', applyTheme);
    
    // System dark mode change listener
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleSystemThemeChange = () => {
      if (db.getTheme() === 'system') {
        applyTheme();
      }
    };
    
    mediaQuery.addEventListener('change', handleSystemThemeChange);

    return () => {
      window.removeEventListener('applytrack_auth_change', handleAuth);
      window.removeEventListener('applytrack_theme_change', applyTheme);
      mediaQuery.removeEventListener('change', handleSystemThemeChange);
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
          />
        );
      case 'settings':
        return <Settings />;
      default:
        return <Dashboard setActiveTab={setActiveTab} setFilters={setFilters} />;
    }
  };

  return (
    <MainLayout activeTab={user ? activeTab : 'login'} setActiveTab={setActiveTab}>
      {renderContent()}
    </MainLayout>
  );
}
