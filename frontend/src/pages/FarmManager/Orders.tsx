import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

type OrderItem = {
  id: number;
  status: string;
  rejectReason?: string | null;
  createdAt?: string;
  productId: number;
  productName?: string;
  productImage?: string;
  productPrice?: number;
  productQuantity?: number;
  retailerId: number;
  retailerName?: string;
  retailerEmail?: string;
  retailerPhone?: string;
  farmName?: string;
  seasonName?: string;
  quantity: number;
  price: number;
  totalAmount?: number;
  deliveryAddr?: string;
  depositRate?: number;
  depositAmount?: number;
};

type FilterKey = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'ALL';

const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'PENDING', label: 'Chờ xử lý' },
  { key: 'ACCEPTED', label: 'Đã chấp nhận' },
  { key: 'REJECTED', label: 'Đã từ chối' },
  { key: 'ALL', label: 'Tất cả' },
];

const STATUS_META: Record<string, { label: string; color: string }> = {
  PENDING: { label: 'Chờ xử lý', color: '#f59e0b' },
  ACCEPTED: { label: 'Đã chấp nhận', color: '#10b981' },
  REJECTED: { label: 'Đã từ chối', color: '#ef4444' },
  DEPOSIT_PAID: { label: 'Đã đặt cọc', color: '#06b6d4' },
};

/**
 * Xử lý yêu cầu mua nông sản từ Nhà bán lẻ (BICAP-20 / SRS-FM-014).
 * Farm Manager xem danh sách yêu cầu trên các nông trại của mình theo trạng thái,
 * chấp nhận (→ ACCEPTED) hoặc từ chối kèm lý do (→ REJECTED).
 */
export default function Orders() {
  const [orders, setOrders] = useState<OrderItem[]>([]);
  const [filter, setFilter] = useState<FilterKey>('PENDING');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [rejecting, setRejecting] = useState<{ id: number; reason: string } | null>(null);

  const load = async () => {
    setError('');
    try {
      const query = filter === 'ALL' ? '' : `?status=${filter}`;
      const res = await fetch(`${API_BASE_URL}/orders${query}`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Không thể tải danh sách đơn hàng.');
      setOrders(await res.json());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể tải dữ liệu.');
    }
  };

  useEffect(() => {
    load().catch(() => setError('Không thể tải dữ liệu.'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const accept = async (order: OrderItem) => {
    if (!window.confirm(`Chấp nhận yêu cầu mua "${order.productName}" (${order.quantity} đơn vị) từ ${order.retailerName}?`)) return;
    setBusy(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE_URL}/orders/${order.id}/accept`, { method: 'PUT', headers: getAuthHeaders() });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || body.error || 'Không thể chấp nhận đơn hàng.');
      }
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể chấp nhận đơn hàng.');
    } finally {
      setBusy(false);
    }
  };

  const reject = async (orderId: number, reason: string) => {
    if (!reason.trim()) {
      setError('Vui lòng nhập lý do từ chối.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE_URL}/orders/${orderId}/reject`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ reason: reason.trim() }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || body.error || 'Không thể từ chối đơn hàng.');
      }
      setRejecting(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể từ chối đơn hàng.');
    } finally {
      setBusy(false);
    }
  };

  const total = (o: OrderItem) => o.totalAmount ?? o.price * o.quantity;

  return (
    <div>
      <h1 className="dashboard-title">Đơn hàng / Yêu cầu mua</h1>
      <p className="dashboard-subtitle">
        BICAP-20 · Xem và xử lý (chấp nhận / từ chối) các yêu cầu mua nông sản từ Nhà bán lẻ.
        Khi chấp nhận, Nhà bán lẻ có 24h để đặt cọc 30% xác nhận đơn hàng.
      </p>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        {FILTERS.map((f) => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            style={{
              ...tabStyle,
              background: filter === f.key ? 'rgba(16, 185, 129, 0.15)' : 'transparent',
              color: filter === f.key ? '#34d399' : '#94a3b8',
              borderColor: filter === f.key ? 'rgba(16, 185, 129, 0.4)' : '#334155',
            }}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <div style={alertStyle}>{error}</div>}

      {orders.length === 0 ? (
        <div className="glass-panel" style={{ padding: 32, textAlign: 'center', color: '#94a3b8' }}>
          {filter === 'PENDING'
            ? 'Chưa có yêu cầu mua nào đang chờ xử lý. Yêu cầu từ Nhà bán lẻ sẽ xuất hiện tại đây.'
            : 'Không có đơn hàng nào ở trạng thái này.'}
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 16 }}>
          {orders.map((o) => {
            const meta = STATUS_META[o.status] || { label: o.status, color: '#94a3b8' };
            return (
              <div key={o.id} className="glass-panel" style={{ padding: 18 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, flexWrap: 'wrap' }}>
                  <div style={{ flex: 1, minWidth: 260 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                      {o.productImage && (
                        <img src={o.productImage} alt={o.productName} width="44" height="44" style={{ borderRadius: 8, objectFit: 'cover' }} />
                      )}
                      <div>
                        <div style={{ color: '#fff', fontWeight: 700, fontSize: 16 }}>
                          {o.productName || `Sản phẩm #${o.productId}`}
                          <span style={{ fontSize: 11, color: meta.color, marginLeft: 10, border: `1px solid ${meta.color}55`, padding: '2px 8px', borderRadius: 10 }}>{meta.label}</span>
                        </div>
                        <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 2 }}>
                          {o.seasonName ? `Mùa vụ: ${o.seasonName} · ` : ''}
                          {o.farmName ? `${o.farmName}` : ''}
                        </div>
                      </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 10, marginTop: 14 }}>
                      <div style={infoCellStyle}>
                        <span style={infoLabelStyle}>Nhà bán lẻ</span>
                        <div style={{ color: '#e2e8f0', fontSize: 14, fontWeight: 600 }}>{o.retailerName || '—'}</div>
                        <div style={{ fontSize: 12, color: '#94a3b8' }}>{o.retailerEmail}</div>
                      </div>
                      <div style={infoCellStyle}>
                        <span style={infoLabelStyle}>Số lượng</span>
                        <div style={{ color: '#e2e8f0', fontSize: 14 }}>{o.quantity} đơn vị</div>
                        <div style={{ fontSize: 12, color: '#94a3b8' }}>Đơn giá {fmt(o.price)} ₫</div>
                      </div>
                      <div style={infoCellStyle}>
                        <span style={infoLabelStyle}>Thành tiền</span>
                        <div style={{ color: '#34d399', fontSize: 14, fontWeight: 700 }}>{fmt(total(o))} ₫</div>
                        <div style={{ fontSize: 12, color: '#94a3b8' }}>Cọc 30%: {fmt((o.depositAmount ?? (total(o) * (o.depositRate ?? 0.3))))} ₫</div>
                      </div>
                      <div style={infoCellStyle}>
                        <span style={infoLabelStyle}>Giao đến</span>
                        <div style={{ color: '#e2e8f0', fontSize: 13 }}>{o.deliveryAddr || '—'}</div>
                        <div style={{ fontSize: 12, color: '#94a3b8' }}>{o.createdAt ? new Date(o.createdAt).toLocaleString('vi-VN') : ''}</div>
                      </div>
                    </div>

                    {o.status === 'REJECTED' && o.rejectReason && (
                      <div style={{ marginTop: 12, fontSize: 13, color: '#fca5a5', background: 'rgba(239,68,68,.08)', border: '1px solid rgba(239,68,68,.2)', borderRadius: 8, padding: '8px 12px' }}>
                        <strong>Lý do từ chối:</strong> {o.rejectReason}
                      </div>
                    )}
                  </div>

                  {o.status === 'PENDING' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, minWidth: 200 }}>
                      {rejecting?.id === o.id ? (
                        <>
                          <textarea
                            autoFocus
                            maxLength={1000}
                            rows={3}
                            value={rejecting.reason}
                            onChange={(e) => setRejecting({ id: o.id, reason: e.target.value })}
                            placeholder="Nhập lý do từ chối (bắt buộc)"
                            style={inputStyle}
                          />
                          <div style={{ display: 'flex', gap: 8 }}>
                            <button disabled={busy} onClick={() => reject(o.id, rejecting.reason)} style={{ ...buttonStyle, background: '#ef4444' }}>
                              {busy ? 'Đang gửi…' : 'Xác nhận từ chối'}
                            </button>
                            <button onClick={() => setRejecting(null)} style={buttonGhostStyle}>Hủy</button>
                          </div>
                        </>
                      ) : (
                        <>
                          <button disabled={busy} onClick={() => accept(o)} style={buttonStyle}>
                            ✓ Chấp nhận
                          </button>
                          <button disabled={busy} onClick={() => setRejecting({ id: o.id, reason: '' })} style={{ ...buttonStyle, background: '#ef4444' }}>
                            ✕ Từ chối
                          </button>
                        </>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

const fmt = (n: number) => n?.toLocaleString('vi-VN') ?? '0';

const tabStyle: React.CSSProperties = {
  padding: '8px 16px',
  borderRadius: 8,
  border: '1px solid #334155',
  background: 'transparent',
  color: '#94a3b8',
  fontSize: 13,
  fontWeight: 600,
  cursor: 'pointer',
};
const infoCellStyle: React.CSSProperties = { background: 'rgba(255,255,255,.03)', border: '1px solid rgba(255,255,255,.06)', borderRadius: 10, padding: '10px 12px' };
const infoLabelStyle: React.CSSProperties = { display: 'block', fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 4 };
const inputStyle: React.CSSProperties = { width: '100%', boxSizing: 'border-box', padding: '10px 12px', borderRadius: 8, border: '1px solid #334155', background: '#111827', color: '#fff', fontSize: 13 };
const buttonStyle: React.CSSProperties = { padding: '10px 16px', border: 0, borderRadius: 8, background: '#10b981', color: '#fff', fontWeight: 700, cursor: 'pointer' };
const buttonGhostStyle: React.CSSProperties = { padding: '10px 16px', border: '1px solid #334155', borderRadius: 8, background: 'transparent', color: '#94a3b8', fontWeight: 600, cursor: 'pointer' };
const alertStyle: React.CSSProperties = { padding: 12, margin: '12px 0', border: '1px solid #ef4444', borderRadius: 8, color: '#fecaca', background: 'rgba(239,68,68,.12)' };
