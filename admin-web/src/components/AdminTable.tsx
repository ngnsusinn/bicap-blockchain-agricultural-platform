import React from 'react';
import type { UserSession } from './LoginSimulator';

interface AdminTableProps {
  admins: any[];
  currentSession: UserSession;
  searchTerm: string;
  onSearchChange: (val: string) => void;
  statusFilter: string;
  onStatusFilterChange: (val: string) => void;
  roleFilter: string;
  onRoleFilterChange: (val: string) => void;
  onEdit: (admin: any) => void;
  onDelete: (id: number) => void;
  currentPage: number;
  onPageChange: (page: number) => void;
  totalPages: number;
}

export const AdminTable: React.FC<AdminTableProps> = ({
  admins,
  currentSession,
  searchTerm,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  roleFilter,
  onRoleFilterChange,
  onEdit,
  onDelete,
  currentPage,
  onPageChange,
  totalPages,
}) => {
  const isSuperAdmin = currentSession.role === 'SUPER_ADMIN';

  return (
    <div className="glass-panel" style={containerStyle}>
      {/* Search & Filters */}
      <div style={filterBarStyle}>
        <div style={searchContainerStyle}>
          <span style={searchIconStyle}>🔍</span>
          <input
            type="text"
            placeholder="Search by name, email, phone..."
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            style={searchInputStyle}
          />
        </div>
        <div style={filtersGroupStyle}>
          <select
            value={roleFilter}
            onChange={(e) => onRoleFilterChange(e.target.value)}
            className="input-control select-control"
            style={selectStyle}
          >
            <option value="">All Roles</option>
            <option value="SUPER_ADMIN">Super Admin</option>
            <option value="ADMIN">Admin</option>
            <option value="MODERATOR">Moderator</option>
          </select>

          <select
            value={statusFilter}
            onChange={(e) => onStatusFilterChange(e.target.value)}
            className="input-control select-control"
            style={selectStyle}
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="INACTIVE">Inactive (Deleted)</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div style={tableWrapperStyle}>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thStyle}>ID</th>
              <th style={thStyle}>Full Name</th>
              <th style={thStyle}>Email</th>
              <th style={thStyle}>Phone</th>
              <th style={thStyle}>Role</th>
              <th style={thStyle}>Permissions</th>
              <th style={thStyle}>Status</th>
              <th style={thStyle}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {admins.length === 0 ? (
              <tr>
                <td colSpan={8} style={noDataStyle}>
                  No administrators found matching the filters.
                </td>
              </tr>
            ) : (
              admins.map((admin) => {
                const isSelf = admin.email === currentSession.email;
                const statusConfig = statusStyles[admin.status] || statusStyles.ACTIVE;

                return (
                  <tr key={admin.id} style={trStyle}>
                    <td style={tdStyle}>{admin.id}</td>
                    <td style={tdStyle}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <div style={avatarStyle}>
                          {admin.avatarUrl ? (
                            <img src={admin.avatarUrl} alt="" style={avatarImgStyle} />
                          ) : (
                            admin.fullName.charAt(0).toUpperCase()
                          )}
                        </div>
                        <div>
                          <div style={{ fontWeight: 600, color: '#fff' }}>
                            {admin.fullName} {isSelf && <span style={selfTagStyle}>You</span>}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td style={tdStyle}>{admin.email}</td>
                    <td style={tdStyle}>{admin.phone || '-'}</td>
                    <td style={tdStyle}>
                      <span style={roleBadgeStyle}>{((admin.roles || [])[0]?.name) || 'N/A'}</span>
                    </td>
                    <td style={tdStyle}>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', maxWidth: '300px' }}>
                        {((admin.roles || []) as any[])
                          .flatMap((role) => role.permissions || [])
                          .map((perm: any) => perm.code)
                          .map((permCode: string) => (
                            <span key={permCode} style={permStyle}>
                              {permCode}
                            </span>
                          ))}
                      </div>
                    </td>
                    <td style={tdStyle}>
                      <span style={{ ...pillStyle, backgroundColor: statusConfig.bg, color: statusConfig.color }}>
                        {admin.status}
                      </span>
                    </td>
                    <td style={tdStyle}>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          onClick={() => onEdit(admin)}
                          className="btn btn-secondary"
                          style={actionBtnStyle}
                          title="Edit admin account"
                        >
                          ✏️ Edit
                        </button>
                        <button
                          onClick={() => onDelete(admin.id)}
                          disabled={!isSuperAdmin || isSelf || admin.status === 'INACTIVE'}
                          className="btn btn-danger"
                          style={{
                            ...actionBtnStyle,
                            opacity: (!isSuperAdmin || isSelf || admin.status === 'INACTIVE') ? 0.4 : 1,
                            cursor: (!isSuperAdmin || isSelf || admin.status === 'INACTIVE') ? 'not-allowed' : 'pointer',
                          }}
                          title={
                            isSelf
                              ? "Cannot delete your own logged-in account"
                              : !isSuperAdmin
                              ? "Only SUPER_ADMIN can delete admin accounts"
                              : admin.status === 'INACTIVE'
                              ? "Account is already inactive (soft-deleted)"
                              : "Soft-delete account"
                          }
                        >
                          🗑️ Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={paginationContainerStyle}>
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0}
            className="btn btn-secondary"
            style={pageBtnStyle}
          >
            ◀ Prev
          </button>
          <span style={pageInfoStyle}>
            Page <strong>{currentPage + 1}</strong> of <strong>{totalPages}</strong>
          </span>
          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage === totalPages - 1}
            className="btn btn-secondary"
            style={pageBtnStyle}
          >
            Next ▶
          </button>
        </div>
      )}
    </div>
  );
};

const statusStyles: Record<string, { bg: string; color: string }> = {
  ACTIVE: { bg: 'var(--success-light)', color: 'var(--success-hover)' },
  SUSPENDED: { bg: 'var(--warning-light)', color: 'var(--warning-hover)' },
  INACTIVE: { bg: 'var(--danger-light)', color: 'var(--danger-hover)' },
};

const containerStyle: React.CSSProperties = {
  padding: '24px',
  background: 'rgba(22, 23, 33, 0.5)',
};

const filterBarStyle: React.CSSProperties = {
  display: 'flex',
  flexWrap: 'wrap',
  gap: '16px',
  justifyContent: 'space-between',
  alignItems: 'center',
  marginBottom: '20px',
};

const searchContainerStyle: React.CSSProperties = {
  position: 'relative',
  flex: 1,
  minWidth: '280px',
};

const searchIconStyle: React.CSSProperties = {
  position: 'absolute',
  left: '16px',
  top: '50%',
  transform: 'translateY(-50%)',
  fontSize: '14px',
  color: 'var(--text-secondary)',
};

const searchInputStyle: React.CSSProperties = {
  width: '100%',
  padding: '12px 16px 12px 42px',
  background: 'rgba(0, 0, 0, 0.2)',
  border: '1px solid var(--border-color)',
  borderRadius: '8px',
  color: '#fff',
  fontSize: '14px',
};

const filtersGroupStyle: React.CSSProperties = {
  display: 'flex',
  gap: '12px',
};

const selectStyle: React.CSSProperties = {
  padding: '10px 16px',
  background: 'rgba(0, 0, 0, 0.2)',
  borderRadius: '8px',
  width: '160px',
  fontSize: '14px',
};

const tableWrapperStyle: React.CSSProperties = {
  overflowX: 'auto',
  borderRadius: '8px',
  border: '1px solid var(--border-color)',
};

const tableStyle: React.CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
  textAlign: 'left',
  fontSize: '14px',
};

const thStyle: React.CSSProperties = {
  padding: '16px',
  background: 'rgba(0, 0, 0, 0.3)',
  color: 'var(--text-secondary)',
  fontWeight: 600,
  borderBottom: '1px solid var(--border-color)',
};

const trStyle: React.CSSProperties = {
  borderBottom: '1px solid rgba(255, 255, 255, 0.04)',
  transition: 'background 0.2s ease',
};

const tdStyle: React.CSSProperties = {
  padding: '16px',
  verticalAlign: 'middle',
  color: 'var(--text-primary)',
};

const noDataStyle: React.CSSProperties = {
  padding: '32px',
  textAlign: 'center',
  color: 'var(--text-muted)',
};

const avatarStyle: React.CSSProperties = {
  width: '36px',
  height: '36px',
  borderRadius: '50%',
  background: 'var(--primary-light)',
  color: 'var(--primary-hover)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontWeight: 'bold',
  fontSize: '14px',
  border: '1px solid rgba(139, 92, 246, 0.2)',
};

const avatarImgStyle: React.CSSProperties = {
  width: '100%',
  height: '100%',
  borderRadius: '50%',
  objectFit: 'cover',
};

const selfTagStyle: React.CSSProperties = {
  fontSize: '9px',
  background: 'rgba(255, 255, 255, 0.1)',
  padding: '2px 6px',
  borderRadius: '10px',
  color: 'var(--text-secondary)',
  marginLeft: '6px',
};

const roleBadgeStyle: React.CSSProperties = {
  padding: '4px 10px',
  borderRadius: '6px',
  fontSize: '11px',
  fontWeight: 700,
  background: 'rgba(255, 255, 255, 0.05)',
  border: '1px solid rgba(255, 255, 255, 0.1)',
};

const permStyle: React.CSSProperties = {
  fontSize: '10px',
  padding: '2px 6px',
  borderRadius: '4px',
  background: 'rgba(139, 92, 246, 0.08)',
  color: 'var(--primary-hover)',
  border: '1px solid rgba(139, 92, 246, 0.15)',
};

const pillStyle: React.CSSProperties = {
  padding: '4px 10px',
  borderRadius: '12px',
  fontSize: '11px',
  fontWeight: 700,
  textTransform: 'uppercase',
};

const actionBtnStyle: React.CSSProperties = {
  padding: '6px 12px',
  fontSize: '12px',
  borderRadius: '6px',
};

const paginationContainerStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  gap: '16px',
  marginTop: '20px',
};

const pageBtnStyle: React.CSSProperties = {
  padding: '6px 12px',
  fontSize: '13px',
};

const pageInfoStyle: React.CSSProperties = {
  fontSize: '13px',
  color: 'var(--text-secondary)',
};
