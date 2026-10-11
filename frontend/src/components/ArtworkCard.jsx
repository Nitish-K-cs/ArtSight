import { useNavigate } from 'react-router-dom';
import './ArtworkCard.css';

export default function ArtworkCard({ artwork, delay = 0, onClick }) {
  const navigate = useNavigate();

  const handleClick = () => {
    if (onClick) onClick();
    else navigate(`/artwork/${artwork.id}`);
  };

  return (
    <button
      id={`artwork-card-${artwork.id}`}
      className="artwork-card card fade-up"
      style={{ animationDelay: `${delay}s` }}
      onClick={handleClick}
    >
      {/* Image */}
      <div className="artwork-card-img">
        {artwork.imageUrl ? (
          <img src={artwork.imageUrl} alt={artwork.title} loading="lazy" />
        ) : (
          <div className="artwork-card-placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="M21 15l-5-5L5 21" />
            </svg>
          </div>
        )}
        <div className="artwork-card-gradient" />
      </div>

      {/* Info */}
      <div className="artwork-card-info">
        <p className="artwork-card-title">{artwork.title}</p>
        <p className="artwork-card-artist">{artwork.artist}</p>
        {artwork.dateCreated && (
          <p className="artwork-card-date">{artwork.dateCreated}</p>
        )}
      </div>
    </button>
  );
}
