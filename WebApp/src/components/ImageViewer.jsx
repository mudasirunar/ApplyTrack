import React, { useState, useEffect, useRef } from 'react';
import { useBodyScrollLock } from '../utils/useBodyScrollLock';
import { CloseIcon, ChevronIcon, DownloadIcon, ExternalLinkIcon, ZoomInIcon, ZoomOutIcon } from './Icons';
import './ViewerModal.css';

function dataUrlToBlob(dataUrl) {
  try {
    const parts = dataUrl.split(',');
    if (parts.length < 2) return null;
    const mimeMatch = parts[0].match(/:(.*?);/);
    const mime = mimeMatch ? mimeMatch[1] : 'image/png';
    const bstr = atob(parts[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new Blob([u8arr], { type: mime });
  } catch {
    return null;
  }
}

export default function ImageViewer({ files, initialIndex, onClose }) {
  const [activeIndex, setActiveIndex] = useState(initialIndex || 0);
  const [imageLoading, setImageLoading] = useState(true);
  const [isZoomed, setIsZoomed] = useState(false);
  const touchStartX = useRef(null);

  const file = (files && files.length > 0) ? files[activeIndex] : null;

  useBodyScrollLock(Boolean(file));

  useEffect(() => {
    const prevTitle = document.title;
    if (file && file.originalName) {
      document.title = file.originalName;
    }

    return () => {
      document.title = prevTitle;
    };
  }, [file]);

  // Whenever the active index changes, reset loading state to true and unzoom
  useEffect(() => {
    setImageLoading(true);
    setIsZoomed(false);
  }, [activeIndex]);

  // Predictive background preloading of adjacent images for zero-lag switching
  useEffect(() => {
    if (!files || files.length <= 1) return;
    const nextIdx = (activeIndex + 1) % files.length;
    const prevIdx = (activeIndex - 1 + files.length) % files.length;
    [nextIdx, prevIdx].forEach(idx => {
      const f = files[idx];
      const src = f?.url || f?.dataUrl;
      if (src) {
        const img = new Image();
        img.src = src;
      }
    });
  }, [activeIndex, files]);

  // Keyboard navigation: ArrowLeft, ArrowRight, Escape
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        if (onClose) onClose();
      } else if (e.key === 'ArrowLeft' && files && files.length > 1) {
        setActiveIndex(prev => (prev === 0 ? files.length - 1 : prev - 1));
      } else if (e.key === 'ArrowRight' && files && files.length > 1) {
        setActiveIndex(prev => (prev === files.length - 1 ? 0 : prev + 1));
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [files, onClose]);

  if (!file) return null;

  const handlePrev = (e) => {
    if (e) e.stopPropagation();
    setActiveIndex(prev => (prev === 0 ? files.length - 1 : prev - 1));
  };

  const handleNext = (e) => {
    if (e) e.stopPropagation();
    setActiveIndex(prev => (prev === files.length - 1 ? 0 : prev + 1));
  };

  const toggleZoom = (e) => {
    if (e) e.stopPropagation();
    setIsZoomed(prev => !prev);
  };

  const handleOpenInNewTab = (e) => {
    if (e) e.stopPropagation();
    const url = file.url || file.dataUrl;
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  };

  // Touch swipe support for mobile devices
  const handleTouchStart = (e) => {
    if (isZoomed) return; // Don't trigger swipe navigation while inspecting zoomed image
    touchStartX.current = e.touches[0].clientX;
  };

  const handleTouchEnd = (e) => {
    if (isZoomed || touchStartX.current === null || !files || files.length <= 1) return;
    const touchEndX = e.changedTouches[0].clientX;
    const diff = touchStartX.current - touchEndX;
    if (Math.abs(diff) > 40) {
      if (diff > 0) {
        handleNext();
      } else {
        handlePrev();
      }
    }
    touchStartX.current = null;
  };

  const handleDownload = async (e) => {
    if (e) e.stopPropagation();
    try {
      const url = file.url || file.dataUrl;
      let blob;
      if (url.startsWith('data:')) {
        blob = dataUrlToBlob(url);
      }
      if (!blob) {
        const response = await fetch(url);
        blob = await response.blob();
      }
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
      window.open(file.url || file.dataUrl, '_blank');
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div 
        className="viewer-modal-card" 
        onClick={(e) => e.stopPropagation()}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
      >
        <div className="viewer-header">
          <span className="viewer-title">
            {file.originalName || `Screenshot ${activeIndex + 1}`} ({activeIndex + 1}/{files.length})
          </span>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <button 
              onClick={toggleZoom} 
              className="job-card-action-btn" 
              title={isZoomed ? "Zoom Out" : "Zoom In (Click image)"}
            >
              {isZoomed ? (
                <ZoomOutIcon style={{ width: '18px', height: '18px' }} />
              ) : (
                <ZoomInIcon style={{ width: '18px', height: '18px' }} />
              )}
            </button>
            <button 
              onClick={handleOpenInNewTab} 
              className="job-card-action-btn" 
              title="Open in New Tab"
            >
              <ExternalLinkIcon style={{ width: '18px', height: '18px' }} />
            </button>
            <button onClick={handleDownload} className="job-card-action-btn" title="Download Image">
              <DownloadIcon style={{ width: '18px', height: '18px' }} />
            </button>
            <button onClick={onClose} className="job-card-action-btn" title="Close (Esc)">
              <CloseIcon />
            </button>
          </div>
        </div>
        <div 
          className="viewer-content" 
          style={{ 
            position: 'relative', 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center', 
            minHeight: '300px',
            overflow: isZoomed ? 'auto' : 'hidden' 
          }}
        >
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
          
          {files.length > 1 && !isZoomed && (
            <button className="viewer-nav-btn prev" onClick={handlePrev} title="Previous image (←)">
              <ChevronIcon direction="left" />
            </button>
          )}
          
          <img 
            src={file.url || file.dataUrl} 
            alt={file.originalName} 
            className="viewer-image"
            onClick={toggleZoom}
            onLoad={() => setImageLoading(false)}
            style={{ 
              opacity: imageLoading ? 0 : 1, 
              transition: 'opacity 0.2s ease-in-out, transform 0.25s ease-in-out',
              transform: isZoomed ? 'scale(1.75)' : 'none',
              cursor: isZoomed ? 'zoom-out' : 'zoom-in',
              maxWidth: isZoomed ? 'none' : '100%',
              maxHeight: isZoomed ? 'none' : '100%'
            }}
          />
          
          {files.length > 1 && !isZoomed && (
            <button className="viewer-nav-btn next" onClick={handleNext} title="Next image (→)">
              <ChevronIcon direction="right" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
