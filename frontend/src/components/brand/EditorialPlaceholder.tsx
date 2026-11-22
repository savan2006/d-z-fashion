import React from 'react';

export interface EditorialPlaceholderProps {
  label?: string;
  caption?: string;
  index?: string;
  minHeight?: string;
  className?: string;
  showNotice?: boolean;
}

export const EditorialPlaceholder: React.FC<EditorialPlaceholderProps> = ({
  label = 'Editorial campaign artwork',
  caption = 'A NEW PERSPECTIVE ON EVERYDAY DRESSING',
  index = 'D&Z / 001',
  minHeight = '460px',
  className = '',
  showNotice = true,
}) => {
  return (
    <div
      className={`relative w-full overflow-hidden ${className}`}
      style={{
        backgroundColor: '#fcfaf6',
        padding: '16px',
        minHeight,
        display: 'flex',
        flexDirection: 'column',
        borderRadius: '4px',
        border: '1px solid #ebd9c8',
      }}
      role="img"
      aria-label={label}
    >
      <div
        style={{
          position: 'relative',
          flex: 1,
          minHeight: '380px',
          overflow: 'hidden',
          background: 'linear-gradient(145deg, #4a0e1a 0%, #6d1a29 55%, #8b2339 100%)',
          borderRadius: '2px',
          border: '1px solid #dfc15d',
          boxShadow: 'inset 0 0 60px rgba(0,0,0,0.4)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          padding: '24px',
        }}
      >
        {/* Ornate Gold Border Filigree */}
        <div
          style={{
            position: 'absolute',
            inset: '8px',
            border: '1px solid rgba(223, 193, 93, 0.45)',
            borderStyle: 'dashed',
            pointerEvents: 'none',
          }}
        />

        {/* Top Header Row with Index Badge */}
        <div style={{ position: 'relative', zIndex: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span
            style={{
              fontSize: '11px',
              letterSpacing: '0.22em',
              fontWeight: 700,
              color: '#f5e8b1',
              backgroundColor: 'rgba(30, 6, 12, 0.6)',
              padding: '4px 10px',
              borderRadius: '2px',
              border: '1px solid rgba(223, 193, 93, 0.4)',
              textTransform: 'uppercase',
              fontFamily: "'Cinzel', Georgia, serif",
            }}
          >
            {index}
          </span>
          <span
            style={{
              fontSize: '10px',
              letterSpacing: '0.18em',
              color: '#e6cb78',
              textTransform: 'uppercase',
              fontFamily: "'Plus Jakarta Sans', sans-serif",
            }}
          >
            Surat Atelier &bull; Handloom
          </span>
        </div>

        {/* Center Zari Medallion & Royal Motif Artwork */}
        <div
          style={{
            position: 'relative',
            zIndex: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            padding: '20px 0',
          }}
        >
          {/* Circular Gold Zari Motif */}
          <div
            style={{
              width: '120px',
              height: '120px',
              borderRadius: '50%',
              border: '2px solid rgba(223, 193, 93, 0.65)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: 'radial-gradient(circle, rgba(223, 193, 93, 0.25) 0%, rgba(109, 26, 41, 0.6) 75%)',
              boxShadow: '0 0 35px rgba(223, 193, 93, 0.25)',
              marginBottom: '16px',
            }}
          >
            <svg width="68" height="68" viewBox="0 0 68 68" fill="none">
              {/* Eight-Point Royal Zari Star / Lotus Petals */}
              <circle cx="34" cy="34" r="28" stroke="#dfc15d" strokeWidth="0.8" strokeDasharray="2,2"/>
              <path d="M34 10 L37 28 L55 25 L40 34 L55 43 L37 40 L34 58 L31 40 L13 43 L28 34 L13 25 L31 28 Z" fill="#dfc15d" opacity="0.9"/>
              <circle cx="34" cy="34" r="6" fill="#fcfbf7"/>
              <circle cx="34" cy="34" r="3" fill="#6d1a29"/>
            </svg>
          </div>

          <p
            style={{
              fontFamily: "'Cormorant Garamond', Georgia, serif",
              fontSize: '26px',
              fontWeight: 600,
              fontStyle: 'italic',
              color: '#ffffff',
              margin: '0 0 6px',
              letterSpacing: '0.02em',
              textShadow: '0 2px 8px rgba(0,0,0,0.4)',
            }}
          >
            Haute Couture Surat
          </p>
          <p
            style={{
              fontSize: '11px',
              letterSpacing: '0.24em',
              color: '#f5e8b1',
              textTransform: 'uppercase',
              margin: 0,
            }}
          >
            Pure Silk &bull; Handcrafted Zari &bull; Heritage Weaves
          </p>
        </div>

        {/* Bottom Caption and Atelier Mark */}
        <div
          style={{
            position: 'relative',
            zIndex: 2,
            borderTop: '1px solid rgba(223, 193, 93, 0.3)',
            paddingTop: '12px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            gap: '12px',
          }}
        >
          <span
            style={{
              fontSize: '9px',
              letterSpacing: '0.15em',
              color: '#d4af37',
              textTransform: 'uppercase',
            }}
          >
            EST. 2026 &bull; GUJARAT, INDIA
          </span>

          <div
            style={{
              textAlign: 'right',
              color: '#fffaf4',
              fontSize: '10px',
              letterSpacing: '0.14em',
              lineHeight: 1.5,
              textTransform: 'uppercase',
              maxWidth: '260px',
              fontWeight: 500,
            }}
          >
            {caption}
          </div>
        </div>
      </div>

      {showNotice && (
        <p
          style={{
            margin: '10px 0 0',
            fontSize: '11px',
            color: 'var(--ink-subtle)',
            letterSpacing: '0.04em',
            textAlign: 'center',
          }}
        >
          <span style={{ fontWeight: 600, color: 'var(--wine)' }}>Non-product placeholder:</span>{' '}
          CSS artwork for editorial layout sizing. Replace with campaign photography before commercial launch.
        </p>
      )}
    </div>
  );
};
