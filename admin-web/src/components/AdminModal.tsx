import React, { useState, useEffect } from 'react';

interface AdminModalProps {
  admin: any | null; // Null if creating
  onClose: () => void;
  onSave: (adminData: any) => void;
}

const ALL_PERMISSIONS = [
  { code: 'ADMIN_CREATE', name: 'Create Admins', desc: 'Allows creating new administrators' },
  { code: 'ADMIN_READ', name: 'Read Admins', desc: 'Allows viewing admin accounts and details' },
  { code: 'ADMIN_UPDATE', name: 'Update Admins', desc: 'Allows editing roles, details, or status' },
  { code: 'ADMIN_DELETE', name: 'Delete Admins', desc: 'Allows soft-deleting administrator profiles' }
];

const DEFAULT_ROLE_PERMISSIONS: Record<string, string[]> = {
  SUPER_ADMIN: ['ADMIN_CREATE', 'ADMIN_READ', 'ADMIN_UPDATE', 'ADMIN_DELETE'],
  ADMIN: ['ADMIN_READ', 'ADMIN_UPDATE'],
  MODERATOR: ['ADMIN_READ'],
};

export const AdminModal: React.FC<AdminModalProps> = ({ admin, onClose, onSave }) => {
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

  // Sync state if edit mode
  const resolveAdminRole = (adminData: any) => {
    if (!adminData?.roles?.length) {
      return 'ADMIN';
    }
    return adminData.roles[0].name || 'ADMIN';
  };

  const resolveAdminPermissions = (adminData: any) => {
    if (!adminData?.roles?.length) {
      return [];
    }
    return adminData.roles.flatMap((role: any) =>
      role.permissions?.map((perm: any) => perm.code) || []
    );
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
      setSelectedPermissions(DEFAULT_ROLE_PERMISSIONS.ADMIN);
    }
    setErrors({});
  }, [admin]);

  // Autofill permissions when role changes
  const handleRoleChange = (selectedRole: string) => {
    setRole(selectedRole);
    if (DEFAULT_ROLE_PERMISSIONS[selectedRole]) {
      setSelectedPermissions(DEFAULT_ROLE_PERMISSIONS[selectedRole]);
    }
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

  const handleSubmit = (e: React.FormEvent) => {
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

    onSave(data);
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

            {/* Permissions */}
            <div style={fieldGroupStyle}>
              <label style={labelStyle}>Role Permissions (Fine-grained RBAC override)</label>
              <div style={permissionsListStyle}>
                {ALL_PERMISSIONS.map((perm) => {
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
                        <div style={{ fontWeight: 600, color: '#fff', fontSize: '13px' }}>{perm.name}</div>
                        <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '2px' }}>
                          {perm.desc}
                        </div>
                      </div>
                    </label>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div style={modalFooterStyle}>
            <button type="button" onClick={onClose} className="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              {isEdit ? 'Save Changes' : 'Create Admin'}
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
