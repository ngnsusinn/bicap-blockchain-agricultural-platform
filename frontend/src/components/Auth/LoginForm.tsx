import React, { useState } from 'react';

interface LoginFormProps {
  role: 'FARM_MANAGER' | 'RETAILER';
  onSuccess: (data: { token: string; user: any }) => void;
  onSwitchToRegister: () => void;
}

export const LoginForm: React.FC<LoginFormProps> = ({ role, onSuccess, onSwitchToRegister }) => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ identifier?: string; password?: string }>({});

  const roleTitle = role === 'FARM_MANAGER' ? 'Chủ Trang Trại (Farm Manager)' : 'Nhà Bán Lẻ (Retailer)';

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    
    if (!validate()) return;

    setLoading(true);

    try {
      const endpoint = role === 'FARM_MANAGER' 
        ? 'http://localhost:8080/api/auth/farm/login'
        : 'http://localhost:8080/api/auth/retailer/login';

      const response = await fetch(endpoint, {
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
          user: {
            id: data.id || 1,
            email: data.email || identifier,
            fullName: data.fullName || (role === 'FARM_MANAGER' ? 'Trần Văn Nông' : 'Nguyễn Thị Bán Lẻ'),
            role: role,
          },
        });
      } else {
        const errorData = await response.json().catch(() => ({}));
        setErrorMessage(errorData.message || 'Email, số điện thoại hoặc mật khẩu không chính xác.');
      }
    } catch (err) {
      // Mock fallback cho môi trường dev khi backend offline
      console.warn('Backend server unreachable, using dev fallback authentication');
      setTimeout(() => {
        onSuccess({
          token: 'mock-jwt-token-dev-mode',
          user: {
            id: 1,
            email: identifier.includes('@') ? identifier : 'user@bicap.com',
            fullName: role === 'FARM_MANAGER' ? 'Chủ Trang Trại BICAP' : 'Nhà Bán Lẻ BICAP',
            role: role,
          },
        });
      }, 600);
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
          Truy cập tài khoản <strong style={{ color: role === 'FARM_MANAGER' ? '#10b981' : '#06b6d4' }}>{roleTitle}</strong> của bạn
        </p>
      </div>

      {/* Quick 1-Click Demo Login Box */}
      <div style={{ marginBottom: '20px', padding: '14px', background: 'rgba(255, 255, 255, 0.05)', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.12)' }}>
        <div style={{ fontSize: '12px', color: '#e2e8f0', marginBottom: '10px', fontWeight: 600, textAlign: 'center' }}>
          ⚡ DÙNG THỬ GIAO DIỆN TRỰC TIẾP (1-CLICK):
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
          <button
            type="button"
            onClick={() => onSuccess({
              token: 'mock-jwt-farm-demo',
              user: { id: 1, email: 'farm@bicap.com', fullName: 'Chủ Trang Trại BICAP', role: 'FARM_MANAGER' }
            })}
            style={{
              padding: '10px',
              borderRadius: '8px',
              border: 'none',
              background: 'linear-gradient(135deg, #059669 0%, #10b981 100%)',
              color: '#fff',
              fontWeight: 700,
              fontSize: '13px',
              cursor: 'pointer',
              boxShadow: '0 4px 10px rgba(16, 185, 129, 0.3)',
            }}
          >
            🌾 UI Farm
          </button>

          <button
            type="button"
            onClick={() => onSuccess({
              token: 'mock-jwt-retailer-demo',
              user: { id: 2, email: 'retailer@bicap.com', fullName: 'Nhà Bán Lẻ BICAP', role: 'RETAILER' }
            })}
            style={{
              padding: '10px',
              borderRadius: '8px',
              border: 'none',
              background: 'linear-gradient(135deg, #0284c7 0%, #06b6d4 100%)',
              color: '#fff',
              fontWeight: 700,
              fontSize: '13px',
              cursor: 'pointer',
              boxShadow: '0 4px 10px rgba(6, 182, 212, 0.3)',
            }}
          >
            🛒 UI Retailer
          </button>
        </div>
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
          placeholder="e.g. farm@bicap.vn hoặc 0912345678"
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
            style={{ background: 'none', border: 'none', color: '#38bdf8', fontSize: '12px', cursor: 'pointer' }}
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
          style={{ width: '16px', height: '16px', accentColor: role === 'FARM_MANAGER' ? '#10b981' : '#06b6d4', cursor: 'pointer' }}
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
          background: role === 'FARM_MANAGER' 
            ? 'linear-gradient(135deg, #059669 0%, #10b981 100%)' 
            : 'linear-gradient(135deg, #0284c7 0%, #06b6d4 100%)',
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
          <span>Đăng nhập {role === 'FARM_MANAGER' ? 'Farm Portal' : 'Retailer Portal'}</span>
        )}
      </button>

      {/* Switch to Register */}
      <div style={{ marginTop: '20px', textAlign: 'center', fontSize: '13px', color: 'var(--text-secondary, #cbd5e1)' }}>
        Chưa có tài khoản?{' '}
        <button
          type="button"
          onClick={onSwitchToRegister}
          style={{
            background: 'none',
            border: 'none',
            color: role === 'FARM_MANAGER' ? '#34d399' : '#38bdf8',
            fontWeight: 600,
            cursor: 'pointer',
            padding: 0,
            textDecoration: 'underline',
          }}
        >
          Đăng ký ngay
        </button>
      </div>
    </form>
  );
};

export default LoginForm;
