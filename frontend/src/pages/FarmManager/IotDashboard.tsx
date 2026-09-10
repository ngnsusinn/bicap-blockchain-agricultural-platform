import React, { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders, getCurrentUser } from '../../utils/auth';

interface Notification {
  id: number;
  userId: number;
  type: string;
  title: string;
  content: string;
  channel: string;
  isRead: boolean;
  createdAt: string;
}

export default function IotDashboard() {
  const [alerts, setAlerts] = useState<Notification[]>([]);
  const [currentTemp, setCurrentTemp] = useState<number>(28.5);
  const [currentHumid, setCurrentHumid] = useState<number>(65.0);
  const [currentPh, setCurrentPh] = useState<number>(6.5);
  const [isSimulating, setIsSimulating] = useState(false);
  const user = getCurrentUser();

  useEffect(() => {
    if (!user) return;

    // Fetch alerts (URGENT and PERIODIC notifications)
    const fetchAlerts = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/notifications/user/${user.id}`, {
          headers: getAuthHeaders(),
        });
        if (res.ok) {
          const data = await res.json();
          // Filter only URGENT or PERIODIC related to IoT
          const iotAlerts = (data.notifications || []).filter(
            (n: Notification) => n.type === 'URGENT' || n.type === 'PERIODIC'
          );
          setAlerts(iotAlerts);
        }
      } catch (err) {
        console.error("Failed to fetch notifications", err);
      }
    };

    fetchAlerts();

    // Listen to real-time alerts
    const sseUrl = `${API_BASE_URL}/notifications/stream/${user.id}`;
    const eventSource = new EventSource(sseUrl);

    eventSource.addEventListener('notification', (event) => {
      try {
        const newNotif = JSON.parse(event.data);
        if (newNotif.type === 'URGENT' || newNotif.type === 'PERIODIC') {
          setAlerts((prev) => [newNotif, ...prev]);
        }
      } catch (e) {
        console.error("Error parsing SSE data", e);
      }
    });

    return () => {
      eventSource.close();
    };
  }, [user]);

  const simulateData = async () => {
    if (!user || !user.farmId) {
      alert("Không tìm thấy Farm ID của bạn! Hãy đảm bảo tài khoản đã được cấp trang trại.");
      return;
    }
    setIsSimulating(true);

    // Generate random values
    // Temp: 10 - 45 (Safe: 15-40)
    const temp = Number((Math.random() * 35 + 10).toFixed(1));
    // Humid: 20 - 95 (Safe: 30-90)
    const humid = Number((Math.random() * 75 + 20).toFixed(1));
    // pH: 4.5 - 8.5 (Safe: 5.5-7.5)
    const ph = Number((Math.random() * 4 + 4.5).toFixed(1));

    setCurrentTemp(temp);
    setCurrentHumid(humid);
    setCurrentPh(ph);

    try {
      await fetch(`${API_BASE_URL}/iot/sensors`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          farmId: user.farmId,
          temperature: temp,
          humidity: humid,
          ph: ph
        })
      });
    } catch (err) {
      console.error("Failed to simulate IoT data", err);
    } finally {
      setIsSimulating(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h1 className="dashboard-title">Giám Sát Cảm Biến IoT</h1>
          <p className="dashboard-subtitle">Theo dõi Dữ liệu Nhiệt độ, Độ ẩm & pH Realtime</p>
        </div>
        <button 
          onClick={simulateData}
          disabled={isSimulating}
          style={{
            background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
            color: '#fff',
            border: 'none',
            padding: '10px 20px',
            borderRadius: '8px',
            fontWeight: 600,
            cursor: isSimulating ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
            opacity: isSimulating ? 0.7 : 1,
            transition: 'all 0.2s'
          }}
        >
          <span>🎲</span> {isSimulating ? 'Đang gửi...' : 'Mô phỏng dữ liệu IoT'}
        </button>
      </div>
      
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '24px', marginTop: '32px' }}>
        {/* Placeholder Stat Cards */}
        <div style={statCardStyle}>
          <div style={{ fontSize: '32px', marginBottom: '8px' }}>🌡️</div>
          <h3 style={{ fontSize: '16px', color: '#94a3b8', margin: '0' }}>Nhiệt độ hiện tại</h3>
          <div style={{ fontSize: '28px', color: '#fff', fontWeight: 'bold', marginTop: '8px' }}>
            {currentTemp.toFixed(1)} <span style={{ fontSize: '16px', color: '#64748b' }}>°C</span>
          </div>
          <div style={{ fontSize: '12px', color: '#10b981', marginTop: '4px' }}>Ngưỡng an toàn: 15-40°C</div>
        </div>

        <div style={statCardStyle}>
          <div style={{ fontSize: '32px', marginBottom: '8px' }}>💧</div>
          <h3 style={{ fontSize: '16px', color: '#94a3b8', margin: '0' }}>Độ ẩm hiện tại</h3>
          <div style={{ fontSize: '28px', color: '#fff', fontWeight: 'bold', marginTop: '8px' }}>
            {currentHumid.toFixed(1)} <span style={{ fontSize: '16px', color: '#64748b' }}>%</span>
          </div>
          <div style={{ fontSize: '12px', color: '#10b981', marginTop: '4px' }}>Ngưỡng an toàn: 30-90%</div>
        </div>

        <div style={statCardStyle}>
          <div style={{ fontSize: '32px', marginBottom: '8px' }}>🧪</div>
          <h3 style={{ fontSize: '16px', color: '#94a3b8', margin: '0' }}>Độ pH hiện tại</h3>
          <div style={{ fontSize: '28px', color: '#fff', fontWeight: 'bold', marginTop: '8px' }}>
            {currentPh.toFixed(1)} <span style={{ fontSize: '16px', color: '#64748b' }}>pH</span>
          </div>
          <div style={{ fontSize: '12px', color: '#10b981', marginTop: '4px' }}>Ngưỡng an toàn: 5.5-7.5 pH</div>
        </div>
      </div>

      <div className="glass-panel" style={{ padding: '32px', marginTop: '32px', borderRadius: '16px' }}>
        <h2 style={{ color: '#fff', fontSize: '20px', fontWeight: 700, marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>📋</span> Lịch sử Cảnh báo & Báo cáo
        </h2>
        
        {alerts.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#94a3b8', padding: '40px' }}>
            Chưa có cảnh báo hoặc báo cáo IoT nào.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {alerts.map((alert) => (
              <div key={alert.id} style={{ 
                background: alert.type === 'URGENT' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(255, 255, 255, 0.03)', 
                border: alert.type === 'URGENT' ? '1px solid rgba(239, 68, 68, 0.2)' : '1px solid rgba(255, 255, 255, 0.08)',
                padding: '20px', 
                borderRadius: '12px',
                display: 'flex',
                gap: '16px',
                alignItems: 'flex-start'
              }}>
                <div style={{ fontSize: '24px', marginTop: '4px' }}>
                  {alert.type === 'URGENT' ? '🚨' : '📊'}
                </div>
                <div>
                  <h4 style={{ margin: '0 0 8px 0', color: alert.type === 'URGENT' ? '#f87171' : '#38bdf8', fontSize: '16px' }}>
                    {alert.title}
                  </h4>
                  <p style={{ margin: 0, color: '#e2e8f0', fontSize: '14px', lineHeight: 1.5 }}>
                    {alert.content}
                  </p>
                  <div style={{ color: '#64748b', fontSize: '12px', marginTop: '8px' }}>
                    {new Date(alert.createdAt).toLocaleString('vi-VN')}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

const statCardStyle: React.CSSProperties = {
  background: 'rgba(255, 255, 255, 0.03)',
  border: '1px solid rgba(255, 255, 255, 0.08)',
  borderRadius: '16px',
  padding: '24px',
  boxShadow: '0 4px 20px rgba(0, 0, 0, 0.2)',
};
