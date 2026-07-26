import React from 'react';

export interface UserSession {
  email: string;
  fullName: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'MODERATOR' | 'GUEST';
  permissions: string[];
}

const mockSessions: UserSession[] = [
  {
    email: 'superadmin@bicap.com',
    fullName: 'Trần Nguyễn Gia Bảo (Super Admin)',
    role: 'SUPER_ADMIN',
    permissions: ['ADMIN_CREATE', 'ADMIN_READ', 'ADMIN_UPDATE', 'ADMIN_DELETE'],
  },
  {
    email: 'admin@bicap.com',
    fullName: 'Lê Minh Tuấn (Admin)',
    role: 'ADMIN',
    permissions: ['ADMIN_READ', 'ADMIN_UPDATE'],
  },
  {
    email: 'moderator@bicap.com',
    fullName: 'Nguyễn Thị Hoa (Moderator)',
    role: 'MODERATOR',
    permissions: ['ADMIN_READ'],
  },
  {
    email: 'unauthorized@bicap.com',
    fullName: 'Kẻ Xâm Nhập (Unauthorized)',
    role: 'GUEST',
    permissions: [],
  }
];

interface LoginSimulatorProps {
  currentSession: UserSession;
  onSessionChange: (session: UserSession) => void;
}

export const LoginSimulator: React.FC<LoginSimulatorProps> = ({ currentSession, onSessionChange }) => {
  return (
    <div className="glass-panel" style={cardStyle}>
      <div style={headerStyle}>
        <div style={dotStyle}></div>
        <h3 style={titleStyle}>Simulated Session (RBAC Quick Selector)</h3>
      </div>
      <p style={descStyle}>
        Toggle below to simulate different logged-in administrators and watch the backend and UI permissions automatically adapt.
      </p>
      <div style={selectorContainerStyle}>
        {mockSessions.map((session) => {
          const isSelected = session.email === currentSession.email;
          return (
            <button
              key={session.email}
              onClick={() => onSessionChange(session)}
              style={{
                ...btnStyle,
                border: isSelected ? '1px solid var(--primary)' : '1px solid var(--border-color)',
                background: isSelected ? 'var(--primary-light)' : 'rgba(0, 0, 0, 0.2)',
                color: isSelected ? 'var(--primary-hover)' : 'var(--text-secondary)',
                boxShadow: isSelected ? '0 0 10px rgba(139, 92, 246, 0.2)' : 'none',
              }}
            >
              <div style={{ fontWeight: 600, fontSize: '13px' }}>{session.role}</div>
              <div style={{ fontSize: '11px', opacity: 0.8, marginTop: '2px' }}>{session.email}</div>
            </button>
          );
        })}
      </div>
      <div style={infoBannerStyle}>
        <span style={{ color: 'var(--primary)' }}>★ Current Actor:</span>
        <strong style={{ color: '#fff', marginLeft: '6px' }}>{currentSession.fullName}</strong>
        <span style={badgeStyle}>{currentSession.role}</span>
        <div style={permsListStyle}>
          <span style={{ color: 'var(--text-muted)' }}>Permissions:</span>
          {currentSession.permissions.length === 0 ? (
            <span style={{ color: 'var(--danger)', fontSize: '12px', marginLeft: '6px' }}>NONE (ACCESS DENIED)</span>
          ) : (
            currentSession.permissions.map((p) => (
              <span key={p} style={permBadgeStyle}>{p}</span>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

const cardStyle: React.CSSProperties = {
  padding: '20px',
  marginBottom: '24px',
  background: 'rgba(26, 27, 38, 0.6)',
  border: '1px solid rgba(255, 255, 255, 0.05)',
};

const headerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '8px',
  marginBottom: '8px',
};

const dotStyle: React.CSSProperties = {
  width: '8px',
  height: '8px',
  borderRadius: '50%',
  background: 'var(--primary)',
  boxShadow: '0 0 8px var(--primary)',
};

const titleStyle: React.CSSProperties = {
  fontSize: '15px',
  fontWeight: 600,
  color: '#fff',
};

const descStyle: React.CSSProperties = {
  fontSize: '13px',
  color: 'var(--text-secondary)',
  marginBottom: '16px',
  lineHeight: '1.4',
};

const selectorContainerStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
  gap: '12px',
  marginBottom: '16px',
};

const btnStyle: React.CSSProperties = {
  padding: '12px',
  borderRadius: '8px',
  textAlign: 'left',
  transition: 'all 0.2s ease',
  cursor: 'pointer',
};

const infoBannerStyle: React.CSSProperties = {
  padding: '12px 16px',
  background: 'rgba(0, 0, 0, 0.3)',
  borderRadius: '8px',
  fontSize: '13px',
  display: 'flex',
  flexWrap: 'wrap',
  alignItems: 'center',
  gap: '8px',
};

const badgeStyle: React.CSSProperties = {
  padding: '2px 8px',
  borderRadius: '12px',
  fontSize: '11px',
  fontWeight: 700,
  background: 'var(--primary-light)',
  color: 'var(--primary-hover)',
  border: '1px solid rgba(139, 92, 246, 0.3)',
  marginLeft: '4px',
};

const permsListStyle: React.CSSProperties = {
  display: 'flex',
  flexWrap: 'wrap',
  alignItems: 'center',
  gap: '6px',
  width: '100%',
  marginTop: '8px',
  paddingTop: '8px',
  borderTop: '1px solid rgba(255, 255, 255, 0.05)',
};

const permBadgeStyle: React.CSSProperties = {
  padding: '1px 6px',
  borderRadius: '4px',
  fontSize: '10px',
  background: 'rgba(255, 255, 255, 0.05)',
  border: '1px solid rgba(255, 255, 255, 0.1)',
  color: 'var(--text-secondary)',
};
