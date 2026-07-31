/**
 * Helper để lấy Authorization header từ JWT token trong localStorage.
 * Sử dụng cho các API yêu cầu đăng nhập.
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
 * Kiểm tra người dùng đã đăng nhập chưa.
 */
export function isLoggedIn(): boolean {
  return !!localStorage.getItem('accessToken');
}
