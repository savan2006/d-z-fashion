import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { Button } from './Button';

export interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
  className?: string;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Unable to complete request',
  message,
  onRetry,
  retryLabel = 'Try again',
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
        padding: '40px 24px',
        backgroundColor: 'var(--color-error-bg)',
        border: '1px solid var(--color-error-border)',
        borderRadius: 'var(--radius-sm)',
      }}
      role="alert"
    >
      <div
        style={{
          width: '48px',
          height: '48px',
          borderRadius: '50%',
          backgroundColor: '#fee2e2',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--color-error)',
          marginBottom: '16px',
        }}
      >
        <AlertCircle size={24} />
      </div>
      <h4
        className="font-serif"
        style={{
          margin: '0 0 8px',
          fontSize: 'var(--text-heading-sm)',
          color: 'var(--color-error-text)',
        }}
      >
        {title}
      </h4>
      <p
        style={{
          margin: '0 0 20px',
          maxWidth: '420px',
          fontSize: 'var(--text-body-sm)',
          color: 'var(--color-error-text)',
          lineHeight: 1.5,
        }}
      >
        {message}
      </p>
      {onRetry && (
        <Button
          variant="outline"
          size="sm"
          onClick={onRetry}
          startIcon={<RefreshCw size={14} />}
          style={{ borderColor: 'var(--color-error)', color: 'var(--color-error)' }}
        >
          {retryLabel}
        </Button>
      )}
    </div>
  );
};
