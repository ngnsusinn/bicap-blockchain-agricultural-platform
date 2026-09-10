import { describe, expect, it } from 'vitest';
import { badgeStyle } from './ui';

/** BICAP-86 — shared farm-portal style helpers. */
describe('badgeStyle', () => {
  it('maps known statuses to their theme colors', () => {
    expect(badgeStyle('ACTIVE').color).toBe('#6ee7b7');
    expect(badgeStyle('PENDING_REVIEW').color).toBe('#fcd34d');
    expect(badgeStyle('REJECTED').color).toBe('#fca5a5');
    expect(badgeStyle('IN_TRANSIT').color).toBe('#7dd3fc');
  });

  it('falls back to neutral for unknown or missing status', () => {
    expect(badgeStyle('WHAT_EVER').color).toBe('#94a3b8');
    expect(badgeStyle(undefined).color).toBe('#94a3b8');
  });

  it('always returns a pill style', () => {
    const s = badgeStyle('OPEN');
    expect(s.borderRadius).toBe(999);
    expect(s.whiteSpace).toBe('nowrap');
  });
});
