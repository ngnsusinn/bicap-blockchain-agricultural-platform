import { useState, useEffect, useCallback } from 'react';
import { Sidebar } from './components/Sidebar';
import type { UserSession, AdminUser } from './types';
import { LoginPage } from './components/LoginPage';
import { StatsCards } from './components/StatsCards';
import { AdminTable } from './components/AdminTable';
import { AdminModal } from './components/AdminModal';
import { DashboardPage } from './components/DashboardPage';
import { NotificationBell } from './components/NotificationBell';
import { FarmApprovalPage } from './components/FarmApprovalPage';
import { FarmManagementPage } from './components/FarmManagementPage';
import { SmartContractPage } from './components/SmartContractPage';
import { ProductMonitoringPage } from './components/ProductMonitoringPage';
import { ReportsPage } from './components/ReportsPage';
import { ServicePackagesPage } from './components/ServicePackagesPage';
import { Toast } from './components/Toast';
import type { ToastMessage } from './components/Toast';
import { API_ORIGIN } from './utils/api';
import {
  getToken,
  getCurrentUser,
  saveSession,
  clearSession,
  isAdminRole,
} from '../shared/session';

const API_BASE_URL = `${API_ORIGIN}/api/admins`;

/**
 * Admin dashboard — served at the `/admin` endpoint of the unified web app
 * (see `src/App.tsx`). It no longer carries its own farm/retail placeholders or
 * landing page: those live in the portal at `/`. Tabs are component state, so
 * only `/admin` needs to be reachable.
 */
export default function AdminApp() {
  // ── Auth & Session (shared with the portal) ──
  const [currentSession, setCurrentSession] = useState<UserSession | null>(() => {
    const user = getCurrentUser();
    return user && isAdminRole(user.role) ? user : null;
  });
  const isAuthenticated = !!currentSession && !!getToken();

  // ── Navigation ──
  const [currentTab, setCurrentTab] = useState('overview');

  // ── Admin Table State ──
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [showModal, setShowModal] = useState(false);
  const [selectedAdmin, setSelectedAdmin] = useState<AdminUser | null>(null);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Debounce search input so typing doesn't fire one request per keystroke (M-19).
  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchTerm), 350);
    return () => clearTimeout(t);
  }, [searchTerm]);

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
    saveSession(session.accessToken || '', session);
    setCurrentSession(session);
    showToast(`Xin chào, ${session.fullName}!`, 'success');
  };

  // ── Logout Handler ──
  const handleLogout = () => {
    clearSession();
    setCurrentSession(null);
    showToast('Đã đăng xuất khỏi hệ thống.', 'info');
  };

  // ── Admin CRUD API calls ──
  const fetchAdmins = useCallback(async () => {
    if (!currentSession) return;
    try {
      const params = new URLSearchParams({ search: debouncedSearch, status: statusFilter, role: roleFilter, page: currentPage.toString(), size: '5' });
      const authToken = getToken();
      const headers: Record<string, string> = { 'X-Actor-Email': currentSession.email };
      if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

      const response = await fetch(`${API_BASE_URL}?${params}`, { headers });
      if (!response.ok) {
        if (response.status === 401) {
          handleLogout();
          showToast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'error');
          return;
        }
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
  }, [debouncedSearch, statusFilter, roleFilter, currentPage, currentSession, showToast]);

  useEffect(() => {
    if (isAuthenticated && currentTab === 'admins') fetchAdmins();
  }, [fetchAdmins, currentTab, isAuthenticated]);

  const handleEditClick = (admin: AdminUser) => { setSelectedAdmin(admin); setShowModal(true); };

  const handleDeleteClick = async (id: number) => {
    if (!currentSession) return;
    if (!window.confirm('Bạn có chắc muốn xoá tài khoản quản trị này?')) return;
    try {
      const authToken = getToken();
      const headers: Record<string, string> = { 'X-Actor-Email': currentSession.email };
      if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
      const response = await fetch(`${API_BASE_URL}/${id}`, { method: 'DELETE', headers });
      if (!response.ok) {
        if (response.status === 401) { handleLogout(); showToast('Phiên đăng nhập đã hết hạn.', 'error'); return; }
        const errorData = await response.json(); throw new Error(errorData.message || 'Xoá thất bại.');
      }
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
      const authToken = getToken();
      const headers: Record<string, string> = { 'Content-Type': 'application/json', 'X-Actor-Email': currentSession.email };
      if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
      const response = await fetch(url, { method, headers, body: JSON.stringify(adminData) });
      if (!response.ok) {
        if (response.status === 401) { handleLogout(); showToast('Phiên đăng nhập đã hết hạn.', 'error'); return; }
        const errorData = await response.json(); throw new Error(errorData.message || 'Thao tác thất bại.');
      }
      showToast(isEdit ? 'Cập nhật thành công.' : 'Tạo tài khoản mới thành công.', 'success');
      setShowModal(false); setSelectedAdmin(null); fetchAdmins();
    } catch (err: any) { showToast(err.message, 'error'); }
  };

  // ────────────────────────────────────────────────────
  // RENDER: login gate
  // ────────────────────────────────────────────────────
  if (!isAuthenticated || !currentSession) {
    return (
      <>
        <LoginPage onLoginSuccess={handleLoginSuccess} />
        <Toast toasts={toasts} onClose={handleCloseToast} />
      </>
    );
  }

  // ────────────────────────────────────────────────────
  // RENDER: admin dashboard
  // ────────────────────────────────────────────────────
  return (
    <div className="app-container">
      <Sidebar
        currentTab={currentTab}
        onTabChange={setCurrentTab}
        currentSession={currentSession}
        onLogout={handleLogout}
      />

      <main className="main-content animate-fade-in">
        {/* Header bar: NotificationBell (detail-design §4.2 Header) */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '12px', marginBottom: '18px' }}>
          <NotificationBell email={currentSession.email} />
        </div>

        {currentTab === 'overview' && (
          <DashboardPage
            currentSession={currentSession}
            onToast={showToast}
            onNavigateTab={setCurrentTab}
          />
        )}

        {currentTab === 'admins' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
              <div>
                <h1 className="dashboard-title">Quản Lý Quản Trị Viên</h1>
                <p className="dashboard-subtitle">CRUD tài khoản, kiểm tra trạng thái, quản lý phân quyền RBAC.</p>
              </div>
              <button
                onClick={() => { setSelectedAdmin(null); setShowModal(true); }}
                disabled={currentSession.role !== 'SUPER_ADMIN'}
                className="btn btn-primary" style={{ marginTop: '8px' }}
                title={currentSession.role !== 'SUPER_ADMIN' ? 'Chỉ SUPER_ADMIN mới có quyền tạo' : 'Tạo quản trị viên mới'}
              >
                ➕ Tạo Admin mới
              </button>
            </div>
            <StatsCards admins={admins} />
            <AdminTable
              admins={admins} currentSession={currentSession}
              searchTerm={searchTerm} onSearchChange={(v) => { setSearchTerm(v); setCurrentPage(0); }}
              statusFilter={statusFilter} onStatusFilterChange={(v) => { setStatusFilter(v); setCurrentPage(0); }}
              roleFilter={roleFilter} onRoleFilterChange={(v) => { setRoleFilter(v); setCurrentPage(0); }}
              onEdit={handleEditClick} onDelete={handleDeleteClick}
              currentPage={currentPage} onPageChange={setCurrentPage} totalPages={totalPages}
            />
          </div>
        )}

        {currentTab === 'farms' && (
          <FarmApprovalPage currentSession={currentSession} onToast={showToast} />
        )}

        {currentTab === 'farmmgmt' && (
          <FarmManagementPage currentSession={currentSession} onToast={showToast} />
        )}

        {currentTab === 'packages' && (
          <ServicePackagesPage currentSession={currentSession} onToast={showToast} />
        )}

        {currentTab === 'contracts' && (
          <SmartContractPage currentSession={currentSession} onToast={showToast} />
        )}

        {currentTab === 'products' && (
          <ProductMonitoringPage currentSession={currentSession} onToast={showToast} />
        )}

        {currentTab === 'reports' && (
          <ReportsPage currentSession={currentSession} onToast={showToast} />
        )}

        {showModal && (
          <AdminModal
            admin={selectedAdmin}
            actorEmail={currentSession.email}
            onClose={() => { setShowModal(false); setSelectedAdmin(null); }}
            onSave={handleSaveAdmin}
          />
        )}
        <Toast toasts={toasts} onClose={handleCloseToast} />
      </main>
    </div>
  );
}
