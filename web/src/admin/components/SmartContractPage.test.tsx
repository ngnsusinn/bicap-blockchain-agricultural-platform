import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SmartContractPage } from './SmartContractPage';
import type { UserSession } from '../types';

/**
 * F6 — UI smart contract phải phản ánh đúng chế độ blockchain thật và không được
 * tuyên bố đã broadcast on-chain khi backend đang ở chế độ mô phỏng.
 */

const superAdmin: UserSession = {
  id: 1, email: 'super@bicap.com', fullName: 'Super Admin',
  role: 'SUPER_ADMIN', permissions: ['ADMIN_READ'], accessToken: 't',
};

const MOCK_MESSAGE = 'Chế độ mô phỏng: hash được sinh cục bộ, không có giao dịch on-chain thật.';

function mockFetch(status: { mode: string; live: boolean; message: string }) {
  return vi.fn((url: string) => {
    const u = String(url);
    if (u.includes('/blockchain-status')) {
      return Promise.resolve({ ok: true, json: async () => status });
    }
    if (u.includes('/deploy')) {
      return Promise.resolve({ ok: true, status: 201, json: async () => ({ id: 5 }) });
    }
    return Promise.resolve({ ok: true, json: async () => [] });
  });
}

describe('SmartContractPage', () => {
  beforeEach(() => localStorage.setItem('accessToken', 't'));
  afterEach(() => vi.unstubAllGlobals());

  it('shows the real MOCK badge and backend message', async () => {
    vi.stubGlobal('fetch', mockFetch({ mode: 'mock', live: false, message: MOCK_MESSAGE }));

    render(<SmartContractPage currentSession={superAdmin} onToast={vi.fn()} />);

    expect(await screen.findByText('🟠 Mô phỏng (MOCK)')).toBeInTheDocument();
    expect(screen.getByText(MOCK_MESSAGE)).toBeInTheDocument();
  });

  it('shows the LIVE badge when the backend is live', async () => {
    vi.stubGlobal('fetch', mockFetch({ mode: 'live', live: true, message: 'Đã broadcast thật.' }));

    render(<SmartContractPage currentSession={superAdmin} onToast={vi.fn()} />);

    expect(await screen.findByText('🟢 VeChainThor LIVE')).toBeInTheDocument();
  });

  it('never claims a real deploy while in mock mode', async () => {
    const user = userEvent.setup();
    const onToast = vi.fn();
    vi.stubGlobal('fetch', mockFetch({ mode: 'mock', live: false, message: MOCK_MESSAGE }));

    render(<SmartContractPage currentSession={superAdmin} onToast={onToast} />);
    await screen.findByText('🟠 Mô phỏng (MOCK)');

    await user.click(screen.getByRole('button', { name: /Triển khai Contract mới/ }));
    await user.type(screen.getByPlaceholderText(/ABI JSON/), 'abi-json');
    await user.type(screen.getByPlaceholderText(/0x6080/), '0x60806040');

    await user.click(screen.getByRole('button', { name: /mô phỏng/i }));

    await waitFor(() => {
      expect(onToast).toHaveBeenCalledWith(expect.stringContaining('MÔ PHỎNG'), 'warning');
    });
    expect(onToast).not.toHaveBeenCalledWith(expect.stringContaining('thành công trên VeChainThor'), 'success');
  });
});
