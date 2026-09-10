import { useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import { panelStyle, titleStyle, labelStyle, inputStyle, buttonStyle, alertStyle, successStyle } from './ui';

export default function Settings() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setNotice('');
    if (form.newPassword.length < 8) { setError('Mật khẩu mới cần tối thiểu 8 ký tự.'); return; }
    if (form.newPassword !== form.confirmPassword) { setError('Xác nhận mật khẩu không khớp.'); return; }
    setBusy(true);
    try {
      const res = await fetch(`${API_BASE_URL}/profile/change-password`, {
        method: 'POST', headers: getAuthHeaders(),
        body: JSON.stringify(form),
      });
      if (!res.ok) { const b = await res.json().catch(() => ({})); throw new Error(b.message || 'Đổi mật khẩu thất bại.'); }
      setNotice('Đã đổi mật khẩu thành công.');
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) { setError(err instanceof Error ? err.message : 'Đổi mật khẩu thất bại.'); }
    finally { setBusy(false); }
  };

  return (
    <div>
      <h1 className="dashboard-title">Cài đặt tài khoản</h1>
      <p className="dashboard-subtitle">Đổi mật khẩu đăng nhập cho tài khoản của bạn.</p>
      {error && <div style={alertStyle}>{error}</div>}
      {notice && <div style={successStyle}>{notice}</div>}

      <form className="glass-panel" style={{ ...panelStyle, maxWidth: 460 }} onSubmit={submit}>
        <h2 style={titleStyle}>Đổi mật khẩu</h2>
        <label style={labelStyle}>Mật khẩu hiện tại</label>
        <input required type="password" value={form.currentPassword} onChange={e => setForm({ ...form, currentPassword: e.target.value })} style={inputStyle} />
        <label style={labelStyle}>Mật khẩu mới</label>
        <input required type="password" minLength={8} value={form.newPassword} onChange={e => setForm({ ...form, newPassword: e.target.value })} style={inputStyle} />
        <label style={labelStyle}>Nhập lại mật khẩu mới</label>
        <input required type="password" minLength={8} value={form.confirmPassword} onChange={e => setForm({ ...form, confirmPassword: e.target.value })} style={inputStyle} />
        <button disabled={busy} style={buttonStyle}>{busy ? 'Đang xử lý…' : 'Đổi mật khẩu'}</button>
      </form>
    </div>
  );
}
