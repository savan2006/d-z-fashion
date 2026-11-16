import React from 'react';
import Image from 'next/image';
import Link from 'next/link';

export interface BrandLogoProps {
  variant?: 'wordmark' | 'monogram';
  width?: number;
  height?: number;
  asLink?: boolean;
  href?: string;
  className?: string;
  priority?: boolean;
}

export const BrandLogo: React.FC<BrandLogoProps> = ({
  variant = 'wordmark',
  width,
  height,
  asLink = true,
  href = '/',
  className = '',
  priority = false,
}) => {
  const isMonogram = variant === 'monogram';
  const src = isMonogram ? '/brand/dz-monogram.svg' : '/brand/dz-wordmark.svg';
  const defaultWidth = isMonogram ? 48 : 172;
  const defaultHeight = isMonogram ? 48 : 54;
  const finalWidth = width || defaultWidth;
  const finalHeight = height || defaultHeight;
  const alt = isMonogram ? 'D & Z Monogram' : 'D & Z Fashion';

  const imageElement = (
    <Image
      src={src}
      alt={alt}
      width={finalWidth}
      height={finalHeight}
      priority={priority}
      style={{ display: 'block', height: 'auto', maxWidth: '100%' }}
    />
  );

  if (!asLink) {
    return <div className={`inline-flex items-center ${className}`}>{imageElement}</div>;
  }

  return (
    <Link
      href={href}
      className={`inline-flex items-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-wine ${className}`}
      aria-label={alt}
    >
      {imageElement}
    </Link>
  );
};
