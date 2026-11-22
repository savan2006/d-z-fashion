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
        backgroundColor: 'var(--paper-deep)',
        padding: '16px',
        minHeight,
        display: 'flex',
        flexDirection: 'column',
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
          backgroundColor: '#d4b8ae',
          border: '1px solid var(--line)',
        }}
      >
        <span
          style={{
            position: 'absolute',
            top: '20px',
            left: '20px',
            fontSize: '10px',
            letterSpacing: '0.18em',
            fontWeight: 600,
            color: '#3d3530',
            zIndex: 2,
            textTransform: 'uppercase',
          }}
        >
          {index}
        </span>

        {/* Abstract Architectural Fabric Shapes */}
        <div
          style={{
            position: 'absolute',
            width: '44%',
            height: '80%',
            left: '22%',
            top: '14%',
            backgroundColor: '#f0e2d2',
            borderRadius: '48% 48% 10% 10%',
            transform: 'rotate(12deg)',
            opacity: 0.95,
          }}
        />
        <div
          style={{
            position: 'absolute',
            width: '38%',
            height: '64%',
            left: '38%',
            top: '24%',
            backgroundColor: 'var(--wine)',
            borderRadius: '48% 48% 10% 10%',
            transform: 'rotate(-8deg)',
            opacity: 0.88,
          }}
        />

        <div
          style={{
            position: 'absolute',
            right: '20px',
            bottom: '20px',
            textAlign: 'right',
            color: '#fffaf4',
            fontSize: '9px',
            letterSpacing: '0.16em',
            lineHeight: 1.6,
            zIndex: 2,
            textTransform: 'uppercase',
            maxWidth: '220px',
          }}
        >
          {caption}
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
