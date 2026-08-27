import React, { useEffect, useMemo, useState } from 'react';
import { API_BASE_URL, getAuthHeaders, getAuthHeadersForUpload } from '../../utils/auth';

type Farm = {
  id: number;
  name: string;
  address: string;
  area: number;
  gpsLat?: number;
  gpsLng?: number;
  description?: string;
  productTypes: string;
  status: string;
};

type FarmForm = {
  name: string;
  address: string;
  area: string;
  gpsLat: string;
  gpsLng: string;
  description: string;
  productTypes: string;
};

const emptyForm: FarmForm = {
  name: '', address: '', area: '', gpsLat: '', gpsLng: '', description: '', productTypes: '',
};

const inputStyle: React.CSSProperties = {
  width: '100%', padding: 11, marginTop: 6, borderRadius: 8,
  border: '1px solid rgba(255,255,255,.15)', background: 'rgba(15,23,42,.6)',
  color: '#fff', boxSizing: 'border-box',
};

export default function FarmInfoPage() {
  const [farms, setFarms] = useState<Farm[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] = useState<FarmForm>(emptyForm);
  const [license, setLicense] = useState<File | null>(null);
  const [certifications, setCertifications] = useState<File[]>([]);
  const [editing, setEditing] = useState(false);
  const [adding, setAdding] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ text: string; ok: boolean } | null>(null);

  const farm = useMemo(() => farms.find(item => item.id === selectedId) || null, [farms, selectedId]);

  const load = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/farms/my`, { headers: getAuthHeaders() });
      const data = await response.json().catch(() => []);
      if (!response.ok) throw new Error(data.message || 'Không thể tải thông tin nông trại.');
      const values = Array.isArray(data) ? data : [];
      setFarms(values);
      if (values.length > 0) {
        const next = values.find((item: Farm) => item.id === selectedId) || values[0];
        selectFarm(next);
      }
    } catch (error) {
      setMessage({ text: error instanceof Error ? error.message : 'Không thể tải thông tin nông trại.', ok: false });
    }
  };

  useEffect(() => { load(); }, []);

  const selectFarm = (value: Farm) => {
    setSelectedId(value.id);
    setForm({
      name: value.name || '', address: value.address || '', area: String(value.area ?? ''),
      gpsLat: value.gpsLat == null ? '' : String(value.gpsLat),
      gpsLng: value.gpsLng == null ? '' : String(value.gpsLng),
      description: value.description || '', productTypes: value.productTypes || '',
    });
    setEditing(false);
    setAdding(false);
  };

  const setField = (key: keyof FarmForm, value: string) => setForm(old => ({ ...old, [key]: value }));

  const validate = () => {
    const area = Number(form.area);
    const lat = form.gpsLat === '' ? null : Number(form.gpsLat);
    const lng = form.gpsLng === '' ? null : Number(form.gpsLng);
    if (!form.name.trim() || !form.address.trim() || !form.productTypes.trim()) return 'Vui lòng nhập tên, địa chỉ và loại cây trồng/vật nuôi.';
    if (!Number.isFinite(area) || area <= 0) return 'Diện tích phải lớn hơn 0 ha.';
    if (lat !== null && (!Number.isFinite(lat) || lat < -90 || lat > 90)) return 'Vĩ độ GPS phải từ -90 đến 90.';
    if (lng !== null && (!Number.isFinite(lng) || lng < -180 || lng > 180)) return 'Kinh độ GPS phải từ -180 đến 180.';
    return null;
  };

  const payload = () => ({
    ...form,
    name: form.name.trim(), address: form.address.trim(), productTypes: form.productTypes.trim(),
    description: form.description.trim() || null, area: Number(form.area),
    gpsLat: form.gpsLat === '' ? null : Number(form.gpsLat), gpsLng: form.gpsLng === '' ? null : Number(form.gpsLng),
  });

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) { setMessage({ text: validationError, ok: false }); return; }
    if (adding && !license) { setMessage({ text: 'Giấy phép kinh doanh là bắt buộc (PDF/JPG/PNG, tối đa 10MB).', ok: false }); return; }
    setSaving(true); setMessage(null);
    try {
      let response: Response;
      if (adding) {
        const multipart = new FormData();
        multipart.append('farm', new Blob([JSON.stringify(payload())], { type: 'application/json' }));
        multipart.append('businessLicense', license as File);
        certifications.forEach(file => multipart.append('certifications', file));
        // Không set Content-Type thủ công khi dùng FormData — browser tự set multipart/form-data với boundary
        response = await fetch(`${API_BASE_URL}/farms/register`, { method: 'POST', headers: getAuthHeadersForUpload(), body: multipart });
      } else {
        response = await fetch(`${API_BASE_URL}/farms/my/${selectedId}`, {
          method: 'PUT', headers: getAuthHeaders(), body: JSON.stringify(payload()),
        });
      }
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || 'Không thể lưu thông tin nông trại.');
      if (adding) {
        setFarms(old => [...old, data]);
        selectFarm(data);
        setMessage({ text: 'Đã thêm nông trại. Hồ sơ đang chờ Admin phê duyệt.', ok: true });
      } else {
        setFarms(old => old.map(item => item.id === data.id ? data : item));
        setEditing(false);
        setMessage({ text: 'Đã cập nhật nông trại. Hồ sơ đang chờ Admin phê duyệt.', ok: true });
      }
      setLicense(null); setCertifications([]);
    } catch (error) {
      setMessage({ text: error instanceof Error ? error.message : 'Không thể lưu thông tin nông trại.', ok: false });
    } finally { setSaving(false); }
  };

  const startAdd = () => { setSelectedId(null); setForm(emptyForm); setLicense(null); setAdding(true); setEditing(false); setMessage(null); };
  const cancelForm = () => { if (farm) selectFarm(farm); else { setAdding(false); setForm(emptyForm); } setMessage(null); };

  return (
    <div style={{ maxWidth: 1000 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, marginBottom: 20 }}>
        <div><h1 className="dashboard-title">Nông Trại Của Tôi</h1><p className="dashboard-subtitle">Quản lý hồ sơ nông trại của chính bạn.</p></div>
        <button type="button" onClick={startAdd}>+ Thêm nông trại</button>
      </div>
      {message && <p style={{ color: message.ok ? '#34d399' : '#f87171' }}>{message.text}</p>}
      {farms.length > 1 && !adding && <label style={{ display: 'block', marginBottom: 16 }}>Chọn nông trại<select value={selectedId ?? ''} onChange={event => { const value = farms.find(item => item.id === Number(event.target.value)); if (value) selectFarm(value); }} style={inputStyle}>{farms.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>}
      {(adding || editing) ? (
        <form className="glass-panel" onSubmit={submit} style={{ padding: 24, display: 'grid', gap: 16 }}>
          <h2>{adding ? 'Thêm nông trại' : 'Cập nhật thông tin nông trại'}</h2>
          <label>Tên nông trại<input required maxLength={255} value={form.name} onChange={event => setField('name', event.target.value)} style={inputStyle} /></label>
          <label>Địa chỉ nông trại<input required maxLength={500} value={form.address} onChange={event => setField('address', event.target.value)} style={inputStyle} /></label>
          <label>Diện tích (ha)<input required min="0.01" step="0.01" type="number" value={form.area} onChange={event => setField('area', event.target.value)} style={inputStyle} /></label>
          <label>Loại cây trồng/vật nuôi<input required maxLength={500} placeholder="Ví dụ: Rau ăn lá, lúa hữu cơ" value={form.productTypes} onChange={event => setField('productTypes', event.target.value)} style={inputStyle} /></label>
          <label>Mô tả nông trại<textarea maxLength={2000} value={form.description} onChange={event => setField('description', event.target.value)} style={inputStyle} /></label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}><label>GPS vĩ độ<input min="-90" max="90" step="any" type="number" value={form.gpsLat} onChange={event => setField('gpsLat', event.target.value)} style={inputStyle} /></label><label>GPS kinh độ<input min="-180" max="180" step="any" type="number" value={form.gpsLng} onChange={event => setField('gpsLng', event.target.value)} style={inputStyle} /></label></div>
          {adding && <label>Giấy phép kinh doanh <small>(bắt buộc: PDF/JPG/PNG, tối đa 10MB)</small><input required type="file" accept="application/pdf,image/jpeg,image/png" onChange={event => setLicense(event.target.files?.[0] || null)} style={inputStyle} /></label>}
          {adding && <label>Chứng nhận (không bắt buộc) <small>(PDF/JPG/PNG, mỗi file tối đa 10MB)</small><input multiple type="file" accept="application/pdf,image/jpeg,image/png" onChange={event => setCertifications(Array.from(event.target.files || []))} style={inputStyle} /></label>}
          <div style={{ display: 'flex', gap: 12 }}><button disabled={saving} type="submit">{saving ? 'Đang lưu...' : 'Lưu thông tin'}</button><button type="button" onClick={cancelForm}>Hủy</button></div>
        </form>
      ) : farm ? (
        <div className="glass-panel" style={{ padding: 24 }}><div style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}><h2>{farm.name}</h2><button type="button" onClick={() => setEditing(true)}>Cập nhật</button></div><dl style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(220px,1fr))', gap: 16 }}><div><dt>Địa chỉ</dt><dd>{farm.address}</dd></div><div><dt>Diện tích</dt><dd>{farm.area} ha</dd></div><div><dt>Loại sản xuất</dt><dd>{farm.productTypes}</dd></div><div><dt>Trạng thái</dt><dd>{farm.status}</dd></div><div><dt>GPS</dt><dd>{farm.gpsLat ?? '—'}, {farm.gpsLng ?? '—'}</dd></div><div><dt>Mô tả</dt><dd>{farm.description || '—'}</dd></div></dl></div>
      ) : (
        <div className="glass-panel" style={{ padding: 32 }}><h2>Chưa có nông trại</h2><p>Hãy thêm nông trại đầu tiên của bạn để bắt đầu quản lý.</p><button type="button" onClick={startAdd}>Thêm nông trại</button></div>
      )}
    </div>
  );
}
