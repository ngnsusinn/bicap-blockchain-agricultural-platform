import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import { gridStyle, panelStyle, titleStyle, labelStyle, inputStyle, buttonStyle, alertStyle, successStyle, cardStyle, badgeStyle } from './ui';

type Report = {
  id: number; type: string; subject: string; content: string; status: string;
  relatedOrderId?: number; adminResponse?: string; createdAt?: string; handledAt?: string;
};

const TYPES = ['COMPLAINT', 'FEEDBACK', 'INCIDENT', 'OTHER'];
const TYPE_LABELS: Record<string, string> = {
  COMPLAINT: 'Khiếu nại', FEEDBACK: 'Phản hồi', INCIDENT: 'Sự cố', OTHER: 'Khác',
};

export default function Reports() {
  const [items, setItems] = useState<Report[]>([]);
  const [form, setForm] = useState({ type: 'FEEDBACK', subject: '', content: '', relatedOrderId: '' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const load = async () => {
    const res = await fetch(`${API_BASE_URL}/reports/my`, { headers: getAuthHeaders() });
    if (res.ok) setItems(await res.json());
  };

  useEffect(() => { load().catch(() => {}); }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setNotice('');
    setBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/reports`, {
        method: 'POST', headers: getAuthHeaders(),
        body: JSON.stringify({
          type: form.type, subject: form.subject, content: form.content,
          relatedOrderId: form.relatedOrderId ? Number(form.relatedOrderId) : null,
        }),
      });
      if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || 'Gửi báo cáo thất bại.'); }
      setNotice('Đã gửi báo cáo tới quản trị viên.');
      setForm({ type: 'FEEDBACK', subject: '', content: '', relatedOrderId: '' });
      await load();
    } catch (err) { setError(err instanceof Error ? err.message : 'Gửi báo cáo thất bại.'); }
    finally { setBusy(false); }
  };

  return (
    <div>
      <h1 className="dashboard-title">Gửi báo cáo cho Admin</h1>
      <p className="dashboard-subtitle">BICAP-27 · Gửi khiếu nại, phản hồi hoặc báo cáo sự cố tới ban quản trị nền tảng.</p>
      {error && <div style={alertStyle}>{error}</div>}
      {notice && <div style={successStyle}>{notice}</div>}

      <div style={gridStyle}>
        <form className="glass-panel" style={panelStyle} onSubmit={submit}>
          <h2 style={titleStyle}>Báo cáo mới</h2>
          <label style={labelStyle}>Loại báo cáo</label>
          <select value={form.type} onChange={e => setForm({ ...form, type: e.target.value })} style={inputStyle}>
            {TYPES.map(t => <option key={t} value={t}>{TYPE_LABELS[t]} ({t})</option>)}
          </select>
          <label style={labelStyle}>Tiêu đề</label>
          <input required maxLength={200} value={form.subject} onChange={e => setForm({ ...form, subject: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Nội dung chi tiết</label>
          <textarea required minLength={10} maxLength={4000} rows={5} value={form.content} onChange={e => setForm({ ...form, content: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Mã đơn hàng liên quan (tuỳ chọn)</label>
          <input type="number" min="1" value={form.relatedOrderId} onChange={e => setForm({ ...form, relatedOrderId: e.target.value })} style={inputStyle} />
          <button disabled={busy} style={buttonStyle}>{busy ? 'Đang gửi…' : 'Gửi báo cáo'}</button>
        </form>

        <section className="glass-panel" style={panelStyle}>
          <h2 style={titleStyle}>Lịch sử báo cáo ({items.length})</h2>
          {!items.length && <p style={{ color: '#94a3b8' }}>Bạn chưa gửi báo cáo nào.</p>}
          {items.map(r => (
            <article key={r.id} style={cardStyle}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                <strong>{r.subject}</strong><span style={badgeStyle(r.status)}>{r.status}</span>
              </div>
              <p style={{ fontSize: 12, color: '#94a3b8', margin: '4px 0' }}>{TYPE_LABELS[r.type] || r.type} · {r.createdAt}</p>
              <p style={{ fontSize: 13 }}>{r.content}</p>
              {r.adminResponse && (
                <div style={{ marginTop: 8, padding: 10, borderRadius: 8, background: 'rgba(16,185,129,.08)', border: '1px solid rgba(16,185,129,.25)', fontSize: 13 }}>
                  <strong style={{ color: '#6ee7b7' }}>Phản hồi của Admin:</strong> {r.adminResponse}
                </div>
              )}
            </article>
          ))}
        </section>
      </div>
    </div>
  );
}
