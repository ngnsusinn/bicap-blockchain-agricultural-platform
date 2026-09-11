import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import TrackingPage from './TrackingPage';

/**
 * F9 — TrackingPage phải nhận lô vận chuyển đã chọn và tự tải chi tiết.
 */

const detail = (id: number) => ({
  id,
  status: 'IN_TRANSIT',
  orderId: 10 + id,
  deliveryAddr: `Kho ${id}`,
  driverName: 'Nguyễn Văn Tài',
  trackingHistory: [
    {
      id: 1,
      shipmentId: id,
      status: 'PICKUP_CONFIRMED',
      gpsLat: 10.1,
      gpsLng: 106.2,
      notes: 'Đã lấy hàng',
      timestamp: '2026-05-01T08:00:00',
    },
  ],
});

function mockFetch() {
  return vi.fn((url: string) => {
    const u = String(url);
    const m = u.match(/\/api\/shipping\/shipments\/(\d+)$/);
    if (m) return Promise.resolve({ ok: true, json: async () => detail(Number(m[1])) });
    if (u.includes('/api/shipping/shipments')) return Promise.resolve({ ok: true, json: async () => [] });
    return Promise.resolve({ ok: false, status: 404, json: async () => ({}) });
  });
}

describe('TrackingPage', () => {
  beforeEach(() => localStorage.setItem('accessToken', 'test-token'));
  afterEach(() => vi.unstubAllGlobals());

  it('auto-loads the shipment id passed in', async () => {
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    render(<TrackingPage initialShipmentId={42} />);

    expect(await screen.findByText('Lô #42')).toBeInTheDocument();
    expect(fetchMock.mock.calls.map((c) => String(c[0])).some((u) => u.endsWith('/api/shipping/shipments/42'))).toBe(true);
  });

  it('loads the new shipment when the prop changes', async () => {
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    const { rerender } = render(<TrackingPage initialShipmentId={42} />);
    await screen.findByText('Lô #42');

    rerender(<TrackingPage initialShipmentId={43} />);

    expect(await screen.findByText('Lô #43')).toBeInTheDocument();
    expect(fetchMock.mock.calls.map((c) => String(c[0])).some((u) => u.endsWith('/api/shipping/shipments/43'))).toBe(true);
  });
});
