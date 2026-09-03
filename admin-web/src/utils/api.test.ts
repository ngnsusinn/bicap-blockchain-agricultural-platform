import { describe, expect, it, beforeEach } from 'vitest';
import { API_ORIGIN, authHeaders, formatDate } from '../utils/api';

/** BICAP-86 — admin portal shared API helpers. */
describe('API_ORIGIN normalization', () => {
  it('strips any /api/... suffix down to the server origin', () => {
    expect(API_ORIGIN).toBe('http://localhost:8080');
    expect(API_ORIGIN).not.toMatch(/\/api/);
  });
});

describe('authHeaders', () => {
  beforeEach(() => localStorage.clear());

  it('always sends the actor email header', () => {
    expect(authHeaders('admin@bicap.com')['X-Actor-Email']).toBe('admin@bicap.com');
  });

  it('attaches the bearer token when one is stored', () => {
    localStorage.setItem('bicap_token', 'jwt-xyz');
    expect(authHeaders('a@b.c').Authorization).toBe('Bearer jwt-xyz');
  });

  it('omits Authorization when no token is stored', () => {
    expect(authHeaders('a@b.c').Authorization).toBeUndefined();
  });
});

describe('formatDate', () => {
  it('renders null/undefined/invalid as an em dash', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDate(undefined)).toBe('—');
    expect(formatDate('not-a-date')).toBe('—');
  });

  it('renders a valid ISO date in Vietnamese locale', () => {
    expect(formatDate('2026-08-30T10:00:00Z')).toContain('2026');
  });
});
