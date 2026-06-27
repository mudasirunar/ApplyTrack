import React, { useState, useEffect, useRef } from 'react';
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
  CheckIcon
} from '../components/Icons';
import './Applications.css';

export default function Applications({ filters, setFilters, setActiveTab, setSelectedJobId }) {
  const [applications, setApplications] = useState(db.getApplications());
  const [analytics, setAnalytics] = useState(db.getAnalytics());
  const [isFabVisible, setIsFabVisible] = useState(true);
  const [showSortMenu, setShowSortMenu] = useState(false);
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  
  // Selection mode state
  const [isSelectionMode, setIsSelectionMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);

  const lastScrollY = useRef(0);
  const scrollContainerRef = useRef(null);

  // Sorting options
  const SORT_OPTIONS = {
    STATUS_LATEST: 'Status Date: Latest first',
    STATUS_OLDEST: 'Status Date: Oldest first',
    CREATION_LATEST: 'Creation Date: Latest first',
    CREATION_OLDEST: 'Creation Date: Oldest first'
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

  // Filter application list based on active filters
  const getFilteredApps = () => {
    let list = [...applications];

    // 1. Search Query
    if (filters.searchQuery) {
      const query = filters.searchQuery.toLowerCase();
      list = list.filter(a => 
        (a.companyName && a.companyName.toLowerCase().includes(query)) ||
        (a.role && a.role.toLowerCase().includes(query)) ||
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
    }

    // 3. Sub-filter: Platform
    if (filters.statusFilter === 'Platform' && filters.selectedPlatform !== 'All') {
      list = list.filter(a => a.platform === filters.selectedPlatform);
    }

    // 4. Sub-filter: Resume
    if (filters.statusFilter === 'Resume' && filters.selectedResume !== 'All') {
      list = list.filter(a => a.resume && a.resume.originalName === filters.selectedResume);
    }

    // 5. Sub-filter: Date
    if (filters.statusFilter === 'Date' && filters.dateFilterMode === 'Month') {
      if (filters.dateMonth && filters.dateYear) {
        list = list.filter(a => {
          const date = new Date(a.createdAt);
          const monthStr = date.toLocaleString('default', { month: 'short' });
          const yearStr = date.getFullYear().toString();
          return monthStr === filters.dateMonth && yearStr === filters.dateYear.toString();
        });
      }
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

  // Filter actions
  const handleStatusFilterClick = (status) => {
    setFilters(prev => ({
      ...prev,
      statusFilter: status
    }));
    if (status === 'All') {
      setSortOption('STATUS_LATEST');
    }
  };

  const handleToggleSelect = (id) => {
    setSelectedIds(prev => 
      prev.includes(id) ? prev.filter(item => item !== id) : [...prev, id]
    );
  };

  const handleSelectAllToggle = () => {
    if (selectedIds.length === displayedIds.length) {
      setSelectedIds([]);
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
    if (window.confirm(`Delete job application for ${app.companyName}?`)) {
      db.deleteApplication(app.id);
      
      // Trigger toast with undo
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: {
          message: `'${app.role || 'Application'}' deleted`,
          action: 'Undo',
          onAction: () => {
            db.undoDelete();
            window.dispatchEvent(new CustomEvent('applytrack_toast', { detail: { message: 'Restored application' } }));
          }
        }
      }));
    }
  };

  const handleDeleteSelected = () => {
    if (selectedIds.length === 0) return;

    if (window.confirm(`Delete ${selectedIds.length} selected applications?`)) {
      const count = selectedIds.length;
      db.deleteMultipleApplications(selectedIds);
      handleExitSelectionMode();

      // Trigger toast with undo
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: {
          message: `${count} applications deleted`,
          action: 'Undo',
          onAction: () => {
            db.undoDelete();
            window.dispatchEvent(new CustomEvent('applytrack_toast', { detail: { message: 'Restored applications' } }));
          }
        }
      }));
    }
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
      
      {/* SELECTION MODE TOP BAR */}
      {isSelectionMode ? (
        <div className="selection-mode-bar">
          <div className="selection-bar-left">
            <button onClick={handleExitSelectionMode} className="selection-bar-btn" title="Exit selection">
              <CloseIcon />
            </button>
            <span>{selectedIds.length} Selected</span>
          </div>
          <div className="selection-bar-actions">
            <span onClick={handleSelectAllToggle} className="selection-action-text">
              {selectedIds.length === displayedIds.length ? 'Deselect All' : 'Select All'}
            </span>
            <button onClick={handleDeleteSelected} className="selection-bar-btn" title="Delete selected">
              <DeleteIcon />
            </button>
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
          <div className="filter-chips-scroll">
            {['All', 'Applied', 'Interview', 'Offer', 'Rejected', 'Saved', 'Platform', 'Resume', 'Date'].map(chip => (
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
            <div className="sub-filter-panel animate-fade-in">
              <div className="sub-filter-group">
                <span className="sub-filter-label">Select Platform</span>
                <select
                  className="sub-filter-select"
                  value={filters.selectedPlatform}
                  onChange={(e) => setFilters(prev => ({ ...prev, selectedPlatform: e.target.value }))}
                >
                  <option value="All">All Platforms</option>
                  {uniquePlatforms.map(p => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* SUB-FILTER PANEL: RESUME */}
          {filters.statusFilter === 'Resume' && (
            <div className="sub-filter-panel animate-fade-in">
              <div className="sub-filter-group">
                <span className="sub-filter-label">Select Resume Used</span>
                <select
                  className="sub-filter-select"
                  value={filters.selectedResume}
                  onChange={(e) => setFilters(prev => ({ ...prev, selectedResume: e.target.value }))}
                >
                  <option value="All">All Resumes</option>
                  {uniqueResumes.map(r => (
                    <option key={r} value={r}>{r}</option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* SUB-FILTER PANEL: DATE */}
          {filters.statusFilter === 'Date' && (
            <div className="sub-filter-panel animate-fade-in">
              <div className="sub-filter-group">
                <span className="sub-filter-label">Filter by Month</span>
                <select
                  className="sub-filter-select"
                  value={filters.dateMonth && filters.dateYear ? `${filters.dateMonth}-${filters.dateYear}` : 'All'}
                  onChange={(e) => {
                    if (e.target.value === 'All') {
                      setFilters(prev => ({ ...prev, dateFilterMode: 'All', dateMonth: '', dateYear: '' }));
                    } else {
                      const [month, year] = e.target.value.split('-');
                      setFilters(prev => ({
                        ...prev,
                        dateFilterMode: 'Month',
                        dateMonth: month,
                        dateYear: year
                      }));
                    }
                  }}
                >
                  <option value="All">All Time</option>
                  {uniqueMonthsAndYears.map(({ month, year }, idx) => (
                    <option key={idx} value={`${month}-${year}`}>{month} {year}</option>
                  ))}
                </select>
              </div>
            </div>
          )}
        </>
      )}

      {/* SORTING INFO ROW */}
      <div className="sort-info-row">
        <span className="sort-info-text">
          Showing {filteredApps.length} of {applications.length} applications
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
                    <div>
                      <h4 className="job-card-company">{app.companyName}</h4>
                      <span className="job-card-role">{app.role}</span>
                    </div>
                    <span 
                      className="status-badge"
                      style={{ color: styles.color, backgroundColor: styles.bg }}
                    >
                      {app.status}
                    </span>
                  </div>

                  <div className="job-card-details">
                    <div className="job-card-detail-item">
                      <CalendarIcon />
                      <span>{new Date(app.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                    </div>
                    {app.platform && (
                      <div className="job-card-detail-item">
                        <LinkIcon />
                        <span>{app.platform}</span>
                      </div>
                    )}
                    {app.resume && app.resume.originalName && (
                      <div className="job-card-detail-item">
                        <FileIcon />
                        <span style={{ maxWidth: '140px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {app.resume.originalName}
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Normal Mode Quick Actions */}
                {!isSelectionMode && (
                  <div className="job-card-actions" style={{ flexDirection: 'column', justifyContent: 'center' }}>
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
            );
          })}
        </div>
      ) : (
        /* EMPTY STATE SCREEN */
        <div className="card-base empty-state-card">
          <FileIcon className="empty-state-icon" />
          <h4 className="empty-state-title">No applications found</h4>
          <p className="empty-state-text">
            {applications.length === 0 
              ? 'You haven\'t tracked any applications yet. Tap the button below to add one!' 
              : 'No applications match your active search queries or filters.'}
          </p>
          {applications.length > 0 && (
            <button 
              onClick={() => setFilters({
                searchQuery: '',
                statusFilter: 'All',
                selectedResume: 'All',
                selectedPlatform: 'All',
                dateFilterMode: 'All',
                dateMonth: '',
                dateYear: ''
              })} 
              className="btn-secondary"
            >
              Clear All Filters
            </button>
          )}
        </div>
      )}

    </div>

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
  </>
);
}
