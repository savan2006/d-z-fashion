'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  customerApi,
  UserProfile,
  OrderSummary,
  StoreCreditBalanceResponse,
} from '@/lib/api/customer';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  Package,
  MapPin,
  RefreshCw,
  ArrowRight,
  ShieldCheck,
} from 'lucide-react';

function getOrderStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'DELIVERED':
      return 'success';
    case 'DISPATCHED':
      return 'neutral';
    case 'PROCESSING':
    case 'CONFIRMED':
    case 'PENDING_APPROVAL':
      return 'warning';
    case 'CANCELLED':
    case 'RETURNED':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function AccountOverviewPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [recentOrders, setRecentOrders] = useState<OrderSummary[]>([]);
  const [storeCredit, setStoreCredit] = useState<StoreCreditBalanceResponse | null>(null);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      customerApi.getProfile().catch(() => null),
      customerApi.getMyOrders(0, 3).catch(() => null),
      customerApi.getStoreCreditBalance().catch(() => null),
    ]).then(([profileRes, ordersRes, creditRes]) => {
      if (!isMounted) return;
      if (profileRes && profileRes.data) setProfile(profileRes.data);
      if (ordersRes && ordersRes.data) setRecentOrders(ordersRes.data.content || []);
      if (creditRes && creditRes.data) setStoreCredit(creditRes.data);
      setLoading(false);
    });

    return () => {
      isMounted = false;
    };
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <Skeleton height="140px" />
        <Skeleton height="200px" />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Overview Cards Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
        {/* Store Credit Card */}
        <div
          style={{
            padding: '20px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span style={{ fontSize: '12px', color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Store Credit
              </span>
              <Badge variant="wine">Never Expires</Badge>
            </div>
            <div className="font-serif" style={{ fontSize: '28px', color: 'var(--wine)', fontWeight: 500 }}>
              ₹{storeCredit?.currentBalance ? storeCredit.currentBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
            </div>
          </div>
          <Link
            href="/account/store-credit"
            style={{
              marginTop: '16px',
              fontSize: '12px',
              color: 'var(--wine)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              textDecoration: 'none',
              fontWeight: 500,
            }}
          >
            <span>View credit ledger</span>
            <ArrowRight size={12} />
          </Link>
        </div>

        {/* Orders Count Card */}
        <div
          style={{
            padding: '20px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span style={{ fontSize: '12px', color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Orders
              </span>
              <Package size={16} color="var(--ink-muted)" />
            </div>
            <div className="font-serif" style={{ fontSize: '28px', color: 'var(--ink)', fontWeight: 500 }}>
              {recentOrders.length > 0 ? recentOrders.length : 0}
            </div>
          </div>
          <Link
            href="/account/orders"
            style={{
              marginTop: '16px',
              fontSize: '12px',
              color: 'var(--wine)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              textDecoration: 'none',
              fontWeight: 500,
            }}
          >
            <span>Manage all orders</span>
            <ArrowRight size={12} />
          </Link>
        </div>

        {/* Quality Returns Card */}
        <div
          style={{
            padding: '20px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span style={{ fontSize: '12px', color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Returns &amp; Exchanges
              </span>
              <RefreshCw size={16} color="var(--ink-muted)" />
            </div>
            <div style={{ fontSize: '14px', color: 'var(--ink)', marginTop: '4px', lineHeight: 1.4 }}>
              3-day window from parcel delivery with Surat quality inspection.
            </div>
          </div>
          <Link
            href="/account/returns"
            style={{
              marginTop: '16px',
              fontSize: '12px',
              color: 'var(--wine)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              textDecoration: 'none',
              fontWeight: 500,
            }}
          >
            <span>Create return request</span>
            <ArrowRight size={12} />
          </Link>
        </div>
      </div>

      {/* Primary Saved Address Card */}
      <div
        style={{
          padding: '24px',
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <MapPin size={18} color="var(--wine)" />
            <h2 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>Primary Saved Address</h2>
          </div>
          <LinkButton href="/account/profile" variant="outline" size="sm">
            {profile?.addressLine1 ? 'Edit Address' : 'Add Address'}
          </LinkButton>
        </div>

        {profile?.addressLine1 ? (
          <div style={{ fontSize: '13px', lineHeight: 1.6, color: 'var(--ink-secondary)' }}>
            <p style={{ margin: 0, fontWeight: 600, color: 'var(--ink)' }}>{profile.fullName || user?.fullName}</p>
            {profile.phoneNumber && <p style={{ margin: 0 }}>Phone: +91 {profile.phoneNumber}</p>}
            <p style={{ margin: 0 }}>{profile.addressLine1}</p>
            {profile.addressLine2 && <p style={{ margin: 0 }}>{profile.addressLine2}</p>}
            <p style={{ margin: 0 }}>
              {profile.city}, {profile.state} - {profile.postalCode}
            </p>
            <div style={{ marginTop: '12px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: 'var(--ink-muted)' }}>
              <ShieldCheck size={14} color="var(--wine)" />
              <span>Used automatically as your default delivery address during 1-click checkout.</span>
            </div>
          </div>
        ) : (
          <div style={{ padding: '16px 0', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No delivery address saved yet. Save an address in your profile for fast, frictionless checkout.
          </div>
        )}
      </div>

      {/* Recent Orders Section */}
      <div
        style={{
          padding: '24px',
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <h2 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>Recent Purchases</h2>
          <Link
            href="/account/orders"
            style={{ fontSize: '12px', color: 'var(--wine)', textDecoration: 'none', fontWeight: 500 }}
          >
            View all orders &rarr;
          </Link>
        </div>

        {recentOrders.length === 0 ? (
          <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            <p style={{ margin: '0 0 16px' }}>You haven&apos;t placed any orders yet.</p>
            <LinkButton href="/collections" variant="primary" size="sm">
              Explore Surat Designer Collections
            </LinkButton>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {recentOrders.map((order) => (
              <div
                key={order.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '14px 16px',
                  backgroundColor: 'var(--surface-subtle)',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid var(--line)',
                  flexWrap: 'wrap',
                  gap: '12px',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
                    <span style={{ fontWeight: 600, fontSize: '13px' }}>#{order.orderNumber}</span>
                    <Badge variant={getOrderStatusVariant(order.orderStatus)}>
                      {order.orderStatus.replace('_', ' ')}
                    </Badge>
                  </div>
                  <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
                    {new Date(order.createdAt).toLocaleDateString('en-IN', {
                      day: 'numeric',
                      month: 'short',
                      year: 'numeric',
                    })}{' '}
                    &bull; {order.totalItems} {order.totalItems === 1 ? 'item' : 'items'}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontWeight: 600, fontSize: '14px', color: 'var(--wine)' }}>
                      ₹{order.totalPayable.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </div>
                    <span style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                      {order.paymentMethod === 'CASH_ON_DELIVERY' ? 'COD' : 'Prepaid'}
                    </span>
                  </div>
                  <LinkButton href={`/account/orders/${order.orderNumber}`} variant="outline" size="sm">
                    View
                  </LinkButton>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
