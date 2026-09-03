import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import { panelStyle, titleStyle, inputStyle, alertStyle, cardStyle, badgeStyle, hashStyle } from './ui';

type Shipment = {
  id: number; status: string; orderId: number; deliveryAddr?: string; routeSummary?: string;
  driverName?: string; driverPhone?: string; vehicleLicensePlate?: string; vehicleType?: string;
  pickupTime?: string; deliveryTime?: string; createdAt?: string;
};
type Tracking = { id: number; status: string; gpsLat: number; gpsLng: number; notes?: string; timestamp?: string };
type Detail = Shipment & { trackingHistory?: Tracking[] };
type Summary = {
  total: number; pickingUp: number; inTransit: number; delivered: number; returned: number;
  onTimeDelivered: number; lateDelivered: number; onTimeRatePercent: number;
};

const STATUSES = ['', 'PICKING_UP', 'IN_TRANSIT', 'DELIVERED', 'RETURNED'];

export default function FarmShipments({ farmId }: { farmId?: number }) {
  const [items, setItems] = useState<Shipment[]>([]);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [status, setStatus] = useState('');
  const [detail, setDetail] = useState<Detail | null>(null);
  const [error, setError] = useState('');

  const load = async () => {
    if (!farmId) return;
    const headers = getAuthHeaders();
    const [listRes, sumRes] = await Promise.all([
      fetch(`${API_BASE_URL}/farms/${farmId}/shipments${status ? `?status=${status}` : ''}`, { headers }),
      fetch(`${API_BASE_URL}/farms/${farmId}/shipments/summary`, { headers }),
    ]);
    if (!listRes.ok) { setError('Không tải được danh sách lô vận chuyển.'); return; }
    setItems(await listRes.json());
    if (sumRes.ok) setSummary(await sumRes.json());
  };

  const openDetail = async (id: number) => {
    if (!farmId) return;
    setError('');
    const res = await fetch(`${API_BASE_URL}/farms/${farmId}/shipments/${id}`, { headers: getAuthHeaders() });
    if (!res.ok) { setError('Không tải được chi tiết lô vận chuyển.'); return; }
    setDetail(await res.json());
  };

  useEffect(() => { load().catch(() => setError('Lỗi kết nối máy chủ.')); }, [farmId, status]);

  if (!farmId) {
    return <div><h1 className="dashboard-title">Vận chuyển</h1><div style={alertStyle}>Chưa xác định được nông trại của tài khoản.</div></div>;
  }

  return (
    <div>
      <h1 className="dashboard-title">Quy trình vận chuyển</h1>
      <p className="dashboard-subtitle">BICAP-22/23 · Theo dõi và báo cáo tổng hợp các lô hàng xuất từ nông trại của bạn.</p>
      {error && <div style={alertStyle}>{error}</div>}

      {summary && (
        <section className="glass-panel" style={{ ...panelStyle, marginBottom: 20 }}>
          <h2 style={titleStyle}>Báo cáo tổng hợp</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: 12 }}>
            <Stat label="Tổng lô" value={summary.total} />
            <Stat label="Đang lấy hàng" value={summary.pickingUp} />
            <Stat label="Đang vận chuyển" value={summary.inTransit} />
            <Stat label="Đã giao" value={summary.delivered} />
            <Stat label="Đúng hạn" value={summary.onTimeDelivered} />
            <Stat label="Tỷ lệ đúng hạn" value={`${summary.onTimeRatePercent}%`} />
          </div>
        </section>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: detail ? '1fr 1fr' : '1fr', gap: 24, alignItems: 'start' }}>
        <section className="glass-panel" style={panelStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <h2 style={titleStyle}>Danh sách lô hàng</h2>
            <select value={status} onChange={e => setStatus(e.target.value)} style={{ ...inputStyle, width: 'auto' }}>
              {STATUSES.map(s => <option key={s} value={s}>{s || 'Tất cả'}</option>)}
            </select>
          </div>
          {!items.length && <p style={{ color: '#94a3b8' }}>Chưa có lô vận chuyển nào cho hàng của nông trại.</p>}
          {items.map(s => (
            <article key={s.id} style={{ ...cardStyle, cursor: 'pointer', borderColor: detail?.id === s.id ? '#10b981' : '#334155' }} onClick={() => openDetail(s.id)}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                <strong>Lô #{s.id} · Đơn #{s.orderId}</strong><span style={badgeStyle(s.status)}>{s.status}</span>
              </div>
              <p style={{ fontSize: 13, margin: '6px 0' }}>{s.driverName || 'Chưa gán tài xế'}{s.vehicleLicensePlate ? ` · ${s.vehicleLicensePlate}` : ''}</p>
              {s.deliveryAddr && <p style={{ fontSize: 12, color: '#94a3b8' }}>Giao tới: {s.deliveryAddr}</p>}
            </article>
          ))}
        </section>

        {detail && (
          <section className="glass-panel" style={panelStyle}>
            <h2 style={titleStyle}>Chi tiết lô #{detail.id}</h2>
            <p style={{ fontSize: 13 }}><span style={badgeStyle(detail.status)}>{detail.status}</span></p>
            <p style={{ fontSize: 13, marginTop: 8 }}>Tài xế: {detail.driverName || '—'} {detail.driverPhone ? `(${detail.driverPhone})` : ''}</p>
            <p style={{ fontSize: 13 }}>Phương tiện: {detail.vehicleType || '—'} {detail.vehicleLicensePlate ? `- ${detail.vehicleLicensePlate}` : ''}</p>
            {detail.routeSummary && <p style={{ fontSize: 13 }}>Tuyến: {detail.routeSummary}</p>}
            <p style={{ fontSize: 12, color: '#94a3b8' }}>Lấy hàng: {detail.pickupTime || '—'} · Giao: {detail.deliveryTime || '—'}</p>
            <h3 style={{ color: '#fff', fontSize: 15, margin: '16px 0 4px' }}>Lịch sử định vị ({detail.trackingHistory?.length || 0})</h3>
            {!detail.trackingHistory?.length && <p style={{ color: '#94a3b8', fontSize: 13 }}>Chưa có dữ liệu định vị.</p>}
            {detail.trackingHistory?.map(t => (
              <div key={t.id} style={{ padding: '8px 0', borderBottom: '1px solid #1f2937', fontSize: 12 }}>
                <span style={badgeStyle(t.status)}>{t.status}</span>{' '}
                <span style={hashStyle}>{t.gpsLat?.toFixed?.(5)}, {t.gpsLng?.toFixed?.(5)}</span>
                <span style={{ color: '#94a3b8' }}> · {t.timestamp}</span>
                {t.notes && <div style={{ color: '#cbd5e1' }}>{t.notes}</div>}
              </div>
            ))}
          </section>
        )}
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number | string }) {
  return (
    <div style={{ background: 'rgba(255,255,255,.03)', border: '1px solid #334155', borderRadius: 10, padding: 14 }}>
      <div style={{ fontSize: 22, fontWeight: 700, color: '#fff' }}>{value}</div>
      <div style={{ fontSize: 12, color: '#94a3b8' }}>{label}</div>
    </div>
  );
}
