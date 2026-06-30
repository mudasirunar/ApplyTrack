import React, { useEffect } from 'react';
import { CloseIcon, FileIcon } from './Icons';
import './ViewerModal.css';

export default function PDFViewer({ file, onClose }) {
  useEffect(() => {
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, []);

  if (!file) return null;

  const pdfUrl = file.url || file.dataUrl;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="viewer-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="viewer-header">
          <span className="viewer-title">{file.originalName || 'PDF Viewer'}</span>
          <button onClick={onClose} className="job-card-action-btn" title="Close">
            <CloseIcon />
          </button>
        </div>
        <div className="viewer-content" style={{ padding: 0 }}>
          {pdfUrl ? (
            <iframe 
              src={pdfUrl} 
              title={file.originalName} 
              className="viewer-iframe"
            />
          ) : (
            <div className="viewer-fallback-text">
              <FileIcon style={{ width: '48px', height: '48px', fill: 'var(--text-secondary)' }} />
              <p style={{ fontWeight: 600 }}>Unable to render PDF preview directly.</p>
              <a 
                href={pdfUrl} 
                download={file.originalName} 
                className="btn-primary"
                style={{ textDecoration: 'none', display: 'inline-block', marginTop: '8px' }}
              >
                Download PDF to View
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
