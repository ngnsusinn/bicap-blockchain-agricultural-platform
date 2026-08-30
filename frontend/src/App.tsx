import React, { useState, useEffect } from 'react';
import { getAuthHeaders, isLoggedIn, getCurrentUser, saveSession, logout, API_BASE_URL } from './utils/auth';
import type { UserSession } from './utils/auth';
import ServicePackages from './pages/FarmManager/ServicePackages';
import AuthPage from './pages/Auth/AuthPage';
import ProfilePage from './pages/FarmManager/ProfilePage';
import RetailerProfilePage from './pages/Retailer/RetailerProfilePage';
import RetailerBusinessPage from './pages/Retailer/RetailerBusinessPage';
import MarketplacePage from './pages/Retailer/MarketplacePage';
import RetailerOrdersPage from './pages/Retailer/RetailerOrdersPage';
import QrScannerPage from './pages/Retailer/QrScannerPage';
import SeasonExports from './pages/FarmManager/SeasonExports';
import TradingFloor from './pages/FarmManager/TradingFloor';
import Orders from './pages/FarmManager/Orders';
import Retailers from './pages/FarmManager/Retailers';
import Seasons from './pages/FarmManager/Seasons';
import FarmInfo from './pages/FarmManager/FarmInfo';
import MyListings from './pages/FarmManager/MyListings';
import FarmShipments from './pages/FarmManager/FarmShipments';
import Reports from './pages/FarmManager/Reports';
import Certificates from './pages/FarmManager/Certificates';
import Settings from './pages/FarmManager/Settings';
import TracePage from './pages/TracePage';
import NotificationBell from './components/NotificationBell';
import IotDashboard from './pages/FarmManager/IotDashboard';
import GuestEducation from './pages/Guest/GuestEducation';
import GuestProductSearch from './pages/Guest/GuestProductSearch';
import GuestNotifications from './pages/Guest/GuestNotifications';

/* ── Admin Portal redirect ──
 * Tài khoản ADMIN dùng bảng điều khiển trong ứng dụng Admin Web (admin-web).
 * Khi đăng nhập admin từ cổng này, chuyển thẳng sang Admin Web kèm token qua
 * ?token= để Admin Web tự thiết lập phiên và vào thẳng dashboard.
 *   - Chế độ 1 port (Spring Boot phục vụ cả 2 app): '/admin/'
 *   - Chế độ dev riêng (Vite 5174): trỏ sang Vite 5173
 * Override bằng VITE_ADMIN_PORTAL_URL khi deploy tách origin. */
function adminPortalUrl(): string {
  const env = import.meta.env.VITE_ADMIN_PORTAL_URL as string | undefined;
  if (env) return env;
  const { protocol, hostname, port } = window.location;
  if (port === '5174') return `${protocol}//${hostname}:5173/admin/`;
  return '/admin/';
}

function redirectToAdminPortal(token: string) {
  const base = adminPortalUrl();
  const sep = base.includes('?') ? '&' : '?';
  window.location.replace(`${base}${sep}token=${encodeURIComponent(token)}`);
}

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
    { id: 'guest-notifications', label: 'Thông Báo (BICAP-69)', icon: '🔔', isProtected: false },
    { id: 'profile', label: 'Cập nhật hồ sơ', icon: '👤', isProtected: false },
    { id: 'packages', label: 'Gói Dịch Vụ', icon: '📦', isProtected: false },
    { id: 'farm-info', label: 'Nông Trại Của Tôi', icon: '🌾', isProtected: false },
    { id: 'seasons', label: 'Quản Lý Mùa Vụ', icon: '🌱', isProtected: true },
    { id: 'exports', label: 'Xuất Kho & QR', icon: '🏷️', isProtected: true },
    { id: 'trading-floor', label: 'Sàn Giao Dịch', icon: '🛒', isProtected: true },
    { id: 'products', label: 'Sản Phẩm Đã Đăng', icon: '📋', isProtected: true },
    { id: 'orders', label: 'Đơn Hàng', icon: '🧾', isProtected: true },
    { id: 'shipments', label: 'Vận Chuyển', icon: '🚚', isProtected: true },
    { id: 'retailers', label: 'Nhà Bán Lẻ', icon: '🤝', isProtected: true },
    { id: 'iot', label: 'Giám Sát IoT', icon: '🌡️', isProtected: true },
    { id: 'certificates', label: 'Chứng Nhận', icon: '📜', isProtected: false },
    { id: 'reports', label: 'Báo Cáo Cho Admin', icon: '📣', isProtected: false },
    { id: 'guest-education', label: 'Nội Dung Giáo Dục (BICAP-71)', icon: '📚', isProtected: false },
    { id: 'guest-products', label: 'Tìm Kiếm Sản Phẩm (BICAP-70)', icon: '🔍', isProtected: false },
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
  const [authenticated, setAuthenticated] = useState<boolean>(isLoggedIn());
  const [user, setUser] = useState<UserSession | null>(getCurrentUser());
  const [currentTab, setCurrentTab] = useState('guest-notifications');
  const [hasActiveSubscription, setHasActiveSubscription] = useState(false);
  const [retailerTab, setRetailerTab] = useState<'dashboard' | 'marketplace' | 'trace' | 'orders' | 'profile' | 'business'>('dashboard');
  
  // Quản lý chế độ xem khách (Guest) khi chưa đăng nhập
  const [isGuestMode, setIsGuestMode] = useState<boolean>(false);

  // Xử lý sau khi Đăng nhập thành công từ AuthPage
  const handleLoginSuccess = (token: string, userData: any, refreshToken?: string) => {
    saveSession(token, userData, refreshToken);
    // ADMIN → vào thẳng Dashboard Admin (Admin Web), không ở lại cổng Farm/Retail.
    if (userData?.role === 'ADMIN') {
      redirectToAdminPortal(token);
      return;
    }
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
    setIsGuestMode(false);
  };

  useEffect(() => {
    // Session ADMIN sẵn có (F5 trên trang chủ) → đưa thẳng về Dashboard Admin.
    if (authenticated && user?.role === 'ADMIN') {
      redirectToAdminPortal(localStorage.getItem('accessToken') || '');
    }
  }, [authenticated, user?.role]);

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated, user?.role, currentTab]);

  if (traceMatch) return <TracePage hash={traceMatch[1]} />;

  // 1. Nếu chưa đăng nhập nhưng bấm "Xem thông báo chung (Guest)"
  if (!authenticated && isGuestMode) {
    return (
      <div style={{ minHeight: '100vh', background: '#0b0f17', padding: '24px' }}>
        <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', maxWidth: '900px', margin: '0 auto 24px auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={logoIconStyle}>B</div>
            <span style={logoTextStyle}>BICAP Platform (Guest)</span>
          </div>
          <button 
            onClick={() => setIsGuestMode(false)}
            style={{ 
              background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)', 
              border: 'none', 
              color: '#fff', 
              padding: '8px 16px', 
              borderRadius: '8px', 
              fontWeight: 600, 
              cursor: 'pointer' 
            }}
          >
            🔐 Đăng nhập hệ thống
          </button>
        </header>

        <GuestNotifications />
      </div>
    );
  }

  // 2. Render AuthPage nếu chưa đăng nhập (kèm nút xem Guest ở góc phải)
  if (!authenticated) {
    return (
      <div style={{ position: 'relative' }}>
        <div style={{ position: 'fixed', top: '20px', right: '20px', zIndex: 1000 }}>
          <button
            onClick={() => setIsGuestMode(true)}
            style={{
              background: 'rgba(16, 185, 129, 0.2)',
              border: '1px solid #10b981',
              color: '#34d399',
              padding: '8px 16px',
              borderRadius: '20px',
              cursor: 'pointer',
              fontWeight: 700,
              fontSize: '13px',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
            }}
          >
            🔔 Xem thông báo chung (Guest)
          </button>
        </div>
        <AuthPage onLoginSuccess={handleLoginSuccess} />
      </div>
    );
  }

  // 3. Phiên ADMIN không có giao diện ở cổng này — tự chuyển sang Dashboard Admin (Admin Web).
  if (user?.role === 'ADMIN') {
    return null;
  }

  // 4. Render Retailer Portal nếu người dùng là RETAILER (BICAP-36)
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
          <button className={retailerTab === 'marketplace' ? 'is-active' : ''} onClick={() => setRetailerTab('marketplace')}>Sàn nông sản</button>
          <button className={retailerTab === 'trace' ? 'is-active' : ''} onClick={() => setRetailerTab('trace')}>Quét QR</button>
          <button className={retailerTab === 'orders' ? 'is-active' : ''} onClick={() => setRetailerTab('orders')}>Đơn mua</button>
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
          {retailerTab === 'marketplace' && <MarketplacePage />}
          {retailerTab === 'trace' && <QrScannerPage />}
          {retailerTab === 'orders' && <RetailerOrdersPage />}
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

  // 4. Render Farm Manager Portal (BICAP-7)
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
          {currentTab === 'guest-notifications' && <GuestNotifications />}
          {currentTab === 'profile' && <ProfilePage onUserUpdated={(updated: UserSession) => setUser(updated)} />}
          {currentTab === 'packages' && <ServicePackages />}
          {currentTab === 'farm-info' && <FarmInfo farmId={user?.farmId} />}
          {currentTab === 'seasons' && <Seasons farmId={user?.farmId} />}
          {currentTab === 'exports' && <SeasonExports farmId={user?.farmId} />}
          {currentTab === 'trading-floor' && <TradingFloor farmId={user?.farmId} />}
          {currentTab === 'products' && <MyListings farmId={user?.farmId} />}
          {currentTab === 'orders' && <Orders />}
          {currentTab === 'shipments' && <FarmShipments farmId={user?.farmId} />}
          {currentTab === 'retailers' && <Retailers />}
          {currentTab === 'certificates' && <Certificates farmId={user?.farmId} />}
          {currentTab === 'reports' && <Reports />}
          {currentTab === 'settings' && <Settings />}
          {currentTab === 'guest-education' && <GuestEducation />}
          {currentTab === 'guest-products' && <GuestProductSearch />}

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
