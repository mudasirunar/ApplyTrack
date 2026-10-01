import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { db } from '../utils/db';
import { useBodyScrollLock } from '../utils/useBodyScrollLock';
import { 
  getFormattedStatusDate,
  getLocalDateString,
  getLocalFirstOfMonth,
  ymdToDmy,
  parseLocalDate,
  dmyToYmd
} from '../utils/dateUtils';
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


function DatePickerField({ value, onChange, min, max, placeholder = "dd/mm/yyyy", className = "form-input", style = {} }) {
  const [tempText, setTempText] = useState('');

  useEffect(() => {
    if (value) {
      setTempText(ymdToDmy(value));
    } else {
      setTempText('');
    }
  }, [value]);

  const handleTextChange = (e) => {
    let inputVal = e.target.value;
    
    // Auto-format dd/mm/yyyy as they type
    let clean = inputVal.replace(/[^0-9]/g, '');
    if (clean.length > 8) clean = clean.substring(0, 8);
    
    let formatted = '';
    if (clean.length > 0) {
      formatted += clean.substring(0, 2);
    }
    if (clean.length > 2) {
      formatted += '/' + clean.substring(2, 4);
    }
    if (clean.length > 4) {
      formatted += '/' + clean.substring(4, 8);
    }
    
    setTempText(formatted);

    const ymd = dmyToYmd(formatted);
    if (ymd) {
      onChange(ymd);
    } else if (formatted === '') {
      onChange('');
    }
  };

  const handleNativeChange = (e) => {
    const ymd = e.target.value;
    onChange(ymd);
  };

  return (
    <div style={{ position: 'relative', display: 'flex', alignItems: 'center', width: '100%' }}>
      <input
        type="text"
        placeholder={placeholder}
        className={className}
        style={{ ...style, width: '100%', paddingRight: '40px', boxSizing: 'border-box' }}
        value={tempText}
        onChange={handleTextChange}
      />
      
      <div 
        style={{ 
          position: 'absolute', 
          right: '12px', 
          top: '50%', 
          transform: 'translateY(-50%)', 
          width: '20px', 
          height: '20px', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center', 
          color: 'var(--text-secondary)',
          pointerEvents: 'none',
          opacity: 0.7
        }}
      >
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
          <path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2zm-7 3h5v5h-5z"/>
        </svg>
      </div>

      <input
        type="date"
        value={value || ''}
        min={min || undefined}
        max={max || undefined}
        onChange={handleNativeChange}
        style={{
          position: 'absolute',
          right: '8px',
          top: '50%',
          transform: 'translateY(-50%)',
          width: '28px',
          height: '28px',
          opacity: 0,
          cursor: 'pointer'
        }}
      />
    </div>
  );
}

function ConfirmationModal({ title, message, confirmLabel, isDestructive, onConfirm, onCancel }) {
  useBodyScrollLock(true);

  const modalContent = (
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

  return createPortal(modalContent, document.body);
}

function DateFilterModal({ isOpen, onClose, dateDraft, setDateDraft, onApply }) {
  useBodyScrollLock(isOpen);

  if (!isOpen || !dateDraft) return null;

  const modalContent = (
    <div className="modal-overlay date-filter-overlay" onClick={onClose}>
      <div 
        className="modal-content-card date-filter-sheet-card animate-scale-in" 
        style={{ maxWidth: '440px' }} 
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sheet-drag-handle" />

        <div className="date-modal-header">
          <h3 className="modal-title" style={{ margin: 0, color: 'var(--brand-primary)', fontSize: '1.1rem', fontWeight: 800 }}>
            Filter by Date
          </h3>
          <button 
            type="button" 
            className="date-modal-close-btn" 
            onClick={onClose}
            title="Close"
          >
            <CloseIcon style={{ width: '18px', height: '18px' }} />
          </button>
        </div>
        <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', margin: '10px 0 16px' }} />

        {/* Date Basis Section */}
        <div className="date-modal-section">
          <span className="date-modal-section-label">Date Basis</span>
          <div className="date-filter-toggle-scroll">
            <button
              type="button"
              onClick={() => setDateDraft(prev => ({ ...prev, dateBasis: 'created' }))}
              className={`filter-chip ${(!dateDraft.dateBasis || dateDraft.dateBasis === 'created') ? 'active' : ''}`}
            >
              Date Added
            </button>
            <button
              type="button"
              onClick={() => setDateDraft(prev => ({ ...prev, dateBasis: 'status' }))}
              className={`filter-chip ${dateDraft.dateBasis === 'status' ? 'active' : ''}`}
            >
              Status Updated
            </button>
          </div>
        </div>

        {/* Filter Type Section */}
        <div className="date-modal-section">
          <span className="date-modal-section-label">Filter Type</span>
          <div className="date-filter-toggle-scroll">
            <button
              type="button"
              onClick={() => setDateDraft(prev => ({
                ...prev,
                dateFilterMode: 'Month',
                dateMonth: prev.dateMonth || (new Date().getMonth() + 1).toString(),
                dateYear: prev.dateYear || new Date().getFullYear().toString()
              }))}
              className={`filter-chip ${(!dateDraft.dateFilterMode || dateDraft.dateFilterMode === 'Month') ? 'active' : ''}`}
            >
              Month
            </button>
            <button
              type="button"
              onClick={() => setDateDraft(prev => ({
                ...prev,
                dateFilterMode: 'Day',
                dateSpecificDay: prev.dateSpecificDay || getLocalDateString()
              }))}
              className={`filter-chip ${dateDraft.dateFilterMode === 'Day' ? 'active' : ''}`}
            >
              Specific Day
            </button>
            <button
              type="button"
              onClick={() => setDateDraft(prev => ({
                ...prev,
                dateFilterMode: 'Range',
                dateStartRange: prev.dateStartRange || getLocalFirstOfMonth(),
                dateEndRange: prev.dateEndRange || getLocalDateString()
              }))}
              className={`filter-chip ${dateDraft.dateFilterMode === 'Range' ? 'active' : ''}`}
            >
              Date Range
            </button>
          </div>
        </div>

        {/* Active Controls */}
        <div className="date-modal-controls">
          {(!dateDraft.dateFilterMode || dateDraft.dateFilterMode === 'Month') && (
            <div className="date-modal-inputs-row">
              <div className="sub-filter-group" style={{ flex: 2 }}>
                <span className="sub-filter-label">Select Month</span>
                <select
                  className="sub-filter-select"
                  value={dateDraft.dateMonth}
                  onChange={(e) => setDateDraft(prev => ({ ...prev, dateMonth: e.target.value }))}
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
              
              <div className="sub-filter-group" style={{ flex: 1 }}>
                <span className="sub-filter-label">Year</span>
                <input
                  type="number"
                  className="form-input"
                  style={{ padding: '7px 10px', fontSize: '0.85rem' }}
                  value={dateDraft.dateYear}
                  onChange={(e) => {
                    const value = e.target.value;
                    if (/^\d*$/.test(value) && value.length <= 4) {
                      setDateDraft(prev => ({ ...prev, dateYear: value }));
                    }
                  }}
                />
              </div>
            </div>
          )}

          {dateDraft.dateFilterMode === 'Day' && (
            <div className="sub-filter-group">
              <span className="sub-filter-label">Selected Date</span>
              <DatePickerField
                value={dateDraft.dateSpecificDay}
                onChange={(val) => setDateDraft(prev => ({ ...prev, dateSpecificDay: val }))}
                className="form-input"
                style={{ padding: '7px 10px', fontSize: '0.85rem' }}
              />
            </div>
          )}

          {dateDraft.dateFilterMode === 'Range' && (
            <div className="date-modal-inputs-row">
              <div className="sub-filter-group" style={{ flex: 1 }}>
                <span className="sub-filter-label">Start Date</span>
                <DatePickerField
                  value={dateDraft.dateStartRange}
                  max={dateDraft.dateEndRange}
                  onChange={(val) => {
                    setDateDraft(prev => {
                      const updates = { ...prev, dateStartRange: val };
                      if (val && prev.dateEndRange && val > prev.dateEndRange) {
                        updates.dateEndRange = val;
                      }
                      return updates;
                    });
                  }}
                  className="form-input"
                  style={{ padding: '7px 8px', fontSize: '0.85rem' }}
                />
              </div>
              
              <div className="sub-filter-group" style={{ flex: 1 }}>
                <span className="sub-filter-label">End Date</span>
                <DatePickerField
                  value={dateDraft.dateEndRange}
                  min={dateDraft.dateStartRange}
                  onChange={(val) => {
                    setDateDraft(prev => {
                      const updates = { ...prev, dateEndRange: val };
                      if (val && prev.dateStartRange && val < prev.dateStartRange) {
                        updates.dateStartRange = val;
                      }
                      return updates;
                    });
                  }}
                  className="form-input"
                  style={{ padding: '7px 8px', fontSize: '0.85rem' }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Modal Actions */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '18px' }}>
          <button 
            type="button" 
            onClick={onClose} 
            className="btn-secondary" 
            style={{ padding: '8px 16px', fontSize: '0.85rem' }}
          >
            Cancel
          </button>
          <button 
            type="button" 
            onClick={onApply} 
            className="btn-primary" 
            style={{ padding: '8px 16px', fontSize: '0.85rem' }}
          >
            Apply Filter
          </button>
        </div>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
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
  const [isDateModalOpen, setIsDateModalOpen] = useState(false);
  const [dateDraft, setDateDraft] = useState(null);
  
  const [appToDelete, setAppToDelete] = useState(null);
  const [appsToDeleteList, setAppsToDeleteList] = useState([]);
  const [deletingIds, setDeletingIds] = useState([]);

  const handleOpenDateModal = () => {
    setDateDraft({
      dateBasis: filters.dateBasis || 'created',
      dateFilterMode: filters.dateFilterMode || 'Month',
      dateMonth: filters.dateMonth || (new Date().getMonth() + 1).toString(),
      dateYear: filters.dateYear || new Date().getFullYear().toString(),
      dateSpecificDay: filters.dateSpecificDay || getLocalDateString(),
      dateStartRange: filters.dateStartRange || getLocalFirstOfMonth(),
      dateEndRange: filters.dateEndRange || getLocalDateString()
    });
    setIsDateModalOpen(true);
  };

  const handleApplyDateModal = () => {
    if (dateDraft) {
      const finalDraft = { ...dateDraft };
      if (finalDraft.dateFilterMode === 'Range') {
        if (finalDraft.dateStartRange && finalDraft.dateEndRange && finalDraft.dateStartRange > finalDraft.dateEndRange) {
          finalDraft.dateEndRange = finalDraft.dateStartRange;
        }
      }
      setFilters(prev => ({
        ...prev,
        ...finalDraft
      }));
    }
    setIsDateModalOpen(false);
  };



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
    const query = filters.searchQuery ? filters.searchQuery.trim().toLowerCase() : '';
    if (query) {
      list = list.filter(a => 
        ((a.role || 'Position unassigned').toLowerCase().includes(query)) ||
        ((a.companyName || 'Unknown Company').toLowerCase().includes(query)) ||
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
      const standardPlatforms = ['LinkedIn', 'Indeed', 'Email', 'Website'];
      if (filters.selectedPlatform === 'Other') {
        list = list.filter(a => {
          const plat = a.platform ? a.platform.trim() : '';
          return !standardPlatforms.some(sp => sp.toLowerCase() === plat.toLowerCase());
        });
      } else {
        list = list.filter(a => {
          const plat = a.platform ? a.platform.trim() : '';
          return plat.toLowerCase() === filters.selectedPlatform.toLowerCase();
        });
      }
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
      const isStatusBasis = filters.dateBasis === 'status';
      list = list.filter(a => {
        const targetTimestamp = isStatusBasis
          ? ((a.statusHistory && a.statusHistory.length > 0) ? a.statusHistory[a.statusHistory.length - 1].timestamp : a.createdAt)
          : a.createdAt;
        const date = new Date(targetTimestamp);

        if (filters.dateFilterMode === 'Month') {
          const appMonth = date.getMonth() + 1; // 1..12
          const appYear = date.getFullYear().toString();
          return appMonth === Number(filters.dateMonth) && appYear === filters.dateYear;
        }

        if (filters.dateFilterMode === 'Day') {
          if (!filters.dateSpecificDay) return true;
          const targetDate = parseLocalDate(filters.dateSpecificDay);
          return targetDate &&
                 date.getFullYear() === targetDate.getFullYear() &&
                 date.getMonth() === targetDate.getMonth() &&
                 date.getDate() === targetDate.getDate();
        }

        if (filters.dateFilterMode === 'Range') {
          const startTarget = parseLocalDate(filters.dateStartRange);
          const startTimestamp = startTarget ? startTarget.setHours(0, 0, 0, 0) : 0;
          const endTarget = parseLocalDate(filters.dateEndRange);
          const endTimestamp = endTarget ? endTarget.setHours(23, 59, 59, 999) : Infinity;
          return targetTimestamp >= startTimestamp && targetTimestamp <= endTimestamp;
        }

        return true;
      });
    }

    // Sort list or Apply Search Relevance Ranking
    if (query) {
      const WORD_DELIMITER_REGEX = /[\s/\-,()[\]_.:]+/;

      const startsWithQuery = (text) => {
        if (!text) return false;
        return text.trim().toLowerCase().startsWith(query);
      };

      const wordStartsWithQuery = (text) => {
        if (!text) return false;
        const words = text.split(WORD_DELIMITER_REGEX);
        return words.some(w => w.toLowerCase().startsWith(query));
      };

      const containsQuery = (text) => {
        if (!text) return false;
        return text.toLowerCase().includes(query);
      };

      const getTier = (app) => {
        const role = app.role || '';
        const company = app.companyName || '';
        if (startsWithQuery(role)) return 1;
        if (wordStartsWithQuery(role)) return 2;
        if (startsWithQuery(company)) return 3;
        if (wordStartsWithQuery(company)) return 4;
        if (containsQuery(role)) return 5;
        if (containsQuery(company)) return 6;
        return 7;
      };

      list.sort((a, b) => {
        const tierA = getTier(a);
        const tierB = getTier(b);
        if (tierA !== tierB) return tierA - tierB;

        const roleA = (a.role || '').trim() || 'Position unassigned';
        const roleB = (b.role || '').trim() || 'Position unassigned';
        const compA = (a.companyName || '').trim() || 'Unknown Company';
        const compB = (b.companyName || '').trim() || 'Unknown Company';

        if (tierA === 1 || tierA === 2 || tierA === 5) {
          // Job Title priority: Role A-Z, then Company A-Z, then createdAt desc
          const roleComp = roleA.localeCompare(roleB, undefined, { sensitivity: 'base' });
          if (roleComp !== 0) return roleComp;
          const compComp = compA.localeCompare(compB, undefined, { sensitivity: 'base' });
          if (compComp !== 0) return compComp;
          return (b.createdAt || 0) - (a.createdAt || 0);
        } else if (tierA === 3 || tierA === 4 || tierA === 6) {
          // Company Name priority: Company A-Z, then Role A-Z, then createdAt desc
          const compComp = compA.localeCompare(compB, undefined, { sensitivity: 'base' });
          if (compComp !== 0) return compComp;
          const roleComp = roleA.localeCompare(roleB, undefined, { sensitivity: 'base' });
          if (roleComp !== 0) return roleComp;
          return (b.createdAt || 0) - (a.createdAt || 0);
        } else {
          // Other matches priority: Role A-Z, then Company A-Z, then createdAt desc
          const roleComp = roleA.localeCompare(roleB, undefined, { sensitivity: 'base' });
          if (roleComp !== 0) return roleComp;
          const compComp = compA.localeCompare(compB, undefined, { sensitivity: 'base' });
          if (compComp !== 0) return compComp;
          return (b.createdAt || 0) - (a.createdAt || 0);
        }
      });
    } else {
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
      if (status === 'Date') {
        if (!prev.dateBasis) next.dateBasis = 'created';
        if (prev.dateFilterMode === 'All' || !prev.dateFilterMode) {
          next.dateFilterMode = 'Month';
          next.dateMonth = (new Date().getMonth() + 1).toString();
          next.dateYear = new Date().getFullYear().toString();
          next.dateSpecificDay = getLocalDateString();
          next.dateStartRange = getLocalFirstOfMonth();
          next.dateEndRange = getLocalDateString();
        }
      } else {
        setIsDateModalOpen(false);
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
    const targetId = app.id;
    setDeletingIds(prev => [...prev, targetId]);
    setAppToDelete(null);
    
    const jobName = app.companyName 
      ? `${app.companyName} - ${app.role || 'Position unassigned'}` 
      : (app.role || 'Application');

    setTimeout(() => {
      db.deleteApplication(targetId);
      setDeletingIds(prev => prev.filter(id => id !== targetId));

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
    }, 250);
  };

  const handleDeleteSelected = () => {
    if (selectedIds.length === 0) return;
    const appsList = applications.filter(a => selectedIds.includes(a.id));
    setAppsToDeleteList(appsList);
  };

  const handleConfirmDeleteList = (appsList) => {
    const ids = appsList.map(a => a.id);
    const count = ids.length;
    
    setDeletingIds(prev => [...prev, ...ids]);
    setAppsToDeleteList([]);

    setTimeout(() => {
      db.deleteMultipleApplications(ids);
      setDeletingIds(prev => prev.filter(id => !ids.includes(id)));
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
    }, 250);
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

  const getDateFilterSummary = () => {
    const basisLabel = filters.dateBasis === 'status' ? 'Status Updated' : 'Date Added';
    if (filters.dateFilterMode === 'Month') {
      const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const mIdx = parseInt(filters.dateMonth, 10) - 1;
      const mName = monthNames[mIdx] || filters.dateMonth;
      return { basis: basisLabel, detail: `${mName} ${filters.dateYear}` };
    }
    if (filters.dateFilterMode === 'Day') {
      if (!filters.dateSpecificDay) return { basis: basisLabel, detail: 'Today' };
      const parsed = parseLocalDate(filters.dateSpecificDay);
      if (!parsed) return { basis: basisLabel, detail: filters.dateSpecificDay };
      const formatted = parsed.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
      return { basis: basisLabel, detail: formatted };
    }
    if (filters.dateFilterMode === 'Range') {
      const start = parseLocalDate(filters.dateStartRange);
      const end = parseLocalDate(filters.dateEndRange);
      const startStr = start ? start.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : filters.dateStartRange;
      const endStr = end ? end.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : filters.dateEndRange;
      return { basis: basisLabel, detail: `${startStr} – ${endStr}` };
    }
    return { basis: basisLabel, detail: 'All' };
  };

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
          {filters.statusFilter === 'Date' && (() => {
            const summary = getDateFilterSummary();
            return (
              <div className="sub-filter-panel animate-fade-in" style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                <div className="sub-filter-group" style={{ flex: 1 }}>
                  <span className="sub-filter-label">Filter by Date</span>
                  <div 
                    className="sub-filter-select-trigger"
                    onClick={handleOpenDateModal}
                    title="Click to modify date filter"
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                      <CalendarIcon style={{ width: '15px', height: '15px', color: 'var(--brand-primary)', flexShrink: 0 }} />
                      <span className="date-filter-summary-badge">
                        <span className="summary-badge-dot" />
                        {summary.basis}
                      </span>
                      <span className="date-filter-summary-divider">•</span>
                      <span className="date-filter-summary-text">{summary.detail}</span>
                    </div>
                    <ChevronIcon direction="down" style={{ width: '14px', height: '14px', color: 'var(--text-secondary)', flexShrink: 0 }} />
                  </div>
                </div>
              </div>
            );
          })()}

          {/* DATE FILTER MODAL */}
          <DateFilterModal 
            isOpen={isDateModalOpen} 
            onClose={() => setIsDateModalOpen(false)} 
            dateDraft={dateDraft} 
            setDateDraft={setDateDraft} 
            onApply={handleApplyDateModal} 
          />
          
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
          {filteredApps.map((app, index) => {
            const styles = getStatusColorClass(app.status);
            const isSelected = selectedIds.includes(app.id);
            const isDeleting = deletingIds.includes(app.id);

            return (
              <div 
                key={app.id} 
                className={`job-card card-base card-interactive ${isSelected ? 'selected' : ''} ${isDeleting ? 'animating-exit' : ''}`}
                onClick={() => !isDeleting && handleJobCardClick(app)}
                onContextMenu={(e) => {
                  e.preventDefault();
                  handleEnterSelectionMode(app.id);
                }}
                style={{
                  borderColor: isSelected ? 'var(--brand-primary)' : 'var(--brand-outline)',
                  backgroundColor: isSelected ? 'var(--bg-surface-variant)' : 'var(--bg-surface)',
                  animationDelay: `${Math.min(index * 35, 300)}ms`
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
                            <span>{app.platform}</span>
                          </div>
                        )}
                        {app.resume && app.resume.originalName && (
                          <div className="job-card-detail-item" style={{ flex: 1, minWidth: '120px' }}>
                            <FileIcon style={{ flexShrink: 0 }} />
                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', minWidth: 0, flex: 1 }} title={app.resume.originalName}>
                              {app.resume.originalName}
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
                dateBasis: 'created',
                dateFilterMode: 'Month',
                dateMonth: (new Date().getMonth() + 1).toString(),
                dateYear: new Date().getFullYear().toString(),
                dateSpecificDay: getLocalDateString(),
                dateStartRange: getLocalFirstOfMonth(),
                dateEndRange: getLocalDateString()
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
