import React, { useState } from 'react';
import PasswordStrengthMeter from './PasswordStrengthMeter';
import { API_BASE_URL } from '../../utils/auth';

interface RegisterFormProps {
  role: 'FARM_MANAGER' | 'RETAILER';
  onSuccess: (data: { token?: string; user?: any; pendingVerification?: boolean }) => void;
  onSwitchToLogin: () => void;
}

export const RegisterForm: React.FC<RegisterFormProps> = ({ role, onSuccess, onSwitchToLogin }) => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const roleTitle = role === 'FARM_MANAGER' ? 'Chủ Trang Trại (Farm Manager)' : 'Nhà Bán Lẻ (Retailer)';

  const validate = () => {
    const errors: Record<string, string> = {};

    if (!fullName.trim() || fullName.trim().length < 2) {
      errors.fullName = 'Họ và tên phải có ít nhất 2 ký tự';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim() || !emailRegex.test(email.trim())) {
      errors.email = 'Vui lòng nhập định dạng email hợp lệ';
    }

    const phoneRegex = /^0[35789]\d{8}$/;
    if (!phone.trim() || !phoneRegex.test(phone.trim())) {
      errors.phone = 'Số điện thoại Việt Nam hợp lệ (ví dụ: 0912345678)';
    }

    const pwdRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&_#^()+=.-])[A-Za-z\d@$!%*?&_#^()+=.-]{8,128}$/;
    if (!password || !pwdRegex.test(password)) {
      errors.password = 'Mật khẩu chưa đạt yêu cầu (ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt)';
    }

    if (password !== confirmPassword) {
      errors.confirmPassword = 'Xác nhận mật khẩu không trùng khớp';
    }

    if (!agreeTerms) {
      errors.agreeTerms = 'Bạn phải đồng ý với Điều khoản dịch vụ và Chính sách bảo mật';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!validate()) return;

    setLoading(true);

    try {
      const endpoint = role === 'FARM_MANAGER'
        ? `${API_BASE_URL}/auth/farm/register`
        : `${API_BASE_URL}/auth/retailer/register`;

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fullName: fullName.trim(),
          email: email.trim().toLowerCase(),
          phone: phone.trim(),
          password,
          confirmPassword,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        if (data.verificationRequired) {
          setSuccessMessage('Đăng ký thành công! Vui lòng kiểm tra email và mở liên kết xác nhận trước khi đăng nhập.');
          return;
        }
        setSuccessMessage('Đăng ký tài khoản thành công! Đang chuyển đến cổng thông tin của bạn.');
        setTimeout(() => {
          onSuccess({
            token: data.accessToken || data.token,
            user: {
              id: data.userId ?? data.id ?? Date.now(),
              email: data.email || email,
              fullName: data.fullName || fullName,
              role,
            },
          });
        }, 1500);
      } else {
        const errorData = await response.json().catch(() => ({}));
        setErrorMessage(errorData.message || 'Đăng ký thất bại. Email hoặc số điện thoại có thể đã được đăng ký.');
      }
    } catch (err) {
      // M-1: no mock/silent "success" on network failure.
      console.warn('Backend server unreachable:', err);
      setErrorMessage('Không thể kết nối đến máy chủ. Vui lòng kiểm tra backend đang chạy.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate aria-labelledby="register-form-heading">
      <div style={{ marginBottom: '20px' }}>
        <h2 id="register-form-heading" style={{ fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '6px' }}>
          Đăng ký tài khoản mới
        </h2>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary, #cbd5e1)' }}>
          Tạo tài khoản dành cho <strong style={{ color: role === 'FARM_MANAGER' ? '#10b981' : '#06b6d4' }}>{roleTitle}</strong>
        </p>
      </div>

      {/* Status Alerts */}
      {errorMessage && (
        <div role="alert" aria-live="polite" className="auth-alert-error" style={alertErrorStyle}>
          <span aria-hidden="true">⚠️</span>
          <span>{errorMessage}</span>
        </div>
      )}

      {successMessage && (
        <div role="status" aria-live="polite" className="auth-alert-success" style={alertSuccessStyle}>
          <span aria-hidden="true">✅</span>
          <span>{successMessage}</span>
        </div>
      )}

      {/* Grid Layout for Form Fields */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
        {/* Full Name */}
        <div className="form-group" style={{ gridColumn: 'span 2' }}>
          <label htmlFor="reg-fullname" className="form-label" style={labelStyle}>
            Họ và tên <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <input
            id="reg-fullname"
            type="text"
            value={fullName}
            onChange={(e) => {
              setFullName(e.target.value);
              if (fieldErrors.fullName) setFieldErrors({ ...fieldErrors, fullName: '' });
            }}
            placeholder="e.g. Nguyễn Văn An"
            aria-invalid={!!fieldErrors.fullName}
            aria-describedby={fieldErrors.fullName ? 'fullname-error' : undefined}
            className="form-input"
            style={{ ...inputStyle, borderColor: fieldErrors.fullName ? '#ef4444' : 'rgba(255, 255, 255, 0.15)' }}
          />
          {fieldErrors.fullName && <span id="fullname-error" role="alert" style={errorTextStyle}>{fieldErrors.fullName}</span>}
        </div>

        {/* Email */}
        <div className="form-group">
          <label htmlFor="reg-email" className="form-label" style={labelStyle}>
            Địa chỉ Email <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <input
            id="reg-email"
            type="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (fieldErrors.email) setFieldErrors({ ...fieldErrors, email: '' });
            }}
            placeholder="name@company.com"
            aria-invalid={!!fieldErrors.email}
            aria-describedby={fieldErrors.email ? 'email-error' : undefined}
            className="form-input"
            style={{ ...inputStyle, borderColor: fieldErrors.email ? '#ef4444' : 'rgba(255, 255, 255, 0.15)' }}
          />
          {fieldErrors.email && <span id="email-error" role="alert" style={errorTextStyle}>{fieldErrors.email}</span>}
        </div>

        {/* Phone */}
        <div className="form-group">
          <label htmlFor="reg-phone" className="form-label" style={labelStyle}>
            Số điện thoại <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <input
            id="reg-phone"
            type="tel"
            value={phone}
            onChange={(e) => {
              setPhone(e.target.value);
              if (fieldErrors.phone) setFieldErrors({ ...fieldErrors, phone: '' });
            }}
            placeholder="0912345678"
            aria-invalid={!!fieldErrors.phone}
            aria-describedby={fieldErrors.phone ? 'phone-error' : undefined}
            className="form-input"
            style={{ ...inputStyle, borderColor: fieldErrors.phone ? '#ef4444' : 'rgba(255, 255, 255, 0.15)' }}
          />
          {fieldErrors.phone && <span id="phone-error" role="alert" style={errorTextStyle}>{fieldErrors.phone}</span>}
        </div>

        {/* Store / Farm Name (Optional Context Field) */}
        <div className="form-group" style={{ gridColumn: 'span 2' }}>
          <label htmlFor="reg-org" className="form-label" style={labelStyle}>
            {role === 'FARM_MANAGER' ? 'Tên Trang trại (Tùy chọn)' : 'Tên Cửa hàng / Doanh nghiệp (Tùy chọn)'}
          </label>
          <input
            id="reg-org"
            type="text"
            value={organizationName}
            onChange={(e) => setOrganizationName(e.target.value)}
            placeholder={role === 'FARM_MANAGER' ? 'e.g. Nông trại Xanh Đà Lạt' : 'e.g. Siêu thị Thực phẩm An Toàn'}
            className="form-input"
            style={inputStyle}
          />
        </div>

        {/* Password */}
        <div className="form-group" style={{ gridColumn: 'span 2' }}>
          <label htmlFor="reg-password" className="form-label" style={labelStyle}>
            Mật khẩu <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <div style={{ position: 'relative' }}>
            <input
              id="reg-password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (fieldErrors.password) setFieldErrors({ ...fieldErrors, password: '' });
              }}
              placeholder="Nhập mật khẩu an toàn..."
              aria-invalid={!!fieldErrors.password}
              aria-describedby={fieldErrors.password ? 'reg-password-error' : undefined}
              className="form-input"
              style={{ ...inputStyle, paddingRight: '42px', borderColor: fieldErrors.password ? '#ef4444' : 'rgba(255, 255, 255, 0.15)' }}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
              style={eyeButtonStyle}
            >
              {showPassword ? '🙈' : '👁️'}
            </button>
          </div>
          {fieldErrors.password && <span id="reg-password-error" role="alert" style={errorTextStyle}>{fieldErrors.password}</span>}

          {/* Password Strength Meter */}
          <PasswordStrengthMeter password={password} />
        </div>

        {/* Confirm Password */}
        <div className="form-group" style={{ gridColumn: 'span 2' }}>
          <label htmlFor="reg-confirm-password" className="form-label" style={labelStyle}>
            Xác nhận Mật khẩu <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <input
            id="reg-confirm-password"
            type={showPassword ? 'text' : 'password'}
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              if (fieldErrors.confirmPassword) setFieldErrors({ ...fieldErrors, confirmPassword: '' });
            }}
            placeholder="Nhập lại mật khẩu vừa đặt..."
            aria-invalid={!!fieldErrors.confirmPassword}
            aria-describedby={fieldErrors.confirmPassword ? 'confirm-password-error' : undefined}
            className="form-input"
            style={{ ...inputStyle, borderColor: fieldErrors.confirmPassword ? '#ef4444' : 'rgba(255, 255, 255, 0.15)' }}
          />
          {fieldErrors.confirmPassword && (
            <span id="confirm-password-error" role="alert" style={errorTextStyle}>
              {fieldErrors.confirmPassword}
            </span>
          )}
        </div>
      </div>

      {/* Terms & Conditions Checkbox */}
      <div style={{ marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
          <input
            id="agree-terms"
            type="checkbox"
            checked={agreeTerms}
            onChange={(e) => {
              setAgreeTerms(e.target.checked);
              if (fieldErrors.agreeTerms) setFieldErrors({ ...fieldErrors, agreeTerms: '' });
            }}
            style={{ marginTop: '3px', width: '16px', height: '16px', accentColor: role === 'FARM_MANAGER' ? '#10b981' : '#06b6d4', cursor: 'pointer' }}
          />
          <label htmlFor="agree-terms" style={{ fontSize: '13px', color: 'var(--text-secondary, #cbd5e1)', cursor: 'pointer', lineHeight: 1.4 }}>
            Tôi đã đọc và đồng ý với <a href="#terms" style={{ color: '#38bdf8' }}>Điều khoản dịch vụ</a> và <a href="#privacy" style={{ color: '#38bdf8' }}>Chính sách bảo mật</a> của BICAP.
          </label>
        </div>
        {fieldErrors.agreeTerms && (
          <span role="alert" style={{ ...errorTextStyle, marginTop: '4px' }}>
            {fieldErrors.agreeTerms}
          </span>
        )}
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={loading}
        className="btn-primary"
        style={{
          width: '100%',
          padding: '14px',
          borderRadius: '8px',
          background: role === 'FARM_MANAGER' 
            ? 'linear-gradient(135deg, #059669 0%, #10b981 100%)' 
            : 'linear-gradient(135deg, #0284c7 0%, #06b6d4 100%)',
          color: '#fff',
          fontWeight: 600,
          fontSize: '15px',
          border: 'none',
          cursor: loading ? 'not-allowed' : 'pointer',
          boxShadow: '0 4px 14px rgba(0, 0, 0, 0.25)',
          transition: 'all 0.2s ease',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          gap: '8px',
        }}
      >
        {loading ? (
          <>
            <span className="spinner" aria-hidden="true" style={{ width: '16px', height: '16px', border: '2px solid rgba(255,255,255,0.3)', borderTopColor: '#fff', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
            <span>Đang xử lý đăng ký...</span>
          </>
        ) : (
          <span>Hoàn tất Đăng ký {role === 'FARM_MANAGER' ? 'Farm' : 'Retailer'}</span>
        )}
      </button>

      {/* Switch to Login */}
      <div style={{ marginTop: '20px', textAlign: 'center', fontSize: '13px', color: 'var(--text-secondary, #cbd5e1)' }}>
        Đã có tài khoản?{' '}
        <button
          type="button"
          onClick={onSwitchToLogin}
          style={{
            background: 'none',
            border: 'none',
            color: role === 'FARM_MANAGER' ? '#34d399' : '#38bdf8',
            fontWeight: 600,
            cursor: 'pointer',
            padding: 0,
            textDecoration: 'underline',
          }}
        >
          Đăng nhập ngay
        </button>
      </div>
    </form>
  );
};

/* Styles */
const labelStyle: React.CSSProperties = {
  display: 'block',
  fontSize: '13px',
  fontWeight: 500,
  color: '#e2e8f0',
  marginBottom: '6px',
};

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '11px 14px',
  background: 'rgba(255, 255, 255, 0.05)',
  border: '1px solid rgba(255, 255, 255, 0.15)',
  borderRadius: '8px',
  color: '#fff',
  fontSize: '14px',
  outline: 'none',
  boxSizing: 'border-box',
};

const errorTextStyle: React.CSSProperties = {
  fontSize: '12px',
  color: '#f87171',
  marginTop: '4px',
  display: 'block',
};

const eyeButtonStyle: React.CSSProperties = {
  position: 'absolute',
  right: '12px',
  top: '50%',
  transform: 'translateY(-50%)',
  background: 'none',
  border: 'none',
  color: 'var(--text-muted, #94a3b8)',
  fontSize: '16px',
  cursor: 'pointer',
  padding: '4px',
};

const alertErrorStyle: React.CSSProperties = {
  background: 'rgba(239, 68, 68, 0.15)',
  border: '1px solid rgba(239, 68, 68, 0.3)',
  color: '#f87171',
  padding: '12px 16px',
  borderRadius: '8px',
  fontSize: '13px',
  marginBottom: '20px',
  display: 'flex',
  alignItems: 'center',
  gap: '8px',
};

const alertSuccessStyle: React.CSSProperties = {
  background: 'rgba(16, 185, 129, 0.15)',
  border: '1px solid rgba(16, 185, 129, 0.3)',
  color: '#34d399',
  padding: '12px 16px',
  borderRadius: '8px',
  fontSize: '13px',
  marginBottom: '20px',
  display: 'flex',
  alignItems: 'center',
  gap: '8px',
};

export default RegisterForm;
