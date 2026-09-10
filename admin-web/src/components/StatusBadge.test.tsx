import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusBadge } from './StatusBadge';

/** BICAP-86 — shared status badge labels (single source of truth for farm/product states). */
describe('StatusBadge', () => {
  it('maps known statuses to Vietnamese labels', () => {
    render(<StatusBadge status="PENDING" />);
    expect(screen.getByText('Chờ duyệt')).toBeInTheDocument();
  });

  it('shows APPROVED as "Đang hoạt động"', () => {
    render(<StatusBadge status="APPROVED" />);
    expect(screen.getByText('Đang hoạt động')).toBeInTheDocument();
  });

  it('falls back to the pending label for unknown statuses', () => {
    render(<StatusBadge status="WHATEVER" />);
    expect(screen.getByText('Chờ duyệt')).toBeInTheDocument();
  });
});
