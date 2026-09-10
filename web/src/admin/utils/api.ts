// ── Shared API helpers for the admin dashboard (/admin/*) ──

import { API_ORIGIN, getToken } from '../../shared/session';

// The backend origin is normalised once in src/shared/session.ts so the portal
// and the admin dashboard always agree on it. Re-exported here because every
// admin component imports it from this module.
export { API_ORIGIN };

/** Builds the request headers: X-Actor-Email (always) + Authorization Bearer (when stored). */
export function authHeaders(email: string): Record<string, string> {
  const headers: Record<string, string> = { 'X-Actor-Email': email };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
}

/** Formats an ISO timestamp as a Vietnamese date, or "—" when absent/invalid. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleDateString('vi-VN');
}
