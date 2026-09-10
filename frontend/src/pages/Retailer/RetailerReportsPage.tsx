import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

/**
 * BICAP-53: Retailer gửi báo cáo cho Admin (SRS-RT-018).
 * Dùng chung API POST /api/reports và GET /api/reports/my với FarmManager/Reports.tsx.
 */

type Report = {
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

const STATUS_COLORS: Record<string, { color: string; bg: string }> = {
  OPEN:        { color: '#fcd34d', bg: 'rgba(245,158,11,.15)' },
  IN_PROGRESS: { color: '#7dd3fc', bg: 'rgba(56,189,248,.15)' },
  RESOLVED:    { color: '#6ee7b7', bg: 'rgba(16,185,129,.15)' },
  REJECTED:    { color: '#fca5a5', bg: 'rgba(239,68,68,.15)' },
};

function StatusBadge({ status }: { status: string }) {
  const c = STATUS_COLORS[status] ?? { color: '#94a3b8', bg: 'rgba(148,163,184,.15)' };
  return (
    <span style={{ fontSize: 11, padding: '4px 8px', borderRadius: 999, color: c.color, background: c.bg, whiteSpace: 'nowrap' }}>
      {status}
    </span>
  );
}

export default function RetailerReportsPage() {
  const [items, setItems] = useState<Report[]>([]);
  const [form, setForm] = useState({ type: 'FEEDBACK', subject: '', content: '', relatedOrderId: '' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const load = async () => {
    const res = await fetch(`${API_BASE_URL}/reports/my`, { headers: getAuthHeaders() });
    if (res.ok) setItems(await res.json());
  };

  useEffect(() => { void load(); }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice('');
    if (!form.subject.trim() || !form.content.trim()) { setError('Vui lòng điền đầy đủ tiêu đề và nội dung.'); return; }
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
      setNotice('Đã gửi báo cáo tới quản trị viên. Chúng tôi sẽ phản hồi sớm nhất có thể.');
      setForm({ type: 'FEEDBACK', subject: '', content: '', relatedOrderId: '' });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Gửi báo cáo thất bại.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section style={{ color: '#e2e8f0' }}>
      {/* Header */}
      <div style={{ marginBottom: 20 }}>
        <h1 style={{ margin: '0 0 6px', fontSize: 24, fontWeight: 700 }}>Gửi báo cáo cho Admin</h1>
        <p style={{ margin: 0, color: '#94a3b8', fontSize: 13 }}>
          Gửi khiếu nại, phản hồi hoặc báo cáo sự cố tới ban quản trị nền tảng.
        </p>
      </div>

      {error  && <div style={s.errorBox}  role="alert">{error}</div>}
      {notice && <div style={s.successBox} role="status">{notice}</div>}

      <div style={s.grid}>
        {/* Form gửi báo cáo */}
        <form className="glass-panel" style={{ padding: 24 }} onSubmit={(e) => void submit(e)}>
          <h2 style={s.sectionTitle}>Báo cáo mới</h2>

          <label style={s.label}>Loại báo cáo</label>
          <select
            value={form.type}
            onChange={e => setForm({ ...form, type: e.target.value })}
            style={s.input}
          >
            {TYPES.map(t => (
              <option key={t} value={t}>{TYPE_LABELS[t]} ({t})</option>
            ))}
          </select>

          <label style={s.label}>Tiêu đề *</label>
          <input
            required
            maxLength={200}
            value={form.subject}
            onChange={e => setForm({ ...form, subject: e.target.value })}
            placeholder="Tóm tắt vấn đề..."
            style={s.input}
          />

          <label style={s.label}>Nội dung chi tiết *</label>
          <textarea
            required
            minLength={10}
            maxLength={4000}
            rows={5}
            value={form.content}
            onChange={e => setForm({ ...form, content: e.target.value })}
            placeholder="Mô tả chi tiết vấn đề bạn gặp phải..."
            style={{ ...s.input, resize: 'vertical' }}
          />
          <p style={{ textAlign: 'right', color: '#64748b', fontSize: 11, margin: '3px 0 0' }}>
            {form.content.length}/4000
          </p>

          <label style={s.label}>Mã đơn hàng liên quan (tuỳ chọn)</label>
          <input
            type="number"
            min="1"
            value={form.relatedOrderId}
            onChange={e => setForm({ ...form, relatedOrderId: e.target.value })}
            placeholder="Ví dụ: 123"
            style={s.input}
          />

          <button disabled={busy} style={{ ...s.primaryBtn, marginTop: 20, width: '100%' }}>
            {busy ? 'Đang gửi...' : 'Gửi báo cáo'}
          </button>
        </form>

        {/* Lịch sử báo cáo */}
        <section className="glass-panel" style={{ padding: 24 }}>
          <h2 style={s.sectionTitle}>Lịch sử báo cáo ({items.length})</h2>

          {!items.length && (
            <div style={s.emptyBox}>
              <div style={{ fontSize: 32, marginBottom: 8 }}>📋</div>
              <p>Bạn chưa gửi báo cáo nào.</p>
            </div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {items.map(r => (
              <article key={r.id} style={s.card}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 10, marginBottom: 6 }}>
                  <strong style={{ fontSize: 14 }}>{r.subject}</strong>
                  <StatusBadge status={r.status} />
                </div>
                <p style={{ fontSize: 12, color: '#94a3b8', margin: '0 0 6px' }}>
                  {TYPE_LABELS[r.type] ?? r.type}
                  {r.relatedOrderId ? ` · Đơn #${r.relatedOrderId}` : ''}
                  {r.createdAt ? ` · ${new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(r.createdAt))}` : ''}
                </p>
                <p style={{ fontSize: 13, color: '#cbd5e1', margin: 0 }}>{r.content}</p>
                {r.adminResponse && (
                  <div style={s.adminReply}>
                    <strong style={{ color: '#6ee7b7', fontSize: 12 }}>Phản hồi của Admin:</strong>
                    <p style={{ margin: '4px 0 0', fontSize: 13 }}>{r.adminResponse}</p>
                    {r.handledAt && (
                      <p style={{ margin: '4px 0 0', fontSize: 11, color: '#64748b' }}>
                        {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(r.handledAt))}
                      </p>
                    )}
                  </div>
                )}
              </article>
            ))}
          </div>
        </section>
      </div>
    </section>
  );
}

const s: Record<string, React.CSSProperties> = {
  grid: { display: 'grid', gridTemplateColumns: 'minmax(300px,420px) minmax(360px,1fr)', gap: 24, alignItems: 'start' },
  sectionTitle: { color: '#fff', fontSize: 18, margin: '0 0 16px' },
  label: { display: 'block', color: '#cbd5e1', fontSize: 13, margin: '14px 0 6px' },
  input: { width: '100%', boxSizing: 'border-box' as const, padding: '11px 12px', borderRadius: 8, border: '1px solid #334155', background: '#111827', color: '#fff', font: 'inherit' },
  primaryBtn: { padding: 12, border: 0, borderRadius: 8, background: '#0891b2', color: '#fff', fontWeight: 700, cursor: 'pointer', fontSize: 14 },
  errorBox: { padding: '11px 14px', borderRadius: 10, color: '#fecaca', background: 'rgba(239,68,68,.13)', marginBottom: 12 },
  successBox: { padding: '11px 14px', borderRadius: 10, color: '#a7f3d0', background: 'rgba(16,185,129,.13)', marginBottom: 12 },
  card: { padding: 16, border: '1px solid #334155', borderRadius: 10, background: '#0f172a' },
  adminReply: { marginTop: 10, padding: 10, borderRadius: 8, background: 'rgba(16,185,129,.08)', border: '1px solid rgba(16,185,129,.25)' },
  emptyBox: { textAlign: 'center', padding: '32px 16px', color: '#94a3b8' },
};
