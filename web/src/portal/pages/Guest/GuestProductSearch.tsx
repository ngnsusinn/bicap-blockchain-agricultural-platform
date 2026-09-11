import { useEffect, useState } from 'react';
import { API_BASE_URL } from '../../utils/auth';

/**
 * BICAP-70 / F3 — Guest: tra cứu & lọc sản phẩm công khai.
 *
 * Trang này CHỈ gọi endpoint công khai `GET /api/public/products` và chỉ hiển thị
 * dữ liệu thật do backend trả về (danh mục, nông trại, địa chỉ nông trại, chứng
 * nhận, giá, ảnh, trace hash). Không suy diễn/thay thế bất kỳ giá trị nào:
 *  - không tự sinh `traceHash`
 *  - không gán mặc định `VietGAP` / `kg`
 *  - không dùng ảnh stock khi `images` rỗng (hiển thị placeholder CSS trung tính)
 *
 * Bộ lọc được gửi thẳng xuống backend: keyword, categoryId, region, certification
 * (lặp lại nhiều lần), availability, sortBy, page, size.
 */

export interface MarketplaceProduct {
  id: number;
  name: string;
  description?: string | null;
  images?: string[] | null;
  price?: number | null;
  quantity?: number | null;
  availability?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  farmId?: number | null;
  farmName?: string | null;
  farmAddress?: string | null;
  certifications?: string[] | null;
  seasonId?: number | null;
  seasonName?: string | null;
  productType?: string | null;
  variety?: string | null;
  seasonStartDate?: string | null;
  harvestDate?: string | null;
  exportId?: number | null;
  traceHash?: string | null;
  qrImage?: string | null;
  transactionHash?: string | null;
  processes?: {
    processType?: string;
    executionDate?: string;
    materials?: string;
    images?: string;
    notes?: string;
    transactionHash?: string;
  }[] | null;
  createdAt?: string | null;
}

interface Category {
  id: number;
  name: string;
}

interface SpringPage<T> {
  content?: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

const PAGE_SIZE = 12;
const ALL = 'ALL';

export default function GuestProductSearch() {
  const [products, setProducts] = useState<MarketplaceProduct[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [certOptions, setCertOptions] = useState<string[]>([]);

  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0);

  // Bộ lọc đang áp dụng (đã gửi xuống backend).
  const [keyword, setKeyword] = useState('');
  const [region, setRegion] = useState('');
  const [categoryId, setCategoryId] = useState<string>(ALL);
  const [certifications, setCertifications] = useState<string[]>([]);
  const [availability, setAvailability] = useState<string>(ALL);
  const [sortBy, setSortBy] = useState<'NEWEST' | 'PRICE_ASC' | 'PRICE_DESC'>('NEWEST');

  // Ô nhập liệu chờ bấm "Tìm kiếm".
  const [keywordDraft, setKeywordDraft] = useState('');
  const [regionDraft, setRegionDraft] = useState('');

  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [selectedProduct, setSelectedProduct] = useState<MarketplaceProduct | null>(null);

  // Danh mục thật từ backend (endpoint công khai).
  useEffect(() => {
    let alive = true;
    fetch(`${API_BASE_URL}/categories`)
      .then((r) => (r.ok ? r.json() : []))
      .then((data) => {
        if (!alive) return;
        const list = Array.isArray(data) ? data : (data?.content ?? []);
        setCategories(Array.isArray(list) ? list : []);
      })
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const load = async () => {
      setLoading(true);
      setErrorMsg('');
      try {
        const params = new URLSearchParams();
        if (keyword.trim()) params.set('keyword', keyword.trim());
        if (region.trim()) params.set('region', region.trim());
        if (categoryId !== ALL) params.set('categoryId', categoryId);
        certifications.forEach((c) => params.append('certification', c));
        if (availability !== ALL) params.set('availability', availability);
        params.set('sortBy', sortBy);
        params.set('page', String(page));
        params.set('size', String(PAGE_SIZE));

        const res = await fetch(`${API_BASE_URL}/public/products?${params.toString()}`, {
          signal: controller.signal,
        });
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          throw new Error(body.message || `Không tải được sản phẩm (mã lỗi ${res.status}).`);
        }
        const body: SpringPage<MarketplaceProduct> = await res.json();
        const list = Array.isArray(body) ? (body as unknown as MarketplaceProduct[]) : (body.content ?? []);
        setProducts(list);
        setTotalElements(body.totalElements ?? list.length);
        setTotalPages(Math.max(1, body.totalPages ?? 1));
        // Chỉ bổ sung chứng nhận THẬT xuất hiện trong kết quả trả về.
        setCertOptions((prev) => {
          const next = new Set(prev);
          list.forEach((p) => (p.certifications ?? []).forEach((c) => c && next.add(c)));
          return Array.from(next).sort((a, b) => a.localeCompare(b));
        });
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        setProducts([]);
        setTotalElements(0);
        setTotalPages(1);
        setErrorMsg(err instanceof Error ? err.message : 'Không thể kết nối tới server Backend.');
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };
    void load();
    return () => controller.abort();
  }, [keyword, region, categoryId, certifications, availability, sortBy, page]);

  const applySearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setKeyword(keywordDraft);
    setRegion(regionDraft);
  };

  const resetFilters = () => {
    setKeywordDraft('');
    setRegionDraft('');
    setKeyword('');
    setRegion('');
    setCategoryId(ALL);
    setCertifications([]);
    setAvailability(ALL);
    setSortBy('NEWEST');
    setPage(0);
  };

  const toggleCertification = (cert: string) => {
    setPage(0);
    setCertifications((prev) =>
      prev.includes(cert) ? prev.filter((c) => c !== cert) : [...prev, cert],
    );
  };

  const imageOf = (p: MarketplaceProduct): string | null =>
    (p.images ?? []).find((img) => !!img) ?? null;

  const priceLabel = (p: MarketplaceProduct): string =>
    p.price != null ? `${Number(p.price).toLocaleString('vi-VN')} đ` : 'Chưa cập nhật giá';

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <h1 style={titleStyle}>🔍 Tra Cứu Sản Phẩm Nông Sản Sạch</h1>
        <p style={subtitleStyle}>
          Dữ liệu công khai trực tiếp từ hệ thống BICAP: nông trại, nguồn gốc, chứng nhận và mã QR
          truy xuất thật.
        </p>
      </div>

      {/* Bộ lọc */}
      <form style={filterBoxStyle} onSubmit={applySearch}>
        <div style={filterGridStyle}>
          <div>
            <label style={filterLabelStyle} htmlFor="guest-product-keyword">
              Từ khóa
            </label>
            <input
              id="guest-product-keyword"
              type="text"
              placeholder="Tên sản phẩm, nông trại…"
              value={keywordDraft}
              onChange={(e) => setKeywordDraft(e.target.value)}
              style={inputStyle}
            />
          </div>

          <div>
            <label style={filterLabelStyle} htmlFor="guest-product-region">
              Nguồn gốc / khu vực
            </label>
            <input
              id="guest-product-region"
              type="text"
              placeholder="VD: Đà Lạt, Lâm Đồng…"
              value={regionDraft}
              onChange={(e) => setRegionDraft(e.target.value)}
              style={inputStyle}
            />
          </div>

          <div>
            <label style={filterLabelStyle} htmlFor="guest-product-category">
              Danh mục
            </label>
            <select
              id="guest-product-category"
              value={categoryId}
              onChange={(e) => {
                setPage(0);
                setCategoryId(e.target.value);
              }}
              style={inputStyle}
            >
              <option value={ALL}>Tất cả danh mục ({categories.length})</option>
              {categories.map((c) => (
                <option key={c.id} value={String(c.id)}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label style={filterLabelStyle} htmlFor="guest-product-availability">
              Tình trạng
            </label>
            <select
              id="guest-product-availability"
              value={availability}
              onChange={(e) => {
                setPage(0);
                setAvailability(e.target.value);
              }}
              style={inputStyle}
            >
              <option value={ALL}>Tất cả</option>
              <option value="AVAILABLE">Còn hàng</option>
              <option value="SOLD_OUT">Hết hàng</option>
            </select>
          </div>

          <div>
            <label style={filterLabelStyle} htmlFor="guest-product-sort">
              Sắp xếp
            </label>
            <select
              id="guest-product-sort"
              value={sortBy}
              onChange={(e) => {
                setPage(0);
                setSortBy(e.target.value as typeof sortBy);
              }}
              style={inputStyle}
            >
              <option value="NEWEST">Mới nhất</option>
              <option value="PRICE_ASC">Giá tăng dần</option>
              <option value="PRICE_DESC">Giá giảm dần</option>
            </select>
          </div>
        </div>

        {certOptions.length > 0 && (
          <div style={{ marginTop: 16 }}>
            <span style={filterLabelStyle}>Chứng nhận (chọn nhiều)</span>
            <div style={chipRowStyle}>
              {certOptions.map((cert) => {
                const active = certifications.includes(cert);
                return (
                  <button
                    key={cert}
                    type="button"
                    aria-pressed={active}
                    onClick={() => toggleCertification(cert)}
                    style={{
                      ...chipStyle,
                      color: active ? '#34d399' : '#94a3b8',
                      borderColor: active ? '#10b981' : 'rgba(255,255,255,0.12)',
                      background: active ? 'rgba(16,185,129,0.12)' : 'transparent',
                    }}
                  >
                    {active ? '✓ ' : ''}
                    {cert}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        <div style={{ display: 'flex', gap: 12, marginTop: 18, flexWrap: 'wrap' }}>
          <button type="submit" style={primaryBtnStyle}>
            🔎 Tìm kiếm
          </button>
          <button type="button" onClick={resetFilters} style={secondaryBtnStyle}>
            Xóa bộ lọc
          </button>
        </div>
      </form>

      {/* Kết quả */}
      {loading ? (
        <div style={emptyStyle}>⏳ Đang tải dữ liệu sản phẩm…</div>
      ) : errorMsg ? (
        <div style={{ ...emptyStyle, color: '#fca5a5', border: '1px solid rgba(239, 68, 68, 0.3)' }}>
          <div style={{ fontSize: 32, marginBottom: 8 }}>⚠️</div>
          <p>{errorMsg}</p>
        </div>
      ) : products.length === 0 ? (
        <div style={emptyStyle}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>🍃</div>
          <h3>Chưa có sản phẩm nào phù hợp.</h3>
          <p>Hãy thử từ khóa khác hoặc bỏ bớt bộ lọc.</p>
        </div>
      ) : (
        <>
          <p style={{ color: '#64748b', fontSize: 13, marginBottom: 14 }}>
            Tìm thấy {totalElements} sản phẩm
          </p>
          <div style={gridStyle}>
            {products.map((p) => {
              const image = imageOf(p);
              return (
                <div key={p.id} style={cardStyle}>
                  <div style={imageWrapperStyle}>
                    {image ? (
                      <img src={image} alt={p.name} style={imageStyle} />
                    ) : (
                      <div style={placeholderImageStyle} aria-label="Chưa có ảnh sản phẩm">
                        🌾
                      </div>
                    )}
                    {p.availability && (
                      <span
                        style={{
                          ...availabilityBadgeStyle,
                          background: p.availability === 'AVAILABLE' ? '#10b981' : '#64748b',
                        }}
                      >
                        {p.availability === 'AVAILABLE' ? 'Còn hàng' : p.availability === 'SOLD_OUT' ? 'Hết hàng' : p.availability}
                      </span>
                    )}
                  </div>

                  <div style={cardBodyStyle}>
                    {p.categoryName && <div style={categoryTagStyle}>{p.categoryName}</div>}
                    <h3 style={productTitleStyle}>{p.name}</h3>

                    {p.farmName && (
                      <div style={infoRowStyle}>
                        <span>🏡 Nông trại:</span>
                        <strong style={{ color: '#e2e8f0' }}>{p.farmName}</strong>
                      </div>
                    )}
                    {p.farmAddress && (
                      <div style={infoRowStyle}>
                        <span>📍 Nguồn gốc:</span>
                        <span style={{ textAlign: 'right' }}>{p.farmAddress}</span>
                      </div>
                    )}
                    {p.quantity != null && (
                      <div style={infoRowStyle}>
                        <span>Số lượng:</span>
                        <span>{p.quantity}</span>
                      </div>
                    )}

                    {(p.certifications ?? []).length > 0 && (
                      <div style={{ ...chipRowStyle, marginTop: 8 }}>
                        {(p.certifications ?? []).map((cert) => (
                          <span key={cert} style={certChipStyle}>
                            🏅 {cert}
                          </span>
                        ))}
                      </div>
                    )}

                    <div style={priceRowStyle}>
                      <div style={priceTextStyle}>{priceLabel(p)}</div>
                    </div>

                    <button type="button" onClick={() => setSelectedProduct(p)} style={detailsBtnStyle}>
                      🔍 Xem chi tiết
                    </button>

                    {p.traceHash ? (
                      <a href={`/trace/${encodeURIComponent(p.traceHash)}`} style={traceLinkStyle}>
                        📲 Tra cứu QR
                      </a>
                    ) : (
                      <button type="button" disabled style={disabledTraceBtnStyle}>
                        📲 Chưa có QR truy xuất
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Phân trang thật theo Spring Page */}
          {totalPages > 1 && (
            <div style={paginationStyle}>
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page <= 0}
                style={pageBtnStyle(page <= 0)}
              >
                ◀ Trước
              </button>
              <span style={{ fontSize: 13, color: '#94a3b8' }}>
                Trang <strong>{page + 1}</strong> / <strong>{totalPages}</strong>
              </span>
              <button
                type="button"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                style={pageBtnStyle(page >= totalPages - 1)}
              >
                Sau ▶
              </button>
            </div>
          )}
        </>
      )}

      {/* Chi tiết */}
      {selectedProduct && (
        <div style={modalOverlayStyle} onClick={() => setSelectedProduct(null)}>
          <div style={modalContentStyle} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              {selectedProduct.categoryName && <span style={categoryTagStyle}>{selectedProduct.categoryName}</span>}
              <button onClick={() => setSelectedProduct(null)} style={closeBtnStyle} aria-label="Đóng">
                ✕
              </button>
            </div>

            <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap' }}>
              {imageOf(selectedProduct) ? (
                <img
                  src={imageOf(selectedProduct)!}
                  alt={selectedProduct.name}
                  style={modalImgStyle}
                />
              ) : (
                <div style={{ ...placeholderImageStyle, width: 200, height: 150, borderRadius: 12 }}>🌾</div>
              )}

              <div style={{ flex: 1, minWidth: 240 }}>
                <h2 style={{ color: '#fff', fontSize: 20, margin: '0 0 8px 0' }}>{selectedProduct.name}</h2>
                <div style={{ fontSize: 18, fontWeight: 700, color: '#34d399', marginBottom: 12 }}>
                  {priceLabel(selectedProduct)}
                </div>

                <div style={modalInfoListStyle}>
                  {selectedProduct.farmName && <p><strong>Nông trại:</strong> {selectedProduct.farmName}</p>}
                  {selectedProduct.farmAddress && <p><strong>Nguồn gốc:</strong> {selectedProduct.farmAddress}</p>}
                  {selectedProduct.seasonName && <p><strong>Mùa vụ:</strong> {selectedProduct.seasonName}</p>}
                  {selectedProduct.productType && <p><strong>Loại nông sản:</strong> {selectedProduct.productType}</p>}
                  {selectedProduct.variety && <p><strong>Giống:</strong> {selectedProduct.variety}</p>}
                  {selectedProduct.harvestDate && <p><strong>Ngày thu hoạch:</strong> {selectedProduct.harvestDate}</p>}
                  <p>
                    <strong>Chứng nhận:</strong>{' '}
                    {(selectedProduct.certifications ?? []).length > 0
                      ? (selectedProduct.certifications ?? []).join(', ')
                      : 'Chưa cập nhật'}
                  </p>
                </div>
              </div>
            </div>

            {selectedProduct.description && (
              <div style={{ marginTop: 20, borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: 16 }}>
                <h4 style={{ color: '#fff', fontSize: 14, marginBottom: 8 }}>Mô tả sản phẩm</h4>
                <p style={{ fontSize: 13, color: '#cbd5e1', lineHeight: 1.6 }}>{selectedProduct.description}</p>
              </div>
            )}

            <div style={qrBoxStyle}>
              <div style={{ fontSize: 32 }}>📲</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: '#fff' }}>Truy xuất nguồn gốc</div>
                <div style={{ fontSize: 11, color: '#94a3b8', overflowWrap: 'anywhere' }}>
                  {selectedProduct.traceHash
                    ? `Trace hash: ${selectedProduct.traceHash}`
                    : 'Lô sản phẩm này chưa có mã truy xuất trên blockchain.'}
                </div>
              </div>
              {selectedProduct.traceHash ? (
                <a
                  href={`/trace/${encodeURIComponent(selectedProduct.traceHash)}`}
                  style={traceLinkStyle}
                >
                  Tra cứu ngay ➔
                </a>
              ) : (
                <button type="button" disabled style={disabledTraceBtnStyle}>
                  Chưa có QR
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ── Inline Styles ── */
const containerStyle: React.CSSProperties = { padding: 24, maxWidth: 1100, margin: '0 auto' };
const headerStyle: React.CSSProperties = { marginBottom: 24 };
const titleStyle: React.CSSProperties = { fontSize: 24, fontWeight: 800, color: '#fff', margin: 0 };
const subtitleStyle: React.CSSProperties = { fontSize: 14, color: 'var(--text-secondary)', marginTop: 6 };
const filterBoxStyle: React.CSSProperties = {
  background: 'rgba(30, 41, 59, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)',
  borderRadius: 16, padding: 20, marginBottom: 24,
};
const filterGridStyle: React.CSSProperties = {
  display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 16,
};
const filterLabelStyle: React.CSSProperties = {
  display: 'block', fontSize: 12, fontWeight: 600, color: '#cbd5e1', marginBottom: 6,
};
const inputStyle: React.CSSProperties = {
  width: '100%', boxSizing: 'border-box', background: 'rgba(15, 23, 42, 0.9)',
  border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: 8, color: '#fff',
  padding: '9px 12px', fontSize: 13, outline: 'none',
};
const chipRowStyle: React.CSSProperties = { display: 'flex', gap: 8, flexWrap: 'wrap' };
const chipStyle: React.CSSProperties = {
  padding: '6px 12px', borderRadius: 20, border: '1px solid', fontSize: 12,
  fontWeight: 600, cursor: 'pointer',
};
const primaryBtnStyle: React.CSSProperties = {
  background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)', border: 'none',
  color: '#fff', padding: '10px 20px', borderRadius: 8, fontSize: 13, fontWeight: 700, cursor: 'pointer',
};
const secondaryBtnStyle: React.CSSProperties = {
  background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.12)',
  color: '#cbd5e1', padding: '10px 20px', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer',
};
const gridStyle: React.CSSProperties = {
  display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 20,
};
const cardStyle: React.CSSProperties = {
  background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255, 255, 255, 0.08)',
  borderRadius: 14, overflow: 'hidden', display: 'flex', flexDirection: 'column',
};
const imageWrapperStyle: React.CSSProperties = { position: 'relative', height: 180, width: '100%' };
const imageStyle: React.CSSProperties = { width: '100%', height: '100%', objectFit: 'cover' };
const placeholderImageStyle: React.CSSProperties = {
  width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
  fontSize: 44, color: '#475569',
  background: 'repeating-linear-gradient(45deg, rgba(148,163,184,0.08) 0 12px, rgba(148,163,184,0.16) 12px 24px)',
};
const availabilityBadgeStyle: React.CSSProperties = {
  position: 'absolute', top: 12, right: 12, color: '#fff', fontSize: 11, fontWeight: 800,
  padding: '4px 10px', borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
};
const cardBodyStyle: React.CSSProperties = { padding: 16, display: 'flex', flexDirection: 'column', flex: 1 };
const categoryTagStyle: React.CSSProperties = {
  fontSize: 11, color: '#06b6d4', fontWeight: 700, textTransform: 'uppercase',
  letterSpacing: '0.5px', marginBottom: 4,
};
const productTitleStyle: React.CSSProperties = {
  fontSize: 16, fontWeight: 700, color: '#fff', margin: '0 0 10px 0', lineHeight: 1.4,
};
const infoRowStyle: React.CSSProperties = {
  fontSize: 12, color: 'var(--text-secondary)', display: 'flex',
  justifyContent: 'space-between', gap: 10, marginBottom: 6,
};
const certChipStyle: React.CSSProperties = {
  fontSize: 10, padding: '2px 8px', borderRadius: 10, color: '#34d399',
  background: 'rgba(16,185,129,0.12)', border: '1px solid rgba(16,185,129,0.25)',
};
const priceRowStyle: React.CSSProperties = {
  marginTop: 'auto', paddingTop: 12, borderTop: '1px solid rgba(255, 255, 255, 0.06)', marginBottom: 14,
};
const priceTextStyle: React.CSSProperties = { fontSize: 16, fontWeight: 800, color: '#34d399' };
const detailsBtnStyle: React.CSSProperties = {
  width: '100%', background: 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)', border: 'none',
  color: '#fff', padding: 10, borderRadius: 8, fontSize: 13, fontWeight: 700, cursor: 'pointer',
};
const traceLinkStyle: React.CSSProperties = {
  display: 'block', textAlign: 'center', marginTop: 8, background: 'rgba(56,189,248,0.12)',
  color: '#7dd3fc', textDecoration: 'none', padding: '8px 14px', borderRadius: 8,
  fontSize: 12, fontWeight: 700, border: '1px solid rgba(56,189,248,0.3)',
};
const disabledTraceBtnStyle: React.CSSProperties = {
  ...traceLinkStyle, cursor: 'not-allowed', opacity: 0.5, color: '#94a3b8',
  background: 'rgba(148,163,184,0.08)', border: '1px solid rgba(148,163,184,0.2)',
};
const emptyStyle: React.CSSProperties = {
  textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)',
  background: 'rgba(255, 255, 255, 0.02)', borderRadius: 16,
};
const paginationStyle: React.CSSProperties = {
  display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 16, marginTop: 24,
};
const pageBtnStyle = (disabled: boolean): React.CSSProperties => ({
  padding: '8px 16px', borderRadius: 8, border: '1px solid rgba(255,255,255,0.12)',
  background: 'rgba(255,255,255,0.05)', color: disabled ? '#475569' : '#cbd5e1',
  cursor: disabled ? 'not-allowed' : 'pointer', fontSize: 13, fontWeight: 600,
});
const modalOverlayStyle: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0, 0, 0, 0.8)', backdropFilter: 'blur(6px)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000, padding: 20,
};
const modalContentStyle: React.CSSProperties = {
  background: '#1e293b', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: 20,
  padding: 28, maxWidth: 640, width: '100%', maxHeight: '90vh', overflowY: 'auto',
};
const modalImgStyle: React.CSSProperties = {
  width: 200, height: 150, objectFit: 'cover', borderRadius: 12,
};
const modalInfoListStyle: React.CSSProperties = { fontSize: 13, color: '#cbd5e1', lineHeight: 1.8 };
const closeBtnStyle: React.CSSProperties = {
  background: 'none', border: 'none', color: 'var(--text-muted)', fontSize: 20, cursor: 'pointer',
};
const qrBoxStyle: React.CSSProperties = {
  marginTop: 20, padding: 16, background: 'rgba(16, 185, 129, 0.1)',
  border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: 12,
  display: 'flex', alignItems: 'center', gap: 16,
};
