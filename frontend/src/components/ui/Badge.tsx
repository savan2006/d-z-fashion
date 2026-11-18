import React from 'react';

export type BadgeVariant =
  | 'neutral'
  | 'wine'
  | 'success'
  | 'warning'
  | 'error'
  | 'stone'
  | 'outline'
  | 'primary'
  | 'secondary'
  | 'danger';

const VARIANT_MAP: Record<BadgeVariant, string> = {
  neutral: 'neutral',
  wine: 'wine',
  success: 'success',
  warning: 'warning',
  error: 'error',
  stone: 'stone',
  outline: 'neutral',
  primary: 'wine',
  secondary: 'stone',
  danger: 'error',
};

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  size?: 'sm' | 'md' | 'lg';
  children: React.ReactNode;
  icon?: React.ReactNode;
}

export const Badge: React.FC<BadgeProps> = ({
  variant = 'neutral',
  size,
  children,
  icon,
  className = '',
  ...rest
}) => {
  const mappedVariant = VARIANT_MAP[variant] || 'neutral';
  const sizeClass = size === 'sm' ? 'badge-sm' : '';
  const combinedClass = `badge badge-${mappedVariant} ${sizeClass} ${className}`.trim();

  return (
    <span className={combinedClass} {...rest}>
      {icon && <span aria-hidden="true" style={{ display: 'inline-flex', alignItems: 'center' }}>{icon}</span>}
      <span>{children}</span>
    </span>
  );
};
