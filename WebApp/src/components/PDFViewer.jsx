import React, { useState, useEffect } from 'react';
import { useBodyScrollLock } from '../utils/useBodyScrollLock';
import { CloseIcon, FileIcon, DownloadIcon, ExternalLinkIcon } from './Icons';
import './ViewerModal.css';

// Global module-level cache to store resolved document Blobs across mounts
const pdfBlobCache = new Map();

const isMobile = typeof navigator !== 'undefined' && /Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent);

function dataUrlToBlob(dataUrl) {
  try {
    const parts = dataUrl.split(',');
    if (parts.length < 2) return null;
    const mimeMatch = parts[0].match(/:(.*?);/);
    const mime = mimeMatch ? mimeMatch[1] : 'application/pdf';
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

export default function PDFViewer({ file, onClose }) {
  const [iframeLoading, setIframeLoading] = useState(true);
  const [pdfBlobUrl, setPdfBlobUrl] = useState(null);
  const [error, setError] = useState(null);

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

  // Keyboard shortcut to close on Escape
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && onClose) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  // Safety fallback for iframe loading state (native PDF viewers may not trigger onLoad reliably)
  useEffect(() => {
    if (!iframeLoading) return;
    const timer = setTimeout(() => {
      setIframeLoading(false);
    }, 1200);
    return () => clearTimeout(timer);
  }, [iframeLoading, pdfBlobUrl]);

  useEffect(() => {
    if (!file) return;

    let active = true;
    let createdUrl = null;

    async function loadPdf() {
      try {
        setIframeLoading(true);
        setError(null);

        const rawUrl = file.url || file.dataUrl;
        if (!rawUrl) {
          throw new Error("No URL found for this file.");
        }

        // On mobile, iframes cannot render blob URLs directly. Use Google Docs viewer for remote URLs
        if (isMobile && rawUrl.startsWith('http')) {
          const googleViewerUrl = `https://docs.google.com/gview?url=${encodeURIComponent(rawUrl)}&embedded=true`;
          if (active) {
            setPdfBlobUrl(googleViewerUrl);
          }
          return;
        }

        let pdfBlob;
        // Check if the document has already been loaded in this session
        if (pdfBlobCache.has(rawUrl)) {
          pdfBlob = pdfBlobCache.get(rawUrl);
        } else if (rawUrl.startsWith('data:')) {
          // Instant in-memory base64 conversion (0ms)
          pdfBlob = dataUrlToBlob(rawUrl);
          if (!pdfBlob) {
            const res = await fetch(rawUrl);
            pdfBlob = await res.blob();
          }
          pdfBlob = new Blob([pdfBlob], { type: 'application/pdf' });
          pdfBlobCache.set(rawUrl, pdfBlob);
        } else {
          // Fetch file data to bypass remote Content-Disposition: attachment header
          const res = await fetch(rawUrl);
          if (!res.ok) throw new Error("Failed to load PDF file from server.");

          const blob = await res.blob();
          // Force the MIME type to application/pdf so the browser native viewer opens it
          pdfBlob = new Blob([blob], { type: 'application/pdf' });
          pdfBlobCache.set(rawUrl, pdfBlob);
        }

        createdUrl = URL.createObjectURL(pdfBlob);

        if (active) {
          setPdfBlobUrl(createdUrl);
        }
      } catch (err) {
        console.error('Error fetching PDF:', err);
        if (active) {
          setError("Unable to render PDF preview directly.");
          setIframeLoading(false);
        }
      }
    }

    loadPdf();

    return () => {
      active = false;
      if (createdUrl) {
        URL.revokeObjectURL(createdUrl);
      }
    };
  }, [file]);

  const pdfUrl = file.url || file.dataUrl;

  const handleDownload = async (e) => {
    if (e) e.stopPropagation();
    try {
      let blob;
      if (pdfBlobCache.has(pdfUrl)) {
        blob = pdfBlobCache.get(pdfUrl);
      } else {
        const response = await fetch(pdfUrl);
        blob = await response.blob();
      }
      const blobUrl = URL.createObjectURL(blob);
      
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = file.originalName || 'document.pdf';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(blobUrl);
    } catch (err) {
      console.error('Failed to download PDF:', err);
      window.open(pdfUrl, '_blank');
    }
  };

  const handleOpenInNewTab = (e) => {
    if (e) e.stopPropagation();
    const target = pdfBlobUrl || pdfUrl;
    if (target) {
      window.open(target, '_blank', 'noopener,noreferrer');
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="viewer-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="viewer-header">
          <span className="viewer-title">{file.originalName || 'PDF Viewer'}</span>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            {pdfUrl && (
              <>
                <button onClick={handleOpenInNewTab} className="job-card-action-btn" title="Open in New Tab">
                  <ExternalLinkIcon style={{ width: '18px', height: '18px' }} />
                </button>
                <button onClick={handleDownload} className="job-card-action-btn" title="Download PDF">
                  <DownloadIcon style={{ width: '18px', height: '18px' }} />
                </button>
              </>
            )}
            <button onClick={onClose} className="job-card-action-btn" title="Close (Esc)">
              <CloseIcon />
            </button>
          </div>
        </div>
        <div className="viewer-content" style={{ padding: 0, position: 'relative', display: 'flex', flexDirection: 'column', minHeight: '300px' }}>
          {pdfBlobUrl && !error ? (
            <>
              {iframeLoading && (
                <div style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  backgroundColor: 'var(--bg-surface)',
                  zIndex: 5
                }}>
                  <div className="signin-spinner-container">
                    <div className="signin-spinner-ring"></div>
                  </div>
                </div>
              )}
              <iframe 
                src={pdfBlobUrl} 
                title={file.originalName} 
                className="viewer-iframe"
                onLoad={() => setIframeLoading(false)}
                style={{ width: '100%', height: '100%', flex: 1, border: 'none' }}
              />
            </>
          ) : (
            <div className="viewer-fallback-text" style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', gap: '16px' }}>
              {!error ? (
                <div style={{ position: 'relative', width: '80px', height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <div className="signin-spinner-ring" style={{ width: '100%', height: '100%', borderWidth: '3px', borderColor: 'var(--brand-primary) transparent transparent transparent' }} />
                  <FileIcon style={{ width: '32px', height: '32px', fill: 'var(--brand-primary)' }} />
                </div>
              ) : (
                <FileIcon style={{ width: '48px', height: '48px', fill: 'var(--text-secondary)' }} />
              )}
              <p style={{ fontWeight: 600 }}>{error || "Loading document preview..."}</p>
              {error && (
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', justifyContent: 'center' }}>
                  <button 
                    onClick={handleOpenInNewTab} 
                    className="btn-secondary"
                    style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <ExternalLinkIcon style={{ width: '16px', height: '16px' }} />
                    Open in New Tab
                  </button>
                  <button 
                    onClick={handleDownload} 
                    className="btn-primary"
                    style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <DownloadIcon style={{ width: '16px', height: '16px' }} />
                    Download PDF
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
