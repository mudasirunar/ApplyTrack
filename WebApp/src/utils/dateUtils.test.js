import { describe, it, expect } from 'vitest';
import {
  getLocalDateString,
  getLocalFirstOfMonth,
  parseLocalDateStringToTimestamp,
  parseLocalDate,
  ymdToDmy,
  dmyToYmd,
  getFormattedStatusDate
} from './dateUtils';

describe('dateUtils', () => {
  describe('getLocalDateString', () => {
    it('formats a date object to YYYY-MM-DD with zero-padding', () => {
      const date = new Date(2026, 3, 5); // April 5, 2026
      expect(getLocalDateString(date)).toBe('2026-04-05');
    });

    it('formats a millisecond timestamp correctly', () => {
      const ts = new Date(2025, 11, 25).getTime(); // Dec 25, 2025
      expect(getLocalDateString(ts)).toBe('2025-12-25');
    });

    it('defaults to a valid non-empty string when called without arguments', () => {
      const str = getLocalDateString();
      expect(str).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });
  });

  describe('getLocalFirstOfMonth', () => {
    it('returns the first day of current month in YYYY-MM-01 format', () => {
      const str = getLocalFirstOfMonth();
      expect(str).toMatch(/^\d{4}-\d{2}-01$/);
    });
  });

  describe('parseLocalDateStringToTimestamp', () => {
    it('parses valid YYYY-MM-DD into a valid timestamp', () => {
      const ts = parseLocalDateStringToTimestamp('2026-06-15');
      const parsedDate = new Date(ts);
      expect(parsedDate.getFullYear()).toBe(2026);
      expect(parsedDate.getMonth()).toBe(5); // June is month index 5
      expect(parsedDate.getDate()).toBe(15);
    });

    it('preserves originalTime if the date string matches the original date string', () => {
      const original = 1718450000000;
      const dateStr = getLocalDateString(original);
      const result = parseLocalDateStringToTimestamp(dateStr, original);
      expect(result).toBe(original);
    });

    it('returns current time when input is today', () => {
      const todayStr = getLocalDateString(new Date());
      const before = Date.now();
      const ts = parseLocalDateStringToTimestamp(todayStr);
      const after = Date.now();
      expect(ts).toBeGreaterThanOrEqual(before);
      expect(ts).toBeLessThanOrEqual(after);
    });

    it('returns current time when input is empty or null', () => {
      const before = Date.now();
      const ts = parseLocalDateStringToTimestamp(null);
      const after = Date.now();
      expect(ts).toBeGreaterThanOrEqual(before);
      expect(ts).toBeLessThanOrEqual(after);
    });
  });

  describe('parseLocalDate', () => {
    it('returns Date object for valid YYYY-MM-DD', () => {
      const d = parseLocalDate('2026-10-31');
      expect(d).toBeInstanceOf(Date);
      expect(d.getFullYear()).toBe(2026);
      expect(d.getMonth()).toBe(9); // October
      expect(d.getDate()).toBe(31);
    });

    it('returns null for empty or invalid input', () => {
      expect(parseLocalDate('')).toBeNull();
      expect(parseLocalDate(null)).toBeNull();
      expect(parseLocalDate('invalid')).toBeNull();
    });
  });

  describe('ymdToDmy and dmyToYmd conversion', () => {
    it('converts YYYY-MM-DD to DD/MM/YYYY accurately', () => {
      expect(ymdToDmy('2026-09-30')).toBe('30/09/2026');
      expect(ymdToDmy('2025-01-05')).toBe('05/01/2025');
    });

    it('handles empty or malformed ymd inputs safely', () => {
      expect(ymdToDmy('')).toBe('');
      expect(ymdToDmy(null)).toBe('');
      expect(ymdToDmy('malformed')).toBe('malformed');
    });

    it('converts DD/MM/YYYY back to YYYY-MM-DD', () => {
      expect(dmyToYmd('30/09/2026')).toBe('2026-09-30');
      expect(dmyToYmd('05/01/2025')).toBe('2025-01-05');
    });

    it('rejects incomplete dmy strings', () => {
      expect(dmyToYmd('3/9/26')).toBe('');
      expect(dmyToYmd('')).toBe('');
      expect(dmyToYmd('invalid')).toBe('');
    });
  });

  describe('getFormattedStatusDate', () => {
    it('formats status using latest statusHistory item when available', () => {
      const app = {
        status: 'Interview',
        createdAt: 1700000000000,
        statusHistory: [
          { status: 'Applied', timestamp: 1700000000000 },
          { status: 'Interview', timestamp: new Date(2026, 4, 10).getTime() }
        ]
      };
      const formatted = getFormattedStatusDate(app);
      expect(formatted).toContain('Interview on');
      expect(formatted).toContain('2026');
    });

    it('falls back to createdAt when statusHistory is empty or null', () => {
      const app = {
        status: 'Applied',
        createdAt: new Date(2026, 0, 15).getTime(),
        statusHistory: []
      };
      const formatted = getFormattedStatusDate(app);
      expect(formatted).toContain('Applied on');
      expect(formatted).toContain('2026');
    });
  });
});
