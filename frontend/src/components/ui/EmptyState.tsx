import React from 'react';
import { PackageOpen } from 'lucide-react';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description: string;
  action?: React.ReactNode;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  action,
  className = '',
}) => {
  return (
    <div
      className={`text-center flex flex-col items-center justify-center p-8 ${className}`}
      style={{
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 24px',
        backgroundColor: 'var(--paper-warm)',
        border: '1px dashed var(--line)',
        borderRadius: 'var(--radius-sm)',
      }}
    >
      <div
        style={{
          width: '56px',
          height: '56px',
          borderRadius: '50%',
          backgroundColor: 'var(--paper-deep)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--ink-muted)',
          marginBottom: '16px',
        }}
      >
        {icon || <PackageOpen size={28} strokeWidth={1.5} />}
      </div>
      <h4
        className="font-serif"
        style={{
          margin: '0 0 8px',
          fontSize: 'var(--text-heading-sm)',
          color: 'var(--ink)',
        }}
      >
        {title}
      </h4>
      <p
        style={{
          margin: '0 0 24px',
          maxWidth: '380px',
          fontSize: 'var(--text-body-sm)',
          color: 'var(--ink-muted)',
          lineHeight: 1.6,
        }}
      >
        {description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
};
