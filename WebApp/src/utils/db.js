// ApplyTrack Mock Database and Auth Layer
// Uses localStorage for persistent client-side testing

const KEYS = {
  USER: 'applytrack_user',
  APPLICATIONS: 'applytrack_applications',
  THEME: 'applytrack_theme',
  DELETED_TEMP: 'applytrack_deleted_temp'
};

// High-fidelity Initial Mock Data to instantly show analytics
const INITIAL_MOCK_APPLICATIONS = [
  {
    id: 1,
    uuid: 'uuid-mock-1',
    companyName: 'Google',
    role: 'Frontend Engineer',
    platform: 'LinkedIn',
    status: 'Offer',
    jobDescription: 'Build next-generation user interfaces for AI platforms using React, TypeScript, and modern styling libraries.',
    notes: 'Had a wonderful chat with the recruiter. Technical rounds went smoothly. Received offer package!',
    url: 'https://careers.google.com/jobs/results/12345',
    email: 'recruiter@google.com',
    createdAt: Date.now() - 30 * 24 * 60 * 60 * 1000, // 30 days ago
    updatedAt: Date.now() - 2 * 24 * 60 * 60 * 1000, // 2 days ago
    statusHistory: [
      { status: 'Applied', timestamp: Date.now() - 30 * 24 * 60 * 60 * 1000 },
      { status: 'Interview', timestamp: Date.now() - 15 * 24 * 60 * 60 * 1000 },
      { status: 'Offer', timestamp: Date.now() - 2 * 24 * 60 * 60 * 1000 }
    ],
    resume: { fileName: 'resume_google_2026.pdf', originalName: 'Alex_Developer_Resume_2026.pdf' },
    coverLetter: { fileName: 'cover_google.pdf', originalName: 'Alex_Google_CoverLetter.pdf' },
    additionalDocument: null,
    screenshots: []
  },
  {
    id: 2,
    uuid: 'uuid-mock-2',
    companyName: 'Meta',
    role: 'Product Engineer',
    platform: 'Other',
    status: 'Interview',
    jobDescription: 'Collaborate with cross-functional teams to build interactive features for millions of users worldwide.',
    notes: 'Referred by John. System design round scheduled for next Tuesday. Need to study caching and CDN details.',
    url: 'https://metacareers.com/jobs/54321',
    email: 'john_referral@meta.com',
    createdAt: Date.now() - 12 * 24 * 60 * 60 * 1000, // 12 days ago
    updatedAt: Date.now() - 4 * 24 * 60 * 60 * 1000, // 4 days ago
    statusHistory: [
      { status: 'Applied', timestamp: Date.now() - 12 * 24 * 60 * 60 * 1000 },
      { status: 'Interview', timestamp: Date.now() - 4 * 24 * 60 * 60 * 1000 }
    ],
    resume: { fileName: 'resume_general.pdf', originalName: 'Alex_Developer_Resume_2026.pdf' },
    coverLetter: null,
    additionalDocument: null,
    screenshots: []
  },
  {
    id: 3,
    uuid: 'uuid-mock-3',
    companyName: 'Netflix',
    role: 'Senior UI Developer',
    platform: 'Website',
    status: 'Rejected',
    jobDescription: 'Optimize streaming dashboard performance and build sleek, premium design systems for international users.',
    notes: 'Resume selected, but rejected after the hiring manager round. Feedback: Looking for more low-level video streaming protocol experience.',
    url: 'https://netflix.com/careers/67890',
    email: 'hiring@netflix.com',
    createdAt: Date.now() - 45 * 24 * 60 * 60 * 1000, // 45 days ago
    updatedAt: Date.now() - 25 * 24 * 60 * 60 * 1000, // 25 days ago
    statusHistory: [
      { status: 'Applied', timestamp: Date.now() - 45 * 24 * 60 * 60 * 1000 },
      { status: 'Interview', timestamp: Date.now() - 35 * 24 * 60 * 60 * 1000 },
      { status: 'Rejected', timestamp: Date.now() - 25 * 24 * 60 * 60 * 1000 }
    ],
    resume: { fileName: 'resume_netflix.pdf', originalName: 'Alex_Netflix_Custom_Resume.pdf' },
    coverLetter: null,
    additionalDocument: null,
    screenshots: []
  },
  {
    id: 4,
    uuid: 'uuid-mock-4',
    companyName: 'Stripe',
    role: 'Software Engineer, Dashboard',
    platform: 'LinkedIn',
    status: 'Applied',
    jobDescription: 'Build beautiful, intuitive tools for merchant dashboard payments and developer-friendly documentation layouts.',
    notes: 'Applied via one-click apply on LinkedIn. Recruiter reached out to schedule a screening call.',
    url: 'https://stripe.com/jobs/999',
    email: null,
    createdAt: Date.now() - 5 * 24 * 60 * 60 * 1000, // 5 days ago
    updatedAt: Date.now() - 5 * 24 * 60 * 60 * 1000,
    statusHistory: [
      { status: 'Applied', timestamp: Date.now() - 5 * 24 * 60 * 60 * 1000 }
    ],
    resume: { fileName: 'resume_general.pdf', originalName: 'Alex_Developer_Resume_2026.pdf' },
    coverLetter: null,
    additionalDocument: null,
    screenshots: []
  },
  {
    id: 5,
    uuid: 'uuid-mock-5',
    companyName: 'Airbnb',
    role: 'Staff Engineer, Design Systems',
    platform: 'Indeed',
    status: 'Saved',
    jobDescription: 'Define core layout systems, tokens, and premium interactions for the global travel booking platform.',
    notes: 'Saved to apply later. Need to customize cover letter highlighting component library architecture experience.',
    url: 'https://airbnb.com/careers/staff-design-sys',
    email: null,
    createdAt: Date.now() - 1 * 24 * 60 * 60 * 1000, // 1 day ago
    updatedAt: Date.now() - 1 * 24 * 60 * 60 * 1000,
    statusHistory: [
      { status: 'Saved', timestamp: Date.now() - 1 * 24 * 60 * 60 * 1000 }
    ],
    resume: null,
    coverLetter: null,
    additionalDocument: null,
    screenshots: []
  },
  {
    id: 6,
    uuid: 'uuid-mock-6',
    companyName: 'Uber',
    role: 'React Native Developer',
    platform: 'LinkedIn',
    status: 'Applied',
    jobDescription: 'Work on rider experience features, improving mobile-web responsiveness and sleek transit dashboards.',
    notes: 'Applied online. Got confirmation email.',
    url: 'https://uber.com/careers/rn-dev',
    email: 'careers@uber.com',
    createdAt: Date.now() - 18 * 24 * 60 * 60 * 1000, // 18 days ago
    updatedAt: Date.now() - 18 * 24 * 60 * 60 * 1000,
    statusHistory: [
      { status: 'Applied', timestamp: Date.now() - 18 * 24 * 60 * 60 * 1000 }
    ],
    resume: { fileName: 'resume_general.pdf', originalName: 'Alex_Developer_Resume_2026.pdf' },
    coverLetter: null,
    additionalDocument: null,
    screenshots: []
  },
  {
    id: 7,
    uuid: 'uuid-mock-7',
    companyName: 'Amazon',
    role: 'SDE II, AWS Console',
    platform: 'Indeed',
    status: 'Interview',
    jobDescription: 'Design and build features for AWS cloud console dashboard, optimizing rendering performance and dark mode supporting systems.',
    notes: 'Coding assessment completed (2/2 test cases passed). Received invitation for full loop virtual interviews.',
    url: 'https://amazon.jobs/sde-ii-aws',
    email: 'aws_hiring@amazon.com',
    createdAt: Date.now() - 25 * 24 * 60 * 60 * 1000, // 25 days ago
    updatedAt: Date.now() - 10 * 24 * 60 * 60 * 1000, // 10 days ago
    statusHistory: [
      { status: 'Applied', timestamp: Date.now() - 25 * 24 * 60 * 60 * 1000 },
      { status: 'Interview', timestamp: Date.now() - 10 * 24 * 60 * 60 * 1000 }
    ],
    resume: { fileName: 'resume_general.pdf', originalName: 'Alex_Developer_Resume_2026.pdf' },
    coverLetter: null,
    additionalDocument: null,
    screenshots: []
  }
];

export const db = {
  // --- AUTH OPERATIONS ---
  getCurrentUser() {
    const user = localStorage.getItem(KEYS.USER);
    return user ? JSON.parse(user) : null;
  },

  setCurrentUser(user) {
    if (user) {
      localStorage.setItem(KEYS.USER, JSON.stringify(user));
    } else {
      localStorage.removeItem(KEYS.USER);
    }
    // Dispatch custom event to notify components
    window.dispatchEvent(new Event('applytrack_auth_change'));
  },

  logout() {
    localStorage.removeItem(KEYS.USER);
    window.dispatchEvent(new Event('applytrack_auth_change'));
  },

  // --- JOB APPLICATIONS CRUD ---
  getApplications() {
    let appsStr = localStorage.getItem(KEYS.APPLICATIONS);
    if (!appsStr) {
      // Pre-populate mock data on first visit
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(INITIAL_MOCK_APPLICATIONS));
      return INITIAL_MOCK_APPLICATIONS;
    }
    let apps = JSON.parse(appsStr);
    
    // Automatically migrate old platform names to standard ones for consistency
    let migrated = false;
    apps = apps.map(a => {
      if (a.platform === 'Referral') {
        a.platform = 'Other';
        migrated = true;
      }
      if (a.platform === 'Company Website') {
        a.platform = 'Website';
        migrated = true;
      }
      return a;
    });
    if (migrated) {
      localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(apps));
    }

    return apps;
  },

  saveApplications(apps) {
    localStorage.setItem(KEYS.APPLICATIONS, JSON.stringify(apps));
    window.dispatchEvent(new Event('applytrack_data_change'));
  },

  getApplicationById(id) {
    const apps = this.getApplications();
    return apps.find(a => a.id === Number(id)) || null;
  },

  addApplication(appData) {
    const apps = this.getApplications();
    const nextId = apps.length > 0 ? Math.max(...apps.map(a => a.id)) + 1 : 1;
    
    const newApp = {
      ...appData,
      id: nextId,
      uuid: crypto.randomUUID ? crypto.randomUUID() : 'uuid-' + Math.random().toString(36).substring(2, 11),
      createdAt: appData.createdAt || Date.now(),
      updatedAt: Date.now(),
      statusHistory: appData.statusHistory || [
        { status: appData.status || 'Applied', timestamp: appData.createdAt || Date.now() }
      ],
      screenshots: appData.screenshots || []
    };

    apps.push(newApp);
    this.saveApplications(apps);
    return newApp;
  },

  updateApplication(id, updatedData) {
    const apps = this.getApplications();
    const index = apps.findIndex(a => a.id === Number(id));
    if (index === -1) return null;

    const originalApp = apps[index];
    let oldHistory = originalApp.statusHistory || [];
    let newHistory = [];

    const timeApplied = updatedData.createdAt || Date.now();

    if (originalApp.status !== updatedData.status) {
      newHistory = [...oldHistory, { status: updatedData.status, timestamp: timeApplied }];
    } else {
      if (oldHistory.length > 0) {
        const updatedLast = { ...oldHistory[oldHistory.length - 1], timestamp: timeApplied };
        newHistory = [...oldHistory.slice(0, -1), updatedLast];
      } else {
        newHistory = [{ status: updatedData.status, timestamp: timeApplied }];
      }
    }

    const updatedApp = {
      ...originalApp,
      ...updatedData,
      statusHistory: newHistory,
      updatedAt: Date.now()
    };

    apps[index] = updatedApp;
    this.saveApplications(apps);
    return updatedApp;
  },

  deleteApplication(id) {
    const apps = this.getApplications();
    const appToDelete = apps.find(a => a.id === Number(id));
    if (!appToDelete) return false;

    // Save to temp deleted cache for undo
    localStorage.setItem(KEYS.DELETED_TEMP, JSON.stringify([appToDelete]));

    const remaining = apps.filter(a => a.id !== Number(id));
    this.saveApplications(remaining);
    return true;
  },

  deleteMultipleApplications(ids) {
    const apps = this.getApplications();
    const idsToCompare = ids.map(Number);
    const appsToDelete = apps.filter(a => idsToCompare.includes(a.id));
    if (appsToDelete.length === 0) return false;

    // Save to temp deleted cache for undo
    localStorage.setItem(KEYS.DELETED_TEMP, JSON.stringify(appsToDelete));

    const remaining = apps.filter(a => !idsToCompare.includes(a.id));
    this.saveApplications(remaining);
    return true;
  },

  undoDelete() {
    const temp = localStorage.getItem(KEYS.DELETED_TEMP);
    if (!temp) return false;

    const appsToRestore = JSON.parse(temp);
    const apps = this.getApplications();
    
    // Add restored applications back, ensuring no duplicate IDs
    const currentIds = apps.map(a => a.id);
    const restored = appsToRestore.filter(a => !currentIds.includes(a.id));
    
    const combined = [...apps, ...restored];
    // Sort by id to maintain sequence
    combined.sort((a, b) => a.id - b.id);

    this.saveApplications(combined);
    localStorage.removeItem(KEYS.DELETED_TEMP);
    return true;
  },

  clearUndoCache() {
    localStorage.removeItem(KEYS.DELETED_TEMP);
  },

  // --- THEME OPERATIONS ---
  getTheme() {
    return localStorage.getItem(KEYS.THEME) || 'system';
  },

  setTheme(theme) {
    localStorage.setItem(KEYS.THEME, theme);
    window.dispatchEvent(new Event('applytrack_theme_change'));
  },

  // --- DATA MANAGEMENT ---
  resetDatabase() {
    localStorage.removeItem(KEYS.APPLICATIONS);
    // Trigger reset with initial data
    this.getApplications();
    window.dispatchEvent(new Event('applytrack_data_change'));
  },

  exportData() {
    const apps = this.getApplications();
    const dataStr = JSON.stringify(apps, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `applytrack_backup_${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  },

  importData(jsonString) {
    try {
      const parsed = JSON.parse(jsonString);
      if (!Array.isArray(parsed)) throw new Error('Data must be an array');
      
      // Basic validation of fields
      const validated = parsed.map((item, index) => {
        return {
          id: item.id || index + 1,
          uuid: item.uuid || 'uuid-' + Math.random().toString(36).substring(2, 11),
          companyName: item.companyName || null,
          role: item.role || null,
          platform: item.platform || 'Direct',
          status: item.status || 'Applied',
          jobDescription: item.jobDescription || '',
          notes: item.notes || '',
          url: item.url || '',
          email: item.email || '',
          createdAt: item.createdAt || Date.now(),
          updatedAt: item.updatedAt || Date.now(),
          statusHistory: item.statusHistory || [{ status: item.status || 'Applied', timestamp: Date.now() }],
          resume: item.resume || null,
          coverLetter: item.coverLetter || null,
          additionalDocument: item.additionalDocument || null,
          screenshots: item.screenshots || []
        };
      });

      this.saveApplications(validated);
      return { success: true, count: validated.length };
    } catch (e) {
      console.error('Import failed:', e);
      return { success: false, error: e.message };
    }
  },

  // --- ANALYTICS ENGINE ---
  getAnalytics() {
    const apps = this.getApplications();
    const total = apps.length;

    if (total === 0) {
      return {
        total: 0,
        applied: 0,
        saved: 0,
        interviews: 0,
        offers: 0,
        rejected: 0,
        responses: 0,
        successRate: 0,
        rejectionRate: 0,
        interviewRate: 0,
        responseRate: 0,
        applicationsThisWeek: 0,
        applicationsThisMonth: 0,
        statusDistribution: [],
        platforms: [],
        resumeStats: [],
        monthlyActivity: {}
      };
    }

    const applied = apps.filter(a => a.status === 'Applied').length;
    const saved = apps.filter(a => a.status === 'Saved').length;
    const interviews = apps.filter(a => a.status === 'Interview').length;
    const offers = apps.filter(a => a.status === 'Offer').length;
    const rejected = apps.filter(a => a.status === 'Rejected').length;
    
    // In Android app, responses represents the COUNT of responded applications
    const responses = interviews + offers + rejected;

    // Conversion Rates based on total applications
    const successRate = total > 0 ? Math.round((offers / total) * 100) : 0;
    const rejectionRate = total > 0 ? Math.round((rejected / total) * 100) : 0;
    const interviewRate = total > 0 ? Math.round((interviews / total) * 100) : 0;
    const responseRate = total > 0 ? Math.round(((interviews + offers + rejected) / total) * 100) : 0;

    // Time calculations
    const now = Date.now();
    const oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000;
    const oneMonthAgo = now - 30 * 24 * 60 * 60 * 1000;
    
    const applicationsThisWeek = apps.filter(a => a.createdAt >= oneWeekAgo && a.status !== 'Saved').length;
    const applicationsThisMonth = apps.filter(a => a.createdAt >= oneMonthAgo && a.status !== 'Saved').length;

    // 1. Status Distribution Slices (For Donut Chart)
    const statusDistribution = [
      { name: 'Applied', count: applied, color: '#FFB300' },
      { name: 'Saved', count: saved, color: '#78909C' },
      { name: 'Interview', count: interviews, color: '#4CAF50' },
      { name: 'Offer', count: offers, color: '#1E88E5' },
      { name: 'Rejected', count: rejected, color: '#E53935' }
    ].filter(s => s.count > 0);

    // 2. Platforms breakdown
    const standardPlatforms = ['LinkedIn', 'Indeed', 'Email', 'Website'];
    const platformMap = {};
    apps.forEach(a => {
      if (a.status === 'Saved') return; // Only count active applications
      const plat = a.platform ? a.platform.trim() : '';
      const matched = standardPlatforms.find(sp => sp.toLowerCase() === plat.toLowerCase()) || 'Other';
      platformMap[matched] = (platformMap[matched] || 0) + 1;
    });
    const platforms = Object.entries(platformMap)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);

    // 3. Resume Effectiveness stats
    // We group by resume filename and calculate statistics
    const resumeMap = {};
    apps.forEach(a => {
      if (!a.resume || !a.resume.originalName) return;
      const resName = a.resume.originalName;
      if (!resumeMap[resName]) {
        resumeMap[resName] = { 
          resumeName: resName, 
          totalUsed: 0, 
          interviewCount: 0, 
          offerCount: 0, 
          rejectedCount: 0 
        };
      }
      resumeMap[resName].totalUsed += 1;
      if (a.status === 'Interview') {
        resumeMap[resName].interviewCount += 1;
      } else if (a.status === 'Offer') {
        resumeMap[resName].offerCount += 1;
      } else if (a.status === 'Rejected') {
        resumeMap[resName].rejectedCount += 1;
      }
    });

    const resumeStats = Object.values(resumeMap)
      .sort((a, b) => b.totalUsed - a.totalUsed);

    // 4. Monthly Activity bar chart data (group by year & month)
    const monthlyActivity = {};
    apps.forEach(a => {
      if (a.status === 'Saved') return;
      const date = new Date(a.createdAt);
      const year = date.getFullYear();
      const month = date.toLocaleString('default', { month: 'short' }); // e.g. 'Jan', 'Feb'
      
      if (!monthlyActivity[year]) {
        monthlyActivity[year] = {};
      }
      monthlyActivity[year][month] = (monthlyActivity[year][month] || 0) + 1;
    });

    return {
      total,
      applied,
      saved,
      interviews,
      offers,
      rejected,
      responses,
      successRate,
      rejectionRate,
      interviewRate,
      responseRate,
      applicationsThisWeek,
      applicationsThisMonth,
      statusDistribution,
      platforms,
      resumeStats,
      monthlyActivity
    };
  }
};
