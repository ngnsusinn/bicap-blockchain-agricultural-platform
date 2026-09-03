import React, { useState } from 'react';
import { API_BASE_URL } from '../../utils/auth';

export type AuthRole = 'FARM_MANAGER' | 'RETAILER' | 'ADMIN';

interface LoginFormProps {
  role: AuthRole;
  onSuccess: (data: { token: string; refreshToken?: string; user: any }) => void;
  onSwitchToRegister: () => void;
}

const THEME: Record<AuthRole, { accent: string; accent2: string; grad: string; label: string; title: string }> = {
  FARM_MANAGER: { accent: '#10b981', accent2: '#34d399', grad: 'linear-gradient(135deg, #059669 0%, #10b981 100%)', label: 'Farm Portal', title: 'Chủ Trang Trại (Farm Manager)' },
  RETAILER: { accent: '#06b6d4', accent2: '#38bdf8', grad: 'linear-gradient(135deg, #0284c7 0%, #06b6d4 100%)', label: 'Retailer Portal', title: 'Nhà Bán Lẻ (Retailer)' },
  ADMIN: { accent: '#8b5cf6', accent2: '#a78bfa', grad: 'linear-gradient(135deg, #7c3aed 0%, #8b5cf6 100%)', label: 'Admin Portal', title: 'Quản Trị Viên (Admin)' },
};

const ENDPOINT: Record<AuthRole, string> = {
  FARM_MANAGER: '/auth/farm/login',
  RETAILER: '/auth/retailer/login',
  ADMIN: '/auth/admin/login',
};

// Tài khoản test đã được seed sẵn trong DatabaseSeeder — bấm để điền nhanh.
const TEST_ACCOUNTS: Record<AuthRole, { id: string; pw: string; note: string }[]> = {
  FARM_MANAGER: [
    { id: 'farm@bicap.com', pw: 'Farmpassword@2026', note: 'Farm Manager (có nông trại & dữ liệu mẫu)' },
    { id: 'farm@bicap.vn', pw: 'Farmpassword@2026', note: 'Farm Manager thứ hai' },
  ],
  RETAILER: [
    { id: 'retailer@bicap.com', pw: 'Retailpassword@2026', note: 'Nhà bán lẻ' },
    { id: 'retail@bicap.com', pw: 'Retailpassword@2026', note: 'Nhà bán lẻ (alias)' },
  ],
  ADMIN: [
    { id: 'superadmin@bicap.com', pw: 'Superadmin@2026', note: 'Super Admin (toàn quyền)' },
    { id: 'admin@bicap.com', pw: 'Adminpassword@2026', note: 'Admin (đọc/ghi)' },
    { id: 'moderator@bicap.com', pw: 'Moderator@2026', note: 'Moderator (chỉ đọc)' },
  ],
};

export const LoginForm: React.FC<LoginFormProps> = ({ role, onSuccess, onSwitchToRegister }) => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showAccounts, setShowAccounts] = useState(true);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ identifier?: string; password?: string }>({});

  const theme = THEME[role];
  const accounts = TEST_ACCOUNTS[role];

  const validate = () => {
    const errors: { identifier?: string; password?: string } = {};
    if (!identifier.trim()) {
      errors.identifier = 'Vui lòng nhập email hoặc số điện thoại';
    }
    if (!password) {
      errors.password = 'Vui lòng nhập mật khẩu';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const fillAccount = (id: string, pw: string) => {
    setIdentifier(id);
    setPassword(pw);
    setFieldErrors({});
    setErrorMessage('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    if (!validate()) return;

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}${ENDPOINT[role]}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          identifier: identifier.trim(),
          password,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        onSuccess({
          token: data.accessToken || data.token,
          refreshToken: data.refreshToken,
          user: {
            id: data.userId ?? data.id ?? 1,
            email: data.email || identifier,
            fullName: data.fullName || theme.title,
            role: role,
          },
        });
      } else {
        const errorData = await response.json().catch(() => ({}));
        setErrorMessage(errorData.message || 'Email, số điện thoại hoặc mật khẩu không chính xác.');
      }
    } catch (err) {
      // M-1: no mock/silent "success" on network failure — a real error must surface
      // so an outage is never mistaken for a successful login.
      console.warn('Backend server unreachable:', err);
      setErrorMessage('Không thể kết nối đến máy chủ. Vui lòng kiểm tra backend đang chạy.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate aria-labelledby="login-form-heading">
      <div style={{ marginBottom: '20px' }}>
        <h2 id="login-form-heading" style={{ fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '6px' }}>
          Đăng nhập hệ thống
        </h2>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary, #cbd5e1)' }}>
          Truy cập tài khoản <strong style={{ color: theme.accent2 }}>{theme.title}</strong> của bạn
        </p>
      </div>

      {/* Test accounts quick-fill */}
      <div
        style={{
          background: 'rgba(139, 92, 246, 0.06)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: '10px',
          padding: '10px 12px',
          marginBottom: '18px',
        }}
      >
        <button
          type="button"
          onClick={() => setShowAccounts((s) => !s)}
          style={{
            background: 'none', border: 'none', cursor: 'pointer', width: '100%',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            color: theme.accent2, fontSize: '12px', fontWeight: 700, padding: 0,
          }}
        >
          <span>🧪 Tài khoản test (bấm để điền nhanh)</span>
          <span>{showAccounts ? '▲' : '▼'}</span>
        </button>
        {showAccounts && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '10px' }}>
            {accounts.map((a) => (
              <button
                key={a.id}
                type="button"
                onClick={() => fillAccount(a.id, a.pw)}
                style={{
                  textAlign: 'left', cursor: 'pointer',
                  background: 'rgba(255, 255, 255, 0.04)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: '8px', padding: '8px 10px',
                }}
              >
                <div style={{ fontSize: '12px', color: '#fff', fontFamily: 'monospace' }}>{a.id} · {a.pw}</div>
                <div style={{ fontSize: '11px', color: 'var(--text-muted, #94a3b8)' }}>{a.note}</div>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Global Error Alert */}
      {errorMessage && (
        <div
          role="alert"
          aria-live="polite"
          className="auth-alert-error"
          style={{
            background: 'rgba(239, 68, 68, 0.15)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            color: '#f87171',
            padding: '12px 16px',
            borderRadius: '8px',
            fontSize: '13px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <span aria-hidden="true">⚠️</span>
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Field: Identifier */}
      <div className="form-group" style={{ marginBottom: '16px' }}>
        <label
          htmlFor="login-identifier"
          className="form-label"
          style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: '#e2e8f0', marginBottom: '6px' }}
        >
          Email hoặc Số điện thoại <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <input
          id="login-identifier"
          type="text"
          value={identifier}
          onChange={(e) => {
            setIdentifier(e.target.value);
            if (fieldErrors.identifier) setFieldErrors({ ...fieldErrors, identifier: undefined });
          }}
          placeholder="e.g. farm@bicap.com hoặc 0912345678"
          aria-invalid={!!fieldErrors.identifier}
          aria-describedby={fieldErrors.identifier ? 'identifier-error' : undefined}
          className="form-input"
          style={{
            width: '100%',
            padding: '12px 14px',
            background: 'rgba(255, 255, 255, 0.05)',
            border: fieldErrors.identifier ? '1px solid #ef4444' : '1px solid rgba(255, 255, 255, 0.15)',
            borderRadius: '8px',
            color: '#fff',
            fontSize: '14px',
            outline: 'none',
            transition: 'border-color 0.2s ease',
          }}
        />
        {fieldErrors.identifier && (
          <span id="identifier-error" role="alert" style={{ fontSize: '12px', color: '#f87171', marginTop: '4px', display: 'block' }}>
            {fieldErrors.identifier}
          </span>
        )}
      </div>

      {/* Field: Password */}
      <div className="form-group" style={{ marginBottom: '16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
          <label
            htmlFor="login-password"
            className="form-label"
            style={{ fontSize: '13px', fontWeight: 500, color: '#e2e8f0' }}
          >
            Mật khẩu <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <button
            type="button"
            className="btn-link"
            style={{ background: 'none', border: 'none', color: theme.accent2, fontSize: '12px', cursor: 'pointer' }}
            onClick={() => alert('Vui lòng liên hệ quản trị viên để khôi phục mật khẩu.')}
          >
            Quên mật khẩu?
          </button>
        </div>

        <div style={{ position: 'relative' }}>
          <input
            id="login-password"
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              if (fieldErrors.password) setFieldErrors({ ...fieldErrors, password: undefined });
            }}
            placeholder="••••••••"
            aria-invalid={!!fieldErrors.password}
            aria-describedby={fieldErrors.password ? 'password-error' : undefined}
            className="form-input"
            style={{
              width: '100%',
              padding: '12px 42px 12px 14px',
              background: 'rgba(255, 255, 255, 0.05)',
              border: fieldErrors.password ? '1px solid #ef4444' : '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: '8px',
              color: '#fff',
              fontSize: '14px',
              outline: 'none',
            }}
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
            style={{
              position: 'absolute',
              right: '12px',
              top: '50%',
              transform: 'translateY(-50%)',
              background: 'none',
              border: 'none',
              color: 'var(--text-muted, #94a3b8)',
              fontSize: '16px',
              cursor: 'pointer',
              padding: '4px',
            }}
          >
            {showPassword ? '🙈' : '👁️'}
          </button>
        </div>
        {fieldErrors.password && (
          <span id="password-error" role="alert" style={{ fontSize: '12px', color: '#f87171', marginTop: '4px', display: 'block' }}>
            {fieldErrors.password}
          </span>
        )}
      </div>

      {/* Remember Me */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '24px' }}>
        <input
          id="remember-me"
          type="checkbox"
          checked={rememberMe}
          onChange={(e) => setRememberMe(e.target.checked)}
          style={{ width: '16px', height: '16px', accentColor: theme.accent, cursor: 'pointer' }}
        />
        <label htmlFor="remember-me" style={{ fontSize: '13px', color: 'var(--text-secondary, #cbd5e1)', cursor: 'pointer' }}>
          Ghi nhớ đăng nhập trên thiết bị này
        </label>
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={loading}
        className="btn-primary"
        style={{
          width: '100%',
          padding: '14px',
          borderRadius: '8px',
          background: theme.grad,
          color: '#fff',
          fontWeight: 600,
          fontSize: '15px',
          border: 'none',
          cursor: loading ? 'not-allowed' : 'pointer',
          boxShadow: '0 4px 14px rgba(0, 0, 0, 0.25)',
          transition: 'all 0.2s ease',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          gap: '8px',
        }}
      >
        {loading ? (
          <>
            <span className="spinner" aria-hidden="true" style={{ width: '16px', height: '16px', border: '2px solid rgba(255,255,255,0.3)', borderTopColor: '#fff', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
            <span>Đang xác thực...</span>
          </>
        ) : (
          <span>Đăng nhập {theme.label}</span>
        )}
      </button>

      {/* Switch to Register */}
      {role !== 'ADMIN' && (
        <div style={{ marginTop: '20px', textAlign: 'center', fontSize: '13px', color: 'var(--text-secondary, #cbd5e1)' }}>
          Chưa có tài khoản?{' '}
          <button
            type="button"
            onClick={onSwitchToRegister}
            style={{
              background: 'none',
              border: 'none',
              color: theme.accent2,
              fontWeight: 600,
              cursor: 'pointer',
              padding: 0,
              textDecoration: 'underline',
            }}
          >
            Đăng ký ngay
          </button>
        </div>
      )}
    </form>
  );
};

export default LoginForm;
