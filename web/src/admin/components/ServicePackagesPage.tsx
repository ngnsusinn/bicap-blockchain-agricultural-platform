/**
 * Admin: Quản lý gói dịch vụ (CRUD).
 * Endpoints: GET/POST/PUT/DELETE /api/service-packages/admin/*
 */
import React, { useState, useEffect, useCallback } from 'react';
import type { UserSession } from '../types';
import { API_BASE_URL, getToken } from '../../shared/session';

interface ServicePackage {
  id: number;
  name: string;
  description: string;
  price: number;
  durationDays: number;
  features: string;
  status: string;
}

interface FormState {
  name: string;
  description: string;
  price: string;
  durationDays: string;
  features: string;   // raw JSON string edited in textarea
  status: string;
}

const EMPTY_FORM: FormState = {
  name: '', description: '', price: '', durationDays: '', features: '[]', status: 'ACTIVE',
};

const STATUS_STYLE: Record<string, React.CSSProperties> = {
  ACTIVE:   { color:'#6ee7b7', background:'rgba(16,185,129,.15)', border:'1px solid rgba(16,185,129,.35)' },
  INACTIVE: { color:'#fca5a5', background:'rgba(239,68,68,.1)',   border:'1px solid rgba(239,68,68,.3)'  },
};

interface Props { currentSession: UserSession; onToast: (msg: string, type: 'success'|'error'|'info') => void; }

export const ServicePackagesPage: React.FC<Props> = ({ currentSession, onToast }) => {
  const [packages, setPackages]   = useState<ServicePackage[]>([]);
  const [loading, setLoading]     = useState(true);
  const [editItem, setEditItem]   = useState<ServicePackage | null>(null); // null = create
  const [showForm, setShowForm]   = useState(false);
  const [form, setForm]           = useState<FormState>(EMPTY_FORM);
  const [busy, setBusy]           = useState(false);
  const [deleteId, setDeleteId]   = useState<number | null>(null);
  const [featuresError, setFeaturesError] = useState('');

  const apiBase = API_BASE_URL;

  const token = getToken() || '';
  const headers = {
    'Content-Type': 'application/json',
    'X-Actor-Email': currentSession.email,
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
  };

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`${apiBase}/service-packages/admin/all`, { headers });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setPackages(await res.json());
    } catch (e: any) {
      onToast(e.message || 'Không tải được danh sách gói dịch vụ.', 'error');
    } finally {
      setLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiBase]);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => {
    setEditItem(null);
    setForm(EMPTY_FORM);
    setFeaturesError('');
    setShowForm(true);
  };

  const openEdit = (pkg: ServicePackage) => {
    setEditItem(pkg);
    setForm({
      name: pkg.name, description: pkg.description,
      price: String(pkg.price), durationDays: String(pkg.durationDays),
      features: pkg.features || '[]', status: pkg.status,
    });
    setFeaturesError('');
    setShowForm(true);
  };

  const validateFeatures = (raw: string): boolean => {
    try { JSON.parse(raw); setFeaturesError(''); return true; }
    catch { setFeaturesError('Features phải là JSON mảng hợp lệ, ví dụ: ["Tính năng 1","Tính năng 2"]'); return false; }
  };

  const submitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateFeatures(form.features)) return;
    setBusy(true);
    try {
      const body = {
        name: form.name.trim(),
        description: form.description.trim(),
        price: form.price,
        durationDays: form.durationDays,
        features: form.features,
        status: form.status,
      };
      const url    = editItem ? `${apiBase}/service-packages/admin/${editItem.id}` : `${apiBase}/service-packages/admin`;
      const method = editItem ? 'PUT' : 'POST';
      const res = await fetch(url, { method, headers, body: JSON.stringify(body) });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || `HTTP ${res.status}`);
      }
      onToast(editItem ? 'Cập nhật gói thành công!' : 'Tạo gói dịch vụ mới thành công!', 'success');
      setShowForm(false);
      await load();
    } catch (e: any) {
      onToast(e.message || 'Lưu thất bại.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const doDelete = async () => {
    if (deleteId == null) return;
    setBusy(true);
    try {
      const res = await fetch(`${apiBase}/service-packages/admin/${deleteId}`, { method: 'DELETE', headers });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || `HTTP ${res.status}`);
      }
      onToast('Đã xoá (hoặc ẩn) gói dịch vụ.', 'success');
      setDeleteId(null);
      await load();
    } catch (e: any) {
      onToast(e.message || 'Xoá thất bại.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const parseFeatureList = (raw: string): string[] => {
    try { return JSON.parse(raw); }
    catch { return raw.split(',').map(s => s.trim()).filter(Boolean); }
  };

  const canEdit = ['SUPER_ADMIN', 'ADMIN'].includes(currentSession.role);

  return (
    <div>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', flexWrap:'wrap', gap:16, marginBottom:24 }}>
        <div>
          <h1 className="dashboard-title">Gói Dịch Vụ</h1>
          <p className="dashboard-subtitle">Quản lý danh mục gói đăng ký cho Farm Manager.</p>
        </div>
        {canEdit && (
          <button
            onClick={openCreate}
            className="btn btn-primary"
            style={{ marginTop:8 }}
          >➕ Tạo gói mới</button>
        )}
      </div>

      {loading && <div style={{ padding:40, textAlign:'center', color:'#8b5cf6' }}>Đang tải…</div>}

      {!loading && packages.length === 0 && (
        <div className="glass-panel" style={{ padding:32, textAlign:'center', color:'#64748b' }}>
          Chưa có gói dịch vụ nào.
        </div>
      )}

      {/* Package cards */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(320px,1fr))', gap:20 }}>
        {packages.map(pkg => (
          <div key={pkg.id} className="glass-panel" style={{ padding:24, display:'flex', flexDirection:'column', gap:12 }}>
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start' }}>
              <h3 style={{ margin:0, fontSize:18, fontWeight:700, color:'#fff' }}>{pkg.name}</h3>
              <span style={{
                padding:'4px 10px', borderRadius:99, fontSize:11, fontWeight:700,
                ...(STATUS_STYLE[pkg.status] || STATUS_STYLE['INACTIVE']),
              }}>{pkg.status}</span>
            </div>
            <p style={{ margin:0, fontSize:13, color:'#94a3b8', lineHeight:1.6 }}>{pkg.description}</p>
            <div style={{ display:'flex', gap:20, fontSize:13 }}>
              <span style={{ color:'#6ee7b7', fontWeight:700, fontSize:18 }}>
                {Number(pkg.price).toLocaleString('vi-VN')} ₫
              </span>
              <span style={{ color:'#64748b', alignSelf:'flex-end' }}>/ {pkg.durationDays} ngày</span>
            </div>
            {parseFeatureList(pkg.features).length > 0 && (
              <ul style={{ margin:0, padding:0, listStyle:'none', display:'flex', flexDirection:'column', gap:5 }}>
                {parseFeatureList(pkg.features).slice(0, 4).map((f, i) => (
                  <li key={i} style={{ fontSize:12, color:'#94a3b8', display:'flex', gap:8 }}>
                    <span style={{ color:'#8b5cf6' }}>✦</span>{f}
                  </li>
                ))}
                {parseFeatureList(pkg.features).length > 4 && (
                  <li style={{ fontSize:11, color:'#475569' }}>+{parseFeatureList(pkg.features).length - 4} tính năng khác…</li>
                )}
              </ul>
            )}
            {canEdit && (
              <div style={{ display:'flex', gap:8, marginTop:4 }}>
                <button
                  onClick={() => openEdit(pkg)}
                  style={{
                    flex:1, padding:'8px 0', border:'1px solid #334155', borderRadius:8,
                    background:'rgba(255,255,255,.04)', color:'#cbd5e1',
                    fontWeight:600, cursor:'pointer', fontSize:13,
                  }}
                >✏️ Sửa</button>
                <button
                  onClick={() => setDeleteId(pkg.id)}
                  style={{
                    flex:1, padding:'8px 0', border:'1px solid rgba(239,68,68,.3)', borderRadius:8,
                    background:'rgba(239,68,68,.08)', color:'#f87171',
                    fontWeight:600, cursor:'pointer', fontSize:13,
                  }}
                >🗑️ Xoá</button>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Create / Edit form modal */}
      {showForm && (
        <div style={{
          position:'fixed', inset:0, background:'rgba(0,0,0,.75)', backdropFilter:'blur(6px)',
          display:'flex', alignItems:'center', justifyContent:'center', zIndex:9000,
        }}>
          <div className="glass-panel" style={{ width:'100%', maxWidth:520, maxHeight:'90vh', overflowY:'auto', padding:32, borderRadius:16 }}>
            <h3 style={{ color:'#fff', margin:'0 0 20px', fontSize:20 }}>
              {editItem ? `Sửa: ${editItem.name}` : 'Tạo gói dịch vụ mới'}
            </h3>
            <form onSubmit={submitForm}>
              {/* Name */}
              <label style={lbl}>Tên gói *</label>
              <input required maxLength={100} value={form.name}
                onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                placeholder="VD: Gói Chuyên Nghiệp" style={inp} />

              {/* Description */}
              <label style={lbl}>Mô tả *</label>
              <textarea required rows={3} value={form.description}
                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                placeholder="Mô tả ngắn về gói…" style={{ ...inp, resize:'vertical' }} />

              {/* Price */}
              <label style={lbl}>Giá (VNĐ) *</label>
              <input required type="number" min="0" step="1000" value={form.price}
                onChange={e => setForm(f => ({ ...f, price: e.target.value }))}
                placeholder="500000" style={inp} />

              {/* Duration */}
              <label style={lbl}>Thời hạn (ngày) *</label>
              <input required type="number" min="1" value={form.durationDays}
                onChange={e => setForm(f => ({ ...f, durationDays: e.target.value }))}
                placeholder="30" style={inp} />

              {/* Features JSON */}
              <label style={lbl}>Tính năng (JSON mảng chuỗi) *</label>
              <textarea
                required rows={5} value={form.features}
                onChange={e => { setForm(f => ({ ...f, features: e.target.value })); validateFeatures(e.target.value); }}
                placeholder={'["Tính năng 1","Tính năng 2"]'}
                style={{ ...inp, resize:'vertical', fontFamily:'monospace', fontSize:12 }}
              />
              {featuresError && <p style={{ color:'#f87171', fontSize:12, margin:'4px 0 0' }}>{featuresError}</p>}

              {/* Status */}
              <label style={lbl}>Trạng thái</label>
              <select value={form.status} onChange={e => setForm(f => ({ ...f, status: e.target.value }))} style={inp}>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>

              <div style={{ display:'flex', gap:10, marginTop:24 }}>
                <button type="submit" disabled={busy} className="btn btn-primary" style={{ flex:1 }}>
                  {busy ? 'Đang lưu…' : (editItem ? '💾 Cập nhật' : '➕ Tạo mới')}
                </button>
                <button type="button" onClick={() => setShowForm(false)}
                  style={{ flex:1, padding:'10px', border:'1px solid #334155', borderRadius:8, background:'rgba(255,255,255,.04)', color:'#cbd5e1', cursor:'pointer', fontWeight:600 }}>
                  Huỷ
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirm modal */}
      {deleteId != null && (
        <div style={{
          position:'fixed', inset:0, background:'rgba(0,0,0,.75)',
          display:'flex', alignItems:'center', justifyContent:'center', zIndex:9000,
        }}>
          <div className="glass-panel" style={{ width:'100%', maxWidth:380, padding:28, borderRadius:14 }}>
            <h3 style={{ color:'#fff', margin:'0 0 12px' }}>Xác nhận xoá gói #{deleteId}</h3>
            <p style={{ color:'#94a3b8', fontSize:13, marginBottom:20 }}>
              Nếu còn subscription đang hoạt động, gói sẽ được ẩn (INACTIVE) thay vì xoá cứng.
            </p>
            <div style={{ display:'flex', gap:10 }}>
              <button disabled={busy} onClick={doDelete}
                style={{ flex:1, padding:12, border:0, borderRadius:8, background:'#ef4444', color:'#fff', fontWeight:700, cursor:'pointer' }}>
                {busy ? 'Đang xoá…' : '🗑️ Xoá'}
              </button>
              <button onClick={() => setDeleteId(null)}
                style={{ flex:1, padding:12, border:'1px solid #334155', borderRadius:8, background:'rgba(255,255,255,.04)', color:'#cbd5e1', cursor:'pointer', fontWeight:600 }}>
                Huỷ
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ── Shared inline styles ──────────────────────────────────────────────────────
const lbl: React.CSSProperties = {
  display:'block', color:'#cbd5e1', fontSize:13, margin:'13px 0 6px',
};
const inp: React.CSSProperties = {
  width:'100%', boxSizing:'border-box', padding:'11px 12px',
  borderRadius:8, border:'1px solid #334155',
  background:'#111827', color:'#fff', fontSize:13,
};
