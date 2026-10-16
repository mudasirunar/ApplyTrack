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

  it('renders correctly for missing or deleted application', () => {
    const setActiveTab = vi.fn();
    const html = renderToString(
      <NotFound 
        setActiveTab={setActiveTab} 
        user={{ email: 'test@example.com' }}
        title="Application Not Found"
        description="This job application may have been permanently removed or the link is invalid."
        isMissingJob={true}
      />
    );
    expect(html).not.toContain('not-found-badge');
    expect(html).toContain('not-found-missing-icon-badge');
    expect(html).toContain('Application Not Found');
    expect(html).toContain('This job application may have been permanently removed or the link is invalid.');
    expect(html).toContain('Go to Applications');
    expect(html).toContain('Back to Dashboard');
  });
});
