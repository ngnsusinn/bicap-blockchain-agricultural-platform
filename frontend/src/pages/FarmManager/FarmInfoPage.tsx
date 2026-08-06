import React, { useState, useEffect, useCallback } from 'react';
import { API_BASE_URL, getAuthHeaders, logout } from '../../utils/auth';

// ── Types ─────────────────────────────────────────────────────────────────────

interface FarmData {
  id: number;
  name: string;
  address: string;
  area: number;
  gpsLat: number | null;
  gpsLng: number | null;
  description: string;
  productTypes: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface Certification {
  id: number;
  type: string;
  fileUrl: string;
  expiryDate: string;
}

interface Message {
  type: 'success' | 'error';
  text: string;
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function FarmInfoPage() {
  // Farm state — null = chưa load xong, undefined = đã load, user chưa có farm
  const [farm, setFarm] = useState<FarmData | null | undefined>(undefined);
  const [loading, setLoading] = useState(true);

  // Form fields — dùng chung cho cả tạo mới lẫn cập nhật
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [area, setArea] = useState('');
  const [gpsLat, setGpsLat] = useState('');
  const [gpsLng, setGpsLng] = useState('');
  const [description, setDescription] = useState('');
  const [productTypes, setProductTypes] = useState('');
  const [saving, setSaving] = useState(false);
  const [farmMessage, setFarmMessage] = useState<Message | null>(null);

  // Certifications
  const [certifications, setCertifications] = useState<Certification[]>([]);
  const [certLoading, setCertLoading] = useState(false);
  const [certMessage, setCertMessage] = useState<Message | null>(null);
  const [showAddCert, setShowAddCert] = useState(false);
  const [certType, setCertType] = useState('');
  const [certFileUrl, setCertFileUrl] = useState('');
  const [certExpiryDate, setCertExpiryDate] = useState('');
  const [addingCert, setAddingCert] = useState(false);
  const [deletingCertId, setDeletingCertId] = useState<number | null>(null);

  // ── Data loading ──────────────────────────────────────────────────────────────

  const fetchFarm = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/my`, { headers: getAuthHeaders() });
      if (res.ok) {
        const farms: FarmData[] = await res.json();
        if (farms && farms.length > 0) {
          const f = farms[0];
          setFarm(f);
          setName(f.name || '');
          setAddress(f.address || '');
          setArea(f.area != null ? String(f.area) : '');
          setGpsLat(f.gpsLat != null ? String(f.gpsLat) : '');
          setGpsLng(f.gpsLng != null ? String(f.gpsLng) : '');
          setDescription(f.description || '');
          setProductTypes(f.productTypes || '');
        } else {
          // Chưa có farm — vẫn hiển thị form để tạo mới
          setFarm(null);
        }
      } else {
        setFarm(null);
      }
    } catch {
      setFarm(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchCertifications = useCallback(async (farmId: number) => {
    setCertLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/${farmId}/certifications`, {
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        const data: Certification[] = await res.json();
        setCertifications(data);
      }
    } catch {
      // non-fatal
    } finally {
      setCertLoading(false);
    }
  }, []);

  useEffect(() => { fetchFarm(); }, [fetchFarm]);

  useEffect(() => {
    if (farm?.id) fetchCertifications(farm.id);
  }, [farm?.id, fetchCertifications]);


  // ── Farm save handler (create OR update) ─────────────────────────────────────

  const handleSaveFarm = async (e: React.FormEvent) => {
    e.preventDefault();
    setFarmMessage(null);

    if (!name.trim()) {
      setFarmMessage({ type: 'error', text: 'Tên nông trại không được để trống.' });
      return;
    }
    if (!address.trim()) {
      setFarmMessage({ type: 'error', text: 'Địa chỉ không được để trống.' });
      return;
    }
    if (!area || parseFloat(area) <= 0) {
      setFarmMessage({ type: 'error', text: 'Diện tích phải lớn hơn 0.' });
      return;
    }

    const payload = {
      name: name.trim(),
      address: address.trim(),
      area: parseFloat(area),
      gpsLat: gpsLat ? parseFloat(gpsLat) : null,
      gpsLng: gpsLng ? parseFloat(gpsLng) : null,
      description: description.trim(),
      productTypes: productTypes.trim(),
    };

    setSaving(true);
    try {
      // The backend owns the create-vs-update decision based on the current
      // authenticated user.  A single endpoint avoids relying on stale farm
      // state in the browser when a farm has just been created or reloaded.
      const res = await fetch(`${API_BASE_URL}/farms/upsert`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload),
      });

      if (res.ok) {
        const saved: FarmData = await res.json();
        setFarm(saved);
        setFarmMessage({
          type: 'success',
          text: farm?.id
            ? 'Cập nhật thông tin nông trại thành công! Trạng thái đã chuyển về PENDING để Admin xét duyệt.'
            : 'Tạo thông tin nông trại thành công! Hồ sơ đang chờ Admin phê duyệt.',
        });
        // Load certifications nếu vừa tạo mới (lần đầu có farmId)
        if (!farm?.id && saved.id) {
          fetchCertifications(saved.id);
        }
      } else {
        if (res.status === 401 || res.status === 403) {
          logout();
          setFarmMessage({
            type: 'error',
            text: 'Phiên đăng nhập đã hết hiệu lực. Đang chuyển về trang đăng nhập để bạn nhận phiên mới.',
          });
          window.setTimeout(() => window.location.reload(), 900);
          return;
        }
        const errorData = await res.json().catch(() => ({}));
        const validationMessage = Array.isArray(errorData.details) ? errorData.details.join('. ') : null;
        setFarmMessage({
          type: 'error',
          text: validationMessage || errorData.message || `Lưu thất bại (mã ${res.status}). Vui lòng kiểm tra lại thông tin.`,
        });
      }
    } catch {
      setFarmMessage({ type: 'error', text: 'Lỗi kết nối. Vui lòng thử lại sau.' });
    } finally {
      setSaving(false);
    }
  };

  const handleResetFarm = () => {
    if (farm) {
      setName(farm.name || '');
      setAddress(farm.address || '');
      setArea(farm.area != null ? String(farm.area) : '');
      setGpsLat(farm.gpsLat != null ? String(farm.gpsLat) : '');
      setGpsLng(farm.gpsLng != null ? String(farm.gpsLng) : '');
      setDescription(farm.description || '');
      setProductTypes(farm.productTypes || '');
    } else {
      setName(''); setAddress(''); setArea('');
      setGpsLat(''); setGpsLng(''); setDescription(''); setProductTypes('');
    }
    setFarmMessage(null);
  };


  // ── Certification handlers ────────────────────────────────────────────────────

  const handleAddCertification = async (e: React.FormEvent) => {
    e.preventDefault();
    setCertMessage(null);

    if (!farm?.id) {
      setCertMessage({ type: 'error', text: 'Vui lòng lưu thông tin nông trại trước khi thêm chứng nhận.' });
      return;
    }
    if (!certType.trim() || !certFileUrl.trim() || !certExpiryDate) {
      setCertMessage({ type: 'error', text: 'Vui lòng điền đầy đủ thông tin chứng nhận.' });
      return;
    }
    if (new Date(certExpiryDate) < new Date()) {
      setCertMessage({ type: 'error', text: 'Ngày hết hạn không được là ngày trong quá khứ.' });
      return;
    }

    setAddingCert(true);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/${farm.id}/documents`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          type: certType.trim(),
          fileUrl: certFileUrl.trim(),
          expiryDate: certExpiryDate,
        }),
      });

      if (res.ok) {
        const newCert: Certification = await res.json();
        setCertifications((prev) => [...prev, newCert]);
        setCertType(''); setCertFileUrl(''); setCertExpiryDate('');
        setShowAddCert(false);
        setCertMessage({
          type: 'success',
          text: 'Thêm chứng nhận thành công! Trạng thái nông trại đã chuyển về PENDING để Admin xét duyệt.',
        });
        fetchFarm(); // Refresh status
      } else {
        const errorData = await res.json().catch(() => ({}));
        setCertMessage({ type: 'error', text: errorData.message || 'Thêm chứng nhận thất bại.' });
      }
    } catch {
      setCertMessage({ type: 'error', text: 'Lỗi kết nối. Vui lòng thử lại sau.' });
    } finally {
      setAddingCert(false);
    }
  };

  const handleDeleteCertification = async (certId: number) => {
    if (!farm?.id) return;
    if (!window.confirm('Bạn có chắc muốn xóa chứng nhận này không?')) return;

    setDeletingCertId(certId);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/${farm.id}/certifications/${certId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });

      if (res.ok || res.status === 204) {
        setCertifications((prev) => prev.filter((c) => c.id !== certId));
        setCertMessage({ type: 'success', text: 'Đã xóa chứng nhận thành công.' });
      } else {
        const errorData = await res.json().catch(() => ({}));
        setCertMessage({ type: 'error', text: errorData.message || 'Xóa chứng nhận thất bại.' });
      }
    } catch {
      setCertMessage({ type: 'error', text: 'Lỗi kết nối. Vui lòng thử lại sau.' });
    } finally {
      setDeletingCertId(null);
    }
  };

  // ── Helpers ──────────────────────────────────────────────────────────────────

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleDateString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  const getStatusBadge = (status: string) => {
    const map: Record<string, { label: string; color: string; bg: string; border: string }> = {
      PENDING: { label: 'Chờ duyệt', color: '#fbbf24', bg: 'rgba(251,191,36,0.1)', border: 'rgba(251,191,36,0.3)' },
      APPROVED: { label: 'Đã duyệt', color: '#34d399', bg: 'rgba(52,211,153,0.1)', border: 'rgba(52,211,153,0.3)' },
      REJECTED: { label: 'Bị từ chối', color: '#f87171', bg: 'rgba(248,113,113,0.1)', border: 'rgba(248,113,113,0.3)' },
      SUSPENDED: { label: 'Tạm ngưng', color: '#f87171', bg: 'rgba(248,113,113,0.1)', border: 'rgba(248,113,113,0.3)' },
      INACTIVE: { label: 'Không hoạt động', color: '#94a3b8', bg: 'rgba(148,163,184,0.1)', border: 'rgba(148,163,184,0.3)' },
    };
    const s = map[status] || { label: status, color: '#94a3b8', bg: 'rgba(148,163,184,0.1)', border: 'rgba(148,163,184,0.3)' };
    return (
      <span style={{
        fontSize: '12px', fontWeight: 600, padding: '4px 12px', borderRadius: '20px',
        color: s.color, background: s.bg, border: `1px solid ${s.border}`,
      }}>
        {s.label}
      </span>
    );
  };

  const certTypeOptions = [
    'BUSINESS_LICENSE', 'VietGAP', 'GlobalGAP', 'Organic', 'HACCP', 'ISO 22000', 'Khác',
  ];


  // ── Render ───────────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '300px' }}>
        <div style={{ color: '#10b981', fontSize: '16px', fontWeight: 600 }}>⏳ Đang tải thông tin...</div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      {/* Page Header */}
      <div style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
          <div style={{ flex: 1 }}>
            <h1 className="dashboard-title" style={{ fontSize: '26px' }}>
              {farm ? 'Cập Nhật Thông Tin Nông Trại' : 'Đăng Ký Thông Tin Nông Trại'}
            </h1>
            <p className="dashboard-subtitle">
              {farm
                ? 'Cập nhật thông tin nông trại và giấy phép kinh doanh (BICAP-9).'
                : 'Nhập thông tin nông trại lần đầu tiên để bắt đầu (BICAP-9).'}
            </p>
          </div>
          {farm && getStatusBadge(farm.status)}
        </div>
      </div>

      {/* Message */}
      {farmMessage && (
        <div style={{
          padding: '14px 20px', borderRadius: '12px', marginBottom: '24px',
          fontSize: '14px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '10px',
          background: farmMessage.type === 'success' ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)',
          border: farmMessage.type === 'success' ? '1px solid rgba(16,185,129,0.3)' : '1px solid rgba(239,68,68,0.3)',
          color: farmMessage.type === 'success' ? '#34d399' : '#f87171',
        }}>
          <span>{farmMessage.type === 'success' ? '✅' : '⚠️'}</span>
          <span>{farmMessage.text}</span>
        </div>
      )}

      {/* Farm Info Form */}
      <form onSubmit={handleSaveFarm}>
        {/* Basic Info */}
        <div className="glass-panel" style={{ padding: '24px', borderRadius: '16px', marginBottom: '24px' }}>
          <h2 style={sectionHeaderStyle}><span>🌾</span> Thông tin cơ bản nông trại</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
            <div>
              <label style={labelStyle}>Tên nông trại <span style={{ color: '#ef4444' }}>*</span></label>
              <input
                type="text" required placeholder="Nhập tên nông trại"
                value={name} onChange={(e) => setName(e.target.value)} style={inputStyle}
              />
            </div>
            <div>
              <label style={labelStyle}>Diện tích (ha) <span style={{ color: '#ef4444' }}>*</span></label>
              <input
                type="number" required min="0.01" step="0.01" placeholder="Ví dụ: 2.5"
                value={area} onChange={(e) => setArea(e.target.value)} style={inputStyle}
              />
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={labelStyle}>Địa chỉ nông trại <span style={{ color: '#ef4444' }}>*</span></label>
              <input
                type="text" required placeholder="Địa chỉ đầy đủ của nông trại"
                value={address} onChange={(e) => setAddress(e.target.value)} style={inputStyle}
              />
            </div>
            <div>
              <label style={labelStyle}>Loại cây trồng / Vật nuôi</label>
              <input
                type="text" placeholder="Ví dụ: rau, củ, quả, lúa"
                value={productTypes} onChange={(e) => setProductTypes(e.target.value)} style={inputStyle}
              />
              <p style={{ fontSize: '11px', color: '#64748b', marginTop: '4px' }}>
                Nhập nhiều loại, phân cách bằng dấu phẩy
              </p>
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={labelStyle}>Mô tả nông trại</label>
              <textarea
                rows={3} placeholder="Mô tả thêm về nông trại, quy trình sản xuất..."
                value={description} onChange={(e) => setDescription(e.target.value)}
                style={{ ...inputStyle, resize: 'vertical' }}
              />
            </div>
          </div>
        </div>

        {/* GPS */}
        <div className="glass-panel" style={{ padding: '24px', borderRadius: '16px', marginBottom: '24px' }}>
          <h2 style={sectionHeaderStyle}><span>📍</span> Tọa độ GPS (tùy chọn)</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
            <div>
              <label style={labelStyle}>Vĩ độ (Latitude)</label>
              <input
                type="number" step="any" placeholder="Ví dụ: 10.762622"
                value={gpsLat} onChange={(e) => setGpsLat(e.target.value)} style={inputStyle}
              />
            </div>
            <div>
              <label style={labelStyle}>Kinh độ (Longitude)</label>
              <input
                type="number" step="any" placeholder="Ví dụ: 106.660172"
                value={gpsLng} onChange={(e) => setGpsLng(e.target.value)} style={inputStyle}
              />
            </div>
          </div>
        </div>


        {/* Read-only (chỉ hiển thị nếu farm đã tồn tại) */}
        {farm && (
          <div className="glass-panel" style={{ padding: '24px', borderRadius: '16px', marginBottom: '24px', opacity: 0.85 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 style={{ ...sectionHeaderStyle, margin: 0 }}><span>🔒</span> Thông tin hệ thống</h2>
              <span style={readOnlyBadgeStyle}>🔒 Chỉ đọc</span>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px' }}>
              <div>
                <label style={labelStyle}>Trạng thái</label>
                <div style={{ marginTop: '4px' }}>{getStatusBadge(farm.status)}</div>
              </div>
              <div>
                <label style={labelStyle}>Ngày đăng ký</label>
                <div style={{ position: 'relative' }}>
                  <input type="text" value={formatDate(farm.createdAt)} disabled style={readOnlyInputStyle} />
                  <span style={lockIconStyle}>🔒</span>
                </div>
              </div>
              <div>
                <label style={labelStyle}>Cập nhật lần cuối</label>
                <div style={{ position: 'relative' }}>
                  <input type="text" value={formatDate(farm.updatedAt)} disabled style={readOnlyInputStyle} />
                  <span style={lockIconStyle}>🔒</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Action buttons */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px', marginBottom: '40px' }}>
          <button type="button" onClick={handleResetFarm} disabled={saving} style={secondaryButtonStyle}>
            🔄 Hoàn tác
          </button>
          <button type="submit" disabled={saving} style={primaryButtonStyle}>
            {saving ? '⏳ Đang lưu...' : (farm ? '💾 Cập nhật thông tin' : '✅ Tạo nông trại')}
          </button>
        </div>
      </form>

      {/* ── Certifications (chỉ hiển thị nếu đã có farm) ── */}
      {farm && (
        <div className="glass-panel" style={{ padding: '24px', borderRadius: '16px', marginBottom: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
            <h2 style={{ ...sectionHeaderStyle, margin: 0 }}>
              <span>📜</span> Giấy phép kinh doanh & Chứng nhận
            </h2>
            <button
              type="button"
              onClick={() => { setShowAddCert(!showAddCert); setCertMessage(null); }}
              style={{
                padding: '8px 18px', borderRadius: '8px', border: '1px solid rgba(16,185,129,0.4)',
                background: 'rgba(16,185,129,0.1)', color: '#34d399', fontSize: '13px',
                fontWeight: 600, cursor: 'pointer',
              }}
            >
              {showAddCert ? '✖ Hủy' : '＋ Thêm giấy phép / chứng nhận'}
            </button>
          </div>

          {certMessage && (
            <div style={{
              padding: '12px 18px', borderRadius: '10px', marginBottom: '20px',
              fontSize: '13px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '8px',
              background: certMessage.type === 'success' ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.12)',
              border: certMessage.type === 'success' ? '1px solid rgba(16,185,129,0.3)' : '1px solid rgba(239,68,68,0.3)',
              color: certMessage.type === 'success' ? '#34d399' : '#f87171',
            }}>
              <span>{certMessage.type === 'success' ? '✅' : '⚠️'}</span>
              <span>{certMessage.text}</span>
            </div>
          )}


          {/* Add certification form */}
          {showAddCert && (
            <form onSubmit={handleAddCertification} style={{
              background: 'rgba(16,185,129,0.05)', border: '1px solid rgba(16,185,129,0.15)',
              borderRadius: '12px', padding: '20px', marginBottom: '24px',
            }}>
              <h3 style={{ fontSize: '14px', fontWeight: 700, color: '#34d399', marginBottom: '16px' }}>
                ➕ Thêm giấy phép / chứng nhận mới
              </h3>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
                <div>
                  <label style={labelStyle}>Loại giấy tờ <span style={{ color: '#ef4444' }}>*</span></label>
                  <select
                    required value={certType} onChange={(e) => setCertType(e.target.value)}
                    style={{ ...inputStyle, cursor: 'pointer' }}
                  >
                    <option value="">-- Chọn loại giấy tờ --</option>
                    {certTypeOptions.map((opt) => (
                      <option key={opt} value={opt}>{opt}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={labelStyle}>Ngày hết hạn <span style={{ color: '#ef4444' }}>*</span></label>
                  <input
                    type="date" required value={certExpiryDate}
                    min={new Date().toISOString().split('T')[0]}
                    onChange={(e) => setCertExpiryDate(e.target.value)} style={inputStyle}
                  />
                </div>
                <div style={{ gridColumn: '1 / -1' }}>
                  <label style={labelStyle}>URL file giấy phép <span style={{ color: '#ef4444' }}>*</span></label>
                  <input
                    type="url" required placeholder="https://example.com/giay-phep.pdf"
                    value={certFileUrl} onChange={(e) => setCertFileUrl(e.target.value)} style={inputStyle}
                  />
                  <p style={{ fontSize: '11px', color: '#64748b', marginTop: '4px' }}>
                    Nhập URL trỏ đến file PDF hoặc ảnh (Google Drive, Dropbox, CDN...)
                  </p>
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={() => { setShowAddCert(false); setCertType(''); setCertFileUrl(''); setCertExpiryDate(''); }}
                  style={secondaryButtonStyle}
                >
                  Hủy
                </button>
                <button type="submit" disabled={addingCert} style={primaryButtonStyle}>
                  {addingCert ? '⏳ Đang thêm...' : '✅ Xác nhận thêm'}
                </button>
              </div>
            </form>
          )}


          {/* Certifications list */}
          {certLoading ? (
            <div style={{ textAlign: 'center', padding: '24px', color: '#64748b', fontSize: '14px' }}>
              ⏳ Đang tải danh sách chứng nhận...
            </div>
          ) : certifications.length === 0 ? (
            <div style={{
              textAlign: 'center', padding: '32px',
              border: '1px dashed rgba(255,255,255,0.1)', borderRadius: '12px',
              color: '#64748b', fontSize: '14px',
            }}>
              <div style={{ fontSize: '32px', marginBottom: '8px' }}>📄</div>
              <p>Chưa có giấy phép hoặc chứng nhận nào.</p>
              <p style={{ fontSize: '12px', marginTop: '4px' }}>
                Nhấn "Thêm giấy phép / chứng nhận" để tải lên giấy phép kinh doanh.
              </p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {certifications.map((cert) => (
                <div key={cert.id} style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '16px 20px', borderRadius: '10px', flexWrap: 'wrap', gap: '12px',
                  background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.07)',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: 1, minWidth: 0 }}>
                    <div style={{
                      width: '36px', height: '36px', borderRadius: '8px', flexShrink: 0,
                      background: 'rgba(16,185,129,0.15)', border: '1px solid rgba(16,185,129,0.3)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '16px',
                    }}>
                      📜
                    </div>
                    <div style={{ minWidth: 0 }}>
                      <div style={{ fontSize: '13px', fontWeight: 700, color: '#fff', marginBottom: '2px' }}>
                        {cert.type}
                      </div>
                      <div style={{ fontSize: '11px', color: '#64748b' }}>
                        Hết hạn: {formatDate(cert.expiryDate)}
                      </div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
                    <a
                      href={cert.fileUrl} target="_blank" rel="noopener noreferrer"
                      style={{
                        fontSize: '12px', fontWeight: 600, color: '#38bdf8', textDecoration: 'none',
                        padding: '5px 12px', borderRadius: '6px',
                        border: '1px solid rgba(56,189,248,0.3)', background: 'rgba(56,189,248,0.08)',
                      }}
                    >
                      🔗 Xem file
                    </a>
                    <button
                      type="button"
                      disabled={deletingCertId === cert.id}
                      onClick={() => handleDeleteCertification(cert.id)}
                      style={{
                        fontSize: '12px', fontWeight: 600, color: '#f87171', cursor: 'pointer',
                        padding: '5px 12px', borderRadius: '6px',
                        border: '1px solid rgba(248,113,113,0.3)', background: 'rgba(248,113,113,0.08)',
                      }}
                    >
                      {deletingCertId === cert.id ? '⏳' : '🗑️ Xóa'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ── Inline Styles ─────────────────────────────────────────────────────────────

const sectionHeaderStyle: React.CSSProperties = {
  fontSize: '17px', fontWeight: 700, color: '#fff',
  marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px',
};

const labelStyle: React.CSSProperties = {
  display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px',
};

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '11px 16px', borderRadius: '10px',
  border: '1px solid rgba(255,255,255,0.12)', background: 'rgba(15,23,42,0.6)',
  color: '#fff', fontSize: '14px', outline: 'none',
  boxSizing: 'border-box', transition: 'border-color 0.2s ease',
};

const readOnlyInputStyle: React.CSSProperties = {
  width: '100%', padding: '11px 16px 11px 38px', borderRadius: '10px',
  border: '1px solid rgba(255,255,255,0.05)', background: 'rgba(255,255,255,0.03)',
  color: '#94a3b8', fontSize: '14px', cursor: 'not-allowed', boxSizing: 'border-box',
};

const lockIconStyle: React.CSSProperties = {
  position: 'absolute', left: '12px', top: '50%',
  transform: 'translateY(-50%)', fontSize: '14px', opacity: 0.6,
};

const readOnlyBadgeStyle: React.CSSProperties = {
  fontSize: '11px', background: 'rgba(239,68,68,0.1)', color: '#f87171',
  padding: '4px 10px', borderRadius: '20px',
  border: '1px solid rgba(239,68,68,0.2)', fontWeight: 500,
};

const primaryButtonStyle: React.CSSProperties = {
  padding: '12px 32px', borderRadius: '10px', border: 'none',
  background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
  color: '#fff', fontSize: '14px', fontWeight: 700, cursor: 'pointer',
  boxShadow: '0 4px 14px rgba(16,185,129,0.4)', transition: 'all 0.2s ease',
};

const secondaryButtonStyle: React.CSSProperties = {
  padding: '12px 24px', borderRadius: '10px',
  border: '1px solid rgba(255,255,255,0.15)',
  background: 'rgba(255,255,255,0.05)', color: '#cbd5e1',
  fontSize: '14px', fontWeight: 600, cursor: 'pointer', transition: 'all 0.2s ease',
};
