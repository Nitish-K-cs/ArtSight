import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api from '../api';
import BottomNav from '../components/BottomNav';
import './Profile.css';

export default function Profile() {
  const { email, logout } = useAuth();
  const navigate = useNavigate();
  const [history, setHistory] = useState([]);
  const [stats, setStats] = useState({ scans: 0, chats: 0, total: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/users/me/history');
        const data = res.data || [];
        setHistory(data.slice(0, 20));
        const scans = data.filter(h => h.type === 'SCAN').length;
        const chats = data.filter(h => h.type === 'CHAT').length;
        setStats({ scans, chats, total: data.length });
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/auth');
  };

  const displayName = email?.split('@')[0] || 'Explorer';
  const initials = displayName.slice(0, 2).toUpperCase();

  return (
    <div className="desktop-main" style={{ minHeight: '100dvh' }}>
      <main className="page profile-page">

        {/* Avatar + name */}
        <div className="profile-hero fade-up">
          <div className="profile-avatar">{initials}</div>
          <h1 className="display-md" style={{ marginTop: 16 }}>{displayName}</h1>
          <p className="body-sm muted" style={{ marginTop: 4 }}>{email}</p>
          <span className="badge badge-gold" style={{ marginTop: 12 }}>Art Explorer</span>
        </div>

        <div className="gold-line fade-up fade-up-1" />

        {/* Stats */}
        <div className="stats-grid fade-up fade-up-1">
          <div className="stat-card">
            <p className="stat-number">{stats.scans}</p>
            <p className="label muted">Scanned</p>
          </div>
          <div className="stat-card">
            <p className="stat-number">{stats.chats}</p>
            <p className="label muted">Chats</p>
          </div>
          <div className="stat-card">
            <p className="stat-number">{stats.total}</p>
            <p className="label muted">Interactions</p>
          </div>
        </div>

        {/* History list */}
        <section className="fade-up fade-up-2" style={{ marginTop: 32 }}>
          <div className="section-header">
            <h2 className="section-title">Your Art Journey</h2>
          </div>

          {loading ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
              <div className="spinner" style={{ width: 28, height: 28 }} />
            </div>
          ) : history.length === 0 ? (
            <div className="profile-empty">
              <p className="muted body-sm">No art explored yet.</p>
              <button
                className="btn-primary"
                style={{ marginTop: 16, maxWidth: 180 }}
                onClick={() => navigate('/scan')}
              >
                Start Scanning
              </button>
            </div>
          ) : (
            <div className="profile-history-list">
              {history.map((item, i) => (
                <button
                  key={i}
                  id={`profile-history-${i}`}
                  className="profile-history-item card"
                  onClick={() => navigate(`/artwork/${item.artworkId}`)}
                >
                  <div className="profile-history-img">
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.title} />
                    ) : (
                      <div className="profile-img-placeholder">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                          <rect x="3" y="3" width="18" height="18" rx="2"/>
                          <circle cx="8.5" cy="8.5" r="1.5"/>
                          <path d="M21 15l-5-5L5 21"/>
                        </svg>
                      </div>
                    )}
                  </div>
                  <div className="profile-history-info">
                    <p className="body-sm" style={{ fontWeight: 500 }}>{item.title}</p>
                    <p className="body-sm muted">{item.artist}</p>
                    <div style={{ display: 'flex', gap: 6, marginTop: 6 }}>
                      <span className={`badge ${item.type === 'CHAT' ? 'badge-gold' : ''}`}>
                        {item.type === 'CHAT' ? '💬 Chatted' : '📷 Scanned'}
                      </span>
                    </div>
                  </div>
                  <svg style={{ width: 16, height: 16, color: 'var(--white-mute)', flexShrink: 0 }} viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <path d="M7 10h6M10 7l3 3-3 3"/>
                  </svg>
                </button>
              ))}
            </div>
          )}
        </section>

        {/* Logout */}
        <div className="fade-up fade-up-3" style={{ marginTop: 40 }}>
          <div className="gold-line" />
          <button
            id="logout-btn"
            className="btn-ghost"
            style={{ width: '100%', color: 'var(--error)', borderColor: 'rgba(224,84,84,0.3)' }}
            onClick={handleLogout}
          >
            Sign out
          </button>
          <p className="label muted" style={{ textAlign: 'center', marginTop: 20 }}>
            ArtSight · Powered by CLIP + Gemini
          </p>
        </div>
      </main>

      <BottomNav active="profile" />
    </div>
  );
}
