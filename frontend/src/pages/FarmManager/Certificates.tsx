import { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import { panelStyle, titleStyle, alertStyle, cardStyle, badgeStyle } from './ui';

type Cert = { id: number; type: string; fileUrl: string; expiryDate?: string };

const TYPE_LABELS: Record<string, string> = {
  BUSINESS_LICENSE: 'Giấy phép kinh doanh', VIETGAP: 'VietGAP', GLOBALGAP: 'GlobalGAP', OTHER: 'Khác',
};

export default function Certificates({ farmId }: { farmId?: number }) {
  const [items, setItems] = useState<Cert[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!farmId) return;
    fetch(`${API_BASE_URL}/farms/${farmId}/certifications`, { headers: getAuthHeaders() })
      .then(async res => { if (res.ok) setItems(await res.json()); else setError('Không tải được danh sách chứng nhận.'); })
      .catch(() => setError('Lỗi kết nối máy chủ.'));
  }, [farmId]);

  if (!farmId) {
    return <div><h1 className="dashboard-title">Chứng nhận</h1><div style={alertStyle}>Chưa xác định được nông trại của tài khoản.</div></div>;
  }

  const isImage = (url: string) => /\.(png|jpe?g|gif|webp)$/i.test(url);

  return (
    <div>
      <h1 className="dashboard-title">Chứng nhận & giấy phép</h1>
      <p className="dashboard-subtitle">Hồ sơ chứng nhận của nông trại (VietGAP / GlobalGAP / giấy phép kinh doanh).</p>
      {error && <div style={alertStyle}>{error}</div>}

      <section className="glass-panel" style={panelStyle}>
        <h2 style={titleStyle}>Tài liệu ({items.length})</h2>
        {!items.length && <p style={{ color: '#94a3b8' }}>Chưa có tài liệu nào. Vào "Thông tin nông trại" để tải lên (BICAP-9).</p>}
        {items.map(c => (
          <article key={c.id} style={cardStyle}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center' }}>
              <strong>{TYPE_LABELS[c.type] || c.type}</strong>
              <span style={badgeStyle(c.expiryDate && new Date(c.expiryDate) < new Date() ? 'REJECTED' : 'ACTIVE')}>
                {c.expiryDate && new Date(c.expiryDate) < new Date() ? 'HẾT HẠN' : 'CÒN HIỆU LỰC'}
              </span>
            </div>
            {c.expiryDate && <p style={{ fontSize: 12, color: '#94a3b8' }}>Hết hạn: {c.expiryDate}</p>}
            {isImage(c.fileUrl)
              ? <img src={c.fileUrl} alt={c.type} style={{ marginTop: 10, maxWidth: 260, borderRadius: 8, border: '1px solid #334155' }} />
              : <a href={c.fileUrl} target="_blank" rel="noreferrer" style={{ color: '#34d399', display: 'inline-block', marginTop: 8 }}>Mở tài liệu</a>}
          </article>
        ))}
      </section>
    </div>
  );
}
