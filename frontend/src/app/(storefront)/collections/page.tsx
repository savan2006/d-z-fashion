import React from 'react';
import Link from 'next/link';
import { Sparkles, ArrowLeft } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/ui/EmptyState';
import { LinkButton } from '@/components/ui/Button';

export default function CollectionsPage() {
  return (
    <div style={{ maxWidth: 'var(--container-max)', margin: '0 auto', padding: '40px clamp(20px, 4vw, 64px)' }}>
      <div style={{ marginBottom: '32px' }}>
        <Link
          href="/"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '12px',
            color: 'var(--ink-muted)',
            marginBottom: '16px',
          }}
        >
          <ArrowLeft size={14} />
          <span>Back to Home</span>
        </Link>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
          <Badge variant="wine" icon={<Sparkles size={12} />}>Catalog Foundation</Badge>
        </div>
        <h1 className="font-serif" style={{ margin: '0 0 8px', fontSize: 'var(--text-display-md)', fontWeight: 400 }}>
          Curated Garment Collections
        </h1>
        <p style={{ margin: 0, color: 'var(--ink-muted)', fontSize: 'var(--text-body-md)' }}>
          Browsing women’s and girls’ clothing crafted in Surat with quality ready stock.
        </p>
      </div>

      <div
        style={{
          display: 'flex',
          gap: '12px',
          overflowX: 'auto',
          paddingBottom: '16px',
          marginBottom: '32px',
          borderBottom: '1px solid var(--line)',
        }}
      >
        <button className="btn btn-primary btn-sm">All Styles</button>
        <button className="btn btn-secondary btn-sm">New Arrivals</button>
        <button className="btn btn-secondary btn-sm">Indian Wear</button>
        <button className="btn btn-secondary btn-sm">Western Wear</button>
        <button className="btn btn-secondary btn-sm">Dresses</button>
        <button className="btn btn-secondary btn-sm">Everyday Essentials</button>
      </div>

      <EmptyState
        title="Active Catalog Integration in Progress"
        description="The editorial design shell is ready. Live product listing grid, filters (fabric, color, size, price) and stock cards connect in Component 12."
        action={
          <div style={{ display: 'flex', gap: '12px' }}>
            <LinkButton href="/design-system" variant="outline" size="sm">
              Explore Design System
            </LinkButton>
            <LinkButton href="/" variant="primary" size="sm">
              Return to Storefront
            </LinkButton>
          </div>
        }
      />
    </div>
  );
}
