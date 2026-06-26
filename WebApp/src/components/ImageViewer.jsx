import React from 'react';
import { CloseIcon } from './Icons';

export default function ImageViewer({ file, onClose }) {
  if (!file) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="viewer-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="viewer-header">
          <span className="viewer-title">{file.originalName || 'Image Viewer'}</span>
          <button onClick={onClose} className="job-card-action-btn" title="Close">
            <CloseIcon />
          </button>
        </div>
        <div className="viewer-content">
          <img 
            src={file.url || file.dataUrl} 
            alt={file.originalName} 
            className="viewer-image"
          />
        </div>
      </div>
    </div>
  );
}
