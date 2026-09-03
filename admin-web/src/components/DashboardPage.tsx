import React, { useState, useEffect, useCallback } from 'react';
import type { UserSession, AdminDashboard } from '../types';
import { API_ORIGIN, authHeaders, formatDate } from '../utils/api';
import { StatusBadge } from './StatusBadge';

interface DashboardPageProps {
  currentSession: UserSession;
  onToast: (text: string, type?: 'info' | 'success' | 'error') => void;
  onNavigateTab: (tab: string) => void;
}

const TX_STATUS_META: Record<string, { label: string; color: string; bg: string }> = {
  CONFIRMED: { label: 'Đã xác nhận', color: '#34d399', bg: 'rgba(52,211,153,0.12)' },
  PENDING: { label: 'Chờ xác nhận', color: '#fbbf24', bg: 'rgba(251,191,36,0.12)' },
  FAILED: { label: 'Lỗi', color: '#f87171', bg: 'rgba(248,113,113,0.12)' },
};

export const DashboardPage: React.FC<DashboardPageProps> = ({ currentSession, onToast, onNavigateTab }) => {
  const [data, setData] = useState<AdminDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/dashboard`, { headers: authHeaders(currentSession.email) });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || 'Không tải được số liệu dashboard.'); }
      setData(await res.json());
    } catch (err: any) {
      onToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentSession]);

  useEffect(() => { load(); }, [load]);

  const farms = data?.farms || {};
  const products = data?.products || {};
  const orders = data?.orders || {};
  const reports = data?.reports || {};

  return (
    <div>
      <h1 className="dashboard-title">Dashboard Overview</h1>
      <p className="dashboard-subtitle">
        Tổng quan vận hành nền tảng BICAP — tài khoản, nông trại, sản phẩm, đơn hàng, báo cáo và nhật ký blockchain.
      </p>

      {/* Stat cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', margin: '24px 0' }}>
        <StatCard label="Tài khoản Admin" value={data?.admins} sub="SUPER_ADMIN · ADMIN · MODERATOR" accent="#a78bfa" onClick={() => onNavigateTab('admins')} />
        <StatCard label="Nông trại" value={farms.TOTAL} sub={`Chờ duyệt: ${farms.PENDING ?? 0} · Hoạt động: ${farms.APPROVED ?? 0}`} accent="#34d399" onClick={() => onNavigateTab('farms')} />
        <StatCard label="Sản phẩm" value={products.TOTAL} sub={`Đang bán: ${products.ACTIVE ?? 0} · Chờ duyệt: ${products.PENDING_REVIEW ?? 0}`} accent="#38bdf8" onClick={() => onNavigateTab('products')} />
        <StatCard label="Đơn hàng" value={orders.TOTAL} sub={`Vận chuyển: ${orders.SHIPPING ?? 0} · Hoàn thành: ${orders.COMPLETED ?? 0}`} accent="#fbbf24" />
        <StatCard label="Báo cáo người dùng" value={reports.TOTAL} sub={`Mới: ${reports.OPEN ?? 0} · Đang xử lý: ${reports.IN_PROGRESS ?? 0}`} accent="#f87171" onClick={() => onNavigateTab('reports')} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px', alignItems: 'start' }}>
        {/* Pending approvals */}
        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2 style={{ margin: 0, color: '#fff', fontSize: '16px', fontWeight: 700 }}>⏳ Hồ sơ chờ duyệt</h2>
            <button onClick={() => onNavigateTab('farms')} style={linkBtnStyle}>Xem tất cả →</button>
          </div>
          {loading && <p style={mutedStyle}>Đang tải…</p>}
          {!loading && (data?.pendingFarms.length === 0) && <p style={mutedStyle}>Không có hồ sơ chờ duyệt 🎉</p>}
          {data?.pendingFarms.map((f) => (
            <div key={f.id} style={rowStyle}>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontWeight: 600, fontSize: '13px', color: '#fff', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{f.name}</div>
                <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{f.ownerName || '—'} · {formatDate(f.createdAt)} · {f.certificationCount > 0 ? `${f.certificationCount} chứng nhận` : 'Thiếu hồ sơ ⚠️'}</div>
              </div>
              <StatusBadge status={f.status} />
            </div>
          ))}
        </div>

        {/* Recent blockchain activity */}
        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2 style={{ margin: 0, color: '#fff', fontSize: '16px', fontWeight: 700 }}>⛓️ Nhật ký blockchain gần đây</h2>
            <button onClick={() => onNavigateTab('contracts')} style={linkBtnStyle}>Smart Contracts →</button>
          </div>
          {loading && <p style={mutedStyle}>Đang tải…</p>}
          {!loading && (data?.recentTransactions.length === 0) && <p style={mutedStyle}>Chưa có giao dịch nào.</p>}
          {data?.recentTransactions.map((t) => {
            const meta = TX_STATUS_META[t.status] || TX_STATUS_META.PENDING;
            return (
              <div key={t.id} style={rowStyle}>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 600, fontSize: '13px', color: '#fff' }}>{t.entityType} #{t.entityId}</div>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'monospace', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '260px' }}>{t.txHash || '(chưa có hash)'}</div>
                </div>
                <span style={{ padding: '3px 10px', borderRadius: '10px', fontSize: '11px', fontWeight: 600, color: meta.color, background: meta.bg, whiteSpace: 'nowrap' }}>{meta.label}</span>
              </div>
            );
          })}
        </div>
      </div>

      {loading && (
        <div style={{ marginTop: '16px', color: 'var(--text-muted)', fontSize: '13px' }}>Đang nạp số liệu…</div>
      )}
    </div>
  );
};

const StatCard: React.FC<{ label: string; value?: number; sub?: string; accent: string; onClick?: () => void }> = ({ label, value, sub, accent, onClick }) => (
  <div
    onClick={onClick}
    style={{
      padding: '18px', borderRadius: '12px', background: 'rgba(255,255,255,0.03)',
      border: '1px solid var(--border-color)', cursor: onClick ? 'pointer' : 'default',
      transition: 'border-color 0.2s ease',
    }}
  >
    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>{label}</div>
    <div style={{ fontSize: '28px', fontWeight: 800, color: accent }}>{value ?? '—'}</div>
    {sub && <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '4px' }}>{sub}</div>}
  </div>
);

const rowStyle: React.CSSProperties = {
  display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px',
  padding: '10px 12px', borderRadius: '8px', background: 'rgba(255,255,255,0.02)',
  border: '1px solid rgba(255,255,255,0.05)', marginBottom: '8px',
};
const linkBtnStyle: React.CSSProperties = {
  background: 'none', border: 'none', color: '#a78bfa', fontSize: '12px', fontWeight: 600, cursor: 'pointer',
};
const mutedStyle: React.CSSProperties = { color: 'var(--text-muted)', fontSize: '13px' };
