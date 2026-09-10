/**
 * Xác thực JWT & Session cho portal (Farm / Retailer / Shipping / Guest).
 *
 * Từ khi gộp `frontend` và `admin-web` thành một app duy nhất, phần lưu phiên
 * dùng chung được chuyển sang `src/shared/session.ts`. File này giữ nguyên API
 * cũ để các trang portal không phải sửa import.
 */

export type { UserSession, UserRole } from '../../shared/session';
export {
  API_ORIGIN,
  API_BASE_URL,
  getToken,
  getRefreshToken,
  getAuthHeaders,
  isLoggedIn,
  saveSession,
  getCurrentUser,
  clearSession,
  isAdminRole,
  STORAGE_KEYS,
} from '../../shared/session';

import { clearSession } from '../../shared/session';

/** Đăng xuất và xoá toàn bộ bối cảnh xác thực (portal + admin). */
export function logout(): void {
  clearSession();
}
