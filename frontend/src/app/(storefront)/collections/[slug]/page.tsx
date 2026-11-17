import React from 'react';
import CollectionsPage from '../page';
import type { Metadata } from 'next';

interface Props {
  params: Promise<{ slug: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const readableName = slug
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

  return {
    title: `${readableName} | D & Z Fashion Surat`,
    description: `Explore our collection of ${readableName}. Handcrafted in Surat with high quality ready stock.`,
    alternates: {
      canonical: `/collections/${slug}`,
    },
  };
}

export default function CategorySlugPage() {
  return <CollectionsPage />;
}
