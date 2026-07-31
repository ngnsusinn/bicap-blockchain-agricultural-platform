// ── Portal Type ──
export type PortalType = 'admin' | 'farm' | 'retail';

// ── User Session (stored in localStorage after login) ──
export interface UserSession {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'MODERATOR' | 'FARM_MANAGER' | 'RETAILER' | 'SHIPPING_MGR' | 'SHIP_DRIVER' | 'GUEST';
  permissions: string[];
  accessToken: string;
}

// ── Backend AuthResponse (from POST /api/auth/*/login) ──
export interface AuthApiResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  email: string;
  phone: string;
  fullName: string;
  roles: string[];
}

// ── Admin CRUD types ──
export interface PermissionResponse {
  id: number;
  code: string;
  description: string;
}

export interface RoleResponse {
  id: number;
  name: string;
  description: string;
  permissions: PermissionResponse[];
}

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phone: string;
  status: 'ACTIVE' | 'INACTIVE';
  avatarUrl?: string;
  roles: RoleResponse[];
}
