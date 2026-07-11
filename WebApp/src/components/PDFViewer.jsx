import React, { useState, useEffect } from 'react';
import { CloseIcon, FileIcon, DownloadIcon } from './Icons';
import './ViewerModal.css';

export default function PDFViewer({ file, onClose }) {
  const [iframeLoading, setIframeLoading] = useState(true);
  const [pdfBlobUrl, setPdfBlobUrl] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, []);

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

        // Fetch file data to bypass the remote Content-Disposition: attachment header
        const res = await fetch(rawUrl);
        if (!res.ok) throw new Error("Failed to load PDF file from server.");

        const blob = await res.blob();
        // Force the MIME type to application/pdf so the browser native viewer opens it
        const pdfBlob = new Blob([blob], { type: 'application/pdf' });
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
      const response = await fetch(pdfUrl);
      const blob = await response.blob();
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
            <div className="viewer-fallback-text" style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center' }}>
              <FileIcon style={{ width: '48px', height: '48px', fill: 'var(--text-secondary)' }} />
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
