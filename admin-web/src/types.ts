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
  seasons?: FarmSeasonSummary[];
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

// ── Product monitoring types (BICAP-5) ──
export type ProductStatusType = 'ACTIVE' | 'INACTIVE' | 'PENDING_REVIEW';

export interface Category {
  id: number;
  name: string;
  description: string | null;
  icon: string | null;
  productCount: number;
  createdAt: string;
}

export interface ProductItem {
  id: number;
  name: string;
  description: string | null;
  price: number;
  quantity: number;
  categoryId: number;
  categoryName: string | null;
  seasonId: number;
  seasonName: string | null;
  farmId: number | null;
  farmName: string | null;
  qrCodeId: number | null;
  status: ProductStatusType;
  createdAt: string;
}

export interface ProductDetail extends ProductItem {
  seasonStartDate: string | null;
  seasonEndDate: string | null;
  seasonProductType: string | null;
  seasonVariety: string | null;
  farmAddress: string | null;
  ownerName: string | null;
  ownerEmail: string | null;
}

export interface CategoryStat {
  categoryId: number;
  categoryName: string | null;
  count: number;
}

export interface ProductStats {
  totalProducts: number;
  activeProducts: number;
  inactiveProducts: number;
  pendingReviewProducts: number;
  newProductsThisWeek: number;
  byCategory: CategoryStat[];
}

// ── Report types (BICAP-27 / SRS-FM-021 — reports sent to Admin by any role) ──
export type ReportStatusType = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';
export type ReportType = 'COMPLAINT' | 'FEEDBACK' | 'INCIDENT' | 'OTHER';

export interface ReportItem {
  id: number;
  reporterId: number;
  reporterName: string | null;
  reporterRole: string;
  type: ReportType;
  subject: string;
  content: string;
  relatedOrderId: number | null;
  status: ReportStatusType;
  adminResponse: string | null;
  handledAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface ReportStats {
  open: number;
  inProgress: number;
  resolved: number;
  rejected: number;
  total: number;
}

// ── Farm season summary shown in admin farm detail (BICAP-4 / SRS-ADM-003) ──
export interface FarmSeasonSummary {
  id: number;
  farmId: number;
  name: string;
  productType: string;
  variety: string;
  area: number | null;
  startDate: string | null;
  endDate: string | null;
  status: string;
  txHash: string | null;
  createdAt: string | null;
}

// ── Admin dashboard (EPIC-1 / detail-design §4.2) ──
export interface AdminDashboard {
  admins: number;
  farms: Record<string, number>;
  products: Record<string, number>;
  orders: Record<string, number>;
  reports: Record<string, number>;
  pendingFarms: FarmRegistration[];
  recentTransactions: BlockchainTransaction[];
}

// ── In-app notification (BICAP-77) ──
export interface AdminNotification {
  id: number;
  type: string;
  title: string;
  content: string;
  channel?: string;
  isRead: boolean;
  createdAt: string;
}

