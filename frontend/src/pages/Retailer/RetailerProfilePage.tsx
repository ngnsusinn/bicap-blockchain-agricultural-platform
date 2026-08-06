import React, { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';
import type { UserSession } from '../../utils/auth';

interface Props {
  user: UserSession;
  onUserUpdated: (user: UserSession) => void;
}

export default function RetailerProfilePage({ user, onUserUpdated }: Props) {
  const [fullName, setFullName] = useState(user.fullName);
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [avatar, setAvatar] = useState<File | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    fetch(`${API_BASE_URL}/retailer/profile`, { headers: getAuthHeaders() })
      .then(async (response) => {
        if (!response.ok) throw new Error('Không thể tải hồ sơ cá nhân.');
        return response.json();
      })
      .then((data) => {
        setFullName(data.fullName || '');
        setPhone(data.phone || '');
        setAddress(data.address || '');
        setAvatarUrl(data.avatarUrl || '');
      })
      .catch((error) => setMessage({ type: 'error', text: error.message }))
      .finally(() => setLoading(false));
  }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setMessage(null);
    if (!fullName.trim() || !/^0[35789]\d{8}$/.test(phone)) {
      setMessage({ type: 'error', text: 'Vui lòng nhập họ tên và số điện thoại Việt Nam hợp lệ.' });
      return;
    }
    if (avatar && (!['image/jpeg', 'image/png'].includes(avatar.type) || avatar.size > 5 * 1024 * 1024)) {
      setMessage({ type: 'error', text: 'Ảnh đại diện phải là JPG/PNG và không vượt quá 5MB.' });
      return;
    }

    const body = new FormData();
    body.append('fullName', fullName.trim());
    body.append('phone', phone);
    body.append('address', address.trim());
    if (avatar) body.append('avatar', avatar);

    setSaving(true);
    try {
      const token = localStorage.getItem('accessToken');
      const response = await fetch(`${API_BASE_URL}/retailer/profile`, {
        method: 'PUT',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body,
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || 'Không thể cập nhật hồ sơ.');
      setAvatarUrl(data.avatarUrl || '');
      setAvatar(null);
      const updated = { ...user, fullName: data.fullName || fullName };
      onUserUpdated(updated);
      setMessage({ type: 'success', text: 'Cập nhật thông tin cá nhân thành công.' });
    } catch (error) {
      setMessage({ type: 'error', text: error instanceof Error ? error.message : 'Không thể cập nhật hồ sơ.' });
    } finally {
      setSaving(false);
    }
  };

  const assetUrl = avatarUrl
    ? `${API_BASE_URL.replace(/\/api$/, '')}${avatarUrl}`
    : '';

  if (loading) return <div className="retailer-panel retailer-skeleton" aria-label="Đang tải hồ sơ" />;

  return (
    <section className="retailer-panel" aria-labelledby="retailer-profile-title">
      <div className="retailer-section-heading">
        <div>
          <h1 id="retailer-profile-title">Thông tin cá nhân</h1>
          <p>Cập nhật thông tin chủ sở hữu</p>
        </div>
        <div className="retailer-avatar">
          {assetUrl ? <img src={assetUrl} alt={`Ảnh đại diện của ${fullName}`} /> : fullName.charAt(0).toUpperCase()}
        </div>
      </div>

      {message && <div role="alert" className={`retailer-alert retailer-alert--${message.type}`}>{message.text}</div>}

      <form onSubmit={submit} className="retailer-form">
        <label>
          <span>Họ và tên *</span>
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} maxLength={255} required />
        </label>
        <label>
          <span>Số điện thoại *</span>
          <input value={phone} onChange={(e) => setPhone(e.target.value)} inputMode="tel" pattern="0[35789][0-9]{8}" required />
        </label>
        <label className="retailer-form__wide">
          <span>Địa chỉ</span>
          <textarea value={address} onChange={(e) => setAddress(e.target.value)} maxLength={500} rows={3} />
        </label>
        <label className="retailer-form__wide retailer-file">
          <span>Ảnh đại diện · JPG/PNG, tối đa 5MB</span>
          <input
            type="file"
            accept="image/jpeg,image/png"
            onChange={(e) => setAvatar(e.target.files?.[0] || null)}
          />
        </label>
        <div className="retailer-form__actions">
          <button className="btn btn-gradient" disabled={saving} type="submit">
            {saving ? 'Đang lưu...' : 'Lưu thông tin'}
          </button>
        </div>
      </form>
    </section>
  );
}
