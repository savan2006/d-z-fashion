import React from 'react';
import Link from 'next/link';
import { BrandLogo } from '../brand/BrandLogo';

export const StorefrontFooter: React.FC = () => {
  return (
    <footer className="storefront-footer">
      <div className="footer-grid">
        <div>
          <div style={{ marginBottom: '16px' }}>
            <BrandLogo variant="wordmark" width={160} height={50} asLink={false} />
          </div>
          <p
            style={{
              fontSize: 'var(--text-body-sm)',
              color: '#c4bcb2',
              lineHeight: 1.7,
              maxWidth: '360px',
              margin: '0 0 16px',
            }}
          >
            A considered wardrobe for every day. Designed with restraint, crafted in Surat,
            and dispatched across India with complete care.
          </p>
          <p style={{ fontSize: '11px', color: '#9c9489', letterSpacing: '0.04em' }}>
            Ready stock inventory managed directly at our Surat warehouse facility.
          </p>
        </div>

        <div>
          <h4 className="footer-heading">Collections</h4>
          <ul className="footer-links">
            <li><Link href="/collections?cat=new">New Arrivals</Link></li>
            <li><Link href="/collections?cat=indian">Indian Wear</Link></li>
            <li><Link href="/collections?cat=western">Western Wear</Link></li>
            <li><Link href="/collections?cat=dresses">Dresses &amp; Co-ords</Link></li>
            <li><Link href="/collections?cat=essentials">Everyday Essentials</Link></li>
          </ul>
        </div>

        <div>
          <h4 className="footer-heading">Customer Care</h4>
          <ul className="footer-links">
            <li><Link href="/support">Support &amp; Contact</Link></li>
            <li><Link href="/support/returns">3-Day Return &amp; Exchange</Link></li>
            <li><Link href="/support/shipping">Shipping &amp; COD Coverage</Link></li>
            <li><Link href="/account/orders">Track Orders</Link></li>
            <li><Link href="/design-system">Brand Design System</Link></li>
          </ul>
        </div>

        <div>
          <h4 className="footer-heading">Surat Dispatch &amp; Assurance</h4>
          <p
            style={{
              fontSize: 'var(--text-body-xs)',
              color: '#c4bcb2',
              lineHeight: 1.6,
              marginBottom: '12px',
            }}
          >
            Orders over ₹2,000 enjoy complimentary delivery throughout India. Cash on Delivery
            supported in eligible PIN codes.
          </p>
          <div
            style={{
              display: 'inline-flex',
              padding: '8px 12px',
              backgroundColor: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '11px',
              color: '#ded5ca',
            }}
          >
            Surat, Gujarat &bull; Authentic Quality
          </div>
        </div>
      </div>

      <div className="footer-bottom">
        <div>&copy; {new Date().getFullYear()} D &amp; Z Fashion. All rights reserved.</div>
        <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
          <Link href="/privacy" style={{ color: '#9c9489' }}>Privacy Policy</Link>
          <Link href="/terms" style={{ color: '#9c9489' }}>Terms of Service</Link>
          <Link href="/design-system" style={{ color: 'var(--rose)' }}>UI Styleguide</Link>
        </div>
      </div>
    </footer>
  );
};
