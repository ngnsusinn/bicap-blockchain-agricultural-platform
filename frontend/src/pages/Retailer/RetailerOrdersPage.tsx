import { useCallback, useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

type Order = {
  id: number; status: string; productName: string; farmName: string; quantity: number;
  price: number; totalAmount: number; deliveryAddr: string; desiredDeliveryDate?: string;
  depositRate: number; depositAmount?: number;
};
type Deposit = { orderId: number; paymentCode: string; bankName: string; accountNumber: string; depositAmount: number };
const fmt = (value: number) => `${new Intl.NumberFormat('vi-VN').format(value)} ₫`;

export default function RetailerOrdersPage() {
  const [items, setItems] = useState<Order[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [pay, setPay] = useState<Deposit | null>(null);

  const load = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/orders/my`, { headers: getAuthHeaders() });
      const body = await response.json().catch(() => []);
      if (!response.ok) throw new Error(body.message || 'Không thể tải đơn mua.');
      setItems(body); setError('');
      return body as Order[];
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Không thể tải đơn mua.');
      return [];
    }
  }, []);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    if (!pay) return;
    const checkPayment = async () => {
      const latest = await load();
      if (latest.find((item) => item.id === pay.orderId)?.status === 'DEPOSIT_PAID') {
        setMessage(`Đơn #${pay.orderId} đã được xác nhận thanh toán đặt cọc.`);
        setPay(null);
      }
    };
    void checkPayment();
    const timer = window.setInterval(() => void checkPayment(), 5000);
    return () => window.clearInterval(timer);
  }, [load, pay]);

  const deposit = async (id: number) => {
    setError(''); setMessage('');
    const response = await fetch(`${API_BASE_URL}/orders/deposit`, {
      method: 'POST', headers: getAuthHeaders(), body: JSON.stringify({ orderId: id }),
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) { setError(body.message || 'Không thể tạo thanh toán.'); return; }
    setPay(body);
  };

  return <section style={{ color: '#e2e8f0' }}>
    <h1>Đơn mua của tôi</h1>
    <p style={muted}>Theo dõi yêu cầu mua và thanh toán đặt cọc sau khi trang trại chấp nhận.</p>
    {error && <p style={{ color: '#fca5a5' }}>{error}</p>}
    {message && <p style={{ color: '#6ee7b7' }}>{message}</p>}
    <div style={{ display: 'grid', gap: 14 }}>{items.map((order) => <article key={order.id} style={card}>
      <div><b>#{order.id} · {order.productName}</b><p style={muted}>{order.farmName} · Giao ngày {order.desiredDeliveryDate || '—'}</p><p>{order.quantity} × {fmt(order.price)} = <strong>{fmt(order.totalAmount)}</strong></p><p style={muted}>Nhận tại: {order.deliveryAddr}</p></div>
      <div style={{ textAlign: 'right' }}><span style={badge}>{order.status}</span>{order.status === 'ACCEPTED' && <><p style={muted}>Cọc 30%: {fmt(order.depositAmount ?? order.totalAmount * (order.depositRate || 0.3))}</p><button style={button} onClick={() => deposit(order.id)}>Thanh toán đặt cọc</button></>}</div>
    </article>)}</div>
    {pay && <div style={overlay}><div style={modal}>
      <button style={{ float: 'right' }} onClick={() => setPay(null)}>×</button><h2>Thông tin chuyển khoản</h2>
      <p>Ngân hàng: <b>{pay.bankName}</b></p><p>Số tài khoản: <b>{pay.accountNumber}</b></p><p>Số tiền: <b>{fmt(pay.depositAmount)}</b></p><p>Nội dung: <b style={{ color: '#34d399' }}>{pay.paymentCode}</b></p>
      <p style={muted}>Hệ thống xác nhận tự động qua SePay. Vui lòng chuyển đúng số tiền và nội dung.</p><p style={{ ...muted, color: '#7dd3fc' }}>Màn hình tự kiểm tra mỗi 5 giây và sẽ đóng khi thanh toán được xác nhận.</p>
    </div></div>}
  </section>;
}

const card: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', gap: 20, padding: 20, border: '1px solid #334155', borderRadius: 14, background: '#0f172a' };
const muted: React.CSSProperties = { color: '#94a3b8', fontSize: 13 };
const badge: React.CSSProperties = { padding: '5px 10px', borderRadius: 999, background: 'rgba(14,165,233,.15)', color: '#7dd3fc', fontSize: 12 };
const button: React.CSSProperties = { border: 0, borderRadius: 8, padding: '10px 14px', background: '#10b981', color: '#fff', fontWeight: 700 };
const overlay: React.CSSProperties = { position: 'fixed', inset: 0, display: 'grid', placeItems: 'center', background: 'rgba(0,0,0,.75)', zIndex: 99 };
const modal: React.CSSProperties = { width: 'min(460px,90vw)', padding: 28, borderRadius: 18, background: '#0f172a', border: '1px solid #334155' };
