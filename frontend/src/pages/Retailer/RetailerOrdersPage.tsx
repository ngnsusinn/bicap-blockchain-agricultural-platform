import { useCallback, useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

type Order = {
  id:number; status:string; createdAt?:string; productName?:string; productImage?:string;
  farmName?:string; seasonName?:string; quantity:number; price:number; totalAmount:number;
  deliveryAddr?:string; desiredDeliveryDate?:string; notes?:string; depositRate?:number;
  depositAmount?:number; depositExpiresAt?:string; acceptedAt?:string; deliveredAt?:string;
  completedAt?:string; rejectReason?:string; cancelledReason?:string; cancelRequestedAt?:string;
};
type Deposit = { orderId:number; paymentCode:string; bankName:string; accountNumber:string; depositAmount:number };

const statusMeta:Record<string,{label:string;color:string}> = {
  PENDING:{label:'Chờ xử lý',color:'#fbbf24'}, ACCEPTED:{label:'Đã chấp nhận',color:'#38bdf8'},
  REJECTED:{label:'Bị từ chối',color:'#fb7185'}, DEPOSIT_PAID:{label:'Đã đặt cọc',color:'#a78bfa'},
  CANCEL_REQUESTED:{label:'Chờ Admin duyệt hủy',color:'#fb923c'}, IN_TRANSIT:{label:'Đang vận chuyển',color:'#22d3ee'},
  DELIVERED:{label:'Đã giao hàng',color:'#2dd4bf'}, COMPLETED:{label:'Hoàn thành',color:'#4ade80'},
  CANCELLED:{label:'Đã hủy',color:'#94a3b8'},
};
const filters = [
  ['', 'Tất cả'], ['PENDING','Chờ xử lý'], ['ACCEPTED','Đã chấp nhận'],
  ['DEPOSIT_PAID','Đã đặt cọc'], ['IN_TRANSIT','Đang vận chuyển'], ['DELIVERED','Đã giao'], ['COMPLETED','Hoàn thành'],
  ['CANCEL_REQUESTED','Chờ duyệt hủy'],
  ['CANCELLED','Đã hủy'], ['REJECTED','Bị từ chối'],
];
const money=(v?:number)=>v==null?'—':`${new Intl.NumberFormat('vi-VN').format(v)} ₫`;
const date=(v?:string)=>v?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',...(v.includes('T')?{timeStyle:'short' as const}:{})}).format(new Date(v)):'—';
const badge=(status:string)=>{const m=statusMeta[status]||{label:status,color:'#cbd5e1'};return <span style={{...s.badge,color:m.color}}>{m.label}</span>};

export default function RetailerOrdersPage(){
  const [items,setItems]=useState<Order[]>([]),[filter,setFilter]=useState('');
  const [loading,setLoading]=useState(true),[error,setError]=useState(''),[message,setMessage]=useState('');
  const [detail,setDetail]=useState<Order|null>(null),[cancel,setCancel]=useState<Order|null>(null);
  const [reason,setReason]=useState(''),[saving,setSaving]=useState(false),[pay,setPay]=useState<Deposit|null>(null);

  const load=useCallback(async(status=filter)=>{setLoading(true);try{
    const q=status?`?status=${encodeURIComponent(status)}`:'';
    const r=await fetch(`${API_BASE_URL}/orders/my${q}`,{headers:getAuthHeaders()});
    const b=await r.json().catch(()=>[]);if(!r.ok)throw new Error(b.message||'Không thể tải lịch sử đơn mua.');
    setItems(b);setError('');return b as Order[];
  }catch(e){setError(e instanceof Error?e.message:'Không thể tải lịch sử đơn mua.');return []}finally{setLoading(false)}},[filter]);
  useEffect(()=>{void load(filter)},[filter,load]);
  useEffect(()=>{if(!pay)return;const timer=window.setInterval(async()=>{const latest=await load(filter);if(latest.find(o=>o.id===pay.orderId)?.status==='DEPOSIT_PAID'){setMessage(`Đơn #${pay.orderId} đã được xác nhận đặt cọc.`);setPay(null)}},5000);return()=>clearInterval(timer)},[filter,load,pay]);

  const openDetail=async(id:number)=>{setError('');const r=await fetch(`${API_BASE_URL}/orders/my/${id}`,{headers:getAuthHeaders()});const b=await r.json().catch(()=>({}));if(!r.ok){setError(b.message||'Không thể tải chi tiết đơn.');return}setDetail(b)};
  const deposit=async(id:number)=>{setError('');const r=await fetch(`${API_BASE_URL}/orders/deposit`,{method:'POST',headers:getAuthHeaders(),body:JSON.stringify({orderId:id})});const b=await r.json().catch(()=>({}));if(!r.ok){setError(b.message||'Không thể tạo thanh toán.');return}setPay(b)};
  const cancelOrder=async()=>{if(!cancel||!reason.trim()){setError('Vui lòng nhập lý do hủy đơn.');return}setSaving(true);setError('');try{
    const r=await fetch(`${API_BASE_URL}/orders/${cancel.id}/cancel`,{method:'PUT',headers:getAuthHeaders(),body:JSON.stringify({reason:reason.trim()})});
    const b=await r.json().catch(()=>({}));if(!r.ok)throw new Error(b.message||'Không thể hủy đơn.');
    setMessage(b.status==='CANCEL_REQUESTED'?`Đã gửi yêu cầu hủy đơn #${cancel.id} đến Admin.`:`Đã hủy đơn #${cancel.id}. Farm Manager sẽ nhận được thông báo.`);setCancel(null);setReason('');setDetail(d=>d?.id===b.id?b:d);await load(filter);
  }catch(e){setError(e instanceof Error?e.message:'Không thể hủy đơn.')}finally{setSaving(false)}};

  return <section style={{color:'#e2e8f0'}}>
    <div style={s.heading}><div><h1 style={{marginBottom:7}}>Lịch sử đơn mua</h1><p style={s.muted}>Theo dõi, xem chi tiết và quản lý các yêu cầu mua nông sản.</p></div><span style={s.badge}>{loading?'Đang tải...':`${items.length} đơn hàng`}</span></div>
    <div style={s.filters}>{filters.map(([value,label])=><button key={value||'all'} style={{...s.filter,...(filter===value?s.filterActive:{})}} onClick={()=>setFilter(value)}>{label}</button>)}</div>
    {error&&<p role="alert" style={s.error}>{error}</p>}{message&&<p role="status" style={s.success}>{message}</p>}
    {!loading&&!items.length&&<div style={s.empty}><div style={{fontSize:34}}>📦</div><h3>Chưa có đơn hàng</h3><p style={s.muted}>Không có đơn phù hợp với trạng thái đang chọn.</p></div>}
    <div style={{display:'grid',gap:14}}>{items.map(o=><article key={o.id} style={s.card}>
      <div style={{display:'flex',gap:15,alignItems:'center',flex:'1 1 500px'}}>
        <div style={s.thumb}>{o.productImage?<img src={`${API_BASE_URL.replace(/\/api$/,'')}${o.productImage}`} alt={o.productName||'Nông sản'} style={{width:'100%',height:'100%',objectFit:'cover'}}/>:'🌿'}</div>
        <div><div style={{display:'flex',gap:9,alignItems:'center',flexWrap:'wrap'}}><strong>#{o.id} · {o.productName||'Sản phẩm'}</strong>{badge(o.status)}</div>
          <p style={s.muted}>{o.farmName||'Chưa có thông tin trang trại'} · Tạo {date(o.createdAt)}</p>
          <p style={{margin:'6px 0'}}>{o.quantity} × {money(o.price)} = <b style={{color:'#6ee7b7'}}>{money(o.totalAmount)}</b></p>
          <p style={s.muted}>Nhận tại: {o.deliveryAddr||'—'} · Ngày mong muốn: {date(o.desiredDeliveryDate)}</p>
        </div>
      </div>
      <div style={s.actions}><button style={s.secondary} onClick={()=>void openDetail(o.id)}>Xem chi tiết</button>{o.status==='ACCEPTED'&&<button style={s.primary} onClick={()=>void deposit(o.id)}>Thanh toán đặt cọc</button>}{['PENDING','ACCEPTED','DEPOSIT_PAID'].includes(o.status)&&<button style={s.danger} onClick={()=>{setCancel(o);setReason('');setError('')}}>{o.status==='DEPOSIT_PAID'?'Yêu cầu hủy':'Hủy đơn'}</button>}</div>
    </article>)}</div>

    {detail&&<Detail order={detail} close={()=>setDetail(null)} cancel={()=>{setCancel(detail);setReason('')}}/>}
    {cancel&&<Modal close={()=>setCancel(null)} title={`${cancel.status==='DEPOSIT_PAID'?'Yêu cầu hủy':'Hủy đơn'} #${cancel.id}`}><p style={s.muted}>{cancel.status==='DEPOSIT_PAID'?'Đơn đã đặt cọc nên yêu cầu sẽ được gửi Admin xem xét.':'Lý do hủy là bắt buộc và sẽ được gửi cho Farm Manager.'}</p><label style={s.label}>Lý do hủy *</label><textarea rows={4} maxLength={1000} value={reason} onChange={e=>setReason(e.target.value)} placeholder="Ví dụ: Thay đổi kế hoạch nhập hàng..." style={s.textarea}/><p style={{...s.muted,textAlign:'right'}}>{reason.length}/1000</p><div style={s.modalActions}><button style={s.secondary} onClick={()=>setCancel(null)}>Giữ đơn</button><button style={s.danger} disabled={saving||!reason.trim()} onClick={()=>void cancelOrder()}>{saving?'Đang gửi...':cancel.status==='DEPOSIT_PAID'?'Gửi yêu cầu':'Xác nhận hủy'}</button></div></Modal>}
    {pay&&<Modal close={()=>setPay(null)} title="Thông tin chuyển khoản"><p>Ngân hàng: <b>{pay.bankName}</b></p><p>Số tài khoản: <b>{pay.accountNumber}</b></p><p>Số tiền: <b>{money(pay.depositAmount)}</b></p><p>Nội dung: <b style={{color:'#34d399'}}>{pay.paymentCode}</b></p><p style={s.muted}>Hệ thống kiểm tra mỗi 5 giây và tự đóng khi khoản cọc được xác nhận.</p></Modal>}
  </section>
}

function Detail({order,close,cancel}:{order:Order;close:()=>void;cancel:()=>void}){
  const stopped=['CANCELLED','REJECTED','CANCEL_REQUESTED'].includes(order.status);
  const steps=[
    ['Đã tạo yêu cầu',true,order.createdAt],
    ['Trang trại chấp nhận',['ACCEPTED','DEPOSIT_PAID','CANCEL_REQUESTED','IN_TRANSIT','DELIVERED','COMPLETED'].includes(order.status),order.acceptedAt],
    ['Đã đặt cọc / chờ vận chuyển',['DEPOSIT_PAID','CANCEL_REQUESTED','IN_TRANSIT','DELIVERED','COMPLETED'].includes(order.status),undefined],
    ['Đang vận chuyển',['IN_TRANSIT','DELIVERED','COMPLETED'].includes(order.status),undefined],
    ['Đã giao hàng',['DELIVERED','COMPLETED'].includes(order.status),order.deliveredAt],
    ['Hoàn thành',order.status==='COMPLETED',order.completedAt],
  ] as [string,boolean,string|undefined][];
  return <Modal close={close} title={`Chi tiết đơn #${order.id}`} wide><div style={{textAlign:'right'}}>{badge(order.status)}</div><div style={s.grid}>
    <Box title="Sản phẩm"><Info k="Tên sản phẩm" v={order.productName}/><Info k="Mùa vụ" v={order.seasonName}/><Info k="Số lượng" v={`${order.quantity}`}/><Info k="Đơn giá" v={money(order.price)}/><Info k="Tổng tiền" v={money(order.totalAmount)} hi/></Box>
    <Box title="Trang trại & giao hàng"><Info k="Trang trại" v={order.farmName}/><Info k="Địa chỉ nhận" v={order.deliveryAddr}/><Info k="Ngày giao mong muốn" v={date(order.desiredDeliveryDate)}/><Info k="Ghi chú" v={order.notes||'Không có'}/></Box>
  </div>{(order.depositAmount!=null||order.depositExpiresAt)&&<Box title="Đặt cọc"><Info k="Số tiền đặt cọc" v={money(order.depositAmount)} hi/><Info k="Hạn thanh toán" v={date(order.depositExpiresAt)}/></Box>}
  {(order.cancelledReason||order.rejectReason)&&<div style={s.reason}><b>{order.cancelledReason?'Lý do hủy đơn':'Lý do từ chối'}</b><p>{order.cancelledReason||order.rejectReason}</p></div>}
  <h3>Tiến trình đơn hàng</h3>{stopped?<div style={s.reason}>{order.status==='CANCEL_REQUESTED'?'Đang chờ Admin xem xét yêu cầu hủy.':`Tiến trình đã dừng vì đơn ${order.status==='CANCELLED'?'đã bị hủy':'bị từ chối'}.`}</div>:<div>{steps.map(([label,reached,at])=><div key={label} style={s.step}><span style={{...s.dot,background:reached?'#10b981':'#334155'}}/><div><b style={{color:reached?'#e2e8f0':'#64748b'}}>{label}</b>{at&&<p style={s.muted}>{date(at)}</p>}</div></div>)}</div>}
  <p style={s.muted}>Chi tiết tài xế, vị trí và ETA thuộc màn hình theo dõi shipment (BICAP-49).</p><div style={s.modalActions}>{['PENDING','ACCEPTED','DEPOSIT_PAID'].includes(order.status)&&<button style={s.danger} onClick={cancel}>{order.status==='DEPOSIT_PAID'?'Yêu cầu hủy':'Hủy đơn'}</button>}<button style={s.secondary} onClick={close}>Đóng</button></div></Modal>
}
function Modal({title,close,wide=false,children}:{title:string;close:()=>void;wide?:boolean;children:React.ReactNode}){return <div style={s.overlay} role="dialog" aria-modal="true"><div style={{...s.modal,...(wide?{width:'min(760px,94vw)',maxHeight:'90vh',overflowY:'auto'}:{})}}><button style={s.close} onClick={close} aria-label="Đóng">×</button><h2>{title}</h2>{children}</div></div>}
function Box({title,children}:{title:string;children:React.ReactNode}){return <section style={s.box}><h3 style={{marginTop:0}}>{title}</h3>{children}</section>}
function Info({k,v,hi=false}:{k:string;v?:string;hi?:boolean}){return <div style={s.info}><span style={s.muted}>{k}</span><b style={{color:hi?'#6ee7b7':'#e2e8f0',textAlign:'right'}}>{v||'—'}</b></div>}

const s:Record<string,React.CSSProperties>={
  heading:{display:'flex',justifyContent:'space-between',gap:16,alignItems:'flex-end',flexWrap:'wrap'}, muted:{color:'#94a3b8',fontSize:13,lineHeight:1.5,margin:'5px 0'},
  badge:{padding:'5px 10px',borderRadius:999,background:'rgba(148,163,184,.14)',fontSize:12,fontWeight:700,whiteSpace:'nowrap'}, filters:{display:'flex',gap:8,overflowX:'auto',padding:'18px 0'},
  filter:{border:'1px solid #334155',borderRadius:999,padding:'8px 13px',background:'#0f172a',color:'#94a3b8',cursor:'pointer',whiteSpace:'nowrap'},filterActive:{color:'#ecfeff',borderColor:'#0891b2',background:'rgba(6,182,212,.16)'},
  card:{display:'flex',justifyContent:'space-between',alignItems:'center',flexWrap:'wrap',gap:20,padding:20,border:'1px solid #334155',borderRadius:14,background:'#0f172a'},thumb:{width:72,height:72,flex:'0 0 72px',borderRadius:12,overflow:'hidden',background:'#172033',display:'grid',placeItems:'center',fontSize:26},
  actions:{display:'flex',justifyContent:'flex-end',gap:9,flexWrap:'wrap'},primary:{border:0,borderRadius:8,padding:'10px 14px',background:'#10b981',color:'#fff',fontWeight:700,cursor:'pointer'},secondary:{border:'1px solid #475569',borderRadius:8,padding:'9px 13px',background:'#1e293b',color:'#e2e8f0',fontWeight:650,cursor:'pointer'},danger:{border:'1px solid rgba(244,63,94,.45)',borderRadius:8,padding:'9px 13px',background:'rgba(244,63,94,.14)',color:'#fda4af',fontWeight:700,cursor:'pointer'},
  error:{padding:'11px 14px',borderRadius:10,color:'#fecaca',background:'rgba(239,68,68,.13)'},success:{padding:'11px 14px',borderRadius:10,color:'#a7f3d0',background:'rgba(16,185,129,.13)'},empty:{textAlign:'center',padding:48,border:'1px dashed #334155',borderRadius:14},
  overlay:{position:'fixed',inset:0,display:'grid',placeItems:'center',padding:16,background:'rgba(0,0,0,.78)',zIndex:99},modal:{position:'relative',width:'min(480px,94vw)',padding:28,borderRadius:18,background:'#0f172a',border:'1px solid #334155',boxShadow:'0 24px 80px rgba(0,0,0,.5)'},close:{position:'absolute',right:18,top:15,border:0,background:'transparent',color:'#cbd5e1',fontSize:24,cursor:'pointer'},
  label:{display:'block',margin:'18px 0 7px',fontWeight:650},textarea:{width:'100%',boxSizing:'border-box',resize:'vertical',borderRadius:10,border:'1px solid #475569',background:'#111827',color:'#f8fafc',padding:12,font:'inherit'},modalActions:{display:'flex',justifyContent:'flex-end',gap:10,marginTop:18},
  grid:{display:'grid',gridTemplateColumns:'repeat(auto-fit,minmax(260px,1fr))',gap:14},box:{padding:16,marginTop:12,borderRadius:12,background:'#111827',border:'1px solid #273449'},info:{display:'flex',justifyContent:'space-between',gap:20,padding:'7px 0',borderBottom:'1px solid rgba(148,163,184,.09)'},reason:{padding:14,marginTop:14,borderRadius:10,color:'#fecdd3',background:'rgba(244,63,94,.1)',border:'1px solid rgba(244,63,94,.28)'},step:{display:'grid',gridTemplateColumns:'22px 1fr',gap:10,minHeight:55},dot:{width:11,height:11,borderRadius:'50%',marginTop:5},
};
