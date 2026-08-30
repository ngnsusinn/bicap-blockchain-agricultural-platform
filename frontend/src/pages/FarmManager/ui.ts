import React from 'react';

/**
 * Style dùng chung cho các trang Farm Manager mới (BICAP-9/12→15/19/22/23/27).
 * Tái sử dụng design token của SeasonExports/TradingFloor để giao diện đồng nhất.
 */
export const gridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'minmax(300px, 420px) minmax(360px, 1fr)',
  gap: 24,
  alignItems: 'start',
};

export const gridSingleStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '1fr',
  gap: 24,
  alignItems: 'start',
};

export const panelStyle: React.CSSProperties = { padding: 24 };

export const titleStyle: React.CSSProperties = { color: '#fff', fontSize: 20, margin: '0 0 18px' };

export const labelStyle: React.CSSProperties = {
  display: 'block',
  color: '#cbd5e1',
  fontSize: 13,
  margin: '13px 0 6px',
};

export const inputStyle: React.CSSProperties = {
  width: '100%',
  boxSizing: 'border-box',
  padding: '11px 12px',
  borderRadius: 8,
  border: '1px solid #334155',
  background: '#111827',
  color: '#fff',
};

export const buttonStyle: React.CSSProperties = {
  width: '100%',
  padding: 12,
  marginTop: 20,
  border: 0,
  borderRadius: 8,
  background: '#10b981',
  color: '#fff',
  fontWeight: 700,
  cursor: 'pointer',
};

export const secondaryButtonStyle: React.CSSProperties = {
  padding: '8px 14px',
  border: '1px solid #334155',
  borderRadius: 8,
  background: 'rgba(255,255,255,.04)',
  color: '#cbd5e1',
  fontWeight: 600,
  cursor: 'pointer',
  fontSize: 13,
};

export const alertStyle: React.CSSProperties = {
  padding: 12,
  margin: '12px 0',
  border: '1px solid #ef4444',
  borderRadius: 8,
  color: '#fecaca',
  background: 'rgba(239,68,68,.12)',
};

export const successStyle: React.CSSProperties = {
  padding: 12,
  margin: '12px 0',
  border: '1px solid #10b981',
  borderRadius: 8,
  color: '#a7f3d0',
  background: 'rgba(16,185,129,.12)',
};

export const cardStyle: React.CSSProperties = {
  padding: 16,
  marginTop: 12,
  border: '1px solid #334155',
  borderRadius: 10,
  color: '#cbd5e1',
};

export const hashStyle: React.CSSProperties = {
  fontFamily: 'monospace',
  fontSize: 11,
  overflowWrap: 'anywhere',
  color: '#94a3b8',
};

const STATUS_COLORS: Record<string, { fg: string; bg: string }> = {
  ACTIVE: { fg: '#6ee7b7', bg: 'rgba(16,185,129,.15)' },
  READY: { fg: '#6ee7b7', bg: 'rgba(16,185,129,.15)' },
  DELIVERED: { fg: '#6ee7b7', bg: 'rgba(16,185,129,.15)' },
  RESOLVED: { fg: '#6ee7b7', bg: 'rgba(16,185,129,.15)' },
  IN_PROGRESS: { fg: '#7dd3fc', bg: 'rgba(56,189,248,.15)' },
  SHIPPING: { fg: '#7dd3fc', bg: 'rgba(56,189,248,.15)' },
  PENDING: { fg: '#fcd34d', bg: 'rgba(245,158,11,.15)' },
  PENDING_REVIEW: { fg: '#fcd34d', bg: 'rgba(245,158,11,.15)' },
  OPEN: { fg: '#fcd34d', bg: 'rgba(245,158,11,.15)' },
  HARVESTED: { fg: '#c4b5fd', bg: 'rgba(139,92,246,.15)' },
  RETURNED: { fg: '#fca5a5', bg: 'rgba(239,68,68,.15)' },
  REJECTED: { fg: '#fca5a5', bg: 'rgba(239,68,68,.15)' },
  CANCELLED: { fg: '#fca5a5', bg: 'rgba(239,68,68,.15)' },
  INACTIVE: { fg: '#94a3b8', bg: 'rgba(148,163,184,.15)' },
};

export const badgeStyle = (status?: string): React.CSSProperties => {
  const c = (status && STATUS_COLORS[status]) || { fg: '#94a3b8', bg: 'rgba(148,163,184,.15)' };
  return {
    fontSize: 11,
    padding: '4px 8px',
    borderRadius: 999,
    color: c.fg,
    background: c.bg,
    whiteSpace: 'nowrap',
  };
};
