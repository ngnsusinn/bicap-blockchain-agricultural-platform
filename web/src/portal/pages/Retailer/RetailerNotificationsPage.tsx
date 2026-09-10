import { useEffect, useRef, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

/**
 * BICAP-47: Retailer nhận thông báo từ Farm Manager.
 * BICAP-50: Retailer nhận thông báo từ Shipper.
 *
 * Dùng lại NotificationController (/api/notifications + /api/notifications/stream)
 * và pattern đồng nhất với NotificationBell của Farm Manager.
 */

type Notification = {
  id: number;
  type: string;
  title: string;
  content: string;
  isRead: boolean;
  createdAt: string;
};

type NotificationListResponse = {
  unreadCount: number;
  notifications: Notification[];
};

const TYPE_ICON: Record<string, string> = {
  SUCCESS: '✅',
  WARNING: '⚠️',
  INFO: '📢',
  URGENT: '🚨',
  PERIODIC: '📊',
};

const formatDate = (iso: string) =>
  new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));

export default function RetailerNotificationsPage() {
  const [items, setItems] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<'ALL' | 'UNREAD'>('ALL');
  const eventSourceRef = useRef<EventSource | null>(null);

  // ── Tải thông báo ban đầu ──────────────────────────────────────────────────
  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE_URL}/notifications`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error(`Lỗi kết nối (${res.status})`);
      const body: NotificationListResponse = await res.json();
      setItems(body.notifications || []);
      setUnreadCount(body.unreadCount ?? 0);
    } catch (e: any) {
      setError(e.message || 'Không thể tải thông báo.');
    } finally {
      setLoading(false);
    }
  };

  // ── SSE — nhận thông báo realtime ─────────────────────────────────────────
  useEffect(() => {
    void load();

    const token = localStorage.getItem('accessToken');
    if (!token) return;

    // EventSource cannot send Authorization headers — JWT via ?token=
    const sseUrl = `${API_BASE_URL}/notifications/stream?token=${encodeURIComponent(token)}`;
    const es = new EventSource(sseUrl);
    eventSourceRef.current = es;

    es.addEventListener('notification', (e) => {
      try {
        const n: Notification = JSON.parse((e as MessageEvent).data);
        setItems((prev) => [n, ...prev]);
        setUnreadCount((prev) => prev + 1);
      } catch {
        // bỏ qua lỗi parse SSE
      }
    });

    es.onerror = () => {
      // EventSource tự reconnect, không cần xử lý thêm
    };

    return () => {
      es.close();
      eventSourceRef.current = null;
    };
  }, []);

  // ── Đánh dấu đã đọc ───────────────────────────────────────────────────────
  const markRead = async (id: number) => {
    setItems((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
    setUnreadCount((prev) => Math.max(0, prev - 1));
    try {
      await fetch(`${API_BASE_URL}/notifications/${id}/read`, {
        method: 'PUT',
        headers: getAuthHeaders(),
      });
    } catch {
      // không rollback UI để tránh flicker
    }
  };

  const markAllRead = async () => {
    setItems((prev) => prev.map((n) => ({ ...n, isRead: true })));
    setUnreadCount(0);
    try {
      await fetch(`${API_BASE_URL}/notifications/read-all`, {
        method: 'PUT',
        headers: getAuthHeaders(),
      });
    } catch {
      // silent
    }
  };

  const displayed = filter === 'UNREAD' ? items.filter((n) => !n.isRead) : items;

  return (
    <section style={{ color: '#e2e8f0' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 12, marginBottom: 20 }}>
        <div>
          <h1 style={{ margin: '0 0 6px', fontSize: 24, fontWeight: 700 }}>Thông Báo</h1>
          <p style={{ margin: 0, color: '#94a3b8', fontSize: 13 }}>
            Nhận cập nhật từ Farm Manager và người vận chuyển (đang giao, đã giao, sự cố).
          </p>
        </div>
        {unreadCount > 0 && (
          <span style={s.unreadBadge}>{unreadCount} chưa đọc</span>
        )}
      </div>

      {/* Toolbar */}
      <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 16, flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', gap: 6 }}>
          {(['ALL', 'UNREAD'] as const).map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              style={{ ...s.filterBtn, ...(filter === f ? s.filterBtnActive : {}) }}
            >
              {f === 'ALL' ? 'Tất cả' : 'Chưa đọc'}
            </button>
          ))}
        </div>
        {unreadCount > 0 && (
          <button onClick={() => void markAllRead()} style={s.secondaryBtn}>
            ✓ Đánh dấu tất cả đã đọc
          </button>
        )}
        <button onClick={() => void load()} style={s.secondaryBtn} title="Làm mới">
          🔄 Làm mới
        </button>
      </div>

      {/* Error */}
      {error && (
        <div style={s.errorBox} role="alert">{error}</div>
      )}

      {/* Loading */}
      {loading && (
        <div style={s.emptyBox}>Đang tải thông báo...</div>
      )}

      {/* Empty */}
      {!loading && !error && displayed.length === 0 && (
        <div style={s.emptyBox}>
          <div style={{ fontSize: 36, marginBottom: 12 }}>🔔</div>
          <p>{filter === 'UNREAD' ? 'Không có thông báo chưa đọc.' : 'Bạn chưa có thông báo nào.'}</p>
        </div>
      )}

      {/* List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {displayed.map((n) => (
          <article
            key={n.id}
            onClick={() => !n.isRead && void markRead(n.id)}
            style={{
              ...s.card,
              background: n.isRead ? 'rgba(255,255,255,0.02)' : 'rgba(6,182,212,0.06)',
              borderColor: n.isRead ? '#334155' : 'rgba(6,182,212,0.35)',
              cursor: n.isRead ? 'default' : 'pointer',
            }}
          >
            <div style={{ display: 'flex', gap: 14, alignItems: 'flex-start' }}>
              <span style={{ fontSize: 22, marginTop: 2, flexShrink: 0 }}>
                {TYPE_ICON[n.type] ?? '🔔'}
              </span>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <strong style={{ color: n.isRead ? '#cbd5e1' : '#f1f5f9', fontSize: 14 }}>
                    {n.title}
                  </strong>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexShrink: 0 }}>
                    {!n.isRead && <span style={s.dot} />}
                    <span style={{ fontSize: 11, color: '#64748b' }}>{formatDate(n.createdAt)}</span>
                  </div>
                </div>
                <p style={{ margin: '5px 0 0', fontSize: 13, color: '#94a3b8', lineHeight: 1.5 }}>
                  {n.content}
                </p>
                {!n.isRead && (
                  <button
                    onClick={(e) => { e.stopPropagation(); void markRead(n.id); }}
                    style={{ ...s.secondaryBtn, marginTop: 8, fontSize: 11, padding: '4px 10px' }}
                  >
                    Đánh dấu đã đọc
                  </button>
                )}
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

const s: Record<string, React.CSSProperties> = {
  unreadBadge: {
    padding: '5px 12px',
    borderRadius: 999,
    background: 'rgba(6,182,212,0.15)',
    border: '1px solid rgba(6,182,212,0.35)',
    color: '#38bdf8',
    fontSize: 12,
    fontWeight: 700,
  },
  filterBtn: {
    padding: '8px 14px',
    borderRadius: 999,
    border: '1px solid #334155',
    background: '#0f172a',
    color: '#94a3b8',
    cursor: 'pointer',
    fontSize: 13,
  },
  filterBtnActive: {
    borderColor: '#0891b2',
    background: 'rgba(6,182,212,0.16)',
    color: '#ecfeff',
  },
  secondaryBtn: {
    padding: '7px 13px',
    borderRadius: 8,
    border: '1px solid #475569',
    background: '#1e293b',
    color: '#e2e8f0',
    cursor: 'pointer',
    fontSize: 13,
    fontWeight: 600,
  },
  errorBox: {
    padding: '11px 14px',
    borderRadius: 10,
    color: '#fecaca',
    background: 'rgba(239,68,68,.13)',
    marginBottom: 12,
  },
  emptyBox: {
    textAlign: 'center',
    padding: '48px 24px',
    border: '1px dashed #334155',
    borderRadius: 14,
    color: '#94a3b8',
  },
  card: {
    padding: '14px 16px',
    borderRadius: 12,
    border: '1px solid #334155',
    transition: 'border-color 0.15s, background 0.15s',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: '50%',
    background: '#38bdf8',
    display: 'inline-block',
    flexShrink: 0,
  },
};
