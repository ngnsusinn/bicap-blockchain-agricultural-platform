import React, { useState, useEffect, useRef } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import jsQR from 'jsqr';
import { Shipment } from '../types';
import { traceProductByQr } from '../utils/api';

interface QRScanScreenProps {
  shipment?: Shipment;
  onBack: () => void;
}

export const QRScanScreen: React.FC<QRScanScreenProps> = ({ shipment, onBack }) => {
  const [traceHash, setTraceHash] = useState('');
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [cameraActive, setCameraActive] = useState(false);
  const [statusMsg, setStatusMsg] = useState('Đang khởi tạo Camera...');

  const html5QrcodeRef = useRef<Html5Qrcode | null>(null);

  const extractHash = (rawInput: string): string => {
    let cleaned = rawInput.trim();
    if (cleaned.includes('/trace/')) {
      cleaned = cleaned.split('/trace/').pop() || cleaned;
    } else if (cleaned.includes('http://') || cleaned.includes('https://')) {
      cleaned = cleaned.split('/').pop() || cleaned;
    }
    return cleaned.split('?')[0].split('#')[0].trim();
  };

  const handleTrace = async (hashToLookup?: string) => {
    const raw = hashToLookup || traceHash;
    const hash = extractHash(raw);
    if (!hash) {
      setError('Vui lòng nhập hoặc quét mã QR Code để truy xuất.');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);

    try {
      const data = await traceProductByQr(hash);
      setResult(data);
    } catch (err: any) {
      setError(err.message || `Không tìm thấy dữ liệu sản phẩm với mã QR: "${hash}"`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    let scanner: Html5Qrcode | null = null;

    const initScanner = async () => {
      try {
        scanner = new Html5Qrcode('qr-reader');
        html5QrcodeRef.current = scanner;

        await scanner.start(
          { facingMode: 'environment' },
          {
            fps: 10,
            qrbox: { width: 220, height: 220 }
          },
          (decodedText) => {
            if (isMounted) {
              const hash = extractHash(decodedText);
              setTraceHash(hash);
              handleTrace(hash);
            }
          },
          () => {}
        );

        if (isMounted) {
          setCameraActive(true);
          setStatusMsg('Đặt mã QR Code nông sản vào khung hình vuông bên dưới');
        }
      } catch (err: any) {
        console.warn('Camera failed to start:', err);
        if (isMounted) {
          setCameraActive(false);
          setStatusMsg('Camera chưa tự động phát hiện. Bạn có thể bấm "Tải Ảnh Mã QR Từ Máy" hoặc nhập mã bên dưới.');
        }
      }
    };

    const timer = setTimeout(initScanner, 300);

    return () => {
      isMounted = false;
      clearTimeout(timer);
      if (scanner && scanner.isScanning) {
        scanner.stop().catch((e) => console.warn('Stop scanner error:', e)).finally(() => {
          try {
            scanner?.clear();
          } catch {}
        });
      }
    };
  }, []);

  const decodeQRFromFile = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = async () => {
          try {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            if (!ctx) throw new Error('Canvas context error');

            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);

            let imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
            let code = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: 'attemptBoth' });
            if (code && code.data && code.data.trim()) {
              return resolve(extractHash(code.data));
            }

            const scaleTargets = [1600, 1200, 800, 500];
            for (const targetSize of scaleTargets) {
              if (img.width > targetSize || img.height > targetSize) {
                const scale = Math.min(targetSize / img.width, targetSize / img.height);
                canvas.width = Math.round(img.width * scale);
                canvas.height = Math.round(img.height * scale);
                ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                code = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: 'attemptBoth' });
                if (code && code.data && code.data.trim()) {
                  return resolve(extractHash(code.data));
                }
              }
            }

            try {
              const html5Qrcode = html5QrcodeRef.current || new Html5Qrcode('qr-reader');
              const decoded = await html5Qrcode.scanFile(file, false);
              const fileNameBase = file.name.substring(0, file.name.lastIndexOf('.')) || file.name;
              if (decoded && decoded !== file.name && decoded !== fileNameBase) {
                return resolve(extractHash(decoded));
              }
            } catch {}

            reject(new Error('Không giải mã được mã QR từ bức ảnh này. Vui lòng chọn ảnh chụp mã QR vuông vắn và rõ nét hơn.'));
          } catch (err: any) {
            reject(err);
          }
        };
        img.onerror = () => reject(new Error('Lỗi đọc nội dung hình ảnh.'));
        img.src = e.target?.result as string;
      };
      reader.onerror = () => reject(new Error('Lỗi tải tệp ảnh từ máy.'));
      reader.readAsDataURL(file);
    });
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setLoading(true);
    setError('');
    setResult(null);

    try {
      const decodedHash = await decodeQRFromFile(file);
      setTraceHash(decodedHash);
      await handleTrace(decodedHash);
    } catch (err: any) {
      console.error('File decode error:', err);
      setError(err.message || 'Không quét được mã QR từ tệp ảnh vừa tải lên.');
    } finally {
      setLoading(false);
      e.target.value = '';
    }
  };

  return (
    <div className="screen-container">
      <div className="screen-header">
        <button onClick={onBack} className="btn-back">← Quay lại</button>
        <h2>Quét QR Mã Lô Hàng</h2>
      </div>

      <div className="qr-scanner-box">
        <div className="scanner-status-text" style={{ fontSize: '13px', color: '#94a3b8', textAlign: 'center', marginBottom: '8px' }}>
          {statusMsg}
        </div>

        <div className="scanner-viewport" style={{ width: '100%', maxWidth: '300px', minHeight: '260px', position: 'relative', overflow: 'hidden', borderRadius: '12px', background: '#000' }}>
          <div id="qr-reader" style={{ width: '100%', height: '100%' }}></div>
          {!cameraActive && (
            <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '16px' }}>
              <span style={{ fontSize: '32px', marginBottom: '8px' }}>📷</span>
              <span style={{ fontSize: '12px', color: '#94a3b8', textAlign: 'center' }}>
                Camera chưa phát hiện hoặc không được cấp quyền.
              </span>
            </div>
          )}
        </div>

        <div className="qr-controls">
          <label className="btn-secondary full-width text-center cursor-pointer" style={{ display: 'inline-block', textAlign: 'center' }}>
            📷 Tải Ảnh Mã QR Từ Máy
            <input type="file" accept="image/*" hidden onChange={handleFileUpload} />
          </label>

          <div className="manual-input-group">
            <input
              type="text"
              value={traceHash}
              onChange={(e) => setTraceHash(e.target.value)}
              placeholder="Nhập mã trace hash hoặc chuỗi..."
            />
            <button onClick={() => handleTrace()} disabled={loading} className="btn-primary">
              {loading ? 'Đang đọc...' : 'Quét Mã'}
            </button>
          </div>
        </div>
      </div>

      {error && <div className="alert-box error">{error}</div>}

      {result && (
        <div className="verification-card">
          <div className="verification-header success">
            <span>✅ DỮ LIỆU BLOCKCHAIN ĐÃ XÁC MINH</span>
          </div>

          <div className="verification-body">
            <h3>{result.name || result.productName}</h3>
            <p><strong>Nông trại sản xuất:</strong> {result.farmName || 'Chưa cập nhật'}</p>
            <p><strong>Địa chỉ:</strong> {result.farmAddress || 'Chưa cập nhật'}</p>
            <p><strong>Số lượng lô hàng:</strong> {result.quantity} {result.unit}</p>

            {result.timeline && result.timeline.length > 0 && (
              <div style={{ marginTop: '12px' }}>
                <strong>Quy trình nhật ký canh tác (Blockchain):</strong>
                <ul style={{ paddingLeft: '20px', marginTop: '6px', fontSize: '13px' }}>
                  {result.timeline.map((proc: any, idx: number) => (
                    <li key={idx}>
                      [{proc.executionDate}] <strong>{proc.processType}</strong>: {proc.notes || 'Thực hiện đúng quy trình'}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <div className="tx-hash" style={{ marginTop: '12px' }}>
              <span>Blockchain TX Hash / Trace Hash:</span>
              <code>{result.transactionHash || result.traceHash || '-'}</code>
            </div>
          </div>

          <div className="verification-footer">
            <button onClick={() => alert('Đã xác nhận thông tin nông sản trùng khớp với đơn hàng!')} className="btn-success full-width">
              👍 Đúng Khớp Thông Tin — Sẵn Sàng Nhận Hàng
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
