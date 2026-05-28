import { describe, expect, it } from 'vitest';
import { fillPercent, fmtDate, isoDateTimeLocal, n, toBackendDateTime } from './format';

describe('format utils', () => {
  it('formats optional dates and leaves invalid dates untouched', () => {
    expect(fmtDate()).toBe('-');
    expect(fmtDate('2026-05-19T12:34:00')).toBe('19.05.2026 12:34');
    expect(fmtDate('not-a-date')).toBe('not-a-date');
  });

  it('formats numbers with russian separators and suffixes', () => {
    expect(n(null)).toBe('-');
    expect(n('')).toBe('-');
    expect(n(12345.5, ' kg')).toBe('12\u00a0345,5 kg');
  });

  it('converts dates for datetime-local inputs and backend payloads', () => {
    expect(isoDateTimeLocal(new Date(2026, 4, 9, 7, 5))).toBe('2026-05-09T07:05');
    expect(toBackendDateTime('2026-05-09T07:05')).toBe('2026-05-09T07:05:00');
    expect(toBackendDateTime('2026-05-09T07:05:30')).toBe('2026-05-09T07:05:30');
  });

  it('calculates fill percentage with caps and empty values', () => {
    expect(fillPercent()).toBe(0);
    expect(fillPercent(20, 0)).toBe(0);
    expect(fillPercent(33, 100)).toBe(33);
    expect(fillPercent(150, 100)).toBe(100);
  });
});
