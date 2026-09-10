/**
 * Unified session + API base shared by the portal (`src/portal`) and the admin
 * dashboard (`src/admin`) now that both ship in one app on one origin.
 *
 * Previously the two apps used different localStorage keys
 * (`accessToken`/`currentUser` in the portal, `bicap_token`/`bicap_session` in
 * admin-web) and had to hand a token over through the URL. This module is the
 * single source of truth for both.
 */

/** Every role the backend can issue across all BICAP portals. */
export type UserRole =
  | 'SUPER_ADMIN'
  | 'ADMIN'
  | 'MODERATOR'
  | 'FARM_MANAGER'
  | 'RETAILER'
  | 'SHIPPING_MGR'
  | 'SHIP_DRIVER'
  | 'GUEST';

export interface UserSession {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  phone?: string;
  address?: string;
  avatarUrl?: string;
  status?: string;
  createdAt?: string;
  /** Farm owned by a FARM_MANAGER (first farm), resolved lazily by the portal. */
  farmId?: number;
  /** RBAC codes for admin accounts. */
  permissions?: string[];
  /** Kept for the admin login flow; the authoritative copy lives in storage. */
  accessToken?: string;
}

export const STORAGE_KEYS = {
  token: 'accessToken',
  refreshToken: 'refreshToken',
  user: 'currentUser',
} as const;

/**
 * Backend origin, e.g. `http://localhost:8080`. `VITE_API_BASE_URL` may point at
 * the origin or at an app-specific base such as `http://localhost:8080/api/admins`;
 * any trailing `/api/...` path is stripped so a single value works for both apps.
 */
/**
 * Chuẩn hoá `VITE_API_BASE_URL` về đúng origin của backend: giá trị có thể là
 * origin (`http://localhost:8080`) hoặc base kèm path (`.../api` / `.../api/admins`).
 * Tách thành hàm thuần để test được độc lập với môi trường.
 */
export function normalizeApiOrigin(raw: string | undefined, fallback = 'http://localhost:8080'): string {
  return (raw || fallback).replace(/\/api(?:\/.*)?$/, '');
}

/** Backend origin, e.g. `http://localhost:8080`. */
export const API_ORIGIN = normalizeApiOrigin(import.meta.env.VITE_API_BASE_URL as string | undefined);

/** REST base for the shared `/api/**` endpoints. */
export const API_BASE_URL = `${API_ORIGIN}/api`;

/** True for roles that belong to the `/admin` dashboard. */
export function isAdminRole(role: string | null | undefined): boolean {
  return role === 'SUPER_ADMIN' || role === 'ADMIN' || role === 'MODERATOR';
}

export function getToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.token);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.refreshToken);
}

/** Persists the JWT + user profile after a successful login/registration. */
export function saveSession(token: string, user: UserSession, refreshToken?: string): void {
  localStorage.setItem(STORAGE_KEYS.token, token);
  localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
  if (refreshToken) localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
}

export function getCurrentUser(): UserSession | null {
  const raw = localStorage.getItem(STORAGE_KEYS.user);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserSession;
  } catch {
    return null;
  }
}

export function isLoggedIn(): boolean {
  return !!getToken();
}

/** Clears every session artefact (including legacy admin-web keys). */
export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEYS.token);
  localStorage.removeItem(STORAGE_KEYS.refreshToken);
  localStorage.removeItem(STORAGE_KEYS.user);
  // Legacy keys written by the pre-merge admin-web build.
  localStorage.removeItem('bicap_token');
  localStorage.removeItem('bicap_session');
}

/** JSON request headers carrying the bearer token when one is stored. */
export function getAuthHeaders(): Record<string, string> {
  const token = getToken();
  return token
    ? { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
    : { 'Content-Type': 'application/json' };
}
