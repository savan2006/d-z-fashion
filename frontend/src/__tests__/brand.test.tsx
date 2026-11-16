import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { BrandLogo } from '@/components/brand/BrandLogo';
import { EditorialPlaceholder } from '@/components/brand/EditorialPlaceholder';

describe('Brand Identity Components', () => {
  it('renders original SVG wordmark with proper accessible link and alt text', () => {
    render(<BrandLogo variant="wordmark" />);
    const link = screen.getByRole('link', { name: /d & z fashion/i });
    expect(link).toBeDefined();
    expect(link.getAttribute('href')).toBe('/');

    const img = screen.getByAltText(/d & z fashion/i);
    expect(img).toBeDefined();
    expect(img.getAttribute('src')).toContain('dz-wordmark.svg');
  });

  it('renders monogram variant with correct image source', () => {
    render(<BrandLogo variant="monogram" href="/admin" />);
    const link = screen.getByRole('link', { name: /d & z monogram/i });
    expect(link.getAttribute('href')).toBe('/admin');

    const img = screen.getByAltText(/d & z monogram/i);
    expect(img.getAttribute('src')).toContain('dz-monogram.svg');
  });

  it('renders non-product editorial placeholder with explicit campaign disclosure notice', () => {
    render(
      <EditorialPlaceholder
        index="D&Z / TEST"
        caption="SEASON SPECIMEN"
        showNotice={true}
      />
    );

    expect(screen.getByText('D&Z / TEST')).toBeDefined();
    expect(screen.getByText('SEASON SPECIMEN')).toBeDefined();
    expect(screen.getByText(/non-product placeholder/i)).toBeDefined();
    expect(screen.getByText(/replace with campaign photography before commercial launch/i)).toBeDefined();
  });
});
