import React from 'react';
import Link from 'next/link';
import { Package, RefreshCw, CreditCard, LifeBuoy, ArrowLeft } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';

export default function AccountPage() {
  return (
    <div style={{ maxWidth: 'var(--container-lg)', margin: '0 auto', padding: '48px clamp(20px, 4vw, 64px)' }}>
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
        <span>Back to Storefront</span>
      </Link>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '32px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
            <Badge variant="wine">Customer Account</Badge>
          </div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: 'var(--text-heading-xl)', fontWeight: 400 }}>
            Account Overview
          </h1>
        </div>
        <LinkButton href="/design-system" variant="outline" size="sm">
          Design System Demo
        </LinkButton>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '20px' }}>
        <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '8px', backgroundColor: 'var(--wine-soft)', color: 'var(--wine)', borderRadius: 'var(--radius-sm)' }}>
              <Package size={20} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>My Orders</h3>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-subtle)' }}>Track &amp; review purchases</p>
            </div>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px', lineHeight: 1.5 }}>
            View order progress (Confirmed &rarr; Processing &rarr; Shipped &rarr; Delivered) and download GST invoices.
          </p>
          <span style={{ fontSize: '12px', color: 'var(--wine)', fontWeight: 500 }}>Connected in Component 13 &rarr;</span>
        </div>

        <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '8px', backgroundColor: 'var(--wine-soft)', color: 'var(--wine)', borderRadius: 'var(--radius-sm)' }}>
              <RefreshCw size={20} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>3-Day Returns</h3>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-subtle)' }}>Quality assurance &amp; exchange</p>
            </div>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px', lineHeight: 1.5 }}>
            Request damaged/defective replacement or inspection within 3 calendar days from parcel delivery.
          </p>
          <span style={{ fontSize: '12px', color: 'var(--wine)', fontWeight: 500 }}>Connected in Component 13 &rarr;</span>
        </div>

        <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '8px', backgroundColor: 'var(--wine-soft)', color: 'var(--wine)', borderRadius: 'var(--radius-sm)' }}>
              <CreditCard size={20} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Store Credit</h3>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-subtle)' }}>Non-expiring wallet</p>
            </div>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px', lineHeight: 1.5 }}>
            Available balance never expires and can be seamlessly applied during checkout towards any order.
          </p>
          <span style={{ fontSize: '12px', color: 'var(--wine)', fontWeight: 500 }}>Balance: ₹0.00 &rarr;</span>
        </div>

        <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '8px', backgroundColor: 'var(--wine-soft)', color: 'var(--wine)', borderRadius: 'var(--radius-sm)' }}>
              <LifeBuoy size={20} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Support Tickets</h3>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-subtle)' }}>Help &amp; inquiries</p>
            </div>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px', lineHeight: 1.5 }}>
            Submit tickets for order inquiries or WhatsApp/phone assistance. Direct communication with staff.
          </p>
          <span style={{ fontSize: '12px', color: 'var(--wine)', fontWeight: 500 }}>Help Desk &rarr;</span>
        </div>
      </div>
    </div>
  );
}
