import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PortalApp from './PortalApp';

/**
 * Yêu cầu người dùng:
 *  - F5 / tải lại trang thì vẫn ở đúng trang đang xem (ví dụ "Cập nhật hồ sơ").
 *  - Menu bên trái phải cuộn được (trước đây bị cắt, không tới được mục cuối).
 *
 * Các trang con được mock để test tập trung vào hành vi điều hướng của shell.
 */
vi.mock('./pages/Guest/GuestNotifications', () => ({ default: () => <div>PAGE_GUEST_NOTIFICATIONS</div> }));
vi.mock('./pages/FarmManager/Settings', () => ({ default: () => <div>PAGE_SETTINGS</div> }));
vi.mock('./pages/FarmManager/ProfilePage', () => ({ default: () => <div>PAGE_PROFILE</div> }));
vi.mock('./pages/FarmManager/Orders', () => ({ default: () => <div>PAGE_ORDERS</div> }));
vi.mock('./pages/FarmManager/ServicePackages', () => ({ default: () => <div>PAGE_PACKAGES</div> }));
vi.mock('./pages/Retailer/MarketplacePage', () => ({ default: () => <div>PAGE_MARKETPLACE</div> }));

/** Fake EventSource: NotificationBell mở SSE khi đã đăng nhập. */
class FakeEventSource {
  static instances: FakeEventSource[] = [];
  readonly url: string;
  onerror: ((event: Event) => void) | null = null;
  closed = false;
  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }
  addEventListener() { /* không cần sự kiện trong test này */ }
  close() { this.closed = true; }
}

function stubFarmFetch(activeSubscription: boolean) {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes('/subscriptions/my')) {
      return Promise.resolve({ ok: true, json: async () => (activeSubscription ? [{ status: 'ACTIVE' }] : []) });
    }
    if (url.includes('/notifications')) {
      return Promise.resolve({ ok: true, json: async () => ({ notifications: [], unreadCount: 0 }) });
    }
    return Promise.resolve({ ok: true, json: async () => ({}) });
  });
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('EventSource', FakeEventSource);
  return fetchMock;
}

function seedFarmManagerSession() {
  localStorage.setItem('currentUser', JSON.stringify({
    id: 7, email: 'farm@bicap.com', fullName: 'Farm Manager', role: 'FARM_MANAGER', farmId: 3,
  }));
}

describe('PortalApp — điều hướng Farm Manager', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.replaceState(null, '', '/');
    // Có token = đã đăng nhập (user đọc từ localStorage, để trống cũng đủ để render shell).
    localStorage.setItem('accessToken', 'test-token');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState(null, '', '/');
  });

  it('mở tab mặc định khi URL chưa có hash', () => {
    render(<PortalApp />);
    expect(screen.getByText('PAGE_GUEST_NOTIFICATIONS')).toBeInTheDocument();
  });

  it('đổi tab thì ghi hash và giữ nguyên tab đó sau khi tải lại trang', async () => {
    const user = userEvent.setup();
    const first = render(<PortalApp />);

    await user.click(screen.getByRole('button', { name: /Cài Đặt/ }));

    expect(screen.getByText('PAGE_SETTINGS')).toBeInTheDocument();
    expect(window.location.hash).toBe('#farm/settings');

    // Giả lập F5: unmount rồi mount lại, URL (hash) vẫn giữ nguyên.
    first.unmount();
    render(<PortalApp />);

    expect(screen.getByText('PAGE_SETTINGS')).toBeInTheDocument();
    expect(screen.queryByText('PAGE_GUEST_NOTIFICATIONS')).not.toBeInTheDocument();
  });

  it('mở thẳng link có hash tới đúng trang (ví dụ #farm/profile)', () => {
    window.history.replaceState(null, '', '/#farm/profile');
    render(<PortalApp />);
    expect(screen.getByText('PAGE_PROFILE')).toBeInTheDocument();
  });

  it('bỏ qua hash không hợp lệ của portal khác', () => {
    window.history.replaceState(null, '', '/#retailer/marketplace');
    render(<PortalApp />);
    expect(screen.getByText('PAGE_GUEST_NOTIFICATIONS')).toBeInTheDocument();
  });

  it('menu bên trái cuộn được (overflow-y: auto) để không bị cắt mục phía dưới', () => {
    render(<PortalApp />);
    const nav = screen.getByRole('navigation', { name: /Điều hướng BICAP Farm/ });
    expect(nav.style.overflowY).toBe('auto');
    expect(nav.style.minHeight).toBe('0px');
    // Mục cuối của menu vẫn tồn tại trong DOM (không bị cắt khỏi nav).
    expect(screen.getByRole('button', { name: /Cài Đặt/ })).toBeInTheDocument();
  });

  it('F5 trên tab VIP vẫn ở nguyên tab đó khi gói dịch vụ còn hiệu lực', async () => {
    seedFarmManagerSession();
    stubFarmFetch(true);
    window.history.replaceState(null, '', '/#farm/orders');

    render(<PortalApp />);

    await waitFor(() => expect(screen.getByText('PAGE_ORDERS')).toBeInTheDocument());
    expect(window.location.hash).toBe('#farm/orders');
  });

  it('đưa về Gói Dịch Vụ khi mở tab VIP mà tài khoản không có gói', async () => {
    seedFarmManagerSession();
    stubFarmFetch(false);
    window.history.replaceState(null, '', '/#farm/orders');

    render(<PortalApp />);

    await waitFor(() => expect(screen.getByText('PAGE_PACKAGES')).toBeInTheDocument());
    expect(window.location.hash).toBe('#farm/packages');
  });

  it('chọn nông trại của gói ACTIVE khi tài khoản sở hữu nhiều nông trại', async () => {
    // farm@bicap.com sở hữu farm 1 (PENDING) và farm 3 (APPROVED + có gói ACTIVE).
    localStorage.setItem('currentUser', JSON.stringify({
      id: 7, email: 'farm@bicap.com', fullName: 'Farm Manager', role: 'FARM_MANAGER',
    }));
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes('/farms/my')) {
        return Promise.resolve({ ok: true, json: async () => ([
          { id: 1, status: 'PENDING' }, { id: 3, status: 'APPROVED' },
        ]) });
      }
      if (url.includes('/subscriptions/my')) {
        return Promise.resolve({ ok: true, json: async () => ([{ status: 'ACTIVE', farmId: 3 }]) });
      }
      if (url.includes('/notifications')) {
        return Promise.resolve({ ok: true, json: async () => ({ notifications: [], unreadCount: 0 }) });
      }
      return Promise.resolve({ ok: true, json: async () => ({}) });
    });
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('EventSource', FakeEventSource);

    render(<PortalApp />);

    await waitFor(() => {
      const stored = JSON.parse(localStorage.getItem('currentUser') || '{}');
      expect(stored.farmId).toBe(3); // KHÔNG phải farms[0] = farm 1 (PENDING)
    });
  });

  it('không có gói ACTIVE thì chọn nông trại đã duyệt thay vì nông trại PENDING', async () => {
    localStorage.setItem('currentUser', JSON.stringify({
      id: 7, email: 'farm@bicap.com', fullName: 'Farm Manager', role: 'FARM_MANAGER',
    }));
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes('/farms/my')) {
        return Promise.resolve({ ok: true, json: async () => ([
          { id: 5, status: 'PENDING' }, { id: 9, status: 'APPROVED' },
        ]) });
      }
      if (url.includes('/subscriptions/my')) {
        return Promise.resolve({ ok: true, json: async () => ([]) });
      }
      return Promise.resolve({ ok: true, json: async () => ({ notifications: [], unreadCount: 0 }) });
    });
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('EventSource', FakeEventSource);

    render(<PortalApp />);

    await waitFor(() => {
      const stored = JSON.parse(localStorage.getItem('currentUser') || '{}');
      expect(stored.farmId).toBe(9);
    });
  });

  it('portal Retailer dùng đúng shell sidebar + main-content như các portal khác', async () => {
    localStorage.setItem('currentUser', JSON.stringify({
      id: 1, email: 'retailer@bicap.com', fullName: 'Nhà bán lẻ', role: 'RETAILER',
    }));
    stubFarmFetch(true);
    // Mở thẳng link tới tab Sàn nông sản → reload vẫn giữ tab này.
    window.history.replaceState(null, '', '/#retailer/marketplace');

    render(<PortalApp />);

    expect(await screen.findByText('PAGE_MARKETPLACE')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: /Điều hướng BICAP Retailer/ })).toBeInTheDocument();
    expect(document.querySelector('main.main-content')).not.toBeNull();
    // Shell cũ (thanh tab ngang) đã được thay bằng sidebar dùng chung.
    expect(document.querySelector('.retailer-portal')).toBeNull();
  });
});
