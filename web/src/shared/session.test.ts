import { afterEach, describe, expect, it } from 'vitest';
import {
  clearSession,
  getAuthHeaders,
  getToken,
  saveSession,
  STORAGE_KEYS,
  type UserSession,
} from './session';

/**
 * Regression cho log backend `Invalid JWT token: ... Found: 0`:
 * response đăng nhập thiếu `accessToken` khiến code cũ lưu chuỗi "undefined"
 * vào localStorage rồi mọi request gửi `Authorization: Bearer undefined`.
 */
const user: UserSession = {
  id: 7,
  email: 'farm@bicap.com',
  fullName: 'Farm Manager',
  role: 'FARM_MANAGER',
};

describe('session token storage', () => {
  afterEach(() => {
    clearSession();
  });

  it('lưu và đọc lại token hợp lệ', () => {
    saveSession('jwt-that', user, 'refresh-that');

    expect(getToken()).toBe('jwt-that');
    expect(getAuthHeaders().Authorization).toBe('Bearer jwt-that');
  });

  it('không lưu token rỗng/undefined/null — tránh gửi "Bearer undefined"', () => {
    saveSession(undefined as unknown as string, user);
    expect(getToken()).toBeNull();
    expect(getAuthHeaders().Authorization).toBeUndefined();

    saveSession('undefined', user);
    expect(getToken()).toBeNull();

    saveSession('null', user);
    expect(getToken()).toBeNull();

    saveSession('   ', user);
    expect(getToken()).toBeNull();
  });

  it('tự dọn token rác đã nằm trong localStorage từ trước', () => {
    localStorage.setItem(STORAGE_KEYS.token, 'undefined');

    expect(getToken()).toBeNull();
    expect(localStorage.getItem(STORAGE_KEYS.token)).toBeNull();
    expect(getAuthHeaders().Authorization).toBeUndefined();
  });

  it('đăng nhập lỗi (không có token) không xoá token đang dùng tốt', () => {
    saveSession('jwt-good', user);

    // Response lỗi/thiếu accessToken không được phép ghi đè token hiện có.
    saveSession('' as unknown as string, user);

    expect(getToken()).toBe('jwt-good');
  });
});
