import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api from '../api';
import './Auth.css';

export default function Auth() {
  const [step, setStep] = useState('email'); // 'email' | 'otp'
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [otpSent, setOtpSent] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSendOtp = async (e) => {
    e.preventDefault();
    if (!email.trim()) return;
    setLoading(true);
    setError('');
    try {
      await api.post('/auth/send-otp', { email });
      setOtpSent(true);
      setStep('otp');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP. Try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    if (!otp.trim()) return;
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/verify-otp', { email, otp });
      login(res.data.token, email);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid OTP. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      {/* Background ambient */}
      <div className="auth-bg">
        <div className="auth-orb auth-orb-1" />
        <div className="auth-orb auth-orb-2" />
      </div>

      <div className="auth-content fade-up">
        {/* Logo */}
        <div className="auth-logo">
          <div className="auth-logo-icon">
            <svg viewBox="0 0 40 40" fill="none">
              <circle cx="20" cy="20" r="19" stroke="currentColor" strokeWidth="1.5" />
              <path d="M12 28L20 12L28 28" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M14.5 23H25.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
          </div>
          <h1 className="display-xl auth-title">ArtSight</h1>
          <p className="auth-subtitle muted">Your AI art companion</p>
        </div>

        <div className="gold-line" />

        {/* Form */}
        <div className="auth-form-wrap">
          {step === 'email' ? (
            <form onSubmit={handleSendOtp} className="fade-up fade-up-1">
              <p className="label muted" style={{ marginBottom: 12 }}>Enter your email</p>
              <input
                id="email-input"
                type="email"
                className="input-field"
                placeholder="you@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                autoFocus
                required
              />
              {error && <p className="msg-error" style={{ marginTop: 12 }}>{error}</p>}
              <button
                id="send-otp-btn"
                type="submit"
                className="btn-primary"
                style={{ marginTop: 20 }}
                disabled={loading}
              >
                {loading ? <span className="spinner" /> : 'Continue with Email'}
              </button>
            </form>
          ) : (
            <form onSubmit={handleVerifyOtp} className="fade-up">
              <div className="otp-header">
                <button
                  type="button"
                  className="otp-back"
                  onClick={() => { setStep('email'); setError(''); }}
                >
                  ← Back
                </button>
                <p className="body-sm muted">Code sent to <span className="gold">{email}</span></p>
              </div>
              <p className="label muted" style={{ marginBottom: 12, marginTop: 20 }}>Enter OTP</p>
              <input
                id="otp-input"
                type="text"
                inputMode="numeric"
                maxLength={6}
                className="input-field otp-input"
                placeholder="000000"
                value={otp}
                onChange={e => setOtp(e.target.value.replace(/\D/g, ''))}
                autoFocus
                required
              />
              {error && <p className="msg-error" style={{ marginTop: 12 }}>{error}</p>}
              <button
                id="verify-otp-btn"
                type="submit"
                className="btn-primary"
                style={{ marginTop: 20 }}
                disabled={loading}
              >
                {loading ? <span className="spinner" /> : 'Verify & Enter'}
              </button>
              <button
                type="button"
                className="btn-ghost"
                style={{ marginTop: 12 }}
                onClick={handleSendOtp}
                disabled={loading}
              >
                Resend OTP
              </button>
            </form>
          )}
        </div>

        <p className="auth-footer muted label">
          Discover art through the lens of AI
        </p>
      </div>
    </div>
  );
}
