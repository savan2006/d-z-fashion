'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth/AuthContext';
import { customerApi, StoreCreditBalanceResponse } from '@/lib/api/customer';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import {
  Package,
  RefreshCw,
  CreditCard,
  Bell,
  MapPin,
  LogOut,
  LayoutDashboard,
} from 'lucide-react';

interface NavItem {
  label: string;
  href: string;
  icon: React.ReactNode;
  badge?: number | string;
}

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [storeCredit, setStoreCredit] = useState<StoreCreditBalanceResponse | null>(null);

  useEffect(() => {
    if (!isAuthenticated && !isLoading) {
      router.push(`/login?redirect=${encodeURIComponent(pathname || '/account')}`);
      return;
    }

    if (isAuthenticated) {
      customerApi.getUnreadNotificationCount()
        .then((res) => {
          if (res.data) setUnreadCount(res.data.unreadCount || 0);
        })
        .catch(() => {});

      customerApi.getStoreCreditBalance()
        .then((res) => {
          if (res.data) setStoreCredit(res.data);
        })
        .catch(() => {});
    }
  }, [isAuthenticated, isLoading, router, pathname]);

  const handleLogout = () => {
    logout();
    router.push('/');
  };

  if (isLoading || !isAuthenticated) {
    return (
      <div style={{ maxWidth: 'var(--container-lg)', margin: '0 auto', padding: '64px 24px', textAlign: 'center' }}>
        <p style={{ color: 'var(--ink-muted)', fontSize: '14px' }}>Loading your account details...</p>
      </div>
    );
  }

  const navItems: NavItem[] = [
    { label: 'Overview', href: '/account', icon: <LayoutDashboard size={16} /> },
    { label: 'My Orders', href: '/account/orders', icon: <Package size={16} /> },
    { label: '3-Day Returns', href: '/account/returns', icon: <RefreshCw size={16} /> },
    {
      label: 'Store Credit',
      href: '/account/store-credit',
      icon: <CreditCard size={16} />,
      badge: storeCredit && storeCredit.currentBalance > 0 ? `₹${storeCredit.currentBalance.toLocaleString('en-IN')}` : undefined,
    },
    {
      label: 'Notifications',
      href: '/account/notifications',
      icon: <Bell size={16} />,
      badge: unreadCount > 0 ? unreadCount : undefined,
    },
    { label: 'Saved Address & Profile', href: '/account/profile', icon: <MapPin size={16} /> },
  ];

  return (
    <div style={{ maxWidth: 'var(--container-xl)', margin: '0 auto', padding: '32px clamp(16px, 4vw, 48px)' }}>
      {/* Top Banner / Account Header */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
          padding: '24px 28px',
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          marginBottom: '28px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: 'var(--wine-soft)',
              color: 'var(--wine)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 600,
              fontSize: '18px',
            }}
          >
            {user?.fullName?.charAt(0).toUpperCase() || user?.username?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 className="font-serif" style={{ margin: 0, fontSize: '20px', fontWeight: 500 }}>
                {user?.fullName || user?.username}
              </h1>
              <Badge variant="wine">{user?.role || 'CUSTOMER'}</Badge>
            </div>
            <p style={{ margin: '2px 0 0', fontSize: '12px', color: 'var(--ink-muted)' }}>
              {user?.email}
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {storeCredit && (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'flex-end',
                paddingRight: '12px',
                borderRight: '1px solid var(--line)',
              }}
            >
              <span style={{ fontSize: '11px', color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Store Credit
              </span>
              <span style={{ fontSize: '15px', fontWeight: 600, color: 'var(--wine)' }}>
                ₹{storeCredit.currentBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </span>
            </div>
          )}

          <Button variant="outline" size="sm" onClick={handleLogout}>
            <LogOut size={14} style={{ marginRight: '6px' }} />
            Sign Out
          </Button>
        </div>
      </div>

      {/* Account Navigation Grid: Sidebar + Main Content */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(220px, 260px) 1fr', gap: '28px', alignItems: 'start' }}>
        {/* Navigation Sidebar */}
        <aside
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '12px',
            display: 'flex',
            flexDirection: 'column',
            gap: '4px',
          }}
        >
          {navItems.map((item) => {
            const isActive = pathname === item.href || (item.href !== '/account' && pathname.startsWith(item.href));
            return (
              <Link
                key={item.href}
                href={item.href}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '10px 14px',
                  borderRadius: 'var(--radius-xs)',
                  fontSize: '13px',
                  fontWeight: isActive ? 600 : 400,
                  color: isActive ? 'var(--wine)' : 'var(--ink)',
                  backgroundColor: isActive ? 'var(--wine-soft)' : 'transparent',
                  textDecoration: 'none',
                  transition: 'background-color 0.15s ease',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  {item.icon}
                  <span>{item.label}</span>
                </div>
                {item.badge !== undefined && (
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 600,
                      padding: '2px 8px',
                      borderRadius: '10px',
                      backgroundColor: isActive ? 'var(--wine)' : 'var(--surface-subtle)',
                      color: isActive ? '#ffffff' : 'var(--wine)',
                    }}
                  >
                    {item.badge}
                  </span>
                )}
              </Link>
            );
          })}
        </aside>

        {/* Content Area */}
        <main style={{ minWidth: 0 }}>
          {children}
        </main>
      </div>
    </div>
  );
}
