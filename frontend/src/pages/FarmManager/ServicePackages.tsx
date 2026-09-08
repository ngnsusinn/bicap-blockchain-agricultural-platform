import React, { useState, useEffect, useCallback } from 'react';
import PaymentModal, { type PaymentData } from '../../components/PaymentModal';
import { getAuthHeaders, isLoggedIn, getCurrentUser, API_BASE_URL } from '../../utils/auth';

interface Package {
  id: number;
  name: string;
  description: string;
  price: number;
  durationDays: number;
  features: string;
  status: string;
}

interface Subscription {
  id: number;
  farmId: number;
  packageName: string;
  startDate: string;
  endDate: string;
  status: string;
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE:          '✅ Đang hoạt động',
  PENDING_PAYMENT: '⏳ Chờ thanh toán',
  EXPIRED:         '⌛ Đã hết hạn',
  CANCELLED:       '✕ Đã huỷ',
};

const STATUS_COLOR: Record<string, { fg: string; bg: string; border: string }> = {
  ACTIVE:          { fg: '#6ee7b7', bg: 'rgba(16,185,129,.15)',  border: 'rgba(16,185,129,.4)'  },
  PENDING_PAYMENT: { fg: '#fcd34d', bg: 'rgba(245,158,11,.15)', border: 'rgba(245,158,11,.4)'  },
  EXPIRED:         { fg: '#94a3b8', bg: 'rgba(148,163,184,.1)', border: 'rgba(148,163,184,.3)' },
  CANCELLED:       { fg: '#fca5a5', bg: 'rgba(239,68,68,.1)',   border: 'rgba(239,68,68,.3)'   },
};

const ServicePackages: React.FC = () => {
  const [packages, setPackages]                     = useState<Package[]>([]);
  const [subscriptions, setSubscriptions]           = useState<Subscription[]>([]);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [paymentData, setPaymentData]               = useState<PaymentData | null>(null);
  const [loading, setLoading]                       = useState(true);
  const [subscribing, setSubscribing]               = useState(false);
  const [cancellingId, setCancellingId]             = useState<number | null>(null);
  const [error, setError]                           = useState<string | null>(null);
  const [success, setSuccess]                       = useState<string | null>(null);

  const [farmId, setFarmId] = useState<number | undefined>(getCurrentUser()?.farmId);

  const resolveFarmId = useCallback(async () => {
    if (farmId) return;
    try {
      const res = await fetch(`${API_BASE_URL}/farms/my`, { headers: getAuthHeaders() });
      if (res.ok) {
        const farms = await res.json();
        if (Array.isArray(farms) && farms.length > 0) setFarmId(farms[0].id);
      }
    } catch { /* non-fatal */ }
  }, [farmId]);

  const fetchData = useCallback(async () => {
    try {
      const [pkgRes, subRes] = await Promise.all([
        fetch(`${API_BASE_URL}/service-packages`),
        fetch(`${API_BASE_URL}/subscriptions/my`, { headers: getAuthHeaders() }),
      ]);
      if (pkgRes.ok) {
        const data = await pkgRes.json();
        setPackages(Array.isArray(data) ? data : []);
      }
      if (subRes.ok) {
        const data = await subRes.json();
        setSubscriptions(Array.isArray(data) ? data : []);
      }
    } catch {
      setError('Không tải được dữ liệu gói dịch vụ.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); resolveFarmId(); }, [fetchData, resolveFarmId]);

  const activeSub   = subscriptions.find(s => s.status === 'ACTIVE');
  const pendingSub  = subscriptions.find(s => s.status === 'PENDING_PAYMENT');

  const handleSubscribe = async (pkg: Package) => {
    if (!isLoggedIn()) { flash('error', 'Vui lòng đăng nhập để mua gói dịch vụ.'); return; }
    if (!farmId)       { flash('error', 'Không xác định được nông trại. Vui lòng đăng ký nông trại trước.'); return; }
    if (activeSub)     { flash('error', `Bạn đang dùng gói ${activeSub.packageName}. Vui lòng chờ hết hạn trước khi mua mới.`); return; }
    if (pendingSub)    { flash('error', 'Bạn đang có thanh toán chờ xử lý. Vui lòng hoàn tất hoặc huỷ trước.'); return; }
    if (subscribing) return;
    setSubscribing(true);
    try {
      const res = await fetch(`${API_BASE_URL}/subscriptions/purchase`, {
        method: 'POST', headers: getAuthHeaders(),
        body: JSON.stringify({ packageId: pkg.id, farmId }),
      });
      if (res.ok) {
        setPaymentData(await res.json());
        setIsPaymentModalOpen(true);
        await fetchData();
      } else {
        const err = await res.json().catch(() => ({}));
        flash('error', err.message || 'Khởi tạo thanh toán thất bại.');
      }
    } catch { flash('error', 'Lỗi kết nối mạng.'); }
    finally { setSubscribing(false); }
  };

  const handleCancelPending = async (subId: number) => {
    if (!window.confirm('Huỷ đơn thanh toán đang chờ? Bạn có thể mua lại sau.')) return;
    setCancellingId(subId);
    try {
      const res = await fetch(`${API_BASE_URL}/subscriptions/${subId}/cancel`, {
        method: 'PUT', headers: getAuthHeaders(),
      });
      if (res.ok) {
        flash('success', 'Đã huỷ đơn thanh toán. Bạn có thể chọn gói mới.');
        await fetchData();
      } else {
        const err = await res.json().catch(() => ({}));
        flash('error', err.message || 'Huỷ thất bại.');
      }
    } catch { flash('error', 'Lỗi kết nối mạng.'); }
    finally { setCancellingId(null); }
  };

  const flash = (type: 'error' | 'success', msg: string) => {
    if (type === 'error')   { setError(msg);   setTimeout(() => setError(null),   4000); }
    else                    { setSuccess(msg);  setTimeout(() => setSuccess(null), 4000); }
  };

  const getFeatures = (f: string | null | undefined): string[] => {
    if (!f) return [];
    try {
      const parsed = JSON.parse(f);
      return Array.isArray(parsed) ? parsed.filter(item => typeof item === 'string') : [];
    } catch {
      return f.split(',').map(s => s.trim()).filter(Boolean);
    }
  };

  const daysLeft = (endDate: string) => {
    const diff = Math.ceil((new Date(endDate).getTime() - Date.now()) / 86400000);
    return diff > 0 ? `Còn ${diff} ngày` : 'Hết hạn hôm nay';
  };

  if (loading) return (
    <div style={{ padding: 60, textAlign: 'center', color: '#8b5cf6', fontSize: 18 }}>
      Đang tải gói dịch vụ…
    </div>
  );

  return (
    <>
      <style>{`
        @keyframes slideUpFade {
          from { opacity:0; transform:translateY(30px); }
          to   { opacity:1; transform:translateY(0); }
        }
        .pkg-card { transition: transform .3s, box-shadow .3s, border-color .3s; }
        .pkg-card:hover {
          transform: scale(1.02) translateY(-4px);
          box-shadow: 0 20px 40px -10px rgba(139,92,246,.25);
          border-color: rgba(139,92,246,.45) !important;
        }
        .subscribe-btn {
          background: linear-gradient(135deg, #8b5cf6, #06b6d4);
          color:#fff; border:none; padding:14px 20px; border-radius:10px;
          font-size:15px; font-weight:700; cursor:pointer; width:100%;
          margin-top:auto; transition:opacity .2s, transform .2s;
        }
        .subscribe-btn:hover:not(:disabled) { opacity:.9; transform:translateY(-2px); }
        .subscribe-btn:disabled { opacity:.5; cursor:not-allowed; }
      `}</style>

      <h1 className="dashboard-title">Gói Dịch Vụ</h1>
      <p className="dashboard-subtitle">Nâng cấp khả năng quản lý nông trại với các công cụ tích hợp Blockchain.</p>

      {error   && <div style={{ padding:'12px 16px', margin:'12px 0', border:'1px solid #ef4444', borderRadius:8, color:'#fecaca', background:'rgba(239,68,68,.12)' }}>{error}</div>}
      {success && <div style={{ padding:'12px 16px', margin:'12px 0', border:'1px solid #10b981', borderRadius:8, color:'#a7f3d0', background:'rgba(16,185,129,.12)' }}>{success}</div>}

      {/* ── Current / pending subscription banner ── */}
      {(activeSub || pendingSub) && (
        <div style={{
          padding:'20px 24px', marginBottom:28, borderRadius:14,
          background: activeSub ? 'rgba(16,185,129,.08)' : 'rgba(245,158,11,.08)',
          border: `1px solid ${activeSub ? 'rgba(16,185,129,.3)' : 'rgba(245,158,11,.3)'}`,
          display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:12,
        }}>
          <div>
            <div style={{ fontSize:18, fontWeight:700, color:'#fff', marginBottom:4 }}>
              {activeSub ? `📦 Gói hiện tại: ${activeSub.packageName}` : `⏳ Đang chờ thanh toán: ${pendingSub!.packageName}`}
            </div>
            <div style={{ fontSize:13, color:'#94a3b8' }}>
              {activeSub
                ? `Hiệu lực đến ${new Date(activeSub.endDate).toLocaleDateString('vi-VN')}, ${daysLeft(activeSub.endDate)}`
                : 'Vui lòng hoàn tất thanh toán hoặc huỷ để chọn gói khác'}
            </div>
          </div>
          <div style={{ display:'flex', gap:10, alignItems:'center' }}>
            {activeSub && (
              <span style={{
                padding:'6px 14px', borderRadius:20, fontWeight:700, fontSize:13,
                ...STATUS_COLOR['ACTIVE'], border: `1px solid ${STATUS_COLOR['ACTIVE'].border}`,
                color: STATUS_COLOR['ACTIVE'].fg, background: STATUS_COLOR['ACTIVE'].bg,
              }}>ACTIVE</span>
            )}
            {pendingSub && (
              <>
                <button
                  onClick={() => { setPaymentData(null); setIsPaymentModalOpen(true); }}
                  style={{
                    padding:'8px 16px', border:'1px solid rgba(245,158,11,.4)',
                    borderRadius:8, background:'rgba(245,158,11,.1)', color:'#fcd34d',
                    fontWeight:600, cursor:'pointer', fontSize:13,
                  }}
                >💳 Xem thanh toán</button>
                <button
                  onClick={() => handleCancelPending(pendingSub!.id)}
                  disabled={cancellingId === pendingSub!.id}
                  style={{
                    padding:'8px 14px', border:'1px solid rgba(239,68,68,.3)',
                    borderRadius:8, background:'rgba(239,68,68,.08)', color:'#f87171',
                    fontWeight:600, cursor:'pointer', fontSize:13,
                  }}
                >{cancellingId === pendingSub!.id ? 'Đang huỷ…' : '✕ Huỷ'}</button>
              </>
            )}
          </div>
        </div>
      )}

      {/* ── Package grid ── */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(300px,1fr))', gap:28, marginBottom:40 }}>
        {packages.map((pkg, idx) => {
          const isCurrent = activeSub?.packageName === pkg.name;
          const colors = [
            { grad:'linear-gradient(135deg,#6366f1,#8b5cf6)', accent:'#a78bfa' },
            { grad:'linear-gradient(135deg,#0284c7,#06b6d4)', accent:'#38bdf8' },
            { grad:'linear-gradient(135deg,#059669,#10b981)', accent:'#34d399' },
          ][idx % 3];

          return (
            <div key={pkg.id} className="pkg-card" style={{
              background:'rgba(22,23,33,.65)', backdropFilter:'blur(14px)',
              border: isCurrent ? `2px solid ${colors.accent}` : '1px solid rgba(255,255,255,.08)',
              borderRadius:20, padding:'32px 28px',
              display:'flex', flexDirection:'column', position:'relative', overflow:'hidden',
              animation:`slideUpFade .5s ease-out ${idx * .12}s both`,
            }}>
              {isCurrent && (
                <div style={{
                  position:'absolute', top:14, right:14,
                  background:colors.grad, color:'#fff', fontSize:11, fontWeight:700,
                  padding:'4px 10px', borderRadius:20,
                }}>Gói hiện tại</div>
              )}
              <h3 style={{ fontSize:22, fontWeight:800, margin:'0 0 10px', background:colors.grad, WebkitBackgroundClip:'text', WebkitTextFillColor:'transparent' }}>
                {pkg.name}
              </h3>
              <p style={{ fontSize:13, color:'#94a3b8', lineHeight:1.6, marginBottom:20 }}>{pkg.description}</p>
              <div style={{ marginBottom:24 }}>
                <span style={{ fontSize:36, fontWeight:800, color:'#fff' }}>{pkg.price.toLocaleString('vi-VN')}</span>
                <span style={{ fontSize:20, color:colors.accent, fontWeight:700 }}> ₫</span>
                <div style={{ fontSize:13, color:'#64748b', marginTop:4 }}>/ {pkg.durationDays} ngày</div>
              </div>
              <ul style={{ listStyle:'none', padding:0, margin:'0 0 28px', display:'flex', flexDirection:'column', gap:10 }}>
                {getFeatures(pkg.features).map((f, i) => (
                  <li key={i} style={{ display:'flex', gap:10, fontSize:14, color:'#e2e8f0', lineHeight:1.5 }}>
                    <span style={{ color:colors.accent, flexShrink:0 }}>✦</span>{f}
                  </li>
                ))}
              </ul>
              {isCurrent ? (
                <div style={{ textAlign:'center', padding:'13px', borderRadius:10, background:'rgba(255,255,255,.04)', color:'#64748b', fontWeight:600 }}>
                  Đang sử dụng
                </div>
              ) : (
                <button
                  className="subscribe-btn"
                  onClick={() => handleSubscribe(pkg)}
                  disabled={subscribing || !!activeSub || !!pendingSub}
                  title={activeSub ? 'Đã có gói đang hoạt động' : pendingSub ? 'Đang có thanh toán chờ xử lý' : ''}
                >
                  {subscribing ? 'Đang xử lý…' : 'Đăng ký ngay'}
                </button>
              )}
            </div>
          );
        })}
      </div>

      {/* ── Subscription history ── */}
      {subscriptions.length > 0 && (
        <div className="glass-panel" style={{ padding:24 }}>
          <h2 style={{ color:'#fff', fontSize:18, margin:'0 0 16px', fontWeight:700 }}>📋 Lịch sử đăng ký ({subscriptions.length})</h2>
          <div style={{ overflowX:'auto' }}>
            <table style={{ width:'100%', borderCollapse:'collapse', fontSize:13 }}>
              <thead>
                <tr style={{ borderBottom:'1px solid #1e293b' }}>
                  {['Gói dịch vụ','Trạng thái','Ngày bắt đầu','Ngày kết thúc','Hành động'].map(h => (
                    <th key={h} style={{ padding:'10px 14px', textAlign:'left', color:'#64748b', fontWeight:600, whiteSpace:'nowrap' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {[...subscriptions].sort((a,b) => b.id - a.id).map(sub => {
                  const c = STATUS_COLOR[sub.status] || STATUS_COLOR['EXPIRED'];
                  return (
                    <tr key={sub.id} style={{ borderBottom:'1px solid #0f172a' }}>
                      <td style={{ padding:'12px 14px', color:'#fff', fontWeight:600 }}>{sub.packageName}</td>
                      <td style={{ padding:'12px 14px' }}>
                        <span style={{ padding:'4px 10px', borderRadius:99, fontSize:11, fontWeight:700, color:c.fg, background:c.bg, border:`1px solid ${c.border}` }}>
                          {STATUS_LABEL[sub.status] || sub.status}
                        </span>
                      </td>
                      <td style={{ padding:'12px 14px', color:'#94a3b8' }}>
                        {sub.startDate ? new Date(sub.startDate).toLocaleDateString('vi-VN') : '—'}
                      </td>
                      <td style={{ padding:'12px 14px', color:'#94a3b8' }}>
                        {sub.endDate ? new Date(sub.endDate).toLocaleDateString('vi-VN') : '—'}
                        {sub.status === 'ACTIVE' && sub.endDate && (
                          <span style={{ marginLeft:8, color:'#6ee7b7', fontSize:11 }}>{daysLeft(sub.endDate)}</span>
                        )}
                      </td>
                      <td style={{ padding:'12px 14px' }}>
                        {sub.status === 'PENDING_PAYMENT' && (
                          <button
                            onClick={() => handleCancelPending(sub.id)}
                            disabled={cancellingId === sub.id}
                            style={{
                              padding:'5px 12px', border:'1px solid rgba(239,68,68,.3)',
                              borderRadius:6, background:'rgba(239,68,68,.08)', color:'#f87171',
                              cursor:'pointer', fontSize:12, fontWeight:600,
                            }}
                          >{cancellingId === sub.id ? 'Đang huỷ…' : '✕ Huỷ'}</button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <PaymentModal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        paymentData={paymentData}
        onSuccess={fetchData}
      />
    </>
  );
};

export default ServicePackages;