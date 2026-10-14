import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';
import BottomNav from '../components/BottomNav';
import './ArtworkDetail.css';

export default function ArtworkDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const messagesEndRef = useRef(null);

  const [artwork, setArtwork] = useState(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState('info'); // 'info' | 'chat'

  // Chat state
  const [sessionId, setSessionId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [starters, setStarters] = useState([]);
  const [input, setInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [sessionLoading, setSessionLoading] = useState(false);
  const [relatedArtworks, setRelatedArtworks] = useState([]);

  // Fetch artwork metadata
  useEffect(() => {
    const fetchArtwork = async () => {
      try {
        const res = await api.get(`/artworks/${id}`);
        setArtwork(res.data);
      } catch (err) {
        console.error('Failed to fetch artwork:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchArtwork();
  }, [id]);


  // Start chat session when switching to chat tab
  const startChatSession = async () => {
    if (sessionId) return;
    setSessionLoading(true);
    try {
      const res = await api.post(`/artworks/${id}/chat/session`);
      setSessionId(res.data.sessionId);
      setStarters(res.data.suggestedStarters || []);
    } catch (err) {
      console.error(err);
    } finally {
      setSessionLoading(false);
    }
  };

  const handleTabChange = (newTab) => {
    setTab(newTab);
    if (newTab === 'chat' && !sessionId) {
      startChatSession();
    }
  };

  // Send message
  const sendMessage = async (text) => {
    const msg = text || input.trim();
    if (!msg || !sessionId || chatLoading) return;

    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: msg }]);
    setChatLoading(true);
    setRelatedArtworks([]);

    try {
      const res = await api.post(`/artworks/${id}/chat/session/${sessionId}/message`, {
        message: msg,
      });
      setMessages(prev => [...prev, { role: 'assistant', content: res.data.reply }]);
      if (res.data.relatedArtworks?.length) {
        setRelatedArtworks(res.data.relatedArtworks);
      }
    } catch (err) {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Sorry, I had trouble responding. Please try again.',
        error: true,
      }]);
    } finally {
      setChatLoading(false);
    }
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, chatLoading]);

  if (loading) {
    return (
      <div className="detail-loading">
        <div className="spinner" style={{ width: 32, height: 32 }} />
      </div>
    );
  }

  return (
    <div className="detail-page desktop-main">
      {/* Back + hero */}
      <div className="detail-hero">
        <button
          className="detail-back"
          onClick={() => navigate(-1)}
          id="back-btn"
        >
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M13 4l-6 6 6 6"/>
          </svg>
        </button>
        {artwork?.imageUrl ? (
          <img src={artwork.imageUrl} alt={artwork.title} className="detail-hero-img" />
        ) : (
          <div className="detail-hero-placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <circle cx="8.5" cy="8.5" r="1.5"/>
              <path d="M21 15l-5-5L5 21"/>
            </svg>
          </div>
        )}
        <div className="detail-hero-overlay" />
        <div className="detail-hero-text">
          <h1 className="display-md">{artwork?.title || `Artwork #${id}`}</h1>
          <p className="gold body-sm">{artwork?.artist}</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="detail-tabs">
        <button
          id="tab-info"
          className={`detail-tab ${tab === 'info' ? 'active' : ''}`}
          onClick={() => handleTabChange('info')}
        >
          Details
        </button>
        <button
          id="tab-chat"
          className={`detail-tab ${tab === 'chat' ? 'active' : ''}`}
          onClick={() => handleTabChange('chat')}
        >
          💬 Ask AI
        </button>
      </div>

      {/* Info tab */}
      {tab === 'info' && (
        <div className="detail-info page fade-up">
          <div className="info-grid">
            {[
              { label: 'Artist', value: artwork?.artist },
              { label: 'Date', value: artwork?.dateCreated },
              { label: 'Medium', value: artwork?.medium },
              { label: 'Culture', value: artwork?.culture },
              { label: 'Department', value: artwork?.department },
            ].filter(i => i.value).map(item => (
              <div key={item.label} className="info-item">
                <p className="label muted">{item.label}</p>
                <p className="body-sm" style={{ marginTop: 4 }}>{item.value}</p>
              </div>
            ))}
          </div>

          {artwork?.description && (
            <>
              <div className="gold-line" />
              <div className="info-description">
                <p className="label muted" style={{ marginBottom: 10 }}>About this work</p>
                <p className="body-sm" style={{ lineHeight: 1.7, color: 'var(--white-dim)' }}>
                  {artwork.description}
                </p>
              </div>
            </>
          )}

          <button
            className="btn-primary"
            style={{ marginTop: 32 }}
            onClick={() => handleTabChange('chat')}
            id="start-chat-btn"
          >
            Ask AI about this artwork
          </button>
        </div>
      )}

      {/* Chat tab */}
      {tab === 'chat' && (
        <div className="chat-panel">
          {sessionLoading ? (
            <div className="chat-session-loading">
              <div className="spinner" style={{ width: 28, height: 28 }} />
              <p className="body-sm muted" style={{ marginTop: 12 }}>Starting your art conversation…</p>
            </div>
          ) : (
            <>
              {/* Messages */}
              <div className="chat-messages">
                {messages.length === 0 && !chatLoading && (
                  <div className="chat-welcome">
                    <p className="display-md gold" style={{ fontStyle: 'italic' }}>
                      Ask me anything
                    </p>
                    <p className="body-sm muted" style={{ marginTop: 8 }}>
                      about this artwork, the artist, or its historical context
                    </p>

                    {/* Suggested starters */}
                    {starters.length > 0 && (
                      <div className="starters-list">
                        {starters.map((s, i) => (
                          <button
                            key={i}
                            id={`starter-${i}`}
                            className="starter-btn"
                            onClick={() => sendMessage(s)}
                          >
                            {s}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {messages.map((msg, i) => (
                  <div key={i} className={`chat-bubble ${msg.role}`}>
                    {msg.role === 'assistant' && (
                      <div className="bubble-avatar">AI</div>
                    )}
                    <div className={`bubble-content ${msg.error ? 'bubble-error' : ''}`}>
                      {msg.content}
                    </div>
                  </div>
                ))}

                {chatLoading && (
                  <div className="chat-bubble assistant">
                    <div className="bubble-avatar">AI</div>
                    <div className="bubble-content bubble-thinking">
                      <span /><span /><span />
                    </div>
                  </div>
                )}

                {/* Related artworks */}
                {relatedArtworks.length > 0 && (
                  <div className="related-artworks fade-up">
                    <p className="label muted" style={{ marginBottom: 10 }}>Related works you might enjoy</p>
                    <div className="related-list">
                      {relatedArtworks.map(a => (
                        <button
                          key={a.id}
                          className="related-item card"
                          onClick={() => navigate(`/artwork/${a.id}`)}
                          id={`related-${a.id}`}
                        >
                          {a.imageUrl && (
                            <img src={a.imageUrl} alt={a.title} className="related-img" />
                          )}
                          <div className="related-info">
                            <p className="body-sm" style={{ fontWeight: 500 }}>{a.title}</p>
                            <p className="body-sm gold">{a.artist}</p>
                          </div>
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Input */}
              <div className="chat-input-bar">
                <input
                  id="chat-input"
                  type="text"
                  className="input-field chat-input"
                  placeholder="Ask about this artwork…"
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && sendMessage()}
                  disabled={chatLoading}
                />
                <button
                  id="send-btn"
                  className="chat-send"
                  onClick={() => sendMessage()}
                  disabled={!input.trim() || chatLoading}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M22 2L11 13M22 2L15 22 11 13 2 9l20-7z"/>
                  </svg>
                </button>
              </div>
            </>
          )}
        </div>
      )}

      <BottomNav active="home" />
    </div>
  );
}
