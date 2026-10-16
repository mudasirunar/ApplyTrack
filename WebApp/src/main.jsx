import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary scope="root">
      <App />
    </ErrorBoundary>
  </StrictMode>,
)

// Register Service Worker so visited pages can be reloaded and opened while offline
if (typeof window !== 'undefined' && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch((err) => {
      console.warn('ApplyTrack Service Worker registration failed:', err);
    });
  });
}


