'use client';

import React, { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth/AuthContext';
import { ManagerSidebar } from '@/components/layout/ManagerSidebar';
import { ManagerHeader } from '@/components/layout/ManagerHeader';
import { ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export default function ManagerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuth();

  const isStaff = user?.role === 'MANAGER' || user?.role === 'OWNER';

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login?redirect=/manager');
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return (
      <div style={{ padding: '64px 24px', textAlign: 'center', color: 'var(--ink-muted)' }}>
        Loading Surat Operations workspace...
      </div>
    );
  }

  if (!isAuthenticated || !isStaff) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '24px',
          backgroundColor: 'var(--paper-warm)',
        }}
      >
        <div
          style={{
            maxWidth: '440px',
            width: '100%',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '32px',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: 'var(--error-soft)',
              color: 'var(--error)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '16px',
            }}
          >
            <ShieldAlert size={24} />
          </div>
          <h1 className="font-serif" style={{ margin: '0 0 8px', fontSize: '20px', fontWeight: 500 }}>
            Staff Access Required
          </h1>
          <p style={{ margin: '0 0 24px', fontSize: '13px', color: 'var(--ink-muted)', lineHeight: 1.5 }}>
            Surat warehouse picking, order claiming, and shipment manifests are restricted to verified Operations staff.
          </p>
          <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
            <Button variant="outline" onClick={() => router.push('/')}>
              Storefront
            </Button>
            <Button variant="primary" onClick={() => router.push('/login?redirect=/manager')}>
              Sign In as Manager
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-shell">
      <ManagerSidebar />
      <div className="dashboard-main">
        <ManagerHeader />
        <main className="dashboard-content">{children}</main>
      </div>
    </div>
  );
}
