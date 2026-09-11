import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import SeasonExports from './SeasonExports';

/**
 * F-chainMode — SeasonExports phải hiển thị đúng chế độ ghi nhận của backend và
 * không bao giờ tuyên bố đã neo on-chain khi chainMode = MOCK.
 */

function mockFetch(chainMode: 'LIVE' | 'MOCK') {
  return vi.fn((url: string) => {
    const u = String(url);
    if (u.includes('/api/farms/9/exports')) {
      return Promise.resolve({
        ok: true,
        json: async () => [
          {
            id: 1,
            seasonId: 3,
            quantity: 10,
            unit: 'kg',
            exportDate: '2026-05-01',
            warehouse: 'Kho E2E',
            status: 'READY',
            transactionHash: '0xabc',
            traceHash: '0xabc',
            chainMode,
            qrImage: null,
          },
        ],
      });
    }
    if (u.includes('/api/farms/9/seasons')) {
      return Promise.resolve({ ok: true, json: async () => [] });
    }
    return Promise.resolve({ ok: false, status: 404, json: async () => ({}) });
  });
}

describe('SeasonExports chainMode', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('labels a MOCK export as simulated and never claims on-chain anchoring', async () => {
    vi.stubGlobal('fetch', mockFetch('MOCK'));
    render(<SeasonExports farmId={9} />);

    expect(await screen.findByText(/Bản ghi mô phỏng \(chưa broadcast on-chain\)/)).toBeInTheDocument();
    expect(screen.queryByText(/Đã neo lên VeChainThor/)).toBeNull();
  });

  it('labels a LIVE export as anchored on VeChainThor', async () => {
    vi.stubGlobal('fetch', mockFetch('LIVE'));
    render(<SeasonExports farmId={9} />);

    expect(await screen.findByText(/Đã neo lên VeChainThor/)).toBeInTheDocument();
  });
});
