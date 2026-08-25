import React, { useState, useEffect } from 'react';
import { getAuthHeaders, API_BASE_URL } from '../../utils/auth';

/**
 * BICAP-69: Guest - Nhận thông báo chung
 */

export interface SystemNotification {
  id: number;
  title: string;
  message: string;
  category: 'PRODUCT' | 'ARTICLE' | 'EVENT' | 'SYSTEM';
  createdAt: string;
  isRead: boolean;
  linkUrl?: string;
}

export default function GuestNotifications() {
  const [notifications, setNotifications] = useState<SystemNotification[]>([]);
  const [filter, setFilter] = useState<'ALL' | 'PRODUCT' | 'ARTICLE' | 'EVENT'>('ALL');
  const [selectedNotif, setSelectedNotif] = useState<SystemNotification | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [errorMsg, setErrorMsg] = useState<string>('');

  // Tải thông báo thực tế từ Backend API & Database
  const fetchNotifications = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const res = await fetch(`${API_BASE_URL}/notifications`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) {
        throw new Error(`Lỗi kết nối Backend (Mã lỗi: ${res.status})`);
      }
      const body = await res.json();
      // Parse NotificationListResponse { unreadCount, notifications: [...] }
      const rawList = Array.isArray(body) ? body : (body.notifications || []);
      const mappedList: SystemNotification[] = rawList.map((item: any) => ({
        id: item.id,
        title: item.title || 'Thông báo từ hệ thống BICAP',
        message: item.content || item.message || '',
        category: (item.type as any) || 'SYSTEM',
        createdAt: item.createdAt || new Date().toISOString(),
        isRead: item.isRead ?? false,
      }));
      setNotifications(mappedList);
    } catch (err: any) {
      setErrorMsg(err.message || 'Không thể tải thông báo từ server Backend.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const unreadCount = notifications.filter(n => !n.isRead).length;

  const filteredList = notifications.filter(n => {
    if (filter === 'ALL') return true;
    return n.category === filter;
  });

  const markAsRead = async (id: number) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
    try {
      await fetch(`${API_BASE_URL}/notifications/${id}/read`, {
        method: 'PUT',
        headers: getAuthHeaders(),
      });
    } catch (e) {
      // Ignore network errors on mutation
    }
  };

  const markAllAsRead = async () => {
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    try {
      await fetch(`${API_BASE_URL}/notifications/read-all`, {
        method: 'PUT',
        headers: getAuthHeaders(),
      });
    } catch (e) {
      // Ignore network errors on mutation
    }
  };

  const getCategoryBadge = (cat: SystemNotification['category']) => {
    switch (cat) {
      case 'PRODUCT':
        return { label: '📦 Sản phẩm mới', color: '#10b981', bg: 'rgba(16, 185, 129, 0.15)' };
      case 'ARTICLE':
        return { label: '📖 Bài viết', color: '#38bdf8', bg: 'rgba(56, 189, 248, 0.15)' };
      case 'EVENT':
        return { label: '🎪 Sự kiện', color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.15)' };
      default:
        return { label: '📢 Thông báo', color: '#a855f7', bg: 'rgba(168, 85, 247, 0.15)' };
    }
  };

  const formatDate = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div style={containerStyle}>
      {/* Header Section */}
      <div style={headerSectionStyle}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h1 style={titleStyle}>🔔 Thông Báo Nền Tảng BICAP</h1>
            {unreadCount > 0 && (
              <span style={unreadBadgeStyle}>{unreadCount} chưa đọc</span>
            )}
          </div>
          <p style={subtitleStyle}>
            Cập nhật thông báo thời gian thực trực tiếp từ hệ thống Backend & Database BICAP.
          </p>
        </div>

        {unreadCount > 0 && (
          <button onClick={markAllAsRead} style={markAllBtnStyle}>
            ✓ Đánh dấu tất cả đã đọc
          </button>
        )}
      </div>

      {/* Filter Tabs */}
      <div style={tabsContainerStyle}>
        {[
          { key: 'ALL', label: 'Tất cả thông báo' },
          { key: 'PRODUCT', label: '📦 Sản phẩm' },
          { key: 'ARTICLE', label: '📖 Bài viết' },
          { key: 'EVENT', label: '🎪 Sự kiện' },
        ].map(tab => (
          <button
            key={tab.key}
            onClick={() => setFilter(tab.key as any)}
            style={{
              ...tabButtonStyle,
              color: filter === tab.key ? '#fff' : 'var(--text-secondary)',
              background: filter === tab.key ? 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)' : 'rgba(255, 255, 255, 0.05)',
              borderColor: filter === tab.key ? '#10b981' : 'rgba(255, 255, 255, 0.1)',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Notifications List State */}
      {loading ? (
        <div style={emptyStateStyle}>⏳ Đang tải danh sách thông báo từ Database Backend...</div>
      ) : errorMsg ? (
        <div style={{ ...emptyStateStyle, color: '#fca5a5', border: '1px solid rgba(239, 68, 68, 0.3)' }}>
          <div style={{ fontSize: '32px', marginBottom: '8px' }}>⚠️</div>
          <p>{errorMsg}</p>
          <button onClick={fetchNotifications} style={{ ...markAllBtnStyle, marginTop: '12px' }}>Tải lại</button>
        </div>
      ) : filteredList.length === 0 ? (
        <div style={emptyStateStyle}>
          <div style={{ fontSize: '40px', marginBottom: '12px' }}>🔕</div>
          <p>Hiện chưa có thông báo nào trong Database của dự án.</p>
        </div>
      ) : (
        <div style={listStyle}>
          {filteredList.map(item => {
            const badge = getCategoryBadge(item.category);
            return (
              <div
                key={item.id}
                onClick={() => {
                  markAsRead(item.id);
                  setSelectedNotif(item);
                }}
                style={{
                  ...cardStyle,
                  borderLeft: item.isRead ? '4px solid transparent' : '4px solid #10b981',
                  background: item.isRead ? 'rgba(255, 255, 255, 0.02)' : 'rgba(16, 185, 129, 0.06)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ ...badgeStyle, color: badge.color, background: badge.bg }}>
                      {badge.label}
                    </span>
                    {!item.isRead && <span style={dotStyle} />}
                  </div>
                  <span style={timeStyle}>{formatDate(item.createdAt)}</span>
                </div>

                <h3 style={{ ...cardTitleStyle, color: item.isRead ? '#e2e8f0' : '#fff' }}>
                  {item.title}
                </h3>
                <p style={cardSnippetStyle}>{item.message}</p>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal Detail */}
      {selectedNotif && (
        <div style={modalOverlayStyle} onClick={() => setSelectedNotif(null)}>
          <div style={modalContentStyle} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <span style={{ ...badgeStyle, ...getCategoryBadge(selectedNotif.category) }}>
                {getCategoryBadge(selectedNotif.category).label}
              </span>
              <button onClick={() => setSelectedNotif(null)} style={closeBtnStyle}>✕</button>
            </div>

            <h2 style={{ color: '#fff', fontSize: '18px', fontWeight: 700, marginBottom: '8px' }}>
              {selectedNotif.title}
            </h2>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Thời gian: {formatDate(selectedNotif.createdAt)}
            </p>

            <div style={{ fontSize: '14px', color: '#cbd5e1', lineHeight: 1.6, marginBottom: '24px' }}>
              {selectedNotif.message}
            </div>

            {selectedNotif.linkUrl && (
              <a
                href={selectedNotif.linkUrl}
                target="_blank"
                rel="noreferrer"
                style={actionBtnStyle}
              >
                🔗 Xem chi tiết liên kết
              </a>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

/* ── Inline Styles matching App Theme ── */
const containerStyle: React.CSSProperties = {
  padding: '24px',
  maxWidth: '900px',
  margin: '0 auto',
};

const headerSectionStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'flex-start',
  flexWrap: 'wrap',
  gap: '16px',
  marginBottom: '24px',
};

const titleStyle: React.CSSProperties = {
  fontSize: '24px',
  fontWeight: 800,
  color: '#fff',
  margin: 0,
};

const subtitleStyle: React.CSSProperties = {
  fontSize: '14px',
  color: 'var(--text-secondary)',
  marginTop: '6px',
};

const unreadBadgeStyle: React.CSSProperties = {
  background: '#ef4444',
  color: '#fff',
  fontSize: '11px',
  fontWeight: 700,
  padding: '2px 8px',
  borderRadius: '12px',
};

const markAllBtnStyle: React.CSSProperties = {
  background: 'rgba(16, 185, 129, 0.15)',
  border: '1px solid rgba(16, 185, 129, 0.3)',
  color: '#34d399',
  padding: '8px 14px',
  borderRadius: '8px',
  fontSize: '13px',
  fontWeight: 600,
  cursor: 'pointer',
};

const tabsContainerStyle: React.CSSProperties = {
  display: 'flex',
  gap: '10px',
  flexWrap: 'wrap',
  marginBottom: '20px',
};

const tabButtonStyle: React.CSSProperties = {
  padding: '8px 16px',
  borderRadius: '20px',
  border: '1px solid',
  fontSize: '13px',
  fontWeight: 600,
  cursor: 'pointer',
  transition: 'all 0.2s ease',
};

const listStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '12px',
};

const cardStyle: React.CSSProperties = {
  borderRadius: '12px',
  padding: '16px 20px',
  cursor: 'pointer',
  transition: 'transform 0.15s ease, background 0.15s ease',
  border: '1px solid rgba(255, 255, 255, 0.08)',
};

const badgeStyle: React.CSSProperties = {
  fontSize: '11px',
  fontWeight: 700,
  padding: '3px 10px',
  borderRadius: '6px',
};

const dotStyle: React.CSSProperties = {
  width: '8px',
  height: '8px',
  borderRadius: '50%',
  background: '#10b981',
  display: 'inline-block',
};

const timeStyle: React.CSSProperties = {
  fontSize: '11px',
  color: 'var(--text-muted)',
};

const cardTitleStyle: React.CSSProperties = {
  fontSize: '15px',
  fontWeight: 700,
  margin: '10px 0 6px 0',
};

const cardSnippetStyle: React.CSSProperties = {
  fontSize: '13px',
  color: 'var(--text-secondary)',
  margin: 0,
  lineHeight: 1.5,
};

const emptyStateStyle: React.CSSProperties = {
  textAlign: 'center',
  padding: '60px 20px',
  color: 'var(--text-muted)',
  fontSize: '14px',
  background: 'rgba(255, 255, 255, 0.02)',
  borderRadius: '12px',
  border: '1px solid rgba(255, 255, 255, 0.05)',
};

const modalOverlayStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  background: 'rgba(0, 0, 0, 0.75)',
  backdropFilter: 'blur(6px)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 2000,
  padding: '20px',
};

const modalContentStyle: React.CSSProperties = {
  background: '#1e293b',
  border: '1px solid rgba(255, 255, 255, 0.15)',
  borderRadius: '16px',
  padding: '28px',
  maxWidth: '520px',
  width: '100%',
  boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
};

const closeBtnStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  color: 'var(--text-muted)',
  fontSize: '18px',
  cursor: 'pointer',
};

const actionBtnStyle: React.CSSProperties = {
  display: 'inline-block',
  background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)',
  color: '#fff',
  textDecoration: 'none',
  padding: '10px 18px',
  borderRadius: '8px',
  fontSize: '13px',
  fontWeight: 700,
  textAlign: 'center',
};
