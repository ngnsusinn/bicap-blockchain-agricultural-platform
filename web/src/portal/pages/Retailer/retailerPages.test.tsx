import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import MarketplacePage from './MarketplacePage';
import QrScannerPage from './QrScannerPage';
import RetailerOrdersPage from './RetailerOrdersPage';
import RetailerShipmentsPage from './RetailerShipmentsPage';
import RetailerNotificationsPage from './RetailerNotificationsPage';
import RetailerReportsPage from './RetailerReportsPage';
import RetailerProfilePage from './RetailerProfilePage';
import RetailerBusinessPage from './RetailerBusinessPage';
import type { UserSession } from '../../utils/auth';

/**
 * Smoke test cho portal Retailer sau khi đồng bộ UI với các portal khác:
 * mọi trang phải render được tiêu đề trang theo design system dùng chung
 * (`dashboard-title`/`dashboard-subtitle`) và không crash khi API trả dữ liệu rỗng.
 */

class FakeEventSource {
  onerror: ((event: Event) => void) | null = null;
  readonly url: string;
  constructor(url: string) {
    this.url = url;
  }
  addEventListener() { /* noop */ }
  close() { /* noop */ }
}

/** Stub API tối thiểu: đúng hình dạng dữ liệu mà từng trang mong đợi. */
function stubFetch() {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes('/marketplace/products?')) {
      return Promise.resolve({ ok: true, json: async () => ({ content: [], totalElements: 0 }) });
    }
    if (url.includes('/retailer/business-profile')) {
      return Promise.resolve({ ok: false, status: 404, json: async () => ({}) });
    }
    if (url.includes('/retailer/profile')) {
      return Promise.resolve({ ok: true, json: async () => ({ fullName: 'Nhà bán lẻ A', phone: '0901234567', address: '', avatarUrl: '' }) });
    }
    if (url.includes('/notifications')) {
      return Promise.resolve({ ok: true, json: async () => ({ notifications: [], unreadCount: 0 }) });
    }
    // Các endpoint dạng danh sách: /categories, /orders/my, /retailer/shipments, /reports/my
    return Promise.resolve({ ok: true, json: async () => [] });
  });
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('EventSource', FakeEventSource);
  return fetchMock;
}

const session: UserSession = {
  id: 12,
  email: 'retailer@bicap.com',
  fullName: 'Nhà bán lẻ A',
  role: 'RETAILER',
};

describe('Portal Retailer — smoke test UI', () => {
  afterEach(() => vi.unstubAllGlobals());

  const cases: { name: string; element: React.ReactNode; title: string; sharedTitleClass: boolean }[] = [
    { name: 'Sàn nông sản', element: <MarketplacePage />, title: 'Sàn nông sản', sharedTitleClass: true },
    { name: 'Quét QR', element: <QrScannerPage />, title: 'Quét QR truy xuất nguồn gốc', sharedTitleClass: true },
    { name: 'Đơn mua', element: <RetailerOrdersPage />, title: 'Lịch sử đơn mua', sharedTitleClass: true },
    { name: 'Vận chuyển', element: <RetailerShipmentsPage />, title: 'Quy trình vận chuyển', sharedTitleClass: true },
    { name: 'Thông báo', element: <RetailerNotificationsPage />, title: 'Thông Báo', sharedTitleClass: true },
    { name: 'Báo cáo', element: <RetailerReportsPage />, title: 'Gửi báo cáo cho Admin', sharedTitleClass: true },
    {
      name: 'Thông tin cá nhân',
      element: <RetailerProfilePage user={session} onUserUpdated={() => {}} />,
      title: 'Thông tin cá nhân',
      sharedTitleClass: false,
    },
    { name: 'Giấy phép kinh doanh', element: <RetailerBusinessPage />, title: 'Hồ sơ doanh nghiệp', sharedTitleClass: false },
  ];

  it.each(cases)('$name: render được tiêu đề trang theo design system', async ({ element, title, sharedTitleClass }) => {
    stubFetch();

    render(element);

    const heading = await screen.findByRole('heading', { level: 1, name: title });
    expect(heading).toBeInTheDocument();
    if (sharedTitleClass) {
      expect(heading).toHaveClass('dashboard-title');
      expect(heading.closest('section')).not.toBeNull();
      // Mô tả trang dùng chung class dashboard-subtitle.
      expect(heading.parentElement?.querySelector('.dashboard-subtitle')).not.toBeNull();
    }
  });
});
