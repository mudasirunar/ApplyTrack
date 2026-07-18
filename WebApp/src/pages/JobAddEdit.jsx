import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { db } from '../utils/db';
import { ChevronIcon, FileIcon, DeleteIcon } from '../components/Icons';
import './JobAddEdit.css';

function ConfirmationModal({ title, message, confirmLabel, isDestructive, onConfirm, onCancel, hideCancel = false }) {
  useEffect(() => {
    const originalStyle = window.getComputedStyle(document.body).overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = originalStyle;
    };
  }, []);

  return (
    <div className="modal-overlay" onClick={hideCancel ? undefined : onCancel}>
      <div className="modal-content-card" style={{ maxWidth: '400px' }} onClick={(e) => e.stopPropagation()}>
        <h3 className="modal-title" style={{ margin: 0, color: isDestructive ? 'var(--error-red)' : 'var(--brand-primary)' }}>
          {title}
        </h3>
        <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', margin: '8px 0' }}></div>
        <p className="modal-text" style={{ fontSize: '0.85rem', lineHeight: '1.6', color: 'var(--text-primary)', margin: '12px 0 20px' }}>
          {message}
        </p>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          {!hideCancel && (
            <button onClick={onCancel} className="btn-secondary" style={{ padding: '8px 16px', fontSize: '0.85rem' }}>
              Cancel
            </button>
          )}
          <button 
            onClick={onConfirm} 
            className="btn-primary" 
            style={{ 
              padding: '8px 16px', 
              fontSize: '0.85rem', 
              backgroundColor: isDestructive ? 'var(--error-red)' : undefined,
              borderColor: isDestructive ? 'var(--error-red)' : undefined,
              color: isDestructive ? '#FFFFFF' : undefined
            }}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

const getLocalDateString = (timestampOrDate = new Date()) => {
  const date = new Date(timestampOrDate);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const parseLocalDateStringToTimestamp = (selectedDateStr, originalTime = null) => {
  if (!selectedDateStr) return Date.now();
  
  if (originalTime) {
    const originalDateStr = getLocalDateString(originalTime);
    if (selectedDateStr === originalDateStr) {
      return originalTime;
    }
  }
  
  const todayStr = getLocalDateString(new Date());
  if (selectedDateStr === todayStr) {
    return Date.now();
  }
  
  const [year, month, day] = selectedDateStr.split('-').map(Number);
  return new Date(year, month - 1, day).getTime();
};

const ymdToDmy = (ymd) => {
  if (!ymd) return '';
  const parts = ymd.split('-');
  if (parts.length !== 3) return ymd;
  const [year, month, day] = parts;
  return `${day}/${month}/${year}`;
};

const dmyToYmd = (dmy) => {
  if (!dmy) return '';
  const parts = dmy.split('/');
  if (parts.length !== 3) return '';
  const [day, month, year] = parts;
  if (day.length === 2 && month.length === 2 && year.length === 4) {
    return `${year}-${month}-${day}`;
  }
  return '';
};

function DatePickerField({ value, onChange, placeholder = "dd/mm/yyyy", className = "form-input", style = {} }) {
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



export default function JobAddEdit({ jobId, setActiveTab, setSelectedJobId, editSource }) {
  const isEditMode = !!jobId;
  const [isSaving, setIsSaving] = useState(false);

  // Form Fields State
  const [companyName, setCompanyName] = useState('');
  const [role, setRole] = useState('');
  const [platformSelect, setPlatformSelect] = useState('LinkedIn');
  const [customPlatformName, setCustomPlatformName] = useState('');
  const [status, setStatus] = useState('Applied');
  const [createdAt, setCreatedAt] = useState(getLocalDateString());
  const [jobDescription, setJobDescription] = useState('');
  const [notes, setNotes] = useState('');
  const [url, setUrl] = useState('');
  const [email, setEmail] = useState('');
  
  // Attachments State
  const [resume, setResume] = useState(null);
  const [coverLetter, setCoverLetter] = useState(null);
  const [additionalDocument, setAdditionalDocument] = useState(null);
  const [screenshots, setScreenshots] = useState([]); // Array of attachment objects
  const [showRemoteDeleteModal, setShowRemoteDeleteModal] = useState(false);



  const hasLoadedOnce = React.useRef(false);

  // Load existing data if in edit mode
  useEffect(() => {
    const loadData = () => {
      if (isEditMode) {
        const app = db.getApplicationById(jobId);
        if (app) {
          hasLoadedOnce.current = true;
          setCompanyName(app.companyName || '');
          setRole(app.role || '');
          
          const plat = app.platform || '';
          const standardPlatforms = ['LinkedIn', 'Indeed', 'Email', 'Website'];
          if (standardPlatforms.includes(plat)) {
            setPlatformSelect(plat);
            setCustomPlatformName('');
          } else {
            setPlatformSelect('Other');
            setCustomPlatformName(plat || 'Direct');
          }

          setStatus(app.status || 'Applied');
          const lastStatusTimestamp = (app.statusHistory && app.statusHistory.length > 0)
            ? app.statusHistory[app.statusHistory.length - 1].timestamp
            : app.createdAt;
          setCreatedAt(getLocalDateString(lastStatusTimestamp));
          setJobDescription(app.jobDescription || '');
          setNotes(app.notes || '');
          setUrl(app.url || '');
          setEmail(app.email || '');
          setResume(app.resume);
          setCoverLetter(app.coverLetter);
          setAdditionalDocument(app.additionalDocument || null);
          setScreenshots(app.screenshots || []);
        } else if (hasLoadedOnce.current) {
          // If we successfully loaded the job data but now it's gone, it was deleted!
          setShowRemoteDeleteModal(true);
        }
      }
    };

    loadData();
    window.addEventListener('applytrack_data_change', loadData);
    return () => window.removeEventListener('applytrack_data_change', loadData);
  }, [jobId, isEditMode]);

  const compressImage = (file, callback) => {
    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const MAX_WIDTH = 1000;
        const MAX_HEIGHT = 1000;
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > MAX_WIDTH) {
            height *= MAX_WIDTH / width;
            width = MAX_WIDTH;
          }
        } else {
          if (height > MAX_HEIGHT) {
            width *= MAX_HEIGHT / height;
            height = MAX_HEIGHT;
          }
        }

        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);

        const compressedDataUrl = canvas.toDataURL('image/jpeg', 0.75);
        callback(compressedDataUrl);
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
  };

  const handleFileUpload = (e, type) => {
    const file = e.target.files[0];
    if (!file) return;

    if (type === 'screenshot') {
      if (!file.type.startsWith('image/')) {
        alert('Please upload an image file only.');
        return;
      }
      if (screenshots.length >= 3) {
        alert('You can upload a maximum of 3 screenshots/images.');
        return;
      }
      compressImage(file, (compressedDataUrl) => {
        const attachment = {
          fileName: Math.random().toString(36).substring(2, 11) + '_' + file.name.replace(/\.[^/.]+$/, "") + ".jpg",
          originalName: file.name.replace(/\.[^/.]+$/, "") + ".jpg",
          dataUrl: compressedDataUrl
        };
        setScreenshots(prev => [...prev, attachment]);
      });
    } else {
      if (file.type !== 'application/pdf') {
        alert('Please upload a PDF document only.');
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        const attachment = {
          fileName: Math.random().toString(36).substring(2, 11) + '_' + file.name,
          originalName: file.name,
          dataUrl: event.target.result // Base64 Data URL
        };

        if (type === 'resume') setResume(attachment);
        if (type === 'coverLetter') setCoverLetter(attachment);
        if (type === 'additionalDocument') setAdditionalDocument(attachment);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleDeleteAttachment = (type, index = null) => {
    if (type === 'resume') setResume(null);
    if (type === 'coverLetter') setCoverLetter(null);
    if (type === 'additionalDocument') setAdditionalDocument(null);
    if (type === 'screenshot' && index !== null) {
      setScreenshots(prev => prev.filter((_, idx) => idx !== index));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (isSaving) return;
    setIsSaving(true);

    const finalPlatform = platformSelect === 'Other' ? customPlatformName.trim() : platformSelect;
    const finalEmail = platformSelect === 'Email' ? email.trim() : '';

    const originalApp = isEditMode ? db.getApplicationById(jobId) : null;
    const originalTimestamp = originalApp
      ? ((originalApp.statusHistory && originalApp.statusHistory.length > 0)
          ? originalApp.statusHistory[originalApp.statusHistory.length - 1].timestamp
          : originalApp.createdAt)
      : null;

    const appData = {
      companyName: companyName.trim() || null,
      role: role.trim() || null,
      platform: finalPlatform || 'Direct',
      status,
      createdAt: parseLocalDateStringToTimestamp(createdAt, originalTimestamp),
      jobDescription: jobDescription.trim() || null,
      notes: notes.trim() || null,
      url: url.trim() || null,
      email: finalEmail || null,
      resume,
      coverLetter,
      additionalDocument,
      screenshots
    };

    setTimeout(async () => {
      try {
        if (isEditMode) {
          await db.updateApplication(jobId, appData);
          // Navigate to previous view
          setActiveTab(editSource || 'job-detail');
        } else {
          await db.addApplication(appData);
          // Reset selected job and go to Applications
          setSelectedJobId(null);
          setActiveTab('applications');
        }
      } catch (err) {
        console.error(err);
        alert('Failed to save application. Please try again.');
        setIsSaving(false);
      }
    }, 600);
  };

  const handleCancel = () => {
    if (isEditMode) {
      setActiveTab(editSource || 'job-detail');
    } else {
      setActiveTab('applications');
    }
  };

  return (
    <div className="content-container animate-fade-in">
      <div className="form-container">
        
        {/* Header */}
        <div className="form-header">
          <button onClick={handleCancel} className="job-card-action-btn" title="Back">
            <ChevronIcon direction="left" style={{ width: '24px', height: '24px' }} />
          </button>
          <h2 className="dashboard-greeting" style={{ margin: 0 }}>
            {isEditMode ? 'Edit Application' : 'Add Application'}
          </h2>
        </div>

        <form id="job-add-edit-form" onSubmit={handleSubmit} className="form-grid" style={{ gap: '20px' }}>
          
          {/* Card 1: Job Details */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Job Details</h3>
            <div className="form-group">
              <label className="form-label">Role / Position</label>
              <input 
                type="text" 
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="form-input"
                placeholder="e.g. Senior Frontend Engineer"
              />
            </div>
            <div className="form-group" style={{ marginTop: '16px' }}>
              <label className="form-label">Company Name</label>
              <input 
                type="text" 
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                className="form-input"
                placeholder="e.g. Acme Corp"
              />
            </div>
          </div>

          {/* Card 2: Platform & Date Details */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Platform & Date Details</h3>
            
            <div className="form-group">
              <label className="form-label">Application Platform</label>
              <select
                value={platformSelect}
                onChange={(e) => setPlatformSelect(e.target.value)}
                className="form-select"
              >
                <option value="LinkedIn">LinkedIn</option>
                <option value="Indeed">Indeed</option>
                <option value="Email">Email</option>
                <option value="Website">Website</option>
                <option value="Other">Other</option>
              </select>
            </div>

            {platformSelect === 'Other' && (
              <div className="form-group" style={{ marginTop: '16px' }}>
                <label className="form-label">Platform Name</label>
                <input 
                  type="text" 
                  value={customPlatformName}
                  onChange={(e) => setCustomPlatformName(e.target.value)}
                  className="form-input"
                  placeholder="e.g. Glassdoor, CareerBuilder"
                />
              </div>
            )}

            {platformSelect === 'Email' && (
              <div className="form-group" style={{ marginTop: '16px' }}>
                <label className="form-label">Email Address</label>
                <input 
                  type="email" 
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="form-input"
                  placeholder="e.g. recruiter@company.com"
                />
              </div>
            )}

            <div className="form-group" style={{ marginTop: '16px' }}>
              <label className="form-label">URL</label>
              <input 
                type="text" 
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                className="form-input"
                placeholder="e.g. company.com/careers or posting link"
              />
            </div>

            <div className="form-group" style={{ marginTop: '16px' }}>
              <label className="form-label">Current Status</label>
              <select 
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="form-select"
              >
                <option value="Applied">Applied</option>
                <option value="Interview">Interview</option>
                <option value="Offer">Offer</option>
                <option value="Rejected">Rejected</option>
                <option value="Saved">Saved</option>
              </select>
            </div>

            <div className="form-group" style={{ marginTop: '16px' }}>
              <label className="form-label">Status Date</label>
              <DatePickerField 
                value={createdAt}
                onChange={(val) => setCreatedAt(val)}
                className="form-input"
              />
            </div>
          </div>

          {/* Card 3: Notes & Description */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Description & Notes</h3>
            <div className="form-group">
              <label className="form-label">Job Description / Posting Link</label>
              <textarea 
                value={jobDescription}
                onChange={(e) => setJobDescription(e.target.value)}
                className="form-textarea"
                placeholder="Paste the job description or link here..."
              />
            </div>
            
            <div className="form-group" style={{ marginTop: '16px' }}>
              <label className="form-label">Personal Notes</label>
              <textarea 
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="form-textarea"
                placeholder="Any thoughts, recruiter names, or specific things to remember..."
              />
            </div>
          </div>

          {/* Card 4: Documents & Attachments */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Documents & Attachments</h3>
            <div className="file-upload-slots" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              
              {/* Resume slot */}
              <div className="file-slot" style={{ gap: '12px', minWidth: 0 }}>
                <div className="file-slot-info" style={{ flex: 1, minWidth: 0 }}>
                  <FileIcon />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 800, fontSize: '0.85rem' }}>Resume / CV</div>
                    {resume ? (
                      <span className="file-slot-filename" style={{ display: 'block', maxWidth: '100%' }}>{resume.originalName}</span>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>No file attached</span>
                    )}
                  </div>
                </div>
                {resume ? (
                  <button type="button" onClick={() => handleDeleteAttachment('resume')} className="file-delete-btn" title="Delete attachment" style={{ flexShrink: 0 }}>
                    <DeleteIcon />
                  </button>
                ) : (
                  <label className="file-upload-btn" style={{ flexShrink: 0 }}>
                    Upload
                    <input 
                      type="file" 
                      accept="application/pdf" 
                      onChange={(e) => handleFileUpload(e, 'resume')}
                      style={{ display: 'none' }}
                    />
                  </label>
                )}
              </div>

              <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>

              {/* Cover Letter slot */}
              <div className="file-slot" style={{ gap: '12px', minWidth: 0 }}>
                <div className="file-slot-info" style={{ flex: 1, minWidth: 0 }}>
                  <FileIcon />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 800, fontSize: '0.85rem' }}>Cover Letter</div>
                    {coverLetter ? (
                      <span className="file-slot-filename" style={{ display: 'block', maxWidth: '100%' }}>{coverLetter.originalName}</span>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>No file attached</span>
                    )}
                  </div>
                </div>
                {coverLetter ? (
                  <button type="button" onClick={() => handleDeleteAttachment('coverLetter')} className="file-delete-btn" title="Delete attachment" style={{ flexShrink: 0 }}>
                    <DeleteIcon />
                  </button>
                ) : (
                  <label className="file-upload-btn" style={{ flexShrink: 0 }}>
                    Upload
                    <input 
                      type="file" 
                      accept="application/pdf" 
                      onChange={(e) => handleFileUpload(e, 'coverLetter')}
                      style={{ display: 'none' }}
                    />
                  </label>
                )}
              </div>

              <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>

              {/* Additional Document slot */}
              <div className="file-slot" style={{ gap: '12px', minWidth: 0 }}>
                <div className="file-slot-info" style={{ flex: 1, minWidth: 0 }}>
                  <FileIcon />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 800, fontSize: '0.85rem' }}>Additional Document</div>
                    {additionalDocument ? (
                      <span className="file-slot-filename" style={{ display: 'block', maxWidth: '100%' }}>{additionalDocument.originalName}</span>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>No file attached</span>
                    )}
                  </div>
                </div>
                {additionalDocument ? (
                  <button type="button" onClick={() => handleDeleteAttachment('additionalDocument')} className="file-delete-btn" title="Delete attachment" style={{ flexShrink: 0 }}>
                    <DeleteIcon />
                  </button>
                ) : (
                  <label className="file-upload-btn" style={{ flexShrink: 0 }}>
                    Upload
                    <input 
                      type="file" 
                      accept="application/pdf" 
                      onChange={(e) => handleFileUpload(e, 'additionalDocument')}
                      style={{ display: 'none' }}
                    />
                  </label>
                )}
              </div>

              <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>

              {/* Screenshots list in Android-style grid */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Screenshots / Additional Images
                </div>
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
                  {screenshots.map((shot, index) => (
                    <div 
                      key={index}
                      style={{
                        position: 'relative',
                        width: '70px',
                        height: '70px',
                        borderRadius: '8px',
                        border: '1px solid var(--brand-outline)',
                        overflow: 'hidden'
                      }}
                    >
                      <img 
                        src={shot.url || shot.dataUrl} 
                        alt="screenshot" 
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                      <button
                        type="button"
                        onClick={() => handleDeleteAttachment('screenshot', index)}
                        style={{
                          position: 'absolute',
                          top: '4px',
                          right: '4px',
                          width: '20px',
                          height: '20px',
                          borderRadius: '50%',
                          backgroundColor: 'rgba(0,0,0,0.6)',
                          color: '#FFFFFF',
                          border: 'none',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          cursor: 'pointer',
                          padding: '3px'
                        }}
                        title="Delete image"
                      >
                        <DeleteIcon style={{ width: '14px', height: '14px', fill: 'currentColor' }} />
                      </button>
                    </div>
                  ))}

                  {screenshots.length < 3 && (
                    <label 
                      style={{
                        width: '70px',
                        height: '70px',
                        borderRadius: '8px',
                        border: '1px dashed var(--brand-primary)',
                        backgroundColor: 'var(--bg-surface-variant)',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '4px',
                        cursor: 'pointer',
                        color: 'var(--brand-primary)',
                        fontSize: '0.62rem',
                        fontWeight: 700,
                        textAlign: 'center',
                        padding: '4px'
                      }}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor" style={{ lineHeight: 1 }}>
                        <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>
                      </svg>
                      <span>Add ({screenshots.length}/3)</span>
                      <input 
                        type="file" 
                        accept="image/*" 
                        onChange={(e) => handleFileUpload(e, 'screenshot')}
                        style={{ display: 'none' }}
                      />
                    </label>
                  )}
                </div>
              </div>

            </div>
          </div>

        </form>
      </div>

      {createPortal(
        <div className="form-actions-bar">
          <div className="form-actions-content">
            <button 
              type="submit" 
              form="job-add-edit-form"
              className="form-submit-btn"
              disabled={isSaving}
            >
              {isSaving ? (
                <>
                  <span className="spinner"></span>
                  <span>{isEditMode ? 'Updating...' : 'Saving...'}</span>
                </>
              ) : (
                <span>{isEditMode ? 'Update Application' : 'Save Application'}</span>
              )}
            </button>
          </div>
        </div>,
        document.body
      )}

      {showRemoteDeleteModal && createPortal(
        <ConfirmationModal 
          title="Application Deleted"
          message="This job application has been deleted from another device"
          confirmLabel="Go to Applications"
          hideCancel={true}
          onConfirm={() => {
            setShowRemoteDeleteModal(false);
            setActiveTab('applications');
            setSelectedJobId(null);
          }}
          onCancel={() => {}}
        />,
        document.body
      )}
    </div>
  );
}
