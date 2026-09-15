import React, { useState, useEffect } from 'react';
import { getAuthHeaders, isLoggedIn, getCurrentUser, saveSession, logout, API_BASE_URL, isAdminRole } from './utils/auth';
import type { UserSession } from './utils/auth';
import ServicePackages from './pages/FarmManager/ServicePackages';
import AuthPage from './pages/Auth/AuthPage';
import ProfilePage from './pages/FarmManager/ProfilePage';
import RetailerProfilePage from './pages/Retailer/RetailerProfilePage';
import RetailerBusinessPage from './pages/Retailer/RetailerBusinessPage';
import MarketplacePage from './pages/Retailer/MarketplacePage';
import RetailerOrdersPage from './pages/Retailer/RetailerOrdersPage';
import QrScannerPage from './pages/Retailer/QrScannerPage';
import RetailerNotificationsPage from './pages/Retailer/RetailerNotificationsPage';
import RetailerShipmentsPage from './pages/Retailer/RetailerShipmentsPage';
import RetailerReportsPage from './pages/Retailer/RetailerReportsPage';
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
import PortalSidebar from './components/PortalSidebar';
import type { PortalNavItem } from './components/PortalSidebar';
import useHashTab from './utils/useHashTab';
import IotDashboard from './pages/FarmManager/IotDashboard';
import GuestEducation from './pages/Guest/GuestEducation';
import GuestProductSearch from './pages/Guest/GuestProductSearch';
import GuestNotifications from './pages/Guest/GuestNotifications';
import GuestArea from './pages/Guest/GuestArea';
import DriverShipmentsPage from './pages/Driver/DriverShipmentsPage';
// Shipping Manager pages (BICAP-54 → BICAP-62)
import CompletedOrdersPage from './pages/Shipping/CompletedOrdersPage';
import ShipmentsPage from './pages/Shipping/ShipmentsPage';
import TrackingPage from './pages/Shipping/TrackingPage';
import VehiclesPage from './pages/Shipping/VehiclesPage';
import DriversPage from './pages/Shipping/DriversPage';
import ShippingReportsPage from './pages/Shipping/ShippingReportsPage';
import ShippingNotificationsPage from './pages/Shipping/ShippingNotificationsPage';

/* ── Admin redirect ──
 * Tài khoản quản trị dùng bảng điều khiển ở endpoint /admin của CÙNG app.
 * Phiên đăng nhập đã được chia sẻ (src/shared/session.ts) nên không cần
 * chuyển token qua URL như thời còn 2 app tách biệt. */
function redirectToAdminPortal() {
  window.location.replace('/admin');
}

/* ── Menu điều hướng các portal (dùng chung PortalSidebar) ──
 * Tab đang xem được lưu vào hash URL (`#farm/<tab>`, `#retailer/<tab>`) nên khi
 * F5 / tải lại trang người dùng vẫn ở đúng màn hình trước đó. */

const FARM_MENU: PortalNavItem[] = [
  { id: 'dashboard', label: 'Dashboard', icon: '📊' },
  { id: 'guest-notifications', label: 'Thông Báo', icon: '🔔' },
  { id: 'profile', label: 'Cập nhật hồ sơ', icon: '👤' },
  { id: 'packages', label: 'Gói Dịch Vụ', icon: '📦' },
  { id: 'farm-info', label: 'Nông Trại Của Tôi', icon: '🌾' },
  { id: 'seasons', label: 'Quản Lý Mùa Vụ', icon: '🌱', isProtected: true },
  { id: 'exports', label: 'Xuất Kho & QR', icon: '🏷️', isProtected: true },
  { id: 'trading-floor', label: 'Sàn Giao Dịch', icon: '🛒', isProtected: true },
  { id: 'products', label: 'Sản Phẩm Đã Đăng', icon: '📋', isProtected: true },
  { id: 'orders', label: 'Đơn Hàng', icon: '🧾', isProtected: true },
  { id: 'shipments', label: 'Vận Chuyển', icon: '🚚', isProtected: true },
  { id: 'retailers', label: 'Nhà Bán Lẻ', icon: '🤝', isProtected: true },
  { id: 'iot', label: 'Giám Sát IoT', icon: '🌡️', isProtected: true },
  { id: 'certificates', label: 'Chứng Nhận', icon: '📜', isProtected: true },
  { id: 'reports', label: 'Báo Cáo Cho Admin', icon: '📣', isProtected: true },
  { id: 'guest-education', label: 'Nội Dung Giáo Dục', icon: '📚' },
  { id: 'guest-products', label: 'Tìm Kiếm Sản Phẩm', icon: '🔍' },
  { id: 'settings', label: 'Cài Đặt', icon: '⚙️' },
];

const RETAILER_MENU: PortalNavItem[] = [
  { id: 'dashboard', label: 'Tổng quan', icon: '📊' },
  { id: 'marketplace', label: 'Sàn nông sản', icon: '🛒' },
  { id: 'trace', label: 'Quét QR', icon: '📷' },
  { id: 'orders', label: 'Đơn mua', icon: '🧾' },
  { id: 'shipments', label: 'Vận chuyển', icon: '🚚' },
  { id: 'notifications', label: 'Thông báo', icon: '🔔' },
  { id: 'reports', label: 'Báo cáo', icon: '📣' },
  { id: 'profile', label: 'Thông tin cá nhân', icon: '👤' },
  { id: 'business', label: 'Giấy phép kinh doanh', icon: '📜' },
];

/** Danh sách id tab hợp lệ (hằng số ở module → identity ổn định cho useHashTab). */
const FARM_TABS = FARM_MENU.map((item) => item.id);
const RETAILER_TABS = RETAILER_MENU.map((item) => item.id);

/* ── Shipping Manager Portal (BICAP-54 → BICAP-62) ── */
type ShippingTab = 'orders' | 'shipments' | 'tracking' | 'vehicles' | 'drivers' | 'reports' | 'notifications';

interface ShippingPortalProps {
  user: UserSession;
  onLogout: () => void;
}

type CompletedOrderForCreate = { id: number; status: string; productName?: string; retailerName?: string; deliveryAddr?: string; quantity?: number; price?: number; totalAmount?: number };
type ShipmentForTracking = { id: number; status: string; orderId?: number; driverName?: string };

const ShippingManagerPortal: React.FC<ShippingPortalProps> = ({ user, onLogout }) => {
  const [tab, setTab] = useState<ShippingTab>('orders');
  const [orderForCreate, setOrderForCreate] = useState<CompletedOrderForCreate | null>(null);
  // F9: "Xem tracking" phải mở đúng lô vận chuyển vừa chọn, không mở trang trống.
  const [trackingShipmentId, setTrackingShipmentId] = useState<number | null>(null);

  const handleCreateShipment = (order: CompletedOrderForCreate) => {
    setOrderForCreate(order);
    setTab('shipments');
  };

  const handleTrack = (shipment: ShipmentForTracking) => {
    setTrackingShipmentId(shipment.id);
    setTab('tracking');
  };

  const navItems: { id: ShippingTab; label: string; icon: string }[] = [
    { id: 'orders',    label: 'Đơn hàng chờ',     icon: '📋' },
    { id: 'shipments', label: 'Lô vận chuyển',     icon: '🚚' },
    { id: 'tracking',  label: 'Quy trình',          icon: '🗺️' },
    { id: 'vehicles',  label: 'Phương tiện',        icon: '🚛' },
    { id: 'drivers',   label: 'Tài xế',             icon: '🧑‍💼' },
    { id: 'reports',   label: 'Báo cáo',            icon: '📣' },
    { id: 'notifications', label: 'Thông báo',      icon: '🔔' },
  ];

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside style={{
        width: 'var(--sidebar-width)',
        position: 'fixed', top: 0, left: 0, bottom: 0,
        background: 'rgba(15, 16, 22, 0.95)',
        borderRight: '1px solid var(--border-color)',
        display: 'flex', flexDirection: 'column',
        zIndex: 1000, padding: '24px 16px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '40px', paddingLeft: '8px' }}>
          <div style={{
            width: '36px', height: '36px', borderRadius: '10px',
            background: 'linear-gradient(135deg, #0284c7 0%, #7c3aed 100%)',
            color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 800, fontSize: '18px', boxShadow: '0 4px 12px rgba(2, 132, 199, 0.4)',
          }}>S</div>
          <span style={{
            fontSize: '16px', fontWeight: 800, letterSpacing: '-0.5px',
            background: 'linear-gradient(to right, #fff, #7c3aed)',
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          }}>BICAP Shipping</span>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1, minHeight: 0, overflowY: 'auto' }}>
          {navItems.map(item => (
            <button
              key={item.id}
              onClick={() => { setTab(item.id); if (item.id !== 'shipments') setOrderForCreate(null); }}
              style={{
                width: '100%', padding: '12px 16px',
                borderRadius: '0 8px 8px 0',
                display: 'flex', alignItems: 'center', gap: '12px',
                textAlign: 'left', border: 0, cursor: 'pointer', fontSize: '14px',
                color: tab === item.id ? '#fff' : 'var(--text-secondary)',
                background: tab === item.id ? 'rgba(2, 132, 199, 0.15)' : 'transparent',
                borderLeft: tab === item.id ? '3px solid #0284c7' : '3px solid transparent',
                transition: 'all 0.2s ease',
              }}
            >
              <span style={{ fontSize: '18px' }}>{item.icon}</span>
              <span style={{ fontWeight: tab === item.id ? 600 : 400 }}>{item.label}</span>
            </button>
          ))}
        </nav>

        <div style={{ marginTop: 'auto', paddingLeft: '8px' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 12px',
            background: 'rgba(255,255,255,.03)', border: '1px solid rgba(255,255,255,.06)',
            borderRadius: '10px', marginBottom: '12px',
          }}>
            <div style={{
              width: '32px', height: '32px', borderRadius: '50%', flexShrink: 0,
              background: 'linear-gradient(135deg, #0284c7 0%, #7c3aed 100%)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#fff', fontWeight: 700, fontSize: '13px',
            }}>
              {user.fullName?.charAt(0)?.toUpperCase() || 'S'}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {user.fullName}
              </div>
              <div style={{ fontSize: '10px', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {user.email}
              </div>
            </div>
          </div>
          <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>BICAP Platform v0.1</p>
        </div>
      </aside>

      {/* Main area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <header style={{
          height: '64px',
          background: 'rgba(15, 23, 42, 0.9)',
          backdropFilter: 'blur(12px)',
          borderBottom: '1px solid rgba(255,255,255,0.08)',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '0 32px',
          marginLeft: 'var(--sidebar-width)',
        }}>
          <div style={{ fontSize: '14px', color: '#cbd5e1' }}>
            Cổng Quản Lý Vận Chuyển
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <NotificationBell />
            <span style={{ fontSize: '13px', color: '#cbd5e1' }}>
              Xin chào, <strong>{user.fullName}</strong>{' '}
              <span style={{
                fontSize: '11px', background: 'rgba(2, 132, 199, 0.2)',
                color: '#38bdf8', padding: '2px 8px', borderRadius: '12px',
                border: '1px solid rgba(2, 132, 199, 0.3)', marginLeft: '6px',
              }}>Shipping Mgr</span>
            </span>
            <button onClick={onLogout} style={{
              background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.2)',
              color: '#f87171', padding: '6px 14px', borderRadius: '8px',
              fontSize: '12px', fontWeight: 600, cursor: 'pointer',
            }}>
              🚪 Đăng xuất
            </button>
          </div>
        </header>

        <main className="main-content animate-fade-in" style={{ marginTop: '60px' }}>
          {tab === 'orders' && (
            <CompletedOrdersPage onCreateShipment={handleCreateShipment} />
          )}
          {tab === 'shipments' && (
            <ShipmentsPage
              initialOrderForCreate={orderForCreate}
              onTrack={handleTrack}
            />
          )}
          {tab === 'tracking' && <TrackingPage initialShipmentId={trackingShipmentId ?? undefined} />}
          {tab === 'vehicles' && <VehiclesPage />}
          {tab === 'drivers' && <DriversPage />}
          {tab === 'reports' && <ShippingReportsPage />}
          {tab === 'notifications' && <ShippingNotificationsPage />}
        </main>
      </div>
    </div>
  );
};

/* ── Main App Component ── */
export default function App() {
  const traceMatch = window.location.pathname.match(/^\/trace\/([a-zA-Z0-9]+)$/);
  const [authenticated, setAuthenticated] = useState<boolean>(isLoggedIn());
  const [user, setUser] = useState<UserSession | null>(getCurrentUser());
  const [currentTab, setCurrentTab] = useHashTab(FARM_TABS, 'guest-notifications', 'farm');
  const [hasActiveSubscription, setHasActiveSubscription] = useState(false);
  // Đã kiểm tra xong gói dịch vụ hay chưa: chỉ chặn tab VIP SAU khi biết kết quả,
  // nếu không thì việc F5 trên một tab VIP sẽ bị đẩy về "Gói Dịch Vụ" oan.
  const [subscriptionChecked, setSubscriptionChecked] = useState(false);
  // Tab Retailer cũng lưu vào hash để reload không mất màn hình đang xem.
  const [retailerTab, setRetailerTab] = useHashTab(RETAILER_TABS, 'dashboard', 'retailer');
  
  // Quản lý chế độ xem khách (Guest) khi chưa đăng nhập
  const [isGuestMode, setIsGuestMode] = useState<boolean>(false);

  // Xử lý sau khi Đăng nhập thành công từ AuthPage
  const handleLoginSuccess = (token: string, userData: any, refreshToken?: string) => {
    saveSession(token, userData, refreshToken);
    // Tài khoản quản trị → chuyển sang endpoint /admin của cùng app.
    if (isAdminRole(userData?.role)) {
      redirectToAdminPortal();
      return;
    }
    setAuthenticated(true);
    setUser(userData);
  };

  // Xử lý Đăng xuất
  const handleLogout = () => {
    logout();
    setAuthenticated(false);
    setUser(null);
    setIsGuestMode(false);
  };

  useEffect(() => {
    // Session quản trị sẵn có (F5 trên trang chủ) → đưa thẳng về /admin.
    if (authenticated && isAdminRole(user?.role)) {
      redirectToAdminPortal();
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
      } finally {
        setSubscriptionChecked(true);
      }
    };

    const resolveFarmId = async () => {
      if (user?.farmId) return;
      try {
        const [farmsRes, subsRes] = await Promise.all([
          fetch(`${API_BASE_URL}/farms/my`, { headers: getAuthHeaders() }),
          fetch(`${API_BASE_URL}/subscriptions/my`, { headers: getAuthHeaders() }),
        ]);
        const farms = farmsRes.ok ? await farmsRes.json() : null;
        if (!Array.isArray(farms) || farms.length === 0) return;

        const subs = subsRes.ok ? await subsRes.json() : null;
        const activeSub = Array.isArray(subs)
          ? subs.find((s: any) => s.status === 'ACTIVE')
          : (subs?.status === 'ACTIVE' ? subs : null);

        // Một tài khoản có thể sở hữu nhiều nông trại (có cái còn PENDING/bị từ chối).
        // Tab VIP gắn với gói dịch vụ đang ACTIVE, còn mùa vụ/xuất kho chỉ chạy được ở
        // nông trại đã duyệt — nên chọn theo thứ tự: nông trại của gói ACTIVE →
        // nông trại APPROVED đầu tiên → phần tử đầu tiên. Trước đây luôn lấy farms[0]
        // nên tài khoản có nông trại PENDING đứng trước bị chặn ở mọi tab nghiệp vụ.
        const chosen =
          farms.find((f: any) => activeSub?.farmId != null && f.id === activeSub.farmId) ||
          farms.find((f: any) => f.status === 'APPROVED') ||
          farms[0];
        if (typeof chosen?.id !== 'number') return;

        const updated = { ...user!, farmId: chosen.id };
        saveSession(localStorage.getItem('accessToken') || '', updated);
        setUser(updated);
      } catch (e) {
        // Non-fatal
      }
    };

    checkSubscription();
    resolveFarmId();
    window.addEventListener('bicap-subscription-changed', checkSubscription);
    return () => window.removeEventListener('bicap-subscription-changed', checkSubscription);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated, user?.role, currentTab]);

  useEffect(() => {
    // Chờ biết chắc tình trạng gói dịch vụ rồi mới chặn tab VIP (tránh đẩy người
    // dùng khỏi tab họ đang xem khi vừa F5 / mở link có hash).
    if (!subscriptionChecked) return;
    const protectedTabs = new Set([
      'seasons', 'exports', 'trading-floor', 'products', 'orders', 'shipments',
      'retailers', 'iot', 'certificates', 'reports',
    ]);
    if (!hasActiveSubscription && protectedTabs.has(currentTab)) {
      setCurrentTab('packages');
    }
  }, [hasActiveSubscription, currentTab, subscriptionChecked, setCurrentTab]);

  if (traceMatch) return <TracePage hash={traceMatch[1]} />;

  // 1. Nếu chưa đăng nhập nhưng bấm "Xem thông báo chung (Guest)"
  if (!authenticated && isGuestMode) {
    return <GuestArea onLogin={() => setIsGuestMode(false)} />;
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

  // 3. Phiên quản trị không có giao diện ở cổng này — chuyển sang /admin.
  if (isAdminRole(user?.role)) {
    return null;
  }

  // 4. Render Retailer Portal nếu người dùng là RETAILER (BICAP-36)
  //    Dùng đúng bố cục của các portal còn lại: sidebar dọc + header + main-content.
  if (user?.role === 'RETAILER') {
    return (
      <div className="app-container">
        <PortalSidebar
          brandLabel="BICAP Retailer"
          brandIcon="R"
          accent="#06b6d4"
          accentSoft="#22d3ee"
          gradient="linear-gradient(135deg, #06b6d4 0%, #0284c7 100%)"
          menuItems={RETAILER_MENU}
          currentTab={retailerTab}
          onTabChange={setRetailerTab}
          user={user}
          onEditProfile={() => setRetailerTab('profile')}
        />

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <header style={{ ...headerStyle, marginLeft: 'var(--sidebar-width)' }}>
            <div style={{ fontSize: '14px', color: '#cbd5e1' }}>
              Cổng Nhà Bán Lẻ
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <NotificationBell />
              <span style={{ fontSize: '13px', color: '#cbd5e1' }}>
                Xin chào, <strong>{user.fullName}</strong> <span style={roleBadgeRetailerStyle}>Retailer</span>
              </span>
              <button onClick={handleLogout} style={logoutButtonStyle}>
                🚪 Đăng xuất
              </button>
            </div>
          </header>

          <main className="main-content animate-fade-in" style={{ marginTop: '60px' }}>
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
            {retailerTab === 'notifications' && <RetailerNotificationsPage />}
            {retailerTab === 'shipments' && <RetailerShipmentsPage />}
            {retailerTab === 'reports' && <RetailerReportsPage />}
            {retailerTab === 'dashboard' && (
              <div>
                <h1 className="dashboard-title">Sàn Giao Dịch Nông Sản Sạch</h1>
                <p className="dashboard-subtitle">
                  Chào mừng nhà bán lẻ <strong>{user.fullName}</strong> ({user.email}) đã đăng nhập thành công.
                </p>

                <div className="glass-panel" style={{ padding: '32px' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px' }}>
                    <button type="button" onClick={() => setRetailerTab('marketplace')} style={{ ...statCardStyle, cursor: 'pointer', textAlign: 'left', font: 'inherit' }}>
                      <div style={{ fontSize: '24px' }}>🔍</div>
                      <h3 style={{ fontSize: '16px', color: '#fff', margin: '8px 0 4px 0' }}>Tìm kiếm Nông sản</h3>
                      <p style={{ fontSize: '12px', color: '#94a3b8' }}>Duyệt danh mục sản phẩm đạt chứng nhận VietGAP/GlobalGAP.</p>
                    </button>

                    <button type="button" onClick={() => setRetailerTab('orders')} style={{ ...statCardStyle, cursor: 'pointer', textAlign: 'left', font: 'inherit' }}>
                      <div style={{ fontSize: '24px' }}>📦</div>
                      <h3 style={{ fontSize: '16px', color: '#fff', margin: '8px 0 4px 0' }}>Đơn hàng của tôi</h3>
                      <p style={{ fontSize: '12px', color: '#94a3b8' }}>Theo dõi đơn, xác nhận nhận hàng và tải ảnh giao nhận.</p>
                    </button>

                    <button type="button" onClick={() => setRetailerTab('shipments')} style={{ ...statCardStyle, cursor: 'pointer', textAlign: 'left', font: 'inherit' }}>
                      <div style={{ fontSize: '24px' }}>🚚</div>
                      <h3 style={{ fontSize: '16px', color: '#fff', margin: '8px 0 4px 0' }}>Theo dõi Vận chuyển</h3>
                      <p style={{ fontSize: '12px', color: '#94a3b8' }}>Tracking thời gian thực tiến trình giao nhận lô hàng.</p>
                    </button>

                    <button type="button" onClick={() => setRetailerTab('notifications')} style={{ ...statCardStyle, cursor: 'pointer', textAlign: 'left', font: 'inherit' }}>
                      <div style={{ fontSize: '24px' }}>🔔</div>
                      <h3 style={{ fontSize: '16px', color: '#fff', margin: '8px 0 4px 0' }}>Thông báo</h3>
                      <p style={{ fontSize: '12px', color: '#94a3b8' }}>Cập nhật từ trang trại và người vận chuyển.</p>
                    </button>

                    <button type="button" onClick={() => setRetailerTab('reports')} style={{ ...statCardStyle, cursor: 'pointer', textAlign: 'left', font: 'inherit' }}>
                      <div style={{ fontSize: '24px' }}>📣</div>
                      <h3 style={{ fontSize: '16px', color: '#fff', margin: '8px 0 4px 0' }}>Báo cáo Admin</h3>
                      <p style={{ fontSize: '12px', color: '#94a3b8' }}>Gửi khiếu nại, phản hồi hoặc báo cáo sự cố.</p>
                    </button>
                  </div>
                </div>
              </div>
            )}
          </main>
        </div>
      </div>
    );
  }

  // 4. Render Shipping Manager Portal (BICAP-54 → BICAP-62)
  if (user?.role === 'SHIPPING_MGR') {
    return <ShippingManagerPortal user={user} onLogout={handleLogout} />;
  }

  // 4b. Render Driver Portal (F11 — BICAP-76): tài xế quét QR lấy hàng & cập nhật tracking.
  if (user?.role === 'SHIP_DRIVER') {
    return <DriverShipmentsPage onLogout={handleLogout} />;
  }

  // 5. Render Farm Manager Portal (BICAP-7)
  return (
    <div className="app-container">
      <PortalSidebar
        brandLabel="BICAP Farm"
        brandIcon="B"
        accent="#10b981"
        accentSoft="#34d399"
        gradient="linear-gradient(135deg, #10b981 0%, #06b6d4 100%)"
        menuItems={FARM_MENU}
        currentTab={currentTab}
        onTabChange={setCurrentTab}
        hasActiveSubscription={hasActiveSubscription}
        user={user}
        onEditProfile={() => setCurrentTab('profile')}
      />

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <header style={{ ...headerStyle, marginLeft: 'var(--sidebar-width)' }}>
          <div style={{ fontSize: '14px', color: '#cbd5e1' }}>
            Cổng Quản Lý Nông Trại <strong style={{ color: '#10b981' }}></strong>
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

/* ── Component Styles ──
 * Sidebar/nav đã chuyển sang `components/PortalSidebar` để Farm và Retailer
 * dùng chung một bố cục. */
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
