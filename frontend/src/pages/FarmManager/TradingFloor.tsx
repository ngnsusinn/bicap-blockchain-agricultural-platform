import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

type ExportItem = { id:number; seasonId:number; quantity:number; unit:string; exportDate:string; warehouse:string; status:string; traceHash:string; qrImage?:string };
type Category = { id:number; name:string; description?:string; icon?:string };
type Listing = { id:number; name:string; description:string; price:number; quantity:number; categoryName?:string; seasonName?:string; exportId:number; traceHash?:string; qrImage?:string; images:string[]; status:string; createdAt:string };

/**
 * Đăng ký đẩy sản phẩm đã xuất kho lên sàn giao dịch (BICAP-18 / SRS-FM-012).
 * Chọn lô xuất kho READY (đã có QR truy xuất), điền tên / mô tả / số lượng / giá / ảnh
 * rồi submit multipart — sản phẩm được tạo ở trạng thái PENDING_REVIEW chờ admin duyệt.
 */
export default function TradingFloor({ farmId }: { farmId?: number }) {
  const [exports, setExports] = useState<ExportItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<Listing | null>(null);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ exportId:'', categoryId:'', name:'', description:'', quantity:'', price:'' });
  const [files, setFiles] = useState<File[]>([]);

  const load = async () => {
    if (!farmId) return;
    const headers = getAuthHeaders();
    const [exportsResult, categoriesResult] = await Promise.all([
      fetch(`${API_BASE_URL}/farms/${farmId}/exports`, { headers }),
      fetch(`${API_BASE_URL}/categories`, { headers }),
    ]);
    if (exportsResult.ok) {
      const body = await exportsResult.json();
      // Chỉ những lô đã READY (đã ghi blockchain + có QR) mới được đăng lên sàn
      setExports((Array.isArray(body) ? body : []).filter((e: ExportItem) => e.status === 'READY'));
    }
    if (categoriesResult.ok) setCategories(await categoriesResult.json());
  };
  useEffect(() => { load().catch(() => setError('Không thể tải dữ liệu.')); }, [farmId]);

  const selectedExport = exports.find(e => String(e.id) === form.exportId);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError(''); setSuccess(null);
    if (!farmId) { setError('Chưa xác định được nông trại của tài khoản.'); return; }
    if (!form.exportId) { setError('Vui lòng chọn lô hàng xuất kho.'); return; }
    if (!files.length) { setError('Vui lòng chọn ít nhất 1 ảnh sản phẩm.'); return; }
    if (files.length > 10) { setError('Tối đa 10 ảnh sản phẩm.'); return; }
    setBusy(true);
    try {
      const payload = {
        exportId: Number(form.exportId),
        name: form.name.trim(),
        description: form.description.trim(),
        quantity: Number(form.quantity),
        price: Number(form.price),
        categoryId: Number(form.categoryId),
      };
      const fd = new FormData();
      fd.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
      files.forEach(f => fd.append('images', f));

      // multipart: không đặt Content-Type thủ công — trình duyệt tự sinh boundary
      const token = localStorage.getItem('accessToken');
      const response = await fetch(`${API_BASE_URL}/farms/${farmId}/marketplace/products`, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: fd,
      });
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        const detail = Array.isArray(body.details) && body.details.length ? body.details[0] : null;
        throw new Error(detail || body.message || body.error || 'Đăng ký sản phẩm không thành công.');
      }
      const data = await response.json();
      setSuccess(data);
      setForm({ exportId:'', categoryId:'', name:'', description:'', quantity:'', price:'' });
      setFiles([]);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Đăng ký sản phẩm không thành công.');
    } finally { setBusy(false); }
  };

  return <div>
    <h1 className="dashboard-title">Sàn giao dịch</h1>
    <p className="dashboard-subtitle">BICAP-18 · Đăng sản phẩm đã xuất kho lên sàn để Nhà bán lẻ tìm kiếm và đặt mua. Sản phẩm cần được admin duyệt trước khi lên sàn.</p>
    {error && <div style={alertStyle}>{error}</div>}
    {success && (
      <div style={successStyle}>
        <strong>✓ Đã gửi đăng ký sản phẩm lên sàn</strong> — mã đăng ký #{success.id}, trạng thái <em>{success.status}</em> (chờ duyệt).
        <div style={{ display:'flex', gap: 16, alignItems: 'center', marginTop: 12 }}>
          {success.qrImage && <img src={success.qrImage} alt="QR truy xuất" width="80" height="80" style={{ borderRadius: 8 }} />}
          {success.traceHash && <a href={`/trace/${success.traceHash}`} target="_blank" rel="noreferrer" style={{ color: '#34d399', fontSize: 13 }}>Mở trang truy xuất nguồn gốc</a>}
        </div>
      </div>
    )}
    <div style={gridStyle}>
      <form className="glass-panel" style={panelStyle} onSubmit={submit}>
        <h2 style={titleStyle}>Đăng sản phẩm lên sàn</h2>

        <label style={labelStyle}>Lô hàng xuất kho (có QR truy xuất)</label>
        {exports.length ? (
          <select required value={form.exportId} onChange={e => {
            const item = exports.find(x => String(x.id) === e.target.value);
            setForm({ ...form, exportId: e.target.value, quantity: item ? String(item.quantity) : form.quantity });
          }} style={inputStyle}>
            <option value="">Chọn lô hàng</option>
            {exports.map(e => <option key={e.id} value={e.id}>Lô #{e.id} · Mùa vụ #{e.seasonId} · {e.quantity} {e.unit}</option>)}
          </select>
        ) : (
          <p style={{ color: '#94a3b8', fontSize: 13 }}>Chưa có lô hàng nào sẵn sàng đăng sàn. Hãy xuất kho &amp; tạo QR trước (BICAP-16/17).</p>
        )}

        {selectedExport && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginTop: 10, padding: 12, border: '1px solid #334155', borderRadius: 10 }}>
            {selectedExport.qrImage && <img src={selectedExport.qrImage} alt="QR lô hàng" width="64" height="64" style={{ borderRadius: 6 }} />}
            <div style={{ fontSize: 12, color: '#cbd5e1', lineHeight: 1.6 }}>
              <div>Số lượng lô: <strong>{selectedExport.quantity} {selectedExport.unit}</strong></div>
              <div>Kho: {selectedExport.warehouse} · Ngày: {selectedExport.exportDate}</div>
              <a href={`/trace/${selectedExport.traceHash}`} target="_blank" rel="noreferrer" style={{ color: '#34d399' }}>Xem truy xuất</a>
            </div>
          </div>
        )}

        <label style={labelStyle}>Danh mục sản phẩm</label>
        <select required value={form.categoryId} onChange={e => setForm({ ...form, categoryId: e.target.value })} style={inputStyle}>
          <option value="">Chọn danh mục</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.icon} {c.name}</option>)}
        </select>

        <label style={labelStyle}>Tên sản phẩm</label>
        <input required maxLength={255} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} style={inputStyle} placeholder="VD: Cải xanh hữu cơ" />

        <label style={labelStyle}>Mô tả sản phẩm (tối thiểu 50 ký tự)</label>
        <textarea required minLength={50} maxLength={2000} rows={4} value={form.description}
          onChange={e => setForm({ ...form, description: e.target.value })} style={inputStyle}
          placeholder="Mô tả nguồn gốc, quy trình canh tác, chất lượng sản phẩm…" />

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div>
            <label style={labelStyle}>Số lượng bán</label>
            <input required min="0.01" step="0.01" type="number" value={form.quantity}
              onChange={e => setForm({ ...form, quantity: e.target.value })} style={inputStyle} />
          </div>
          <div>
            <label style={labelStyle}>Đơn giá dự kiến (VND)</label>
            <input required min="1" step="1000" type="number" value={form.price}
              onChange={e => setForm({ ...form, price: e.target.value })} style={inputStyle} placeholder="VD: 15000" />
          </div>
        </div>

        <label style={labelStyle}>Hình ảnh sản phẩm (1–10 ảnh, JPG/PNG tối đa 5MB)</label>
        <input required type="file" accept="image/jpeg,image/png" multiple
          onChange={e => setFiles(Array.from(e.target.files || []))} style={inputStyle} />
        {files.length > 0 && <p style={{ fontSize: 12, color: '#94a3b8', marginTop: 6 }}>Đã chọn {files.length} ảnh.</p>}

        <button disabled={busy || !exports.length} style={buttonStyle}>
          {busy ? 'Đang đăng ký…' : 'Đăng ký đẩy lên sàn'}
        </button>
      </form>

      <section className="glass-panel" style={panelStyle}>
        <h2 style={titleStyle}>Hướng dẫn</h2>
        <ul style={{ listStyle: 'none', padding: 0, color: '#cbd5e1', fontSize: 14, lineHeight: 1.9 }}>
          <li>① Chọn lô hàng đã xuất kho và có mã QR truy xuất (trạng thái READY).</li>
          <li>② Điền tên, mô tả ≥ 50 ký tự, số lượng và đơn giá dự kiến.</li>
          <li>③ Tải lên ít nhất 1 ảnh sản phẩm (tối đa 10).</li>
          <li>④ Nhấn <strong>Đăng ký đẩy lên sàn</strong> — sản phẩm ở trạng thái <strong>PENDING_REVIEW</strong> (chờ duyệt).</li>
          <li>⑤ Sau khi admin duyệt, Nhà bán lẻ có thể tìm kiếm &amp; đặt mua trên sàn.</li>
        </ul>
      </section>
    </div>
  </div>;
}

const gridStyle:React.CSSProperties={display:'grid',gridTemplateColumns:'minmax(320px,460px) minmax(300px,1fr)',gap:24,alignItems:'start'};
const panelStyle:React.CSSProperties={padding:24};
const titleStyle:React.CSSProperties={color:'#fff',fontSize:20,margin:'0 0 18px'};
const labelStyle:React.CSSProperties={display:'block',color:'#cbd5e1',fontSize:13,margin:'13px 0 6px'};
const inputStyle:React.CSSProperties={width:'100%',boxSizing:'border-box',padding:'11px 12px',borderRadius:8,border:'1px solid #334155',background:'#111827',color:'#fff'};
const buttonStyle:React.CSSProperties={width:'100%',padding:12,marginTop:20,border:0,borderRadius:8,background:'#10b981',color:'#fff',fontWeight:700,cursor:'pointer'};
const alertStyle:React.CSSProperties={padding:12,margin:'12px 0',border:'1px solid #ef4444',borderRadius:8,color:'#fecaca',background:'rgba(239,68,68,.12)'};
const successStyle:React.CSSProperties={padding:14,margin:'12px 0',border:'1px solid #10b981',borderRadius:8,color:'#d1fae5',background:'rgba(16,185,129,.12)'};
