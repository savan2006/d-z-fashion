import React from 'react';
import { FormField } from './FormField';

export interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
  containerClassName?: string;
}

export const TextField = React.forwardRef<HTMLInputElement, TextFieldProps>(
  (
    {
      id,
      label,
      error,
      helperText,
      required,
      disabled,
      startIcon,
      endIcon,
      className = '',
      containerClassName = '',
      ...rest
    },
    ref
  ) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);
    const hasError = Boolean(error);

    return (
      <FormField
        id={inputId}
        label={label}
        required={required}
        error={error}
        helperText={helperText}
        className={containerClassName}
      >
        <div className="form-input-wrapper">
          {startIcon && (
            <span
              style={{
                position: 'absolute',
                left: '10px',
                color: 'var(--ink-subtle)',
                pointerEvents: 'none',
                display: 'flex',
                alignItems: 'center',
              }}
              aria-hidden="true"
            >
              {startIcon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            disabled={disabled}
            required={required}
            aria-invalid={hasError ? 'true' : undefined}
            aria-describedby={
              hasError && inputId
                ? `${inputId}-error`
                : helperText && inputId
                ? `${inputId}-helper`
                : undefined
            }
            className={`form-input ${hasError ? 'has-error' : ''} ${className}`}
            style={{
              paddingLeft: startIcon ? '34px' : '12px',
              paddingRight: endIcon ? '34px' : '12px',
            }}
            {...rest}
          />
          {endIcon && (
            <span
              style={{
                position: 'absolute',
                right: '10px',
                color: 'var(--ink-subtle)',
                display: 'flex',
                alignItems: 'center',
              }}
            >
              {endIcon}
            </span>
          )}
        </div>
      </FormField>
    );
  }
);

TextField.displayName = 'TextField';
