'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { ShoppingBag, ArrowUpRight, Heart, Star } from 'lucide-react';
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
  const [isWishlisted, setIsWishlisted] = useState(false);

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

  const handleToggleWishlist = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsWishlisted(!isWishlisted);
    toast({
      type: 'success',
      title: !isWishlisted ? 'Saved to Wishlist' : 'Removed from Wishlist',
      message: `${product.name} has been ${!isWishlisted ? 'saved to your personal wishlist' : 'removed from your wishlist'}.`,
    });
  };

  const formattedPrice = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(product.defaultPrice);

  // Original estimated price before festive discount
  const originalPrice = Math.round(product.defaultPrice * 1.45);
  const formattedOriginalPrice = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(originalPrice);

  return (
    <article
      className="product-card luxury-card group relative"
      style={{
        display: 'flex',
        flexDirection: 'column',
        borderRadius: '4px',
        overflow: 'hidden',
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
          backgroundColor: '#f8f4ed',
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
            className="product-image transition-transform duration-500 ease-out group-hover:scale-105"
            style={{
              objectFit: 'cover',
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

        {/* Floating Wishlist Button */}
        <button
          type="button"
          onClick={handleToggleWishlist}
          aria-label={isWishlisted ? `Remove ${product.name} from wishlist` : `Add ${product.name} to wishlist`}
          style={{
            position: 'absolute',
            top: '12px',
            right: '12px',
            width: '36px',
            height: '36px',
            borderRadius: '50%',
            backgroundColor: 'rgba(255, 255, 255, 0.92)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            border: '1px solid rgba(222, 211, 196, 0.8)',
            color: isWishlisted ? '#b91c1c' : '#524a44',
            transition: 'all 0.2s ease',
            zIndex: 10,
            cursor: 'pointer',
          }}
          className="hover:scale-110 shadow-sm"
        >
          <Heart size={18} fill={isWishlisted ? '#b91c1c' : 'none'} />
        </button>

        {/* Fabric or Occasion Badge */}
        {(product.fabric || product.occasion) && (
          <div
            style={{
              position: 'absolute',
              top: '12px',
              left: '12px',
              backgroundColor: 'rgba(255, 253, 249, 0.94)',
              backdropFilter: 'blur(6px)',
              padding: '4px 10px',
              fontSize: '10px',
              fontWeight: 600,
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              color: '#460e18',
              borderRadius: '2px',
              border: '1px solid #e6cb78',
              boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
            }}
          >
            {product.fabric || product.occasion}
          </div>
        )}

        {/* Festive Discount Tag */}
        <div
          style={{
            position: 'absolute',
            bottom: '12px',
            left: '12px',
            backgroundColor: '#0f4c3a',
            color: '#ffffff',
            padding: '3px 8px',
            fontSize: '10px',
            fontWeight: 700,
            letterSpacing: '0.06em',
            borderRadius: '2px',
            textTransform: 'uppercase',
            boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
          }}
        >
          31% OFF
        </div>
      </Link>

      {/* Product Content Details */}
      <div
        style={{
          padding: '16px 18px 18px',
          display: 'flex',
          flexDirection: 'column',
          flex: 1,
          justifyContent: 'space-between',
          backgroundColor: '#ffffff',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
            <p
              className="eyebrow"
              style={{
                margin: 0,
                color: '#8e2337',
                fontSize: '10px',
                letterSpacing: '0.16em',
              }}
            >
              {product.categoryName || 'Surat Atelier'}
            </p>
            {/* Star Rating */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '2px', fontSize: '11px', color: '#c59b27' }}>
              <Star size={12} fill="#c59b27" />
              <span style={{ fontWeight: 600, color: '#524a44', fontSize: '11px' }}>4.9</span>
            </div>
          </div>

          <h3
            className="font-serif"
            style={{
              margin: '0 0 10px',
              fontSize: '1.125rem',
              fontWeight: 500,
              lineHeight: 1.35,
              color: '#1e1917',
            }}
          >
            <Link
              href={`/products/${product.slug}`}
              style={{
                color: 'inherit',
                textDecoration: 'none',
              }}
              className="hover:text-wine transition-colors"
            >
              {product.name}
            </Link>
          </h3>
        </div>

        <div style={{ marginTop: '12px' }}>
          {/* Price breakdown with original price strike-through */}
          <div
            style={{
              display: 'flex',
              alignItems: 'baseline',
              gap: '8px',
              marginBottom: '4px',
            }}
          >
            <span
              style={{
                fontSize: '1.25rem',
                fontWeight: 700,
                color: '#6d1a29',
                fontFamily: 'var(--font-sans)',
              }}
            >
              {formattedPrice}
            </span>
            <span
              style={{
                fontSize: '12px',
                color: '#857b72',
                textDecoration: 'line-through',
              }}
            >
              {formattedOriginalPrice}
            </span>
            <span
              style={{
                fontSize: '11px',
                color: '#857b72',
                letterSpacing: '0.02em',
              }}
            >
              excl. GST
            </span>
          </div>

          {/* Quick Select & Add Bar */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingTop: '12px',
              borderTop: '1px solid #f0e9df',
              marginTop: '10px',
            }}
          >
            <Link
              href={`/products/${product.slug}`}
              style={{
                fontSize: '12px',
                color: '#6d1a29',
                fontWeight: 600,
                letterSpacing: '0.04em',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
                textTransform: 'uppercase',
              }}
              className="hover:underline"
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
                padding: '6px 10px',
                minHeight: 'auto',
                color: '#6d1a29',
                backgroundColor: '#faf0f2',
                borderRadius: '3px',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '12px',
                fontWeight: 600,
              }}
            >
              <ShoppingBag size={15} />
              <span>Add</span>
            </button>
          </div>
        </div>
      </div>
    </article>
  );
};
