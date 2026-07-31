import { useState, useEffect, useCallback } from 'react';
import { Sidebar } from './components/Sidebar';
import type { UserSession, PortalType } from './types';
import { LoginPage } from './components/LoginPage';
import { StatsCards } from './components/StatsCards';
import { AdminTable } from './components/AdminTable';
import { AdminModal } from './components/AdminModal';
import { Toast } from './components/Toast';
import type { ToastMessage } from './components/Toast';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/admins';

// ── Helpers ──
function getPortalFromPath(path: string): PortalType | null {
  if (path.startsWith('/admin')) return 'admin';
  if (path.startsWith('/farm')) return 'farm';
  if (path.startsWith('/retail')) return 'retail';
  return null;
}

function isRoleAllowedForPortal(role: UserSession['role'], portal: PortalType): boolean {
  switch (portal) {
    case 'admin': return ['SUPER_ADMIN', 'ADMIN', 'MODERATOR'].includes(role);
    case 'farm': return role === 'FARM_MANAGER';
    case 'retail': return role === 'RETAILER';
    default: return false;
  }
}

export default function App() {
  // ── Navigation & URL Routing ──
  const [currentPath, setCurrentPath] = useState<string>(() => window.location.pathname);
  const [currentTab, setCurrentTab] = useState('overview');

  useEffect(() => {
    const handlePopState = () => setCurrentPath(window.location.pathname);
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigateTo = (path: string) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
  };

  // ── Auth & Session ──
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => !!localStorage.getItem('bicap_session'));
  const [currentSession, setCurrentSession] = useState<UserSession | null>(() => {
    const saved = localStorage.getItem('bicap_session');
    if (saved) { try { return JSON.parse(saved); } catch { return null; } }
    return null;
  });

  // ── Admin Table State ──
  const [admins, setAdmins] = useState<any[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [showModal, setShowModal] = useState(false);
  const [selectedAdmin, setSelectedAdmin] = useState<any | null>(null);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // ── Toast Helper ──
  const showToast = useCallback((text: string, type: ToastMessage['type'] = 'info') => {
    const id = Date.now().toString();
    setToasts((prev) => [...prev, { id, text, type }]);
  }, []);
  const handleCloseToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  // ── Login Handler (PRODUCTION — no mock tokens) ──
  const handleLoginSuccess = (session: UserSession) => {
    setCurrentSession(session);
    setIsAuthenticated(true);
    localStorage.setItem('bicap_session', JSON.stringify(session));
    localStorage.setItem('bicap_token', session.accessToken);
    showToast(`Xin chào, ${session.fullName}!`, 'success');
  };

  // ── Logout Handler ──
  const handleLogout = () => {
    setIsAuthenticated(false);
    setCurrentSession(null);
    localStorage.removeItem('bicap_session');
    localStorage.removeItem('bicap_token');
    showToast('Đã đăng xuất khỏi hệ thống.', 'info');
  };

  // ── Admin CRUD API calls ──
  const fetchAdmins = useCallback(async () => {
    if (!currentSession) return;
    try {
      const params = new URLSearchParams({ search: searchTerm, status: statusFilter, role: roleFilter, page: currentPage.toString(), size: '5' });
      const token = localStorage.getItem('bicap_token');
      const headers: Record<string, string> = { 'X-Actor-Email': currentSession.email };
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const response = await fetch(`${API_BASE_URL}?${params}`, { headers });
      if (!response.ok) {
        if (response.status === 403) throw new Error('Access Denied (HTTP 403): Bạn không có quyền xem danh sách quản trị viên.');
        const errorData = await response.json();
        throw new Error(errorData.message || 'Lỗi tải danh sách quản trị viên.');
      }
      const data = await response.json();
      setAdmins(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err: any) {
      setAdmins([]); setTotalPages(1);
      showToast(err.message, 'error');
    }
  }, [searchTerm, statusFilter, roleFilter, currentPage, currentSession, showToast]);

  useEffect(() => {
    if (isAuthenticated && currentTab === 'admins' && currentPath.startsWith('/admin')) fetchAdmins();
  }, [fetchAdmins, currentTab, isAuthenticated, currentPath]);

  const handleEditClick = (admin: any) => { setSelectedAdmin(admin); setShowModal(true); };

  const handleDeleteClick = async (id: number) => {
    if (!currentSession) return;
    if (!window.confirm('Bạn có chắc muốn xoá tài khoản quản trị này?')) return;
    try {
      const token = localStorage.getItem('bicap_token');
      const headers: Record<string, string> = { 'X-Actor-Email': currentSession.email };
      if (token) headers['Authorization'] = `Bearer ${token}`;
      const response = await fetch(`${API_BASE_URL}/${id}`, { method: 'DELETE', headers });
      if (!response.ok) { const errorData = await response.json(); throw new Error(errorData.message || 'Xoá thất bại.'); }
      showToast('Đã xoá tài khoản quản trị viên.', 'success');
      fetchAdmins();
    } catch (err: any) { showToast(err.message, 'error'); }
  };

  const handleSaveAdmin = async (adminData: any) => {
    if (!currentSession) return;
    try {
      const isEdit = !!selectedAdmin;
      const url = isEdit ? `${API_BASE_URL}/${selectedAdmin.id}` : API_BASE_URL;
      const method = isEdit ? 'PUT' : 'POST';
      const token = localStorage.getItem('bicap_token');
      const headers: Record<string, string> = { 'Content-Type': 'application/json', 'X-Actor-Email': currentSession.email };
      if (token) headers['Authorization'] = `Bearer ${token}`;
      const response = await fetch(url, { method, headers, body: JSON.stringify(adminData) });
      if (!response.ok) { const errorData = await response.json(); throw new Error(errorData.message || 'Thao tác thất bại.'); }
      showToast(isEdit ? 'Cập nhật thành công.' : 'Tạo tài khoản mới thành công.', 'success');
      setShowModal(false); setSelectedAdmin(null); fetchAdmins();
    } catch (err: any) { showToast(err.message, 'error'); }
  };

  // ── Determine current portal ──
  const currentPortal = getPortalFromPath(currentPath);

  // ────────────────────────────────────────────────────
  // RENDER: Portal Routes (/admin, /farm, /retail)
  // ────────────────────────────────────────────────────
  if (currentPortal) {
    const isAllowed = isAuthenticated && currentSession && isRoleAllowedForPortal(currentSession.role, currentPortal);

    // Not logged in or wrong role → show portal-specific login
    if (!isAllowed) {
      return (
        <>
          <LoginPage portalType={currentPortal} onLoginSuccess={handleLoginSuccess} onNavigate={navigateTo} />
          <Toast toasts={toasts} onClose={handleCloseToast} />
        </>
      );
    }

    // ── Logged in → Show Dashboard ──
    return (
      <div className="app-container">
        <Sidebar
          portalType={currentPortal}
          currentTab={currentTab}
          onTabChange={setCurrentTab}
          currentSession={currentSession!}
          onLogout={handleLogout}
        />

        <main className="main-content animate-fade-in">
          {/* ── ADMIN DASHBOARD ── */}
          {currentPortal === 'admin' && currentTab === 'overview' && (
            <div>
              <h1 className="dashboard-title">Dashboard Overview</h1>
              <p className="dashboard-subtitle">BICAP — Blockchain Agricultural Platform Administrator Portal</p>
              <div className="glass-panel" style={{ padding: '32px', textAlign: 'center', background: 'rgba(22,23,33,0.4)' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌱</div>
                <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Chào mừng đến Bảng điều khiển Admin!</h2>
                <p style={{ color: 'var(--text-secondary)', marginTop: '8px', maxWidth: '600px', marginInline: 'auto', fontSize: '14px', lineHeight: '1.6' }}>
                  Dashboard quản trị IoT, chứng nhận nông sản, hợp đồng thông minh VeChainThor, và phân quyền RBAC.
                </p>
                <button onClick={() => setCurrentTab('admins')} className="btn btn-primary" style={{ marginTop: '24px' }}>
                  Quản lý Admin →
                </button>
              </div>
            </div>
          )}

          {currentPortal === 'admin' && currentTab === 'admins' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
                <div>
                  <h1 className="dashboard-title">Quản Lý Quản Trị Viên</h1>
                  <p className="dashboard-subtitle">CRUD tài khoản, kiểm tra trạng thái, quản lý phân quyền RBAC.</p>
                </div>
                <button
                  onClick={() => { setSelectedAdmin(null); setShowModal(true); }}
                  disabled={currentSession!.role !== 'SUPER_ADMIN'}
                  className="btn btn-primary" style={{ marginTop: '8px' }}
                  title={currentSession!.role !== 'SUPER_ADMIN' ? 'Chỉ SUPER_ADMIN mới có quyền tạo' : 'Tạo quản trị viên mới'}
                >
                  ➕ Tạo Admin mới
                </button>
              </div>
              <StatsCards admins={admins} />
              <AdminTable
                admins={admins} currentSession={currentSession!}
                searchTerm={searchTerm} onSearchChange={(v) => { setSearchTerm(v); setCurrentPage(0); }}
                statusFilter={statusFilter} onStatusFilterChange={(v) => { setStatusFilter(v); setCurrentPage(0); }}
                roleFilter={roleFilter} onRoleFilterChange={(v) => { setRoleFilter(v); setCurrentPage(0); }}
                onEdit={handleEditClick} onDelete={handleDeleteClick}
                currentPage={currentPage} onPageChange={setCurrentPage} totalPages={totalPages}
              />
            </div>
          )}

          {/* ── FARM DASHBOARD ── */}
          {currentPortal === 'farm' && currentTab === 'overview' && (
            <div>
              <h1 className="dashboard-title" style={{ background: 'linear-gradient(to right, #fff, #34d399)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                Cổng Quản Lý Nông Trại
              </h1>
              <p className="dashboard-subtitle">Chào mừng {currentSession!.fullName}. Quản lý mùa vụ, cảm biến IoT & blockchain truy xuất nguồn gốc.</p>
              <div className="feature-grid">
                <div className="feature-card">
                  <div className="feature-card__icon">📊</div>
                  <div className="feature-card__title">Bảng Điều Khiển Mùa Vụ</div>
                  <div className="feature-card__desc">Theo dõi diện tích canh tác, sản lượng dự kiến & nhật ký thu hoạch theo thời gian thực.</div>
                </div>
                <div className="feature-card">
                  <div className="feature-card__icon">🔗</div>
                  <div className="feature-card__title">Mã QR VeChain Blockchain</div>
                  <div className="feature-card__desc">Cấp mã QR truy xuất nguồn gốc nông sản lên blockchain công khai VeChainThor.</div>
                </div>
                <div className="feature-card">
                  <div className="feature-card__icon">🌡️</div>
                  <div className="feature-card__title">Giám Sát Cảm Biến IoT</div>
                  <div className="feature-card__desc">Dữ liệu nhiệt độ, độ ẩm đất, độ pH thời gian thực từ thiết bị cảm biến trên nông trại.</div>
                </div>
                <div className="feature-card">
                  <div className="feature-card__icon">📋</div>
                  <div className="feature-card__title">Nhật Ký Canh Tác</div>
                  <div className="feature-card__desc">Ghi chép sử dụng phân bón, thuốc trừ sâu, tưới tiêu theo tiêu chuẩn VietGAP.</div>
                </div>
              </div>
            </div>
          )}

          {currentPortal === 'farm' && currentTab === 'crops' && (
            <div>
              <h1 className="dashboard-title" style={{ background: 'linear-gradient(to right, #fff, #34d399)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                Quản Lý Mùa Vụ
              </h1>
              <p className="dashboard-subtitle">Theo dõi và quản lý các mùa vụ canh tác trên trang trại của bạn.</p>
              <div className="glass-panel" style={{ padding: '48px', textAlign: 'center', background: 'rgba(22,23,33,0.4)' }}>
                <div style={{ fontSize: '56px', marginBottom: '16px' }}>🌱</div>
                <h2 style={{ color: '#34d399', fontSize: '20px', fontWeight: 700 }}>Tính năng đang phát triển</h2>
                <p style={{ color: 'var(--text-secondary)', marginTop: '8px', fontSize: '14px' }}>
                  Module quản lý mùa vụ sẽ sớm được cập nhật trong phiên bản tiếp theo.
                </p>
              </div>
            </div>
          )}

          {/* ── RETAIL DASHBOARD ── */}
          {currentPortal === 'retail' && currentTab === 'overview' && (
            <div>
              <h1 className="dashboard-title" style={{ background: 'linear-gradient(to right, #fff, #38bdf8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                Sàn Giao Dịch Nông Sản Sạch
              </h1>
              <p className="dashboard-subtitle">Chào mừng {currentSession!.fullName}. Tìm kiếm, đặt hàng & theo dõi vận chuyển nông sản VietGAP.</p>
              <div className="feature-grid">
                <div className="feature-card">
                  <div className="feature-card__icon">🔍</div>
                  <div className="feature-card__title">Tìm Kiếm Nông Sản VietGAP</div>
                  <div className="feature-card__desc">Duyệt danh mục nguồn hàng nông sản sạch trực tiếp từ trang trại được chứng nhận.</div>
                </div>
                <div className="feature-card">
                  <div className="feature-card__icon">📦</div>
                  <div className="feature-card__title">Quản Lý Đơn Hàng & Hợp Đồng</div>
                  <div className="feature-card__desc">Đặt cọc 30%, xác nhận nhận hàng và tạo hợp đồng thông minh trên blockchain.</div>
                </div>
                <div className="feature-card">
                  <div className="feature-card__icon">🚚</div>
                  <div className="feature-card__title">Theo Dõi Vận Chuyển Realtime</div>
                  <div className="feature-card__desc">Tracking thời gian thực tiến trình giao nhận lô hàng từ đơn vị vận chuyển.</div>
                </div>
                <div className="feature-card">
                  <div className="feature-card__icon">📝</div>
                  <div className="feature-card__title">Hợp Đồng Thông Minh</div>
                  <div className="feature-card__desc">Smart contracts trên VeChainThor đảm bảo thanh toán minh bạch, tự động.</div>
                </div>
              </div>
            </div>
          )}

          {showModal && (
            <AdminModal
              admin={selectedAdmin}
              onClose={() => { setShowModal(false); setSelectedAdmin(null); }}
              onSave={handleSaveAdmin}
            />
          )}
          <Toast toasts={toasts} onClose={handleCloseToast} />
        </main>
      </div>
    );
  }

  // ────────────────────────────────────────────────────
  // RENDER: Landing Page (/)
  // ────────────────────────────────────────────────────
  return (
    <div style={{ minHeight: '100vh', background: '#0b0f19', color: '#fff' }}>
      {/* Global Header */}
      <header className="global-header">
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <span className="global-header__brand" onClick={() => navigateTo('/')}>BICAP Platform</span>
          <nav className="global-header__nav">
            <button className="global-header__nav-btn" onClick={() => navigateTo('/admin')}>🛡️ Admin</button>
            <button className="global-header__nav-btn" onClick={() => navigateTo('/farm')}>🌾 Farm</button>
            <button className="global-header__nav-btn" onClick={() => navigateTo('/retail')}>🛒 Retail</button>
          </nav>
        </div>
      </header>

      {/* Hero Section */}
      <section className="landing-hero">
        <div className="landing-logo">B</div>
        <h1 className="landing-title">
          BICAP Platform
        </h1>
        <p style={{ fontSize: '18px', color: '#94a3b8', marginBottom: '8px' }}>
          Blockchain-Integrated Clean Agricultural Platform
        </p>
        <p className="landing-desc">
          Nền tảng truy xuất nguồn gốc nông sản sạch trên blockchain VeChainThor.
          Kết nối Nông trại — Nhà bán lẻ — Người tiêu dùng với dữ liệu minh bạch, xác thực IoT.
        </p>
      </section>

      {/* Portal Cards */}
      <div className="portal-grid">
        <div className="portal-card portal-card--admin" onClick={() => navigateTo('/admin')}>
          <div className="portal-card__icon">🛡️</div>
          <h2 className="portal-card__title" style={{ color: '#c084fc' }}>Cổng Quản Trị Hệ Thống</h2>
          <p className="portal-card__desc">
            Quản lý tài khoản Admin, phân quyền RBAC, kiểm duyệt dữ liệu và giám sát toàn hệ thống blockchain.
          </p>
          <span className="portal-card__link" style={{ color: '#c084fc' }}>Truy cập /admin →</span>
        </div>

        <div className="portal-card portal-card--farm" onClick={() => navigateTo('/farm')}>
          <div className="portal-card__icon">🌾</div>
          <h2 className="portal-card__title" style={{ color: '#34d399' }}>Cổng Quản Lý Nông Trại</h2>
          <p className="portal-card__desc">
            Quản lý mùa vụ, ghi nhật ký canh tác, giám sát cảm biến IoT & cấp mã QR truy xuất VeChain.
          </p>
          <span className="portal-card__link" style={{ color: '#34d399' }}>Truy cập /farm →</span>
        </div>

        <div className="portal-card portal-card--retail" onClick={() => navigateTo('/retail')}>
          <div className="portal-card__icon">🛒</div>
          <h2 className="portal-card__title" style={{ color: '#38bdf8' }}>Sàn Giao Dịch Nông Sản</h2>
          <p className="portal-card__desc">
            Tìm kiếm nguồn hàng nông sản sạch VietGAP, đặt hàng, ký hợp đồng & tracking vận chuyển realtime.
          </p>
          <span className="portal-card__link" style={{ color: '#38bdf8' }}>Truy cập /retail →</span>
        </div>
      </div>

      {/* Footer */}
      <footer style={{ textAlign: 'center', padding: '48px 20px 32px', color: 'var(--text-muted)', fontSize: '12px' }}>
        <p>© 2025 BICAP — Blockchain-Integrated Clean Agricultural Platform</p>
        <p style={{ marginTop: '4px' }}>UT Education · Java Programming · VeChainThor</p>
      </footer>

      <Toast toasts={toasts} onClose={handleCloseToast} />
    </div>
  );
}
