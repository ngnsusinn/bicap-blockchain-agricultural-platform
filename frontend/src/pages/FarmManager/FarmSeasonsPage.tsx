import React, { useState, useEffect, useCallback } from 'react';
import { API_BASE_URL, getAuthHeaders, getCurrentUser } from '../../utils/auth';

// ── Types ─────────────────────────────────────────────────────────────────────

interface Season {
  id: number;
  description: string | null;
  farmId: number;
  name: string;
  productType: string;
  variety: string;
  area: number;
  startDate: string;
  endDate: string | null;
  notes: string | null;
  status: 'IN_PROGRESS' | 'HARVESTED' | 'CANCELLED';
  txHash: string | null;
  createdAt: string;
  processCount: number;
  processes?: Process[];
}

interface Process {
  id: number;
  seasonId: number;
  processType: string;
  name: string;
  description: string | null;
  performedBy: string | null;
  executionDate: string;
  status: 'PENDING' | 'COMPLETED' | 'CANCELLED';
  materials: string | null;
  images: string | null;
  notes: string | null;
  txHash: string | null;
  createdAt: string;
}

type View = 'list' | 'create' | 'detail';
interface Message { type: 'success' | 'error'; text: string; }

// ── Component ─────────────────────────────────────────────────────────────────

export default function FarmSeasonsPage() {
  const [farmId, setFarmId] = useState<number | null>(getCurrentUser()?.farmId ?? null);
  const [farmStatus, setFarmStatus] = useState<string>('');
  const [view, setView] = useState<View>('list');
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [selectedSeason, setSelectedSeason] = useState<Season | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<Message | null>(null);

  // Create season form
  const [sName, setSName] = useState('');
  const [sDescription, setSDescription] = useState('');
  const [sProductType, setSProductType] = useState('');
  const [sVariety, setSVariety] = useState('');
  const [sArea, setSArea] = useState('');
  const [sStartDate, setSStartDate] = useState('');
  const [sEndDate, setSEndDate] = useState('');
  const [sNotes, setSNotes] = useState('');
  const [sCreating, setSCreating] = useState(false);

  // Add process form
  const [showAddProcess, setShowAddProcess] = useState(false);
  const [pType, setPType] = useState('');
  const [pName, setPName] = useState('');
  const [pDescription, setPDescription] = useState('');
  const [pDate, setPDate] = useState('');
  const [pNotes, setPNotes] = useState('');
  const [pMaterials, setPMaterials] = useState('');
  const [pImages, setPImages] = useState('');
  const [pStatus, setPStatus] = useState<Process['status']>('COMPLETED');
  const [editingProcessId, setEditingProcessId] = useState<number | null>(null);
  const [pAdding, setPAdding] = useState(false);
  const [processMessage, setProcessMessage] = useState<Message | null>(null);

  // ── Resolve farmId ───────────────────────────────────────────────────────────

  const resolveFarmId = useCallback(async () => {
    if (farmId) return farmId;
    try {
      const res = await fetch(`${API_BASE_URL}/farms/my`, { headers: getAuthHeaders() });
      if (res.ok) {
        const farms = await res.json();
        if (Array.isArray(farms) && farms.length > 0) {
          setFarmId(farms[0].id);
          setFarmStatus(farms[0].status || '');
          return farms[0].id as number;
        }
      }
    } catch { /* non-fatal */ }
    return null;
  }, [farmId]);

  // ── Fetch seasons ────────────────────────────────────────────────────────────

  const fetchSeasons = useCallback(async (fId: number) => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/${fId}/seasons?size=50&sort=createdAt,desc`, {
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        const data = await res.json();
        const list: Season[] = data.content ?? data;
        setSeasons(list);
      }
    } catch { /* non-fatal */ }
    finally { setLoading(false); }
  }, []);

  useEffect(() => {
    resolveFarmId().then((fId) => {
      if (fId) fetchSeasons(fId);
      else setLoading(false);
    });
  }, [resolveFarmId, fetchSeasons]);

  // ── Fetch season detail ──────────────────────────────────────────────────────

  const openDetail = async (season: Season) => {
    setMessage(null);
    setProcessMessage(null);
    setShowAddProcess(false);
    if (!farmId) return;
    try {
      const res = await fetch(`${API_BASE_URL}/farms/${farmId}/seasons/${season.id}`, {
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        const detail: Season = await res.json();
        setSelectedSeason(detail);
        setView('detail');
      }
    } catch {
      setMessage({ type: 'error', text: 'Không thể tải chi tiết mùa vụ.' });
    }
  };


  // ── Create season ────────────────────────────────────────────────────────────

  const handleCreateSeason = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    if (!farmId) return;

    if (farmStatus && farmStatus !== 'APPROVED') {
      setMessage({ type: 'error', text: 'Nông trại chưa được Admin phê duyệt. Vui lòng chờ phê duyệt trước khi tạo mùa vụ.' });
      return;
    }

    setSCreating(true);
    try {
      const res = await fetch(`${API_BASE_URL}/farms/${farmId}/seasons`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          name: sName.trim(),
          description: sDescription.trim() || null,
          productType: sProductType.trim(),
          variety: sVariety.trim(),
          area: parseFloat(sArea),
          startDate: sStartDate,
          endDate: sEndDate || null,
          notes: sNotes.trim() || null,
        }),
      });

      if (res.ok) {
        const created: Season = await res.json();
        setSeasons((prev) => [created, ...prev]);
        setMessage({ type: 'success', text: `Mùa vụ "${created.name}" đã được tạo thành công và ghi lên Blockchain!` });
        setSName(''); setSDescription(''); setSProductType(''); setSVariety('');
        setSArea(''); setSStartDate(''); setSEndDate(''); setSNotes('');
        setView('list');
      } else {
        const err = await res.json().catch(() => ({}));
        setMessage({ type: 'error', text: err.message || 'Tạo mùa vụ thất bại. Vui lòng thử lại.' });
      }
    } catch {
      setMessage({ type: 'error', text: 'Lỗi kết nối. Vui lòng thử lại sau.' });
    } finally {
      setSCreating(false);
    }
  };

  // ── Add process ──────────────────────────────────────────────────────────────

  const handleAddProcess = async (e: React.FormEvent) => {
    e.preventDefault();
    setProcessMessage(null);
    if (!farmId || !selectedSeason) return;

    setPAdding(true);
    try {
      // The API stores these optional fields in JSON columns.  Keep the form
      // convenient for users while always sending valid JSON to the backend.
      const materialsJson = pMaterials.trim()
        ? JSON.stringify(pMaterials.split(',').map(item => item.trim()).filter(Boolean))
        : null;
      const imagesJson = pImages.trim()
        ? JSON.stringify(pImages.split('\n').map(url => url.trim()).filter(Boolean))
        : null;

      const isEditing = editingProcessId !== null;
      const res = await fetch(
        `${API_BASE_URL}/farms/${farmId}/seasons/${selectedSeason.id}/processes${isEditing ? `/${editingProcessId}` : ''}`,
        {
        method: isEditing ? 'PUT' : 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          processType: pType,
          name: pName.trim(),
          description: pDescription.trim() || null,
          executionDate: pDate,
          status: pStatus,
          materials: materialsJson,
          images: imagesJson,
          notes: pNotes.trim() || null,
        }),
        });

      if (res.ok) {
        const savedProcess: Process = await res.json();
        setSelectedSeason((prev) =>
          prev ? {
            ...prev,
            processes: isEditing
              ? (prev.processes || []).map(process => process.id === savedProcess.id ? savedProcess : process)
              : [...(prev.processes || []), savedProcess],
            processCount: isEditing ? prev.processCount : (prev.processCount || 0) + 1,
          } : prev
        );
        setPType(''); setPName(''); setPDescription(''); setPDate(''); setPNotes(''); setPMaterials(''); setPImages(''); setPStatus('COMPLETED');
        setEditingProcessId(null);
        setShowAddProcess(false);
        setProcessMessage({ type: 'success', text: isEditing ? 'Đã cập nhật quy trình và ghi lên Blockchain!' : 'Đã thêm quy trình thành công và ghi lên Blockchain!' });
      } else {
        const err = await res.json().catch(() => ({}));
        setProcessMessage({ type: 'error', text: err.message || 'Thêm quy trình thất bại.' });
      }
    } catch {
      setProcessMessage({ type: 'error', text: 'Lỗi kết nối. Vui lòng thử lại sau.' });
    } finally {
      setPAdding(false);
    }
  };

  // ── Helpers ──────────────────────────────────────────────────────────────────

  const formatDate = (d: string | null) => {
    if (!d) return '—';
    try { return new Date(d).toLocaleDateString('vi-VN'); } catch { return d; }
  };

  const statusBadge = (status: string) => {
    const map: Record<string, { label: string; color: string; bg: string; border: string }> = {
      IN_PROGRESS: { label: '🌱 Đang tiến hành', color: '#34d399', bg: 'rgba(52,211,153,0.1)', border: 'rgba(52,211,153,0.3)' },
      HARVESTED:   { label: '🌾 Đã thu hoạch',   color: '#fbbf24', bg: 'rgba(251,191,36,0.1)', border: 'rgba(251,191,36,0.3)' },
      CANCELLED:   { label: '❌ Đã hủy',           color: '#f87171', bg: 'rgba(248,113,113,0.1)', border: 'rgba(248,113,113,0.3)' },
    };
    const s = map[status] || { label: status, color: '#94a3b8', bg: 'rgba(148,163,184,0.1)', border: 'rgba(148,163,184,0.3)' };
    return <span style={{ fontSize: '12px', fontWeight: 600, padding: '3px 10px', borderRadius: '20px', color: s.color, background: s.bg, border: `1px solid ${s.border}` }}>{s.label}</span>;
  };

  const processTypeLabel: Record<string, string> = {
    SOIL_PREP: '🪱 Làm đất',
    SEEDING: '🌱 Gieo trồng',
    FERTILIZATION: '🧪 Bón phân',
    PEST_CONTROL: '🐛 Phòng trừ sâu bệnh',
    HARVESTING: '🌾 Thu hoạch',
    OTHER: '📋 Khác',
  };

  const processTypes = Object.keys(processTypeLabel);

  const displayMaterials = (materials: string) => {
    try {
      const values = JSON.parse(materials);
      return Array.isArray(values) ? values.join(', ') : materials;
    } catch {
      return materials;
    }
  };

  const imageUrls = (images: string | null) => {
    if (!images) return [];
    try {
      const values = JSON.parse(images);
      return Array.isArray(values) ? values.filter((value): value is string => typeof value === 'string') : [];
    } catch {
      return [];
    }
  };



  // ── Render ────────────────────────────────────────────────────────────────────

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '300px' }}>
      <div style={{ color: '#10b981', fontSize: '16px', fontWeight: 600 }}>⏳ Đang tải mùa vụ...</div>
    </div>
  );

  if (!farmId) return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <h1 className="dashboard-title" style={{ fontSize: '26px' }}>Quản Lý Mùa Vụ</h1>
      <div className="glass-panel" style={{ padding: '48px', textAlign: 'center', borderRadius: '16px' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌾</div>
        <h2 style={{ color: '#fff', fontSize: '18px', marginBottom: '8px' }}>Không thể tải dữ liệu nông trại</h2>
        <p style={{ color: '#94a3b8', fontSize: '14px', marginBottom: '20px' }}>Vui lòng tải lại để kết nối nông trại của bạn trước khi quản lý mùa vụ.</p>
        <button onClick={() => { setLoading(true); resolveFarmId().then((id) => { if (id) fetchSeasons(id); else setLoading(false); }); }} style={primaryBtnStyle}>Tải lại dữ liệu</button>
      </div>
    </div>
  );

  const MessageBar = ({ msg }: { msg: Message }) => (
    <div style={{
      padding: '14px 20px', borderRadius: '12px', marginBottom: '20px',
      fontSize: '14px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '10px',
      background: msg.type === 'success' ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)',
      border: msg.type === 'success' ? '1px solid rgba(16,185,129,0.3)' : '1px solid rgba(239,68,68,0.3)',
      color: msg.type === 'success' ? '#34d399' : '#f87171',
    }}>
      <span>{msg.type === 'success' ? '✅' : '⚠️'}</span><span>{msg.text}</span>
    </div>
  );

  // ── LIST VIEW ─────────────────────────────────────────────────────────────────
  if (view === 'list') return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 className="dashboard-title" style={{ fontSize: '26px' }}>Quản Lý Mùa Vụ</h1>
          <p className="dashboard-subtitle">Danh sách mùa vụ nông trại của bạn (BICAP-12/13/14/15).</p>
        </div>
        <button onClick={() => { setView('create'); setMessage(null); }} style={primaryBtnStyle}>
          ＋ Tạo mùa vụ mới
        </button>
      </div>

      {message && <MessageBar msg={message} />}

      {seasons.length === 0 ? (
        <div className="glass-panel" style={{ padding: '48px', textAlign: 'center', borderRadius: '16px' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌱</div>
          <h2 style={{ color: '#fff', fontSize: '18px', fontWeight: 700, marginBottom: '8px' }}>Chưa có mùa vụ nào</h2>
          <p style={{ color: '#94a3b8', fontSize: '14px', marginBottom: '24px' }}>Nhấn "Tạo mùa vụ mới" để bắt đầu ghi nhật ký sản xuất lên Blockchain.</p>
          <button onClick={() => setView('create')} style={primaryBtnStyle}>＋ Tạo mùa vụ đầu tiên</button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {seasons.map((s) => (
            <div key={s.id} className="glass-panel" style={{ padding: '20px 24px', borderRadius: '14px', cursor: 'pointer', border: '1px solid rgba(255,255,255,0.08)', transition: 'border-color 0.2s' }}
              onClick={() => openDetail(s)}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                    <span style={{ fontSize: '15px', fontWeight: 700, color: '#fff' }}>{s.name}</span>
                    {statusBadge(s.status)}
                  </div>
                  <div style={{ fontSize: '12px', color: '#94a3b8', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                    <span>Mã mùa vụ: #{s.id}</span>
                    <span>🌿 {s.productType} — {s.variety}</span>
                    <span>📐 {s.area} ha</span>
                    <span>📅 {formatDate(s.startDate)}{s.endDate ? ` → ${formatDate(s.endDate)}` : ''}</span>
                    <span>📋 {s.processCount ?? 0} quy trình</span>
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
                  {s.txHash && (
                    <span title={s.txHash} style={{ fontSize: '11px', color: '#38bdf8', background: 'rgba(56,189,248,0.08)', border: '1px solid rgba(56,189,248,0.2)', padding: '3px 8px', borderRadius: '6px' }}>
                      ⛓ BC
                    </span>
                  )}
                  <button type="button" onClick={(event) => { event.stopPropagation(); openDetail(s); }} style={{ ...secondaryBtnStyle, padding: '6px 10px', fontSize: '12px' }}>Xem chi tiết</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );

  // ── CREATE VIEW ───────────────────────────────────────────────────────────────
  if (view === 'create') return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
        <button onClick={() => { setView('list'); setMessage(null); }} style={backBtnStyle}>← Quay lại</button>
        <div>
          <h1 className="dashboard-title" style={{ fontSize: '24px' }}>Tạo Mùa Vụ Mới</h1>
          <p className="dashboard-subtitle">Thông tin sẽ được lưu vào Blockchain (BICAP-14).</p>
        </div>
      </div>

      {message && <MessageBar msg={message} />}

      <form onSubmit={handleCreateSeason}>
        <div className="glass-panel" style={{ padding: '24px', borderRadius: '16px', marginBottom: '24px' }}>
          <h2 style={sectionHeaderStyle}><span>🌾</span> Thông tin mùa vụ</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '20px' }}>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={labelStyle}>Tên mùa vụ <span style={{ color: '#ef4444' }}>*</span></label>
              <input required type="text" placeholder="VD: Vụ Hè Thu 2026 — Lúa ST25" value={sName} onChange={e => setSName(e.target.value)} style={inputStyle} />
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={labelStyle}>Mô tả</label>
              <textarea rows={2} maxLength={2000} placeholder="Mô tả mục tiêu và kế hoạch của mùa vụ" value={sDescription} onChange={e => setSDescription(e.target.value)} style={{ ...inputStyle, resize: 'vertical' }} />
            </div>
            <div>
              <label style={labelStyle}>Loại sản phẩm <span style={{ color: '#ef4444' }}>*</span></label>
              <input required type="text" placeholder="VD: Lúa, Rau cải, Bắp" value={sProductType} onChange={e => setSProductType(e.target.value)} style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>Giống cây / Vật nuôi <span style={{ color: '#ef4444' }}>*</span></label>
              <input required type="text" placeholder="VD: Lúa ST25, Rau muống địa phương" value={sVariety} onChange={e => setSVariety(e.target.value)} style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>Diện tích canh tác (ha) <span style={{ color: '#ef4444' }}>*</span></label>
              <input required type="number" min="0.01" step="0.01" placeholder="VD: 1.5" value={sArea} onChange={e => setSArea(e.target.value)} style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>Ngày bắt đầu <span style={{ color: '#ef4444' }}>*</span></label>
              <input required type="date" value={sStartDate} onChange={e => setSStartDate(e.target.value)} style={inputStyle} />
            </div>
            <div>
              <label style={labelStyle}>Ngày kết thúc dự kiến</label>
              <input type="date" value={sEndDate} min={sStartDate} onChange={e => setSEndDate(e.target.value)} style={inputStyle} />
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={labelStyle}>Ghi chú</label>
              <textarea rows={2} maxLength={2000} placeholder="Ghi chú thêm cho mùa vụ" value={sNotes} onChange={e => setSNotes(e.target.value)} style={{ ...inputStyle, resize: 'vertical' }} />
            </div>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '16px 20px', borderRadius: '12px', marginBottom: '24px', background: 'rgba(56,189,248,0.05)', border: '1px solid rgba(56,189,248,0.15)' }}>
          <p style={{ fontSize: '13px', color: '#38bdf8', margin: 0 }}>
            ⛓ Sau khi tạo, thông tin mùa vụ sẽ được ghi lên <strong>VeChainThor Blockchain</strong> để đảm bảo tính minh bạch và bất biến.
          </p>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '16px' }}>
          <button type="button" onClick={() => { setView('list'); setMessage(null); }} style={secondaryBtnStyle}>Hủy</button>
          <button type="submit" disabled={sCreating} style={primaryBtnStyle}>
            {sCreating ? '⏳ Đang tạo...' : '⛓ Tạo & Ghi Blockchain'}
          </button>
        </div>
      </form>
    </div>
  );



  // ── DETAIL VIEW ───────────────────────────────────────────────────────────────
  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px', flexWrap: 'wrap' }}>
        <button onClick={() => { setView('list'); setSelectedSeason(null); setMessage(null); }} style={backBtnStyle}>← Danh sách</button>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
            <h1 className="dashboard-title" style={{ fontSize: '22px', margin: 0 }}>{selectedSeason?.name}</h1>
            {selectedSeason && statusBadge(selectedSeason.status)}
          </div>
          <p className="dashboard-subtitle" style={{ marginTop: '4px' }}>Chi tiết mùa vụ và lịch sử quy trình (BICAP-13).</p>
        </div>
      </div>

      {message && <MessageBar msg={message} />}

      {/* Season info */}
      <div className="glass-panel" style={{ padding: '20px 24px', borderRadius: '16px', marginBottom: '24px' }}>
        <h2 style={sectionHeaderStyle}><span>📋</span> Thông tin mùa vụ</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
          {[
            { label: 'Loại sản phẩm', value: selectedSeason?.productType },
            { label: 'Giống', value: selectedSeason?.variety },
            { label: 'Diện tích', value: selectedSeason?.area ? `${selectedSeason.area} ha` : '—' },
            { label: 'Ngày bắt đầu', value: formatDate(selectedSeason?.startDate ?? null) },
            { label: 'Ngày kết thúc', value: selectedSeason?.endDate ? formatDate(selectedSeason.endDate) : 'Chưa xác định' },
            { label: 'Số quy trình', value: String(selectedSeason?.processCount ?? 0) },
          ].map(({ label, value }) => (
            <div key={label}>
              <div style={{ fontSize: '11px', color: '#64748b', marginBottom: '4px' }}>{label}</div>
              <div style={{ fontSize: '14px', color: '#fff', fontWeight: 500 }}>{value ?? '—'}</div>
            </div>
          ))}
        </div>
        {selectedSeason?.txHash && (
          <div style={{ marginTop: '16px', padding: '10px 14px', borderRadius: '8px', background: 'rgba(56,189,248,0.05)', border: '1px solid rgba(56,189,248,0.15)' }}>
            <span style={{ fontSize: '12px', color: '#64748b', marginRight: '8px' }}>⛓ Blockchain TX:</span>
            <span style={{ fontSize: '11px', color: '#38bdf8', fontFamily: 'monospace', wordBreak: 'break-all' }}>{selectedSeason.txHash}</span>
          </div>
        )}
        {selectedSeason?.description && <p style={{ color: '#cbd5e1', fontSize: '14px', lineHeight: 1.6, margin: '0 0 16px' }}>{selectedSeason.description}</p>}
        {selectedSeason?.notes && <p style={{ color: '#94a3b8', fontSize: '13px', margin: '0 0 16px' }}>📝 {selectedSeason.notes}</p>}
      </div>

      {/* Processes */}
      <div className="glass-panel" style={{ padding: '20px 24px', borderRadius: '16px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
          <h2 style={{ ...sectionHeaderStyle, margin: 0 }}><span>🗓️</span> Timeline Quy Trình (BICAP-12)</h2>
          {selectedSeason?.status === 'IN_PROGRESS' && (
            <button onClick={() => { setEditingProcessId(null); setPName(''); setPType(''); setPDescription(''); setPDate(''); setPStatus('COMPLETED'); setPNotes(''); setPMaterials(''); setPImages(''); setShowAddProcess(!showAddProcess); setProcessMessage(null); }} style={{ ...primaryBtnStyle, fontSize: '13px', padding: '8px 16px' }}>
              {showAddProcess ? '✖ Hủy' : '＋ Thêm quy trình'}
            </button>
          )}
        </div>

        {processMessage && <MessageBar msg={processMessage} />}

        {/* Add process form */}
        {showAddProcess && (
          <form onSubmit={handleAddProcess} style={{ background: 'rgba(16,185,129,0.05)', border: '1px solid rgba(16,185,129,0.15)', borderRadius: '12px', padding: '20px', marginBottom: '24px' }}>
            <h3 style={{ fontSize: '14px', fontWeight: 700, color: '#34d399', marginBottom: '16px' }}>{editingProcessId ? '✏️ Chỉnh sửa quy trình' : '➕ Thêm bước quy trình mới (BICAP-15)'}</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
              <div>
                <label style={labelStyle}>Tên quy trình <span style={{ color: '#ef4444' }}>*</span></label>
                <input required maxLength={255} placeholder="VD: Bón phân đợt 1" value={pName} onChange={e => setPName(e.target.value)} style={inputStyle} />
              </div>
              <div>
                <label style={labelStyle}>Loại hoạt động <span style={{ color: '#ef4444' }}>*</span></label>
                <select required value={pType} onChange={e => setPType(e.target.value)} style={{ ...inputStyle, cursor: 'pointer' }}>
                  <option value="">-- Chọn loại hoạt động --</option>
                  {processTypes.map(t => <option key={t} value={t}>{processTypeLabel[t]}</option>)}
                </select>
              </div>
              <div>
                <label style={labelStyle}>Ngày thực hiện <span style={{ color: '#ef4444' }}>*</span></label>
                <input required type="date" value={pDate} max={new Date().toISOString().split('T')[0]} onChange={e => setPDate(e.target.value)} style={inputStyle} />
              </div>
              <div>
                <label style={labelStyle}>Trạng thái <span style={{ color: '#ef4444' }}>*</span></label>
                <select value={pStatus} onChange={e => setPStatus(e.target.value as Process['status'])} style={{ ...inputStyle, cursor: 'pointer' }}>
                  <option value="PENDING">Chờ thực hiện</option>
                  <option value="COMPLETED">Hoàn thành</option>
                  <option value="CANCELLED">Đã hủy</option>
                </select>
              </div>
              <div>
                <label style={labelStyle}>Vật tư sử dụng</label>
                <input type="text" placeholder="VD: Ure 50kg, DAP 30kg" value={pMaterials} onChange={e => setPMaterials(e.target.value)} style={inputStyle} />
              </div>
              <div>
                <label style={labelStyle}>URL ảnh minh chứng</label>
                <input type="text" placeholder="Mỗi URL trên 1 dòng" value={pImages} onChange={e => setPImages(e.target.value)} style={inputStyle} />
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={labelStyle}>Mô tả</label>
                <textarea rows={2} maxLength={2000} placeholder="Mô tả nội dung quy trình..." value={pDescription} onChange={e => setPDescription(e.target.value)} style={{ ...inputStyle, resize: 'vertical' }} />
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={labelStyle}>Ghi chú</label>
                <textarea rows={2} placeholder="Ghi chú thêm về quy trình..." value={pNotes} onChange={e => setPNotes(e.target.value)} style={{ ...inputStyle, resize: 'vertical' }} />
              </div>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
              <button type="button" onClick={() => setShowAddProcess(false)} style={secondaryBtnStyle}>Hủy</button>
              <button type="submit" disabled={pAdding} style={primaryBtnStyle}>
                {pAdding ? '⏳ Đang lưu...' : '⛓ Thêm & Ghi Blockchain'}
              </button>
            </div>
          </form>
        )}

        {/* Process timeline */}
        {!selectedSeason?.processes || selectedSeason.processes.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px', color: '#64748b', fontSize: '14px', border: '1px dashed rgba(255,255,255,0.1)', borderRadius: '10px' }}>
            <div style={{ fontSize: '28px', marginBottom: '8px' }}>🗓️</div>
            <p>Chưa có bước quy trình nào. {selectedSeason?.status === 'IN_PROGRESS' ? 'Nhấn "Thêm quy trình" để bắt đầu ghi nhật ký.' : ''}</p>
          </div>
        ) : (
          <div style={{ position: 'relative', paddingLeft: '24px' }}>
            <div style={{ position: 'absolute', left: '11px', top: 0, bottom: 0, width: '2px', background: 'rgba(16,185,129,0.2)', borderRadius: '1px' }} />
            {selectedSeason.processes.map((p, idx) => (
              <div key={p.id} style={{ position: 'relative', marginBottom: idx === (selectedSeason.processes?.length ?? 0) - 1 ? 0 : '16px' }}>
                <div style={{ position: 'absolute', left: '-19px', top: '12px', width: '10px', height: '10px', borderRadius: '50%', background: '#10b981', border: '2px solid rgba(16,185,129,0.4)' }} />
                <div style={{ padding: '14px 18px', borderRadius: '10px', background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.07)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px', flexWrap: 'wrap', gap: '8px' }}>
                    <div>
                      <span style={{ fontSize: '14px', fontWeight: 600, color: '#fff' }}>{p.name || processTypeLabel[p.processType] || p.processType}</span>
                      <span style={{ fontSize: '11px', color: '#94a3b8', marginLeft: '8px' }}>{processTypeLabel[p.processType] ?? p.processType} · {p.status === 'COMPLETED' ? 'Hoàn thành' : p.status === 'PENDING' ? 'Chờ thực hiện' : 'Đã hủy'}</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontSize: '12px', color: '#64748b' }}>📅 {formatDate(p.executionDate)}</span>
                      {p.txHash && <span title={p.txHash} style={{ fontSize: '11px', color: '#38bdf8', background: 'rgba(56,189,248,0.08)', border: '1px solid rgba(56,189,248,0.2)', padding: '2px 6px', borderRadius: '4px' }}>⛓ BC</span>}
                      {selectedSeason?.status === 'IN_PROGRESS' && <button type="button" onClick={() => { setEditingProcessId(p.id); setPName(p.name || ''); setPType(p.processType); setPDescription(p.description || ''); setPDate(p.executionDate); setPStatus(p.status || 'COMPLETED'); setPNotes(p.notes || ''); setPMaterials(displayMaterials(p.materials || '')); setPImages(imageUrls(p.images).join('\n')); setShowAddProcess(true); setProcessMessage(null); }} style={{ ...secondaryBtnStyle, padding: '4px 9px', fontSize: '11px' }}>Chỉnh sửa</button>}
                    </div>
                  </div>
                  {p.materials && <p style={{ fontSize: '12px', color: '#94a3b8', margin: '4px 0 0 0' }}>🧪 Vật tư: {displayMaterials(p.materials)}</p>}
                  {p.description && <p style={{ fontSize: '12px', color: '#cbd5e1', margin: '4px 0 0 0' }}>{p.description}</p>}
                  {p.performedBy && <p style={{ fontSize: '12px', color: '#94a3b8', margin: '4px 0 0 0' }}>👤 Người thực hiện: {p.performedBy}</p>}
                  {p.notes && <p style={{ fontSize: '12px', color: '#94a3b8', margin: '4px 0 0 0' }}>📝 {p.notes}</p>}
                  {imageUrls(p.images).length > 0 && (
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '10px' }}>
                      {imageUrls(p.images).map((url, imageIndex) => (
                        <a key={`${p.id}-${imageIndex}`} href={url} target="_blank" rel="noreferrer">
                          <img src={url} alt={`Ảnh minh chứng ${imageIndex + 1}`} style={{ width: '72px', height: '72px', objectFit: 'cover', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.12)' }} />
                        </a>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Styles ─────────────────────────────────────────────────────────────────────

const sectionHeaderStyle: React.CSSProperties = {
  fontSize: '16px', fontWeight: 700, color: '#fff',
  marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px',
};

const labelStyle: React.CSSProperties = {
  display: 'block', fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px',
};

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '11px 16px', borderRadius: '10px',
  border: '1px solid rgba(255,255,255,0.12)', background: 'rgba(15,23,42,0.6)',
  color: '#fff', fontSize: '14px', outline: 'none',
  boxSizing: 'border-box', transition: 'border-color 0.2s ease',
};

const primaryBtnStyle: React.CSSProperties = {
  padding: '10px 24px', borderRadius: '10px', border: 'none',
  background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
  color: '#fff', fontSize: '14px', fontWeight: 700, cursor: 'pointer',
  boxShadow: '0 4px 12px rgba(16,185,129,0.35)', transition: 'all 0.2s ease',
};

const secondaryBtnStyle: React.CSSProperties = {
  padding: '10px 20px', borderRadius: '10px',
  border: '1px solid rgba(255,255,255,0.15)',
  background: 'rgba(255,255,255,0.05)', color: '#cbd5e1',
  fontSize: '14px', fontWeight: 600, cursor: 'pointer',
};

const backBtnStyle: React.CSSProperties = {
  padding: '8px 16px', borderRadius: '8px',
  border: '1px solid rgba(255,255,255,0.1)',
  background: 'rgba(255,255,255,0.04)', color: '#94a3b8',
  fontSize: '13px', fontWeight: 500, cursor: 'pointer', flexShrink: 0,
};
