import React from 'react';
import Link from 'next/link';
import { BrandLogo } from '@/components/brand/BrandLogo';
import { ArrowLeft } from 'lucide-react';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: 'var(--paper)',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Top Banner */}
      <header
        style={{
          padding: '20px clamp(20px, 4vw, 48px)',
          borderBottom: '1px solid var(--line)',
          backgroundColor: '#ffffff',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <BrandLogo variant="wordmark" width={150} height={48} />
        <Link
          href="/"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '13px',
            color: 'var(--ink-muted)',
            textDecoration: 'none',
          }}
        >
          <ArrowLeft size={14} />
          <span>Return to Store</span>
        </Link>
      </header>

      {/* Main Content Area */}
      <main
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '40px 20px',
        }}
      >
        <div
          style={{
            width: '100%',
            maxWidth: '460px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '36px clamp(20px, 4vw, 36px)',
            boxShadow: 'var(--shadow-card)',
          }}
        >
          {children}
        </div>
      </main>

      {/* Footer */}
      <footer
        style={{
          padding: '16px',
          textAlign: 'center',
          fontSize: '12px',
          color: 'var(--ink-subtle)',
          borderTop: '1px solid var(--line)',
        }}
      >
        D &amp; Z Fashion Surat &bull; Secure Customer Authentication
      </footer>
    </div>
  );
}
