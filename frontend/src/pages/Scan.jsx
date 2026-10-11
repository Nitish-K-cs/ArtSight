import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import BottomNav from '../components/BottomNav';
import './Scan.css';

export default function Scan() {
  const [state, setState] = useState('idle'); // idle | uploading | result | error
  const [preview, setPreview] = useState(null);
  const [result, setResult] = useState(null);
  const [errorMsg, setErrorMsg] = useState('');
  const fileRef = useRef();
  const navigate = useNavigate();

  const handleFile = async (file) => {
    if (!file) return;

    // Show preview immediately
    const reader = new FileReader();
    reader.onload = (e) => setPreview(e.target.result);
    reader.readAsDataURL(file);

    setState('uploading');
    setErrorMsg('');

    const form = new FormData();
    form.append('file', file);

    try {
      const res = await api.post('/artworks/recognize', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      if (res.data.matched) {
        setResult(res.data);
        setState('result');
      } else {
        setErrorMsg(res.data.message || 'No artwork match found. Try a clearer photo.');
        setState('error');
      }
    } catch (err) {
      setErrorMsg('Recognition failed. Check your connection and try again.');
      setState('error');
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file?.type.startsWith('image/')) handleFile(file);
  };

  const reset = () => {
    setState('idle');
    setPreview(null);
    setResult(null);
    setErrorMsg('');
    if (fileRef.current) fileRef.current.value = '';
  };

  return (
    <div style={{ minHeight: '100dvh' }}>
      <header className="scan-header">
        <h1 className="display-md">
          <span className="gold">Scan</span> Artwork
        </h1>
        <p className="body-sm muted">Point your camera at any artwork to identify it</p>
      </header>

      <main className="page">
        {/* Upload zone */}
        {state === 'idle' && (
          <div
            className="upload-zone fade-up"
            onDrop={handleDrop}
            onDragOver={e => e.preventDefault()}
            onClick={() => fileRef.current?.click()}
            id="upload-zone"
          >
            <input
              ref={fileRef}
              type="file"
              accept="image/*"
              capture="environment"
              style={{ display: 'none' }}
              onChange={e => handleFile(e.target.files[0])}
              id="file-input"
            />
            <div className="upload-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
            </div>
            <p className="display-md" style={{ marginTop: 20 }}>Tap to Scan</p>
            <p className="body-sm muted" style={{ marginTop: 8 }}>or drag & drop an image</p>
            <div className="upload-tips">
              <span className="badge">JPG</span>
              <span className="badge">PNG</span>
              <span className="badge">WEBP</span>
            </div>
          </div>
        )}

        {/* Uploading state */}
        {state === 'uploading' && (
          <div className="scan-processing fade-up">
            {preview && (
              <div className="scan-preview-wrap">
                <img src={preview} alt="Scanning..." className="scan-preview-img" />
                <div className="scan-overlay">
                  <div className="scan-line" />
                </div>
              </div>
            )}
            <div className="scan-status">
              <div className="spinner" style={{ width: 28, height: 28 }} />
              <p className="body-sm muted" style={{ marginTop: 12 }}>Identifying artwork via CLIP + Pinecone…</p>
            </div>
          </div>
        )}

        {/* Result */}
        {state === 'result' && result && (
          <div className="scan-result fade-up">
            {preview && (
              <div className="result-img-wrap">
                <img src={preview} alt="Scanned artwork" className="result-img" />
                <div className="result-match-badge badge badge-gold">
                  ✓ Match found · {Math.round((result.score || 0) * 100)}%
                </div>
              </div>
            )}
            <div className="result-info glass" style={{ padding: 20, marginTop: 16 }}>
              <p className="label muted">Identified as</p>
              <h2 className="display-md" style={{ marginTop: 6, marginBottom: 4 }}>
                {result.artwork?.title}
              </h2>
              <p className="gold body-sm">{result.artwork?.artist}</p>
              <p className="muted body-sm" style={{ marginTop: 4 }}>
                {result.artwork?.dateCreated} · {result.artwork?.medium}
              </p>
            </div>
            <div className="result-actions">
              <button
                id="view-details-btn"
                className="btn-primary"
                onClick={() => navigate(`/artwork/${result.artwork?.id}`)}
              >
                View Full Details & Chat
              </button>
              <button className="btn-ghost" onClick={reset}>
                Scan Another
              </button>
            </div>
          </div>
        )}

        {/* Error */}
        {state === 'error' && (
          <div className="scan-error fade-up">
            {preview && (
              <img src={preview} alt="Failed scan" className="result-img" style={{ opacity: 0.5 }} />
            )}
            <div className="msg-error" style={{ marginTop: 16 }}>
              <strong>No match found</strong>
              <p style={{ marginTop: 4 }}>{errorMsg}</p>
            </div>
            <button className="btn-primary" style={{ marginTop: 20 }} onClick={reset}>
              Try Again
            </button>
          </div>
        )}
      </main>

      <BottomNav active="scan" />
    </div>
  );
}
