/**
 * BICAP-54 — Shipping: Xem đơn hàng đã thanh toán (DEPOSIT_PAID) chờ tạo lô vận chuyển.
 *
 * Shipping Manager xem danh sách đơn hàng đã đặt cọc, chưa có lô vận chuyển,
 * sau đó có thể chuyển sang trang tạo lô (BICAP-55).
 */
import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  panelStyle, titleStyle, cardStyle, badgeStyle, alertStyle, buttonStyle,
} from '../FarmManager/ui';

type CompletedOrder = {
  id: number;
  status: string;
  productName?: string;
  productId?: number;
  retailerName?: string;
  retailerPhone?: string;
  farmName?: string;
  quantity?: number;
  price?: number;
  totalAmount?: number;
  deliveryAddr?: string;
  depositCode?: string;
  createdAt?: string;
  desiredDeliveryDate?: string;
  notes?: string;
};

interface Props {
  onCreateShipment?: (order: CompletedOrder) => void;
}

export default function CompletedOrdersPage({ onCreateShipment }: Props) {
  const [orders, setOrders] = useState<CompletedOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/orders/completed`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setOrders(await res.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được danh sách đơn hàng.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load().catch(() => {}); }, []);

  const filtered = orders.filter(o => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      String(o.id).includes(q) ||
      (o.productName || '').toLowerCase().includes(q) ||
      (o.retailerName || '').toLowerCase().includes(q) ||
      (o.deliveryAddr || '').toLowerCase().includes(q)
    );
  });

  return (
    <div>
      <h1 className="dashboard-title">Đơn hàng chờ vận chuyển</h1>
      <p className="dashboard-subtitle">
        Danh sách đơn hàng đã thanh toán đặt cọc (DEPOSIT_PAID) — sẵn sàng để tạo lô vận chuyển.
      </p>

      {error && <div style={alertStyle}>{error}</div>}

      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <input
          placeholder="Tìm kiếm theo mã đơn, sản phẩm, nhà bán lẻ, địa chỉ…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{
            flex: 1, minWidth: 240, padding: '10px 14px',
            borderRadius: 8, border: '1px solid #334155',
            background: '#111827', color: '#fff', fontSize: 13,
          }}
        />
        <button
          onClick={() => load()}
          style={{ ...buttonStyle, width: 'auto', padding: '10px 20px', marginTop: 0 }}
        >
          🔄 Làm mới
        </button>
      </div>

      {loading && (
        <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>
          Đang tải…
        </div>
      )}

      {!loading && !error && filtered.length === 0 && (
        <div className="glass-panel" style={{ ...panelStyle, textAlign: 'center', color: '#94a3b8' }}>
          {orders.length === 0
            ? 'Không có đơn hàng nào chờ vận chuyển.'
            : 'Không tìm thấy đơn hàng phù hợp.'}
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        {filtered.map(order => (
          <div key={order.id} className="glass-panel" style={{ ...cardStyle, padding: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
                  <span style={{ color: '#fff', fontWeight: 700, fontSize: 16 }}>
                    Đơn hàng #{order.id}
                  </span>
                  <span style={badgeStyle(order.status)}>{order.status}</span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '6px 20px', fontSize: 13, color: '#94a3b8' }}>
                  {order.productName && (
                    <span>📦 <strong style={{ color: '#cbd5e1' }}>{order.productName}</strong></span>
                  )}
                  {order.retailerName && (
                    <span>🏪 {order.retailerName}{order.retailerPhone ? `, ${order.retailerPhone}` : ''}</span>
                  )}
                  {order.farmName && (
                    <span>🌾 {order.farmName}</span>
                  )}
                  {order.deliveryAddr && (
                    <span>📍 {order.deliveryAddr}</span>
                  )}
                  {order.quantity !== undefined && order.price !== undefined && (
                    <span>
                      🔢 {order.quantity} kg × {order.price?.toLocaleString('vi-VN')} đ
                      {order.totalAmount !== undefined && (
                        <strong style={{ color: '#6ee7b7', marginLeft: 6 }}>
                          = {order.totalAmount.toLocaleString('vi-VN')} đ
                        </strong>
                      )}
                    </span>
                  )}
                  {order.desiredDeliveryDate && (
                    <span>📅 Giao trước: {order.desiredDeliveryDate}</span>
                  )}
                  {order.createdAt && (
                    <span>🕐 {new Date(order.createdAt).toLocaleString('vi-VN')}</span>
                  )}
                  {order.notes && (
                    <span style={{ gridColumn: '1 / -1' }}>📝 {order.notes}</span>
                  )}
                </div>
              </div>

              {onCreateShipment && (
                <button
                  onClick={() => onCreateShipment(order)}
                  style={{
                    padding: '10px 20px',
                    border: 0,
                    borderRadius: 8,
                    background: 'linear-gradient(135deg, #10b981, #06b6d4)',
                    color: '#fff',
                    fontWeight: 700,
                    cursor: 'pointer',
                    fontSize: 13,
                    whiteSpace: 'nowrap',
                  }}
                >
                  🚚 Tạo lô vận chuyển
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {!loading && filtered.length > 0 && (
        <p style={{ marginTop: 16, color: '#64748b', fontSize: 12 }}>
          Hiển thị {filtered.length} / {orders.length} đơn hàng
        </p>
      )}
    </div>
  );
}
