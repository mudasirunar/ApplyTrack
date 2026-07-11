import React, { useState, useEffect } from 'react';
import { CloseIcon, FileIcon, DownloadIcon } from './Icons';
import './ViewerModal.css';

// Global module-level cache to store resolved document Blobs across mounts
const pdfBlobCache = new Map();

const isMobile = typeof navigator !== 'undefined' && /Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent);

export default function PDFViewer({ file, onClose }) {
  const [iframeLoading, setIframeLoading] = useState(true);
  const [pdfBlobUrl, setPdfBlobUrl] = useState(null);
  const [error, setError] = useState(null);

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

        if (isMobile) {
          // On mobile, iframes cannot render local blob PDFs or direct attachment downloads.
          // Fall back to Google Docs PDF viewer which works beautifully on iOS and Android.
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
        } else {
          // Fetch file data to bypass the remote Content-Disposition: attachment header
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

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="viewer-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="viewer-header">
          <span className="viewer-title">{file.originalName || 'PDF Viewer'}</span>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            {pdfUrl && (
              <button onClick={handleDownload} className="job-card-action-btn" title="Download PDF">
                <DownloadIcon style={{ width: '18px', height: '18px' }} />
              </button>
            )}
            <button onClick={onClose} className="job-card-action-btn" title="Close">
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
                <button 
                  onClick={handleDownload} 
                  className="btn-primary"
                  style={{ marginTop: '8px' }}
                >
                  Download PDF to View
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
