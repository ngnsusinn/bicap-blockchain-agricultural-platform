import React, { useState, useEffect } from 'react';
import { API_BASE_URL, getAuthHeaders, getCurrentUser, saveSession } from '../../utils/auth';
import type { UserSession } from '../../utils/auth';

interface ProfileData {
  id: number;
  email: string;
  fullName: string;
  phone: string;
  address: string;
  avatarUrl: string;
  role: string;
  status: string;
  createdAt: string;
}

interface ProfilePageProps {
  onUserUpdated?: (updatedUser: UserSession) => void;
}

export default function ProfilePage({ onUserUpdated }: ProfilePageProps) {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Profile data state
  const [profile, setProfile] = useState<ProfileData>({
    id: 0,
    email: '',
    fullName: '',
    phone: '',
    address: '',
    avatarUrl: '',
    role: 'FARM_MANAGER',
    status: 'ACTIVE',
    createdAt: '',
  });

  // Editable form fields
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Sample avatar presets for quick selection
  const avatarPresets = [
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
  ];

  // Load profile from API or local session fallback
  useEffect(() => {
    const fetchProfile = async () => {
      setLoading(true);
      try {
        const res = await fetch(`${API_BASE_URL}/profile`, {
          headers: getAuthHeaders(),
        });
        if (res.ok) {
          const data: ProfileData = await res.json();
          setProfile(data);
          setFullName(data.fullName || '');
          setPhone(data.phone || '');
          setAddress(data.address || '');
          setAvatarUrl(data.avatarUrl || '');
        } else {
          // Fallback to getCurrentUser()
          const currentUser = getCurrentUser();
          if (currentUser) {
            setProfile({
              id: currentUser.id || 1,
              email: currentUser.email || '',
              fullName: currentUser.fullName || '',
              phone: currentUser.phone || '',
              address: currentUser.address || '',
              avatarUrl: currentUser.avatarUrl || '',
              role: currentUser.role || 'FARM_MANAGER',
              status: currentUser.status || 'ACTIVE',
              createdAt: currentUser.createdAt || new Date().toISOString(),
            });
            setFullName(currentUser.fullName || '');
            setPhone(currentUser.phone || '');
            setAddress(currentUser.address || '');
            setAvatarUrl(currentUser.avatarUrl || '');
          }
        }
      } catch (err) {
        const currentUser = getCurrentUser();
        if (currentUser) {
          setFullName(currentUser.fullName || '');
          setPhone(currentUser.phone || '');
          setAddress(currentUser.address || '');
          setAvatarUrl(currentUser.avatarUrl || '');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);

    if (!fullName.trim()) {
      setMessage({ type: 'error', text: 'Họ và tên không được để trống.' });
      return;
    }
    if (newPassword && newPassword !== confirmPassword) {
      setMessage({ type: 'error', text: 'Xác nhận mật khẩu không khớp.' });
      return;
    }

    setSaving(true);
    try {
      const res = await fetch(`${API_BASE_URL}/profile`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          fullName: fullName.trim(),
          phone: phone.trim(),
          address: address.trim(),
          avatarUrl: avatarUrl.trim(),
          newPassword: newPassword || undefined,
          confirmPassword: confirmPassword || undefined,
        }),
      });

      if (res.ok) {
        const updated: ProfileData = await res.json();
        setProfile(updated);
        setFullName(updated.fullName);
        setPhone(updated.phone || '');
        setAddress(updated.address || '');
        setAvatarUrl(updated.avatarUrl || '');
        setNewPassword('');
        setConfirmPassword('');

        // Update local session
        const currentUser = getCurrentUser();
        const updatedSession: UserSession = {
          ...currentUser,
          id: updated.id,
          email: updated.email,
          fullName: updated.fullName,
          role: (updated.role as any) || 'FARM_MANAGER',
          phone: updated.phone,
          address: updated.address,
          avatarUrl: updated.avatarUrl,
          status: updated.status,
          createdAt: updated.createdAt,
        };

        saveSession(localStorage.getItem('accessToken') || '', updatedSession);
        if (onUserUpdated) {
          onUserUpdated(updatedSession);
        }

        setMessage({ type: 'success', text: 'Cập nhật thông tin cá nhân thành công!' });
      } else {
        const errorData = await res.json().catch(() => ({}));
        setMessage({
          type: 'error',
          text: errorData.message || 'Cập nhật thất bại. Vui lòng kiểm tra lại thông tin.',
        });
      }
    } catch (err) {
      setMessage({ type: 'error', text: 'Không thể kết nối máy chủ để cập nhật hồ sơ.' });
    } finally {
      setSaving(false);
    }
  };

  const handleReset = () => {
    setFullName(profile.fullName || '');
    setPhone(profile.phone || '');
    setAddress(profile.address || '');
    setAvatarUrl(profile.avatarUrl || '');
    setNewPassword('');
    setConfirmPassword('');
    setMessage(null);
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '01/01/2026';
    try {
      const d = new Date(dateStr);
      return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '300px' }}>
        <div style={{ color: '#10b981', fontSize: '16px', fontWeight: 600 }}>⏳ Đang tải thông tin hồ sơ...</div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 className="dashboard-title" style={{ fontSize: '26px' }}>Cập Nhật Hồ Sơ</h1>
        <p className="dashboard-subtitle">
          Quản lý và cập nhật thông tin tài khoản Chủ trang trại (BICAP-8).
        </p>
      </div>

      {message && (
        <div
          style={{
            padding: '14px 20px',
            borderRadius: '12px',
            marginBottom: '24px',
            fontSize: '14px',
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            background: message.type === 'success' ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
            border: message.type === 'success' ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid rgba(239, 68, 68, 0.3)',
            color: message.type === 'success' ? '#34d399' : '#f87171',
          }}
        >
          <span>{message.type === 'success' ? '✅' : '⚠️'}</span>
          <span>{message.text}</span>
        </div>
      )}

      <form onSubmit={handleSave}>
        {/* Avatar Section */}
        <div className="glass-panel" style={{ padding: '24px', borderRadius: '16px', marginBottom: '24px' }}>
          <h2 style={sectionHeaderStyle}>
            <span>🖼️</span> Ảnh đại diện
          </h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '24px', flexWrap: 'wrap' }}>
            <div style={{ position: 'relative' }}>
              <div
                style={{
                  width: '96px',
                  height: '96px',
                  borderRadius: '50%',
                  overflow: 'hidden',
                  border: '3px solid #10b981',
                  boxShadow: '0 4px 16px rgba(16, 185, 129, 0.3)',
                  background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '36px',
                  color: '#fff',
                }}
              >
                {avatarUrl ? (
                  <img
                    src={avatarUrl}
                    alt="Avatar"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    onError={(e) => {
                      (e.target as HTMLElement).style.display = 'none';
                    }}
                  />
                ) : (
                  <span>{fullName?.charAt(0)?.toUpperCase() || '👤'}</span>
                )}
              </div>
            </div>

            <div style={{ flex: 1, minWidth: '260px' }}>
              <label style={labelStyle}>URL Ảnh đại diện</label>
              <div style={{ display: 'flex', gap: '10px', marginBottom: '12px' }}>
                <input
                  type="text"
                  placeholder="https://example.com/avatar.jpg"
                  value={avatarUrl}
                  onChange={(e) => setAvatarUrl(e.target.value)}
                  style={inputStyle}
                />
              </div>

              <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '8px' }}>
                Hoặc chọn nhanh ảnh mẫu:
              </div>
              <div style={{ display: 'flex', gap: '10px' }}>
                {avatarPresets.map((url, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setAvatarUrl(url)}
                    style={{
                      width: '36px',
                      height: '36px',
                      borderRadius: '50%',
                      overflow: 'hidden',
                      border: avatarUrl === url ? '2px solid #10b981' : '1px solid rgba(255,255,255,0.2)',
                      cursor: 'pointer',
                      padding: 0,
                    }}
                  >
                    <img src={url} alt={`Preset ${idx + 1}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Editable Fields Section */}
        <div className="glass-panel" style={{ padding: '24px', borderRadius: '16px', marginBottom: '24px' }}>
          <h2 style={sectionHeaderStyle}>
            <span>✏️</span> Thông tin cá nhân (Được phép chỉnh sửa)
          </h2>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
            <div>
              <label style={labelStyle}>
                Họ và tên <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <input
                type="text"
                required
                placeholder="Nhập họ và tên đầy đủ"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                style={inputStyle}
              />
            </div>

            <div>
              <label style={labelStyle}>
                Số điện thoại <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <input
                type="tel"
                required
                pattern="0[35789][0-9]{8}"
                placeholder="0912345678"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                style={inputStyle}
              />
            </div>

            <div style={{ gridColumn: '1 / -1' }}>
              <label style={labelStyle}>Địa chỉ liên hệ</label>
              <textarea
                rows={3}
                placeholder="Nhập địa chỉ nhà / địa chỉ trang trại..."
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                style={{ ...inputStyle, resize: 'vertical' }}
              />
            </div>
          </div>
        </div>

        {/* System fields */}
        <div className="glass-panel" style={{ padding: '24px', borderRadius: '16px', marginBottom: '32px', opacity: 0.9 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h2 style={{ ...sectionHeaderStyle, margin: 0 }}>
              <span>🔒</span> Thông tin hệ thống
            </h2>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px' }}>
            <div>
              <label style={labelStyle}>Email đăng nhập</label>
              <div style={{ position: 'relative' }}>
                <input type="text" value={profile.email} disabled style={readOnlyInputStyle} />
                <span style={lockIconStyle}>🔒</span>
              </div>
            </div>

            <div>
              <label style={labelStyle}>Mật khẩu mới</label>
              <input type="password" minLength={8} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="Để trống nếu không đổi" style={inputStyle} />
              <label style={{ ...labelStyle, marginTop: '10px' }}>Xác nhận mật khẩu mới</label>
              <input type="password" minLength={8} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} placeholder="Nhập lại mật khẩu mới" style={inputStyle} />
              <small style={{ color: 'var(--text-muted)' }}>Ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</small>
            </div>

            <div>
              <label style={labelStyle}>Vai trò (Role)</label>
              <div style={{ position: 'relative' }}>
                <input type="text" value={profile.role || 'FARM_MANAGER'} disabled style={readOnlyInputStyle} />
                <span style={lockIconStyle}>🔒</span>
              </div>
            </div>

            <div>
              <label style={labelStyle}>Trạng thái tài khoản</label>
              <div style={{ position: 'relative' }}>
                <input type="text" value={profile.status || 'ACTIVE'} disabled style={readOnlyInputStyle} />
                <span style={lockIconStyle}>🔒</span>
              </div>
            </div>

            <div>
              <label style={labelStyle}>Ngày khởi tạo (Created Date)</label>
              <div style={{ position: 'relative' }}>
                <input type="text" value={formatDate(profile.createdAt)} disabled style={readOnlyInputStyle} />
                <span style={lockIconStyle}>🔒</span>
              </div>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px' }}>
          <button
            type="button"
            onClick={handleReset}
            disabled={saving}
            style={{
              padding: '12px 24px',
              borderRadius: '10px',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              background: 'rgba(255, 255, 255, 0.05)',
              color: '#cbd5e1',
              fontSize: '14px',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'all 0.2s ease',
            }}
          >
            🔄 Hoàn tác
          </button>

          <button
            type="submit"
            disabled={saving}
            style={{
              padding: '12px 32px',
              borderRadius: '10px',
              border: 'none',
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
              color: '#fff',
              fontSize: '14px',
              fontWeight: 700,
              cursor: 'pointer',
              boxShadow: '0 4px 14px rgba(16, 185, 129, 0.4)',
              transition: 'all 0.2s ease',
            }}
          >
            {saving ? '⏳ Đang lưu...' : '💾 Lưu thông tin thay đổi'}
          </button>
        </div>
      </form>
    </div>
  );
}

/* ── Inline Styles ── */
const sectionHeaderStyle: React.CSSProperties = {
  fontSize: '17px',
  fontWeight: 700,
  color: '#fff',
  marginBottom: '16px',
  display: 'flex',
  alignItems: 'center',
  gap: '8px',
};
const labelStyle: React.CSSProperties = {
  display: 'block',
  fontSize: '13px',
  fontWeight: 600,
  color: '#cbd5e1',
  marginBottom: '8px',
};

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '11px 16px',
  borderRadius: '10px',
  border: '1px solid rgba(255, 255, 255, 0.12)',
  background: 'rgba(15, 23, 42, 0.6)',
  color: '#fff',
  fontSize: '14px',
  outline: 'none',
  boxSizing: 'border-box',
  transition: 'border-color 0.2s ease',
};

const readOnlyInputStyle: React.CSSProperties = {
  width: '100%',
  padding: '11px 16px 11px 38px',
  borderRadius: '10px',
  border: '1px solid rgba(255, 255, 255, 0.05)',
  background: 'rgba(255, 255, 255, 0.03)',
  color: '#94a3b8',
  fontSize: '14px',
  cursor: 'not-allowed',
  boxSizing: 'border-box',
};

const lockIconStyle: React.CSSProperties = {
  position: 'absolute',
  left: '12px',
  top: '50%',
  transform: 'translateY(-50%)',
  fontSize: '14px',
  opacity: 0.6,
};

