/**
 * BICAP-57 — Shipping: Xem quy trình vận chuyển (tracking timeline).
 *
 * Shipping Manager xem lịch sử GPS tracking, trạng thái từng bước, thời gian
 * và ghi chú của từng checkpoint.
 */
import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  panelStyle, titleStyle, badgeStyle, alertStyle, inputStyle, buttonStyle,
  secondaryButtonStyle, cardStyle,
} from '../FarmManager/ui';

type TrackingPoint = {
  id: number;
  shipmentId: number;
  status: string;
  gpsLat: number;
  gpsLng: number;
  images?: string[];
  notes?: string;
  timestamp?: string;
};

type ShipmentDetail = {
  id: number;
  status: string;
  orderId?: number;
  deliveryAddr?: string;
  driverName?: string;
  driverPhone?: string;
  vehicleLicensePlate?: string;
  vehicleType?: string;
  routeSummary?: string;
  pickupTime?: string;
  deliveryTime?: string;
  createdAt?: string;
  trackingHistory?: TrackingPoint[];
};

const STEP_ICONS: Record<string, string> = {
  PICKUP_CONFIRMED: '📦',
  DELIVERY_CONFIRMED: '✅',
  PICKING_UP: '🔍',
  IN_TRANSIT: '🚛',
  DELIVERED: '🎯',
  RETURNED: '↩️',
};

const STEP_LABELS: Record<string, string> = {
  PICKUP_CONFIRMED: 'Đã lấy hàng',
  DELIVERY_CONFIRMED: 'Giao hàng thành công',
  PICKING_UP: 'Đang đến lấy hàng',
  IN_TRANSIT: 'Đang vận chuyển',
  DELIVERED: 'Đã giao',
  RETURNED: 'Đã hoàn trả',
};

export default function TrackingPage() {
  const [shipmentId, setShipmentId] = useState('');
  const [detail, setDetail] = useState<ShipmentDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [allShipments, setAllShipments] = useState<{ id: number; status: string; orderId?: number; driverName?: string }[]>([]);

  useEffect(() => {
    fetch(`${API_BASE_URL}/shipping/shipments`, { headers: getAuthHeaders() })
      .then(r => r.ok ? r.json() : [])
      .then(data => setAllShipments(data))
      .catch(() => {});
  }, []);

  const loadDetail = async (id: string | number) => {
    const sid = Number(id);
    if (!sid) { setError('Vui lòng nhập hoặc chọn mã lô vận chuyển.'); return; }
    setLoading(true);
    setError('');
    setDetail(null);
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/shipments/${sid}`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setDetail(await res.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được thông tin lô vận chuyển.');
    } finally {
      setLoading(false);
    }
  };

  const statusColor = (status: string) => {
    if (status === 'DELIVERED' || status === 'DELIVERY_CONFIRMED') return '#6ee7b7';
    if (status === 'IN_TRANSIT' || status === 'PICKUP_CONFIRMED') return '#7dd3fc';
    if (status.startsWith('REPORT_')) return '#fca5a5';
    return '#fcd34d';
  };

  return (
    <div>
      <h1 className="dashboard-title">Quy trình vận chuyển</h1>
      <p className="dashboard-subtitle">Xem toàn bộ lịch sử tracking GPS và trạng thái của lô vận chuyển.</p>

      {error && <div style={alertStyle}>{error}</div>}

      {/* Selector */}
      <div className="glass-panel" style={{ ...panelStyle, marginBottom: 24 }}>
        <h2 style={titleStyle}>Chọn lô vận chuyển</h2>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <select
            value={shipmentId}
            onChange={e => { setShipmentId(e.target.value); if (e.target.value) loadDetail(e.target.value); }}
            style={{ ...inputStyle, flex: 2, minWidth: 200 }}
          >
            <option value="">-- Chọn từ danh sách --</option>
            {allShipments.map(s => (
              <option key={s.id} value={s.id}>
                #{s.id} — {s.status}{s.orderId ? `, Đơn #${s.orderId}` : ''}{s.driverName ? `, ${s.driverName}` : ''}
              </option>
            ))}
          </select>
          <input
            type="number"
            placeholder="Hoặc nhập mã lô…"
            value={shipmentId}
            onChange={e => setShipmentId(e.target.value)}
            style={{ ...inputStyle, flex: 1, minWidth: 140 }}
            min="1"
          />
          <button
            onClick={() => loadDetail(shipmentId)}
            style={{ ...buttonStyle, width: 'auto', padding: '10px 20px', marginTop: 0 }}
            disabled={loading}
          >
            {loading ? 'Đang tải…' : '🔍 Xem'}
          </button>
        </div>
      </div>

      {/* Shipment detail */}
      {detail && (
        <>
          {/* Header */}
          <div className="glass-panel" style={{ ...panelStyle, marginBottom: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
                  <h2 style={{ ...titleStyle, margin: 0 }}>Lô #{detail.id}</h2>
                  <span style={badgeStyle(detail.status)}>{detail.status}</span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '6px 20px', fontSize: 13, color: '#94a3b8' }}>
                  {detail.orderId && <span>🧾 Đơn hàng #{detail.orderId}</span>}
                  {detail.deliveryAddr && <span>📍 {detail.deliveryAddr}</span>}
                  {detail.driverName && <span>🧑‍💼 {detail.driverName}{detail.driverPhone ? `, ${detail.driverPhone}` : ''}</span>}
                  {detail.vehicleLicensePlate && <span>🚛 {detail.vehicleLicensePlate} ({detail.vehicleType})</span>}
                  {detail.routeSummary && <span style={{ gridColumn: '1 / -1' }}>🗺️ {detail.routeSummary}</span>}
                  {detail.pickupTime && <span>📦 Lấy hàng: {new Date(detail.pickupTime).toLocaleString('vi-VN')}</span>}
                  {detail.deliveryTime && <span>✅ Giao hàng: {new Date(detail.deliveryTime).toLocaleString('vi-VN')}</span>}
                  {detail.createdAt && <span>🕐 Tạo: {new Date(detail.createdAt).toLocaleString('vi-VN')}</span>}
                </div>
              </div>
              <button onClick={() => loadDetail(detail.id)} style={{ ...secondaryButtonStyle }}>
                🔄 Làm mới
              </button>
            </div>
          </div>

          {/* Status flow */}
          <div className="glass-panel" style={{ ...panelStyle, marginBottom: 20 }}>
            <h2 style={titleStyle}>Trạng thái hiện tại</h2>
            <div style={{ display: 'flex', gap: 0, alignItems: 'center', flexWrap: 'wrap' }}>
              {['PICKING_UP', 'IN_TRANSIT', 'DELIVERED'].map((step, idx) => {
                const active = detail.status === step;
                const past = (
                  (step === 'PICKING_UP') ||
                  (step === 'IN_TRANSIT' && ['IN_TRANSIT', 'DELIVERED'].includes(detail.status)) ||
                  (step === 'DELIVERED' && detail.status === 'DELIVERED')
                );
                return (
                  <div key={step} style={{ display: 'flex', alignItems: 'center' }}>
                    <div style={{
                      padding: '8px 16px',
                      borderRadius: 20,
                      fontWeight: active ? 700 : 400,
                      fontSize: 13,
                      background: active ? 'rgba(16,185,129,.2)' : past ? 'rgba(56,189,248,.1)' : 'rgba(255,255,255,.04)',
                      color: active ? '#6ee7b7' : past ? '#7dd3fc' : '#64748b',
                      border: `1px solid ${active ? 'rgba(16,185,129,.4)' : past ? 'rgba(56,189,248,.2)' : 'rgba(255,255,255,.06)'}`,
                      whiteSpace: 'nowrap',
                    }}>
                      {STEP_ICONS[step] || '•'} {STEP_LABELS[step] || step}
                      {active && ' ◀ Hiện tại'}
                    </div>
                    {idx < 2 && <span style={{ color: '#334155', margin: '0 4px' }}>→</span>}
                  </div>
                );
              })}
              {detail.status === 'RETURNED' && (
                <span style={{ marginLeft: 8, ...badgeStyle('RETURNED') }}>↩️ Đã hủy/hoàn trả</span>
              )}
            </div>
          </div>

          {/* Timeline */}
          <div className="glass-panel" style={panelStyle}>
            <h2 style={titleStyle}>
              Lịch sử tracking ({detail.trackingHistory?.length ?? 0} điểm)
            </h2>
            {(!detail.trackingHistory || detail.trackingHistory.length === 0) && (
              <p style={{ color: '#64748b', fontSize: 13 }}>Chưa có điểm tracking nào.</p>
            )}
            <div style={{ position: 'relative' }}>
              {detail.trackingHistory?.map((pt, idx) => {
                const isReport = pt.status.startsWith('REPORT_');
                return (
                  <div key={pt.id} style={{
                    display: 'flex', gap: 14, marginBottom: 16,
                    paddingLeft: 8, position: 'relative',
                  }}>
                    {/* Timeline line */}
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
                      <div style={{
                        width: 32, height: 32, borderRadius: '50%',
                        background: isReport ? 'rgba(239,68,68,.2)' : 'rgba(16,185,129,.15)',
                        border: `2px solid ${isReport ? '#f87171' : statusColor(pt.status)}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 14, flexShrink: 0,
                      }}>
                        {isReport ? '⚠️' : (STEP_ICONS[pt.status] || '📍')}
                      </div>
                      {idx < (detail.trackingHistory!.length - 1) && (
                        <div style={{ width: 2, flex: 1, background: '#1e293b', minHeight: 20, marginTop: 4 }} />
                      )}
                    </div>
                    <div style={{ flex: 1, paddingBottom: 8 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                        <span style={{ color: statusColor(pt.status), fontWeight: 600, fontSize: 13 }}>
                          {STEP_LABELS[pt.status] || pt.status}
                        </span>
                        {pt.timestamp && (
                          <span style={{ color: '#64748b', fontSize: 12 }}>
                            {new Date(pt.timestamp).toLocaleString('vi-VN')}
                          </span>
                        )}
                      </div>
                      {pt.notes && (
                        <p style={{ margin: '4px 0 0', color: '#94a3b8', fontSize: 13 }}>{pt.notes}</p>
                      )}
                      <p style={{ margin: '3px 0 0', color: '#475569', fontSize: 11 }}>
                        GPS: {pt.gpsLat.toFixed(6)}, {pt.gpsLng.toFixed(6)}
                      </p>
                      {pt.images && pt.images.length > 0 && (
                        <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
                          {pt.images.map((img, i) => (
                            <a key={i} href={img} target="_blank" rel="noopener noreferrer">
                              <img
                                src={img}
                                alt={`tracking-${i}`}
                                style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 6, border: '1px solid #334155' }}
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
        </>
      )}
    </div>
  );
}
