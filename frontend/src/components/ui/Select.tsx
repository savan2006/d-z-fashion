import React from 'react';
import { FormField } from './FormField';
import { ChevronDown } from 'lucide-react';

export interface SelectOption {
  value: string | number;
  label: string;
  disabled?: boolean;
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  helperText?: string;
  options: SelectOption[];
  placeholder?: string;
  containerClassName?: string;
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  (
    {
      id,
      label,
      error,
      helperText,
      required,
      disabled,
      options,
      placeholder,
      className = '',
      containerClassName = '',
      ...rest
    },
    ref
  ) => {
    const selectId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);
    const hasError = Boolean(error);

    return (
      <FormField
        id={selectId}
        label={label}
        required={required}
        error={error}
        helperText={helperText}
        className={containerClassName}
      >
        <div className="form-input-wrapper">
          <select
            ref={ref}
            id={selectId}
            disabled={disabled}
            required={required}
            aria-invalid={hasError ? 'true' : undefined}
            className={`form-input ${hasError ? 'has-error' : ''} ${className}`}
            style={{
              paddingRight: '36px',
              appearance: 'none',
              cursor: disabled ? 'not-allowed' : 'pointer',
            }}
            {...rest}
          >
            {placeholder && (
              <option value="" disabled>
                {placeholder}
              </option>
            )}
            {options.map((opt) => (
              <option key={opt.value} value={opt.value} disabled={opt.disabled}>
                {opt.label}
              </option>
            ))}
          </select>
          <span
            style={{
              position: 'absolute',
              right: '10px',
              pointerEvents: 'none',
              color: 'var(--ink-subtle)',
              display: 'flex',
              alignItems: 'center',
            }}
            aria-hidden="true"
          >
            <ChevronDown size={16} />
          </span>
        </div>
      </FormField>
    );
  }
);

Select.displayName = 'Select';
