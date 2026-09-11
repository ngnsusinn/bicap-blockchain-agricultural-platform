/**
 * BICAP-54 / F8 — Shipping: danh sách đơn hàng dùng để tạo lô vận chuyển.
 *
 * Tab chính ("Đơn đủ điều kiện tạo vận chuyển") gọi `GET /api/shipping/orders/ready-to-ship`:
 * các đơn đã thanh toán cọc và CHƯA có lô vận chuyển — đây mới là danh sách Shipping
 * Manager cần để tạo shipment.
 *
 * Tab phụ ("Đơn đã hoàn tất") gọi `GET /api/shipping/orders/completed` và được tách
 * hoàn toàn khỏi danh sách tạo vận chuyển.
 */
import { useCallback, useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  panelStyle, cardStyle, badgeStyle, alertStyle, buttonStyle,
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
  deliveredAt?: string;
  completedAt?: string;
  notes?: string;
};

type OrderView = 'ready' | 'completed';

const VIEWS: Record<OrderView, { path: string; title: string; subtitle: string; empty: string }> = {
  ready: {
    path: '/shipping/orders/ready-to-ship',
    title: 'Đơn đủ điều kiện tạo vận chuyển',
    subtitle: 'Đơn đã cọc, chưa có lô vận chuyển — sẵn sàng để tạo lô vận chuyển.',
    empty: 'Không có đơn hàng nào đủ điều kiện tạo vận chuyển.',
  },
  completed: {
    path: '/shipping/orders/completed',
    title: 'Đơn đã hoàn tất',
    subtitle: 'Các đơn hàng đã được giao và xác nhận hoàn tất.',
    empty: 'Chưa có đơn hàng nào hoàn tất.',
  },
};

interface Props {
  onCreateShipment?: (order: CompletedOrder) => void;
}

export default function CompletedOrdersPage({ onCreateShipment }: Props) {
  const [view, setView] = useState<OrderView>('ready');
  const [orders, setOrders] = useState<CompletedOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  const load = useCallback(async (target: OrderView = view) => {
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE_URL}${VIEWS[target].path}`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setOrders(await res.json());
    } catch (err) {
      setOrders([]);
      setError(err instanceof Error ? err.message : 'Không tải được danh sách đơn hàng.');
    } finally {
      setLoading(false);
    }
  }, [view]);

  useEffect(() => { load(view).catch(() => {}); }, [load, view]);

  const meta = VIEWS[view];

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
      <h1 className="dashboard-title">{meta.title}</h1>
      <p className="dashboard-subtitle">{meta.subtitle}</p>

      {/* Tách rõ hai danh sách: tạo vận chuyển vs. đã hoàn tất */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
        {(['ready', 'completed'] as OrderView[]).map(v => (
          <button
            key={v}
            type="button"
            onClick={() => { setSearch(''); setView(v); }}
            style={{
              padding: '8px 16px',
              borderRadius: 20,
              border: `1px solid ${view === v ? '#10b981' : 'rgba(255,255,255,0.12)'}`,
              background: view === v ? 'rgba(16,185,129,0.12)' : 'transparent',
              color: view === v ? '#34d399' : '#94a3b8',
              fontSize: 13,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {v === 'ready' ? '🚚 Đơn đủ điều kiện tạo vận chuyển' : '✅ Đơn đã hoàn tất'}
          </button>
        ))}
      </div>

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
          {orders.length === 0 ? meta.empty : 'Không tìm thấy đơn hàng phù hợp.'}
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
                  {view === 'completed' && order.completedAt && (
                    <span>✅ Hoàn tất: {new Date(order.completedAt).toLocaleString('vi-VN')}</span>
                  )}
                  {view === 'completed' && !order.completedAt && order.deliveredAt && (
                    <span>✅ Giao lúc: {new Date(order.deliveredAt).toLocaleString('vi-VN')}</span>
                  )}
                  {order.notes && (
                    <span style={{ gridColumn: '1 / -1' }}>📝 {order.notes}</span>
                  )}
                </div>
              </div>

              {view === 'ready' && onCreateShipment && (
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
