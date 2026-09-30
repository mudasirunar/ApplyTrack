import { describe, it, expect } from 'vitest';
import { getDashboardMessage } from './dashboardMessageProvider';

describe('dashboardMessageProvider', () => {
  it('returns a non-empty string for 0 applications', () => {
    const msg = getDashboardMessage(0);
    expect(msg).toBeDefined();
    expect(typeof msg).toBe('string');
    expect(msg.trim().length).toBeGreaterThan(0);
  });

  it('handles negative count gracefully', () => {
    const msg = getDashboardMessage(-5);
    expect(msg).toBeDefined();
    expect(typeof msg).toBe('string');
    expect(msg.trim().length).toBeGreaterThan(0);
  });

  it('returns valid messages for low count (1..3)', () => {
    [1, 2, 3].forEach((count) => {
      const msg = getDashboardMessage(count);
      expect(msg).toBeDefined();
      expect(msg.length).toBeGreaterThan(0);
    });
  });

  it('returns valid messages for medium count (4..9)', () => {
    [4, 7, 9].forEach((count) => {
      const msg = getDashboardMessage(count);
      expect(msg).toBeDefined();
      expect(msg.length).toBeGreaterThan(0);
    });
  });

  it('returns valid messages for high count (10..19)', () => {
    [10, 15, 19].forEach((count) => {
      const msg = getDashboardMessage(count);
      expect(msg).toBeDefined();
      expect(msg.length).toBeGreaterThan(0);
    });
  });

  it('returns valid messages for very high count (20..49)', () => {
    [20, 35, 49].forEach((count) => {
      const msg = getDashboardMessage(count);
      expect(msg).toBeDefined();
      expect(msg.length).toBeGreaterThan(0);
    });
  });

  it('returns valid messages for ultra high count (50..99)', () => {
    [50, 75, 99].forEach((count) => {
      const msg = getDashboardMessage(count);
      expect(msg).toBeDefined();
      expect(msg.length).toBeGreaterThan(0);
    });
  });

  it('returns valid messages for legendary count (100+)', () => {
    [100, 250, 1000].forEach((count) => {
      const msg = getDashboardMessage(count);
      expect(msg).toBeDefined();
      expect(msg.length).toBeGreaterThan(0);
    });
  });

  it('produces random variations over multiple calls', () => {
    const results = new Set();
    for (let i = 0; i < 40; i++) {
      results.add(getDashboardMessage(15));
    }
    expect(results.size).toBeGreaterThan(1);
  });
});
