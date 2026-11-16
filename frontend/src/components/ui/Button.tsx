import React from 'react';
import Link from 'next/link';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'destructive';
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      disabled = false,
      startIcon,
      endIcon,
      className = '',
      type = 'button',
      ...rest
    },
    ref
  ) => {
    const classNames = `btn btn-${variant} btn-${size} ${className}`;

    return (
      <button
        ref={ref}
        type={type}
        disabled={disabled || isLoading}
        className={classNames}
        {...rest}
      >
        {isLoading && (
          <span
            style={{
              width: '14px',
              height: '14px',
              border: '2px solid currentColor',
              borderRightColor: 'transparent',
              borderRadius: '50%',
              display: 'inline-block',
              animation: 'spin 0.75s linear infinite',
            }}
            aria-hidden="true"
          />
        )}
        {!isLoading && startIcon && <span aria-hidden="true">{startIcon}</span>}
        <span>{children}</span>
        {!isLoading && endIcon && <span aria-hidden="true">{endIcon}</span>}
      </button>
    );
  }
);

Button.displayName = 'Button';

export interface LinkButtonProps extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
  href: string;
  variant?: ButtonVariant;
  size?: ButtonSize;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
  className?: string;
  children: React.ReactNode;
}

export const LinkButton: React.FC<LinkButtonProps> = ({
  href,
  children,
  variant = 'primary',
  size = 'md',
  startIcon,
  endIcon,
  className = '',
  ...rest
}) => {
  const classNames = `btn btn-${variant} btn-${size} ${className}`;

  return (
    <Link href={href} className={classNames} {...rest}>
      {startIcon && <span aria-hidden="true">{startIcon}</span>}
      <span>{children}</span>
      {endIcon && <span aria-hidden="true">{endIcon}</span>}
    </Link>
  );
};
