import React, { useState, useEffect, useCallback } from 'react';
import type { UserSession, ReportItem, ReportStats } from '../types';
import { API_ORIGIN, authHeaders, formatDate } from '../utils/api';

interface ReportsPageProps {
  currentSession: UserSession;
  onToast: (text: string, type?: 'info' | 'success' | 'error') => void;
}

const TABS = [
  { id: '', label: '📋 Tất Cả' },
  { id: 'OPEN', label: '🟡 Mới' },
  { id: 'IN_PROGRESS', label: '🔵 Đang Xử Lý' },
  { id: 'RESOLVED', label: '🟢 Đã Giải Quyết' },
  { id: 'REJECTED', label: '🔴 Bị Từ Chối' },
] as const;

const STATUS_META: Record<string, { label: string; color: string; bg: string }> = {
  OPEN: { label: 'Mới', color: '#fbbf24', bg: 'rgba(251,191,36,0.12)' },
  IN_PROGRESS: { label: 'Đang xử lý', color: '#38bdf8', bg: 'rgba(56,189,248,0.12)' },
  RESOLVED: { label: 'Đã giải quyết', color: '#34d399', bg: 'rgba(52,211,153,0.12)' },
  REJECTED: { label: 'Bị từ chối', color: '#f87171', bg: 'rgba(248,113,113,0.12)' },
};

const TYPE_LABELS: Record<string, string> = {
  COMPLAINT: 'Khiếu nại', FEEDBACK: 'Phản hồi', INCIDENT: 'Sự cố', OTHER: 'Khác',
};

const HANDLE_OPTIONS = [
  { id: 'IN_PROGRESS', label: '🔵 Đánh dấu đang xử lý' },
  { id: 'RESOLVED', label: '🟢 Đã giải quyết' },
  { id: 'REJECTED', label: '🔴 Từ chối' },
];

export const ReportsPage: React.FC<ReportsPageProps> = ({ currentSession, onToast }) => {
  const [tab, setTab] = useState<string>('OPEN');
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [stats, setStats] = useState<ReportStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<ReportItem | null>(null);
  const [handleStatus, setHandleStatus] = useState('RESOLVED');
  const [adminResponse, setAdminResponse] = useState('');
  const [saving, setSaving] = useState(false);

  const canHandle = currentSession.role === 'SUPER_ADMIN' || currentSession.role === 'ADMIN';

  const fetchStats = useCallback(async () => {
    try {
      const res = await fetch(`${API_ORIGIN}/api/reports/admin/stats`, { headers: authHeaders(currentSession.email) });
      if (res.ok) setStats(await res.json());
    } catch { /* non-fatal */ }
  }, [currentSession]);

  const fetchReports = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (tab) params.set('status', tab);
      const res = await fetch(`${API_ORIGIN}/api/reports/admin?${params}`, {
        headers: authHeaders(currentSession.email), signal,
      });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || 'Lỗi tải danh sách báo cáo.'); }
      setReports(await res.json());
    } catch (err: any) {
      if (err?.name === 'AbortError') return;
      setReports([]);
      onToast(err.message, 'error');
    } finally { setLoading(false); }
  }, [tab, currentSession, onToast]);

  useEffect(() => {
    const controller = new AbortController();
    fetchReports(controller.signal);
    return () => controller.abort();
  }, [fetchReports]);

  useEffect(() => { fetchStats(); }, [fetchStats]);

  const openHandle = (report: ReportItem) => {
    if (!canHandle) { onToast('Bạn không có quyền xử lý báo cáo.', 'error'); return; }
    setSelected(report);
    setHandleStatus(report.status === 'OPEN' ? 'RESOLVED' : report.status);
    setAdminResponse(report.adminResponse || '');
  };

  const confirmHandle = async () => {
    if (!selected) return;
    if (!adminResponse.trim()) { onToast('Nội dung phản hồi không được để trống.', 'error'); return; }
    setSaving(true);
    try {
      const res = await fetch(`${API_ORIGIN}/api/reports/admin/${selected.id}/handle`, {
        method: 'PUT',
        headers: { ...authHeaders(currentSession.email), 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: handleStatus, adminResponse: adminResponse.trim() }),
      });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || 'Xử lý báo cáo thất bại.'); }
      onToast('Đã phản hồi và gửi thông báo tới người gửi.', 'success');
      setSelected(null);
      fetchReports();
      fetchStats();
    } catch (err: any) {
      onToast(err.message, 'error');
    } finally { setSaving(false); }
  };

  return (
    <div>
      <div>
        <h1 className="dashboard-title">Báo Cáo Từ Người Dùng</h1>
        <p className="dashboard-subtitle">
          Tiếp nhận và xử lý khiếu nại / phản hồi / sự cố từ Nông trại, Nhà bán lẻ, Vận chuyển (BICAP-27).
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '12px', margin: '20px 0' }}>
        <StatCard label="Mới" value={stats?.open} accent="#fbbf24" />
        <StatCard label="Đang xử lý" value={stats?.inProgress} accent="#38bdf8" />
        <StatCard label="Đã giải quyết" value={stats?.resolved} accent="#34d399" />
        <StatCard label="Bị từ chối" value={stats?.rejected} accent="#f87171" />
        <StatCard label="Tổng cộng" value={stats?.total} accent="#a78bfa" />
      </div>

      <div style={{ display: 'flex', gap: '10px', marginBottom: '20px', flexWrap: 'wrap' }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            style={{
              padding: '10px 18px', borderRadius: '10px', border: '1px solid var(--border-color)',
              background: tab === t.id ? 'rgba(139,92,246,0.15)' : 'rgba(255,255,255,0.03)',
              color: tab === t.id ? '#c4b5fd' : 'var(--text-secondary)',
              fontWeight: tab === t.id ? 700 : 500, fontSize: '13px', cursor: 'pointer',
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
                <th style={thStyle}>ID</th>
                <th style={thStyle}>Tiêu Đề</th>
                <th style={thStyle}>Loại</th>
                <th style={thStyle}>Người Gửi</th>
                <th style={thStyle}>Vai Trò</th>
                <th style={thStyle}>Ngày Gửi</th>
                <th style={thStyle}>Trạng Thái</th>
                <th style={thStyle}>Thao Tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={8} style={emptyStyle}>Đang tải dữ liệu...</td></tr>
              ) : reports.length === 0 ? (
                <tr><td colSpan={8} style={emptyStyle}>Không có báo cáo nào.</td></tr>
              ) : (
                reports.map((r) => (
                  <tr key={r.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <td style={tdStyle}>{r.id}</td>
                    <td style={tdStyle}><span style={{ fontWeight: 600, color: '#fff' }}>{r.subject}</span></td>
                    <td style={tdStyle}>{TYPE_LABELS[r.type] || r.type}</td>
                    <td style={tdStyle}>{r.reporterName || `#${r.reporterId}`}</td>
                    <td style={tdStyle}><span style={{ fontSize: '12px' }}>{r.reporterRole}</span></td>
                    <td style={tdStyle}>{formatDate(r.createdAt)}</td>
                    <td style={tdStyle}><Badge status={r.status} /></td>
                    <td style={tdStyle}>
                      <button onClick={() => openHandle(r)} style={actionBtnStyle} title="Xem & xử lý">
                        {canHandle ? '⚙️ Xử lý' : '👁️ Xem'}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {selected && (
        <div style={overlayStyle} onClick={() => setSelected(null)}>
          <div style={{ ...modalStyle, maxWidth: '640px' }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
              <div>
                <h2 style={{ margin: 0, color: '#fff', fontSize: '19px', fontWeight: 700 }}>{selected.subject}</h2>
                <p style={{ margin: '6px 0 0', color: 'var(--text-secondary)', fontSize: '13px' }}>
                  {TYPE_LABELS[selected.type] || selected.type} · từ {selected.reporterName || `#${selected.reporterId}`} ({selected.reporterRole})
                  {selected.relatedOrderId ? ` · Đơn #${selected.relatedOrderId}` : ''}
                </p>
              </div>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                <Badge status={selected.status} />
                <button onClick={() => setSelected(null)} style={closeBtnStyle}>✕</button>
              </div>
            </div>

            <div style={sectionStyle}>
              <h3 style={sectionTitleStyle}>📝 Nội dung báo cáo</h3>
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: '1.6', margin: 0, whiteSpace: 'pre-wrap' }}>
                {selected.content}
              </p>
            </div>

            {canHandle ? (
              <>
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '6px' }}>Trạng thái xử lý</label>
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    {HANDLE_OPTIONS.map((opt) => (
                      <button
                        key={opt.id}
                        onClick={() => setHandleStatus(opt.id)}
                        style={{
                          padding: '8px 14px', borderRadius: '8px', border: '1px solid var(--border-color)',
                          background: handleStatus === opt.id ? 'rgba(139,92,246,0.15)' : 'rgba(255,255,255,0.03)',
                          color: handleStatus === opt.id ? '#c4b5fd' : 'var(--text-secondary)',
                          fontWeight: handleStatus === opt.id ? 700 : 500, fontSize: '12px', cursor: 'pointer',
                        }}
                      >
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '6px' }}>Phản hồi tới người gửi</label>
                  <textarea
                    rows={4} value={adminResponse} onChange={(e) => setAdminResponse(e.target.value)}
                    placeholder="Nội dung phản hồi sẽ được gửi cho người báo cáo..."
                    style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit' }}
                  />
                </div>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '8px' }}>
                  <button onClick={() => setSelected(null)} className="btn btn-secondary">Hủy</button>
                  <button onClick={confirmHandle} disabled={saving} className="btn btn-primary">
                    {saving ? 'Đang lưu…' : 'Gửi phản hồi'}
                  </button>
                </div>
              </>
            ) : (
              selected.adminResponse && (
                <div style={sectionStyle}>
                  <h3 style={sectionTitleStyle}>✅ Phản hồi của Admin</h3>
                  <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: 0 }}>{selected.adminResponse}</p>
                </div>
              )
            )}
          </div>
        </div>
      )}
    </div>
  );
};

const Badge: React.FC<{ status: string }> = ({ status }) => {
  const meta = STATUS_META[status] || STATUS_META.OPEN;
  return (
    <span style={{ padding: '4px 12px', borderRadius: '12px', fontSize: '12px', fontWeight: 600, color: meta.color, background: meta.bg, whiteSpace: 'nowrap' }}>
      {meta.label}
    </span>
  );
};

const StatCard: React.FC<{ label: string; value?: number; accent: string }> = ({ label, value, accent }) => (
  <div style={{ padding: '16px', borderRadius: '12px', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border-color)' }}>
    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>{label}</div>
    <div style={{ fontSize: '26px', fontWeight: 800, color: accent }}>{value ?? '—'}</div>
  </div>
);

const thStyle: React.CSSProperties = { textAlign: 'left', padding: '12px', fontSize: '11px', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px', whiteSpace: 'nowrap' };
const tdStyle: React.CSSProperties = { padding: '12px', fontSize: '13px', color: 'var(--text-secondary)', verticalAlign: 'middle' };
const emptyStyle: React.CSSProperties = { padding: '40px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' };
const actionBtnStyle: React.CSSProperties = { padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', fontSize: '12px', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)', color: 'var(--text-secondary)' };
const inputStyle: React.CSSProperties = { width: '100%', boxSizing: 'border-box', padding: '10px 12px', borderRadius: '8px', background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border-color)', color: '#fff', fontSize: '13px', outline: 'none' };
const overlayStyle: React.CSSProperties = { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 2000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' };
const modalStyle: React.CSSProperties = { width: '100%', maxHeight: '90vh', overflowY: 'auto', borderRadius: '14px', background: '#16171f', border: '1px solid var(--border-color)', padding: '24px', boxShadow: '0 25px 60px rgba(0,0,0,0.6)' };
const closeBtnStyle: React.CSSProperties = { width: '32px', height: '32px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(255,255,255,0.05)', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '14px' };
const sectionStyle: React.CSSProperties = { padding: '16px', borderRadius: '10px', background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.06)', marginBottom: '14px' };
const sectionTitleStyle: React.CSSProperties = { margin: '0 0 12px', fontSize: '13px', fontWeight: 700, color: '#a78bfa', textTransform: 'uppercase', letterSpacing: '0.5px' };
