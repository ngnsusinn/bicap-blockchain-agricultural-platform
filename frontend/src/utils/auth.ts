/**
 * Quản lý xác thực JWT & Session người dùng trong localStorage.
 * Hỗ trợ các role: FARM_MANAGER (BICAP-7) và RETAILER (BICAP-36).
 */

export interface UserSession {
  id: number;
  email: string;
  fullName: string;
  role: 'FARM_MANAGER' | 'RETAILER' | 'ADMIN';
  phone?: string;
  address?: string;
  avatarUrl?: string;
  status?: string;
  createdAt?: string;
  /** farmId của nông trại đầu tiên người dùng sở hữu (nếu là Farm Manager). */
  farmId?: number;
}

/** Base URL của backend — dùng VITE_API_BASE_URL nếu được cấu hình. */
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
  .replace(/\/api(?:\/.*)?$/, '') + '/api';

/**
 * Lấy Authorization header từ JWT token.
 */
export function getAuthHeaders(): Record<string, string> {
  const token = localStorage.getItem('accessToken');
  if (token) {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    };
  }
  return {
    'Content-Type': 'application/json',
  };
}

/**
 * Kiểm tra trạng thái đã đăng nhập.
 */
export function isLoggedIn(): boolean {
  return !!localStorage.getItem('accessToken');
}

/**
 * Lưu thông tin phiên làm việc sau khi Đăng nhập/Đăng ký thành công.
 */
export function saveSession(token: string, user: UserSession, refreshToken?: string): void {
  localStorage.setItem('accessToken', token);
  localStorage.setItem('currentUser', JSON.stringify(user));
  if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
}

/**
 * Lấy thông tin người dùng hiện tại từ localStorage.
 */
export function getCurrentUser(): UserSession | null {
  const raw = localStorage.getItem('currentUser');
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserSession;
  } catch (err) {
    return null;
  }
}

/**
 * Đăng xuất và xóa bối cảnh xác thực.
 */
export function logout(): void {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('currentUser');
  // Chế độ 1 port: xóa luôn phiên Admin Web (nếu dùng chung origin) để logout
  // ở cổng nào cũng thoát khỏi toàn bộ hệ thống.
  localStorage.removeItem('bicap_session');
  localStorage.removeItem('bicap_token');
}
