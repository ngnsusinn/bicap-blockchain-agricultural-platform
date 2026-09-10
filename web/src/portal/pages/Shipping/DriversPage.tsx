/**
 * BICAP-59 — Shipping: Quản lý tài xế (CRUD + gán phương tiện).
 *
 * Shipping Manager thêm, sửa, xóa tài xế và gán phương tiện.
 */
import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  panelStyle, titleStyle, badgeStyle, alertStyle, successStyle,
  buttonStyle, labelStyle, inputStyle, secondaryButtonStyle,
} from '../FarmManager/ui';

type Driver = {
  id: number;
  userId?: number;
  userName?: string;
  userEmail?: string;
  userPhone?: string;
  citizenId: string;
  licenseNumber: string;
  status: string;
  vehicleId?: number;
  vehicleLicensePlate?: string;
  vehicleType?: string;
  createdAt?: string;
};

type Vehicle = {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status: string;
};

const STATUS_LABELS: Record<string, string> = {
  IDLE: 'Sẵn sàng',
  ON_TRIP: 'Đang chạy',
  OFFLINE: 'Ngoại tuyến',
};

export default function DriversPage() {
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Create form — uses existing SHIP_DRIVER user ID
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({
    userId: '', citizenId: '', licenseNumber: '', vehicleId: '',
  });
  const [createBusy, setCreateBusy] = useState(false);
  // Users with SHIP_DRIVER role but no driver profile yet
  const [availableDriverUsers, setAvailableDriverUsers] = useState<{ id: number; fullName: string; email: string; phone?: string }[]>([]);

  // Edit form
  const [editDriver, setEditDriver] = useState<Driver | null>(null);
  const [editForm, setEditForm] = useState({ citizenId: '', licenseNumber: '' });
  const [editBusy, setEditBusy] = useState(false);

  // Assign vehicle
  const [assignDriver, setAssignDriver] = useState<Driver | null>(null);
  const [assignVehicleId, setAssignVehicleId] = useState('');
  const [assignBusy, setAssignBusy] = useState(false);

  // Delete
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [dRes, vRes] = await Promise.all([
        fetch(`${API_BASE_URL}/shipping/drivers`, { headers: getAuthHeaders() }),
        fetch(`${API_BASE_URL}/shipping/vehicles`, { headers: getAuthHeaders() }),
      ]);
      if (dRes.ok) {
        const driverList: Driver[] = await dRes.json();
        setDrivers(driverList);
        // Build set of already-registered user IDs so we can filter them out in the create form
        const registeredIds = new Set(driverList.map(d => d.userId).filter(Boolean));
        setAvailableDriverUsers(prev => prev.filter(u => !registeredIds.has(u.id)));
      }
      if (vRes.ok) setVehicles(await vRes.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được danh sách tài xế.');
    } finally {
      setLoading(false);
    }
  };

  // Fetch all SHIP_DRIVER role users from admin endpoint
  const loadDriverUsers = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/driver-users`, { headers: getAuthHeaders() });
      if (res.ok) setAvailableDriverUsers(await res.json());
    } catch {
      // Non-fatal — SM can still type userId manually
    }
  };

  useEffect(() => {
    load().catch(() => {});
    loadDriverUsers().catch(() => {});
  }, []);

  const submitCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setNotice('');
    setCreateBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/drivers`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          userId: Number(createForm.userId),
          citizenId: createForm.citizenId.trim(),
          licenseNumber: createForm.licenseNumber.trim(),
          vehicleId: createForm.vehicleId ? Number(createForm.vehicleId) : null,
        }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setNotice('Thêm hồ sơ tài xế thành công!');
      setShowCreate(false);
      setCreateForm({ userId: '', citizenId: '', licenseNumber: '', vehicleId: '' });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Thêm tài xế thất bại.');
    } finally {
      setCreateBusy(false);
    }
  };

  const submitEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editDriver) return;
    setError(''); setNotice('');
    setEditBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/drivers/${editDriver.id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          citizenId: editForm.citizenId.trim(),
          licenseNumber: editForm.licenseNumber.trim(),
        }),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setNotice('Cập nhật tài xế thành công!');
      setEditDriver(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Cập nhật tài xế thất bại.');
    } finally {
      setEditBusy(false);
    }
  };

  const submitAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assignDriver) return;
    setError(''); setNotice('');
    setAssignBusy(true);
    try {
      const url = assignVehicleId
        ? `${API_BASE_URL}/shipping/drivers/${assignDriver.id}/assign?vehicleId=${assignVehicleId}`
        : `${API_BASE_URL}/shipping/drivers/${assignDriver.id}/assign?vehicleId=0`;
      const res = await fetch(url, { method: 'PUT', headers: getAuthHeaders() });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setNotice('Gán phương tiện thành công!');
      setAssignDriver(null);
      setAssignVehicleId('');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Gán phương tiện thất bại.');
    } finally {
      setAssignBusy(false);
    }
  };

  const doDelete = async () => {
    if (deleteId == null) return;
    setDeleteBusy(true);
    setError(''); setNotice('');
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/drivers/${deleteId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const b = await res.json().catch(() => ({}));
        throw new Error(b.message || `Lỗi ${res.status}`);
      }
      setNotice('Đã xóa tài xế.');
      setDeleteId(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Xóa tài xế thất bại.');
    } finally {
      setDeleteBusy(false);
    }
  };

  const availableVehicles = vehicles.filter(v => v.status === 'AVAILABLE');

  return (
    <div>
      <h1 className="dashboard-title">Quản lý tài xế</h1>
      <p className="dashboard-subtitle">Thêm, sửa, xóa tài xế và gán phương tiện.</p>

      {error && <div style={alertStyle}>{error}</div>}
      {notice && <div style={successStyle}>{notice}</div>}

      <div style={{ display: 'flex', gap: 12, marginBottom: 20, alignItems: 'center' }}>
        <button onClick={() => { setShowCreate(v => !v); setError(''); }} style={{ ...buttonStyle, width: 'auto', padding: '10px 20px', marginTop: 0 }}>
          {showCreate ? '✕ Đóng' : '+ Thêm tài xế'}
        </button>
        <button onClick={load} style={secondaryButtonStyle}>🔄 Làm mới</button>
        <span style={{ color: '#64748b', fontSize: 13 }}>{drivers.length} tài xế</span>
      </div>

      {/* Create form */}
      {showCreate && (
        <div className="glass-panel" style={{ ...panelStyle, marginBottom: 24 }}>
          <h2 style={titleStyle}>Thêm hồ sơ tài xế mới</h2>
          <p style={{ color: '#94a3b8', fontSize: 13, marginBottom: 12 }}>
            Chọn tài khoản đã có role SHIP_DRIVER, sau đó nhập thông tin hồ sơ tài xế.
          </p>
          <form onSubmit={submitCreate}>
            <label style={labelStyle}>Tài khoản SHIP_DRIVER *</label>
            {availableDriverUsers.length > 0 ? (
              <select
                required
                value={createForm.userId}
                onChange={e => setCreateForm(f => ({ ...f, userId: e.target.value }))}
                style={inputStyle}
              >
                <option value="">-- Chọn tài khoản --</option>
                {availableDriverUsers.map(u => (
                  <option key={u.id} value={u.id}>
                    #{u.id} — {u.fullName} ({u.email}){u.phone ? `, ${u.phone}` : ''}
                  </option>
                ))}
              </select>
            ) : (
              <input
                required
                type="number"
                min="1"
                value={createForm.userId}
                onChange={e => setCreateForm(f => ({ ...f, userId: e.target.value }))}
                placeholder="Nhập User ID của tài khoản SHIP_DRIVER"
                style={inputStyle}
              />
            )}
            <label style={labelStyle}>Số CCCD / CMND *</label>
            <input
              required
              maxLength={20}
              value={createForm.citizenId}
              onChange={e => setCreateForm(f => ({ ...f, citizenId: e.target.value }))}
              placeholder="012345678901"
              style={inputStyle}
            />
            <label style={labelStyle}>Số giấy phép lái xe *</label>
            <input
              required
              maxLength={30}
              value={createForm.licenseNumber}
              onChange={e => setCreateForm(f => ({ ...f, licenseNumber: e.target.value }))}
              placeholder="B2-000001"
              style={inputStyle}
            />
            <label style={labelStyle}>Gán phương tiện ngay (tuỳ chọn)</label>
            <select
              value={createForm.vehicleId}
              onChange={e => setCreateForm(f => ({ ...f, vehicleId: e.target.value }))}
              style={inputStyle}
            >
              <option value="">-- Không gán --</option>
              {vehicles.filter(v => v.status === 'AVAILABLE').map(v => (
                <option key={v.id} value={v.id}>
                  {v.licensePlate} — {v.type} ({v.capacity} tấn)
                </option>
              ))}
            </select>
            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
              <button type="submit" disabled={createBusy} style={{ ...buttonStyle, flex: 1, marginTop: 0 }}>
                {createBusy ? 'Đang thêm…' : '+ Thêm hồ sơ tài xế'}
              </button>
              <button type="button" onClick={() => setShowCreate(false)} style={{ ...secondaryButtonStyle }}>
                Hủy
              </button>
            </div>
          </form>
        </div>
      )}

      {loading && <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>Đang tải…</div>}

      {!loading && drivers.length === 0 && (
        <div className="glass-panel" style={{ ...panelStyle, textAlign: 'center', color: '#94a3b8' }}>
          Chưa có tài xế nào.
        </div>
      )}

      {drivers.length > 0 && (
        <div className="glass-panel" style={{ padding: 0, overflow: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 700 }}>
            <thead>
              <tr style={{ background: 'rgba(255,255,255,.03)', borderBottom: '1px solid #1e293b' }}>
                {['Tên tài xế', 'Email / SĐT', 'CCCD', 'GPLX', 'Phương tiện', 'Trạng thái', 'Thao tác'].map(h => (
                  <th key={h} style={{ padding: '12px 14px', textAlign: 'left', color: '#64748b', fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap' }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {drivers.map(d => (
                <tr key={d.id} style={{ borderBottom: '1px solid #0f172a' }}>
                  <td style={{ padding: '12px 14px', color: '#fff', fontWeight: 600 }}>
                    {d.userName || `Tài xế #${d.id}`}
                  </td>
                  <td style={{ padding: '12px 14px', color: '#94a3b8', fontSize: 12 }}>
                    {d.userEmail || '—'}<br />{d.userPhone || '—'}
                  </td>
                  <td style={{ padding: '12px 14px', color: '#cbd5e1', fontFamily: 'monospace', fontSize: 12 }}>{d.citizenId}</td>
                  <td style={{ padding: '12px 14px', color: '#cbd5e1', fontSize: 12 }}>{d.licenseNumber}</td>
                  <td style={{ padding: '12px 14px', color: '#94a3b8', fontSize: 12 }}>
                    {d.vehicleLicensePlate ? (
                      <span>{d.vehicleLicensePlate} <span style={{ color: '#64748b' }}>({d.vehicleType})</span></span>
                    ) : <span style={{ color: '#475569' }}>—</span>}
                  </td>
                  <td style={{ padding: '12px 14px' }}>
                    <span style={badgeStyle(d.status)}>{STATUS_LABELS[d.status] || d.status}</span>
                  </td>
                  <td style={{ padding: '12px 14px' }}>
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                      <button
                        onClick={() => { setEditDriver(d); setEditForm({ citizenId: d.citizenId, licenseNumber: d.licenseNumber }); }}
                        disabled={d.status === 'ON_TRIP'}
                        style={{ ...secondaryButtonStyle, fontSize: 11 }}
                        title={d.status === 'ON_TRIP' ? 'Không thể sửa khi đang chạy' : ''}
                      >
                        ✏️ Sửa
                      </button>
                      <button
                        onClick={() => { setAssignDriver(d); setAssignVehicleId(d.vehicleId ? String(d.vehicleId) : ''); }}
                        disabled={d.status === 'ON_TRIP'}
                        style={{ ...secondaryButtonStyle, fontSize: 11 }}
                      >
                        🚛 Gán xe
                      </button>
                      <button
                        onClick={() => setDeleteId(d.id)}
                        disabled={d.status === 'ON_TRIP'}
                        style={{
                          padding: '5px 9px', border: '1px solid rgba(239,68,68,.3)',
                          borderRadius: 6, background: 'rgba(239,68,68,.08)',
                          color: d.status === 'ON_TRIP' ? '#64748b' : '#f87171',
                          cursor: d.status === 'ON_TRIP' ? 'not-allowed' : 'pointer',
                          fontSize: 11, fontWeight: 600,
                        }}
                      >
                        🗑️
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Edit modal */}
      {editDriver && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9000 }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: 420, padding: 28, borderRadius: 14 }}>
            <h3 style={{ color: '#fff', margin: '0 0 16px' }}>Cập nhật tài xế: {editDriver.userName}</h3>
            <form onSubmit={submitEdit}>
              <label style={labelStyle}>Số CCCD / CMND *</label>
              <input required maxLength={20} value={editForm.citizenId}
                onChange={e => setEditForm(f => ({ ...f, citizenId: e.target.value }))} style={inputStyle} />
              <label style={labelStyle}>Số GPLX *</label>
              <input required maxLength={30} value={editForm.licenseNumber}
                onChange={e => setEditForm(f => ({ ...f, licenseNumber: e.target.value }))} style={inputStyle} />
              <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
                <button type="submit" disabled={editBusy} style={{ ...buttonStyle, flex: 1, marginTop: 0 }}>
                  {editBusy ? 'Đang lưu…' : '💾 Lưu'}
                </button>
                <button type="button" onClick={() => setEditDriver(null)} style={{ ...secondaryButtonStyle }}>Hủy</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Assign vehicle modal */}
      {assignDriver && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9000 }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: 420, padding: 28, borderRadius: 14 }}>
            <h3 style={{ color: '#fff', margin: '0 0 16px' }}>Gán phương tiện: {assignDriver.userName}</h3>
            <form onSubmit={submitAssign}>
              <label style={labelStyle}>Phương tiện</label>
              <select value={assignVehicleId} onChange={e => setAssignVehicleId(e.target.value)} style={inputStyle}>
                <option value="">-- Không gán --</option>
                {availableVehicles.map(v => (
                  <option key={v.id} value={v.id}>
                    {v.licensePlate} — {v.type} ({v.capacity} tấn)
                  </option>
                ))}
              </select>
              <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
                <button type="submit" disabled={assignBusy} style={{ ...buttonStyle, flex: 1, marginTop: 0 }}>
                  {assignBusy ? 'Đang gán…' : '🚛 Gán xe'}
                </button>
                <button type="button" onClick={() => setAssignDriver(null)} style={{ ...secondaryButtonStyle }}>Hủy</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirm */}
      {deleteId != null && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9000 }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: 360, padding: 28, borderRadius: 14 }}>
            <h3 style={{ color: '#fff', margin: '0 0 12px' }}>Xác nhận xóa tài xế #{deleteId}</h3>
            <p style={{ color: '#94a3b8', fontSize: 13, marginBottom: 20 }}>Thao tác này sẽ xóa hồ sơ tài xế. Tài khoản người dùng vẫn tồn tại.</p>
            <div style={{ display: 'flex', gap: 10 }}>
              <button disabled={deleteBusy} onClick={doDelete}
                style={{ flex: 1, padding: 12, border: 0, borderRadius: 8, background: '#ef4444', color: '#fff', fontWeight: 700, cursor: 'pointer' }}>
                {deleteBusy ? 'Đang xóa…' : '🗑️ Xóa'}
              </button>
              <button onClick={() => setDeleteId(null)} style={{ ...secondaryButtonStyle, flex: 1 }}>Hủy</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
