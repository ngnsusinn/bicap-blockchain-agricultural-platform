import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import LoginForm from './LoginForm';

/** BICAP-86 — functional tests for the login form (all three roles). */
describe('LoginForm', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it('renders the role title and test-account quick-fill for FARM_MANAGER', () => {
    render(<LoginForm role="FARM_MANAGER" onSuccess={vi.fn()} onSwitchToRegister={vi.fn()} />);
    expect(screen.getByText(/Chủ Trang Trại/)).toBeInTheDocument();
    expect(screen.getByText('farm@bicap.com · Farmpassword@2026')).toBeInTheDocument();
  });

  it('renders admin test accounts for the ADMIN role', () => {
    render(<LoginForm role="ADMIN" onSuccess={vi.fn()} onSwitchToRegister={vi.fn()} />);
    expect(screen.getByText('superadmin@bicap.com · Superadmin@2026')).toBeInTheDocument();
    expect(screen.getByText('admin@bicap.com · Adminpassword@2026')).toBeInTheDocument();
    // Admin cannot self-register → no "Đăng ký ngay" link
    expect(screen.queryByText('Đăng ký ngay')).not.toBeInTheDocument();
  });

  it('quick-fill populates identifier and password', () => {
    render(<LoginForm role="RETAILER" onSuccess={vi.fn()} onSwitchToRegister={vi.fn()} />);
    fireEvent.click(screen.getByText('retailer@bicap.com · Retailpassword@2026'));
    expect(screen.getByLabelText(/Email hoặc Số điện thoại/)).toHaveValue('retailer@bicap.com');
    expect(screen.getByLabelText(/^Mật khẩu/)).toHaveValue('Retailpassword@2026');
  });

  it('blocks submit and shows field errors when empty', () => {
    const onSuccess = vi.fn();
    render(<LoginForm role="FARM_MANAGER" onSuccess={onSuccess} onSwitchToRegister={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /Đăng nhập Farm Portal/ }));
    expect(onSuccess).not.toHaveBeenCalled();
    expect(screen.getByText('Vui lòng nhập email hoặc số điện thoại')).toBeInTheDocument();
    expect(screen.getByText('Vui lòng nhập mật khẩu')).toBeInTheDocument();
  });

  it('posts to the farm login endpoint and maps the response on success', async () => {
    const onSuccess = vi.fn();
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ accessToken: 'jwt-1', refreshToken: 'r-1', userId: 9, email: 'farm@bicap.com', fullName: 'Khuong' }),
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<LoginForm role="FARM_MANAGER" onSuccess={onSuccess} onSwitchToRegister={vi.fn()} />);
    fireEvent.change(screen.getByLabelText(/Email hoặc Số điện thoại/), { target: { value: 'farm@bicap.com' } });
    fireEvent.change(screen.getByLabelText(/^Mật khẩu/), { target: { value: 'Farmpassword@2026' } });
    fireEvent.click(screen.getByRole('button', { name: /Đăng nhập Farm Portal/ }));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledOnce());
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/auth/farm/login');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ identifier: 'farm@bicap.com', password: 'Farmpassword@2026' });
    expect(onSuccess.mock.calls[0][0]).toMatchObject({ token: 'jwt-1', user: { id: 9, role: 'FARM_MANAGER' } });
  });

  it('posts to the admin login endpoint for ADMIN role', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ accessToken: 'jwt-admin', userId: 2, email: 'admin@bicap.com', fullName: 'Admin', roles: ['ADMIN'] }),
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<LoginForm role="ADMIN" onSuccess={vi.fn()} onSwitchToRegister={vi.fn()} />);
    fireEvent.click(screen.getByText('admin@bicap.com · Adminpassword@2026'));
    fireEvent.click(screen.getByRole('button', { name: /Đăng nhập Admin Portal/ }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledOnce());
    expect(String(fetchMock.mock.calls[0][0])).toContain('/auth/admin/login');
  });

  it('surfaces backend error message on failed login', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ message: 'Sai tai khoan hoac mat khau' }),
    }));

    render(<LoginForm role="RETAILER" onSuccess={vi.fn()} onSwitchToRegister={vi.fn()} />);
    fireEvent.change(screen.getByLabelText(/Email hoặc Số điện thoại/), { target: { value: 'x@y.z' } });
    fireEvent.change(screen.getByLabelText(/^Mật khẩu/), { target: { value: 'whatever123' } });
    fireEvent.click(screen.getByRole('button', { name: /Đăng nhập Retailer Portal/ }));

    expect(await screen.findByText('Sai tai khoan hoac mat khau')).toBeInTheDocument();
  });
});
