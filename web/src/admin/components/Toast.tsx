import React, { useEffect } from 'react';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  text: string;
}

interface ToastProps {
  toasts: ToastMessage[];
  onClose: (id: string) => void;
}

export const Toast: React.FC<ToastProps> = ({ toasts, onClose }) => {
  return (
    <div style={containerStyle}>
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onClose={onClose} />
      ))}
    </div>
  );
};

const ToastItem: React.FC<{ toast: ToastMessage; onClose: (id: string) => void }> = ({ toast, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose(toast.id);
    }, 4000);
    return () => clearTimeout(timer);
  }, [toast.id, onClose]);

  const config = toastConfig[toast.type];

  return (
    <div className="glass-panel" style={{ ...itemStyle, borderLeft: `4px solid ${config.color}` }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', width: '100%' }}>
        <span style={{ fontSize: '20px', color: config.color }}>{config.icon}</span>
        <div style={{ flex: 1 }}>
          <p style={{ fontSize: '14px', fontWeight: 600, color: '#f3f4f6' }}>
            {toast.type.charAt(0).toUpperCase() + toast.type.slice(1)}
          </p>
          <p style={{ fontSize: '13px', color: '#9ca3af', marginTop: '2px' }}>{toast.text}</p>
        </div>
        <button 
          onClick={() => onClose(toast.id)} 
          style={closeBtnStyle}
          aria-label="Close notification"
        >
          &times;
        </button>
      </div>
    </div>
  );
};

const toastConfig = {
  success: { color: 'var(--success)', icon: '✓' },
  error: { color: 'var(--danger)', icon: '✗' },
  warning: { color: 'var(--warning)', icon: '⚠' },
  info: { color: 'var(--secondary)', icon: 'ℹ' },
};

const containerStyle: React.CSSProperties = {
  position: 'fixed',
  top: '24px',
  right: '24px',
  display: 'flex',
  flexDirection: 'column',
  gap: '12px',
  zIndex: 9999,
  pointerEvents: 'none',
};

const itemStyle: React.CSSProperties = {
  padding: '16px',
  minWidth: '320px',
  maxWidth: '420px',
  display: 'flex',
  alignItems: 'center',
  background: 'rgba(22, 23, 33, 0.9)',
  pointerEvents: 'auto',
  animation: 'slideInRight 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards',
};

const closeBtnStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  fontSize: '18px',
  color: 'var(--text-muted)',
  cursor: 'pointer',
  padding: '4px',
  lineHeight: 1,
};
