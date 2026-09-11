import { useState, useEffect, useCallback } from 'react';
import type { UserSession, SmartContract, BlockchainTransaction } from '../types';
import { API_ORIGIN, authHeaders } from '../utils/api';

interface SmartContractPageProps {
  currentSession: UserSession;
  onToast: (text: string, type?: 'info' | 'success' | 'error' | 'warning') => void;
}

type BlockchainStatus = { mode: 'live' | 'mock' | string; live: boolean; message: string };

const CONTRACT_API_URL = `${API_ORIGIN}/api/admin/contracts`;
const BLOCKCHAIN_API_URL = `${API_ORIGIN}/api/blockchain/transactions`;

const CONTRACT_STATUSES = ['PENDING', 'DEPLOYED', 'ACTIVE', 'INACTIVE', 'FAILED'] as const;
const ENVIRONMENTS = ['TESTNET', 'MAINNET'] as const;

/** Real status → existing badge classes (F6d). */
const contractStatusClass = (status: string): string => {
  switch (status) {
    case 'ACTIVE': return 'badge badge-active';
    case 'DEPLOYED': return 'badge badge-role';
    case 'PENDING': return 'badge badge-suspended';
    case 'FAILED': return 'badge badge-inactive';
    case 'INACTIVE': return 'badge badge-inactive';
    default: return 'badge badge-suspended';
  }
};

export const SmartContractPage: React.FC<SmartContractPageProps> = ({ currentSession, onToast }) => {
  const [activeTab, setActiveTab] = useState<'contracts' | 'transactions'>('contracts');
  const [contracts, setContracts] = useState<SmartContract[]>([]);
  const [transactions, setTransactions] = useState<BlockchainTransaction[]>([]);
  const [chainStatus, setChainStatus] = useState<BlockchainStatus | null>(null);
  const [statusError, setStatusError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showDeployModal, setShowDeployModal] = useState(false);
  const [editing, setEditing] = useState<SmartContract | null>(null);
  const [savingEdit, setSavingEdit] = useState(false);
  const [statusBusyId, setStatusBusyId] = useState<number | null>(null);

  // Deploy form state
  const [contractName, setContractName] = useState('TraceabilityContract');
  const [bytecode, setBytecode] = useState('');
  const [abi, setAbi] = useState('');
  const [environment, setEnvironment] = useState('TESTNET');
  const [version, setVersion] = useState('1.0.0');
  const [deploying, setDeploying] = useState(false);

  // Edit form state (F6c)
  const [editForm, setEditForm] = useState({
    name: '', environment: 'TESTNET', version: '', status: 'PENDING', address: '',
  });

  /** F6a — real runtime blockchain mode; never guess. */
  const fetchChainStatus = useCallback(async () => {
    setStatusError('');
    try {
      const res = await fetch(`${CONTRACT_API_URL}/blockchain-status`, {
        headers: authHeaders(currentSession.email),
      });
      if (!res.ok) throw new Error('Không tải được trạng thái blockchain.');
      setChainStatus(await res.json());
    } catch (err: any) {
      setChainStatus(null);
      setStatusError(err.message || 'Không tải được trạng thái blockchain.');
    }
  }, [currentSession.email]);

  // Fetch Deployed Contracts
  const fetchContracts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(CONTRACT_API_URL, { headers: authHeaders(currentSession.email) });
      if (!res.ok) throw new Error('Không thể tải danh sách smart contracts.');
      setContracts(await res.json());
    } catch (err: any) {
      onToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [currentSession.email, onToast]);

  // Fetch Blockchain Transactions
  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(BLOCKCHAIN_API_URL, { headers: authHeaders(currentSession.email) });
      if (!res.ok) throw new Error('Không thể tải nhật ký blockchain transactions.');
      setTransactions(await res.json());
    } catch (err: any) {
      onToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [currentSession.email, onToast]);

  useEffect(() => { void fetchChainStatus(); }, [fetchChainStatus]);

  useEffect(() => {
    if (activeTab === 'contracts') {
      fetchContracts();
    } else {
      fetchTransactions();
    }
  }, [activeTab, fetchContracts, fetchTransactions]);

  /** True only when the backend confirms real VeChainThor broadcast. */
  const isLive = chainStatus?.live === true;

  // Deploy Contract Handler
  const handleDeploy = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!contractName || !bytecode || !abi) {
      onToast('Vui lòng nhập đầy đủ thông tin tên contract, bytecode và ABI.', 'warning');
      return;
    }

    setDeploying(true);
    try {
      const res = await fetch(`${CONTRACT_API_URL}/deploy`, {
        method: 'POST',
        headers: { ...authHeaders(currentSession.email), 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: contractName, bytecode, abi, environment, version }),
      });

      if (!res.ok) {
        if (res.status === 403) {
          throw new Error('Chỉ SUPER_ADMIN mới có quyền triển khai Smart Contract.');
        }
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || 'Lỗi deploy Smart Contract.');
      }

      // F6b — honest toast: only claim a real on-chain deploy in live mode.
      if (isLive) {
        onToast('Triển khai Smart Contract thành công trên VeChainThor!', 'success');
      } else {
        onToast(
          'Hợp đồng đã được ghi nhận ở chế độ MÔ PHỎNG (mock) — CHƯA broadcast lên VeChainThor.',
          'warning',
        );
      }
      setShowDeployModal(false);
      setBytecode('');
      setAbi('');
      fetchContracts();
      fetchChainStatus();
    } catch (err: any) {
      onToast(err.message, 'error');
    } finally {
      setDeploying(false);
    }
  };

  const openEdit = (contract: SmartContract) => {
    setEditing(contract);
    setEditForm({
      name: contract.name ?? '',
      environment: contract.environment ?? 'TESTNET',
      version: contract.version ?? '',
      status: contract.status ?? 'PENDING',
      address: contract.address ?? '',
    });
  };

  /** F6c — update contract metadata via PUT /api/admin/contracts/{id}. */
  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editing) return;
    setSavingEdit(true);
    try {
      const res = await fetch(`${CONTRACT_API_URL}/${editing.id}`, {
        method: 'PUT',
        headers: { ...authHeaders(currentSession.email), 'Content-Type': 'application/json' },
        body: JSON.stringify(editForm),
      });
      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || 'Cập nhật contract thất bại.');
      }
      onToast('Cập nhật Smart Contract thành công.', 'success');
      setEditing(null);
      fetchContracts();
    } catch (err: any) {
      onToast(err.message, 'error');
    } finally {
      setSavingEdit(false);
    }
  };

  /** F6c — dedicated status transition via PUT /api/admin/contracts/{id}/status. */
  const handleStatusChange = async (contract: SmartContract, nextStatus: string) => {
    setStatusBusyId(contract.id);
    try {
      const res = await fetch(`${CONTRACT_API_URL}/${contract.id}/status`, {
        method: 'PUT',
        headers: { ...authHeaders(currentSession.email), 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: nextStatus }),
      });
      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || 'Đổi trạng thái contract thất bại.');
      }
      onToast(`Đã chuyển trạng thái contract sang ${nextStatus}.`, 'success');
      fetchContracts();
    } catch (err: any) {
      onToast(err.message, 'error');
    } finally {
      setStatusBusyId(null);
    }
  };

  // Retry Transaction Handler
  const handleRetry = async (txId: number) => {
    try {
      const res = await fetch(`${BLOCKCHAIN_API_URL}/${txId}/retry`, {
        method: 'POST',
        headers: authHeaders(currentSession.email),
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || 'Thao tác retry thất bại.');
      }

      const result = await res.json();
      if (result.success) {
        onToast('Gửi và xác nhận giao dịch thành công!', 'success');
      } else {
        onToast('Thử lại giao dịch thất bại.', 'warning');
      }
      fetchTransactions();
    } catch (err: any) {
      onToast(err.message, 'error');
    }
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    onToast(`Đã sao chép ${label} vào clipboard.`, 'success');
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleString('vi-VN');
  };

  const truncate = (str: string, len: number = 10) => {
    if (!str) return 'N/A';
    if (str.length <= len * 2) return str;
    return `${str.substring(0, len)}...${str.substring(str.length - len)}`;
  };

  return (
    <div className="smart-contract-page">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="dashboard-title">Quản Lý Smart Contract</h1>
          <p className="dashboard-subtitle">Triển khai, kiểm tra trạng thái và quản lý hợp đồng thông minh VeChainThor.</p>
        </div>
        <button
          onClick={() => {
            if (currentSession.role !== 'SUPER_ADMIN') {
              onToast('Chỉ SUPER_ADMIN mới có quyền triển khai Smart Contract.', 'error');
              return;
            }
            setShowDeployModal(true);
          }}
          disabled={currentSession.role !== 'SUPER_ADMIN'}
          className="btn btn-primary"
          style={{ marginTop: '8px' }}
          title={currentSession.role !== 'SUPER_ADMIN' ? 'Yêu cầu quyền SUPER_ADMIN' : 'Deploy contract mới'}
        >
          ⛓️ Triển khai Contract mới
        </button>
      </div>

      {/* F6a — real blockchain mode banner */}
      <div style={modeBannerStyle(chainStatus)} role="status">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <span style={modeBadgeStyle(chainStatus)}>
            {chainStatus ? (isLive ? '🟢 VeChainThor LIVE' : '🟠 Mô phỏng (MOCK)') : '⚪ Chưa xác định'}
          </span>
          <span style={{ fontSize: 13, color: '#cbd5e1' }}>
            {chainStatus?.message
              || statusError
              || 'Đang kiểm tra chế độ ghi nhận blockchain…'}
          </span>
        </div>
        <button
          type="button"
          onClick={() => void fetchChainStatus()}
          className="btn btn-secondary"
          style={{ padding: '6px 12px', fontSize: 12 }}
        >
          🔄 Kiểm tra lại
        </button>
      </div>

      {/* Tabs */}
      <div style={tabsContainerStyle}>
        <button
          onClick={() => setActiveTab('contracts')}
          style={{
            ...tabButtonStyle,
            borderBottom: activeTab === 'contracts' ? '3px solid #8b5cf6' : '3px solid transparent',
            color: activeTab === 'contracts' ? '#a78bfa' : 'var(--text-secondary)',
          }}
        >
          📂 Hợp đồng đã triển khai ({contracts.length})
        </button>
        <button
          onClick={() => setActiveTab('transactions')}
          style={{
            ...tabButtonStyle,
            borderBottom: activeTab === 'transactions' ? '3px solid #8b5cf6' : '3px solid transparent',
            color: activeTab === 'transactions' ? '#a78bfa' : 'var(--text-secondary)',
          }}
        >
          📜 Nhật ký giao dịch Blockchain ({transactions.length})
        </button>
      </div>

      {/* Content */}
      <div style={{ marginTop: '20px' }}>
        {loading && (
          <div style={{ textAlign: 'center', padding: '40px', color: '#cbd5e1' }}>
            <span style={{ fontSize: '20px', display: 'inline-block', animation: 'spin 1s linear infinite' }}>⏳</span> Đang tải dữ liệu...
          </div>
        )}

        {!loading && activeTab === 'contracts' && (
          <div className="glass-panel" style={{ overflowX: 'auto', padding: '0px' }}>
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Tên Contract</th>
                  <th>Phiên bản</th>
                  <th>Địa chỉ Contract</th>
                  <th>Môi trường</th>
                  <th>Trạng thái</th>
                  <th>Giao dịch Deploy (TxHash)</th>
                  <th>Thời gian tạo</th>
                  <th style={{ textAlign: 'center' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {contracts.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>
                      Chưa có Smart Contract nào được triển khai.
                    </td>
                  </tr>
                ) : (
                  contracts.map((c) => (
                    <tr key={c.id}>
                      <td style={{ fontWeight: 600, color: '#fff' }}>{c.name}</td>
                      <td>
                        <span className="badge badge-role" style={{ background: 'rgba(99,102,241,0.2)', color: '#818cf8', border: '1px solid rgba(99,102,241,0.3)' }}>
                          v{c.version}
                        </span>
                      </td>
                      <td style={{ fontFamily: 'monospace' }}>
                        {c.address ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span title={c.address}>{truncate(c.address, 8)}</span>
                            <button
                              onClick={() => copyToClipboard(c.address!, 'địa chỉ Contract')}
                              style={copyIconButtonStyle}
                              title="Sao chép địa chỉ"
                            >
                              📋
                            </button>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--text-muted)' }}>N/A</span>
                        )}
                      </td>
                      <td>{c.environment}</td>
                      <td>
                        <span className={contractStatusClass(c.status)}>{c.status}</span>
                      </td>
                      <td style={{ fontFamily: 'monospace' }}>
                        {c.txHash ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <a
                              href={`https://explore.vechain.org/transactions/${c.txHash}`}
                              target="_blank"
                              rel="noreferrer"
                              style={{ color: '#a78bfa', textDecoration: 'underline' }}
                              title="Xem trên VeChainThor Explorer"
                            >
                              {truncate(c.txHash, 6)}
                            </a>
                            <button
                              onClick={() => copyToClipboard(c.txHash!, 'transaction hash')}
                              style={copyIconButtonStyle}
                              title="Sao chép TxHash"
                            >
                              📋
                            </button>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--text-muted)' }}>N/A</span>
                        )}
                      </td>
                      <td>{formatDate(c.createdAt)}</td>
                      <td style={{ textAlign: 'center' }}>
                        <div style={{ display: 'flex', gap: 6, justifyContent: 'center', flexWrap: 'wrap' }}>
                          <button
                            onClick={() => openEdit(c)}
                            className="btn btn-secondary"
                            style={{ padding: '4px 10px', fontSize: '11px' }}
                          >
                            ✏️ Sửa
                          </button>
                          {c.status !== 'ACTIVE' ? (
                            <button
                              onClick={() => handleStatusChange(c, 'ACTIVE')}
                              disabled={statusBusyId === c.id}
                              className="btn btn-secondary"
                              style={{ padding: '4px 10px', fontSize: '11px', background: 'rgba(16,185,129,0.15)', border: '1px solid rgba(16,185,129,0.3)', color: '#6ee7b7' }}
                            >
                              ✅ Kích hoạt
                            </button>
                          ) : (
                            <button
                              onClick={() => handleStatusChange(c, 'INACTIVE')}
                              disabled={statusBusyId === c.id}
                              className="btn btn-secondary"
                              style={{ padding: '4px 10px', fontSize: '11px', background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.3)', color: '#fca5a5' }}
                            >
                              ⏸️ Vô hiệu hóa
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {!loading && activeTab === 'transactions' && (
          <div className="glass-panel" style={{ overflowX: 'auto', padding: '0px' }}>
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Loại Entity</th>
                  <th>ID Entity</th>
                  <th>Transaction Hash (TxHash)</th>
                  <th>Địa chỉ Contract</th>
                  <th>Trạng thái</th>
                  <th>Lượt thử lại</th>
                  <th>Thời gian</th>
                  <th style={{ textAlign: 'center' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {transactions.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>
                      Chưa ghi nhận giao dịch blockchain nào trong hệ thống.
                    </td>
                  </tr>
                ) : (
                  transactions.map((t) => (
                    <tr key={t.id}>
                      <td style={{ fontWeight: 600, color: '#fff' }}>{t.entityType}</td>
                      <td>#{t.entityId}</td>
                      <td style={{ fontFamily: 'monospace' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <a
                            href={`https://explore.vechain.org/transactions/${t.txHash}`}
                            target="_blank"
                            rel="noreferrer"
                            style={{ color: '#a78bfa', textDecoration: 'underline' }}
                            title="Xem trên VeChain Explorer"
                          >
                            {truncate(t.txHash, 8)}
                          </a>
                          <button
                            onClick={() => copyToClipboard(t.txHash, 'transaction hash')}
                            style={copyIconButtonStyle}
                            title="Sao chép TxHash"
                          >
                            📋
                          </button>
                        </div>
                      </td>
                      <td style={{ fontFamily: 'monospace' }}>
                        {t.contractAddress ? (
                          <span title={t.contractAddress}>{truncate(t.contractAddress, 6)}</span>
                        ) : (
                          <span style={{ color: 'var(--text-muted)' }}>N/A</span>
                        )}
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            t.status === 'CONFIRMED'
                              ? 'badge-active'
                              : t.status === 'FAILED'
                              ? 'badge-inactive'
                              : 'badge-suspended'
                          }`}
                        >
                          {t.status}
                        </span>
                      </td>
                      <td style={{ textAlign: 'center' }}>{t.retryCount}</td>
                      <td>{formatDate(t.createdAt)}</td>
                      <td style={{ textAlign: 'center' }}>
                        {t.status === 'FAILED' ? (
                          <button
                            onClick={() => handleRetry(t.id)}
                            className="btn btn-secondary"
                            style={{ padding: '4px 10px', fontSize: '11px', background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.3)', color: '#fca5a5' }}
                          >
                            🔄 Retry
                          </button>
                        ) : (
                          <span style={{ color: 'var(--text-muted)', fontSize: '12px' }}>Không cần</span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Deploy Modal */}
      {showDeployModal && (
        <div className="modal-backdrop">
          <div className="modal-content glass-panel" style={{ maxWidth: '600px', width: '90%' }}>
            <div className="modal-header">
              <h2 className="modal-title">Triển Khai Smart Contract Mới</h2>
              <button onClick={() => setShowDeployModal(false)} className="modal-close-btn">✕</button>
            </div>

            <p style={{ fontSize: 12, color: isLive ? '#6ee7b7' : '#fcd34d', marginTop: 8 }}>
              {isLive
                ? 'Chế độ LIVE: giao dịch sẽ được broadcast thật lên VeChainThor.'
                : 'Chế độ MOCK: contract chỉ được mô phỏng, không có giao dịch on-chain thật.'}
            </p>

            <form onSubmit={handleDeploy} style={{ marginTop: '16px' }}>
              <div className="form-group">
                <label className="form-label">Tên Smart Contract</label>
                <input
                  type="text"
                  className="form-input"
                  value={contractName}
                  onChange={(e) => setContractName(e.target.value)}
                  placeholder="Ví dụ: FarmingSeasonContract"
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="form-group">
                  <label className="form-label">Phiên bản</label>
                  <input
                    type="text"
                    className="form-input"
                    value={version}
                    onChange={(e) => setVersion(e.target.value)}
                    placeholder="1.0.0"
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Môi trường</label>
                  <select
                    className="form-input"
                    value={environment}
                    onChange={(e) => setEnvironment(e.target.value)}
                  >
                    {ENVIRONMENTS.map((env) => (
                      <option key={env} value={env}>{env}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">ABI (JSON format)</label>
                <textarea
                  className="form-input"
                  value={abi}
                  onChange={(e) => setAbi(e.target.value)}
                  placeholder="Nhập chuỗi ABI JSON của smart contract..."
                  rows={4}
                  required
                  style={{ fontFamily: 'monospace', fontSize: '12px' }}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Bytecode (Compiled hex code)</label>
                <textarea
                  className="form-input"
                  value={bytecode}
                  onChange={(e) => setBytecode(e.target.value)}
                  placeholder="0x608060405234801561001057600080fd5b50..."
                  rows={4}
                  required
                  style={{ fontFamily: 'monospace', fontSize: '12px' }}
                />
              </div>

              <div className="modal-actions" style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                <button type="button" onClick={() => setShowDeployModal(false)} className="btn btn-secondary" disabled={deploying}>
                  Hủy bỏ
                </button>
                <button type="submit" className="btn btn-primary" disabled={deploying}>
                  {deploying ? '⏳ Đang deploy...' : isLive ? '⛓️ Triển khai lên VeChainThor' : '🧪 Triển khai (mô phỏng)'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Modal (F6c) */}
      {editing && (
        <div className="modal-backdrop">
          <div className="modal-content glass-panel" style={{ maxWidth: '560px', width: '90%' }}>
            <div className="modal-header">
              <h2 className="modal-title">Cập Nhật Contract #{editing.id}</h2>
              <button onClick={() => setEditing(null)} className="modal-close-btn">✕</button>
            </div>

            <form onSubmit={handleSaveEdit} style={{ marginTop: '16px' }}>
              <div className="form-group">
                <label className="form-label">Tên Contract</label>
                <input
                  type="text"
                  className="form-input"
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="form-group">
                  <label className="form-label">Phiên bản</label>
                  <input
                    type="text"
                    className="form-input"
                    value={editForm.version}
                    onChange={(e) => setEditForm({ ...editForm, version: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Môi trường</label>
                  <select
                    className="form-input"
                    value={editForm.environment}
                    onChange={(e) => setEditForm({ ...editForm, environment: e.target.value })}
                  >
                    {ENVIRONMENTS.map((env) => (
                      <option key={env} value={env}>{env}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="form-group">
                  <label className="form-label">Trạng thái</label>
                  <select
                    className="form-input"
                    value={editForm.status}
                    onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}
                  >
                    {CONTRACT_STATUSES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Địa chỉ Contract</label>
                  <input
                    type="text"
                    className="form-input"
                    value={editForm.address}
                    onChange={(e) => setEditForm({ ...editForm, address: e.target.value })}
                    placeholder="0x…"
                    style={{ fontFamily: 'monospace', fontSize: 12 }}
                  />
                </div>
              </div>

              <div className="modal-actions" style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                <button type="button" onClick={() => setEditing(null)} className="btn btn-secondary" disabled={savingEdit}>
                  Hủy bỏ
                </button>
                <button type="submit" className="btn btn-primary" disabled={savingEdit}>
                  {savingEdit ? 'Đang lưu...' : '💾 Lưu thay đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

const modeBannerStyle = (status: BlockchainStatus | null): React.CSSProperties => ({
  marginTop: 20,
  padding: '14px 18px',
  borderRadius: 12,
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: 12,
  flexWrap: 'wrap',
  border: `1px solid ${status?.live === true ? 'rgba(16,185,129,0.35)' : 'rgba(245,158,11,0.35)'}`,
  background: status?.live === true ? 'rgba(16,185,129,0.08)' : 'rgba(245,158,11,0.08)',
});

const modeBadgeStyle = (status: BlockchainStatus | null): React.CSSProperties => ({
  fontSize: 12,
  fontWeight: 800,
  padding: '5px 12px',
  borderRadius: 999,
  whiteSpace: 'nowrap',
  color: status?.live === true ? '#6ee7b7' : status ? '#fcd34d' : '#cbd5e1',
  background: status?.live === true ? 'rgba(16,185,129,0.18)' : status ? 'rgba(245,158,11,0.18)' : 'rgba(148,163,184,0.15)',
  border: `1px solid ${status?.live === true ? 'rgba(16,185,129,0.4)' : status ? 'rgba(245,158,11,0.4)' : 'rgba(148,163,184,0.3)'}`,
});

const tabsContainerStyle: React.CSSProperties = {
  display: 'flex',
  borderBottom: '1px solid var(--border-color)',
  marginTop: '24px',
  gap: '16px',
};

const tabButtonStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  padding: '12px 16px',
  fontSize: '14px',
  fontWeight: 600,
  cursor: 'pointer',
  transition: 'all 0.2s ease',
  outline: 'none',
};

const copyIconButtonStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  cursor: 'pointer',
  padding: '4px',
  opacity: 0.7,
  fontSize: '12px',
  transition: 'opacity 0.2s',
  display: 'inline-flex',
  alignItems: 'center',
  outline: 'none',
};
