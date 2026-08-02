import React from 'react';

// Single source of truth for farm status labels — used by Farm Approval (BICAP-3)
// and Farm Management (BICAP-4) so the same farm never shows contradictory labels.
export const STATUS_META: Record<string, { label: string; color: string; bg: string }> = {
  PENDING: { label: 'Chờ duyệt', color: '#fbbf24', bg: 'rgba(251,191,36,0.12)' },
  APPROVED: { label: 'Đang hoạt động', color: '#34d399', bg: 'rgba(52,211,153,0.12)' },
  REJECTED: { label: 'Bị từ chối', color: '#f87171', bg: 'rgba(248,113,113,0.12)' },
  SUSPENDED: { label: 'Tạm ngưng', color: '#fb923c', bg: 'rgba(251,146,60,0.12)' },
  INACTIVE: { label: 'Ngừng hoạt động', color: '#94a3b8', bg: 'rgba(148,163,184,0.12)' },
};

export const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const meta = STATUS_META[status] || STATUS_META.PENDING;
  return (
    <span
      style={{
        padding: '4px 12px',
        borderRadius: '12px',
        fontSize: '12px',
        fontWeight: 600,
        color: meta.color,
        background: meta.bg,
        whiteSpace: 'nowrap',
      }}
    >
      {meta.label}
    </span>
  );
};
