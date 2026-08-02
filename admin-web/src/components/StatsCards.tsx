import React from 'react';
import type { AdminUser } from '../types';

interface StatsProps {
  admins: AdminUser[];
}

export const StatsCards: React.FC<StatsProps> = ({ admins }) => {
  const total = admins.length;
  const active = admins.filter(a => a.status === 'ACTIVE').length;
  const suspended = admins.filter(a => a.status === 'SUSPENDED').length;
  const inactive = admins.filter(a => a.status === 'INACTIVE').length;

  const cards = [
    { title: 'Total Admins', count: total, color: 'var(--primary)', glow: 'var(--primary-glow)', desc: 'Registered system profiles' },
    { title: 'Active Accounts', count: active, color: 'var(--success)', glow: 'var(--success-glow)', desc: 'Active & operational' },
    { title: 'Suspended Accounts', count: suspended, color: 'var(--warning)', glow: 'var(--warning-light)', desc: 'Temporarily locked' },
    { title: 'Soft Deleted (Inactive)', count: inactive, color: 'var(--danger)', glow: 'var(--danger-glow)', desc: 'Soft-deleted admins' },
  ];

  return (
    <div style={gridStyle}>
      {cards.map((card, i) => (
        <div key={i} className="glass-panel pulse-on-hover" style={cardStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <span style={{ fontSize: '13px', color: 'var(--text-secondary)', fontWeight: 500 }}>{card.title}</span>
            <div style={{ ...dotStyle, background: card.color, boxShadow: `0 0 8px ${card.glow}` }} />
          </div>
          <div style={countStyle}>{card.count}</div>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{card.desc}</span>
        </div>
      ))}
    </div>
  );
};

const gridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
  gap: '20px',
  marginBottom: '32px',
};

const cardStyle: React.CSSProperties = {
  padding: '24px',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'space-between',
  height: '140px',
};

const dotStyle: React.CSSProperties = {
  width: '8px',
  height: '8px',
  borderRadius: '50%',
};

const countStyle: React.CSSProperties = {
  fontSize: '36px',
  fontWeight: 800,
  color: '#fff',
  margin: '12px 0 4px',
};
