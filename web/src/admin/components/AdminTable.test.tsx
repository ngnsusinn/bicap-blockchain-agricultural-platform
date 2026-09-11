import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { AdminTable, permissionCodes } from './AdminTable';
import type { AdminUser, UserSession } from '../types';

/**
 * F2 — quyền hiển thị phải là hợp của `permissions` (effective) và `roles[].permissions`,
 * đã dedupe theo code, khớp với dữ liệu backend đã lưu.
 */

const session: UserSession = {
  id: 99, email: 'admin@bicap.com', fullName: 'Admin User',
  role: 'SUPER_ADMIN', permissions: [], accessToken: 't',
};

const admin: AdminUser = {
  id: 7,
  email: 'editor@bicap.com',
  fullName: 'Editor User',
  phone: '0987654321',
  status: 'ACTIVE',
  roles: [
    {
      id: 2,
      name: 'ADMIN',
      description: 'Admin',
      permissions: [
        { id: 1, code: 'ADMIN_READ', description: 'Read admins' },
        { id: 3, code: 'ADMIN_UPDATE', description: 'Update admins' },
      ],
    },
  ],
  // effective set from the backend (union) — includes a directly granted permission
  permissions: [
    { id: 1, code: 'ADMIN_READ', description: 'Read admins' },
    { id: 4, code: 'REPORT_VIEW', description: 'View reports' },
  ],
};

const tableProps = {
  admins: [admin],
  currentSession: session,
  searchTerm: '',
  onSearchChange: vi.fn(),
  statusFilter: '',
  onStatusFilterChange: vi.fn(),
  roleFilter: '',
  onRoleFilterChange: vi.fn(),
  onEdit: vi.fn(),
  onDelete: vi.fn(),
  currentPage: 0,
  onPageChange: vi.fn(),
  totalPages: 1,
};

describe('AdminTable permissions', () => {
  it('merges top-level permissions with role permissions and dedupes by code', () => {
    expect(permissionCodes(admin)).toEqual(['ADMIN_READ', 'REPORT_VIEW', 'ADMIN_UPDATE']);
  });

  it('renders every effective permission chip exactly once', () => {
    render(<AdminTable {...tableProps} />);

    expect(screen.getByText('REPORT_VIEW')).toBeInTheDocument();
    expect(screen.getByText('ADMIN_UPDATE')).toBeInTheDocument();
    expect(screen.getAllByText('ADMIN_READ')).toHaveLength(1);
  });
});
