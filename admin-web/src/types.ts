export interface UserSession {
  id?: number;
  email: string;
  fullName: string;
  phone?: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'MODERATOR' | 'FARM_MANAGER' | 'RETAILER' | 'SHIPPING_MGR' | 'SHIP_DRIVER' | 'GUEST';
  permissions: string[];
}

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
