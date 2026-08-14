import { useEffect, useState } from 'react';
import { API_BASE_URL } from '../utils/auth';

export default function TracePage({ hash }: { hash:string }) {
  const [data,setData]=useState<any>(); const [error,setError]=useState('');
  useEffect(()=>{ fetch(`${API_BASE_URL}/trace/${encodeURIComponent(hash)}`).then(async r=>{
    if(!r.ok) throw new Error('Không tìm thấy lô hàng hợp lệ.'); setData(await r.json());
  }).catch(e=>setError(e.message)); },[hash]);
  return <main style={{minHeight:'100vh',padding:24,display:'grid',placeItems:'center',background:'#07120f',color:'#e2e8f0'}}>
    <section className="glass-panel" style={{padding:32,width:'min(720px,100%)'}}><h1 style={{color:'#34d399'}}>BICAP · Truy xuất nguồn gốc</h1>
      {error && <p style={{color:'#fca5a5'}}>{error}</p>}{!data&&!error&&<p>Đang xác thực blockchain…</p>}
      {data&&<><p style={{color:'#6ee7b7'}}>✓ Lô xuất đã được xác thực</p>
        <dl style={{display:'grid',gridTemplateColumns:'160px 1fr',gap:12}}><dt>Mã lô xuất</dt><dd>#{data.id}</dd><dt>Mã mùa vụ</dt><dd>#{data.seasonId}</dd>
          <dt>Số lượng</dt><dd>{data.quantity} {data.unit}</dd><dt>Ngày xuất</dt><dd>{data.exportDate}</dd><dt>Kho xuất</dt><dd>{data.warehouse}</dd>
          <dt>Trạng thái</dt><dd>{data.status}</dd><dt>Transaction hash</dt><dd style={{overflowWrap:'anywhere',fontFamily:'monospace'}}>{data.transactionHash}</dd></dl>
        <p style={{color:'#94a3b8',fontSize:13}}>Thông tin chi tiết mùa vụ, quy trình, hình ảnh và chứng nhận sẽ được tổng hợp từ module mùa vụ khi BICAP-14/15 cung cấp dữ liệu.</p></>}
    </section></main>;
}
