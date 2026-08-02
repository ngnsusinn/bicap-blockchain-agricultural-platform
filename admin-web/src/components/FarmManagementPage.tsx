import React, { useState, useEffect, useCallback, useRef } from 'react';
import type { UserSession, FarmRegistration, FarmCertification } from '../types';
import { API_ORIGIN, authHeaders, formatDate } from '../utils/api';
import { STATUS_META, StatusBadge } from './StatusBadge';

interface FarmManagementPageProps {
  currentSession: UserSession;
  onToast: (text: string, type?: 'info' | 'success' | 'error') => void;
}

const TABS = [
  { id: '', label: '📋 Tất Cả' },
  { id: 'APPROVED', label: '✅ Đang Hoạt Động' },
  { id: 'PENDING', label: '⏳ Chờ Duyệt' },
  { id: 'REJECTED', label: '❌ Bị Từ Chối' },
  { id: 'SUSPENDED', label: '🛑 Tạm Ngưng' },
  { id: 'INACTIVE', label: '⚪ Ngừng Hoạt Động' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const STATUS_OPTIONS = [
  { id: 'APPROVED', label: '✅ Đang hoạt động' },
  { id: 'SUSPENDED', label: '🛑 Tạm ngưng' },
  { id: 'INACTIVE', label: '⚪ Ngừng hoạt động' },
];

export const FarmManagementPage: React.FC<FarmManagementPageProps> = ({ currentSession, onToast }) => {
  const [tab, setTab] = useState<TabId>('APPROVED');
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [farms, setFarms] = useState<FarmRegistration[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedFarm, setSelectedFarm] = useState<FarmRegistration | null>(null);
  const [statusFarm, setStatusFarm] = useState<FarmRegistration | null>(null);
  const [newStatus, setNewStatus] = useState('');
  const [notesFarm, setNotesFarm] = useState<FarmRegistration | null>(null);
  const [notesText, setNotesText] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const detailSeqRef = useRef(0);

  const canManage = currentSession.role === 'SUPER_ADMIN' || currentSession.role === 'ADMIN';

  // Debounce the search input so typing doesn't fire one request per keystroke.
  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchTerm), 300);
    return () => clearTimeout(t);
  }, [searchTerm]);

  const fetchFarms = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ status: tab, search: debouncedSearch, page: page.toString(), size: '10' });
      const res = await fetch(`${API_ORIGIN}/api/admin/farms?${params}`, {
        headers: authHeaders(currentSession.email),
        signal,
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Lỗi tải danh sách nông trại.');
      }
      const data = await res.json();
      setFarms(data.content || []);
      const totalPagesValue = Math.max(1, data.totalPages || 1);
      setTotalPages(totalPagesValue);
      // Clamp the current page when the list shrank (e.g. after a status change moved the row out).
      if (page >= totalPagesValue) {
        setPage(Math.max(0, totalPagesValue - 1));
      }
    } catch (err: any) {
      if (err?.name === 'AbortError') return; // superseded request — ignore
      setFarms([]);
      setTotalPages(1);
      onToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, debouncedSearch, page, currentSession, onToast]);

  // Abort the in-flight request whenever a new fetch supersedes it.
  useEffect(() => {
    const controller = new AbortController();
    fetchFarms(controller.signal);
    return () => controller.abort();
  }, [fetchFarms]);

  // ── Open detail (fetches certifications + full info); sequence-guarded and
  // ── surfaces fetch errors instead of silently showing summary-only data.
  const handleViewDetail = async (farm: FarmRegistration) => {
    const seq = ++detailSeqRef.current;
    setSelectedFarm(farm);
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/farms/${farm.id}`, { headers: authHeaders(currentSession.email) });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        if (seq === detailSeqRef.current) setSelectedFarm(null);
        onToast(err.message || 'Không thể tải chi tiết hồ sơ.', 'error');
        return;
      }
      const data = await res.json();
      if (seq === detailSeqRef.current) setSelectedFarm({ ...farm, ...data });
    } catch (err: any) {
      if (seq === detailSeqRef.current) setSelectedFarm(null);
      onToast(err.message || 'Không thể tải chi tiết hồ sơ.', 'error');
    }
  };

  // ── Change operating status ──
  const openStatusModal = (farm: FarmRegistration) => {
    if (!canManage) { onToast('Bạn không có quyền thay đổi trạng thái nông trại.', 'error'); return; }
    setStatusFarm(farm);
    setNewStatus(farm.status === 'APPROVED' ? 'SUSPENDED' : 'APPROVED');
  };

  const confirmStatusChange = async () => {
    if (!statusFarm) return;
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/farms/${statusFarm.id}/status`, {
        method: 'PUT',
        headers: { ...authHeaders(currentSession.email), 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus }),
      });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || 'Cập nhật trạng thái thất bại.'); }
      onToast(`Đã cập nhật trạng thái "${statusFarm.name}" thành "${STATUS_META[newStatus].label}".`, 'success');
      setStatusFarm(null);
      setSelectedFarm(null);
      fetchFarms();
    } catch (err: any) {
      onToast(err.message, 'error');
    }
  };

  // ── Edit admin notes ──
  const openNotesModal = (farm: FarmRegistration) => {
    if (!canManage) { onToast('Bạn không có quyền chỉnh sửa ghi chú.', 'error'); return; }
    setNotesFarm(farm);
    setNotesText(farm.adminNotes || '');
  };

  const confirmNotesSave = async () => {
    if (!notesFarm) return;
    // Trim BEFORE validating/sending so surrounding whitespace never trips the
    // 2000-char limit (mirrors the backend trim-first behaviour).
    const trimmed = notesText.trim();
    if (trimmed.length > 2000) {
      onToast('Ghi chú không được vượt quá 2000 ký tự.', 'error');
      return;
    }
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/farms/${notesFarm.id}/notes`, {
        method: 'PUT',
        headers: { ...authHeaders(currentSession.email), 'Content-Type': 'application/json' },
        body: JSON.stringify({ notes: trimmed }),
      });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || 'Lưu ghi chú thất bại.'); }
      onToast('Đã lưu ghi chú Admin.', 'success');
      setNotesFarm(null);
      setSelectedFarm(null);
      fetchFarms();
    } catch (err: any) {
      onToast(err.message, 'error');
    }
  };

  const hasGps = (f: FarmRegistration) => f.gpsLat != null && f.gpsLng != null;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="dashboard-title">Quản Lý Nông Trại</h1>
          <p className="dashboard-subtitle">
            Xem và quản lý thông tin chi tiết của tất cả nông trại: chứng nhận, liên hệ, vị trí, trạng thái hoạt động và ghi chú nội bộ.
          </p>
        </div>
      </div>

      {/* Status tabs */}
      <div style={{ display: 'flex', gap: '10px', marginBottom: '20px', flexWrap: 'wrap' }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => { setTab(t.id); setPage(0); }}
            style={{
              padding: '10px 18px', borderRadius: '10px', border: '1px solid var(--border-color)',
              background: tab === t.id ? 'rgba(139,92,246,0.15)' : 'rgba(255,255,255,0.03)',
              color: tab === t.id ? '#c4b5fd' : 'var(--text-secondary)',
              fontWeight: tab === t.id ? 700 : 500, fontSize: '13px', cursor: 'pointer',
              transition: 'all 0.2s ease',
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="glass-panel" style={{ padding: '24px' }}>
        {/* Search */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: '8px', background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border-color)', borderRadius: '8px', padding: '0 12px' }}>
            <span>🔍</span>
            <input
              type="text"
              placeholder="Tìm theo tên nông trại hoặc địa chỉ..."
              value={searchTerm}
              onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
              style={{ flex: 1, background: 'transparent', border: 'none', outline: 'none', color: '#fff', padding: '12px 0', fontSize: '13px' }}
            />
          </div>
        </div>

        {/* Table */}
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
                <th style={thStyle}>ID</th>
                <th style={thStyle}>Tên Nông Trại</th>
                <th style={thStyle}>Chủ Sở Hữu</th>
                <th style={thStyle}>Địa Chỉ</th>
                <th style={thStyle}>Diện Tích</th>
                <th style={thStyle}>Loại Sản Phẩm</th>
                <th style={thStyle}>Chứng Nhận</th>
                <th style={thStyle}>Trạng Thái</th>
                <th style={thStyle}>Thao Tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={9} style={emptyStyle}>Đang tải dữ liệu...</td></tr>
              ) : farms.length === 0 ? (
                <tr><td colSpan={9} style={emptyStyle}>Không tìm thấy nông trại nào trong danh mục này.</td></tr>
              ) : (
                farms.map((farm) => (
                  <tr key={farm.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <td style={tdStyle}>{farm.id}</td>
                    <td style={tdStyle}><span style={{ fontWeight: 600, color: '#fff' }}>{farm.name}</span></td>
                    <td style={tdStyle}>
                      <div>{farm.ownerName || '—'}</div>
                      <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{farm.ownerEmail || ''}</div>
                    </td>
                    <td style={tdStyle}><span style={{ fontSize: '12px' }}>{farm.address}</span></td>
                    <td style={tdStyle}>{farm.area} ha</td>
                    <td style={tdStyle}><span style={{ fontSize: '12px' }}>{farm.productTypes || '—'}</span></td>
                    <td style={tdStyle}>
                      <span
                        style={{
                          padding: '3px 10px', borderRadius: '10px', fontSize: '11px', fontWeight: 600,
                          color: farm.certificationCount > 0 ? '#6ee7b7' : '#fca5a5',
                          background: farm.certificationCount > 0 ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.12)',
                        }}
                      >
                        {farm.certificationCount > 0 ? `${farm.certificationCount} chứng nhận` : 'Không có'}
                      </span>
                    </td>
                    <td style={tdStyle}><StatusBadge status={farm.status} /></td>
                    <td style={tdStyle}>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button onClick={() => handleViewDetail(farm)} style={actionBtnStyle} title="Xem chi tiết">👁️</button>
                        <button onClick={() => openNotesModal(farm)} style={actionBtnStyle} title="Ghi chú Admin">📝</button>
                        <button onClick={() => openStatusModal(farm)} style={{ ...actionBtnStyle, background: 'rgba(139,92,246,0.12)', borderColor: 'rgba(139,92,246,0.3)' }} title="Đổi trạng thái hoạt động">🔄</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '16px', alignItems: 'center' }}>
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="btn btn-secondary"
              style={{ padding: '8px 14px', fontSize: '12px' }}
            >
              ← Trước
            </button>
            <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
              Trang {page + 1} / {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="btn btn-secondary"
              style={{ padding: '8px 14px', fontSize: '12px' }}
            >
              Sau →
            </button>
          </div>
        )}
      </div>

      {/* ── Detail modal ── */}
      {selectedFarm && (
        <div style={overlayStyle} onClick={() => setSelectedFarm(null)}>
          <div style={{ ...modalStyle, maxWidth: '760px' }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
              <div>
                <h2 style={{ margin: 0, color: '#fff', fontSize: '20px', fontWeight: 700 }}>{selectedFarm.name}</h2>
                <p style={{ margin: '6px 0 0', color: 'var(--text-secondary)', fontSize: '13px' }}>{selectedFarm.address}</p>
              </div>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                <StatusBadge status={selectedFarm.status} />
                <button onClick={() => setSelectedFarm(null)} style={closeBtnStyle}>✕</button>
              </div>
            </div>

            {/* Owner / contact */}
            <div style={sectionStyle}>
              <h3 style={sectionTitleStyle}>👤 Liên Hệ Chủ Sở Hữu</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <InfoField label="Họ tên" value={selectedFarm.ownerName || '—'} />
                <InfoField label="Email" value={selectedFarm.ownerEmail || '—'} />
                <InfoField label="Số điện thoại" value={selectedFarm.ownerPhone || '—'} />
                <InfoField label="Ngày đăng ký" value={formatDate(selectedFarm.createdAt)} />
              </div>
            </div>

            {/* Farm info */}
            <div style={sectionStyle}>
              <h3 style={sectionTitleStyle}>🏞️ Thông Tin Nông Trại</h3>
              {selectedFarm.description && (
                <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: '1.6', margin: '0 0 12px' }}>
                  {selectedFarm.description}
                </p>
              )}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <InfoField label="Diện tích" value={`${selectedFarm.area} ha`} />
                <InfoField label="Loại sản phẩm" value={selectedFarm.productTypes || '—'} />
                <InfoField label="Ngày cập nhật" value={formatDate(selectedFarm.updatedAt)} />
                <InfoField
                  label="Vị trí (GPS)"
                  value={hasGps(selectedFarm)
                    ? `${selectedFarm.gpsLat!.toFixed(6)}, ${selectedFarm.gpsLng!.toFixed(6)}`
                    : '—'}
                />
              </div>
              {hasGps(selectedFarm) && (
                <a
                  href={`https://www.google.com/maps?q=${selectedFarm.gpsLat},${selectedFarm.gpsLng}`}
                  target="_blank"
                  rel="noreferrer"
                  style={{
                    display: 'inline-block', marginTop: '12px', padding: '8px 14px', borderRadius: '8px',
                    fontSize: '12px', fontWeight: 600, background: 'rgba(6,182,212,0.12)',
                    border: '1px solid rgba(6,182,212,0.3)', color: '#67e8f9', textDecoration: 'none',
                  }}
                >
                  🗺️ Xem vị trí trên Google Maps
                </a>
              )}
            </div>

            {/* Certifications */}
            <div style={sectionStyle}>
              <h3 style={sectionTitleStyle}>📄 Giấy Phép & Chứng Nhận</h3>
              {!selectedFarm.certifications || selectedFarm.certifications.length === 0 ? (
                <p style={{ color: '#fca5a5', fontSize: '13px' }}>⚠️ Nông trại chưa đính kèm giấy phép kinh doanh hoặc chứng nhận.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {selectedFarm.certifications.map((cert: FarmCertification) => (
                    <div
                      key={cert.id}
                      style={{
                        display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px',
                        padding: '12px 14px', borderRadius: '8px', background: 'rgba(255,255,255,0.03)',
                        border: '1px solid var(--border-color)',
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '13px', color: '#fff' }}>{cert.type}</div>
                        <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                          Hết hạn: {formatDate(cert.expiryDate)}
                        </div>
                      </div>
                      <a
                        href={cert.fileUrl}
                        target="_blank"
                        rel="noreferrer"
                        style={{
                          padding: '6px 12px', borderRadius: '6px', fontSize: '12px', fontWeight: 600,
                          background: 'rgba(139,92,246,0.15)', border: '1px solid rgba(139,92,246,0.3)',
                          color: '#c4b5fd', textDecoration: 'none', whiteSpace: 'nowrap',
                        }}
                      >
                        📎 Xem tài liệu
                      </a>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Admin notes */}
            <div style={sectionStyle}>
              <h3 style={sectionTitleStyle}>🗒️ Ghi Chú Admin</h3>
              <p style={{ fontSize: '13px', color: selectedFarm.adminNotes ? '#fff' : 'var(--text-muted)', lineHeight: '1.6', margin: 0, whiteSpace: 'pre-wrap' }}>
                {selectedFarm.adminNotes || 'Chưa có ghi chú nội bộ.'}
              </p>
              {canManage && (
                <button onClick={() => openNotesModal(selectedFarm)} className="btn btn-secondary" style={{ marginTop: '12px', padding: '8px 14px', fontSize: '12px' }}>
                  ✏️ {selectedFarm.adminNotes ? 'Sửa ghi chú' : 'Thêm ghi chú'}
                </button>
              )}
            </div>

            {/* Actions */}
            {canManage && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button onClick={() => openStatusModal(selectedFarm)} className="btn btn-secondary">
                  🔄 Đổi trạng thái hoạt động
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── Status change modal ── */}
      {statusFarm && (
        <div style={overlayStyle} onClick={() => setStatusFarm(null)}>
          <div style={{ ...modalStyle, maxWidth: '440px' }} onClick={(e) => e.stopPropagation()}>
            <h3 style={{ margin: '0 0 16px', color: '#fff', fontSize: '17px', fontWeight: 700 }}>
              🔄 Đổi trạng thái — {statusFarm.name}
            </h3>
            <p style={{ margin: '0 0 14px', color: 'var(--text-secondary)', fontSize: '13px' }}>
              Trạng thái hiện tại: <StatusBadge status={statusFarm.status} />
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {STATUS_OPTIONS.map((opt) => (
                <button
                  key={opt.id}
                  onClick={() => setNewStatus(opt.id)}
                  style={{
                    padding: '12px 14px', borderRadius: '8px', border: '1px solid var(--border-color)',
                    background: newStatus === opt.id ? 'rgba(139,92,246,0.15)' : 'rgba(255,255,255,0.03)',
                    color: newStatus === opt.id ? '#c4b5fd' : 'var(--text-secondary)',
                    fontWeight: newStatus === opt.id ? 700 : 500, fontSize: '13px', cursor: 'pointer', textAlign: 'left',
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '18px' }}>
              <button onClick={() => setStatusFarm(null)} className="btn btn-secondary">Hủy</button>
              <button onClick={confirmStatusChange} className="btn btn-primary">Xác nhận</button>
            </div>
          </div>
        </div>
      )}

      {/* ── Notes modal ── */}
      {notesFarm && (
        <div style={overlayStyle} onClick={() => setNotesFarm(null)}>
          <div style={{ ...modalStyle, maxWidth: '520px' }} onClick={(e) => e.stopPropagation()}>
            <h3 style={{ margin: '0 0 16px', color: '#fff', fontSize: '17px', fontWeight: 700 }}>
              🗒️ Ghi chú Admin — {notesFarm.name}
            </h3>
            <textarea
              value={notesText}
              onChange={(e) => setNotesText(e.target.value)}
              placeholder="Nhập ghi chú nội bộ (tối đa 2000 ký tự)..."
              rows={6}
              style={{
                width: '100%', padding: '12px', borderRadius: '8px', boxSizing: 'border-box',
                background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border-color)',
                color: '#fff', fontSize: '13px', outline: 'none', resize: 'vertical', fontFamily: 'inherit',
              }}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
              <span style={{ fontSize: '11px', color: notesText.trim().length > 2000 ? '#f87171' : 'var(--text-muted)' }}>
                {notesText.trim().length} / 2000 ký tự
              </span>
              <div style={{ display: 'flex', gap: '10px' }}>
                <button onClick={() => setNotesFarm(null)} className="btn btn-secondary">Hủy</button>
                <button onClick={confirmNotesSave} className="btn btn-primary">Lưu ghi chú</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ── Small helpers ──
const InfoField: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div>
    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '2px' }}>{label}</div>
    <div style={{ fontSize: '13px', color: '#fff' }}>{value}</div>
  </div>
);

const thStyle: React.CSSProperties = {
  textAlign: 'left', padding: '12px', fontSize: '11px', fontWeight: 700,
  color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px',
  whiteSpace: 'nowrap',
};
const tdStyle: React.CSSProperties = { padding: '12px', fontSize: '13px', color: 'var(--text-secondary)', verticalAlign: 'middle' };
const emptyStyle: React.CSSProperties = { padding: '40px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' };
const actionBtnStyle: React.CSSProperties = {
  padding: '6px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '13px',
  background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)',
  transition: 'all 0.2s ease',
};
const overlayStyle: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 2000,
  display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px',
};
const modalStyle: React.CSSProperties = {
  width: '100%', maxHeight: '90vh', overflowY: 'auto', borderRadius: '14px',
  background: '#16171f', border: '1px solid var(--border-color)', padding: '24px',
  boxShadow: '0 25px 60px rgba(0,0,0,0.6)',
};
const closeBtnStyle: React.CSSProperties = {
  width: '32px', height: '32px', borderRadius: '8px', border: '1px solid var(--border-color)',
  background: 'rgba(255,255,255,0.05)', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '14px',
};
const sectionStyle: React.CSSProperties = {
  padding: '16px', borderRadius: '10px', background: 'rgba(255,255,255,0.02)',
  border: '1px solid rgba(255,255,255,0.06)', marginBottom: '14px',
};
const sectionTitleStyle: React.CSSProperties = {
  margin: '0 0 12px', fontSize: '13px', fontWeight: 700, color: '#a78bfa',
  textTransform: 'uppercase', letterSpacing: '0.5px',
};
