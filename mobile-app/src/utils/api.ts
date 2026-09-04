import {
  UserSession,
  Shipment,
  TrackingAddRequest,
  PickupConfirmRequest,
  DeliveryConfirmRequest,
  DriverReportRequest
} from '../types';

export const API_BASE_URL = '/api';

export function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
}

export function saveSession(token: string, user: any): UserSession {
  localStorage.setItem('accessToken', token);
  const session: UserSession = {
    token,
    id: user.id || 0,
    email: user.email || '',
    fullName: user.fullName || user.email || 'Driver',
    role: user.role || 'SHIP_DRIVER',
    phone: user.phone || ''
  };
  localStorage.setItem('driverUser', JSON.stringify(session));
  return session;
}

export function getCurrentSession(): UserSession | null {
  const data = localStorage.getItem('driverUser');
  if (!data) return null;
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

export function logoutDriver(): void {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('driverUser');
}

export async function loginDriver(email: string, password: string): Promise<UserSession> {
  const payload = { identifier: email, email, password };
  let res = await fetch(`${API_BASE_URL}/auth/driver/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

  if (!res.ok) {
    res = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
  }

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    const errMsg = errorData.message || errorData.error || errorData.detail;
    throw new Error(errMsg ? `${errMsg}` : `Lỗi đăng nhập (HTTP ${res.status}). Vui lòng kiểm tra lại email/mật khẩu.`);
  }

  const data = await res.json();
  const token = data.token || data.accessToken;
  const user = data.user || data;

  if (user.role && user.role !== 'SHIP_DRIVER' && user.role !== 'ADMIN') {
    throw new Error('Tài khoản này không có quyền truy cập ứng dụng Tài xế.');
  }

  return saveSession(token, user);
}

export async function fetchMyShipments(status?: string): Promise<Shipment[]> {
  const url = status && status !== 'ALL'
    ? `${API_BASE_URL}/driver/shipments?status=${encodeURIComponent(status)}`
    : `${API_BASE_URL}/driver/shipments`;

  const res = await fetch(url, { headers: getAuthHeaders() });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Không thể lấy danh sách chuyến hàng từ hệ thống.');
  }

  return res.json();
}

export async function fetchShipmentDetail(id: number): Promise<Shipment> {
  const res = await fetch(`${API_BASE_URL}/driver/shipments/${id}`, { headers: getAuthHeaders() });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Không thể lấy chi tiết chuyến hàng.');
  }

  return res.json();
}

export async function addTrackingCheckpoint(id: number, req: TrackingAddRequest): Promise<any> {
  const res = await fetch(`${API_BASE_URL}/driver/shipments/${id}/tracking`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(req)
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Không thể cập nhật tiến độ vận chuyển.');
  }

  return res.json();
}

export async function confirmPickupGoods(id: number, req: PickupConfirmRequest): Promise<Shipment> {
  const res = await fetch(`${API_BASE_URL}/driver/shipments/${id}/pickup`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(req)
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Xác nhận nhận hàng không thành công.');
  }

  return res.json();
}

export async function confirmDeliveryGoods(id: number, req: DeliveryConfirmRequest): Promise<Shipment> {
  const res = await fetch(`${API_BASE_URL}/driver/shipments/${id}/deliver`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(req)
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Xác nhận giao hàng không thành công.');
  }

  return res.json();
}

export async function sendDriverReport(req: DriverReportRequest): Promise<any> {
  const res = await fetch(`${API_BASE_URL}/driver/reports`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(req)
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Gửi báo cáo không thành công.');
  }

  return res.json();
}

export async function traceProductByQr(hash: string): Promise<any> {
  const res = await fetch(`${API_BASE_URL}/marketplace/products/trace/${encodeURIComponent(hash)}`, {
    headers: getAuthHeaders()
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Không tìm thấy dữ liệu sản phẩm thực tế từ mã QR.');
  }

  return res.json();
}
