import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import { translateProcessType } from '../../utils/processTypes';

type Product = {
  id: number; name: string; description: string; images: string[]; price: number; quantity: number;
  availability: string; categoryId: number; categoryName: string; farmName: string; farmAddress: string;
  certifications: string[]; seasonName?: string; productType?: string; variety?: string;
  seasonStartDate?: string; harvestDate?: string; traceHash?: string; qrImage?: string; transactionHash?: string;
  processes?: { processType: string; executionDate: string; materials?: string; images?: string; notes?: string; transactionHash?: string }[];
};
type Category = { id: number; name: string };

const money = (n: number) => new Intl.NumberFormat('vi-VN').format(n) + ' ₫';

/**
 * Sàn nông sản dành cho Nhà bán lẻ (BICAP-39/40/41).
 * Giao diện dùng chung design system với các trang Farm Manager: `glass-panel`,
 * `dashboard-title`, `form-input`... (trước đây trang tự hardcode màu nên lệch tông).
 */
export default function MarketplacePage() {
  const [items, setItems] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selected, setSelected] = useState<Product | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [f, setF] = useState({
    keyword: '', categoryId: '', region: '', certification: '',
    minPrice: '', maxPrice: '', availability: '', sortBy: 'NEWEST',
  });
  const [order, setOrder] = useState({ quantity: '', proposedPrice: '', desiredDeliveryDate: '', deliveryAddr: '', notes: '' });

  const load = async (next = page) => {
    setLoading(true);
    setError('');
    try {
      const q = new URLSearchParams({ page: String(next), size: '20', sortBy: f.sortBy });
      Object.entries(f).forEach(([k, v]) => v && k !== 'sortBy' && q.set(k, v));
      const r = await fetch(`${API_BASE_URL}/marketplace/products?${q}`, { headers: getAuthHeaders() });
      const b = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(b.message || 'Không thể tải sàn nông sản.');
      setItems(b.content || []);
      setTotal(b.totalElements || 0);
      setPage(next);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể tải dữ liệu.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetch(`${API_BASE_URL}/categories`, { headers: getAuthHeaders() })
      .then((r) => (r.ok ? r.json() : []))
      .then(setCategories);
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const detail = async (id: number) => {
    setError('');
    const r = await fetch(`${API_BASE_URL}/marketplace/products/${id}`, { headers: getAuthHeaders() });
    const b = await r.json().catch(() => ({}));
    if (!r.ok) {
      setError(b.message || 'Không thể tải chi tiết.');
      return;
    }
    setSelected(b);
    setOrder((v) => ({ ...v, proposedPrice: String(b.price) }));
  };

  const place = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    const r = await fetch(`${API_BASE_URL}/orders`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        productId: selected.id,
        quantity: Number(order.quantity),
        proposedPrice: Number(order.proposedPrice),
        desiredDeliveryDate: order.desiredDeliveryDate,
        deliveryAddr: order.deliveryAddr,
        notes: order.notes,
      }),
    });
    const b = await r.json().catch(() => ({}));
    if (!r.ok) {
      setError(b.message || 'Không thể tạo yêu cầu mua.');
      return;
    }
    window.alert(`Đã gửi yêu cầu mua #${b.id} đến ${selected.farmName}.`);
    setSelected(null);
  };

  const img = (p: Product) => (p.images?.[0] ? `${API_BASE_URL.replace(/\/api$/, '')}${p.images[0]}` : '');

  if (selected) {
    return (
      <section>
        <button className="btn btn-secondary" onClick={() => setSelected(null)}>← Quay lại sàn</button>

        <div style={detailGrid}>
          <div>
            {selected.images?.length
              ? <img src={img(selected)} alt={selected.name} style={hero} />
              : <div style={placeholder}>🌿</div>}

            {selected.qrImage && (
              <div className="glass-panel" style={qrBox}>
                <img src={selected.qrImage} width={150} alt="QR truy xuất" />
                <div>
                  <strong>QR truy xuất nguồn gốc</strong>
                  <p style={muted}>TX: {selected.transactionHash || 'Đang cập nhật'}</p>
                  {selected.traceHash && (
                    <a href={`/trace/${selected.traceHash}`} target="_blank" rel="noreferrer">Mở hồ sơ truy xuất ↗</a>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="glass-panel" style={panel}>
            <span style={badge}>{selected.categoryName}</span>
            <h1 className="dashboard-title" style={{ fontSize: 26 }}>{selected.name}</h1>
            <h2 style={{ color: 'var(--success-hover)', marginBottom: 12 }}>{money(selected.price)}</h2>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>{selected.description}</p>
            <p>🏡 {selected.farmName} , {selected.farmAddress}</p>
            <p>📦 Còn {selected.quantity} , {selected.availability === 'AVAILABLE' ? 'Đang bán' : 'Hết hàng'}</p>
            <p>🏅 {selected.certifications?.join(', ') || 'Chưa cập nhật chứng nhận'}</p>

            <hr style={line} />
            <h3 style={sectionTitle}>Mùa vụ &amp; quy trình</h3>
            <p>{selected.seasonName} , {selected.variety} , Thu hoạch {selected.harvestDate || '—'}</p>
            {selected.processes?.map((x, i) => (
              <div key={i} style={timeline}>
                <b>{x.executionDate} , {translateProcessType(x.processType)}</b>
                <div>{x.materials || x.notes || 'Không có ghi chú'}</div>
              </div>
            ))}

            <hr style={line} />
            <h3 style={sectionTitle}>Gửi yêu cầu đặt mua</h3>
            <form onSubmit={place} style={formGrid}>
              <input
                className="form-input" required min="0.01" max={selected.quantity} step="0.01" type="number"
                placeholder="Số lượng" value={order.quantity}
                onChange={(e) => setOrder({ ...order, quantity: e.target.value })}
              />
              <input
                className="form-input" required min="1" type="number" placeholder="Giá đề xuất"
                value={order.proposedPrice} onChange={(e) => setOrder({ ...order, proposedPrice: e.target.value })}
              />
              <input
                className="form-input" required min={new Date(Date.now() + 86400000).toISOString().slice(0, 10)}
                type="date" value={order.desiredDeliveryDate}
                onChange={(e) => setOrder({ ...order, desiredDeliveryDate: e.target.value })}
              />
              <input
                className="form-input" required maxLength={500} placeholder="Địa chỉ nhận hàng"
                value={order.deliveryAddr} onChange={(e) => setOrder({ ...order, deliveryAddr: e.target.value })}
              />
              <textarea
                className="form-input" maxLength={2000} placeholder="Ghi chú (không bắt buộc)" rows={3}
                value={order.notes} onChange={(e) => setOrder({ ...order, notes: e.target.value })}
              />
              <button style={primary}>Gửi yêu cầu mua</button>
            </form>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section>
      <div style={{ marginBottom: 24 }}>
        <h1 className="dashboard-title">Sàn nông sản</h1>
        <p className="dashboard-subtitle" style={{ marginBottom: 0 }}>
          Tìm nông sản đã được BICAP xác minh và kết nối trực tiếp với trang trại.
        </p>
      </div>

      <form onSubmit={(e) => { e.preventDefault(); load(0); }} className="glass-panel" style={filters}>
        <input
          className="form-input" placeholder="Tên sản phẩm hoặc trang trại" value={f.keyword}
          onChange={(e) => setF({ ...f, keyword: e.target.value })}
        />
        <select className="form-input" value={f.categoryId} onChange={(e) => setF({ ...f, categoryId: e.target.value })}>
          <option value="">Tất cả loại</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input
          className="form-input" placeholder="Tỉnh/thành, vùng miền" value={f.region}
          onChange={(e) => setF({ ...f, region: e.target.value })}
        />
        <input
          className="form-input" placeholder="Chứng nhận" value={f.certification}
          onChange={(e) => setF({ ...f, certification: e.target.value })}
        />
        <input
          className="form-input" type="number" min="0" placeholder="Giá từ" value={f.minPrice}
          onChange={(e) => setF({ ...f, minPrice: e.target.value })}
        />
        <input
          className="form-input" type="number" min="0" placeholder="Giá đến" value={f.maxPrice}
          onChange={(e) => setF({ ...f, maxPrice: e.target.value })}
        />
        <select className="form-input" value={f.availability} onChange={(e) => setF({ ...f, availability: e.target.value })}>
          <option value="">Mọi tình trạng</option>
          <option value="AVAILABLE">Còn hàng</option>
          <option value="SOLD_OUT">Hết hàng</option>
        </select>
        <select className="form-input" value={f.sortBy} onChange={(e) => setF({ ...f, sortBy: e.target.value })}>
          <option value="NEWEST">Mới nhất</option>
          <option value="PRICE_ASC">Giá tăng dần</option>
          <option value="PRICE_DESC">Giá giảm dần</option>
        </select>
        <button style={primary}>Tìm kiếm</button>
      </form>

      {error && <div style={alert} role="alert">{error}</div>}

      <p style={muted}>{loading ? 'Đang tải...' : `${total} sản phẩm phù hợp`}</p>

      <div style={grid}>
        {items.map((p) => (
          <article key={p.id} className="glass-panel" style={card}>
            {img(p) ? <img src={img(p)} alt={p.name} style={thumb} /> : <div style={placeholder}>🌱</div>}
            <div style={{ padding: 16 }}>
              <span style={badge}>{p.categoryName}</span>
              <h3 style={{ margin: '10px 0 6px' }}>{p.name}</h3>
              <strong style={{ color: 'var(--success-hover)' }}>{money(p.price)}</strong>
              <p style={muted}>🏡 {p.farmName} , {p.farmAddress}</p>
              <p style={muted}>🏅 {p.certifications?.join(', ') || 'Chưa cập nhật'}</p>
              <button style={{ ...primary, marginTop: 12 }} onClick={() => detail(p.id)}>Xem chi tiết</button>
            </div>
          </article>
        ))}
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 12, marginTop: 24 }}>
        <button className="btn btn-secondary" disabled={page === 0} onClick={() => load(page - 1)}>Trang trước</button>
        <span style={{ color: 'var(--text-secondary)', fontSize: 13 }}>Trang {page + 1}</span>
        <button className="btn btn-secondary" disabled={(page + 1) * 20 >= total} onClick={() => load(page + 1)}>Trang sau</button>
      </div>
    </section>
  );
}

/* ── Styles ── */
const panel: React.CSSProperties = { padding: 24 };
const filters: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
  gap: 10,
  margin: '0 0 20px',
  padding: 20,
};
const grid: React.CSSProperties = { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(245px, 1fr))', gap: 18 };
const card: React.CSSProperties = { overflow: 'hidden', padding: 0 };
const thumb: React.CSSProperties = { width: '100%', height: 180, objectFit: 'cover' };
const hero: React.CSSProperties = { ...thumb, height: 360, borderRadius: 16 };
const placeholder: React.CSSProperties = {
  height: 180,
  display: 'grid',
  placeItems: 'center',
  fontSize: 54,
  background: 'rgba(255, 255, 255, 0.03)',
  color: 'var(--text-muted)',
};
const primary: React.CSSProperties = {
  border: 0,
  borderRadius: 9,
  padding: '11px 16px',
  background: 'var(--success)',
  color: 'white',
  fontWeight: 700,
  cursor: 'pointer',
};
const muted: React.CSSProperties = { color: 'var(--text-secondary)', fontSize: 13, margin: '6px 0' };
const badge: React.CSSProperties = {
  fontSize: 11,
  padding: '4px 8px',
  borderRadius: 999,
  background: 'rgba(16, 185, 129, 0.15)',
  color: '#6ee7b7',
};
const alert: React.CSSProperties = {
  padding: 12,
  marginBottom: 12,
  border: '1px solid var(--danger)',
  color: '#fecaca',
  background: 'var(--danger-light)',
  borderRadius: 8,
};
const detailGrid: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'minmax(280px, 1fr) minmax(360px, 1fr)',
  gap: 24,
  marginTop: 20,
  alignItems: 'start',
};
const qrBox: React.CSSProperties = { display: 'flex', gap: 16, alignItems: 'center', marginTop: 16, padding: 20 };
const formGrid: React.CSSProperties = { display: 'grid', gap: 10 };
const timeline: React.CSSProperties = {
  borderLeft: '2px solid var(--success)',
  padding: '4px 0 12px 14px',
  color: 'var(--text-secondary)',
};
const sectionTitle: React.CSSProperties = { color: '#fff', fontSize: 17, margin: '0 0 10px' };
const line: React.CSSProperties = { border: 0, borderTop: '1px solid var(--border-color)', margin: '20px 0' };
