import React, { useState, useEffect } from 'react';
import { db } from '../utils/db';
import { ChevronIcon, FileIcon, DeleteIcon } from '../components/Icons';
import './JobAddEdit.css';

export default function JobAddEdit({ jobId, setActiveTab, setSelectedJobId, editSource }) {
  const isEditMode = !!jobId;

  // Form Fields State
  const [companyName, setCompanyName] = useState('');
  const [role, setRole] = useState('');
  const [platformSelect, setPlatformSelect] = useState('LinkedIn');
  const [customPlatformName, setCustomPlatformName] = useState('');
  const [status, setStatus] = useState('Applied');
  const [createdAt, setCreatedAt] = useState(new Date().toISOString().split('T')[0]);
  const [jobDescription, setJobDescription] = useState('');
  const [notes, setNotes] = useState('');
  const [url, setUrl] = useState('');
  const [email, setEmail] = useState('');
  
  // Attachments State
  const [resume, setResume] = useState(null);
  const [coverLetter, setCoverLetter] = useState(null);
  const [additionalDocument, setAdditionalDocument] = useState(null);
  const [screenshots, setScreenshots] = useState([]); // Array of attachment objects

  // Load existing data if in edit mode
  useEffect(() => {
    if (isEditMode) {
      const app = db.getApplicationById(jobId);
      if (app) {
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
        setCreatedAt(new Date(app.createdAt).toISOString().split('T')[0]);
        setJobDescription(app.jobDescription || '');
        setNotes(app.notes || '');
        setUrl(app.url || '');
        setEmail(app.email || '');
        setResume(app.resume);
        setCoverLetter(app.coverLetter);
        setAdditionalDocument(app.additionalDocument || null);
        setScreenshots(app.screenshots || []);
      }
    }
  }, [jobId, isEditMode]);

  const handleFileUpload = (e, type) => {
    const file = e.target.files[0];
    if (!file) return;

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
      if (type === 'screenshot') {
        if (screenshots.length >= 3) {
          alert('You can upload a maximum of 3 screenshots/images.');
          return;
        }
        setScreenshots(prev => [...prev, attachment]);
      }
    };
    reader.readAsDataURL(file);
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
    if (!companyName.trim() || !role.trim()) {
      alert('Company Name and Role are required.');
      return;
    }

    const finalPlatform = platformSelect === 'Other' ? customPlatformName.trim() : platformSelect;
    const finalEmail = platformSelect === 'Email' ? email.trim() : '';

    const appData = {
      companyName: companyName.trim(),
      role: role.trim(),
      platform: finalPlatform || 'Direct',
      status,
      createdAt: new Date(createdAt).getTime(),
      jobDescription: jobDescription.trim(),
      notes: notes.trim(),
      url: url.trim(),
      email: finalEmail,
      resume,
      coverLetter,
      additionalDocument,
      screenshots
    };

    if (isEditMode) {
      db.updateApplication(jobId, appData);
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: { message: `Successfully updated ${companyName}` }
      }));
      // Navigate to previous view
      setActiveTab(editSource || 'job-detail');
    } else {
      const newApp = db.addApplication(appData);
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: { message: `Successfully added ${companyName}` }
      }));
      // Reset selected job and go to Applications
      setSelectedJobId(null);
      setActiveTab('applications');
    }
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

        <form onSubmit={handleSubmit} className="form-grid" style={{ gap: '20px' }}>
          
          {/* Card 1: Job Details */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Job Details</h3>
            <div className="form-grid grid-2">
              <div className="form-group">
                <label className="form-label">Company Name *</label>
                <input 
                  type="text" 
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  className="form-input"
                  placeholder="e.g. Google"
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Role / Position *</label>
                <input 
                  type="text" 
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                  className="form-input"
                  placeholder="e.g. Frontend Engineer"
                  required
                />
              </div>
            </div>
            
            <div className="form-group" style={{ marginTop: '16px' }}>
              <label className="form-label">Application Status</label>
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
          </div>

          {/* Card 2: Platform & Date Details */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Platform & Date Details</h3>
            
            <div className="form-grid grid-2">
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

              <div className="form-group">
                <label className="form-label">Status Date</label>
                <input 
                  type="date" 
                  value={createdAt}
                  onChange={(e) => setCreatedAt(e.target.value)}
                  className="form-input"
                />
              </div>
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
          </div>

          {/* Card 3: Notes & Description */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Description & Notes</h3>
            <div className="form-group">
              <label className="form-label">Job Description</label>
              <textarea 
                value={jobDescription}
                onChange={(e) => setJobDescription(e.target.value)}
                className="form-textarea"
                placeholder="Paste the job description details here..."
              />
            </div>
            
            <div className="form-group" style={{ marginTop: '16px' }}>
              <label className="form-label">Personal Notes</label>
              <textarea 
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="form-textarea"
                placeholder="Record interview notes, salary info, prep plans, etc..."
              />
            </div>
          </div>

          {/* Card 4: Documents & Attachments */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Documents & Attachments</h3>
            <div className="file-upload-slots" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              
              {/* Resume slot */}
              <div className="file-slot">
                <div className="file-slot-info">
                  <FileIcon />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Resume / CV</div>
                    {resume ? (
                      <span className="file-slot-filename">{resume.originalName}</span>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>No file attached</span>
                    )}
                  </div>
                </div>
                {resume ? (
                  <button type="button" onClick={() => handleDeleteAttachment('resume')} className="file-delete-btn" title="Delete attachment">
                    <DeleteIcon />
                  </button>
                ) : (
                  <label className="file-upload-btn">
                    Upload
                    <input 
                      type="file" 
                      accept=".pdf,.doc,.docx" 
                      onChange={(e) => handleFileUpload(e, 'resume')}
                      style={{ display: 'none' }}
                    />
                  </label>
                )}
              </div>

              <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>

              {/* Cover Letter slot */}
              <div className="file-slot">
                <div className="file-slot-info">
                  <FileIcon />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Cover Letter</div>
                    {coverLetter ? (
                      <span className="file-slot-filename">{coverLetter.originalName}</span>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>No file attached</span>
                    )}
                  </div>
                </div>
                {coverLetter ? (
                  <button type="button" onClick={() => handleDeleteAttachment('coverLetter')} className="file-delete-btn" title="Delete attachment">
                    <DeleteIcon />
                  </button>
                ) : (
                  <label className="file-upload-btn">
                    Upload
                    <input 
                      type="file" 
                      accept=".pdf,.doc,.docx" 
                      onChange={(e) => handleFileUpload(e, 'coverLetter')}
                      style={{ display: 'none' }}
                    />
                  </label>
                )}
              </div>

              <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>

              {/* Additional Document slot */}
              <div className="file-slot">
                <div className="file-slot-info">
                  <FileIcon />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Additional Document</div>
                    {additionalDocument ? (
                      <span className="file-slot-filename">{additionalDocument.originalName}</span>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>No file attached</span>
                    )}
                  </div>
                </div>
                {additionalDocument ? (
                  <button type="button" onClick={() => handleDeleteAttachment('additionalDocument')} className="file-delete-btn" title="Delete attachment">
                    <DeleteIcon />
                  </button>
                ) : (
                  <label className="file-upload-btn">
                    Upload
                    <input 
                      type="file" 
                      accept=".pdf,.doc,.docx" 
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
                        width: '80px',
                        height: '80px',
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
                        width: '80px',
                        height: '80px',
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
                        fontSize: '0.65rem',
                        fontWeight: 700,
                        textAlign: 'center',
                        padding: '4px'
                      }}
                    >
                      <span style={{ fontSize: '1.2rem', lineHeight: 1 }}>+</span>
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

          {/* Form Actions */}
          <div className="form-actions-bar">
            <button type="button" onClick={handleCancel} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn-primary">
              {isEditMode ? 'Save Changes' : 'Create Application'}
            </button>
          </div>

        </form>
      </div>
    </div>
  );
}
