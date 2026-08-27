import React, { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

// ─── Types ───────────────────────────────────────────────────────────────────

type OrderItem = {
  id: number;
  status: string;
  productName?: string;
  retailerName?: string;
  retailerEmail?: string;
  quantity?: number;
  totalAmount?: number;
  deliveryAddr?: string;
  depositAmount?: number;
  createdAt?: string;
};

type Vehicle = {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status: string;
};

type Driver = {
  id: number;
  fullName?: string;
  phone?: string;
  licenseNumber: string;
  citizenId: string;
  status: string;
  vehicleLicensePlate?: string;
};

type TrackingEntry = {
  id: number;
  status: string;
  gpsLat: number;
  gpsLng: number;
  notes?: string;
  timestamp?: string;
};

type Shipment = {
  id: number;
  status: string;
  orderId: number;
  orderStatus?: string;
  productName?: string;
  retailerName?: string;
  retailerEmail?: string;
  quantity?: number;
  totalAmount?: number;
  deliveryAddr?: string;
  driverName?: string;
  driverPhone?: string;
  driverLicenseNumber?: string;
  vehicleLicensePlate?: string;
  vehicleType?: string;
  routeSummary?: string;
  pickupTime?: string;
  deliveryTime?: string;
  createdAt?: string;
  trackingHistory?: TrackingEntry[];
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '10px 12px', marginTop: 6, borderRadius: 8,
  border: '1px solid rgba(255,255,255,.15)', background: 'rgba(15,23,42,.6)',
  color: '#fff', boxSizing: 'border-box', fontSize: 14,
};

const fmtCurrency = (v?: number) =>
  v == null ? '—' : v.toLocaleString('vi-VN') + ' ₫';

const fmtDate = (v?: string) =>
  v ? new Date(v).toLocaleString('vi-VN') : '—';

const statusColor = (s: string) => {
  switch (s) {
    case 'PICKING_UP': return '#f59e0b';
    case 'IN_TRANSIT': return '#3b82f6';
    case 'DELIVERED':  return '#10b981';
    case 'RETURNED':   return '#ef4444';
    case 'AVAILABLE':  return '#10b981';
    case 'IN_USE':     return '#f59e0b';
    case 'MAINTENANCE':return '#ef4444';
    case 'IDLE':       return '#10b981';
    case 'ON_TRIP':    return '#3b82f6';
    case 'OFFLINE':    return '#6b7280';
    case 'DEPOSIT_PAID': return '#8b5cf6';
    default: return '#94a3b8';
  }
};

const statusLabel = (s: string) => {
  const map: Record<string, string> = {
    PICKING_UP: 'Đang lấy hàng', IN_TRANSIT: 'Đang vận chuyển',
    DELIVERED: 'Đã giao', RETURNED: 'Đã hủy/Hoàn trả',
    AVAILABLE: 'Sẵn sàng', IN_USE: 'Đang sử dụng', MAINTENANCE: 'Bảo trì',
    IDLE: 'Sẵn sàng', ON_TRIP: 'Đang chuyến', OFFLINE: 'Ngoại tuyến',
    DEPOSIT_PAID: 'Đã cọc - Chờ giao',
  };
  return map[s] || s;
};

// ─── Components ──────────────────────────────────────────────────────────────

/** BICAP-54: Danh sách đơn hàng chờ vận chuyển */
function CompletedOrdersTab() {
  const [orders, setOrders] = useState<OrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API_BASE_URL}/shipping/orders/completed`, { headers: getAuthHeaders() })
      .then(async r => {
        const data = await r.json().catch(() => ({}));
        if (!r.ok) throw new Error(data.message || 'Không thể tải đơn hàng');
        setOrders(Array.isArray(data) ? data : []);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p style={{ color: '#94a3b8' }}>Đang tải...</p>;
  if (error) return <p style={{ color: '#f87171' }}>{error}</p>;

  return (
    <div>
      <h2 style={{ color: '#fff', marginBottom: 16 }}>
        Đơn hàng chờ vận chuyển
        <span style={{ marginLeft: 10, fontSize: 14, color: '#94a3b8', fontWeight: 400 }}>
          ({orders.length} đơn)
        </span>
      </h2>

      {orders.length === 0 ? (
        <div className="glass-panel" style={{ padding: 32, textAlign: 'center' }}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>📦</div>
          <p style={{ color: '#94a3b8' }}>Không có đơn hàng nào đang chờ vận chuyển.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 16 }}>
          {orders.map(order => (
            <div key={order.id} className="glass-panel" style={{ padding: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                    <span style={{ fontWeight: 700, color: '#fff', fontSize: 16 }}>#{order.id}</span>
                    <span style={{
                      fontSize: 12, padding: '2px 10px', borderRadius: 20,
                      background: `${statusColor(order.status)}22`,
                      color: statusColor(order.status),
                      border: `1px solid ${statusColor(order.status)}44`,
                    }}>
                      {statusLabel(order.status)}
                    </span>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))', gap: 12 }}>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b', marginBottom: 2 }}>Sản phẩm</dt>
                      <dd style={{ color: '#e2e8f0', fontSize: 14 }}>{order.productName || '—'}</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b', marginBottom: 2 }}>Nhà bán lẻ</dt>
                      <dd style={{ color: '#e2e8f0', fontSize: 14 }}>{order.retailerName || '—'}</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b', marginBottom: 2 }}>Số lượng</dt>
                      <dd style={{ color: '#e2e8f0', fontSize: 14 }}>{order.quantity ?? '—'} kg</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b', marginBottom: 2 }}>Tổng tiền</dt>
                      <dd style={{ color: '#34d399', fontSize: 14, fontWeight: 600 }}>
                        {fmtCurrency(order.totalAmount)}
                      </dd>
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                      <dt style={{ fontSize: 11, color: '#64748b', marginBottom: 2 }}>Địa chỉ giao hàng</dt>
                      <dd style={{ color: '#e2e8f0', fontSize: 14 }}>{order.deliveryAddr || '—'}</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b', marginBottom: 2 }}>Ngày đặt</dt>
                      <dd style={{ color: '#94a3b8', fontSize: 13 }}>{fmtDate(order.createdAt)}</dd>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/** BICAP-55: Tạo lô vận chuyển */
function CreateShipmentTab() {
  const [orders, setOrders] = useState<OrderItem[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ text: string; ok: boolean } | null>(null);

  const [form, setForm] = useState({
    orderId: '', driverId: '', vehicleId: '', routeSummary: '',
  });

  useEffect(() => {
    Promise.all([
      fetch(`${API_BASE_URL}/shipping/orders/completed`, { headers: getAuthHeaders() }),
      fetch(`${API_BASE_URL}/shipping/vehicles`, { headers: getAuthHeaders() }),
      fetch(`${API_BASE_URL}/shipping/drivers`, { headers: getAuthHeaders() }),
    ]).then(async ([oRes, vRes, dRes]) => {
      const [oData, vData, dData] = await Promise.all([
        oRes.json().catch(() => []),
        vRes.json().catch(() => []),
        dRes.json().catch(() => []),
      ]);
      setOrders(Array.isArray(oData) ? oData : []);
      setVehicles(Array.isArray(vData) ? vData : []);
      setDrivers(Array.isArray(dData) ? dData : []);
    }).catch(() => {
      setMessage({ text: 'Không thể tải dữ liệu cần thiết.', ok: false });
    }).finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.orderId || !form.driverId || !form.vehicleId) {
      setMessage({ text: 'Vui lòng chọn đơn hàng, tài xế và phương tiện.', ok: false });
      return;
    }
    setSaving(true); setMessage(null);
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/shipments`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          orderId: Number(form.orderId),
          driverId: Number(form.driverId),
          vehicleId: Number(form.vehicleId),
          routeSummary: form.routeSummary.trim() || null,
        }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || 'Không thể tạo lô vận chuyển');
      setMessage({ text: `✅ Đã tạo lô vận chuyển #${data.id} thành công!`, ok: true });
      setForm({ orderId: '', driverId: '', vehicleId: '', routeSummary: '' });
      // Reload orders list
      fetch(`${API_BASE_URL}/shipping/orders/completed`, { headers: getAuthHeaders() })
        .then(r => r.json()).then(d => setOrders(Array.isArray(d) ? d : [])).catch(() => {});
    } catch (err) {
      setMessage({ text: err instanceof Error ? err.message : 'Lỗi tạo lô vận chuyển', ok: false });
    } finally { setSaving(false); }
  };

  if (loading) return <p style={{ color: '#94a3b8' }}>Đang tải...</p>;

  const availableVehicles = vehicles.filter(v => v.status === 'AVAILABLE');
  const idleDrivers = drivers.filter(d => d.status === 'IDLE');

  return (
    <div style={{ maxWidth: 700 }}>
      <h2 style={{ color: '#fff', marginBottom: 20 }}>Tạo lô vận chuyển mới</h2>

      {message && (
        <p style={{
          color: message.ok ? '#34d399' : '#f87171',
          background: message.ok ? 'rgba(52,211,153,.1)' : 'rgba(248,113,113,.1)',
          padding: '10px 16px', borderRadius: 8, marginBottom: 20,
          border: `1px solid ${message.ok ? 'rgba(52,211,153,.3)' : 'rgba(248,113,113,.3)'}`,
        }}>
          {message.text}
        </p>
      )}

      <form className="glass-panel" onSubmit={handleSubmit} style={{ padding: 28, display: 'grid', gap: 20 }}>

        {/* Order select */}
        <label>
          <span style={{ color: '#94a3b8', fontSize: 13, fontWeight: 600 }}>
            Đơn hàng chờ vận chuyển <span style={{ color: '#f87171' }}>*</span>
          </span>
          <select
            required
            value={form.orderId}
            onChange={e => setForm(f => ({ ...f, orderId: e.target.value }))}
            style={inputStyle}
          >
            <option value="">-- Chọn đơn hàng --</option>
            {orders.map(o => (
              <option key={o.id} value={o.id}>
                #{o.id} — {o.productName || 'Sản phẩm'} | {o.retailerName || '?'} | {fmtCurrency(o.totalAmount)}
              </option>
            ))}
          </select>
          {orders.length === 0 && (
            <p style={{ color: '#f59e0b', fontSize: 12, marginTop: 4 }}>
              Không có đơn hàng nào chờ vận chuyển.
            </p>
          )}
        </label>

        {/* Driver select */}
        <label>
          <span style={{ color: '#94a3b8', fontSize: 13, fontWeight: 600 }}>
            Tài xế (IDLE) <span style={{ color: '#f87171' }}>*</span>
          </span>
          <select
            required
            value={form.driverId}
            onChange={e => setForm(f => ({ ...f, driverId: e.target.value }))}
            style={inputStyle}
          >
            <option value="">-- Chọn tài xế --</option>
            {idleDrivers.map(d => (
              <option key={d.id} value={d.id}>
                {d.fullName || `Tài xế #${d.id}`} | {d.licenseNumber}
                {d.vehicleLicensePlate ? ` | 🚚 ${d.vehicleLicensePlate}` : ''}
              </option>
            ))}
          </select>
          {idleDrivers.length === 0 && (
            <p style={{ color: '#f59e0b', fontSize: 12, marginTop: 4 }}>
              Không có tài xế nào đang sẵn sàng (IDLE).
            </p>
          )}
        </label>

        {/* Vehicle select */}
        <label>
          <span style={{ color: '#94a3b8', fontSize: 13, fontWeight: 600 }}>
            Phương tiện (AVAILABLE) <span style={{ color: '#f87171' }}>*</span>
          </span>
          <select
            required
            value={form.vehicleId}
            onChange={e => setForm(f => ({ ...f, vehicleId: e.target.value }))}
            style={inputStyle}
          >
            <option value="">-- Chọn phương tiện --</option>
            {availableVehicles.map(v => (
              <option key={v.id} value={v.id}>
                {v.licensePlate} | {v.type} | {v.capacity} tấn
              </option>
            ))}
          </select>
          {availableVehicles.length === 0 && (
            <p style={{ color: '#f59e0b', fontSize: 12, marginTop: 4 }}>
              Không có phương tiện nào sẵn sàng (AVAILABLE).
            </p>
          )}
        </label>

        {/* Route summary */}
        <label>
          <span style={{ color: '#94a3b8', fontSize: 13, fontWeight: 600 }}>Lộ trình (không bắt buộc)</span>
          <input
            type="text"
            maxLength={500}
            placeholder="Ví dụ: Đồng Nai → TP.HCM"
            value={form.routeSummary}
            onChange={e => setForm(f => ({ ...f, routeSummary: e.target.value }))}
            style={inputStyle}
          />
        </label>

        <button
          type="submit"
          disabled={saving || orders.length === 0 || idleDrivers.length === 0 || availableVehicles.length === 0}
          style={{
            padding: '12px 28px', borderRadius: 8, border: 'none', cursor: 'pointer',
            background: 'linear-gradient(135deg, #10b981, #06b6d4)',
            color: '#fff', fontWeight: 700, fontSize: 15, opacity: saving ? 0.7 : 1,
          }}
        >
          {saving ? 'Đang tạo...' : '🚚 Tạo lô vận chuyển'}
        </button>
      </form>
    </div>
  );
}

/** BICAP-56: Hủy lô vận chuyển + danh sách lô */
function ShipmentListTab({ onViewDetail }: { onViewDetail: (id: number) => void }) {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; ok: boolean } | null>(null);
  const [cancelling, setCancelling] = useState<number | null>(null);

  const loadShipments = () => {
    setLoading(true);
    fetch(`${API_BASE_URL}/shipping/shipments`, { headers: getAuthHeaders() })
      .then(async r => {
        const data = await r.json().catch(() => []);
        if (!r.ok) throw new Error(data.message || 'Không thể tải danh sách');
        setShipments(Array.isArray(data) ? data : []);
      })
      .catch(e => setMessage({ text: e.message, ok: false }))
      .finally(() => setLoading(false));
  };

  useEffect(loadShipments, []);

  const handleCancel = async (id: number) => {
    if (!confirm(`Bạn có chắc muốn hủy lô vận chuyển #${id}?`)) return;
    setCancelling(id);
    setMessage(null);
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/shipments/${id}/cancel`, {
        method: 'PUT', headers: getAuthHeaders(),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || 'Không thể hủy');
      setMessage({ text: `✅ ${data.message}`, ok: true });
      loadShipments();
    } catch (e) {
      setMessage({ text: e instanceof Error ? e.message : 'Lỗi hủy lô', ok: false });
    } finally { setCancelling(null); }
  };

  if (loading) return <p style={{ color: '#94a3b8' }}>Đang tải...</p>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ color: '#fff' }}>
          Danh sách lô vận chuyển
          <span style={{ marginLeft: 10, fontSize: 14, color: '#94a3b8', fontWeight: 400 }}>
            ({shipments.length} lô)
          </span>
        </h2>
        <button
          onClick={loadShipments}
          style={{ padding: '6px 14px', borderRadius: 8, border: '1px solid rgba(255,255,255,.15)', background: 'transparent', color: '#94a3b8', cursor: 'pointer', fontSize: 13 }}
        >
          🔄 Làm mới
        </button>
      </div>

      {message && (
        <p style={{
          color: message.ok ? '#34d399' : '#f87171',
          padding: '10px 16px', borderRadius: 8, marginBottom: 16,
          background: message.ok ? 'rgba(52,211,153,.1)' : 'rgba(248,113,113,.1)',
          border: `1px solid ${message.ok ? 'rgba(52,211,153,.3)' : 'rgba(248,113,113,.3)'}`,
        }}>
          {message.text}
        </p>
      )}

      {shipments.length === 0 ? (
        <div className="glass-panel" style={{ padding: 32, textAlign: 'center' }}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>🚚</div>
          <p style={{ color: '#94a3b8' }}>Chưa có lô vận chuyển nào.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 14 }}>
          {shipments.map(s => (
            <div key={s.id} className="glass-panel" style={{ padding: 18 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
                    <span style={{ fontWeight: 700, color: '#fff', fontSize: 16 }}>Lô #{s.id}</span>
                    <span style={{
                      fontSize: 12, padding: '2px 10px', borderRadius: 20,
                      background: `${statusColor(s.status)}22`, color: statusColor(s.status),
                      border: `1px solid ${statusColor(s.status)}44`,
                    }}>
                      {statusLabel(s.status)}
                    </span>
                    <span style={{ fontSize: 12, color: '#64748b' }}>Đơn #{s.orderId}</span>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(180px,1fr))', gap: 10 }}>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b' }}>Sản phẩm</dt>
                      <dd style={{ color: '#e2e8f0', fontSize: 13 }}>{s.productName || '—'}</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b' }}>Tài xế</dt>
                      <dd style={{ color: '#e2e8f0', fontSize: 13 }}>{s.driverName || '—'}</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b' }}>Phương tiện</dt>
                      <dd style={{ color: '#e2e8f0', fontSize: 13 }}>{s.vehicleLicensePlate || '—'} {s.vehicleType ? `(${s.vehicleType})` : ''}</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b' }}>Giao tới</dt>
                      <dd style={{ color: '#e2e8f0', fontSize: 13 }}>{s.deliveryAddr || '—'}</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b' }}>Tổng tiền</dt>
                      <dd style={{ color: '#34d399', fontSize: 13, fontWeight: 600 }}>{fmtCurrency(s.totalAmount)}</dd>
                    </div>
                    <div>
                      <dt style={{ fontSize: 11, color: '#64748b' }}>Tạo lúc</dt>
                      <dd style={{ color: '#94a3b8', fontSize: 12 }}>{fmtDate(s.createdAt)}</dd>
                    </div>
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, flexShrink: 0 }}>
                  <button
                    onClick={() => onViewDetail(s.id)}
                    style={{
                      padding: '7px 16px', borderRadius: 8,
                      background: 'rgba(59,130,246,.15)', border: '1px solid rgba(59,130,246,.3)',
                      color: '#93c5fd', cursor: 'pointer', fontSize: 13, fontWeight: 600,
                    }}
                  >
                    🔍 Chi tiết
                  </button>
                  {s.status === 'PICKING_UP' && (
                    <button
                      onClick={() => handleCancel(s.id)}
                      disabled={cancelling === s.id}
                      style={{
                        padding: '7px 16px', borderRadius: 8,
                        background: 'rgba(239,68,68,.15)', border: '1px solid rgba(239,68,68,.3)',
                        color: '#f87171', cursor: 'pointer', fontSize: 13, fontWeight: 600,
                        opacity: cancelling === s.id ? 0.6 : 1,
                      }}
                    >
                      {cancelling === s.id ? 'Đang hủy...' : '✕ Hủy lô'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/** BICAP-57: Chi tiết lô vận chuyển + tracking */
function ShipmentDetailView({ shipmentId, onBack }: { shipmentId: number; onBack: () => void }) {
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API_BASE_URL}/shipping/shipments/${shipmentId}`, { headers: getAuthHeaders() })
      .then(async r => {
        const data = await r.json().catch(() => ({}));
        if (!r.ok) throw new Error(data.message || 'Không tìm thấy lô vận chuyển');
        setShipment(data);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [shipmentId]);

  if (loading) return <p style={{ color: '#94a3b8' }}>Đang tải...</p>;
  if (error || !shipment) return (
    <div>
      <button onClick={onBack} style={{ marginBottom: 16, background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: 14 }}>← Quay lại</button>
      <p style={{ color: '#f87171' }}>{error || 'Không tìm thấy lô vận chuyển'}</p>
    </div>
  );

  return (
    <div>
      <button
        onClick={onBack}
        style={{ marginBottom: 16, background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: 14 }}
      >
        ← Quay lại danh sách
      </button>

      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 24 }}>
        <h2 style={{ color: '#fff', margin: 0 }}>Lô vận chuyển #{shipment.id}</h2>
        <span style={{
          fontSize: 13, padding: '3px 14px', borderRadius: 20,
          background: `${statusColor(shipment.status)}22`, color: statusColor(shipment.status),
          border: `1px solid ${statusColor(shipment.status)}44`,
        }}>
          {statusLabel(shipment.status)}
        </span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(300px,1fr))', gap: 20, marginBottom: 24 }}>
        {/* Order Info */}
        <div className="glass-panel" style={{ padding: 20 }}>
          <h3 style={{ color: '#10b981', fontSize: 14, marginBottom: 14 }}>📦 Thông tin đơn hàng</h3>
          <dl style={{ display: 'grid', gap: 10 }}>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Mã đơn</dt><dd style={{ color: '#e2e8f0' }}>#{shipment.orderId}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Sản phẩm</dt><dd style={{ color: '#e2e8f0' }}>{shipment.productName || '—'}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Số lượng</dt><dd style={{ color: '#e2e8f0' }}>{shipment.quantity ?? '—'} kg</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Tổng tiền</dt><dd style={{ color: '#34d399', fontWeight: 600 }}>{fmtCurrency(shipment.totalAmount)}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Nhà bán lẻ</dt><dd style={{ color: '#e2e8f0' }}>{shipment.retailerName || '—'} ({shipment.retailerEmail || ''})</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Giao tới</dt><dd style={{ color: '#e2e8f0' }}>{shipment.deliveryAddr || '—'}</dd></div>
          </dl>
        </div>

        {/* Driver & Vehicle */}
        <div className="glass-panel" style={{ padding: 20 }}>
          <h3 style={{ color: '#3b82f6', fontSize: 14, marginBottom: 14 }}>🚚 Tài xế & Phương tiện</h3>
          <dl style={{ display: 'grid', gap: 10 }}>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Tài xế</dt><dd style={{ color: '#e2e8f0' }}>{shipment.driverName || '—'}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>SĐT tài xế</dt><dd style={{ color: '#e2e8f0' }}>{shipment.driverPhone || '—'}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Bằng lái</dt><dd style={{ color: '#e2e8f0' }}>{shipment.driverLicenseNumber || '—'}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Biển số xe</dt><dd style={{ color: '#e2e8f0' }}>{shipment.vehicleLicensePlate || '—'}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Loại xe</dt><dd style={{ color: '#e2e8f0' }}>{shipment.vehicleType || '—'}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Lộ trình</dt><dd style={{ color: '#e2e8f0' }}>{shipment.routeSummary || '—'}</dd></div>
          </dl>
        </div>

        {/* Timeline */}
        <div className="glass-panel" style={{ padding: 20 }}>
          <h3 style={{ color: '#8b5cf6', fontSize: 14, marginBottom: 14 }}>⏱ Thời gian</h3>
          <dl style={{ display: 'grid', gap: 10 }}>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Tạo lô</dt><dd style={{ color: '#e2e8f0' }}>{fmtDate(shipment.createdAt)}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Lấy hàng</dt><dd style={{ color: '#e2e8f0' }}>{fmtDate(shipment.pickupTime)}</dd></div>
            <div><dt style={{ fontSize: 11, color: '#64748b' }}>Giao hàng</dt><dd style={{ color: '#e2e8f0' }}>{fmtDate(shipment.deliveryTime)}</dd></div>
          </dl>
        </div>
      </div>

      {/* Tracking history (BICAP-57) */}
      <div className="glass-panel" style={{ padding: 24 }}>
        <h3 style={{ color: '#fff', marginBottom: 16 }}>
          📍 Lịch sử tracking
          <span style={{ marginLeft: 8, fontSize: 13, color: '#64748b', fontWeight: 400 }}>
            ({shipment.trackingHistory?.length ?? 0} bản ghi)
          </span>
        </h3>

        {!shipment.trackingHistory || shipment.trackingHistory.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <div style={{ fontSize: 32, marginBottom: 8 }}>📍</div>
            <p style={{ color: '#64748b' }}>Chưa có bản ghi tracking nào.</p>
          </div>
        ) : (
          <div style={{ position: 'relative', paddingLeft: 28 }}>
            {/* Timeline line */}
            <div style={{
              position: 'absolute', left: 8, top: 6, bottom: 6,
              width: 2, background: 'rgba(59,130,246,.3)',
            }} />

            {shipment.trackingHistory.map((t, idx) => (
              <div key={t.id} style={{ position: 'relative', marginBottom: 20, paddingLeft: 12 }}>
                {/* dot */}
                <div style={{
                  position: 'absolute', left: -20, top: 4,
                  width: 12, height: 12, borderRadius: '50%',
                  background: idx === (shipment.trackingHistory!.length - 1) ? statusColor(t.status) : '#334155',
                  border: `2px solid ${statusColor(t.status)}`,
                }} />
                <div style={{ background: 'rgba(255,255,255,.03)', borderRadius: 8, padding: '10px 14px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
                    <span style={{
                      fontSize: 12, padding: '2px 8px', borderRadius: 20,
                      background: `${statusColor(t.status)}22`, color: statusColor(t.status),
                      border: `1px solid ${statusColor(t.status)}44`,
                    }}>
                      {statusLabel(t.status)}
                    </span>
                    <span style={{ fontSize: 12, color: '#64748b' }}>{fmtDate(t.timestamp)}</span>
                  </div>
                  <div style={{ display: 'flex', gap: 16 }}>
                    <span style={{ fontSize: 12, color: '#94a3b8' }}>
                      📍 {t.gpsLat.toFixed(5)}, {t.gpsLng.toFixed(5)}
                    </span>
                    {t.notes && <span style={{ fontSize: 12, color: '#94a3b8' }}>📝 {t.notes}</span>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Main ShippingPage ────────────────────────────────────────────────────────

type ShippingTab = 'orders' | 'create' | 'shipments';

export default function ShippingPage() {
  const [activeTab, setActiveTab] = useState<ShippingTab>('orders');
  const [detailId, setDetailId] = useState<number | null>(null);

  const tabs: { id: ShippingTab; label: string; icon: string }[] = [
    { id: 'orders',   label: 'Đơn hàng chờ vận chuyển', icon: '📦' },
    { id: 'create',   label: 'Tạo lô vận chuyển',        icon: '🚚' },
    { id: 'shipments',label: 'Quản lý lô',               icon: '📋' },
  ];

  return (
    <div style={{ maxWidth: 1100 }}>
      <div style={{ marginBottom: 24 }}>
        <h1 className="dashboard-title">Quản Lý Vận Chuyển</h1>
        <p className="dashboard-subtitle">
          BICAP-54 · BICAP-55 · BICAP-56 · BICAP-57 — Shipping Management
        </p>
      </div>

      {/* Tabs */}
      {!detailId && (
        <nav style={{ display: 'flex', gap: 8, marginBottom: 28, borderBottom: '1px solid rgba(255,255,255,.08)', paddingBottom: 0 }}>
          {tabs.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                padding: '10px 20px', borderRadius: '8px 8px 0 0',
                border: 'none',
                background: activeTab === tab.id ? 'rgba(16,185,129,.15)' : 'transparent',
                color: activeTab === tab.id ? '#34d399' : '#64748b',
                fontWeight: activeTab === tab.id ? 700 : 400,
                cursor: 'pointer', fontSize: 14, display: 'flex', alignItems: 'center', gap: 8,
                borderBottom: activeTab === tab.id ? '2px solid #10b981' : '2px solid transparent',
                transition: 'all .2s',
              }}
            >
              {tab.icon} {tab.label}
            </button>
          ))}
        </nav>
      )}

      {/* Content */}
      {detailId ? (
        <ShipmentDetailView
          shipmentId={detailId}
          onBack={() => setDetailId(null)}
        />
      ) : activeTab === 'orders' ? (
        <CompletedOrdersTab />
      ) : activeTab === 'create' ? (
        <CreateShipmentTab />
      ) : (
        <ShipmentListTab onViewDetail={id => setDetailId(id)} />
      )}
    </div>
  );
}
