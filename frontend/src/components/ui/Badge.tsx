import React from 'react';

export type BadgeVariant = 'neutral' | 'wine' | 'success' | 'warning' | 'error' | 'stone';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  children: React.ReactNode;
  icon?: React.ReactNode;
}

export const Badge: React.FC<BadgeProps> = ({
  variant = 'neutral',
  children,
  icon,
  className = '',
  ...rest
}) => {
  return (
    <span className={`badge badge-${variant} ${className}`} {...rest}>
      {icon && <span aria-hidden="true" style={{ display: 'inline-flex', alignItems: 'center' }}>{icon}</span>}
      <span>{children}</span>
    </span>
  );
};
