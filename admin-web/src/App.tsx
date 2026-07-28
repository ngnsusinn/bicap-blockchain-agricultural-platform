import { useState, useEffect, useCallback } from 'react';
import { Sidebar } from './components/Sidebar';
import { LoginSimulator } from './components/LoginSimulator';
import type { UserSession } from './components/LoginSimulator';
import { StatsCards } from './components/StatsCards';
import { AdminTable } from './components/AdminTable';
import { AdminModal } from './components/AdminModal';
import { Toast } from './components/Toast';
import type { ToastMessage } from './components/Toast';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/admins';

export default function App() {
  // Navigation & Session State
  const [currentTab, setCurrentTab] = useState('admins');
  const [currentSession, setCurrentSession] = useState<UserSession>({
    email: 'superadmin@bicap.com',
    fullName: 'Trần Nguyễn Gia Bảo (Super Admin)',
    role: 'SUPER_ADMIN',
    permissions: ['ADMIN_CREATE', 'ADMIN_READ', 'ADMIN_UPDATE', 'ADMIN_DELETE'],
  });

  // JWT stored after login/register
  const [token, setToken] = useState<string | null>(localStorage.getItem('ACCESS_TOKEN'));


  // Table Data & Filter State
  const [admins, setAdmins] = useState<any[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // UI State
  const [showModal, setShowModal] = useState(false);
  const [selectedAdmin, setSelectedAdmin] = useState<any | null>(null);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Toast Helper
  const showToast = useCallback((text: string, type: ToastMessage['type'] = 'info') => {
    const id = Date.now().toString();
    setToasts((prev) => [...prev, { id, text, type }]);
  }, []);

  const handleCloseToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  // Fetch Admins from API
  const fetchAdmins = useCallback(async () => {
    try {
      const params = new URLSearchParams({
        search: searchTerm,
        status: statusFilter,
        role: roleFilter,
        page: currentPage.toString(),
        size: '5',
      });

      const headers: Record<string, string> = {
        'X-Actor-Email': currentSession.email,
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${API_BASE_URL}?${params}`, {
        headers,
      });

      if (!response.ok) {
        if (response.status === 403) {
          throw new Error('Access Denied (HTTP 403): You do not have permissions to view administrators.');
        }
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to fetch administrator accounts.');
      }

      const data = await response.json();
      setAdmins(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err: any) {
      setAdmins([]);
      setTotalPages(1);
      showToast(err.message, 'error');
    }
  }, [searchTerm, statusFilter, roleFilter, currentPage, currentSession.email, token, showToast]);

  // Sync Data on Filter or Session Switch
  useEffect(() => {
    if (currentTab === 'admins') {
      fetchAdmins();
    }
  }, [fetchAdmins, currentTab]);

  // Handle Edit Click
  const handleEditClick = (admin: any) => {
    setSelectedAdmin(admin);
    setShowModal(true);
  };

  // Handle Delete Click (Soft-delete)
  const handleDeleteClick = async (id: number) => {
    if (!window.confirm('Are you sure you want to soft-delete this administrator account?')) return;

    try {
      const headers: Record<string, string> = {
        'X-Actor-Email': currentSession.email,
      };
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const response = await fetch(`${API_BASE_URL}/${id}`, {
        method: 'DELETE',
        headers,
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Deletion failed.');
      }

      showToast('Administrator soft-deleted successfully (marked as INACTIVE).', 'success');
      fetchAdmins();
    } catch (err: any) {
      showToast(err.message, 'error');
    }
  };

  // Handle Create or Update Save
  const handleSaveAdmin = async (adminData: any) => {
    try {
      const isEdit = !!selectedAdmin;
      const url = isEdit ? `${API_BASE_URL}/${selectedAdmin.id}` : API_BASE_URL;
      const method = isEdit ? 'PUT' : 'POST';

      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'X-Actor-Email': currentSession.email,
      };
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const response = await fetch(url, {
        method,
        headers,
        body: JSON.stringify(adminData),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Operation failed.');
      }

      showToast(
        isEdit
          ? 'Administrator details updated successfully.'
          : 'New administrator created successfully (sent default activation status).',
        'success'
      );
      setShowModal(false);
      setSelectedAdmin(null);
      fetchAdmins();
    } catch (err: any) {
      showToast(err.message, 'error');
    }
  };

  return (
    <div className="app-container">
      <Sidebar currentTab={currentTab} onTabChange={setCurrentTab} />

      <main className="main-content animate-fade-in">
        {currentTab === 'overview' && (
          <div>
            <h1 className="dashboard-title">Dashboard Overview</h1>
            <p className="dashboard-subtitle">BICAP - Blockchain Agricultural Platform Administrator Portal</p>

            <div className="glass-panel" style={{ padding: '32px', textAlign: 'center', background: 'rgba(22, 23, 33, 0.4)' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌱</div>
              <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Welcome to the Blockchain Admin panel!</h2>
              <p style={{ color: 'var(--text-secondary)', marginTop: '8px', maxWidth: '600px', marginInline: 'auto', fontSize: '14px', lineHeight: '1.6' }}>
                This dashboard allows system managers to inspect IoT data sensors, approve agricultural certificate requests, track supply chain contracts on VeChainThor, and configure secure RBAC administrator profiles.
              </p>
              <button 
                onClick={() => setCurrentTab('admins')} 
                className="btn btn-primary" 
                style={{ marginTop: '24px' }}
              >
                Go to Admin Management
              </button>
            </div>
          </div>
        )}

        {currentTab === 'admins' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
              <div>
                <h1 className="dashboard-title">Administrator Management</h1>
                <p className="dashboard-subtitle">CRUD accounts, audit statuses, and manage role-based fine-grained permissions.</p>
              </div>
              <button
                onClick={() => {
                  setSelectedAdmin(null);
                  setShowModal(true);
                }}
                disabled={currentSession.role !== 'SUPER_ADMIN'}
                className="btn btn-primary"
                style={{ marginTop: '8px' }}
                title={
                  currentSession.role !== 'SUPER_ADMIN'
                    ? 'Only SUPER_ADMIN has permission to create administrators'
                    : 'Create new administrator'
                }
              >
                ➕ Create Admin Account
              </button>
            </div>

            {/* Login Simulation panel */}
            <LoginSimulator
              currentSession={currentSession}
              onSessionChange={setCurrentSession}
              token={token}
              onTokenChange={(t: string | null) => {
                if (t) {
                  localStorage.setItem('ACCESS_TOKEN', t);
                } else {
                  localStorage.removeItem('ACCESS_TOKEN');
                }
                setToken(t);
              }}
            />

            {/* General metrics cards */}
            <StatsCards admins={admins} />

            {/* Main Data Table */}
            <AdminTable
              admins={admins}
              currentSession={currentSession}
              searchTerm={searchTerm}
              onSearchChange={(val) => {
                setSearchTerm(val);
                setCurrentPage(0);
              }}
              statusFilter={statusFilter}
              onStatusFilterChange={(val) => {
                setStatusFilter(val);
                setCurrentPage(0);
              }}
              roleFilter={roleFilter}
              onRoleFilterChange={(val) => {
                setRoleFilter(val);
                setCurrentPage(0);
              }}
              onEdit={handleEditClick}
              onDelete={handleDeleteClick}
              currentPage={currentPage}
              onPageChange={setCurrentPage}
              totalPages={totalPages}
            />
          </div>
        )}

        {/* Create/Edit Modal overlay */}
        {showModal && (
          <AdminModal
            admin={selectedAdmin}
            onClose={() => {
              setShowModal(false);
              setSelectedAdmin(null);
            }}
            onSave={handleSaveAdmin}
          />
        )}

        {/* Global animated toasts */}
        <Toast toasts={toasts} onClose={handleCloseToast} />
      </main>
    </div>
  );
}
