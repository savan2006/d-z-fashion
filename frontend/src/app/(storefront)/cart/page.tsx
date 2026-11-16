import React from 'react';
import Link from 'next/link';
import { ShoppingBag, ArrowLeft } from 'lucide-react';
import { EmptyState } from '@/components/ui/EmptyState';
import { LinkButton } from '@/components/ui/Button';

export default function CartPage() {
  return (
    <div style={{ maxWidth: 'var(--container-md)', margin: '0 auto', padding: '48px clamp(20px, 4vw, 48px)' }}>
      <Link
        href="/"
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          fontSize: '12px',
          color: 'var(--ink-muted)',
          marginBottom: '24px',
        }}
      >
        <ArrowLeft size={14} />
        <span>Continue Shopping</span>
      </Link>

      <h1 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)', fontWeight: 400 }}>
        Shopping Bag
      </h1>

      <EmptyState
        icon={<ShoppingBag size={32} strokeWidth={1.5} />}
        title="Your shopping bag is empty"
        description="Thoughtful clothing made for everyday movement. Explore the seasonal edit and find pieces that feel like your own."
        action={
          <LinkButton href="/collections" variant="primary" size="md">
            Discover Collections
          </LinkButton>
        }
      />
    </div>
  );
}
