import React, { useState, useEffect } from 'react';
import { Shipment } from '../types';
import { confirmPickupGoods } from '../utils/api';

interface PickupConfirmScreenProps {
  shipment: Shipment;
  onBack: () => void;
  onSuccess: () => void;
}

export const PickupConfirmScreen: React.FC<PickupConfirmScreenProps> = ({ shipment, onBack, onSuccess }) => {
  const [notes, setNotes] = useState('');
  const [lat, setLat] = useState<number>(0);
  const [lng, setLng] = useState<number>(0);
  const [images, setImages] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await confirmPickupGoods(shipment.id, {
        gpsLat: lat,
        gpsLng: lng,
        notes,
        images
      });
      alert('Đã xác nhận nhận đủ hàng! Đơn vận chuyển đã chuyển sang trạng thái ĐANG VẬN CHUYỂN.');
      onSuccess();
    } catch (err: any) {
      setError(err.message || 'Xác nhận nhận hàng không thành công.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="screen-container">
      <div className="screen-header">
        <button onClick={onBack} className="btn-back">← Quay lại</button>
        <h2>Xác Nhận Nhận Hàng Nông Trại</h2>
      </div>

      <div className="shipment-brief-card highlight">
        <h3>📦 Lô Hàng #{shipment.shipmentCode || shipment.id}</h3>
        <p><strong>Nông trại giao:</strong> {shipment.farmName || 'Chưa cập nhật'}</p>
        <p><strong>Địa chỉ:</strong> {shipment.farmAddress || 'Chưa cập nhật'}</p>
        {shipment.items && shipment.items.length > 0 && (
          <p><strong>Tổng sản phẩm:</strong> {shipment.items.map(i => `${i.productName} (${i.quantity} ${i.unit})`).join(', ')}</p>
        )}
      </div>

      {error && <div className="alert-box error">{error}</div>}

      <form onSubmit={handleSubmit} className="form-card">
        <div className="form-group">
          <label>Tọa độ GPS điểm bốc hàng (Tự động)</label>
          <div className="gps-inputs">
            <input type="number" step="any" value={lat} onChange={(e) => setLat(parseFloat(e.target.value) || 0)} placeholder="Lat" />
            <input type="number" step="any" value={lng} onChange={(e) => setLng(parseFloat(e.target.value) || 0)} placeholder="Lng" />
          </div>
        </div>

        <div className="form-group">
          <label>Ảnh chụp hàng hóa đã xếp lên xe</label>
          <input type="file" accept="image/*" multiple onChange={handleImageUpload} />
          {images.length > 0 && (
            <div className="image-preview-grid">
              {images.map((img, idx) => (
                <img key={idx} src={img} alt="Bốc hàng" />
              ))}
            </div>
          )}
        </div>

        <div className="form-group">
          <label>Ghi chú biên bản bàn giao tại nông trại</label>
          <textarea
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Nhập tình trạng đóng gói, số lượng thực tế nhận..."
          />
        </div>

        <button type="submit" disabled={loading} className="btn-success full-width">
          {loading ? 'Đang gửi xác nhận...' : '✅ Xác Nhận Đã Nhận Đủ Hàng (Chuyển Đang Giao)'}
        </button>
      </form>
    </div>
  );
};
