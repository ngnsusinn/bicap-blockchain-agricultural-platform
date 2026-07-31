import React, { useState, useEffect } from 'react';
import { getAuthHeaders, isLoggedIn, getCurrentUser, saveSession, logout } from './utils/auth';
import type { UserSession } from './utils/auth';
import ServicePackages from './pages/FarmManager/ServicePackages';
import AuthPage from './pages/Auth/AuthPage';

const API_BASE_URL = 'http://localhost:8080/api';

/* ── Sidebar Component (Dành cho Farm Manager - BICAP-7) ── */
interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
  hasActiveSubscription: boolean;
}

const Sidebar: React.FC<SidebarProps> = ({ currentTab, onTabChange, hasActiveSubscription }) => {
  const menuItems = [
    { id: 'dashboard', label: 'Dashboard', icon: '📊', isProtected: false },
    { id: 'packages', label: 'Gói Dịch Vụ', icon: '📦', isProtected: false },
    { id: 'farm-info', label: 'Nông Trại Của Tôi', icon: '🌾', isProtected: false },
    { id: 'products', label: 'Sản Phẩm & QR Code', icon: '🔍', isProtected: true },
    { id: 'iot', label: 'Giám Sát IoT', icon: '🌡️', isProtected: true },
    { id: 'certificates', label: 'Chứng Nhận VietGAP', icon: '📜', isProtected: true },
    { id: 'settings', label: 'Cài Đặt', icon: '⚙️', isProtected: false },
  ];

  const handleTabClick = (item: typeof menuItems[0]) => {
    if (item.isProtected && !hasActiveSubscription) {
      alert('Bạn cần đăng ký gói dịch vụ để sử dụng tính năng bảo vệ này.');
      return;
    }
    onTabChange(item.id);
  };

  return (
    <aside style={sidebarStyle}>
      <div style={logoContainerStyle}>
        <div style={logoIconStyle}>B</div>
        <span style={logoTextStyle}>BICAP Farm</span>
      </div>
      <nav style={navStyle}>
        {menuItems.map((item) => {
          const isActive = item.id === currentTab;
          const isLocked = item.isProtected && !hasActiveSubscription;

          return (
            <button
              key={item.id}
              onClick={() => handleTabClick(item)}
              style={{
                ...navItemStyle,
                color: isActive ? '#fff' : isLocked ? 'var(--text-muted)' : 'var(--text-secondary)',
                background: isActive ? 'rgba(16, 185, 129, 0.15)' : 'transparent',
                borderLeft: isActive ? '3px solid #10b981' : '3px solid transparent',
                cursor: 'pointer',
              }}
            >
              <span style={{ fontSize: '18px' }}>{item.icon}</span>
              <span style={{ fontWeight: isActive ? 600 : 400 }}>{item.label}</span>
              {isLocked && <span style={tagStyle}>🔒 VIP</span>}
            </button>
          );
        })}
      </nav>
      <div style={footerStyle}>
        <div style={farmBadgeStyle}>
          <span style={{ fontSize: '14px' }}>🌱</span>
          <div>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)' }}>Farm Manager Portal</div>
            <div style={{ fontSize: '10px', color: 'var(--text-muted)' }}>BICAP-7 Active</div>
          </div>
        </div>
        <p style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '12px' }}>BICAP Platform v0.1</p>
      </div>
    </aside>
  );
};

/* ── Main App Component ── */
export default function App() {
  const [authenticated, setAuthenticated] = useState<boolean>(isLoggedIn());
  const [user, setUser] = useState<UserSession | null>(getCurrentUser());
  const [currentTab, setCurrentTab] = useState('packages');
  const [hasActiveSubscription, setHasActiveSubscription] = useState(false);
  const farmId = 1;

  // Xử lý sau khi Đăng nhập thành công từ AuthPage
  const handleLoginSuccess = (token: string, userData: any) => {
    saveSession(token, userData);
    setAuthenticated(true);
    setUser(userData);
  };

  // Xử lý Đăng xuất
  const handleLogout = () => {
    logout();
    setAuthenticated(false);
    setUser(null);
  };

  // Check subscription status nếu là Farm Manager
  useEffect(() => {
    if (!authenticated || user?.role !== 'FARM_MANAGER') return;

    const checkSubscription = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/subscriptions/farm/${farmId}`, {
          headers: getAuthHeaders(),
        });
        if (res.ok) {
          const subData = await res.json();
          const activeSub = Array.isArray(subData)
            ? subData.find((s: any) => s.status === 'ACTIVE')
            : (subData?.status === 'ACTIVE' ? subData : null);

          setHasActiveSubscription(!!activeSub);
        }
      } catch (err) {
        setHasActiveSubscription(false);
      }
    };

    checkSubscription();
  }, [authenticated, user, currentTab]);

  // Render AuthPage nếu chưa đăng nhập (Hỗ trợ cả BICAP-7 và BICAP-36)
  if (!authenticated) {
    return <AuthPage onLoginSuccess={handleLoginSuccess} />;
  }

  // Hàm chuyển đổi giao diện trực tiếp giữa Farm Manager và Retailer
  const switchRole = (newRole: 'FARM_MANAGER' | 'RETAILER') => {
    const updatedUser: UserSession = {
      id: newRole === 'FARM_MANAGER' ? 1 : 2,
      email: newRole === 'FARM_MANAGER' ? 'farm@bicap.com' : 'retailer@bicap.com',
      fullName: newRole === 'FARM_MANAGER' ? 'Chủ Trang Trại BICAP' : 'Nhà Bán Lẻ BICAP',
      role: newRole,
    };
    saveSession('mock-token-' + newRole, updatedUser);
    setUser(updatedUser);
  };

  // Render Retailer Portal nếu người dùng là RETAILER (BICAP-36)
  if (user?.role === 'RETAILER') {
    return (
      <div className="retailer-portal" style={{ minHeight: '100vh', background: '#0b0f19', color: '#fff', padding: '24px' }}>
        {/* Retailer Header */}
        <header style={headerStyle}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ ...logoIconStyle, background: 'linear-gradient(135deg, #0284c7 0%, #06b6d4 100%)' }}>R</div>
            <span style={logoTextStyle}>BICAP Retailer Marketplace (BICAP-36)</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <button
              onClick={() => switchRole('FARM_MANAGER')}
              style={{
                background: 'rgba(16, 185, 129, 0.2)',
                border: '1px solid rgba(16, 185, 129, 0.4)',
                color: '#34d399',
                padding: '6px 14px',
                borderRadius: '8px',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              🌾 Chuyển sang UI Farm Manager
            </button>
            <span style={{ fontSize: '13px', color: '#cbd5e1' }}>
              Xin chào, <strong>{user.fullName}</strong> <span style={roleBadgeRetailerStyle}>Retailer</span>
            </span>
            <button onClick={handleLogout} style={logoutButtonStyle}>
              🚪 Đăng xuất
            </button>
          </div>
        </header>

        {/* Retailer Content */}
        <main style={{ maxWidth: '1200px', margin: '40px auto 0 auto' }}>
          <div className="glass-panel" style={{ padding: '36px', borderRadius: '16px' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🛒</div>
            <h1 className="dashboard-title" style={{ background: 'linear-gradient(to right, #38bdf8, #06b6d4)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              Sàn Giao Dịch Nông Sản Sạch - Nhà Bán Lẻ
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '15px', marginTop: '8px', lineHeight: 1.6 }}>
              Chào mừng nhà bán lẻ <strong>{user.fullName}</strong> ({user.email}) đã đăng nhập thành công theo yêu cầu <strong>BICAP-36 (SRS-RT-001)</strong>.
            </p>

            {/* Quick stats for Retailer */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px', marginTop: '32px' }}>
              <div style={statCardStyle}>
                <div style={{ fontSize: '24px' }}>🔍</div>
                <h3 style={{ fontSize: '16px', color: '#fff', margin: '8px 0 4px 0' }}>Tìm kiếm Nông sản</h3>
                <p style={{ fontSize: '12px', color: '#94a3b8' }}>Duyệt danh mục sản phẩm đạt chứng nhận VietGAP/GlobalGAP.</p>
              </div>

              <div style={statCardStyle}>
                <div style={{ fontSize: '24px' }}>📦</div>
                <h3 style={{ fontSize: '16px', color: '#fff', margin: '8px 0 4px 0' }}>Đơn hàng của tôi</h3>
                <p style={{ fontSize: '12px', color: '#94a3b8' }}>Quản lý các hợp đồng mua bán nông sản trực tiếp từ trang trại.</p>
              </div>

              <div style={statCardStyle}>
                <div style={{ fontSize: '24px' }}>🚚</div>
                <h3 style={{ fontSize: '16px', color: '#fff', margin: '8px 0 4px 0' }}>Theo dõi Vận chuyển</h3>
                <p style={{ fontSize: '12px', color: '#94a3b8' }}>Tracking thời gian thực tiến trình giao nhận lô hàng.</p>
              </div>
            </div>
          </div>
        </main>
      </div>
    );
  }

  // Render Farm Manager Portal (BICAP-7)
  return (
    <div className="app-container">
      <Sidebar
        currentTab={currentTab}
        onTabChange={setCurrentTab}
        hasActiveSubscription={hasActiveSubscription}
      />

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {/* Top Header Bar */}
        <header style={{ ...headerStyle, marginLeft: 'var(--sidebar-width)' }}>
          <div style={{ fontSize: '14px', color: '#cbd5e1' }}>
            Cổng Quản Lý Nông Trại <strong style={{ color: '#10b981' }}>(BICAP-7)</strong>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <button
              onClick={() => switchRole('RETAILER')}
              style={{
                background: 'rgba(6, 182, 212, 0.2)',
                border: '1px solid rgba(6, 182, 212, 0.4)',
                color: '#38bdf8',
                padding: '6px 14px',
                borderRadius: '8px',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              🛒 Chuyển sang UI Retailer
            </button>
            <span style={{ fontSize: '13px', color: '#cbd5e1' }}>
              Xin chào, <strong>{user?.fullName}</strong> <span style={roleBadgeFarmStyle}>Farm Manager</span>
            </span>
            <button onClick={handleLogout} style={logoutButtonStyle}>
              🚪 Đăng xuất
            </button>
          </div>
        </header>

        <main className="main-content animate-fade-in" style={{ marginTop: '60px' }}>
          {currentTab === 'packages' && <ServicePackages />}

          {currentTab === 'dashboard' && (
            <div>
              <h1 className="dashboard-title">Farm Dashboard</h1>
              <p className="dashboard-subtitle">Tổng quan hoạt động nông trại và nhật ký Blockchain.</p>
              <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
                <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Bảng Điều Khiển Nông Trại</h2>
                <p style={{ color: 'var(--text-secondary)', marginTop: '8px', maxWidth: '500px', marginInline: 'auto', fontSize: '14px', lineHeight: 1.6 }}>
                  Đã xác thực tài khoản Chủ trang trại thành công ({user?.email}).
                </p>
              </div>
            </div>
          )}

          {currentTab === 'farm-info' && (
            <div>
              <h1 className="dashboard-title">Thông Tin Nông Trại</h1>
              <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌾</div>
                <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Hồ Sơ & Vùng Canh Tác Trang Trại</h2>
              </div>
            </div>
          )}

          {currentTab === 'products' && (
            <div>
              <h1 className="dashboard-title">Sản Phẩm & Mã QR Blockchain</h1>
              <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
                <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Quản lý Sản Phẩm & Mã Truy Xuất VeChain</h2>
              </div>
            </div>
          )}

          {currentTab === 'iot' && (
            <div>
              <h1 className="dashboard-title">Giám Sát Cảm Biến IoT</h1>
              <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌡️</div>
                <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Theo Dõi Dữ Liệu Nhiệt Độ, Độ Ẩm & pH Realtime</h2>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

/* ── Component Styles ── */
const sidebarStyle: React.CSSProperties = {
  width: 'var(--sidebar-width)',
  position: 'fixed',
  top: 0,
  left: 0,
  bottom: 0,
  background: 'rgba(15, 16, 22, 0.95)',
  borderRight: '1px solid var(--border-color)',
  display: 'flex',
  flexDirection: 'column',
  zIndex: 1000,
  padding: '24px 16px',
};

const logoContainerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '12px',
  marginBottom: '40px',
  paddingLeft: '8px',
};

const logoIconStyle: React.CSSProperties = {
  width: '36px',
  height: '36px',
  borderRadius: '10px',
  background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
  color: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontWeight: 800,
  fontSize: '18px',
  boxShadow: '0 4px 12px rgba(16, 185, 129, 0.4)',
};

const logoTextStyle: React.CSSProperties = {
  fontSize: '18px',
  fontWeight: 800,
  letterSpacing: '-0.5px',
  background: 'linear-gradient(to right, #fff, #06b6d4)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
};

const navStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '4px',
  flex: 1,
};

const navItemStyle: React.CSSProperties = {
  width: '100%',
  padding: '12px 16px',
  borderRadius: '0 8px 8px 0',
  display: 'flex',
  alignItems: 'center',
  gap: '12px',
  textAlign: 'left',
  transition: 'all 0.2s ease',
  fontSize: '14px',
};

const tagStyle: React.CSSProperties = {
  marginLeft: 'auto',
  fontSize: '9px',
  background: 'rgba(239, 68, 68, 0.1)',
  padding: '2px 6px',
  borderRadius: '10px',
  color: '#ef4444',
  border: '1px solid rgba(239, 68, 68, 0.2)',
};

const footerStyle: React.CSSProperties = {
  marginTop: 'auto',
  paddingLeft: '8px',
};

const farmBadgeStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '10px',
  padding: '10px 12px',
  background: 'rgba(255, 255, 255, 0.03)',
  border: '1px solid rgba(255, 255, 255, 0.06)',
  borderRadius: '10px',
};

const headerStyle: React.CSSProperties = {
  height: '64px',
  background: 'rgba(15, 23, 42, 0.9)',
  backdropFilter: 'blur(12px)',
  borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: '0 32px',
};

const roleBadgeFarmStyle: React.CSSProperties = {
  fontSize: '11px',
  background: 'rgba(16, 185, 129, 0.2)',
  color: '#34d399',
  padding: '2px 8px',
  borderRadius: '12px',
  border: '1px solid rgba(16, 185, 129, 0.3)',
  marginLeft: '6px',
};

const roleBadgeRetailerStyle: React.CSSProperties = {
  fontSize: '11px',
  background: 'rgba(6, 182, 212, 0.2)',
  color: '#38bdf8',
  padding: '2px 8px',
  borderRadius: '12px',
  border: '1px solid rgba(6, 182, 212, 0.3)',
  marginLeft: '6px',
};

const logoutButtonStyle: React.CSSProperties = {
  background: 'rgba(239, 68, 68, 0.1)',
  border: '1px solid rgba(239, 68, 68, 0.2)',
  color: '#f87171',
  padding: '6px 14px',
  borderRadius: '8px',
  fontSize: '12px',
  fontWeight: 600,
  cursor: 'pointer',
  transition: 'all 0.2s ease',
};

const statCardStyle: React.CSSProperties = {
  background: 'rgba(255, 255, 255, 0.03)',
  border: '1px solid rgba(255, 255, 255, 0.08)',
  borderRadius: '12px',
  padding: '20px',
  textAlign: 'left',
};