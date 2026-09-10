import { describe, expect, it, beforeEach } from 'vitest';
import { API_ORIGIN, authHeaders, formatDate } from '../utils/api';
import { normalizeApiOrigin } from '../../shared/session';

/** BICAP-86 — admin portal shared API helpers. */
describe('API_ORIGIN normalization', () => {
  it('strips /api and any sub-path down to the server origin', () => {
    expect(normalizeApiOrigin('http://localhost:8080/api/admins')).toBe('http://localhost:8080');
    expect(normalizeApiOrigin('http://localhost:8080/api')).toBe('http://localhost:8080');
    expect(normalizeApiOrigin('http://host:9000')).toBe('http://host:9000');
  });

  it('falls back to the default origin when the env value is missing', () => {
    expect(normalizeApiOrigin(undefined)).toBe('http://localhost:8080');
    expect(normalizeApiOrigin('')).toBe('http://localhost:8080');
  });

  it('exposes an API_ORIGIN without any /api suffix (whatever the env value)', () => {
    expect(API_ORIGIN).not.toMatch(/\/api/);
  });
});

describe('authHeaders', () => {
  beforeEach(() => localStorage.clear());

  it('always sends the actor email header', () => {
    expect(authHeaders('admin@bicap.com')['X-Actor-Email']).toBe('admin@bicap.com');
  });

  it('attaches the bearer token when one is stored', () => {
    localStorage.setItem('accessToken', 'jwt-xyz');
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
