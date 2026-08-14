import React from 'react';
import type { UserSession, PortalType } from '../types';

// ── Menu configurations per portal ──
const MENU_ITEMS: Record<PortalType, { id: string; label: string; icon: string; disabled?: boolean }[]> = {
  admin: [
    { id: 'overview', label: 'Dashboard Overview', icon: '📊' },
    { id: 'admins', label: 'Admin Management', icon: '👤' },
    { id: 'farms', label: 'Farm Approval', icon: '🚜' },
    { id: 'farmmgmt', label: 'Farm Management', icon: '🏞️' },
    { id: 'contracts', label: 'Smart Contracts', icon: '⛓️' },
    { id: 'products', label: 'Giám Sát Sản Phẩm', icon: '📦' },
    { id: 'iot', label: 'IoT Sensoring', icon: '🌡️', disabled: true },
    { id: 'settings', label: 'System Settings', icon: '⚙️', disabled: true },
  ],
  farm: [
    { id: 'overview', label: 'Tổng Quan', icon: '📊' },
    { id: 'crops', label: 'Quản Lý Mùa Vụ', icon: '🌱' },
    { id: 'iot', label: 'Cảm Biến IoT', icon: '🌡️', disabled: true },
    { id: 'qr', label: 'Mã QR Blockchain', icon: '🔗', disabled: true },
    { id: 'harvest', label: 'Nhật Ký Thu Hoạch', icon: '📋', disabled: true },
    { id: 'settings', label: 'Cài Đặt', icon: '⚙️', disabled: true },
  ],
  retail: [
    { id: 'overview', label: 'Tổng Quan', icon: '📊' },
    { id: 'products', label: 'Tìm Nông Sản', icon: '🔍', disabled: true },
    { id: 'orders', label: 'Đơn Hàng', icon: '📦', disabled: true },
    { id: 'shipping', label: 'Vận Chuyển', icon: '🚚', disabled: true },
    { id: 'contracts', label: 'Hợp Đồng', icon: '📝', disabled: true },
    { id: 'settings', label: 'Cài Đặt', icon: '⚙️', disabled: true },
  ],
};

const PORTAL_LABELS: Record<PortalType, string> = {
  admin: 'BICAP Admin',
  farm: 'BICAP Farm',
  retail: 'BICAP Retail',
};

const PORTAL_COLORS: Record<PortalType, { gradient: string; glow: string; accent: string }> = {
  admin: { gradient: 'linear-gradient(135deg, #8b5cf6, #6366f1)', glow: 'rgba(139,92,246,0.4)', accent: '#a78bfa' },
  farm: { gradient: 'linear-gradient(135deg, #059669, #10b981)', glow: 'rgba(16,185,129,0.4)', accent: '#34d399' },
  retail: { gradient: 'linear-gradient(135deg, #0284c7, #06b6d4)', glow: 'rgba(6,182,212,0.4)', accent: '#38bdf8' },
};

interface SidebarProps {
  portalType: PortalType;
  currentTab: string;
  onTabChange: (tab: string) => void;
  currentSession: UserSession;
  onLogout: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  portalType,
  currentTab,
  onTabChange,
  currentSession,
  onLogout,
}) => {
  const menuItems = MENU_ITEMS[portalType];
  const colors = PORTAL_COLORS[portalType];

  return (
    <aside style={sidebarStyle}>
      <div style={logoContainerStyle}>
        <div style={{ ...logoIconStyle, background: colors.gradient, boxShadow: `0 4px 10px ${colors.glow}` }}>
          B
        </div>
        <span style={{ ...logoTextStyle, background: `linear-gradient(to right, #fff, ${colors.accent})`, WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          {PORTAL_LABELS[portalType]}
        </span>
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
                background: isActive ? `${colors.accent}22` : 'transparent',
                borderLeft: isActive ? `3px solid ${colors.accent}` : '3px solid transparent',
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
      <div style={userCardStyle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
          <div style={{ ...userAvatarStyle, background: colors.gradient }}>
            {currentSession.fullName.charAt(0)}
          </div>
          <div style={{ overflow: 'hidden' }}>
            <div style={{ fontWeight: 600, fontSize: '13px', color: '#fff', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {currentSession.fullName}
            </div>
            <div style={{ fontSize: '11px', color: colors.accent, fontWeight: 600 }}>
              {currentSession.role}
            </div>
          </div>
        </div>
        <button onClick={onLogout} style={logoutBtnStyle}>
          🚪 Đăng xuất
        </button>
      </div>

      <div style={footerStyle}>
        <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>BICAP Platform v1.0</p>
      </div>
    </aside>
  );
};

const sidebarStyle: React.CSSProperties = {
  width: 'var(--sidebar-width)', position: 'fixed', top: 0, left: 0, bottom: 0,
  background: 'rgba(15, 16, 22, 0.95)', borderRight: '1px solid var(--border-color)',
  display: 'flex', flexDirection: 'column', zIndex: 1000, padding: '24px 16px',
};

const logoContainerStyle: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '40px', paddingLeft: '8px',
};

const logoIconStyle: React.CSSProperties = {
  width: '32px', height: '32px', borderRadius: '8px', color: '#fff',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  fontWeight: 800, fontSize: '18px',
};

const logoTextStyle: React.CSSProperties = {
  fontSize: '18px', fontWeight: 800, letterSpacing: '-0.5px',
};

const navStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 };

const navItemStyle: React.CSSProperties = {
  width: '100%', padding: '12px 16px', borderRadius: '0 8px 8px 0',
  display: 'flex', alignItems: 'center', gap: '12px', textAlign: 'left',
  transition: 'all 0.2s ease', fontSize: '14px',
};

const tagStyle: React.CSSProperties = {
  marginLeft: 'auto', fontSize: '9px', background: 'rgba(255,255,255,0.05)',
  padding: '2px 6px', borderRadius: '10px', color: 'var(--text-muted)',
  border: '1px solid rgba(255,255,255,0.05)',
};

const userCardStyle: React.CSSProperties = {
  padding: '12px', borderRadius: '10px', background: 'rgba(255,255,255,0.03)',
  border: '1px solid rgba(255,255,255,0.08)', marginBottom: '16px',
};

const userAvatarStyle: React.CSSProperties = {
  width: '32px', height: '32px', borderRadius: '50%', color: '#fff',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  fontWeight: 700, fontSize: '14px', flexShrink: 0,
};

const logoutBtnStyle: React.CSSProperties = {
  width: '100%', padding: '8px', borderRadius: '6px',
  background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.3)',
  color: '#fca5a5', fontSize: '12px', fontWeight: 600, cursor: 'pointer',
  textAlign: 'center', marginTop: '4px', transition: 'all 0.2s ease',
};

const footerStyle: React.CSSProperties = { marginTop: 'auto', paddingLeft: '8px' };
