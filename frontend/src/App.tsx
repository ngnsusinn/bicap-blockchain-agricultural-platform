import React, { useState, useEffect } from 'react';
import { getAuthHeaders, isLoggedIn, getCurrentUser, saveSession, logout, API_BASE_URL } from './utils/auth';
import type { UserSession } from './utils/auth';
import ServicePackages from './pages/FarmManager/ServicePackages';
import AuthPage from './pages/Auth/AuthPage';
import ProfilePage from './pages/FarmManager/ProfilePage';
import RetailerProfilePage from './pages/Retailer/RetailerProfilePage';
import RetailerBusinessPage from './pages/Retailer/RetailerBusinessPage';
import SeasonExports from './pages/FarmManager/SeasonExports';
import TradingFloor from './pages/FarmManager/TradingFloor';
import Orders from './pages/FarmManager/Orders';
import Retailers from './pages/FarmManager/Retailers';
import TracePage from './pages/TracePage';
import NotificationBell from './components/NotificationBell';
import IotDashboard from './pages/FarmManager/IotDashboard';

// === THÊM MỚI (NV1): 3 Màn hình Guest (BICAP 69 - 71) ===
import GuestNotifications from './pages/Guest/GuestNotifications';
import GuestProductSearch from './pages/Guest/GuestProductSearch';
import GuestEducation from './pages/Guest/GuestEducation';

/* ── Sidebar Component (Dành cho Farm Manager - BICAP-7 / BICAP-8) ── */
interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
  hasActiveSubscription: boolean;
  user?: UserSession | null;
}

const Sidebar: React.FC<SidebarProps> = ({ currentTab, onTabChange, hasActiveSubscription, user }) => {
  const menuItems = [
    { id: 'dashboard', label: 'Dashboard', icon: '📊', isProtected: false },
    { id: 'profile', label: 'Cập nhật hồ sơ', icon: '👤', isProtected: false },
    { id: 'packages', label: 'Gói Dịch Vụ', icon: '📦', isProtected: false },
    { id: 'farm-info', label: 'Nông Trại Của Tôi', icon: '🌾', isProtected: false },
    { id: 'exports', label: 'Xuất Kho & QR', icon: '🏷️', isProtected: true },
    { id: 'trading-floor', label: 'Sàn Giao Dịch', icon: '🛒', isProtected: true },
    { id: 'orders', label: 'Đơn Hàng', icon: '🧾', isProtected: true },
    { id: 'retailers', label: 'Nhà Bán Lẻ', icon: '🤝', isProtected: true },
    { id: 'products', label: 'Sản Phẩm & QR Code', icon: '🔍', isProtected: true },
    { id: 'iot', label: 'Giám Sát IoT', icon: '🌡️', isProtected: true },
    { id: 'certificates', label: 'Chứng Nhận VietGAP', icon: '📜', isProtected: true },
    // === THÊM MỚI (NV1): Thêm mục menu để test 3 trang Guest trực tiếp trên Sidebar ===
    { id: 'guest-notifications', label: '[Guest] Thông Báo (69)', icon: '🔔', isProtected: false },
    { id: 'guest-search', label: '[Guest] Tìm Kiếm (70)', icon: '🔍', isProtected: false },
    { id: 'guest-education', label: '[Guest] Bài Viết/Video (71)', icon: '📚', isProtected: false },
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
        <div style={{ ...farmBadgeStyle, flexDirection: 'column', gap: '8px', padding: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', width: '100%' }}>
            {/* Account Avatar */}
            <div
              style={{
                width: '34px',
                height: '34px',
                borderRadius: '50%',
                overflow: 'hidden',
                background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                fontWeight: 700,
                fontSize: '14px',
                flexShrink: 0,
              }}
            >
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt="Avatar" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              ) : (
                user?.fullName?.charAt(0)?.toUpperCase() || '👤'
              )}
            </div>

            {/* Account Info */}
            <div style={{ flex: 1, minWidth: 0, overflow: 'hidden' }}>
              <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.fullName || 'Farm Manager'}
              </div>
              <div style={{ fontSize: '10px', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.email || 'farm@bicap.com'}
              </div>
            </div>

            {/* Edit Button next to Account Avatar (BICAP-8) */}
            <button
              onClick={() => onTabChange('profile')}
              title="Cập nhật thông tin cá nhân (BICAP-8)"
              style={{
                background: 'rgba(16, 185, 129, 0.15)',
                border: '1px solid rgba(16, 185, 129, 0.3)',
                color: '#34d399',
                borderRadius: '6px',
                padding: '4px 8px',
                fontSize: '11px',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                transition: 'all 0.2s ease',
                flexShrink: 0,
              }}
            >
              ✏️ Edit
            </button>
          </div>
        </div>
        <p style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '12px' }}>BICAP Platform v0.1</p>
      </div>
    </aside>
  );
};

/* ── Main App Component ── */
export default function App() {
  const traceMatch = window.location.pathname.match(/^\/trace\/([a-zA-Z0-9]+)$/);
  const pathName = window.location.pathname;

  const [authenticated, setAuthenticated] = useState<boolean>(isLoggedIn());
  const [user, setUser] = useState<UserSession | null>(getCurrentUser());
  const [currentTab, setCurrentTab] = useState('packages');
  const [hasActiveSubscription, setHasActiveSubscription] = useState(false);
  const [retailerTab, setRetailerTab] = useState<'dashboard' | 'profile' | 'business'>('dashboard');

  // Xử lý sau khi Đăng nhập thành công từ AuthPage
  const handleLoginSuccess = (token: string, userData: any, refreshToken?: string) => {
    saveSession(token, userData, refreshToken);
    setAuthenticated(true);
    setUser(userData);
    if (userData?.role === 'RETAILER' && sessionStorage.getItem('retailerProfileRequired') === '1') {
      setRetailerTab('profile');
      sessionStorage.removeItem('retailerProfileRequired');
    }
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
        const res = await fetch(`${API_BASE_URL}/subscriptions/my`, {
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

    const resolveFarmId = async () => {
      if (user?.farmId) return;
      try {
        const res = await fetch(`${API_BASE_URL}/farms/my`, { headers: getAuthHeaders() });
        if (res.ok) {
          const farms = await res.json();
          if (Array.isArray(farms) && farms.length > 0 && typeof farms[0]?.id === 'number') {
            const updated = { ...user!, farmId: farms[0].id };
            saveSession(localStorage.getItem('accessToken') || '', updated);
            setUser(updated);
          }
        }
      } catch (e) {
        // Non-fatal
      }
    };

    checkSubscription();
    resolveFarmId();
  }, [authenticated, user?.role, currentTab]);

  // 1. Kiểm tra Route Tra cứu QR
  if (traceMatch) return <TracePage hash={traceMatch[1]} />;

  // 2. === THÊM MỚI (NV1): Cho phép truy cập công khai các đường dẫn Guest trực tiếp qua URL ===
  if (pathName === '/guest/notifications') return <GuestNotifications />;
  if (pathName === '/guest/search') return <GuestProductSearch />;
  if (pathName === '/guest/education') return <GuestEducation />;

  // Render AuthPage nếu chưa đăng nhập
  if (!authenticated) {
    return <AuthPage onLoginSuccess={handleLoginSuccess} />;
  }

  // Render Retailer Portal nếu người dùng là RETAILER (BICAP-36)
  if (user?.role === 'RETAILER') {
    return (
      <div className="retailer-portal">
        <header style={headerStyle}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ ...logoIconStyle, background: 'linear-gradient(135deg, #0284c7 0%, #06b6d4 100%)' }}>R</div>
            <span style={logoTextStyle}>BICAP Retailer</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <span style={{ fontSize: '13px', color: '#cbd5e1' }}>
              Xin chào, <strong>{user.fullName}</strong> <span style={roleBadgeRetailerStyle}>Retailer</span>
            </span>
            <button onClick={handleLogout} style={logoutButtonStyle}>
              🚪 Đăng xuất
            </button>
          </div>
        </header>

        <nav className="retailer-nav" aria-label="Điều hướng hồ sơ Nhà bán lẻ">
          <button className={retailerTab === 'dashboard' ? 'is-active' : ''} onClick={() => setRetailerTab('dashboard')}>Tổng quan</button>
          <button className={retailerTab === 'profile' ? 'is-active' : ''} onClick={() => setRetailerTab('profile')}>Thông tin cá nhân</button>
          <button className={retailerTab === 'business' ? 'is-active' : ''} onClick={() => setRetailerTab('business')}>Giấy phép kinh doanh</button>
        </nav>

        <main className="retailer-main">
          {retailerTab === 'profile' && (
            <RetailerProfilePage
              user={user}
              onUserUpdated={(updated) => {
                const updatedSession = { ...user, ...updated };
                saveSession(localStorage.getItem('accessToken') || '', updatedSession, localStorage.getItem('refreshToken') || undefined);
                setUser(updatedSession);
              }}
            />
          )}
          {retailerTab === 'business' && <RetailerBusinessPage />}
          {retailerTab === 'dashboard' && (
            <div className="glass-panel retailer-panel">
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>🛒</div>
              <h1 className="dashboard-title" style={{ background: 'linear-gradient(to right, #38bdf8, #06b6d4)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                Sàn Giao Dịch Nông Sản Sạch - Nhà Bán Lẻ
              </h1>
              <p style={{ color: 'var(--text-secondary)', fontSize: '15px', marginTop: '8px', lineHeight: 1.6 }}>
                Chào mừng nhà bán lẻ <strong>{user.fullName}</strong> ({user.email}) đã đăng nhập thành công.
              </p>

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
          )}
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
        user={user}
      />

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <header style={{ ...headerStyle, marginLeft: 'var(--sidebar-width)' }}>
          <div style={{ fontSize: '14px', color: '#cbd5e1' }}>
            Cổng Quản Lý Nông Trại <strong style={{ color: '#10b981' }}>(BICAP-7)</strong>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <NotificationBell />
            <span style={{ fontSize: '13px', color: '#cbd5e1' }}>
              Xin chào, <strong>{user?.fullName}</strong> <span style={roleBadgeFarmStyle}>Farm Manager</span>
            </span>
            <button onClick={handleLogout} style={logoutButtonStyle}>
              🚪 Đăng xuất
            </button>
          </div>
        </header>

        <main className="main-content animate-fade-in" style={{ marginTop: '60px' }}>
          {currentTab === 'profile' && <ProfilePage onUserUpdated={(updated: UserSession) => setUser(updated)} />}
          {currentTab === 'packages' && <ServicePackages />}
          {currentTab === 'exports' && <SeasonExports farmId={user?.farmId} />}
          {currentTab === 'trading-floor' && <TradingFloor farmId={user?.farmId} />}
          {currentTab === 'orders' && <Orders />}
          {currentTab === 'retailers' && <Retailers />}

          {/* === THÊM MỚI (NV1): Render 3 màn hình Guest khi bấm từ Sidebar === */}
          {currentTab === 'guest-notifications' && <GuestNotifications />}
          {currentTab === 'guest-search' && <GuestProductSearch />}
          {currentTab === 'guest-education' && <GuestEducation />}

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

          {currentTab === 'iot' && <IotDashboard />}
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
  overflowY: 'auto',
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