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
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  avatarUrl?: string;
  roles: RoleResponse[];
}

// ── Farm types (shared by Farm Approval BICAP-3 & Farm Management BICAP-4) ──
export type FarmStatusType = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED' | 'INACTIVE';

export interface FarmCertification {
  id: number;
  type: string;
  fileUrl: string;
  expiryDate: string;
}

export interface FarmRegistration {
  id: number;
  name: string;
  address: string;
  area: number;
  gpsLat: number | null;
  gpsLng: number | null;
  description?: string | null;
  productTypes?: string | null;
  adminNotes?: string | null;
  status: FarmStatusType;
  createdAt: string;
  updatedAt?: string | null;
  ownerName: string | null;
  ownerEmail: string | null;
  ownerPhone: string | null;
  certificationCount: number;
  certifications?: FarmCertification[];
}

// ── Smart Contract and Blockchain types ──
export interface SmartContract {
  id: number;
  name: string;
  address: string | null;
  bytecode: string;
  abi: string;
  environment: string; // TESTNET, MAINNET
  status: string; // PENDING, DEPLOYED, ACTIVE, INACTIVE, FAILED
  version: string;
  txHash: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface BlockchainTransaction {
  id: number;
  entityType: string; // SEASON, PROCESS, QR, EXPORT, CONTRACT
  entityId: number;
  txHash: string;
  contractAddress: string | null;
  status: string; // PENDING, CONFIRMED, FAILED
  retryCount: number;
  idempotencyKey: string;
  createdAt: string;
}

