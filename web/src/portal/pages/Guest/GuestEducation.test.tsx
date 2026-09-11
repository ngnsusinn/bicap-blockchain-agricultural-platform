import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GuestEducation from './GuestEducation';

/**
 * F5 — Nội dung giáo dục phải lấy từ GET /api/public/education, không còn mock.
 */

const articles = [
  {
    id: 1,
    title: 'Kỹ thuật trồng rau an toàn theo VietGAP',
    summary: 'Hướng dẫn thực tế từ nông trại.',
    content: 'Nội dung chi tiết của bài viết thật.',
    type: 'ARTICLE',
    videoUrl: null,
    coverImageUrl: 'https://cdn.bicap.vn/cover.jpg',
    tags: ['vietgap', 'rau-sach'],
    publishedAt: '2026-05-01T08:00:00',
  },
];

const videos = [
  {
    id: 2,
    title: 'Video hướng dẫn quét QR truy xuất',
    summary: 'Thực hành quét mã.',
    content: null,
    type: 'VIDEO',
    videoUrl: 'https://cdn.bicap.vn/video.mp4',
    coverImageUrl: 'https://cdn.bicap.vn/poster.jpg',
    tags: ['blockchain'],
    publishedAt: '2026-04-20T08:00:00',
  },
];

function mockFetch() {
  return vi.fn((url: string) => {
    const u = String(url);
    if (u.includes('/api/public/education')) {
      const isVideo = u.includes('type=VIDEO');
      return Promise.resolve({ ok: true, json: async () => (isVideo ? videos : articles) });
    }
    return Promise.resolve({ ok: false, status: 404, json: async () => ({}) });
  });
}

describe('GuestEducation', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('renders API articles and tag chips, with no mock content', async () => {
    const fetchMock = mockFetch();
    vi.stubGlobal('fetch', fetchMock);

    render(<GuestEducation />);

    expect(await screen.findByText('Kỹ thuật trồng rau an toàn theo VietGAP')).toBeInTheDocument();
    expect(screen.getByText('#vietgap')).toBeInTheDocument();
    expect(screen.getByText('Hướng dẫn thực tế từ nông trại.')).toBeInTheDocument();

    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls.some((u) => u.includes('/api/public/education') && u.includes('type=ARTICLE'))).toBe(true);

    // Không còn dữ liệu mock / video placeholder w3schools.
    expect(document.body.innerHTML).not.toContain('w3schools');
    expect(document.body.innerHTML).not.toContain('MOCK_ARTICLES');
  });

  it('renders real videos with the API videoUrl', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', mockFetch());

    render(<GuestEducation />);
    await screen.findByText('Kỹ thuật trồng rau an toàn theo VietGAP');

    await user.click(screen.getByRole('button', { name: /Video hướng dẫn/ }));

    expect(await screen.findByText('Video hướng dẫn quét QR truy xuất')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Xem video/ }));

    await waitFor(() => {
      const video = document.querySelector('video');
      expect(video).not.toBeNull();
      expect(video).toHaveAttribute('src', 'https://cdn.bicap.vn/video.mp4');
      expect(video).toHaveAttribute('poster', 'https://cdn.bicap.vn/poster.jpg');
    });
  });
});
