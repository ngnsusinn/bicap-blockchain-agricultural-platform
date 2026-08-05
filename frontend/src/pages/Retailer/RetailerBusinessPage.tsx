import React, { useEffect, useState } from 'react';
import { API_BASE_URL, getAuthHeaders } from '../../utils/auth';

type BusinessType = 'RETAIL_STORE' | 'WHOLESALE' | 'SUPERMARKET' | 'OTHER';

export default function RetailerBusinessPage() {
  const [businessName, setBusinessName] = useState('');
  const [address, setAddress] = useState('');
  const [businessType, setBusinessType] = useState<BusinessType>('RETAIL_STORE');
  const [license, setLicense] = useState<File | null>(null);
  const [licenseUrl, setLicenseUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    fetch(`${API_BASE_URL}/retailer/business-profile`, { headers: getAuthHeaders() })
      .then(async (response) => {
        if (response.status === 404) return null;
        if (!response.ok) throw new Error('Không thể tải thông tin doanh nghiệp.');
        return response.json();
      })
      .then((data) => {
        if (!data) return;
        setBusinessName(data.businessName || '');
        setAddress(data.address || '');
        setBusinessType(data.businessType || 'RETAIL_STORE');
        setLicenseUrl(data.licenseUrl || '');
      })
      .catch((error) => setMessage({ type: 'error', text: error.message }))
      .finally(() => setLoading(false));
  }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setMessage(null);
    if (!businessName.trim() || !address.trim() || !license) {
      setMessage({ type: 'error', text: 'Vui lòng nhập đầy đủ thông tin và chọn giấy phép kinh doanh.' });
      return;
    }
    const allowed = ['application/pdf', 'image/jpeg', 'image/png'];
    if (!allowed.includes(license.type) || license.size > 10 * 1024 * 1024) {
      setMessage({ type: 'error', text: 'Giấy phép phải là PDF/JPG/PNG và không vượt quá 10MB.' });
      return;
    }

    const body = new FormData();
    body.append('businessName', businessName.trim());
    body.append('address', address.trim());
    body.append('businessType', businessType);
    body.append('license', license);

    setSaving(true);
    try {
      const token = localStorage.getItem('accessToken');
      const response = await fetch(`${API_BASE_URL}/retailer/documents`, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body,
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || 'Không thể cập nhật giấy phép kinh doanh.');
      setLicenseUrl(data.licenseUrl || '');
      setLicense(null);
      setMessage({ type: 'success', text: 'Cập nhật thông tin doanh nghiệp và giấy phép thành công.' });
    } catch (error) {
      setMessage({ type: 'error', text: error instanceof Error ? error.message : 'Không thể cập nhật giấy phép.' });
    } finally {
      setSaving(false);
    }
  };

  const documentUrl = licenseUrl
    ? `${API_BASE_URL.replace(/\/api$/, '')}${licenseUrl}`
    : '';

  if (loading) return <div className="retailer-panel retailer-skeleton" aria-label="Đang tải hồ sơ doanh nghiệp" />;

  return (
    <section className="retailer-panel" aria-labelledby="retailer-business-title">
      <div className="retailer-section-heading">
        <div>
          <h1 id="retailer-business-title">Hồ sơ doanh nghiệp</h1>
          <p>Thông tin cửa hàng và giấy phép kinh doanh</p>
        </div>
        {documentUrl && <a className="retailer-document-link" href={documentUrl} target="_blank" rel="noreferrer">Xem giấy phép hiện tại</a>}
      </div>

      {message && <div role="alert" className={`retailer-alert retailer-alert--${message.type}`}>{message.text}</div>}

      <form onSubmit={submit} className="retailer-form">
        <label>
          <span>Tên cửa hàng / doanh nghiệp *</span>
          <input value={businessName} onChange={(e) => setBusinessName(e.target.value)} maxLength={255} required />
        </label>
        <label>
          <span>Loại hình kinh doanh *</span>
          <select value={businessType} onChange={(e) => setBusinessType(e.target.value as BusinessType)}>
            <option value="RETAIL_STORE">Cửa hàng bán lẻ</option>
            <option value="WHOLESALE">Bán buôn</option>
            <option value="SUPERMARKET">Siêu thị</option>
            <option value="OTHER">Khác</option>
          </select>
        </label>
        <label className="retailer-form__wide">
          <span>Địa chỉ cửa hàng / doanh nghiệp *</span>
          <textarea value={address} onChange={(e) => setAddress(e.target.value)} maxLength={500} rows={3} required />
        </label>
        <label className="retailer-form__wide retailer-file">
          <span>Giấy phép kinh doanh * · PDF/JPG/PNG, tối đa 10MB</span>
          <input
            type="file"
            accept="application/pdf,image/jpeg,image/png"
            onChange={(e) => setLicense(e.target.files?.[0] || null)}
            required
          />
        </label>
        <div className="retailer-form__actions">
          <button className="btn btn-gradient" disabled={saving} type="submit">
            {saving ? 'Đang tải lên...' : 'Lưu hồ sơ doanh nghiệp'}
          </button>
        </div>
      </form>
    </section>
  );
}
