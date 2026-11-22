'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { ArrowUpRight, Sparkles, ShieldCheck, Truck, RefreshCw } from 'lucide-react';
import { EditorialPlaceholder } from '@/components/brand/EditorialPlaceholder';
import { LinkButton } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { ProductCard } from '@/components/storefront/ProductCard';
import { Skeleton } from '@/components/ui/Skeleton';
import { Category, ProductSummary, catalogApi } from '@/lib/api/catalog';

export default function HomePage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [newArrivals, setNewArrivals] = useState<ProductSummary[]>([]);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [loadingProducts, setLoadingProducts] = useState(true);

  useEffect(() => {
    let isMounted = true;

    catalogApi
      .getCategories(false)
      .then((res) => {
        if (isMounted && res.data) {
          setCategories(res.data.filter((c) => c.active));
        }
      })
      .catch(() => {
        // Fallback gracefully without breaking layout
      })
      .finally(() => {
        if (isMounted) setLoadingCategories(false);
      });

    catalogApi
      .getProducts({ page: 0, pageSize: 4, sort: 'createdAt,desc' })
      .then((res) => {
        if (isMounted && res.data && res.data.content) {
          setNewArrivals(res.data.content);
        }
      })
      .catch(() => {
        // Fallback gracefully
      })
      .finally(() => {
        if (isMounted) setLoadingProducts(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div>
      {/* Hero Section */}
      <section
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(320px, 0.95fr) minmax(380px, 1.05fr)',
          minHeight: '560px',
          padding: '48px clamp(20px, 6vw, 96px) 64px',
          gap: 'clamp(32px, 5vw, 80px)',
          alignItems: 'center',
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
        }}
      >
        <div style={{ maxWidth: '520px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
            <Badge variant="wine" icon={<Sparkles size={12} />}>
              Surat Atelier &bull; Autumn / Festive 2026
            </Badge>
          </div>

          <h1
            className="font-serif"
            style={{
              margin: '0 0 20px',
              fontSize: 'var(--text-display-xl)',
              lineHeight: 1.02,
              letterSpacing: '-0.04em',
              fontWeight: 400,
            }}
          >
            Style that feels <br />
            <em style={{ color: 'var(--wine)', fontStyle: 'italic' }}>like your own.</em>
          </h1>

          <p
            style={{
              color: 'var(--ink-muted)',
              fontSize: 'var(--text-body-lg)',
              lineHeight: 1.7,
              marginBottom: '32px',
              maxWidth: '440px',
            }}
          >
            A thoughtful edit of women’s and girls’ garments, designed with restraint,
            crafted in Surat, and made for the moments that make up your everyday.
          </p>

          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
            <LinkButton
              href="/collections"
              variant="primary"
              size="lg"
              endIcon={<ArrowUpRight size={18} />}
            >
              Explore Collections
            </LinkButton>
            <LinkButton
              href="/design-system"
              variant="outline"
              size="lg"
            >
              Design System
            </LinkButton>
          </div>

          <div
            style={{
              display: 'flex',
              gap: '24px',
              marginTop: '40px',
              paddingTop: '24px',
              borderTop: '1px solid var(--line)',
              flexWrap: 'wrap',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Truck size={16} color="var(--wine)" />
              <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>Free delivery over ₹2,000 (pre-GST)</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <RefreshCw size={16} color="var(--wine)" />
              <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>3-day quality return policy</span>
            </div>
          </div>
        </div>

        <div>
          <EditorialPlaceholder
            index="D&Z / 001"
            caption="A NEW PERSPECTIVE ON EVERYDAY DRESSING &bull; SURAT ATELIER"
            minHeight="480px"
          />
        </div>
      </section>

      {/* Dynamic Database-Driven Categories Section */}
      <section
        id="categories"
        style={{
          borderTop: '1px solid var(--line)',
          padding: '64px clamp(20px, 6vw, 96px)',
          backgroundColor: 'var(--paper-warm)',
        }}
      >
        <div style={{ maxWidth: 'var(--container-max)', margin: '0 auto' }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-end',
              flexWrap: 'wrap',
              gap: '20px',
              marginBottom: '36px',
            }}
          >
            <div>
              <p className="eyebrow">Database-Driven Wardrobe</p>
              <h2
                className="font-serif"
                style={{
                  margin: 0,
                  fontSize: 'var(--text-display-md)',
                  fontWeight: 400,
                  letterSpacing: '-0.03em',
                }}
              >
                Curated Collections
              </h2>
            </div>
            <p style={{ maxWidth: '340px', fontSize: 'var(--text-body-sm)', color: 'var(--ink-muted)', margin: 0 }}>
              Each garment is patterned, sampled and finished in Surat with attention to drape, fabric weight and ease of wear.
            </p>
          </div>

          <div style={{ borderTop: '1px solid var(--line)' }}>
            {loadingCategories ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', padding: '24px 0' }}>
                <Skeleton height="60px" />
                <Skeleton height="60px" />
                <Skeleton height="60px" />
              </div>
            ) : categories.length > 0 ? (
              categories.map((cat, idx) => (
                <Link
                  key={cat.id || cat.slug}
                  href={`/collections?category=${encodeURIComponent(cat.slug)}`}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '80px 1fr auto',
                    alignItems: 'center',
                    minHeight: '76px',
                    borderBottom: '1px solid var(--line)',
                    padding: '0 8px',
                    transition: 'background-color var(--transition-fast), padding var(--transition-fast)',
                    textDecoration: 'none',
                  }}
                >
                  <span style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>
                    0{idx + 1}
                  </span>
                  <span
                    className="font-serif"
                    style={{ fontSize: 'var(--text-heading-md)', color: 'var(--ink)' }}
                  >
                    {cat.name}
                  </span>
                  <span
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      width: '32px',
                      height: '32px',
                      borderRadius: '50%',
                      backgroundColor: 'var(--paper-deep)',
                      color: 'var(--ink)',
                    }}
                  >
                    <ArrowUpRight size={16} />
                  </span>
                </Link>
              ))
            ) : (
              <div style={{ padding: '36px 0', textAlign: 'center' }}>
                <p style={{ margin: '0 0 16px', color: 'var(--ink-muted)', fontSize: 'var(--text-body-md)' }}>
                  New seasonal collections are currently being curated at our Surat workshop.
                </p>
                <LinkButton href="/collections" variant="outline" size="sm">
                  Browse All Ready Stock
                </LinkButton>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* Dynamic New Arrivals Section */}
      <section
        style={{
          padding: '64px clamp(20px, 6vw, 96px)',
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            marginBottom: '36px',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div>
            <p className="eyebrow">Fresh Off The Loom</p>
            <h2
              className="font-serif"
              style={{
                margin: 0,
                fontSize: 'var(--text-display-md)',
                fontWeight: 400,
                letterSpacing: '-0.03em',
              }}
            >
              Latest Garment Releases
            </h2>
          </div>
          <Link
            href="/collections?sort=createdAt,desc"
            style={{
              fontSize: '13px',
              color: 'var(--wine)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              fontWeight: 500,
            }}
          >
            <span>View All Garments</span> &rarr;
          </Link>
        </div>

        {loadingProducts ? (
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: '24px',
            }}
          >
            <Skeleton height="380px" />
            <Skeleton height="380px" />
            <Skeleton height="380px" />
            <Skeleton height="380px" />
          </div>
        ) : newArrivals.length > 0 ? (
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: '24px',
            }}
          >
            {newArrivals.map((product) => (
              <ProductCard key={product.id || product.slug} product={product} />
            ))}
          </div>
        ) : (
          <EditorialPlaceholder
            index="D&Z / ARRIVALS"
            caption="SURAT TEXTILE ATELIER &bull; NEW SEASON SILHOUETTES IN PRODUCTION"
            minHeight="320px"
          />
        )}
      </section>

      {/* Brand Values / Quality Promise */}
      <section
        style={{
          padding: '64px clamp(20px, 6vw, 96px)',
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
          borderTop: '1px solid var(--line)',
        }}
      >
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '32px',
          }}
        >
          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ color: 'var(--wine)', marginBottom: '12px' }}>
              <ShieldCheck size={28} />
            </div>
            <h3 className="font-serif" style={{ margin: '0 0 8px', fontSize: 'var(--text-heading-sm)' }}>
              Surat Manufacturing Heritage
            </h3>
            <p style={{ margin: 0, fontSize: 'var(--text-body-sm)', color: 'var(--ink-muted)', lineHeight: 1.6 }}>
              Rooted in the textile capital of India, D &amp; Z Fashion maintains complete internal control over fabric selection, cutting, stitching and finishing.
            </p>
          </div>

          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ color: 'var(--wine)', marginBottom: '12px' }}>
              <Truck size={28} />
            </div>
            <h3 className="font-serif" style={{ margin: '0 0 8px', fontSize: 'var(--text-heading-sm)' }}>
              Pan-India Transport &amp; COD
            </h3>
            <p style={{ margin: 0, fontSize: 'var(--text-body-sm)', color: 'var(--ink-muted)', lineHeight: 1.6 }}>
              Dispatched daily from Surat via trusted regional and national carrier agencies. Cash on Delivery is supported across service-verified PIN codes.
            </p>
          </div>

          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ color: 'var(--wine)', marginBottom: '12px' }}>
              <RefreshCw size={28} />
            </div>
            <h3 className="font-serif" style={{ margin: '0 0 8px', fontSize: 'var(--text-heading-sm)' }}>
              3-Day Quality Assurance
            </h3>
            <p style={{ margin: 0, fontSize: 'var(--text-body-sm)', color: 'var(--ink-muted)', lineHeight: 1.6 }}>
              Every delivered parcel carries our 3-day quality pledge for defect verification and hassle-free exchange or store credit resolution.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
