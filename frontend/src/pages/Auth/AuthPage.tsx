import React, { useState } from 'react';
import LoginForm from '../../components/Auth/LoginForm';
import RegisterForm from '../../components/Auth/RegisterForm';

interface AuthPageProps {
  onLoginSuccess: (token: string, user: any) => void;
  defaultRole?: 'FARM_MANAGER' | 'RETAILER';
  defaultMode?: 'login' | 'register';
}

export const AuthPage: React.FC<AuthPageProps> = ({
  onLoginSuccess,
  defaultRole = 'FARM_MANAGER',
  defaultMode = 'login',
}) => {
  const [role, setRole] = useState<'FARM_MANAGER' | 'RETAILER'>(defaultRole);
  const [mode, setMode] = useState<'login' | 'register'>(defaultMode);

  const isFarm = role === 'FARM_MANAGER';

  const handleSuccess = (data: { token?: string; user?: any; pendingVerification?: boolean }) => {
    if (data.token && data.user) {
      onLoginSuccess(data.token, data.user);
    } else {
      // Chuyển sang login sau khi đăng ký thành công nếu chưa trả về token ngay
      setMode('login');
    }
  };

  return (
    <div className="auth-page-wrapper" style={pageWrapperStyle}>
      {/* Background Glow Overlay */}
      <div
        style={{
          position: 'absolute',
          top: '-15%',
          left: isFarm ? '10%' : '50%',
          width: '500px',
          height: '500px',
          borderRadius: '50%',
          background: isFarm 
            ? 'radial-gradient(circle, rgba(16, 185, 129, 0.2) 0%, rgba(0, 0, 0, 0) 70%)'
            : 'radial-gradient(circle, rgba(6, 182, 212, 0.2) 0%, rgba(0, 0, 0, 0) 70%)',
          pointerEvents: 'none',
          transition: 'all 0.5s ease',
        }}
      />

      <div style={containerStyle}>
        {/* Left Side: Brand Showcase & Slogan */}
        <div style={leftPanelStyle} className="hide-mobile">
          <div style={logoBadgeStyle}>
            <div style={logoIconStyle}>B</div>
            <span style={logoTextStyle}>BICAP Platform</span>
          </div>

          <h1 style={{ fontSize: '36px', fontWeight: 800, color: '#fff', lineHeight: 1.25, marginTop: '24px', marginBottom: '16px' }}>
            Nền tảng Tích hợp Blockchain trong Sản xuất & Tiêu thụ <span style={{ background: isFarm ? 'linear-gradient(to right, #34d399, #10b981)' : 'linear-gradient(to right, #38bdf8, #06b6d4)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Nông sản Sạch</span>
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

            <div style={featureItemStyle}>
              <span style={featureIconStyle}>🛒</span>
              <div>
                <strong style={{ color: '#fff', fontSize: '14px', display: 'block' }}>Dành cho Nhà Bán Lẻ (BICAP-36)</strong>
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
        <div style={rightPanelStyle}>
          <div className="auth-card glass-panel" style={cardStyle}>
            
            {/* 1. Role Selector Tablist (BICAP-7 vs BICAP-36) */}
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
                onClick={() => setRole('FARM_MANAGER')}
                style={{
                  flex: 1,
                  padding: '10px 14px',
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
                <span>Farm Manager</span>
                <span style={{ fontSize: '10px', opacity: 0.7 }}>(BICAP-7)</span>
              </button>

              <button
                id="tab-retailer"
                role="tab"
                aria-selected={!isFarm}
                aria-controls="auth-form-panel"
                onClick={() => setRole('RETAILER')}
                style={{
                  flex: 1,
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: 'none',
                  background: !isFarm ? 'rgba(6, 182, 212, 0.2)' : 'transparent',
                  color: !isFarm ? '#38bdf8' : 'var(--text-secondary, #cbd5e1)',
                  fontWeight: !isFarm ? 700 : 500,
                  fontSize: '13px',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  boxShadow: !isFarm ? '0 2px 8px rgba(6, 182, 212, 0.2)' : 'none',
                }}
              >
                <span>🛒</span>
                <span>Retailer</span>
                <span style={{ fontSize: '10px', opacity: 0.7 }}>(BICAP-36)</span>
              </button>
            </div>

            {/* 2. Mode Switcher (Login vs Register) */}
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
                  borderBottom: mode === 'login' ? `3px solid ${isFarm ? '#10b981' : '#06b6d4'}` : '3px solid transparent',
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
                  borderBottom: mode === 'register' ? `3px solid ${isFarm ? '#10b981' : '#06b6d4'}` : '3px solid transparent',
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

            {/* 3. Form Content Panel */}
            <div id="auth-form-panel" role="tabpanel" tabIndex={0} style={{ outline: 'none' }}>
              {mode === 'login' ? (
                <LoginForm
                  role={role}
                  onSuccess={handleSuccess}
                  onSwitchToRegister={() => setMode('register')}
                />
              ) : (
                <RegisterForm
                  role={role}
                  onSuccess={handleSuccess}
                  onSwitchToLogin={() => setMode('login')}
                />
              )}
            </div>

            {/* 4. Quick Access Demo Buttons */}
            <div style={{ marginTop: '24px', paddingTop: '20px', borderTop: '1px dashed rgba(255, 255, 255, 0.15)', textAlign: 'center' }}>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '12px', fontWeight: 600 }}>
                ⚡ TRUY CẬP TRỰC TIẾP GIAO DIỆN (KHÔNG CẦN BẤM ĐĂNG NHẬP):
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <button
                  type="button"
                  onClick={() => onLoginSuccess('demo-farm-token', { id: 1, email: 'farm@bicap.com', fullName: 'Chủ Trang Trại BICAP', role: 'FARM_MANAGER' })}
                  style={{
                    padding: '11px 14px',
                    borderRadius: '8px',
                    border: '1px solid rgba(16, 185, 129, 0.4)',
                    background: 'linear-gradient(135deg, rgba(5, 150, 105, 0.2) 0%, rgba(16, 185, 129, 0.3) 100%)',
                    color: '#34d399',
                    fontWeight: 700,
                    fontSize: '13px',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    boxShadow: '0 4px 12px rgba(16, 185, 129, 0.15)',
                  }}
                >
                  🌾 Mở UI Farm Manager
                </button>

                <button
                  type="button"
                  onClick={() => onLoginSuccess('demo-retailer-token', { id: 2, email: 'retailer@bicap.com', fullName: 'Nhà Bán Lẻ BICAP', role: 'RETAILER' })}
                  style={{
                    padding: '11px 14px',
                    borderRadius: '8px',
                    border: '1px solid rgba(6, 182, 212, 0.4)',
                    background: 'linear-gradient(135deg, rgba(2, 132, 199, 0.2) 0%, rgba(6, 182, 212, 0.3) 100%)',
                    color: '#38bdf8',
                    fontWeight: 700,
                    fontSize: '13px',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    boxShadow: '0 4px 12px rgba(6, 182, 212, 0.15)',
                  }}
                >
                  🛒 Mở UI Retailer
                </button>
              </div>
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
