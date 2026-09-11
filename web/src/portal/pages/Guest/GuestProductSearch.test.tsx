import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GuestProductSearch from './GuestProductSearch';

/**
 * F3 — Guest product search must use the public catalogue endpoint and never
 * fabricate a trace hash, certification, unit or stock image.
 */

const REAL_HASH = `0x${'a'.repeat(64)}`;

const page = {
  content: [
    {
      id: 5,
      name: 'Xoài Cát Chu',
      description: 'Xoài chín cây',
      images: [],
      price: 45000,
      quantity: 100,
      availability: 'AVAILABLE',
      categoryId: 2,
      categoryName: 'Trái cây',
      farmId: 9,
      farmName: 'Nông trại Xanh',
      farmAddress: 'Cao Lãnh, Đồng Tháp',
      certifications: ['VietGAP'],
      seasonId: 3,
      seasonName: 'Vụ Hè',
      productType: 'Xoài',
      variety: 'Cát Chu',
      seasonStartDate: '2026-01-01',
      harvestDate: '2026-05-01',
      exportId: null,
      traceHash: null, // chưa neo blockchain → KHÔNG được tự sinh hash
      qrImage: null,
      transactionHash: null,
      processes: [],
      createdAt: '2026-05-02T00:00:00',
    },
    {
      id: 6,
      name: 'Gạo ST25',
      images: ['https://cdn.bicap.vn/gao.jpg'],
      price: 20000,
      quantity: 0,
      availability: 'SOLD_OUT',
      categoryId: 3,
      categoryName: 'Lúa gạo',
      farmId: 10,
      farmName: 'HTX Gạo Sóc Trăng',
      farmAddress: 'Sóc Trăng',
      certifications: [],
      seasonId: null,
      seasonName: null,
      productType: null,
      variety: null,
      seasonStartDate: null,
      harvestDate: null,
      exportId: 12,
      traceHash: REAL_HASH,
      qrImage: null,
      transactionHash: REAL_HASH,
      processes: [],
      createdAt: '2026-05-01T00:00:00',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 12,
};

function mockFetch() {
  return vi.fn((url: string) => {
    const u = String(url);
    if (u.includes('/api/categories')) {
      return Promise.resolve({ ok: true, json: async () => [{ id: 2, name: 'Trái cây' }] });
    }
    if (u.includes('/api/public/products')) {
      return Promise.resolve({ ok: true, json: async () => page });
    }
    return Promise.resolve({ ok: false, status: 404, json: async () => ({}) });
  });
}

describe('GuestProductSearch', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('renders real public API data and never fabricates a trace hash', async () => {
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    render(<GuestProductSearch />);

    expect(await screen.findByText('Xoài Cát Chu')).toBeInTheDocument();
    expect(screen.getByText('Gạo ST25')).toBeInTheDocument();

    // Real fields from the payload.
    expect(screen.getByText('Nông trại Xanh')).toBeInTheDocument();
    expect(screen.getByText('Cao Lãnh, Đồng Tháp')).toBeInTheDocument();
    expect(screen.getByText('🏅 VietGAP')).toBeInTheDocument();

    // Calls the public endpoint, never the admin monitoring endpoint.
    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls.some((u) => u.includes('/api/public/products'))).toBe(true);
    expect(urls.some((u) => u.includes('/api/admin/products'))).toBe(false);

    // The product without a real hash is never given a fabricated one.
    expect(document.body.innerHTML).not.toContain('a91b2c4e');
    expect(screen.getByRole('button', { name: /Chưa có QR truy xuất/ })).toBeDisabled();

    // The real hash navigates to /trace/{hash}.
    const traceLink = screen.getByRole('link', { name: /Tra cứu QR/ });
    expect(traceLink).toHaveAttribute('href', `/trace/${REAL_HASH}`);

    // No stock-photo placeholder was invented.
    expect(document.querySelector('img[src*="unsplash"]')).toBeNull();
    // Neutral CSS placeholder for the image-less product.
    expect(screen.getByLabelText('Chưa có ảnh sản phẩm')).toBeInTheDocument();
  });

  it('sends filters to the backend', async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    render(<GuestProductSearch />);
    await screen.findByText('Xoài Cát Chu');

    await user.selectOptions(screen.getByLabelText('Tình trạng'), 'SOLD_OUT');

    await waitFor(() => {
      const urls = fetchMock.mock.calls.map((c) => String(c[0]));
      expect(urls.some((u) => u.includes('availability=SOLD_OUT'))).toBe(true);
    });
  });
});
