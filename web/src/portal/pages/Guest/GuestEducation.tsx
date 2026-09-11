import { useEffect, useState } from 'react';
import { API_BASE_URL } from '../../utils/auth';

/**
 * BICAP-71 / F5 — Guest: nội dung giáo dục lấy từ API công khai.
 *
 * Toàn bộ bài viết/video đến từ `GET /api/public/education` (chỉ nội dung đã
 * PUBLISHED). Không còn dữ liệu mock hay video placeholder: nếu backend chưa có
 * nội dung thì hiển thị trạng thái rỗng trung thực.
 */

export interface EducationalContent {
  id: number;
  title: string;
  summary?: string | null;
  content?: string | null;
  type?: string | null;
  videoUrl?: string | null;
  coverImageUrl?: string | null;
  tags?: string[] | null;
  publishedAt?: string | null;
}

export default function GuestEducation() {
  const [activeTab, setActiveTab] = useState<'ARTICLE' | 'VIDEO'>('ARTICLE');
  const [keywordDraft, setKeywordDraft] = useState('');
  const [keyword, setKeyword] = useState('');
  const [items, setItems] = useState<EducationalContent[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [selectedArticle, setSelectedArticle] = useState<EducationalContent | null>(null);
  const [selectedVideo, setSelectedVideo] = useState<EducationalContent | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    const load = async () => {
      setLoading(true);
      setErrorMsg('');
      try {
        const params = new URLSearchParams({ type: activeTab });
        if (keyword.trim()) params.set('keyword', keyword.trim());
        const res = await fetch(`${API_BASE_URL}/public/education?${params.toString()}`, {
          signal: controller.signal,
        });
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          throw new Error(body.message || `Không tải được nội dung (mã lỗi ${res.status}).`);
        }
        const body = await res.json();
        setItems(Array.isArray(body) ? body : (body?.content ?? []));
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        setItems([]);
        setErrorMsg(err instanceof Error ? err.message : 'Không thể kết nối tới server Backend.');
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };
    void load();
    return () => controller.abort();
  }, [activeTab, keyword]);

  const applySearch = (e: React.FormEvent) => {
    e.preventDefault();
    setKeyword(keywordDraft);
  };

  const formatDate = (value?: string | null): string => {
    if (!value) return 'Chưa cập nhật ngày';
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString('vi-VN');
  };

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <h1 style={titleStyle}>📚 Kiến Thức Nông Nghiệp Sạch</h1>
        <p style={subtitleStyle}>
          Bài viết và video hướng dẫn về canh tác an toàn, tiêu chuẩn chất lượng và truy xuất
          nguồn gốc, được cập nhật từ hệ thống BICAP.
        </p>
      </div>

      {/* Tab bài viết / video */}
      <div style={topTabContainerStyle}>
        <button
          type="button"
          onClick={() => setActiveTab('ARTICLE')}
          style={{
            ...topTabStyle,
            color: activeTab === 'ARTICLE' ? '#fff' : 'var(--text-secondary)',
            background: activeTab === 'ARTICLE' ? 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)' : 'rgba(255, 255, 255, 0.05)',
          }}
        >
          📖 Bài viết
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('VIDEO')}
          style={{
            ...topTabStyle,
            color: activeTab === 'VIDEO' ? '#fff' : 'var(--text-secondary)',
            background: activeTab === 'VIDEO' ? 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)' : 'rgba(255, 255, 255, 0.05)',
          }}
        >
          🎥 Video hướng dẫn
        </button>
      </div>

      <form style={searchBoxStyle} onSubmit={applySearch}>
        <span>🔍</span>
        <input
          type="text"
          aria-label="Tìm kiếm nội dung"
          placeholder={activeTab === 'ARTICLE' ? 'Tìm kiếm bài viết…' : 'Tìm kiếm video…'}
          value={keywordDraft}
          onChange={(e) => setKeywordDraft(e.target.value)}
          style={searchInputStyle}
        />
        <button type="submit" style={searchBtnStyle}>
          Tìm
        </button>
      </form>

      {loading ? (
        <div style={emptyStyle}>⏳ Đang tải nội dung…</div>
      ) : errorMsg ? (
        <div style={{ ...emptyStyle, color: '#fca5a5' }}>
          <div style={{ fontSize: 32, marginBottom: 8 }}>⚠️</div>
          <p>{errorMsg}</p>
        </div>
      ) : items.length === 0 ? (
        <div style={emptyStyle}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>📭</div>
          <p>
            {keyword.trim()
              ? 'Không tìm thấy nội dung nào phù hợp với từ khóa.'
              : activeTab === 'ARTICLE'
                ? 'Hệ thống chưa có bài viết nào được xuất bản.'
                : 'Hệ thống chưa có video nào được xuất bản.'}
          </p>
        </div>
      ) : activeTab === 'ARTICLE' ? (
        <div style={articlesGridStyle}>
          {items.map((art) => (
            <article key={art.id} style={articleCardStyle}>
              {art.coverImageUrl ? (
                <img src={art.coverImageUrl} alt={art.title} style={articleImgStyle} />
              ) : (
                <div style={placeholderCoverStyle}>📰</div>
              )}
              <div style={articleBodyStyle}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, gap: 8 }}>
                  <span style={categoryBadgeStyle}>Bài viết</span>
                  <span style={readTimeStyle}>📅 {formatDate(art.publishedAt)}</span>
                </div>

                <h3 style={articleTitleStyle}>{art.title}</h3>
                {art.summary && <p style={articleSummaryStyle}>{art.summary}</p>}

                {(art.tags ?? []).length > 0 && (
                  <div style={tagRowStyle}>
                    {(art.tags ?? []).map((tag) => (
                      <span key={tag} style={tagStyle}>
                        #{tag}
                      </span>
                    ))}
                  </div>
                )}

                <div style={articleFooterStyle}>
                  <button type="button" onClick={() => setSelectedArticle(art)} style={readBtnStyle}>
                    Đọc tiếp ➔
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div style={videosGridStyle}>
          {items.map((vid) => (
            <div key={vid.id} style={videoCardStyle}>
              <div style={videoThumbWrapperStyle} onClick={() => setSelectedVideo(vid)}>
                {vid.coverImageUrl ? (
                  <img src={vid.coverImageUrl} alt={vid.title} style={videoThumbStyle} />
                ) : (
                  <div style={placeholderCoverStyle}>🎬</div>
                )}
                {vid.videoUrl && <div style={playOverlayStyle}>▶</div>}
              </div>

              <div style={{ padding: 16 }}>
                <span style={categoryBadgeStyle}>Video</span>
                <h3 style={videoTitleStyle}>{vid.title}</h3>
                {vid.summary && <p style={articleSummaryStyle}>{vid.summary}</p>}
                <div style={videoMetaStyle}>📅 {formatDate(vid.publishedAt)}</div>
                {(vid.tags ?? []).length > 0 && (
                  <div style={tagRowStyle}>
                    {(vid.tags ?? []).map((tag) => (
                      <span key={tag} style={tagStyle}>
                        #{tag}
                      </span>
                    ))}
                  </div>
                )}
                {vid.videoUrl ? (
                  <button type="button" onClick={() => setSelectedVideo(vid)} style={readBtnStyle}>
                    ▶ Xem video
                  </button>
                ) : (
                  <span style={{ fontSize: 12, color: '#64748b' }}>Chưa có đường dẫn video.</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Chi tiết bài viết */}
      {selectedArticle && (
        <div style={modalOverlayStyle} onClick={() => setSelectedArticle(null)}>
          <div style={modalContentStyle} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <span style={categoryBadgeStyle}>Bài viết</span>
              <button onClick={() => setSelectedArticle(null)} style={closeBtnStyle} aria-label="Đóng">
                ✕
              </button>
            </div>

            <h2 style={{ color: '#fff', fontSize: 22, fontWeight: 800, marginBottom: 12, lineHeight: 1.4 }}>
              {selectedArticle.title}
            </h2>
            <div style={{ fontSize: 12, color: '#64748b', marginBottom: 20 }}>
              📅 {formatDate(selectedArticle.publishedAt)}
            </div>

            {selectedArticle.coverImageUrl && (
              <img
                src={selectedArticle.coverImageUrl}
                alt={selectedArticle.title}
                style={{ width: '100%', height: 240, objectFit: 'cover', borderRadius: 12, marginBottom: 20 }}
              />
            )}

            {selectedArticle.content ? (
              <div style={articleContentFormattedStyle}>{selectedArticle.content}</div>
            ) : (
              <p style={{ color: '#64748b', fontSize: 13 }}>Bài viết chưa có nội dung chi tiết.</p>
            )}
          </div>
        </div>
      )}

      {/* Trình phát video */}
      {selectedVideo && (
        <div style={modalOverlayStyle} onClick={() => setSelectedVideo(null)}>
          <div style={{ ...modalContentStyle, maxWidth: 720 }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <span style={categoryBadgeStyle}>Video</span>
              <button onClick={() => setSelectedVideo(null)} style={closeBtnStyle} aria-label="Đóng">
                ✕
              </button>
            </div>

            <h3 style={{ color: '#fff', fontSize: 18, fontWeight: 700, marginBottom: 16 }}>
              {selectedVideo.title}
            </h3>

            {selectedVideo.videoUrl ? (
              <video
                controls
                autoPlay
                src={selectedVideo.videoUrl}
                poster={selectedVideo.coverImageUrl ?? undefined}
                style={{ width: '100%', borderRadius: 12, background: '#000' }}
              />
            ) : (
              <p style={{ color: '#64748b', fontSize: 13 }}>Video này chưa có đường dẫn phát.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

/* ── Inline Styles ── */
const containerStyle: React.CSSProperties = { padding: 24, maxWidth: 1050, margin: '0 auto' };
const headerStyle: React.CSSProperties = { marginBottom: 24 };
const titleStyle: React.CSSProperties = { fontSize: 24, fontWeight: 800, color: '#fff', margin: 0 };
const subtitleStyle: React.CSSProperties = { fontSize: 14, color: 'var(--text-secondary)', marginTop: 6 };
const topTabContainerStyle: React.CSSProperties = { display: 'flex', gap: 12, marginBottom: 20 };
const topTabStyle: React.CSSProperties = {
  padding: '12px 24px', borderRadius: 12, border: 'none', fontSize: 14, fontWeight: 700, cursor: 'pointer',
};
const searchBoxStyle: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24,
  background: 'rgba(30, 41, 59, 0.7)', border: '1px solid rgba(255, 255, 255, 0.1)',
  borderRadius: 10, padding: '10px 16px',
};
const searchInputStyle: React.CSSProperties = {
  flex: 1, background: 'transparent', border: 'none', outline: 'none', color: '#fff', fontSize: 14,
};
const searchBtnStyle: React.CSSProperties = {
  background: 'rgba(16,185,129,0.15)', border: '1px solid rgba(16,185,129,0.3)', color: '#34d399',
  padding: '6px 16px', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer',
};
const articlesGridStyle: React.CSSProperties = {
  display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(310px, 1fr))', gap: 24,
};
const articleCardStyle: React.CSSProperties = {
  background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255, 255, 255, 0.08)',
  borderRadius: 16, overflow: 'hidden', display: 'flex', flexDirection: 'column',
};
const articleImgStyle: React.CSSProperties = { width: '100%', height: 180, objectFit: 'cover' };
const placeholderCoverStyle: React.CSSProperties = {
  width: '100%', height: 180, display: 'flex', alignItems: 'center', justifyContent: 'center',
  fontSize: 44, color: '#475569',
  background: 'repeating-linear-gradient(45deg, rgba(148,163,184,0.08) 0 12px, rgba(148,163,184,0.16) 12px 24px)',
};
const articleBodyStyle: React.CSSProperties = { padding: 20, display: 'flex', flexDirection: 'column', flex: 1 };
const categoryBadgeStyle: React.CSSProperties = {
  fontSize: 11, fontWeight: 700, color: '#38bdf8', background: 'rgba(56, 189, 248, 0.15)',
  padding: '2px 8px', borderRadius: 6, alignSelf: 'flex-start',
};
const readTimeStyle: React.CSSProperties = { fontSize: 11, color: 'var(--text-muted)' };
const articleTitleStyle: React.CSSProperties = {
  fontSize: 16, fontWeight: 700, color: '#fff', margin: '10px 0', lineHeight: 1.4,
};
const articleSummaryStyle: React.CSSProperties = {
  fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: 12,
};
const tagRowStyle: React.CSSProperties = { display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 12 };
const tagStyle: React.CSSProperties = {
  fontSize: 11, color: '#a5b4fc', background: 'rgba(129,140,248,0.12)',
  border: '1px solid rgba(129,140,248,0.25)', padding: '2px 8px', borderRadius: 10,
};
const articleFooterStyle: React.CSSProperties = {
  marginTop: 'auto', display: 'flex', justifyContent: 'flex-end', alignItems: 'center',
  paddingTop: 12, borderTop: '1px solid rgba(255, 255, 255, 0.06)',
};
const readBtnStyle: React.CSSProperties = {
  background: 'none', border: 'none', color: '#34d399', fontWeight: 700, cursor: 'pointer', fontSize: 13,
};
const videosGridStyle: React.CSSProperties = {
  display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))', gap: 20,
};
const videoCardStyle: React.CSSProperties = {
  background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255, 255, 255, 0.08)',
  borderRadius: 16, overflow: 'hidden',
};
const videoThumbWrapperStyle: React.CSSProperties = { position: 'relative', height: 170, cursor: 'pointer' };
const videoThumbStyle: React.CSSProperties = { width: '100%', height: '100%', objectFit: 'cover' };
const playOverlayStyle: React.CSSProperties = {
  position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
  width: 48, height: 48, borderRadius: '50%', background: 'rgba(16, 185, 129, 0.9)', color: '#fff',
  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20,
  boxShadow: '0 4px 15px rgba(0,0,0,0.5)',
};
const videoTitleStyle: React.CSSProperties = {
  fontSize: 15, fontWeight: 700, color: '#fff', margin: '8px 0 6px 0', lineHeight: 1.4,
};
const videoMetaStyle: React.CSSProperties = { fontSize: 12, color: 'var(--text-muted)', marginBottom: 8 };
const emptyStyle: React.CSSProperties = {
  textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)',
  background: 'rgba(255,255,255,0.02)', borderRadius: 12,
};
const modalOverlayStyle: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0, 0, 0, 0.8)', backdropFilter: 'blur(6px)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000, padding: 20,
};
const modalContentStyle: React.CSSProperties = {
  background: '#1e293b', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: 20,
  padding: 28, maxWidth: 680, width: '100%', maxHeight: '90vh', overflowY: 'auto',
};
const closeBtnStyle: React.CSSProperties = {
  background: 'none', border: 'none', color: 'var(--text-muted)', fontSize: 20, cursor: 'pointer',
};
const articleContentFormattedStyle: React.CSSProperties = {
  fontSize: 14, color: '#cbd5e1', lineHeight: 1.7, whiteSpace: 'pre-wrap', overflowWrap: 'anywhere',
};
