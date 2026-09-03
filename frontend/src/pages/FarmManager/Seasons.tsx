import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  gridStyle, panelStyle, titleStyle, labelStyle, inputStyle, buttonStyle,
  secondaryButtonStyle, alertStyle, successStyle, cardStyle, hashStyle, badgeStyle,
} from './ui';

type Season = {
  id: number; name: string; productType: string; variety: string; area: number;
  startDate: string; endDate?: string; status: string; txHash?: string; createdAt?: string;
};
type Process = {
  id: number; seasonId: number; processType: string; executionDate: string;
  materials?: string; notes?: string; txHash?: string;
};
type SeasonDetail = Season & { processes: Process[]; exports?: unknown[]; farmName?: string };

const PROCESS_TYPES = ['SOIL_PREP', 'SEEDING', 'FERTILIZATION', 'PEST_CONTROL', 'HARVESTING'];
const STATUS_FLOW = ['IN_PROGRESS', 'HARVESTED', 'CANCELLED'];

export default function Seasons({ farmId }: { farmId?: number }) {
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [selected, setSelected] = useState<SeasonDetail | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const [form, setForm] = useState({ name: '', productType: '', variety: '', area: '', startDate: new Date().toISOString().slice(0, 10) });
  const [procForm, setProcForm] = useState({ processType: 'SOIL_PREP', executionDate: new Date().toISOString().slice(0, 10), materials: '', notes: '' });

  const load = async () => {
    if (!farmId) return;
    const headers = getAuthHeaders();
    const url = `${API_BASE_URL}/farms/${farmId}/seasons?size=50${statusFilter ? `&status=${statusFilter}` : ''}`;
    const res = await fetch(url, { headers });
    if (!res.ok) { setError('Không tải được danh sách mùa vụ.'); return; }
    const body = await res.json();
    const list: Season[] = Array.isArray(body) ? body : (body.content || []);
    setSeasons(list);
  };

  const loadDetail = async (seasonId: number) => {
    if (!farmId) return;
    setError(''); setNotice('');
    const res = await fetch(`${API_BASE_URL}/farms/${farmId}/seasons/${seasonId}`, { headers: getAuthHeaders() });
    if (!res.ok) { setError('Không tải được chi tiết mùa vụ.'); return; }
    setSelected(await res.json());
  };

  useEffect(() => { load().catch(() => setError('Lỗi kết nối máy chủ.')); }, [farmId, statusFilter]);
  useEffect(() => { if (selected) loadDetail(selected.id); }, [selected?.id]);

  const createSeason = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setNotice('');
    if (!farmId) { setError('Chưa xác định được nông trại của tài khoản.'); return; }
    setBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/${farmId}/seasons`, {
        method: 'POST', headers: getAuthHeaders(),
        body: JSON.stringify({ ...form, area: Number(form.area), startDate: form.startDate }),
      });
      if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || 'Tạo mùa vụ thất bại.'); }
      setNotice('Đã tạo mùa vụ và ghi lên blockchain.');
      setForm(v => ({ ...v, name: '', productType: '', variety: '', area: '' }));
      await load();
    } catch (err) { setError(err instanceof Error ? err.message : 'Tạo mùa vụ thất bại.'); }
    finally { setBusy(false); }
  };

  const changeStatus = async (status: string) => {
    if (!farmId || !selected) return;
    setError(''); setNotice('');
    let body: Record<string, unknown> = { status };
    if (status === 'HARVESTED') {
      const qty = window.prompt(`Sản lượng thu hoạch của "${selected.name}" (số, vd: 120.5):`, '100');
      if (qty === null) return;
      const unit = window.prompt('Đơn vị thu hoạch (kg, tấn, hộp...):', 'kg');
      if (unit === null) return;
      const parsed = Number(qty);
      if (!parsed || parsed <= 0) { setError('Sản lượng thu hoạch phải là số dương.'); return; }
      body = { status, harvestedQuantity: parsed, harvestUnit: unit || 'kg' };
    }
    const res = await fetch(`${API_BASE_URL}/farms/${farmId}/seasons/${selected.id}/status`, {
      method: 'PATCH', headers: getAuthHeaders(), body: JSON.stringify(body),
    });
    if (!res.ok) { const b = await res.json().catch(() => ({})); setError(b.message || 'Cập nhật trạng thái thất bại.'); return; }
    setNotice(`Đã cập nhật trạng thái mùa vụ → ${status}.`);
    await loadDetail(selected.id); await load();
  };

  const addProcess = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setNotice('');
    if (!selected) return;
    setBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/seasons/${selected.id}/processes`, {
        method: 'POST', headers: getAuthHeaders(),
        body: JSON.stringify({ processType: procForm.processType, executionDate: procForm.executionDate, materials: procForm.materials, notes: procForm.notes }),
      });
      if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || 'Thêm quy trình thất bại.'); }
      setNotice('Đã bổ sung bước quy trình và ghi lên blockchain.');
      setProcForm(v => ({ ...v, materials: '', notes: '' }));
      await loadDetail(selected.id);
    } catch (err) { setError(err instanceof Error ? err.message : 'Thêm quy trình thất bại.'); }
    finally { setBusy(false); }
  };

  if (!farmId) {
    return <div><h1 className="dashboard-title">Quản lý mùa vụ</h1><div style={alertStyle}>Chưa xác định được nông trại của tài khoản.</div></div>;
  }

  return (
    <div>
      <h1 className="dashboard-title">Quản lý mùa vụ</h1>
      <p className="dashboard-subtitle">BICAP-12→15 · Tạo mùa vụ, cập nhật quy trình sản xuất và ghi dữ liệu lên Blockchain.</p>
      {error && <div style={alertStyle}>{error}</div>}
      {notice && <div style={successStyle}>{notice}</div>}

      <div style={gridStyle}>
        <form className="glass-panel" style={panelStyle} onSubmit={createSeason}>
          <h2 style={titleStyle}>Tạo mùa vụ mới</h2>
          <label style={labelStyle}>Tên mùa vụ</label>
          <input required maxLength={255} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Loại nông sản</label>
          <input required maxLength={100} value={form.productType} onChange={e => setForm({ ...form, productType: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Giống/cây trồng</label>
          <input required maxLength={100} value={form.variety} onChange={e => setForm({ ...form, variety: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Diện tích (m²)</label>
          <input required min="0.01" step="0.01" type="number" value={form.area} onChange={e => setForm({ ...form, area: e.target.value })} style={inputStyle} />
          <label style={labelStyle}>Ngày bắt đầu</label>
          <input required type="date" value={form.startDate} onChange={e => setForm({ ...form, startDate: e.target.value })} style={inputStyle} />
          <button disabled={busy} style={buttonStyle}>{busy ? 'Đang xử lý…' : 'Tạo mùa vụ & ghi Blockchain'}</button>
        </form>

        <section className="glass-panel" style={panelStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
            <h2 style={titleStyle}>Danh sách mùa vụ</h2>
            <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} style={{ ...inputStyle, width: 'auto' }}>
              <option value="">Tất cả trạng thái</option>
              {STATUS_FLOW.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          {!seasons.length && <p style={{ color: '#94a3b8' }}>Chưa có mùa vụ nào.</p>}
          {seasons.map(s => (
            <article key={s.id} style={{ ...cardStyle, cursor: 'pointer', borderColor: selected?.id === s.id ? '#10b981' : '#334155' }} onClick={() => setSelected(s as SeasonDetail)}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                <strong>{s.name}</strong><span style={badgeStyle(s.status)}>{s.status}</span>
              </div>
              <p style={{ margin: '6px 0' }}>{s.productType} · {s.variety} · {s.area} m²</p>
              <p style={{ color: '#94a3b8', fontSize: 12 }}>{s.startDate}{s.endDate ? ` → ${s.endDate}` : ''}</p>
              {s.txHash && <p style={hashStyle}>TX: {s.txHash}</p>}
            </article>
          ))}
        </section>
      </div>

      {selected && (
        <section className="glass-panel" style={{ ...panelStyle, marginTop: 24 }}>
          <h2 style={titleStyle}>Chi tiết: {selected.name}</h2>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 16 }}>
            {STATUS_FLOW.filter(s => s !== selected.status).map(s => (
              <button key={s} onClick={() => changeStatus(s)} style={secondaryButtonStyle}>Chuyển sang {s}</button>
            ))}
          </div>

          <h3 style={{ color: '#fff', fontSize: 16, margin: '8px 0' }}>Quy trình sản xuất ({selected.processes?.length || 0})</h3>
          {!selected.processes?.length && <p style={{ color: '#94a3b8' }}>Chưa có bước quy trình nào.</p>}
          {selected.processes?.map(p => (
            <article key={p.id} style={cardStyle}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                <strong>{p.processType}</strong><span style={{ color: '#94a3b8', fontSize: 12 }}>{p.executionDate}</span>
              </div>
              {p.materials && <p style={{ fontSize: 13 }}>Vật tư: {p.materials}</p>}
              {p.notes && <p style={{ fontSize: 13, color: '#cbd5e1' }}>{p.notes}</p>}
              {p.txHash && <p style={hashStyle}>TX: {p.txHash}</p>}
            </article>
          ))}

          <form onSubmit={addProcess} style={{ marginTop: 20, borderTop: '1px solid #334155', paddingTop: 16 }}>
            <h3 style={{ color: '#fff', fontSize: 16, margin: '0 0 8px' }}>Thêm bước quy trình</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label style={labelStyle}>Loại quy trình</label>
                <select value={procForm.processType} onChange={e => setProcForm({ ...procForm, processType: e.target.value })} style={inputStyle}>
                  {PROCESS_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <label style={labelStyle}>Ngày thực hiện</label>
                <input required type="date" value={procForm.executionDate} onChange={e => setProcForm({ ...procForm, executionDate: e.target.value })} style={inputStyle} />
              </div>
            </div>
            <label style={labelStyle}>Vật tư / phân bón (JSON hoặc mô tả)</label>
            <input value={procForm.materials} onChange={e => setProcForm({ ...procForm, materials: e.target.value })} style={inputStyle} />
            <label style={labelStyle}>Ghi chú</label>
            <textarea rows={2} value={procForm.notes} onChange={e => setProcForm({ ...procForm, notes: e.target.value })} style={inputStyle} />
            <button disabled={busy} style={buttonStyle}>{busy ? 'Đang ghi…' : 'Thêm quy trình & ghi Blockchain'}</button>
          </form>
        </section>
      )}
    </div>
  );
}
