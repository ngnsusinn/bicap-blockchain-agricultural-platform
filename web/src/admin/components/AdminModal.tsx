import React, { useState, useEffect } from 'react';
import type { AdminUser, PermissionResponse } from '../types';
import { API_ORIGIN, authHeaders } from '../utils/api';

interface AdminModalProps {
  admin: AdminUser | null; // Null if creating
  /** F2 — current admin's email, sent as X-Actor-Email when loading the permission catalogue. */
  actorEmail: string;
  onClose: () => void;
  onSave: (adminData: any) => void;
}

export const AdminModal: React.FC<AdminModalProps> = ({ admin, actorEmail, onClose, onSave }) => {
  const isEdit = !!admin;

  // Form State
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState('ADMIN');
  const [status, setStatus] = useState('ACTIVE');
  const [selectedPermissions, setSelectedPermissions] = useState<string[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  // F2 — real permission catalogue from GET /api/admins/permissions (no hard-coded codes).
  const [catalog, setCatalog] = useState<PermissionResponse[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState('');

  useEffect(() => {
    let alive = true;
    setCatalogLoading(true);
    setCatalogError('');
    fetch(`${API_ORIGIN}/api/admins/permissions`, { headers: authHeaders(actorEmail) })
      .then(async (res) => {
        if (!res.ok) throw new Error(`Không tải được danh sách quyền (mã lỗi ${res.status}).`);
        return res.json();
      })
      .then((data) => {
        if (alive) setCatalog(Array.isArray(data) ? data : []);
      })
      .catch((err) => {
        if (alive) {
          setCatalog([]);
          setCatalogError(err instanceof Error ? err.message : 'Không tải được danh sách quyền.');
        }
      })
      .finally(() => {
        if (alive) setCatalogLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [actorEmail]);

  // Sync state if edit mode
  const resolveAdminRole = (adminData: any) => {
    if (!adminData?.roles?.length) {
      return 'ADMIN';
    }
    return adminData.roles[0].name || 'ADMIN';
  };

  /**
   * F2 — effective permissions preferred (role permissions ∪ direct grants); if the
   * backend did not send a top-level set, fall back to the role permissions.
   */
  const resolveAdminPermissions = (adminData: any): string[] => {
    const direct: string[] = (adminData?.permissions ?? [])
      .map((perm: any) => perm?.code)
      .filter(Boolean);
    if (direct.length > 0) return Array.from(new Set(direct));

    return Array.from(new Set(
      (adminData?.roles ?? []).flatMap((role: any) =>
        (role.permissions ?? []).map((perm: any) => perm.code).filter(Boolean)
      )
    ));
  };

  useEffect(() => {
    if (admin) {
      setFullName(admin.fullName);
      setEmail(admin.email);
      setPhone(admin.phone || '');
      setRole(resolveAdminRole(admin));
      setStatus(admin.status);
      setSelectedPermissions(resolveAdminPermissions(admin));
      setPassword(''); // Don't edit password unless entered
    } else {
      setFullName('');
      setEmail('');
      setPassword('');
      setPhone('');
      setRole('ADMIN');
      setStatus('ACTIVE');
      setSelectedPermissions([]);
    }
    setErrors({});
  }, [admin]);

  // Role changes no longer inject hard-coded permission codes; the real catalogue is
  // the single source of truth and the admin chooses explicitly.
  const handleRoleChange = (selectedRole: string) => {
    setRole(selectedRole);
  };

  const handlePermissionToggle = (code: string) => {
    if (selectedPermissions.includes(code)) {
      setSelectedPermissions(selectedPermissions.filter(p => p !== code));
    } else {
      setSelectedPermissions([...selectedPermissions, code]);
    }
  };

  const validate = () => {
    const tempErrors: Record<string, string> = {};

    if (!fullName.trim()) tempErrors.fullName = 'Full name is required';
    
    if (!isEdit) {
      if (!email.trim()) {
        tempErrors.email = 'Email is required';
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        tempErrors.email = 'Email format is invalid';
      }

      if (!password) {
        tempErrors.password = 'Password is required';
      } else {
        if (password.length < 8) tempErrors.password = 'Password must be at least 8 characters';
        if (!/[A-Z]/.test(password)) tempErrors.password = 'Password must contain an uppercase letter';
        if (!/[a-z]/.test(password)) tempErrors.password = 'Password must contain a lowercase letter';
        if (!/[0-9]/.test(password)) tempErrors.password = 'Password must contain a number';
        if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) tempErrors.password = 'Password must contain a special character';
      }
    }

    if (phone.trim() && !/^(0[3|5|7|8|9])([0-9]{8})$/.test(phone.trim())) {
      tempErrors.phone = 'Phone number is invalid (Vietnam format: 10 digits starting with 0)';
    }

    setErrors(tempErrors);
    return Object.keys(tempErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const data: any = {
      fullName,
      phone,
      role,
      permissions: selectedPermissions,
      status,
    };

    if (!isEdit) {
      data.email = email;
      data.password = password;
    }

    setSubmitting(true);
    try {
      await onSave(data);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={overlayStyle}>
      <div className="glass-panel" style={modalStyle}>
        <div style={modalHeaderStyle}>
          <h2 style={{ fontSize: '20px', fontWeight: 700, color: '#fff' }}>
            {isEdit ? '✏️ Edit Administrator Details' : '➕ Create New Administrator'}
          </h2>
          <button onClick={onClose} style={closeBtnStyle} aria-label="Close modal">
            &times;
          </button>
        </div>

        <form onSubmit={handleSubmit} style={formStyle}>
          <div style={scrollContainerStyle}>
            {/* Full Name */}
            <div style={fieldGroupStyle}>
              <label style={labelStyle}>Full Name <span style={{ color: 'var(--danger)' }}>*</span></label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="input-control"
                placeholder="e.g. Nguyễn Văn A"
              />
              {errors.fullName && <span style={errorTextStyle}>{errors.fullName}</span>}
            </div>

            {/* Email */}
            <div style={fieldGroupStyle}>
              <label style={labelStyle}>Email Address <span style={{ color: 'var(--danger)' }}>*</span></label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="input-control"
                disabled={isEdit}
                placeholder="e.g. admin@bicap.com"
                style={{
                  background: isEdit ? 'rgba(255, 255, 255, 0.02)' : 'rgba(0, 0, 0, 0.2)',
                  color: isEdit ? 'var(--text-muted)' : '#fff',
                  cursor: isEdit ? 'not-allowed' : 'text',
                }}
              />
              {errors.email && <span style={errorTextStyle}>{errors.email}</span>}
            </div>

            {/* Password */}
            {!isEdit && (
              <div style={fieldGroupStyle}>
                <label style={labelStyle}>Password <span style={{ color: 'var(--danger)' }}>*</span></label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="input-control"
                  placeholder="At least 8 chars (Uppercase, Number, Symbol)"
                />
                <span style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Must contain 8+ characters, uppercase, lowercase, number, and a special character (!@#$).
                </span>
                {errors.password && <span style={errorTextStyle}>{errors.password}</span>}
              </div>
            )}

            {/* Phone */}
            <div style={fieldGroupStyle}>
              <label style={labelStyle}>Phone Number (Optional)</label>
              <input
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="input-control"
                placeholder="e.g. 0987654321"
              />
              {errors.phone && <span style={errorTextStyle}>{errors.phone}</span>}
            </div>

            <div style={twoColGridStyle}>
              {/* Role */}
              <div style={fieldGroupStyle}>
                <label style={labelStyle}>System Role</label>
                <select
                  value={role}
                  onChange={(e) => handleRoleChange(e.target.value)}
                  className="input-control select-control"
                >
                  <option value="SUPER_ADMIN">Super Admin</option>
                  <option value="ADMIN">Admin</option>
                  <option value="MODERATOR">Moderator</option>
                </select>
              </div>

              {/* Status */}
              {isEdit && (
                <div style={fieldGroupStyle}>
                  <label style={labelStyle}>Account Status</label>
                  <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                    className="input-control select-control"
                  >
                    <option value="ACTIVE">Active</option>
                    <option value="SUSPENDED">Suspended</option>
                    <option value="INACTIVE">Inactive (Soft Deleted)</option>
                  </select>
                </div>
              )}
            </div>

            {/* Permissions — catalogue thật từ GET /api/admins/permissions */}
            <div style={fieldGroupStyle}>
              <label style={labelStyle}>Role Permissions (Fine-grained RBAC override)</label>
              {catalogLoading ? (
                <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: 8 }}>
                  Đang tải danh sách quyền…
                </p>
              ) : catalogError ? (
                <p style={errorTextStyle}>{catalogError}</p>
              ) : catalog.length === 0 ? (
                <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: 8 }}>
                  Hệ thống chưa cấu hình quyền nào.
                </p>
              ) : (
                <div style={permissionsListStyle}>
                  {catalog.map((perm) => {
                    const isChecked = selectedPermissions.includes(perm.code);
                    return (
                      <label key={perm.code} style={{
                        ...checkboxContainerStyle,
                        border: isChecked ? '1px solid var(--primary-hover)' : '1px solid var(--border-color)',
                        background: isChecked ? 'rgba(139, 92, 246, 0.05)' : 'rgba(0, 0, 0, 0.1)',
                      }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handlePermissionToggle(perm.code)}
                          style={checkboxStyle}
                        />
                        <div>
                          <div style={{ fontWeight: 600, color: '#fff', fontSize: '13px' }}>{perm.code}</div>
                          <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '2px' }}>
                            {perm.description}
                          </div>
                        </div>
                      </label>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Footer Actions */}
          <div style={modalFooterStyle}>
            <button type="button" onClick={onClose} className="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" disabled={submitting} className="btn btn-primary" style={{ opacity: submitting ? 0.6 : 1, cursor: submitting ? 'not-allowed' : 'pointer' }}>
              {submitting ? 'Đang lưu...' : isEdit ? 'Save Changes' : 'Create Admin'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

const overlayStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  background: 'rgba(0, 0, 0, 0.6)',
  backdropFilter: 'blur(8px)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 2000,
  padding: '16px',
};

const modalStyle: React.CSSProperties = {
  width: '100%',
  maxWidth: '600px',
  background: 'var(--bg-panel)',
  maxHeight: '90vh',
  display: 'flex',
  flexDirection: 'column',
  animation: 'fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards',
};

const modalHeaderStyle: React.CSSProperties = {
  padding: '20px 24px',
  borderBottom: '1px solid var(--border-color)',
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
};

const closeBtnStyle: React.CSSProperties = {
  fontSize: '28px',
  color: 'var(--text-secondary)',
  cursor: 'pointer',
  lineHeight: 1,
};

const formStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  flex: 1,
  overflow: 'hidden',
};

const scrollContainerStyle: React.CSSProperties = {
  padding: '24px',
  overflowY: 'auto',
  flex: 1,
  display: 'flex',
  flexDirection: 'column',
  gap: '20px',
};

const fieldGroupStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
};

const labelStyle: React.CSSProperties = {
  fontSize: '13px',
  fontWeight: 600,
  color: 'var(--text-secondary)',
  marginBottom: '8px',
};

const twoColGridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '1fr 1fr',
  gap: '16px',
};

const permissionsListStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '1fr',
  gap: '10px',
  marginTop: '8px',
};

const checkboxContainerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'flex-start',
  gap: '12px',
  padding: '12px 16px',
  borderRadius: '8px',
  cursor: 'pointer',
  transition: 'all 0.2s ease',
};

const checkboxStyle: React.CSSProperties = {
  marginTop: '3px',
  accentColor: 'var(--primary)',
};

const errorTextStyle: React.CSSProperties = {
  color: 'var(--danger)',
  fontSize: '11px',
  marginTop: '4px',
  fontWeight: 500,
};

const modalFooterStyle: React.CSSProperties = {
  padding: '16px 24px',
  borderTop: '1px solid var(--border-color)',
  display: 'flex',
  justifyContent: 'flex-end',
  gap: '12px',
  background: 'rgba(0, 0, 0, 0.1)',
};
