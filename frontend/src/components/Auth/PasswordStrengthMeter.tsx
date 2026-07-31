import React from 'react';

interface PasswordStrengthMeterProps {
  password: string;
}

export const PasswordStrengthMeter: React.FC<PasswordStrengthMeterProps> = ({ password }) => {
  const requirements = [
    { label: 'Ít nhất 8 ký tự', test: (p: string) => p.length >= 8 },
    { label: 'Chứa chữ cái in hoa (A-Z)', test: (p: string) => /[A-Z]/.test(p) },
    { label: 'Chứa chữ cái in thường (a-z)', test: (p: string) => /[a-z]/.test(p) },
    { label: 'Chứa ít nhất 1 chữ số (0-9)', test: (p: string) => /\d/.test(p) },
    { label: 'Chứa ký tự đặc biệt (@, $, !, %, *, ?, &, ...)', test: (p: string) => /[@$!%*?&_#^()+=.-]/.test(p) },
  ];

  const passedCount = requirements.filter((r) => r.test(password)).length;

  const getStrengthLabel = (score: number) => {
    if (password.length === 0) return { label: 'Chưa nhập', color: 'var(--text-muted, #94a3b8)', percent: 0 };
    if (score <= 2) return { label: 'Yếu', color: '#ef4444', percent: 33 };
    if (score === 3 || score === 4) return { label: 'Trung bình', color: '#f59e0b', percent: 66 };
    return { label: 'Rất mạnh', color: '#10b981', percent: 100 };
  };

  const strength = getStrengthLabel(passedCount);

  return (
    <div className="password-strength-container" style={{ marginTop: '8px', marginBottom: '16px' }}>
      <div 
        style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}
        aria-live="polite"
      >
        <span id="strength-label-text" style={{ fontSize: '12px', color: 'var(--text-secondary, #cbd5e1)', fontWeight: 500 }}>
          Độ mạnh mật khẩu: <strong style={{ color: strength.color }}>{strength.label}</strong>
        </span>
        <span style={{ fontSize: '11px', color: 'var(--text-muted, #94a3b8)' }}>
          {passedCount}/{requirements.length} quy tắc
        </span>
      </div>

      {/* Strength Bar */}
      <div
        role="progressbar"
        aria-valuenow={strength.percent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-labelledby="strength-label-text"
        style={{
          height: '6px',
          width: '100%',
          backgroundColor: 'rgba(255, 255, 255, 0.1)',
          borderRadius: '4px',
          overflow: 'hidden',
          marginBottom: '10px',
        }}
      >
        <div
          style={{
            height: '100%',
            width: `${strength.percent}%`,
            backgroundColor: strength.color,
            transition: 'width 0.3s ease, background-color 0.3s ease',
          }}
        />
      </div>

      {/* Rules Checklist */}
      <ul
        style={{
          listStyle: 'none',
          padding: 0,
          margin: 0,
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '4px 12px',
        }}
        aria-label="Danh sách yêu cầu bảo mật mật khẩu"
      >
        {requirements.map((req, idx) => {
          const isPassed = req.test(password);
          return (
            <li
              key={idx}
              style={{
                fontSize: '11px',
                color: isPassed ? '#10b981' : 'var(--text-muted, #94a3b8)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'color 0.2s ease',
              }}
            >
              <span aria-hidden="true">{isPassed ? '✓' : '○'}</span>
              <span>{req.label}</span>
              <span className="sr-only">
                {isPassed ? ': Đã đạt' : ': Chưa đạt'}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
};

export default PasswordStrengthMeter;
