'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { BrandLogo } from '../brand/BrandLogo';
import { ShoppingBag, User, Search, Heart, Menu, X } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';

const NAV_ITEMS = [
  { label: 'New Arrivals', href: '/collections?cat=new' },
  { label: 'Indian Wear', href: '/collections?cat=indian' },
  { label: 'Western Wear', href: '/collections?cat=western' },
  { label: 'Dresses', href: '/collections?cat=dresses' },
  { label: 'Everyday Essentials', href: '/collections?cat=essentials' },
];

export const StorefrontHeader: React.FC = () => {
  const pathname = usePathname();
  const { user, isAuthenticated } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <>
      <div className="announcement-bar">
        <span>A considered wardrobe for every day &bull; Surat Craftsmanship &bull; Pan-India Shipping</span>
      </div>

      <header className="storefront-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <button
            type="button"
            onClick={() => setMobileMenuOpen(true)}
            className="btn btn-ghost"
            style={{ padding: '6px', minHeight: 'auto', display: 'none' }}
            id="mobile-nav-toggle"
            aria-label="Open navigation menu"
          >
            <Menu size={22} />
          </button>
          <BrandLogo variant="wordmark" width={164} height={52} priority />
        </div>

        <nav className="storefront-nav" aria-label="Main Storefront Categories">
          {NAV_ITEMS.map((item) => {
            const isActive = pathname === item.href;
            return (
              <Link key={item.label} href={item.href} className={isActive ? 'active' : ''}>
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="storefront-actions">
          <Link
            href="/search"
            className="storefront-action-link"
            aria-label="Search collection"
          >
            <Search size={18} />
            <span style={{ fontSize: '11px', letterSpacing: '0.04em' }}>Search</span>
          </Link>

          <Link
            href="/account/wishlist"
            className="storefront-action-link"
            aria-label="View wishlist"
          >
            <Heart size={18} />
          </Link>

          <Link
            href="/cart"
            className="storefront-action-link"
            aria-label="Shopping bag"
          >
            <ShoppingBag size={18} />
            <span style={{ fontSize: '11px', letterSpacing: '0.04em' }}>Bag</span>
          </Link>

          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Link
                href="/account"
                className="storefront-action-link"
                style={{ fontWeight: 600, color: 'var(--wine)' }}
              >
                <User size={18} />
                <span>{user?.fullName?.split(' ')[0] || user?.username}</span>
              </Link>
              {user?.role === 'OWNER' && (
                <Link
                  href="/admin"
                  className="badge badge-wine"
                  style={{ textDecoration: 'none' }}
                >
                  Admin
                </Link>
              )}
              {user?.role === 'MANAGER' && (
                <Link
                  href="/manager"
                  className="badge badge-success"
                  style={{ textDecoration: 'none' }}
                >
                  Manager
                </Link>
              )}
            </div>
          ) : (
            <Link
              href="/login"
              className="storefront-action-link"
              style={{ fontWeight: 500 }}
            >
              <User size={18} />
              <span>Sign In</span>
            </Link>
          )}
        </div>
      </header>

      {/* Mobile Drawer Navigation */}
      {mobileMenuOpen && (
        <div
          className="dialog-backdrop"
          style={{ justifyContent: 'flex-start', padding: 0 }}
          onClick={() => setMobileMenuOpen(false)}
        >
          <div
            style={{
              width: '80%',
              maxWidth: '320px',
              height: '100vh',
              backgroundColor: '#ffffff',
              padding: '24px 20px',
              display: 'flex',
              flexDirection: 'column',
              boxShadow: 'var(--shadow-dialog)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
              <BrandLogo variant="wordmark" width={140} height={44} />
              <button
                type="button"
                onClick={() => setMobileMenuOpen(false)}
                className="btn btn-ghost"
                style={{ padding: '6px', minHeight: 'auto' }}
                aria-label="Close menu"
              >
                <X size={20} />
              </button>
            </div>

            <nav style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <p className="eyebrow" style={{ marginBottom: '8px' }}>Collections</p>
              {NAV_ITEMS.map((item) => (
                <Link
                  key={item.label}
                  href={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  style={{ fontSize: '15px', color: 'var(--ink)', padding: '6px 0' }}
                >
                  {item.label}
                </Link>
              ))}

              <div style={{ borderTop: '1px solid var(--line)', margin: '16px 0', paddingTop: '16px' }}>
                <p className="eyebrow" style={{ marginBottom: '8px' }}>Account &amp; Quick Links</p>
                <Link
                  href="/account"
                  onClick={() => setMobileMenuOpen(false)}
                  style={{ display: 'block', padding: '8px 0', fontSize: '14px', color: 'var(--ink)' }}
                >
                  My Account
                </Link>
                <Link
                  href="/cart"
                  onClick={() => setMobileMenuOpen(false)}
                  style={{ display: 'block', padding: '8px 0', fontSize: '14px', color: 'var(--ink)' }}
                >
                  Shopping Bag
                </Link>
                <Link
                  href="/design-system"
                  onClick={() => setMobileMenuOpen(false)}
                  style={{ display: 'block', padding: '8px 0', fontSize: '14px', color: 'var(--wine)' }}
                >
                  Design System Showcase
                </Link>
              </div>
            </nav>
          </div>
        </div>
      )}
    </>
  );
};
