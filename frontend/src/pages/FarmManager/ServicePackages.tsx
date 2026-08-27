import React, { useState, useEffect, useCallback } from 'react';
import PaymentModal, { type PaymentData } from '../../components/PaymentModal';
import { getAuthHeaders, isLoggedIn, API_BASE_URL } from '../../utils/auth';

interface Package {
  id: number;
  name: string;
  description: string;
  price?: number;
  durationDays?: number;
  features?: string | string[] | null;
  status: string;
}

interface Subscription {
  id: number;
  farmId: number;
  packageName: string;
  startDate: string;
  endDate: string;
  status: string;
}

const ServicePackages: React.FC = () => {
  const [packages, setPackages] = useState<Package[]>([]);
  const [currentSubscription, setCurrentSubscription] = useState<Subscription | null>(null);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [paymentData, setPaymentData] = useState<PaymentData | null>(null);
  const [loading, setLoading] = useState(true);
  const [subscribing, setSubscribing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    try {
      const pkgRes = await fetch(`${API_BASE_URL}/service-packages`);
      if (pkgRes.ok) {
        const pkgData = await pkgRes.json();
        setPackages(pkgData);
      }

      // M-2: use the authenticated user's own subscriptions rather than a hardcoded farmId.
      const subRes = await fetch(`${API_BASE_URL}/subscriptions/my`, {
        headers: getAuthHeaders(),
      });
      if (subRes.ok) {
        const subData = await subRes.json();
        const activeSub = Array.isArray(subData)
          ? subData.find((s: Subscription) => s.status === 'ACTIVE')
          : (subData && subData.status === 'ACTIVE' ? subData : null);
        setCurrentSubscription(activeSub || null);
      }
    } catch (err) {
      setError('Không thể tải dữ liệu gói dịch vụ.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSubscribe = async (pkg: Package) => {
    if (!isLoggedIn()) {
      setError('Vui lòng đăng nhập để mua gói dịch vụ');
      setTimeout(() => setError(null), 3000);
      return;
    }
    if (subscribing) return; // M-17: prevent double-click duplicate purchases.
    setSubscribing(true);
    try {
      const res = await fetch(`${API_BASE_URL}/subscriptions/purchase`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ packageId: pkg.id })
      });
      if (res.ok) {
        const data = await res.json();
        setPaymentData(data);
        setIsPaymentModalOpen(true);
      } else {
        const errData = await res.json().catch(() => ({}));
        setError(errData.message || 'Không thể khởi tạo thanh toán cho gói dịch vụ.');
        setTimeout(() => setError(null), 3000);
      }
    } catch (err) {
      setError('Không thể kết nối máy chủ. Vui lòng thử lại.');
      setTimeout(() => setError(null), 3000);
    } finally {
      setSubscribing(false);
    }
  };

  const pageStyle: React.CSSProperties = {
    minHeight: '100vh',
    backgroundColor: '#0a0b10',
    color: '#fff',
    fontFamily: "'Inter', sans-serif",
    padding: '40px 20px',
    boxSizing: 'border-box'
  };

  const containerStyle: React.CSSProperties = {
    maxWidth: '1200px',
    margin: '0 auto',
  };

  const headerStyle: React.CSSProperties = {
    textAlign: 'center',
    marginBottom: '60px',
    animation: 'fadeInDown 0.6s ease-out forwards'
  };

  const titleStyle: React.CSSProperties = {
    fontSize: '48px',
    fontWeight: 800,
    marginBottom: '16px',
    background: 'linear-gradient(135deg, #8b5cf6, #06b6d4)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  };

  const subtitleStyle: React.CSSProperties = {
    fontSize: '18px',
    color: '#a1a1aa',
    maxWidth: '600px',
    margin: '0 auto',
    lineHeight: 1.6
  };

  const bannerStyle: React.CSSProperties = {
    background: 'linear-gradient(90deg, rgba(139, 92, 246, 0.1), rgba(6, 182, 212, 0.1))',
    border: '1px solid rgba(139, 92, 246, 0.3)',
    borderRadius: '16px',
    padding: '24px',
    marginBottom: '40px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    animation: 'fadeIn 0.8s ease-out forwards'
  };

  const gridStyle: React.CSSProperties = {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
    gap: '32px',
    padding: '20px 0'
  };

  const cardStyle = (index: number): React.CSSProperties => ({
    background: 'rgba(22, 23, 33, 0.6)',
    backdropFilter: 'blur(16px)',
    border: '1px solid rgba(255, 255, 255, 0.08)',
    borderRadius: '24px',
    padding: '40px 32px',
    display: 'flex',
    flexDirection: 'column',
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    cursor: 'default',
    animation: `slideUpFade 0.6s ease-out ${index * 0.15}s forwards`,
    opacity: 0,
    position: 'relative',
    overflow: 'hidden'
  });

  const getFeatures = (features: Package['features']): string[] => {
    if (Array.isArray(features)) {
      return features.filter((feature): feature is string => typeof feature === 'string' && feature.trim().length > 0);
    }
    if (!features || !features.trim()) {
      return [];
    }
    try {
      const parsed = JSON.parse(features);
      return Array.isArray(parsed)
        ? parsed.filter((feature): feature is string => typeof feature === 'string' && feature.trim().length > 0)
        : [];
    } catch {
      return features.split(',').map(feature => feature.trim()).filter(Boolean);
    }
  };

  if (loading) {
    return (
      <div style={{...pageStyle, display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
        <div style={{color: '#8b5cf6', fontSize: '24px', animation: 'pulse 1.5s infinite'}}>Đang tải gói dịch vụ...</div>
      </div>
    );
  }

  return (
    <>
      <style>
        {`
          @keyframes fadeInDown {
            from { opacity: 0; transform: translateY(-30px); }
            to { opacity: 1; transform: translateY(0); }
          }
          @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
          }
          @keyframes slideUpFade {
            from { opacity: 0; transform: translateY(40px); }
            to { opacity: 1; transform: translateY(0); }
          }
          .pkg-card:hover {
            transform: scale(1.03) translateY(-5px);
            box-shadow: 0 20px 40px -10px rgba(139, 92, 246, 0.2);
            border-color: rgba(139, 92, 246, 0.4);
          }
          .subscribe-btn {
            background: linear-gradient(135deg, #8b5cf6, #06b6d4);
            color: #fff;
            border: none;
            padding: 16px 24px;
            border-radius: 12px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
            width: 100%;
            margin-top: auto;
          }
          .subscribe-btn:hover {
            opacity: 0.9;
            transform: translateY(-2px);
            box-shadow: 0 10px 20px -10px rgba(139, 92, 246, 0.5);
          }
          .subscribe-btn:active {
            transform: translateY(0);
          }
        `}
      </style>
      <div style={pageStyle}>
        <div style={containerStyle}>
          
          <div style={headerStyle}>
            <h1 style={titleStyle}>Gói Dịch Vụ</h1>
            <p style={subtitleStyle}>Chọn gói phù hợp để sử dụng các tính năng quản lý nông trại nâng cao.</p>
          </div>

          {error && (
            <div style={{ background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', padding: '16px', borderRadius: '12px', textAlign: 'center', marginBottom: '24px' }}>
              {error}
            </div>
          )}

          {currentSubscription && currentSubscription.status === 'ACTIVE' && (
            <div style={bannerStyle}>
              <div>
                <h3 style={{ margin: '0 0 8px 0', fontSize: '20px', color: '#fff' }}>Gói hiện tại: <span style={{color: '#06b6d4'}}>{currentSubscription.packageName}</span></h3>
                <p style={{ margin: 0, color: '#a1a1aa', fontSize: '14px' }}>
                  Hiệu lực từ {new Date(currentSubscription.startDate).toLocaleDateString('vi-VN')} đến {new Date(currentSubscription.endDate).toLocaleDateString('vi-VN')}
                </p>
              </div>
              <div style={{ background: 'rgba(16, 185, 129, 0.2)', color: '#10b981', padding: '8px 16px', borderRadius: '20px', fontWeight: 600, fontSize: '14px' }}>
                ĐANG HOẠT ĐỘNG
              </div>
            </div>
          )}

          <div style={gridStyle}>
            {packages.map((pkg, index) => {
              const isCurrent = currentSubscription?.packageName === pkg.name;
              
              return (
                <div key={pkg.id} className="pkg-card" style={cardStyle(index)}>
                  <h3 style={{ fontSize: '24px', fontWeight: 700, margin: '0 0 16px 0', background: 'linear-gradient(90deg, #fff, #a1a1aa)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                    {pkg.name}
                  </h3>
                  
                  <div style={{ fontSize: '14px', color: '#a1a1aa', marginBottom: '24px', lineHeight: 1.5 }}>
                    {pkg.description}
                  </div>

                  <div style={{ marginBottom: '32px' }}>
                    <span style={{ fontSize: '40px', fontWeight: 800, color: '#fff' }}>{(pkg.price ?? 0).toLocaleString('vi-VN')}</span>
                    <span style={{ fontSize: '24px', color: '#8b5cf6', fontWeight: 600 }}> ₫</span>
                    <div style={{ fontSize: '14px', color: '#a1a1aa', marginTop: '4px' }}>Thời hạn {pkg.durationDays ?? 0} ngày</div>
                  </div>

                  <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 40px 0', display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    {getFeatures(pkg.features).map((feature: string, i: number) => (
                      <li key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', fontSize: '15px', color: '#e4e4e7', lineHeight: 1.5 }}>
                        <span style={{ color: '#06b6d4', marginTop: '2px' }}>✦</span>
                        {feature}
                      </li>
                    ))}
                  </ul>

                  {isCurrent ? (
                    <div style={{ background: 'rgba(255,255,255,0.05)', color: '#a1a1aa', textAlign: 'center', padding: '16px', borderRadius: '12px', fontWeight: 600, marginTop: 'auto' }}>
                      Gói hiện tại
                    </div>
                  ) : (
                    <button
                      className="subscribe-btn"
                      onClick={() => handleSubscribe(pkg)}
                      disabled={subscribing}
                      style={{ opacity: subscribing ? 0.6 : 1, cursor: subscribing ? 'not-allowed' : 'pointer' }}
                    >
                      {subscribing ? 'Đang xử lý...' : 'Mua ngay'}
                    </button>
                  )}
                </div>
              );
            })}
            {packages.length === 0 && !error && (
              <p style={{ color: '#a1a1aa', textAlign: 'center', gridColumn: '1 / -1' }}>
                Hiện chưa có gói dịch vụ khả dụng.
              </p>
            )}
          </div>
        </div>

        <PaymentModal 
          isOpen={isPaymentModalOpen}
          onClose={() => setIsPaymentModalOpen(false)}
          paymentData={paymentData}
          onSuccess={fetchData}
        />
      </div>
    </>
  );
};

export default ServicePackages;
