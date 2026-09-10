// ── Shared API helpers for the admin portal ──

// Normalize the API base to the server ORIGIN.
// VITE_API_BASE_URL may be set to the origin (http://localhost:8080) or to an
// app-specific base such as http://localhost:8080/api/admins (App.tsx convention).
// Endpoints live under /api/admin/farms etc., so strip any trailing /api/... path.
export const API_ORIGIN = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/admins')
  .replace(/\/api(?:\/.*)?$/, '');

/** Builds the request headers: X-Actor-Email (always) + Authorization Bearer (when stored). */
export function authHeaders(email: string): Record<string, string> {
  const headers: Record<string, string> = { 'X-Actor-Email': email };
  const token = localStorage.getItem('bicap_token');
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
}

/** Formats an ISO timestamp as a Vietnamese date, or "—" when absent/invalid. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleDateString('vi-VN');
}
