import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api from '../api';
import BottomNav from '../components/BottomNav';
import ArtworkCard from '../components/ArtworkCard';
import './Home.css';

export default function Home() {
  const { email } = useAuth();
  const navigate = useNavigate();
  const [recommendations, setRecommendations] = useState([]);
  const [history, setHistory] = useState([]);
  const [personalised, setPersonalised] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const [recRes, histRes] = await Promise.all([
          api.get('/users/me/recommendations'),
          api.get('/users/me/history'),
        ]);
        setRecommendations(recRes.data.recommendations || []);
        setPersonalised(recRes.data.personalised);
        setHistory(histRes.data?.slice(0, 5) || []);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const displayName = email?.split('@')[0] || 'Explorer';

  return (
    <div className="desktop-main" style={{ minHeight: '100dvh' }}>
      {/* Header */}
      <header className="home-header">
        <div className="home-header-inner">
          <div>
            <p className="label muted">Good evening,</p>
            <h1 className="display-md home-name">{displayName}</h1>
          </div>
          <button
            id="scan-fab"
            className="scan-fab"
            onClick={() => navigate('/scan')}
            title="Scan an artwork"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
              <circle cx="12" cy="13" r="4"/>
            </svg>
          </button>
        </div>
      </header>

      <main className="page">
        {loading ? (
          <div className="home-loading">
            <div className="spinner" style={{ width: 32, height: 32 }} />
          </div>
        ) : (
          <>
            {/* Recommendations */}
            <section className="fade-up fade-up-1">
              <div className="section-header">
                <h2 className="section-title">
                  {personalised ? 'Curated for You' : 'Discover Art'}
                </h2>
                {!personalised && (
                  <span className="badge badge-gold">Popular</span>
                )}
              </div>
              {personalised && (
                <p className="body-sm muted" style={{ marginBottom: 16 }}>
                  Based on your art journey
                </p>
              )}

              <div className="recommendations-grid">
                {recommendations.map((artwork, i) => (
                  <ArtworkCard
                    key={artwork.id}
                    artwork={artwork}
                    delay={i * 0.05}
                    onClick={() => navigate(`/artwork/${artwork.id}`)}
                  />
                ))}
                {recommendations.length === 0 && (
                  <div className="empty-state">
                    <p className="muted body-sm">
                      Scan your first artwork to get personalised recommendations
                    </p>
                    <button className="btn-primary" style={{ marginTop: 16, maxWidth: 200 }} onClick={() => navigate('/scan')}>
                      Scan Now
                    </button>
                  </div>
                )}
              </div>
            </section>

            {/* History */}
            {history.length > 0 && (
              <section className="fade-up fade-up-2" style={{ marginTop: 40 }}>
                <div className="section-header">
                  <h2 className="section-title">Recently Scanned</h2>
                </div>
                <div className="history-list">
                  {history.map((item, i) => (
                    <button
                      key={i}
                      className="history-item card"
                      onClick={() => navigate(`/artwork/${item.artworkId}`)}
                      id={`history-item-${item.artworkId}`}
                    >
                      <div className="history-img">
                        {item.imageUrl ? (
                          <img src={item.imageUrl} alt={item.title} />
                        ) : (
                          <div className="history-img-placeholder">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                              <rect x="3" y="3" width="18" height="18" rx="2"/>
                              <circle cx="8.5" cy="8.5" r="1.5"/>
                              <path d="M21 15l-5-5L5 21"/>
                            </svg>
                          </div>
                        )}
                      </div>
                      <div className="history-info">
                        <p className="body-sm" style={{ fontWeight: 500 }}>{item.title}</p>
                        <p className="body-sm muted">{item.artist}</p>
                        <span className={`badge ${item.type === 'CHAT' ? 'badge-gold' : ''}`}>
                          {item.type === 'CHAT' ? '💬 Chatted' : '📷 Scanned'}
                        </span>
                      </div>
                      <svg className="history-arrow" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
                        <path d="M7 10h6M10 7l3 3-3 3"/>
                      </svg>
                    </button>
                  ))}
                </div>
              </section>
            )}
          </>
        )}
      </main>

      <BottomNav active="home" />
    </div>
  );
}
