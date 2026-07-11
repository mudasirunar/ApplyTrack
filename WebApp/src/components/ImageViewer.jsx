import React, { useState, useEffect } from 'react';
import { CloseIcon, ChevronIcon, DownloadIcon } from './Icons';
import './ViewerModal.css';

export default function ImageViewer({ files, initialIndex, onClose }) {
  const [activeIndex, setActiveIndex] = useState(initialIndex || 0);
  const [imageLoading, setImageLoading] = useState(true);

  const file = (files && files.length > 0) ? files[activeIndex] : null;

  useEffect(() => {
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    
    const prevTitle = document.title;
    if (file && file.originalName) {
      document.title = file.originalName;
    }

    return () => {
      document.body.style.overflow = originalOverflow;
      document.title = prevTitle;
    };
  }, [file]);

  // Whenever the active index changes, reset loading state to true
  useEffect(() => {
    setImageLoading(true);
  }, [activeIndex]);

  if (!file) return null;

  const handlePrev = (e) => {
    e.stopPropagation();
    setActiveIndex(prev => (prev === 0 ? files.length - 1 : prev - 1));
  };

  const handleNext = (e) => {
    e.stopPropagation();
    setActiveIndex(prev => (prev === files.length - 1 ? 0 : prev + 1));
  };

  const handleDownload = async (e) => {
    e.stopPropagation();
    try {
      const url = file.url || file.dataUrl;
      const response = await fetch(url);
      const blob = await response.blob();
      const blobUrl = URL.createObjectURL(blob);
      
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = file.originalName || `screenshot_${activeIndex + 1}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(blobUrl);
    } catch (err) {
      console.error('Failed to download image:', err);
      // Fallback: open in new tab
      window.open(file.url || file.dataUrl, '_blank');
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="viewer-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="viewer-header">
          <span className="viewer-title">
            {file.originalName || `Screenshot ${activeIndex + 1}`} ({activeIndex + 1}/{files.length})
          </span>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <button onClick={handleDownload} className="job-card-action-btn" title="Download Image">
              <DownloadIcon style={{ width: '18px', height: '18px' }} />
            </button>
            <button onClick={onClose} className="job-card-action-btn" title="Close">
              <CloseIcon />
            </button>
          </div>
        </div>
        <div className="viewer-content" style={{ position: 'relative', display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '300px' }}>
          {imageLoading && (
            <div style={{
              position: 'absolute',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 5
            }}>
              <div className="signin-spinner-container">
                <div className="signin-spinner-ring"></div>
              </div>
            </div>
          )}
          
          {files.length > 1 && (
            <button className="viewer-nav-btn prev" onClick={handlePrev} title="Previous image">
              <ChevronIcon direction="left" />
            </button>
          )}
          
          <img 
            src={file.url || file.dataUrl} 
            alt={file.originalName} 
            className="viewer-image"
            onLoad={() => setImageLoading(false)}
            style={{ opacity: imageLoading ? 0 : 1, transition: 'opacity 0.2s ease-in-out' }}
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
