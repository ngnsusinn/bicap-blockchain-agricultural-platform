import React from 'react';
import type { UserSession } from '../utils/auth';

/**
 * Sidebar điều hướng dùng chung cho các portal (Farm Manager, Retailer...).
 *
 * Trước đây mỗi portal tự dựng sidebar/nav riêng nên giao diện lệch nhau
 * (Farm dùng sidebar dọc, Retailer dùng thanh tab ngang). Gom về một component
 * để mọi portal có cùng bố cục, cùng typography và cùng trạng thái active.
 *
 * Lưu ý: danh sách menu dài hơn chiều cao màn hình nên `<nav>` được đặt
 * `overflow-y: auto` + `min-height: 0` để **cuộn được** (trước đây bị cắt, không
 * xem được các mục phía dưới như "Cài Đặt").
 */

export interface PortalNavItem {
  id: string;
  label: string;
  icon: string;
  /** Mục chỉ dùng được khi tài khoản có gói dịch vụ đang hoạt động. */
  isProtected?: boolean;
}

export interface PortalSidebarProps {
  brandLabel: string;
  brandIcon: string;
  /** Màu nhấn của portal (hex) cho trạng thái active. */
  accent: string;
  /** Tông sáng hơn của `accent`, dùng cho chữ/icon. */
  accentSoft: string;
  /** Gradient của logo + avatar. */
  gradient: string;
  menuItems: PortalNavItem[];
  currentTab: string;
  onTabChange: (tab: string) => void;
  hasActiveSubscription?: boolean;
  user?: UserSession | null;
  /** Nút "Edit" cạnh avatar; bỏ trống nếu portal không cần. */
  onEditProfile?: () => void;
}

/** rgba() từ mã hex (#rrggbb) — tránh phải truyền thêm prop độ mờ. */
function withAlpha(hex: string, alpha: number): string {
  const value = hex.replace('#', '');
  const r = parseInt(value.slice(0, 2), 16);
  const g = parseInt(value.slice(2, 4), 16);
  const b = parseInt(value.slice(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

export default function PortalSidebar({
  brandLabel,
  brandIcon,
  accent,
  accentSoft,
  gradient,
  menuItems,
  currentTab,
  onTabChange,
  hasActiveSubscription = true,
  user,
  onEditProfile,
}: PortalSidebarProps) {
  const handleTabClick = (item: PortalNavItem) => {
    if (item.isProtected && !hasActiveSubscription) {
      alert('Bạn cần đăng ký gói dịch vụ để sử dụng tính năng bảo vệ này.');
      return;
    }
    onTabChange(item.id);
  };

  return (
    <aside style={sidebarStyle}>
      <div style={logoContainerStyle}>
        <div style={{ ...logoIconStyle, background: gradient, boxShadow: `0 4px 12px ${withAlpha(accent, 0.4)}` }}>
          {brandIcon}
        </div>
        <span style={logoTextStyle}>{brandLabel}</span>
      </div>

      <nav style={navStyle} aria-label={`Điều hướng ${brandLabel}`}>
        {menuItems.map((item) => {
          const isActive = item.id === currentTab;
          const isLocked = !!item.isProtected && !hasActiveSubscription;

          return (
            <button
              key={item.id}
              type="button"
              onClick={() => handleTabClick(item)}
              aria-current={isActive ? 'page' : undefined}
              style={{
                ...navItemStyle,
                color: isActive ? '#fff' : isLocked ? 'var(--text-muted)' : 'var(--text-secondary)',
                background: isActive ? withAlpha(accent, 0.15) : 'transparent',
                borderLeft: isActive ? `3px solid ${accent}` : '3px solid transparent',
              }}
            >
              <span style={{ fontSize: '18px' }}>{item.icon}</span>
              <span style={{ fontWeight: isActive ? 600 : 400 }}>{item.label}</span>
              {isLocked && <span style={tagStyle}>🔒 VIP</span>}
            </button>
          );
        })}
      </nav>

      <div style={footerStyle}>
        <div style={{ ...farmBadgeStyle, flexDirection: 'column', gap: '8px', padding: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', width: '100%' }}>
            <div
              style={{
                width: '34px',
                height: '34px',
                borderRadius: '50%',
                overflow: 'hidden',
                background: gradient,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                fontWeight: 700,
                fontSize: '14px',
                flexShrink: 0,
              }}
            >
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt="Avatar" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              ) : (
                user?.fullName?.charAt(0)?.toUpperCase() || '👤'
              )}
            </div>

            <div style={{ flex: 1, minWidth: 0, overflow: 'hidden' }}>
              <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.fullName || 'Người dùng'}
              </div>
              <div style={{ fontSize: '10px', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.email || ''}
              </div>
            </div>

            {onEditProfile && (
              <button
                type="button"
                onClick={onEditProfile}
                title="Cập nhật thông tin cá nhân"
                style={{
                  background: withAlpha(accent, 0.15),
                  border: `1px solid ${withAlpha(accent, 0.3)}`,
                  color: accentSoft,
                  borderRadius: '6px',
                  padding: '4px 8px',
                  fontSize: '11px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  transition: 'all 0.2s ease',
                  flexShrink: 0,
                }}
              >
                ✏️ Edit
              </button>
            )}
          </div>
        </div>
        <p style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '12px' }}>BICAP Platform v0.1</p>
      </div>
    </aside>
  );
}

/* ── Styles (giữ nguyên design system của sidebar Farm) ── */
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
  width: '36px',
  height: '36px',
  borderRadius: '10px',
  color: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontWeight: 800,
  fontSize: '18px',
};

const logoTextStyle: React.CSSProperties = {
  fontSize: '18px',
  fontWeight: 800,
  letterSpacing: '-0.5px',
  background: 'linear-gradient(to right, #fff, #06b6d4)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
};

const navStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '4px',
  flex: 1,
  // Menu dài hơn màn hình → cho phép cuộn (minHeight:0 để flex item co lại được).
  minHeight: 0,
  overflowY: 'auto',
  overscrollBehavior: 'contain',
  marginRight: '-6px',
  paddingRight: '6px',
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
  flexShrink: 0,
};

const tagStyle: React.CSSProperties = {
  marginLeft: 'auto',
  fontSize: '9px',
  background: 'rgba(239, 68, 68, 0.1)',
  padding: '2px 6px',
  borderRadius: '10px',
  color: '#ef4444',
  border: '1px solid rgba(239, 68, 68, 0.2)',
};

const footerStyle: React.CSSProperties = {
  marginTop: 'auto',
  paddingLeft: '8px',
  flexShrink: 0,
};

const farmBadgeStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '10px',
  padding: '10px 12px',
  background: 'rgba(255, 255, 255, 0.03)',
  border: '1px solid rgba(255, 255, 255, 0.06)',
  borderRadius: '10px',
};
