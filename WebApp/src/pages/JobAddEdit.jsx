import React, { useState, useEffect } from 'react';
import { db } from '../utils/db';
import { ChevronIcon, FileIcon, DeleteIcon } from '../components/Icons';

export default function JobAddEdit({ jobId, setActiveTab, setSelectedJobId }) {
  const isEditMode = !!jobId;

  // Form Fields State
  const [companyName, setCompanyName] = useState('');
  const [role, setRole] = useState('');
  const [platform, setPlatform] = useState('');
  const [status, setStatus] = useState('Applied');
  const [createdAt, setCreatedAt] = useState(new Date().toISOString().split('T')[0]);
  const [jobDescription, setJobDescription] = useState('');
  const [notes, setNotes] = useState('');
  const [url, setUrl] = useState('');
  const [email, setEmail] = useState('');
  
  // Attachments State
  const [resume, setResume] = useState(null);
  const [coverLetter, setCoverLetter] = useState(null);
  const [screenshots, setScreenshots] = useState([]); // Array of attachment objects

  // Load existing data if in edit mode
  useEffect(() => {
    if (isEditMode) {
      const app = db.getApplicationById(jobId);
      if (app) {
        setCompanyName(app.companyName || '');
        setRole(app.role || '');
        setPlatform(app.platform || '');
        setStatus(app.status || 'Applied');
        setCreatedAt(new Date(app.createdAt).toISOString().split('T')[0]);
        setJobDescription(app.jobDescription || '');
        setNotes(app.notes || '');
        setUrl(app.url || '');
        setEmail(app.email || '');
        setResume(app.resume);
        setCoverLetter(app.coverLetter);
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
      if (type === 'screenshot') {
        setScreenshots(prev => [...prev, attachment]);
      }
    };
    reader.readAsDataURL(file);
  };

  const handleDeleteAttachment = (type, index = null) => {
    if (type === 'resume') setResume(null);
    if (type === 'coverLetter') setCoverLetter(null);
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

    const appData = {
      companyName: companyName.trim(),
      role: role.trim(),
      platform: platform.trim() || 'Direct',
      status,
      createdAt: new Date(createdAt).getTime(),
      jobDescription: jobDescription.trim(),
      notes: notes.trim(),
      url: url.trim(),
      email: email.trim(),
      resume,
      coverLetter,
      screenshots
    };

    if (isEditMode) {
      db.updateApplication(jobId, appData);
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: { message: `Successfully updated ${companyName}` }
      }));
      // Navigate to detail view
      setActiveTab('job-detail');
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
      setActiveTab('job-detail');
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

          {/* Card 2: Platform & Date */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Platform & Date Details</h3>
            <div className="form-grid grid-2">
              <div className="form-group">
                <label className="form-label">Date Applied / Tracked</label>
                <input 
                  type="date" 
                  value={createdAt}
                  onChange={(e) => setCreatedAt(e.target.value)}
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Platform / Source</label>
                <input 
                  type="text" 
                  value={platform}
                  onChange={(e) => setPlatform(e.target.value)}
                  className="form-input"
                  placeholder="e.g. LinkedIn, Referral, Indeed"
                />
              </div>
            </div>
            
            <div className="form-grid grid-2" style={{ marginTop: '16px' }}>
              <div className="form-group">
                <label className="form-label">Job Post URL</label>
                <input 
                  type="url" 
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  className="form-input"
                  placeholder="https://..."
                />
              </div>
              <div className="form-group">
                <label className="form-label">Contact Email</label>
                <input 
                  type="email" 
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="form-input"
                  placeholder="recruiter@company.com"
                />
              </div>
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

          {/* Card 4: Attachments */}
          <div className="card-base form-card">
            <h3 className="form-card-title">Attachments & Files</h3>
            <div className="file-upload-slots">
              
              {/* Resume slot */}
              <div className="file-slot">
                <div className="file-slot-info">
                  <FileIcon />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Resume</div>
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

              {/* Screenshots list */}
              <div className="file-slot" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '8px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div className="file-slot-info">
                    <FileIcon />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Screenshots / Images</div>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        {screenshots.length} file(s) uploaded
                      </span>
                    </div>
                  </div>
                  <label className="file-upload-btn">
                    Add Screenshot
                    <input 
                      type="file" 
                      accept="image/*" 
                      onChange={(e) => handleFileUpload(e, 'screenshot')}
                      style={{ display: 'none' }}
                    />
                  </label>
                </div>

                {screenshots.length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '8px', borderTop: '1px solid var(--brand-outline)', paddingTop: '8px' }}>
                    {screenshots.map((shot, idx) => (
                      <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8rem', padding: '4px 8px', backgroundColor: 'var(--bg-surface)', borderRadius: '4px', border: '1px solid var(--brand-outline)' }}>
                        <span style={{ maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontWeight: 600 }}>
                          {shot.originalName}
                        </span>
                        <button type="button" onClick={() => handleDeleteAttachment('screenshot', idx)} className="file-delete-btn" style={{ padding: '2px' }}>
                          <DeleteIcon style={{ width: '16px', height: '16px' }} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
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
