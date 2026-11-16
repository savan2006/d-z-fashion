'use client';

import React from 'react';
import { useAuth } from '@/lib/auth/AuthContext';
import { Bell, LogOut, ExternalLink } from 'lucide-react';
import Link from 'next/link';

export interface AdminHeaderProps {
  title?: string;
}

export const AdminHeader: React.FC<AdminHeaderProps> = ({ title = 'Owner Control Center' }) => {
  const { user, logout, switchRoleForDemo } = useAuth();

  return (
    <header className="dashboard-header">
      <div>
        <h1
          className="font-serif"
          style={{
            margin: 0,
            fontSize: 'var(--text-heading-md)',
            color: 'var(--ink)',
            fontWeight: 500,
          }}
        >
          {title}
        </h1>
        <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '2px' }}>
          Surat Facility &bull; Authoritative Business Controls
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {/* Role Demo Switcher */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ fontSize: '11px', color: 'var(--ink-subtle)' }}>Preview as:</span>
          <button
            type="button"
            onClick={() => switchRoleForDemo('OWNER')}
            className="badge badge-wine"
            style={{ cursor: 'pointer', border: 'none' }}
          >
            Owner
          </button>
          <button
            type="button"
            onClick={() => switchRoleForDemo('MANAGER')}
            className="badge badge-neutral"
            style={{ cursor: 'pointer', border: 'none' }}
          >
            Manager
          </button>
        </div>

        <Link
          href="/"
          target="_blank"
          className="btn btn-ghost btn-sm"
          style={{ gap: '4px', fontSize: '12px' }}
        >
          <span>Live Store</span>
          <ExternalLink size={14} />
        </Link>

        <button
          type="button"
          className="btn btn-ghost"
          style={{ padding: '8px', minHeight: 'auto', borderRadius: 'var(--radius-sm)' }}
          aria-label="View notifications"
        >
          <Bell size={18} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', borderLeft: '1px solid var(--line)', paddingLeft: '16px' }}>
          <div
            style={{
              width: '32px',
              height: '32px',
              borderRadius: '50%',
              backgroundColor: 'var(--wine-soft)',
              color: 'var(--wine)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 600,
              fontSize: '12px',
            }}
          >
            OW
          </div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink)' }}>
              {user?.fullName || 'Store Owner'}
            </span>
            <span style={{ fontSize: '10px', color: 'var(--wine)', fontWeight: 600 }}>
              FULL ACCESS
            </span>
          </div>
          <button
            type="button"
            onClick={logout}
            className="btn btn-ghost"
            style={{ padding: '6px', minHeight: 'auto', color: 'var(--ink-subtle)' }}
            aria-label="Log out"
          >
            <LogOut size={16} />
          </button>
        </div>
      </div>
    </header>
  );
};
