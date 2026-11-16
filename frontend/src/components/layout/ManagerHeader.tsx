'use client';

import React from 'react';
import { useAuth } from '@/lib/auth/AuthContext';
import { Bell, LogOut, ExternalLink } from 'lucide-react';
import Link from 'next/link';

export interface ManagerHeaderProps {
  title?: string;
}

export const ManagerHeader: React.FC<ManagerHeaderProps> = ({
  title = 'Surat Operations Center',
}) => {
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
          Surat Warehouse Operations &bull; Strictly Operational Role (Zero Financial Visibility)
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {/* Role Demo Switcher */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ fontSize: '11px', color: 'var(--ink-subtle)' }}>Switch Role:</span>
          <button
            type="button"
            onClick={() => switchRoleForDemo('MANAGER')}
            className="badge badge-success"
            style={{ cursor: 'pointer', border: 'none' }}
          >
            Manager
          </button>
          <button
            type="button"
            onClick={() => switchRoleForDemo('OWNER')}
            className="badge badge-neutral"
            style={{ cursor: 'pointer', border: 'none' }}
          >
            Owner
          </button>
        </div>

        <Link
          href="/"
          target="_blank"
          className="btn btn-ghost btn-sm"
          style={{ gap: '4px', fontSize: '12px' }}
        >
          <span>Storefront</span>
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
              backgroundColor: 'var(--color-success-bg)',
              color: 'var(--color-success)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 600,
              fontSize: '12px',
            }}
          >
            MG
          </div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink)' }}>
              {user?.fullName || 'Warehouse Manager'}
            </span>
            <span style={{ fontSize: '10px', color: 'var(--color-success)', fontWeight: 600 }}>
              OPERATIONS
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
