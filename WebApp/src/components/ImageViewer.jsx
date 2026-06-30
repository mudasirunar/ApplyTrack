import React, { useState, useEffect } from 'react';
import { CloseIcon, ChevronIcon } from './Icons';
import './ViewerModal.css';

export default function ImageViewer({ files, initialIndex, onClose }) {
  const [activeIndex, setActiveIndex] = useState(initialIndex || 0);

  useEffect(() => {
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, []);

  if (!files || files.length === 0) return null;
  const file = files[activeIndex];
  if (!file) return null;

  const handlePrev = (e) => {
    e.stopPropagation();
    setActiveIndex(prev => (prev === 0 ? files.length - 1 : prev - 1));
  };

  const handleNext = (e) => {
    e.stopPropagation();
    setActiveIndex(prev => (prev === files.length - 1 ? 0 : prev + 1));
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="viewer-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="viewer-header">
          <span className="viewer-title">
            {file.originalName || `Screenshot ${activeIndex + 1}`} ({activeIndex + 1}/{files.length})
          </span>
          <button onClick={onClose} className="job-card-action-btn" title="Close">
            <CloseIcon />
          </button>
        </div>
        <div className="viewer-content">
          {files.length > 1 && (
            <button className="viewer-nav-btn prev" onClick={handlePrev} title="Previous image">
              <ChevronIcon direction="left" />
            </button>
          )}
          
          <img 
            src={file.url || file.dataUrl} 
            alt={file.originalName} 
            className="viewer-image"
          />
          
          {files.length > 1 && (
            <button className="viewer-nav-btn next" onClick={handleNext} title="Next image">
              <ChevronIcon direction="right" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
