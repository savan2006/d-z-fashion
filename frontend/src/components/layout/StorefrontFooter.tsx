import React from 'react';
import Link from 'next/link';
import { BrandLogo } from '../brand/BrandLogo';
import { Award, Truck, RefreshCw, CreditCard, Sparkles } from 'lucide-react';

export const StorefrontFooter: React.FC = () => {
  return (
    <footer className="storefront-footer" style={{ borderTop: '2px solid #dfc15d' }}>
      {/* Upper Assurance Badges Strip */}
      <div
        style={{
          borderBottom: '1px solid rgba(223, 193, 93, 0.25)',
          paddingBottom: '48px',
          marginBottom: '56px',
        }}
      >
        <div
          style={{
            maxWidth: 'var(--container-max)',
            margin: '0 auto',
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: '28px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div
              style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: 'rgba(223, 193, 93, 0.12)',
                border: '1px solid #dfc15d',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#f6e8b1',
                flexShrink: 0,
              }}
            >
              <Award size={22} />
            </div>
            <div>
              <h5 style={{ margin: '0 0 2px', fontSize: '14px', color: '#ffffff', fontWeight: 600 }}>
                100% Surat Handloom
              </h5>
              <p style={{ margin: 0, fontSize: '12px', color: '#c4bcb2' }}>
                Authentic silk yarn &amp; artisan weave
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div
              style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: 'rgba(223, 193, 93, 0.12)',
                border: '1px solid #dfc15d',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#f6e8b1',
                flexShrink: 0,
              }}
            >
              <Truck size={22} />
            </div>
            <div>
              <h5 style={{ margin: '0 0 2px', fontSize: '14px', color: '#ffffff', fontWeight: 600 }}>
                Pan-India Free Shipping
              </h5>
              <p style={{ margin: 0, fontSize: '12px', color: '#c4bcb2' }}>
                On all prepaid orders over ₹2,000
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div
              style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: 'rgba(223, 193, 93, 0.12)',
                border: '1px solid #dfc15d',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#f6e8b1',
                flexShrink: 0,
              }}
            >
              <RefreshCw size={22} />
            </div>
            <div>
              <h5 style={{ margin: '0 0 2px', fontSize: '14px', color: '#ffffff', fontWeight: 600 }}>
                3-Day Quality Assurance
              </h5>
              <p style={{ margin: 0, fontSize: '12px', color: '#c4bcb2' }}>
                Hassle-free exchange &amp; store credit
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div
              style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: 'rgba(223, 193, 93, 0.12)',
                border: '1px solid #dfc15d',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#f6e8b1',
                flexShrink: 0,
              }}
            >
              <CreditCard size={22} />
            </div>
            <div>
              <h5 style={{ margin: '0 0 2px', fontSize: '14px', color: '#ffffff', fontWeight: 600 }}>
                Pan-India COD Available
              </h5>
              <p style={{ margin: 0, fontSize: '12px', color: '#c4bcb2' }}>
                Cash on delivery in verified PINs
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="footer-grid">
        {/* Brand Column */}
        <div>
          <div style={{ marginBottom: '20px' }}>
            <BrandLogo variant="wordmark" width={180} height={56} asLink={false} />
          </div>
          <p
            style={{
              fontSize: '13px',
              color: '#d4ccbf',
              lineHeight: 1.8,
              maxWidth: '360px',
              margin: '0 0 20px',
            }}
          >
            D &amp; Z Fashion represents the royal silk weaving heritage of Surat, Gujarat.
            Every saree, lehenga and Anarkali kurti is patterned, woven and finished with
            impeccable craftsmanship and delivered directly to women across India.
          </p>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
              padding: '6px 14px',
              backgroundColor: 'rgba(223, 193, 93, 0.12)',
              border: '1px solid rgba(223, 193, 93, 0.35)',
              borderRadius: '2px',
              fontSize: '11px',
              color: '#f6e8b1',
              letterSpacing: '0.06em',
              textTransform: 'uppercase',
            }}
          >
            <Sparkles size={13} color="#dfc15d" />
            <span>Surat, Gujarat &bull; Authentic Quality</span>
          </div>
        </div>

        {/* Collections Links */}
        <div>
          <h4 className="footer-heading" style={{ color: '#f6e8b1', letterSpacing: '0.08em' }}>
            Ethnic Collections
          </h4>
          <ul className="footer-links">
            <li><Link href="/collections?sort=createdAt,desc">New Arrivals</Link></li>
            <li><Link href="/collections?category=indian-wear">Indian Wear</Link></li>
            <li><Link href="/collections?category=banarasi-sarees">Banarasi Sarees</Link></li>
            <li><Link href="/collections?category=kanjeevaram-sarees">Kanjeevaram Silk Sarees</Link></li>
            <li><Link href="/collections?category=georgette-lehengas">Georgette Lehengas</Link></li>
            <li><Link href="/collections?category=anarkali-kurtis">Anarkali Kurtis</Link></li>
            <li><Link href="/collections?category=girls-ethnic-wear">Girls Ethnic Wear</Link></li>
            <li><Link href="/collections">All Collections</Link></li>
          </ul>
        </div>

        {/* Customer Care */}
        <div>
          <h4 className="footer-heading" style={{ color: '#f6e8b1', letterSpacing: '0.08em' }}>
            Customer Experience
          </h4>
          <ul className="footer-links">
            <li><Link href="/account/orders">Track Orders</Link></li>
            <li><Link href="/support/shipping">Shipping &amp; COD Coverage</Link></li>
            <li><Link href="/support/returns">3-Day Return &amp; Exchange</Link></li>
            <li><Link href="/support">Support &amp; Contact</Link></li>
            <li><Link href="/design-system">Brand Design System</Link></li>
            <li><Link href="/privacy">Privacy Policy</Link></li>
            <li><Link href="/terms">Terms of Service</Link></li>
          </ul>
        </div>

        {/* Newsletter & Atelier Club */}
        <div>
          <h4 className="footer-heading" style={{ color: '#f6e8b1', letterSpacing: '0.08em' }}>
            The D&amp;Z Atelier Club
          </h4>
          <p
            style={{
              fontSize: '12px',
              color: '#c4bcb2',
              lineHeight: 1.6,
              marginBottom: '16px',
            }}
          >
            Subscribe to receive private previews of our new silk drops, wedding season edits, and an instant <strong style={{ color: '#f6e8b1' }}>₹200 discount voucher</strong> for your first order.
          </p>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
            <input
              type="email"
              placeholder="Enter your email address"
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.08)',
                border: '1px solid rgba(223, 193, 93, 0.4)',
                borderRadius: '2px',
                padding: '8px 12px',
                color: '#ffffff',
                fontSize: '12px',
                flex: 1,
                outline: 'none',
              }}
            />
            <button
              type="button"
              className="btn btn-sm"
              style={{
                backgroundColor: '#dfc15d',
                color: '#3d0a14',
                fontWeight: 700,
                padding: '8px 14px',
                border: 'none',
              }}
            >
              Join
            </button>
          </div>
          <p style={{ margin: 0, fontSize: '11px', color: '#9c9489' }}>
            Surat Central Atelier &bull; Ring Road Textile Market, Surat, Gujarat - 395002
          </p>
        </div>
      </div>

      <div className="footer-bottom">
        <div>&copy; {new Date().getFullYear()} D &amp; Z Fashion Surat. All rights reserved. Handcrafted in India.</div>
        <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
          <Link href="/privacy" style={{ color: '#9c9489' }}>Privacy Policy</Link>
          <Link href="/terms" style={{ color: '#9c9489' }}>Terms of Service</Link>
          <Link href="/design-system" style={{ color: '#dfc15d' }}>Design System</Link>
        </div>
      </div>
    </footer>
  );
};
