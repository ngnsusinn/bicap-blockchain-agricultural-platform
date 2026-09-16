import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  gridStyle, panelStyle, titleStyle, labelStyle, inputStyle, buttonStyle,
  alertStyle, successStyle, badgeStyle,
} from './ui';

type Farm = {
  id: number; name: string; address: string; area: number; gpsLat?: number; gpsLng?: number;
  description?: string; productTypes?: string; status: string; updatedAt?: string;
};

interface FarmInfoProps {
  farmId?: number;
  onFarmRegistered?: (farm: Farm) => void;
}

export default function FarmInfo({ farmId, onFarmRegistered }: FarmInfoProps) {
  const [farm, setFarm] = useState<Farm | null>(null);
  const [registeredFarmId, setRegisteredFarmId] = useState<number>();
  const [form, setForm] = useState({ name: '', address: '', area: '', gpsLat: '', gpsLng: '', description: '', productTypes: '' });
  const [cert, setCert] = useState({ type: 'BUSINESS_LICENSE', expiryDate: '', file: null as File | null });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const effectiveFarmId = farmId ?? registeredFarmId;

  const load = async () => {
    if (!farmId) return;
    const res = await fetch(`${API_BASE_URL}/farms/${farmId}`, { headers: getAuthHeaders() });
    if (!res.ok) { setError('Không tải được thông tin nông trại.'); return; }
    const f: Farm = await res.json();
    setFarm(f);
    setForm({
      name: f.name || '', address: f.address || '', area: String(f.area ?? ''),
      gpsLat: f.gpsLat != null ? String(f.gpsLat) : '', gpsLng: f.gpsLng != null ? String(f.gpsLng) : '',
      description: f.description || '', productTypes: f.productTypes || '',
    });
  };

  useEffect(() => { load().catch(() => setError('Lỗi kết nối máy chủ.')); }, [farmId]);

  const register = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setNotice(''); setBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/register`, {
        method: 'POST', headers: getAuthHeaders(),
        body: JSON.stringify({
          name: form.name, address: form.address, area: Number(form.area),
          gpsLat: form.gpsLat ? Number(form.gpsLat) : null,
          gpsLng: form.gpsLng ? Number(form.gpsLng) : null,
          description: form.description, productTypes: form.productTypes,
        }),
      });
      if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || 'Đăng ký nông trại thất bại.'); }
      const created: Farm = await res.json();
      setFarm(created);
      setRegisteredFarmId(created.id);
      setNotice('Đã gửi hồ sơ đăng ký. Vui lòng chờ admin phê duyệt trước khi thực hiện các tác vụ tiếp theo.');
      onFarmRegistered?.(created);
    } catch (err) { setError(err instanceof Error ? err.message : 'Đăng ký nông trại thất bại.'); }
    finally { setBusy(false); }
  };

  const save = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setNotice('');
    if (!effectiveFarmId) return;
    setBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/${effectiveFarmId}`, {
        method: 'PUT', headers: getAuthHeaders(),
        body: JSON.stringify({
          name: form.name, address: form.address, area: Number(form.area),
          gpsLat: form.gpsLat ? Number(form.gpsLat) : null,
          gpsLng: form.gpsLng ? Number(form.gpsLng) : null,
          description: form.description, productTypes: form.productTypes,
        }),
      });
      if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || 'Cập nhật thất bại.'); }
      setNotice('Đã cập nhật thông tin nông trại.');
      await load();
    } catch (err) { setError(err instanceof Error ? err.message : 'Cập nhật thất bại.'); }
    finally { setBusy(false); }
  };

  const uploadCert = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setNotice('');
    if (!effectiveFarmId || !cert.file) { setError('Chọn tệp giấy phép/chứng nhận để tải lên.'); return; }
    setBusy(true);
    try {
      const fd = new FormData();
      fd.append('file', cert.file);
      if (cert.type) fd.append('type', cert.type);
      if (cert.expiryDate) fd.append('expiryDate', cert.expiryDate);
      const token = localStorage.getItem('accessToken');
      const res = await fetch(`${API_BASE_URL}/farms/${effectiveFarmId}/certifications`, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: fd,
      });
      if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || 'Tải tài liệu thất bại.'); }
      setNotice('Đã tải lên giấy phép / chứng nhận.');
      setCert({ type: 'BUSINESS_LICENSE', expiryDate: '', file: null });
    } catch (err) { setError(err instanceof Error ? err.message : 'Tải tài liệu thất bại.'); }
    finally { setBusy(false); }
  };

  return (
    <div>
      <h1 className="dashboard-title">Thông tin nông trại</h1>
      <p className="dashboard-subtitle">
        {effectiveFarmId ? 'Cập nhật hồ sơ nông trại, vùng canh tác và giấy phép kinh doanh.' : 'Đăng ký hồ sơ nông trại để admin xem xét và phê duyệt.'}
      </p>
      {error && <div style={alertStyle}>{error}</div>}
      {notice && <div style={successStyle}>{notice}</div>}

      {!effectiveFarmId ? (
        <form className="glass-panel" style={panelStyle} onSubmit={register}>
          <h2 style={titleStyle}>Đăng ký nông trại</h2>
          <p style={{ color: '#94a3b8', fontSize: 13 }}>Hồ sơ sẽ được chuyển đến admin ở trạng thái chờ duyệt.</p>
          <label style={labelStyle}>Tên nông trại</label>
          <input required maxLength={255} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Địa chỉ</label>
          <input required maxLength={500} value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Diện tích (m²)</label>
          <input required min="0.01" step="0.01" type="number" value={form.area} onChange={e => setForm({ ...form, area: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Loại nông sản</label>
          <input maxLength={500} value={form.productTypes} onChange={e => setForm({ ...form, productTypes: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Mô tả</label>
          <textarea rows={3} maxLength={2000} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} style={inputStyle} />
          <button disabled={busy} style={buttonStyle}>{busy ? 'Đang gửi…' : 'Gửi đăng ký'}</button>
        </form>
      ) : <div style={gridStyle}>
        <form className="glass-panel" style={panelStyle} onSubmit={save}>
          <h2 style={titleStyle}>Hồ sơ nông trại</h2>
          {farm && <div style={{ marginBottom: 8 }}><span style={badgeStyle(farm.status)}>{farm.status}</span></div>}
          <label style={labelStyle}>Tên nông trại</label>
          <input required maxLength={255} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Địa chỉ</label>
          <input required maxLength={500} value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Diện tích (m²)</label>
          <input required min="0.01" step="0.01" type="number" value={form.area} onChange={e => setForm({ ...form, area: e.target.value })} style={inputStyle} />
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div>
              <label style={labelStyle}>Vĩ độ (GPS)</label>
              <input type="number" step="any" value={form.gpsLat} onChange={e => setForm({ ...form, gpsLat: e.target.value })} style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>Kinh độ (GPS)</label>
              <input type="number" step="any" value={form.gpsLng} onChange={e => setForm({ ...form, gpsLng: e.target.value })} style={inputStyle} />
            </div>
          </div>
          <label style={labelStyle}>Loại nông sản</label>
          <input maxLength={500} value={form.productTypes} onChange={e => setForm({ ...form, productTypes: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Mô tả</label>
          <textarea rows={3} maxLength={2000} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} style={inputStyle} />
          <button disabled={busy} style={buttonStyle}>{busy ? 'Đang lưu…' : 'Lưu thông tin'}</button>
        </form>

        <form className="glass-panel" style={panelStyle} onSubmit={uploadCert}>
          <h2 style={titleStyle}>Giấy phép & chứng nhận</h2>
          <p style={{ color: '#94a3b8', fontSize: 13 }}>Tải giấy phép kinh doanh hoặc chứng nhận VietGAP/GlobalGAP. Admin sẽ xem xét khi phê duyệt).</p>
          <label style={labelStyle}>Loại tài liệu</label>
          <select value={cert.type} onChange={e => setCert({ ...cert, type: e.target.value })} style={inputStyle}>
            <option value="BUSINESS_LICENSE">Giấy phép kinh doanh</option>
            <option value="VIETGAP">Chứng nhận VietGAP</option>
            <option value="GLOBALGAP">Chứng nhận GlobalGAP</option>
            <option value="OTHER">Khác</option>
          </select>
          <label style={labelStyle}>Ngày hết hạn</label>
          <input type="date" value={cert.expiryDate} onChange={e => setCert({ ...cert, expiryDate: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Tệp tài liệu (ảnh / PDF)</label>
          <input type="file" accept="image/*,application/pdf" onChange={e => setCert({ ...cert, file: e.target.files?.[0] || null })} style={{ ...inputStyle, padding: 8 }} />
          <button disabled={busy} style={buttonStyle}>{busy ? 'Đang tải…' : 'Tải lên tài liệu'}</button>
        </form>
      </div>}
    </div>
  );
}
