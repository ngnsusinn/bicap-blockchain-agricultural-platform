import { beforeEach, describe, expect, it } from 'vitest';
import {
  getAuthHeaders, isLoggedIn, getCurrentUser, saveSession, logout, API_BASE_URL,
} from '../utils/auth';

/** BICAP-86 — unit tests for the JWT session helpers (farm portal). */
describe('auth session helpers', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('reports logged-out state when no token exists', () => {
    expect(isLoggedIn()).toBe(false);
    expect(getCurrentUser()).toBeNull();
  });

  it('persists and restores a session round-trip', () => {
    saveSession('tok-123', { id: 7, email: 'farm@bicap.com', fullName: 'Khuong', role: 'FARM_MANAGER', farmId: 3 }, 'refresh-1');
    expect(isLoggedIn()).toBe(true);
    const user = getCurrentUser();
    expect(user?.email).toBe('farm@bicap.com');
    expect(user?.farmId).toBe(3);
    expect(localStorage.getItem('refreshToken')).toBe('refresh-1');
  });

  it('returns null for a corrupted stored session instead of throwing', () => {
    localStorage.setItem('currentUser', '{not json');
    expect(getCurrentUser()).toBeNull();
  });

  it('getAuthHeaders attaches the bearer token and JSON content type', () => {
    localStorage.setItem('accessToken', 'abc');
    const headers = getAuthHeaders();
    expect(headers.Authorization).toBe('Bearer abc');
    expect(headers['Content-Type']).toBe('application/json');
  });

  it('getAuthHeaders still returns content type without a token', () => {
    expect(getAuthHeaders().Authorization).toBeUndefined();
    expect(getAuthHeaders()['Content-Type']).toBe('application/json');
  });

  it('logout clears every credential', () => {
    saveSession('tok', { id: 1, email: 'a@b.c', fullName: 'A', role: 'RETAILER' }, 'rt');
    logout();
    expect(isLoggedIn()).toBe(false);
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('bicap_session')).toBeNull();
  });

  it('API_BASE_URL always ends with /api regardless of env shape', () => {
    expect(API_BASE_URL.endsWith('/api')).toBe(true);
    expect(API_BASE_URL).not.toMatch(/\/api\/api/);
  });
});
