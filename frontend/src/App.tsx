import React, { useState, useEffect } from 'react';
import ServicePackages from './pages/FarmManager/ServicePackages';

const API_BASE_URL = 'http://localhost:8080/api';

/* ── Sidebar Component ── */
interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
  hasActiveSubscription: boolean;
}

const Sidebar: React.FC<SidebarProps> = ({ currentTab, onTabChange, hasActiveSubscription }) => {
  // 1. Mở khóa các tab (Bỏ disabled: true)
  const menuItems = [
    { id: 'dashboard', label: 'Dashboard', icon: '📊', isProtected: false },
    { id: 'packages', label: 'Service Packages', icon: '📦', isProtected: false },
    { id: 'farm-info', label: 'Farm Information', icon: '🌾', isProtected: false },
    { id: 'products', label: 'Products & QR', icon: '🔍', isProtected: true },
    { id: 'iot', label: 'IoT Monitoring', icon: '🌡️', isProtected: true },
    { id: 'certificates', label: 'Certificates', icon: '📜', isProtected: true },
    { id: 'settings', label: 'Settings', icon: '⚙️', isProtected: false },
  ];

  const handleTabClick = (item: typeof menuItems[0]) => {
    // 2. Logic Bảo vệ Route (Subscription Guard)
    if (item.isProtected && !hasActiveSubscription) {
      alert('Bạn cần đăng ký gói dịch vụ để sử dụng tính năng này');
      return;
    }
    onTabChange(item.id);
  };

  return (
    <aside style={sidebarStyle}>
      <div style={logoContainerStyle}>
        <div style={logoIconStyle}>B</div>
        <span style={logoTextStyle}>BICAP Farm</span>
      </div>
      <nav style={navStyle}>
        {menuItems.map((item) => {
          const isActive = item.id === currentTab;
          const isLocked = item.isProtected && !hasActiveSubscription;

          return (
            <button
              key={item.id}
              onClick={() => handleTabClick(item)}
              style={{
                ...navItemStyle,
                color: isActive ? '#fff' : isLocked ? 'var(--text-muted)' : 'var(--text-secondary)',
                background: isActive ? 'var(--primary-light)' : 'transparent',
                borderLeft: isActive ? '3px solid var(--primary)' : '3px solid transparent',
                cursor: 'pointer',
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
        <div style={farmBadgeStyle}>
          <span style={{ fontSize: '14px' }}>🌱</span>
          <div>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)' }}>My Farm</div>
            <div style={{ fontSize: '10px', color: 'var(--text-muted)' }}>Farm ID: 1</div>
          </div>
        </div>
        <p style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '12px' }}>BICAP Platform v0.1</p>
        <p style={{ fontSize: '10px', color: 'var(--text-muted)', marginTop: '2px' }}>Farm Manager Portal</p>
      </div>
    </aside>
  );
};

/* ── Sidebar Styles ── */
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
  background: 'linear-gradient(135deg, #8b5cf6 0%, #06b6d4 100%)',
  color: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontWeight: 800,
  fontSize: '18px',
  boxShadow: '0 4px 12px rgba(139, 92, 246, 0.4)',
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
  background: 'rgba(239, 68, 68, 0.1)',
  padding: '2px 6px',
  borderRadius: '10px',
  color: '#ef4444',
  border: '1px solid rgba(239, 68, 68, 0.2)',
};

const footerStyle: React.CSSProperties = {
  marginTop: 'auto',
  paddingLeft: '8px',
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

/* ── Main App Component ── */
export default function App() {
  const [currentTab, setCurrentTab] = useState('packages');
  const [hasActiveSubscription, setHasActiveSubscription] = useState(false);
  const farmId = 1;

  // Fetch trạng thái đăng ký của Farm để bảo vệ các tab nâng cao
  useEffect(() => {
    const checkSubscription = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/subscriptions/farm/${farmId}`);
        if (res.ok) {
          const subData = await res.json();
          const activeSub = Array.isArray(subData) 
            ? subData.find((s: any) => s.status === 'ACTIVE')
            : (subData?.status === 'ACTIVE' ? subData : null);
            
          setHasActiveSubscription(!!activeSub);
        }
      } catch (err) {
        setHasActiveSubscription(false);
      }
    };

    checkSubscription();
  }, [currentTab]);

  return (
    <div className="app-container">
      <Sidebar 
        currentTab={currentTab} 
        onTabChange={setCurrentTab} 
        hasActiveSubscription={hasActiveSubscription} 
      />

      <main className="main-content animate-fade-in">
        {currentTab === 'packages' && <ServicePackages />}

        {currentTab === 'dashboard' && (
          <div>
            <h1 className="dashboard-title">Farm Dashboard</h1>
            <p className="dashboard-subtitle">Overview of your farm operations and blockchain activity.</p>
            <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
              <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Farm Dashboard</h2>
              <p style={{ color: 'var(--text-secondary)', marginTop: '8px', maxWidth: '500px', marginInline: 'auto', fontSize: '14px', lineHeight: 1.6 }}>
                Chào mừng bạn đến với bảng điều khiển quản lý nông trại.
              </p>
            </div>
          </div>
        )}

        {currentTab === 'farm-info' && (
          <div>
            <h1 className="dashboard-title">Farm Information</h1>
            <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌾</div>
              <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Thông Tin Nông Trại</h2>
            </div>
          </div>
        )}

        {currentTab === 'products' && (
          <div>
            <h1 className="dashboard-title">Products & QR Code</h1>
            <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
              <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Quản lý sản phẩm & QR Blockchain</h2>
            </div>
          </div>
        )}

        {currentTab === 'iot' && (
          <div>
            <h1 className="dashboard-title">IoT Monitoring</h1>
            <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌡️</div>
              <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 700 }}>Giám sát thiết bị Cảm biến IoT</h2>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}