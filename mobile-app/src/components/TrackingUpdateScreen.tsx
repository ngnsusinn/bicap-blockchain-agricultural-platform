import React, { useState, useEffect } from 'react';
import { Shipment } from '../types';
import { addTrackingCheckpoint } from '../utils/api';

interface TrackingUpdateScreenProps {
  shipment: Shipment;
  onBack: () => void;
  onSuccess: () => void;
}

export const TrackingUpdateScreen: React.FC<TrackingUpdateScreenProps> = ({ shipment, onBack, onSuccess }) => {
  const [status, setStatus] = useState('IN_TRANSIT');
  const [notes, setNotes] = useState('');
  const [lat, setLat] = useState<number>(0);
  const [lng, setLng] = useState<number>(0);
  const [images, setImages] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [gpsStatus, setGpsStatus] = useState('Đang lấy vị trí GPS từ thiết bị...');

  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLat(pos.coords.latitude);
          setLng(pos.coords.longitude);
          setGpsStatus(`📍 Tọa độ GPS tự động: ${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)}`);
        },
        () => {
          setGpsStatus('📍 Vui lòng cấp quyền truy cập vị trí trên thiết bị hoặc nhập tay bên dưới');
        }
      );
    } else {
      setGpsStatus('📍 Thiết bị không hỗ trợ Geolocation API, vui lòng nhập tọa độ thủ công');
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
      await addTrackingCheckpoint(shipment.id, {
        status,
        gpsLat: lat,
        gpsLng: lng,
        notes,
        images
      });
      alert('Cập nhật tiến độ hành trình thành công!');
      onSuccess();
    } catch (err: any) {
      setError(err.message || 'Cập nhật thất bại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="screen-container">
      <div className="screen-header">
        <button onClick={onBack} className="btn-back">← Quay lại</button>
        <h2>Cập Nhật Hành Trình</h2>
      </div>

      <div className="shipment-brief-card">
        <div>Mã chuyến: <strong>#{shipment.shipmentCode || shipment.id}</strong></div>
        <div>Nông trại: <strong>{shipment.farmName || 'Chưa cập nhật'}</strong></div>
        <div>Nhà bán lẻ: <strong>{shipment.retailerName || 'Chưa cập nhật'}</strong></div>
      </div>

      {error && <div className="alert-box error">{error}</div>}

      <form onSubmit={handleSubmit} className="form-card">
        <div className="form-group">
          <label>Trạng thái hành trình</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="IN_TRANSIT">Đang di chuyển trên đường</option>
            <option value="RESTING">Đang dừng nghỉ / Đợi giao</option>
            <option value="INCIDENT">Gặp sự cố / Kẹt xe</option>
            <option value="PICKED_UP">Đã bốc hàng lên xe</option>
          </select>
        </div>

        <div className="form-group">
          <label>Ghi chú lộ trình</label>
          <textarea
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Nhập ghi chú địa điểm, tình trạng xe hoặc tình hình giao thông..."
          />
        </div>

        <div className="form-group">
          <label>Định vị GPS ({gpsStatus})</label>
          <div className="gps-inputs">
            <input
              type="number"
              step="any"
              value={lat}
              onChange={(e) => setLat(parseFloat(e.target.value) || 0)}
              placeholder="Vĩ độ Lat"
            />
            <input
              type="number"
              step="any"
              value={lng}
              onChange={(e) => setLng(parseFloat(e.target.value) || 0)}
              placeholder="Kinh độ Lng"
            />
          </div>
        </div>

        <div className="form-group">
          <label>Hình ảnh thực tế trên đường</label>
          <input type="file" accept="image/*" multiple onChange={handleImageUpload} />
          {images.length > 0 && (
            <div className="image-preview-grid">
              {images.map((img, idx) => (
                <img key={idx} src={img} alt="Preview" />
              ))}
            </div>
          )}
        </div>

        <button type="submit" disabled={loading} className="btn-primary full-width">
          {loading ? 'Đang cập nhật...' : '📍 Ghi Nhận Tiến Độ & GPS'}
        </button>
      </form>
    </div>
  );
};
