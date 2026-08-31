import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { DashboardPage } from './DashboardPage';
import type { UserSession } from '../types';

/** BICAP-86 — admin dashboard aggregates counters, pending approvals and chain activity. */
const session: UserSession = {
  id: 1, email: 'admin@bicap.com', fullName: 'Admin User',
  role: 'ADMIN', permissions: [], accessToken: 't',
};

const dashboardPayload = {
  admins: 3,
  farms: { PENDING: 2, APPROVED: 1, REJECTED: 1, SUSPENDED: 0, INACTIVE: 0, TOTAL: 4 },
  products: { ACTIVE: 1, INACTIVE: 0, PENDING_REVIEW: 0, TOTAL: 1 },
  orders: { PENDING: 1, COMPLETED: 2, TOTAL: 3 },
  reports: { OPEN: 1, IN_PROGRESS: 0, RESOLVED: 0, REJECTED: 0, TOTAL: 1 },
  pendingFarms: [
    { id: 11, name: 'Trang Trai Xanh', status: 'PENDING', ownerName: 'Khuong', createdAt: '2026-08-01T00:00:00', certificationCount: 1 },
  ],
  recentTransactions: [
    { id: 21, entityType: 'SEASON', entityId: 5, txHash: '0xabc', status: 'CONFIRMED' },
  ],
};

describe('DashboardPage', () => {
  beforeEach(() => {
    localStorage.setItem('bicap_token', 't');
    vi.unstubAllGlobals();
  });

  it('renders stat cards, pending approvals and blockchain activity from the API', async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) =>
      Promise.resolve({
        ok: true,
        json: async () => (String(url).includes('/api/admin/dashboard') ? dashboardPayload : {}),
      }));
    vi.stubGlobal('fetch', fetchMock);

    render(<DashboardPage currentSession={session} onToast={vi.fn()} onNavigateTab={vi.fn()} />);

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/admin/dashboard');

    expect(await screen.findByText('Trang Trai Xanh')).toBeInTheDocument(); // pending approval row
    expect(screen.getByText('Tài khoản Admin')).toBeInTheDocument();        // stat card label
    expect(screen.getByText(/SEASON #5/)).toBeInTheDocument();              // chain activity
    expect(screen.getByText('Đã xác nhận')).toBeInTheDocument();             // confirmed badge label
    expect(screen.getAllByText('3').length).toBeGreaterThan(0);             // admins counter
  });

  it('surfaces an error toast when the dashboard call fails', async () => {
    const onToast = vi.fn();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ message: 'Khong tai duoc dashboard' }),
    }));

    render(<DashboardPage currentSession={session} onToast={onToast} onNavigateTab={vi.fn()} />);

    await waitFor(() => expect(onToast).toHaveBeenCalledWith('Khong tai duoc dashboard', 'error'));
  });
});
