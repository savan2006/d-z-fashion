'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  ArrowUpRight,
  Sparkles,
  ShieldCheck,
  Truck,
  RefreshCw,
  Star,
  Copy,
  ChevronRight,
} from 'lucide-react';
import { EditorialPlaceholder } from '@/components/brand/EditorialPlaceholder';
import { LinkButton } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { ProductCard } from '@/components/storefront/ProductCard';
import { Skeleton } from '@/components/ui/Skeleton';
import { Category, ProductSummary, catalogApi } from '@/lib/api/catalog';
import { useToast } from '@/components/ui/Toast';

// Curated Ladies Ethnic Wear Categories with Authentic Surat Imagery & Descriptions
const FEATURED_LADIES_CATEGORIES = [
  {
    title: 'Banarasi Sarees',
    slug: 'banarasi-sarees',
    fabric: 'Pure Silk & Zari',
    desc: 'Handcrafted gold brocades with intricate floral jaal weaves for weddings and grand festivals.',
    tag: 'BRIDAL BESTSELLER',
    gradient: 'linear-gradient(145deg, #4a0e1a 0%, #721c24 100%)',
    border: '#dfc15d',
    itemCount: '35+ Designs',
  },
  {
    title: 'Kanjeevaram Sarees',
    slug: 'kanjeevaram-sarees',
    fabric: 'Kanchipuram Silk',
    desc: 'Lustrous drapes in royal golds and jewel tones with traditional temple border motifs.',
    tag: 'HERITAGE SILK',
    gradient: 'linear-gradient(145deg, #0f4c3a 0%, #1b7358 100%)',
    border: '#e6cb78',
    itemCount: '28+ Designs',
  },
  {
    title: 'Georgette Lehengas',
    slug: 'georgette-lehengas',
    fabric: 'Georgette & Resham',
    desc: 'Flared semi-stitched lehenga cholis with authentic mirror work and can-can layering.',
    tag: 'FESTIVE SANGEET',
    gradient: 'linear-gradient(145deg, #6d1a29 0%, #a33249 100%)',
    border: '#f6e8b1',
    itemCount: '42+ Designs',
  },
  {
    title: 'Anarkali Kurtis',
    slug: 'anarkali-kurtis',
    fabric: 'Chanderi Cotton',
    desc: 'Floor-length flared kurtis paired with woven dupattas and tailored churidars.',
    tag: 'EVERYDAY ROYAL',
    gradient: 'linear-gradient(145deg, #2c1a1d 0%, #522731 100%)',
    border: '#dfc15d',
    itemCount: '50+ Designs',
  },
  {
    title: 'Girls Ethnic Wear',
    slug: 'girls-ethnic-wear',
    fabric: 'Cotton Silk',
    desc: 'Vibrant Bandhani flared frocks, ghagras and festive ensembles for young celebrations.',
    tag: 'NEW FESTIVE',
    gradient: 'linear-gradient(145deg, #8a5712 0%, #b38a2e 100%)',
    border: '#fcedb4',
    itemCount: '24+ Designs',
  },
];

export default function HomePage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [newArrivals, setNewArrivals] = useState<ProductSummary[]>([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const { toast } = useToast();

  const handleCopyCode = (code: string) => {
    navigator.clipboard?.writeText(code);
    setCopiedCode(code);
    toast({
      type: 'success',
      title: 'Coupon Copied!',
      message: `Code "${code}" copied to clipboard. Apply at checkout for your discount.`,
    });
    setTimeout(() => setCopiedCode(null), 3000);
  };

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
    <div style={{ backgroundColor: '#fcfbf7' }}>
      {/* =========================================================================
          1. HERO SECTION: Royal Surat Atelier & Autumn / Festive 2026 Showcase
          ========================================================================= */}
      <section
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(320px, 1.05fr) minmax(360px, 0.95fr)',
          minHeight: '620px',
          padding: '56px clamp(20px, 6vw, 96px) 72px',
          gap: 'clamp(36px, 6vw, 84px)',
          alignItems: 'center',
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
        }}
      >
        <div style={{ maxWidth: '580px' }}>
          {/* Atelier Heritage Pill */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
            <Badge variant="wine" icon={<Sparkles size={12} />}>
              Surat Atelier &bull; Autumn / Festive 2026
            </Badge>
            <span
              style={{
                fontSize: '11px',
                fontWeight: 600,
                color: '#c59b27',
                letterSpacing: '0.12em',
                textTransform: 'uppercase',
              }}
            >
              ★ Handwoven In Gujarat
            </span>
          </div>

          <h1
            className="font-serif"
            style={{
              margin: '0 0 24px',
              fontSize: 'clamp(2.5rem, 4.5vw, 4rem)',
              lineHeight: 1.08,
              letterSpacing: '-0.03em',
              fontWeight: 500,
              color: '#1e1917',
            }}
          >
            Royal Surat Grace, <br />
            <em
              style={{
                color: '#6d1a29',
                fontStyle: 'italic',
                fontWeight: 400,
                fontFamily: "'Cormorant Garamond', Georgia, serif",
              }}
            >
              woven for your finest moments.
            </em>
          </h1>

          <p
            style={{
              color: '#524a44',
              fontSize: '1.125rem',
              lineHeight: 1.75,
              marginBottom: '36px',
              maxWidth: '500px',
            }}
          >
            Experience the splendour of authentic Banarasi silks, heirloom Kanjeevaram drapes,
            and handcrafted georgette lehengas. Tailored directly in Surat&apos;s weaving workshops
            with zero compromises on drape, gold zari purity, and timeless elegance.
          </p>

          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
            <LinkButton
              href="/collections"
              variant="primary"
              size="lg"
              endIcon={<ArrowUpRight size={18} />}
              style={{
                backgroundColor: '#6d1a29',
                borderColor: '#6d1a29',
                padding: '14px 28px',
                fontSize: '15px',
                fontWeight: 600,
                letterSpacing: '0.04em',
                boxShadow: '0 6px 20px -2px rgba(109, 26, 41, 0.25)',
              }}
            >
              Explore Collections
            </LinkButton>

            <LinkButton
              href="/collections?category=banarasi-sarees"
              variant="outline"
              size="lg"
              style={{
                borderColor: '#dfc15d',
                color: '#460e18',
                backgroundColor: '#fcf9ee',
                padding: '14px 24px',
                fontSize: '15px',
                fontWeight: 600,
              }}
            >
              View Bridal &amp; Saree Edit
            </LinkButton>
          </div>

          {/* Trust Guarantees Bar & Design System Link */}
          <div
            style={{
              display: 'flex',
              gap: '24px',
              marginTop: '44px',
              paddingTop: '24px',
              borderTop: '1px solid #ebd9c8',
              flexWrap: 'wrap',
              alignItems: 'center',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Truck size={17} color="#6d1a29" />
              <span style={{ fontSize: '12px', color: '#524a44', fontWeight: 500 }}>
                Free delivery on orders ₹2,000+
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <RefreshCw size={17} color="#6d1a29" />
              <span style={{ fontSize: '12px', color: '#524a44', fontWeight: 500 }}>
                3-day quality return pledge
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShieldCheck size={17} color="#0f4c3a" />
              <span style={{ fontSize: '12px', color: '#524a44', fontWeight: 500 }}>
                100% Certified Surat Handloom
              </span>
            </div>
            <Link
              href="/design-system"
              style={{
                fontSize: '11px',
                color: '#857b72',
                textDecoration: 'underline',
                textUnderlineOffset: '3px',
              }}
            >
              Atelier Design System
            </Link>
          </div>
        </div>

        {/* Right Hero Editorial Visual */}
        <div>
          <EditorialPlaceholder
            index="D&Z / ATELIER"
            caption="SURAT ROYAL WEAVES &bull; BANARASI SILK &bull; AUTUMN / FESTIVE 2026"
            minHeight="520px"
          />
        </div>
      </section>

      {/* =========================================================================
          2. FESTIVE VOUCHER TICKER: Instant Discounts & Promo Codes
          ========================================================================= */}
      <section
        style={{
          background: 'linear-gradient(90deg, #3d0a14 0%, #6d1a29 50%, #3d0a14 100%)',
          padding: '20px clamp(20px, 6vw, 96px)',
          borderTop: '1px solid #dfc15d',
          borderBottom: '1px solid #dfc15d',
        }}
      >
        <div
          style={{
            maxWidth: 'var(--container-max)',
            margin: '0 auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span
              style={{
                backgroundColor: '#dfc15d',
                color: '#3d0a14',
                padding: '4px 10px',
                borderRadius: '2px',
                fontSize: '11px',
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
              }}
            >
              FESTIVE SPECIAL
            </span>
            <p style={{ margin: 0, color: '#ffffff', fontSize: '14px', fontWeight: 500 }}>
              Unlock festive savings on your wedding and celebration wardrobe.
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flexWrap: 'wrap' }}>
            {/* Coupon 1: SURATFIRST */}
            <button
              type="button"
              onClick={() => handleCopyCode('SURATFIRST')}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                backgroundColor: copiedCode === 'SURATFIRST' ? 'rgba(223, 193, 93, 0.3)' : 'rgba(255, 255, 255, 0.1)',
                border: '1px dashed #dfc15d',
                borderRadius: '3px',
                padding: '6px 14px',
                color: '#f6e8b1',
                fontSize: '12px',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
              title="Click to copy coupon code"
            >
              <span>{copiedCode === 'SURATFIRST' ? '✓ COPIED TO CLIPBOARD' : <>Code: <strong>SURATFIRST</strong> (Flat ₹200 OFF)</>}</span>
              <Copy size={13} />
            </button>

            {/* Coupon 2: FESTIVE10 */}
            <button
              type="button"
              onClick={() => handleCopyCode('FESTIVE10')}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                backgroundColor: copiedCode === 'FESTIVE10' ? 'rgba(223, 193, 93, 0.3)' : 'rgba(255, 255, 255, 0.1)',
                border: '1px dashed #dfc15d',
                borderRadius: '3px',
                padding: '6px 14px',
                color: '#f6e8b1',
                fontSize: '12px',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
              title="Click to copy coupon code"
            >
              <span>{copiedCode === 'FESTIVE10' ? '✓ COPIED TO CLIPBOARD' : <>Code: <strong>FESTIVE10</strong> (10% OFF)</>}</span>
              <Copy size={13} />
            </button>
          </div>
        </div>
      </section>

      {/* =========================================================================
          3. CURATED LADIES WEAR CATEGORIES SHOWCASE
          ========================================================================= */}
      <section
        id="categories"
        style={{
          padding: '72px clamp(20px, 6vw, 96px)',
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            flexWrap: 'wrap',
            gap: '20px',
            marginBottom: '40px',
          }}
        >
          <div>
            <p className="eyebrow" style={{ color: '#8e2337' }}>
              Handcrafted in Surat
            </p>
            <h2
              className="font-serif"
              style={{
                margin: 0,
                fontSize: 'clamp(2rem, 3.2vw, 2.75rem)',
                fontWeight: 400,
                letterSpacing: '-0.02em',
                color: '#1e1917',
              }}
            >
              Curated Ethnic Silhouettes
            </h2>
          </div>
          <p style={{ maxWidth: '420px', fontSize: '14px', color: '#524a44', margin: 0, lineHeight: 1.6 }}>
            Each garment is patterned, hand-woven and tailored at our Surat atelier with strict attention to
            drape, fabric weight, and royal festive aesthetics.
          </p>
        </div>

        {/* 5-Card High-Fashion Category Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: '24px',
            marginBottom: '48px',
          }}
        >
          {FEATURED_LADIES_CATEGORIES.map((item) => (
            <Link
              key={item.slug}
              href={`/collections?category=${item.slug}`}
              className="luxury-card"
              style={{
                padding: '24px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: '4px',
                minHeight: '260px',
                background: '#ffffff',
                border: '1px solid #ebd9c8',
                textDecoration: 'none',
                position: 'relative',
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                  <span
                    style={{
                      fontSize: '9px',
                      fontWeight: 700,
                      letterSpacing: '0.08em',
                      backgroundColor: '#faf0f2',
                      color: '#6d1a29',
                      padding: '3px 8px',
                      borderRadius: '2px',
                      border: '1px solid #ebd5dd',
                      textTransform: 'uppercase',
                    }}
                  >
                    {item.tag}
                  </span>
                  <span style={{ fontSize: '11px', color: '#857b72', fontWeight: 500 }}>
                    {item.itemCount}
                  </span>
                </div>

                <h3
                  className="font-serif"
                  style={{
                    fontSize: '1.25rem',
                    fontWeight: 600,
                    margin: '0 0 8px',
                    color: '#1e1917',
                  }}
                >
                  {item.title}
                </h3>

                <p style={{ fontSize: '11px', color: '#c59b27', fontWeight: 600, margin: '0 0 8px', letterSpacing: '0.04em' }}>
                  Fabric: {item.fabric}
                </p>

                <p style={{ fontSize: '12px', color: '#524a44', margin: 0, lineHeight: 1.5 }}>
                  {item.desc}
                </p>
              </div>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  paddingTop: '16px',
                  borderTop: '1px solid #f8f4ed',
                  marginTop: '16px',
                }}
              >
                <span style={{ fontSize: '12px', color: '#6d1a29', fontWeight: 600 }}>Explore Designs</span>
                <span
                  style={{
                    width: '28px',
                    height: '28px',
                    borderRadius: '50%',
                    backgroundColor: '#faf0f2',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#6d1a29',
                  }}
                >
                  <ArrowUpRight size={14} />
                </span>
              </div>
            </Link>
          ))}
        </div>

        {/* Database-Driven Dynamic Categories Listing */}
        {categories.length > 0 && (
          <div style={{ borderTop: '1px solid #ebd9c8', paddingTop: '32px' }}>
            <p style={{ fontSize: '11px', color: '#857b72', letterSpacing: '0.12em', textTransform: 'uppercase', marginBottom: '16px', fontWeight: 600 }}>
              All Ready Stock Departments &bull; Surat Central Inventory
            </p>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px' }}>
              {categories.map((cat) => (
                <Link
                  key={cat.id || cat.slug}
                  href={`/collections?category=${encodeURIComponent(cat.slug)}`}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#ffffff',
                    border: '1px solid #ded3c4',
                    borderRadius: '2px',
                    fontSize: '13px',
                    color: '#1e1917',
                    fontWeight: 500,
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                  className="hover:border-wine hover:text-wine transition-colors"
                >
                  <span>{cat.name}</span>
                  <ChevronRight size={13} color="#857b72" />
                </Link>
              ))}
            </div>
          </div>
        )}
      </section>

      {/* =========================================================================
          4. FRESH OFF THE LOOM: Ready-Stock Ethnic Garment Releases
          ========================================================================= */}
      <section
        style={{
          padding: '72px clamp(20px, 6vw, 96px)',
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
          borderTop: '1px solid #ebd9c8',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            marginBottom: '40px',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div>
            <p className="eyebrow" style={{ color: '#8e2337' }}>
              Fresh Off The Loom
            </p>
            <h2
              className="font-serif"
              style={{
                margin: 0,
                fontSize: 'clamp(2rem, 3.2vw, 2.75rem)',
                fontWeight: 400,
                letterSpacing: '-0.02em',
                color: '#1e1917',
              }}
            >
              Latest Garment Releases
            </h2>
          </div>
          <Link
            href="/collections?sort=createdAt,desc"
            style={{
              fontSize: '13px',
              color: '#6d1a29',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontWeight: 600,
              textTransform: 'uppercase',
              letterSpacing: '0.04em',
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
              gap: '28px',
            }}
          >
            <Skeleton height="400px" />
            <Skeleton height="400px" />
            <Skeleton height="400px" />
            <Skeleton height="400px" />
          </div>
        ) : newArrivals.length > 0 ? (
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: '28px',
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
            minHeight="340px"
          />
        )}
      </section>

      {/* =========================================================================
          5. THE SURAT SILK CRAFT STORY: Heritage & Craftsmanship Banner
          ========================================================================= */}
      <section
        style={{
          backgroundColor: '#f8f4ed',
          borderTop: '1px solid #ebd9c8',
          borderBottom: '1px solid #ebd9c8',
          padding: '80px clamp(20px, 6vw, 96px)',
        }}
      >
        <div
          style={{
            maxWidth: 'var(--container-max)',
            margin: '0 auto',
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: '56px',
            alignItems: 'center',
          }}
        >
          <div>
            <span
              style={{
                fontSize: '11px',
                fontWeight: 700,
                color: '#c59b27',
                letterSpacing: '0.2em',
                textTransform: 'uppercase',
                display: 'block',
                marginBottom: '12px',
              }}
            >
              Surat Heritage &bull; Since 2026
            </span>
            <h2
              className="font-serif"
              style={{
                fontSize: 'clamp(2rem, 3.5vw, 3rem)',
                fontWeight: 400,
                margin: '0 0 20px',
                lineHeight: 1.15,
                color: '#1e1917',
              }}
            >
              The Master Weavers of Surat, <br />
              <em style={{ color: '#6d1a29', fontStyle: 'italic' }}>direct to your wardrobe.</em>
            </h2>
            <p style={{ fontSize: '15px', color: '#524a44', lineHeight: 1.8, marginBottom: '24px' }}>
              For centuries, the riverbanks of the Tapi River in Surat have been renowned as the textile
              nerve centre of India. At D &amp; Z Fashion, we eliminate layers of traditional distributors
              and luxury markups. Every thread of pure Katan silk, intricate gold zari, and hand-pleated
              georgette is inspected by master craftsmen before dispatch.
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginTop: '28px' }}>
              <div style={{ borderLeft: '2px solid #dfc15d', paddingLeft: '14px' }}>
                <h4 style={{ margin: '0 0 4px', fontSize: '15px', color: '#1e1917', fontWeight: 600 }}>
                  Pure Silk Guarantee
                </h4>
                <p style={{ margin: 0, fontSize: '12px', color: '#857b72' }}>
                  Tested yarns, certified zari work, and rich tactile weight.
                </p>
              </div>
              <div style={{ borderLeft: '2px solid #dfc15d', paddingLeft: '14px' }}>
                <h4 style={{ margin: '0 0 4px', fontSize: '15px', color: '#1e1917', fontWeight: 600 }}>
                  24-Hour Dispatch
                </h4>
                <p style={{ margin: 0, fontSize: '12px', color: '#857b72' }}>
                  Physical inventory ready in Surat for rapid express delivery.
                </p>
              </div>
            </div>
          </div>

          <div>
            <EditorialPlaceholder
              index="D&Z / HERITAGE"
              caption="HANDWOVEN TRADITION &bull; SURAT TEXTILE MARKET ATELIER &bull; GUJARAT"
              minHeight="420px"
              showNotice={false}
            />
          </div>
        </div>
      </section>

      {/* =========================================================================
          6. VERIFIED CUSTOMER REVIEWS & PRAISE
          ========================================================================= */}
      <section
        style={{
          padding: '80px clamp(20px, 6vw, 96px)',
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
        }}
      >
        <div style={{ textAlign: 'center', maxWidth: '640px', margin: '0 auto 48px' }}>
          <p className="eyebrow" style={{ color: '#8e2337' }}>
            Customer Adornment
          </p>
          <h2
            className="font-serif"
            style={{
              margin: '0 0 16px',
              fontSize: 'clamp(2rem, 3.2vw, 2.75rem)',
              fontWeight: 400,
              color: '#1e1917',
            }}
          >
            Loved by Women Across India
          </h2>
          <p style={{ fontSize: '14px', color: '#524a44', margin: 0 }}>
            Read real feedback from brides, bridesmaids, and festive shoppers who chose D &amp; Z Fashion.
          </p>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: '28px',
          }}
        >
          <div
            className="luxury-card"
            style={{
              padding: '32px 28px',
              borderRadius: '4px',
              backgroundColor: '#ffffff',
              border: '1px solid #ebd9c8',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
            }}
          >
            <div>
              <div style={{ display: 'flex', gap: '2px', color: '#c59b27', marginBottom: '16px' }}>
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
              </div>
              <p style={{ fontStyle: 'italic', fontSize: '15px', color: '#1e1917', lineHeight: 1.7, margin: '0 0 20px' }}>
                &ldquo;The Banarasi silk drape in Ruby Red exceeded all expectations! The gold zari doesn&apos;t feel stiff at all, and it fell so gracefully for my reception.&rdquo;
              </p>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', borderTop: '1px solid #f8f4ed', paddingTop: '16px' }}>
              <div
                style={{
                  width: '38px',
                  height: '38px',
                  borderRadius: '50%',
                  backgroundColor: '#faf0f2',
                  color: '#6d1a29',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 700,
                  fontSize: '14px',
                }}
              >
                PS
              </div>
              <div>
                <h5 style={{ margin: '0 0 2px', fontSize: '13px', fontWeight: 600, color: '#1e1917' }}>
                  Priya Sharma
                </h5>
                <p style={{ margin: 0, fontSize: '11px', color: '#857b72' }}>
                  Verified Buyer &bull; Surat, Gujarat
                </p>
              </div>
            </div>
          </div>

          <div
            className="luxury-card"
            style={{
              padding: '32px 28px',
              borderRadius: '4px',
              backgroundColor: '#ffffff',
              border: '1px solid #ebd9c8',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
            }}
          >
            <div>
              <div style={{ display: 'flex', gap: '2px', color: '#c59b27', marginBottom: '16px' }}>
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
              </div>
              <p style={{ fontStyle: 'italic', fontSize: '15px', color: '#1e1917', lineHeight: 1.7, margin: '0 0 20px' }}>
                &ldquo;Ordered the Blush Pink Georgette Lehenga for my friend&apos;s sangeet. Arrived in Mumbai in 48 hours. The mirror work shines brilliantly under festive lights.&rdquo;
              </p>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', borderTop: '1px solid #f8f4ed', paddingTop: '16px' }}>
              <div
                style={{
                  width: '38px',
                  height: '38px',
                  borderRadius: '50%',
                  backgroundColor: '#faf0f2',
                  color: '#6d1a29',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 700,
                  fontSize: '14px',
                }}
              >
                AK
              </div>
              <div>
                <h5 style={{ margin: '0 0 2px', fontSize: '13px', fontWeight: 600, color: '#1e1917' }}>
                  Ananya Kulkarni
                </h5>
                <p style={{ margin: 0, fontSize: '11px', color: '#857b72' }}>
                  Verified Buyer &bull; Mumbai, Maharashtra
                </p>
              </div>
            </div>
          </div>

          <div
            className="luxury-card"
            style={{
              padding: '32px 28px',
              borderRadius: '4px',
              backgroundColor: '#ffffff',
              border: '1px solid #ebd9c8',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
            }}
          >
            <div>
              <div style={{ display: 'flex', gap: '2px', color: '#c59b27', marginBottom: '16px' }}>
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
                <Star size={16} fill="#c59b27" />
              </div>
              <p style={{ fontStyle: 'italic', fontSize: '15px', color: '#1e1917', lineHeight: 1.7, margin: '0 0 20px' }}>
                &ldquo;Finding authentic Surat handlooms without inflated retail prices is rare. The Chanderi Anarkali kurti is breathable, regal, and the fit was spot on!&rdquo;
              </p>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', borderTop: '1px solid #f8f4ed', paddingTop: '16px' }}>
              <div
                style={{
                  width: '38px',
                  height: '38px',
                  borderRadius: '50%',
                  backgroundColor: '#faf0f2',
                  color: '#6d1a29',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 700,
                  fontSize: '14px',
                }}
              >
                MD
              </div>
              <div>
                <h5 style={{ margin: '0 0 2px', fontSize: '13px', fontWeight: 600, color: '#1e1917' }}>
                  Meera Dave
                </h5>
                <p style={{ margin: 0, fontSize: '11px', color: '#857b72' }}>
                  Verified Buyer &bull; Ahmedabad, Gujarat
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          7. BRAND VALUES / PILLARS OF EXCELLENCE
          ========================================================================= */}
      <section
        style={{
          padding: '64px clamp(20px, 6vw, 96px)',
          maxWidth: 'var(--container-max)',
          margin: '0 auto',
          borderTop: '1px solid #ebd9c8',
        }}
      >
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '32px',
          }}
        >
          <div style={{ padding: '28px', backgroundColor: '#ffffff', border: '1px solid #ebd9c8', borderRadius: '4px' }}>
            <div style={{ color: '#6d1a29', marginBottom: '14px' }}>
              <ShieldCheck size={30} />
            </div>
            <h3 className="font-serif" style={{ margin: '0 0 8px', fontSize: '1.2rem', color: '#1e1917' }}>
              Surat Manufacturing Heritage
            </h3>
            <p style={{ margin: 0, fontSize: '13px', color: '#524a44', lineHeight: 1.7 }}>
              Rooted in the textile capital of India, D &amp; Z Fashion maintains internal control over fabric selection,
              zari purity, precision cutting, and artisanal finishing.
            </p>
          </div>

          <div style={{ padding: '28px', backgroundColor: '#ffffff', border: '1px solid #ebd9c8', borderRadius: '4px' }}>
            <div style={{ color: '#6d1a29', marginBottom: '14px' }}>
              <Truck size={30} />
            </div>
            <h3 className="font-serif" style={{ margin: '0 0 8px', fontSize: '1.2rem', color: '#1e1917' }}>
              Pan-India Transport &amp; COD
            </h3>
            <p style={{ margin: 0, fontSize: '13px', color: '#524a44', lineHeight: 1.7 }}>
              Dispatched daily from Surat via Blue Dart and Delhivery. Cash on Delivery is supported across service-verified PIN codes.
            </p>
          </div>

          <div style={{ padding: '28px', backgroundColor: '#ffffff', border: '1px solid #ebd9c8', borderRadius: '4px' }}>
            <div style={{ color: '#6d1a29', marginBottom: '14px' }}>
              <RefreshCw size={30} />
            </div>
            <h3 className="font-serif" style={{ margin: '0 0 8px', fontSize: '1.2rem', color: '#1e1917' }}>
              3-Day Quality Assurance
            </h3>
            <p style={{ margin: 0, fontSize: '13px', color: '#524a44', lineHeight: 1.7 }}>
              Every delivered parcel carries our 3-day quality pledge for defect inspection and prompt exchange or store credit resolution.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
