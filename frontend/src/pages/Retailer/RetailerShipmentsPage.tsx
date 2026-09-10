import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

/**
 * BICAP-49: Retailer xem & chi tiết quy trình vận chuyển.
 * BICAP-48: Nút "Gửi thông báo cho Farm" tích hợp trong trang này.
 *
 * Gọi:
 *   GET  /api/retailer/shipments?status=...  → danh sách
 *   GET  /api/retailer/shipments/{id}        → chi tiết + tracking
 *   POST /api/retailer/notify-farm           → BICAP-48 gửi tin nhắn
 */

type Shipment = {
  id: number;
  status: string;
  orderId: number;
  deliveryAddr?: string;
  routeSummary?: string;
  driverName?: string;
  driverPhone?: string;
  vehicleLicensePlate?: string;
  vehicleType?: string;
  pickupTime?: string;
  deliveryTime?: string;
  createdAt?: string;
};

type Tracking = {
  id: number;
  status: string;
  gpsLat: number;
  gpsLng: number;
  notes?: string;
  timestamp?: string;
};

type Detail = Shipment & { trackingHistory?: Tracking[] };

const STATUSES = ['', 'PICKING_UP', 'IN_TRANSIT', 'DELIVERED', 'RETURNED'];
const STATUS_LABELS: Record<string, string> = {
  '': 'Tất cả',
  PICKING_UP: 'Đang lấy hàng',
  IN_TRANSIT: 'Đang vận chuyển',
  DELIVERED: 'Đã giao',
  RETURNED: 'Trả hàng',
};

const STATUS_COLORS: Record<string, { color: string; bg: string }> = {
  PICKING_UP: { color: '#fcd34d', bg: 'rgba(245,158,11,.15)' },
  IN_TRANSIT: { color: '#7dd3fc', bg: 'rgba(56,189,248,.15)' },
  DELIVERED:  { color: '#6ee7b7', bg: 'rgba(16,185,129,.15)' },
  RETURNED:   { color: '#fca5a5', bg: 'rgba(239,68,68,.15)' },
};

function StatusBadge({ status }: { status: string }) {
  const c = STATUS_COLORS[status] ?? { color: '#94a3b8', bg: 'rgba(148,163,184,.15)' };
  return (
    <span style={{ fontSize: 11, padding: '4px 8px', borderRadius: 999, color: c.color, background: c.bg, whiteSpace: 'nowrap' }}>
      {STATUS_LABELS[status] ?? status}
    </span>
  );
}

const fmt = (iso?: string) =>
  iso ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(iso)) : '—';

export default function RetailerShipmentsPage() {
  const [items, setItems] = useState<Shipment[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [detail, setDetail] = useState<Detail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // BICAP-48: notify-farm modal state
  const [notifyModal, setNotifyModal] = useState<{ orderId: number } | null>(null);
  const [notifyMsg, setNotifyMsg] = useState('');
  const [notifySending, setNotifySending] = useState(false);
  const [notifyError, setNotifyError] = useState('');

  // ── Load danh sách ────────────────────────────────────────────────────────
  const load = async (status = statusFilter) => {
    setLoading(true);
    setError('');
    try {
      const q = status ? `?status=${encodeURIComponent(status)}` : '';
      const res = await fetch(`${API_BASE_URL}/retailer/shipments${q}`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error(`Lỗi ${res.status}`);
      setItems(await res.json());
    } catch (e: any) {
      setError(e.message || 'Không thể tải danh sách vận chuyển.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(statusFilter); }, [statusFilter]);

  // ── Load chi tiết ─────────────────────────────────────────────────────────
  const openDetail = async (id: number) => {
    setError('');
    setDetail(null);
    try {
      const res = await fetch(`${API_BASE_URL}/retailer/shipments/${id}`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error(`Lỗi ${res.status}`);
      setDetail(await res.json());
    } catch (e: any) {
      setError(e.message || 'Không thể tải chi tiết lô vận chuyển.');
    }
  };

  // ── BICAP-48: Gửi thông báo cho Farm Manager ──────────────────────────────
  const sendNotifyFarm = async () => {
    if (!notifyModal) return;
    if (!notifyMsg.trim()) { setNotifyError('Vui lòng nhập nội dung tin nhắn.'); return; }
    setNotifySending(true);
    setNotifyError('');
    try {
      const res = await fetch(`${API_BASE_URL}/retailer/notify-farm`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ orderId: notifyModal.orderId, message: notifyMsg.trim() }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.message || 'Gửi thông báo thất bại.');
      setSuccess('Đã gửi thông báo đến Farm Manager.');
      setNotifyModal(null);
      setNotifyMsg('');
    } catch (e: any) {
      setNotifyError(e.message || 'Gửi thông báo thất bại.');
    } finally {
      setNotifySending(false);
    }
  };

  return (
    <section style={{ color: '#e2e8f0' }}>
      {/* Header */}
      <div style={{ marginBottom: 20 }}>
        <h1 style={{ margin: '0 0 6px', fontSize: 24, fontWeight: 700 }}>Quy trình vận chuyển</h1>
        <p style={{ margin: 0, color: '#94a3b8', fontSize: 13 }}>
          BICAP-49 · Theo dõi các lô hàng vận chuyển đến bạn theo thời gian thực.
        </p>
      </div>

      {/* Status filter tabs */}
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 16, overflowX: 'auto', paddingBottom: 4 }}>
        {STATUSES.map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            style={{
              ...style.filterBtn,
              ...(statusFilter === s ? style.filterBtnActive : {}),
            }}
          >
            {STATUS_LABELS[s]}
          </button>
        ))}
      </div>

      {/* Messages */}
      {error && <div style={style.errorBox} role="alert">{error}</div>}
      {success && <div style={style.successBox} role="status">{success}</div>}

      {/* Main grid */}
      <div style={{ display: 'grid', gridTemplateColumns: detail ? '1fr 1fr' : '1fr', gap: 20, alignItems: 'start' }}>

        {/* Shipment list */}
        <div className="glass-panel" style={{ padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
            <h2 style={style.sectionTitle}>Danh sách lô hàng</h2>
            <button onClick={() => void load()} style={style.iconBtn} title="Làm mới">🔄</button>
          </div>

          {loading && <p style={{ color: '#94a3b8' }}>Đang tải...</p>}
          {!loading && items.length === 0 && (
            <div style={style.emptyBox}>
              <div style={{ fontSize: 32, marginBottom: 8 }}>🚚</div>
              <p>Chưa có lô vận chuyển nào.</p>
            </div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {items.map((s) => (
              <article
                key={s.id}
                style={{
                  ...style.card,
                  borderColor: detail?.id === s.id ? 'rgba(6,182,212,0.6)' : '#334155',
                  cursor: 'pointer',
                }}
                onClick={() => void openDetail(s.id)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <strong style={{ fontSize: 14 }}>Lô #{s.id} · Đơn #{s.orderId}</strong>
                  <StatusBadge status={s.status} />
                </div>
                <p style={style.meta}>
                  {s.driverName ?? 'Chưa gán tài xế'}
                  {s.vehicleLicensePlate ? ` · ${s.vehicleLicensePlate}` : ''}
                </p>
                {s.deliveryAddr && (
                  <p style={{ ...style.meta, color: '#64748b' }}>Giao tới: {s.deliveryAddr}</p>
                )}
                <div style={{ marginTop: 8, display: 'flex', gap: 8 }}>
                  <button
                    onClick={(e) => { e.stopPropagation(); void openDetail(s.id); }}
                    style={style.secondaryBtn}
                  >
                    🔍 Chi tiết
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setNotifyModal({ orderId: s.orderId });
                      setNotifyMsg('');
                      setNotifyError('');
                      setSuccess('');
                    }}
                    style={style.infoBtn}
                    title="Gửi thông báo cho Farm Manager (BICAP-48)"
                  >
                    💬 Nhắn Farm
                  </button>
                </div>
              </article>
            ))}
          </div>
        </div>

        {/* Detail panel */}
        {detail && (
          <div className="glass-panel" style={{ padding: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <h2 style={style.sectionTitle}>Chi tiết lô #{detail.id}</h2>
              <button onClick={() => setDetail(null)} style={style.closeBtn} aria-label="Đóng">×</button>
            </div>

            <StatusBadge status={detail.status} />

            <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 6, fontSize: 13 }}>
              <Row label="Đơn hàng" value={`#${detail.orderId}`} />
              <Row label="Tài xế" value={detail.driverName ? `${detail.driverName}${detail.driverPhone ? ` (${detail.driverPhone})` : ''}` : '—'} />
              <Row label="Phương tiện" value={detail.vehicleType ? `${detail.vehicleType}${detail.vehicleLicensePlate ? ` — ${detail.vehicleLicensePlate}` : ''}` : '—'} />
              {detail.routeSummary && <Row label="Tuyến đường" value={detail.routeSummary} />}
              <Row label="Giao tới" value={detail.deliveryAddr ?? '—'} />
              <Row label="Lấy hàng lúc" value={fmt(detail.pickupTime)} />
              <Row label="Giao xong lúc" value={fmt(detail.deliveryTime)} />
              <Row label="Tạo lúc" value={fmt(detail.createdAt)} />
            </div>

            {/* Tracking history */}
            <h3 style={{ color: '#fff', fontSize: 15, margin: '18px 0 8px' }}>
              Lịch sử định vị ({detail.trackingHistory?.length ?? 0})
            </h3>
            {!detail.trackingHistory?.length ? (
              <p style={{ color: '#94a3b8', fontSize: 13 }}>Chưa có dữ liệu định vị.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {detail.trackingHistory.map((t) => (
                  <div key={t.id} style={{ padding: '8px 0', borderBottom: '1px solid #1f2937', fontSize: 12 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 4 }}>
                      <StatusBadge status={t.status} />
                      <span style={{ color: '#64748b' }}>{fmt(t.timestamp)}</span>
                    </div>
                    <p style={{ margin: '4px 0 0', color: '#94a3b8', fontFamily: 'monospace', fontSize: 11 }}>
                      GPS: {t.gpsLat?.toFixed(5)}, {t.gpsLng?.toFixed(5)}
                    </p>
                    {t.notes && <p style={{ margin: '3px 0 0', color: '#cbd5e1' }}>{t.notes}</p>}
                  </div>
                ))}
              </div>
            )}

            <button
              onClick={() => {
                setNotifyModal({ orderId: detail.orderId });
                setNotifyMsg('');
                setNotifyError('');
                setSuccess('');
              }}
              style={{ ...style.infoBtn, marginTop: 16, width: '100%' }}
            >
              💬 Gửi thông báo cho Farm Manager (BICAP-48)
            </button>
          </div>
        )}
      </div>

      {/* BICAP-48: Notify-Farm Modal */}
      {notifyModal && (
        <div style={style.overlay} role="dialog" aria-modal="true">
          <div style={style.modal}>
            <button onClick={() => setNotifyModal(null)} style={style.closeBtn} aria-label="Đóng">×</button>
            <h2 style={{ marginTop: 0, marginBottom: 6 }}>💬 Nhắn tin cho Farm Manager</h2>
            <p style={{ color: '#94a3b8', fontSize: 13, margin: '0 0 16px' }}>
              Đơn hàng <strong>#{notifyModal.orderId}</strong> — Farm Manager sẽ nhận được thông báo trong hệ thống.
            </p>
            {notifyError && <div style={style.errorBox} role="alert">{notifyError}</div>}
            <label style={style.label}>Nội dung tin nhắn *</label>
            <textarea
              rows={4}
              maxLength={1000}
              value={notifyMsg}
              onChange={(e) => setNotifyMsg(e.target.value)}
              placeholder="Ví dụ: Tôi muốn hỏi về tình trạng đơn hàng, dự kiến giao lúc nào?"
              style={style.textarea}
            />
            <p style={{ textAlign: 'right', color: '#64748b', fontSize: 11, margin: '4px 0 16px' }}>
              {notifyMsg.length}/1000
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button onClick={() => setNotifyModal(null)} style={style.secondaryBtn}>Huỷ</button>
              <button
                onClick={() => void sendNotifyFarm()}
                disabled={notifySending || !notifyMsg.trim()}
                style={{ ...style.primaryBtn, opacity: notifySending || !notifyMsg.trim() ? 0.55 : 1 }}
              >
                {notifySending ? 'Đang gửi...' : '📤 Gửi thông báo'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, padding: '5px 0', borderBottom: '1px solid rgba(148,163,184,.08)' }}>
      <span style={{ color: '#94a3b8', flexShrink: 0 }}>{label}</span>
      <span style={{ color: '#e2e8f0', textAlign: 'right' }}>{value}</span>
    </div>
  );
}

const style: Record<string, React.CSSProperties> = {
  sectionTitle: { color: '#fff', fontSize: 17, margin: 0 },
  filterBtn: { padding: '8px 13px', borderRadius: 999, border: '1px solid #334155', background: '#0f172a', color: '#94a3b8', cursor: 'pointer', whiteSpace: 'nowrap', fontSize: 13 },
  filterBtnActive: { borderColor: '#0891b2', background: 'rgba(6,182,212,0.16)', color: '#ecfeff' },
  card: { padding: '12px 14px', borderRadius: 10, border: '1px solid #334155', background: '#0f172a', transition: 'border-color 0.15s' },
  meta: { fontSize: 12, color: '#94a3b8', margin: '3px 0 0' },
  secondaryBtn: { padding: '7px 13px', borderRadius: 8, border: '1px solid #475569', background: '#1e293b', color: '#e2e8f0', cursor: 'pointer', fontSize: 12, fontWeight: 600 },
  infoBtn: { padding: '7px 13px', borderRadius: 8, border: '1px solid rgba(6,182,212,0.4)', background: 'rgba(6,182,212,0.1)', color: '#38bdf8', cursor: 'pointer', fontSize: 12, fontWeight: 600 },
  primaryBtn: { padding: '10px 16px', borderRadius: 8, border: 0, background: '#0891b2', color: '#fff', cursor: 'pointer', fontWeight: 700 },
  iconBtn: { padding: '6px 10px', borderRadius: 8, border: '1px solid #334155', background: 'transparent', color: '#94a3b8', cursor: 'pointer' },
  closeBtn: { background: 'transparent', border: 0, color: '#cbd5e1', fontSize: 22, cursor: 'pointer', padding: 0 },
  errorBox: { padding: '11px 14px', borderRadius: 10, color: '#fecaca', background: 'rgba(239,68,68,.13)', marginBottom: 10 },
  successBox: { padding: '11px 14px', borderRadius: 10, color: '#a7f3d0', background: 'rgba(16,185,129,.13)', marginBottom: 10 },
  emptyBox: { textAlign: 'center', padding: '32px 16px', color: '#94a3b8' },
  overlay: { position: 'fixed', inset: 0, display: 'grid', placeItems: 'center', padding: 16, background: 'rgba(0,0,0,.78)', zIndex: 99 },
  modal: { position: 'relative', width: 'min(480px,94vw)', padding: 28, borderRadius: 18, background: '#0f172a', border: '1px solid #334155', boxShadow: '0 24px 80px rgba(0,0,0,.5)' },
  label: { display: 'block', margin: '0 0 7px', fontWeight: 650, fontSize: 13 },
  textarea: { width: '100%', boxSizing: 'border-box', resize: 'vertical', borderRadius: 10, border: '1px solid #475569', background: '#111827', color: '#f8fafc', padding: 12, font: 'inherit', fontSize: 13 },
};
