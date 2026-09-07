import React, { useState, useEffect } from 'react';
import { Shipment } from '../types';
import { sendDriverReport } from '../utils/api';

interface ReportScreenProps {
  shipment?: Shipment;
  onBack: () => void;
  onSuccess: () => void;
}

export const ReportScreen: React.FC<ReportScreenProps> = ({ shipment, onBack, onSuccess }) => {
  const [reportType, setReportType] = useState('PRODUCT_DAMAGE');
  const [description, setDescription] = useState('');
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
    if (!shipment) {
      setError('Vui lòng chọn một chuyến hàng cụ thể để gửi báo cáo sự cố.');
      return;
    }

    if (!description.trim()) {
      setError('Vui lòng nhập chi tiết nội dung sự cố.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await sendDriverReport({
        shipmentId: shipment.id,
        reportType,
        description,
        gpsLat: lat,
        gpsLng: lng,
        images
      });
      alert('Đã gửi báo cáo sự cố tới Shipping Manager thành công!');
      onSuccess();
    } catch (err: any) {
      setError(err.message || 'Gửi báo cáo sự cố không thành công.');
    } finally {
      setLoading(false);
    }
  };

  const farmName = shipment?.farmName || 'Nông trại';
  const retailerName = shipment?.retailerName || shipment?.deliveryAddr || 'Nhà bán lẻ';

  return (
    <div className="screen-container">
      <div className="screen-header">
        <button onClick={onBack} className="btn-back">← Quay lại</button>
        <h2>Báo Cáo Sự Cố Cho Manager</h2>
      </div>

      {shipment && (
        <div className="shipment-brief-card warning">
          <div>Mã chuyến hàng: <strong>#{shipment.shipmentCode || shipment.id}</strong></div>
          <div>Lộ trình: <strong>{farmName}</strong> → <strong>{retailerName}</strong></div>
        </div>
      )}

      {error && <div className="alert-box error">{error}</div>}

      <form onSubmit={handleSubmit} className="form-card">
        <div className="form-group">
          <label>Phân loại sự cố</label>
          <select value={reportType} onChange={(e) => setReportType(e.target.value)}>
            <option value="PRODUCT_DAMAGE">Hàng hóa bị hư hỏng / Dập nát</option>
            <option value="TRAFFIC_ACCIDENT">Sự cố hỏng xe / Tai nạn giao thông</option>
            <option value="WEATHER_DELAY">Thời tiết xấu / Trễ giờ giao</option>
            <option value="CUSTOMER_COMPLAINT">Nhà bán lẻ khiếu nại chất lượng</option>
            <option value="OTHER">Sự cố khác</option>
          </select>
        </div>

        <div className="form-group">
          <label>Mô tả chi tiết sự cố phát sinh *</label>
          <textarea
            rows={4}
            required
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Mô tả cụ thể diễn biến sự cố, nguyên nhân và đề xuất hỗ trợ từ Shipping Manager..."
          />
        </div>

        <div className="form-group">
          <label>Hình ảnh thực tế hiện trường sự cố</label>
          <input type="file" accept="image/*" multiple onChange={handleImageUpload} />
          {images.length > 0 && (
            <div className="image-preview-grid">
              {images.map((img, idx) => (
                <img key={idx} src={img} alt="Hiện trường sự cố" />
              ))}
            </div>
          )}
        </div>

        <div className="form-group">
          <label>Vị trí xảy ra sự cố (GPS)</label>
          <div className="gps-inputs">
            <input type="number" step="any" value={lat} onChange={(e) => setLat(parseFloat(e.target.value) || 0)} placeholder="Lat" />
            <input type="number" step="any" value={lng} onChange={(e) => setLng(parseFloat(e.target.value) || 0)} placeholder="Lng" />
          </div>
        </div>

        <button type="submit" disabled={loading} className="btn-danger full-width">
          {loading ? 'Đang gửi...' : '🚨 Gửi Báo Cáo Khẩn Cấp Cho Shipping Manager'}
        </button>
      </form>
    </div>
  );
};
