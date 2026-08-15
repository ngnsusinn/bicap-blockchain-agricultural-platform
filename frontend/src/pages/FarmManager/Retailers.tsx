import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

type RetailerPartner = {
  retailerId: number;
  retailerName?: string;
  retailerEmail?: string;
  retailerPhone?: string;
  retailerAddress?: string;
  businessName?: string;
  businessAddress?: string;
  businessType?: string;
  licenseUrl?: string;
  totalOrders: number;
  totalSpent?: number;
  firstOrderAt?: string;
  lastOrderAt?: string;
};

type RetailerTransaction = {
  orderId: number;
  status: string;
  createdAt?: string;
  productId: number;
  productName?: string;
  productImage?: string;
  farmId?: number;
  farmName?: string;
  quantity: number;
  price: number;
  totalAmount?: number;
};

type RetailerDetail = {
  retailer: RetailerPartner;
  transactions: RetailerTransaction[];
};

const STATUS_META: Record<string, { label: string; color: string }> = {
  PENDING: { label: 'Chờ xử lý', color: '#f59e0b' },
  ACCEPTED: { label: 'Đã chấp nhận', color: '#10b981' },
  REJECTED: { label: 'Đã từ chối', color: '#ef4444' },
  DEPOSIT_PAID: { label: 'Đã đặt cọc', color: '#06b6d4' },
};

const BUSINESS_TYPE_LABELS: Record<string, string> = {
  RETAIL_STORE: 'Cửa hàng bán lẻ',
  WHOLESALE: 'Bán sỉ',
  SUPERMARKET: 'Siêu thị',
  OTHER: 'Khác',
};

/**
 * Xem thông tin Nhà bán lẻ đã ký hợp đồng (BICAP-21 / SRS-FM-015).
 * Farm Manager xem danh sách đối tác đã có giao dịch trên nông trại của mình
 * (tên, địa chỉ, giấy phép kinh doanh) và chi tiết kèm lịch sử giao dịch để
 * đánh giá đối tác.
 */
export default function Retailers() {
  const [retailers, setRetailers] = useState<RetailerPartner[]>([]);
  const [detail, setDetail] = useState<RetailerDetail | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const loadList = async () => {
    setError('');
    try {
      const res = await fetch(`${API_BASE_URL}/retailers`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Không thể tải danh sách Nhà bán lẻ.');
      setRetailers(await res.json());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể tải dữ liệu.');
    }
  };

  useEffect(() => {
    loadList().catch(() => setError('Không thể tải dữ liệu.'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openDetail = async (retailer: RetailerPartner) => {
    setBusy(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE_URL}/retailers/${retailer.retailerId}`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Không thể tải chi tiết Nhà bán lẻ.');
      setDetail(await res.json());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể tải chi tiết.');
    } finally {
      setBusy(false);
    }
  };

  if (detail) {
    const r = detail.retailer;
    return (
      <div>
        <button onClick={() => setDetail(null)} style={buttonGhostStyle}>← Quay lại danh sách</button>

        <h1 className="dashboard-title" style={{ marginTop: 12 }}>{r.businessName || r.retailerName}</h1>
        <p className="dashboard-subtitle">
          BICAP-21 · Nhà bán lẻ đã ký hợp đồng · Thông tin chi tiết và lịch sử giao dịch.
        </p>

        {error && <div style={alertStyle}>{error}</div>}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))', gap: 12, marginTop: 16 }}>
          <div style={infoCellStyle}>
            <span style={infoLabelStyle}>Người đại diện</span>
            <div style={{ color: '#e2e8f0', fontSize: 14, fontWeight: 600 }}>{r.retailerName || '—'}</div>
            <div style={{ fontSize: 12, color: '#94a3b8' }}>{r.retailerEmail}</div>
          </div>
          <div style={infoCellStyle}>
            <span style={infoLabelStyle}>Điện thoại</span>
            <div style={{ color: '#e2e8f0', fontSize: 14 }}>{r.retailerPhone || '—'}</div>
          </div>
          <div style={infoCellStyle}>
            <span style={infoLabelStyle}>Loại hình kinh doanh</span>
            <div style={{ color: '#e2e8f0', fontSize: 14 }}>
              {BUSINESS_TYPE_LABELS[r.businessType || ''] || r.businessType || '—'}
            </div>
          </div>
          <div style={infoCellStyle}>
            <span style={infoLabelStyle}>Giấy phép kinh doanh</span>
            <div style={{ fontSize: 13 }}>
              {r.licenseUrl ? (
                <a href={r.licenseUrl} target="_blank" rel="noreferrer" style={{ color: '#38bdf8' }}>Xem giấy phép ↗</a>
              ) : '—'}
            </div>
          </div>
          <div style={infoCellStyle}>
            <span style={infoLabelStyle}>Địa chỉ doanh nghiệp</span>
            <div style={{ color: '#e2e8f0', fontSize: 13 }}>{r.businessAddress || '—'}</div>
            <div style={{ fontSize: 12, color: '#94a3b8' }}>{r.retailerAddress || ''}</div>
          </div>
          <div style={infoCellStyle}>
            <span style={infoLabelStyle}>Tổng kết giao dịch</span>
            <div style={{ color: '#34d399', fontSize: 14, fontWeight: 700 }}>
              {fmt(r.totalSpent)} ₫ · {r.totalOrders} đơn
            </div>
            <div style={{ fontSize: 12, color: '#94a3b8' }}>
              {r.lastOrderAt ? `Giao dịch cuối: ${new Date(r.lastOrderAt).toLocaleDateString('vi-VN')}` : ''}
            </div>
          </div>
        </div>

        <h2 style={{ color: '#fff', fontSize: 16, margin: '24px 0 12px' }}>Lịch sử giao dịch</h2>
        {detail.transactions.length === 0 ? (
          <div className="glass-panel" style={{ padding: 24, textAlign: 'center', color: '#94a3b8' }}>Chưa có giao dịch.</div>
        ) : (
          <div style={{ display: 'grid', gap: 12 }}>
            {detail.transactions.map((t) => {
              const meta = STATUS_META[t.status] || { label: t.status, color: '#94a3b8' };
              return (
                <div key={t.orderId} className="glass-panel" style={{ padding: 16 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                    {t.productImage && (
                      <img src={t.productImage} alt={t.productName} width="40" height="40" style={{ borderRadius: 8, objectFit: 'cover' }} />
                    )}
                    <div style={{ flex: 1, minWidth: 200 }}>
                      <div style={{ color: '#fff', fontWeight: 600, fontSize: 14 }}>
                        {t.productName || `Sản phẩm #${t.productId}`}
                        <span style={{ fontSize: 11, color: meta.color, marginLeft: 10, border: `1px solid ${meta.color}55`, padding: '2px 8px', borderRadius: 10 }}>{meta.label}</span>
                      </div>
                      <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 2 }}>
                        #{t.orderId} · {t.farmName || `Nông trại #${t.farmId}`} · {t.createdAt ? new Date(t.createdAt).toLocaleString('vi-VN') : ''}
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ color: '#e2e8f0', fontSize: 13 }}>{t.quantity} đơn vị × {fmt(t.price)} ₫</div>
                      <div style={{ color: '#34d399', fontSize: 15, fontWeight: 700 }}>{fmt(t.totalAmount ?? t.price * t.quantity)} ₫</div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  return (
    <div>
      <h1 className="dashboard-title">Nhà bán lẻ đã ký hợp đồng</h1>
      <p className="dashboard-subtitle">
        BICAP-21 · Xem thông tin chi tiết của các Nhà bán lẻ đã có giao dịch với nông trại của bạn
        (tên, địa chỉ, giấy phép kinh doanh, lịch sử giao dịch) để đánh giá đối tác.
      </p>

      {error && <div style={alertStyle}>{error}</div>}

      {busy && <div style={{ color: '#94a3b8', padding: '16px 0' }}>Đang tải chi tiết…</div>}

      {!busy && retailers.length === 0 ? (
        <div className="glass-panel" style={{ padding: 32, textAlign: 'center', color: '#94a3b8' }}>
          Chưa có Nhà bán lẻ nào đặt hàng trên nông trại của bạn. Khi Nhà bán lẻ tạo yêu cầu mua,
          họ sẽ xuất hiện tại đây như các đối tác đã ký hợp đồng.
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 14 }}>
          {retailers.map((r) => (
            <button
              key={r.retailerId}
              onClick={() => openDetail(r)}
              style={{ textAlign: 'left', cursor: 'pointer', border: '1px solid rgba(255,255,255,.08)', background: 'rgba(255,255,255,.03)', borderRadius: 12, padding: 18 }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, flexWrap: 'wrap' }}>
                <div style={{ flex: 1, minWidth: 240 }}>
                  <div style={{ color: '#fff', fontWeight: 700, fontSize: 16 }}>
                    {r.businessName || r.retailerName}
                    <span style={{ fontSize: 11, color: '#38bdf8', marginLeft: 10, border: '1px solid rgba(56,189,248,.4)', padding: '2px 8px', borderRadius: 10 }}>
                      {BUSINESS_TYPE_LABELS[r.businessType || ''] || r.businessType || 'Nhà bán lẻ'}
                    </span>
                  </div>
                  <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 2 }}>
                    {r.retailerName ? `Người đại diện: ${r.retailerName} · ` : ''}{r.retailerEmail}
                  </div>
                  {r.retailerAddress && <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 2 }}>📍 {r.retailerAddress}</div>}
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ color: '#34d399', fontSize: 15, fontWeight: 700 }}>{fmt(r.totalSpent)} ₫</div>
                  <div style={{ fontSize: 12, color: '#94a3b8' }}>{r.totalOrders} đơn hàng</div>
                  {r.lastOrderAt && <div style={{ fontSize: 12, color: '#94a3b8' }}>Lần cuối: {new Date(r.lastOrderAt).toLocaleDateString('vi-VN')}</div>}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

const fmt = (n: number | undefined) => n?.toLocaleString('vi-VN') ?? '0';

const infoCellStyle: React.CSSProperties = { background: 'rgba(255,255,255,.03)', border: '1px solid rgba(255,255,255,.06)', borderRadius: 10, padding: '12px 14px' };
const infoLabelStyle: React.CSSProperties = { display: 'block', fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 4 };
const buttonGhostStyle: React.CSSProperties = { padding: '10px 16px', border: '1px solid #334155', borderRadius: 8, background: 'transparent', color: '#94a3b8', fontWeight: 600, cursor: 'pointer' };
const alertStyle: React.CSSProperties = { padding: 12, margin: '12px 0', border: '1px solid #ef4444', borderRadius: 8, color: '#fecaca', background: 'rgba(239,68,68,.12)' };
