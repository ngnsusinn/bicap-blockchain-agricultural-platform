import { useState } from 'react';
import GuestNotifications from './GuestNotifications';
import GuestProductSearch from './GuestProductSearch';
import GuestEducation from './GuestEducation';

/**
 * F4 — Khu vực khách (Guest, chưa đăng nhập).
 *
 * Cung cấp thanh tab riêng với ba mục hoạt động hoàn toàn không cần token:
 *  - Thông báo chung (chỉ thông báo nền tảng do backend trả về cho khách)
 *  - Sản phẩm (tra cứu catalogue công khai)
 *  - Kiến thức (nội dung giáo dục đã xuất bản)
 */

type GuestTab = 'notifications' | 'products' | 'education';

interface GuestAreaProps {
  /** Quay lại màn hình đăng nhập. */
  onLogin: () => void;
}

const TABS: { id: GuestTab; label: string; icon: string }[] = [
  { id: 'notifications', label: 'Thông báo chung', icon: '🔔' },
  { id: 'products', label: 'Sản phẩm', icon: '🔍' },
  { id: 'education', label: 'Kiến thức', icon: '📚' },
];

export default function GuestArea({ onLogin }: GuestAreaProps) {
  const [tab, setTab] = useState<GuestTab>('notifications');

  return (
    <div style={shellStyle}>
      <header style={headerStyle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={logoIconStyle}>B</div>
          <span style={logoTextStyle}>BICAP Platform (Guest)</span>
        </div>
        <button onClick={onLogin} style={loginBtnStyle}>
          🔐 Đăng nhập hệ thống
        </button>
      </header>

      <nav style={tabBarStyle} aria-label="Điều hướng khu vực khách">
        {TABS.map((item) => {
          const active = tab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setTab(item.id)}
              aria-current={active ? 'page' : undefined}
              style={{
                ...tabButtonStyle,
                color: active ? '#fff' : 'var(--text-secondary)',
                background: active
                  ? 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)'
                  : 'rgba(255, 255, 255, 0.04)',
                borderColor: active ? 'transparent' : 'rgba(255, 255, 255, 0.1)',
              }}
            >
              <span style={{ fontSize: 16 }}>{item.icon}</span>
              {item.label}
            </button>
          );
        })}
      </nav>

      <main style={mainStyle}>
        {tab === 'notifications' && <GuestNotifications />}
        {tab === 'products' && <GuestProductSearch />}
        {tab === 'education' && <GuestEducation />}
      </main>
    </div>
  );
}

/* ── Inline Styles ── */
const shellStyle: React.CSSProperties = { minHeight: '100vh', background: '#0b0f17' };
const headerStyle: React.CSSProperties = {
  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
  maxWidth: 1100, margin: '0 auto', padding: '20px 24px 8px',
};
const logoIconStyle: React.CSSProperties = {
  width: 36, height: 36, borderRadius: 10,
  background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
  color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
  fontWeight: 800, fontSize: 18, boxShadow: '0 4px 12px rgba(16, 185, 129, 0.4)',
};
const logoTextStyle: React.CSSProperties = {
  fontSize: 18, fontWeight: 800, letterSpacing: '-0.5px',
  background: 'linear-gradient(to right, #fff, #06b6d4)',
  WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
};
const loginBtnStyle: React.CSSProperties = {
  background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
  border: 'none', color: '#fff', padding: '8px 16px', borderRadius: 8,
  fontWeight: 600, cursor: 'pointer',
};
const tabBarStyle: React.CSSProperties = {
  display: 'flex', gap: 10, flexWrap: 'wrap',
  maxWidth: 1100, margin: '0 auto', padding: '8px 24px 4px',
};
const tabButtonStyle: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 8,
  padding: '10px 18px', borderRadius: 20, border: '1px solid',
  fontSize: 13, fontWeight: 600, cursor: 'pointer', transition: 'all 0.2s ease',
};
const mainStyle: React.CSSProperties = { paddingBottom: 40 };
