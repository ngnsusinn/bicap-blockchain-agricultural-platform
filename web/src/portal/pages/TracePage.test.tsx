import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import TracePage from './TracePage';

/** BICAP-17 — public traceability page (/trace/{hash}). */
const payload = {
  id: 7,
  seasonId: 3,
  quantity: 10.5,
  unit: 'kg',
  exportDate: '2026-09-10',
  warehouse: 'Kho E2E',
  status: 'CONFIRMED',
  transactionHash: '0xabc123',
};

describe('TracePage', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('hiển thị thông tin lô xuất khi hash hợp lệ', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => payload }));
    render(<TracePage hash="abc" />);
    await waitFor(() => expect(screen.getByText(/đã được xác thực/i)).toBeTruthy());
    expect(screen.getByText('#7')).toBeTruthy();
    expect(screen.getByText('10.5 kg')).toBeTruthy();
    expect(screen.getByText('0xabc123')).toBeTruthy();
  });

  it('hiển thị thông báo thân thiện khi hash không tồn tại (404)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 404, json: async () => ({}) }));
    render(<TracePage hash="khong-ton-tai" />);
    await waitFor(() => expect(screen.getByText(/Không tìm thấy lô hàng hợp lệ/i)).toBeTruthy());
  });
});
