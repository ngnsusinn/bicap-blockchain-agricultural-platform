import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GuestArea from './GuestArea';

/**
 * F4 — Khu vực khách phải có đủ ba tab và tất cả hoạt động không cần token.
 */

const article = {
  id: 1,
  title: 'Bài viết thật từ API',
  summary: 'Tóm tắt',
  content: 'Nội dung',
  type: 'ARTICLE',
  videoUrl: null,
  coverImageUrl: null,
  tags: ['vietgap'],
  publishedAt: '2026-05-01T00:00:00',
};

function mockFetch() {
  return vi.fn((url: string) => {
    const u = String(url);
    if (u.includes('/api/notifications')) {
      return Promise.resolve({ ok: true, json: async () => ({ unreadCount: 0, notifications: [] }) });
    }
    if (u.includes('/api/public/education')) {
      return Promise.resolve({ ok: true, json: async () => [article] });
    }
    if (u.includes('/api/public/products')) {
      return Promise.resolve({
        ok: true,
        json: async () => ({ content: [], totalElements: 0, totalPages: 1, number: 0, size: 12 }),
      });
    }
    if (u.includes('/api/categories')) {
      return Promise.resolve({ ok: true, json: async () => [] });
    }
    return Promise.resolve({ ok: false, status: 404, json: async () => ({}) });
  });
}

describe('GuestArea', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('shows all three guest tabs and the login button', () => {
    vi.stubGlobal('fetch', mockFetch());
    render(<GuestArea onLogin={vi.fn()} />);

    const nav = screen.getByRole('navigation', { name: 'Điều hướng khu vực khách' });
    expect(within(nav).getByRole('button', { name: /Thông báo chung/ })).toBeInTheDocument();
    expect(within(nav).getByRole('button', { name: /Sản phẩm/ })).toBeInTheDocument();
    expect(within(nav).getByRole('button', { name: /Kiến thức/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Đăng nhập hệ thống/ })).toBeInTheDocument();
  });

  it('switches between products and education without a token', async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    render(<GuestArea onLogin={vi.fn()} />);

    const nav = screen.getByRole('navigation', { name: 'Điều hướng khu vực khách' });
    await user.click(within(nav).getByRole('button', { name: /Sản phẩm/ }));
    await waitFor(() => {
      expect(fetchMock.mock.calls.map((c) => String(c[0])).some((u) => u.includes('/api/public/products'))).toBe(true);
    });

    await user.click(within(nav).getByRole('button', { name: /Kiến thức/ }));
    expect(await screen.findByText('Bài viết thật từ API')).toBeInTheDocument();

    // Thông báo nền tảng cho khách: không được gọi API đánh dấu đã đọc (401).
    const mutations = fetchMock.mock.calls.filter((c) => {
      const init = (c as unknown as [string, RequestInit?])[1];
      return init?.method === 'PUT';
    });
    expect(mutations).toHaveLength(0);
  });
});
