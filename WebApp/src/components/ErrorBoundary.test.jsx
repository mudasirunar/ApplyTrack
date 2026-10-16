import { describe, it, expect, vi, beforeEach } from 'vitest';
import ErrorBoundary from './ErrorBoundary';

describe('ErrorBoundary component logic', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('getDerivedStateFromError', () => {
    it('updates state to hasError: true with the given error object', () => {
      const sampleError = new Error('Render failed unexpectedly');
      const newState = ErrorBoundary.getDerivedStateFromError(sampleError);
      expect(newState).toEqual({
        hasError: true,
        error: sampleError
      });
    });
  });

  describe('component lifecycle and handlers', () => {
    it('initializes with default state (no error)', () => {
      const boundary = new ErrorBoundary({});
      expect(boundary.state).toEqual({
        hasError: false,
        error: null,
        errorInfo: null,
        showDetails: false
      });
    });

    it('componentDidCatch updates errorInfo state and executes onError callback', () => {
      const onErrorMock = vi.fn();
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const boundary = new ErrorBoundary({ onError: onErrorMock });
      const testError = new Error('Component crashed');
      const testErrorInfo = { componentStack: '\n    in FaultyComponent\n    in App' };

      boundary.setState = vi.fn((updater) => {
        if (typeof updater === 'function') {
          boundary.state = { ...boundary.state, ...updater(boundary.state) };
        } else {
          boundary.state = { ...boundary.state, ...updater };
        }
      });

      boundary.componentDidCatch(testError, testErrorInfo);

      expect(boundary.setState).toHaveBeenCalledWith({ errorInfo: testErrorInfo });
      expect(onErrorMock).toHaveBeenCalledWith(testError, testErrorInfo);
      expect(consoleErrorSpy).toHaveBeenCalled();
    });

    it('handleReset resets error state and executes onReset callback', () => {
      const onResetMock = vi.fn();
      const boundary = new ErrorBoundary({ onReset: onResetMock });
      boundary.state = {
        hasError: true,
        error: new Error('Crash'),
        errorInfo: { componentStack: 'stack' },
        showDetails: true
      };

      boundary.setState = vi.fn((updater) => {
        boundary.state = { ...boundary.state, ...updater };
      });

      boundary.handleReset();

      expect(boundary.setState).toHaveBeenCalledWith({
        hasError: false,
        error: null,
        errorInfo: null,
        showDetails: false
      });
      expect(onResetMock).toHaveBeenCalledTimes(1);
    });

    it('toggleDetails toggles the showDetails state boolean', () => {
      const boundary = new ErrorBoundary({});
      expect(boundary.state.showDetails).toBe(false);

      boundary.setState = vi.fn((updater) => {
        if (typeof updater === 'function') {
          boundary.state = { ...boundary.state, ...updater(boundary.state) };
        }
      });

      boundary.toggleDetails();
      expect(boundary.state.showDetails).toBe(true);

      boundary.toggleDetails();
      expect(boundary.state.showDetails).toBe(false);
    });

    it('handleClearCacheAndReload removes cached storage items', () => {
      const removeItemMock = vi.fn();
      const clearMock = vi.fn();
      const reloadMock = vi.fn();

      // Mock global window objects for Node testing environment
      const originalWindow = global.window;
      global.window = {
        localStorage: { removeItem: removeItemMock },
        sessionStorage: { clear: clearMock },
        location: { reload: reloadMock }
      };

      try {
        const boundary = new ErrorBoundary({});
        boundary.handleClearCacheAndReload();

        expect(removeItemMock).toHaveBeenCalledWith('applytrack_applications');
        expect(removeItemMock).toHaveBeenCalledWith('applivra_applications');
        expect(removeItemMock).toHaveBeenCalledWith('applytrack_deleted_temp');
        expect(removeItemMock).toHaveBeenCalledWith('applivra_deleted_temp');
        expect(clearMock).toHaveBeenCalledTimes(1);
        expect(reloadMock).toHaveBeenCalledTimes(1);
      } finally {
        global.window = originalWindow;
      }
    });
  });
});
