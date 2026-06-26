import React, { useState, useEffect } from 'react';
import { db } from '../utils/db';
import { 
  ChevronIcon, 
  CalendarIcon, 
  LinkIcon, 
  EmailIcon, 
  FileIcon,
  EditIcon,
  DeleteIcon
} from '../components/Icons';
import ImageViewer from '../components/ImageViewer';
import PDFViewer from '../components/PDFViewer';

export default function JobDetail({ jobId, setActiveTab, setSelectedJobId }) {
  const [app, setApp] = useState(null);
  
  // Description collapse state
  const [isCollapsed, setIsCollapsed] = useState(true);
  
  // Inline Notes editor state
  const [isEditingNotes, setIsEditingNotes] = useState(false);
  const [editedNotes, setEditedNotes] = useState('');

  // Attachments overlay states
  const [activeImageFile, setActiveImageFile] = useState(null);
  const [activePdfFile, setActivePdfFile] = useState(null);

  // Load app data
  useEffect(() => {
    if (jobId) {
      const data = db.getApplicationById(jobId);
      if (data) {
        setApp(data);
        setEditedNotes(data.notes || '');
      }
    }
  }, [jobId]);

  if (!app) {
    return (
      <div className="content-container animate-fade-in text-center" style={{ padding: '48px' }}>
        <p>Application details not found.</p>
        <button onClick={() => setActiveTab('applications')} className="btn-primary" style={{ marginTop: '12px' }}>
          Back to Applications
        </button>
      </div>
    );
  }

  const handleStatusChange = (e) => {
    const nextStatus = e.target.value;
    const updated = db.updateApplication(app.id, { status: nextStatus });
    if (updated) {
      setApp(updated);
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: { message: `Status updated to ${nextStatus}` }
      }));
    }
  };

  const handleSaveNotes = () => {
    const updated = db.updateApplication(app.id, { notes: editedNotes.trim() });
    if (updated) {
      setApp(updated);
      setIsEditingNotes(false);
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: { message: 'Personal notes updated successfully' }
      }));
    }
  };

  const handleCancelNotesEdit = () => {
    setEditedNotes(app.notes || '');
    setIsEditingNotes(false);
  };

  const handleDelete = () => {
    if (window.confirm(`Are you sure you want to delete this application for ${app.companyName}?`)) {
      db.deleteApplication(app.id);
      
      // Navigate away
      setActiveTab('applications');
      setSelectedJobId(null);

      // Trigger undo toast
      window.dispatchEvent(new CustomEvent('applytrack_toast', {
        detail: {
          message: `'${app.role || 'Application'}' deleted`,
          action: 'Undo',
          onAction: () => {
            db.undoDelete();
            // Try to reload
            window.dispatchEvent(new CustomEvent('applytrack_toast', { detail: { message: 'Restored application' } }));
          }
        }
      }));
    }
  };

  const handleAttachmentClick = (file, type) => {
    if (type === 'image') {
      setActiveImageFile(file);
    } else if (type === 'pdf') {
      setActivePdfFile(file);
    }
  };

  // Status Badge styling helper
  const getStatusColor = (status) => {
    switch (status) {
      case 'Applied': return '#FFB300';
      case 'Saved': return '#78909C';
      case 'Interview': return '#4CAF50';
      case 'Offer': return '#1E88E5';
      case 'Rejected': return '#E53935';
      default: return 'var(--text-secondary)';
    }
  };

  return (
    <div className="content-container animate-fade-in">
      <div className="detail-container">
        
        {/* Navigation Toolbar */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
          <button onClick={() => setActiveTab('applications')} className="job-card-action-btn" title="Back to list">
            <ChevronIcon direction="left" style={{ width: '24px', height: '24px' }} />
          </button>
          
          <div style={{ display: 'flex', gap: '10px' }}>
            <button 
              onClick={() => setActiveTab('edit-job')} 
              className="btn-secondary" 
              style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 14px' }}
            >
              <EditIcon style={{ width: '16px', height: '16px' }} />
              <span>Edit</span>
            </button>
            <button 
              onClick={handleDelete} 
              className="backup-btn reset" 
              style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 14px' }}
            >
              <DeleteIcon style={{ width: '16px', height: '16px' }} />
              <span>Delete</span>
            </button>
          </div>
        </div>

        {/* Card 1: Header Info & Dropdown Status */}
        <div className="card-base detail-header-card">
          <div className="detail-header-row">
            <div className="detail-title-group">
              <h2 className="detail-company">{app.companyName}</h2>
              <span className="detail-role">{app.role}</span>
            </div>
            
            <select
              value={app.status}
              onChange={handleStatusChange}
              className="detail-status-select"
              style={{ 
                color: getStatusColor(app.status),
                borderColor: getStatusColor(app.status),
                backgroundColor: `${getStatusColor(app.status)}10` // 10% alpha tint
              }}
            >
              <option value="Applied">Applied</option>
              <option value="Interview">Interview</option>
              <option value="Offer">Offer</option>
              <option value="Rejected">Rejected</option>
              <option value="Saved">Saved</option>
            </select>
          </div>

          <div className="metadata-grid">
            <div className="metadata-item">
              <span className="metadata-label">Date Tracked</span>
              <span className="metadata-value">
                <CalendarIcon style={{ width: '16px', height: '16px' }} />
                <span>{new Date(app.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</span>
              </span>
            </div>
            <div className="metadata-item">
              <span className="metadata-label">Platform Source</span>
              <span className="metadata-value">
                <LinkIcon style={{ width: '16px', height: '16px' }} />
                <span>{app.platform || 'Direct'}</span>
              </span>
            </div>
            <div className="metadata-item">
              <span className="metadata-label">Job Post URL</span>
              <span className="metadata-value">
                {app.url ? (
                  <a href={app.url} target="_blank" rel="noopener noreferrer">Visit Website</a>
                ) : (
                  <span style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>None provided</span>
                )}
              </span>
            </div>
            <div className="metadata-item">
              <span className="metadata-label">Contact Email</span>
              <span className="metadata-value">
                {app.email ? (
                  <a href={`mailto:${app.email}`}>
                    <EmailIcon style={{ width: '14px', height: '14px', marginRight: '4px', display: 'inline' }} />
                    Email
                  </a>
                ) : (
                  <span style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>None provided</span>
                )}
              </span>
            </div>
          </div>
        </div>

        {/* Card 2: Collapsible Job Description */}
        {app.jobDescription && (
          <div className="card-base collapsible-card">
            <h3 className="section-title">Job Description</h3>
            <div className={`collapsible-content ${isCollapsed ? 'collapsed' : ''}`}>
              {app.jobDescription}
            </div>
            <button onClick={() => setIsCollapsed(!isCollapsed)} className="collapsible-toggle-btn">
              <span>{isCollapsed ? 'Show More' : 'Show Less'}</span>
              <ChevronIcon direction={isCollapsed ? 'down' : 'up'} style={{ width: '16px', height: '16px' }} />
            </button>
          </div>
        )}

        {/* Card 3: Inline Editable Personal Notes */}
        <div className="card-base notes-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 className="section-title" style={{ margin: 0 }}>Personal Notes</h3>
            {!isEditingNotes && (
              <button 
                onClick={() => setIsEditingNotes(true)} 
                className="job-card-action-btn" 
                title="Edit notes"
                style={{ padding: '4px' }}
              >
                <EditIcon style={{ width: '16px', height: '16px' }} />
              </button>
            )}
          </div>

          {isEditingNotes ? (
            <div className="animate-fade-in">
              <textarea
                value={editedNotes}
                onChange={(e) => setEditedNotes(e.target.value)}
                className="form-textarea"
                style={{ width: '100%', minHeight: '120px' }}
                placeholder="Write interview notes, preparation plans, or key details..."
              />
              <div className="notes-editor-actions">
                <button onClick={handleCancelNotesEdit} className="btn-secondary" style={{ padding: '6px 12px', fontSize: '0.8rem' }}>
                  Cancel
                </button>
                <button onClick={handleSaveNotes} className="btn-primary" style={{ padding: '6px 12px', fontSize: '0.8rem' }}>
                  Save Notes
                </button>
              </div>
            </div>
          ) : (
            <div 
              className="notes-display-area"
              onClick={() => setIsEditingNotes(true)}
            >
              {app.notes ? (
                app.notes
              ) : (
                <span className="notes-placeholder">Click here to add personal notes, preparation strategies, or salary range details...</span>
              )}
            </div>
          )}
        </div>

        {/* Card 4: Attachments */}
        {((app.resume) || (app.coverLetter) || (app.screenshots && app.screenshots.length > 0)) && (
          <div className="card-base" style={{ padding: '24px' }}>
            <h3 className="section-title">Attachments & Documents</h3>
            <div className="file-upload-slots">
              
              {/* Resume */}
              {app.resume && (
                <div 
                  className="file-slot" 
                  style={{ cursor: 'pointer' }}
                  onClick={() => handleAttachmentClick(app.resume, 'pdf')}
                >
                  <div className="file-slot-info">
                    <FileIcon />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Resume</div>
                      <span className="file-slot-filename" style={{ color: 'var(--link-blue)' }}>{app.resume.originalName}</span>
                    </div>
                  </div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)' }}>View PDF</span>
                </div>
              )}

              {/* Cover Letter */}
              {app.coverLetter && (
                <div 
                  className="file-slot" 
                  style={{ cursor: 'pointer' }}
                  onClick={() => handleAttachmentClick(app.coverLetter, 'pdf')}
                >
                  <div className="file-slot-info">
                    <FileIcon />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Cover Letter</div>
                      <span className="file-slot-filename" style={{ color: 'var(--link-blue)' }}>{app.coverLetter.originalName}</span>
                    </div>
                  </div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)' }}>View PDF</span>
                </div>
              )}

              {/* Screenshots list */}
              {app.screenshots && app.screenshots.map((shot, idx) => (
                <div 
                  key={idx}
                  className="file-slot" 
                  style={{ cursor: 'pointer' }}
                  onClick={() => handleAttachmentClick(shot, 'image')}
                >
                  <div className="file-slot-info">
                    <FileIcon />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Screenshot {idx + 1}</div>
                      <span className="file-slot-filename" style={{ color: 'var(--link-blue)' }}>{shot.originalName}</span>
                    </div>
                  </div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)' }}>View Image</span>
                </div>
              ))}

            </div>
          </div>
        )}

        {/* Card 5: Stepper Timeline */}
        <div className="card-base timeline-card">
          <h3 className="section-title">Status History Timeline</h3>
          <div className="timeline-list">
            {app.statusHistory && app.statusHistory.length > 0 ? (
              app.statusHistory.map((history, idx) => (
                <div key={idx} className="timeline-item">
                  <div className="timeline-node" style={{ borderColor: getStatusColor(history.status) }}></div>
                  <div className="timeline-content">
                    <span className="timeline-status">{history.status}</span>
                    <span className="timeline-time">
                      {new Date(history.timestamp).toLocaleString(undefined, {
                        month: 'long',
                        day: 'numeric',
                        year: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <div className="timeline-item">
                <div className="timeline-node" style={{ borderColor: 'var(--warning-amber)' }}></div>
                <div className="timeline-content">
                  <span className="timeline-status">{app.status}</span>
                  <span className="timeline-time">{new Date(app.createdAt).toLocaleString()}</span>
                </div>
              </div>
            )}
          </div>
        </div>

      </div>

      {/* OVERLAY VIEWERS */}
      {activeImageFile && (
        <ImageViewer file={activeImageFile} onClose={() => setActiveImageFile(null)} />
      )}
      {activePdfFile && (
        <PDFViewer file={activePdfFile} onClose={() => setActivePdfFile(null)} />
      )}
    </div>
  );
}
