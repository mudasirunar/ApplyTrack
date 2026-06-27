import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { db } from '../utils/db';
import { 
  ChevronIcon, 
  CalendarIcon, 
  LinkIcon, 
  EmailIcon, 
  FileIcon,
  EditIcon,
  DeleteIcon,
  WorkIcon,
  InfoIcon,
  NotesIcon
} from '../components/Icons';
import ImageViewer from '../components/ImageViewer';
import PDFViewer from '../components/PDFViewer';
import './JobDetail.css';

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

function InfoRow({ label, value, isLink, href }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid var(--brand-outline-light, rgba(0,0,0,0.03))' }}>
      <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 500 }}>{label}</span>
      {isLink ? (
        <a 
          href={href} 
          target="_blank" 
          rel="noopener noreferrer" 
          style={{ 
            color: 'var(--link-blue)', 
            fontSize: '0.85rem', 
            fontWeight: 600, 
            textDecoration: 'underline',
            maxWidth: '65%',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap'
          }}
        >
          {value}
        </a>
      ) : (
        <span style={{ color: 'var(--text-primary)', fontSize: '0.85rem', fontWeight: 600, maxWidth: '65%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {value}
        </span>
      )}
    </div>
  );
}

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
  const [showDeleteModal, setShowDeleteModal] = useState(false);

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
    setShowDeleteModal(true);
  };

  const handleConfirmDelete = () => {
    db.deleteApplication(app.id);
    setShowDeleteModal(false);
    
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
          window.dispatchEvent(new CustomEvent('applytrack_toast', { detail: { message: 'Restored application' } }));
        }
      }
    }));
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

  const getHrefUrl = (rawUrl) => {
    if (!rawUrl) return '';
    if (!/^https?:\/\//i.test(rawUrl)) {
      return `https://${rawUrl}`;
    }
    return rawUrl;
  };

  const formatMetadataDate = (timestamp) => {
    if (!timestamp) return 'Unrecorded';
    return new Date(timestamp).toLocaleString(undefined, {
      month: 'short',
      day: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
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

        {/* Card 1: Detail Header Card */}
        <div className="card-base detail-header-card" style={{ padding: '16px', display: 'flex', alignItems: 'center' }}>
          <div 
            style={{ 
              width: '60px', 
              height: '60px', 
              borderRadius: '12px', 
              backgroundColor: 'rgba(47, 58, 74, 0.08)', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center',
              flexShrink: 0
            }}
          >
            <WorkIcon style={{ width: '28px', height: '28px', color: 'var(--brand-primary)' }} />
          </div>

          <div style={{ marginLeft: '16px', flex: 1, minWidth: 0 }}>
            <h2 
              style={{ 
                margin: 0, 
                fontSize: '1.25rem', 
                fontWeight: 800, 
                color: 'var(--brand-primary)',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis'
              }}
            >
              {app.role || "Position unassigned"}
            </h2>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '4px' }}>
              <span style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '60%' }}>
                {app.companyName || "Unknown Company"}
              </span>
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
          </div>
        </div>

        {/* Card 2: Collapsible Job Description */}
        <div className="card-base collapsible-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 className="section-title" style={{ display: 'flex', alignItems: 'center', gap: '8px', margin: 0 }}>
              <InfoIcon style={{ width: '20px', height: '20px', color: 'var(--brand-primary)' }} />
              <span>Job Description</span>
            </h3>
          </div>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '12px' }}></div>
          <div 
            ref={descRef}
            className={`collapsible-content ${isCollapsed ? 'collapsed' : ''}`}
          >
            {app.jobDescription ? (
              app.jobDescription.trim().toLowerCase().startsWith('http://') || app.jobDescription.trim().toLowerCase().startsWith('https://') ? (
                <a 
                  href={app.jobDescription.trim()} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  style={{ textDecoration: 'underline', color: 'var(--link-blue)', wordBreak: 'break-all' }}
                >
                  {app.jobDescription}
                </a>
              ) : (
                app.jobDescription
              )
            ) : (
              <span style={{ fontStyle: 'italic', color: 'var(--text-secondary)', opacity: 0.8 }}>
                No job description has been recorded. You can add one by editing this application.
              </span>
            )}
          </div>
          {isDescCollapsible && app.jobDescription && (
            <button onClick={() => setIsCollapsed(!isCollapsed)} className="collapsible-toggle-btn">
              <span>{isCollapsed ? 'Show More' : 'Show Less'}</span>
              <ChevronIcon direction={isCollapsed ? 'down' : 'up'} style={{ width: '16px', height: '16px' }} />
            </button>
          )}
        </div>

        {/* Card 3: Personal Notes */}
        <div className="card-base notes-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 className="section-title" style={{ display: 'flex', alignItems: 'center', gap: '8px', margin: 0 }}>
              <NotesIcon style={{ width: '20px', height: '20px', color: 'var(--brand-primary)' }} />
              <span>Personal Notes</span>
            </h3>
          </div>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '12px' }}></div>

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

        {/* Card 4: Platform Details Card */}
        <div className="card-base platform-details-card" style={{ padding: '24px' }}>
          <h3 className="section-title" style={{ marginBottom: '12px' }}>Platform Details</h3>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '12px' }}></div>
          
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <InfoRow label="Platform" value={app.platform || "Unrecorded (Direct)"} />
            <InfoRow 
              label="URL" 
              value={app.url ? app.url : "Not specified"} 
              isLink={!!app.url} 
              href={app.url ? getHrefUrl(app.url) : undefined} 
            />
            {app.email && (
              <InfoRow 
                label="Contact Email" 
                value={app.email} 
                isLink={true} 
                href={`mailto:${app.email}`} 
              />
            )}
          </div>
        </div>

        {/* Card 5: Documents & Attachments */}
        {((app.resume) || (app.coverLetter) || (app.additionalDocument) || (app.screenshots && app.screenshots.length > 0)) && (
          <div className="card-base" style={{ padding: '24px' }}>
            <h3 className="section-title" style={{ marginBottom: '12px' }}>Documents & Attachments</h3>
            <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '12px' }}></div>
            <div className="file-upload-slots" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              
              {/* Resume */}
              {app.resume && (
                <div 
                  className="file-slot" 
                  style={{ 
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    borderRadius: 'var(--border-radius-sm)',
                    border: '1px dashed var(--brand-outline)',
                    backgroundColor: 'var(--bg-surface-variant)',
                    gap: '12px'
                  }}
                  onClick={() => handleAttachmentClick(app.resume, 'pdf')}
                >
                  <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <div style={{ fontWeight: 800, fontSize: '0.85rem', color: 'var(--text-primary)' }}>Resume / CV</div>
                    <span style={{ 
                      fontSize: '0.75rem', 
                      color: 'var(--brand-primary)',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      display: 'block',
                      maxWidth: '100%'
                    }}>
                      {app.resume.originalName}
                    </span>
                  </div>
                  <div 
                    style={{ 
                      padding: '6px 12px', 
                      borderRadius: '8px', 
                      backgroundColor: 'rgba(47, 58, 74, 0.08)', 
                      color: 'var(--brand-primary)', 
                      fontSize: '0.75rem', 
                      fontWeight: 700,
                      flexShrink: 0
                    }}
                  >
                    View
                  </div>
                </div>
              )}

              {app.resume && ((app.coverLetter) || (app.additionalDocument)) && (
                <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>
              )}

              {/* Cover Letter */}
              {app.coverLetter && (
                <div 
                  className="file-slot" 
                  style={{ 
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    borderRadius: 'var(--border-radius-sm)',
                    border: '1px dashed var(--brand-outline)',
                    backgroundColor: 'var(--bg-surface-variant)',
                    gap: '12px'
                  }}
                  onClick={() => handleAttachmentClick(app.coverLetter, 'pdf')}
                >
                  <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <div style={{ fontWeight: 800, fontSize: '0.85rem', color: 'var(--text-primary)' }}>Cover Letter</div>
                    <span style={{ 
                      fontSize: '0.75rem', 
                      color: 'var(--brand-primary)',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      display: 'block',
                      maxWidth: '100%'
                    }}>
                      {app.coverLetter.originalName}
                    </span>
                  </div>
                  <div 
                    style={{ 
                      padding: '6px 12px', 
                      borderRadius: '8px', 
                      backgroundColor: 'rgba(47, 58, 74, 0.08)', 
                      color: 'var(--brand-primary)', 
                      fontSize: '0.75rem', 
                      fontWeight: 700,
                      flexShrink: 0
                    }}
                  >
                    View
                  </div>
                </div>
              )}

              {app.coverLetter && app.additionalDocument && (
                <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%' }}></div>
              )}

              {/* Additional Document */}
              {app.additionalDocument && (
                <div 
                  className="file-slot" 
                  style={{ 
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    borderRadius: 'var(--border-radius-sm)',
                    border: '1px dashed var(--brand-outline)',
                    backgroundColor: 'var(--bg-surface-variant)',
                    gap: '12px'
                  }}
                  onClick={() => handleAttachmentClick(app.additionalDocument, 'pdf')}
                >
                  <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <div style={{ fontWeight: 800, fontSize: '0.85rem', color: 'var(--text-primary)' }}>Additional Document</div>
                    <span style={{ 
                      fontSize: '0.75rem', 
                      color: 'var(--brand-primary)',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      display: 'block',
                      maxWidth: '100%'
                    }}>
                      {app.additionalDocument.originalName}
                    </span>
                  </div>
                  <div 
                    style={{ 
                      padding: '6px 12px', 
                      borderRadius: '8px', 
                      backgroundColor: 'rgba(47, 58, 74, 0.08)', 
                      color: 'var(--brand-primary)', 
                      fontSize: '0.75rem', 
                      fontWeight: 700,
                      flexShrink: 0
                    }}
                  >
                    View
                  </div>
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
                          width: '70px',
                          height: '70px',
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
          <h3 className="section-title" style={{ marginBottom: '12px' }}>Status History Timeline</h3>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '12px' }}></div>
          <div className="timeline-list">
            {(() => {
              const historyEntries = app.statusHistory && app.statusHistory.length > 0
                ? app.statusHistory
                : [{ status: app.status, timestamp: app.createdAt }];
              
              return [...historyEntries].reverse().map((history, idx) => (
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
              ));
            })()}
          </div>
        </div>

        {/* Card 7: Metadata Card */}
        <div className="card-base metadata-card" style={{ padding: '24px' }}>
          <h3 className="section-title" style={{ marginBottom: '12px' }}>Metadata</h3>
          <div style={{ borderBottom: '1px solid var(--brand-outline)', width: '100%', marginBottom: '12px' }}></div>
          
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <InfoRow label="Created Time" value={formatMetadataDate(app.createdAt)} />
            <InfoRow label="Last Updated" value={formatMetadataDate(app.updatedAt || app.createdAt)} />
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
      {showDeleteModal && createPortal(
        <ConfirmationModal
          title="Delete Application"
          message="Are you sure you want to delete this job application?"
          confirmLabel="Delete"
          isDestructive={true}
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowDeleteModal(false)}
        />,
        document.body
      )}
    </div>
  );
}
