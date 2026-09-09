/**
 * BICAP-58 — Shipping: Quản lý phương tiện (CRUD).
 *
 * Shipping Manager thêm, sửa, xóa phương tiện. Xem biển số, loại xe, tải trọng, trạng thái.
 */
import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  panelStyle, titleStyle, badgeStyle, alertStyle, successStyle,
  buttonStyle, labelStyle, inputStyle, secondaryButtonStyle,
} from '../FarmManager/ui';

type Vehicle = {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status: string;
  createdAt?: string;
};

type VehicleForm = {
  licensePlate: string;
  type: string;
  capacity: string;
};

const EMPTY_FORM: VehicleForm = { licensePlate: '', type: '', capacity: '' };

const VEHICLE_TYPES = ['Xe tải', 'Xe van', 'Xe container', 'Xe máy', 'Xe lạnh', 'Khác'];

const STATUS_LABELS: Record<string, string> = {
  AVAILABLE: 'Sẵn sàng',
  IN_USE: 'Đang sử dụng',
  MAINTENANCE: 'Đang bảo trì',
};

export default function VehiclesPage() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [editId, setEditId] = useState<number | null>(null); // null = create new
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<VehicleForm>(EMPTY_FORM);
  const [busy, setBusy] = useState(false);

  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/vehicles`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error(`Lỗi ${res.status}`);
      setVehicles(await res.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được danh sách phương tiện.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load().catch(() => {}); }, []);

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setError('');
    setShowForm(true);
  };

  const openEdit = (v: Vehicle) => {
    setEditId(v.id);
    setForm({ licensePlate: v.licensePlate, type: v.type, capacity: String(v.capacity) });
    setError('');
    setShowForm(true);
  };

  const submitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice('');
    const cap = Number(form.capacity);
    if (!form.licensePlate.trim()) { setError('Biển số xe không được để trống.'); return; }
    if (!form.type.trim()) { setError('Loại xe không được để trống.'); return; }
    if (isNaN(cap) || cap <= 0) { setError('Tải trọng phải là số dương.'); return; }

    setBusy(true);
    try {
      const url = editId != null
        ? `${API_BASE_URL}/shipping/vehicles/${editId}`
        : `${API_BASE_URL}/shipping/vehicles`;
      const method = editId != null ? 'PUT' : 'POST';
      const res = await fetch(url, {
        method,
        headers: getAuthHeaders(),
        body: JSON.stringify({
          licensePlate: form.licensePlate.trim().toUpperCase(),
          type: form.type.trim(),
          capacity: cap,
        }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setNotice(editId != null ? 'Cập nhật phương tiện thành công!' : 'Thêm phương tiện thành công!');
      setShowForm(false);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lưu phương tiện thất bại.');
    } finally {
      setBusy(false);
    }
  };

  const doDelete = async () => {
    if (deleteId == null) return;
    setDeleteBusy(true);
    setError(''); setNotice('');
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/vehicles/${deleteId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setNotice('Đã xóa phương tiện.');
      setDeleteId(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Xóa phương tiện thất bại.');
    } finally {
      setDeleteBusy(false);
    }
  };

  return (
    <div>
      <h1 className="dashboard-title">Quản lý phương tiện</h1>
      <p className="dashboard-subtitle">Thêm, sửa, xóa phương tiện vận chuyển.</p>

      {error && <div style={alertStyle}>{error}</div>}
      {notice && <div style={successStyle}>{notice}</div>}

      <div style={{ display: 'flex', gap: 12, marginBottom: 20, alignItems: 'center' }}>
        <button onClick={openCreate} style={{ ...buttonStyle, width: 'auto', padding: '10px 20px', marginTop: 0 }}>
          + Thêm phương tiện
        </button>
        <button onClick={load} style={secondaryButtonStyle}>🔄 Làm mới</button>
        <span style={{ color: '#64748b', fontSize: 13, marginLeft: 4 }}>
          {vehicles.length} phương tiện
        </span>
      </div>

      {/* Form */}
      {showForm && (
        <div className="glass-panel" style={{ ...panelStyle, marginBottom: 24 }}>
          <h2 style={titleStyle}>{editId != null ? 'Cập nhật phương tiện' : 'Thêm phương tiện mới'}</h2>
          <form onSubmit={submitForm}>
            <label style={labelStyle}>Biển số xe *</label>
            <input
              required
              maxLength={20}
              value={form.licensePlate}
              onChange={e => setForm(f => ({ ...f, licensePlate: e.target.value }))}
              placeholder="VD: 51A-12345"
              style={inputStyle}
            />

            <label style={labelStyle}>Loại xe *</label>
            <select
              value={form.type}
              onChange={e => setForm(f => ({ ...f, type: e.target.value }))}
              style={inputStyle}
            >
              <option value="">-- Chọn loại xe --</option>
              {VEHICLE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>

            <label style={labelStyle}>Tải trọng (tấn) *</label>
            <input
              required
              type="number"
              min="0.1"
              step="0.1"
              value={form.capacity}
              onChange={e => setForm(f => ({ ...f, capacity: e.target.value }))}
              placeholder="VD: 2.5"
              style={inputStyle}
            />

            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
              <button type="submit" disabled={busy} style={{ ...buttonStyle, flex: 1, marginTop: 0 }}>
                {busy ? 'Đang lưu…' : (editId != null ? '💾 Cập nhật' : '+ Thêm mới')}
              </button>
              <button
                type="button"
                onClick={() => setShowForm(false)}
                style={{ ...secondaryButtonStyle, flex: '0 0 auto' }}
              >
                Hủy
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Table */}
      {loading && <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>Đang tải…</div>}

      {!loading && vehicles.length === 0 && (
        <div className="glass-panel" style={{ ...panelStyle, textAlign: 'center', color: '#94a3b8' }}>
          Chưa có phương tiện nào. Nhấn "+ Thêm phương tiện" để bắt đầu.
        </div>
      )}

      {vehicles.length > 0 && (
        <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: 'rgba(255,255,255,.03)', borderBottom: '1px solid #1e293b' }}>
                {['Biển số', 'Loại xe', 'Tải trọng', 'Trạng thái', 'Ngày thêm', 'Thao tác'].map(h => (
                  <th key={h} style={{ padding: '12px 16px', textAlign: 'left', color: '#64748b', fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap' }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {vehicles.map(v => (
                <tr key={v.id} style={{ borderBottom: '1px solid #0f172a' }}>
                  <td style={{ padding: '12px 16px', color: '#fff', fontWeight: 600 }}>{v.licensePlate}</td>
                  <td style={{ padding: '12px 16px', color: '#cbd5e1' }}>{v.type}</td>
                  <td style={{ padding: '12px 16px', color: '#cbd5e1' }}>{v.capacity} tấn</td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={badgeStyle(v.status)}>{STATUS_LABELS[v.status] || v.status}</span>
                  </td>
                  <td style={{ padding: '12px 16px', color: '#64748b', fontSize: 12 }}>
                    {v.createdAt ? new Date(v.createdAt).toLocaleDateString('vi-VN') : '—'}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button
                        onClick={() => openEdit(v)}
                        style={{ ...secondaryButtonStyle, fontSize: 12 }}
                        disabled={v.status === 'IN_USE'}
                        title={v.status === 'IN_USE' ? 'Không thể sửa xe đang sử dụng' : ''}
                      >
                        ✏️ Sửa
                      </button>
                      <button
                        onClick={() => setDeleteId(v.id)}
                        disabled={v.status === 'IN_USE'}
                        title={v.status === 'IN_USE' ? 'Không thể xóa xe đang sử dụng' : ''}
                        style={{
                          padding: '6px 10px', border: '1px solid rgba(239,68,68,.3)',
                          borderRadius: 6, background: 'rgba(239,68,68,.08)',
                          color: v.status === 'IN_USE' ? '#64748b' : '#f87171',
                          cursor: v.status === 'IN_USE' ? 'not-allowed' : 'pointer',
                          fontSize: 12, fontWeight: 600,
                        }}
                      >
                        🗑️ Xóa
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Delete confirm */}
      {deleteId != null && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9000,
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: 380, padding: 28, borderRadius: 14 }}>
            <h3 style={{ color: '#fff', margin: '0 0 12px', fontSize: 18 }}>Xác nhận xóa phương tiện</h3>
            <p style={{ color: '#94a3b8', fontSize: 13, marginBottom: 20 }}>
              Thao tác này sẽ xóa vĩnh viễn phương tiện #{deleteId}. Không thể hoàn tác.
            </p>
            <div style={{ display: 'flex', gap: 10 }}>
              <button
                disabled={deleteBusy}
                onClick={doDelete}
                style={{ flex: 1, padding: 12, border: 0, borderRadius: 8, background: '#ef4444', color: '#fff', fontWeight: 700, cursor: 'pointer' }}
              >
                {deleteBusy ? 'Đang xóa…' : '🗑️ Xóa'}
              </button>
              <button onClick={() => setDeleteId(null)} style={{ ...secondaryButtonStyle, flex: 1 }}>
                Hủy
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
