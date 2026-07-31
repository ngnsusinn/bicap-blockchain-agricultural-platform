/**
 * Quản lý xác thực JWT & Session người dùng trong localStorage.
 * Hỗ trợ các role: FARM_MANAGER (BICAP-7) và RETAILER (BICAP-36).
 */

export interface UserSession {
  id: number;
  email: string;
  fullName: string;
  role: 'FARM_MANAGER' | 'RETAILER' | 'ADMIN';
}

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
export function saveSession(token: string, user: UserSession): void {
  localStorage.setItem('accessToken', token);
  localStorage.setItem('currentUser', JSON.stringify(user));
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
  localStorage.removeItem('currentUser');
}
