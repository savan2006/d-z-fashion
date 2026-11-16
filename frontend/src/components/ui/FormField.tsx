import React from 'react';

export interface FormFieldProps {
  id?: string;
  label?: string;
  required?: boolean;
  error?: string;
  helperText?: string;
  className?: string;
  children: React.ReactNode;
}

export const FormField: React.FC<FormFieldProps> = ({
  id,
  label,
  required = false,
  error,
  helperText,
  className = '',
  children,
}) => {
  return (
    <div className={`form-field ${className}`}>
      {label && (
        <label htmlFor={id} className="form-label">
          {label}
          {required && <span className="form-label-required" aria-hidden="true">*</span>}
        </label>
      )}
      {children}
      {error ? (
        <p className="form-error-text" role="alert" id={id ? `${id}-error` : undefined}>
          {error}
        </p>
      ) : helperText ? (
        <p className="form-helper-text" id={id ? `${id}-helper` : undefined}>
          {helperText}
        </p>
      ) : null}
    </div>
  );
};
