import React, { useEffect, useState } from 'react';
import { API_BASE_URL } from '../../utils/auth';
import LoginForm from '../../components/Auth/LoginForm';
import RegisterForm from '../../components/Auth/RegisterForm';
import type { AuthRole } from '../../components/Auth/LoginForm';

interface AuthPageProps {
  onLoginSuccess: (token: string, user: any, refreshToken?: string) => void;
  defaultRole?: AuthRole;
  defaultMode?: 'login' | 'register';
}

export const AuthPage: React.FC<AuthPageProps> = ({
  onLoginSuccess,
  defaultRole = 'FARM_MANAGER',
  defaultMode = 'login',
}) => {
  const [role, setRole] = useState<AuthRole>(defaultRole);
  const [mode, setMode] = useState<'login' | 'register'>(defaultMode);
  const [verificationStatus, setVerificationStatus] = useState<'loading' | 'success' | 'error' | null>(null);

  const isFarm = role === 'FARM_MANAGER';
  const isAdmin = role === 'ADMIN';
  // Accent used for the active role's theming (farm=green, retailer=cyan, admin=purple).
  const accent = isAdmin ? '#8b5cf6' : isFarm ? '#10b981' : '#06b6d4';
  const accent2 = isAdmin ? '#a78bfa' : isFarm ? '#34d399' : '#38bdf8';

  const selectRole = (next: AuthRole) => {
    setRole(next);
    // Admin không tự đăng ký → luôn trở về chế độ đăng nhập.
    if (next === 'ADMIN') setMode('login');
  };

  const handleSuccess = (data: { token?: string; refreshToken?: string; user?: any; pendingVerification?: boolean }) => {
    if (data.token && data.user) {
      onLoginSuccess(data.token, data.user, data.refreshToken);
    } else {
      // Chuyển sang login sau khi đăng ký thành công nếu chưa trả về token ngay
      setMode('login');
    }
  };

  useEffect(() => {
    const token = new URLSearchParams(window.location.search).get('verifyToken');
    if (!token) return;
    setRole('RETAILER');
    setMode('login');
    setVerificationStatus('loading');
    fetch(`${API_BASE_URL}/auth/retailer/verify-email?token=${encodeURIComponent(token)}`, {
      method: 'POST',
    })
      .then((response) => {
        if (!response.ok) throw new Error('Verification failed');
        setVerificationStatus('success');
        sessionStorage.setItem('retailerProfileRequired', '1');
        window.history.replaceState({}, document.title, window.location.pathname);
      })
      .catch(() => setVerificationStatus('error'));
  }, []);

  return (
    <div
      className={`auth-page-wrapper ${isAdmin ? 'auth-page-wrapper--admin' : isFarm ? 'auth-page-wrapper--farm' : 'auth-page-wrapper--retailer'}`}
      style={pageWrapperStyle}
    >
      {/* Background Glow Overlay */}
      <div
        style={{
          position: 'absolute',
          top: '-15%',
          left: isFarm ? '10%' : '50%',
          width: '500px',
          height: '500px',
          borderRadius: '50%',
          background: isAdmin
            ? 'radial-gradient(circle, rgba(139, 92, 246, 0.2) 0%, rgba(0, 0, 0, 0) 70%)'
            : isFarm
            ? 'radial-gradient(circle, rgba(16, 185, 129, 0.2) 0%, rgba(0, 0, 0, 0) 70%)'
            : 'radial-gradient(circle, rgba(6, 182, 212, 0.2) 0%, rgba(0, 0, 0, 0) 70%)',
          pointerEvents: 'none',
          transition: 'all 0.5s ease',
        }}
      />

      <div style={containerStyle}>
        {/* Left Side: Brand Showcase & Slogan */}
        <div style={leftPanelStyle} className="auth-showcase hide-mobile">
          <div style={logoBadgeStyle}>
            <div style={logoIconStyle}>B</div>
            <span style={logoTextStyle}>BICAP Platform</span>
          </div>

          <h1 style={{ fontSize: '36px', fontWeight: 800, color: '#fff', lineHeight: 1.25, marginTop: '24px', marginBottom: '16px' }}>
            Nền tảng Tích hợp Blockchain trong Sản xuất & Tiêu thụ <span style={{ background: `linear-gradient(to right, ${accent2}, ${accent})`, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Nông sản Sạch</span>
          </h1>

          <p style={{ fontSize: '15px', color: 'var(--text-secondary, #cbd5e1)', lineHeight: 1.6, marginBottom: '32px' }}>
            Minh bạch hóa chuỗi cung ứng nông nghiệp với Blockchain VeChainThor. Kết nối trực tiếp giữa Trang trại chuẩn VietGAP và Nhà bán lẻ toàn quốc.
          </p>

          {/* Value Props list */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={featureItemStyle}>
              <span style={featureIconStyle}>🌾</span>
              <div>
                <strong style={{ color: '#fff', fontSize: '14px', display: 'block' }}>Dành cho Chủ Trang Trại (BICAP-7)</strong>
                <span style={{ fontSize: '12px', color: 'var(--text-muted, #94a3b8)' }}>Quản lý mùa vụ, ghi nhật ký canh tác & cấp mã QR truy xuất nguồn gốc lên Blockchain.</span>
              </div>
            </div>

            <div
              style={featureItemStyle}
              className={!isFarm ? 'auth-feature auth-feature--retailer-active' : 'auth-feature'}
            >
              <span style={featureIconStyle} className="auth-feature__icon" aria-hidden="true">🛒</span>
              <div>
                <strong style={{ color: '#fff', fontSize: '14px', display: 'block' }}>Dành cho Nhà Bán Lẻ</strong>
                <span style={{ fontSize: '12px', color: 'var(--text-muted, #94a3b8)' }}>Tìm kiếm nguồn hàng nông sản sạch, đặt đơn hàng lớn & theo dõi tiến trình vận chuyển.</span>
              </div>
            </div>

            <div style={featureItemStyle}>
              <span style={featureIconStyle}>🔗</span>
              <div>
                <strong style={{ color: '#fff', fontSize: '14px', display: 'block' }}>Bảo mật & Chuẩn Trợ Năng (WCAG AA)</strong>
                <span style={{ fontSize: '12px', color: 'var(--text-muted, #94a3b8)' }}>Xác thực JWT Token bảo mật cao, tối ưu trải nghiệm Screen Reader cho mọi người dùng.</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side: Auth Card Container */}
        <div style={rightPanelStyle} className="auth-panel">
          <div
            className={`auth-card glass-panel ${!isFarm ? 'auth-card--retailer' : ''}`}
            style={cardStyle}
          >
            {verificationStatus && (
              <div
                role={verificationStatus === 'error' ? 'alert' : 'status'}
                aria-live="polite"
                className={`retailer-verification retailer-verification--${verificationStatus}`}
              >
                {verificationStatus === 'loading' && 'Đang xác nhận địa chỉ email...'}
                {verificationStatus === 'success' && 'Email đã được xác nhận. Bạn có thể đăng nhập ngay.'}
                {verificationStatus === 'error' && 'Liên kết xác nhận không hợp lệ hoặc đã hết hạn.'}
              </div>
            )}
            
            {/* 1. Role Selector Tablist (BICAP-7 vs BICAP-36 vs Admin) */}
            <div
              role="tablist"
              aria-label="Chọn Vai trò Tài khoản"
              style={{
                display: 'flex',
                background: 'rgba(0, 0, 0, 0.3)',
                padding: '4px',
                borderRadius: '10px',
                marginBottom: '20px',
                border: '1px solid rgba(255, 255, 255, 0.08)',
              }}
            >
              <button
                id="tab-farm-manager"
                role="tab"
                aria-selected={isFarm}
                aria-controls="auth-form-panel"
                onClick={() => selectRole('FARM_MANAGER')}
                style={{
                  flex: 1,
                  padding: '10px 8px',
                  borderRadius: '8px',
                  border: 'none',
                  background: isFarm ? 'rgba(16, 185, 129, 0.2)' : 'transparent',
                  color: isFarm ? '#34d399' : 'var(--text-secondary, #cbd5e1)',
                  fontWeight: isFarm ? 700 : 500,
                  fontSize: '13px',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  boxShadow: isFarm ? '0 2px 8px rgba(16, 185, 129, 0.2)' : 'none',
                }}
              >
                <span>🌾</span>
                <span>Farm</span>
              </button>

              <button
                id="tab-retailer"
                role="tab"
                className="auth-role-tab auth-role-tab--retailer"
                aria-selected={!isFarm && !isAdmin}
                aria-controls="auth-form-panel"
                onClick={() => selectRole('RETAILER')}
                style={{
                  flex: 1,
                  padding: '10px 8px',
                  borderRadius: '8px',
                  border: 'none',
                  background: !isFarm && !isAdmin ? 'rgba(6, 182, 212, 0.2)' : 'transparent',
                  color: !isFarm && !isAdmin ? '#38bdf8' : 'var(--text-secondary, #cbd5e1)',
                  fontWeight: !isFarm && !isAdmin ? 700 : 500,
                  fontSize: '13px',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  boxShadow: !isFarm && !isAdmin ? '0 2px 8px rgba(6, 182, 212, 0.2)' : 'none',
                }}
              >
                <span>🛒</span>
                <span>Retailer</span>
              </button>

              <button
                id="tab-admin"
                role="tab"
                aria-selected={isAdmin}
                aria-controls="auth-form-panel"
                onClick={() => selectRole('ADMIN')}
                style={{
                  flex: 1,
                  padding: '10px 8px',
                  borderRadius: '8px',
                  border: 'none',
                  background: isAdmin ? 'rgba(139, 92, 246, 0.2)' : 'transparent',
                  color: isAdmin ? '#a78bfa' : 'var(--text-secondary, #cbd5e1)',
                  fontWeight: isAdmin ? 700 : 500,
                  fontSize: '13px',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  boxShadow: isAdmin ? '0 2px 8px rgba(139, 92, 246, 0.2)' : 'none',
                }}
              >
                <span>🛡️</span>
                <span>Admin</span>
              </button>
            </div>

            {/* 2. Mode Switcher (Login vs Register) — Admin chỉ đăng nhập, không tự đăng ký */}
            {!isAdmin && (
            <div
              role="tablist"
              aria-label="Chọn Chế độ Xác thực"
              style={{
                display: 'flex',
                borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
                marginBottom: '24px',
              }}
            >
              <button
                id="tab-login-mode"
                role="tab"
                aria-selected={mode === 'login'}
                aria-controls="auth-form-panel"
                onClick={() => setMode('login')}
                style={{
                  flex: 1,
                  padding: '12px',
                  background: 'none',
                  border: 'none',
                  borderBottom: mode === 'login' ? `3px solid ${accent}` : '3px solid transparent',
                  color: mode === 'login' ? '#fff' : 'var(--text-muted, #94a3b8)',
                  fontWeight: mode === 'login' ? 700 : 500,
                  fontSize: '14px',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                }}
              >
                Đăng Nhập
              </button>

              <button
                id="tab-register-mode"
                role="tab"
                aria-selected={mode === 'register'}
                aria-controls="auth-form-panel"
                onClick={() => setMode('register')}
                style={{
                  flex: 1,
                  padding: '12px',
                  background: 'none',
                  border: 'none',
                  borderBottom: mode === 'register' ? `3px solid ${accent}` : '3px solid transparent',
                  color: mode === 'register' ? '#fff' : 'var(--text-muted, #94a3b8)',
                  fontWeight: mode === 'register' ? 700 : 500,
                  fontSize: '14px',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                }}
              >
                Tạo Tài Khoản Mới
              </button>
            </div>
            )}

            {/* 3. Form Content Panel */}
            <div id="auth-form-panel" role="tabpanel" tabIndex={0} style={{ outline: 'none' }}>
              {mode === 'login' || isAdmin ? (
                <LoginForm
                  role={role}
                  onSuccess={handleSuccess}
                  onSwitchToRegister={() => setMode('register')}
                />
              ) : (
                <RegisterForm
                  role={role as 'FARM_MANAGER' | 'RETAILER'}
                  onSuccess={handleSuccess}
                  onSwitchToLogin={() => setMode('login')}
                />
              )}
            </div>

          </div>
        </div>

      </div>
    </div>
  );
};

/* Styles */
const pageWrapperStyle: React.CSSProperties = {
  minHeight: '100vh',
  width: '100%',
  backgroundColor: '#0b0f19',
  backgroundImage: 'radial-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px)',
  backgroundSize: '24px 24px',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '32px 16px',
  position: 'relative',
  boxSizing: 'border-box',
  overflowX: 'hidden',
};

const containerStyle: React.CSSProperties = {
  width: '100%',
  maxWidth: '1100px',
  display: 'grid',
  gridTemplateColumns: '1fr 1.1fr',
  gap: '48px',
  alignItems: 'center',
  zIndex: 1,
};

const leftPanelStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
};

const rightPanelStyle: React.CSSProperties = {
  width: '100%',
};

const cardStyle: React.CSSProperties = {
  padding: '36px',
  borderRadius: '16px',
  background: 'rgba(15, 23, 42, 0.85)',
  backdropFilter: 'blur(16px)',
  border: '1px solid rgba(255, 255, 255, 0.1)',
  boxShadow: '0 20px 50px rgba(0, 0, 0, 0.5)',
};

const logoBadgeStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '12px',
};

const logoIconStyle: React.CSSProperties = {
  width: '42px',
  height: '42px',
  borderRadius: '12px',
  background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
  color: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontWeight: 800,
  fontSize: '22px',
  boxShadow: '0 4px 14px rgba(16, 185, 129, 0.4)',
};

const logoTextStyle: React.CSSProperties = {
  fontSize: '22px',
  fontWeight: 800,
  letterSpacing: '-0.5px',
  background: 'linear-gradient(to right, #fff, #38bdf8)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
};

const featureItemStyle: React.CSSProperties = {
  display: 'flex',
  gap: '14px',
  alignItems: 'flex-start',
  padding: '12px 16px',
  background: 'rgba(255, 255, 255, 0.03)',
  borderRadius: '10px',
  border: '1px solid rgba(255, 255, 255, 0.06)',
};

const featureIconStyle: React.CSSProperties = {
  fontSize: '20px',
  lineHeight: 1,
};

export default AuthPage;
