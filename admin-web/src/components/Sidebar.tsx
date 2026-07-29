import React from 'react';
import type { UserSession } from '../types';

interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
  currentSession?: UserSession;
  onLogout?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, onTabChange, currentSession, onLogout }) => {
  const menuItems = [
    { id: 'overview', label: 'Dashboard Overview', icon: '📊' },
    { id: 'admins', label: 'Admin Management', icon: '👤' },
    { id: 'farms', label: 'Farm Approval', icon: '🚜', disabled: true },
    { id: 'products', label: 'Traceability & QR', icon: '🔍', disabled: true },
    { id: 'iot', label: 'IoT Sensoring', icon: '🌡️', disabled: true },
    { id: 'settings', label: 'System Settings', icon: '⚙️', disabled: true },
  ];

  return (
    <aside style={sidebarStyle}>
      <div style={logoContainerStyle}>
        <div style={logoIconStyle}>B</div>
        <span style={logoTextStyle}>BICAP Admin</span>
      </div>
      <nav style={navStyle}>
        {menuItems.map((item) => {
          const isActive = item.id === currentTab;
          return (
            <button
              key={item.id}
              onClick={() => !item.disabled && onTabChange(item.id)}
              disabled={item.disabled}
              style={{
                ...navItemStyle,
                color: isActive ? '#fff' : item.disabled ? 'var(--text-muted)' : 'var(--text-secondary)',
                background: isActive ? 'var(--primary-light)' : 'transparent',
                borderLeft: isActive ? '3px solid var(--primary)' : '3px solid transparent',
                cursor: item.disabled ? 'not-allowed' : 'pointer',
              }}
            >
              <span style={{ fontSize: '18px' }}>{item.icon}</span>
              <span style={{ fontWeight: isActive ? 600 : 400 }}>{item.label}</span>
              {item.disabled && <span style={tagStyle}>Soon</span>}
            </button>
          );
        })}
      </nav>

      {/* User profile & Logout button */}
      {currentSession && (
        <div style={userCardStyle}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <div style={userAvatarStyle}>{currentSession.fullName.charAt(0)}</div>
            <div style={{ overflow: 'hidden' }}>
              <div style={{ fontWeight: 600, fontSize: '13px', color: '#fff', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {currentSession.fullName}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--primary-hover)', fontWeight: 600 }}>
                {currentSession.role}
              </div>
            </div>
          </div>
          {onLogout && (
            <button onClick={onLogout} style={logoutBtnStyle}>
              🚪 Sign Out / Logout
            </button>
          )}
        </div>
      )}

      <div style={footerStyle}>
        <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>BICAP Platform v0.1</p>
        <p style={{ fontSize: '10px', color: 'var(--text-muted)', marginTop: '2px' }}>IntelliJ Synced</p>
      </div>
    </aside>
  );
};

const userCardStyle: React.CSSProperties = {
  padding: '12px',
  borderRadius: '10px',
  background: 'rgba(255, 255, 255, 0.03)',
  border: '1px solid rgba(255, 255, 255, 0.08)',
  marginBottom: '16px',
};

const userAvatarStyle: React.CSSProperties = {
  width: '32px',
  height: '32px',
  borderRadius: '50%',
  background: 'var(--primary)',
  color: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontWeight: 700,
  fontSize: '14px',
  flexShrink: 0,
};

const logoutBtnStyle: React.CSSProperties = {
  width: '100%',
  padding: '8px',
  borderRadius: '6px',
  background: 'rgba(239, 68, 68, 0.15)',
  border: '1px solid rgba(239, 68, 68, 0.3)',
  color: '#fca5a5',
  fontSize: '12px',
  fontWeight: 600,
  cursor: 'pointer',
  textAlign: 'center',
  marginTop: '4px',
  transition: 'all 0.2s ease',
};

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
  width: '32px',
  height: '32px',
  borderRadius: '8px',
  background: 'linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%)',
  color: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontWeight: '800',
  fontSize: '18px',
  boxShadow: '0 4px 10px rgba(139, 92, 246, 0.4)',
};

const logoTextStyle: React.CSSProperties = {
  fontSize: '18px',
  fontWeight: '800',
  letterSpacing: '-0.5px',
  background: 'linear-gradient(to right, #fff, #a78bfa)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
};

const navStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '8px',
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
  background: 'rgba(255, 255, 255, 0.05)',
  padding: '2px 6px',
  borderRadius: '10px',
  color: 'var(--text-muted)',
  border: '1px solid rgba(255, 255, 255, 0.05)',
};

const footerStyle: React.CSSProperties = {
  marginTop: 'auto',
  paddingLeft: '8px',
};
