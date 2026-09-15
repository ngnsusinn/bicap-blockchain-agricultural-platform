import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import NotificationBell from './NotificationBell';
import { STORAGE_KEYS } from '../../shared/session';

/**
 * Regression test cho bug "spam request" (Network tab: hÃ ng nghÃ¬n cáº·p
 * `GET /api/notifications` + `GET /api/notifications/stream?token=...`).
 *
 * NguyÃªn nhÃ¢n: `useEffect(..., [user])` vá»›i `user = getCurrentUser()` â€” má»—i láº§n
 * render tráº£ vá» má»™t object má»›i nÃªn effect luÃ´n bá»‹ coi lÃ  "dependency thay Ä‘á»•i",
 * cháº¡y láº¡i â†’ setState â†’ render â†’ cháº¡y láº¡i â†’ vÃ²ng láº·p vÃ´ háº¡n.
 *
 * Test nÃ y khoÃ¡ hÃ nh vi Ä‘Ãºng: dÃ¹ component re-render nhiá»u láº§n thÃ¬ chá»‰ Ä‘Æ°á»£c gá»i
 * API má»™t láº§n vÃ  chá»‰ má»Ÿ Ä‘Ãºng má»™t káº¿t ná»‘i SSE.
 */

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  static reset() { FakeEventSource.instances = []; }

  readonly url: string;
  closed = false;
  onerror: ((event: Event) => void) | null = null;
  private listeners: Record<string, ((event: MessageEvent) => void)[]> = {};

  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, listener: (event: MessageEvent) => void) {
    (this.listeners[type] ||= []).push(listener);
  }

  close() { this.closed = true; }

  /** MÃ´ phá»ng server Ä‘áº©y má»™t sá»± kiá»‡n SSE tÃªn `notification`. */
  emit(type: string, payload: unknown) {
    const event = { data: JSON.stringify(payload) } as MessageEvent;
    (this.listeners[type] || []).forEach((listener) => listener(event));
  }
}

function seedSession() {
  localStorage.setItem(STORAGE_KEYS.token, 'header.payload.signature');
  localStorage.setItem(STORAGE_KEYS.user, JSON.stringify({
    id: 7,
    email: 'farm@bicap.com',
    fullName: 'Farm Manager',
    role: 'FARM_MANAGER',
  }));
}

const sampleNotification = {
  id: 1,
  userId: 7,
  type: 'URGENT',
  title: 'Nhiá»‡t Ä‘á»™ vÆ°á»£t ngÆ°á»¡ng',
  content: 'Cáº£m biáº¿n A1 bÃ¡o 45Â°C',
  channel: 'IN_APP',
  isRead: false,
  createdAt: '2026-09-14T11:54:00',
};

describe('NotificationBell', () => {
  beforeEach(() => {
    localStorage.clear();
    FakeEventSource.reset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('chá»‰ gá»i /api/notifications vÃ  má»Ÿ má»™t SSE connection duy nháº¥t dÃ¹ re-render nhiá»u láº§n', async () => {
    seedSession();
    const fetchMock = vi.fn((_input: RequestInfo | URL) => Promise.resolve({
      ok: true,
      json: async () => ({ unreadCount: 1, notifications: [sampleNotification] }),
    }));
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('EventSource', FakeEventSource);

    const { rerender } = render(<NotificationBell />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());

    // Ã‰p component re-render liÃªn tá»¥c: vá»›i bug [user] má»—i láº§n render láº¡i báº¯n thÃªm request.
    rerender(<NotificationBell />);
    rerender(<NotificationBell />);
    rerender(<NotificationBell />);
    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/notifications');
    expect(FakeEventSource.instances).toHaveLength(1);
    expect(FakeEventSource.instances[0].url).toContain('/api/notifications/stream?token=');
    expect(FakeEventSource.instances[0].closed).toBe(false);
  });

  it('cáº­p nháº­t badge khi nháº­n thÃ´ng bÃ¡o realtime qua SSE', async () => {
    seedSession();
    const fetchMock = vi.fn((_input: RequestInfo | URL) => Promise.resolve({
      ok: true,
      json: async () => ({ unreadCount: 1, notifications: [sampleNotification] }),
    }));
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('EventSource', FakeEventSource);

    render(<NotificationBell />);
    await waitFor(() => expect(String(screen.getByRole('button').textContent)).toContain('1'));

    act(() => {
      FakeEventSource.instances[0].emit('notification', { ...sampleNotification, id: 2 });
    });

    await waitFor(() => expect(String(screen.getByRole('button').textContent)).toContain('2'));
    // Sá»± kiá»‡n SSE khÃ´ng Ä‘Æ°á»£c kÃ©o theo vÃ²ng láº·p gá»i láº¡i API.
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('khÃ´ng gá»i API vÃ  khÃ´ng má»Ÿ SSE khi chÆ°a Ä‘Äƒng nháº­p', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('EventSource', FakeEventSource);

    const { container } = render(<NotificationBell />);

    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(fetchMock).not.toHaveBeenCalled();
    expect(FakeEventSource.instances).toHaveLength(0);
    expect(container).toBeEmptyDOMElement();
  });
});
