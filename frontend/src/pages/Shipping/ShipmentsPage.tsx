/**
 * BICAP-55 — Tạo lô vận chuyển
 * BICAP-56 — Hủy lô vận chuyển
 *
 * Shipping Manager quản lý danh sách lô vận chuyển, tạo mới từ đơn hàng DEPOSIT_PAID,
 * hủy lô khi còn ở trạng thái PICKING_UP (BR1).
 */
import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  panelStyle, titleStyle, cardStyle, badgeStyle, alertStyle, successStyle,
  buttonStyle, labelStyle, inputStyle, secondaryButtonStyle,
} from '../FarmManager/ui';

type ShipmentSummary = {
  id: number;
  status: string;
  orderId?: number;
  deliveryAddr?: string;
  driverId?: number;
  driverName?: string;
  driverPhone?: string;
  vehicleId?: number;
  vehicleLicensePlate?: string;
  vehicleType?: string;
  routeSummary?: string;
  createdAt?: string;
  pickupTime?: string;
  deliveryTime?: string;
};

type CompletedOrder = {
  id: number;
  status: string;
  productName?: string;
  retailerName?: string;
  deliveryAddr?: string;
  quantity?: number;
  price?: number;
  totalAmount?: number;
};

type Driver = { id: number; status: string; userName?: string; userPhone?: string; licenseNumber?: string; vehicleLicensePlate?: string };
type Vehicle = { id: number; status: string; licensePlate: string; type: string; capacity: number };

interface Props {
  initialOrderForCreate?: CompletedOrder | null;
  onTrack?: (shipment: ShipmentSummary) => void;
}

const STATUS_LABELS: Record<string, string> = {
  PICKING_UP: 'Đang lấy hàng',
  IN_TRANSIT: 'Đang vận chuyển',
  DELIVERED: 'Đã giao hàng',
  RETURNED: 'Đã hủy/hoàn trả',
};

export default function ShipmentsPage({ initialOrderForCreate, onTrack }: Props) {
  const [shipments, setShipments] = useState<ShipmentSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Create form
  const [showCreate, setShowCreate] = useState(!!initialOrderForCreate);
  const [completedOrders, setCompletedOrders] = useState<CompletedOrder[]>([]);
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [createForm, setCreateForm] = useState({
    orderId: initialOrderForCreate ? String(initialOrderForCreate.id) : '',
    driverId: '',
    vehicleId: '',
    routeSummary: '',
  });
  const [createBusy, setCreateBusy] = useState(false);

  // Cancel
  const [cancelId, setCancelId] = useState<number | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelBusy, setCancelBusy] = useState(false);

  const loadShipments = async () => {
    setLoading(true);
    try {
      const url = statusFilter
        ? `${API_BASE_URL}/shipping/shipments?status=${statusFilter}`
        : `${API_BASE_URL}/shipping/shipments`;
      const res = await fetch(url, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error(`Lỗi ${res.status}`);
      setShipments(await res.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được danh sách lô vận chuyển.');
    } finally {
      setLoading(false);
    }
  };

  const loadCreateFormData = async () => {
    try {
      const [ordersRes, driversRes, vehiclesRes] = await Promise.all([
        fetch(`${API_BASE_URL}/shipping/orders/completed`, { headers: getAuthHeaders() }),
        fetch(`${API_BASE_URL}/shipping/drivers`, { headers: getAuthHeaders() }),
        fetch(`${API_BASE_URL}/shipping/vehicles`, { headers: getAuthHeaders() }),
      ]);
      if (ordersRes.ok) setCompletedOrders(await ordersRes.json());
      if (driversRes.ok) setDrivers(await driversRes.json());
      if (vehiclesRes.ok) setVehicles(await vehiclesRes.json());
    } catch {
      // Non-fatal
    }
  };

  useEffect(() => { loadShipments().catch(() => {}); }, [statusFilter]);

  useEffect(() => {
    if (showCreate) loadCreateFormData().catch(() => {});
  }, [showCreate]);

  useEffect(() => {
    if (initialOrderForCreate) {
      setShowCreate(true);
      setCreateForm(f => ({ ...f, orderId: String(initialOrderForCreate.id) }));
    }
  }, [initialOrderForCreate]);

  const submitCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice('');
    if (!createForm.orderId || !createForm.driverId || !createForm.vehicleId) {
      setError('Vui lòng chọn đơn hàng, tài xế và phương tiện.');
      return;
    }
    setCreateBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/shipments`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          orderId: Number(createForm.orderId),
          driverId: Number(createForm.driverId),
          vehicleId: Number(createForm.vehicleId),
          routeSummary: createForm.routeSummary || null,
        }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setNotice('Tạo lô vận chuyển thành công!');
      setShowCreate(false);
      setCreateForm({ orderId: '', driverId: '', vehicleId: '', routeSummary: '' });
      await loadShipments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Tạo lô vận chuyển thất bại.');
    } finally {
      setCreateBusy(false);
    }
  };

  const doCancel = async () => {
    if (cancelId == null) return;
    setCancelBusy(true);
    setError(''); setNotice('');
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/shipments/${cancelId}/cancel`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ reason: cancelReason || null }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setNotice('Đã hủy lô vận chuyển.');
      setCancelId(null);
      setCancelReason('');
      await loadShipments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Hủy lô vận chuyển thất bại.');
    } finally {
      setCancelBusy(false);
    }
  };

  const idleDrivers = drivers.filter(d => d.status === 'IDLE');
  const availableVehicles = vehicles.filter(v => v.status === 'AVAILABLE');

  return (
    <div>
      <h1 className="dashboard-title">Quản lý lô vận chuyển</h1>
      <p className="dashboard-subtitle">Tạo, theo dõi và hủy các lô vận chuyển.</p>

      {error && <div style={alertStyle}>{error}</div>}
      {notice && <div style={successStyle}>{notice}</div>}

      {/* Action bar */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap', alignItems: 'center' }}>
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ ...inputStyle, width: 'auto', minWidth: 160 }}
        >
          <option value="">Tất cả trạng thái</option>
          {Object.entries(STATUS_LABELS).map(([k, v]) => (
            <option key={k} value={k}>{v}</option>
          ))}
        </select>
        <button
          onClick={() => { setShowCreate(v => !v); if (!showCreate) setError(''); }}
          style={{ ...buttonStyle, width: 'auto', padding: '10px 20px', marginTop: 0 }}
        >
          {showCreate ? '✕ Đóng' : '+ Tạo lô mới'}
        </button>
        <button
          onClick={() => loadShipments()}
          style={{ ...secondaryButtonStyle }}
        >
          🔄 Làm mới
        </button>
      </div>

      {/* Create form — BICAP-55 */}
      {showCreate && (
        <div className="glass-panel" style={{ ...panelStyle, marginBottom: 24 }}>
          <h2 style={titleStyle}>Tạo lô vận chuyển mới</h2>
          <form onSubmit={submitCreate}>
            <label style={labelStyle}>Đơn hàng *</label>
            <select
              required
              value={createForm.orderId}
              onChange={e => setCreateForm(f => ({ ...f, orderId: e.target.value }))}
              style={inputStyle}
            >
              <option value="">-- Chọn đơn hàng DEPOSIT_PAID --</option>
              {completedOrders.map(o => (
                <option key={o.id} value={o.id}>
                  #{o.id} — {o.productName || 'Sản phẩm'}, {o.retailerName || 'Nhà bán lẻ'}
                  {o.deliveryAddr ? ` → ${o.deliveryAddr}` : ''}
                </option>
              ))}
            </select>

            <label style={labelStyle}>Tài xế * (chỉ hiển thị tài xế đang IDLE)</label>
            <select
              required
              value={createForm.driverId}
              onChange={e => setCreateForm(f => ({ ...f, driverId: e.target.value }))}
              style={inputStyle}
            >
              <option value="">-- Chọn tài xế --</option>
              {idleDrivers.map(d => (
                <option key={d.id} value={d.id}>
                  {d.userName || `Tài xế #${d.id}`}, GPLX: {d.licenseNumber}
                  {d.vehicleLicensePlate ? ` (xe: ${d.vehicleLicensePlate})` : ''}
                </option>
              ))}
              {idleDrivers.length === 0 && (
                <option disabled value="">Không có tài xế nào đang sảnh sàng</option>
              )}
            </select>

            <label style={labelStyle}>Phương tiện * (chỉ hiển thị xe AVAILABLE)</label>
            <select
              required
              value={createForm.vehicleId}
              onChange={e => setCreateForm(f => ({ ...f, vehicleId: e.target.value }))}
              style={inputStyle}
            >
              <option value="">-- Chọn phương tiện --</option>
              {availableVehicles.map(v => (
                <option key={v.id} value={v.id}>
                  {v.licensePlate} — {v.type} ({v.capacity} tấn)
                </option>
              ))}
              {availableVehicles.length === 0 && (
                <option disabled value="">Không có phương tiện nào khả dụng</option>
              )}
            </select>

            <label style={labelStyle}>Tuyến đường / Ghi chú (tuỳ chọn)</label>
            <input
              value={createForm.routeSummary}
              onChange={e => setCreateForm(f => ({ ...f, routeSummary: e.target.value }))}
              placeholder="VD: Hà Nội → TP.HCM qua QL1A"
              style={inputStyle}
            />

            <div style={{ display: 'flex', gap: 12, marginTop: 20 }}>
              <button type="submit" disabled={createBusy} style={{ ...buttonStyle, flex: 1, marginTop: 0 }}>
                {createBusy ? 'Đang tạo…' : '🚚 Tạo lô vận chuyển'}
              </button>
              <button
                type="button"
                onClick={() => { setShowCreate(false); setCreateForm({ orderId: '', driverId: '', vehicleId: '', routeSummary: '' }); }}
                style={{ ...secondaryButtonStyle, flex: '0 0 auto' }}
              >
                Hủy
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Shipment list */}
      {loading && (
        <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>Đang tải…</div>
      )}

      {!loading && shipments.length === 0 && (
        <div className="glass-panel" style={{ ...panelStyle, textAlign: 'center', color: '#94a3b8' }}>
          Không có lô vận chuyển nào.
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        {shipments.map(s => (
          <div key={s.id} className="glass-panel" style={{ ...cardStyle, padding: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                  <span style={{ color: '#fff', fontWeight: 700, fontSize: 16 }}>
                    Lô vận chuyển #{s.id}
                  </span>
                  <span style={badgeStyle(s.status)}>
                    {STATUS_LABELS[s.status] || s.status}
                  </span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '5px 20px', fontSize: 13, color: '#94a3b8' }}>
                  {s.orderId && <span>🧾 Đơn hàng #{s.orderId}</span>}
                  {s.deliveryAddr && <span>📍 {s.deliveryAddr}</span>}
                  {s.driverName && (
                    <span>🧑‍💼 {s.driverName}{s.driverPhone ? `, ${s.driverPhone}` : ''}</span>
                  )}
                  {s.vehicleLicensePlate && (
                    <span>🚛 {s.vehicleLicensePlate} ({s.vehicleType})</span>
                  )}
                  {s.routeSummary && <span style={{ gridColumn: '1 / -1' }}>🗺️ {s.routeSummary}</span>}
                  {s.pickupTime && <span>📦 Lấy hàng: {new Date(s.pickupTime).toLocaleString('vi-VN')}</span>}
                  {s.deliveryTime && <span>✅ Giao hàng: {new Date(s.deliveryTime).toLocaleString('vi-VN')}</span>}
                  {s.createdAt && <span>🕐 Tạo: {new Date(s.createdAt).toLocaleString('vi-VN')}</span>}
                </div>
              </div>

              <div style={{ display: 'flex', gap: 8, flexDirection: 'column', alignItems: 'flex-end' }}>
                {onTrack && (
                  <button
                    onClick={() => onTrack(s)}
                    style={{ ...secondaryButtonStyle, fontSize: 12 }}
                  >
                    🗺️ Xem tracking
                  </button>
                )}
                {/* BICAP-56: Cancel only when PICKING_UP */}
                {s.status === 'PICKING_UP' && (
                  <button
                    onClick={() => { setCancelId(s.id); setCancelReason(''); }}
                    style={{
                      padding: '6px 12px',
                      border: '1px solid rgba(239,68,68,.4)',
                      borderRadius: 8,
                      background: 'rgba(239,68,68,.1)',
                      color: '#f87171',
                      cursor: 'pointer',
                      fontSize: 12,
                      fontWeight: 600,
                    }}
                  >
                    ✕ Hủy lô
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Cancel confirm dialog — BICAP-56 */}
      {cancelId != null && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9000,
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: 440, padding: 28, borderRadius: 14 }}>
            <h3 style={{ color: '#fff', margin: '0 0 14px', fontSize: 18 }}>
              Xác nhận hủy lô vận chuyển #{cancelId}
            </h3>
            <p style={{ color: '#94a3b8', fontSize: 13, marginBottom: 16 }}>
              Hành động này sẽ đặt lô về trạng thái HỦY, hoàn trả đơn hàng về DEPOSIT_PAID,
              giải phóng tài xế và phương tiện.
            </p>
            <label style={labelStyle}>Lý do hủy (tuỳ chọn)</label>
            <input
              value={cancelReason}
              onChange={e => setCancelReason(e.target.value)}
              placeholder="VD: Tài xế bị sự cố, cần phân công lại…"
              style={inputStyle}
            />
            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
              <button
                disabled={cancelBusy}
                onClick={doCancel}
                style={{
                  flex: 1, padding: 12, border: 0, borderRadius: 8,
                  background: '#ef4444', color: '#fff', fontWeight: 700, cursor: 'pointer',
                }}
              >
                {cancelBusy ? 'Đang hủy…' : '✕ Xác nhận hủy'}
              </button>
              <button
                onClick={() => setCancelId(null)}
                style={{ ...secondaryButtonStyle, flex: 1 }}
              >
                Quay lại
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
