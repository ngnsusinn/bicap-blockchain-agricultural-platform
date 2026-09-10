import React, { useState, useEffect } from 'react';
import { Shipment } from '../types';
import { fetchMyShipments, fetchShipmentDetail } from '../utils/api';

interface ShipmentListScreenProps {
  onSelectShipment: (shipment: Shipment, action: 'detail' | 'tracking' | 'qr' | 'pickup' | 'deliver' | 'report') => void;
}

export const ShipmentListScreen: React.FC<ShipmentListScreenProps> = ({ onSelectShipment }) => {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedItem, setSelectedItem] = useState<Shipment | null>(null);
  const [loadingDetail, setLoadingDetail] = useState<boolean>(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await fetchMyShipments(filterStatus === 'ALL' ? undefined : filterStatus);
      setShipments(data);
    } catch {
      setShipments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [filterStatus]);

  const handleOpenDetail = async (s: Shipment) => {
    setSelectedItem(s);
    setLoadingDetail(true);
    try {
      const detail = await fetchShipmentDetail(s.id);
      setSelectedItem(detail);
    } catch (err) {
      console.warn('Could not load full detail:', err);
    } finally {
      setLoadingDetail(false);
    }
  };

  const renderStatusBadge = (status: string) => {
    switch (status) {
      case 'PICKING_UP':
      case 'PENDING_PICKUP':
        return <span className="status-badge warning">Chờ lấy hàng</span>;
      case 'IN_TRANSIT':
        return <span className="status-badge info">Đang di chuyển</span>;
      case 'DELIVERED':
        return <span className="status-badge success">Đã giao thành công</span>;
      case 'RETURNED':
        return <span className="status-badge danger">Đã trả hàng</span>;
      default:
        return <span className="status-badge">{status}</span>;
    }
  };

  return (
    <div className="screen-container">
      <div className="screen-header">
        <h2>Chuyến Hàng Của Tôi</h2>
        <button onClick={loadData} className="btn-icon" title="Làm mới">🔄</button>
      </div>

      <div className="tab-filters">
        <button className={filterStatus === 'ALL' ? 'active' : ''} onClick={() => setFilterStatus('ALL')}>
          Tất cả
        </button>
        <button className={filterStatus === 'PICKING_UP' ? 'active' : ''} onClick={() => setFilterStatus('PICKING_UP')}>
          Chờ lấy
        </button>
        <button className={filterStatus === 'IN_TRANSIT' ? 'active' : ''} onClick={() => setFilterStatus('IN_TRANSIT')}>
          Đang giao
        </button>
        <button className={filterStatus === 'DELIVERED' ? 'active' : ''} onClick={() => setFilterStatus('DELIVERED')}>
          Đã giao
        </button>
      </div>

      {loading ? (
        <div className="loading-state">Đang tải danh sách chuyến hàng...</div>
      ) : shipments.length === 0 ? (
        <div className="empty-state">Không tìm thấy chuyến hàng nào phù hợp.</div>
      ) : (
        <div className="shipment-list">
          {shipments.map((s) => {
            const shipmentCode = s.shipmentCode || `CH-${s.id}`;
            const farmName = s.farmName || 'Nông trại';
            const farmAddr = s.farmAddress || 'Địa chỉ nông trại chưa cập nhật';
            const retailerName = s.retailerName || 'Nhà bán lẻ';
            const retailerAddr = s.deliveryAddr || s.retailerAddress || 'Địa chỉ giao hàng chưa cập nhật';

            return (
              <div key={s.id} className="shipment-card">
                <div className="card-top">
                  <span className="shipment-code">📦 {shipmentCode}</span>
                  {renderStatusBadge(s.status)}
                </div>

                <div className="card-body">
                  <div className="location-row">
                    <div className="dot green"></div>
                    <div className="location-info">
                      <span className="label">Điểm lấy hàng (Nông trại):</span>
                      <strong>{farmName}</strong>
                      <p>{farmAddr}</p>
                    </div>
                  </div>

                  <div className="location-divider"></div>

                  <div className="location-row">
                    <div className="dot blue"></div>
                    <div className="location-info">
                      <span className="label">Điểm giao hàng (Nhà bán lẻ):</span>
                      <strong>{retailerName}</strong>
                      <p>{retailerAddr}</p>
                    </div>
                  </div>

                  {s.items && s.items.length > 0 && (
                    <div className="goods-preview">
                      <span>Hàng hóa: </span>
                      <strong>{s.items.map((i) => `${i.productName} (${i.quantity}${i.unit})`).join(', ')}</strong>
                    </div>
                  )}
                </div>

                <div className="card-actions">
                  <button onClick={() => handleOpenDetail(s)} className="btn-secondary">
                    🔍 Chi tiết
                  </button>
                  <button onClick={() => onSelectShipment(s, 'qr')} className="btn-accent">
                    📱 QR Scan
                  </button>

                  {(s.status === 'PICKING_UP' || s.status === 'PENDING_PICKUP') && (
                    <button onClick={() => onSelectShipment(s, 'pickup')} className="btn-primary">
                      📦 Nhận hàng
                    </button>
                  )}

                  {s.status === 'IN_TRANSIT' && (
                    <>
                      <button onClick={() => onSelectShipment(s, 'tracking')} className="btn-info">
                        📍 Định vị GPS
                      </button>
                      <button onClick={() => onSelectShipment(s, 'deliver')} className="btn-success">
                        ✅ Giao hàng
                      </button>
                    </>
                  )}

                  <button onClick={() => onSelectShipment(s, 'report')} className="btn-danger-outline">
                    ⚠️ Báo sự cố
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {selectedItem && (
        <div className="modal-overlay" onClick={() => setSelectedItem(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Chi Tiết Chuyến Hàng #{selectedItem.shipmentCode || selectedItem.id}</h3>
              <button className="close-btn" onClick={() => setSelectedItem(null)}>✕</button>
            </div>

            <div className="modal-body">
              {loadingDetail ? (
                <div className="loading-state">Đang lấy dữ liệu chi tiết...</div>
              ) : (
                <>
                  <div className="detail-section">
                    <h4>Trạng thái & Phương tiện</h4>
                    <p>Trạng thái hiện tại: {renderStatusBadge(selectedItem.status)}</p>
                    <p>Biển số xe: <strong>{selectedItem.vehicleLicensePlate || selectedItem.licensePlate || 'Chưa phân công'}</strong></p>
                    <p>Mã đơn hàng: <strong>{selectedItem.orderCode || selectedItem.orderId}</strong></p>
                    {selectedItem.routeSummary && <p>Lộ trình: {selectedItem.routeSummary}</p>}
                  </div>

                  <div className="detail-section">
                    <h4>Thông tin Nông trại (Lấy hàng)</h4>
                    <p>Tên nông trại: <strong>{selectedItem.farmName || 'Chưa cập nhật'}</strong></p>
                    <p>Địa chỉ: {selectedItem.farmAddress || 'Chưa cập nhật'}</p>
                    {selectedItem.farmPhone && (
                      <p>SĐT liên hệ: <a href={`tel:${selectedItem.farmPhone}`}>{selectedItem.farmPhone}</a></p>
                    )}
                  </div>

                  <div className="detail-section">
                    <h4>Thông tin Nhà bán lẻ (Giao hàng)</h4>
                    <p>Tên đơn vị: <strong>{selectedItem.retailerName || 'Chưa cập nhật'}</strong></p>
                    <p>Địa chỉ: {selectedItem.deliveryAddr || selectedItem.retailerAddress || 'Chưa cập nhật'}</p>
                    {selectedItem.retailerPhone && (
                      <p>SĐT liên hệ: <a href={`tel:${selectedItem.retailerPhone}`}>{selectedItem.retailerPhone}</a></p>
                    )}
                  </div>

                  {selectedItem.items && selectedItem.items.length > 0 && (
                    <div className="detail-section">
                      <h4>Danh sách sản phẩm</h4>
                      <ul>
                        {selectedItem.items.map((it, idx) => (
                          <li key={idx}>
                            {it.productName} — <strong>{it.quantity} {it.unit}</strong>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {(selectedItem.trackingHistory || selectedItem.trackingList) && (selectedItem.trackingHistory || selectedItem.trackingList)!.length > 0 && (
                    <div className="detail-section">
                      <h4>Lịch sử hành trình</h4>
                      <div className="timeline">
                        {(selectedItem.trackingHistory || selectedItem.trackingList)!.map((t) => (
                          <div key={t.id} className="timeline-item">
                            <div className="timeline-date">{new Date(t.timestamp).toLocaleString('vi-VN')}</div>
                            <div className="timeline-status">[{t.status}]</div>
                            {t.notes && <p>{t.notes}</p>}
                            {t.gpsLat !== 0 && (
                              <div className="gps-tag">📍 GPS: {t.gpsLat.toFixed(4)}, {t.gpsLng.toFixed(4)}</div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setSelectedItem(null)}>Đóng</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
