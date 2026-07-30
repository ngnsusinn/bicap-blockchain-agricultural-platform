import React, { useState } from 'react';

export interface UserSession {
  email: string;
  fullName: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'MODERATOR' | 'GUEST';
  permissions: string[];
}

interface LoginSimulatorProps {
  currentSession: UserSession;
  onSessionChange: (session: UserSession) => void;
  token: string | null;
  onTokenChange: (token: string | null) => void;
}

// Backend base (matches App.tsx usage)
const envAdminApiUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/admins').replace(/\/$/, '');
const ADMIN_API_BASE_URL = envAdminApiUrl;
const AUTH_API_BASE_URL = (import.meta.env.VITE_AUTH_BASE_URL || envAdminApiUrl.replace(/\/admins$/, '')).replace(/\/$/, '');
const AUTH_URL = AUTH_API_BASE_URL + '/auth';
const ADMIN_URL = ADMIN_API_BASE_URL;

export const LoginSimulator: React.FC<LoginSimulatorProps> = ({ currentSession, onSessionChange, token, onTokenChange }) => {
  const [mode, setMode] = useState<'simulate' | 'login' | 'register'>('simulate');

  // Login form state
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');

  // Register form state
  const [regFullName, setRegFullName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regPhone, setRegPhone] = useState('');

  // Keep a small set of mock sessions for quick switching (falls back if backend not used)
  const mockSessions: UserSession[] = [
    {
      email: 'superadmin@bicap.com',
      fullName: 'Trần Nguyễn Gia Bảo (Super Admin)',
      role: 'SUPER_ADMIN',
      permissions: ['ADMIN_CREATE', 'ADMIN_READ', 'ADMIN_UPDATE', 'ADMIN_DELETE'],
    },
    {
      email: 'admin@bicap.com',
      fullName: 'Lê Minh Tuấn (Admin)',
      role: 'ADMIN',
      permissions: ['ADMIN_READ', 'ADMIN_UPDATE'],
    },
    {
      email: 'moderator@bicap.com',
      fullName: 'Nguyễn Thị Hoa (Moderator)',
      role: 'MODERATOR',
      permissions: ['ADMIN_READ'],
    },
    {
      email: 'unauthorized@bicap.com',
      fullName: 'Kẻ Xâm Nhập (Unauthorized)',
      role: 'GUEST',
      permissions: [],
    }
  ];

  const applyAdminResponseToSession = async (email: string, authToken: string | null) => {
    // Try to fetch admin details to extract roles/permissions
    try {
      const headers: Record<string, string> = {};
      if (authToken) {
        headers['Authorization'] = 'Bearer ' + authToken;
      }
      const resp = await fetch(`${ADMIN_URL}?search=${encodeURIComponent(email)}`, { headers });
      if (!resp.ok) return null;
      const body = await resp.json();
      const items = body.content || [];
      const me = items.find((a: any) => a.email === email);
      if (!me) return null;

      const roles = me.roles || [];
      const roleName = roles.length > 0 ? roles[0].name : 'ADMIN';
      const perms: string[] = [];
      roles.forEach((r: any) => {
        if (r.permissions) {
          r.permissions.forEach((p: any) => perms.push(p.code));
        }
      });

      return {
        email: me.email,
        fullName: me.fullName || me.email,
        role: roleName as UserSession['role'],
        permissions: perms,
      } as UserSession;
    } catch (err) {
      return null;
    }
  };

  const handleSimulate = (s: UserSession) => {
    onSessionChange(s);
    // clear real token when simulating
    onTokenChange(null);
    localStorage.removeItem('ACCESS_TOKEN');
  };

  const handleLogout = () => {
    onTokenChange(null);
    onSessionChange({
      email: 'unauthorized@bicap.com',
      fullName: 'Guest (Not logged in)',
      role: 'GUEST',
      permissions: [],
    });
  };

  const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&_#^()+=.\-])[A-Za-z\d@$!%*?&_#^()+=.\-]{8,}$/;
  const gmailPattern = /^[^\s@]+@gmail\.com$/i;

  const handleLogin = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!loginEmail.trim() || !loginPassword) {
      alert('Please enter both email and password.');
      return;
    }

    try {
      const resp = await fetch(`${AUTH_URL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: loginEmail.trim(), password: loginPassword }),
      });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        alert(err.message || 'Login failed');
        return;
      }
      const auth = await resp.json();
      const t = auth.accessToken;
      onTokenChange(t);
      localStorage.setItem('ACCESS_TOKEN', t);

      const resolved = await applyAdminResponseToSession(auth.email, t);
      if (resolved) {
        onSessionChange(resolved);
      } else {
        onSessionChange({
          email: auth.email,
          fullName: auth.fullName || auth.email,
          role: 'GUEST',
          permissions: [],
        });
      }
      setMode('simulate');
    } catch (err) {
      alert('Login error: ' + String(err));
    }
  };

  const handleRegister = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();

    const email = regEmail.trim();
    const password = regPassword;
    const fullName = regFullName.trim();

    if (!fullName || !email || !password) {
      alert('Please complete full name, email, and password.');
      return;
    }
    if (!gmailPattern.test(email)) {
      alert('Email must be a valid Gmail address ending with @gmail.com.');
      return;
    }
    if (!passwordPattern.test(password)) {
      alert('Password must be at least 8 characters and include uppercase, lowercase, number, and special character.');
      return;
    }

    try {
      const resp = await fetch(`${AUTH_URL}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fullName, email, password, phone: regPhone.trim() }),
      });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        alert(err.message || 'Registration failed');
        return;
      }
      const auth = await resp.json();
      const t = auth.accessToken;
      onTokenChange(t);
      localStorage.setItem('ACCESS_TOKEN', t);

      const resolved = await applyAdminResponseToSession(auth.email, t);
      if (resolved) {
        onSessionChange(resolved);
      } else {
        onSessionChange({
          email: auth.email,
          fullName: auth.fullName || auth.email,
          role: 'GUEST',
          permissions: [],
        });
      }
      setMode('simulate');
    } catch (err) {
      alert('Registration error: ' + String(err));
    }
  };

  return (
    <div className="glass-panel" style={cardStyle}>
      <div style={headerStyle}>
        <div style={dotStyle}></div>
        <h3 style={titleStyle}>Authentication</h3>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <button className="btn btn-secondary" onClick={() => setMode('simulate')} style={{ opacity: mode === 'simulate' ? 1 : 0.7 }}>Quick simulate</button>
        <button className="btn btn-secondary" onClick={() => setMode('login')} style={{ opacity: mode === 'login' ? 1 : 0.7 }}>Login</button>
        <button className="btn btn-secondary" onClick={() => setMode('register')} style={{ opacity: mode === 'register' ? 1 : 0.7 }}>Register</button>
        {token ? (
          <button className="btn btn-danger" onClick={handleLogout} style={{ marginLeft: 'auto' }}>Logout</button>
        ) : null}
      </div>

      {mode === 'simulate' && (
        <>
          <p style={descStyle}>Use quick mock sessions for UI permission preview, or login/register to use the real backend.</p>
          <div style={selectorContainerStyle}>
            {mockSessions.map((session) => {
              const isSelected = session.email === currentSession.email;
              return (
                <button
                  key={session.email}
                  onClick={() => handleSimulate(session)}
                  style={{
                    ...btnStyle,
                    border: isSelected ? '1px solid var(--primary)' : '1px solid var(--border-color)',
                    background: isSelected ? 'var(--primary-light)' : 'rgba(0, 0, 0, 0.2)',
                    color: isSelected ? 'var(--primary-hover)' : 'var(--text-secondary)',
                  }}
                >
                  <div style={{ fontWeight: 600, fontSize: '13px' }}>{session.role}</div>
                  <div style={{ fontSize: '11px', opacity: 0.8, marginTop: '2px' }}>{session.email}</div>
                </button>
              );
            })}
          </div>
        </>
      )}

      {mode === 'login' && (
        <form onSubmit={handleLogin} style={{ display: 'flex', gap: 8, flexDirection: 'column' }}>
          <input className="input-control" placeholder="Email" value={loginEmail} onChange={(e) => setLoginEmail(e.target.value)} />
          <input className="input-control" placeholder="Password" type="password" value={loginPassword} onChange={(e) => setLoginPassword(e.target.value)} />
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" className="btn btn-primary">Login</button>
            <button type="button" className="btn btn-secondary" onClick={() => setMode('simulate')}>Back</button>
          </div>
        </form>
      )}

      {mode === 'register' && (
        <form onSubmit={handleRegister} style={{ display: 'flex', gap: 8, flexDirection: 'column' }}>
          <input className="input-control" placeholder="Full name" value={regFullName} onChange={(e) => setRegFullName(e.target.value)} />
          <input className="input-control" placeholder="Email" value={regEmail} onChange={(e) => setRegEmail(e.target.value)} />
          <input className="input-control" placeholder="Password" type="password" value={regPassword} onChange={(e) => setRegPassword(e.target.value)} />
          <input className="input-control" placeholder="Phone (optional)" value={regPhone} onChange={(e) => setRegPhone(e.target.value)} />
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" className="btn btn-primary">Register</button>
            <button type="button" className="btn btn-secondary" onClick={() => setMode('simulate')}>Back</button>
          </div>
        </form>
      )}

      <div style={infoBannerStyle}>
        <span style={{ color: 'var(--primary)' }}>★ Current Actor:</span>
        <strong style={{ color: '#fff', marginLeft: '6px' }}>{currentSession.fullName}</strong>
        <span style={badgeStyle}>{currentSession.role}</span>
        <div style={permsListStyle}>
          <span style={{ color: 'var(--text-muted)' }}>Permissions:</span>
          {currentSession.permissions.length === 0 ? (
            <span style={{ color: 'var(--danger)', fontSize: '12px', marginLeft: '6px' }}>NONE (ACCESS DENIED)</span>
          ) : (
            currentSession.permissions.map((p) => (
              <span key={p} style={permBadgeStyle}>{p}</span>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

const cardStyle: React.CSSProperties = {
  padding: '20px',
  marginBottom: '24px',
  background: 'rgba(26, 27, 38, 0.6)',
  border: '1px solid rgba(255, 255, 255, 0.05)',
};

const headerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '8px',
  marginBottom: '8px',
};

const dotStyle: React.CSSProperties = {
  width: '8px',
  height: '8px',
  borderRadius: '50%',
  background: 'var(--primary)',
  boxShadow: '0 0 8px var(--primary)',
};

const titleStyle: React.CSSProperties = {
  fontSize: '15px',
  fontWeight: 600,
  color: '#fff',
};

const descStyle: React.CSSProperties = {
  fontSize: '13px',
  color: 'var(--text-secondary)',
  marginBottom: '16px',
  lineHeight: '1.4',
};

const selectorContainerStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
  gap: '12px',
  marginBottom: '16px',
};

const btnStyle: React.CSSProperties = {
  padding: '12px',
  borderRadius: '8px',
  textAlign: 'left',
  transition: 'all 0.2s ease',
  cursor: 'pointer',
};

const infoBannerStyle: React.CSSProperties = {
  padding: '12px 16px',
  background: 'rgba(0, 0, 0, 0.3)',
  borderRadius: '8px',
  fontSize: '13px',
  display: 'flex',
  flexWrap: 'wrap',
  alignItems: 'center',
  gap: '8px',
};

const badgeStyle: React.CSSProperties = {
  padding: '2px 8px',
  borderRadius: '12px',
  fontSize: '11px',
  fontWeight: 700,
  background: 'var(--primary-light)',
  color: 'var(--primary-hover)',
  border: '1px solid rgba(139, 92, 246, 0.3)',
  marginLeft: '4px',
};

const permsListStyle: React.CSSProperties = {
  display: 'flex',
  flexWrap: 'wrap',
  alignItems: 'center',
  gap: '6px',
  width: '100%',
  marginTop: '8px',
  paddingTop: '8px',
  borderTop: '1px solid rgba(255, 255, 255, 0.05)',
};

const permBadgeStyle: React.CSSProperties = {
  padding: '1px 6px',
  borderRadius: '4px',
  fontSize: '10px',
  background: 'rgba(255, 255, 255, 0.05)',
  border: '1px solid rgba(255, 255, 255, 0.1)',
  color: 'var(--text-secondary)',
};
