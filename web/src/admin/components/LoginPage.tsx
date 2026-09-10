import React, { useState } from 'react';
import type { UserSession, AuthApiResponse } from '../types';
import { API_ORIGIN } from '../utils/api';
import { isAdminRole } from '../../shared/session';

// The admin dashboard lives at the /admin endpoint only, so this page always
// authenticates against the admin login API.
const LOGIN_URL = `${API_ORIGIN}/api/auth/admin/login`;

// Tài khoản test đã seed sẵn (DatabaseSeeder) — bấm để điền nhanh vào form.
const TEST_ACCOUNTS: { id: string; pw: string; note: string }[] = [
  { id: 'superadmin@bicap.com', pw: 'Superadmin@2026', note: 'Super Admin (toàn quyền)' },
  { id: 'admin@bicap.com', pw: 'Adminpassword@2026', note: 'Admin (đọc/ghi)' },
  { id: 'moderator@bicap.com', pw: 'Moderator@2026', note: 'Moderator (chỉ đọc)' },
];

interface LoginPageProps {
  onLoginSuccess: (session: UserSession) => void;
}

/** Maps the backend `roles` array to the admin RBAC session. */
function buildSession(data: AuthApiResponse, fallbackIdentifier: string): UserSession {
  const userRoles: string[] = data.roles || [];
  let role: UserSession['role'] = 'GUEST';
  let permissions: string[] = [];

  if (userRoles.includes('SUPER_ADMIN')) {
    role = 'SUPER_ADMIN';
    permissions = ['ADMIN_CREATE', 'ADMIN_READ', 'ADMIN_UPDATE', 'ADMIN_DELETE'];
  } else if (userRoles.includes('ADMIN')) {
    role = 'ADMIN';
    permissions = ['ADMIN_READ', 'ADMIN_UPDATE'];
  } else if (userRoles.includes('MODERATOR')) {
    role = 'MODERATOR';
    permissions = ['ADMIN_READ'];
  }

  return {
    id: data.userId,
    email: data.email || fallbackIdentifier,
    fullName: data.fullName || 'User',
    phone: data.phone,
    role,
    permissions,
    accessToken: data.accessToken,
  };
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showAccounts, setShowAccounts] = useState(true);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleFetchError = (err: any) => {
    if (err instanceof TypeError && err.message === 'Failed to fetch') {
      setErrorMsg('Không thể kết nối đến máy chủ. Vui lòng kiểm tra backend đang chạy tại http://localhost:8080.');
    } else {
      setErrorMsg(err.message || 'Đã xảy ra lỗi. Vui lòng thử lại.');
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!identifier.trim() || !password.trim()) {
      setErrorMsg('Vui lòng nhập Email/SĐT và Mật khẩu.');
      return;
    }

    setLoading(true);
    setErrorMsg(null);

    try {
      const res = await fetch(LOGIN_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identifier: identifier.trim(), password }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Sai tài khoản hoặc mật khẩu. Vui lòng thử lại.');
      }

      const data: AuthApiResponse = await res.json();
      const session = buildSession(data, identifier);
      if (!isAdminRole(session.role)) {
        throw new Error('Tài khoản này không có quyền truy cập bảng điều khiển quản trị.');
      }
      onLoginSuccess(session);
    } catch (err: any) {
      handleFetchError(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-wrapper login-wrapper--admin">
      <div className="login-card login-card--admin">
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div className="login-icon-badge login-icon-badge--admin">🛡️</div>
          <h1 className="login-title">BICAP — Quản Trị Hệ Thống</h1>
          <p className="login-subtitle">Đăng nhập vào Bảng điều khiển Quản trị viên</p>
        </div>

        {/* Error message */}
        {errorMsg && <div className="login-error">{errorMsg}</div>}

        <form onSubmit={handleLogin}>
          {/* Test accounts quick-fill */}
          <div style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '10px', padding: '10px 12px', marginBottom: '18px' }}>
            <button
              type="button"
              onClick={() => setShowAccounts((s) => !s)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: '#c4b5fd', fontSize: '12px', fontWeight: 700, padding: 0 }}
            >
              <span>🧪 Tài khoản test (bấm để điền nhanh)</span>
              <span>{showAccounts ? '▲' : '▼'}</span>
            </button>
            {showAccounts && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '10px' }}>
                {TEST_ACCOUNTS.map((a) => (
                  <button
                    key={a.id}
                    type="button"
                    onClick={() => { setIdentifier(a.id); setPassword(a.pw); setErrorMsg(null); }}
                    style={{ textAlign: 'left', cursor: 'pointer', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', padding: '8px 10px' }}
                  >
                    <div style={{ fontSize: '12px', color: '#fff', fontFamily: 'monospace' }}>{a.id}, {a.pw}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted, #94a3b8)' }}>{a.note}</div>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div style={{ marginBottom: '18px' }}>
            <label className="login-label">Email hoặc Số điện thoại</label>
            <input
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="email@example.com"
              className="login-input login-input--admin"
              autoComplete="username"
              required
            />
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label className="login-label">Mật khẩu</label>
            <div className="login-input-wrapper">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="login-input login-input--admin"
                autoComplete="current-password"
                required
              />
              <button
                type="button" className="login-toggle-pwd"
                onClick={() => setShowPassword(!showPassword)} tabIndex={-1}
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
          </div>

          <button type="submit" disabled={loading} className="login-btn login-btn--admin">
            {loading ? (<><div className="login-spinner" /> Đang xác thực...</>) : 'Đăng nhập Admin'}
          </button>
        </form>

        {/* Other portal links — the Farm/Retailer portal is the same app at "/" */}
        <div className="login-portal-links">
          <p>Truy cập cổng khác:</p>
          <div>
            <a href="/">Về cổng Nông trại / Nhà bán lẻ</a>
          </div>
        </div>
      </div>
    </div>
  );
};
