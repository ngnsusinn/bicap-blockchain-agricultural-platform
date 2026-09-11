import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CompletedOrdersPage from './CompletedOrdersPage';

/**
 * F8 — Trang tạo vận chuyển phải dùng GET /api/shipping/orders/ready-to-ship.
 */

const readyOrder = {
  id: 101,
  status: 'DEPOSIT_PAID',
  productName: 'Xoài Cát Chu',
  retailerName: 'Siêu thị Xanh',
  farmName: 'Nông trại Xanh',
  quantity: 50,
  price: 45000,
  totalAmount: 2250000,
  deliveryAddr: 'Quận 1, TP.HCM',
};

const completedOrder = {
  id: 202,
  status: 'COMPLETED',
  productName: 'Gạo ST25',
  retailerName: 'Siêu thị Xanh',
  completedAt: '2026-05-10T10:00:00',
};

function mockFetch() {
  return vi.fn((url: string) => {
    const u = String(url);
    if (u.includes('/api/shipping/orders/ready-to-ship')) {
      return Promise.resolve({ ok: true, json: async () => [readyOrder] });
    }
    if (u.includes('/api/shipping/orders/completed')) {
      return Promise.resolve({ ok: true, json: async () => [completedOrder] });
    }
    return Promise.resolve({ ok: false, status: 404, json: async () => ({}) });
  });
}

describe('CompletedOrdersPage', () => {
  beforeEach(() => localStorage.setItem('accessToken', 'test-token'));
  afterEach(() => vi.unstubAllGlobals());

  it('calls ready-to-ship and labels the list for shipment creation', async () => {
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    render(<CompletedOrdersPage />);

    expect(await screen.findByText('Đơn hàng #101')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Đơn đủ điều kiện tạo vận chuyển' })).toBeInTheDocument();
    expect(screen.getByText(/Đơn đã cọc, chưa có lô vận chuyển/)).toBeInTheDocument();

    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls.some((u) => u.includes('/api/shipping/orders/ready-to-ship'))).toBe(true);
    expect(urls.some((u) => u.includes('/api/shipping/orders/completed'))).toBe(false);
  });

  it('keeps the genuinely completed list in a separate tab', async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    render(<CompletedOrdersPage />);
    await screen.findByText('Đơn hàng #101');

    await user.click(screen.getByRole('button', { name: /Đơn đã hoàn tất/ }));

    expect(await screen.findByText('Đơn hàng #202')).toBeInTheDocument();
    await waitFor(() => {
      expect(fetchMock.mock.calls.map((c) => String(c[0])).some((u) => u.includes('/api/shipping/orders/completed'))).toBe(true);
    });
  });
});
