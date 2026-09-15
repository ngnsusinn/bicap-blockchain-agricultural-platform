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

/**
 * Giá trị "token" không dùng được: rỗng, hoặc chuỗi "null"/"undefined" do code cũ
 * ghi `localStorage.setItem(key, someUndefinedValue)`. Nếu lọt vào header sẽ thành
 * `Authorization: Bearer undefined` và backend phải log "Invalid JWT token".
 */
function isUsableToken(value: string | null | undefined): value is string {
  if (!value) return false;
  const trimmed = value.trim();
  return trimmed !== '' && trimmed !== 'null' && trimmed !== 'undefined';
}

export function getToken(): string | null {
  const raw = localStorage.getItem(STORAGE_KEYS.token);
  if (!isUsableToken(raw)) {
    // Tự dọn giá trị rác để không gửi `Bearer undefined` lên backend.
    if (raw !== null) localStorage.removeItem(STORAGE_KEYS.token);
    return null;
  }
  return raw;
}

export function getRefreshToken(): string | null {
  const raw = localStorage.getItem(STORAGE_KEYS.refreshToken);
  return isUsableToken(raw) ? raw : null;
}

/** Persists the JWT + user profile after a successful login/registration. */
export function saveSession(token: string, user: UserSession, refreshToken?: string): void {
  // Chỉ ghi token hợp lệ — trước đây `accessToken` rỗng/undefined của response vẫn
  // được lưu thành chuỗi "undefined" và mọi request sau đó gửi `Bearer undefined`.
  if (isUsableToken(token)) localStorage.setItem(STORAGE_KEYS.token, token.trim());
  localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
  if (isUsableToken(refreshToken)) localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken.trim());
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
