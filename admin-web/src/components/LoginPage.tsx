import React, { useState } from 'react';
import type { UserSession } from '../types';

interface LoginPageProps {
  onLoginSuccess: (session: UserSession, token: string) => void;
  apiAuthUrl?: string;
}

export const LoginPage: React.FC<LoginPageProps> = ({
  onLoginSuccess,
  apiAuthUrl = 'http://localhost:8080/api/auth/login',
}) => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!identifier.trim() || !password.trim()) {
      setErrorMsg('Please enter both Email/Phone and Password.');
      return;
    }

    setLoading(true);
    setErrorMsg(null);

    try {
      const res = await fetch(apiAuthUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          identifier: identifier.trim(),
          password: password,
        }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Invalid email/phone or password');
      }

      const data = await res.json();
      const userRoles: string[] = data.roles || [];

      let role: UserSession['role'] = 'GUEST';
      let permissions: string[] = [];

      if (userRoles.includes('SUPER_ADMIN') || data.email === 'superadmin@bicap.com') {
        role = 'SUPER_ADMIN';
        permissions = ['ADMIN_CREATE', 'ADMIN_READ', 'ADMIN_UPDATE', 'ADMIN_DELETE'];
      } else if (userRoles.includes('ADMIN') || data.email === 'admin@bicap.com') {
        role = 'ADMIN';
        permissions = ['ADMIN_READ', 'ADMIN_UPDATE'];
      } else if (userRoles.includes('MODERATOR') || data.email === 'moderator@bicap.com') {
        role = 'MODERATOR';
        permissions = ['ADMIN_READ'];
      } else if (userRoles.includes('FARM_MANAGER')) {
        role = 'FARM_MANAGER';
      } else if (userRoles.includes('RETAILER')) {
        role = 'RETAILER';
      } else if (userRoles.includes('SHIPPING_MGR')) {
        role = 'SHIPPING_MGR';
      } else if (userRoles.includes('SHIP_DRIVER')) {
        role = 'SHIP_DRIVER';
      }

      const session: UserSession = {
        id: data.id,
        email: data.email || identifier,
        fullName: data.fullName || 'Administrator',
        phone: data.phone,
        role,
        permissions,
      };

      onLoginSuccess(session, data.accessToken);
    } catch (err: any) {
      setErrorMsg(err.message || 'Login failed. Please verify your credentials and network connection.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={loginWrapperStyle}>
      <div className="glass-panel" style={cardStyle}>
        {/* Logo / Header */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div style={iconBadgeStyle}>🌱</div>
          <h1 style={titleStyle}>BICAP Admin Portal</h1>
          <p style={subtitleStyle}>Blockchain Agricultural Platform Administrator Login</p>
        </div>

        {/* Error Alert */}
        {errorMsg && <div style={errorAlertStyle}>{errorMsg}</div>}

        {/* Login Form */}
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '18px' }}>
            <label style={labelStyle}>Email or Phone Number</label>
            <input
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="e.g. superadmin@bicap.com"
              style={inputStyle}
              required
            />
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label style={labelStyle}>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              style={inputStyle}
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={submitBtnStyle}
          >
            {loading ? 'Authenticating...' : 'Sign In to Portal ➔'}
          </button>
        </form>
      </div>
    </div>
  );
};

// Styles
const loginWrapperStyle: React.CSSProperties = {
  minHeight: '100vh',
  width: '100vw',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: 'radial-gradient(circle at 50% 20%, #1e1b4b 0%, #0f172a 70%, #020617 100%)',
  padding: '20px',
  position: 'fixed',
  top: 0,
  left: 0,
  zIndex: 9999,
};

const cardStyle: React.CSSProperties = {
  width: '100%',
  maxWidth: '440px',
  padding: '40px 32px',
  borderRadius: '20px',
  background: 'rgba(15, 23, 42, 0.75)',
  backdropFilter: 'blur(16px)',
  border: '1px solid rgba(255, 255, 255, 0.1)',
  boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.6), 0 0 30px rgba(139, 92, 246, 0.15)',
};

const iconBadgeStyle: React.CSSProperties = {
  width: '64px',
  height: '64px',
  borderRadius: '16px',
  background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.3) 0%, rgba(59, 130, 246, 0.3) 100%)',
  border: '1px solid rgba(139, 92, 246, 0.4)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: '32px',
  margin: '0 auto 16px auto',
  boxShadow: '0 8px 16px rgba(0,0,0,0.3)',
};

const titleStyle: React.CSSProperties = {
  fontSize: '22px',
  fontWeight: 700,
  color: '#ffffff',
  margin: 0,
  letterSpacing: '-0.5px',
};

const subtitleStyle: React.CSSProperties = {
  fontSize: '13px',
  color: 'var(--text-secondary)',
  marginTop: '6px',
};

const errorAlertStyle: React.CSSProperties = {
  padding: '12px 16px',
  borderRadius: '8px',
  background: 'rgba(239, 68, 68, 0.15)',
  border: '1px solid rgba(239, 68, 68, 0.4)',
  color: '#fca5a5',
  fontSize: '13px',
  marginBottom: '20px',
  lineHeight: '1.4',
};

const labelStyle: React.CSSProperties = {
  display: 'block',
  fontSize: '12px',
  fontWeight: 600,
  color: 'var(--text-secondary)',
  marginBottom: '6px',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
};

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '12px 16px',
  borderRadius: '10px',
  background: 'rgba(2, 6, 23, 0.5)',
  border: '1px solid rgba(255, 255, 255, 0.12)',
  color: '#ffffff',
  fontSize: '14px',
  outline: 'none',
  boxSizing: 'border-box',
};

const submitBtnStyle: React.CSSProperties = {
  width: '100%',
  padding: '14px',
  borderRadius: '10px',
  fontSize: '15px',
  fontWeight: 600,
  cursor: 'pointer',
  boxShadow: '0 4px 14px rgba(139, 92, 246, 0.4)',
};
