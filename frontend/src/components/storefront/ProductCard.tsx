'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { ShoppingBag, ArrowUpRight } from 'lucide-react';
import { ProductSummary } from '@/lib/api/catalog';
import { EditorialPlaceholder } from '@/components/brand/EditorialPlaceholder';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/components/ui/Toast';

export interface ProductCardProps {
  product: ProductSummary;
  priority?: boolean;
}

export const ProductCard: React.FC<ProductCardProps> = ({ product, priority = false }) => {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { toast } = useToast();

  const handleQuickAdd = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      toast({
        type: 'info',
        title: 'Sign in required',
        message: 'Please sign in to add items to your shopping bag.',
      });
      router.push(`/login?redirect=${encodeURIComponent(`/products/${product.slug}`)}`);
      return;
    }

    // Direct user to product page to select size/color variant
    router.push(`/products/${product.slug}`);
  };

  const formattedPrice = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(product.defaultPrice);

  return (
    <article
      className="product-card group"
      style={{
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#ffffff',
        border: '1px solid var(--line)',
        borderRadius: 'var(--radius-sm)',
        overflow: 'hidden',
        transition: 'transform var(--transition-fast), box-shadow var(--transition-fast)',
      }}
    >
      {/* Image container */}
      <Link
        href={`/products/${product.slug}`}
        style={{
          position: 'relative',
          display: 'block',
          width: '100%',
          aspectRatio: '3 / 4',
          backgroundColor: 'var(--paper-warm)',
          overflow: 'hidden',
        }}
        aria-label={`View details for ${product.name}`}
      >
        {product.primaryImageUrl ? (
          <Image
            src={product.primaryImageUrl}
            alt={product.name}
            fill
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
            priority={priority}
            className="product-image"
            style={{
              objectFit: 'cover',
              transition: 'transform var(--transition-medium)',
            }}
          />
        ) : (
          <EditorialPlaceholder
            index={product.slug.toUpperCase().slice(0, 8)}
            caption={product.categoryName ? `${product.categoryName.toUpperCase()} • SURAT` : 'D&Z ATELIER'}
            minHeight="100%"
            showNotice={false}
          />
        )}

        {/* Fabric or Occasion Badge */}
        {(product.fabric || product.occasion) && (
          <div
            style={{
              position: 'absolute',
              top: '12px',
              left: '12px',
              backgroundColor: 'rgba(252, 251, 248, 0.92)',
              backdropFilter: 'blur(4px)',
              padding: '4px 8px',
              fontSize: '11px',
              fontWeight: 500,
              letterSpacing: '0.04em',
              textTransform: 'uppercase',
              color: 'var(--ink)',
              borderRadius: '2px',
              border: '1px solid var(--line)',
            }}
          >
            {product.fabric || product.occasion}
          </div>
        )}
      </Link>

      {/* Product Content Details */}
      <div
        style={{
          padding: '16px',
          display: 'flex',
          flexDirection: 'column',
          flex: 1,
          justifyContent: 'space-between',
        }}
      >
        <div>
          <p
            className="eyebrow"
            style={{
              margin: '0 0 6px',
              color: 'var(--ink-subtle)',
              fontSize: '11px',
            }}
          >
            {product.categoryName || 'Surat Atelier'}
          </p>

          <h3
            className="font-serif"
            style={{
              margin: '0 0 8px',
              fontSize: 'var(--text-heading-sm)',
              fontWeight: 400,
              lineHeight: 1.3,
            }}
          >
            <Link
              href={`/products/${product.slug}`}
              style={{
                color: 'var(--ink)',
                textDecoration: 'none',
              }}
            >
              {product.name}
            </Link>
          </h3>
        </div>

        <div style={{ marginTop: '12px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'baseline',
              gap: '6px',
              marginBottom: '4px',
            }}
          >
            <span
              style={{
                fontSize: 'var(--text-body-lg)',
                fontWeight: 600,
                color: 'var(--ink)',
                fontFamily: 'var(--font-mono)',
              }}
            >
              {formattedPrice}
            </span>
            <span
              style={{
                fontSize: '11px',
                color: 'var(--ink-subtle)',
                letterSpacing: '0.02em',
              }}
            >
              excl. GST
            </span>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingTop: '12px',
              borderTop: '1px solid var(--line)',
              marginTop: '12px',
            }}
          >
            <Link
              href={`/products/${product.slug}`}
              style={{
                fontSize: '12px',
                color: 'var(--wine)',
                fontWeight: 500,
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
              }}
            >
              <span>Select Size</span>
              <ArrowUpRight size={14} />
            </Link>

            <button
              type="button"
              onClick={handleQuickAdd}
              aria-label={`Select options for ${product.name}`}
              className="btn btn-ghost"
              style={{
                padding: '6px',
                minHeight: 'auto',
                color: 'var(--ink)',
              }}
            >
              <ShoppingBag size={18} />
            </button>
          </div>
        </div>
      </div>
    </article>
  );
};
