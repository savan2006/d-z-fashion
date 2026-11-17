'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { MediaAsset } from '@/lib/api/catalog';
import { EditorialPlaceholder } from '@/components/brand/EditorialPlaceholder';

export interface ProductGalleryProps {
  media: MediaAsset[];
  productName: string;
  selectedVariantId?: string;
}

export const ProductGallery: React.FC<ProductGalleryProps> = ({
  media,
  productName,
  selectedVariantId,
}) => {
  // Sort media by displayOrder
  const sortedMedia = [...media].sort((a, b) => a.displayOrder - b.displayOrder);

  // If a variant is selected, check if there are variant-specific images
  const variantMedia = selectedVariantId
    ? sortedMedia.filter((m) => m.variantId === selectedVariantId)
    : [];

  const displayList = variantMedia.length > 0 ? variantMedia : sortedMedia;

  const [activeIndex, setActiveIndex] = useState(0);
  const [prevVariantId, setPrevVariantId] = useState(selectedVariantId);

  // Official React pattern for adjusting state when props change during render
  if (selectedVariantId !== prevVariantId) {
    setPrevVariantId(selectedVariantId);
    setActiveIndex(0);
  }

  if (displayList.length === 0) {
    return (
      <div
        style={{
          width: '100%',
          aspectRatio: '3 / 4',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
          backgroundColor: 'var(--paper-warm)',
          border: '1px solid var(--line)',
        }}
      >
        <EditorialPlaceholder
          index="D&Z / GALLERY"
          caption="SURAT ATELIER • READY STOCK PHOTOGRAPHY"
          minHeight="100%"
        />
      </div>
    );
  }

  const activeImage = displayList[activeIndex] || displayList[0];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {/* Primary Display Viewer */}
      <div
        style={{
          position: 'relative',
          width: '100%',
          aspectRatio: '3 / 4',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
          backgroundColor: 'var(--paper-warm)',
          border: '1px solid var(--line)',
        }}
      >
        <Image
          src={activeImage.secureUrl}
          alt={activeImage.altText || `${productName} view ${activeIndex + 1}`}
          fill
          priority
          sizes="(max-width: 768px) 100vw, 50vw"
          style={{ objectFit: 'cover' }}
        />

        {displayList.length > 1 && (
          <div
            style={{
              position: 'absolute',
              bottom: '12px',
              right: '12px',
              backgroundColor: 'rgba(252, 251, 248, 0.9)',
              padding: '4px 10px',
              fontSize: '11px',
              fontWeight: 500,
              fontFamily: 'var(--font-mono)',
              borderRadius: '2px',
              border: '1px solid var(--line)',
            }}
          >
            {activeIndex + 1} / {displayList.length}
          </div>
        )}
      </div>

      {/* Thumbnails Row */}
      {displayList.length > 1 && (
        <div
          style={{
            display: 'flex',
            gap: '12px',
            overflowX: 'auto',
            paddingBottom: '4px',
          }}
          aria-label="Product thumbnails"
        >
          {displayList.map((item, idx) => {
            const isSelected = idx === activeIndex;
            return (
              <button
                key={item.id || idx}
                type="button"
                onClick={() => setActiveIndex(idx)}
                aria-label={`View image ${idx + 1} of ${displayList.length}`}
                style={{
                  position: 'relative',
                  width: '72px',
                  height: '96px',
                  flexShrink: 0,
                  borderRadius: 'var(--radius-sm)',
                  overflow: 'hidden',
                  border: isSelected ? '2px solid var(--wine)' : '1px solid var(--line)',
                  cursor: 'pointer',
                  padding: 0,
                  backgroundColor: 'var(--paper-warm)',
                  opacity: isSelected ? 1 : 0.7,
                  transition: 'opacity var(--transition-fast), border-color var(--transition-fast)',
                }}
              >
                <Image
                  src={item.secureUrl}
                  alt={item.altText || `Thumbnail ${idx + 1}`}
                  fill
                  sizes="72px"
                  style={{ objectFit: 'cover' }}
                />
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};
