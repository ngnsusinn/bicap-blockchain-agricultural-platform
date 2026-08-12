import React, { useState, useEffect, useCallback, useRef } from 'react';
import type { UserSession, Category, ProductItem, ProductDetail, ProductStats } from '../types';
import { API_ORIGIN, authHeaders, formatDate } from '../utils/api';
import { StatusBadge } from './StatusBadge';

interface ProductMonitoringPageProps {
  currentSession: UserSession;
  onToast: (text: string, type?: 'info' | 'success' | 'error') => void;
}

const TABS = [
  { id: '', label: '📋 Tất Cả' },
  { id: 'ACTIVE', label: '✅ Đang Hoạt Động' },
  { id: 'INACTIVE', label: '⚪ Ngừng Hoạt Động' },
  { id: 'PENDING_REVIEW', label: '⚠️ Chờ Xem Xét' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const STATUS_OPTIONS = [
  { id: 'ACTIVE', label: '✅ Đang hoạt động' },
  { id: 'INACTIVE', label: '⚪ Ngừng hoạt động' },
  { id: 'PENDING_REVIEW', label: '⚠️ Chờ xem xét' },
];

const CURRENCY = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });

export const ProductMonitoringPage: React.FC<ProductMonitoringPageProps> = ({ currentSession, onToast }) => {
  const [tab, setTab] = useState<TabId>('ACTIVE');
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [products, setProducts] = useState<ProductItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [stats, setStats] = useState<ProductStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<ProductDetail | null>(null);
  const [statusProduct, setStatusProduct] = useState<ProductItem | null>(null);
  const [newStatus, setNewStatus] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [categoryName, setCategoryName] = useState('');
  const [categoryDesc, setCategoryDesc] = useState('');
  const [categoryIcon, setCategoryIcon] = useState('');
  const detailSeqRef = useRef(0);

  const canManage = currentSession.role === 'SUPER_ADMIN' || currentSession.role === 'ADMIN';

  // Debounce the search input so typing doesn't fire one request per keystroke.
  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchTerm), 300);
    return () => clearTimeout(t);
  }, [searchTerm]);

  const fetchStats = useCallback(async () => {
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/products/stats`, {
        headers: authHeaders(currentSession.email),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Lỗi tải thống kê sản phẩm.');
      }
      setStats(await res.json());
    } catch (err: any) {
      // Non-fatal — the table still renders.
      onToast(err.message, 'error');
    }
  }, [currentSession, onToast]);

  const fetchCategories = useCallback(async () => {
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/products/categories`, {
        headers: authHeaders(currentSession.email),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Lỗi tải danh sách danh mục.');
      }
      setCategories(await res.json());
    } catch (err: any) {
      onToast(err.message, 'error');
    }
  }, [currentSession, onToast]);

  const fetchProducts = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        status: tab,
        search: debouncedSearch,
        page: page.toString(),
        size: '10',
      });
      if (categoryFilter) params.set('categoryId', categoryFilter);
      const res = await fetch(`${API_ORIGIN}/api/admin/products?${params}`, {
        headers: authHeaders(currentSession.email),
        signal,
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Lỗi tải danh sách sản phẩm.');
      }
      const data = await res.json();
      setProducts(data.content || []);
      const totalPagesValue = Math.max(1, data.totalPages || 1);
      setTotalPages(totalPagesValue);
      if (page >= totalPagesValue) {
        setPage(Math.max(0, totalPagesValue - 1));
      }
    } catch (err: any) {
      if (err?.name === 'AbortError') return; // superseded request — ignore
      setProducts([]);
      setTotalPages(1);
      onToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, debouncedSearch, categoryFilter, page, currentSession, onToast]);

  useEffect(() => {
    const controller = new AbortController();
    fetchProducts(controller.signal);
    return () => controller.abort();
  }, [fetchProducts]);

  // Load categories & stats once on mount, and refresh stats when the list changes.
  useEffect(() => {
    fetchCategories();
    fetchStats();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Product detail (sequence-guarded, surfaces fetch errors) ──
  const handleViewDetail = async (product: ProductItem) => {
    const seq = ++detailSeqRef.current;
    setSelectedProduct(product as ProductDetail);
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/products/${product.id}`, {
        headers: authHeaders(currentSession.email),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        if (seq === detailSeqRef.current) setSelectedProduct(null);
        onToast(err.message || 'Không thể tải chi tiết sản phẩm.', 'error');
        return;
      }
      const data = await res.json();
      if (seq === detailSeqRef.current) setSelectedProduct({ ...product, ...data });
    } catch (err: any) {
      if (seq === detailSeqRef.current) setSelectedProduct(null);
      onToast(err.message || 'Không thể tải chi tiết sản phẩm.', 'error');
    }
  };

  // ── Change product status ──
  const openStatusModal = (product: ProductItem) => {
    if (!canManage) { onToast('Bạn không có quyền thay đổi trạng thái sản phẩm.', 'error'); return; }
    setStatusProduct(product);
    setNewStatus(product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE');
  };

  const confirmStatusChange = async () => {
    if (!statusProduct) return;
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/products/${statusProduct.id}/status`, {
        method: 'PUT',
        headers: { ...authHeaders(currentSession.email), 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus }),
      });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || 'Cập nhật trạng thái thất bại.'); }
      onToast(`Đã cập nhật trạng thái sản phẩm "${statusProduct.name}".`, 'success');
      setStatusProduct(null);
      setSelectedProduct(null);
      fetchProducts();
      fetchStats();
    } catch (err: any) {
      onToast(err.message, 'error');
    }
  };

  // ── Category CRUD ──
  const openCategoryModal = (category?: Category) => {
    if (!canManage) { onToast('Bạn không có quyền quản lý danh mục.', 'error'); return; }
    setEditingCategory(category ?? null);
    setCategoryName(category?.name ?? '');
    setCategoryDesc(category?.description ?? '');
    setCategoryIcon(category?.icon ?? '');
    setShowCategoryModal(true);
  };

  const confirmCategorySave = async () => {
    const trimmed = categoryName.trim();
    if (!trimmed) { onToast('Tên danh mục không được để trống.', 'error'); return; }
    try {
      const body = JSON.stringify({
        name: trimmed,
        description: categoryDesc.trim() || null,
        icon: categoryIcon.trim() || null,
      });
      const url = editingCategory
        ? `${API_ORIGIN}/api/admin/products/categories/${editingCategory.id}`
        : `${API_ORIGIN}/api/admin/products/categories`;
      const res = await fetch(url, {
        method: editingCategory ? 'PUT' : 'POST',
        headers: { ...authHeaders(currentSession.email), 'Content-Type': 'application/json' },
        body,
      });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || 'Lưu danh mục thất bại.'); }
      onToast(editingCategory ? 'Đã cập nhật danh mục.' : 'Đã tạo danh mục mới.', 'success');
      setShowCategoryModal(false);
      setEditingCategory(null);
      fetchCategories();
    } catch (err: any) {
      onToast(err.message, 'error');
    }
  };

  const confirmCategoryDelete = async (category: Category) => {
    if (!window.confirm(`Bạn có chắc muốn xoá danh mục "${category.name}"?`)) return;
    try {
      const res = await fetch(`${API_ORIGIN}/api/admin/products/categories/${category.id}`, {
        method: 'DELETE',
        headers: authHeaders(currentSession.email),
      });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || 'Xoá danh mục thất bại.'); }
      onToast(`Đã xoá danh mục "${category.name}".`, 'success');
      fetchCategories();
    } catch (err: any) {
      onToast(err.message, 'error');
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="dashboard-title">Giám Sát Sản Phẩm</h1>
          <p className="dashboard-subtitle">
            Quản lý toàn bộ sản phẩm nông sản trên nền tảng: danh mục, mô tả, trạng thái hoạt động và tính chính xác dữ liệu.
          </p>
        </div>
        {canManage && (
          <button onClick={() => openCategoryModal()} className="btn btn-primary" style={{ marginTop: '8px' }}>
            🗂️ Quản Lý Danh Mục
          </button>
        )}
      </div>

      {/* Stats row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '12px', marginBottom: '20px' }}>
        <StatCard label="Tổng Sản Phẩm" value={stats?.totalProducts} accent="#a78bfa" />
        <StatCard label="Đang Hoạt Động" value={stats?.activeProducts} accent="#34d399" />
        <StatCard label="Ngừng Hoạt Động" value={stats?.inactiveProducts} accent="#94a3b8" />
        <StatCard label="Chờ Xem Xét" value={stats?.pendingReviewProducts} accent="#fbbf24" />
        <StatCard label="Mới Trong Tuần" value={stats?.newProductsThisWeek} accent="#38bdf8" />
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
        {/* Filter bar */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: '220px', display: 'flex', alignItems: 'center', gap: '8px', background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border-color)', borderRadius: '8px', padding: '0 12px' }}>
            <span>🔍</span>
            <input
              type="text"
              placeholder="Tìm theo tên sản phẩm hoặc nông trại..."
              value={searchTerm}
              onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
              style={{ flex: 1, background: 'transparent', border: 'none', outline: 'none', color: '#fff', padding: '12px 0', fontSize: '13px' }}
            />
          </div>
          <select
            value={categoryFilter}
            onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }}
            style={{
              padding: '10px 14px', borderRadius: '8px', background: 'rgba(255,255,255,0.04)',
              border: '1px solid var(--border-color)', color: '#fff', fontSize: '13px', outline: 'none',
            }}
          >
            <option value="">Tất cả danh mục</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.icon ? `${c.icon} ` : ''}{c.name}</option>
            ))}
          </select>
        </div>

        {/* Table */}
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
                <th style={thStyle}>ID</th>
                <th style={thStyle}>Tên Sản Phẩm</th>
                <th style={thStyle}>Danh Mục</th>
                <th style={thStyle}>Nông Trại</th>
                <th style={thStyle}>Giá</th>
                <th style={thStyle}>Số Lượng</th>
                <th style={thStyle}>Trạng Thái</th>
                <th style={thStyle}>Thao Tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={8} style={emptyStyle}>Đang tải dữ liệu...</td></tr>
              ) : products.length === 0 ? (
                <tr><td colSpan={8} style={emptyStyle}>Không tìm thấy sản phẩm nào trong danh mục này.</td></tr>
              ) : (
                products.map((product) => (
                  <tr key={product.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <td style={tdStyle}>{product.id}</td>
                    <td style={tdStyle}><span style={{ fontWeight: 600, color: '#fff' }}>{product.name}</span></td>
                    <td style={tdStyle}>
                      <span style={{ fontSize: '12px' }}>
                        {product.categoryName ? `${categories.find((c) => c.id === product.categoryId)?.icon ?? ''} ${product.categoryName}` : '—'}
                      </span>
                    </td>
                    <td style={tdStyle}><span style={{ fontSize: '12px' }}>{product.farmName || '—'}</span></td>
                    <td style={tdStyle}>{CURRENCY.format(product.price)}</td>
                    <td style={tdStyle}>{product.quantity}</td>
                    <td style={tdStyle}><StatusBadge status={product.status} /></td>
                    <td style={tdStyle}>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button onClick={() => handleViewDetail(product)} style={actionBtnStyle} title="Xem chi tiết">👁️</button>
                        {canManage && (
                          <button onClick={() => openStatusModal(product)} style={{ ...actionBtnStyle, background: 'rgba(139,92,246,0.12)', borderColor: 'rgba(139,92,246,0.3)' }} title="Đổi trạng thái">🔄</button>
                        )}
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

      {/* ── Product detail modal ── */}
      {selectedProduct && (
        <div style={overlayStyle} onClick={() => setSelectedProduct(null)}>
          <div style={{ ...modalStyle, maxWidth: '720px' }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
              <div>
                <h2 style={{ margin: 0, color: '#fff', fontSize: '20px', fontWeight: 700 }}>{selectedProduct.name}</h2>
                <p style={{ margin: '6px 0 0', color: 'var(--text-secondary)', fontSize: '13px' }}>
                  {selectedProduct.categoryName || '—'} · {CURRENCY.format(selectedProduct.price)}
                </p>
              </div>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                <StatusBadge status={selectedProduct.status} />
                <button onClick={() => setSelectedProduct(null)} style={closeBtnStyle}>✕</button>
              </div>
            </div>

            {selectedProduct.description && (
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: '1.6', margin: '0 0 14px' }}>
                {selectedProduct.description}
              </p>
            )}

            {/* Product info */}
            <div style={sectionStyle}>
              <h3 style={sectionTitleStyle}>📦 Thông Tin Sản Phẩm</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <InfoField label="Giá bán" value={CURRENCY.format(selectedProduct.price)} />
                <InfoField label="Số lượng" value={`${selectedProduct.quantity}`} />
                <InfoField label="Danh mục" value={selectedProduct.categoryName || '—'} />
                <InfoField label="Ngày tạo" value={formatDate(selectedProduct.createdAt)} />
              </div>
            </div>

            {/* Farm info */}
            <div style={sectionStyle}>
              <h3 style={sectionTitleStyle}>🏞️ Nông Trại Nguồn Gốc</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <InfoField label="Tên nông trại" value={selectedProduct.farmName || '—'} />
                <InfoField label="Địa chỉ" value={selectedProduct.farmAddress || '—'} />
                <InfoField label="Chủ sở hữu" value={selectedProduct.ownerName || '—'} />
                <InfoField label="Email liên hệ" value={selectedProduct.ownerEmail || '—'} />
              </div>
            </div>

            {/* Season info */}
            <div style={sectionStyle}>
              <h3 style={sectionTitleStyle}>🌱 Mùa Vụ</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <InfoField label="Tên mùa vụ" value={selectedProduct.seasonName || '—'} />
                <InfoField label="Loại sản phẩm" value={selectedProduct.seasonProductType || '—'} />
                <InfoField label="Giống" value={selectedProduct.seasonVariety || '—'} />
                <InfoField label="Thời gian" value={
                  selectedProduct.seasonStartDate
                    ? `${formatDate(selectedProduct.seasonStartDate)} → ${formatDate(selectedProduct.seasonEndDate)}`
                    : '—'
                } />
              </div>
            </div>

            {canManage && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button onClick={() => openStatusModal(selectedProduct)} className="btn btn-secondary">
                  🔄 Đổi trạng thái
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── Status change modal ── */}
      {statusProduct && (
        <div style={overlayStyle} onClick={() => setStatusProduct(null)}>
          <div style={{ ...modalStyle, maxWidth: '440px' }} onClick={(e) => e.stopPropagation()}>
            <h3 style={{ margin: '0 0 16px', color: '#fff', fontSize: '17px', fontWeight: 700 }}>
              🔄 Đổi trạng thái — {statusProduct.name}
            </h3>
            <p style={{ margin: '0 0 14px', color: 'var(--text-secondary)', fontSize: '13px' }}>
              Trạng thái hiện tại: <StatusBadge status={statusProduct.status} />
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
              <button onClick={() => setStatusProduct(null)} className="btn btn-secondary">Hủy</button>
              <button onClick={confirmStatusChange} className="btn btn-primary">Xác nhận</button>
            </div>
          </div>
        </div>
      )}

      {/* ── Category management modal ── */}
      {showCategoryModal && (
        <div style={overlayStyle} onClick={() => { setShowCategoryModal(false); setEditingCategory(null); }}>
          <div style={{ ...modalStyle, maxWidth: '620px' }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h3 style={{ margin: 0, color: '#fff', fontSize: '17px', fontWeight: 700 }}>🗂️ Quản Lý Danh Mục</h3>
              <button onClick={() => { setShowCategoryModal(false); setEditingCategory(null); }} style={closeBtnStyle}>✕</button>
            </div>

            {/* Create/edit form */}
            <div style={{ ...sectionStyle, marginBottom: '16px' }}>
              <h3 style={sectionTitleStyle}>{editingCategory ? '✏️ Sửa danh mục' : '➕ Thêm danh mục mới'}</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <input
                    type="text"
                    placeholder="Icon (emoji)"
                    value={categoryIcon}
                    onChange={(e) => setCategoryIcon(e.target.value)}
                    maxLength={10}
                    style={inputStyle}
                  />
                  <input
                    type="text"
                    placeholder="Tên danh mục *"
                    value={categoryName}
                    onChange={(e) => setCategoryName(e.target.value)}
                    maxLength={100}
                    style={{ ...inputStyle, flex: 3 }}
                  />
                </div>
                <input
                  type="text"
                  placeholder="Mô tả (tối đa 500 ký tự)"
                  value={categoryDesc}
                  onChange={(e) => setCategoryDesc(e.target.value)}
                  maxLength={500}
                  style={inputStyle}
                />
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                  {editingCategory && (
                    <button onClick={() => setEditingCategory(null)} className="btn btn-secondary">Hủy sửa</button>
                  )}
                  <button onClick={confirmCategorySave} className="btn btn-primary">
                    {editingCategory ? 'Cập nhật' : 'Thêm mới'}
                  </button>
                </div>
              </div>
            </div>

            {/* Category list */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '320px', overflowY: 'auto' }}>
              {categories.length === 0 ? (
                <p style={{ color: 'var(--text-muted)', fontSize: '13px', textAlign: 'center', padding: '24px' }}>
                  Chưa có danh mục nào.
                </p>
              ) : (
                categories.map((c) => (
                  <div
                    key={c.id}
                    style={{
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px',
                      padding: '12px 14px', borderRadius: '8px', background: 'rgba(255,255,255,0.03)',
                      border: '1px solid var(--border-color)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
                      <span style={{ fontSize: '18px' }}>{c.icon || '📦'}</span>
                      <div style={{ minWidth: 0 }}>
                        <div style={{ fontWeight: 600, fontSize: '13px', color: '#fff' }}>{c.name}</div>
                        {c.description && (
                          <div style={{ fontSize: '11px', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '280px' }}>
                            {c.description}
                          </div>
                        )}
                        <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>
                          {c.productCount} sản phẩm
                        </div>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '6px', flexShrink: 0 }}>
                      <button onClick={() => openCategoryModal(c)} style={actionBtnStyle} title="Sửa danh mục">✏️</button>
                      <button
                        onClick={() => confirmCategoryDelete(c)}
                        disabled={c.productCount > 0}
                        style={{
                          ...actionBtnStyle,
                          opacity: c.productCount > 0 ? 0.4 : 1,
                          cursor: c.productCount > 0 ? 'not-allowed' : 'pointer',
                        }}
                        title={c.productCount > 0 ? 'Không thể xóa — danh mục có sản phẩm' : 'Xóa danh mục'}
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ── Small helpers ──
const StatCard: React.FC<{ label: string; value?: number; accent: string }> = ({ label, value, accent }) => (
  <div
    style={{
      padding: '16px', borderRadius: '12px', background: 'rgba(255,255,255,0.03)',
      border: '1px solid var(--border-color)',
    }}
  >
    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
      {label}
    </div>
    <div style={{ fontSize: '26px', fontWeight: 800, color: accent }}>
      {value ?? '—'}
    </div>
  </div>
);

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
const inputStyle: React.CSSProperties = {
  padding: '10px 12px', borderRadius: '8px', background: 'rgba(255,255,255,0.04)',
  border: '1px solid var(--border-color)', color: '#fff', fontSize: '13px',
  outline: 'none', fontFamily: 'inherit',
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
