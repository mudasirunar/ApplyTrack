import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { db } from '../utils/db';
import { 
  SearchIcon, 
  AddIcon, 
  EditIcon, 
  DeleteIcon, 
  CloseIcon,
  ChevronIcon,
  CalendarIcon,
  LinkIcon,
  EmailIcon,
  FileIcon,
  CheckIcon,
  SelectAllIcon,
  DeselectAllIcon
} from '../components/Icons';
import './Applications.css';

const getFormattedStatusDate = (app) => {
  const history = app.statusHistory || [];
  const statusTimestamp = history.length > 0 ? history[history.length - 1].timestamp : app.createdAt;
  const dateStr = new Date(statusTimestamp).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  return `${app.status} on ${dateStr}`;
};

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

function ConfirmationModal({ title, message, confirmLabel, isDestructive, onConfirm, onCancel }) {
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-content-card" style={{ maxWidth: '400px' }} onClick={(e) => e.stopPropagation()}>
        <h3 className="modal-title" style={{ margin: 0, color: isDestructive ? 'var(--error-red)' : 'var(--brand-primary)' }}>
          {title}
        </h3>
        <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', margin: '8px 0' }}></div>
        <p className="modal-text" style={{ fontSize: '0.85rem', lineHeight: '1.6', color: 'var(--text-primary)', margin: '12px 0 20px' }}>
          {message}
        </p>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <button onClick={onCancel} className="btn-secondary" style={{ padding: '8px 16px', fontSize: '0.85rem' }}>
            Cancel
          </button>
          <button 
            onClick={onConfirm} 
            className="btn-primary" 
            style={{ 
              padding: '8px 16px', 
              fontSize: '0.85rem', 
              backgroundColor: isDestructive ? 'var(--error-red)' : undefined,
              borderColor: isDestructive ? 'var(--error-red)' : undefined,
              color: '#FFFFFF'
            }}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function Applications({ 
  filters, 
  setFilters, 
  setActiveTab, 
  setSelectedJobId,
  isSelectionMode,
  setIsSelectionMode,
  selectedIds,
  setSelectedIds
}) {
  const [applications, setApplications] = useState(db.getApplications());
  const [analytics, setAnalytics] = useState(db.getAnalytics());
  const [isFabVisible, setIsFabVisible] = useState(true);
  const [showSortMenu, setShowSortMenu] = useState(false);
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  
  const [showDateInfoModal, setShowDateInfoModal] = useState(false);
  const [appToDelete, setAppToDelete] = useState(null);
  const [appsToDeleteList, setAppsToDeleteList] = useState([]);



  const lastScrollY = useRef(0);
  const scrollContainerRef = useRef(null);
  const chipContainerRef = useRef(null);

  // Sorting options
  const SORT_OPTIONS = {
    STATUS_LATEST: 'Latest Status',
    STATUS_OLDEST: 'Oldest Status',
    CREATION_LATEST: 'Latest Added',
    CREATION_OLDEST: 'Oldest Added'
  };
  const [sortOption, setSortOption] = useState('STATUS_LATEST');

  useEffect(() => {
    const handleDataChange = () => {
      setApplications(db.getApplications());
      setAnalytics(db.getAnalytics());
    };
    window.addEventListener('applytrack_data_change', handleDataChange);

    // Scroll listener to hide/show FAB
    const handleScroll = () => {
      const currentScrollY = window.scrollY;
      if (currentScrollY > lastScrollY.current && currentScrollY > 60) {
        setIsFabVisible(false);
      } else {
        setIsFabVisible(true);
      }
      lastScrollY.current = currentScrollY;
    };

    window.addEventListener('scroll', handleScroll);

    return () => {
      window.removeEventListener('applytrack_data_change', handleDataChange);
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  // Auto-scroll the filter chips container to make the active chip visible
  useEffect(() => {
    if (chipContainerRef.current) {
      const activeBtn = chipContainerRef.current.querySelector('.filter-chip.active');
      if (activeBtn) {
        activeBtn.scrollIntoView({
          behavior: 'smooth',
          block: 'nearest',
          inline: 'center'
        });
      }
    }
  }, [filters.statusFilter]);

  // Filter application list based on active filters
  const getFilteredApps = () => {
    let list = [...applications];

    // 1. Search Query
    if (filters.searchQuery) {
      const query = filters.searchQuery.toLowerCase();
      list = list.filter(a => 
        ((a.companyName || 'Unknown Company').toLowerCase().includes(query)) ||
        ((a.role || 'Position unassigned').toLowerCase().includes(query)) ||
        (a.jobDescription && a.jobDescription.toLowerCase().includes(query)) ||
        (a.notes && a.notes.toLowerCase().includes(query)) ||
        (a.resume && a.resume.originalName && a.resume.originalName.toLowerCase().includes(query)) ||
        (a.coverLetter && a.coverLetter.originalName && a.coverLetter.originalName.toLowerCase().includes(query)) ||
        (a.additionalDocument && a.additionalDocument.originalName && a.additionalDocument.originalName.toLowerCase().includes(query)) ||
        (a.url && a.url.toLowerCase().includes(query)) ||
        (a.email && a.email.toLowerCase().includes(query))
      );
    }

    // 2. Status Filter Chip
    if (['Applied', 'Saved', 'Interview', 'Offer', 'Rejected'].includes(filters.statusFilter)) {
      list = list.filter(a => a.status === filters.statusFilter);
    } else if (filters.statusFilter === 'Response') {
      list = list.filter(a => ['Interview', 'Offer', 'Rejected'].includes(a.status));
    }

    // 3. Sub-filter: Platform
    if (filters.statusFilter === 'Platform') {
      list = list.filter(a => a.platform === filters.selectedPlatform);
    }

    // 4. Sub-filter: Resume
    if (filters.statusFilter === 'Resume') {
      if (filters.selectedResume === 'Select---') {
        list = [];
      } else {
        list = list.filter(a => a.resume && a.resume.originalName === filters.selectedResume);
      }
    }

    // 5. Sub-filter: Date
    if (filters.statusFilter === 'Date') {
      list = list.filter(a => {
        const statusTimestamp = (a.statusHistory && a.statusHistory.length > 0) 
          ? a.statusHistory[a.statusHistory.length - 1].timestamp 
          : a.createdAt;
        
        const date = new Date(statusTimestamp);

        if (filters.dateFilterMode === 'Month') {
          const appMonth = date.getMonth() + 1; // 1..12
          const appYear = date.getFullYear().toString();
          return appMonth === Number(filters.dateMonth) && appYear === filters.dateYear;
        }

        if (filters.dateFilterMode === 'Day') {
          if (!filters.dateSpecificDay) return true;
          const targetDate = new Date(filters.dateSpecificDay);
          return date.getFullYear() === targetDate.getFullYear() &&
                 date.getMonth() === targetDate.getMonth() &&
                 date.getDate() === targetDate.getDate();
        }

        if (filters.dateFilterMode === 'Range') {
          const startTimestamp = filters.dateStartRange ? new Date(filters.dateStartRange).setHours(0, 0, 0, 0) : 0;
          const endTimestamp = filters.dateEndRange ? new Date(filters.dateEndRange).setHours(23, 59, 59, 999) : Infinity;
          return statusTimestamp >= startTimestamp && statusTimestamp <= endTimestamp;
        }

        return true;
      });
    }

    // Sort list
    switch (sortOption) {
      case 'STATUS_LATEST':
        list.sort((a, b) => {
          const tA = (a.statusHistory && a.statusHistory.length > 0) ? a.statusHistory[a.statusHistory.length - 1].timestamp : a.createdAt;
          const tB = (b.statusHistory && b.statusHistory.length > 0) ? b.statusHistory[b.statusHistory.length - 1].timestamp : b.createdAt;
          return tB - tA;
        });
        break;
      case 'STATUS_OLDEST':
        list.sort((a, b) => {
          const tA = (a.statusHistory && a.statusHistory.length > 0) ? a.statusHistory[a.statusHistory.length - 1].timestamp : a.createdAt;
          const tB = (b.statusHistory && b.statusHistory.length > 0) ? b.statusHistory[b.statusHistory.length - 1].timestamp : b.createdAt;
          return tA - tB;
        });
        break;
      case 'CREATION_LATEST':
        list.sort((a, b) => b.createdAt - a.createdAt);
        break;
      case 'CREATION_OLDEST':
        list.sort((a, b) => a.createdAt - b.createdAt);
        break;
      default:
        list.sort((a, b) => {
          const tA = (a.statusHistory && a.statusHistory.length > 0) ? a.statusHistory[a.statusHistory.length - 1].timestamp : a.createdAt;
          const tB = (b.statusHistory && b.statusHistory.length > 0) ? b.statusHistory[b.statusHistory.length - 1].timestamp : b.createdAt;
          return tB - tA;
        });
        break;
    }

    return list;
  };

  const filteredApps = getFilteredApps();
  const displayedIds = filteredApps.map(a => a.id);
  const allSelected = displayedIds.length > 0 && displayedIds.every(id => selectedIds.includes(id));

  // Filter actions
  const handleStatusFilterClick = (status) => {
    setFilters(prev => {
      const next = { ...prev, statusFilter: status };
      if (status === 'Platform' && (prev.selectedPlatform === 'All' || !prev.selectedPlatform)) {
        next.selectedPlatform = 'LinkedIn';
      }
      if (status === 'Resume' && (prev.selectedResume === 'All' || !prev.selectedResume)) {
        next.selectedResume = 'Select---';
      }
      if (status === 'Date' && (prev.dateFilterMode === 'All' || !prev.dateFilterMode)) {
        next.dateFilterMode = 'Month';
        next.dateMonth = (new Date().getMonth() + 1).toString();
        next.dateYear = new Date().getFullYear().toString();
        next.dateSpecificDay = getLocalDateString();
        next.dateStartRange = getLocalFirstOfMonth();
        next.dateEndRange = getLocalDateString();
      }
      return next;
    });
    if (status === 'All') {
      setSortOption('STATUS_LATEST');
    }
  };

  const handleToggleSelect = (id) => {
    setSelectedIds(prev => {
      const isSelected = prev.includes(id);
      const newSelection = isSelected ? prev.filter(item => item !== id) : [...prev, id];
      if (newSelection.length === 0) {
        setIsSelectionMode(false);
      }
      return newSelection;
    });
  };

  const handleSelectAllToggle = () => {
    if (allSelected) {
      setSelectedIds([]);
      setIsSelectionMode(false);
    } else {
      setSelectedIds([...displayedIds]);
    }
  };

  const handleEnterSelectionMode = (firstId) => {
    setIsSelectionMode(true);
    setSelectedIds([firstId]);
  };

  const handleExitSelectionMode = () => {
    setIsSelectionMode(false);
    setSelectedIds([]);
  };

  const handleDeleteSingle = (e, app) => {
    e.stopPropagation();
    setAppToDelete(app);
  };

  const handleConfirmDeleteSingle = (app) => {
    db.deleteApplication(app.id);
    setAppToDelete(null);
    
    const jobName = app.companyName 
      ? `${app.companyName} - ${app.role || 'Position unassigned'}` 
      : (app.role || 'Application');

    // Trigger toast with undo
    window.dispatchEvent(new CustomEvent('applytrack_toast', {
      detail: {
        message: `'${jobName}' deleted`,
        action: 'Undo',
        onAction: () => {
          db.undoDelete();
        }
      }
    }));
  };

  const handleDeleteSelected = () => {
    if (selectedIds.length === 0) return;
    const appsList = applications.filter(a => selectedIds.includes(a.id));
    setAppsToDeleteList(appsList);
  };

  const handleConfirmDeleteList = (appsList) => {
    const ids = appsList.map(a => a.id);
    const count = ids.length;
    db.deleteMultipleApplications(ids);
    setAppsToDeleteList([]);
    handleExitSelectionMode();

    // Trigger toast with undo
    window.dispatchEvent(new CustomEvent('applytrack_toast', {
      detail: {
        message: `${count} applications deleted`,
        action: 'Undo',
        onAction: () => {
          db.undoDelete();
        }
      }
    }));
  };

  const handleJobCardClick = (app) => {
    if (isSelectionMode) {
      handleToggleSelect(app.id);
    } else {
      setSelectedJobId(app.id);
      setActiveTab('job-detail');
    }
  };

  const handleEditClick = (e, app) => {
    e.stopPropagation();
    setSelectedJobId(app.id);
    setActiveTab('edit-job');
  };

  // Helper for Status Badge colors
  const getStatusColorClass = (status) => {
    switch (status) {
      case 'Applied': return { color: 'var(--warning-amber)', bg: 'var(--warning-amber-tint)' };
      case 'Saved': return { color: 'var(--saved-gray)', bg: 'var(--saved-gray-tint)' };
      case 'Interview': return { color: 'var(--accent-green)', bg: 'var(--accent-green-tint)' };
      case 'Offer': return { color: 'var(--link-blue)', bg: 'var(--link-blue-tint)' };
      case 'Rejected': return { color: 'var(--error-red)', bg: 'var(--error-red-tint)' };
      default: return { color: 'var(--text-secondary)', bg: 'var(--bg-surface-variant)' };
    }
  };

  // Populate sub-filter options
  const uniquePlatforms = analytics.platforms.map(p => p.name);
  const uniqueResumes = analytics.resumeStats.map(r => r.resumeName);
  const uniqueMonthsAndYears = [];
  
  // Extract months/years that actually have data
  Object.entries(analytics.monthlyActivity || {}).forEach(([year, monthsObj]) => {
    Object.keys(monthsObj).forEach(month => {
      uniqueMonthsAndYears.push({ month, year });
    });
  });

  return (
    <>
      <div className="content-container animate-fade-in" style={{ position: 'relative' }}>
      
      <div className={`apps-floating-header ${isSelectionMode ? 'selection-active' : ''}`}>
        {/* SELECTION MODE TOP BAR */}
        {isSelectionMode ? (
          <div className="selection-mode-bar">
            <div className="selection-bar-left">
              <button onClick={handleExitSelectionMode} className="selection-bar-btn" title="Cancel selection">
                <CloseIcon />
              </button>
              <span className="selection-bar-title">{selectedIds.length} Selected</span>
            </div>
          </div>
        ) : (
          /* STANDARD SEARCH & HEADER */
          <div className="apps-header-row">
          <div className="search-bar-container">
            <SearchIcon className="search-icon" />
            <input 
              type="text" 
              placeholder="Search..." 
              value={filters.searchQuery}
              onChange={(e) => setFilters(prev => ({ ...prev, searchQuery: e.target.value }))}
              onFocus={() => setIsSearchFocused(true)}
              onBlur={() => setIsSearchFocused(false)}
              className="search-input"
            />
            {filters.searchQuery && (
              <button 
                onClick={() => setFilters(prev => ({ ...prev, searchQuery: '' }))} 
                className="job-card-action-btn"
                style={{ padding: '4px' }}
              >
                <CloseIcon style={{ width: '18px', height: '18px' }} />
              </button>
            )}
          </div>
        </div>
      )}

      {/* FILTER CHIPS (Only in normal mode) */}
      {!isSelectionMode && (
        <>
          <div className="filter-chips-scroll" ref={chipContainerRef}>
            {['All', 'Applied', 'Interview', 'Offer', 'Rejected', 'Saved', 'Response', 'Resume', 'Platform', 'Date'].map(chip => (
              <button
                key={chip}
                onClick={() => handleStatusFilterClick(chip)}
                className={`filter-chip ${filters.statusFilter === chip ? 'active' : ''}`}
              >
                {chip}
              </button>
            ))}
          </div>

          {/* SUB-FILTER PANEL: PLATFORM */}
          {filters.statusFilter === 'Platform' && (
            <div className="sub-filter-panel animate-fade-in" style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <div className="sub-filter-group" style={{ flex: 1 }}>
                <span className="sub-filter-label">Select Platform</span>
                <select
                  className="sub-filter-select"
                  value={filters.selectedPlatform}
                  onChange={(e) => setFilters(prev => ({ ...prev, selectedPlatform: e.target.value }))}
                >
                  <option value="LinkedIn">LinkedIn</option>
                  <option value="Indeed">Indeed</option>
                  <option value="Email">Email</option>
                  <option value="Website">Website</option>
                  <option value="Other">Other</option>
                </select>
              </div>
            </div>
          )}

          {/* SUB-FILTER PANEL: RESUME */}
          {filters.statusFilter === 'Resume' && (
            <div className="sub-filter-panel animate-fade-in" style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <div className="sub-filter-group" style={{ flex: 1 }}>
                <span className="sub-filter-label">Select Resume / CV</span>
                <select
                  className="sub-filter-select"
                  value={filters.selectedResume}
                  onChange={(e) => setFilters(prev => ({ ...prev, selectedResume: e.target.value }))}
                >
                  <option value="Select---">Select---</option>
                  {uniqueResumes.map(r => {
                    const displayName = r.replace(/\.(pdf|docx|doc)$/i, '');
                    return <option key={r} value={r}>{displayName}</option>;
                  })}
                </select>
              </div>
            </div>
          )}

          {/* SUB-FILTER PANEL: DATE */}
          {filters.statusFilter === 'Date' && (
            <div className="sub-filter-panel animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              
              {/* Filter Type Chips and Info button */}
              <div className="date-filter-type-row">
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexShrink: 0 }}>
                  <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Filter Type:</span>
                  <button 
                    type="button"
                    onClick={() => setShowDateInfoModal(true)}
                    className="job-card-action-btn"
                    style={{ padding: '2px', color: 'var(--brand-primary)', cursor: 'pointer' }}
                    title="Date filtering info"
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                    </svg>
                  </button>
                </div>
                
                <div className="date-filter-toggle-scroll">
                  <button
                    type="button"
                    onClick={() => setFilters(prev => ({
                      ...prev,
                      dateFilterMode: 'Month',
                      dateMonth: (new Date().getMonth() + 1).toString(),
                      dateYear: new Date().getFullYear().toString()
                    }))}
                    className={`filter-chip ${filters.dateFilterMode === 'Month' ? 'active' : ''}`}
                    style={{ fontSize: '0.75rem', padding: '6px 12px' }}
                  >
                    Month
                  </button>
                  <button
                    type="button"
                    onClick={() => setFilters(prev => ({
                      ...prev,
                      dateFilterMode: 'Day',
                      dateSpecificDay: getLocalDateString()
                    }))}
                    className={`filter-chip ${filters.dateFilterMode === 'Day' ? 'active' : ''}`}
                    style={{ fontSize: '0.75rem', padding: '6px 12px' }}
                  >
                    Specific Day
                  </button>
                  <button
                    type="button"
                    onClick={() => setFilters(prev => ({
                      ...prev,
                      dateFilterMode: 'Range',
                      dateStartRange: getLocalFirstOfMonth(),
                      dateEndRange: getLocalDateString()
                    }))}
                    className={`filter-chip ${filters.dateFilterMode === 'Range' ? 'active' : ''}`}
                    style={{ fontSize: '0.75rem', padding: '6px 12px' }}
                  >
                    Date Range
                  </button>
                </div>
              </div>

              {/* Sub-filter Month inputs */}
              {filters.dateFilterMode === 'Month' && (
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                  <div className="sub-filter-group" style={{ flex: 1 }}>
                    <span className="sub-filter-label">Select Month</span>
                    <select
                      className="sub-filter-select"
                      value={filters.dateMonth}
                      onChange={(e) => setFilters(prev => ({ ...prev, dateMonth: e.target.value }))}
                    >
                      <option value="1">January</option>
                      <option value="2">February</option>
                      <option value="3">March</option>
                      <option value="4">April</option>
                      <option value="5">May</option>
                      <option value="6">June</option>
                      <option value="7">July</option>
                      <option value="8">August</option>
                      <option value="9">September</option>
                      <option value="10">October</option>
                      <option value="11">November</option>
                      <option value="12">December</option>
                    </select>
                  </div>
                  
                  <div className="sub-filter-group" style={{ width: '100px' }}>
                    <span className="sub-filter-label">Year</span>
                    <input
                      type="number"
                      className="form-input"
                      style={{ padding: '6px 10px', fontSize: '0.85rem' }}
                      value={filters.dateYear}
                      onChange={(e) => {
                        const value = e.target.value;
                        if (/^\d*$/.test(value) && value.length <= 4) {
                          setFilters(prev => ({ ...prev, dateYear: value }));
                        }
                      }}
                    />
                  </div>
                </div>
              )}

              {/* Sub-filter Day inputs */}
              {filters.dateFilterMode === 'Day' && (
                <div className="sub-filter-group">
                  <span className="sub-filter-label">Selected Date</span>
                  <input
                    type="date"
                    className="form-input"
                    style={{ padding: '6px 10px', fontSize: '0.85rem', width: '100%' }}
                    value={filters.dateSpecificDay}
                    onChange={(e) => setFilters(prev => ({ ...prev, dateSpecificDay: e.target.value }))}
                  />
                </div>
              )}

              {/* Sub-filter Range inputs */}
              {filters.dateFilterMode === 'Range' && (
                <div className="date-filter-grid">
                  <div className="sub-filter-group">
                    <span className="sub-filter-label">Start Date</span>
                    <input
                      type="date"
                      className="form-input"
                      style={{ padding: '6px 8px', fontSize: '0.85rem', width: '100%', minWidth: 0, boxSizing: 'border-box' }}
                      value={filters.dateStartRange}
                      onChange={(e) => setFilters(prev => ({ ...prev, dateStartRange: e.target.value }))}
                    />
                  </div>
                  
                  <div className="sub-filter-group">
                    <span className="sub-filter-label">End Date</span>
                    <input
                      type="date"
                      className="form-input"
                      style={{ padding: '6px 8px', fontSize: '0.85rem', width: '100%', minWidth: 0, boxSizing: 'border-box' }}
                      value={filters.dateEndRange}
                      onChange={(e) => setFilters(prev => ({ ...prev, dateEndRange: e.target.value }))}
                    />
                  </div>
                </div>
              )}

            </div>
          )}
          
          {/* SORTING INFO ROW */}
          <div className="sort-info-row">
            <span className="sort-info-text">
              Showing {filteredApps.length} of {applications.length}
            </span>
            <div style={{ position: 'relative' }}>
              <button onClick={() => setShowSortMenu(!showSortMenu)} className="sort-trigger-btn">
                <span>Sort: {SORT_OPTIONS[sortOption]}</span>
                <ChevronIcon direction={showSortMenu ? 'up' : 'down'} style={{ width: '14px', height: '14px' }} />
              </button>
              
              {showSortMenu && (
                <div 
                  className="card-base animate-scale-in" 
                  style={{
                    position: 'absolute',
                    top: '26px',
                    right: 0,
                    zIndex: 150,
                    width: '200px',
                    padding: '8px 0',
                    display: 'flex',
                    flexDirection: 'column'
                  }}
                >
                  {Object.entries(SORT_OPTIONS).map(([key, label]) => (
                    <button
                      key={key}
                      onClick={() => {
                        setSortOption(key);
                        setShowSortMenu(false);
                      }}
                      style={{
                        padding: '10px 16px',
                        textAlign: 'left',
                        background: 'transparent',
                        border: 'none',
                        fontSize: '0.85rem',
                        fontWeight: 600,
                        cursor: 'pointer',
                        color: sortOption === key ? 'var(--brand-primary)' : 'var(--text-secondary)',
                        backgroundColor: sortOption === key ? 'var(--bg-surface-variant)' : 'transparent'
                      }}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}
      </div>

      {/* APPLICATIONS LIST */}
      {filteredApps.length > 0 ? (
        <div className="job-cards-list">
          {filteredApps.map((app) => {
            const styles = getStatusColorClass(app.status);
            const isSelected = selectedIds.includes(app.id);

            return (
              <div 
                key={app.id} 
                className={`job-card card-base card-interactive ${isSelected ? 'selected' : ''}`}
                onClick={() => handleJobCardClick(app)}
                onContextMenu={(e) => {
                  e.preventDefault();
                  handleEnterSelectionMode(app.id);
                }}
                style={{
                  borderColor: isSelected ? 'var(--brand-primary)' : 'var(--brand-outline)',
                  backgroundColor: isSelected ? 'var(--bg-surface-variant)' : 'var(--bg-surface)'
                }}
              >
                {/* Checkbox visible in selection mode */}
                {isSelectionMode && (
                  <input 
                    type="checkbox" 
                    checked={isSelected}
                    onChange={() => handleToggleSelect(app.id)}
                    className="job-card-select-checkbox"
                    onClick={(e) => e.stopPropagation()} // stop bubbling to card click
                  />
                )}

                <div className="job-card-main">
                  <div className="job-card-header">
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <h4 className="job-card-role">{app.role || 'Position unassigned'}</h4>
                      <span className="job-card-company">{app.companyName || 'Unknown Company'}</span>
                    </div>
                    <span 
                      className="status-badge"
                      style={{ color: styles.color, backgroundColor: styles.bg }}
                    >
                      {app.status}
                    </span>
                  </div>

                  <div className="job-card-footer">
                    <div className="job-card-details">
                      <div className="job-card-detail-item">
                        <CalendarIcon />
                        <span>{getFormattedStatusDate(app)}</span>
                      </div>
                    </div>

                    {/* Normal Mode Quick Actions */}
                    {!isSelectionMode && (
                      <div className="job-card-actions">
                        <button 
                          onClick={(e) => handleEditClick(e, app)} 
                          className="job-card-action-btn"
                          title="Edit"
                        >
                          <EditIcon />
                        </button>
                        <button 
                          onClick={(e) => handleDeleteSingle(e, app)} 
                          className="job-card-action-btn delete"
                          title="Delete"
                        >
                          <DeleteIcon />
                        </button>
                      </div>
                    )}
                  </div>

                  {((app.platform) || (app.resume && app.resume.originalName)) && (
                    <>
                      <hr className="job-card-divider" />
                      <div className="job-card-extra-row">
                        {app.platform && (
                          <div className="job-card-detail-item">
                            <LinkIcon />
                            <span>Platform: {app.platform}</span>
                          </div>
                        )}
                        {app.resume && app.resume.originalName && (
                          <div className="job-card-detail-item" style={{ flex: 1, minWidth: '120px' }}>
                            <FileIcon style={{ flexShrink: 0 }} />
                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', minWidth: 0, flex: 1 }} title={app.resume.originalName}>
                              CV: {app.resume.originalName}
                            </span>
                          </div>
                        )}
                      </div>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* EMPTY STATE SCREEN */
        <div className="card-base empty-state-card">
          {(() => {
            const isSearchOrFilterActive = filters.searchQuery || filters.statusFilter !== 'All';
            const isResumeStatsEmpty = uniqueResumes.length === 0;

            if (filters.statusFilter === 'Resume' && applications.length > 0 && isResumeStatsEmpty) {
              return (
                <>
                  <FileIcon className="empty-state-icon" />
                  <h4 className="empty-state-title">No resumes found</h4>
                  <p className="empty-state-text">
                    Attach a CV/resume (PDF) to your applications to track and filter them.
                  </p>
                </>
              );
            }

            if (filters.statusFilter === 'Resume' && applications.length > 0 && filters.selectedResume === 'Select---') {
              return (
                <>
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="currentColor" className="empty-state-icon" style={{ opacity: 0.3, color: 'var(--brand-primary)' }}>
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                  </svg>
                  <h4 className="empty-state-title">Select a Resume</h4>
                  <p className="empty-state-text">
                    Choose a resume from the dropdown above to filter your applications.
                  </p>
                </>
              );
            }

            if (isSearchOrFilterActive) {
              return (
                <>
                  <SearchIcon className="empty-state-icon" style={{ width: '48px', height: '48px', opacity: 0.3, color: 'var(--brand-primary)' }} />
                  <h4 className="empty-state-title">No results found</h4>
                  <p className="empty-state-text">
                    No applications match your current search terms or active filters. Try adjusting them!
                  </p>
                </>
              );
            }

            return (
              <>
                <FileIcon className="empty-state-icon" />
                <h4 className="empty-state-title">No applications saved yet</h4>
                <p className="empty-state-text">
                  Tap the '+' button in the bottom right corner to start!
                </p>
              </>
            );
          })()}
          
          {(filters.searchQuery || filters.statusFilter !== 'All') && (
            <button 
              onClick={() => setFilters({
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
              })} 
              className="btn-secondary"
              style={{ marginTop: '12px' }}
            >
              Clear All Filters
            </button>
          )}
        </div>
      )}

    </div>

    {/* Date Filtering Info Dialog Modal */}
    {showDateInfoModal && createPortal(
      <div className="modal-overlay" style={{ zIndex: 2000 }} onClick={() => setShowDateInfoModal(false)}>
        <div className="modal-content-card" style={{ maxWidth: '400px' }} onClick={(e) => e.stopPropagation()}>
          <h3 className="modal-title" style={{ margin: 0 }}>How Date Filtering Works</h3>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', margin: '4px 0' }}></div>
          
          <p className="modal-text" style={{ fontSize: '0.9rem', lineHeight: '1.6', color: 'var(--text-primary)' }}>
            To help you track active timelines, date filters match the date of your most recent status change (such as when you originally applied, or when the role moved to Interview or Offer).
          </p>
          <p className="modal-text" style={{ fontSize: '0.8rem', fontStyle: 'italic', color: 'var(--text-secondary)' }}>
            Note: This can differ from the Dashboard, which counts application volumes strictly based on the date they were first added to the app.
          </p>
          
          <div className="modal-actions" style={{ marginTop: '8px' }}>
            <button 
              onClick={() => setShowDateInfoModal(false)} 
              className="btn-primary" 
              style={{ padding: '6px 16px', fontSize: '0.85rem' }}
            >
              Got it
            </button>
          </div>
        </div>
      </div>,
      document.body
    )}

    {/* Delete Single Application Modal */}
    {appToDelete && createPortal(
      <ConfirmationModal 
        title="Delete Application"
        message="Are you sure you want to delete this job application?"
        confirmLabel="Delete"
        isDestructive={true}
        onConfirm={() => handleConfirmDeleteSingle(appToDelete)}
        onCancel={() => setAppToDelete(null)}
      />,
      document.body
    )}

    {/* Delete Multiple Applications Modal */}
    {appsToDeleteList.length > 0 && createPortal(
      <ConfirmationModal 
        title="Delete Applications"
        message={
          appsToDeleteList.length === 1 
            ? "Are you sure you want to delete this job application?" 
            : `Are you sure you want to delete these ${appsToDeleteList.length} job applications?`
        }
        confirmLabel="Delete"
        isDestructive={true}
        onConfirm={() => handleConfirmDeleteList(appsToDeleteList)}
        onCancel={() => setAppsToDeleteList([])}
      />,
      document.body
    )}

    {/* FLOATING ACTION BUTTON (FAB) */}
    {!isSelectionMode && (
      <button 
        onClick={() => {
          setSelectedJobId(null);
          setActiveTab('add-job');
        }} 
        className={`fab-btn ${(isFabVisible && !isSearchFocused) ? '' : 'fab-hidden'}`}
        title="Add Job Application"
      >
        <AddIcon />
      </button>
    )}

    {/* SELECTION BOTTOM BAR */}
    {isSelectionMode && (
      <div className="selection-bottom-bar animate-fade-in">
        <div className="selection-bottom-content">
          <button 
            onClick={handleSelectAllToggle} 
            className="selection-bottom-btn select-all-toggle"
          >
            {allSelected ? <DeselectAllIcon /> : <SelectAllIcon />}
            <span>{allSelected ? 'Deselect All' : 'Select All'}</span>
          </button>
          
          <button 
            onClick={handleDeleteSelected} 
            className="selection-bottom-btn delete-btn"
            disabled={selectedIds.length === 0}
          >
            <DeleteIcon />
            <span>Delete</span>
          </button>
        </div>
      </div>
    )}
  </>
);
}
