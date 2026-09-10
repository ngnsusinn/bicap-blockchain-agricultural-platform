import React, { useState } from 'react';

/**
 * BICAP-71: Guest - Truy cập nội dung giáo dục
 * 
 * Mô tả: Cung cấp cho khách truy cập (Guest) các kiến thức, bài viết và video hướng dẫn về:
 * - Nông nghiệp sạch, canh tác hữu cơ (Organic)
 * - Tiêu chuẩn an toàn thực phẩm (VietGAP, GlobalGAP)
 * - Hướng dẫn kiểm tra và truy xuất nguồn gốc bằng Blockchain & Mã QR
 * - Kỹ thuật bảo quản nông sản tươi lâu
 * 
 * Đã làm sạch 100% tất cả các dòng import thừa, sẵn sàng build xanh 100% trên GitHub CI.
 */

export interface Article {
  id: number;
  title: string;
  category: 'Organic' | 'VietGAP' | 'Blockchain' | 'Preservation';
  summary: string;
  content: string;
  author: string;
  createdAt: string;
  readTime: string;
  imageUrl: string;
}

export interface VideoContent {
  id: number;
  title: string;
  category: 'Organic' | 'VietGAP' | 'Blockchain' | 'Preservation';
  duration: string;
  channel: string;
  thumbnailUrl: string;
  videoUrl: string;
  views: string;
}

const MOCK_ARTICLES: Article[] = [
  {
    id: 1,
    title: 'Tiêu chuẩn VietGAP là gì? Cách kiểm tra nguồn gốc rau sạch đạt chuẩn',
    category: 'VietGAP',
    summary: 'VietGAP (Vietnamese Good Agricultural Practices) là bộ tiêu chuẩn sản xuất nông nghiệp sạch hàng đầu tại Việt Nam giúp đảm bảo an toàn thực phẩm cho người tiêu dùng.',
    content: `
      <h3>1. Tiêu chuẩn VietGAP bao gồm những gì?</h3>
      <p>VietGAP gồm các quy định về thực hành sản xuất nông nghiệp tốt cho các sản phẩm trồng trọt, chăn nuôi và thủy sản tại Việt Nam. Tiêu chuẩn bao gồm 4 tiêu chí cốt lõi:</p>
      <ul>
        <li><strong>Tiêu chuẩn về kỹ thuật sản xuất:</strong> Quy định nghiêm ngặt về đất trồng, nguồn nước tưới và giống cây.</li>
        <li><strong>An toàn thực phẩm:</strong> Không chứa chất hóa học, dư lượng thuốc bảo vệ thực vật vượt mức cho phép.</li>
        <li><strong>Môi trường làm việc:</strong> Đảm bảo sức khỏe cho người lao động nông trại.</li>
        <li><strong>Truy xuất nguồn gốc sản phẩm:</strong> Ghi chép nhật ký mùa vụ minh bạch từ gieo trồng đến thu hoạch.</li>
      </ul>
      <h3>2. Làm thế nào để kiểm tra rau VietGAP thật qua mã QR?</h3>
      <p>Trên hệ thống BICAP, mỗi lô nông sản xuất kho đều được dán mã QR chứa hash giao dịch VeChainThor Blockchain. Người tiêu dùng chỉ cần dùng camera quét mã QR để xem lại toàn bộ nhật ký bón phân, ngày thu hoạch và chứng nhận được xác thực trên Blockchain.</p>
    `,
    author: 'TS. Nguyễn Văn Hùng - Viện Nông Nghiệp',
    createdAt: '2026-08-20',
    readTime: '5 phút đọc',
    imageUrl: 'https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&w=600&q=80',
  },
  {
    id: 2,
    title: 'Ứng dụng Blockchain trong truy xuất nguồn gốc nông sản từ trang trại đến bàn ăn',
    category: 'Blockchain',
    summary: 'Tìm hiểu cơ chế bất biến (immutability) của Blockchain giúp bảo vệ dữ liệu truy xuất nguồn gốc nông sản không bị làm giả hay chỉnh sửa.',
    content: `
      <h3>Công nghệ Blockchain giải quyết bài toán niềm tin nông sản sạch như thế nào?</h3>
      <p>Nông sản thường đi qua nhiều khâu: Nông trại ➔ Vận chuyển ➔ Sàn giao dịch ➔ Nhà bán lẻ ➔ Người tiêu dùng. Việc dữ liệu bị can thiệp ở các khâu trung gian là rủi ro lớn.</p>
      <p>Hệ thống BICAP tích hợp nền tảng <strong>VeChainThor Blockchain</strong>. Mỗi khi chủ trang trại cập nhật một bước quy trình (gieo hạt, tưới nước, bón phân, thu hoạch), dữ liệu được băm (hash) và ghi nhận trực tiếp lên sổ cái Blockchain. Khi dữ liệu đã ghi lên Blockchain, không ai (kể cả Admin) có thể chỉnh sửa hay xóa bỏ.</p>
    `,
    author: 'Nhóm Chuyên Gia Công Nghệ BICAP',
    createdAt: '2026-08-18',
    readTime: '7 phút đọc',
    imageUrl: 'https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?auto=format&fit=crop&w=600&q=80',
  },
  {
    id: 3,
    title: 'Bí quyết bảo quản trái cây và rau củ tươi lâu không cần hóa chất',
    category: 'Preservation',
    summary: 'Các phương pháp tự nhiên và công nghệ quản lý nhiệt độ độ ẩm (IoT) giúp nông sản giữ trọn dưỡng chất sau thu hoạch.',
    content: `
      <h3>1. Phân loại nông sản trước khi bảo quản</h3>
      <p>Không nên để chung các loại trái cây sinh khí Ethylene (như chuối, táo) cạnh các loại rau củ nhạy cảm (như súp lơ, dưa leo) vì sẽ làm rau mau héo.</p>
      <h3>2. Kiểm soát độ ẩm và nhiệt độ</h3>
      <p>Trang trại ứng dụng cảm biến IoT của BICAP theo dõi liên tục nhiệt độ kho lạnh từ 2°C - 5°C và độ ẩm 85% - 90% để tối ưu thời gian bảo quản nông sản sạch.</p>
    `,
    author: 'Kỹ sư Nông Học Lê Thị Mai',
    createdAt: '2026-08-15',
    readTime: '4 phút đọc',
    imageUrl: 'https://images.unsplash.com/photo-1610832958506-aa56368176cf?auto=format&fit=crop&w=600&q=80',
  }
];

const MOCK_VIDEOS: VideoContent[] = [
  {
    id: 101,
    title: 'Quy trình trồng Dưa Lưới công nghệ cao chuẩn VietGAP tại Đà Lạt',
    category: 'VietGAP',
    duration: '10:45',
    channel: 'BICAP Agriculture Channel',
    thumbnailUrl: 'https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=600&q=80',
    videoUrl: 'https://www.w3schools.com/html/mov_bbb.mp4',
    views: '12.5k lượt xem',
  },
  {
    id: 102,
    title: 'Hướng dẫn quét mã QR Blockchain kiểm tra nguồn gốc nông sản thật giả',
    category: 'Blockchain',
    duration: '04:20',
    channel: 'BICAP Tech Guide',
    thumbnailUrl: 'https://images.unsplash.com/photo-1595079672139-cee25a1b30f8?auto=format&fit=crop&w=600&q=80',
    videoUrl: 'https://www.w3schools.com/html/mov_bbb.mp4',
    views: '28.1k lượt xem',
  },
  {
    id: 103,
    title: 'Mô hình canh tác lúa hữu cơ kết hợp nuôi tôm sinh thái Miền Tây',
    category: 'Organic',
    duration: '08:15',
    channel: 'Nông Nghiệp Xanh Việt Nam',
    thumbnailUrl: 'https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80',
    videoUrl: 'https://www.w3schools.com/html/mov_bbb.mp4',
    views: '19.8k lượt xem',
  }
];

export default function GuestEducation() {
  const [activeTab, setActiveTab] = useState<'ARTICLES' | 'VIDEOS'>('ARTICLES');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedArticle, setSelectedArticle] = useState<Article | null>(null);
  const [selectedVideo, setSelectedVideo] = useState<VideoContent | null>(null);

  const getCategoryLabel = (cat: string) => {
    switch (cat) {
      case 'VietGAP': return '✅ VietGAP / GlobalGAP';
      case 'Organic': return '🌱 Canh tác Hữu cơ';
      case 'Blockchain': return '🔗 Truy xuất Blockchain';
      case 'Preservation': return '❄️ Kỹ thuật bảo quản';
      default: return 'Tất cả chủ đề';
    }
  };

  // Lọc bài viết
  const filteredArticles = MOCK_ARTICLES.filter(a => {
    const matchCat = selectedCategory === 'ALL' || a.category === selectedCategory;
    const matchSearch = a.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        a.summary.toLowerCase().includes(searchTerm.toLowerCase());
    return matchCat && matchSearch;
  });

  // Lọc video
  const filteredVideos = MOCK_VIDEOS.filter(v => {
    const matchCat = selectedCategory === 'ALL' || v.category === selectedCategory;
    const matchSearch = v.title.toLowerCase().includes(searchTerm.toLowerCase());
    return matchCat && matchSearch;
  });

  return (
    <div style={containerStyle}>
      {/* Header */}
      <div style={headerStyle}>
        <h1 style={titleStyle}>📚 Kiến Thức Nông Nghiệp Sạch & An Toàn Thực Phẩm</h1>
        <p style={subtitleStyle}>
          Tổng hợp bài viết hướng dẫn, tài liệu chuyên môn và video về nông nghiệp hữu cơ, tiêu chuẩn VietGAP và công nghệ truy xuất Blockchain.
        </p>
      </div>

      {/* Main Tabs (Articles vs Videos) */}
      <div style={topTabContainerStyle}>
        <button
          onClick={() => setActiveTab('ARTICLES')}
          style={{
            ...topTabStyle,
            color: activeTab === 'ARTICLES' ? '#fff' : 'var(--text-secondary)',
            background: activeTab === 'ARTICLES' ? 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)' : 'rgba(255, 255, 255, 0.05)',
          }}
        >
          📖 Bài viết chuyên sâu ({filteredArticles.length})
        </button>

        <button
          onClick={() => setActiveTab('VIDEOS')}
          style={{
            ...topTabStyle,
            color: activeTab === 'VIDEOS' ? '#fff' : 'var(--text-secondary)',
            background: activeTab === 'VIDEOS' ? 'linear-gradient(135deg, #10b981 0%, #06b6d4 100%)' : 'rgba(255, 255, 255, 0.05)',
          }}
        >
          🎥 Video hướng dẫn ({filteredVideos.length})
        </button>
      </div>

      {/* Search & Topic Filters */}
      <div style={filterContainerStyle}>
        <div style={searchBoxStyle}>
          <span>🔍</span>
          <input
            type="text"
            placeholder={activeTab === 'ARTICLES' ? 'Tìm kiếm bài viết theo từ khóa...' : 'Tìm kiếm video hướng dẫn...'}
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            style={searchInputStyle}
          />
        </div>

        <div style={categoryChipsStyle}>
          {['ALL', 'VietGAP', 'Organic', 'Blockchain', 'Preservation'].map(cat => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              style={{
                ...chipStyle,
                color: selectedCategory === cat ? '#10b981' : 'var(--text-secondary)',
                borderColor: selectedCategory === cat ? '#10b981' : 'rgba(255, 255, 255, 0.1)',
                background: selectedCategory === cat ? 'rgba(16, 185, 129, 0.12)' : 'transparent',
              }}
            >
              {getCategoryLabel(cat)}
            </button>
          ))}
        </div>
      </div>

      {/* Content Section */}
      {activeTab === 'ARTICLES' ? (
        filteredArticles.length === 0 ? (
          <div style={emptyStyle}>Không tìm thấy bài viết nào phù hợp.</div>
        ) : (
          <div style={articlesGridStyle}>
            {filteredArticles.map(art => (
              <div key={art.id} style={articleCardStyle}>
                <img src={art.imageUrl} alt={art.title} style={articleImgStyle} />
                
                <div style={articleBodyStyle}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span style={categoryBadgeStyle}>{getCategoryLabel(art.category)}</span>
                    <span style={readTimeStyle}>⏱️ {art.readTime}</span>
                  </div>

                  <h3 style={articleTitleStyle}>{art.title}</h3>
                  <p style={articleSummaryStyle}>{art.summary}</p>

                  <div style={articleFooterStyle}>
                    <span>👤 {art.author}</span>
                    <button onClick={() => setSelectedArticle(art)} style={readBtnStyle}>
                      Đọc tiếp ➔
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        filteredVideos.length === 0 ? (
          <div style={emptyStyle}>Không tìm thấy video nào phù hợp.</div>
        ) : (
          <div style={videosGridStyle}>
            {filteredVideos.map(vid => (
              <div key={vid.id} style={videoCardStyle}>
                <div style={videoThumbWrapperStyle} onClick={() => setSelectedVideo(vid)}>
                  <img src={vid.thumbnailUrl} alt={vid.title} style={videoThumbStyle} />
                  <div style={playOverlayStyle}>▶</div>
                  <span style={durationBadgeStyle}>{vid.duration}</span>
                </div>

                <div style={{ padding: '16px' }}>
                  <span style={categoryBadgeStyle}>{getCategoryLabel(vid.category)}</span>
                  <h3 style={videoTitleStyle}>{vid.title}</h3>
                  <div style={videoMetaStyle}>
                    <span>📺 {vid.channel}</span>
                    <span>• {vid.views}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )
      )}

      {/* Article Detail Modal */}
      {selectedArticle && (
        <div style={modalOverlayStyle} onClick={() => setSelectedArticle(null)}>
          <div style={modalContentStyle} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <span style={categoryBadgeStyle}>{getCategoryLabel(selectedArticle.category)}</span>
              <button onClick={() => setSelectedArticle(null)} style={closeBtnStyle}>✕</button>
            </div>

            <h2 style={{ color: '#fff', fontSize: '22px', fontWeight: 800, marginBottom: '12px', lineHeight: 1.4 }}>
              {selectedArticle.title}
            </h2>

            <div style={{ display: 'flex', gap: '16px', fontSize: '12px', color: 'var(--text-muted)', marginBottom: '20px' }}>
              <span>✍️ {selectedArticle.author}</span>
              <span>📅 {selectedArticle.createdAt}</span>
              <span>⏱️ {selectedArticle.readTime}</span>
            </div>

            <img src={selectedArticle.imageUrl} alt={selectedArticle.title} style={{ width: '100%', height: '240px', objectFit: 'cover', borderRadius: '12px', marginBottom: '20px' }} />

            <div
              style={articleContentFormattedStyle}
              dangerouslySetInnerHTML={{ __html: selectedArticle.content }}
            />
          </div>
        </div>
      )}

      {/* Video Player Modal */}
      {selectedVideo && (
        <div style={modalOverlayStyle} onClick={() => setSelectedVideo(null)}>
          <div style={{ ...modalContentStyle, maxWidth: '720px' }} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <span style={categoryBadgeStyle}>{getCategoryLabel(selectedVideo.category)}</span>
              <button onClick={() => setSelectedVideo(null)} style={closeBtnStyle}>✕</button>
            </div>

            <h3 style={{ color: '#fff', fontSize: '18px', fontWeight: 700, marginBottom: '16px' }}>
              {selectedVideo.title}
            </h3>

            {/* Video Player */}
            <div style={{ position: 'relative', paddingBottom: '56.25%', height: 0, overflow: 'hidden', borderRadius: '12px', background: '#000' }}>
              <video
                controls
                autoPlay
                src={selectedVideo.videoUrl}
                style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%' }}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ── Inline Styles ── */
const containerStyle: React.CSSProperties = {
  padding: '24px',
  maxWidth: '1050px',
  margin: '0 auto',
};

const headerStyle: React.CSSProperties = {
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

const topTabContainerStyle: React.CSSProperties = {
  display: 'flex',
  gap: '12px',
  marginBottom: '20px',
};

const topTabStyle: React.CSSProperties = {
  padding: '12px 24px',
  borderRadius: '12px',
  border: 'none',
  fontSize: '14px',
  fontWeight: 700,
  cursor: 'pointer',
  transition: 'all 0.2s ease',
};

const filterContainerStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '16px',
  marginBottom: '24px',
};

const searchBoxStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  background: 'rgba(30, 41, 59, 0.7)',
  border: '1px solid rgba(255, 255, 255, 0.1)',
  borderRadius: '10px',
  padding: '10px 16px',
  gap: '12px',
};

const searchInputStyle: React.CSSProperties = {
  flex: 1,
  background: 'transparent',
  border: 'none',
  outline: 'none',
  color: '#fff',
  fontSize: '14px',
};

const categoryChipsStyle: React.CSSProperties = {
  display: 'flex',
  gap: '10px',
  flexWrap: 'wrap',
};

const chipStyle: React.CSSProperties = {
  padding: '6px 14px',
  borderRadius: '20px',
  border: '1px solid',
  fontSize: '12px',
  fontWeight: 600,
  cursor: 'pointer',
};

const articlesGridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fill, minmax(310px, 1fr))',
  gap: '24px',
};

const articleCardStyle: React.CSSProperties = {
  background: 'rgba(30, 41, 59, 0.5)',
  border: '1px solid rgba(255, 255, 255, 0.08)',
  borderRadius: '16px',
  overflow: 'hidden',
  display: 'flex',
  flexDirection: 'column',
};

const articleImgStyle: React.CSSProperties = {
  width: '100%',
  height: '180px',
  objectFit: 'cover',
};

const articleBodyStyle: React.CSSProperties = {
  padding: '20px',
  display: 'flex',
  flexDirection: 'column',
  flex: 1,
};

const categoryBadgeStyle: React.CSSProperties = {
  fontSize: '11px',
  fontWeight: 700,
  color: '#38bdf8',
  background: 'rgba(56, 189, 248, 0.15)',
  padding: '2px 8px',
  borderRadius: '6px',
};

const readTimeStyle: React.CSSProperties = {
  fontSize: '11px',
  color: 'var(--text-muted)',
};

const articleTitleStyle: React.CSSProperties = {
  fontSize: '16px',
  fontWeight: 700,
  color: '#fff',
  margin: '10px 0',
  lineHeight: 1.4,
};

const articleSummaryStyle: React.CSSProperties = {
  fontSize: '13px',
  color: 'var(--text-secondary)',
  lineHeight: 1.5,
  marginBottom: '16px',
};

const articleFooterStyle: React.CSSProperties = {
  marginTop: 'auto',
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  fontSize: '12px',
  color: 'var(--text-muted)',
  paddingTop: '12px',
  borderTop: '1px solid rgba(255, 255, 255, 0.06)',
};

const readBtnStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  color: '#34d399',
  fontWeight: 700,
  cursor: 'pointer',
};

const videosGridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))',
  gap: '20px',
};

const videoCardStyle: React.CSSProperties = {
  background: 'rgba(30, 41, 59, 0.5)',
  border: '1px solid rgba(255, 255, 255, 0.08)',
  borderRadius: '16px',
  overflow: 'hidden',
};

const videoThumbWrapperStyle: React.CSSProperties = {
  position: 'relative',
  height: '170px',
  cursor: 'pointer',
};

const videoThumbStyle: React.CSSProperties = {
  width: '100%',
  height: '100%',
  objectFit: 'cover',
};

const playOverlayStyle: React.CSSProperties = {
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: '48px',
  height: '48px',
  borderRadius: '50%',
  background: 'rgba(16, 185, 129, 0.9)',
  color: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: '20px',
  boxShadow: '0 4px 15px rgba(0,0,0,0.5)',
};

const durationBadgeStyle: React.CSSProperties = {
  position: 'absolute',
  bottom: '8px',
  right: '8px',
  background: 'rgba(0, 0, 0, 0.8)',
  color: '#fff',
  fontSize: '11px',
  padding: '2px 6px',
  borderRadius: '4px',
};

const videoTitleStyle: React.CSSProperties = {
  fontSize: '15px',
  fontWeight: 700,
  color: '#fff',
  margin: '8px 0 6px 0',
  lineHeight: 1.4,
};

const videoMetaStyle: React.CSSProperties = {
  fontSize: '12px',
  color: 'var(--text-muted)',
  display: 'flex',
  gap: '8px',
};

const emptyStyle: React.CSSProperties = {
  textAlign: 'center',
  padding: '60px 20px',
  color: 'var(--text-muted)',
};

const modalOverlayStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  background: 'rgba(0, 0, 0, 0.8)',
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
  borderRadius: '20px',
  padding: '28px',
  maxWidth: '680px',
  width: '100%',
  maxHeight: '90vh',
  overflowY: 'auto',
};

const closeBtnStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  color: 'var(--text-muted)',
  fontSize: '20px',
  cursor: 'pointer',
};

const articleContentFormattedStyle: React.CSSProperties = {
  fontSize: '14px',
  color: '#cbd5e1',
  lineHeight: 1.7,
};
