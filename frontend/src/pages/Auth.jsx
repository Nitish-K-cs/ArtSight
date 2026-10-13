import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api from '../api';
import './Auth.css';

// ─── constants ───────────────────────────────────────────────────────────────
const RESEND_SECONDS = 60;

// ─── tiny helper ─────────────────────────────────────────────────────────────
function apiError(err, fallback) {
  return err?.response?.data?.message || fallback;
}

// ─── OTP sub-form (shared by sign-up and login-otp) ──────────────────────────
function OtpStep({ email, onBack, onVerify, loading, error, setError }) {
  const [otp, setOtp] = useState('');
  const [countdown, setCountdown] = useState(RESEND_SECONDS);
  const [resending, setResending] = useState(false);
  const inputRef = useRef(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    if (countdown <= 0) return;
    const id = setTimeout(() => setCountdown(c => c - 1), 1000);
    return () => clearTimeout(id);
  }, [countdown]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onVerify(otp);
  };

  return (
    <form onSubmit={handleSubmit} className="fade-up">
      <div className="otp-header">
        <button type="button" className="otp-back" onClick={onBack}>← Back</button>
        <p className="body-sm muted">
          Code sent to <span className="gold">{email}</span>
        </p>
      </div>

      <p className="label muted" style={{ marginTop: 20, marginBottom: 12 }}>Enter 6-digit code</p>
      <input
        ref={inputRef}
        id="otp-input"
        type="text"
        inputMode="numeric"
        maxLength={6}
        autoComplete="one-time-code"
        className="input-field otp-input"
        placeholder="000000"
        value={otp}
        onChange={e => { setOtp(e.target.value.replace(/\D/g, '')); setError(''); }}
        required
      />

      <p className="body-sm muted" style={{ marginTop: 8 }}>
        If you don't see it, check your spam folder.
      </p>

      {error && <p className="msg-error" style={{ marginTop: 12 }}>{error}</p>}

      <button
        id="verify-otp-btn"
        type="submit"
        className="btn-primary"
        style={{ marginTop: 20 }}
        disabled={loading || otp.length < 6}
      >
        {loading ? <span className="spinner" /> : 'Verify code'}
      </button>

      {/* Resend */}
      <div className="auth-resend-row">
        {countdown > 0 ? (
          <p className="body-sm muted">Resend code in {countdown}s</p>
        ) : (
          <button
            type="button"
            id="resend-btn"
            className="btn-ghost"
            style={{ marginTop: 0 }}
            disabled={resending}
            onClick={async () => {
              setResending(true);
              setError('');
              try {
                await onBack({ resend: true });
              } finally {
                setResending(false);
                setCountdown(RESEND_SECONDS);
              }
            }}
          >
            {resending ? <span className="spinner" /> : 'Resend code'}
          </button>
        )}
        <button
          type="button"
          className="otp-back"
          onClick={() => onBack({ changeEmail: true })}
        >
          Use a different email
        </button>
      </div>
    </form>
  );
}

// ─── set-password step (sign-up only) ────────────────────────────────────────
function SetPasswordStep({ verificationToken, email, onSuccess, loading, setLoading, error, setError }) {
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const clientError = () => {
    if (password.length < 8) return 'Password must be at least 8 characters.';
    if (password !== confirm) return 'Passwords do not match.';
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const ce = clientError();
    if (ce) { setError(ce); return; }
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/signup/set-password', { verificationToken, password });
      login(res.data.token, email.trim().toLowerCase());
      navigate('/');
    } catch (err) {
      setError(apiError(err, 'Could not set password. Please try again.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="fade-up">
      <p className="label muted" style={{ marginBottom: 12 }}>Create your password</p>

      <input
        id="new-password-input"
        type="password"
        autoComplete="new-password"
        className="input-field"
        placeholder="Minimum 8 characters"
        value={password}
        onChange={e => { setPassword(e.target.value); setError(''); }}
        autoFocus
        required
      />

      <input
        id="confirm-password-input"
        type="password"
        autoComplete="new-password"
        className="input-field"
        placeholder="Confirm password"
        value={confirm}
        onChange={e => { setConfirm(e.target.value); setError(''); }}
        style={{ marginTop: 12 }}
        required
      />

      {error && <p className="msg-error" style={{ marginTop: 12 }}>{error}</p>}

      <button
        id="set-password-btn"
        type="submit"
        className="btn-primary"
        style={{ marginTop: 20 }}
        disabled={loading}
      >
        {loading ? <span className="spinner" /> : 'Create account'}
      </button>
    </form>
  );
}

// ─── Main Auth page ───────────────────────────────────────────────────────────
export default function Auth() {
  // mode: 'login' | 'signup'
  const [mode, setMode] = useState('login');
  // loginMethod: 'password' | 'otp'
  const [loginMethod, setLoginMethod] = useState('password');
  // step (per-mode):
  //   login-password: 'credentials'
  //   login-otp:      'email' | 'otp'
  //   signup:         'email' | 'otp' | 'setPassword'
  const [step, setStep] = useState('credentials');

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [verificationToken, setVerificationToken] = useState(''); // never leaves state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();

  // Clear error + reset step whenever mode or loginMethod changes
  const switchMode = (newMode) => {
    setMode(newMode);
    setStep(newMode === 'login' ? 'credentials' : 'email');
    setError('');
    setEmail('');
    setPassword('');
    setVerificationToken('');
  };

  const switchLoginMethod = (method) => {
    setLoginMethod(method);
    setStep(method === 'password' ? 'credentials' : 'email');
    setError('');
  };

  // ── login with password ──────────────────────────────────────────────────
  const handleLoginPassword = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/login', { email: email.trim().toLowerCase(), password });
      login(res.data.token, email.trim().toLowerCase());
      navigate('/');
    } catch (err) {
      setError(apiError(err, 'Login failed. Please try again.'));
    } finally {
      setLoading(false);
    }
  };

  // ── send OTP — shared logic for login-otp and signup ────────────────────
  const sendOtp = async (targetEmail) => {
    const endpoint = mode === 'signup' ? '/auth/signup/send-otp' : '/auth/login/send-otp';
    await api.post(endpoint, { email: targetEmail });
  };

  const handleSendOtp = async (e, opts = {}) => {
    if (e && e.preventDefault) e.preventDefault();
    const targetEmail = (email || '').trim().toLowerCase();
    if (!targetEmail) return;
    setLoading(true);
    setError('');
    try {
      await sendOtp(targetEmail);
      setStep('otp');
    } catch (err) {
      setError(apiError(err, 'Failed to send code. Try again.'));
    } finally {
      setLoading(false);
    }
  };

  // Called by OtpStep's onBack callback
  const handleOtpBack = async (opts = {}) => {
    if (opts.resend) {
      // re-send OTP to same email — throws so OtpStep can handle resend error
      await sendOtp(email.trim().toLowerCase());
      return;
    }
    // go back to email input (opts.changeEmail or plain back)
    setStep(mode === 'login' ? 'email-otp-entry' : 'email');
    // normalise: in login-otp flow 'email-otp-entry' doesn't exist, just use 'email'
    setStep('email');
    setError('');
  };

  // ── verify OTP — signup path ─────────────────────────────────────────────
  const handleSignupVerifyOtp = async (otp) => {
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/signup/verify-otp', { email: email.trim().toLowerCase(), otp });
      setVerificationToken(res.data.verificationToken);
      setStep('setPassword');
    } catch (err) {
      setError(apiError(err, 'Invalid or expired code. Request a new one.'));
    } finally {
      setLoading(false);
    }
  };

  // ── verify OTP — login path ──────────────────────────────────────────────
  const handleLoginVerifyOtp = async (otp) => {
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/login/verify-otp', { email: email.trim().toLowerCase(), otp });
      login(res.data.token, email.trim().toLowerCase());
      navigate('/');
    } catch (err) {
      setError(apiError(err, 'Invalid or expired code. Request a new one.'));
    } finally {
      setLoading(false);
    }
  };

  // ── render helpers ────────────────────────────────────────────────────────
  const renderLoginPassword = () => (
    <form onSubmit={handleLoginPassword} className="fade-up fade-up-1">
      <p className="label muted" style={{ marginBottom: 12 }}>Sign in to your account</p>

      <input
        id="email-input"
        type="email"
        className="input-field"
        placeholder="you@example.com"
        value={email}
        onChange={e => { setEmail(e.target.value); setError(''); }}
        autoFocus
        required
      />

      <input
        id="password-input"
        type="password"
        autoComplete="current-password"
        className="input-field"
        placeholder="Password"
        value={password}
        onChange={e => { setPassword(e.target.value); setError(''); }}
        style={{ marginTop: 12 }}
        required
      />

      {error && <p className="msg-error" style={{ marginTop: 12 }}>{error}</p>}

      <button
        id="login-password-btn"
        type="submit"
        className="btn-primary"
        style={{ marginTop: 20 }}
        disabled={loading}
      >
        {loading ? <span className="spinner" /> : 'Log in'}
      </button>

      <button
        type="button"
        id="switch-to-otp-btn"
        className="auth-method-toggle"
        onClick={() => switchLoginMethod('otp')}
      >
        Email me a code instead
      </button>
    </form>
  );

  const renderEmailStep = () => (
    <form onSubmit={handleSendOtp} className="fade-up fade-up-1">
      <p className="label muted" style={{ marginBottom: 12 }}>
        {mode === 'signup' ? 'Create your account' : 'Sign in with a code'}
      </p>

      <input
        id="email-input"
        type="email"
        className="input-field"
        placeholder="you@example.com"
        value={email}
        onChange={e => { setEmail(e.target.value); setError(''); }}
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
        {loading ? <span className="spinner" /> : 'Send code'}
      </button>

      {mode === 'login' && (
        <button
          type="button"
          id="switch-to-password-btn"
          className="auth-method-toggle"
          onClick={() => switchLoginMethod('password')}
        >
          Use my password instead
        </button>
      )}
    </form>
  );

  const renderStep = () => {
    // Sign-up flow
    if (mode === 'signup') {
      if (step === 'email') return renderEmailStep();
      if (step === 'otp') return (
        <OtpStep
          email={email}
          onBack={handleOtpBack}
          onVerify={handleSignupVerifyOtp}
          loading={loading}
          error={error}
          setError={setError}
        />
      );
      if (step === 'setPassword') return (
        <SetPasswordStep
          verificationToken={verificationToken}
          email={email}
          loading={loading}
          setLoading={setLoading}
          error={error}
          setError={setError}
        />
      );
    }

    // Login flow
    if (mode === 'login') {
      if (loginMethod === 'password') return renderLoginPassword();
      // loginMethod === 'otp'
      if (step === 'credentials' || step === 'email') return renderEmailStep();
      if (step === 'otp') return (
        <OtpStep
          email={email}
          onBack={handleOtpBack}
          onVerify={handleLoginVerifyOtp}
          loading={loading}
          error={error}
          setError={setError}
        />
      );
    }

    return null;
  };

  return (
    <div className="auth-page">
      {/* Ambient background */}
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

        {/* Mode tabs: Log in / Sign up */}
        <div className="auth-tabs">
          <button
            id="tab-login"
            className={`auth-tab ${mode === 'login' ? 'active' : ''}`}
            onClick={() => switchMode('login')}
          >
            Log in
          </button>
          <button
            id="tab-signup"
            className={`auth-tab ${mode === 'signup' ? 'active' : ''}`}
            onClick={() => switchMode('signup')}
          >
            Sign up
          </button>
        </div>

        {/* Form area */}
        <div className="auth-form-wrap">
          {renderStep()}
        </div>

        <p className="auth-footer muted label">
          Discover art through the lens of AI
        </p>
      </div>
    </div>
  );
}
