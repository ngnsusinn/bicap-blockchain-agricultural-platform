import { useEffect, useRef, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

type TraceResult = {
  name: string; farmName: string; farmAddress: string; seasonName?: string; harvestDate?: string;
  certifications: string[]; transactionHash?: string;
  processes?: { processType: string; executionDate: string; materials?: string; notes?: string; transactionHash?: string }[];
};

/** Tra cứu nguồn gốc bằng QR — dùng chung design system với các trang còn lại. */
export default function QrScannerPage() {
  const video = useRef<HTMLVideoElement>(null);
  const stream = useRef<MediaStream | null>(null);
  const timer = useRef<number | null>(null);
  const [value, setValue] = useState('');
  const [error, setError] = useState('');
  const [scanning, setScanning] = useState(false);
  const [result, setResult] = useState<TraceResult | null>(null);

  const stop = () => {
    if (timer.current) window.clearInterval(timer.current);
    timer.current = null;
    stream.current?.getTracks().forEach((t) => t.stop());
    stream.current = null;
    setScanning(false);
  };

  useEffect(() => stop, []);

  const lookup = async (raw: string) => {
    const hash = raw.trim().split('/').filter(Boolean).pop() || '';
    if (!hash) {
      setError('Mã QR không chứa mã truy xuất hợp lệ.');
      return;
    }
    setError('');
    const r = await fetch(`${API_BASE_URL}/marketplace/products/trace/${encodeURIComponent(hash)}`, { headers: getAuthHeaders() });
    const b = await r.json().catch(() => ({}));
    if (!r.ok) {
      setError(b.message || 'Không tìm thấy sản phẩm từ mã QR.');
      return;
    }
    setValue(raw);
    setResult(b);
    stop();
  };

  const detector = () => {
    const D = (window as unknown as { BarcodeDetector?: new (o: { formats: string[] }) => { detect: (s: unknown) => Promise<{ rawValue?: string }[]> } }).BarcodeDetector;
    if (!D) throw new Error('Trình duyệt này chưa hỗ trợ đọc QR. Hãy dùng Chrome/Edge mới hoặc dán liên kết truy xuất.');
    return new D({ formats: ['qr_code'] });
  };

  const start = async () => {
    try {
      setError('');
      const d = detector();
      stream.current = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      if (video.current) video.current.srcObject = stream.current;
      setScanning(true);
      timer.current = window.setInterval(async () => {
        if (!video.current || video.current.readyState < 2) return;
        const codes = await d.detect(video.current).catch(() => []);
        if (codes[0]?.rawValue) void lookup(codes[0].rawValue);
      }, 700);
    } catch (e) {
      stop();
      setError(e instanceof Error ? e.message : 'Không thể mở camera.');
    }
  };

  const upload = async (file?: File) => {
    if (!file) return;
    try {
      setError('');
      const image = await createImageBitmap(file);
      const codes = await detector().detect(image);
      image.close();
      if (!codes[0]?.rawValue) throw new Error('Không phát hiện mã QR trong ảnh.');
      await lookup(codes[0].rawValue);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể đọc ảnh QR.');
    }
  };

  return (
    <section>
      <div style={{ marginBottom: 24 }}>
        <h1 className="dashboard-title">Quét QR truy xuất nguồn gốc</h1>
        <p className="dashboard-subtitle" style={{ marginBottom: 0 }}>
          Quét bằng camera, tải ảnh QR hoặc dán liên kết truy xuất được tạo khi xuất kho.
        </p>
      </div>

      <div className="glass-panel" style={actions}>
        <button className="btn btn-gradient" onClick={scanning ? stop : () => void start()}>
          {scanning ? 'Dừng camera' : 'Mở camera'}
        </button>
        <label className="btn btn-secondary" style={{ cursor: 'pointer' }}>
          Tải ảnh QR
          <input hidden type="file" accept="image/*" onChange={(e) => void upload(e.target.files?.[0])} />
        </label>
        <input
          className="form-input" style={{ flex: '1 1 260px' }} placeholder="Dán liên kết hoặc trace hash"
          value={value} onChange={(e) => setValue(e.target.value)}
        />
        <button className="btn btn-secondary" onClick={() => void lookup(value)}>Truy xuất</button>
      </div>

      {scanning && <video ref={video} autoPlay playsInline muted style={camera} />}

      {error && <div style={alert} role="alert">{error}</div>}

      {result && (
        <article className="glass-panel" style={panel}>
          <p style={{ color: 'var(--success-hover)', fontWeight: 700 }}>✓ Đã xác minh dữ liệu truy xuất</p>
          <h2 style={{ marginBottom: 10 }}>{result.name}</h2>
          <p>🏡 {result.farmName} , {result.farmAddress}</p>
          <p>🌾 {result.seasonName || 'Chưa cập nhật mùa vụ'} , Thu hoạch {result.harvestDate || '—'}</p>
          <p>🏅 {result.certifications?.join(', ') || 'Chưa cập nhật chứng nhận'}</p>
          <p style={hash}>Blockchain TX: {result.transactionHash || '—'}</p>

          <h3 style={{ marginTop: 18, marginBottom: 10 }}>Quy trình canh tác</h3>
          {result.processes?.length ? result.processes.map((p, i) => (
            <div key={i} style={timeline}>
              <b>{p.executionDate} , {p.processType}</b>
              <div>{p.materials || p.notes || 'Không có ghi chú'}</div>
              <small style={hash}>{p.transactionHash}</small>
            </div>
          )) : <p style={muted}>Chưa có quy trình được ghi nhận.</p>}
        </article>
      )}
    </section>
  );
}

/* ── Styles ── */
const actions: React.CSSProperties = {
  display: 'flex',
  flexWrap: 'wrap',
  alignItems: 'center',
  gap: 10,
  padding: 20,
};
const muted: React.CSSProperties = { color: 'var(--text-secondary)', fontSize: 13 };
const camera: React.CSSProperties = {
  display: 'block',
  width: 'min(640px, 100%)',
  margin: '18px auto',
  borderRadius: 16,
  border: '2px solid var(--success)',
};
const alert: React.CSSProperties = {
  marginTop: 16,
  padding: 12,
  border: '1px solid var(--danger)',
  background: 'var(--danger-light)',
  borderRadius: 8,
  color: '#fecaca',
};
const panel: React.CSSProperties = { marginTop: 18, padding: 24 };
const timeline: React.CSSProperties = { borderLeft: '2px solid var(--success)', padding: '4px 0 14px 14px', color: 'var(--text-secondary)' };
const hash: React.CSSProperties = { color: 'var(--text-muted)', fontFamily: 'monospace', overflowWrap: 'anywhere', fontSize: 12 };
