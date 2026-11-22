'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { BrandLogo } from '../brand/BrandLogo';
import { ShoppingBag, User, Search, Heart, Menu, X, ChevronRight, Phone } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { catalogApi, Category } from '@/lib/api/catalog';

const FALLBACK_NAV_ITEMS = [
  { label: 'All Collections', href: '/collections' },
  { label: 'New Arrivals', href: '/collections?sort=createdAt,desc', tag: 'NEW' },
  { label: 'Indian Wear', href: '/collections?category=indian-wear', tag: 'POPULAR' },
  { label: 'Banarasi Sarees', href: '/collections?category=banarasi-sarees' },
  { label: 'Designer Lehengas', href: '/collections?category=georgette-lehengas', tag: 'BRIDAL' },
  { label: 'Anarkali Kurtis', href: '/collections?category=anarkali-kurtis' },
  { label: 'Girls Ethnic', href: '/collections?category=girls-ethnic-wear' },
];

export const StorefrontHeader: React.FC = () => {
  const pathname = usePathname();
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [navItems, setNavItems] = useState(FALLBACK_NAV_ITEMS);

  useEffect(() => {
    let isMounted = true;
    catalogApi
      .getCategories(false)
      .then((res) => {
        if (isMounted && res.data && res.data.length > 0) {
          const dynamicItems = res.data
            .filter((c: Category) => c.active)
            .slice(0, 6)
            .map((c: Category) => ({
              label: c.name,
              href: `/collections?category=${encodeURIComponent(c.slug)}`,
              tag: c.slug.includes('saree') || c.slug.includes('lehenga') ? 'FESTIVE' : undefined,
            }));
          if (dynamicItems.length > 0) {
            setNavItems([
              { label: 'All Collections', href: '/collections' },
              { label: 'New Arrivals', href: '/collections?sort=createdAt,desc', tag: 'NEW' },
              { label: 'Indian Wear', href: '/collections?category=indian-wear', tag: 'POPULAR' },
              ...dynamicItems,
            ]);
          }
        }
      })
      .catch(() => {
        // Fallback to static items
      });

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <>
      {/* Royal Gold & Wine Announcement Bar */}
      <div className="announcement-bar">
        <span>
          ✨ Festive Collection 2026 &bull; Surat Craftsmanship &bull; Complimentary Pan-India Shipping on Orders ₹2,000+ &bull; Use Code <strong style={{ color: '#f6e8b1', letterSpacing: '0.12em' }}>SURATFIRST</strong>
        </span>
      </div>

      <header className="storefront-header" style={{ borderBottom: '1px solid #ebd9c8', boxShadow: '0 4px 18px rgba(45, 20, 26, 0.03)' }}>
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
          <BrandLogo variant="wordmark" width={180} height={54} priority />
        </div>

        {/* Desktop Category Navigation */}
        <nav className="storefront-nav" aria-label="Main Storefront Categories">
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.label}
                href={item.href}
                className={isActive ? 'active' : ''}
                style={{
                  fontSize: '13px',
                  fontWeight: 500,
                  letterSpacing: '0.04em',
                  position: 'relative',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <span>{item.label}</span>
                {item.tag && (
                  <span
                    style={{
                      fontSize: '9px',
                      fontWeight: 700,
                      backgroundColor: item.tag === 'NEW' ? '#0f4c3a' : '#6d1a29',
                      color: '#ffffff',
                      padding: '1px 5px',
                      borderRadius: '2px',
                      letterSpacing: '0.06em',
                      lineHeight: 1.2,
                    }}
                  >
                    {item.tag}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>

        {/* Header Action Tools */}
        <div className="storefront-actions">
          <button
            type="button"
            onClick={() => setSearchOpen(!searchOpen)}
            className="storefront-action-link"
            aria-label="Search collection"
            style={{ background: 'none', border: 'none', cursor: 'pointer' }}
          >
            <Search size={19} color="#524a44" />
            <span style={{ fontSize: '12px', letterSpacing: '0.03em', fontWeight: 500 }}>Search</span>
          </button>

          <Link
            href="/account/wishlist"
            className="storefront-action-link"
            aria-label="View wishlist"
          >
            <Heart size={19} color="#524a44" />
          </Link>

          <Link
            href="/cart"
            className="storefront-action-link"
            aria-label="Shopping bag"
            style={{ position: 'relative' }}
          >
            <ShoppingBag size={20} color="#6d1a29" />
            <span
              style={{
                fontSize: '12px',
                letterSpacing: '0.04em',
                fontWeight: 600,
                color: '#6d1a29',
              }}
            >
              Bag
            </span>
          </Link>

          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Link
                href="/account"
                className="storefront-action-link"
                style={{ fontWeight: 600, color: '#6d1a29' }}
              >
                <User size={19} />
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
              className="btn btn-sm btn-primary"
              style={{
                borderRadius: '3px',
                padding: '6px 14px',
                fontSize: '12px',
                fontWeight: 600,
                letterSpacing: '0.04em',
              }}
            >
              <User size={14} />
              <span>Sign In</span>
            </Link>
          )}
        </div>
      </header>

      {/* Interactive Search Overlay Drawer */}
      {searchOpen && (
        <div
          style={{
            backgroundColor: '#ffffff',
            borderBottom: '2px solid #dfc15d',
            padding: '20px clamp(20px, 6vw, 96px)',
            boxShadow: '0 12px 28px rgba(0,0,0,0.08)',
            position: 'relative',
            zIndex: 35,
          }}
        >
          <div style={{ maxWidth: '800px', margin: '0 auto' }}>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (searchQuery.trim()) {
                  router.push(`/collections?q=${encodeURIComponent(searchQuery.trim())}`);
                  setSearchOpen(false);
                }
              }}
              style={{ display: 'flex', alignItems: 'center', gap: '12px' }}
            >
              <Search size={22} color="#6d1a29" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search Banarasi sarees, bridal lehengas, Anarkali kurtis, fabrics..."
                style={{
                  flex: 1,
                  border: 'none',
                  fontSize: '1.1rem',
                  outline: 'none',
                  color: '#1e1917',
                  fontFamily: 'var(--font-sans)',
                }}
                autoFocus
              />
              <button
                type="submit"
                className="btn btn-sm btn-primary"
                style={{ padding: '8px 18px' }}
              >
                Search
              </button>
              <button
                type="button"
                onClick={() => setSearchOpen(false)}
                className="btn btn-ghost"
                style={{ padding: '6px' }}
                aria-label="Close search"
              >
                <X size={20} />
              </button>
            </form>

            {/* Trending Ladies Wear Search Suggestions */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '14px', flexWrap: 'wrap' }}>
              <span style={{ fontSize: '11px', color: '#857b72', fontWeight: 600, textTransform: 'uppercase' }}>
                Trending Searches:
              </span>
              {['Banarasi Silk', 'Kanjeevaram Saree', 'Georgette Lehenga', 'Chanderi Kurti', 'Bandhani', 'Ruby Red'].map(
                (term) => (
                  <Link
                    key={term}
                    href={`/collections?q=${encodeURIComponent(term)}`}
                    onClick={() => setSearchOpen(false)}
                    style={{
                      fontSize: '11px',
                      color: '#6d1a29',
                      backgroundColor: '#faf0f2',
                      padding: '3px 8px',
                      borderRadius: '2px',
                      border: '1px solid #ebd5dd',
                    }}
                  >
                    {term}
                  </Link>
                )
              )}
            </div>
          </div>
        </div>
      )}

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(30, 25, 23, 0.6)',
            backdropFilter: 'blur(4px)',
            zIndex: 60,
            display: 'flex',
          }}
          onClick={() => setMobileMenuOpen(false)}
        >
          <div
            style={{
              width: '85%',
              maxWidth: '360px',
              backgroundColor: '#ffffff',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              padding: '24px',
              boxShadow: '0 0 30px rgba(0,0,0,0.25)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
              <BrandLogo variant="wordmark" width={150} height={46} />
              <button
                type="button"
                onClick={() => setMobileMenuOpen(false)}
                className="btn btn-ghost"
                style={{ padding: '6px' }}
                aria-label="Close menu"
              >
                <X size={22} />
              </button>
            </div>

            <div style={{ borderTop: '1px solid #ebd9c8', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
              {navItems.map((item) => (
                <Link
                  key={item.label}
                  href={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 8px',
                    fontSize: '15px',
                    color: '#1e1917',
                    fontWeight: 500,
                    borderBottom: '1px solid #f8f4ed',
                  }}
                >
                  <span>{item.label}</span>
                  <ChevronRight size={16} color="#857b72" />
                </Link>
              ))}
            </div>

            <div style={{ marginTop: 'auto', borderTop: '1px solid #ebd9c8', paddingTop: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', color: '#6d1a29', marginBottom: '12px' }}>
                <Phone size={14} />
                <span>Atelier Support: Surat, Gujarat</span>
              </div>
              <p style={{ margin: 0, fontSize: '11px', color: '#857b72' }}>
                &copy; {new Date().getFullYear()} D &amp; Z Fashion Surat.
              </p>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
