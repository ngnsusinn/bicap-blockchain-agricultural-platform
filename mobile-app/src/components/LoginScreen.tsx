import React, { useState } from 'react';
import { loginDriver } from '../utils/api';
import { UserSession } from '../types';

interface LoginScreenProps {
  onLoginSuccess: (user: UserSession) => void;
  onOpenPublicQRScan: () => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess, onOpenPublicQRScan }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const session = await loginDriver(email, password);
      onLoginSuccess(session);
    } catch (err: any) {
      setError(err.message || 'Đăng nhập không thành công. Vui lòng kiểm tra lại email hoặc mật khẩu.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="app-badge">🚚 BICAP Driver Mobile</div>
        <h1 className="login-title">Đăng Nhập Tài Xế</h1>
        <p className="login-subtitle">Ứng dụng dành cho Tài xế vận chuyển nông sản</p>

        {error && <div className="alert-box error">{error}</div>}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label>Email Tài Xế</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Nhập email tài xế..."
            />
          </div>

          <div className="form-group">
            <label>Mật Khẩu</label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                style={{ width: '100%', paddingRight: '40px' }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute',
                  right: '10px',
                  background: 'none',
                  border: 'none',
                  fontSize: '16px',
                  cursor: 'pointer',
                  padding: '4px',
                  color: '#94a3b8'
                }}
                title={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
          </div>

          <button type="submit" disabled={loading} className="btn-primary full-width">
            {loading ? 'Đang xác thực...' : '🔐 Đăng Nhập Hệ Thống'}
          </button>
        </form>

        <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid #334155', textAlign: 'center' }}>
          <button
            type="button"
            onClick={onOpenPublicQRScan}
            className="btn-accent full-width"
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
          >
            <span>📱</span>
            <span>Quét Mã QR Truy Xuất Nông Sản</span>
          </button>
        </div>
      </div>
    </div>
  );
};
