/**
 * BICAP-60 — Shipping Manager gửi báo cáo cho Admin.
 * BICAP-62 — Shipping Manager xem báo cáo từ tài xế.
 *
 * Tab 1: Gửi / xem báo cáo của Shipping Manager gửi lên Admin.
 * Tab 2: Xem các báo cáo sự cố từ tài xế (tracking entries với status REPORT_*).
 */
import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  panelStyle, titleStyle, cardStyle, badgeStyle, alertStyle, successStyle,
  buttonStyle, labelStyle, inputStyle, secondaryButtonStyle, gridStyle,
} from '../FarmManager/ui';

// ── Tab 1: Send/view reports to Admin (BICAP-60) ──────────────────────────

type AdminReport = {
  id: number;
  type: string;
  subject: string;
  content: string;
  status: string;
  relatedOrderId?: number;
  adminResponse?: string;
  createdAt?: string;
  handledAt?: string;
};

const TYPES = ['COMPLAINT', 'FEEDBACK', 'INCIDENT', 'OTHER'];
const TYPE_LABELS: Record<string, string> = {
  COMPLAINT: 'Khiếu nại',
  FEEDBACK: 'Phản hồi',
  INCIDENT: 'Sự cố',
  OTHER: 'Khác',
};

function SendReportTab() {
  const [items, setItems] = useState<AdminReport[]>([]);
  const [form, setForm] = useState({ type: 'INCIDENT', subject: '', content: '', relatedOrderId: '' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const load = async () => {
    const res = await fetch(`${API_BASE_URL}/reports/my`, { headers: getAuthHeaders() });
    if (res.ok) setItems(await res.json());
  };

  useEffect(() => { load().catch(() => {}); }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice('');
    setBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/reports`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          type: form.type,
          subject: form.subject.trim(),
          content: form.content.trim(),
          relatedOrderId: form.relatedOrderId ? Number(form.relatedOrderId) : null,
        }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || 'Gửi báo cáo thất bại.');
      }
      setNotice('Đã gửi báo cáo tới quản trị viên.');
      setForm({ type: 'INCIDENT', subject: '', content: '', relatedOrderId: '' });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Gửi báo cáo thất bại.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={gridStyle}>
      <form className="glass-panel" style={panelStyle} onSubmit={submit}>
        <h2 style={titleStyle}>Báo cáo mới gửi Admin</h2>

        {error && <div style={alertStyle}>{error}</div>}
        {notice && <div style={successStyle}>{notice}</div>}

        <label style={labelStyle}>Loại báo cáo</label>
        <select value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))} style={inputStyle}>
          {TYPES.map(t => <option key={t} value={t}>{TYPE_LABELS[t]} ({t})</option>)}
        </select>

        <label style={labelStyle}>Tiêu đề *</label>
        <input
          required
          maxLength={200}
          value={form.subject}
          onChange={e => setForm(f => ({ ...f, subject: e.target.value }))}
          style={inputStyle}
        />

        <label style={labelStyle}>Nội dung chi tiết *</label>
        <textarea
          required
          minLength={10}
          maxLength={4000}
          rows={5}
          value={form.content}
          onChange={e => setForm(f => ({ ...f, content: e.target.value }))}
          style={{ ...inputStyle, resize: 'vertical' }}
        />

        <label style={labelStyle}>Mã đơn hàng liên quan (tuỳ chọn)</label>
        <input
          type="number"
          min="1"
          value={form.relatedOrderId}
          onChange={e => setForm(f => ({ ...f, relatedOrderId: e.target.value }))}
          style={inputStyle}
        />

        <button disabled={busy} style={buttonStyle}>
          {busy ? 'Đang gửi…' : '📨 Gửi báo cáo'}
        </button>
      </form>

      <section className="glass-panel" style={panelStyle}>
        <h2 style={titleStyle}>Lịch sử báo cáo ({items.length})</h2>
        {!items.length && <p style={{ color: '#94a3b8' }}>Bạn chưa gửi báo cáo nào.</p>}
        {items.map(r => (
          <article key={r.id} style={{ ...cardStyle, marginTop: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
              <strong style={{ color: '#cbd5e1' }}>{r.subject}</strong>
              <span style={badgeStyle(r.status)}>{r.status}</span>
            </div>
            <p style={{ fontSize: 12, color: '#94a3b8', margin: '4px 0' }}>
              {TYPE_LABELS[r.type] || r.type}
              {r.relatedOrderId ? `, Đơn #${r.relatedOrderId}` : ''}
              {r.createdAt ? `, ${new Date(r.createdAt).toLocaleDateString('vi-VN')}` : ''}
            </p>
            <p style={{ fontSize: 13, color: '#94a3b8' }}>{r.content}</p>
            {r.adminResponse && (
              <div style={{
                marginTop: 10, padding: 10, borderRadius: 8,
                background: 'rgba(16,185,129,.08)', border: '1px solid rgba(16,185,129,.25)', fontSize: 13,
              }}>
                <strong style={{ color: '#6ee7b7' }}>Phản hồi Admin:</strong> {r.adminResponse}
                {r.handledAt && (
                  <span style={{ color: '#64748b', fontSize: 11, marginLeft: 8 }}>
                    ({new Date(r.handledAt).toLocaleDateString('vi-VN')})
                  </span>
                )}
              </div>
            )}
          </article>
        ))}
      </section>
    </div>
  );
}

// ── Tab 2: View driver reports (BICAP-62) ─────────────────────────────────

type DriverReport = {
  id: number;
  shipmentId: number;
  status: string;          // "REPORT_INCIDENT", "REPORT_DELAY", etc.
  notes?: string;
  gpsLat: number;
  gpsLng: number;
  images?: string[];
  timestamp?: string;
};

type ShipmentSummary = {
  id: number;
  orderId?: number;
  driverName?: string;
  vehicleLicensePlate?: string;
};

function DriverReportsTab() {
  const [shipments, setShipments] = useState<ShipmentSummary[]>([]);
  const [reports, setReports] = useState<DriverReport[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [filterShipmentId, setFilterShipmentId] = useState('');

  // Load all shipments for the filter selector
  useEffect(() => {
    fetch(`${API_BASE_URL}/shipping/shipments`, { headers: getAuthHeaders() })
      .then(r => r.ok ? r.json() : [])
      .then(data => setShipments(data))
      .catch(() => {});
    // Load all driver reports initially
    loadReports('');
  }, []);

  const loadReports = async (shipmentId: string) => {
    setLoading(true);
    setError('');
    try {
      const url = shipmentId
        ? `${API_BASE_URL}/shipping/driver-reports?shipmentId=${shipmentId}`
        : `${API_BASE_URL}/shipping/driver-reports`;
      const res = await fetch(url, { headers: getAuthHeaders() });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setReports(await res.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được báo cáo từ tài xế.');
    } finally {
      setLoading(false);
    }
  };

  const handleFilter = (id: string) => {
    setFilterShipmentId(id);
    loadReports(id);
  };

  const reportType = (status: string) => status.replace('REPORT_', '');

  const reportColor = (status: string) => {
    const t = reportType(status).toUpperCase();
    if (t === 'INCIDENT' || t === 'DAMAGE') return '#fca5a5';
    if (t === 'DELAY') return '#fcd34d';
    return '#7dd3fc';
  };

  return (
    <div>
      {error && <div style={alertStyle}>{error}</div>}

      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap', alignItems: 'center' }}>
        <select
          value={filterShipmentId}
          onChange={e => handleFilter(e.target.value)}
          style={{ ...inputStyle, width: 'auto', minWidth: 220 }}
        >
          <option value="">Tất cả lô vận chuyển</option>
          {shipments.map(s => (
            <option key={s.id} value={s.id}>
              Lô #{s.id}{s.driverName ? `, ${s.driverName}` : ''}{s.orderId ? `, Đơn #${s.orderId}` : ''}
            </option>
          ))}
        </select>
        <button
          onClick={() => loadReports(filterShipmentId)}
          style={{ ...secondaryButtonStyle }}
        >
          🔄 Làm mới
        </button>
        <span style={{ color: '#64748b', fontSize: 13 }}>
          {reports.length} báo cáo
        </span>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>Đang tải…</div>}

      {!loading && reports.length === 0 && (
        <div className="glass-panel" style={{ ...panelStyle, textAlign: 'center', color: '#94a3b8' }}>
          Không có báo cáo nào từ tài xế.
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        {reports.map(r => {
          const matchShipment = shipments.find(s => s.id === r.shipmentId);
          return (
            <div key={r.id} className="glass-panel" style={{ ...cardStyle, padding: 20, borderLeft: `3px solid ${reportColor(r.status)}` }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 10 }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6, flexWrap: 'wrap' }}>
                    <span style={{ fontSize: 14, fontWeight: 700, color: reportColor(r.status) }}>
                      ⚠️ Báo cáo: {reportType(r.status)}
                    </span>
                    <span style={{ fontSize: 12, color: '#64748b', background: 'rgba(255,255,255,.05)', padding: '2px 8px', borderRadius: 99 }}>
                      Lô #{r.shipmentId}
                      {matchShipment?.driverName ? `, ${matchShipment.driverName}` : ''}
                    </span>
                    {matchShipment?.orderId && (
                      <span style={{ fontSize: 12, color: '#64748b' }}>Đơn #{matchShipment.orderId}</span>
                    )}
                  </div>
                  {r.notes && <p style={{ fontSize: 13, color: '#cbd5e1', margin: '0 0 8px' }}>{r.notes}</p>}
                  <div style={{ fontSize: 12, color: '#64748b' }}>
                    GPS: {r.gpsLat?.toFixed(5)}, {r.gpsLng?.toFixed(5)}
                          {r.timestamp && `, ${new Date(r.timestamp).toLocaleString('vi-VN')}`}
                  </div>
                </div>
                {r.images && r.images.length > 0 && (
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    {r.images.map((img, i) => (
                      <a key={i} href={img} target="_blank" rel="noopener noreferrer">
                        <img
                          src={img}
                          alt={`report-img-${i}`}
                          style={{ width: 56, height: 56, objectFit: 'cover', borderRadius: 6, border: '1px solid #334155' }}
                        />
                      </a>
                    ))}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────

export default function ShippingReportsPage() {
  const [tab, setTab] = useState<'send' | 'driver'>('driver');

  return (
    <div>
      <h1 className="dashboard-title">Báo cáo</h1>
      <p className="dashboard-subtitle">
        Gửi báo cáo cho Admin, Xem báo cáo từ tài xế.
      </p>

      <div style={{ display: 'flex', gap: 0, marginBottom: 24, borderBottom: '1px solid #1e293b' }}>
        {[
          { key: 'driver', label: '⚠️ Báo cáo từ tài xế' },
          { key: 'send', label: '📨 Gửi báo cáo Admin' },
        ].map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setTab(key as 'send' | 'driver')}
            style={{
              padding: '10px 20px',
              border: 0,
              borderBottom: tab === key ? '2px solid #10b981' : '2px solid transparent',
              background: 'transparent',
              color: tab === key ? '#6ee7b7' : '#64748b',
              fontWeight: tab === key ? 700 : 400,
              cursor: 'pointer',
              fontSize: 14,
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'send' && <SendReportTab />}
      {tab === 'driver' && <DriverReportsTab />}
    </div>
  );
}
