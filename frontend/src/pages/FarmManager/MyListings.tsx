import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import { panelStyle, titleStyle, inputStyle, alertStyle, cardStyle, badgeStyle, hashStyle } from './ui';

type Listing = {
  id: number; name: string; description: string; price: number; quantity: number;
  categoryName?: string; seasonName?: string; status: string; traceHash?: string;
  qrImage?: string; images?: string[]; createdAt?: string;
};

const FILTERS = ['', 'PENDING_REVIEW', 'ACTIVE', 'INACTIVE', 'REJECTED'];

export default function MyListings({ farmId }: { farmId?: number }) {
  const [items, setItems] = useState<Listing[]>([]);
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    if (!farmId) return;
    const url = `${API_BASE_URL}/farms/${farmId}/marketplace/products${status ? `?status=${status}` : ''}`;
    const res = await fetch(url, { headers: getAuthHeaders() });
    if (!res.ok) { setError('Không tải được danh sách sản phẩm đã đăng.'); return; }
    setItems(await res.json());
  };

  useEffect(() => { load().catch(() => setError('Lỗi kết nối máy chủ.')); }, [farmId, status]);

  if (!farmId) {
    return <div><h1 className="dashboard-title">Sản phẩm trên sàn</h1><div style={alertStyle}>Chưa xác định được nông trại của tài khoản.</div></div>;
  }

  return (
    <div>
      <h1 className="dashboard-title">Sản phẩm đã đẩy lên sàn</h1>
      <p className="dashboard-subtitle">BICAP-19 · Theo dõi trạng thái duyệt của từng sản phẩm sau khi đăng lên sàn giao dịch.</p>
      {error && <div style={alertStyle}>{error}</div>}

      <section className="glass-panel" style={panelStyle}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 8 }}>
          <h2 style={titleStyle}>Danh sách ({items.length})</h2>
          <select value={status} onChange={e => setStatus(e.target.value)} style={{ ...inputStyle, width: 'auto' }}>
            {FILTERS.map(f => <option key={f} value={f}>{f || 'Tất cả trạng thái'}</option>)}
          </select>
        </div>
        {!items.length && <p style={{ color: '#94a3b8' }}>Chưa có sản phẩm nào được đăng lên sàn.</p>}
        {items.map(p => (
          <article key={p.id} style={cardStyle}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
              <strong>{p.name}</strong><span style={badgeStyle(p.status)}>{p.status}</span>
            </div>
            <p style={{ fontSize: 13, margin: '6px 0' }}>{p.categoryName || '—'} · {p.seasonName || ''} · {p.quantity} · {p.price.toLocaleString('vi-VN')} ₫</p>
            {p.traceHash && <p style={hashStyle}>Trace: {p.traceHash}</p>}
            {p.qrImage && <img src={p.qrImage} alt={`QR ${p.name}`} width="96" height="96" style={{ marginTop: 8, borderRadius: 8 }} />}
          </article>
        ))}
      </section>
    </div>
  );
}
