import React, { useState, useEffect, useCallback, useRef } from 'react';
import type { AdminNotification } from '../types';
import { API_ORIGIN, authHeaders } from '../utils/api';

interface NotificationBellProps {
  email: string;
}

const TYPE_ICONS: Record<string, string> = {
  SUCCESS: '✅', WARNING: '⚠️', INFO: 'ℹ️', ERROR: '❌',
};

/**
 * Chuông thông báo real-time cho portal Admin (BICAP-77 / detail-design §4.2 Header).
 * Đọc cùng API với farm portal: GET /api/notifications (JWT).
 */
export const NotificationBell: React.FC<NotificationBellProps> = ({ email }) => {
  const [items, setItems] = useState<AdminNotification[]>([]);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);

  const load = useCallback(async () => {
    try {
      const res = await fetch(`${API_ORIGIN}/api/notifications`, { headers: authHeaders(email) });
      if (!res.ok) return;
      const data = await res.json();
      setItems(data.notifications || []);
      setUnread(data.unreadCount || 0);
    } catch { /* im lặng — nền chưa chạy thì thôi */ }
  }, [email]);

  useEffect(() => {
    load();
    const timer = setInterval(load, 30000);
    return () => clearInterval(timer);
  }, [load]);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const markRead = async (id: number) => {
    setLoading(true);
    try {
      await fetch(`${API_ORIGIN}/api/notifications/${id}/read`, { method: 'PUT', headers: authHeaders(email) });
      await load();
    } finally { setLoading(false); }
  };

  const markAllRead = async () => {
    setLoading(true);
    try {
      await fetch(`${API_ORIGIN}/api/notifications/read-all`, { method: 'PUT', headers: authHeaders(email) });
      await load();
    } finally { setLoading(false); }
  };

  return (
    <div ref={boxRef} style={{ position: 'relative' }}>
      <button
        onClick={() => { setOpen((o) => !o); if (!open) load(); }}
        title="Thông báo của bạn"
        style={{
          position: 'relative', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)',
          borderRadius: '10px', width: '40px', height: '40px', cursor: 'pointer', fontSize: '18px',
        }}
      >
        🔔
        {unread > 0 && (
          <span style={{
            position: 'absolute', top: '-6px', right: '-6px', minWidth: '18px', height: '18px',
            borderRadius: '9px', background: '#ef4444', color: '#fff', fontSize: '10px', fontWeight: 700,
            display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0 4px',
          }}>
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div style={{
          position: 'absolute', right: 0, top: '48px', width: '360px', maxHeight: '440px', zIndex: 3000,
          background: '#16171f', border: '1px solid var(--border-color)', borderRadius: '12px',
          boxShadow: '0 20px 50px rgba(0,0,0,0.6)', display: 'flex', flexDirection: 'column', overflow: 'hidden',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 16px', borderBottom: '1px solid var(--border-color)' }}>
            <strong style={{ color: '#fff', fontSize: '14px' }}>Thông báo</strong>
            {unread > 0 && (
              <button onClick={markAllRead} disabled={loading} style={{ background: 'none', border: 'none', color: '#a78bfa', fontSize: '12px', cursor: 'pointer' }}>
                Đánh dấu tất cả đã đọc
              </button>
            )}
          </div>
          <div style={{ overflowY: 'auto', flex: 1 }}>
            {items.length === 0 && (
              <p style={{ color: 'var(--text-muted)', fontSize: '13px', textAlign: 'center', padding: '28px 16px' }}>Chưa có thông báo nào.</p>
            )}
            {items.map((n) => (
              <div
                key={n.id}
                onClick={() => { if (!n.isRead) markRead(n.id); }}
                style={{
                  padding: '12px 16px', borderBottom: '1px solid rgba(255,255,255,0.04)', cursor: n.isRead ? 'default' : 'pointer',
                  background: n.isRead ? 'transparent' : 'rgba(139,92,246,0.06)',
                }}
              >
                <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
                  <span>{TYPE_ICONS[n.type] || '🔔'}</span>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ fontSize: '13px', fontWeight: n.isRead ? 500 : 700, color: '#fff' }}>{n.title}</div>
                    <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>{n.content}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>
                      {n.createdAt ? new Date(n.createdAt).toLocaleString('vi-VN') : ''}
                    </div>
                  </div>
                  {!n.isRead && <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#8b5cf6', flexShrink: 0, marginTop: '6px' }} />}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
