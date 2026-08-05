import React, { useState, useEffect, useRef } from 'react';
import { API_BASE_URL, getAuthHeaders, getCurrentUser } from '../utils/auth';

interface Notification {
  id: number;
  userId: number;
  type: string;
  title: string;
  content: string;
  channel: string;
  isRead: boolean;
  createdAt: string;
}

export default function NotificationBell() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const user = getCurrentUser();

  useEffect(() => {
    if (!user) return;

    // Fetch initial notifications
    const fetchNotifications = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/notifications/user/${user.id}`, {
          headers: getAuthHeaders(),
        });
        if (res.ok) {
          const data = await res.json();
          setNotifications(data.notifications || []);
          setUnreadCount(data.unreadCount || 0);
        }
      } catch (err) {
        console.error("Failed to fetch notifications", err);
      }
    };

    fetchNotifications();

    // Set up SSE
    const sseUrl = `${API_BASE_URL}/notifications/stream/${user.id}`;
    const eventSource = new EventSource(sseUrl);

    eventSource.addEventListener('notification', (event) => {
      try {
        const newNotif = JSON.parse(event.data);
        setNotifications((prev) => [newNotif, ...prev]);
        setUnreadCount((prev) => prev + 1);
      } catch (e) {
        console.error("Error parsing SSE data", e);
      }
    });

    eventSource.onerror = (err) => {
      console.error("SSE Error:", err);
      // EventSource automatically reconnects
    };

    return () => {
      eventSource.close();
    };
  }, [user]);

  // Handle click outside to close dropdown
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleMarkAsRead = async (id: number) => {
    try {
      const res = await fetch(`${API_BASE_URL}/notifications/${id}/read`, {
        method: 'PUT',
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        setNotifications((prev) =>
          prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
    } catch (err) {
      console.error("Failed to mark notification as read", err);
    }
  };

  const getIconForType = (type: string) => {
    if (type === 'URGENT') return '🚨';
    if (type === 'PERIODIC') return '📊';
    return '🔔';
  };

  if (!user) return null;

  return (
    <div style={{ position: 'relative' }} ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        style={{
          background: 'rgba(255, 255, 255, 0.05)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '50%',
          width: '36px',
          height: '36px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          position: 'relative',
          transition: 'all 0.2s',
        }}
      >
        <span style={{ fontSize: '18px' }}>🔔</span>
        {unreadCount > 0 && (
          <span
            style={{
              position: 'absolute',
              top: '-4px',
              right: '-4px',
              background: '#ef4444',
              color: 'white',
              fontSize: '10px',
              fontWeight: 'bold',
              borderRadius: '10px',
              padding: '2px 6px',
              minWidth: '18px',
              textAlign: 'center',
            }}
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          style={{
            position: 'absolute',
            top: '48px',
            right: 0,
            width: '320px',
            background: 'rgba(15, 23, 42, 0.95)',
            backdropFilter: 'blur(12px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '12px',
            boxShadow: '0 10px 25px rgba(0, 0, 0, 0.5)',
            zIndex: 1000,
            overflow: 'hidden',
          }}
        >
          <div style={{ padding: '16px', borderBottom: '1px solid rgba(255, 255, 255, 0.1)' }}>
            <h3 style={{ margin: 0, fontSize: '16px', color: '#fff', fontWeight: 600 }}>Thông Báo</h3>
          </div>
          
          <div style={{ maxHeight: '350px', overflowY: 'auto' }}>
            {notifications.length === 0 ? (
              <div style={{ padding: '24px', textAlign: 'center', color: '#94a3b8', fontSize: '14px' }}>
                Không có thông báo nào.
              </div>
            ) : (
              notifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => !notif.isRead && handleMarkAsRead(notif.id)}
                  style={{
                    padding: '12px 16px',
                    borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
                    background: notif.isRead ? 'transparent' : 'rgba(16, 185, 129, 0.05)',
                    cursor: notif.isRead ? 'default' : 'pointer',
                    display: 'flex',
                    gap: '12px',
                    transition: 'background 0.2s',
                  }}
                >
                  <div style={{ fontSize: '20px', marginTop: '2px' }}>
                    {getIconForType(notif.type)}
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '4px' }}>
                      <strong style={{ fontSize: '13px', color: notif.isRead ? '#cbd5e1' : '#fff' }}>
                        {notif.title}
                      </strong>
                      {!notif.isRead && (
                        <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#ef4444', marginTop: '4px' }}></div>
                      )}
                    </div>
                    <p style={{ margin: 0, fontSize: '12px', color: '#94a3b8', lineHeight: 1.4 }}>
                      {notif.content}
                    </p>
                    <div style={{ fontSize: '10px', color: '#64748b', marginTop: '6px' }}>
                      {new Date(notif.createdAt).toLocaleString('vi-VN')}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
