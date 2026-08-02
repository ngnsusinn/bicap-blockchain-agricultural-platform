import React, { useState, useEffect, useCallback } from 'react';
import { getAuthHeaders } from '../../utils/auth';

export interface PaymentData {
  subscriptionId: number;
  paymentCode: string;
  bankName: string;
  accountNumber: string;
  amount: number;
  transferContent: string;
}

export interface PaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
  paymentData: PaymentData | null;
  onSuccess?: () => void;
}

const API_BASE_URL = 'http://localhost:8080/api';

const PaymentModal: React.FC<PaymentModalProps> = ({ isOpen, onClose, paymentData, onSuccess }) => {
  const [status, setStatus] = useState<'PENDING' | 'ACTIVE' | 'FAILED'>('PENDING');
  const [copiedField, setCopiedField] = useState<string | null>(null);

  const checkStatus = useCallback(async () => {
    if (!paymentData?.paymentCode) return;
    try {
      const res = await fetch(`${API_BASE_URL}/subscriptions/payment-status/${paymentData.paymentCode}`, {
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        const data = await res.json();
        if (data.status === 'ACTIVE' || data.status === 'FAILED') {
          setStatus(data.status);
        }
      }
    } catch (e) {
      
    }
  }, [paymentData]);

  useEffect(() => {
    if (!isOpen) {
      setStatus('PENDING');
      setCopiedField(null);
      return;
    }
    
    const interval = setInterval(checkStatus, 5000);
    return () => clearInterval(interval);
  }, [isOpen, checkStatus]);

  useEffect(() => {
    if (status === 'ACTIVE') {
      onSuccess?.();
      const timer = setTimeout(() => {
        onClose();
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [status, onClose, onSuccess]);

  const handleCopy = async (text: string, field: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedField(field);
      setTimeout(() => setCopiedField(null), 2000);
    } catch (e) {
      
    }
  };

  if (!isOpen || !paymentData) return null;

  const overlayStyle: React.CSSProperties = {
    position: 'fixed',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(10, 11, 16, 0.85)',
    backdropFilter: 'blur(12px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 9999,
    animation: 'fadeIn 0.3s ease-out forwards'
  };

  const modalStyle: React.CSSProperties = {
    background: 'linear-gradient(145deg, rgba(22, 23, 33, 0.95), rgba(15, 16, 22, 0.95))',
    border: '1px solid rgba(139, 92, 246, 0.3)',
    borderRadius: '24px',
    padding: '32px',
    width: '100%',
    maxWidth: '500px',
    boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 30px rgba(139, 92, 246, 0.15)',
    color: '#fff',
    fontFamily: "'Inter', sans-serif",
    position: 'relative',
    animation: 'slideUp 0.4s ease-out forwards'
  };

  const closeBtnStyle: React.CSSProperties = {
    position: 'absolute',
    top: '20px',
    right: '20px',
    background: 'transparent',
    border: 'none',
    color: '#a1a1aa',
    cursor: 'pointer',
    fontSize: '24px',
    transition: 'color 0.2s',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '32px',
    height: '32px',
    borderRadius: '50%',
  };

  const titleStyle: React.CSSProperties = {
    fontSize: '24px',
    fontWeight: 700,
    marginBottom: '8px',
    background: 'linear-gradient(90deg, #8b5cf6, #06b6d4)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
    textAlign: 'center'
  };

  const subtitleStyle: React.CSSProperties = {
    color: '#a1a1aa',
    fontSize: '14px',
    textAlign: 'center',
    marginBottom: '24px'
  };

  const rowStyle: React.CSSProperties = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px',
    background: 'rgba(255, 255, 255, 0.03)',
    borderRadius: '12px',
    marginBottom: '12px',
    border: '1px solid rgba(255, 255, 255, 0.05)'
  };

  const labelStyle: React.CSSProperties = {
    color: '#a1a1aa',
    fontSize: '13px',
    fontWeight: 500,
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  };

  const valueStyle: React.CSSProperties = {
    fontSize: '16px',
    fontWeight: 600,
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  };

  const copyBtnStyle: React.CSSProperties = {
    background: 'rgba(139, 92, 246, 0.1)',
    border: '1px solid rgba(139, 92, 246, 0.3)',
    color: '#8b5cf6',
    padding: '6px 12px',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '12px',
    fontWeight: 600,
    transition: 'all 0.2s',
  };

  const highlightContentStyle: React.CSSProperties = {
    background: 'linear-gradient(145deg, rgba(139, 92, 246, 0.15), rgba(6, 182, 212, 0.15))',
    border: '1px solid rgba(139, 92, 246, 0.5)',
    borderRadius: '16px',
    padding: '24px',
    textAlign: 'center',
    marginTop: '24px',
    marginBottom: '24px'
  };

  const qrContainerStyle: React.CSSProperties = {
    display: 'flex',
    justifyContent: 'center',
    background: '#fff',
    padding: '16px',
    borderRadius: '16px',
    width: 'fit-content',
    margin: '0 auto 24px auto',
    boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.5)'
  };

  const statusContainerStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '12px',
    padding: '16px',
    borderRadius: '12px',
    background: status === 'ACTIVE' 
      ? 'rgba(16, 185, 129, 0.1)' 
      : status === 'FAILED' 
        ? 'rgba(239, 68, 68, 0.1)' 
        : 'rgba(245, 158, 11, 0.1)',
    color: status === 'ACTIVE' 
      ? '#10b981' 
      : status === 'FAILED' 
        ? '#ef4444' 
        : '#f59e0b',
    fontWeight: 600,
    fontSize: '15px'
  };

  const pulseAnim = status === 'PENDING' ? 'pulse 2s infinite' : 'none';

  return (
    <>
      <style>
        {`
          @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
          }
          @keyframes slideUp {
            from { opacity: 0; transform: translateY(20px) scale(0.95); }
            to { opacity: 1; transform: translateY(0) scale(1); }
          }
          @keyframes pulse {
            0% { opacity: 1; }
            50% { opacity: 0.5; }
            100% { opacity: 1; }
          }
        `}
      </style>
      <div style={overlayStyle}>
        <div style={modalStyle}>
          <button style={closeBtnStyle} onClick={onClose} onMouseOver={e => e.currentTarget.style.color = '#fff'} onMouseOut={e => e.currentTarget.style.color = '#a1a1aa'}>×</button>
          
          <h2 style={titleStyle}>Payment Instructions</h2>
          <p style={subtitleStyle}>Quét mã QR hoặc chuyển khoản thủ công</p>

          <div style={qrContainerStyle}>
            <img 
              src={`https://img.vietqr.io/image/${paymentData.bankName}-${paymentData.accountNumber}-compact2.png?amount=${paymentData.amount}&addInfo=${encodeURIComponent(paymentData.transferContent || paymentData.paymentCode)}`} 
              alt="VietQR Code" 
              style={{ width: '220px', height: '220px', objectFit: 'contain' }}
              onError={(e) => {
                // Ẩn QR code nếu ngân hàng không hỗ trợ VietQR format này
                (e.target as HTMLImageElement).style.display = 'none';
              }}
            />
          </div>

          <div style={rowStyle}>
            <span style={labelStyle}>Bank</span>
            <span style={valueStyle}>{paymentData.bankName}</span>
          </div>

          <div style={rowStyle}>
            <span style={labelStyle}>Account Number</span>
            <div style={valueStyle}>
              {paymentData.accountNumber}
              <button 
                style={copyBtnStyle} 
                onClick={() => handleCopy(paymentData.accountNumber, 'acc')}
              >
                {copiedField === 'acc' ? 'Copied!' : 'Copy'}
              </button>
            </div>
          </div>

          <div style={rowStyle}>
            <span style={labelStyle}>Amount</span>
            <div style={valueStyle}>
              <span style={{ color: '#06b6d4' }}>
                {paymentData.amount.toLocaleString('vi-VN')} ₫
              </span>
              <button 
                style={copyBtnStyle} 
                onClick={() => handleCopy(paymentData.amount.toString(), 'amount')}
              >
                {copiedField === 'amount' ? 'Copied!' : 'Copy'}
              </button>
            </div>
          </div>

          <div style={highlightContentStyle}>
            <div style={{ ...labelStyle, marginBottom: '12px' }}>Transfer Content</div>
            <div style={{ 
              fontSize: '28px', 
              fontWeight: 800, 
              letterSpacing: '2px', 
              color: '#8b5cf6',
              marginBottom: '16px'
            }}>
              {paymentData.transferContent || paymentData.paymentCode}
            </div>
            <button 
              style={{...copyBtnStyle, width: '100%', padding: '12px', fontSize: '14px'}} 
              onClick={() => handleCopy(paymentData.transferContent || paymentData.paymentCode, 'content')}
            >
              {copiedField === 'content' ? 'Copied Successfully!' : 'Copy Transfer Content'}
            </button>
          </div>

          <div style={{...statusContainerStyle, animation: pulseAnim}}>
            {status === 'PENDING' && (
              <>
                <span style={{ fontSize: '20px' }}>⏳</span>
                Waiting for payment...
              </>
            )}
            {status === 'ACTIVE' && (
              <>
                <span style={{ fontSize: '20px' }}>✅</span>
                Payment confirmed!
              </>
            )}
            {status === 'FAILED' && (
              <>
                <span style={{ fontSize: '20px' }}>❌</span>
                Payment failed.
              </>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default PaymentModal;