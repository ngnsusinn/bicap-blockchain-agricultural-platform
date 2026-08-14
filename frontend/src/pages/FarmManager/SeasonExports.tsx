import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

type ExportItem = { id:number; seasonId:number; quantity:number; unit:string; exportDate:string;
  warehouse:string; status:string; transactionHash?:string; traceHash:string; qrImage?:string };
type Season = { id:number; name?:string; harvestedQuantity?:number; harvestUnit?:string };

export default function SeasonExports({ farmId }: { farmId?: number }) {
  const [items, setItems] = useState<ExportItem[]>([]);
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ seasonId:'', quantity:'', unit:'kg', exportDate:new Date().toISOString().slice(0,10), warehouse:'' });

  const load = async () => {
    if (!farmId) return;
    const headers = getAuthHeaders();
    const [exportsResult, seasonsResult] = await Promise.all([
      fetch(`${API_BASE_URL}/farms/${farmId}/exports`, { headers }),
      fetch(`${API_BASE_URL}/farms/${farmId}/seasons?status=HARVESTED`, { headers })
    ]);
    if (exportsResult.ok) setItems(await exportsResult.json());
    if (seasonsResult.ok) {
      const body = await seasonsResult.json();
      setSeasons(Array.isArray(body) ? body : (body.content || []));
    }
  };
  useEffect(() => { load().catch(() => setError('Không thể tải dữ liệu xuất kho.')); }, [farmId]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError('');
    if (!farmId) { setError('Chưa xác định được nông trại của tài khoản.'); return; }
    setBusy(true);
    try {
      const response = await fetch(`${API_BASE_URL}/farms/${farmId}/seasons/${form.seasonId}/export`, {
        method:'POST', headers:{ ...getAuthHeaders(), 'Content-Type':'application/json', 'X-Idempotency-Key':crypto.randomUUID() },
        body:JSON.stringify({ quantity:Number(form.quantity), unit:form.unit, exportDate:form.exportDate, warehouse:form.warehouse })
      });
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || body.error || 'Xuất kho không thành công.');
      }
      setForm(v => ({ ...v, quantity:'', warehouse:'' }));
      await load();
    } catch (e) { setError(e instanceof Error ? e.message : 'Xuất kho không thành công.'); }
    finally { setBusy(false); }
  };

  return <div>
    <h1 className="dashboard-title">Xuất kho & QR truy xuất</h1>
    <p className="dashboard-subtitle">BICAP-16/17 · Mỗi lần xuất tạo một mã QR riêng sau khi ghi nhận blockchain.</p>
    {error && <div style={alertStyle}>{error}</div>}
    <div style={gridStyle}>
      <form className="glass-panel" style={panelStyle} onSubmit={submit}>
        <h2 style={titleStyle}>Tạo lần xuất kho</h2>
        <label style={labelStyle}>Mùa vụ đã thu hoạch</label>
        {seasons.length ? <select required value={form.seasonId} onChange={e=>setForm({...form,seasonId:e.target.value})} style={inputStyle}>
          <option value="">Chọn mùa vụ</option>{seasons.map(s=><option key={s.id} value={s.id}>{s.name || `Mùa vụ #${s.id}`}</option>)}
        </select> : <input required min="1" type="number" placeholder="Mã mùa vụ HARVESTED" value={form.seasonId} onChange={e=>setForm({...form,seasonId:e.target.value})} style={inputStyle}/>} 
        <label style={labelStyle}>Số lượng</label>
        <div style={{display:'grid',gridTemplateColumns:'2fr 1fr',gap:10}}>
          <input required min="0.01" step="0.01" type="number" value={form.quantity} onChange={e=>setForm({...form,quantity:e.target.value})} style={inputStyle}/>
          <input required maxLength={30} value={form.unit} onChange={e=>setForm({...form,unit:e.target.value})} style={inputStyle}/>
        </div>
        <label style={labelStyle}>Ngày xuất</label>
        <input required max={new Date().toISOString().slice(0,10)} type="date" value={form.exportDate} onChange={e=>setForm({...form,exportDate:e.target.value})} style={inputStyle}/>
        <label style={labelStyle}>Kho xuất</label>
        <input required maxLength={255} value={form.warehouse} onChange={e=>setForm({...form,warehouse:e.target.value})} style={inputStyle}/>
        <button disabled={busy} style={buttonStyle}>{busy ? 'Đang ghi nhận…' : 'Xuất kho & tạo QR'}</button>
      </form>
      <section className="glass-panel" style={panelStyle}>
        <h2 style={titleStyle}>Các lần xuất kho</h2>
        {!items.length && <p style={{color:'#94a3b8'}}>Chưa có lần xuất kho nào.</p>}
        {items.map(item=><article key={item.id} style={cardStyle}>
          <div style={{display:'flex',justifyContent:'space-between',gap:12}}><strong>Lô xuất #{item.id}</strong><span style={badgeStyle(item.status)}>{item.status}</span></div>
          <p>Mùa vụ #{item.seasonId} · {item.quantity} {item.unit}</p><p>{item.exportDate} · {item.warehouse}</p>
          {item.transactionHash && <p style={hashStyle}>TX: {item.transactionHash}</p>}
          {item.qrImage && <div style={{display:'flex',alignItems:'center',gap:16}}><img src={item.qrImage} alt={`QR lô ${item.id}`} width="112" height="112"/>
            <div><a href={`/trace/${item.traceHash}`} target="_blank" rel="noreferrer" style={{color:'#34d399'}}>Mở trang truy xuất</a><br/>
            <a href={item.qrImage} download={`bicap-export-${item.id}.png`} style={{color:'#7dd3fc'}}>Tải QR PNG</a></div></div>}
        </article>)}
      </section>
    </div>
  </div>;
}

const gridStyle:React.CSSProperties={display:'grid',gridTemplateColumns:'minmax(300px,420px) minmax(360px,1fr)',gap:24,alignItems:'start'};
const panelStyle:React.CSSProperties={padding:24}; const titleStyle:React.CSSProperties={color:'#fff',fontSize:20,margin:'0 0 18px'};
const labelStyle:React.CSSProperties={display:'block',color:'#cbd5e1',fontSize:13,margin:'13px 0 6px'};
const inputStyle:React.CSSProperties={width:'100%',boxSizing:'border-box',padding:'11px 12px',borderRadius:8,border:'1px solid #334155',background:'#111827',color:'#fff'};
const buttonStyle:React.CSSProperties={width:'100%',padding:12,marginTop:20,border:0,borderRadius:8,background:'#10b981',color:'#fff',fontWeight:700,cursor:'pointer'};
const alertStyle:React.CSSProperties={padding:12,margin:'12px 0',border:'1px solid #ef4444',borderRadius:8,color:'#fecaca',background:'rgba(239,68,68,.12)'};
const cardStyle:React.CSSProperties={padding:16,marginTop:12,border:'1px solid #334155',borderRadius:10,color:'#cbd5e1'};
const hashStyle:React.CSSProperties={fontFamily:'monospace',fontSize:11,overflowWrap:'anywhere',color:'#94a3b8'};
const badgeStyle=(status:string):React.CSSProperties=>({fontSize:11,padding:'4px 8px',borderRadius:999,color:status==='READY'?'#6ee7b7':'#fcd34d',background:status==='READY'?'rgba(16,185,129,.15)':'rgba(245,158,11,.15)'});
