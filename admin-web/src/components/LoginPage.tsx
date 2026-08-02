import React, { useState } from 'react';
import type { UserSession, PortalType, AuthApiResponse } from '../types';
import { API_ORIGIN } from '../utils/api';

// ── Portal configurations ──
const PORTAL_CONFIG = {
  admin: {
    icon: '🛡️',
    title: 'Quản Trị Hệ Thống',
    subtitle: 'Đăng nhập vào Bảng điều khiển Quản trị viên',
    loginUrl: `${API_ORIGIN}/api/auth/admin/login`,
    registerUrl: '',
    canRegister: false,
    btnLabel: 'Đăng nhập Admin',
    registerLabel: '',
    otherPortals: [
      { label: 'Chủ nông trại?', path: '/farm', text: 'Đăng nhập Farm' },
      { label: 'Nhà bán lẻ?', path: '/retail', text: 'Đăng nhập Retail' },
    ],
  },
  farm: {
    icon: '🌾',
    title: 'Cổng Nông Trại',
    subtitle: 'Đăng nhập vào hệ thống Quản lý Nông trại',
    loginUrl: `${API_ORIGIN}/api/auth/farm/login`,
    registerUrl: `${API_ORIGIN}/api/auth/farm/register`,
    canRegister: true,
    btnLabel: 'Đăng nhập Farm Manager',
    registerLabel: 'Đăng ký tài khoản Nông trại',
    otherPortals: [
      { label: 'Quản trị viên?', path: '/admin', text: 'Đăng nhập Admin' },
      { label: 'Nhà bán lẻ?', path: '/retail', text: 'Đăng nhập Retail' },
    ],
  },
  retail: {
    icon: '🛒',
    title: 'Sàn Nông Sản',
    subtitle: 'Đăng nhập vào Sàn giao dịch Nông sản sạch',
    loginUrl: `${API_ORIGIN}/api/auth/retailer/login`,
    registerUrl: `${API_ORIGIN}/api/auth/retailer/register`,
    canRegister: true,
    btnLabel: 'Đăng nhập Retailer',
    registerLabel: 'Đăng ký tài khoản Nhà bán lẻ',
    otherPortals: [
      { label: 'Quản trị viên?', path: '/admin', text: 'Đăng nhập Admin' },
      { label: 'Chủ nông trại?', path: '/farm', text: 'Đăng nhập Farm' },
    ],
  },
} as const;

interface LoginPageProps {
  portalType: PortalType;
  onLoginSuccess: (session: UserSession) => void;
  onNavigate: (path: string) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({
  portalType,
  onLoginSuccess,
  onNavigate,
}) => {
  const config = PORTAL_CONFIG[portalType];

  // ── Mode: login | register ──
  const [mode, setMode] = useState<'login' | 'register'>('login');

  // ── Login fields ──
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // ── Register fields ──
  const [regFullName, setRegFullName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');
  const [showRegPassword, setShowRegPassword] = useState(false);

  // ── Shared state ──
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const switchMode = (newMode: 'login' | 'register') => {
    setMode(newMode);
    setErrorMsg(null);
    setSuccessMsg(null);
  };

  // ── Determine role from API response based on current portal ──
  const buildSession = (data: AuthApiResponse, fallbackIdentifier: string): UserSession => {
    const userRoles: string[] = data.roles || [];
    let role: UserSession['role'] = 'GUEST';
    let permissions: string[] = [];

    if (portalType === 'farm') {
      role = 'FARM_MANAGER';
    } else if (portalType === 'retail') {
      role = 'RETAILER';
    } else if (portalType === 'admin') {
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
    } else {
      if (userRoles.includes('SUPER_ADMIN')) {
        role = 'SUPER_ADMIN';
        permissions = ['ADMIN_CREATE', 'ADMIN_READ', 'ADMIN_UPDATE', 'ADMIN_DELETE'];
      } else if (userRoles.includes('ADMIN')) {
        role = 'ADMIN';
        permissions = ['ADMIN_READ', 'ADMIN_UPDATE'];
      } else if (userRoles.includes('FARM_MANAGER')) {
        role = 'FARM_MANAGER';
      } else if (userRoles.includes('RETAILER')) {
        role = 'RETAILER';
      }
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
  };

  // ── Handle network errors ──
  const handleFetchError = (err: any) => {
    if (err instanceof TypeError && err.message === 'Failed to fetch') {
      setErrorMsg('Không thể kết nối đến máy chủ. Vui lòng kiểm tra backend đang chạy tại http://localhost:8080.');
    } else {
      setErrorMsg(err.message || 'Đã xảy ra lỗi. Vui lòng thử lại.');
    }
  };

  // ── LOGIN handler ──
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!identifier.trim() || !password.trim()) {
      setErrorMsg('Vui lòng nhập Email/SĐT và Mật khẩu.');
      return;
    }

    setLoading(true);
    setErrorMsg(null);

    try {
      const res = await fetch(config.loginUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identifier: identifier.trim(), password }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Sai tài khoản hoặc mật khẩu. Vui lòng thử lại.');
      }

      const data: AuthApiResponse = await res.json();
      onLoginSuccess(buildSession(data, identifier));
    } catch (err: any) {
      handleFetchError(err);
    } finally {
      setLoading(false);
    }
  };

  // ── REGISTER handler ──
  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!regFullName.trim() || !regEmail.trim() || !regPhone.trim() || !regPassword || !regConfirmPassword) {
      setErrorMsg('Vui lòng điền đầy đủ tất cả các trường.');
      return;
    }

    if (regPassword !== regConfirmPassword) {
      setErrorMsg('Mật khẩu xác nhận không khớp.');
      return;
    }

    if (regPassword.length < 8) {
      setErrorMsg('Mật khẩu phải có ít nhất 8 ký tự.');
      return;
    }

    const phoneRegex = /^0[35789]\d{8}$/;
    if (!phoneRegex.test(regPhone.trim())) {
      setErrorMsg('Số điện thoại phải là 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.');
      return;
    }

    setLoading(true);
    setErrorMsg(null);
    setSuccessMsg(null);

    try {
      const res = await fetch(config.registerUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fullName: regFullName.trim(),
          email: regEmail.trim(),
          phone: regPhone.trim(),
          password: regPassword,
          confirmPassword: regConfirmPassword,
        }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.');
      }

      const data: AuthApiResponse = await res.json();

      // Auto-login after successful registration
      onLoginSuccess(buildSession(data, regEmail));
    } catch (err: any) {
      handleFetchError(err);
    } finally {
      setLoading(false);
    }
  };

  // ── Subtitle text ──
  const subtitleText = mode === 'register'
    ? config.registerLabel
    : config.subtitle;

  return (
    <div className={`login-wrapper login-wrapper--${portalType}`}>
      <div className={`login-card login-card--${portalType}`}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div className={`login-icon-badge login-icon-badge--${portalType}`}>
            {config.icon}
          </div>
          <h1 className="login-title">BICAP — {config.title}</h1>
          <p className="login-subtitle">{subtitleText}</p>
        </div>

        {/* Tab toggle (only for farm/retail) */}
        {config.canRegister && (
          <div style={{ display: 'flex', marginBottom: '20px', borderRadius: '10px', background: 'rgba(255,255,255,0.04)', padding: '4px', border: '1px solid rgba(255,255,255,0.06)' }}>
            <button
              type="button"
              onClick={() => switchMode('login')}
              style={{
                flex: 1, padding: '10px', borderRadius: '8px', border: 'none',
                fontSize: '13px', fontWeight: mode === 'login' ? 700 : 500, cursor: 'pointer',
                background: mode === 'login' ? 'rgba(255,255,255,0.1)' : 'transparent',
                color: mode === 'login' ? '#fff' : 'var(--text-muted)',
                transition: 'all 0.2s ease',
              }}
            >
              Đăng nhập
            </button>
            <button
              type="button"
              onClick={() => switchMode('register')}
              style={{
                flex: 1, padding: '10px', borderRadius: '8px', border: 'none',
                fontSize: '13px', fontWeight: mode === 'register' ? 700 : 500, cursor: 'pointer',
                background: mode === 'register' ? 'rgba(255,255,255,0.1)' : 'transparent',
                color: mode === 'register' ? '#fff' : 'var(--text-muted)',
                transition: 'all 0.2s ease',
              }}
            >
              Đăng ký
            </button>
          </div>
        )}

        {/* Success message */}
        {successMsg && (
          <div style={{ padding: '12px 16px', borderRadius: '8px', background: 'rgba(16,185,129,0.12)', border: '1px solid rgba(16,185,129,0.3)', color: '#6ee7b7', fontSize: '13px', marginBottom: '20px', lineHeight: '1.5' }}>
            {successMsg}
          </div>
        )}

        {/* Error message */}
        {errorMsg && <div className="login-error">{errorMsg}</div>}

        {/* ── LOGIN FORM ── */}
        {mode === 'login' && (
          <form onSubmit={handleLogin}>
            <div style={{ marginBottom: '18px' }}>
              <label className="login-label">Email hoặc Số điện thoại</label>
              <input
                type="text"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                placeholder="email@example.com"
                className={`login-input login-input--${portalType}`}
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
                  className={`login-input login-input--${portalType}`}
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

            <button type="submit" disabled={loading} className={`login-btn login-btn--${portalType}`}>
              {loading ? (<><div className="login-spinner" /> Đang xác thực...</>) : config.btnLabel}
            </button>
          </form>
        )}

        {/* ── REGISTER FORM ── */}
        {mode === 'register' && (
          <form onSubmit={handleRegister}>
            <div style={{ marginBottom: '14px' }}>
              <label className="login-label">Họ và Tên</label>
              <input
                type="text"
                value={regFullName}
                onChange={(e) => setRegFullName(e.target.value)}
                placeholder="Nguyễn Văn A"
                className={`login-input login-input--${portalType}`}
                autoComplete="name"
                required
              />
            </div>

            <div style={{ marginBottom: '14px' }}>
              <label className="login-label">Email</label>
              <input
                type="email"
                value={regEmail}
                onChange={(e) => setRegEmail(e.target.value)}
                placeholder="email@example.com"
                className={`login-input login-input--${portalType}`}
                autoComplete="email"
                required
              />
            </div>

            <div style={{ marginBottom: '14px' }}>
              <label className="login-label">Số điện thoại</label>
              <input
                type="tel"
                value={regPhone}
                onChange={(e) => setRegPhone(e.target.value)}
                placeholder="0912345678"
                className={`login-input login-input--${portalType}`}
                autoComplete="tel"
                required
              />
            </div>

            <div style={{ marginBottom: '14px' }}>
              <label className="login-label">Mật khẩu</label>
              <div className="login-input-wrapper">
                <input
                  type={showRegPassword ? 'text' : 'password'}
                  value={regPassword}
                  onChange={(e) => setRegPassword(e.target.value)}
                  placeholder="Ít nhất 8 ký tự, 1 hoa, 1 số, 1 đặc biệt"
                  className={`login-input login-input--${portalType}`}
                  autoComplete="new-password"
                  required
                />
                <button
                  type="button" className="login-toggle-pwd"
                  onClick={() => setShowRegPassword(!showRegPassword)} tabIndex={-1}
                  aria-label={showRegPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showRegPassword ? '🙈' : '👁️'}
                </button>
              </div>
              <p style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>
                Yêu cầu: 8+ ký tự, chữ hoa, chữ thường, số, ký tự đặc biệt
              </p>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label className="login-label">Xác nhận mật khẩu</label>
              <input
                type="password"
                value={regConfirmPassword}
                onChange={(e) => setRegConfirmPassword(e.target.value)}
                placeholder="Nhập lại mật khẩu"
                className={`login-input login-input--${portalType}`}
                autoComplete="new-password"
                required
              />
            </div>

            <button type="submit" disabled={loading} className={`login-btn login-btn--${portalType}`}>
              {loading ? (<><div className="login-spinner" /> Đang đăng ký...</>) : config.registerLabel}
            </button>
          </form>
        )}

        {/* Other portal links */}
        <div className="login-portal-links">
          <p>Truy cập cổng khác:</p>
          <div>
            {config.otherPortals.map((p) => (
              <a
                key={p.path}
                href={p.path}
                onClick={(e) => { e.preventDefault(); onNavigate(p.path); }}
              >
                {p.text}
              </a>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
