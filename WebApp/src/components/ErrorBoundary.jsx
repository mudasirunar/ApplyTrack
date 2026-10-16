import React from 'react';
import './ErrorBoundary.css';

/**
 * ErrorBoundary component that catches runtime rendering errors in its child
 * component tree and renders a graceful, user-friendly recovery UI instead of
 * crashing the entire web application to a blank white screen.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false
    };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({ errorInfo });
    // Log structured error telemetry
    console.error('ErrorBoundary caught an unhandled rendering error:', error, errorInfo);
    if (typeof this.props.onError === 'function') {
      try {
        this.props.onError(error, errorInfo);
      } catch (loggingError) {
        console.error('ErrorBoundary onError callback failed:', loggingError);
      }
    }
  }

  handleReload = () => {
    if (typeof window !== 'undefined') {
      window.location.reload();
    }
  };

  handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false
    });
    if (typeof this.props.onReset === 'function') {
      this.props.onReset();
    }
  };

  handleGoHome = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false
    });
    if (typeof window !== 'undefined') {
      window.location.href = '/dashboard';
    }
  };

  handleClearCacheAndReload = () => {
    if (typeof window !== 'undefined') {
      try {
        // Clear cached application lists that might contain malformed or corrupted JSON
        window.localStorage.removeItem('applytrack_applications');
        window.localStorage.removeItem('applivra_applications');
        window.localStorage.removeItem('applytrack_deleted_temp');
        window.localStorage.removeItem('applivra_deleted_temp');
        window.sessionStorage.clear();
      } catch (storageError) {
        console.error('ErrorBoundary failed to clear cache:', storageError);
      }
      window.location.reload();
    }
  };

  toggleDetails = () => {
    this.setState((prev) => ({ showDetails: !prev.showDetails }));
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return typeof this.props.fallback === 'function'
          ? this.props.fallback({
              error: this.state.error,
              errorInfo: this.state.errorInfo,
              resetError: this.handleReset,
              reload: this.handleReload
            })
          : this.props.fallback;
      }

      const scopeClass = this.props.scope === 'root' ? 'scope-root' : 'scope-content';

      return (
        <div className={`error-boundary-container ${scopeClass}`} role="alert" aria-live="assertive">
          <div className="error-boundary-card">
            <div className="error-boundary-icon-wrapper" aria-hidden="true">
              <svg 
                viewBox="0 0 24 24" 
                width="36" 
                height="36" 
                fill="none" 
                stroke="currentColor" 
                strokeLinecap="round" 
                strokeLinejoin="round"
                className="error-boundary-icon"
              >
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
            </div>

            <h2 className="error-boundary-title">Something went wrong</h2>
            <p className="error-boundary-subtitle">
              An unexpected error occurred while displaying this page. Your data remains safely preserved in local storage and cloud sync.
            </p>

            <div className="error-boundary-actions">
              <button 
                type="button"
                className="error-boundary-btn error-boundary-btn-primary" 
                onClick={this.handleReload}
                aria-label="Reload current application page"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" aria-hidden="true">
                  <path d="M17.65 6.35A7.958 7.958 0 0012 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0112 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                </svg>
                Reload Page
              </button>

              <button 
                type="button"
                className="error-boundary-btn error-boundary-btn-secondary" 
                onClick={this.handleGoHome}
                aria-label="Return to application dashboard"
              >
                Return to Dashboard
              </button>
            </div>

            <div className="error-boundary-footer">
              <button 
                type="button" 
                className="error-boundary-link-btn" 
                onClick={this.handleClearCacheAndReload}
                title="Clears local cache and re-downloads fresh applications from Cloud Firestore"
              >
                Clear Corrupted Cache &amp; Reload
              </button>

              <button 
                type="button" 
                className="error-boundary-link-btn" 
                onClick={this.toggleDetails}
                aria-expanded={this.state.showDetails}
              >
                {this.state.showDetails ? 'Hide Error Details' : 'View Error Details'}
              </button>
            </div>

            {this.state.showDetails && (
              <div className="error-boundary-details">
                <p className="error-boundary-message">
                  <strong>Error:</strong> {this.state.error ? (this.state.error.message || String(this.state.error)) : 'Unknown runtime error'}
                </p>
                {this.state.errorInfo?.componentStack && (
                  <pre className="error-boundary-stack">
                    {this.state.errorInfo.componentStack}
                  </pre>
                )}
              </div>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
