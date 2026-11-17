'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { ProductVariant, StockResponse, catalogApi } from '@/lib/api/catalog';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/components/ui/Toast';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Ruler, ShoppingBag, Heart, AlertCircle, CheckCircle2 } from 'lucide-react';
import { SizeChartModal } from './SizeChartModal';

export interface VariantSelectorProps {
  variants: ProductVariant[];
  defaultPrice: number;
  productSlug: string;
  productName: string;
  categoryName?: string;
  onVariantSelected?: (variant: ProductVariant | null) => void;
}

export const VariantSelector: React.FC<VariantSelectorProps> = ({
  variants,
  defaultPrice,
  productSlug,
  productName,
  categoryName,
  onVariantSelected,
}) => {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { toast } = useToast();

  // Extract unique sizes and colours from active variants
  const activeVariants = variants.filter((v) => v.active);
  const availableSizes = Array.from(new Set(activeVariants.map((v) => v.size)));
  const availableColours = Array.from(new Set(activeVariants.map((v) => v.colour)));

  const [selectedSize, setSelectedSize] = useState<string>(availableSizes[0] || '');
  const [selectedColour, setSelectedColour] = useState<string>(availableColours[0] || '');
  const [sizeChartOpen, setSizeChartOpen] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [stockInfo, setStockInfo] = useState<StockResponse | null>(null);
  const [loadingStock, setLoadingStock] = useState(false);

  // Find variant matching current selection
  const currentVariant =
    activeVariants.find(
      (v) => v.size === selectedSize && v.colour === selectedColour
    ) || null;

  useEffect(() => {
    if (onVariantSelected) {
      onVariantSelected(currentVariant);
    }
  }, [currentVariant, onVariantSelected]);

  // Fetch live inventory stock asynchronously when currentVariant changes
  useEffect(() => {
    let isMounted = true;
    if (!currentVariant) {
      return;
    }

    Promise.resolve().then(() => {
      if (isMounted) setLoadingStock(true);
    });

    catalogApi
      .getVariantStock(currentVariant.id)
      .then((res) => {
        if (isMounted) {
          setStockInfo(res.data);
          setLoadingStock(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          setStockInfo(null);
          setLoadingStock(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [currentVariant]);

  const effectivePrice = currentVariant
    ? currentVariant.effectivePrice
    : defaultPrice;

  const formattedPrice = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(effectivePrice);

  const isSoldOut = stockInfo ? stockInfo.availableQty <= 0 : false;
  const isLowStock =
    stockInfo && stockInfo.availableQty > 0 && stockInfo.availableQty <= (stockInfo.lowStockThreshold || 3);

  const handleAddToCart = () => {
    if (!isAuthenticated) {
      toast({
        type: 'info',
        title: 'Sign in required',
        message: 'Please sign in to add items to your shopping bag.',
      });
      router.push(`/login?redirect=${encodeURIComponent(`/products/${productSlug}`)}`);
      return;
    }

    if (!currentVariant) {
      toast({
        type: 'warning',
        title: 'Select size and colour',
        message: 'Please select a size and colour variant.',
      });
      return;
    }

    if (isSoldOut) {
      toast({
        type: 'error',
        title: 'Sold Out',
        message: 'This variant is currently out of stock.',
      });
      return;
    }

    setIsAdding(true);
    // In Component 13, this will call commerce /api/v1/cart
    setTimeout(() => {
      setIsAdding(false);
      toast({
        type: 'success',
        title: 'Added to Shopping Bag',
        message: `${productName} (${selectedSize} / ${selectedColour}) has been added.`,
      });
    }, 400);
  };

  const handleAddToWishlist = () => {
    if (!isAuthenticated) {
      toast({
        type: 'info',
        title: 'Sign in required',
        message: 'Please sign in to manage your wishlist.',
      });
      router.push(`/login?redirect=${encodeURIComponent(`/products/${productSlug}`)}`);
      return;
    }

    toast({
      type: 'success',
      title: 'Saved to Wishlist',
      message: `${productName} saved to your wishlist.`,
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Price & GST Disclosure */}
      <div style={{ paddingBottom: '20px', borderBottom: '1px solid var(--line)' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '6px' }}>
          <span
            style={{
              fontSize: 'var(--text-display-md)',
              fontWeight: 600,
              color: 'var(--ink)',
              fontFamily: 'var(--font-mono)',
            }}
          >
            {formattedPrice}
          </span>
          <span
            style={{
              fontSize: '12px',
              color: 'var(--ink-muted)',
              letterSpacing: '0.04em',
              textTransform: 'uppercase',
            }}
          >
            Price excl. GST
          </span>
        </div>

        <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-subtle)' }}>
          Applicable GST calculated at checkout. Free shipping on orders over ₹2,000 (pre-GST).
        </p>

        {currentVariant && currentVariant.priceOverride && (
          <div style={{ marginTop: '8px' }}>
            <Badge variant="wine">Special Variant Pricing Applied</Badge>
          </div>
        )}
      </div>

      {/* Colour Selector */}
      {availableColours.length > 0 && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span className="eyebrow" style={{ margin: 0 }}>
              Colour: <strong style={{ color: 'var(--ink)', textTransform: 'capitalize' }}>{selectedColour}</strong>
            </span>
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {availableColours.map((colour) => {
              const isSelected = selectedColour === colour;
              return (
                <button
                  key={colour}
                  type="button"
                  onClick={() => setSelectedColour(colour)}
                  className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-secondary'}`}
                  style={{
                    minHeight: 'auto',
                    padding: '6px 14px',
                    fontSize: '13px',
                    borderRadius: 'var(--radius-sm)',
                  }}
                >
                  {colour}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Size Selector + Size Chart Link */}
      {availableSizes.length > 0 && (
        <div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '8px',
            }}
          >
            <span className="eyebrow" style={{ margin: 0 }}>
              Select Size: <strong style={{ color: 'var(--ink)' }}>{selectedSize}</strong>
            </span>
            <button
              type="button"
              onClick={() => setSizeChartOpen(true)}
              className="btn btn-ghost"
              style={{
                padding: '4px 6px',
                minHeight: 'auto',
                fontSize: '12px',
                color: 'var(--wine)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
              }}
            >
              <Ruler size={14} />
              <span>Size Guide</span>
            </button>
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {availableSizes.map((size) => {
              const isSelected = selectedSize === size;
              const matchingVar = activeVariants.find(
                (v) => v.size === size && v.colour === selectedColour
              );
              const exists = Boolean(matchingVar);

              return (
                <button
                  key={size}
                  type="button"
                  disabled={!exists}
                  onClick={() => setSelectedSize(size)}
                  style={{
                    minWidth: '52px',
                    padding: '8px 14px',
                    fontSize: '13px',
                    fontWeight: isSelected ? 600 : 400,
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: isSelected ? 'var(--wine)' : '#ffffff',
                    color: isSelected ? '#ffffff' : exists ? 'var(--ink)' : 'var(--ink-subtle)',
                    border: `1px solid ${isSelected ? 'var(--wine)' : exists ? 'var(--line)' : 'var(--line)'}`,
                    cursor: exists ? 'pointer' : 'not-allowed',
                    opacity: exists ? 1 : 0.4,
                    textDecoration: exists ? 'none' : 'line-through',
                    transition: 'all var(--transition-fast)',
                  }}
                  aria-label={`Size ${size}${!exists ? ' (Unavailable)' : ''}`}
                >
                  {size}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Stock Availability Indicator */}
      <div>
        {loadingStock ? (
          <span style={{ fontSize: '12px', color: 'var(--ink-subtle)' }}>
            Checking inventory stock in Surat warehouse...
          </span>
        ) : isSoldOut ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              color: 'var(--color-error)',
              fontSize: '13px',
              fontWeight: 500,
            }}
          >
            <AlertCircle size={16} />
            <span>Sold Out — Currently out of stock in Surat warehouse</span>
          </div>
        ) : isLowStock ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              color: 'var(--color-warning)',
              fontSize: '13px',
              fontWeight: 500,
            }}
          >
            <AlertCircle size={16} />
            <span>Only {stockInfo?.availableQty} pieces remaining in ready stock</span>
          </div>
        ) : (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              color: 'var(--color-success)',
              fontSize: '13px',
            }}
          >
            <CheckCircle2 size={16} />
            <span>In Ready Stock • Dispatches in 24 hours from Surat</span>
          </div>
        )}
      </div>

      {/* Action Buttons: Add to Bag & Wishlist */}
      <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
        <Button
          type="button"
          variant={isSoldOut ? 'outline' : 'primary'}
          size="lg"
          disabled={isSoldOut || !currentVariant}
          isLoading={isAdding}
          onClick={handleAddToCart}
          startIcon={<ShoppingBag size={18} />}
          style={{ flex: 2 }}
        >
          {isSoldOut ? 'Sold Out' : 'Add to Shopping Bag'}
        </Button>

        <Button
          type="button"
          variant="outline"
          size="lg"
          onClick={handleAddToWishlist}
          aria-label="Save to Wishlist"
          style={{ flex: 1, padding: '0 16px' }}
        >
          <Heart size={18} />
          <span style={{ marginLeft: '6px' }}>Wishlist</span>
        </Button>
      </div>

      {/* Size Chart Modal */}
      <SizeChartModal
        isOpen={sizeChartOpen}
        onClose={() => setSizeChartOpen(false)}
        categoryName={categoryName}
      />
    </div>
  );
};
