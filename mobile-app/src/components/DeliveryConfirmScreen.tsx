import React, { useState, useEffect, useRef } from 'react';
import { Shipment } from '../types';
import { confirmDeliveryGoods } from '../utils/api';

interface DeliveryConfirmScreenProps {
  shipment: Shipment;
  onBack: () => void;
  onSuccess: () => void;
}

export const DeliveryConfirmScreen: React.FC<DeliveryConfirmScreenProps> = ({ shipment, onBack, onSuccess }) => {
  const [notes, setNotes] = useState('');
  const [lat, setLat] = useState<number>(0);
  const [lng, setLng] = useState<number>(0);
  const [images, setImages] = useState<string[]>([]);
  const [signature, setSignature] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const isDrawing = useRef(false);

  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((pos) => {
        setLat(pos.coords.latitude);
        setLng(pos.coords.longitude);
      });
    }
  }, []);

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;

    Array.from(files).forEach((file) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setImages((prev) => [...prev, event.target!.result as string]);
        }
      };
      reader.readAsDataURL(file);
    });
  };

  const startDrawing = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    isDrawing.current = true;
    const rect = canvas.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;

    ctx.beginPath();
    ctx.moveTo(clientX - rect.left, clientY - rect.top);
  };

  const draw = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    if (!isDrawing.current) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const rect = canvas.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;

    ctx.lineTo(clientX - rect.left, clientY - rect.top);
    ctx.strokeStyle = '#10b981';
    ctx.lineWidth = 3;
    ctx.stroke();
  };

  const stopDrawing = () => {
    if (isDrawing.current && canvasRef.current) {
      isDrawing.current = false;
      setSignature(canvasRef.current.toDataURL());
    }
  };

  const clearSignature = () => {
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      ctx?.clearRect(0, 0, canvas.width, canvas.height);
      setSignature('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await confirmDeliveryGoods(shipment.id, {
        gpsLat: lat,
        gpsLng: lng,
        images: images,
        notes,
        recipientSignature: signature
      });
      alert('Xác nhận giao hàng thành công! Đơn hàng đã chuyển sang trạng thái ĐÃ GIAO HÀNG.');
      onSuccess();
    } catch (err: any) {
      setError(err.message || 'Xác nhận giao hàng thất bại.');
    } finally {
      setLoading(false);
    }
  };

  const retailerName = shipment.retailerName || 'Nhà bán lẻ';
  const retailerAddr = shipment.deliveryAddr || shipment.retailerAddress || 'Địa chỉ chưa cập nhật';

  return (
    <div className="screen-container">
      <div className="screen-header">
        <button onClick={onBack} className="btn-back">← Quay lại</button>
        <h2>Xác Nhận Giao Hàng Cho Nhà Bán Lẻ</h2>
      </div>

      <div className="shipment-brief-card highlight-blue">
        <h3>🏬 Giao tới: {retailerName}</h3>
        <p><strong>Địa chỉ giao:</strong> {retailerAddr}</p>
        <p><strong>SĐT liên hệ người nhận:</strong> {shipment.retailerPhone || 'Chưa cập nhật'}</p>
      </div>

      {error && <div className="alert-box error">{error}</div>}

      <form onSubmit={handleSubmit} className="form-card">
        <div className="form-group">
          <label>Tọa độ GPS điểm giao hàng (Tự động)</label>
          <div className="gps-inputs">
            <input type="number" step="any" value={lat} onChange={(e) => setLat(parseFloat(e.target.value) || 0)} placeholder="Lat" />
            <input type="number" step="any" value={lng} onChange={(e) => setLng(parseFloat(e.target.value) || 0)} placeholder="Lng" />
          </div>
        </div>

        <div className="form-group">
          <label>Ảnh chụp bằng chứng bàn giao hàng</label>
          <input type="file" accept="image/*" multiple onChange={handleImageUpload} />
          {images.length > 0 && (
            <div className="image-preview-grid">
              {images.map((img, idx) => (
                <img key={idx} src={img} alt="Bằng chứng giao hàng" />
              ))}
            </div>
          )}
        </div>

        <div className="form-group">
          <div className="flex-between">
            <label>Chữ ký điện tử người nhận hàng (Ký trực tiếp bên dưới)</label>
            <button type="button" onClick={clearSignature} className="btn-link">Xóa chữ ký</button>
          </div>
          <canvas
            ref={canvasRef}
            width={320}
            height={150}
            className="signature-canvas"
            onMouseDown={startDrawing}
            onMouseMove={draw}
            onMouseUp={stopDrawing}
            onMouseLeave={stopDrawing}
            onTouchStart={startDrawing}
            onTouchMove={draw}
            onTouchEnd={stopDrawing}
          />
        </div>

        <div className="form-group">
          <label>Ghi chú giao hàng / Biên bản nhận hàng</label>
          <textarea
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Ghi chú thêm về người nhận, tình trạng thùng hàng..."
          />
        </div>

        <button type="submit" disabled={loading} className="btn-success full-width large">
          {loading ? 'Đang gửi hoàn tất...' : '🎉 BÀN GIAO THÀNH CÔNG (HOÀN TẤT ĐƠN HÀNG)'}
        </button>
      </form>
    </div>
  );
};
