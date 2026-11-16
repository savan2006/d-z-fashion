import React from 'react';

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  width?: string | number;
  height?: string | number;
  variant?: 'text' | 'rect' | 'circle';
}

export const Skeleton: React.FC<SkeletonProps> = ({
  width,
  height,
  variant = 'rect',
  className = '',
  style,
  ...rest
}) => {
  const getRadius = () => {
    if (variant === 'circle') return '50%';
    if (variant === 'text') return 'var(--radius-sm)';
    return 'var(--radius-sm)';
  };

  const getDefaultHeight = () => {
    if (height) return height;
    if (variant === 'text') return '1rem';
    if (variant === 'circle') return '40px';
    return '100px';
  };

  const getDefaultWidth = () => {
    if (width) return width;
    if (variant === 'circle') return '40px';
    return '100%';
  };

  return (
    <div
      className={`skeleton-box ${className}`}
      style={{
        width: getDefaultWidth(),
        height: getDefaultHeight(),
        borderRadius: getRadius(),
        ...style,
      }}
      aria-hidden="true"
      {...rest}
    />
  );
};
