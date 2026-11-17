'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { Sparkles, Truck, RefreshCw, ShieldCheck } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { ErrorState } from '@/components/ui/ErrorState';
import { ProductGallery } from '@/components/storefront/ProductGallery';
import { VariantSelector } from '@/components/storefront/VariantSelector';
import { ProductCard } from '@/components/storefront/ProductCard';
import { ProductDetail, ProductSummary, ProductVariant, catalogApi } from '@/lib/api/catalog';

export interface ProductDetailClientProps {
  slug: string;
}

export const ProductDetailClient: React.FC<ProductDetailClientProps> = ({ slug }) => {
  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [relatedProducts, setRelatedProducts] = useState<ProductSummary[]>([]);
  const [selectedVariant, setSelectedVariant] = useState<ProductVariant | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    catalogApi
      .getProductBySlug(slug)
      .then((res) => {
        if (!isMounted) return;
        const prod = res.data;
        setProduct(prod);
        setLoading(false);

        // Fetch related products from same category if categorySlug exists
        if (prod && prod.categorySlug) {
          catalogApi
            .getProducts({ categorySlug: prod.categorySlug, pageSize: 5 })
            .then((relatedRes) => {
              if (isMounted && relatedRes.data && relatedRes.data.content) {
                // Filter out current product
                const filtered = relatedRes.data.content
                  .filter((p) => p.slug !== slug)
                  .slice(0, 4);
                setRelatedProducts(filtered);
              }
            })
            .catch(() => {
              // Silently ignore related products fetch failure
            });
        }
      })
      .catch((err: unknown) => {
        if (!isMounted) return;
        const msg = err instanceof Error ? err.message : 'Unable to load garment details';
        setError(msg);
        setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [slug]);

  if (loading) {
    return (
      <div
        style={{
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
          padding: '40px clamp(20px, 4vw, 64px) 80px',
        }}
      >
        <Skeleton height="20px" width="240px" style={{ marginBottom: '24px' }} />
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: '48px',
          }}
        >
          <Skeleton height="560px" />
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <Skeleton height="32px" width="160px" />
            <Skeleton height="48px" width="80%" />
            <Skeleton height="24px" width="120px" />
            <Skeleton height="160px" />
            <Skeleton height="48px" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div
        style={{
          maxWidth: '680px',
          margin: '60px auto',
          padding: '0 20px',
        }}
      >
        <ErrorState
          title="Garment Not Found"
          message={error || 'This garment could not be found or has not yet been published.'}
          onRetry={() => window.location.reload()}
          retryLabel="Try again"
        />
        <div style={{ textAlign: 'center', marginTop: '24px' }}>
          <Link href="/collections" className="btn btn-secondary btn-sm">
            &larr; Return to All Collections
          </Link>
        </div>
      </div>
    );
  }

  // Schema.org Product structured data for SEO
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: product.name,
    description: product.description,
    image: product.media.map((m) => m.secureUrl),
    category: product.categoryName,
    brand: {
      '@type': 'Brand',
      name: 'D & Z Fashion',
    },
    offers: {
      '@type': 'Offer',
      priceCurrency: 'INR',
      price: selectedVariant ? selectedVariant.effectivePrice : product.defaultPrice,
      availability:
        selectedVariant && selectedVariant.active
          ? 'https://schema.org/InStock'
          : 'https://schema.org/OutOfStock',
      itemCondition: 'https://schema.org/NewCondition',
    },
  };

  return (
    <>
      {/* Structured data injection */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />

      <div
        style={{
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
          padding: '32px clamp(20px, 4vw, 64px) 80px',
        }}
      >
        {/* Breadcrumbs */}
        <nav
          aria-label="Breadcrumbs"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '12px',
            color: 'var(--ink-muted)',
            marginBottom: '32px',
            flexWrap: 'wrap',
          }}
        >
          <Link href="/" style={{ color: 'var(--ink-muted)', textDecoration: 'none' }}>
            Home
          </Link>
          <span>/</span>
          <Link href="/collections" style={{ color: 'var(--ink-muted)', textDecoration: 'none' }}>
            Collections
          </Link>
          {product.categorySlug && (
            <>
              <span>/</span>
              <Link
                href={`/collections?category=${encodeURIComponent(product.categorySlug)}`}
                style={{ color: 'var(--ink-muted)', textDecoration: 'none' }}
              >
                {product.categoryName}
              </Link>
            </>
          )}
          <span>/</span>
          <span style={{ color: 'var(--ink)', fontWeight: 500 }}>{product.name}</span>
        </nav>

        {/* Product Two-Column Layout */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(320px, 1fr) minmax(340px, 1fr)',
            gap: 'clamp(32px, 5vw, 64px)',
            alignItems: 'start',
            marginBottom: '80px',
          }}
        >
          {/* Left Column: Media Gallery */}
          <div>
            <ProductGallery
              media={product.media}
              productName={product.name}
              selectedVariantId={selectedVariant?.id}
            />
          </div>

          {/* Right Column: Garment Information & Selector */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                <Badge variant="wine" icon={<Sparkles size={12} />}>
                  {product.categoryName || 'Surat Atelier'}
                </Badge>
                {product.fabric && <Badge variant="stone">{product.fabric}</Badge>}
                {product.occasion && <Badge variant="neutral">{product.occasion}</Badge>}
              </div>

              <h1
                className="font-serif"
                style={{
                  margin: '0 0 12px',
                  fontSize: 'var(--text-display-md)',
                  fontWeight: 400,
                  lineHeight: 1.15,
                  letterSpacing: '-0.02em',
                }}
              >
                {product.name}
              </h1>

              {product.fabric && (
                <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
                  Weave &amp; Composition: <strong style={{ color: 'var(--ink)' }}>{product.fabric}</strong>
                </p>
              )}
            </div>

            {/* Variant Selector (Handles Price, Size, Colour, Stock Check, Add to Cart) */}
            <VariantSelector
              variants={product.variants || []}
              defaultPrice={product.defaultPrice}
              productSlug={product.slug}
              productName={product.name}
              categoryName={product.categoryName}
              onVariantSelected={setSelectedVariant}
            />

            {/* Description & Silhouette Details */}
            <div
              style={{
                borderTop: '1px solid var(--line)',
                paddingTop: '24px',
                fontSize: '14px',
                lineHeight: 1.7,
                color: 'var(--ink)',
              }}
            >
              <h2
                className="font-serif"
                style={{
                  margin: '0 0 12px',
                  fontSize: 'var(--text-heading-sm)',
                  fontWeight: 500,
                }}
              >
                Atelier Notes &amp; Silhouette
              </h2>
              <div style={{ whiteSpace: 'pre-line', color: 'var(--ink-muted)' }}>
                {product.description ||
                  'Carefully tailored in our Surat manufacturing facility using high quality ready stock fabric. Built for comfort, longevity and elegant movement.'}
              </div>
            </div>

            {/* Shipping, Returns & Quality Assurance */}
            <div
              style={{
                borderTop: '1px solid var(--line)',
                paddingTop: '24px',
                display: 'flex',
                flexDirection: 'column',
                gap: '16px',
              }}
            >
              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <Truck size={18} color="var(--wine)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <strong style={{ fontSize: '13px', color: 'var(--ink)' }}>
                    Pan-India Carrier Dispatch
                  </strong>
                  <p style={{ margin: '2px 0 0', fontSize: '12px', color: 'var(--ink-muted)' }}>
                    Dispatched within 24 hours from Surat warehouse. Free delivery on orders over ₹2,000 (pre-GST). Cash on Delivery available for verified PIN codes.
                  </p>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <RefreshCw size={18} color="var(--wine)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <strong style={{ fontSize: '13px', color: 'var(--ink)' }}>
                    3-Day Quality Assurance &amp; Exchange
                  </strong>
                  <p style={{ margin: '2px 0 0', fontSize: '12px', color: 'var(--ink-muted)' }}>
                    Notify us within 3 days of delivery for defect inspection or size exchange.
                  </p>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <ShieldCheck size={18} color="var(--wine)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <strong style={{ fontSize: '13px', color: 'var(--ink)' }}>
                    Surat Direct Sourcing
                  </strong>
                  <p style={{ margin: '2px 0 0', fontSize: '12px', color: 'var(--ink-muted)' }}>
                    Zero intermediary markups. Guaranteed garment construction and seam integrity.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Related Garments Section (Data-Driven Only) */}
        {relatedProducts.length > 0 && (
          <section style={{ borderTop: '1px solid var(--line)', paddingTop: '64px' }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-end',
                marginBottom: '32px',
                flexWrap: 'wrap',
                gap: '16px',
              }}
            >
              <div>
                <p className="eyebrow">Complementary Garments</p>
                <h2
                  className="font-serif"
                  style={{
                    margin: 0,
                    fontSize: 'var(--text-display-sm)',
                    fontWeight: 400,
                  }}
                >
                  More from {product.categoryName}
                </h2>
              </div>
              <Link
                href={`/collections?category=${encodeURIComponent(product.categorySlug)}`}
                style={{
                  fontSize: '13px',
                  color: 'var(--wine)',
                  textDecoration: 'none',
                  fontWeight: 500,
                }}
              >
                View full category &rarr;
              </Link>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
                gap: '24px',
              }}
            >
              {relatedProducts.map((relProduct) => (
                <ProductCard key={relProduct.id || relProduct.slug} product={relProduct} />
              ))}
            </div>
          </section>
        )}
      </div>
    </>
  );
};
