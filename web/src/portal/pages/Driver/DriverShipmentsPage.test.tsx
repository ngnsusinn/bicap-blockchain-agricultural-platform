import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DriverShipmentsPage, { extractTraceHash } from './DriverShipmentsPage';

/**
 * F11 — tài xế phải quét/nhập trace hash trước khi xác nhận lấy hàng, và lỗi từ
 * backend phải được hiển thị nguyên văn.
 */

const VALID_HASH = `0x${'b'.repeat(64)}`;
const WRONG_HASH_MESSAGE = 'Mã QR không khớp với lô hàng được phân công';

const shipment = { id: 7, status: 'PICKING_UP', orderId: 1, deliveryAddr: 'Kho A' };
const detail = { ...shipment, trackingHistory: [] };

function mockFetch() {
  return vi.fn((url: string, init?: RequestInit) => {
    const u = String(url);
    if (u.includes('/pickup')) {
      return Promise.resolve({ ok: false, status: 400, json: async () => ({ message: WRONG_HASH_MESSAGE }) });
    }
    if (/\/api\/driver\/shipments\/\d+$/.test(u)) {
      return Promise.resolve({ ok: true, json: async () => detail });
    }
    if (u.includes('/api/driver/shipments')) {
      return Promise.resolve({ ok: true, json: async () => [shipment] });
    }
    if (init?.method) {
      return Promise.resolve({ ok: true, json: async () => ({}) });
    }
    return Promise.resolve({ ok: false, status: 404, json: async () => ({}) });
  });
}

async function openPickup(user: ReturnType<typeof userEvent.setup>) {
  render(<DriverShipmentsPage />);
  await screen.findByText('Lô #7');
  await user.click(screen.getByRole('button', { name: /Chi tiết & thao tác/ }));
  await screen.findByText('📲 Quét QR xác nhận lấy hàng');
}

describe('DriverShipmentsPage', () => {
  beforeEach(() => localStorage.setItem('accessToken', 'driver-token'));
  afterEach(() => vi.unstubAllGlobals());

  it('extracts the trace hash from a /trace/{hash} URL', () => {
    expect(extractTraceHash(`https://bicap.vn/trace/${VALID_HASH}`)).toBe(VALID_HASH);
    expect(extractTraceHash(`  ${VALID_HASH}  `)).toBe(VALID_HASH);
    expect(extractTraceHash('')).toBe('');
  });

  it('blocks pickup without a scanned trace hash and never calls the pickup API', async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    await openPickup(user);

    await user.click(screen.getByRole('button', { name: /Xác nhận lấy hàng/ }));

    expect(await screen.findByText(/Vui lòng quét hoặc nhập mã trace hash/)).toBeInTheDocument();
    expect(fetchMock.mock.calls.map((c) => String(c[0])).some((u) => u.includes('/pickup'))).toBe(false);
  });

  it('surfaces the backend error when the scanned hash does not match', async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    await openPickup(user);

    await user.type(screen.getByLabelText('Trace hash'), VALID_HASH);
    await user.click(screen.getByRole('button', { name: /Xác nhận lấy hàng/ }));

    expect(await screen.findByText(WRONG_HASH_MESSAGE)).toBeInTheDocument();
    await waitFor(() => {
      expect(fetchMock.mock.calls.map((c) => String(c[0])).some((u) => u.includes('/pickup'))).toBe(true);
    });
  });
});
