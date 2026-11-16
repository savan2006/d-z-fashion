import React from 'react';
import { Check } from 'lucide-react';

export interface CheckboxProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: React.ReactNode;
  description?: React.ReactNode;
  error?: string;
}

export const Checkbox = React.forwardRef<HTMLInputElement, CheckboxProps>(
  ({ id, label, description, error, disabled, className = '', checked, defaultChecked, onChange, ...rest }, ref) => {
    const inputId = id || `cb-${Math.random().toString(36).substring(2, 9)}`;

    return (
      <div style={{ marginBottom: '8px' }}>
        <label htmlFor={inputId} className={`checkbox-label ${className}`}>
          <div style={{ position: 'relative', display: 'inline-flex' }}>
            <input
              ref={ref}
              type="checkbox"
              id={inputId}
              disabled={disabled}
              checked={checked}
              defaultChecked={defaultChecked}
              onChange={onChange}
              className="sr-only"
              {...rest}
            />
            <span className="checkbox-custom">
              <Check size={12} strokeWidth={3} />
            </span>
          </div>
          <div>
            <span style={{ color: disabled ? 'var(--color-disabled)' : 'var(--ink)' }}>{label}</span>
            {description && (
              <p style={{ margin: '2px 0 0', fontSize: 'var(--text-caption)', color: 'var(--ink-subtle)' }}>
                {description}
              </p>
            )}
          </div>
        </label>
        {error && (
          <p className="form-error-text" style={{ marginLeft: '26px', marginTop: '2px' }}>
            {error}
          </p>
        )}
      </div>
    );
  }
);

Checkbox.displayName = 'Checkbox';
