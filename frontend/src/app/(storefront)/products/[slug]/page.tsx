import React from 'react';
import type { Metadata } from 'next';
import { ProductDetailClient } from './ProductDetailClient';

interface Props {
  params: Promise<{ slug: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const readableTitle = slug
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

  return {
    title: `${readableTitle} | D & Z Fashion Surat`,
    description: `Discover ${readableTitle}. Tailored with precision at the Surat textile atelier with quality ready stock.`,
    alternates: {
      canonical: `/products/${slug}`,
    },
    openGraph: {
      title: `${readableTitle} | D & Z Fashion`,
      description: `Surat craftsmanship • Ready stock • Pan-India shipping.`,
      url: `/products/${slug}`,
      type: 'website',
    },
  };
}

export default async function ProductDetailPage({ params }: Props) {
  const { slug } = await params;
  return <ProductDetailClient slug={slug} />;
}
