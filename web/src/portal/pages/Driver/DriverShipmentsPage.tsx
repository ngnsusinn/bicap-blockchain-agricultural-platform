import { useEffect, useRef, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

/**
 * F11 — Ứng dụng tài xế (SHIP_DRIVER), thiết kế mobile-first.
 *
 * Bao gồm:
 *  - Danh sách lô hàng được phân công (GET /api/driver/shipments) + lọc theo trạng thái
 *  - Chi tiết lô hàng kèm lịch sử tracking (GET /api/driver/shipments/{id})
 *  - Quét QR lấy hàng: nhập tay / dán URL /trace/{hash} / camera / tải ảnh, rồi
 *    POST /api/driver/shipments/{id}/pickup với {gpsLat, gpsLng, traceHash, notes}
 *  - Cập nhật tracking, xác nhận giao hàng và gửi báo cáo cho Shipping Manager
 *
 * Không có thư viện decode QR nào được cài: dùng BarcodeDetector của trình duyệt khi
 * có, kèm phương án nhập tay / dán liên kết để luôn hoạt động.
 */

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

type Shipment = {
  id: number;
  status: string;
  createdAt?: string;
  pickupTime?: string;
  deliveryTime?: string;
  routeSummary?: string;
  orderId?: number;
  deliveryAddr?: string;
  driverId?: number;
  driverName?: string;
  driverPhone?: string;
  vehicleId?: number;
  vehicleLicensePlate?: string;
  vehicleType?: string;
  trackingHistory?: TrackingPoint[];
};

interface DriverShipmentsPageProps {
  /** Đăng xuất khỏi cổng tài xế (tuỳ chọn — PortalApp truyền vào). */
  onLogout?: () => void;
}

const FALLBACK_GPS = { lat: 10.7769, lng: 106.7009 };

const STATUS_FILTERS: { value: string; label: string }[] = [
  { value: '', label: 'Tất cả' },
  { value: 'PICKING_UP', label: 'Chờ lấy hàng' },
  { value: 'IN_TRANSIT', label: 'Đang vận chuyển' },
  { value: 'DELIVERED', label: 'Đã giao' },
  { value: 'RETURNED', label: 'Hoàn trả' },
];

const STEP_LABELS: Record<string, string> = {
  PICKUP_CONFIRMED: 'Đã lấy hàng',
  DELIVERY_CONFIRMED: 'Giao hàng thành công',
  PICKING_UP: 'Đang đến lấy hàng',
  IN_TRANSIT: 'Đang vận chuyển',
  ARRIVED: 'Đã tới điểm giao',
  DELIVERED: 'Đã giao',
  RETURNED: 'Đã hoàn trả',
};

const REPORT_TYPES = ['INCIDENT', 'DELAY', 'DAMAGE', 'OTHER'];

/** Tách trace hash từ giá trị quét/dán: chấp nhận hash thuần hoặc URL /trace/{hash}. */
export function extractTraceHash(raw: string): string {
  const trimmed = (raw ?? '').trim();
  if (!trimmed) return '';
  const parts = trimmed.split(/[\s/?#]+/).filter(Boolean);
  return (parts.length ? parts[parts.length - 1] : trimmed).trim();
}

const statusStyle = (status: string): React.CSSProperties => {
  const color =
    status === 'DELIVERED' || status === 'DELIVERY_CONFIRMED' ? '#6ee7b7'
      : status === 'IN_TRANSIT' || status === 'PICKUP_CONFIRMED' ? '#7dd3fc'
        : status === 'RETURNED' ? '#fca5a5'
          : '#fcd34d';
  const bg =
    status === 'DELIVERED' || status === 'DELIVERY_CONFIRMED' ? 'rgba(16,185,129,.15)'
      : status === 'IN_TRANSIT' || status === 'PICKUP_CONFIRMED' ? 'rgba(56,189,248,.15)'
        : status === 'RETURNED' ? 'rgba(239,68,68,.15)'
          : 'rgba(245,158,11,.15)';
  return {
    fontSize: 11, fontWeight: 700, padding: '4px 10px', borderRadius: 999,
    color, background: bg, whiteSpace: 'nowrap',
  };
};

export default function DriverShipmentsPage({ onLogout }: DriverShipmentsPageProps) {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [listError, setListError] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<Shipment | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [detailError, setDetailError] = useState('');

  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // QR / pickup
  const [traceHash, setTraceHash] = useState('');
  const [pickupNotes, setPickupNotes] = useState('');
  const [scanning, setScanning] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const scanTimer = useRef<number | null>(null);

  // Tracking / delivery / report
  const [trackingStatus, setTrackingStatus] = useState('IN_TRANSIT');
  const [trackingNotes, setTrackingNotes] = useState('');
  const [deliveryNotes, setDeliveryNotes] = useState('');
  const [reportType, setReportType] = useState('INCIDENT');
  const [reportDescription, setReportDescription] = useState('');

  // GPS
  const [gps, setGps] = useState(FALLBACK_GPS);
  const [gpsSource, setGpsSource] = useState<'device' | 'fallback' | null>(null);

  const loadList = async (filter: string) => {
    setLoadingList(true);
    setListError('');
    try {
      const query = filter ? `?status=${encodeURIComponent(filter)}` : '';
      const res = await fetch(`${API_BASE_URL}/driver/shipments${query}`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Không tải được danh sách lô hàng (mã lỗi ${res.status}).`);
      }
      setShipments(await res.json());
    } catch (err) {
      setShipments([]);
      setListError(err instanceof Error ? err.message : 'Không tải được danh sách lô hàng.');
    } finally {
      setLoadingList(false);
    }
  };

  useEffect(() => { void loadList(statusFilter); }, [statusFilter]);

  const loadDetail = async (id: number) => {
    setLoadingDetail(true);
    setDetailError('');
    try {
      const res = await fetch(`${API_BASE_URL}/driver/shipments/${id}`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Không tải được chi tiết lô hàng (mã lỗi ${res.status}).`);
      }
      setDetail(await res.json());
    } catch (err) {
      setDetail(null);
      setDetailError(err instanceof Error ? err.message : 'Không tải được chi tiết lô hàng.');
    } finally {
      setLoadingDetail(false);
    }
  };

  const openShipment = (id: number) => {
    setSelectedId(id);
    setDetail(null);
    setActionError('');
    setActionSuccess('');
    setTraceHash('');
    setPickupNotes('');
    setTrackingNotes('');
    setDeliveryNotes('');
    setReportDescription('');
    void loadDetail(id);
  };

  const backToList = () => {
    stopScan();
    setSelectedId(null);
    setDetail(null);
    setDetailError('');
    setActionError('');
    setActionSuccess('');
  };

  /* ── GPS ── */
  const getPosition = (): Promise<{ lat: number; lng: number }> => {
    return new Promise((resolve) => {
      if (typeof navigator === 'undefined' || !navigator.geolocation) {
        setGps(FALLBACK_GPS);
        setGpsSource('fallback');
        resolve(FALLBACK_GPS);
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const coords = { lat: pos.coords.latitude, lng: pos.coords.longitude };
          setGps(coords);
          setGpsSource('device');
          resolve(coords);
        },
        () => {
          setGps(FALLBACK_GPS);
          setGpsSource('fallback');
          resolve(FALLBACK_GPS);
        },
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 30000 },
      );
    });
  };

  const readPosition = async () => {
    const coords = await getPosition();
    setActionSuccess(`Đã lấy toạ độ: ${coords.lat.toFixed(6)}, ${coords.lng.toFixed(6)}`);
  };

  /* ── QR scanner (BarcodeDetector, không cần thư viện ngoài) ── */
  function stopScan() {
    if (scanTimer.current) window.clearInterval(scanTimer.current);
    scanTimer.current = null;
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
    setScanning(false);
  }

  useEffect(() => stopScan, []);

  const barcodeDetector = () => {
    const D = (window as unknown as { BarcodeDetector?: new (opts: { formats: string[] }) => { detect: (src: unknown) => Promise<{ rawValue?: string }[]> } }).BarcodeDetector;
    if (!D) {
      throw new Error('Trình duyệt chưa hỗ trợ đọc QR. Hãy nhập tay hoặc dán liên kết /trace/{hash}.');
    }
    return new D({ formats: ['qr_code'] });
  };

  const applyScannedValue = (raw: string) => {
    const hash = extractTraceHash(raw);
    if (!hash) {
      setActionError('Mã QR không chứa trace hash hợp lệ.');
      return;
    }
    setTraceHash(hash);
    setActionError('');
  };

  const startScan = async () => {
    try {
      setActionError('');
      const detector = barcodeDetector();
      streamRef.current = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      if (videoRef.current) videoRef.current.srcObject = streamRef.current;
      setScanning(true);
      scanTimer.current = window.setInterval(async () => {
        if (!videoRef.current || videoRef.current.readyState < 2) return;
        const codes = await detector.detect(videoRef.current).catch(() => []);
        if (codes[0]?.rawValue) {
          applyScannedValue(codes[0].rawValue);
          stopScan();
        }
      }, 700);
    } catch (err) {
      stopScan();
      setActionError(err instanceof Error ? err.message : 'Không thể mở camera.');
    }
  };

  const uploadQrImage = async (file?: File) => {
    if (!file) return;
    try {
      setActionError('');
      const image = await createImageBitmap(file);
      const codes = await barcodeDetector().detect(image);
      image.close();
      if (!codes[0]?.rawValue) throw new Error('Không phát hiện mã QR trong ảnh.');
      applyScannedValue(codes[0].rawValue);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể đọc ảnh QR.');
    }
  };

  /* ── Actions ── */
  const refreshAfterAction = async (updated: Shipment, successMessage: string) => {
    setDetail(updated);
    setActionSuccess(successMessage);
    await loadList(statusFilter);
  };

  const submitPickup = async () => {
    if (!detail) return;
    setActionError('');
    setActionSuccess('');

    const hash = extractTraceHash(traceHash);
    // F11: bắt buộc phải có trace hash quét từ QR trước khi gọi backend.
    if (!hash) {
      setActionError('Vui lòng quét hoặc nhập mã trace hash trên QR của lô hàng trước khi xác nhận lấy hàng.');
      return;
    }
    if (!/^0x[0-9a-fA-F]{64}$/.test(hash)) {
      setActionError('Mã trace hash không hợp lệ: cần bắt đầu bằng 0x và đủ 64 ký tự hex.');
      return;
    }

    setBusy('pickup');
    try {
      const coords = await getPosition();
      const res = await fetch(`${API_BASE_URL}/driver/shipments/${detail.id}/pickup`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          gpsLat: coords.lat,
          gpsLng: coords.lng,
          traceHash: hash,
          notes: pickupNotes || undefined,
        }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.message || `Xác nhận lấy hàng thất bại (mã lỗi ${res.status}).`);
      setTraceHash('');
      setPickupNotes('');
      await refreshAfterAction(body, 'Đã xác nhận lấy hàng thành công.');
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Xác nhận lấy hàng thất bại.');
    } finally {
      setBusy(null);
    }
  };

  const submitTracking = async () => {
    if (!detail) return;
    setActionError('');
    setActionSuccess('');
    setBusy('tracking');
    try {
      const coords = await getPosition();
      const res = await fetch(`${API_BASE_URL}/driver/shipments/${detail.id}/tracking`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          status: trackingStatus,
          gpsLat: coords.lat,
          gpsLng: coords.lng,
          notes: trackingNotes || undefined,
        }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.message || `Cập nhật tracking thất bại (mã lỗi ${res.status}).`);
      setTrackingNotes('');
      setDetail((prev) => (prev ? { ...prev, trackingHistory: [body, ...(prev.trackingHistory ?? [])] } : prev));
      setActionSuccess('Đã thêm điểm tracking mới.');
      await loadList(statusFilter);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Cập nhật tracking thất bại.');
    } finally {
      setBusy(null);
    }
  };

  const submitDelivery = async () => {
    if (!detail) return;
    setActionError('');
    setActionSuccess('');
    setBusy('delivery');
    try {
      const coords = await getPosition();
      const res = await fetch(`${API_BASE_URL}/driver/shipments/${detail.id}/deliver`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          gpsLat: coords.lat,
          gpsLng: coords.lng,
          notes: deliveryNotes || undefined,
        }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.message || `Xác nhận giao hàng thất bại (mã lỗi ${res.status}).`);
      setDeliveryNotes('');
      await refreshAfterAction(body, 'Đã xác nhận giao hàng thành công.');
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Xác nhận giao hàng thất bại.');
    } finally {
      setBusy(null);
    }
  };

  const submitReport = async () => {
    if (!detail) return;
    setActionError('');
    setActionSuccess('');
    if (!reportDescription.trim()) {
      setActionError('Vui lòng nhập nội dung báo cáo.');
      return;
    }
    setBusy('report');
    try {
      const coords = await getPosition();
      const res = await fetch(`${API_BASE_URL}/driver/reports`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          shipmentId: detail.id,
          reportType,
          description: reportDescription.trim(),
          gpsLat: coords.lat,
          gpsLng: coords.lng,
        }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.message || `Gửi báo cáo thất bại (mã lỗi ${res.status}).`);
      setReportDescription('');
      setActionSuccess('Đã gửi báo cáo tới Shipping Manager.');
      setDetail((prev) => (prev ? { ...prev, trackingHistory: [body, ...(prev.trackingHistory ?? [])] } : prev));
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Gửi báo cáo thất bại.');
    } finally {
      setBusy(null);
    }
  };

  const gpsLabel = gpsSource === 'device'
    ? `GPS thiết bị: ${gps.lat.toFixed(6)}, ${gps.lng.toFixed(6)}`
    : gpsSource === 'fallback'
      ? `GPS dự phòng: ${gps.lat.toFixed(6)}, ${gps.lng.toFixed(6)}`
      : 'Chưa lấy toạ độ — sẽ dùng GPS thiết bị hoặc toạ độ dự phòng.';

  return (
    <div style={shellStyle}>
      <header style={headerStyle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={logoStyle}>D</div>
          <span style={{ fontWeight: 800, fontSize: 16 }}>BICAP Driver</span>
        </div>
        {onLogout && (
          <button onClick={onLogout} style={logoutStyle}>🚪 Đăng xuất</button>
        )}
      </header>

      <main style={mainStyle}>
        {selectedId == null ? (
          <>
            <h1 style={pageTitleStyle}>Lô hàng được phân công</h1>
            <div style={filterRowStyle}>
              {STATUS_FILTERS.map((f) => (
                <button
                  key={f.value || 'ALL'}
                  type="button"
                  onClick={() => setStatusFilter(f.value)}
                  style={{
                    ...filterChipStyle,
                    color: statusFilter === f.value ? '#34d399' : '#94a3b8',
                    borderColor: statusFilter === f.value ? '#10b981' : 'rgba(255,255,255,0.12)',
                    background: statusFilter === f.value ? 'rgba(16,185,129,0.12)' : 'transparent',
                  }}
                >
                  {f.label}
                </button>
              ))}
            </div>

            {loadingList && <div style={emptyStyle}>⏳ Đang tải lô hàng…</div>}
            {!loadingList && listError && (
              <div style={{ ...emptyStyle, color: '#fca5a5' }}>
                ⚠️ {listError}
                <div style={{ marginTop: 12 }}>
                  <button onClick={() => void loadList(statusFilter)} style={secondaryBtnStyle}>Tải lại</button>
                </div>
              </div>
            )}
            {!loadingList && !listError && shipments.length === 0 && (
              <div style={emptyStyle}>📭 Không có lô hàng nào ở trạng thái này.</div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {shipments.map((s) => (
                <button key={s.id} type="button" onClick={() => openShipment(s.id)} style={cardButtonStyle}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}>
                    <strong style={{ color: '#fff', fontSize: 15 }}>Lô #{s.id}</strong>
                    <span style={statusStyle(s.status)}>{s.status}</span>
                  </div>
                  <div style={cardMetaStyle}>
                    {s.orderId != null && <span>🧾 Đơn #{s.orderId}</span>}
                    {s.deliveryAddr && <span>📍 {s.deliveryAddr}</span>}
                    {s.vehicleLicensePlate && <span>🚛 {s.vehicleLicensePlate}</span>}
                    {s.routeSummary && <span>🗺️ {s.routeSummary}</span>}
                    {s.pickupTime && <span>📦 Lấy hàng: {new Date(s.pickupTime).toLocaleString('vi-VN')}</span>}
                    {s.deliveryTime && <span>✅ Giao: {new Date(s.deliveryTime).toLocaleString('vi-VN')}</span>}
                  </div>
                  <span style={{ color: '#34d399', fontSize: 12, fontWeight: 700 }}>Chi tiết & thao tác ➔</span>
                </button>
              ))}
            </div>
          </>
        ) : (
          <>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 12 }}>
              <button type="button" onClick={backToList} style={secondaryBtnStyle}>← Danh sách</button>
              <button
                type="button"
                onClick={() => void loadDetail(selectedId)}
                style={secondaryBtnStyle}
                disabled={loadingDetail}
              >
                🔄 Làm mới
              </button>
            </div>

            {loadingDetail && <div style={emptyStyle}>⏳ Đang tải chi tiết…</div>}
            {!loadingDetail && detailError && <div style={{ ...emptyStyle, color: '#fca5a5' }}>⚠️ {detailError}</div>}

            {detail && (
              <>
                <div style={panelStyle}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}>
                    <h2 style={{ margin: 0, color: '#fff', fontSize: 18 }}>Lô #{detail.id}</h2>
                    <span style={statusStyle(detail.status)}>{detail.status}</span>
                  </div>
                  <div style={detailGridStyle}>
                    {detail.orderId != null && <span>🧾 Đơn hàng #{detail.orderId}</span>}
                    {detail.deliveryAddr && <span>📍 {detail.deliveryAddr}</span>}
                    {detail.driverName && <span>🧑‍💼 {detail.driverName}{detail.driverPhone ? `, ${detail.driverPhone}` : ''}</span>}
                    {detail.vehicleLicensePlate && <span>🚛 {detail.vehicleLicensePlate} ({detail.vehicleType})</span>}
                    {detail.routeSummary && <span>🗺️ {detail.routeSummary}</span>}
                    {detail.pickupTime && <span>📦 Lấy hàng: {new Date(detail.pickupTime).toLocaleString('vi-VN')}</span>}
                    {detail.deliveryTime && <span>✅ Giao hàng: {new Date(detail.deliveryTime).toLocaleString('vi-VN')}</span>}
                  </div>
                  <div style={gpsBoxStyle}>
                    <span style={{ fontSize: 12, color: '#94a3b8' }}>{gpsLabel}</span>
                    <button type="button" onClick={() => void readPosition()} style={secondaryBtnStyle}>
                      📍 Lấy vị trí
                    </button>
                  </div>
                </div>

                {actionError && <div style={alertStyle} role="alert">{actionError}</div>}
                {actionSuccess && <div style={successStyle}>{actionSuccess}</div>}

                {/* QR pickup */}
                {detail.status === 'PICKING_UP' && (
                  <section style={panelStyle}>
                    <h3 style={sectionTitleStyle}>📲 Quét QR xác nhận lấy hàng</h3>
                    <p style={{ fontSize: 12, color: '#94a3b8', lineHeight: 1.5 }}>
                      Quét mã QR trên lô hàng (hoặc dán liên kết <code>/trace/&#123;hash&#125;</code>).
                      Mã phải khớp với lô hàng được phân công.
                    </p>

                    <label style={labelStyle} htmlFor="driver-trace-hash">Trace hash</label>
                    <input
                      id="driver-trace-hash"
                      type="text"
                      value={traceHash}
                      onChange={(e) => setTraceHash(e.target.value)}
                      placeholder="0x… hoặc https://…/trace/0x…"
                      style={inputStyle}
                    />

                    <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginTop: 10 }}>
                      <button type="button" onClick={() => (scanning ? stopScan() : void startScan())} style={primaryBtnStyle}>
                        {scanning ? '⏹ Dừng camera' : '📷 Mở camera quét'}
                      </button>
                      <label style={fileLabelStyle}>
                        🖼️ Tải ảnh QR
                        <input
                          hidden
                          type="file"
                          accept="image/*"
                          onChange={(e) => void uploadQrImage(e.target.files?.[0])}
                        />
                      </label>
                    </div>

                    {scanning && (
                      <video ref={videoRef} autoPlay playsInline muted style={cameraStyle} />
                    )}

                    <label style={labelStyle} htmlFor="driver-pickup-notes">Ghi chú (tuỳ chọn)</label>
                    <textarea
                      id="driver-pickup-notes"
                      value={pickupNotes}
                      onChange={(e) => setPickupNotes(e.target.value)}
                      rows={2}
                      style={inputStyle}
                    />

                    <button
                      type="button"
                      onClick={() => void submitPickup()}
                      disabled={busy === 'pickup'}
                      style={{ ...primaryBtnStyle, width: '100%', marginTop: 14, opacity: busy === 'pickup' ? 0.6 : 1 }}
                    >
                      {busy === 'pickup' ? '⏳ Đang xác nhận…' : '✅ Xác nhận lấy hàng'}
                    </button>
                  </section>
                )}

                {/* Tracking update */}
                {(detail.status === 'PICKING_UP' || detail.status === 'IN_TRANSIT') && (
                  <section style={panelStyle}>
                    <h3 style={sectionTitleStyle}>🗺️ Cập nhật tracking</h3>
                    <label style={labelStyle} htmlFor="driver-tracking-status">Trạng thái</label>
                    <select
                      id="driver-tracking-status"
                      value={trackingStatus}
                      onChange={(e) => setTrackingStatus(e.target.value)}
                      style={inputStyle}
                    >
                      <option value="IN_TRANSIT">Đang vận chuyển</option>
                      <option value="PICKING_UP">Đang đến lấy hàng</option>
                      <option value="ARRIVED">Đã tới điểm giao</option>
                    </select>

                    <label style={labelStyle} htmlFor="driver-tracking-notes">Ghi chú</label>
                    <textarea
                      id="driver-tracking-notes"
                      value={trackingNotes}
                      onChange={(e) => setTrackingNotes(e.target.value)}
                      rows={2}
                      style={inputStyle}
                    />

                    <button
                      type="button"
                      onClick={() => void submitTracking()}
                      disabled={busy === 'tracking'}
                      style={{ ...primaryBtnStyle, width: '100%', marginTop: 14, opacity: busy === 'tracking' ? 0.6 : 1 }}
                    >
                      {busy === 'tracking' ? '⏳ Đang gửi…' : '➕ Thêm điểm tracking'}
                    </button>
                  </section>
                )}

                {/* Delivery */}
                {detail.status === 'IN_TRANSIT' && (
                  <section style={panelStyle}>
                    <h3 style={sectionTitleStyle}>✅ Xác nhận giao hàng</h3>
                    <label style={labelStyle} htmlFor="driver-delivery-notes">Ghi chú giao nhận</label>
                    <textarea
                      id="driver-delivery-notes"
                      value={deliveryNotes}
                      onChange={(e) => setDeliveryNotes(e.target.value)}
                      rows={2}
                      style={inputStyle}
                    />
                    <button
                      type="button"
                      onClick={() => void submitDelivery()}
                      disabled={busy === 'delivery'}
                      style={{ ...primaryBtnStyle, width: '100%', marginTop: 14, opacity: busy === 'delivery' ? 0.6 : 1 }}
                    >
                      {busy === 'delivery' ? '⏳ Đang gửi…' : '🎯 Xác nhận đã giao hàng'}
                    </button>
                  </section>
                )}

                {/* Report to Shipping Manager */}
                {detail.status !== 'DELIVERED' && detail.status !== 'RETURNED' && (
                  <section style={panelStyle}>
                    <h3 style={sectionTitleStyle}>📣 Báo cáo Shipping Manager</h3>
                    <label style={labelStyle} htmlFor="driver-report-type">Loại báo cáo</label>
                    <select
                      id="driver-report-type"
                      value={reportType}
                      onChange={(e) => setReportType(e.target.value)}
                      style={inputStyle}
                    >
                      {REPORT_TYPES.map((t) => (
                        <option key={t} value={t}>{t}</option>
                      ))}
                    </select>

                    <label style={labelStyle} htmlFor="driver-report-desc">Nội dung</label>
                    <textarea
                      id="driver-report-desc"
                      value={reportDescription}
                      onChange={(e) => setReportDescription(e.target.value)}
                      rows={3}
                      placeholder="Mô tả sự cố / chậm trễ / hư hỏng…"
                      style={inputStyle}
                    />

                    <button
                      type="button"
                      onClick={() => void submitReport()}
                      disabled={busy === 'report'}
                      style={{ ...secondaryBtnStyle, width: '100%', marginTop: 14, opacity: busy === 'report' ? 0.6 : 1 }}
                    >
                      {busy === 'report' ? '⏳ Đang gửi…' : '📣 Gửi báo cáo'}
                    </button>
                  </section>
                )}

                {/* Tracking history */}
                <section style={panelStyle}>
                  <h3 style={sectionTitleStyle}>
                    Lịch sử tracking ({detail.trackingHistory?.length ?? 0} điểm)
                  </h3>
                  {(!detail.trackingHistory || detail.trackingHistory.length === 0) && (
                    <p style={{ color: '#64748b', fontSize: 13 }}>Chưa có điểm tracking nào.</p>
                  )}
                  {detail.trackingHistory?.map((pt) => (
                    <div key={pt.id} style={timelineItemStyle}>
                      <div style={timelineDotStyle(pt.status)}>
                        {pt.status.startsWith('REPORT_') ? '⚠️' : '📍'}
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                          <span style={{ color: '#e2e8f0', fontWeight: 600, fontSize: 13 }}>
                            {STEP_LABELS[pt.status] || pt.status}
                          </span>
                          {pt.timestamp && (
                            <span style={{ color: '#64748b', fontSize: 11 }}>
                              {new Date(pt.timestamp).toLocaleString('vi-VN')}
                            </span>
                          )}
                        </div>
                        {pt.notes && <p style={{ margin: '4px 0 0', color: '#94a3b8', fontSize: 12 }}>{pt.notes}</p>}
                        <p style={{ margin: '3px 0 0', color: '#475569', fontSize: 11 }}>
                          GPS: {Number(pt.gpsLat).toFixed(6)}, {Number(pt.gpsLng).toFixed(6)}
                        </p>
                      </div>
                    </div>
                  ))}
                </section>
              </>
            )}
          </>
        )}
      </main>
    </div>
  );
}

/* ── Inline Styles (mobile-first) ── */
const shellStyle: React.CSSProperties = { minHeight: '100vh', background: '#0b0f17', color: '#e2e8f0' };
const headerStyle: React.CSSProperties = {
  position: 'sticky', top: 0, zIndex: 100,
  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
  padding: '12px 16px', background: 'rgba(15, 23, 42, 0.95)',
  borderBottom: '1px solid rgba(255,255,255,0.08)', backdropFilter: 'blur(10px)',
};
const logoStyle: React.CSSProperties = {
  width: 32, height: 32, borderRadius: 9,
  background: 'linear-gradient(135deg, #f59e0b 0%, #10b981 100%)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, color: '#fff',
};
const logoutStyle: React.CSSProperties = {
  background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)', color: '#f87171',
  padding: '6px 12px', borderRadius: 8, fontSize: 12, fontWeight: 600, cursor: 'pointer',
};
const mainStyle: React.CSSProperties = { maxWidth: 640, margin: '0 auto', padding: '16px 16px 48px' };
const pageTitleStyle: React.CSSProperties = { fontSize: 20, fontWeight: 800, color: '#fff', margin: '0 0 14px' };
const filterRowStyle: React.CSSProperties = { display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 };
const filterChipStyle: React.CSSProperties = {
  padding: '6px 12px', borderRadius: 20, border: '1px solid', fontSize: 12,
  fontWeight: 600, cursor: 'pointer',
};
const cardButtonStyle: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', gap: 8, textAlign: 'left',
  padding: 16, borderRadius: 14, border: '1px solid rgba(255,255,255,0.1)',
  background: 'rgba(30,41,59,0.6)', cursor: 'pointer', width: '100%',
};
const cardMetaStyle: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', gap: 4, fontSize: 12, color: '#94a3b8',
};
const panelStyle: React.CSSProperties = {
  padding: 16, marginBottom: 16, borderRadius: 14,
  border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(30,41,59,0.55)',
};
const sectionTitleStyle: React.CSSProperties = { fontSize: 15, fontWeight: 700, color: '#fff', margin: '0 0 10px' };
const detailGridStyle: React.CSSProperties = {
  display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
  gap: '6px 16px', fontSize: 12, color: '#94a3b8', marginTop: 10,
};
const gpsBoxStyle: React.CSSProperties = {
  display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10,
  marginTop: 12, paddingTop: 12, borderTop: '1px solid rgba(255,255,255,0.08)', flexWrap: 'wrap',
};
const labelStyle: React.CSSProperties = { display: 'block', fontSize: 12, color: '#cbd5e1', margin: '12px 0 6px' };
const inputStyle: React.CSSProperties = {
  width: '100%', boxSizing: 'border-box', padding: '10px 12px', borderRadius: 8,
  border: '1px solid #334155', background: '#111827', color: '#fff', fontSize: 13,
};
const primaryBtnStyle: React.CSSProperties = {
  padding: '10px 16px', border: 0, borderRadius: 8,
  background: 'linear-gradient(135deg, #10b981, #06b6d4)', color: '#fff',
  fontWeight: 700, cursor: 'pointer', fontSize: 13,
};
const secondaryBtnStyle: React.CSSProperties = {
  padding: '8px 14px', borderRadius: 8, border: '1px solid #334155',
  background: 'rgba(255,255,255,0.04)', color: '#cbd5e1', fontWeight: 600, cursor: 'pointer', fontSize: 12,
};
const fileLabelStyle: React.CSSProperties = {
  ...primaryBtnStyle, background: 'rgba(255,255,255,0.06)', border: '1px solid #334155',
  display: 'inline-flex', alignItems: 'center', cursor: 'pointer',
};
const cameraStyle: React.CSSProperties = {
  display: 'block', width: '100%', marginTop: 12, borderRadius: 12, border: '2px solid #10b981',
};
const alertStyle: React.CSSProperties = {
  padding: 12, marginBottom: 16, border: '1px solid #ef4444', borderRadius: 8,
  color: '#fecaca', background: 'rgba(239,68,68,.12)', fontSize: 13,
};
const successStyle: React.CSSProperties = {
  padding: 12, marginBottom: 16, border: '1px solid #10b981', borderRadius: 8,
  color: '#a7f3d0', background: 'rgba(16,185,129,.12)', fontSize: 13,
};
const emptyStyle: React.CSSProperties = {
  textAlign: 'center', padding: '40px 16px', color: '#94a3b8', fontSize: 13,
  background: 'rgba(255,255,255,0.02)', borderRadius: 12,
};
const timelineItemStyle: React.CSSProperties = { display: 'flex', gap: 12, marginBottom: 14 };
const timelineDotStyle = (status: string): React.CSSProperties => ({
  width: 28, height: 28, borderRadius: '50%', flexShrink: 0,
  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13,
  background: status.startsWith('REPORT_') ? 'rgba(239,68,68,.15)' : 'rgba(16,185,129,.15)',
  border: `2px solid ${status.startsWith('REPORT_') ? '#f87171' : '#34d399'}`,
});
