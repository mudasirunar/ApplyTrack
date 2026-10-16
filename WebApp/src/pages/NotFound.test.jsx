import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { renderToString } from 'react-dom/server';
import NotFound from './NotFound';

describe('NotFound Component', () => {
  it('renders correctly for authenticated user', () => {
    const setActiveTab = vi.fn();
    const html = renderToString(<NotFound setActiveTab={setActiveTab} user={{ email: 'test@example.com' }} />);
    expect(html).toContain('404');
    expect(html).toContain('Page Not Found');
    expect(html).toContain('Return to Dashboard');
    expect(html).toContain('View Applications');
  });

  it('renders correctly for unauthenticated visitor', () => {
    const setActiveTab = vi.fn();
    const html = renderToString(<NotFound setActiveTab={setActiveTab} user={null} />);
    expect(html).toContain('404');
    expect(html).toContain('Page Not Found');
    expect(html).toContain('Back to Home');
    expect(html).toContain('Sign In');
  });
});
