import { describe, expect, it, beforeEach } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import useHashTab, { readHashTab } from './useHashTab';

/**
 * FARM/Retailer: reload trang phải giữ nguyên màn hình người dùng đang xem.
 * Tab được ghi vào hash URL nên F5 vẫn vào đúng tab (yêu cầu của người dùng).
 */

const TABS = ['dashboard', 'profile', 'settings'] as const;

describe('useHashTab', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/');
  });

  it('dùng tab mặc định khi URL chưa có hash', () => {
    const { result } = renderHook(() => useHashTab(TABS, 'dashboard', 'farm'));
    expect(result.current[0]).toBe('dashboard');
    expect(window.location.hash).toBe('');
  });

  it('khôi phục đúng tab đang xem sau khi reload (đọc từ hash)', () => {
    window.history.replaceState(null, '', '/#farm/profile');
    const { result } = renderHook(() => useHashTab(TABS, 'dashboard', 'farm'));
    expect(result.current[0]).toBe('profile');
  });

  it('ghi hash khi đổi tab để lần reload sau vào đúng trang đó', () => {
    const { result } = renderHook(() => useHashTab(TABS, 'dashboard', 'farm'));

    act(() => result.current[1]('profile'));

    expect(result.current[0]).toBe('profile');
    expect(window.location.hash).toBe('#farm/profile');
    // Giả lập F5: mount lại hook, đọc lại từ URL.
    const reloaded = renderHook(() => useHashTab(TABS, 'dashboard', 'farm'));
    expect(reloaded.result.current[0]).toBe('profile');
  });

  it('bỏ qua hash sai namespace hoặc tab không tồn tại', () => {
    window.history.replaceState(null, '', '/#retailer/marketplace');
    expect(readHashTab(TABS, 'farm')).toBeNull();
    expect(renderHook(() => useHashTab(TABS, 'dashboard', 'farm')).result.current[0]).toBe('dashboard');

    window.history.replaceState(null, '', '/#farm/khong-ton-tai');
    expect(readHashTab(TABS, 'farm')).toBeNull();
  });

  it('cập nhật khi hash đổi từ bên ngoài (dán link / back-forward)', () => {
    const { result } = renderHook(() => useHashTab(TABS, 'dashboard', 'farm'));

    act(() => {
      window.location.hash = '#farm/settings';
      window.dispatchEvent(new Event('hashchange'));
    });

    expect(result.current[0]).toBe('settings');
  });
});
