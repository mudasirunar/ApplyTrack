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
import './JobDetail.css';

export default function JobDetail({ jobId, setActiveTab, setSelectedJobId }) {
  const [app, setApp] = useState(null);
  
  // Description collapse state
  const [isCollapsed, setIsCollapsed] = useState(true);
  const [isDescCollapsible, setIsDescCollapsible] = useState(false);
  const descRef = React.useRef(null);

  // Notes collapse state
  const [isNotesCollapsed, setIsNotesCollapsed] = useState(true);
  const [isNotesCollapsible, setIsNotesCollapsible] = useState(false);
  const notesRef = React.useRef(null);

  // Attachments overlay states
  const [activeImageIndex, setActiveImageIndex] = useState(null);
  const [activePdfFile, setActivePdfFile] = useState(null);

  // Load app data
  useEffect(() => {
    if (jobId) {
      const data = db.getApplicationById(jobId);
      if (data) {
        setApp(data);
      }
    }
  }, [jobId]);

  // Check if job description overflows 3 lines
  useEffect(() => {
    if (descRef.current && app?.jobDescription && isCollapsed) {
      const hasOverflow = descRef.current.scrollHeight > descRef.current.clientHeight;
      setIsDescCollapsible(hasOverflow);
    }
  }, [app?.jobDescription, isCollapsed]);

  // Check if personal notes overflow 3 lines
  useEffect(() => {
    if (notesRef.current && app?.notes && isNotesCollapsed) {
      const hasOverflow = notesRef.current.scrollHeight > notesRef.current.clientHeight;
      setIsNotesCollapsible(hasOverflow);
    }
  }, [app?.notes, isNotesCollapsed]);

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
    if (type === 'pdf') {
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
            
            <span
              className="detail-status-badge"
              style={{ 
                color: getStatusColor(app.status),
                borderColor: getStatusColor(app.status),
                backgroundColor: `${getStatusColor(app.status)}10` // 10% alpha tint
              }}
            >
              {app.status}
            </span>
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
            <div 
              ref={descRef}
              className={`collapsible-content ${isCollapsed ? 'collapsed' : ''}`}
            >
              {app.jobDescription}
            </div>
            {isDescCollapsible && (
              <button onClick={() => setIsCollapsed(!isCollapsed)} className="collapsible-toggle-btn">
                <span>{isCollapsed ? 'Show More' : 'Show Less'}</span>
                <ChevronIcon direction={isCollapsed ? 'down' : 'up'} style={{ width: '16px', height: '16px' }} />
              </button>
            )}
          </div>
        )}

        {/* Card 3: Personal Notes */}
        <div className="card-base notes-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 className="section-title" style={{ margin: 0 }}>Personal Notes</h3>
          </div>

          <div 
            ref={notesRef}
            className={`notes-display-area ${isNotesCollapsed ? 'collapsed' : ''}`}
            style={{ cursor: 'default' }}
          >
            {app.notes ? (
              app.notes
            ) : (
              <span className="notes-placeholder" style={{ fontStyle: 'italic' }}>
                No personal notes have been recorded. You can add notes by editing this application.
              </span>
            )}
          </div>
              {isNotesCollapsible && app.notes && (
                <button 
                  onClick={() => setIsNotesCollapsed(!isNotesCollapsed)} 
                  className="collapsible-toggle-btn"
                  style={{ padding: '4px 8px', marginTop: '4px' }}
                >
                  <span>{isNotesCollapsed ? 'Show More' : 'Show Less'}</span>
                  <ChevronIcon direction={isNotesCollapsed ? 'down' : 'up'} style={{ width: '16px', height: '16px' }} />
                </button>
              )}
        </div>

        {/* Card 4: Documents & Attachments */}
        {((app.resume) || (app.coverLetter) || (app.additionalDocument) || (app.screenshots && app.screenshots.length > 0)) && (
          <div className="card-base" style={{ padding: '24px' }}>
            <h3 className="section-title" style={{ marginBottom: '16px' }}>Documents & Attachments</h3>
            <div className="file-upload-slots" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              
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
                      <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Resume / CV</div>
                      <span className="file-slot-filename" style={{ color: 'var(--link-blue)' }}>{app.resume.originalName}</span>
                    </div>
                  </div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)' }}>View PDF</span>
                </div>
              )}

              {app.resume && ((app.coverLetter) || (app.additionalDocument)) && (
                <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>
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

              {app.coverLetter && app.additionalDocument && (
                <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>
              )}

              {/* Additional Document */}
              {app.additionalDocument && (
                <div 
                  className="file-slot" 
                  style={{ cursor: 'pointer' }}
                  onClick={() => handleAttachmentClick(app.additionalDocument, 'pdf')}
                >
                  <div className="file-slot-info">
                    <FileIcon />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.8rem' }}>Additional Document</div>
                      <span className="file-slot-filename" style={{ color: 'var(--link-blue)' }}>{app.additionalDocument.originalName}</span>
                    </div>
                  </div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)' }}>View PDF</span>
                </div>
              )}

              {/* Screenshots list in Android-style grid */}
              {app.screenshots && app.screenshots.length > 0 && (
                <div style={{ marginTop: '8px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '8px' }}></div>
                  <div style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    Screenshots / Additional Images
                  </div>
                  <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
                    {app.screenshots.map((shot, index) => (
                      <div 
                        key={index}
                        onClick={() => {
                          setActiveImageIndex(index);
                        }}
                        style={{
                          width: '80px',
                          height: '80px',
                          borderRadius: '8px',
                          border: '1px solid var(--brand-outline)',
                          overflow: 'hidden',
                          cursor: 'pointer',
                          transition: 'transform var(--transition-fast)'
                        }}
                        className="screenshot-thumbnail-hover"
                      >
                        <img 
                          src={shot.url || shot.dataUrl} 
                          alt={`Screenshot ${index + 1}`} 
                          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}

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
      {activeImageIndex !== null && app.screenshots && (
        <ImageViewer 
          files={app.screenshots} 
          initialIndex={activeImageIndex} 
          onClose={() => setActiveImageIndex(null)} 
        />
      )}
      {activePdfFile && (
        <PDFViewer file={activePdfFile} onClose={() => setActivePdfFile(null)} />
      )}
    </div>
  );
}
