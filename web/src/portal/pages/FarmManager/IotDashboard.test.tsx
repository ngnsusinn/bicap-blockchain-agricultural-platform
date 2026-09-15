import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import IotDashboard from './IotDashboard';
import { STORAGE_KEYS } from '../../utils/auth';

/**
 * CÃ¹ng lá»›p bug vá»›i NotificationBell: `useEffect(..., [user])` khiáº¿n effect cháº¡y
 * láº¡i vÃ´ háº¡n vÃ¬ `getCurrentUser()` tráº£ vá» object má»›i má»—i láº§n render. Dashboard IoT
 * cÅ©ng má»Ÿ SSE + gá»i /api/notifications nÃªn cÅ©ng bá»‹ spam request.
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

  emit(type: string, payload: unknown) {
    const event = { data: JSON.stringify(payload) } as MessageEvent;
    (this.listeners[type] || []).forEach((listener) => listener(event));
  }
}

const urgentAlert = {
  id: 1,
  userId: 4,
  type: 'URGENT',
  title: 'Nhiá»‡t Ä‘á»™ vÆ°á»£t ngÆ°á»¡ng',
  content: 'Cáº£m biáº¿n A1 bÃ¡o 45Â°C',
  channel: 'IN_APP',
  isRead: false,
  createdAt: '2026-09-14T11:54:00',
};

describe('IotDashboard', () => {
  beforeEach(() => {
    localStorage.clear();
    FakeEventSource.reset();
    localStorage.setItem(STORAGE_KEYS.token, 'header.payload.signature');
    localStorage.setItem(STORAGE_KEYS.user, JSON.stringify({
      id: 4,
      email: 'farm@bicap.com',
      fullName: 'Farm Manager',
      role: 'FARM_MANAGER',
      farmId: 9,
    }));
  });

  afterEach(() => vi.unstubAllGlobals());

  it('chá»‰ gá»i /api/notifications má»™t láº§n vÃ  má»Ÿ má»™t SSE connection duy nháº¥t', async () => {
    const fetchMock = vi.fn((_input: RequestInfo | URL) => Promise.resolve({
      ok: true,
      json: async () => ({ unreadCount: 1, notifications: [urgentAlert] }),
    }));
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('EventSource', FakeEventSource);

    const { rerender } = render(<IotDashboard />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(await screen.findByText('Nhiá»‡t Ä‘á»™ vÆ°á»£t ngÆ°á»¡ng')).toBeInTheDocument();

    rerender(<IotDashboard />);
    rerender(<IotDashboard />);
    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(FakeEventSource.instances).toHaveLength(1);
    expect(FakeEventSource.instances[0].url).toContain('/api/notifications/stream?token=');
  });

  it('chá»‰ hiá»ƒn thá»‹ cáº£nh bÃ¡o URGENT/PERIODIC nháº­n qua SSE', async () => {
    const fetchMock = vi.fn((_input: RequestInfo | URL) => Promise.resolve({
      ok: true,
      json: async () => ({ unreadCount: 0, notifications: [] }),
    }));
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('EventSource', FakeEventSource);

    render(<IotDashboard />);
    await waitFor(() => expect(FakeEventSource.instances).toHaveLength(1));

    act(() => {
      FakeEventSource.instances[0].emit('notification', urgentAlert);
      FakeEventSource.instances[0].emit('notification', { ...urgentAlert, id: 2, type: 'SHIPPING', title: 'KhÃ´ng pháº£i cáº£nh bÃ¡o IoT' });
    });

    expect(await screen.findByText('Nhiá»‡t Ä‘á»™ vÆ°á»£t ngÆ°á»¡ng')).toBeInTheDocument();
    expect(screen.queryByText('KhÃ´ng pháº£i cáº£nh bÃ¡o IoT')).not.toBeInTheDocument();
  });
});
