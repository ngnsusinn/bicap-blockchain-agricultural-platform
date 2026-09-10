import { useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import {
  panelStyle, titleStyle, alertStyle, successStyle,
  buttonStyle, labelStyle, inputStyle,
} from '../FarmManager/ui';

export default function ShippingNotificationsPage() {
  const [form, setForm] = useState({ target: 'BOTH', title: '', content: '', sendEmail: false });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const response = await fetch(`${API_BASE_URL}/notifications/broadcast`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(form),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(body.message || 'Không thể gửi thông báo.');
      setNotice(`Đã gửi thông báo tới ${body.recipientCount ?? 0} tài khoản.`);
      setForm({ target: 'BOTH', title: '', content: '', sendEmail: false });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể gửi thông báo.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h1 className="dashboard-title">Gửi thông báo</h1>
      <p className="dashboard-subtitle">Gửi thông tin vận chuyển tới Farm Manager và Nhà bán lẻ.</p>
      {error && <div style={alertStyle}>{error}</div>}
      {notice && <div style={successStyle}>{notice}</div>}

      <form className="glass-panel" style={{ ...panelStyle, maxWidth: 720 }} onSubmit={submit}>
        <h2 style={titleStyle}>Thông báo mới</h2>
        <label style={labelStyle}>Người nhận</label>
        <select
          value={form.target}
          onChange={event => setForm(current => ({ ...current, target: event.target.value }))}
          style={inputStyle}
        >
          <option value="BOTH">Farm Manager và Nhà bán lẻ</option>
          <option value="FARM_MANAGER">Chỉ Farm Manager</option>
          <option value="RETAILER">Chỉ Nhà bán lẻ</option>
        </select>

        <label style={labelStyle}>Tiêu đề *</label>
        <input
          required
          maxLength={120}
          value={form.title}
          onChange={event => setForm(current => ({ ...current, title: event.target.value }))}
          style={inputStyle}
          placeholder="Ví dụ: Cập nhật lịch giao hàng"
        />

        <label style={labelStyle}>Nội dung *</label>
        <textarea
          required
          minLength={10}
          maxLength={4000}
          rows={7}
          value={form.content}
          onChange={event => setForm(current => ({ ...current, content: event.target.value }))}
          style={{ ...inputStyle, resize: 'vertical' }}
          placeholder="Nhập nội dung thông báo vận chuyển..."
        />

        <label style={{ ...labelStyle, display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={form.sendEmail}
            onChange={event => setForm(current => ({ ...current, sendEmail: event.target.checked }))}
          />
          Gửi thêm email nếu tài khoản có cấu hình email
        </label>

        <button type="submit" disabled={busy} style={buttonStyle}>
          {busy ? 'Đang gửi…' : '📨 Gửi thông báo'}
        </button>
      </form>
    </div>
  );
}
